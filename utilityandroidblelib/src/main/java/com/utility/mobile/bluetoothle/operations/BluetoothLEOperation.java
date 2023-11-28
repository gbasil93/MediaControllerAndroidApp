package com.utility.mobile.bluetoothle.operations;

import com.utility.mobile.bluetoothle.devices.BluetoothLEDevice;

/**
 * Created by garrettbasil on 4/16/18.
 */

public abstract class BluetoothLEOperation {

    protected BluetoothLEDevice bluetoothLEDevice;
    protected String operationName;
    protected boolean executed;

    public BluetoothLEOperation(String operationName, BluetoothLEDevice bluetoothLEDevice) {
        this.operationName = operationName;
        this.bluetoothLEDevice = bluetoothLEDevice;
    }

    public abstract void execute();
    public abstract boolean hasAvailableCompletionCallback();

    public void sleepAway(long millis){
        try{
            Thread.sleep(millis);
        }catch (Exception e){

        }
    }


    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("BluetoothLEOperation{");
        sb.append("bluetoothLEDevice=").append(bluetoothLEDevice.getDeviceName());
        sb.append(", operationName='").append(operationName).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public boolean isDeviceConnected() {
        return bluetoothLEDevice.isConnected();
    }
    public String getOperationName() { return operationName; }
public String getDeviceAddress() { return bluetoothLEDevice.getMacAddress(); }
}
