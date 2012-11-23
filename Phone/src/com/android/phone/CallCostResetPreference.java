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

package com.android.phone;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandException.Error;
import com.android.phone.ChangeIccPinScreen;
import android.os.SystemProperties;
import com.android.phone.PhoneApp;
import android.os.SystemProperties;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Settings;
import static com.android.phone.TimeConsumingPreferenceActivity.EXCEPTION_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.RESPONSE_ERROR;
import com.mediatek.xlog.Xlog;

/* Fion add start */
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;
/* Fion add end */

public class CallCostResetPreference extends Preference {
    private static final String LOG_TAG = "Settings/CallCostResetPreference";
    private static final boolean DBG = true;// (PhoneApp.DBG_LEVEL >= 2);
    private static final int MESSAGE_RESET_TOTAL_CALL_COST = 100;

    private MyHandler mHandler = new MyHandler();
    Phone phone;
    TimeConsumingPreferenceListener mListener;
    CallCostResetTotalInterface mResetTotalInterface = null;

/* Fion add start */
    public static final int DEFAULT_SIM = 2; /* 0: SIM1, 1: SIM2 */

    private int mSimId = DEFAULT_SIM;
/* Fion add end */

    public CallCostResetPreference(Context context) {
        this(context, null);
    }

    public CallCostResetPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        phone = PhoneFactory.getDefaultPhone();
        if(isAirplaneModeOn()) {
            this.setEnabled(false);
        } else {
            this.setEnabled(true);
        }
    }

    public void setAocInterface(CallCostResetTotalInterface in) {
        mResetTotalInterface = in;
    }

    private void showInputDialog(String pinRetryLeft) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        final View textEntryView = inflater.inflate(R.layout.get_pin, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(textEntryView);
        builder.setTitle(getPrefContext().getString(R.string.reset_call_cost));
        TextView content = (TextView) textEntryView.findViewById(R.id.content);

        if (pinRetryLeft != null) {
            String s = getPrefContext().getString(R.string.enter_your_pin)
                    + "\n" + pinRetryLeft;
            content.setText(s);
        } else {
            content.setText(R.string.enter_your_pin);
        }

        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        EditText pin2 = (EditText) textEntryView
                                .findViewById(R.id.pin);
                        String pin = pin2.getText().toString();
                        if (checkPin(pin)) {
                            resetTotalCallCost(pin);
                            if (mListener != null)
                                mListener.onStarted(
                                        CallCostResetPreference.this, false);
                        } else {
                            displayMessage(R.string.call_cost_input_pin_error_msg);
                        }
                    }
                });
        AlertDialog dialog=builder.create();
        Window window = dialog.getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE |
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        dialog.show();
    }

    private Context getPrefContext() {
        return getContext();
    }

    private void checkPinRetryAndShowDialog() {
        int mPinEnterLimit = 0;
        if(CallSettings.isMultipleSim())
        {
        	if (mSimId == Phone.GEMINI_SIM_1)
        	{
        	    mPinEnterLimit = SystemProperties.getInt("gsm.sim.retry.pin2", -1);
        	}
        	else
        	{
        		mPinEnterLimit = SystemProperties.getInt("gsm.sim.retry.pin2.2", -1);
        	}
        }
        else
        {
        	mPinEnterLimit = SystemProperties.getInt("gsm.sim.retry.pin2", -1);
        }
        
        if (mPinEnterLimit >= 1 && mPinEnterLimit <= 3) {
            int resId = 0;
             String temp;
             if (mPinEnterLimit == 1){
                 resId = R.string.one_retry_left;
             	temp=getPrefContext().getString(resId);
             }
             else{
                 resId = R.string.retries_left;
                 temp=getPrefContext().getString(resId,mPinEnterLimit);
             }
            showInputDialog(temp);
        } else if (mPinEnterLimit == 0) {
            String errorTitle = getPrefContext()
                    .getString(R.string.error_title);
            String errorMsg = getPrefContext().getString(
                    R.string.pin_retry_error_info);
            showTipDialog(errorTitle, errorMsg);
        } else {
            showInputDialog(null);
            Xlog.i(LOG_TAG, "can not get the pin2 retries left");
        }
    }

    @Override
    protected void onClick() {
        super.onClick();
        checkPinRetryAndShowDialog();
    }

    private void resetTotalCallCost(String pin) {
        Message msg = mHandler.obtainMessage(MESSAGE_RESET_TOTAL_CALL_COST, 0,
                MESSAGE_RESET_TOTAL_CALL_COST, null);

/* Fion add start */
        if (CallSettings.isMultipleSim())
        {
            ((GeminiPhone)phone).resetAccumulatedCallMeterGemini(pin, msg, mSimId);
        }
        else
        {
            phone.resetAccumulatedCallMeter(pin, msg);
        }
/* Fion add end */

        Xlog.i(LOG_TAG, "resetTotalCallCost: send reset total call  msg, pin2="+pin);
    }

