package com.utility.mobile.mediacontroller.bluetooth.devices;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.utility.mobile.bluetoothle.devices.BluetoothLEDevice;
import com.utility.mobile.mediacontroller.utils.BitsAndByteHelper;

import org.json.JSONObject;

import java.io.File;
import java.util.UUID;

public class MediaController extends BluetoothLEDevice {
    private static final String TAG = "MediaController";

    public static final String BROADCAST_MEDIA_CONTROLLER_CONNECTED = "MEDIA_CONTROLLER_CONNECTED";
    public static final String BROADCAST_MEDIA_CONTROLLER_DISCONNECTED = "MEDIA_CONTROLLER_DISCONNECTED";
    public static final String BROADCAST_MEDIA_CONTROLLER_BUTTON = "MEDIA_CONTROLLER_BUTTON";
    public static final String BROADCAST_MEDIA_CONTROLLER_BATTERY = "MEDIA_CONTROLLER_BATTERY";
    public static final String BROADCAST_MEDIA_CONTROLLER_FIRMWARE = "MEDIA_CONTROLLER_FIRMWARE";


    public static final UUID LIGHT_CHARACTERISTIC = UUID.fromString("61ad84b2-9eea-4be4-bc53-c1677c3a54fd");
    public static final UUID BUTTON_SERVICE = UUID.fromString("61ad84b1-9eea-4be4-bc53-c1677c3a54fd");
    public static final UUID BUTTON_CHARACTERISTIC = UUID.fromString("61ad84b1-9eea-4be4-bc53-c1677c3a54fd");
    public static final UUID CONFIG_SERVICE = UUID.fromString("7baf716b-f694-4dd2-b2f7-590df4c2a771");

    public static final int BUTTON_PRESS = 1;
    public static final int BUTTON_RELEASE = 2;

    private Context context;
    private boolean recordingLightOn = false;

    public MediaController(Context context, BluetoothDevice bluetoothDevice) {
        super(context, bluetoothDevice);
        this.context = context;
    }

    @Override
    public boolean isOtaRunning() {
        return false;
    }

    @Override
    public File findOtaFile() {
        return null;
    }

    @Override
    public int getOtaFileSize() {
        return 0;
    }

    @Override
    public int getCurrentOffset() {
        return 0;
    }

