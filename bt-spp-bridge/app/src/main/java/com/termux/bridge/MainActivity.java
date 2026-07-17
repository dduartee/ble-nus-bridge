package com.termux.bridge;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Set;

public class MainActivity extends Activity implements View.OnClickListener {

    private BluetoothAdapter bluetoothAdapter;
    private LinearLayout deviceList;
    private TextView statusText;
    private HashMap<View, BluetoothDevice> buttonDeviceMap = new HashMap<>();
    private HashMap<String, Boolean> seenDevices = new HashMap<>();
    private boolean isScanning = false;
    private TextView dataLog;
    private int totalBytes = 0;

    // ── Named inner classes (d8 requer, não suporta anônimas) ──

    private class ScanClickListener implements View.OnClickListener {
        public void onClick(View v) { startDiscovery(); }
    }

    private class DeviceClickListener implements View.OnClickListener {
        private final BluetoothDevice device;
        DeviceClickListener(BluetoothDevice device) { this.device = device; }
        public void onClick(View v) { connectToDevice(device); }
    }

    private class DiscoveryReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                if (device != null && device.getName() != null && !seenDevices.containsKey(device.getAddress())) {
                    seenDevices.put(device.getAddress(), true);
                    addDeviceButton(device);
                    statusText.setText("📡 Escaneando... " + seenDevices.size() + " encontrados");
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                isScanning = false;
                statusText.setText("📱 " + seenDevices.size() + " dispositivo(s) encontrado(s)");
            }
        }
    }

    private BroadcastReceiver discoveryReceiver = new DiscoveryReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerReceiver(discoveryReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(discoveryReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        // Receiver for BLE data preview from BridgeService
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String preview = intent.getStringExtra("preview");
                int bytes = intent.getIntExtra("bytes", 0);
                totalBytes += bytes;
                String log = "📦 " + totalBytes + "B total | " + bytes + "B now\n" + preview;
                if (preview != null) {
                    dataLog.setText(log);
                }
            }
        }, new IntentFilter("DATA_RECEIVED"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(new String[]{
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            }, 1);
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            statusText = new TextView(this);
            statusText.setText("❌ Bluetooth não disponível");
            setContentView(statusText);
            return;
        }

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("🔵 BT SPP Bridge");
        title.setTextSize(20);
        root.addView(title);

        statusText = new TextView(this);
        statusText.setTextSize(14);
        statusText.setPadding(0, 16, 0, 16);
        root.addView(statusText);

        deviceList = new LinearLayout(this);
        deviceList.setOrientation(LinearLayout.VERTICAL);
        root.addView(deviceList);

        // Scan button
        Button scanBtn = new Button(this);
        scanBtn.setText("🔍 Escanear dispositivos");
        scanBtn.setOnClickListener(new ScanClickListener());
        root.addView(scanBtn);

        // Refresh paired button
        Button refreshBtn = new Button(this);
        refreshBtn.setText("🔄 Pareados");
        refreshBtn.setOnClickListener(this);
        root.addView(refreshBtn);

        TextView tcpInfo = new TextView(this);
        tcpInfo.setText("\n📡 Após conectar: nc localhost 8090");
        tcpInfo.setTextSize(12);
        tcpInfo.setPadding(0, 24, 0, 0);
        root.addView(tcpInfo);

        dataLog = new TextView(this);
        dataLog.setTextSize(10);
        dataLog.setText("📦 Aguardando dados...");
        dataLog.setPadding(0, 12, 0, 0);
        dataLog.setBackgroundColor(0xFF1A1A2E);
        dataLog.setTextColor(0xFF00FF88);
        root.addView(dataLog);

        scroll.addView(root);
        setContentView(scroll);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                loadPairedDevices();
            } else {
                statusText.setText("🔐 Permita o acesso Bluetooth para ver dispositivos");
            }
        } else {
            loadPairedDevices();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(discoveryReceiver); } catch (Exception ignored) {}
        if (isScanning && bluetoothAdapter != null) bluetoothAdapter.cancelDiscovery();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadPairedDevices();
        }
    }

    @Override
    public void onClick(View v) {
        BluetoothDevice device = buttonDeviceMap.get(v);
        if (device != null) {
            connectToDevice(device);
        } else {
            loadPairedDevices();
        }
    }

    private void startDiscovery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                }, 2);
                return;
            }
        }
        if (isScanning) {
            bluetoothAdapter.cancelDiscovery();
        }
        seenDevices.clear();
        deviceList.removeAllViews();
        buttonDeviceMap.clear();
        isScanning = true;
        bluetoothAdapter.startDiscovery();
        statusText.setText("📡 Escaneando...");
    }

    private void loadPairedDevices() {
        if (!isScanning) {
            deviceList.removeAllViews();
            buttonDeviceMap.clear();
            seenDevices.clear();
        }

        if (!bluetoothAdapter.isEnabled()) {
            statusText.setText("Ligue o Bluetooth primeiro");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                statusText.setText("🔐 Permissão necessária");
                return;
            }
        }

        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            String name = device.getName();
            if (name != null && !seenDevices.containsKey(device.getAddress())) {
                seenDevices.put(device.getAddress(), true);
                addDeviceButton(device);
            }
        }
        statusText.setText("📱 " + seenDevices.size() + " dispositivo(s)");
    }

    private void addDeviceButton(BluetoothDevice device) {
        Button btn = new Button(this);
        String name = device.getName() != null ? device.getName() : "Sem nome";
        btn.setText("📱 " + name + "\n" + device.getAddress());
        btn.setTextSize(11);
        btn.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        btn.setOnClickListener(new DeviceClickListener(device));
        deviceList.addView(btn);
    }

    private void connectToDevice(BluetoothDevice device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                Toast.makeText(this, "Permita o Bluetooth primeiro", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Toast.makeText(this, "Conectando " + device.getName() + "...", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, BridgeService.class);
        intent.putExtra("device_address", device.getAddress());
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            statusText.setText("🔗 Conectado: " + device.getName() + "\n📡 TCP: localhost:8090");
        } catch (Exception e) {
            Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
