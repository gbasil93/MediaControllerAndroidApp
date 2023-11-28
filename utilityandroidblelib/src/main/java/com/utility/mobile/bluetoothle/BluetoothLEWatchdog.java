package com.utility.mobile.bluetoothle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanSettings;
import android.os.SystemClock;
import android.util.Log;

import com.utility.mobile.bluetoothle.devices.BluetoothLEDevice;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by garrettbasil on 4/16/18.
 */

public class BluetoothLEWatchdog extends Thread {

    private String TAG = "BluetoothLEWatchdog";

    private final static int BLUETOOTH_REQUEST_TIMEOUT = 15000;
    private final static int SCANNING_TIMEOUT = 120000;
    private final static int SCAN_RESULT_TIMEOUT = 5000;
    private final static int DISCOVER_SERVICES_TIMEOUT = 10000;
    private final static int BATTERY_CHECK_INTERVAL_DEFAULT = 300000; // five minutes

    private ConcurrentHashMap<String, BluetoothLEDevice> devicesList;
    private ConcurrentHashMap<String, Integer> scanFilterList;
    private boolean stopThread;

    private long lastScanResultTime;
    private int batteryCheckInterval;

    public BluetoothLEWatchdog(ConcurrentHashMap<String, BluetoothLEDevice> devicesList){
        this.devicesList = devicesList;
        this.batteryCheckInterval = BATTERY_CHECK_INTERVAL_DEFAULT;
    }

    public BluetoothLEWatchdog(ConcurrentHashMap<String, BluetoothLEDevice> devicesList, int batteryCheckInterval){
        this.devicesList = devicesList;
        this.batteryCheckInterval = batteryCheckInterval;

    }

    public void updateScanFilterList(ConcurrentHashMap<String, Integer> scanFilterList) {
        this.scanFilterList = scanFilterList;
        this.interrupt();
    }

    private void printDisconnectCounts() {
        Log.d(TAG, "Printing disconnect counts, list size: " + devicesList.size());
        for(BluetoothLEDevice device : devicesList.values()) {
            Log.d(TAG, "Device " + device.getDeviceName() + " isConnected: " + device.isConnected() + " and has been disconnected " + device.getDisconnectCount() + " times");
        }
    }

