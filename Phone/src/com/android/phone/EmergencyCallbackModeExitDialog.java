/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.phone;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;


/* Mtk add start */
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;
/* Mtk add end */

/**
 * Displays dialog that enables users to exit Emergency Callback Mode
 *
 * @see EmergencyCallbackModeService
 */
public class EmergencyCallbackModeExitDialog extends Activity implements OnDismissListener {

    /** Intent to trigger the Emergency Callback Mode exit dialog */
    static final String ACTION_SHOW_ECM_EXIT_DIALOG =
            "com.android.phone.action.ACTION_SHOW_ECM_EXIT_DIALOG";
    /** Used to get the users choice from the return Intent's extra */
    public static final String EXTRA_EXIT_ECM_RESULT = "exit_ecm_result";

    public static final int EXIT_ECM_BLOCK_OTHERS = 1;
    public static final int EXIT_ECM_DIALOG = 2;
    public static final int EXIT_ECM_PROGRESS_DIALOG = 3;
    public static final int EXIT_ECM_IN_EMERGENCY_CALL_DIALOG = 4;

    AlertDialog mAlertDialog = null;
    ProgressDialog mProgressDialog = null;
    CountDownTimer mTimer = null;
    EmergencyCallbackModeService mService = null;
    Handler mHandler = null;
    int mDialogType = 0;
    long mEcmTimeout = 0;
    private boolean mInEmergencyCall = false;
    private static final int ECM_TIMER_RESET = 1;
    private Phone mPhone = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if phone is in Emergency Callback Mode. If not, exit.
        if (!Boolean.parseBoolean(
                    SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {
            finish();
        }

        mHandler = new Handler();

        // Start thread that will wait for the connection completion so that it can get
        // timeout value from the service
        Thread waitForConnectionCompleteThread = new Thread(null, mTask,
                "EcmExitDialogWaitThread");
        waitForConnectionCompleteThread.start();

        // Register ECM timer reset notfication
        mPhone = PhoneFactory.getDefaultPhone();

/* Mtk add start */
        if (FeatureOption.MTK_GEMINI_SUPPORT == true)
        {
            ((GeminiPhone)mPhone).registerForEcmTimerResetGemini(mTimerResetHandler, ECM_TIMER_RESET, null, Phone.GEMINI_SIM_1);
            ((GeminiPhone)mPhone).registerForEcmTimerResetGemini(mTimerResetHandler, ECM_TIMER_RESET, null, Phone.GEMINI_SIM_2);			
        }
        else
        {			
        mPhone.registerForEcmTimerReset(mTimerResetHandler, ECM_TIMER_RESET, null);
        }
/* Mtk add end */

        // Register receiver for intent closing the dialog
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED);
        registerReceiver(mEcmExitReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mEcmExitReceiver);
        // Unregister ECM timer reset notification

/* Mtk add start */
        if (FeatureOption.MTK_GEMINI_SUPPORT == true)
        {
            ((GeminiPhone)mPhone).unregisterForEcmTimerResetGemini(mHandler, Phone.GEMINI_SIM_1);
            ((GeminiPhone)mPhone).unregisterForEcmTimerResetGemini(mHandler, Phone.GEMINI_SIM_2);			
        }
        else
        {			
        mPhone.unregisterForEcmTimerReset(mHandler);
    }
/* Mtk add end */
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mDialogType = savedInstanceState.getInt("DIALOG_TYPE");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("DIALOG_TYPE", mDialogType);
    }

