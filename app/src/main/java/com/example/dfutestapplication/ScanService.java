package com.example.dfutestapplication;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.core.app.ActivityCompat;

public class ScanService extends Service {
    private static final String TAG = "ScanService";
    BluetoothAdapter adapter;
    BluetoothLeScanner scanner;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log(Log.INFO, "Сервіс запущений.");

        adapter = BluetoothAdapter.getDefaultAdapter();

        String isCached = isCached("F4:9A:D5:AA:4D:5B") ? "" : "NOT ";
        log(Log.WARN, "The device is " + isCached + "cached");

        scanner = adapter.getBluetoothLeScanner();

        scan(new String[]{"F4:9A:D5:AA:4D:5B"});

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scanner.stopScan(scanCallback);
        log(Log.INFO, "Сервіс зупинений.");
    }

    private boolean isCached(String address) {
        // Get device object for a mac address
        BluetoothDevice device = adapter.getRemoteDevice(address);
        int deviceType = device.getType();
        return deviceType != BluetoothDevice.DEVICE_TYPE_UNKNOWN;
    }

    private void scan(String[] peripheralAddresses) {
        List<ScanFilter> filters = null;
        if (peripheralAddresses != null) {
            filters = new ArrayList<>();
            for (String address : peripheralAddresses) {
                ScanFilter filter = new ScanFilter.Builder()
                        .setDeviceAddress(address)
                        .build();
                filters.add(filter);
            }
        }

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .setReportDelay(0L)
                .build();

        if (scanner != null) {
            scanner.startScan(filters, scanSettings, scanCallback);
            log(Log.VERBOSE, "scan started");
        } else {
            log(Log.ERROR, "could not get scanner object");
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            log(Log.ASSERT, "BluetoothDevice: " + device + ", RSSI: " + result.getRssi());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            // Ignore for now
        }

        @Override
        public void onScanFailed(int errorCode) {
            // Ignore for now
        }
    };

    private static void log(int priority, String message) {
        Log.println(priority, TAG, "### " + android.os.Process.myTid() + " # " + message);
    }
}