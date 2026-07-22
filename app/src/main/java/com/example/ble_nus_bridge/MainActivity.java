package com.example.ble_nus_bridge;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Set;

@SuppressLint("MissingPermission")
public class MainActivity extends Activity implements View.OnClickListener {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private LinearLayout deviceList;
    private TextView statusText;
    private TextView dataLog;
    private HashMap<View, BluetoothDevice> buttonDeviceMap = new HashMap<>();
    private HashMap<String, Boolean> seenDevices = new HashMap<>();
    private boolean isScanning = false;
    private int totalBytes = 0;
    private final Handler scanHandler = new Handler(Looper.getMainLooper());
    private BroadcastReceiver dataReceiver;

    private class ScanClickListener implements View.OnClickListener {
        public void onClick(View v) { startBleScan(); }
    }

    private class DeviceClickListener implements View.OnClickListener {
        private final BluetoothDevice device;
        DeviceClickListener(BluetoothDevice device) { this.device = device; }
        public void onClick(View v) { connectToDevice(device); }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String address = device.getAddress();
            if (!seenDevices.containsKey(address)) {
                seenDevices.put(address, true);
                runOnUiThread(() -> {
                    addDeviceButton(device);
                    statusText.setText("📡 Escaneando... " + seenDevices.size() + " encontrados");
                });
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            runOnUiThread(() -> {
                isScanning = false;
                statusText.setText("❌ Erro no scan: " + errorCode);
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        deviceList = findViewById(R.id.deviceList);
        dataLog = findViewById(R.id.dataLog);

        Button scanBtn = findViewById(R.id.scanBtn);
        Button refreshBtn = findViewById(R.id.refreshBtn);
        scanBtn.setOnClickListener(new ScanClickListener());
        refreshBtn.setOnClickListener(this);

        dataReceiver = new BroadcastReceiver() {
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
        };
        ContextCompat.registerReceiver(this, dataReceiver, new IntentFilter("DATA_RECEIVED"), ContextCompat.RECEIVER_NOT_EXPORTED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(new String[]{
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            }, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{
                Manifest.permission.POST_NOTIFICATIONS
            }, 1);
        }

        BluetoothManager btManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = btManager != null ? btManager.getAdapter() : null;
        if (bluetoothAdapter == null) {
            statusText.setText("❌ Bluetooth não disponível");
            return;
        }
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();

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
        if (dataReceiver != null) {
            try { unregisterReceiver(dataReceiver); } catch (Exception ignored) {}
        }
        stopBleScan();
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

    private void startBleScan() {
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
            stopBleScan();
        }

        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bleScanner == null) {
            statusText.setText("❌ BLE Scanner não disponível");
            return;
        }

        seenDevices.clear();
        deviceList.removeAllViews();
        buttonDeviceMap.clear();
        isScanning = true;
        bleScanner.startScan(scanCallback);
        statusText.setText("📡 Escaneando BLE...");

        // Auto-stop after 12 seconds
        scanHandler.postDelayed(this::stopBleScan, 12000);
    }

    private void stopBleScan() {
        if (isScanning && bleScanner != null) {
            bleScanner.stopScan(scanCallback);
        }
        isScanning = false;
        scanHandler.removeCallbacksAndMessages(null);
        if (seenDevices.isEmpty()) {
            statusText.setText("📱 Nenhum dispositivo BLE encontrado");
        } else {
            statusText.setText("📱 " + seenDevices.size() + " dispositivo(s) encontrado(s)");
        }
    }

    private void loadPairedDevices() {
        deviceList.removeAllViews();
        buttonDeviceMap.clear();
        seenDevices.clear();

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