    /**
     * Waits until bind to the service completes
     */
    private Runnable mTask = new Runnable() {
        public void run() {
            Looper.prepare();

            // Bind to the remote service
            bindService(new Intent(EmergencyCallbackModeExitDialog.this,
                    EmergencyCallbackModeService.class), mConnection, Context.BIND_AUTO_CREATE);

            // Wait for bind to finish
            synchronized (EmergencyCallbackModeExitDialog.this) {
                try {
                    if (mService == null) {
                        EmergencyCallbackModeExitDialog.this.wait();
                    }
                } catch (InterruptedException e) {
                    Log.d("ECM", "EmergencyCallbackModeExitDialog InterruptedException: "
                            + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Get timeout value and call state from the service
            if (mService != null) {
                mEcmTimeout = mService.getEmergencyCallbackModeTimeout();
                mInEmergencyCall = mService.getEmergencyCallbackModeCallState();
            }

            // Unbind from remote service
            unbindService(mConnection);

            // Show dialog
            mHandler.post(new Runnable() {
                public void run() {
                    showEmergencyCallbackModeExitDialog();
                }
            });
        }
    };

    /**
     * Shows Emergency Callback Mode dialog and starts countdown timer
     */
    private void showEmergencyCallbackModeExitDialog() {

        if(mInEmergencyCall) {
            mDialogType = EXIT_ECM_IN_EMERGENCY_CALL_DIALOG;
            showDialog(EXIT_ECM_IN_EMERGENCY_CALL_DIALOG);
        } else {
            if (getIntent().getAction().equals(
                    TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS)) {
                mDialogType = EXIT_ECM_BLOCK_OTHERS;
                showDialog(EXIT_ECM_BLOCK_OTHERS);
            } else if (getIntent().getAction().equals(ACTION_SHOW_ECM_EXIT_DIALOG)) {
                mDialogType = EXIT_ECM_DIALOG;
                showDialog(EXIT_ECM_DIALOG);
            }

            mTimer = new CountDownTimer(mEcmTimeout, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    CharSequence text = getDialogText(millisUntilFinished);
                    mAlertDialog.setMessage(text);
                }

                @Override
                public void onFinish() {
                    //Do nothing
                }
            }.start();
        }
    }

    /**
     * Creates dialog that enables users to exit Emergency Callback Mode
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case EXIT_ECM_BLOCK_OTHERS:
        case EXIT_ECM_DIALOG:
            CharSequence text = getDialogText(mEcmTimeout);
            mAlertDialog = new AlertDialog.Builder(EmergencyCallbackModeExitDialog.this)
                    .setIcon(R.drawable.picture_emergency32x32)
                    .setTitle(R.string.phone_in_ecm_notification_title)
                    .setMessage(text)
                    .setPositiveButton(R.string.alert_dialog_yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int whichButton) {
                                    // User clicked Yes. Exit Emergency Callback Mode.

                            /* Mtk add start */
                                    if (FeatureOption.MTK_GEMINI_SUPPORT == true)
                                    {
                                        ((GeminiPhone)mPhone).exitEmergencyCallbackModeGemini(Phone.GEMINI_SIM_1);
                                        ((GeminiPhone)mPhone).exitEmergencyCallbackModeGemini(Phone.GEMINI_SIM_2);										
                                    }
                                    else
                                    {			
                                    mPhone.exitEmergencyCallbackMode();
                                    }
                            /* Mtk add end */


                                    // Show progress dialog
                                    showDialog(EXIT_ECM_PROGRESS_DIALOG);
                                    mTimer.cancel();
                                }
                            })
                    .setNegativeButton(R.string.alert_dialog_no,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // User clicked No
                                    setResult(RESULT_OK, (new Intent()).putExtra(
                                            EXTRA_EXIT_ECM_RESULT, false));
                                    finish();
                                }
                            }).create();
            mAlertDialog.setOnDismissListener(this);
            return mAlertDialog;

        case EXIT_ECM_IN_EMERGENCY_CALL_DIALOG:
            mAlertDialog = new AlertDialog.Builder(EmergencyCallbackModeExitDialog.this)
                    .setIcon(R.drawable.picture_emergency32x32)
                    .setTitle(R.string.phone_in_ecm_notification_title)
                    .setMessage(R.string.alert_dialog_in_ecm_call)
                    .setNeutralButton(R.string.alert_dialog_dismiss,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // User clicked Dismiss
                                    setResult(RESULT_OK, (new Intent()).putExtra(
                                            EXTRA_EXIT_ECM_RESULT, false));
                                    finish();
                                }
                            }).create();
            mAlertDialog.setOnDismissListener(this);
            return mAlertDialog;

        case EXIT_ECM_PROGRESS_DIALOG:
            mProgressDialog = new ProgressDialog(EmergencyCallbackModeExitDialog.this);
            mProgressDialog.setMessage(getText(R.string.progress_dialog_exiting_ecm));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            return mProgressDialog;

        default:
            return null;
        }
    }

    /**
     * Returns dialog box text with updated timeout value
     */
    private CharSequence getDialogText(long millisUntilFinished) {
        // Format time
        int minutes = (int)(millisUntilFinished / 60000);
        String time = String.format("%d:%02d", minutes,
                (millisUntilFinished % 60000) / 1000);

        switch (mDialogType) {
        case EXIT_ECM_BLOCK_OTHERS:
            return String.format(getResources().getQuantityText(
                    R.plurals.alert_dialog_not_avaialble_in_ecm, minutes).toString(), time);
        case EXIT_ECM_DIALOG:
            return String.format(getResources().getQuantityText(R.plurals.alert_dialog_exit_ecm,
                    minutes).toString(), time);
        }
        return null;
    }

    /**
     * Closes activity when dialog is dismissed
     */
    public void onDismiss(DialogInterface dialog) {
        EmergencyCallbackModeExitDialog.this.setResult(RESULT_OK, (new Intent())
                .putExtra(EXTRA_EXIT_ECM_RESULT, false));
        finish();
    }

    /**
     * Listens for Emergency Callback Mode state change intents
     */
    private BroadcastReceiver mEcmExitReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Received exit Emergency Callback Mode notification close all dialogs
            if (intent.getAction().equals(
                    TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED)) {
                if (intent.getBooleanExtra("phoneinECMState", false) == false) {
                    if (mAlertDialog != null)
                        mAlertDialog.dismiss();
                    if (mProgressDialog != null)
                        mProgressDialog.dismiss();
                    EmergencyCallbackModeExitDialog.this.setResult(RESULT_OK, (new Intent())
                            .putExtra(EXTRA_EXIT_ECM_RESULT, true));
                    finish();
                }
            }
        }
    };

    /**
     * Class for interacting with the interface of the service
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((EmergencyCallbackModeService.LocalBinder)service).getService();
            // Notify thread that connection is ready
            synchronized (EmergencyCallbackModeExitDialog.this) {
                EmergencyCallbackModeExitDialog.this.notify();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    /**
     * Class for receiving framework timer reset notifications
     */
    private Handler mTimerResetHandler = new Handler () {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ECM_TIMER_RESET:
                    if(!((Boolean)((AsyncResult) msg.obj).result).booleanValue()) {
                        EmergencyCallbackModeExitDialog.this.setResult(RESULT_OK, (new Intent())
                                .putExtra(EXTRA_EXIT_ECM_RESULT, false));
                        finish();
                    }
                    break;
            }
        }
    };
}
