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

import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.EditPhoneNumberPreference;
import com.android.phone.PhoneApp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.view.WindowManager.BadTokenException;
import com.mediatek.xlog.Xlog;

import static com.android.phone.TimeConsumingPreferenceActivity.EXCEPTION_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.RESPONSE_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.PASSWORD_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.FDN_FAIL;

/* Fion add start */
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;
/* Fion add end */

public class CallBarringBasePreference extends CheckBoxPreference implements
        OnPreferenceClickListener {
    private static final String LOG_TAG = "Settings/CallBarringBasePreference";
    private static final boolean DBG = true; // (PhoneApp.DBG_LEVEL >= 2);

    private MyHandler mHandler = new MyHandler();
    private TimeConsumingPreferenceListener tcpListener;
    private Context mContext = null;
    private Phone phone;
    private int mTitle;
    private String mFacility;
    private boolean mCurrentClickState = false;
    private static final int PASSWORD_LENGTH = 4;
    private CallBarringInterface mCallBarringInterface = null;

    //which is used for store the query result
    private boolean bResult = true;


/* Fion add start */
    public static final int DEFAULT_SIM = 2; /* 0: SIM1, 1: SIM2 */

    private int mSimId = DEFAULT_SIM;
/* Fion add end */

    private boolean isVtCBarring = false;
    private int mServiceClass = CommandsInterface.SERVICE_CLASS_VOICE;

    public CallBarringBasePreference(Context context) {
        this(context, null);
        setEnabled(false);
        mContext = context;
        phone = PhoneFactory.getDefaultPhone();
    }

    public CallBarringBasePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEnabled(false);
        mContext = context;
        phone = PhoneFactory.getDefaultPhone();
    }

    public void setRefreshInterface(CallBarringInterface i) {
        mCallBarringInterface = i;
    }
    public int getmTitle() {
        return mTitle;
    }

    public void setmTitle(int mTitle) {
        this.mTitle = mTitle;
    }

    public String getmFacility() {
        return mFacility;
    }

    public void setmFacility(String mFacility) {
        this.mFacility = mFacility;
    }

    @Override
    protected void onClick() {
        // not change the check box status
        mCurrentClickState = !this.isChecked();
    }

    public boolean onPreferenceClick(Preference preference) {
        doPreferenceClick(mContext.getString(mTitle));
        return true;
    }

    public void setCallState(String password) {
        setCallState(mFacility, false, password);
        if (tcpListener != null) {
            tcpListener.onStarted(this, false);
        }
    }
    
    private void doPreferenceClick(final String title) {
        LayoutInflater inflater = LayoutInflater.from(this.getContext());
        final View textEntryView = inflater.inflate(R.layout.callbarring_option, null);

        TextView content = (TextView) textEntryView.findViewById(R.id.ViewTop);
        content.setText(mContext
                .getString(R.string.enter_callbarring_password));

        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setView(textEntryView);
        builder.setTitle(title);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        EditText passwordText = (EditText) textEntryView
                                .findViewById(R.id.EditPassword);
                        String password = passwordText.getText().toString();
                       
                        if (!validatePassword(password)) {
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(
                                    mContext);
                            builder1.setTitle(title);
                            builder1.setMessage(mContext
                                    .getText(R.string.wrong_password));
                            builder1.setCancelable(false);
                            builder1.setPositiveButton(R.string.ok, null);
                            builder1.create().show();
                        } else {
                            if (tcpListener != null) {
                                tcpListener.onStarted(
                                        CallBarringBasePreference.this, false);
                                setCallState(
                                        mFacility,
                                        CallBarringBasePreference.this.mCurrentClickState,
                                        password);
                            }
                        }
                    }
                });
        
        AlertDialog dlg = builder.create();
        
        if(dlg != null) {
            Window window = dlg.getWindow();
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            dlg.show();
        }
        

        return;
    }

    // reason=true means get the call state when init.
    // reason=false means get the call stete after setting the state.
    private void getCallState(String facility, String password, boolean reason) {
        if (DBG)
            Xlog.i(LOG_TAG, "getCallState() is called with facility is "
                    + facility + "password is " + password + "reason is "
                    + reason);
        Message m;
        if (reason == true) {
            m = mHandler.obtainMessage(MyHandler.MESSAGE_GET_CALLBARRING_STATE,
                    0, MyHandler.MESSAGE_GET_CALLBARRING_STATE, null);
        } else {
            m = mHandler.obtainMessage(MyHandler.MESSAGE_GET_CALLBARRING_STATE,
                    0, MyHandler.MESSAGE_SET_CALLBARRING_STATE, null);
        }
/* Fion add start */
        if (CallSettings.isMultipleSim())
        {
        	if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO)
        	{
        		((GeminiPhone)phone).getVtFacilityLockGemini(facility, password, m, mSimId);  
        	}else
        	{
                ((GeminiPhone)phone).getFacilityLockGemini(facility, password, m, mSimId);  
        	}
        }
        else
        {
        	if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO)
        	{
                phone.getVtFacilityLock(facility, password, m);
        	}else
        	{
        		phone.getFacilityLock(facility, password, m);
        	}
        }