    @Override
    protected void handleConnect() {
        try {
            Log.d(TAG, getDeviceName() + " is connected");
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(BROADCAST_MEDIA_CONTROLLER_CONNECTED));
        }catch (Exception e){
            Log.e(TAG, "[MediaControllerUtility] Exception handling connect event: " + e.getMessage(), e);
        }
    }

    @Override
    protected void handleDisconnect() {
        try {
            Log.d(TAG, getDeviceName() + " is disconnected");
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(BROADCAST_MEDIA_CONTROLLER_DISCONNECTED));
        }catch (Exception e){
            Log.e(TAG, "[MediaControllerUtility] Exception handling disconnect event: " + e.getMessage(), e);
        }
    }

    @Override
    protected void handleChangedCharacteristic(BluetoothGattCharacteristic characteristic) {
        if(characteristic.getUuid().equals(BUTTON_CHARACTERISTIC)) {
            Log.d(TAG, "*************************** Characteristic " + characteristic.getUuid() + " It is a button event");
            processEnhancedRemoteData(characteristic.getValue());
        }else{
            Log.d(TAG, "[MediaControllerUtility]*************************** Characteristic " + characteristic.getUuid() + " It is not a button event");
        }
    }


    private void processEnhancedRemoteData(byte[] data){
        Log.d(TAG, "[MediaControllerUtility] Received data from bluetooth remote");
        if (data != null && data.length > 0) {
            int buttonData = data[0];
            String buttonValue = "Unknown";
            int newBatteryLevel = data[1];
            if (newBatteryLevel != this.batteryLevel) {
                this.batteryLevel = newBatteryLevel;
            }
            int buttonAction = data[2];
            Log.d(TAG, "[MediaControllerUtility] buttonData: " + buttonData + " buttonAction: " + buttonAction);

            switch (buttonData) {
                case 0x10:
                    //center physical button
                    buttonValue = "center";
                    break;
                case 0x04:
                    // up arrow Button
                    buttonValue = "up";
                    break;
                case 0x08:
                    // down arrow Button
                    buttonValue = "down";
                    break;
                case 0x02:
                    //Right button
                    buttonValue = "right";
                    break;
                case 0x01:
                    //Left button
                    buttonValue = "left";
                    break;
                case 0x20:
                    //Bottom right button
                    buttonValue = "corner";
                    break;

            }

            onBluetoothKeyEvent(buttonValue, buttonAction);
        }
    }

    protected void onBluetoothKeyEvent(String key, int event) {
        try {
            JSONObject bluetoothDeviceInfo = getDeviceInfo();
            bluetoothDeviceInfo.put("buttonPressed", key);
            bluetoothDeviceInfo.put("buttonEvent", event);
            Log.d(TAG, getDeviceName() + " Broadcasting button press: " + bluetoothDeviceInfo);
            Intent intent = new Intent(BROADCAST_MEDIA_CONTROLLER_BUTTON);
            intent.putExtra("json", bluetoothDeviceInfo.toString());
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        } catch (Exception e) {
            Log.e(TAG, "[MediaControllerUtility] Exception in onBluetoothKeyEvent: " + e.getMessage(), e);
        }
    }

    @Override
    protected void handleReadCharacteristic(BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "Parsing characteristic data " + characteristic.getUuid().toString());
        try {
            if (characteristic.getUuid().equals(BATTERY_CHARACTERISTIC_UUID)) {
                batteryLevel = characteristic.getValue()[0];
                Log.d(TAG, "[MediaControllerUtility] batteryLevel = " + batteryLevel);
                broadcastBatteryLevel();
            } else if(characteristic.getUuid().equals(SOFTWARE_VERSION_UUID)) {
                String localSoftwareVersion = characteristic.getStringValue(0);
                // check to see if it has changed if it already existed
                if(softwareVersion != null && !softwareVersion.equals(localSoftwareVersion)){
                    // if it has changed then we need to resend the peripheral report
                    hasSentFirmwareInfo = false;
                }
                softwareVersion = localSoftwareVersion;
                Log.d(TAG, "[MediaControllerUtility] softwareVersion = " + softwareVersion);
                broadcastBatteryLevel();
            } else if(characteristic.getUuid().equals(HARDWARE_VERSION_UUID)) {
                hardwareVersion = characteristic.getStringValue(0);
                Log.d(TAG, "[MediaControllerUtility] hardwareVersion = " + hardwareVersion);
                broadcastBatteryLevel();
            } else if(characteristic.getUuid().equals(MODEL_UUID)) {
                model = characteristic.getStringValue(0);
                Log.d(TAG, "[MediaControllerUtility] model = " + model);
                broadcastBatteryLevel();
            }

            if(softwareVersion != null && hardwareVersion != null && model != null){
                broadcastFirmwareInfo();
            }
        } catch (Exception e) {
            Log.d(TAG, "[MediaControllerUtility] Exception parsing characteristic read " + characteristic.getUuid().toString() + ": " + e.getMessage(), e);
        }
    }

    @Override
    protected void handleWriteCharacteristic(BluetoothGattCharacteristic characteristic) {
        try {
            byte[] value = characteristic.getValue();
            StringBuilder stringBuilder = new StringBuilder("[");
            for(Byte b : value){
                stringBuilder.append(b);
                stringBuilder.append(",");
            }
            String rawBytes = stringBuilder.toString();
            rawBytes = rawBytes.substring(0, rawBytes.length() - 1) + "]";
            Log.d(TAG, "[MediaControllerUtility] Received characteristic: " + characteristic.getUuid() + " hexValue: " + BitsAndByteHelper.toHexString(value) + " rawValue: " + rawBytes);
        }catch(Exception e){
            Log.e(TAG, "[MediaControllerUtility] Failed to parse characteristic: " + characteristic.getUuid());
        }
    }

    @Override
    protected void setProperties() {

    }

    @Override
    protected void configure() {

    }

    @Override
    protected void readDeviceInfo() {
        try {
            findButtonCharacteristic();
            readBluetooth(BATTERY_SERVICE_UUID, BATTERY_CHARACTERISTIC_UUID);
            readBluetooth(INFO_SERVICE_UUID, SOFTWARE_VERSION_UUID);
            readBluetooth(INFO_SERVICE_UUID, HARDWARE_VERSION_UUID);
            readBluetooth(INFO_SERVICE_UUID, MODEL_UUID);
        } catch (Exception e) {
            Log.e(TAG, "[MediaControllerUtility] Exception reading deviceinfo: " + e.getMessage(), e);
        }
    }

    private void findButtonCharacteristic() {
        try {
            BluetoothGattService buttonGattService = getBluetoothGatt().getService(BUTTON_SERVICE);
            if (buttonGattService != null) {
                BluetoothGattCharacteristic buttonCharacteristic = buttonGattService.getCharacteristic(BUTTON_CHARACTERISTIC);
                if (buttonCharacteristic != null) {
                    setCharacteristicNotification(buttonCharacteristic, true);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "[MediaControllerUtility] Exception finding button characteristic: "  + e.getMessage(), e);
        }
    }

    @Override
    protected void broadcastBatteryLevel() {
        try {
            Log.d(TAG, "[MediaControllerUtility] Broadcasting device info");
            JSONObject bluetoothDeviceInfo = getDeviceInfo();
            Intent intent = new Intent(BROADCAST_MEDIA_CONTROLLER_BATTERY);
            intent.putExtra("json", bluetoothDeviceInfo.toString());
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "[MediaControllerUtility] Error in broadcasting battery level: " + e.getMessage(), e);
        }
    }

    @Override
    protected void broadcastFirmwareInfo() {
        try {
            Log.d(TAG, "[MediaControllerUtility] Sending peripheral report");
            JSONObject bluetoothDeviceInfo = getDeviceInfoNoException();
            Intent intent = new Intent(BROADCAST_MEDIA_CONTROLLER_FIRMWARE);
            intent.putExtra("json", bluetoothDeviceInfo.toString());
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }catch (Exception e){
            Log.e(TAG, "[MediaControllerUtility] Error in broadcasting firmware: " + e.getMessage(), e);
        }
    }

    @Override
    protected void handleDescriptorWrite(BluetoothGattDescriptor descriptor, BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public boolean shouldAndroidAutoConnect() {
        return false;
    }

    @Override
    public boolean shouldBLELibAutoConnect() {
        return true;
    }

    public void toggleRecordingLight(){
        if(recordingLightOn){
            turnRecordingLightOff();
            recordingLightOn = false;
        }else{
            turnRecordingLightOn();
            recordingLightOn = true;
        }
    }

    public void turnRecordingLightOn(){
        try {
            writeBluetooth(BUTTON_SERVICE, LIGHT_CHARACTERISTIC, new byte[]{1});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void turnRecordingLightOff(){
        try {
            writeBluetooth(BUTTON_SERVICE, LIGHT_CHARACTERISTIC, new byte[]{0});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
