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

import java.util.ArrayList;

import com.android.phone.CallCostLastPreference;
import com.android.phone.TimeConsumingPreferenceActivity;

import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceScreen;

/* Fion add start */
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;
/* Fion add end */
import com.mediatek.xlog.Xlog;

public class CallCostSettings extends TimeConsumingPreferenceActivity implements
        CallCostResetTotalInterface {
    private static final String LOG_TAG = "Settings/Callsettings";
    private final boolean DBG = true;

    private static final String BUTTON_LAST_CALL_COST_KEY = "button_last_call_cost_key";
    private static final String BUTTON_TOTAL_CALL_COST_KEY = "button_total_call_cost_key";
    private static final String BUTTON_MAX_CALL_COST_KEY = "button_max_call_cost_key";
    private static final String BUTTON_RESET_CALL_COST_KEY = "button_reset_call_cost_key";
    private static final String BUTTON_SET_PPU_CURRENCY_KEY = "button_set_ppu_currency_key";
    private static final int GET_SIM_RETRY_EMPTY = -1;

    private CallCostLastPreference mLastCallButton;
    private CallCostTotalPreference mTotalCallButton;
    private CallCostMaxPreference mMaxCallButton;
    private CallCostResetPreference mResetCallCostButton;
    private CallCostSetPpuCurrencyPreference mSetPpuCurrencyButton;

    private ArrayList<Preference> mPreferences = new ArrayList<Preference>();
    private int mInitIndex = 0;

/* Fion add start */
    public static final int DEFAULT_SIM = 2; /* 0: SIM1, 1: SIM2 */

    private int mSimId = DEFAULT_SIM;
/* Fion add end */


    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

/* Fion add start */
        if (CallSettings.isMultipleSim())
        {
            PhoneApp app = PhoneApp.getInstance();
            mSimId = getIntent().getIntExtra(app.phone.GEMINI_SIM_ID_KEY, -1);
        }
        Xlog.d(LOG_TAG, "[CallCostSettings]Sim Id : " + mSimId);		
/* Fion add end */

        addPreferencesFromResource(R.xml.call_cost_settings);
        PreferenceScreen prefSet = getPreferenceScreen();
        
        mLastCallButton = (CallCostLastPreference) prefSet
                .findPreference(BUTTON_LAST_CALL_COST_KEY);
        mTotalCallButton = (CallCostTotalPreference) prefSet
                .findPreference(BUTTON_TOTAL_CALL_COST_KEY);
        mMaxCallButton = (CallCostMaxPreference) prefSet
                .findPreference(BUTTON_MAX_CALL_COST_KEY);
        mResetCallCostButton = (CallCostResetPreference) prefSet
                .findPreference(BUTTON_RESET_CALL_COST_KEY);
        mSetPpuCurrencyButton = (CallCostSetPpuCurrencyPreference) prefSet
                .findPreference(BUTTON_SET_PPU_CURRENCY_KEY);
        mResetCallCostButton.setAocInterface(this);

        mPreferences.add(mLastCallButton);
        mPreferences.add(mTotalCallButton);
        mPreferences.add(mMaxCallButton);
        mPreferences.add(mSetPpuCurrencyButton);
        mPreferences.add(mResetCallCostButton);
        
        if (null != getIntent().getStringExtra(MultipleSimActivity.SUB_TITLE_NAME))
        {
            setTitle(getIntent().getStringExtra(MultipleSimActivity.SUB_TITLE_NAME));
        }

        if (icicle == null) {
            if (DBG)
                Xlog.d(LOG_TAG, "[CallCostSettings]start to init ");
/* Fion add start */
            mLastCallButton.init(this, false, mSimId);
/* Fion add emd */
        } else {
            
        	finish();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if(getRetryPuk2Count()==0){
            mLastCallButton.setEnabled(false);
            mLastCallButton.setSummary(R.string.disable);
            mTotalCallButton.setEnabled(false);
            mTotalCallButton.setSummary(R.string.disable);
            mMaxCallButton.setEnabled(false);
            mMaxCallButton.setSummary(R.string.disable);
            mSetPpuCurrencyButton.setEnabled(false);
            mSetPpuCurrencyButton.setSummary(R.string.disable);
            mResetCallCostButton.setEnabled(false);
            mResetCallCostButton.setSummary(R.string.disable);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // TODO
    }

    @Override
    public void onFinished(Preference preference, boolean reading) {
        if (mInitIndex < mPreferences.size() - 1 && !isFinishing()) {
            mInitIndex++;
            Preference pref = mPreferences.get(mInitIndex);

/* Fion add start */
            if (pref instanceof CallCostLastPreference) {
                ((CallCostLastPreference) pref).init(this, false, mSimId);
            } else if (pref instanceof CallCostTotalPreference) {
                ((CallCostTotalPreference) pref).init(this, false, mSimId);
            } else if (pref instanceof CallCostMaxPreference) {
                ((CallCostMaxPreference) pref).init(this, false, mSimId);
            } else if (pref instanceof CallCostSetPpuCurrencyPreference) {
                ((CallCostSetPpuCurrencyPreference) pref).init(this, false, mSimId);
            } else if (pref instanceof CallCostResetPreference) {
                ((CallCostResetPreference) pref).init(this, false, mSimId);
            }
        }
/* Fion add end */
        super.onFinished(preference, reading);
    }

    public void resetTotalCost() {
        mTotalCallButton.setSummary("0");
    }
    
    private int getRetryPuk2Count() {
        return SystemProperties.getInt("gsm.sim.retry.puk2",GET_SIM_RETRY_EMPTY);
    }
}
