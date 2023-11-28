package com.utility.mobile.bluetoothle.devices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.utility.mobile.bluetoothle.BluetoothLECommQueue;
import com.utility.mobile.bluetoothle.operations.BluetoothLEConnectOperation;
import com.utility.mobile.bluetoothle.operations.BluetoothLEDisconnectOperation;
import com.utility.mobile.bluetoothle.operations.BluetoothLEDiscoverServicesOperation;
import com.utility.mobile.bluetoothle.operations.BluetoothLEReadOperation;
import com.utility.mobile.bluetoothle.operations.BluetoothLESetNotificationOperation;
import com.utility.mobile.bluetoothle.operations.BluetoothLEWriteOperation;
import com.utility.mobile.bluetoothle.utils.BitsAndByteHelper;
import com.utility.mobile.bluetoothle.utils.SoftwareVersionString;

import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Created by garrettbasil on 1/29/18.
 */

public abstract class BluetoothLEDevice extends BluetoothGattCallback {
    private final static String TAG = "BluetoothLEDevice";

    public static final UUID BATTERY_SERVICE_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    public static final UUID BATTERY_CHARACTERISTIC_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    public static final UUID INFO_SERVICE_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static final UUID SOFTWARE_VERSION_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID HARDWARE_VERSION_UUID = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb");
    public static final UUID MODEL_UUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");
    public static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private Context context;
    private BluetoothDevice bluetoothDevice;
    private BluetoothGatt bluetoothGatt;
    private boolean isConnected;
    private BluetoothLECommQueue bluetoothLECommQueue;

    private String deviceName;
    private String macAddress;
    private long lastConnectAttemptTime;

    protected int batteryLevel;
    protected String softwareVersion;
    protected String hardwareVersion;
    protected String model;
    protected int deviceType;
    protected boolean hasSentFirmwareInfo;
    private long lastTimeDisconnectedMillis;
    private long totalDisconnectSeconds;
    private int disconnectCount;
    private int manualDisconnectCount;
    private boolean servicesDiscovered;
    private long lastDiscoverServicesCallTime;
    private boolean disconnectSent = false;
    private long lastBatteryRequestTime;
    private long lastBatteryResponseTime;

    public BluetoothLEDevice(Context context, BluetoothDevice bluetoothDevice) {
        this.context = context;
        this.bluetoothDevice = bluetoothDevice;
        this.deviceName = bluetoothDevice.getName();
        this.macAddress = bluetoothDevice.getAddress();
        Log.d(TAG,"isConnected is set to false");
        this.isConnected = false;
        this.disconnectCount = 0;
        this.manualDisconnectCount = 0;
        this.bluetoothLECommQueue = BluetoothLECommQueue.getInstance();
        this.totalDisconnectSeconds = 0;
        this.batteryLevel = -1;
        setProperties();
    }

    /**
     * Connects application to GATT server on Bluetooth LE device
     */
    public void connectBluetooth(boolean autoConnect)  throws Exception{
        Log.d(TAG, getDeviceName() + " Sending connect operation");
        BluetoothLEConnectOperation connectOperation = new BluetoothLEConnectOperation(context, this, autoConnect);
        BluetoothLECommQueue.getInstance().postOperationToQueue(connectOperation);
        lastConnectAttemptTime = SystemClock.elapsedRealtime();
    }

