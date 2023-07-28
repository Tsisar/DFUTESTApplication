package com.example.dfutestapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import androidx.core.app.ActivityCompat;

public class MyService extends Service {
    private static final String TAG = "MyService";
    public static final UUID DFU_V1_CONTROL_SERVICE = UUID.fromString("E95D93B0-251D-470A-A062-FA1922DFA9A8");
    public static final UUID DFU_V1_CONTROL_CHARACTERISTIC = UUID.fromString("E95D93B1-251D-470A-A062-FA1922DFA9A8");
    private final Object mLock = new Object();

    int state = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = -1;
    private static final int STATE_CONNECTING = 0;
    private static final int STATE_CONNECTED = 1;
    private static final int STATE_SERVICES_DISCOVERED = 2;

    int stage = 0;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log(Log.INFO, "Сервіс запущений.");

        nextStage();

        return START_STICKY;
    }

    private void nextStage() {
        String address = "F4:9A:D5:AA:4D:5B";
        //String address = "F49AD5_B";

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (stage == 1) {
            waitFor(2000);
        }

        String isCached = isCached(address, adapter) ? "" : "NOT ";
        log(Log.WARN, "The device is " + isCached + "cached");

        connect(address, adapter);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        log(Log.INFO, "Сервіс зупинений.");
    }

    private boolean isCached(String address, BluetoothAdapter adapter) {
        // Get device object for a mac address
        BluetoothDevice device = adapter.getRemoteDevice(address);
        // Check if the peripheral is cached or not
        int deviceType = device.getType();
        return deviceType != BluetoothDevice.DEVICE_TYPE_UNKNOWN;
    }

    protected void enterToBootloader(BluetoothGatt gatt) {
        BluetoothGattService flashService = gatt.getService(DFU_V1_CONTROL_SERVICE);
        if (flashService == null) {
            log(Log.ERROR, "Cannot find MINI_FLASH_SERVICE_UUID");
            return;
        }

        final BluetoothGattCharacteristic flashServiceCharacteristic = flashService.getCharacteristic(DFU_V1_CONTROL_CHARACTERISTIC);
        if (flashServiceCharacteristic == null) {
            log(Log.ERROR, "Cannot find MINI_FLASH_SERVICE_CONTROL_CHARACTERISTIC_UUID");
            return;
        }

        flashServiceCharacteristic.setValue(1, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        try {
            log(Log.INFO, "Writing Flash Command...");
            gatt.writeCharacteristic(flashServiceCharacteristic);
        } catch (Exception e) {
            e.printStackTrace();
            log(Log.ERROR, e.toString());
        }
    }

    protected void waitFor(final long millis) {
        synchronized (mLock) {
            try {
                mLock.wait(millis);
            } catch (final InterruptedException e) {
                log(Log.ERROR, "Sleeping interrupted. " + e);
            }
        }
    }

    private void connect(String address, BluetoothAdapter adapter) {
        setState(STATE_CONNECTING);
        log(Log.INFO, "Connecting to the device...");
        BluetoothDevice device = adapter.getRemoteDevice(address);
        device.connectGatt(this, false, gattCallback,
                BluetoothDevice.TRANSPORT_LE, BluetoothDevice.PHY_LE_1M_MASK | BluetoothDevice.PHY_LE_2M_MASK);
    }

    protected void clearServicesCache(BluetoothGatt gatt, boolean force) {
        if (force || gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE) {
            try {
                //noinspection JavaReflectionMemberAccess
                final Method refresh = gatt.getClass().getMethod("refresh");
                //noinspection ConstantConditions
                final boolean success = (boolean) refresh.invoke(gatt);
                log(Log.VERBOSE, "Refreshing result: " + success);
            } catch (final Exception e) {
                log(Log.ERROR, "An exception occurred while refreshing device. " + e);
            }
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            log(Log.ASSERT, "onConnectionStateChange(gatt: " + gatt + ", status: " + status + ", newState: " + newState + ")");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    int bondState = gatt.getDevice().getBondState();
                    log(Log.DEBUG, "bondState: " + bondState);

                    setState(STATE_CONNECTED);
                    gatt.discoverServices();
                } else {
                    setState(STATE_DISCONNECTED);
                    gatt.close();
                }
            } else {
                setState(STATE_DISCONNECTED);
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            String methodName = new Object() {
            }.getClass().getEnclosingMethod().getName();
            log(Log.ASSERT, methodName);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                setState(STATE_SERVICES_DISCOVERED);
                if (stage == 0) {
                    enterToBootloader(gatt);
                } else {
                    log(Log.ASSERT, "######## CONNECTED TO DEVICE IN BOOTLOADER ########");
                }
            } else {
                setState(STATE_DISCONNECTED);
                gatt.disconnect();
            }
        }

        // Other methods just pass the parameters through
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            String methodName = new Object() {
            }.getClass().getEnclosingMethod().getName();
            log(Log.ASSERT, methodName);

            if (stage == 0) {
                clearServicesCache(gatt, true);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            String methodName = new Object() {
            }.getClass().getEnclosingMethod().getName();
            log(Log.ASSERT, methodName);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String methodName = new Object() {
            }.getClass().getEnclosingMethod().getName();
            log(Log.ASSERT, methodName);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            String methodName = new Object() {
            }.getClass().getEnclosingMethod().getName();
            log(Log.ASSERT, methodName);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            String methodName = new Object() {
            }.getClass().getEnclosingMethod().getName();
            log(Log.ASSERT, methodName);
        }

        @SuppressLint("NewApi")
        @Override
        public void onMtuChanged(final BluetoothGatt gatt, final int mtu, final int status) {
            String methodName = new Object() {
            }.getClass().getEnclosingMethod().getName();
            log(Log.ASSERT, methodName);
        }

        @SuppressLint("NewApi")
        @Override
        public void onPhyUpdate(final BluetoothGatt gatt, final int txPhy, final int rxPhy, final int status) {
            String methodName = new Object() {
            }.getClass().getEnclosingMethod().getName();
            log(Log.ASSERT, methodName);
        }
    };

    private void setState(int newState) {
        log(Log.WARN, "State: " + newState);
        if (state == STATE_SERVICES_DISCOVERED && newState == STATE_DISCONNECTED) {
            stage++;
            log(Log.ASSERT, "Stage: " + stage);
            nextStage();
        }
        state = newState;
    }
    private static void log(int priority, String message) {
        Log.println(priority, TAG, "### " + android.os.Process.myTid() + " # " + message);
    }
}