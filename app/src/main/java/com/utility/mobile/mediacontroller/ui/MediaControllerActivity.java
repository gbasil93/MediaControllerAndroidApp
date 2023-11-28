package com.utility.mobile.mediacontroller.ui;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.utility.mobile.bluetoothle.BluetoothLEManager;
import com.utility.mobile.mediacontroller.R;
import com.utility.mobile.mediacontroller.bluetooth.devices.MediaController;
import com.utility.mobile.mediacontroller.utils.DfuService;
import com.utility.mobile.mediacontroller.utils.DialogHelper;

import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuServiceController;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

public class MediaControllerActivity extends AppCompatActivity{

    private final static String TAG = "MediaControllerActivity";

    private int connectionAttempts = -1;
    private String macAddress;
    private boolean deviceReady;
    private MediaController mediaController;
    private ProgressDialog progressDialog;
    private LocalBroadcastReceiver broadcastReceiver;
    private BluetoothLEBroadcastReceiver bluetoothLEBroadcastReceiver;
    private boolean connecting = false;

    private String[] instructions = {"Press Left Button",
            "Release Left Button",
            "Press Center Button",
            "Release Center Button",
            "Press Up Button",
            "Release Up Button",
            "Press Down Button",
            "Release Down Button",
            "Press Corner Button",
            "Release Corner Button",
            "Press Right Button and confirm LED is green",
            "Press Right Button",
            "Release Right Button",
            "Press Right Button and confirm LED is red",
            "Finished"};
    private int currentInstruction = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "[MediaControllerUtility] Registering receivers");
        registerReceivers();
        setContentView(R.layout.activity_media_controller);

        Intent intent = getIntent();
        String deviceName = intent.getStringExtra("name");
        macAddress = intent.getStringExtra("address");
        setDeviceName(deviceName);
        setDeviceAddr(macAddress);

        try {
            if(mediaController == null || !mediaController.isConnected() || !mediaController.getMacAddress().equals(macAddress)) {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
                mediaController = new MediaController(this, device);
                BluetoothLEManager.getInstance().addDevice(mediaController);
                connecting = true;
                pairDevice();
                setScanFilterList();
                showProgressDialog("Pairing to device " + macAddress, true);
                connectionAttempts = 1;
            }

            Button toggleLed = (Button) findViewById(R.id.btn_toggle_led);
            toggleLed.setOnClickListener(v -> {
                if(mediaController != null){
                    mediaController.toggleRecordingLight();
                }
            });

            Button launchDfu = (Button) findViewById(R.id.btn_launch_dfu);
            launchDfu.setOnClickListener(v -> {
                if (mediaController != null && mediaController.isConnected()) {
                    Intent newIntent = new Intent(this, DfuActivitiy.class);
                    newIntent.putExtra("name", deviceName);
                    newIntent.putExtra("addr", macAddress);

                    startActivity(newIntent);
                }
            });
        }catch (Exception e){
            Log.e(TAG, "[MediaControllerUtility] Exception disconnecting from old device: " + e.getMessage(), e);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        unregisterReceiver(bluetoothLEBroadcastReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setTitle(CharSequence title) {
        getSupportActionBar().setTitle(title);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }


    @Override
    public void onBackPressed() {
        try {
            mediaController.disconnectBluetooth();
        }catch (Exception e){
            Log.e(TAG, "Exception disconnecting bluetooth: " + e.getMessage(), e);
        }
        super.onBackPressed();
    }

    private void showProgressDialog(String message, boolean cancelable){

        if(cancelable){
            DialogInterface.OnCancelListener onCancelListener = dialog -> {
                dialog.dismiss();

                View.OnClickListener positiveListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogHelper.getInstance().dismissCurrentDialog();
                        onBackPressed();
                    }
                };

                View.OnClickListener negativeListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogHelper.getInstance().dismissCurrentDialog();
                        if(mediaController.getBatteryLevel() <= 0) {
                            progressDialog.show();
                        }
                    }
                };

                DialogHelper.getInstance().showNotificationCustomLayoutDialog(this, "Are you sure?", "Are you sure you wish to go back to the scan list?",
                        "YES", positiveListener, "NO", negativeListener, false);
            };

            progressDialog = ProgressDialog.show(this, "", message, true, true, onCancelListener);
        }else{
            progressDialog = ProgressDialog.show(this, "", message, true, false);
        }


    }

    /**
     * Set the device name text field at the top of the activity
     * @param deviceName String containing the device name
     */
    private void setDeviceName(String deviceName) {
        TextView tvDeviceName = findViewById(R.id.tv_device_name);
        tvDeviceName.setText("Name: " + deviceName);
    }


    /**
     * Set the device MAC text field at the top of the activity
     * @param deviceAddr String containing the device MAC address
     */
    private void setDeviceAddr(String deviceAddr) {
        TextView tvDeviceAddr = findViewById(R.id.tv_device_addr);
        tvDeviceAddr.setText("MAC: " + deviceAddr);
    }

    /**
     * Sets the software version
     * @param softwareVersion - version string to set
     */
    private void setSoftwareVersion(String softwareVersion) {
        TextView tvDeviceAddr = findViewById(R.id.tv_device_software_version);
        tvDeviceAddr.setText("Software Version: " + softwareVersion);
    }

    /**
     * Set the device Model text field at the top of the activity
     * @param model - String containing the device model
     */
    private void setModel(String model) {
        TextView tvDeviceAddr = findViewById(R.id.tv_device_model);
        tvDeviceAddr.setText("Model: " + model);
    }

    /**
     *
     * @param button
     */
    private void setLastButtonPress(String button){
        if(button != null && button.length() > 0) {
            TextView textView = findViewById(R.id.tv_last_button);
            textView.setText("Last Button Press: " + button);
        }
    }

    /**
     *
     * @param button
     * @param event
     */
    private void updateInstructions(String button, int event){
        Dialog dialog = DialogHelper.getInstance().getCurrentDialog();
        if((progressDialog == null || !progressDialog.isShowing()) && (dialog == null || !dialog.isShowing())){
            String instruction = instructions[currentInstruction].toLowerCase();
            if (button != null && button.length() > 0 && instruction.contains(button)) {
                Log.d(TAG, "button: " + button + " instruction: " + instruction);
                if (instruction.contains("green")) {
                    showInstructionConfirmationDialog("Check LED", "Is LED Green?", true);
                } else if (instruction.contains("red")) {
                    showInstructionConfirmationDialog("Check LED", "Is LED Red?", false);
                } else if (event == MediaController.BUTTON_PRESS) {
                    if (instruction.contains("press")) {
                        showNextInstructions();
                    }
                } else if (event == MediaController.BUTTON_RELEASE) {
                    if (instruction.contains("release")) {
                        showNextInstructions();
                    }
                }
            } else {
                showInstructions();
            }
        }
    }


    /**
     *
     */
    private void showNextInstructions(){
        ++currentInstruction;
        showInstructions();
    }

    private void showInstructions(){
        TextView textView = (TextView) findViewById(R.id.tv_instructions);
        textView.setText(instructions[currentInstruction]);

        if(instructions[currentInstruction].equalsIgnoreCase("finished")){
            Log.d(TAG, "Enabling finished button");
            Button finishedButton = (Button)findViewById(R.id.btn_finished);
            finishedButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
            finishedButton.setEnabled(true);
        }
    }

    /**
     *
     */
    private void showFailedMessage(){
        TextView textView = (TextView) findViewById(R.id.tv_instructions);
        textView.setText("Failed");
    }

    /**
     *
     * @param title
     * @param message
     * @param toggleLED
     */
    private void showInstructionConfirmationDialog(String title, String message, boolean toggleLED) {
        View.OnClickListener positiveListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogHelper.getInstance().dismissCurrentDialog();
                if(toggleLED) {
                    toggleLED();
                }else{
                    showNextInstructions();
                }
            }
        };
        View.OnClickListener negativeListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogHelper.getInstance().dismissCurrentDialog();
                showFailedMessage();
            }
        };

        DialogHelper.getInstance().showNotificationCustomLayoutDialog(this, title, message, "YES", positiveListener, "NO", negativeListener, false);

    }

    private void toggleLED() {
        mediaController.toggleRecordingLight();

        showProgressDialog("Changing LED color", false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {

                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        showNextInstructions();
                    }
                });

            }
        }).start();
    }



    /**
     * Updates the view with the passed in battery level
     * @param batteryLevel int describing the battery percentage of the Holster device
     */
    private void updateBatteryLevelView(int batteryLevel) {
        TextView tvBatteryPercent = findViewById(R.id.tv_battery_level);
        tvBatteryPercent.setText("Battery Level: " + batteryLevel + "%");
    }


    private void disconnectDevice() throws Exception{
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
        MediaController mediaController = new MediaController(this, device);
        mediaController.disconnectBluetooth();
    }

    /**
     * pair the device selected
     */
    private void pairDevice() throws Exception{
        BluetoothDevice bluetoothDevice = mediaController.getBluetoothDevice();
        bluetoothDevice.createBond();
    }

    /**
     * Instantiate HolsterDevice wrapper for BluetoothDevice, and connect to its GATT server
     * @param context Context of the activity calling the method
     * @param deviceAddr MAC of the device to be connected to
     */
    public void connect(Context context, String deviceAddr)  throws Exception{

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddr);
            Log.d(TAG, "[MediaControllerUtility] deviceName: " + device.getName());
            mediaController = new MediaController(this, device);
            BluetoothLEManager.getInstance().connectDevice(mediaController, false);
        }
    }

    private void registerReceivers() {
        broadcastReceiver = new LocalBroadcastReceiver();
        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(MediaController.BROADCAST_MEDIA_CONTROLLER_BATTERY);
        localIntentFilter.addAction(MediaController.BROADCAST_MEDIA_CONTROLLER_CONNECTED);
        localIntentFilter.addAction(MediaController.BROADCAST_MEDIA_CONTROLLER_DISCONNECTED);
        localIntentFilter.addAction(MediaController.BROADCAST_MEDIA_CONTROLLER_FIRMWARE);
        localIntentFilter.addAction(MediaController.BROADCAST_MEDIA_CONTROLLER_BUTTON);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, localIntentFilter);

        bluetoothLEBroadcastReceiver = new BluetoothLEBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(bluetoothLEBroadcastReceiver, intentFilter);
    }

    private void setScanFilterList() throws Exception{
        BluetoothLEManager bluetoothLEManager = BluetoothLEManager.getInstance();
        ConcurrentHashMap<String, Integer> filterList = new ConcurrentHashMap<>();
        filterList.put(macAddress, BluetoothLEManager.DEVICE_TYPE_FULL_MAC_ADDRESS);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.getRemoteDevice(macAddress);
        bluetoothLEManager.setScanFilterList(filterList);
    }


    private class LocalBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                Log.d(TAG, "[MediaControllerUtility] onReceive action: " + action);
                String jsonString = intent.getStringExtra("json");
                if(jsonString != null) {
                    JSONObject jsonObject = new JSONObject(jsonString);
                    Log.d(TAG, "[MediaControllerUtility] jsonObject: " + jsonString);
                    updateBatteryLevelView(jsonObject.getInt("battery"));
                    setSoftwareVersion(jsonObject.optString("softwareVersion", ""));
                    setModel(jsonObject.optString("model", ""));
                    String buttonPressed = jsonObject.optString("buttonPressed", "");
                    setLastButtonPress(buttonPressed);
                    if(connecting) {
                        progressDialog.dismiss();
                        connecting = false;
                    }
                    updateInstructions(buttonPressed, jsonObject.optInt("buttonEvent", -1));
                }
            }catch (Exception e){
                Log.e(TAG, "[MediaControllerUtility] Exception in broadcast receiver: " + e.getMessage(), e);
            }
        }
    }

    private class BluetoothLEBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                Log.d(TAG, "[MediaControllerUtility] onReceive: " + intent.getAction());
                if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                    Log.d(TAG, "[MediaControllerUtility] Received ACL connect");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String address = device.getAddress();
                    String name = device.getName();
                    progressDialog.setMessage("Discovering services for device " + macAddress);
                    Log.d(TAG, "[MediaControllerUtility] [RECEIVER] Device:" + name + " at:" + address + " connected");
                } else if (intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                    int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String address = device.getAddress();
                    String name = device.getName();
                    Log.d(TAG, "[MediaControllerUtility] [RECEIVER] Device:" + name + " at:" + address + " state:" + bondState);
                    if(bondState == BluetoothDevice.BOND_BONDED){
                        if(!mediaController.isConnected()) {
                            progressDialog.setMessage("Connecting to device " + macAddress);
                            connect(context, device.getAddress());
                        }
                    }else if(bondState == BluetoothDevice.BOND_BONDING){
                        progressDialog.setMessage("Pairing to device " + macAddress);
                    }else if(bondState == BluetoothDevice.BOND_NONE){
                        progressDialog.dismiss();

                        View.OnClickListener positiveListener = new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    DialogHelper.getInstance().dismissCurrentDialog();
                                    pairDevice();
                                    progressDialog.show();
                                }catch (Exception e){
                                    Log.e(TAG, "Exception trying to pair: " + e.getMessage(), e);
                                }
                            }
                        };

                        View.OnClickListener negativeListener = new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                DialogHelper.getInstance().dismissCurrentDialog();
                                onBackPressed();
                            }
                        };

                        DialogHelper.getInstance().showNotificationCustomLayoutDialog(MediaControllerActivity.this,
                                "Device Failed to Pair",
                                "Please put device into pairing mode and hit OK",
                                "OK",
                                positiveListener,
                                "Cancel",
                                negativeListener,
                                false);
                    }
                } else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                    Log.d(TAG, "[MediaControllerUtility] Received ACL Disconnect");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String address = device.getAddress();
                    String name = device.getName();
                    progressDialog.setMessage("Connecting to device " + macAddress);
                    Log.d(TAG, "[MediaControllerUtility] [RECEIVER] Device:" + name + " at:" + address + " disconnected");
                } else if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
                    Log.d(TAG, "[MediaControllerUtility] [RECEIVER] Device found");
                } else if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    if (state == BluetoothAdapter.STATE_ON) {
                        Log.d(TAG, "[MediaControllerUtility] Bluetooth turning on");
                        connect(context, macAddress);
                    } else if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                        Log.d(TAG, "[MediaControllerUtility] Bluetooth turning off");
                    }
                } else if (intent.getAction().equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
                    Log.d(TAG, "[MediaControllerUtility] Connection state changed");
                }
            } catch (Exception e) {
                Log.e(TAG, "[MediaControllerUtility] Exception in onReceive: " + e.getMessage(), e);
            }
        }
    }
}
