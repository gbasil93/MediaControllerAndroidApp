package com.utility.mobile.bluetoothle.event;

import android.bluetooth.BluetoothDevice;

import com.utility.mobile.bluetoothle.utils.EventDispatcher;

import java.util.Iterator;

public class BluetoothLEEventDispatcher extends EventDispatcher<BluetoothLEListener>{

    private static BluetoothLEEventDispatcher instance;

    private BluetoothLEEventDispatcher(){}

    public static synchronized BluetoothLEEventDispatcher getInstance(){
        if(instance == null){
            instance = new BluetoothLEEventDispatcher();
        }
        return instance;
    }

    public void sendOnScanResultEvent(BluetoothDevice device, byte[] manufacturerSpecificData){
        startedIterating();
        Iterator<BluetoothLEListener> iterator = listeners.iterator();
        while(iterator.hasNext()){
            BluetoothLEListener listener = iterator.next();
            listener.onScanResult(device, manufacturerSpecificData);

        }
        finishedIterating();
    }

    public void sendOnDeviceBondedStateChange(BluetoothDevice device, int state){
        startedIterating();
        Iterator<BluetoothLEListener> iterator = listeners.iterator();
        while(iterator.hasNext()){
            BluetoothLEListener listener = iterator.next();
            listener.onDeviceBonded(device, state);

        }
        finishedIterating();
    }

    public void sendOnBluetoothAdapterStateChanged(int state){
        startedIterating();
        Iterator<BluetoothLEListener> iterator = listeners.iterator();
        while(iterator.hasNext()){
            BluetoothLEListener listener = iterator.next();
            listener.onBluetoothAdapterStateChange(state);

        }
        finishedIterating();
    }

    public void sendOnBluetoothDeviceConnected(BluetoothDevice device){
        startedIterating();
        Iterator<BluetoothLEListener> iterator = listeners.iterator();
        while(iterator.hasNext()){
            BluetoothLEListener listener = iterator.next();
            listener.onBluetoothDeviceConnected(device);

        }
        finishedIterating();
    }

    public void sendOnBluetoothDeviceDisconnected(BluetoothDevice device){
        startedIterating();
        Iterator<BluetoothLEListener> iterator = listeners.iterator();
        while(iterator.hasNext()){
            BluetoothLEListener listener = iterator.next();
            listener.onBluetoothDeviceDisconnected(device);

        }
        finishedIterating();
    }
}
