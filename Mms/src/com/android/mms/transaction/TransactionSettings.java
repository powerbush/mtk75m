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
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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

package com.android.mms.transaction;

import android.database.sqlite.SqliteWrapper;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.android.internal.telephony.Phone;
import android.provider.Telephony;
import android.provider.Telephony.SIMInfo;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;
import com.android.mms.MmsApp;
import com.mediatek.xlog.Xlog;
import com.mediatek.xlog.SXlog;


/**
 * Container of transaction settings. Instances of this class are contained
 * within Transaction instances to allow overriding of the default APN
 * settings or of the MMS Client.
 */
public class TransactionSettings {
    private static final String TAG = "TransactionSettings";
    private static final boolean DEBUG = true;
    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;

    private String mServiceCenter;
    private String mProxyAddress;
    private int mProxyPort = -1;

    private static final String[] APN_PROJECTION = {
            Telephony.Carriers.TYPE,            // 0
            Telephony.Carriers.MMSC,            // 1
            Telephony.Carriers.MMSPROXY,        // 2
            Telephony.Carriers.MMSPORT          // 3
    };
    private static final int COLUMN_TYPE         = 0;
    private static final int COLUMN_MMSC         = 1;
    private static final int COLUMN_MMSPROXY     = 2;
    private static final int COLUMN_MMSPORT      = 3;

    /**
     * Constructor that uses the default settings of the MMS Client.
     *
     * @param context The context of the MMS Client
     */
    public TransactionSettings(Context context, String apnName) {
        String selection = (apnName != null)?
                Telephony.Carriers.APN + "='" + apnName.trim() + "'": null;

        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                            Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "current"),
                            APN_PROJECTION, selection, null, null);

        if (cursor == null) {
            Log.e(TAG, "Apn is not found in Database!");
            return;
        }

        boolean sawValidApn = false;
        try {
            while (cursor.moveToNext() && TextUtils.isEmpty(mServiceCenter)) {
                // Read values from APN settings
                if (isValidApnType(cursor.getString(COLUMN_TYPE), Phone.APN_TYPE_MMS)) {
                    sawValidApn = true;
                    mServiceCenter = cursor.getString(COLUMN_MMSC) != null ? cursor.getString(COLUMN_MMSC).trim():null;
                    mProxyAddress = cursor.getString(COLUMN_MMSPROXY);
                    if (isProxySet()) {
                        String portString = cursor.getString(COLUMN_MMSPORT);
                        try {
                            mProxyPort = Integer.parseInt(portString);
                        } catch (NumberFormatException e) {
                            if (TextUtils.isEmpty(portString)) {
                                Log.w(TAG, "mms port not set!");
                            } else {
                                Log.e(TAG, "Bad port number format: " + portString, e);
                            }
                        }
                    }
                }
            }
        } finally {
            cursor.close();
        }

        if (sawValidApn && TextUtils.isEmpty(mServiceCenter)) {
            Log.e(TAG, "Invalid APN setting: MMSC is empty");
        }
    }

    // add for gemini
    /**
     * Constructor that uses the default settings of the MMS Client.
     *
     * @param context The context of the MMS Client
     */
    public TransactionSettings(Context context, String apnName, int slotId) {
        String selection = (apnName != null)?
                Telephony.Carriers.APN + "='" + apnName.trim() + "'": null;
        Cursor cursor = null;
        if (Phone.GEMINI_SIM_1 == slotId) {
            cursor = SqliteWrapper.query(context, context.getContentResolver(),
                                Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "current"),
                                APN_PROJECTION, selection, null, null);
        } else if (Phone.GEMINI_SIM_2 == slotId){
            cursor = SqliteWrapper.query(context, context.getContentResolver(),
                                Uri.withAppendedPath(Telephony.Carriers.GeminiCarriers.CONTENT_URI, "current"),
                                APN_PROJECTION, selection, null, null);
        } else {
            Xlog.e(MmsApp.TXN_TAG, "Invalide slot id:" + slotId);
        }

        if (cursor == null) {
            Xlog.e(MmsApp.TXN_TAG, "Apn is not found in Database!");
            return;
        }

        boolean sawValidApn = false;
        try {
            while (cursor.moveToNext() && TextUtils.isEmpty(mServiceCenter)) {
                // Read values from APN settings
                if (isValidApnType(cursor.getString(COLUMN_TYPE), Phone.APN_TYPE_MMS)) {
                    sawValidApn = true;
                    mServiceCenter = cursor.getString(COLUMN_MMSC) != null ? cursor.getString(COLUMN_MMSC).trim():null;
                    Xlog.d(MmsApp.TXN_TAG, "Service Center=" + mServiceCenter);
                    mProxyAddress = cursor.getString(COLUMN_MMSPROXY);
                    Xlog.d(MmsApp.TXN_TAG, "Proxy=" + mProxyAddress);
                    if (isProxySet()) {
                        String portString = cursor.getString(COLUMN_MMSPORT);
                        Xlog.d(MmsApp.TXN_TAG, "Port=" + portString);
                        try {
                            mProxyPort = Integer.parseInt(portString);
                        } catch (NumberFormatException e) {
                            if (TextUtils.isEmpty(portString)) {
                                Log.w(TAG, "mms port not set!");
                            } else {
                                Log.e(TAG, "Bad port number format: " + portString, e);
                            }
                        }
                    }
                }
            }
        } finally {
            cursor.close();
        }

        if (sawValidApn && TextUtils.isEmpty(mServiceCenter)) {
            Log.e(TAG, "Invalid APN setting: MMSC is empty");
        }
    }


    /**
     * Constructor that overrides the default settings of the MMS Client.
     *
     * @param mmscUrl The MMSC URL
     * @param proxyAddr The proxy address
     * @param proxyPort The port used by the proxy address
     * immediately start a SendTransaction upon completion of a NotificationTransaction,
     * false otherwise.
     */
    public TransactionSettings(String mmscUrl, String proxyAddr, int proxyPort) {
        mServiceCenter = mmscUrl != null ? mmscUrl.trim() : null;
        mProxyAddress = proxyAddr;
        mProxyPort = proxyPort;
    }

    public String getMmscUrl() {
        return mServiceCenter;
    }

    public String getProxyAddress() {
        return mProxyAddress;
    }

    public int getProxyPort() {
        return mProxyPort;
    }

    public boolean isProxySet() {
        return (mProxyAddress != null) && (mProxyAddress.trim().length() != 0);
    }

    static private boolean isValidApnType(String types, String requestType) {
        // If APN type is unspecified, assume APN_TYPE_ALL.
        if (TextUtils.isEmpty(types)) {
            return true;
        }

        for (String t : types.split(",")) {
            if (t.equals(requestType) || t.equals(Phone.APN_TYPE_ALL)) {
                return true;
            }
        }
        return false;
    }
}
