/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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
 */

/*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.util.List;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.phone.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.net.Uri;
import android.net.ThrottleManager;
import android.provider.Telephony.GPRSInfo;
import android.provider.Telephony.SIMInfo;
import com.mediatek.xlog.Xlog;
import com.mediatek.featureoption.FeatureOption;
/**
 * Lists the data usage and throttle settings
 */
public class DataUsage extends PreferenceActivity implements DialogInterface.OnClickListener{

    private Preference mCurrentUsagePref;
    private Preference mResetPref;
    private Preference mTimeFramePref;
    private Preference mThrottleRatePref;
    private Preference mHelpPref;
    private String mHelpUri;
    private int mSlotId = 0;
    private long mSimId = 0;
    private String mSum = "";
    private static long M = 1024 * 1024;

    private DataUsageListener mDataUsageListener;
    private ThrottleManager mThrottleManager;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mThrottleManager = (ThrottleManager) getSystemService(Context.THROTTLE_SERVICE);

        addPreferencesFromResource(R.xml.data_usage_settings);

        mCurrentUsagePref = findPreference("throttle_current_usage");
        mResetPref = findPreference("rest_total_usage");
        if (CallSettings.isMultipleSim())
        {
            mSlotId = getIntent().getIntExtra(Phone.GEMINI_SIM_ID_KEY, 0);
          //Avoid klocwork, if we can get here, the info must not null
            SIMInfo info = SIMInfo.getSIMInfoBySlot(this, mSlotId);
            mSimId = info != null ? info.mSimId : 0;
            //mSimId = SIMInfo.getSIMInfoBySlot(this, mSlotId).mSimId;
        }else
        {
            List<SIMInfo> sims = SIMInfo.getInsertedSIMList(this);
            if (sims == null || sims.size() == 0) {
               	Xlog.d("Settings/DataUsage:", "onCreate: Fatal error  Insert sim list is null!");
                finish();
                return ;
            }
        	mSimId = sims.get(0).mSimId;
        }
        
        if (null != getIntent().getStringExtra(MultipleSimActivity.SUB_TITLE_NAME))
        {
            setTitle(getIntent().getStringExtra(MultipleSimActivity.SUB_TITLE_NAME));
        }

        /*mTimeFramePref = findPreference("throttle_time_frame");
        mThrottleRatePref = findPreference("throttle_rate");
        mHelpPref = findPreference("throttle_help");

        mHelpUri = mThrottleManager.getHelpUri();
        if (mHelpUri == null ) {
            getPreferenceScreen().removePreference(mHelpPref);
        } else {
            mHelpPref.setSummary(getString(R.string.throttle_help_subtext));
        }

        mDataUsageListener = new DataUsageListener(this, mCurrentUsagePref,
                mTimeFramePref, mThrottleRatePref);*/
    }

    private String subSpecialString(double d)
    {
    	String src = String.valueOf(d);
    	int pos = src.indexOf('.');
    	if (pos != -1 && (src.length() - pos -1 > 3))
    	{
    		src = src.substring(0, pos + 4);
    	}
    	
    	return src;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        PhoneApp app = PhoneApp.getInstance();
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            ((GeminiPhone)app.phone).updateMobileData(mSlotId);
        } else {
            ((PhoneProxy)app.phone).updateMobileData();
        }
        //mDataUsageListener.resume();
        //Get the latest data usage amount
        GPRSInfo info = GPRSInfo.getGprsInfoBySim(this, mSimId);
        long total = info.mGprsIn + info.mGprsOut;
        /*
        if (total / M < 1)
        {
        	double temp = (double)total / 1024;
        	mSum = this.subSpecialString(temp) + "K";
        }else {
        	mSum = this.subSpecialString((double)total / M) + "M";
        }
        
        if (total == 0)
        {
        	mSum = "0K";
        }
        */
        mSum = toReadable(total);
        mCurrentUsagePref.setSummary(mSum);
    }
    
    private static String toReadable (long data) {
        long KB = 1024;
        long MB = 1024 * KB;
        long GB = 1024 * MB;
        long TB = 1024 * GB;
        String ret;

        if (data < KB) {
            ret = data + " bytes";
        } else if (data < MB) {
            ret = (data / KB) + " KB";
        } else if (data < GB) {
            ret = (data / MB) + " MB";
        } else if (data < TB) {
            ret = (data / GB) + " GB";
        } else {
            ret = (data / TB) + " TB";
        }
        return ret;
    }

    @Override
    protected void onPause() {
        super.onPause();
        //mDataUsageListener.pause();
    }


    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == mResetPref) {
        	new AlertDialog.Builder(this).setMessage(
                    getResources().getString(R.string.reset_alert))
                    .setTitle(android.R.string.dialog_alert_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(R.string.reset, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .show();
            return true;
        }

        return false;
    }
    
    public void onClick(DialogInterface dialog, int which) {
    	if (which == DialogInterface.BUTTON_POSITIVE)
    	{
    		Xlog.d("Settings/DataUsage: ", "Reset data amount!");
    		//Call the data amout
    		mCurrentUsagePref.setSummary("0 bytes");
    		GPRSInfo.resetGprsBySim(this, mSimId);
    	}
    }
}
