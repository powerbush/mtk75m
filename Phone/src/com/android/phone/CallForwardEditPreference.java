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

import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.telephony.PhoneNumberUtils;
import android.text.BidiDirection;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.WindowManager.BadTokenException;
import com.mediatek.xlog.Xlog;

/* Fion add start */
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;
/* Fion add end */


import static com.android.phone.TimeConsumingPreferenceActivity.EXCEPTION_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.RESPONSE_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.FDN_FAIL;

public class CallForwardEditPreference extends EditPhoneNumberPreference {
    private static final String LOG_TAG = "Settings/CallForwardEditPreference";
    private static final boolean DBG = true; //(PhoneApp.DBG_LEVEL >= 2);

    private static final String SRC_TAGS[]       = {"{0}"};
    private CharSequence mSummaryOnTemplate;
    private int mButtonClicked = DialogInterface.BUTTON_NEGATIVE;
    private int mServiceClass;
    private MyHandler mHandler = new MyHandler();
    private boolean mCancel = false;
    int reason;
    Phone phone;
    CallForwardInfo callForwardInfo;
    TimeConsumingPreferenceListener tcpListener;
/* Fion add start */	
    private int mSimId;
/* Fion add end */

    private boolean bResult = true;
    private CallForwardInfo lastCallForwardInfo;
    
    private static final String BUTTON_CFU_KEY   = "button_cfu_key";
    private static final String BUTTON_CFB_KEY   = "button_cfb_key";
    private static final String BUTTON_CFNRY_KEY = "button_cfnry_key";
    private static final String BUTTON_CFNRC_KEY = "button_cfnrc_key";

    public CallForwardEditPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        phone = PhoneFactory.getDefaultPhone();
        mSummaryOnTemplate = this.getSummaryOn();

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.CallForwardEditPreference, 0, R.style.EditPhoneNumberPreference);
        mServiceClass = a.getInt(R.styleable.CallForwardEditPreference_serviceClass,
                CommandsInterface.SERVICE_CLASS_VOICE);
        reason = a.getInt(R.styleable.CallForwardEditPreference_reason,
                CommandsInterface.CF_REASON_UNCONDITIONAL);
        a.recycle();

        if (DBG) Xlog.d(LOG_TAG, "mServiceClass=" + mServiceClass + ", reason=" + reason);
    }

    public CallForwardEditPreference(Context context) {
        this(context, null);
    }

    /* Fion add start */
    void init(TimeConsumingPreferenceListener listener, boolean skipReading, int simId) {	
        tcpListener = listener;
        Xlog.d(LOG_TAG, "tcpListener =" + tcpListener);
        if (!skipReading) {

            if (CallSettings.isMultipleSim())
            {
                mSimId = simId;   
                Xlog.d(LOG_TAG, "init - simId =" + mSimId);

                if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO)
                {
                    ((GeminiPhone)phone).getVtCallForwardingOptionGemini(reason,
                            mHandler.obtainMessage(MyHandler.MESSAGE_GET_CF, reason,
                            MyHandler.MESSAGE_GET_CF, null), simId);
                }else
                {
                	((GeminiPhone)phone).getCallForwardingOptionGemini(reason,
                                mHandler.obtainMessage(MyHandler.MESSAGE_GET_CF, reason,
                                MyHandler.MESSAGE_GET_CF, null), simId);
                }
            }            
            else
            {
            	if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO)
            	{
            		phone.getVtCallForwardingOption(reason,
                            mHandler.obtainMessage(MyHandler.MESSAGE_GET_CF, reason,
                                MyHandler.MESSAGE_GET_CF, null));
            	}else
            	{
            		phone.getCallForwardingOption(reason,
                        mHandler.obtainMessage(MyHandler.MESSAGE_GET_CF, reason,
                            MyHandler.MESSAGE_GET_CF, null));
            	}
            }

            if (tcpListener != null) {
                tcpListener.onStarted(this, true);
            }
        }
    }
