package com.utility.mobile.bluetoothle.operations;

import android.util.Log;

import com.utility.mobile.bluetoothle.devices.BluetoothLEDevice;

/**
 * Created by jtorres on 4/20/18.
 */

public class BluetoothLEDiscoverServicesOperation extends BluetoothLEOperation {
    private String TAG = "BluetoothLEDiscoverServ";
    public BluetoothLEDiscoverServicesOperation(BluetoothLEDevice bluetoothLEDevice) {
        super("BluetoothLEDiscoverServicesOperation", bluetoothLEDevice);
    }

    @Override
    public void execute() {
        try {
            if(bluetoothLEDevice.isConnected()) {
                boolean discoverServices = bluetoothLEDevice.getBluetoothGatt().discoverServices();
                Log.d(TAG, "Discover Services started: " + discoverServices);
                executed = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasAvailableCompletionCallback() {
        return executed;
    }
}
