package com.utility.mobile.bluetoothle.operations;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.utility.mobile.bluetoothle.devices.BluetoothLEDevice;

/**
 * Created by garrettbasil on 4/16/18.
 */

public class BluetoothLEReadOperation extends BluetoothLEOperation {
    private final static String TAG = "BluetoothLEReadOp";

    private BluetoothGattCharacteristic bluetoothGattCharacteristic;

    public BluetoothLEReadOperation(BluetoothLEDevice bluetoothLEDevice, BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        super("BluetoothLEReadOperation", bluetoothLEDevice);
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
    }

    @Override
    public void execute() {
        try {
            if(bluetoothLEDevice.isConnected() && bluetoothLEDevice.isServicesDiscovered()) {
                bluetoothLEDevice.getBluetoothGatt().readCharacteristic(bluetoothGattCharacteristic);
                executed = true;
            } else {
                Log.d(TAG, "isConnected: " + bluetoothLEDevice.isConnected() + "; isServicesDiscovered(): " + bluetoothLEDevice.isServicesDiscovered());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasAvailableCompletionCallback() {
        return executed;
    }

    @Override
    public String toString() {
        return super.toString() + ". Read operation of characteristic with UUID " + bluetoothGattCharacteristic.getUuid().toString();
    }
}
