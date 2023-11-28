//package com.utility.mobile.bluetoothle.scanner;
//
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.le.BluetoothLeScanner;
//import android.bluetooth.le.ScanCallback;
//import android.bluetooth.le.ScanFilter;
//import android.bluetooth.le.ScanSettings;
//import android.os.SystemClock;
//import android.util.Log;
//
//import com.utility.mobile.bluetoothle.BluetoothLEManager;
//
//import java.util.Collection;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * Created by garrettbasil on 4/2/18.
// */
//
//public class BluetoothLEScannerManager {
//
//    private static BluetoothLEScannerManager bluetoothLEScannerManager;
//    private BluetoothLeScanner bluetoothLeScanner;
//    private ScanSettings scanSettings;
//    private long scanningStartTime;
//    private boolean isScanning;
//
//    private final static String TAG = "BluetoothLEScanMgr";
//
//    public BluetoothLEScannerManager() {
//        this.bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
//        this.scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
//        this.isScanning = false;
//    }
//
//    public static BluetoothLEScannerManager getInstance() {
//        if(bluetoothLEScannerManager == null) {
//            bluetoothLEScannerManager = new BluetoothLEScannerManager();
//        }
//        return bluetoothLEScannerManager;
//    }
//
//    public void startLowLatencyScan(ScanCallback scanCallback, boolean preFilterEnabled) {
//        try {
//            Log.d(TAG, "starting low latency scan preFilterEnabled: " + preFilterEnabled);
//            if(isScanning()) {
//                if(getScanSettings().getScanMode() != ScanSettings.SCAN_MODE_LOW_LATENCY) {
//                    stopScanning(scanCallback);
//                } else {
//                    Log.d(TAG, "Already in low latency mode");
//                    return;
//                }
//            }
//            setScanLatency(ScanSettings.SCAN_MODE_LOW_LATENCY);
////            if(preFilterEnabled) {
////                scannerManager.scanForSpecificDevices(scanFilterList, this);
////            } else {
//            scanForAllDevices(scanCallback);
////            }
//        } catch (Exception e) {
//            Log.e(TAG, "Error starting active scan", e);
//        }
//    }
//
//    public void startPassiveScan(ScanCallback scanCallback, ConcurrentHashMap<String, Integer> scanFilterList, boolean preFilterEnabled) {
//        try {
//            int passiveScanSetting = ScanSettings.SCAN_MODE_BALANCED;
//            if(isScanning()) {
//                if(getScanSettings().getScanMode() != passiveScanSetting) {
//                    stopScanning(scanCallback);
//                } else {
//                    Log.d(TAG, "Already in correct scanning mode for passive scan");
//                    return;
//                }
//            }
//            Log.d(TAG, "Starting passive scan preFilterEnabled: " + preFilterEnabled);
//            setScanLatency(passiveScanSetting);
////            bluetoothLEScannerManager.scanForAllDevices(this);
//            if(preFilterEnabled) {
//                scanForSpecificDevices(scanFilterList, scanCallback);
//            } else {
//                scanForAllDevices(scanCallback);
//            }
////            bluetoothLEScannerManager.startScanning(bluetoothDeviceList.keySet(), this);
//        } catch (Exception e) {
//            Log.e(TAG, "Error starting passive scan", e);
//        }
//    }
//
//
//    public void scanForSpecificDevices(ConcurrentHashMap<String, Integer> filterList, ScanCallback scanCallback) {
//        List<ScanFilter> scanFilters = new LinkedList<>();
//        for(String key : filterList.keySet()) {
//            int type = filterList.get(key);
//            if(type == BluetoothLEManager.DEVICE_TYPE_FULL_MAC_ADDRESS) {
//                scanFilters.add(new ScanFilter.Builder().setDeviceAddress(key).build());
//            } else if(type == BluetoothLEManager.DEVICE_TYPE_FULL_NAME) {
//                scanFilters.add(new ScanFilter.Builder().setDeviceName(key).build());
//            } else {
//                Log.e(TAG, "Error: Partial item in list");
//                return;
//            }
//        }
//
//        Log.d(TAG,"scanFilters: " + scanFilters);
//        bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback);
//        scanningStartTime = SystemClock.elapsedRealtime();
//        isScanning = true;
//    }
//
//    public void scanForAllDevices(ScanCallback scanCallback) {
//        List<ScanFilter> emptyFilterList = new LinkedList<>();
//        bluetoothLeScanner.startScan(emptyFilterList, scanSettings, scanCallback);
//        scanningStartTime = SystemClock.elapsedRealtime();
//        isScanning = true;
//    }
//
//    public void startScanning(Collection<String> bondedDeviceAddresses, ScanCallback scanCallback) throws Exception {
//        if(bondedDeviceAddresses != null && !bondedDeviceAddresses.isEmpty()){
//            Log.d(TAG, "Starting Bluetooth LE scan for addresses: " + bondedDeviceAddresses);
//            List<ScanFilter> scanFilterList = updateScanList(bondedDeviceAddresses);
//            scanFilterList.add(new ScanFilter.Builder().setDeviceName("Smart Trigger").build());
//            bluetoothLeScanner.startScan(scanFilterList, scanSettings, scanCallback);
//            scanningStartTime = SystemClock.elapsedRealtime();
//            isScanning = true;
//        } else {
//            throw new Exception("Error: bondedDeviceAddresses == null");
//        }
//
//    }
//
//    public void stopScanning(ScanCallback scanCallback) {
//        Log.d(TAG, "Stopping Bluetooth LE scan");
//        bluetoothLeScanner.stopScan(scanCallback);
//        isScanning = false;
//    }
//
//    public List<ScanFilter> updateScanList(Collection<String> bluetoothLEDeviceAddresses) {
//        LinkedList<ScanFilter> scanFilterList = new LinkedList<>();
//        for(String deviceAddress : bluetoothLEDeviceAddresses) {
//            scanFilterList.add(new ScanFilter.Builder().setDeviceAddress(deviceAddress).build());
//        }
//        return scanFilterList;
//    }
//
//    public long getScanningStartTime() { return scanningStartTime; }
//    public boolean isScanning() { return isScanning; }
//    public ScanSettings getScanSettings() { return scanSettings; }
//    public void setScanLatency(int scanLatency) {
//        scanSettings = new ScanSettings.Builder().setScanMode(scanLatency).build();
//    }
//
//
//
//
//}
