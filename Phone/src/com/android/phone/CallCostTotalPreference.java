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
import android.widget.Toast;
import com.mediatek.xlog.Xlog;

import static com.android.phone.TimeConsumingPreferenceActivity.EXCEPTION_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.RESPONSE_ERROR;

/* Fion add start */
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;
/* Fion add end */

public class CallCostTotalPreference extends Preference {
    private static final String LOG_TAG = "Settings/CallCostTotalPreference";
    private static final boolean DBG = true; // (PhoneApp.DBG_LEVEL >= 2);
    private static final int MESSAGE_GET_TOTAL_CALL_COST = 100;
    
    private MyHandler mHandler = new MyHandler();
    Phone phone;
    TimeConsumingPreferenceListener mListener;

/* Fion add start */
    public static final int DEFAULT_SIM = 2; /* 0: SIM1, 1: SIM2 */

    private int mSimId = DEFAULT_SIM;
/* Fion add end */

    public CallCostTotalPreference(Context context) {
        this(context, null);
    }

    public CallCostTotalPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        phone = PhoneFactory.getDefaultPhone();
        setEnabled(false);
    }

/* Fion add start */
    void init(TimeConsumingPreferenceListener listener, boolean skipReading, int simId) {
        Xlog.d(LOG_TAG,"init, simId = "+simId);
        mListener = listener;
        mSimId = simId;
        if (!skipReading) {
            getTotalCallCost();
            if (mListener != null) {
                mListener.onStarted(this, true);
            }
        }
    }
/* Fion add end */

    public void getTotalCallCost() {
         Message m = mHandler.obtainMessage(MESSAGE_GET_TOTAL_CALL_COST, 0,MESSAGE_GET_TOTAL_CALL_COST, null);

/* Fion add start */
        if (CallSettings.isMultipleSim())
        {
             ((GeminiPhone)phone).getAccumulatedCallMeterGemini(m, mSimId);
        }
        else
        {
            phone.getAccumulatedCallMeter(m);
        }
/* Fion add end */

     	 Xlog.i(LOG_TAG, "getTotalCallCost: send msg to system.");
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_GET_TOTAL_CALL_COST:
                handleGetTotalCallCostResponse(msg);
                break;
            }
        }

        private void handleGetTotalCallCostResponse(Message msg) {
       	 	Xlog.i(LOG_TAG, "handleGetTotalCallCostResponse: request get from system.");
            if (msg.arg2 == MESSAGE_GET_TOTAL_CALL_COST) {
                mListener.onFinished(CallCostTotalPreference.this, true);
            }
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception != null) {
                if (DBG) Xlog.d(LOG_TAG,"handleGetLastCallCostResponse: ar.exception="+ ar.exception);
                if (mListener != null)
                    mListener.onError(CallCostTotalPreference.this,EXCEPTION_ERROR);
                setEnabled(false);
                setSummary(R.string.disable);
            } else {
                if (ar.userObj instanceof Throwable) {
                    mListener.onError(CallCostTotalPreference.this,RESPONSE_ERROR);
                } else {
                    String totalCallCostNum;
                    totalCallCostNum = ((String) ar.result);
                    if (checkHexStr(totalCallCostNum)) {
                        totalCallCostNum = String.valueOf(Integer.valueOf(totalCallCostNum, 16));
                        setSummary(totalCallCostNum.subSequence(0, totalCallCostNum.length()));
                    } else {
                        setSummary(R.string.loading_data_error);
                        setEnabled(false);
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
