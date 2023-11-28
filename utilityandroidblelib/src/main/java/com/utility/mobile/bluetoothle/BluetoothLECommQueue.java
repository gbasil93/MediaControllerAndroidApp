package com.utility.mobile.bluetoothle;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;

import com.utility.mobile.bluetoothle.operations.BluetoothLEConnectOperation;
import com.utility.mobile.bluetoothle.operations.BluetoothLEOperation;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by garrettbasil on 4/16/18.
 */

public class BluetoothLECommQueue{

    private final static String TAG = "BluetoothLECommQueue";

    private static BluetoothLECommQueue bluetoothLECommQueue;

    private ConcurrentLinkedDeque<BluetoothLEOperation> bluetoothOperationQueue;

    private long lastOperationTime;
    private boolean isExecuting;
    private Handler handler;

    private BluetoothLECommQueue() {
        this.bluetoothOperationQueue = new ConcurrentLinkedDeque<>();
        HandlerThread handlerThread = new HandlerThread("BleThread");
        handlerThread.start();
        this.handler = new Handler(handlerThread.getLooper());
    }

    public static synchronized BluetoothLECommQueue getInstance() {
        if(bluetoothLECommQueue == null) {
            bluetoothLECommQueue = new BluetoothLECommQueue();
        }
        return bluetoothLECommQueue;
    }

    public synchronized void postOperationToQueue(BluetoothLEOperation bluetoothLEOperation) {
        Log.d(TAG, "Posting operation: " + bluetoothLEOperation);
        bluetoothOperationQueue.addLast(bluetoothLEOperation);
        if(!isExecuting) {
            executeCurrentOperation();
        }
    }

    public synchronized void executeCurrentOperation(){
        try {
            isExecuting = true;
            // Posts the next request in the queue to be read or written
            if (bluetoothOperationQueue.isEmpty()) {
                Log.d(TAG, "Nothing to execute");
                isExecuting = false;
                return;
            }

            final BluetoothLEOperation currentOperation = bluetoothOperationQueue.removeFirst();

            if(currentOperation.getOperationName().contains("Connect") || currentOperation.getOperationName().contains("Disconnect")) {
                Iterator iterator = bluetoothOperationQueue.iterator();
                while (iterator.hasNext()) {
                    BluetoothLEOperation operation = (BluetoothLEOperation)iterator.next();
                    if(operation.getDeviceAddress().equals(currentOperation.getDeviceAddress())) {
                        Log.d(TAG, "Got connection event, removing duplicate operation from queue");
                        bluetoothOperationQueue.remove(operation);
                    }
                }
            }

            Log.d(TAG, "Posting current request: " + currentOperation.toString());
            lastOperationTime = SystemClock.elapsedRealtime();

            // make sure all BLE operations are done on the same thread.
            handler.post(new Runnable() {
                @Override
                public void run() {
                    currentOperation.execute();
                    if (!currentOperation.hasAvailableCompletionCallback()) {
                        isExecuting = false;
                        Log.d(TAG, "Posting next operation request because current opp has no callback");
                        executeCurrentOperation();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isOperationQueueEmpty() {
        return bluetoothOperationQueue.isEmpty();
    }

    public long getLastOperationTime() { return lastOperationTime; }
    public void setExecuting(boolean isExecuting) {
        this.isExecuting = isExecuting;
    }

}
