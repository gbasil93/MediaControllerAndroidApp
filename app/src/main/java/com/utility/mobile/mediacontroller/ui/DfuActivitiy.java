package com.utility.mobile.mediacontroller.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.utility.mobile.mediacontroller.R;
import com.utility.mobile.mediacontroller.utils.DfuService;

import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuServiceController;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

public class DfuActivitiy extends AppCompatActivity {

    private final String TAG = DfuActivitiy.class.getSimpleName();

    private ProgressBar mProgressBar;
    private TextView mTvProgress;
    private TextView mTvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dfu);

        Intent intent = getIntent();
        String deviceName = intent.getStringExtra("name");
        String deviceAddr = intent.getStringExtra("addr");

        Log.d(TAG, "deviceName: " + deviceName + " deviceAddr: " + deviceAddr);

        try {
            mProgressBar = (ProgressBar) findViewById(R.id.progress_dfu);
            mTvProgress = (TextView) findViewById(R.id.tv_progress_percent);
            mTvStatus = (TextView) findViewById(R.id.tv_dfu_status);

            Button selectFile = (Button) findViewById(R.id.btn_select_update_file);
            selectFile.setOnClickListener(v -> {
                startDfuService(deviceAddr, deviceName);
                mProgressBar.setVisibility(View.VISIBLE);
                mTvProgress.setVisibility(View.VISIBLE);
                mTvStatus.setVisibility(View.VISIBLE);
            });
        } catch (Exception e) {

        }
    }

    private final DfuProgressListener dfuProgressListener = new DfuProgressListener() {
        @Override
        public void onDeviceConnecting(@NonNull String s) {
            Log.d(TAG, "Connecting to device");
            mTvStatus.setText(R.string.dfu_connecting);
        }

        @Override
        public void onDeviceConnected(@NonNull String s) {
            Log.d(TAG, "Connected to device");
            mTvStatus.setText(R.string.dfu_connected);
        }

        @Override
        public void onDfuProcessStarting(@NonNull String s) {
            Log.d(TAG, "Starting DFU process");
            mTvStatus.setText(R.string.dfu_starting);
        }

        @Override
        public void onDfuProcessStarted(@NonNull String s) {
            Log.d(TAG, "Started DFU process");
            mTvStatus.setText(R.string.dfu_started);
        }

        @Override
        public void onEnablingDfuMode(@NonNull String s) {
            Log.d(TAG, "Enabling DFU on device");
            mTvStatus.setText(R.string.dfu_enabling);
        }

        @Override
        public void onProgressChanged(@NonNull String s, int i, float v, float v1, int i1, int i2) {
            Log.d(TAG, "onProgressChanged");
            mProgressBar.setProgress(i);
            mTvProgress.setText(getString(R.string.dfu_progress, i));
        }

        @Override
        public void onFirmwareValidating(@NonNull String s) {
            Log.d(TAG, "Validating firmware");
            mTvStatus.setText(R.string.dfu_validating);
        }

        @Override
        public void onDeviceDisconnecting(String s) {
            Log.d(TAG, "Disconnecting from device");
            mTvStatus.setText(R.string.dfu_disconnecting);
        }

        @Override
        public void onDeviceDisconnected(@NonNull String s) {
            Log.d(TAG, "Disconnected from device");
            mTvStatus.setText(R.string.dfu_disconnected);
        }

        @Override
        public void onDfuCompleted(@NonNull String s) {
            Log.d(TAG, "DFU complete!");
            mTvStatus.setText(R.string.dfu_complete);
        }

        @Override
        public void onDfuAborted(@NonNull String s) {
            Log.d(TAG, "DFU aborted");
            mTvStatus.setText(R.string.dfu_aborted);
        }

        @Override
        public void onError(@NonNull String s, int i, int i1, String s1) {
            Log.d(TAG, "Error: " + s);
            mTvStatus.setText(getString(R.string.dfu_error, s));
        }
    };


    @Override
    public void onResume() {
        super.onResume();

        DfuServiceListenerHelper.registerProgressListener(this, dfuProgressListener);
    }


    @Override
    public void onPause() {
        super.onPause();

        DfuServiceListenerHelper.registerProgressListener(this, dfuProgressListener);
    }

    private void startDfuService(String deviceAddr, String deviceName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DfuServiceInitiator.createDfuNotificationChannel(this);
        }
        final DfuServiceInitiator starter = new DfuServiceInitiator(deviceAddr)
                .setDeviceName(deviceName)
                .setKeepBond(true);

        starter.setPrepareDataObjectDelay(300L);

        starter.setZip("/sdcard/FirmwareImages/mc_dfu.zip");
        Log.d(TAG, "Starting DFU service controller");
        final DfuServiceController controller = starter.start(this, DfuService.class);
    }
}
