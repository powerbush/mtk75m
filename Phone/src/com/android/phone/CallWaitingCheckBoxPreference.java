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

import static com.android.phone.TimeConsumingPreferenceActivity.EXCEPTION_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.FDN_FAIL;
import static com.android.phone.TimeConsumingPreferenceActivity.RESPONSE_ERROR;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import static com.android.phone.TimeConsumingPreferenceActivity.RESPONSE_ERROR;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.xlog.Xlog;

/* Fion add start */
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;
/* Fion add end */

public class CallWaitingCheckBoxPreference extends CheckBoxPreference {
    private static final String LOG_TAG = "Settings/CallWaitingCheckBoxPreference";
    private final boolean DBG = (PhoneApp.DBG_LEVEL >= 2);

    private final MyHandler mHandler = new MyHandler();
    Phone phone;
    TimeConsumingPreferenceListener tcpListener;

/* Fion add start */
    public static final int DEFAULT_SIM = 2; /* 0: SIM1, 1: SIM2 */
    private int mSimId = DEFAULT_SIM;
/* Fion add end */
    
    private int mServiceClass = CommandsInterface.SERVICE_CLASS_VOICE;

    public CallWaitingCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        phone = PhoneFactory.getDefaultPhone();
    }

    public CallWaitingCheckBoxPreference(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.checkBoxPreferenceStyle);
    }

    public CallWaitingCheckBoxPreference(Context context) {
        this(context, null);
    }

/* Fion add start */
    void init(TimeConsumingPreferenceListener listener, boolean skipReading, int simId) {
        Xlog.d(LOG_TAG,"init, simId = "+simId);
        tcpListener = listener;
        mSimId = simId;
        
        /*if (this.mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO) {
            return;	
        }*/

        if (!skipReading) {
/* Fion add start */
            if (CallSettings.isMultipleSim())
            {
            	if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO)
            	{
            		((GeminiPhone)phone).getVtCallWaitingGemini(mHandler.obtainMessage(MyHandler.MESSAGE_GET_CALL_WAITING,
	                        MyHandler.MESSAGE_GET_CALL_WAITING, MyHandler.MESSAGE_GET_CALL_WAITING), mSimId);
            	}else
            	{
	                ((GeminiPhone)phone).getCallWaitingGemini(mHandler.obtainMessage(MyHandler.MESSAGE_GET_CALL_WAITING,
	                        MyHandler.MESSAGE_GET_CALL_WAITING, MyHandler.MESSAGE_GET_CALL_WAITING), mSimId);
            	}
            }
            else
            {
            	if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO)
            	{
            		phone.getVtCallWaiting(mHandler.obtainMessage(MyHandler.MESSAGE_GET_CALL_WAITING,
		                    MyHandler.MESSAGE_GET_CALL_WAITING, MyHandler.MESSAGE_GET_CALL_WAITING));
            	}else
            	{
		            phone.getCallWaiting(mHandler.obtainMessage(MyHandler.MESSAGE_GET_CALL_WAITING,
		                    MyHandler.MESSAGE_GET_CALL_WAITING, MyHandler.MESSAGE_GET_CALL_WAITING));
            	}
            }
/* Fion add end */
            if (tcpListener != null) {
                tcpListener.onStarted(this, true);
            }
        }
    }
/* Fion add end */

    @Override
    protected void onClick() {
        super.onClick();
        boolean toState=isChecked();
		setChecked(!toState);
/* Fion add start */
            if (CallSettings.isMultipleSim())
            {
            	if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO)
            	{
            		((GeminiPhone)phone).setVtCallWaitingGemini(toState,
	                        mHandler.obtainMessage(MyHandler.MESSAGE_SET_CALL_WAITING), mSimId);
            	}else
            	{
	                ((GeminiPhone)phone).setCallWaitingGemini(toState,
	                        mHandler.obtainMessage(MyHandler.MESSAGE_SET_CALL_WAITING), mSimId);
            	}
            }
            else
            {
            	if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO)
            	{
            		phone.setVtCallWaiting(toState,
    	                    mHandler.obtainMessage(MyHandler.MESSAGE_SET_CALL_WAITING));
            	}else
            	{
	                phone.setCallWaiting(toState,
	                    mHandler.obtainMessage(MyHandler.MESSAGE_SET_CALL_WAITING));
            	}
            }
/* Fion add end */
        if (tcpListener != null) {
            tcpListener.onStarted(this, false);
        }
    }

    private class MyHandler extends Handler {
        private static final int MESSAGE_GET_CALL_WAITING = 0;
        private static final int MESSAGE_SET_CALL_WAITING = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_CALL_WAITING:
                    handleGetCallWaitingResponse(msg);
                    break;
                case MESSAGE_SET_CALL_WAITING:
                    handleSetCallWaitingResponse(msg);
                    break;
            }
        }

        private void handleGetCallWaitingResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (tcpListener != null) {
                if (msg.arg2 == MESSAGE_SET_CALL_WAITING) {
                    tcpListener.onFinished(CallWaitingCheckBoxPreference.this, false);
                } else {
                    tcpListener.onFinished(CallWaitingCheckBoxPreference.this, true);
                }
            }

            if (ar.exception != null) {
                if (DBG) {
                    Xlog.d(LOG_TAG, "handleGetCallWaitingResponse: ar.exception=" + ar.exception);
                }
                setEnabled(false);
                if (tcpListener != null) {
                    tcpListener.onException(CallWaitingCheckBoxPreference.this,
                            (CommandException)ar.exception);
                }
            } else if (ar.userObj instanceof Throwable) {
                if (tcpListener != null) tcpListener.onError(CallWaitingCheckBoxPreference.this, RESPONSE_ERROR);
            } else {
                if (DBG) Xlog.d(LOG_TAG, "handleGetCallWaitingResponse: CW state successfully queried.");
                setChecked(((int[]) ar.result)[0] == 1);
            }
        }

        private void handleSetCallWaitingResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception != null) {
                if (DBG) Xlog.d(LOG_TAG, "handleSetCallWaitingResponse: ar.exception=" + ar.exception);
                //setEnabled(false);
            }
            if (DBG) Xlog.d(LOG_TAG, "handleSetCallWaitingResponse: re get");

/* Fion add start */
            if (CallSettings.isMultipleSim())
            {
            	if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO)
            	{
            		((GeminiPhone)phone).getVtCallWaitingGemini(obtainMessage(MESSAGE_GET_CALL_WAITING,
                            MESSAGE_SET_CALL_WAITING, MESSAGE_SET_CALL_WAITING, ar.exception), mSimId);
            	}else
            	{
                    ((GeminiPhone)phone).getCallWaitingGemini(obtainMessage(MESSAGE_GET_CALL_WAITING,
                        MESSAGE_SET_CALL_WAITING, MESSAGE_SET_CALL_WAITING, ar.exception), mSimId);
            	}
            }
            else
            {
            	if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO)
            	{
            		phone.getVtCallWaiting(obtainMessage(MESSAGE_GET_CALL_WAITING,
    	                    MESSAGE_SET_CALL_WAITING, MESSAGE_SET_CALL_WAITING, ar.exception));
            	}else
            	{
	                phone.getCallWaiting(obtainMessage(MESSAGE_GET_CALL_WAITING,
	                    MESSAGE_SET_CALL_WAITING, MESSAGE_SET_CALL_WAITING, ar.exception));
            	}
        }
/* Fion add end */
        }
    }
    
    public void setServiceClass(int serviceClass)
    {
    	mServiceClass = serviceClass;
    }
}