    public void cleanupGatt(){
        if(bluetoothGatt != null){
            Log.d(TAG, "Cleaning up previous connection attempt");
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    /**
     * Disconnects application from GATT server on the Bluetooth LE device
     */
    public void disconnectBluetooth() throws Exception{
        isConnected = false;
        Log.d(TAG,"[disconnectBluetooth()] isConnected is set to false");
        manualDisconnectCount++;
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if(bluetoothGatt != null) {
                if(bluetoothAdapter.getState() != BluetoothAdapter.STATE_TURNING_OFF || bluetoothAdapter.getState() != BluetoothAdapter.STATE_OFF) {
                    Log.d(TAG, getDeviceName() + " Disconnecting bluetooth device " + deviceName);
                    BluetoothLEDisconnectOperation disconnectOperation = new BluetoothLEDisconnectOperation(this);
                    BluetoothLECommQueue.getInstance().postOperationToQueue(disconnectOperation);
                } else {
                    Log.d(TAG, getDeviceName() + " Bluetooth adapter either off or turning off");
                }
            } else {
                Log.d(TAG, getDeviceName() + " bluetoothGatt == null");
                throw new Exception("bluetoothGatt is null");
            }
        } catch(Exception e) {
            Log.e(TAG, getDeviceName() + " Error in disconnect process", e);
        }
    }

    /**
     *
     * @param service
     * @param characteristic
     * @param data
     * @throws Exception
     */
    protected void writeBluetooth(UUID service, UUID characteristic, byte[] data) throws Exception{
       writeBluetooth(service, characteristic, data, true);
    }

    /**
     *
     * @param service
     * @param characteristic
     * @param data
     * @throws Exception
     */
    protected void writeBluetoothNoResponse(UUID service, UUID characteristic, byte[] data) throws Exception{
        writeBluetooth(service, characteristic, data, false);
    }

    /**
     *
     * @param service
     * @param characteristic
     * @param data
     * @param ackEnable
     * @throws Exception
     */
    protected void writeBluetooth(UUID service, UUID characteristic, byte[] data, boolean ackEnable) throws Exception{
        if(bluetoothGatt != null) {
            BluetoothGattService bluetoothGattService = bluetoothGatt.getService(service);
            if (bluetoothGattService != null) {
                BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(characteristic);
                if (bluetoothGattCharacteristic != null) {
                    BluetoothLEWriteOperation bluetoothLEWriteOperation = new BluetoothLEWriteOperation(this, bluetoothGattCharacteristic, data, ackEnable);
                    Log.d(TAG, getDeviceName() + " Adding " + bluetoothLEWriteOperation.toString() + " to queue");
                    bluetoothLECommQueue.postOperationToQueue(bluetoothLEWriteOperation);
                }else {
                    throw new Exception ("bluetoothGattCharacteristic is null for service: " + service + " and characteristic: " + characteristic);
                }
            }
        } else {
            Log.e(TAG, "bluetoothGatt == null");
            throw new Exception("bluetoothGatt is null");
        }
    }

    protected void readBluetooth(UUID service, UUID characteristic) throws Exception{
        if(bluetoothGatt != null) {
            BluetoothGattService bluetoothGattService = bluetoothGatt.getService(service);
            if (bluetoothGattService != null) {
                BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(characteristic);
                if (bluetoothGattCharacteristic != null) {
                    BluetoothLEReadOperation bluetoothLEReadOperation = new BluetoothLEReadOperation(this, bluetoothGattCharacteristic);
                    Log.d(TAG, getDeviceName() + " Adding " + bluetoothLEReadOperation.toString() + " to queue");
                    bluetoothLECommQueue.postOperationToQueue(bluetoothLEReadOperation);

                    if(characteristic.equals(BATTERY_CHARACTERISTIC_UUID)){
                        lastBatteryRequestTime = SystemClock.elapsedRealtime();
                    }
                }else {
                    throw new Exception ("bluetoothGattCharacteristic is null for service: " + service + " and characteristic: " + characteristic);
                }
            }
        } else {
            Log.e(TAG, "bluetoothGatt == null");
            throw new Exception("bluetoothGatt is null");
        }
    }

    public boolean refreshDeviceCache(BluetoothGatt gatt) throws Exception{
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            if(localBluetoothGatt != null) {
                Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
                if (localMethod != null) {
                    boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                    return bool;
                }
            }
        } catch (Exception localException) {
            Log.e(TAG, getDeviceName() + " An exception occured while refreshing device", localException);
        }
        return false;
    }

    /**
     * Enables/Disables notification on update of specific GATT characteristic
     * @param service
     * @param characteristic
     * @param enabled
     * @throws Exception
     */
    protected void setCharacteristicNotification(UUID service, UUID characteristic, final boolean enabled)  throws Exception{
        if(bluetoothGatt != null) {
            BluetoothGattService bluetoothGattService = bluetoothGatt.getService(service);
            if (bluetoothGattService != null) {
                BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(characteristic);
                if (bluetoothGattCharacteristic != null) {
                    setCharacteristicNotification(bluetoothGattCharacteristic, enabled);
                }else {
                    throw new Exception ("bluetoothGattCharacteristic is null for service: " + service + " and characteristic: " + characteristic);
                }
            }
        } else {
            Log.e(TAG, "bluetoothGatt == null");
            throw new Exception("bluetoothGatt is null");
        }
    }

