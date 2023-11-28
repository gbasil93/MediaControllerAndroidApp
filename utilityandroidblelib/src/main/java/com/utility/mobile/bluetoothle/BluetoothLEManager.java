package com.utility.mobile.bluetoothle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.util.Log;

import com.utility.mobile.bluetoothle.devices.BluetoothLEDevice;
import com.utility.mobile.bluetoothle.event.BluetoothLEEventDispatcher;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by garrettbasil on 1/29/18.
 */

public class BluetoothLEManager extends ScanCallback {

    private final static String TAG = "BluetoothLEManager";
    public final static int DEVICE_TYPE_FULL_MAC_ADDRESS = 1;
    public final static int DEVICE_TYPE_FULL_NAME = 2;
    public final static int DEVICE_TYPE_PARTIAL_MAC_ADDRESS = 3;
    public final static int DEVICE_TYPE_PARTIAL_NAME = 4;

    public static final int MANUFACTURER_SPECIFIC_DATA = 0xFFFF;

    private static BluetoothLEManager bluetoothLEManager;
    private ConcurrentHashMap<String, BluetoothLEDevice> bluetoothDeviceList;
    private ConcurrentHashMap<String, Integer> scanFilterList;
    private ConcurrentHashMap<String, Long> bluetoothDeviceConnectionTimes;

    private boolean passiveScanEnabled;

    private BluetoothLEWatchdog watchdogThread;

    private long connectionTimeoutMillis;
    private BroadcastReceiver broadcastReceiver;

    private BluetoothLeScanner bluetoothLeScanner;
    private ScanSettings scanSettings;
    private long scanningStartTime;
    private boolean isScanning;

    private BluetoothLEManager() {
        this.bluetoothDeviceList = new ConcurrentHashMap<>();
        this.bluetoothDeviceConnectionTimes = new ConcurrentHashMap<>();
        this.passiveScanEnabled = false;
        this.connectionTimeoutMillis = 5000;
        this.bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        this.scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        this.isScanning = false;
    }

    public static BluetoothLEManager getInstance() {
        if (bluetoothLEManager == null) {
            bluetoothLEManager = new BluetoothLEManager();
        }
        return bluetoothLEManager;
    }

    public void init(Context context) {
        if (watchdogThread == null || !watchdogThread.isAlive()) {
            this.watchdogThread = new BluetoothLEWatchdog(bluetoothDeviceList);
            watchdogThread.start();
        }

        registerReceiver(context);
    }

    private void registerReceiver(Context context) {
        if (broadcastReceiver == null) {
            broadcastReceiver = new BluetoothLEManagerBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
            context.registerReceiver(broadcastReceiver, intentFilter);
        }
    }

    public void deinit(Context context) {
        watchdogThread.stopThread();
        watchdogThread.interrupt();
        unregisterReceiver(context);
        disconnectAll();
    }

    public void addDevice(BluetoothLEDevice device){
        bluetoothDeviceList.put(device.getMacAddress(), device);
    }

    private void unregisterReceiver(Context context) {
        context.unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
    }

    /**
     * @return number of devices successfully disconnected
     */
    public int disconnectAll() {
        int disconnectedDevices = 0;
        for (BluetoothLEDevice device : bluetoothDeviceList.values()) {
            try {
                disconnectDevice(device);
                ++disconnectedDevices;
            } catch (Exception e) {
                Log.e(TAG, "Exception disconnecting device " + device.getDeviceName() + " " + device.getMacAddress() + ": " + e.getMessage(), e);
            }
        }

        return disconnectedDevices;
    }

    /**
     * @param address
     * @throws Exception
     */
    public void disconnectDevice(String address) throws Exception {
        Log.d(TAG, "Unregistering address: " + address);
        final BluetoothLEDevice device = bluetoothDeviceList.get(address);
        if (device != null && device.isConnected()) {
            device.disconnectBluetooth();
        }
    }

    /**
     * @param device
     * @throws Exception
     */
    public synchronized void disconnectDevice(BluetoothLEDevice device) throws Exception {
        Log.d(TAG, "Disconnecting device " + device.getDeviceName());
        bluetoothDeviceList.remove(device.getMacAddress());
        device.disconnectBluetooth();
        device.cleanupGatt();
    }

