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

import com.android.phone.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.net.Uri;
import android.net.ThrottleManager;
import com.mediatek.xlog.Xlog;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.text.DateFormat;

/**
 * Listener for broadcasts from ThrottleManager
 */
public class DataUsageListener {

    private ThrottleManager mThrottleManager;
    private Preference mCurrentUsagePref = null;
    private Preference mTimeFramePref = null;
    private Preference mThrottleRatePref = null;
    private Preference mSummaryPref = null;
    private PreferenceScreen mPrefScreen = null;
    private boolean mSummaryPrefEnabled = false;

    private final Context mContext;
    private IntentFilter mFilter;
    private BroadcastReceiver mReceiver;

    private int mPolicyThrottleValue;  //in kbps
    private long mPolicyThreshold;
    private int mCurrentThrottleRate;
    private long mDataUsed;
    private Calendar mStart;
    private Calendar mEnd;

    public DataUsageListener(Context context, Preference summary, PreferenceScreen prefScreen) {
        mContext = context;
        mSummaryPref = summary;
        mPrefScreen = prefScreen;
        mSummaryPrefEnabled = true;
        initialize();
    }

    public DataUsageListener(Context context, Preference currentUsage,
            Preference timeFrame, Preference throttleRate) {
        mContext = context;
        mCurrentUsagePref = currentUsage;
        mTimeFramePref = timeFrame;
        mThrottleRatePref = throttleRate;
        initialize();
    }

    private void initialize() {

        mThrottleManager = (ThrottleManager) mContext.getSystemService(Context.THROTTLE_SERVICE);

        mStart = GregorianCalendar.getInstance();
        mEnd = GregorianCalendar.getInstance();

        mFilter = new IntentFilter();
        mFilter.addAction(ThrottleManager.THROTTLE_POLL_ACTION);
        mFilter.addAction(ThrottleManager.THROTTLE_ACTION);
        mFilter.addAction(ThrottleManager.POLICY_CHANGED_ACTION);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ThrottleManager.THROTTLE_POLL_ACTION.equals(action)) {
                    updateUsageStats(intent.getLongExtra(ThrottleManager.EXTRA_CYCLE_READ, 0),
                        intent.getLongExtra(ThrottleManager.EXTRA_CYCLE_WRITE, 0),
                        intent.getLongExtra(ThrottleManager.EXTRA_CYCLE_START, 0),
                        intent.getLongExtra(ThrottleManager.EXTRA_CYCLE_END, 0));
                } else if (ThrottleManager.POLICY_CHANGED_ACTION.equals(action)) {
                    updatePolicy();
                } else if (ThrottleManager.THROTTLE_ACTION.equals(action)) {
                    updateThrottleRate(intent.getIntExtra(ThrottleManager.EXTRA_THROTTLE_LEVEL, -1));
                }
            }
        };
    }

    void resume() {
        mContext.registerReceiver(mReceiver, mFilter);
        updatePolicy();
    }

    void pause() {
        mContext.unregisterReceiver(mReceiver);
    }

    private void updatePolicy() {
        /* Fetch values for default interface */
        mPolicyThrottleValue = mThrottleManager.getCliffLevel(null, 1);
        mPolicyThreshold = mThrottleManager.getCliffThreshold(null, 1);

        if (mSummaryPref != null) { /* Settings preference */
            /**
             * Remove data usage preference in settings
             * if policy change disables throttling
             */
            /*if (mPolicyThreshold == 0) {
                if (mSummaryPrefEnabled) {
                    mPrefScreen.removePreference(mSummaryPref);
                    mSummaryPrefEnabled = false;
                }
            } else {
                if (!mSummaryPrefEnabled) {
                    mSummaryPrefEnabled = true;
                    mPrefScreen.addPreference(mSummaryPref);
                }
            }*/
        }
        updateUI();
    }

    private void updateThrottleRate(int throttleRate) {
        mCurrentThrottleRate = throttleRate;
        updateUI();
    }

    private void updateUsageStats(long readByteCount, long writeByteCount,
            long startTime, long endTime) {
        mDataUsed = readByteCount + writeByteCount;
        mStart.setTimeInMillis(startTime);
        mEnd.setTimeInMillis(endTime);
        updateUI();
    }

    private void updateUI() {
        if (mPolicyThreshold == 0)
            return;
        int dataUsedPercent = (int) ((mDataUsed * 100) / mPolicyThreshold);

        long cycleTime = mEnd.getTimeInMillis() - mStart.getTimeInMillis();
        long currentTime = GregorianCalendar.getInstance().getTimeInMillis()
                            - mStart.getTimeInMillis();

        int cycleThroughPercent = (cycleTime == 0) ? 0 : (int) ((currentTime * 100) / cycleTime);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(cycleTime - currentTime);
        int daysLeft = cal.get(Calendar.DAY_OF_YEAR);
        //cal.get() returns 365 for less than a day
        if (daysLeft >= 365) daysLeft = 0;

        if (mCurrentUsagePref != null) {
            /* Update the UI based on whether we are in a throttled state */
            if (mCurrentThrottleRate > 0) {
                mCurrentUsagePref.setSummary(mContext.getString(
                        R.string.throttle_data_rate_reduced_subtext,
                        toReadable(mPolicyThreshold),
                        mCurrentThrottleRate));
            } else {
                mCurrentUsagePref.setSummary(mContext.getString(
                        R.string.throttle_data_usage_subtext,
                        toReadable(mDataUsed), dataUsedPercent, toReadable(mPolicyThreshold)));
            }
        }
        if (mTimeFramePref != null) {
            mTimeFramePref.setSummary(mContext.getString(R.string.throttle_time_frame_subtext,
                        cycleThroughPercent, daysLeft,
                        DateFormat.getDateInstance(DateFormat.SHORT).format(mEnd.getTime())));
        }
        if (mThrottleRatePref != null) {
            mThrottleRatePref.setSummary(mContext.getString(R.string.throttle_rate_subtext,
                    mPolicyThrottleValue));
        }
        if (mSummaryPref != null && mSummaryPrefEnabled) {

            /* Update the UI based on whether we are in a throttled state */
            if (mCurrentThrottleRate > 0) {
                mSummaryPref.setSummary(mContext.getString(
                        R.string.throttle_data_rate_reduced_subtext,
                        toReadable(mPolicyThreshold),
                        mCurrentThrottleRate));
            } else {
                mSummaryPref.setSummary(mContext.getString(R.string.throttle_status_subtext,
                            toReadable(mDataUsed),
                            dataUsedPercent,
                            toReadable(mPolicyThreshold),
                            daysLeft,
                            DateFormat.getDateInstance(DateFormat.SHORT).format(mEnd.getTime())));
            }
        }
    }

    private String toReadable (long data) {
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
}