/* Fion add end */


    @Override
    public void onClick(DialogInterface dialog, int which) {
        
        if (which == DialogInterface.BUTTON_NEUTRAL) {
            this.setSummaryOn("");
        }
        super.onClick(dialog, which);
        mButtonClicked = which;
    }

    protected void onClick() {
        //showDialog(null);
        Dialog dialog = super.getDialog();
        if (dialog != null)
        {
            //There is a dialog has been displayed
            return ;
        }
        super.onClick();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if(mCancel || 0 == mButtonClicked){
           return;
        }
        if (DBG) Xlog.d(LOG_TAG, "mButtonClicked=" + mButtonClicked
                + ", positiveResult=" + positiveResult);
        if (this.mButtonClicked != DialogInterface.BUTTON_NEGATIVE) {
            int action = (isToggled() || (mButtonClicked == DialogInterface.BUTTON_POSITIVE)) ?
                    CommandsInterface.CF_ACTION_REGISTRATION :
                    CommandsInterface.CF_ACTION_DISABLE;
            int time = (reason != CommandsInterface.CF_REASON_NO_REPLY) ? 0 : 20;
            final String number = getPhoneNumber();

            if (DBG) Xlog.d(LOG_TAG, "callForwardInfo=" + callForwardInfo);

            //We save last callForwardInfo used for judge the set result.
            lastCallForwardInfo = callForwardInfo;

            if (action == CommandsInterface.CF_ACTION_REGISTRATION
                    && callForwardInfo != null
                    && callForwardInfo.status == 1
                    && number.equals(callForwardInfo.number)) {
                // no change, do nothing
                if (DBG) Xlog.d(LOG_TAG, "no change, do nothing");
            } else {
                // set to network
                if (DBG) Xlog.d(LOG_TAG, "reason=" + reason + ", action=" + action
                        + ", number=" + number);

                // Display no forwarding number while we're waiting for
                // confirmation
                setSummaryOn("");

                // the interface of Phone.setCallForwardingOption has error:
                // should be action, reason...
        /* Fion add start */
                if (CallSettings.isMultipleSim())
                {
                    if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO)
                    {
                    	((GeminiPhone)phone).setVtCallForwardingOptionGemini(action,
	                                reason,
	                                number,
	                                time,
	                                mHandler.obtainMessage(MyHandler.MESSAGE_SET_CF, action, MyHandler.MESSAGE_SET_CF), mSimId);
                    }else  {
	                        ((GeminiPhone)phone).setCallForwardingOptionGemini(action,
	                                reason,
	                                number,
	                                time,
                                    mHandler.obtainMessage(MyHandler.MESSAGE_SET_CF, action, MyHandler.MESSAGE_SET_CF), mSimId);
                   }
                        
                } else {
                        if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO)
                        {
                            phone.setVtCallForwardingOption(action,
                                    reason,
                                    number,
                                    time,
                                    mHandler.obtainMessage(MyHandler.MESSAGE_SET_CF, action, MyHandler.MESSAGE_SET_CF));
                        }else
                        {
                            phone.setCallForwardingOption(action,
                                    reason,
                                    number,
                                    time,
                                    mHandler.obtainMessage(MyHandler.MESSAGE_SET_CF, action, MyHandler.MESSAGE_SET_CF));
                        }
                    }

                if (tcpListener != null) {
                    tcpListener.onStarted(this, false);
                }
            }
        }
        //Clear the mButtonClicked because we have cosumed it, if user click back key, we don't handle it again
        mButtonClicked = 0;
    }

    void handleCallForwardResult(CallForwardInfo cf) {
        callForwardInfo = cf;
        if (DBG) Xlog.d(LOG_TAG, "handleGetCFResponse done, callForwardInfo=" + callForwardInfo);

        setToggled(callForwardInfo.status == 1);
        setPhoneNumber(callForwardInfo.number);
        //Update the summary text to avoid the phonenum == {0} after switch language
        updateSummaryText();
        updatePrefStatus();
    }
    
    private void updatePrefStatus() {
        if (0 == this.reason)
        {
            PreferenceManager pm = this.getPreferenceManager();
            if (this.isToggled() && this.isEnabled())
            {
                pm.findPreference(BUTTON_CFB_KEY).setEnabled(false);
                pm.findPreference(BUTTON_CFNRY_KEY).setEnabled(false);
                pm.findPreference(BUTTON_CFNRC_KEY).setEnabled(false);
            }
            else
            {
                if ((isToggled() == false) && (isEnabled() == true))
                pm.findPreference(BUTTON_CFB_KEY).setEnabled(true);
                pm.findPreference(BUTTON_CFNRY_KEY).setEnabled(true);
                pm.findPreference(BUTTON_CFNRC_KEY).setEnabled(true);
            }
        }
    }

    private void updateSummaryText() {
        if (isToggled()) {
            CharSequence summaryOn;
            final String number = getRawPhoneNumber();
            if (number != null && number.length() > 0) {
                String values[] = { number };
                
                BidiDirection dir = Layout.getDefaultDirection();
                if (dir == BidiDirection.DIRECTION_RTL) { //For language : ar
                    switch (this.reason) {
                        case 0:
                            summaryOn = getContext().getString(R.string.sum_cfu_enabled, values[0]);
                            break;
                        case 1:
                            summaryOn = getContext().getString(R.string.sum_cfb_enabled, values[0]);
                            break;
                        case 2:
                            summaryOn = getContext().getString(R.string.sum_cfnry_enabled, values[0]);
                            break;
                        case 3:
                            summaryOn = getContext().getString(R.string.sum_cfnrc_enabled, values[0]);
                            break;
                            default:
                                summaryOn = values[0];
                    }
                    
                } else {
                    summaryOn = TextUtils.replace(mSummaryOnTemplate, SRC_TAGS, values);
                }
            } else {
                summaryOn = getContext().getString(R.string.sum_cfu_enabled_no_number);
            }
            setSummaryOn(summaryOn);
        }

    }

    // Message protocol:
    // what: get vs. set
    // arg1: action -- register vs. disable
    // arg2: get vs. set for the preceding request
    private class MyHandler extends Handler {
        private static final int MESSAGE_GET_CF = 0;
        private static final int MESSAGE_SET_CF = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_CF:                    
                    handleGetCFResponse(msg);
                    break;
                case MESSAGE_SET_CF:                    
/* Fion add end */
                    handleSetCFResponse(msg);
                    break;
            }
        }

        private void handleGetCFResponse(Message msg) {
            if (DBG) Xlog.d(LOG_TAG, "handleGetCFResponse: done");

            /*if ((msg.arg2 == MESSAGE_SET_CF) ||  (msg.arg2 == MESSAGE_SET_CF2)){
                tcpListener.onFinished(CallForwardEditPreference.this, false);
            } else {
                tcpListener.onFinished(CallForwardEditPreference.this, true);
            }*/
            boolean foundServiceClass = false;
            AsyncResult ar = (AsyncResult) msg.obj;
            boolean isUserException = false;
            callForwardInfo = null;
            if (ar.exception != null) {
                if (DBG) Xlog.d(LOG_TAG, "handleGetCFResponse: ar.exception=" + ar.exception);
                setEnabled(false);
                bResult = false;
                tcpListener.onException(CallForwardEditPreference.this,
                        (CommandException) ar.exception);
            } else {
                if (ar.userObj instanceof Throwable) {
                    if (DBG) Xlog.d(LOG_TAG, "userObj == Throwable");
                    isUserException = true;
                    tcpListener.onError(CallForwardEditPreference.this, RESPONSE_ERROR);
                    tcpListener.onUpdate(tcpListener, isUserException);
                }
                CallForwardInfo cfInfoArray[] = (CallForwardInfo[]) ar.result;
                if (cfInfoArray.length == 0) {
                    if (DBG) Xlog.d(LOG_TAG, "handleGetCFResponse: cfInfoArray.length==0");
                    setEnabled(false);
                    bResult = false;
                    tcpListener.onError(CallForwardEditPreference.this, RESPONSE_ERROR);
                } else {
                    bResult = true;
                    //Enable the preference if the query is ok
                    setEnabled(true);

                    for (int i = 0, length = cfInfoArray.length; i < length; i++) {
                        if (DBG) Xlog.d(LOG_TAG, "handleGetCFResponse, cfInfoArray[" + i + "]="
                                + cfInfoArray[i]);
                        if ((mServiceClass & cfInfoArray[i].serviceClass) != 0) {
                            
                            //Found the correct serviceClass
                            foundServiceClass = true;
                            // corresponding class
                            CallForwardInfo info = cfInfoArray[i];
                            handleCallForwardResult(info);

                            // Show an alert if we got a success response but
                            // with unexpected values.
                            // Currently only handle the fail-to-disable case
                            // since we haven't observed fail-to-enable.
                            if (msg.arg2 == MESSAGE_SET_CF &&
                                    msg.arg1 == CommandsInterface.CF_ACTION_DISABLE &&
                                    info.status == 1) {
                                CharSequence s=null;
                                switch (reason) {
                                    case CommandsInterface.CF_REASON_BUSY:
                                        s = getContext().getText(R.string.disable_cfb_forbidden);
                                        break;
                                    case CommandsInterface.CF_REASON_NO_REPLY:
                                        s = getContext().getText(R.string.disable_cfnry_forbidden);
                                        break;
                                    case CommandsInterface.CF_REASON_NOT_REACHABLE:
                                        s = getContext().getText(R.string.disable_cfnrc_forbidden);
                                        break;                                        
                                    default: 
                                        break;
                                }
                                
                                if (s!=null)
                                {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                builder.setNeutralButton(R.string.close_dialog, null);
                                builder.setTitle(getContext().getText(R.string.error_updating_title));
                                builder.setMessage(s);
                                builder.setCancelable(true);
                                builder.create().show();
                            }
                        }
                    }
                }

                    
            }
            }

            if ((!foundServiceClass)&&isToggled()) {
               setToggled(false);

            }
            //Make sure the query to serialize
            Xlog.d(LOG_TAG, "tcpListener2 =" + tcpListener);
            if (msg.arg2 == MESSAGE_SET_CF){
                tcpListener.onFinished(CallForwardEditPreference.this, false);
            } else {
                tcpListener.onFinished(CallForwardEditPreference.this, true);
            }

            if ((msg.arg2 == MESSAGE_SET_CF) && (reason == 0) 
                && (!isToggled()) /*&& (callForwardInfo != null)*/)
            {
                if (((tcpListener instanceof GsmUmtsCallForwardOptions) 
                    && (lastCallForwardInfo != null) && (lastCallForwardInfo.status == 1)
                    && (callForwardInfo != null) && (callForwardInfo.status == 0)))
                {
                    ((GsmUmtsCallForwardOptions) tcpListener).refreshSettings(true);
                }
            }

            // Now whether or not we got a new number, reset our enabled
            // summary text since it may have been replaced by an empty
            // placeholder.
            updateSummaryText();
        }

        private void handleSetCFResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception != null) {
                if (DBG) Xlog.d(LOG_TAG, "handleSetCFResponse: ar.exception=" + ar.exception);
                // setEnabled(false);
            }
            if (DBG) Xlog.d(LOG_TAG, "handleSetCFResponse: re get");

            Xlog.d(LOG_TAG, "mSimdId: "+mSimId);            

                if (CallSettings.isMultipleSim())
                {
                        if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO)
                        {
                            ((GeminiPhone)phone).getVtCallForwardingOptionGemini(reason,
                                    obtainMessage(MESSAGE_GET_CF, msg.arg1, MESSAGE_SET_CF, ar.exception), mSimId);
                        }else
                        {
                            ((GeminiPhone)phone).getCallForwardingOptionGemini(reason,
                                    obtainMessage(MESSAGE_GET_CF, msg.arg1, MESSAGE_SET_CF, ar.exception), mSimId);
                        }
                        
                }      
                else
                {
                    if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO)
                    {
                        phone.getVtCallForwardingOption(reason,
                                obtainMessage(MESSAGE_GET_CF, msg.arg1, MESSAGE_SET_CF, ar.exception));
                    }else
                    {
                        phone.getCallForwardingOption(reason,
                            obtainMessage(MESSAGE_GET_CF, msg.arg1, MESSAGE_SET_CF, ar.exception));
                    }
               }
        }
    }
    public void setStatus(boolean status){
        mCancel = status;
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