    public synchronized void connectDevice(BluetoothLEDevice device) throws Exception {
        connectDevice(device, device.shouldAndroidAutoConnect());
    }

    public synchronized void connectDevice(BluetoothLEDevice device, boolean autoConnect) throws Exception {
        Log.d(TAG, "ConnectDevice called on device: " + device.getMacAddress() + ":" + device.getDeviceName());
        String macAddress = device.getMacAddress();

        if (!device.isConnected()) {
            long lastConnectionTime = 0;
            if (bluetoothDeviceConnectionTimes.containsKey(macAddress)) {
                lastConnectionTime = bluetoothDeviceConnectionTimes.get(macAddress);
            }

            if ((SystemClock.elapsedRealtime() - lastConnectionTime) > connectionTimeoutMillis) {
                bluetoothDeviceConnectionTimes.put(macAddress, SystemClock.elapsedRealtime());
                // make sure the previous connection attempt is cleaned up
                bluetoothDeviceList.put(macAddress, device);
                device.cleanupGatt();
                device.connectBluetooth(autoConnect);
                Log.d(TAG, "Starting connection to device: " + device.getDeviceName());
            } else {
                Log.d(TAG, "Not enough time has passed to retry connection for macAddress: " + macAddress);
            }
        }
    }

    public boolean hasConnectedDevices() {
        for (BluetoothLEDevice device : bluetoothDeviceList.values()) {
            boolean isConnected = device.isConnected();
            Log.d(TAG, "device: " + device.getDeviceName() + " isConnected: " + isConnected);
            if (isConnected) {
                Log.d(TAG, "hasConnectedDevices returning true");
                return true;
            }
        }
        Log.d(TAG, "No devices connected");
        return false;
    }

