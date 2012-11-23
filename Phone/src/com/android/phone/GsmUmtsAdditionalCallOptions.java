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

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import com.mediatek.xlog.Xlog;

/* Fion add start */
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;
/* Fion add end */

public class GsmUmtsAdditionalCallOptions extends
        TimeConsumingPreferenceActivity {
    private static final String LOG_TAG = "Settings/Callsettings";
    private final boolean DBG = true; // (PhoneApp.DBG_LEVEL >= 2);

    private static final String BUTTON_CLIR_KEY  = "button_clir_key";
    private static final String BUTTON_CW_KEY    = "button_cw_key";

    private CLIRListPreference mCLIRButton;
    private CallWaitingCheckBoxPreference mCWButton;

    private ArrayList<Preference> mPreferences = new ArrayList<Preference> ();
    private int mInitIndex= 0;
    Bundle mIcicle = null;
    boolean mFirstResume = false;

/* Fion add start */
    public static final int DEFAULT_SIM = 2; /* 0: SIM1, 1: SIM2 */
    private int mSimId = DEFAULT_SIM;
/* Fion add end */
    
    private boolean isVtSetting = false;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

/* Fion add start */
        if (CallSettings.isMultipleSim())
        {
            PhoneApp app = PhoneApp.getInstance();
            mSimId = getIntent().getIntExtra(app.phone.GEMINI_SIM_ID_KEY, -1);
        }
        isVtSetting = getIntent().getBooleanExtra("ISVT", false);
        Xlog.d(LOG_TAG, "[GsmUmtsAdditionalCallOptions]Sim Id : " + mSimId + " ISVT = " + isVtSetting);		
/* Fion add end */

        addPreferencesFromResource(R.xml.gsm_umts_additional_options);

        PreferenceScreen prefSet = getPreferenceScreen();
        mCLIRButton = (CLIRListPreference) prefSet.findPreference(BUTTON_CLIR_KEY);
        mCWButton = (CallWaitingCheckBoxPreference) prefSet.findPreference(BUTTON_CW_KEY);

        mPreferences.add(mCLIRButton);
        mPreferences.add(mCWButton);
        mIcicle = icicle;
        mFirstResume = true;
        
        if (null != getIntent().getStringExtra(MultipleSimActivity.SUB_TITLE_NAME))
        {
            setTitle(getIntent().getStringExtra(MultipleSimActivity.SUB_TITLE_NAME));
        }
        
        //mCLIRButton.setVtSetting(isVtSetting);
        if (isVtSetting)
        {
            mCWButton.setServiceClass(CommandsInterface.SERVICE_CLASS_VIDEO);
            //Our VT don't support Call Waiting, so
            /*mCWButton.setChecked(false);
			mCWButton.setEnabled(false);*/
        }
        
        PhoneUtils.setMmiFinished(false);
    }

    public void onResume()
    {
        super.onResume();
        mInitIndex = 0;
        if (mFirstResume == true)
        {
            mCLIRButton.init(this, false, mSimId);
            mFirstResume = false;
        }
        else
        {
        	if (PhoneUtils.getMmiFinished() == true)
        	{
                     mCLIRButton.init(this, false, mSimId);
                     PhoneUtils.setMmiFinished(false);
        	}
        	else
        	{
        		mInitIndex = mPreferences.size() - 1;
            }
        }
        
		/*if (isVtSetting) {
			//Our VT don't support Call Waiting, so
			mCWButton.setChecked(false);
			mCWButton.setEnabled(false);
		}*/
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mCLIRButton.clirArray != null) {
            outState.putIntArray(mCLIRButton.getKey(), mCLIRButton.clirArray);
        }
    }

    @Override
    public void onFinished(Preference preference, boolean reading) {
        if (mInitIndex < mPreferences.size()-1 && !isFinishing()) {
            mInitIndex++;
            Preference pref = mPreferences.get(mInitIndex);
            if (pref instanceof CallWaitingCheckBoxPreference) {
/* Fion add start */
                ((CallWaitingCheckBoxPreference) pref).init(this, false, mSimId);
/* Fion add end */
            }
        }
        super.onFinished(preference, reading);
        
        /*if (isVtSetting) {
			//Our VT don't support Call Waiting, so
			mCWButton.setChecked(false);
			mCWButton.setEnabled(false);
		}*/
    }

}
