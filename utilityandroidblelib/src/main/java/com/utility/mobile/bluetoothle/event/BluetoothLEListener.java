package com.utility.mobile.bluetoothle.event;

import android.bluetooth.BluetoothDevice;

public interface BluetoothLEListener {
    public void onScanResult(BluetoothDevice device, byte[] manufacturerSpecificData);
    public void onDeviceBonded(BluetoothDevice device, int state);
    public void onBluetoothAdapterStateChange(int state);
    public void onBluetoothDeviceConnected(BluetoothDevice bluetoothDevice);
    public void onBluetoothDeviceDisconnected(BluetoothDevice bluetoothDevice);
}
