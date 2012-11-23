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
import com.android.phone.PhoneApp;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.util.AttributeSet;
import com.mediatek.xlog.Xlog;

import static com.android.phone.TimeConsumingPreferenceActivity.EXCEPTION_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.RESPONSE_ERROR;

/* Fion add start */
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;
/* Fion add end */

public class CallCostLastPreference extends Preference {
    private static final String LOG_TAG = "Settings/CallCostLastPreference";
    private static final boolean DBG = true;// (PhoneApp.DBG_LEVEL >= 2);
    private static final int MESSAGE_GET_LAST_CALL_COST = 100;

    private MyHandler mHandler = new MyHandler();
    Phone phone;
    TimeConsumingPreferenceListener mListener;

/* Fion add start */
    public static final int DEFAULT_SIM = 2; /* 0: SIM1, 1: SIM2 */

    private int mSimId = DEFAULT_SIM;
/* Fion add end */

    public CallCostLastPreference(Context context) {
        this(context, null);
    }

    public CallCostLastPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        phone = PhoneFactory.getDefaultPhone();
        setEnabled(false);
    }

    private void getLastCallCost() {
        Message m = mHandler.obtainMessage(MESSAGE_GET_LAST_CALL_COST, 0,
                MESSAGE_GET_LAST_CALL_COST, null);
/* Fion add start */
        if (CallSettings.isMultipleSim())
        {
            ((GeminiPhone)phone).getCurrentCallMeterGemini(m, mSimId);
        }
        else
        {
            phone.getCurrentCallMeter(m);
        }
/* Fion add end */
    }

/* Fion add start */
    void init(TimeConsumingPreferenceListener listener, boolean skipReading, int simId) {
        Xlog.d(LOG_TAG,"init, simId = "+simId);
        mListener = listener;
        mSimId = simId;
		
        if (!skipReading) {
            getLastCallCost();
            if (mListener != null) {
                mListener.onStarted(this, true);
            }
        } else {
            // TODO
        }
    }
/* Fion add end */

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_GET_LAST_CALL_COST:
                handleGetLastCallCostResponse(msg);
                break;
            }
        }

        private void handleGetLastCallCostResponse(Message msg) {
            Xlog.d(LOG_TAG,"handleGetLastCallCostResponse: request get.");
            if (msg.arg2 == MESSAGE_GET_LAST_CALL_COST) {
                if (mListener != null)
                    mListener.onFinished(CallCostLastPreference.this, true);
            }
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception != null) {
                setSummary(R.string.disable);
                setEnabled(false);
                if (DBG)
                    Xlog.d(LOG_TAG,
                            "handleGetLastCallCostResponse: ar.exception="
                                    + ar.exception);
                if (mListener != null)
                    mListener.onError(CallCostLastPreference.this,
                            EXCEPTION_ERROR);
            } else {
                if (ar.userObj instanceof Throwable) {
                    if (mListener != null)
                        mListener.onError(CallCostLastPreference.this,
                                RESPONSE_ERROR);
                } else {
                    if ((String) ar.result != null) {
                        String lastCallCostNum = ((String) ar.result);
                        if (checkHexStr(lastCallCostNum)) {
                        	double lastCost = Long.valueOf(lastCallCostNum, 16);
                            lastCallCostNum = String.valueOf(lastCost / 1000);
                            setSummary(lastCallCostNum.subSequence(0,
                                    lastCallCostNum.length()));
                            Xlog.d(LOG_TAG,"handleGetLastCallCostResponse: lastCallCostNum="+lastCallCostNum);
                        } else {
                            setSummary(R.string.loading_data_error);
                        }
                    } else {
                        Xlog.i(LOG_TAG,
                                "handleGetLastCallCostResponse: ar.result is null");
                    }
                }
            }
        }
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
}
