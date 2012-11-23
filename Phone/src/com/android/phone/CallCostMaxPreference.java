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
import android.os.SystemProperties;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandException.Error;
import com.android.phone.ChangeIccPinScreen;
import com.android.phone.PhoneApp;
import android.widget.TextView;
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
import android.widget.Toast;
import com.mediatek.xlog.Xlog;

import static com.android.phone.TimeConsumingPreferenceActivity.EXCEPTION_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.RESPONSE_ERROR;

/* Fion add start */
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;
/* Fion add end */

public class CallCostMaxPreference extends Preference {
    private static final String LOG_TAG = "Settings/CallCostMaxPreference";
    private static final boolean DBG = true;// (PhoneApp.DBG_LEVEL >= 2);
    private static final int MESSAGE_GET_MAX_CALL_COST = 100;
    private static final int MESSAGE_SET_MAX_CALL_COST = 101;
    private static final int MAX_COST_NUMBER = 16777215;

    private MyHandler mHandler = new MyHandler();
    Phone phone;
    TimeConsumingPreferenceListener mListener;

/* Fion add start */
    public static final int DEFAULT_SIM = 2; /* 0: SIM1, 1: SIM2 */

    private int mSimId = DEFAULT_SIM;
/* Fion add end */

    public CallCostMaxPreference(Context context) {
        this(context, null);
    }

    public CallCostMaxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        phone = PhoneFactory.getDefaultPhone();
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

    private void showInputDialog(String pinRetryLeft) {
        LayoutInflater inflater = LayoutInflater.from(this.getContext());
        final View textEntryView = inflater.inflate(R.layout.pref_max_call_set,
                null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setView(textEntryView);
        builder.setTitle(R.string.max_call_cost_set);

        TextView pinTextView = (TextView) textEntryView
                .findViewById(R.id.showPinTitle);
        if (pinRetryLeft != null) {
            String pinTitle = pinTextView.getText().toString() + "\n( "
                    + pinRetryLeft + " )";
            pinTextView.setText(pinTitle);
        }

        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        EditText limit_num = (EditText) textEntryView
                                .findViewById(R.id.editCallCostSettingLimitNum);
                        EditText pin2 = (EditText) textEntryView
                                .findViewById(R.id.editCallCostSettingPin);
                        String maxCost = limit_num.getText().toString();
                        String pin = pin2.getText().toString();
                        String errorInfo = "";
                        // check input
                        if (!checkMaxCost(maxCost)) {
                            errorInfo += getPrefContext().getString(
                                    R.string.call_cost_input_max_error_msg);
                            errorInfo += "\n";
                        }
                        if (!checkPin(pin))
                            errorInfo += getPrefContext().getString(
                                    R.string.call_cost_input_pin_error_msg);
                        if (errorInfo.equals("")) {
                            setMaxCallCost(maxCost, pin);
                            if (mListener != null) {
                                mListener.onStarted(CallCostMaxPreference.this,
                                        false);
                            }
                        } else
                            displayMessage(errorInfo);
                    }
                });
        AlertDialog dialog=builder.create();
        Window window = dialog.getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE |
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        dialog.show();
    }

    // reason=true means get the Max call cost when init.
    // reason=false means get the Max call cost after setting the Max call cost
    private void getMaxCallCost(boolean reason) {
        Message msg;
        if (reason == true) {
            msg = mHandler.obtainMessage(MESSAGE_GET_MAX_CALL_COST, 0,
                    MESSAGE_GET_MAX_CALL_COST, null);
        } else {
            msg = mHandler.obtainMessage(MESSAGE_GET_MAX_CALL_COST, 0,
                    MESSAGE_SET_MAX_CALL_COST, null);
        }
/* Fion add start */
        if (CallSettings.isMultipleSim())
        {
            ((GeminiPhone)phone).getAccumulatedCallMeterMaximumGemini(msg, mSimId);
        }
        else
        {
            phone.getAccumulatedCallMeterMaximum(msg);
        }
/* Fion add end */

    }
    
    private void setMaxCallCost(String maxCost, String pin) {
        Message m = mHandler.obtainMessage(MESSAGE_SET_MAX_CALL_COST, 0,
                MESSAGE_SET_MAX_CALL_COST, null);
        String hexStr = Integer.toHexString(Integer.valueOf(maxCost)
                .intValue());
        hexStr = formatHexStr(hexStr);
/* Fion add start */
        if (CallSettings.isMultipleSim())
        {
            ((GeminiPhone)phone).setAccumulatedCallMeterMaximumGemini(hexStr, pin, m, mSimId);
        }
        else
        {
            phone.setAccumulatedCallMeterMaximum(hexStr, pin, m);
        }
/* Fion add end */

        Xlog.d(LOG_TAG,"setMaxCallCost: send msg to system,maxLimit="+hexStr+"  pin="+pin);
    }

    private String formatHexStr(String hexStr) {
        int length = hexStr.length();
        switch (length) {
        case 1:
            hexStr = "00000" + hexStr;
            break;
        case 2:
            hexStr = "0000" + hexStr;
            break;
        case 3:
            hexStr = "000" + hexStr;
            break;
        case 4:
            hexStr = "00" + hexStr;
            break;
        case 5:
            hexStr = "0" + hexStr;
            break;
        }
        for (int i = 0; i < 6; i++) {
            char c = hexStr.charAt(i);
            if (c >= 'a' && c <= 'z')
                hexStr = hexStr.replace(c, (char) (c - 32));
        }
        return hexStr;
    }

