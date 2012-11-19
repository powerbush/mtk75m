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
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.mms;

import com.android.mms.data.Contact;
import com.android.mms.data.Conversation;
import com.android.mms.layout.LayoutManager;
import com.android.mms.util.DownloadManager;
import com.android.mms.util.DraftCache;
import com.android.mms.drm.DrmUtils;
import com.android.mms.util.SmileyParser;
import com.android.mms.util.RateController;
import com.android.mms.MmsConfig;
import com.android.mms.transaction.MessagingNotification;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import com.mediatek.xlog.Xlog;
import com.mediatek.xlog.SXlog;

public class MmsApp extends Application {
    public static final String LOG_TAG = "Mms";
    public static final String TXN_TAG = "Mms/Txn";

    private SearchRecentSuggestions mRecentSuggestions;
    private TelephonyManager mTelephonyManager;
    private static MmsApp sMmsApp = null;

    // for toast thread
    public static final int MSG_RETRIEVE_FAILURE_DEVICE_MEMORY_FULL = 2;
    public static final int MSG_SHOW_TRANSIENTLY_FAILED_NOTIFICATION = 4;
    public static final int MSG_MMS_TOO_BIG_TO_DOWNLOAD = 6;
    public static final int MSG_MMS_CAN_NOT_OPEN = 10;
    public static final int EVENT_QUIT = 100;
    private static HandlerThread mToastthread = null;
    private static Looper mToastLooper = null;
    private static ToastHandler mToastHandler = null;

    @Override
    public void onCreate() {
        super.onCreate();

        sMmsApp = this;

        // Load the default preference values
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        MmsConfig.init(this);
        Contact.init(this);
        DraftCache.init(this);
        Conversation.init(this);
        DownloadManager.init(this);
        RateController.init(this);
        //DrmUtils.cleanupStorage(this);
        LayoutManager.init(this);
        SmileyParser.init(this);
        MessagingNotification.init(this);
        InitToastThread();
    }

    synchronized public static MmsApp getApplication() {
        return sMmsApp;
    }

    @Override
    public void onTerminate() {
        DrmUtils.cleanupStorage(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        LayoutManager.getInstance().onConfigurationChanged(newConfig);
    }

    /**
     * @return Returns the TelephonyManager.
     */
    public TelephonyManager getTelephonyManager() {
        if (mTelephonyManager == null) {
            mTelephonyManager = (TelephonyManager)getApplicationContext()
                    .getSystemService(Context.TELEPHONY_SERVICE);
        }
        return mTelephonyManager;
    }

    /**
     * Returns the content provider wrapper that allows access to recent searches.
     * @return Returns the content provider wrapper that allows access to recent searches.
     */
    public SearchRecentSuggestions getRecentSuggestions() {
        /*
        if (mRecentSuggestions == null) {
            mRecentSuggestions = new SearchRecentSuggestions(this,
                    SuggestionsProvider.AUTHORITY, SuggestionsProvider.MODE);
        }
        */
        return mRecentSuggestions;
    }

    private void InitToastThread() {
        if (null == mToastHandler) {
            HandlerThread thread = new HandlerThread("MMSAppToast");
            thread.start();
            mToastLooper = thread.getLooper();
            if (null != mToastLooper) {
                mToastHandler = new ToastHandler(mToastLooper);
            }
        }
    }

    public synchronized static ToastHandler getToastHandler() {
        /*if (null == mToastHandler) {
            HandlerThread thread = new HandlerThread("MMSAppToast");
            thread.start();
            mToastLooper = thread.getLooper();
            mToastHandler = new ToastHandler(mToastLooper);
        }*/
        return mToastHandler;
    }

    public final class ToastHandler extends Handler {
        public ToastHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Xlog.d(MmsApp.TXN_TAG, "Toast Handler handleMessage :" + msg);
            
            switch (msg.what) {
                case EVENT_QUIT: {
                    Xlog.d(MmsApp.TXN_TAG, "EVENT_QUIT");
                    getLooper().quit();
                    return;
                }

                case MSG_RETRIEVE_FAILURE_DEVICE_MEMORY_FULL: {
                    Toast.makeText(sMmsApp, R.string.download_failed_due_to_full_memory, Toast.LENGTH_LONG).show();
                    break;
                }

                case MSG_SHOW_TRANSIENTLY_FAILED_NOTIFICATION: {
                    Toast.makeText(sMmsApp, R.string.transmission_transiently_failed, Toast.LENGTH_LONG).show();
                    break;
                }

                case MSG_MMS_TOO_BIG_TO_DOWNLOAD: {
                    Toast.makeText(sMmsApp, R.string.mms_too_big_to_download, Toast.LENGTH_LONG).show();
                    break;
                }
                
                case MSG_MMS_CAN_NOT_OPEN: {
                    String str = sMmsApp.getResources().getString(R.string.unsupported_media_format, (String)msg.obj);
                    Toast.makeText(sMmsApp, str, Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }
    }

}