/* Fion add end */
    }

    private void setCallState(String facility, boolean enable, String password) {
        if (DBG)
            Xlog.i(LOG_TAG, "setCallState() is called with facility is "
                    + facility + "password is " + password + "enable is "
                    + enable);
        Message m = mHandler.obtainMessage(
                MyHandler.MESSAGE_SET_CALLBARRING_STATE, 0,
                MyHandler.MESSAGE_SET_CALLBARRING_STATE, null);

/* Fion add start */
        if (CallSettings.isMultipleSim())
        {
        	if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO)
        	{
        		((GeminiPhone)phone).setVtFacilityLockGemini(facility, enable, password, m, mSimId);
        	}else
        	{
                ((GeminiPhone)phone).setFacilityLockGemini(facility, enable, password, m, mSimId);
        	}
        }
        else
        {
        	if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO)
        	{
        		phone.setVtFacilityLock(facility, enable, password, m);
        	}else
        	{
                phone.setFacilityLock(facility, enable, password, m);
        	}
        }
/* Fion add end */

    }

/* Fion add start */
    void init(TimeConsumingPreferenceListener listener, boolean skipReading, int simId) {
        mSimId = simId;
/* Fion add end */

        setOnPreferenceClickListener(this);
        tcpListener = listener;
        if (!skipReading) {
            if (tcpListener != null) {
                Xlog.i(LOG_TAG, "init() is called");
                tcpListener.onStarted(this, true);
            }
            getCallState(mFacility, "", true);
        }
    }

    public String convertService(int value) {
        String str = "";
        if ((value & CommandsInterface.SERVICE_CLASS_VOICE) == CommandsInterface.SERVICE_CLASS_VOICE) {
            str += mContext.getString(R.string.lable_voice);
            str += ",";
        }
        if ((value & CommandsInterface.SERVICE_CLASS_DATA) == CommandsInterface.SERVICE_CLASS_DATA) {
            str += mContext.getString(R.string.lable_data);
            str += ",";
        }
        if ((value & CommandsInterface.SERVICE_CLASS_FAX) == CommandsInterface.SERVICE_CLASS_FAX) {
            str += mContext.getString(R.string.lable_fax);
            str += ",";
        }
        if ((value & CommandsInterface.SERVICE_CLASS_SMS) == CommandsInterface.SERVICE_CLASS_SMS) {
            str += mContext.getString(R.string.lable_sms);
            str += ",";
        }
        if ((value & CommandsInterface.SERVICE_CLASS_DATA_SYNC) == CommandsInterface.SERVICE_CLASS_DATA_SYNC) {
            str += mContext.getString(R.string.lable_data_sync);
            str += ",";
        }
        if ((value & CommandsInterface.SERVICE_CLASS_DATA_ASYNC) == CommandsInterface.SERVICE_CLASS_DATA_ASYNC) {
            str += mContext.getString(R.string.lable_data_async);
            str += ",";
        }
        if ((value & CommandsInterface.SERVICE_CLASS_PACKET) == CommandsInterface.SERVICE_CLASS_PACKET) {
            str += mContext.getString(R.string.lable_packet);
            str += ",";
        }
        if ((value & CommandsInterface.SERVICE_CLASS_PAD) == CommandsInterface.SERVICE_CLASS_PAD) {
            str += mContext.getString(R.string.lable_pad);
            str += ",";
        }
        // Should remove the last ","
        if (str.length() > 0)
            str = str.substring(0, str.length() - 1);
        Xlog.i(LOG_TAG, str);
        return str;
    }
    
    private boolean validatePassword(String password) {
        if (password == null || password.length() != PASSWORD_LENGTH) 
            return false;
        return true;
    }

    private class MyHandler extends Handler {
        private static final int MESSAGE_GET_CALLBARRING_STATE = 0;
        private static final int MESSAGE_SET_CALLBARRING_STATE = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_GET_CALLBARRING_STATE:
                handleGetCallBarringResponse(msg);
                break;
            case MESSAGE_SET_CALLBARRING_STATE:
                handleSetCallBarringResponse(msg);
                break;
            }
        }

        private void handleSetCallBarringResponse(Message msg) {
        	int errorid;
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception != null) {
                if (DBG)
                    Xlog.i(LOG_TAG, "handleSetCallBarringResponse: ar.exception="
                            + ar.exception);
                // TODO password error
                // if(password error)
                CommandException ce = (CommandException) ar.exception;
                if (ce.getCommandError() == CommandException.Error.PASSWORD_INCORRECT){
                	errorid =  PASSWORD_ERROR;
                }else if (ce.getCommandError() == CommandException.Error.FDN_CHECK_FAILURE){
                	errorid = FDN_FAIL;
                }else{
                	errorid = EXCEPTION_ERROR;
                }
                mCallBarringInterface.setErrorState(errorid);
                tcpListener.onFinished(CallBarringBasePreference.this, false);
                tcpListener.onError(CallBarringBasePreference.this,
                		errorid);
                // TODO Other error
            } else {
                // TODO Maybe this password should be the input password.
                if (DBG)
                    Xlog.i(LOG_TAG,
                            "handleSetCallBarringResponse is called without exception");
                CallBarringBasePreference.this.getCallState(mFacility, "",
                        false);
            }
        }

        private void handleGetCallBarringResponse(Message msg) {
            int errorid;
            AsyncResult ar = (AsyncResult) msg.obj;
            /*if (msg.arg2 == MESSAGE_GET_CALLBARRING_STATE) {
                tcpListener.onFinished(CallBarringBasePreference.this, true);
            } else {
                tcpListener.onFinished(CallBarringBasePreference.this, false);
            }*/
            if (ar.exception != null) {
                bResult = false;
                if (DBG)
                    Xlog.i(LOG_TAG,
                            "handleGetCallBarringResponse: ar.exception="
                                    + ar.exception);
                CommandException ce = (CommandException) ar.exception;
                if (ce.getCommandError() == CommandException.Error.PASSWORD_INCORRECT){
                	errorid =  PASSWORD_ERROR;
                }else if (ce.getCommandError() == CommandException.Error.FDN_CHECK_FAILURE){
                	errorid = FDN_FAIL;
                }else{
                	errorid = EXCEPTION_ERROR;
                }
                mCallBarringInterface.setErrorState(errorid);
                
                
                try{
                	tcpListener.onError(CallBarringBasePreference.this, errorid);
                }
                catch(BadTokenException e)
                {
                	if (DBG) Xlog.d(LOG_TAG, "BadTokenException");
                }
                
                
                
            } else {
                if (DBG)
                    Xlog.i(LOG_TAG,
                            "handleGetCallBarringResponse is called without exception");
                CallBarringBasePreference.this.setEnabled(true);
                int[] ints = (int[]) ar.result;
                if (ints != null && ints.length > 0) {
                    bResult = true;
                    int value = ints[0];
                    //we consider voice service in here
                    Xlog.i(LOG_TAG, "Current value = " + value + "  Current serviceClass = " + mServiceClass);
                    value = value & mServiceClass;

                    Xlog.i(LOG_TAG, "After value & mServiceClass = " + value);
                    String summary = null;
                    if (DBG)
                        Xlog.i(LOG_TAG, "Value is " + value);
                    if (value == 0) {
                        summary = mContext.getString(R.string.lable_disable);
                        CallBarringBasePreference.this.setChecked(false);
                    } else {
                        summary = mContext.getString(R.string.lable_enable);
                        // TODO do not show full info
                        // summary += CallBarringBasePreference.this
                        // .convertService(value);
                        CallBarringBasePreference.this.setChecked(true);
                        mCallBarringInterface.doCallBarringRefresh(mFacility);
                    }
                    CallBarringBasePreference.this.setSummary(summary);
                } else {
                    bResult = false;
                    if (DBG)
                        Xlog.i(LOG_TAG,
                                "handleGetCallBarringResponse ar.result get error");
                }
            }
            
            if (msg.arg2 == MESSAGE_GET_CALLBARRING_STATE) {
                tcpListener.onFinished(CallBarringBasePreference.this, true);
            } else {
                tcpListener.onFinished(CallBarringBasePreference.this, false);
        }
    }
    }
    
    public boolean isSuccess()
    {
        return bResult;
    }

    public void setServiceClass(int serviceClass)
    {
    	mServiceClass = serviceClass;
    }
}
