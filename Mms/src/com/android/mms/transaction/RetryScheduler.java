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

package com.android.mms.transaction;

import com.android.mms.R;
import com.android.mms.LogTag;
import com.android.mms.util.DownloadManager;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPersister;
import android.database.sqlite.SqliteWrapper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.text.format.DateFormat;
import android.util.Config;
import android.util.Log;
import android.widget.Toast;
// add for gemini
import com.mediatek.featureoption.FeatureOption;
import com.android.mms.MmsApp;
import com.mediatek.xlog.Xlog;
import com.mediatek.xlog.SXlog;


public class RetryScheduler implements Observer {
    private static final String TAG = "RetryScheduler";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;

    private final Context mContext;
    private final ContentResolver mContentResolver;

    private RetryScheduler(Context context) {
        mContext = context;
        mContentResolver = context.getContentResolver();
    }

    private static RetryScheduler sInstance;
    public static RetryScheduler getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new RetryScheduler(context);
        }
        return sInstance;
    }

    private boolean isConnected() {
        ConnectivityManager mConnMgr = (ConnectivityManager)
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        return (mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS).
                isConnected());
    }

    public void update(Observable observable) {
        try {
            Transaction t = (Transaction) observable;

            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "[RetryScheduler] update " + observable);
            }
            
            // We are only supposed to handle M-Notification.ind, M-Send.req
            // and M-ReadRec.ind.
            if ((t instanceof NotificationTransaction)
                    || (t instanceof RetrieveTransaction)
                    || (t instanceof ReadRecTransaction)
                    || (t instanceof SendTransaction)) {
                try {
                    TransactionState state = t.getState();
                    if (state.getState() == TransactionState.FAILED) {
                        Uri uri = state.getContentUri();
                        if (uri != null) {
                            scheduleRetry(uri);
                        }
                    }
                } finally {
                    t.detach(this);
                }
            }
        } finally {
            // add for gemini
            //if (FeatureOption.MTK_GEMINI_SUPPORT) {
            setRetryAlarm(mContext);
            //}else if (isConnected()) {
            //    setRetryAlarm(mContext);
            //}
        }
    }

    private void scheduleRetry(Uri uri) {
        long msgId = ContentUris.parseId(uri);

        Uri.Builder uriBuilder = PendingMessages.CONTENT_URI.buildUpon();
        uriBuilder.appendQueryParameter("protocol", "mms");
        uriBuilder.appendQueryParameter("message", String.valueOf(msgId));

        Cursor cursor = SqliteWrapper.query(mContext, mContentResolver,
                uriBuilder.build(), null, null, null, null);

        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    int msgType = cursor.getInt(cursor.getColumnIndexOrThrow(
                            PendingMessages.MSG_TYPE));

                    int retryIndex = cursor.getInt(cursor.getColumnIndexOrThrow(
                            PendingMessages.RETRY_INDEX)) + 1; // Count this time.

                    // TODO Should exactly understand what was happened.
                    int errorType = MmsSms.ERR_TYPE_GENERIC;

                    DefaultRetryScheme scheme = new DefaultRetryScheme(mContext, retryIndex);

                    ContentValues values = new ContentValues(4);
                    //long current = System.currentTimeMillis();
                    long current = SystemClock.elapsedRealtime();
                    boolean isRetryDownloading =
                            (msgType == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND);
                    boolean retry = true;
                    int respStatus = getResponseStatus(msgId);
                    //check the response status value in sending mms case
                    retry = checkSendMmsResponseStatus(respStatus);

                    if ((retryIndex < scheme.getRetryLimit()) && retry) {
                        long retryAt = current + scheme.getWaitingInterval();

                        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                            Log.v(TAG, "scheduleRetry: retry for " + uri + " is scheduled at "
                                    + (retryAt - System.currentTimeMillis()) + "ms from now");
                        }
                        Xlog.d(MmsApp.TXN_TAG, uri + " is scheduled at " + scheme.getWaitingInterval());

                        values.put(PendingMessages.DUE_TIME, retryAt);

                        if (isRetryDownloading) {
                            // Downloading process is transiently failed.
                            DownloadManager.getInstance().markState(
                                    uri, DownloadManager.STATE_TRANSIENT_FAILURE);
                        }

                        // notify transaction transiently failed
                        MmsApp.getToastHandler().sendEmptyMessage(MmsApp.MSG_SHOW_TRANSIENTLY_FAILED_NOTIFICATION);
                    } else {
                        errorType = MmsSms.ERR_TYPE_GENERIC_PERMANENT;
                        if (isRetryDownloading) {
                            Cursor c = SqliteWrapper.query(mContext, mContext.getContentResolver(), uri,
                                    new String[] { Mms.THREAD_ID }, null, null, null);
                            
                            long threadId = -1;
                            if (c != null) {
                                try {
                                    if (c.moveToFirst()) {
                                        threadId = c.getLong(0);
                                    }
                                } finally {
                                    c.close();
                                }
                            }

                            if (threadId != -1) {
                                // Downloading process is permanently failed.
                                MessagingNotification.notifyDownloadFailed(mContext, threadId);
                            }

                            DownloadManager.getInstance().markState(
                                    uri, DownloadManager.STATE_PERMANENT_FAILURE);
                        } else {
                            // Mark the failed message as unread.
                            ContentValues stateValues = new ContentValues(2);
                            stateValues.put(Mms.READ, 0);
                            stateValues.put(Mms.STATUS, PduHeaders.STATUS_UNREACHABLE);
                            SqliteWrapper.update(mContext, mContext.getContentResolver(),
                                    uri, stateValues, null, null);
                            MessagingNotification.notifySendFailed(mContext, true);
                        }
                    }

                    values.put(PendingMessages.ERROR_TYPE,  errorType);
                    values.put(PendingMessages.RETRY_INDEX, retryIndex);
                    values.put(PendingMessages.LAST_TRY,    current);

                    int columnIndex = cursor.getColumnIndexOrThrow(
                            PendingMessages._ID);
                    long id = cursor.getLong(columnIndex);
                    SqliteWrapper.update(mContext, mContentResolver,
                            PendingMessages.CONTENT_URI,
                            values, PendingMessages._ID + "=" + id, null);
                } else if (LOCAL_LOGV) {
                    Log.v(TAG, "Cannot found correct pending status for: " + msgId);
                }
            } finally {
                cursor.close();
            }
        }
    }

    private int getResponseStatus(long msgID) {
        int respStatus = 0;
        Cursor cursor = SqliteWrapper.query(mContext, mContentResolver,
                Mms.Outbox.CONTENT_URI, null, Mms._ID + "=" + msgID, null, null);
        try {
            if (cursor.moveToFirst()) {
                respStatus = cursor.getInt(cursor.getColumnIndexOrThrow(Mms.RESPONSE_STATUS));
            }
        } finally {
            cursor.close();
        }
        if (respStatus != 0) {
            Log.e(TAG, "Response status is: " + respStatus);
        }
        return respStatus;
    }

    public static void setRetryAlarm(Context context) {
        Cursor cursor = PduPersister.getPduPersister(context).getPendingMessages(
                Long.MAX_VALUE);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    // The result of getPendingMessages() is order by due time.
                    long retryAt = cursor.getLong(cursor.getColumnIndexOrThrow(
                            PendingMessages.DUE_TIME));
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(
                            PendingMessages.MSG_ID));
                    if (retryAt == 0) {
                        Xlog.d(MmsApp.TXN_TAG, "pendingid=" + id + "; ra=" + retryAt);
                        while (cursor.moveToNext()) {
                            retryAt = cursor.getLong(cursor.getColumnIndexOrThrow(
                                    PendingMessages.DUE_TIME));
                            id = cursor.getLong(cursor.getColumnIndexOrThrow(
                                    PendingMessages.MSG_ID));
                            if (retryAt == 0) {
                                continue;
                            } 
                            break;
                        }
                    }

                    if (retryAt == 0) {
                        return;
                    }

                    Intent service = new Intent(TransactionService.ACTION_ONALARM,
                                        null, context, TransactionService.class);
                    PendingIntent operation = PendingIntent.getService(
                            context, 0, service, PendingIntent.FLAG_ONE_SHOT);
                    AlarmManager am = (AlarmManager) context.getSystemService(
                            Context.ALARM_SERVICE);
                    //am.set(AlarmManager.RTC, retryAt, operation);
                    am.set(AlarmManager.ELAPSED_REALTIME, retryAt, operation);

                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "Next retry is scheduled at"
                                + (retryAt - System.currentTimeMillis()) + "ms from now");
                    }
                    Xlog.d(MmsApp.TXN_TAG, "Next is scheduled at" + (retryAt - SystemClock.elapsedRealtime()) 
                            + "pendingid=" + id + "; ra=" + retryAt);
                }
            } finally {
                cursor.close();
            }
        }
    }
    /*
    check the response status returned by server in sending mms case.
    the status value may indicate temporary fail or permanent fail.
    return true if it's a temporary fail and retry can be triggered.
    return false if it's a permanent fail.
    */
    private boolean checkSendMmsResponseStatus(int respStatus) {
        boolean retry = false;
        Xlog.d(MmsApp.TXN_TAG,"response status is " + respStatus);
        //current transaction is not a SendTransaction.
        if (respStatus < PduHeaders.RESPONSE_STATUS_OK) {
            return true;
        }
        if (respStatus >= PduHeaders.RESPONSE_STATUS_ERROR_PERMANENT_FAILURE) {
            retry = false;
        }else if (respStatus >= PduHeaders.RESPONSE_STATUS_ERROR_TRANSIENT_FAILURE) {
            retry = true;
        }
        switch(respStatus) {
            //ok status, normally never get here.
            case PduHeaders.RESPONSE_STATUS_OK:
                return true;
            //the status that should retry.
            //none.
            // the status that should permanent fail.
            case PduHeaders.RESPONSE_STATUS_ERROR_SENDING_ADDRESS_UNRESOLVED:
            case PduHeaders.RESPONSE_STATUS_ERROR_TRANSIENT_SENDNG_ADDRESS_UNRESOLVED:
                //show toast for this case.
                DownloadManager.getInstance().showErrorCodeToast(R.string.invalid_destination);
                return false;
            case PduHeaders.RESPONSE_STATUS_ERROR_UNSPECIFIED:
            case PduHeaders.RESPONSE_STATUS_ERROR_SERVICE_DENIED:
            case PduHeaders.RESPONSE_STATUS_ERROR_MESSAGE_FORMAT_CORRUPT:
            case PduHeaders.RESPONSE_STATUS_ERROR_MESSAGE_NOT_FOUND:
            case PduHeaders.RESPONSE_STATUS_ERROR_NETWORK_PROBLEM:
            case PduHeaders.RESPONSE_STATUS_ERROR_CONTENT_NOT_ACCEPTED:
            case PduHeaders.RESPONSE_STATUS_ERROR_UNSUPPORTED_MESSAGE:
                return false;
            default:
                break;
        }
        return retry;
    }
}