/* Fion add start */
    public void init(TimeConsumingPreferenceListener listener,
            boolean skipReading, int simId) {

        Xlog.d(LOG_TAG,"init, simId = "+simId);
		
        mListener = listener;
        mSimId = simId;
        String simState = SystemProperties.get("gsm.sim.state");
        if (simState != null && simState.equals("ABSENT"))
            setEnabled(false);
    }
/* Fion add end */

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_RESET_TOTAL_CALL_COST:
                handleResetCallCostResponse(msg);
                break;
            }
        }

        private void handleResetCallCostResponse(Message msg) {
            Xlog.i(LOG_TAG, "handleResetCallCostResponse: request get from system.");
            if (mListener != null) {
                mListener.onFinished(CallCostResetPreference.this, false);
            }
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception != null) {
                if (DBG)
                    Xlog.d(LOG_TAG, "handleResetCallCostResponse: ar.exception="+ ar.exception);
                handleException(ar);
            } else {
                if (DBG)
                    Xlog.d(LOG_TAG, "handleResetCallCostResponse: re get");
                if (mResetTotalInterface != null) {
                    mResetTotalInterface.resetTotalCost();
                    displayMessage(R.string.reset_call_cost_success);
                }
            }
        }
    }
    private void  handleException(AsyncResult ar){
    	Error errorCode=((CommandException)(ar.exception)).getCommandError();
        Xlog.i(LOG_TAG,"= loyee = handleResetCallCostResponse errorCode="+errorCode);
        switch(errorCode){
		case PASSWORD_INCORRECT:
			showExceptionTipDialog(getPrefContext().getString(R.string.error_title),getPrefContext().getString(R.string.pin2_invalid));
			break;
		/*
		 * TODO add the other Exceptions handle here
		 */
		default:
			if (mListener != null)
				mListener.onError(CallCostResetPreference.this,EXCEPTION_ERROR);
			break;
        }
    }

    private void showExceptionTipDialog(String title, String msg) {
        new AlertDialog.Builder(getPrefContext())
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
    
    private void showTipDialog(String title, String msg) {
        new AlertDialog.Builder(getPrefContext())
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(
                        R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                gotoUnlockPin2Screen();
                            }
                        }).show();
    }

    private boolean checkPin(String pin) {
        if (pin == null || pin.length() > 8 || pin.length() < 4)
            return false;
        return true;
    }

    private final void displayMessage(int strId) {
        Toast.makeText(CallCostResetPreference.this.getContext(),
                CallCostResetPreference.this.getContext().getString(strId),
                Toast.LENGTH_SHORT).show();
    }

    private final void displayMessage(String str) {
        Toast.makeText(getPrefContext(), str, Toast.LENGTH_SHORT).show();
    }
    
    private  void gotoUnlockPin2Screen(){
        Intent intent = new Intent();
        intent.putExtra("pin2", true);
        intent.setClass(getPrefContext(), ChangeIccPinScreen.class);
        
        if (CallSettings.isMultipleSim())
        {
        	intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
        }
        
        getPrefContext().startActivity(intent);
    }
 
    /* Returns true if airplane mode is currently on */
    private final boolean isAirplaneModeOn() {
        return Settings.System.getInt(getPrefContext().getContentResolver(), Settings.System.AIRPLANE_MODE_ON,
                0) == 1;
    }
}