    public int getLowestBluetoothLEBatteryLevel() {
        int minBatteryLevel = 100;
        try {
            Iterator<String> keys = bluetoothDeviceList.keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                int batteryLevel = getBluetoothLEBatteryLevel(key);
                if (batteryLevel > -1) {
                    minBatteryLevel = Math.min(minBatteryLevel, batteryLevel);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get battery level", e);
        }

        return minBatteryLevel;
    }

    public int getBluetoothLEBatteryLevel(String macAddress) {
        BluetoothLEDevice device = bluetoothDeviceList.get(macAddress);
        if (device != null) {
            return device.getBatteryLevel();
        }
        return 0;
    }

    private void sendFoundDeviceBroadcast(ScanResult result) {
        BluetoothLEEventDispatcher.getInstance().sendOnScanResultEvent(result.getDevice(), result.getScanRecord().getManufacturerSpecificData(0xFFFF));
    }

    public JSONArray getAllDeviceInfo() throws Exception {
        JSONArray deviceInfoArray = new JSONArray();
        for (BluetoothLEDevice device : bluetoothDeviceList.values()) {
            if (device != null) {
                JSONObject deviceInfo = device.getDeviceInfo();
                if (deviceInfo != null && deviceInfo.length() > 0) {
                    deviceInfoArray.put(deviceInfo);
                }
            }
        }
        return deviceInfoArray;
    }

    public void setScanFilterList(ConcurrentHashMap<String, Integer> scanFilterList) {
        this.scanFilterList = scanFilterList;
        watchdogThread.updateScanFilterList(this.scanFilterList);
    }

    public void startLowLatencyScan(boolean preFilterEnabled) {
        try {
            Log.d(TAG, "starting low latency scan preFilterEnabled: " + preFilterEnabled);
            if (isScanning()) {
                if (getScanSettings().getScanMode() != ScanSettings.SCAN_MODE_LOW_LATENCY) {
                    stopScanning();
                } else {
                    Log.d(TAG, "Already in low latency mode");
                    return;
                }
            }

            setScanLatency(ScanSettings.SCAN_MODE_LOW_LATENCY);
            scanForAllDevices();
        } catch (Exception e) {
            Log.e(TAG, "Error starting active scan", e);
        }
    }

    protected void startPassiveScan(ConcurrentHashMap<String, Integer> scanFilterList, boolean preFilterEnabled) {
        try {
            int passiveScanSetting = ScanSettings.SCAN_MODE_BALANCED;
            if(isScanning()) {
                if(getScanSettings().getScanMode() != passiveScanSetting) {
                    stopScanning();
                } else {
                    Log.d(TAG, "Already in correct scanning mode for passive scan");
                    return;
                }
            }
            Log.d(TAG, "Starting passive scan preFilterEnabled: " + preFilterEnabled);
            setScanLatency(passiveScanSetting);
//            bluetoothLEScannerManager.scanForAllDevices(this);
            if(preFilterEnabled) {
                scanForSpecificDevices(scanFilterList);
            } else {
                scanForAllDevices();
            }
//            bluetoothLEScannerManager.startScanning(bluetoothDeviceList.keySet(), this);
        } catch (Exception e) {
            Log.e(TAG, "Error starting passive scan", e);
        }
    }


    public List<ScanFilter> updateScanList(Collection<String> bluetoothLEDeviceAddresses) {
        LinkedList<ScanFilter> scanFilterList = new LinkedList<>();
        for (String deviceAddress : bluetoothLEDeviceAddresses) {
            scanFilterList.add(new ScanFilter.Builder().setDeviceAddress(deviceAddress).build());
        }
        return scanFilterList;
    }

    protected void scanForSpecificDevices(ConcurrentHashMap<String, Integer> filterList) {
        List<ScanFilter> scanFilters = new LinkedList<>();
        for (String key : filterList.keySet()) {
            int type = filterList.get(key);
            if (type == BluetoothLEManager.DEVICE_TYPE_FULL_MAC_ADDRESS) {
                scanFilters.add(new ScanFilter.Builder().setDeviceAddress(key).build());
            } else if (type == BluetoothLEManager.DEVICE_TYPE_FULL_NAME) {
                scanFilters.add(new ScanFilter.Builder().setDeviceName(key).build());
            } else {
                Log.e(TAG, "Error: Partial item in list");
                return;
            }
        }

        Log.d(TAG, "scanFilters: " + scanFilters);
        bluetoothLeScanner.startScan(scanFilters, scanSettings, this);
        scanningStartTime = SystemClock.elapsedRealtime();
        isScanning = true;
    }

    protected void scanForAllDevices() {
        List<ScanFilter> emptyFilterList = new LinkedList<>();
        bluetoothLeScanner.startScan(emptyFilterList, scanSettings, this);
        scanningStartTime = SystemClock.elapsedRealtime();
        isScanning = true;
    }

    protected void startScanning(Collection<String> bondedDeviceAddresses, ScanCallback scanCallback) throws Exception {
        if (bondedDeviceAddresses != null && !bondedDeviceAddresses.isEmpty()) {
            Log.d(TAG, "Starting Bluetooth LE scan for addresses: " + bondedDeviceAddresses);
            List<ScanFilter> scanFilterList = updateScanList(bondedDeviceAddresses);
            scanFilterList.add(new ScanFilter.Builder().setDeviceName("Smart Trigger").build());
            bluetoothLeScanner.startScan(scanFilterList, scanSettings, scanCallback);
            scanningStartTime = SystemClock.elapsedRealtime();
            isScanning = true;
        } else {
            throw new Exception("Error: bondedDeviceAddresses == null");
        }

    }


    protected void stopScanning() {
        if(isScanning) {
            Log.d(TAG, "Stopping Bluetooth LE scan");
            bluetoothLeScanner.stopScan(this);
            isScanning = false;
        }
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        BluetoothDevice bluetoothDevice = result.getDevice();
        String name = bluetoothDevice.getName();
        String address = bluetoothDevice.getAddress();

        watchdogThread.setLastScanResultTime(SystemClock.elapsedRealtime());
        if (result.getDevice().getName() == null || result.getDevice().getAddress() == null) {
            return;
        }

        Log.d(TAG, "result: " + name + " (" + address + ")");
        if (scanFilterList.size() > 0) {
            Log.d(TAG, "scanFilterList: " + scanFilterList);
            for (String key : scanFilterList.keySet()) {
                if (scanFilterList.get(key) == BluetoothLEManager.DEVICE_TYPE_FULL_MAC_ADDRESS) {
                    if (address.equals(key)) {
                        Log.d(TAG, "Found device by full mac address, sending broadcast");
                        if(bluetoothDeviceList.containsKey(address)){
                            BluetoothLEDevice bluetoothLEDevice = bluetoothDeviceList.get(address);
                            if(bluetoothLEDevice.shouldBLELibAutoConnect()) {
                                try {
                                    connectDevice(bluetoothLEDevice);
                                }catch (Exception e){
                                    Log.e(TAG, "Exception connecting to device: " + name + " (" + address + "): " + e.getMessage(), e);
                                }
                            }
                        }
                        sendFoundDeviceBroadcast(result);
                    }
                } else if (scanFilterList.get(key) == BluetoothLEManager.DEVICE_TYPE_FULL_NAME) {
                    if (name.equals(key)) {
                        Log.d(TAG, "Found device by full name, sending broadcast");
                        sendFoundDeviceBroadcast(result);
                    }
                } else if (scanFilterList.get(key) == BluetoothLEManager.DEVICE_TYPE_PARTIAL_MAC_ADDRESS) {
                    if (address.contains(key)) {
                        Log.d(TAG, "Found device by partial mac address, sending broadcast");
                        sendFoundDeviceBroadcast(result);
                    }
                } else if (scanFilterList.get(key) == BluetoothLEManager.DEVICE_TYPE_PARTIAL_NAME) {
                    if (name.contains(key)) {
                        Log.d(TAG, "Found device by partial name, sending broadcast");
                        sendFoundDeviceBroadcast(result);
                    }
                }
            }
        } else {
            sendFoundDeviceBroadcast(result);
        }
    }

    /**
     * @return
     */
    public long getScanningStartTime() {
        return scanningStartTime;
    }

    /**
     * @return
     */
    public boolean isScanning() {
        return isScanning;
    }

    /**
     * @return
     */
    public ScanSettings getScanSettings() {
        return scanSettings;
    }

    /**
     * @param scanLatency
     */
    public void setScanLatency(int scanLatency) {
        scanSettings = new ScanSettings.Builder().setScanMode(scanLatency).build();
    }

    /**
     * @return
     */
    public ConcurrentHashMap<String, BluetoothLEDevice> getBluetoothDeviceList() {
        return bluetoothDeviceList;
    }

    /**
     *
     * @param key
     * @return
     */
    public BluetoothLEDevice getBluetoothDevice(String key) {
        return bluetoothDeviceList.get(key);
    }

    /**
     *
     * @return
     */
    public boolean isPassiveScanEnabled() {
        return passiveScanEnabled;
    }

    /**
     *
     * @param passiveScanEnabled
     */
    public void setPassiveScanEnabled(boolean passiveScanEnabled) {
        this.passiveScanEnabled = passiveScanEnabled;
    }

    /**
     *
     * @param connectionTimeoutMillis
     */
    public void setConnectionTimeoutMillis(long connectionTimeoutMillis) {
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }


    private class BluetoothLEManagerBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                Log.d(TAG, "onReceive: " + intent.getAction());
                if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                    Log.d(TAG, "Received ACL connect");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String address = device.getAddress();
                    String name = device.getName();
                    Log.d(TAG, "[RECEIVER] Device:" + name + " at:" + address + " connected");
                    BluetoothLEEventDispatcher.getInstance().sendOnBluetoothDeviceConnected(device);
                } else if (intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                    int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String address = device.getAddress();
                    String name = device.getName();
                    Log.d(TAG, "[RECEIVER] Device:" + name + " at:" + address + " state:" + bondState);
                    BluetoothLEEventDispatcher.getInstance().sendOnDeviceBondedStateChange(device, bondState);
                } else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                    Log.d(TAG, "Received ACL Disconnect");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String address = device.getAddress();
                    String name = device.getName();
                    Log.d(TAG, "[RECEIVER] Device:" + name + " at:" + address + " disconnected");
                    disconnectDevice(address);
                    BluetoothLEEventDispatcher.getInstance().sendOnBluetoothDeviceDisconnected(device);
                } else if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
                    Log.d(TAG, "[RECEIVER] Device found");
                } else if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    if (state == BluetoothAdapter.STATE_ON) {
                        Log.d(TAG, "Bluetooth turning on");
                    } else if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                        Log.d(TAG, "Bluetooth turning off");
                        disconnectAll();
                    }
                    BluetoothLEEventDispatcher.getInstance().sendOnBluetoothAdapterStateChanged(state);
                } else if (intent.getAction().equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
                    Log.d(TAG, "Connection state changed");
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception in onReceive: " + e.getMessage(), e);
            }
        }
    }
}