    /**
     * Enables/Disables notification on update of specific GATT characteristic
     * @param characteristic BluetoothGattCharacteristic object to change notification enable/disable
     * @param enabled boolean representing if the notification should be enabled or not
     * @return boolean on success of operation
     */
    protected void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, final boolean enabled)  throws Exception{
        if(characteristic == null){
            throw new Exception("Characteristic is null");
        }
        final BluetoothGattDescriptor notificationDescriptor = characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
        if(notificationDescriptor != null) {
            if(enabled) {
                Log.d(TAG, getDeviceName() + " Notification descriptor enabled");
                notificationDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else {
                Log.d(TAG, getDeviceName() + " Notification descriptor disabled");
                notificationDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            }
        } else {
            Log.d(TAG, getDeviceName() + " Notification descriptor is null");
            throw new Exception("Notification descriptor is null for characteristic: " + characteristic.getUuid() + " and service: " + characteristic.getService().getUuid());
        }
        BluetoothLESetNotificationOperation bluetoothLESetNotificationOperation = new BluetoothLESetNotificationOperation(this, characteristic, notificationDescriptor);
        Log.d(TAG, getDeviceName() + " Adding " + bluetoothLESetNotificationOperation.toString() + " to queue");
        bluetoothLECommQueue.postOperationToQueue(bluetoothLESetNotificationOperation);
    }

    public boolean hasOtaFile()  throws Exception{
        if(findOtaFile() != null) {
            return true;
        }
        return false;
    }

    public String getFirmwareUpdateVersion() throws Exception{
        File otaFile = findOtaFile();
        if(otaFile != null) {
            return SoftwareVersionString.fromFile(otaFile.getName()).toString();
        }
        return "";
    }

    public boolean isFirmwareUpdateAvailable() throws Exception{
        try {
            if(softwareVersion != null) {
                Log.d(TAG, "Getting SoftwareVersionString for softwareVersion " + softwareVersion);
                SoftwareVersionString currentVersion = SoftwareVersionString.fromString(softwareVersion);
                Log.d(TAG, "SoftwareVersionString = " + currentVersion.toString());
                File otaFile = findOtaFile();

                if (otaFile != null && currentVersion != null && !currentVersion.toString().equals("0.0.0")) {
                    Log.d(TAG, "otaFile = " + otaFile.getName());
                    SoftwareVersionString updateVersion = SoftwareVersionString.fromFile(otaFile.getName());
                    if (updateVersion != null) {
                        int result = currentVersion.compareTo(updateVersion);
                        return result < 0;
                    }
                }
            }else{
                Log.d(TAG, "software version string not available yet for device: " + getDeviceName());
            }
        }catch (Exception e){
            Log.e(TAG, getDeviceName() + " Failed to parse firmware version for device: " + getDeviceName(), e);
        }
        return false;
    }

    public JSONObject getDeviceInfo() throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", model);
        jsonObject.put("softwareVersion", softwareVersion);
        jsonObject.put("hardwareVersion", hardwareVersion);
        jsonObject.put("battery", batteryLevel);
        jsonObject.put("uniqueId", macAddress);
//        jsonObject.put("connectedTs", lastConnectTs);
        return jsonObject;
    }

    public JSONObject getDeviceInfoNoException(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("model", model);
            jsonObject.put("softwareVersion", softwareVersion);
            jsonObject.put("hardwareVersion", hardwareVersion);
            jsonObject.put("battery", batteryLevel);
            jsonObject.put("uniqueId", macAddress);
//            jsonObject.put("connectedTs", lastConnectTs);
        }catch(Exception e){
            Log.e(TAG, "Failed to create device info", e);
        }
        return jsonObject;
    }

    public void connectGatt(boolean autoConnect) throws Exception{
        bluetoothGatt = bluetoothDevice.connectGatt(context, autoConnect, this);
        Log.d(TAG, "Refreshing cache");
        refreshDeviceCache(bluetoothGatt);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        try {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, getDeviceName() + " onConnectionStateChange: " + newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                if(bluetoothGatt == null){
                    bluetoothGatt = gatt; // set the GATT if it is not set
                }
                if (!isConnected) {
                    int count = 0;
                    while (bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                        try{Thread.sleep(1000);}catch (Exception e){} // give time for the bond to form
                        if(count++ > 5){
                            throw new Exception("Failed to bond in: " + count + " seconds");
                        }
                    }

                    if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                        try{Thread.sleep(1000);}catch (Exception e){} // add a delay before discovering services
                    }

                    Log.d(TAG, "[onConnectionStateChange()] isConnected set to true");
                    isConnected = true;
                    servicesDiscovered = false;
                    disconnectSent = false;
                    handleConnect();
                    Log.d(TAG, bluetoothDevice.getName() + " is connected");
                    lastDiscoverServicesCallTime = SystemClock.elapsedRealtime();
                    BluetoothLEDiscoverServicesOperation bluetoothLEDiscoverServicesOperation = new BluetoothLEDiscoverServicesOperation(this);
                    BluetoothLECommQueue.getInstance().postOperationToQueue(bluetoothLEDiscoverServicesOperation);
//                Broadcaster.broadcastMessageAll(BodyWornApplication.getApplication(), NetworkEvent.BROADCAST_MESSAGE_SOURCE_TRIGGER, Constants.MESSAGE_REMOTE_CONNECTED, deviceInfo);
                    totalDisconnectSeconds += (SystemClock.elapsedRealtime() - lastTimeDisconnectedMillis) / 1000;
                    Log.d(TAG, getDeviceName() + " totalDisconnectSeconds: " + totalDisconnectSeconds);
                    bluetoothLECommQueue.setExecuting(false);
                    bluetoothLECommQueue.executeCurrentOperation();
                } else {
                    Log.d(TAG, getDeviceName() + " was already connected!");
                }
            } else {
                Log.d(TAG, getDeviceName() + " is disconnected");
//            Broadcaster.broadcastMessageAll(BodyWornApplication.getApplication(), NetworkEvent.BROADCAST_MESSAGE_SOURCE_TRIGGER, Constants.MESSAGE_REMOTE_DISCONNECTED, deviceInfo);
                // Done with this gatt object, close the port
                if(isConnected || !disconnectSent) {
                    BluetoothLEDisconnectOperation bluetoothLEDisconnectOperation = new BluetoothLEDisconnectOperation(this);
                    BluetoothLECommQueue.getInstance().postOperationToQueue(bluetoothLEDisconnectOperation);
                    Log.d(TAG, getDeviceName() + " Incrementing disconnect count");
                    disconnectCount++;
                    lastTimeDisconnectedMillis = SystemClock.elapsedRealtime();
                    Log.d(TAG, "[onConnectionStateChange()]isConnected is set to false");
                    isConnected = false;
                    servicesDiscovered = false;
                    disconnectSent = true;
                }
                handleDisconnect();
            }
        }catch (Exception e){
            Log.e(TAG, "Exception in onConnectionStateChanged: " + e.getMessage(), e);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        Log.d(TAG, getDeviceName() + " onServicesDiscovered: " + status);
        servicesDiscovered = true;
        if(status == BluetoothGatt.GATT_SUCCESS) {
            for(BluetoothGattService service : gatt.getServices()){
                Log.d(TAG, getDeviceName() + " Service = " + service.getUuid().toString());
            }
            configure();
            readDeviceInfo();
            bluetoothLECommQueue.setExecuting(false);
            bluetoothLECommQueue.executeCurrentOperation();
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        if(status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, getDeviceName() + " Successful read");
            if (characteristic.getUuid().equals(BATTERY_CHARACTERISTIC_UUID)) {
                lastBatteryResponseTime = SystemClock.elapsedRealtime();
            }
            handleReadCharacteristic(characteristic);
            Log.d(TAG, getDeviceName() + " Posting request from Read callback");
            bluetoothLECommQueue.setExecuting(false);
            bluetoothLECommQueue.executeCurrentOperation();
        } else {
            Log.d(TAG, getDeviceName() + " Unsuccessful read - status = " + status);
            bluetoothLECommQueue.setExecuting(false);
            bluetoothLECommQueue.executeCurrentOperation();
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        if(status == BluetoothGatt.GATT_SUCCESS) {
            byte[] valueData = characteristic.getValue();
            if(valueData != null) {
                try {
                    Log.d(TAG, getDeviceName() + " Successful write to " + characteristic.getUuid().toString() + " of value " + BitsAndByteHelper.toHexString(valueData));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            handleWriteCharacteristic(characteristic);
            Log.d(TAG, getDeviceName() + " Posting request from write callback");
            bluetoothLECommQueue.setExecuting(false);
            bluetoothLECommQueue.executeCurrentOperation();
        } else {
            Log.d(TAG, getDeviceName() + " Unsuccessful write - status = " + status);
            bluetoothLECommQueue.setExecuting(false);
            bluetoothLECommQueue.executeCurrentOperation();
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        Log.d(TAG, getDeviceName() + " Characteristic changed: " + characteristic.getUuid() + " for device of type " + deviceType);
        handleChangedCharacteristic(characteristic);
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        Log.d(TAG, "onDescriptorWrite: " + descriptor.getUuid().toString() + " for characteristic " + descriptor.getCharacteristic().getUuid());
        handleDescriptorWrite(descriptor, descriptor.getCharacteristic());
        bluetoothLECommQueue.setExecuting(false);
        bluetoothLECommQueue.executeCurrentOperation();
    }


    @Override
    public String toString() {
        return getDeviceName() + " (" + getMacAddress() + ")";
    }


    // Abstract methods
    public abstract boolean isOtaRunning();
    public abstract File findOtaFile();
    public abstract int getOtaFileSize();
    public abstract int getCurrentOffset();

    protected abstract void handleConnect();
    protected abstract void handleDisconnect();
    protected abstract void handleChangedCharacteristic(BluetoothGattCharacteristic characteristic);
    protected abstract void handleReadCharacteristic(BluetoothGattCharacteristic characteristic);
    protected abstract void handleWriteCharacteristic(BluetoothGattCharacteristic characteristic);
    protected abstract void setProperties();
    protected abstract void configure();
    protected abstract void readDeviceInfo();
    protected abstract void broadcastBatteryLevel();
    protected abstract void broadcastFirmwareInfo();
    protected abstract void handleDescriptorWrite(BluetoothGattDescriptor descriptor, BluetoothGattCharacteristic characteristic);

    // Getters
    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }
    public boolean isServicesDiscovered() {
        return servicesDiscovered;
    }
    public boolean isConnected() {
        return isConnected;
    }
    public int getBatteryLevel() { return batteryLevel; }
    public String getSoftwareVersion() { return softwareVersion; }
    public String getHardwareVersion() { return hardwareVersion; }
    public String getModel() { return model; }
    public int getDeviceType() { return deviceType; }
    public BluetoothGatt getBluetoothGatt() { return bluetoothGatt; }
    public String getDeviceName() { return deviceName; }
    public String getMacAddress() { return macAddress; }
    public BluetoothLECommQueue getBluetoothLECommQueue() { return bluetoothLECommQueue; }
    public int getDisconnectCount() { return disconnectCount; }
    public int getManualDisconnectCount() { return manualDisconnectCount; }
    public long getTotalDisconnectSeconds() { return totalDisconnectSeconds; }

    public abstract boolean shouldAndroidAutoConnect();
    public abstract boolean shouldBLELibAutoConnect();


    public long getLastDiscoverServicesCallTime() {
        return lastDiscoverServicesCallTime;
    }


    public long getLastBatteryRequestTime() {
        return lastBatteryRequestTime;
    }

    public void setLastBatteryRequestTime(long lastBatteryRequestTime) {
        this.lastBatteryRequestTime = lastBatteryRequestTime;
    }

    public long getLastBatteryResponseTime() {
        return lastBatteryResponseTime;
    }
}