/* Fion add start */
    void init(TimeConsumingPreferenceListener listener, boolean skipReading, int simId) {
        mListener = listener;
        mSimId = simId;		
        if (!skipReading) {
            getMaxCallCost(true);
            if (mListener != null) {
                mListener.onStarted(this, true);
            }
        }
    }
/* Fion add end */

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_GET_MAX_CALL_COST:
                handleGetMaxCallCostResponse(msg);
                break;
            case MESSAGE_SET_MAX_CALL_COST:
                handleSetMaxCallCostResponse(msg);
                break;
            }
        }

        private void handleGetMaxCallCostResponse(Message msg) {
        	Xlog.d(LOG_TAG,"handleGetMaxCallCostResponse: request get from system.");	
            if (msg.arg2 == MESSAGE_GET_MAX_CALL_COST) {
                if (mListener != null)
                    mListener.onFinished(CallCostMaxPreference.this, true);
            } else {
                if (mListener != null)
                    mListener.onFinished(CallCostMaxPreference.this, false);
            }
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception != null) {
                if (DBG)
                    Xlog.d(LOG_TAG,
                            "handleGetMaxCallCostResponse: ar.exception="
                                    + ar.exception);
                if (mListener != null)
                    mListener.onError(CallCostMaxPreference.this,
                            EXCEPTION_ERROR);
                if (msg.arg2 == MESSAGE_GET_MAX_CALL_COST) {
                    setSummary(R.string.disable);
                    setEnabled(false);
                }
                if (DBG)
                    Xlog.d(LOG_TAG,
                            "handleGetMaxCallCostResponse: ar.exception="
                                    + ar.exception);
                if (mListener != null)
                    mListener.onError(CallCostMaxPreference.this,
                            EXCEPTION_ERROR);
            } else {
                if (ar.userObj instanceof Throwable) {
                    if (mListener != null)
                        mListener.onError(CallCostMaxPreference.this,
                                RESPONSE_ERROR);
                } else {
                    String maxCallCostNum = (String) ar.result;
                    if (checkHexStr(maxCallCostNum)) {
                        Integer temp = Integer.valueOf(maxCallCostNum, 16);
                        String max = temp.toString();
                        setSummary(max.subSequence(0, max.length()));
                    } else {
                        if (msg.arg2 == MESSAGE_GET_MAX_CALL_COST) {
                            setSummary(R.string.loading_data_error);
                            setEnabled(false);
                        }
                    }
                }
            }
        }

        private void handleSetMaxCallCostResponse(Message msg) {
        	Xlog.d(LOG_TAG,"handleSetMaxCallCostResponse: request get from system.");	
            if (msg.arg2 == MESSAGE_SET_MAX_CALL_COST) {
                if (mListener != null)
                    mListener.onFinished(CallCostMaxPreference.this, false);
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    if (DBG)
                        Xlog.d(LOG_TAG,
                                "handleSetMaxCallCostResponse: ar.exception="
                                        + ar.exception);
                    handleException(ar);
                } else {
                    Xlog
                            .i(LOG_TAG,
                                    "Function:handleGetMaxCallCostResponse : system return  ok ");
                    getMaxCallCost(false);
                }
            }
        }
    }
    
    private void  handleException(AsyncResult ar){
    	Error errorCode=((CommandException)(ar.exception)).getCommandError();
        Xlog.i(LOG_TAG,"= loyee = CallCostMaxPreference errorCode="+errorCode);
        switch(errorCode){
		case PASSWORD_INCORRECT:
			showExceptionTipDialog(getPrefContext().getString(R.string.error_title),getPrefContext().getString(R.string.pin2_invalid));
			break;
		/*
		 * TODO add the other Exceptions handle here
		 */
		default:
			if (mListener != null)
				mListener.onError(CallCostMaxPreference.this,EXCEPTION_ERROR);
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

    private boolean checkPin(String pin) {
        if (pin == null || pin.length() > 8 || pin.length() < 4)
            return false;
        else
            return true;
    }

    private boolean checkMaxCost(String maxCost) {
        if (maxCost == null || maxCost.length() <= 0) {
            return false;
        }
        for (int i = 0; i < maxCost.length(); i++) {
            if (maxCost.charAt(i) < '0' || maxCost.charAt(i) > '9')
                return false;
        }
        Integer integer = Integer.valueOf(maxCost);
        int tempMaxNumber = integer.intValue();
        if (tempMaxNumber < 0 || tempMaxNumber > MAX_COST_NUMBER)
            return false;
        return true;
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

    public final void displayMessage(int strId) {
        Toast.makeText(getPrefContext(), getPrefContext().getString(strId),
                Toast.LENGTH_SHORT).show();
    }

    public final void displayMessage(String str) {
        Toast.makeText(getPrefContext(), str, Toast.LENGTH_SHORT).show();
    }

    private boolean checkHexStr(String str) {
        if (str == null || str.length() <= 0 || str.length() > 6)
            return false;

        for (int i = 0; i < str.length(); i++) {
            if ((str.charAt(i) >= '0' && str.charAt(i) <= '9')
                    || (str.charAt(i) >= 'a' && str.charAt(i) <= 'f')
                    || (str.charAt(i) >= 'A' && str.charAt(i) <= 'F')) {
                continue;
            } else
                return false;
        }
        return true;
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
}
