package com.utility.mobile.bluetoothle.operations;

import android.bluetooth.BluetoothGatt;
import android.util.Log;

import com.utility.mobile.bluetoothle.devices.BluetoothLEDevice;


/**
 * Created by jtorres on 4/24/18.
 */

public class BluetoothLEDisconnectOperation extends BluetoothLEOperation {

    private static final String TAG = "BluetoothLEDisconnect";


    public BluetoothLEDisconnectOperation(BluetoothLEDevice bluetoothLEDevice) {
        super("BluetoothLEDisconnectOperation", bluetoothLEDevice);
    }

    @Override
    public void execute() {
        try {
            BluetoothGatt bluetoothGatt = bluetoothLEDevice.getBluetoothGatt();
            if (bluetoothGatt != null) {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
                sleepAway(1000);
            } else {
                Log.d(TAG, bluetoothLEDevice.getDeviceName() + " Bluetooth GATT was null");
            }
            executed = true;
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasAvailableCompletionCallback() {
        return false;
    }
}
