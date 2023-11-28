package com.utility.mobile.mediacontroller.ui;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


import com.utility.mobile.mediacontroller.R;

import java.util.ArrayList;

public class LeDeviceListAdapter extends BaseAdapter {

    private static final int RSSI_THRESHOLD = -70;
    private LayoutInflater mLayoutInflater = null;
    private ArrayList<DeviceRecord> mLeDevices;

    public LeDeviceListAdapter(Context context) {
        super();
        Context c = context;
        mLayoutInflater =(LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLeDevices = new ArrayList<>();
    }

    public void addDevice(BluetoothDevice device, int rssi, boolean connected) {
        if(rssi > RSSI_THRESHOLD) {
            DeviceRecord deviceRecord = new DeviceRecord(device, rssi, connected);

            int deviceIndex = mLeDevices.indexOf(deviceRecord);
            if (deviceIndex == -1) {
                mLeDevices.add(deviceRecord);
            } else {
                DeviceRecord currentRecord = mLeDevices.get(deviceIndex);
                currentRecord.rssi = rssi;
                currentRecord.connected = connected;
            }
        }
    }

    public BluetoothDevice getDevice(int position) {
        DeviceRecord deviceRecord = mLeDevices.get(position);
        if(deviceRecord == null) {
            return null;
        } else {
            return deviceRecord.device;
        }
    }

    public void clear() {
        mLeDevices.clear();
    }

    @Override
    public int getCount() {
        return mLeDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return mLeDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if(view == null) {
            view = mLayoutInflater.inflate(R.layout.listitem_device, null);
        }
        DeviceRecord deviceRecord = mLeDevices.get(i);

        TextView tvDeviceName = (TextView)view.findViewById(R.id.device_name);
        String deviceName = deviceRecord.name;
        if(deviceName != null && deviceName.length() > 0) {
            tvDeviceName.setText(deviceRecord.device.getName());
        } else {
            tvDeviceName.setText("Unknown Device");
        }

        TextView tvDeviceAddr = (TextView)view.findViewById(R.id.device_address);
        String deviceAddr = deviceRecord.device.getAddress();
        tvDeviceAddr.setText(deviceAddr);


        TextView tvDeviceRssi = (TextView)view.findViewById(R.id.device_rssi);
        if(deviceRecord.rssi != 0) {
            String rssi = deviceRecord.rssi + " dBm";
            tvDeviceRssi.setText(rssi);
        }else{
            tvDeviceRssi.setText(""); // if the rssi is unknown, do not populate it.
        }

        // set the bonded state
        TextView bondedTextView = (TextView) view.findViewById(R.id.listitem_device_bonded);
        if(deviceRecord.connected) {
            bondedTextView.setText("Connected");

        }else{
            bondedTextView.setText("");
        }

        return view;
    }

    private class DeviceRecord {
        BluetoothDevice device;
        String name;
        boolean connected;
        int rssi;

        public DeviceRecord(BluetoothDevice device, int rssi, boolean connected) {
            this.device = device;
            this.rssi = rssi;
            this.connected = connected;
            if(device.getName() != null && device.getName().length() > 0){
                name = device.getName();
            }
        }

        @Override
        public boolean equals(Object object) {
            if(this == object) {
                return true;
            }
            if((object == null) || (getClass() != object.getClass())) {
                return false;
            }

            DeviceRecord objectRecord = (DeviceRecord)object;
            return this.device.equals(objectRecord.device);
        }

        @Override
        public int hashCode() {
            return device.hashCode();
        }
    }
}