    private void checkDevices() {

        boolean deviceTimedOut = false;
        boolean deviceDiscoveredServices = false;
        for(BluetoothLEDevice device : devicesList.values()) {
            try {
                if(device.isServicesDiscovered()) {
                    deviceDiscoveredServices = true;
                }else if (device.isConnected()) {
                    long diff = SystemClock.elapsedRealtime() - device.getLastDiscoverServicesCallTime();
                    if (diff > DISCOVER_SERVICES_TIMEOUT) {
                        Log.d(TAG, "device is connected but services have not been discovered after " + diff + " milliseconds, disconnecting device");

                        deviceTimedOut = true;
                    }
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
        if(!deviceDiscoveredServices && deviceTimedOut) {
            try {
                BluetoothAdapter.getDefaultAdapter().disable();
                Thread.sleep(2000);
                BluetoothAdapter.getDefaultAdapter().enable();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void checkRequestCycleTimes() {
        BluetoothLECommQueue queue = BluetoothLECommQueue.getInstance();
        if(!queue.isOperationQueueEmpty()) {
            long millisSinceLastRequest = SystemClock.elapsedRealtime() - queue.getLastOperationTime();
            Log.d(TAG, "millisSinceLastRequest = " + millisSinceLastRequest);
            if(millisSinceLastRequest > BLUETOOTH_REQUEST_TIMEOUT) {
                Log.d(TAG, "Posting request from watchdog");
                queue.executeCurrentOperation();
            }
        } else {
            Log.d(TAG, "Device is not connected or its queue is empty");
        }
    }

    private boolean shouldPreFilter() {
        for(Integer type : scanFilterList.values()) {
            Log.d(TAG, "shouldPreFilter() type: " + type);
            if(type > BluetoothLEManager.DEVICE_TYPE_FULL_NAME) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldBeActiveScanning() {
        // Cycle through bonded devices to see if they're things we care about
        for(BluetoothDevice bondedDevice : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
            BluetoothLEDevice bluetoothLEDevice = devicesList.get(bondedDevice.getAddress());
            if(bluetoothLEDevice == null || !bluetoothLEDevice.isConnected()) {
                for(String filterKey : scanFilterList.keySet()) {
                    if(scanFilterList.get(filterKey) == BluetoothLEManager.DEVICE_TYPE_FULL_MAC_ADDRESS) {
                        if(bondedDevice.getAddress().equals(filterKey)) {
                            return true;
                        }
                    } else if (scanFilterList.get(filterKey) == BluetoothLEManager.DEVICE_TYPE_FULL_NAME) {
                        if(bondedDevice.getName().equals(filterKey)) {
                            return true;
                        }
                    } else if(scanFilterList.get(filterKey) == BluetoothLEManager.DEVICE_TYPE_PARTIAL_MAC_ADDRESS) {
                        if(bondedDevice.getAddress().contains(filterKey)){
                            return true;
                        }
                    } else if(scanFilterList.get(filterKey) == BluetoothLEManager.DEVICE_TYPE_PARTIAL_NAME) {
                        if(bondedDevice.getName().contains(filterKey)) {
                            return true;
                        }
                    }
                }
            } else if(!bluetoothLEDevice.isConnected()) {
                return true;
            }else {
                Log.d(TAG, bondedDevice.getName() + " is already connected");
            }
        }
        return false;
    }

    private void checkScanner() {
        BluetoothLEManager manager = BluetoothLEManager.getInstance();
        Log.d(TAG, "Checking scanner");
        if(shouldBeActiveScanning()) {
            Log.d(TAG, "Should be active scanning, isScanning: " + manager.isScanning() + "scanMode: " + manager.getScanSettings().getScanMode());
            if(manager.isScanning() && manager.getScanSettings().getScanMode() == ScanSettings.SCAN_MODE_LOW_LATENCY) {
                long elapsedScanTime = SystemClock.elapsedRealtime() - manager.getScanningStartTime();
                long elapsedScanResultTime = SystemClock.elapsedRealtime() - getLastScanResultTime();

                if(elapsedScanTime > SCANNING_TIMEOUT || elapsedScanResultTime > SCAN_RESULT_TIMEOUT) {
                    manager.stopScanning();
                    if(manager.isPassiveScanEnabled()) {
                        manager.startPassiveScan(scanFilterList, shouldPreFilter());
                    }
                    Log.d(TAG, "Stopping scanner, elapsedScanTime " + elapsedScanTime + " elapsedScanResultTime: " + elapsedScanResultTime);
                }
            } else {
                Log.d(TAG, "Starting low latency scan");
                lastScanResultTime = SystemClock.elapsedRealtime();
                manager.startLowLatencyScan(shouldPreFilter());
            }
        } else {
            if(manager.isPassiveScanEnabled()) {
                Log.d(TAG, "Starting passive scan");
                manager.startPassiveScan(scanFilterList, shouldPreFilter());
            } else {
                manager.stopScanning();
            }
        }
    }

    public void stopThread(){
        stopThread = true;
    }


    @Override
    public void run() {
        try {
            while(!stopThread) {
                try {
                    if (BluetoothAdapter.getDefaultAdapter().getState() != BluetoothAdapter.STATE_OFF && BluetoothAdapter.getDefaultAdapter().getState() != BluetoothAdapter.STATE_TURNING_OFF) {
                        Log.d(TAG, "Executing watchdog activities");
                        checkDevices();
                        checkRequestCycleTimes();
                        checkScanner();
                        printDisconnectCounts();
                    } else {
                        Log.d(TAG, "Bluetooth adapter is either off or turning off");
//                    Logger.writeToFile(TAG, "Bluetooth adapter is either off or turning off");
                    }
                }catch(Exception e){
                    Log.e(TAG, "Exception in while loop: " + e.getMessage(), e);
                }finally{
                    try { Thread.sleep(5000); } catch (Exception e) { }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Watchdog stopped");
        BluetoothLEManager.getInstance().stopScanning(); // make sure the scanning is stopped
    }


    public long getLastScanResultTime() {
        return lastScanResultTime;
    }

    public void setLastScanResultTime(long lastScanResultTime) {
        this.lastScanResultTime = lastScanResultTime;
    }


}
