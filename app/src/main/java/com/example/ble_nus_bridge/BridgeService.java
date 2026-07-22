package com.example.ble_nus_bridge;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressLint("MissingPermission")
public class BridgeService extends Service {

    // Nordic UART Service (NUS) UUIDs
    private static final UUID NUS_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID NUS_TX_CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E"); // ESP32 -> phone (notify)
    private static final UUID NUS_RX_CHAR_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E"); // phone -> ESP32 (write)
    private static final UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");
    private static final int TCP_PORT = 8090;
    private static final String CHANNEL_ID = "bridge_service";
    private static final int NOTIFY_ID = 1;

    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic txCharacteristic;
    private BluetoothGattCharacteristic rxCharacteristic;
    private boolean bleReady = false;
    private ServerSocket tcpServer;
    private Socket tcpClient;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String address = intent.getStringExtra("device_address");
        if (address == null) return START_NOT_STICKY;

        try {
            startForeground(NOTIFY_ID, buildNotification("Conectando..."));
        } catch (Exception e) {
            android.util.Log.e("BridgeService", "startForeground failed: " + e.getMessage());
            // Continue anyway on older Android or if notification permission not granted
        }
        new Thread(new BleConnector(), "BLE-Scan").start();
        return START_STICKY;
    }

    // ---- Named inner classes (D8 crashes on anonymous classes) ----

    private class BleConnector implements Runnable {
        BleConnector() { }

        public void run() {
            try {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter == null || !adapter.isEnabled()) {
                    updateNotification("Bluetooth desligado");
                    return;
                }

                BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
                if (scanner == null) {
                    updateNotification("BLE scanner indisponivel");
                    return;
                }

                updateNotification("Procurando track-kinesis...");

                /* No UUID filter — ESP32 advertises UUID in scan response
                 * (separate 31-byte budget), not in main advertising data.
                 * Filter by device name in onScanResult below. */
                ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

                ScanCallback scanCallback = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        BluetoothDevice device = result.getDevice();
                        String name = device.getName();
                        if (name != null && name.equals("track-kinesis")) {
                            scanner.stopScan(this);
                            updateNotification("Conectando " + name + "...");
                            bluetoothGatt = device.connectGatt(BridgeService.this, false, gattCallback);
                        }
                    }

                    @Override
                    public void onScanFailed(int errorCode) {
                        updateNotification("Scan falhou: " + errorCode);
                    }
                };

                scanner.startScan(null, settings, scanCallback);

            } catch (SecurityException e) {
                updateNotification("Permissao Bluetooth negada");
            } catch (Exception e) {
                updateNotification("Erro: " + e.getMessage());
            }
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                bleReady = false;
                // Request larger MTU so full IMU frames (~110 bytes) fit in one notification.
                // Default MTU is 23 bytes (20-byte payload) — way too small.
                // discoverServices() is called from onMtuChanged callback.
                gatt.requestMtu(256);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                bleReady = false;
                txCharacteristic = null;
                rxCharacteristic = null;
                updateNotification("Desconectado");

                Intent discIntent = new Intent("DATA_RECEIVED");
                discIntent.setPackage(getPackageName());
                discIntent.putExtra("preview", "❌ Desconectado");
                discIntent.putExtra("bytes", 0);
                sendBroadcast(discIntent);

                // Restart scanning
                new Thread(new BleConnector(), "BLE-Scan").start();
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            // MTU negotiated — now discover services
            gatt.discoverServices();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                updateNotification("Servicos nao encontrados");
                return;
            }

            BluetoothGattService nusService = gatt.getService(NUS_SERVICE_UUID);
            if (nusService == null) {
                updateNotification("NUS service nao encontrado");
                return;
            }

            txCharacteristic = nusService.getCharacteristic(NUS_TX_CHAR_UUID);
            rxCharacteristic = nusService.getCharacteristic(NUS_RX_CHAR_UUID);

            if (txCharacteristic == null) {
                updateNotification("TX char nao encontrado");
                return;
            }

            // Enable notifications on TX characteristic
            gatt.setCharacteristicNotification(txCharacteristic, true);
            BluetoothGattDescriptor cccd = txCharacteristic.getDescriptor(CCCD_UUID);
            if (cccd != null) {
                cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(cccd);
            }

            // Request high-priority connection for low latency
            gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);

            BluetoothDevice device = gatt.getDevice();
            bleReady = true;

            // Notify ESP32 that phone is connected
            if (rxCharacteristic != null) {
                byte[] connectedMsg = "connected\n".getBytes(StandardCharsets.UTF_8);
                rxCharacteristic.setValue(connectedMsg);
                gatt.writeCharacteristic(rxCharacteristic);
            }

            // Broadcast to MainActivity
            Intent connIntent = new Intent("DATA_RECEIVED");
            connIntent.setPackage(getPackageName());
            connIntent.putExtra("preview", "🔗 Conectado a " + device.getName());
            connIntent.putExtra("bytes", 0);
            sendBroadcast(connIntent);

            updateNotification(device.getName() + " | TCP :" + TCP_PORT);
            startTcpBridge();
            running.set(true);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(NUS_TX_CHAR_UUID)) {
                byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    sendToTcp(data, data.length);

                    // Broadcast to MainActivity for display
                    Intent dataIntent = new Intent("DATA_RECEIVED");
                    dataIntent.setPackage(getPackageName());
                    String preview = new String(data, 0, Math.min(data.length, 120), StandardCharsets.UTF_8);
                    dataIntent.putExtra("preview", preview);
                    dataIntent.putExtra("bytes", data.length);
                    sendBroadcast(dataIntent);
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            // CCCD written — notifications are now active
        }
    };

    private class TcpAcceptor implements Runnable {
        public void run() {
            try {
                tcpServer = new ServerSocket(TCP_PORT);
                tcpServer.setReuseAddress(true);
                while (running.get()) {
                    tcpClient = tcpServer.accept();
                    new Thread(new TcpReader(), "TCP2BT").start();
                }
            } catch (IOException e) { /* server stopped */ }
        }
    }

    private class TcpReader implements Runnable {
        public void run() {
            try {
                InputStream tcpIn = tcpClient.getInputStream();
                byte[] buf = new byte[1024];
                while (running.get()) {
                    int len = tcpIn.read(buf);
                    if (len < 0) break;
                    if (rxCharacteristic != null && bleReady) {
                        byte[] txData = java.util.Arrays.copyOfRange(buf, 0, len);
                        rxCharacteristic.setValue(txData);
                        bluetoothGatt.writeCharacteristic(rxCharacteristic);
                    }
                }
            } catch (IOException e) { /* client disconnected */ }
        }
    }

    // ---- Bridge methods ----

    private void startTcpBridge() {
        Thread t = new Thread(new TcpAcceptor(), "TCP-Accept");
        t.setDaemon(true);
        t.start();
    }

    private void sendToTcp(byte[] data, int len) {
        if (tcpClient != null && tcpClient.isConnected()) {
            try {
                tcpClient.getOutputStream().write(data, 0, len);
                tcpClient.getOutputStream().flush();
            } catch (IOException ignored) {}
        }
    }

    private void stop() {
        running.set(false);
        try { if (bluetoothGatt != null) bluetoothGatt.close(); } catch (Exception ignored) {}
        try { if (tcpServer != null) tcpServer.close(); } catch (Exception ignored) {}
        try { if (tcpClient != null) tcpClient.close(); } catch (Exception ignored) {}
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stop();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    // ---- Notifications ----

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Bridge Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String text) {
        Intent intent = new Intent(this, MainActivity.class);
        int flags = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, flags);
        return new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("BLE NUS Bridge")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentIntent(pi)
            .setOngoing(true)
            .build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFY_ID, buildNotification(text));
    }
}
