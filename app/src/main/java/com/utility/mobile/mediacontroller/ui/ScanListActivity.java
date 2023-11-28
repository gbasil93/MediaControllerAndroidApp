package com.utility.mobile.mediacontroller.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.utility.mobile.bluetoothle.BluetoothLEManager;
import com.utility.mobile.bluetoothle.devices.BluetoothLEDevice;
import com.utility.mobile.mediacontroller.R;
import com.utility.mobile.mediacontroller.utils.BitsAndByteHelper;

import java.lang.reflect.Method;
import java.util.List;

public class ScanListActivity extends AppCompatActivity implements ListView.OnItemClickListener{

    private final static String TAG = ScanListActivity.class.getSimpleName();

    ListView mListView;
    LeDeviceListAdapter mListAdapter;
    private boolean mScanning;
    private BroadcastReceiver broadcastReceiver;

    private Toolbar mToolbar;
    private boolean refreshed = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_list);

        BluetoothLEManager.getInstance().init(this.getApplicationContext());


        mListView = findViewById(R.id.lv_scan_list);
        mListView.setOnItemClickListener(this);

        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                },
                0
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Clearing all bonds");
        BluetoothLEManager.getInstance().disconnectAll();
        clearBonds();

        setupBluetoothReceiver();
        mListAdapter = new LeDeviceListAdapter(this);
        mListView.setAdapter(mListAdapter);
        Log.d(TAG, "Starting scan from onResume()");
        populateListWithConnectedDevices();
        startLeScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
        stopLeScan();
        mListAdapter.clear();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scan_list, menu);
        menu.findItem(R.id.menu_refresh_list).setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.menu_refresh_list) {
            mListAdapter.clear();
            mListAdapter.notifyDataSetChanged();
            if(mScanning){
                stopLeScan();
                BluetoothAdapter.getDefaultAdapter().disable();
                refreshed = true;
            }
//            if(!mScanning) {
//                Log.d(TAG, "Starting scan from refresh");
//                startLeScan();
//            }
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        BluetoothDevice device = mListAdapter.getDevice(position);
        if(device == null) {
            return;
        }

        Log.d(TAG, "Selected " + device.getName() + " with address: " + device.getAddress());
        Intent intent = new Intent(this, MediaControllerActivity.class);
        intent.putExtra("name", device.getName());
        intent.putExtra("address", device.getAddress());
        if(mScanning) {
            stopLeScan();
        }
        startActivity(intent);
    }

//    private void setupToolbar() {
//        mToolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(mToolbar);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
//    }

    private void startLeScan() {
            mScanning = true;
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if(!bluetoothAdapter.isEnabled()){
                bluetoothAdapter.enable();
            }

            BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            scanner.startScan(null, settings, mScanCallback);
    }

    private void stopLeScan(){
        mScanning = false;
        BluetoothLeScanner scanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        if(scanner != null) {
            scanner.stopScan(mScanCallback);
        }
    }

    /**
     * Adds connected devices to the list
     */
    private void populateListWithConnectedDevices(){
        for(BluetoothLEDevice bluetoothLEDevice : BluetoothLEManager.getInstance().getBluetoothDeviceList().values()){
            if(bluetoothLEDevice.isConnected()){
                mListAdapter.addDevice(bluetoothLEDevice.getBluetoothDevice(), 0, true);
            }
        }
    }

    ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            try {
//                Log.d(TAG, "onScanResult: " + result.getDevice().getAddress() + " : " + result.getDevice().getName() + ": " + BitsAndByteHelper.toHexString(result.getScanRecord().getBytes()));
                super.onScanResult(callbackType, result);
                runOnUiThread(() -> {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (isScanResultMediaController(result) && result.isConnectable()) {
                            Log.d(TAG, "[MediaControllerUtility] result: " + result.getDevice().getName() + " is connectable");
                            mListAdapter.addDevice(result.getDevice(), result.getRssi(), false);
                            mListAdapter.notifyDataSetChanged();
                        }
                    } else {
                        if (isScanResultMediaController(result)) {
                            mListAdapter.addDevice(result.getDevice(), result.getRssi(), false);
                            mListAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }catch (Exception e){
                Log.e(TAG, "Exception in onScanResults: " + e.getMessage(), e);
            }
        }


        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            runOnUiThread(() -> {
                for(ScanResult r : results) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if(isScanResultMediaController(r) && r.isConnectable()) {
                            mListAdapter.addDevice(r.getDevice(), r.getRssi(), false);
                        }
                    }else{
                        if(isScanResultMediaController(r)) {
                            mListAdapter.addDevice(r.getDevice(), r.getRssi(), false);
                        }
                    }
                }
                mListAdapter.notifyDataSetChanged();
            });
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            mScanning = false;
        }

        private boolean isScanResultMediaController(ScanResult scanResult){
            if(scanResult != null){
                BluetoothDevice device = scanResult.getDevice();
                if(device != null){
                    String name = device.getName();
                    return name != null && name.startsWith("BodyWorn Remote");
                }
            }
            return false;
        }
    };


    /**
     * Broadcast Receiver for bluetooth scan
     * Filters added here
     */
    private void setupBluetoothReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleBtEvent(context, intent);
            }
        };
        IntentFilter eventFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND); //bt found
        eventFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED); //bt connected
        eventFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED); //state change
        eventFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); //scan complete
        eventFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); //Adapter enabled/disable change
        this.registerReceiver(broadcastReceiver, eventFilter);
    }

    /**
     * From onReceive in our BroadcastReceiver
     * As scan is in progress,devices will be added to lists based on mac addresses
     *
     * @param context - context
     * @param intent  - intent
     */
    private void handleBtEvent(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "action received: " + action);
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (state == BluetoothAdapter.STATE_ON) {
                if(mScanning) {
                    stopLeScan();
                }

                startLeScan();
            }else if(state == BluetoothAdapter.STATE_OFF){
                if(refreshed) {
                    BluetoothAdapter.getDefaultAdapter().enable();
                }
            }
        }
    }

    /**
     * Clear bonds of the current device
     *
     */
    public void clearBonds() {
        Log.d(TAG, "Removing bond information");
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            for(BluetoothDevice bluetoothDevice : bluetoothAdapter.getBondedDevices()){
                if(bluetoothDevice != null) {
                    Method m = bluetoothDevice.getClass().getMethod("removeBond", (Class[]) null);
                    m.invoke(bluetoothDevice, (Object[]) null);
                } else {
                    Log.e(TAG, "bluetoothDevice == null");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing bonds: " + e.getMessage(), e);
        }
    }
}
