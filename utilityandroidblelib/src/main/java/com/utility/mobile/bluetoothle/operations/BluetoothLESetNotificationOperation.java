package com.utility.mobile.bluetoothle.operations;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import com.utility.mobile.bluetoothle.devices.BluetoothLEDevice;

/**
 * Created by garrettbasil on 4/16/18.
 */

public class BluetoothLESetNotificationOperation extends BluetoothLEOperation {

    private BluetoothGattCharacteristic bluetoothGattCharacteristic;
    private BluetoothGattDescriptor bluetoothGattDescriptor;

    public BluetoothLESetNotificationOperation(BluetoothLEDevice bluetoothLEDevice, BluetoothGattCharacteristic bluetoothGattCharacteristic, BluetoothGattDescriptor bluetoothGattDescriptor) {
        super("BluetoothLESetNotificationOperation", bluetoothLEDevice);
        this.bluetoothGattCharacteristic  = bluetoothGattCharacteristic;
        this.bluetoothGattDescriptor = bluetoothGattDescriptor;
    }

    @Override
    public void execute() {
        try {
            if(bluetoothLEDevice.isConnected() && bluetoothLEDevice.isServicesDiscovered()) {
                if (bluetoothGattDescriptor != null) {
                    if (bluetoothGattDescriptor.getValue().equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                        bluetoothLEDevice.getBluetoothGatt().setCharacteristicNotification(bluetoothGattCharacteristic, false);
                    } else {
                        bluetoothLEDevice.getBluetoothGatt().setCharacteristicNotification(bluetoothGattCharacteristic, true);
                    }
                    sleepAway(1000);
                    bluetoothLEDevice.getBluetoothGatt().writeDescriptor(bluetoothGattDescriptor);
                } else {
                    bluetoothLEDevice.getBluetoothGatt().setCharacteristicNotification(bluetoothGattCharacteristic, true);
                }
                executed = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasAvailableCompletionCallback() {
        return true;
    }

    @Override
    public String toString() {
        if(bluetoothGattDescriptor != null) {
            if (bluetoothGattDescriptor.getValue().equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                return super.toString() + ". Disabling notification on characteristic with UUID " + bluetoothGattCharacteristic.getUuid().toString();
            } else {
                return super.toString() + ". Enabling notification on characteristic with UUID " + bluetoothGattCharacteristic.getUuid().toString();
            }
        } else {
            return super.toString() + ". Enabling notification on characteristic with UUID " + bluetoothGattCharacteristic.getUuid().toString();
        }
    }
}
