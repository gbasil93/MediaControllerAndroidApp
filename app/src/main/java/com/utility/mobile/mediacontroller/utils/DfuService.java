package com.utility.mobile.mediacontroller.utils;

import android.app.Activity;

import androidx.annotation.Nullable;

import com.utility.mobile.mediacontroller.BuildConfig;
import com.utility.mobile.mediacontroller.ui.NotificationActivity;

import no.nordicsemi.android.dfu.DfuBaseService;

public class DfuService extends DfuBaseService {
    @Nullable
    @Override
    protected Class<? extends Activity> getNotificationTarget() {
        return NotificationActivity.class;
    }

    @Override
    protected boolean isDebug()
    {
        return BuildConfig.DEBUG;
    }

}
