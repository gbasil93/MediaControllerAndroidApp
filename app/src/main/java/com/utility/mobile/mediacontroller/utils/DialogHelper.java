package com.utility.mobile.mediacontroller.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.utility.mobile.mediacontroller.R;


/**
 * Created by jtorres on 6/2/15.
 *
 */
public class DialogHelper {

    private static final String TAG = "DialogHelper";

    public static final int TEXT_AREA_ID = 100;

    private static DialogHelper instance;

    private Dialog currentDialog;
    private EditText editText;
    private Toast currentToast;

    private DialogHelper(){

    }

    public synchronized static DialogHelper getInstance(){
        if(instance == null){
            instance = new DialogHelper();
        }

        return instance;
    }

    public DialogHelper(Dialog currentDialog) {
        this.currentDialog = currentDialog;
    }


    public void showDialog(final Activity activity, final String message, final String dismissButtonText){
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        };
        showDialog(activity, message, dismissButtonText, clickListener);
    }

    public void showDialog(final Activity activity, final String message, final String dismissButtonText, DialogInterface.OnClickListener clickListener){
        showDialog(activity, message, dismissButtonText, clickListener, null, null);
    }

    /**
     * Creates a non-cancelable dialog
     * @param activity
     * @param message
     * @param positiveButtonLabel
     * @param positiveListener
     * @param negativeButtonLabel
     * @param negativeListener
     */
    public void showDialog(final Activity activity,
                           final String message,
                           final String positiveButtonLabel,
                           final DialogInterface.OnClickListener positiveListener,
                           final String negativeButtonLabel,
                           final DialogInterface.OnClickListener negativeListener){
        showDialog(activity, message, positiveButtonLabel, positiveListener, negativeButtonLabel, negativeListener, false);
    }

    /**
     * Creates dialog with specified cancaleable
     * @param activity
     * @param message
     * @param positiveButtonLabel
     * @param positiveListener
     * @param negativeButtonLabel
     * @param negativeListener
     * @param cancelable
     */
    public void showDialog(final Activity activity,
                           final String message,
                           final String positiveButtonLabel,
                           final DialogInterface.OnClickListener positiveListener,
                           final String negativeButtonLabel,
                           final DialogInterface.OnClickListener negativeListener,
                           final boolean cancelable){
        showOnUIThreadDialog(activity, message, positiveButtonLabel, positiveListener, negativeButtonLabel, negativeListener, cancelable, false);
    }

    /**
     * Creates dialog with text area that is not cancelable
     * @param activity
     * @param message
     * @param positiveButtonLabel
     * @param positiveListener
     * @param negativeButtonLabel
     * @param negativeListener
     */
    public void showTextAreaDialog(final Activity activity,
                                   final String message,
                                   final String positiveButtonLabel,
                                   final DialogInterface.OnClickListener positiveListener,
                                   final String negativeButtonLabel,
                                   final DialogInterface.OnClickListener negativeListener){
        showTextAreaDialog(activity, message, positiveButtonLabel, positiveListener, negativeButtonLabel, negativeListener, false);
    }


    public void showCustomLayoutDialog(final Activity activity,
                                       final LinearLayout layout) {
        displayCustomLayoutDialog(activity, layout, null,null, null, null, null, true);
    }

    public void showCustomLayoutDialog(final Activity activity,
                                         final LinearLayout layout,
                                         final String message,
                                         final String positiveButtonLabel,
                                         final DialogInterface.OnClickListener positiveListener,
                                         final String negativeButtonLabel,
                                         final DialogInterface.OnClickListener negativeListener,
                                         final Boolean cancelable) {
        displayCustomLayoutDialog(activity, layout, message, positiveButtonLabel, positiveListener, negativeButtonLabel, negativeListener, cancelable);
    }

    /**
     * Creates dialog with text area with specified cancelable
     * @param activity
     * @param message
     * @param positiveButtonLabel
     * @param positiveListener
     * @param negativeButtonLabel
     * @param negativeListener
     * @param cancelable
     */
    public void showTextAreaDialog(final Activity activity,
                                   final String message,
                                   final String positiveButtonLabel,
                                   final DialogInterface.OnClickListener positiveListener,
                                   final String negativeButtonLabel,
                                   final DialogInterface.OnClickListener negativeListener,
                                   final boolean cancelable){
        showOnUIThreadDialog(activity, message, positiveButtonLabel, positiveListener, negativeButtonLabel, negativeListener, cancelable, true);
    }

    private void showOnUIThreadDialog(final Activity activity,
                                      final String message,
                                      final String positiveButtonLabel,
                                      final DialogInterface.OnClickListener positiveListener,
                                      final String negativeButtonLabel,
                                      final DialogInterface.OnClickListener negativeListener,
                                      final boolean cancelable,
                                      final boolean showTextArea){
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            try {
                if(showTextArea) {
                    displayTextAreaDialog(activity, message, positiveButtonLabel, positiveListener, negativeButtonLabel, negativeListener, cancelable);
                }else{
                    displayDialog(activity, message, positiveButtonLabel, positiveListener, negativeButtonLabel, negativeListener, cancelable);
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }else{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(showTextArea) {
                            displayTextAreaDialog(activity, message, positiveButtonLabel, positiveListener, negativeButtonLabel, negativeListener, cancelable);
                        }else{
                            displayDialog(activity, message, positiveButtonLabel, positiveListener, negativeButtonLabel, negativeListener, cancelable);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void displayDialog(final Activity activity,
                               final String message,
                               final String positiveButtonLabel,
                               final DialogInterface.OnClickListener positiveListener,
                               final String negativeButtonLabel,
                               final DialogInterface.OnClickListener negativeListener,
                               boolean cancelable) throws Exception {

        if(activity != null && !activity.isDestroyed()){
            AlertDialog.Builder builder  = getAlertDialogBuilder(activity, cancelable);
            builder.setMessage(message);
            showDialog(builder, positiveButtonLabel, positiveListener, negativeButtonLabel, negativeListener);
        } else {
            Log.d(TAG, "Failed to display " + activity);
        }
    }


    private void displayTextAreaDialog(final Activity activity,
                                       final String message,
                                       final String positiveButtonLabel,
                                       final DialogInterface.OnClickListener positiveListener,
                                       final String negativeButtonLabel,
                                       final DialogInterface.OnClickListener negativeListener,
                                       boolean cancelable){
        if(activity != null && !activity.isDestroyed()){
            AlertDialog.Builder builder = getAlertDialogBuilder(activity, cancelable);
            editText = new EditText(activity);
            editText.setHint(message);
            editText.setLines(5);
            editText.setSingleLine(false);
            builder.setView(editText);
            showDialog(builder, positiveButtonLabel, positiveListener, negativeButtonLabel, negativeListener);
        }
    }

    private void displayCustomLayoutDialog(final Activity activity,
                                                   final LinearLayout layout,
                                                   final String message,
                                                   final String positiveButtonLabel,
                                                   final DialogInterface.OnClickListener positiveListener,
                                                   final String negativeButtonLabel,
                                                   final DialogInterface.OnClickListener negativeListener,
                                                   boolean cancelable) {
        if (activity != null && !activity.isDestroyed()) {
            AlertDialog.Builder builder = getAlertDialogBuilder(activity, cancelable);
            if(message != null) {
                builder.setMessage(message);
            }
            builder.setView(layout);
            showDialog(builder, positiveButtonLabel, positiveListener, negativeButtonLabel, negativeListener);
        }
    }

    public AlertDialog.Builder getAlertDialogBuilder(Activity activity, boolean cancelable){
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(cancelable);

        return builder;
    }

    private void showDialog(final AlertDialog.Builder builder, final String positiveButtonLabel,
                            final DialogInterface.OnClickListener positiveListener,
                            final String negativeButtonLabel,
                            final DialogInterface.OnClickListener negativeListener){
        if (positiveButtonLabel != null && positiveListener != null) {
            builder.setPositiveButton(positiveButtonLabel, positiveListener);
        }

        if (negativeButtonLabel != null && negativeListener != null) {
            builder.setNegativeButton(negativeButtonLabel, negativeListener);
        }

        Log.d(TAG, "Get ready we're about to show you a dialog now here it comes");
        currentDialog = builder.create();
        currentDialog.show();

//        currentDialog.getWindow().getAttributes().width = ViewGroup.LayoutParams.WRAP_CONTENT;
//        currentDialog.getWindow().getAttributes().height = 350;
//        currentDialog.getWindow().setAttributes(currentDialog.getWindow().getAttributes());
    }

    public Dialog getCurrentDialog() {
        Log.d(TAG, "******** returning current dialog");
        return currentDialog;
    }

//    public void setCurrentDialog(Dialog currentDialog) {
//        this.currentDialog = currentDialog;
//    }

    public void showDialog(Dialog dialog){
        if (currentDialog != null){
            currentDialog.dismiss();
        }
        currentDialog = dialog;
        currentDialog.show();
    }

    public EditText getEditText() {
        return editText;
    }

    /**
     *
     */
    public synchronized void dismissCurrentDialog(){
        Log.d(TAG, "Dismissing dialog");
        try {
            if (currentDialog != null) {
                currentDialog.dismiss();
                currentDialog = null;
            }else{
                Log.d(TAG, "Current dialog was null");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Tells it to run a runnable on the ui thread
     * @param runnable
     */
    protected void runOnUiThread(Runnable runnable){
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(runnable);
    }

    public void showNotificationCustomLayoutDialog(final Activity activity,
                                                   final String title,
                                                   final String message,
                                                   final String positiveButtonLabel,
                                                   final View.OnClickListener positiveListener,
                                                   final String negativeButtonLabel,
                                                   final View.OnClickListener negativeListener,
                                                   final boolean cancelable){

        if (activity != null && !activity.isDestroyed()) {
            dismissCurrentDialog();
            // setup the dialog
            currentDialog = new Dialog(activity);
            currentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            currentDialog.setCancelable(cancelable);
            currentDialog.setContentView(R.layout.dialog_notification);


            // set the title
            TextView titleTextView = (TextView)currentDialog.findViewById(R.id.dialog_notification_title);
            titleTextView.setText(title);

            // set the message
            TextView messageTextView = (TextView)currentDialog.findViewById(R.id.dialog_notification_message);
            messageTextView.setText(message);


            // hide the button area if there is no positive button
            if(positiveButtonLabel == null){
                View view = currentDialog.findViewById(R.id.dialog_notification_button_layout);
                view.setVisibility(View.GONE);
            }else{
                // setup the positive button
                Button positiveButton = (Button)currentDialog.findViewById(R.id.dialog_notification_success_button);
                positiveButton.setText(positiveButtonLabel);
                if(positiveListener == null){
                    positiveButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dismissCurrentDialog();
                        }
                    });
                }
                positiveButton.setOnClickListener(positiveListener);


                // setup the negative button
                Button negativeButton = (Button)currentDialog.findViewById(R.id.dialog_notification_cancel_button);
                if(negativeButtonLabel == null){
                    negativeButton.setText("Cancel");
                }else{
                    negativeButton.setText(negativeButtonLabel);
                }

                // default the negative listener if it is null to dismiss the popup
                if(negativeListener == null){
                    negativeButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dismissCurrentDialog();
                        }
                    });
                }else{
                    negativeButton.setOnClickListener(negativeListener);
                }
            }

            showOnUIThread(currentDialog);
        }
    }

    /**
     * Shows a toast and cancels any toasts that are currently displaying (prevents issue of stacking toasts)
     * @param context
     * @param text
     * @param duration
     */
    public synchronized void showToastOnUIThread(final Context context, final String text, final int duration){
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            showToast(context, text, duration);
        }else{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast(context, text, duration);
                }
            });
        }
    }

    private void showToast(Context context, String text, int duration){
        if (currentToast != null){
            currentToast.cancel();
        }
        currentToast = Toast.makeText(context, text, duration);
        currentToast.show();
    }

    /**
     *
     * @param dialog
     */
    private void showOnUIThread(final Dialog dialog){
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            dialog.show();
        }else{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.show();
                }
            });
        }
    }
}
