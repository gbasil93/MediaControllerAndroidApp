package com.utility.mobile.bluetoothle.operations;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import com.utility.mobile.bluetoothle.devices.BluetoothLEDevice;

/**
 * Created by jtorres on 4/20/18.
 */

public class BluetoothLEConnectOperation extends BluetoothLEOperation {

    private static final String TAG = "BluetoothLEConnect";

    private Context context;
    private boolean hasCallback;
    private boolean autoConnect;

    public BluetoothLEConnectOperation(Context context, BluetoothLEDevice bluetoothLEDevice, boolean autoConnect) {
        super("BluetoothLEConnectOperation", bluetoothLEDevice);
        this.context = context;
        this.autoConnect = autoConnect;
    }

    @Override
    public void execute() {
        hasCallback = false;
        try {
            BluetoothDevice bluetoothDevice = bluetoothLEDevice.getBluetoothDevice();
            String deviceName = bluetoothLEDevice.getDeviceName();
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            int connectionState = bluetoothManager.getConnectionState(bluetoothDevice, BluetoothProfile.GATT);

            if (connectionState == BluetoothGatt.STATE_DISCONNECTED) {
                hasCallback = true;
                Log.d(TAG, "Starting connection attempt for device " + deviceName + " with autoconnect set to: " + autoConnect);
                bluetoothLEDevice.connectGatt(autoConnect);
                Log.d(TAG, "device " + deviceName + " is in connection state " + connectionState);
            }else{
                Log.d(TAG, "Device is already connected");
            }
            executed = true;
        }catch(Exception e){
            Log.e(TAG, "Failed to execute connect operation", e);
        }
    }

    @Override
    public boolean hasAvailableCompletionCallback() {
        return hasCallback;
    }
}
