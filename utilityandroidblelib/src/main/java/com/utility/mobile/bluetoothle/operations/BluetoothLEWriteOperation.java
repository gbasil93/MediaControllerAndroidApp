package com.utility.mobile.bluetoothle.operations;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.utility.mobile.bluetoothle.devices.BluetoothLEDevice;
import com.utility.mobile.bluetoothle.utils.BitsAndByteHelper;

/**
 * Created by garrettbasil on 4/16/18.
 */

public class BluetoothLEWriteOperation extends BluetoothLEOperation{

    private final static String TAG = "BluetoothLEWriteOp";

    private BluetoothGattCharacteristic bluetoothGattCharacteristic;
    private byte[] data;
    private boolean ackEnable;

    public BluetoothLEWriteOperation(BluetoothLEDevice bluetoothLEDevice, BluetoothGattCharacteristic bluetoothGattCharacteristic, byte[] data, boolean ackEnable) {
        super("BluetoothLEWriteOperation", bluetoothLEDevice);
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
        this.data = data;
        this.ackEnable = ackEnable;
    }

    @Override
    public void execute() {
        try {
            if(bluetoothLEDevice.isConnected() && bluetoothLEDevice.isServicesDiscovered()) {
                /// Writes to a specific characteristic on the GATT server of a Bluetooth LE device
                bluetoothGattCharacteristic.setValue(data);
                if (ackEnable) {
                    bluetoothGattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                } else {
                    bluetoothGattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                }
                bluetoothLEDevice.getBluetoothGatt().writeCharacteristic(bluetoothGattCharacteristic);
                executed = true;
            } else {
                Log.d(TAG, "isConnected: " + bluetoothLEDevice.isConnected() + "; isServicesDiscovered(): " + bluetoothLEDevice.isServicesDiscovered());
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception executing write operation: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasAvailableCompletionCallback() {
        return executed;
    }

    @Override
    public String toString() {
        try {
            return super.toString() + ". Write operation of value " + BitsAndByteHelper.toHexString(data) + " to characteristic with UUID " + bluetoothGattCharacteristic.getUuid().toString();
        } catch (Exception e) {
            e.printStackTrace();
            return super.toString() + ". Error in parsing data byte array";
        }
    }



}
