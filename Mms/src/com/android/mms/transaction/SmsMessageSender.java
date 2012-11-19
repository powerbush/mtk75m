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

import com.android.mms.MmsConfig;
import com.android.mms.LogTag;
import com.android.mms.MmsApp;
import com.android.mms.ui.MessagingPreferenceActivity;
import com.android.mms.ui.MessageUtils;
import com.google.android.mms.MmsException;
import android.database.sqlite.SqliteWrapper;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;

// add for gemini
import com.mediatek.featureoption.FeatureOption;
import android.telephony.gemini.GeminiSmsManager;
import com.android.internal.telephony.Phone;
import com.mediatek.xlog.Xlog;
import com.mediatek.xlog.SXlog;



public class SmsMessageSender implements MessageSender {
    protected final Context mContext;
    protected final int mNumberOfDests;
    private final String[] mDests;
    protected final String mMessageText;
    protected final String mServiceCenter;
    protected final long mThreadId;
    protected long mTimestamp;
    // add for gemini
    protected int mSimId = 0;

    // Default preference values
    private static final boolean DEFAULT_DELIVERY_REPORT_MODE  = false;

    private static final String[] SERVICE_CENTER_PROJECTION = new String[] {
        Sms.Conversations.REPLY_PATH_PRESENT,
        Sms.Conversations.SERVICE_CENTER,
    };

    private static final int COLUMN_REPLY_PATH_PRESENT = 0;
    private static final int COLUMN_SERVICE_CENTER     = 1;

    public SmsMessageSender(Context context, String[] dests, String msgText, long threadId) {
        mContext = context;
        mMessageText = msgText;
        if (dests != null) {
            mNumberOfDests = dests.length;
            mDests = new String[mNumberOfDests];
            System.arraycopy(dests, 0, mDests, 0, mNumberOfDests);
        } else {
            mNumberOfDests = 0;
            mDests = null;
        }
        mTimestamp = System.currentTimeMillis();
        mThreadId = threadId;
        mServiceCenter = getOutgoingServiceCenter(mThreadId);
    }

    // add for gemini
    public SmsMessageSender(Context context, String[] dests, String msgText, long threadId, int simId) {
        mContext = context;
        mMessageText = msgText;
        if (dests != null) {
            mNumberOfDests = dests.length;
            mDests = new String[mNumberOfDests];
            System.arraycopy(dests, 0, mDests, 0, mNumberOfDests);
        } else {
            mNumberOfDests = 0;
            mDests = null;
        }
        mTimestamp = System.currentTimeMillis();
        mThreadId = threadId;
        mServiceCenter = getOutgoingServiceCenter(mThreadId);
        mSimId = simId;
    }

    public boolean sendMessage(long token) throws MmsException {
        // In order to send the message one by one, instead of sending now, the message will split,
        // and be put into the queue along with each destinations 
        return queueMessage(token);
    }

    private boolean queueMessage(long token) throws MmsException {
        Xlog.v(MmsApp.TXN_TAG, "queueMessage()");
        if ((mMessageText == null) || (mNumberOfDests == 0)) {
            // Don't try to send an empty message.
            throw new MmsException("Null message body or dest.");
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean requestDeliveryReport = false;
        // add for gemini
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            requestDeliveryReport = prefs.getBoolean(Integer.toString(mSimId)+"_"+
                MessagingPreferenceActivity.SMS_DELIVERY_REPORT_MODE,
                DEFAULT_DELIVERY_REPORT_MODE);
        } else {
            requestDeliveryReport = prefs.getBoolean(
                MessagingPreferenceActivity.SMS_DELIVERY_REPORT_MODE,
                DEFAULT_DELIVERY_REPORT_MODE);
        }
        Xlog.d(MmsApp.TXN_TAG, "SMS DR request=" + requestDeliveryReport);

        for (int i = 0; i < mNumberOfDests; i++) {
            try {
                // add for gemini
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    Sms.addMessageToUri(mContext.getContentResolver(), 
                        Uri.parse("content://sms/queued"), mDests[i],
                        mMessageText, null, mTimestamp,
                        true /* read */,
                        requestDeliveryReport,
                        mThreadId,
                        mSimId);
                } else {
                    Sms.addMessageToUri(mContext.getContentResolver(), 
                        Uri.parse("content://sms/queued"), mDests[i],
                        mMessageText, null, mTimestamp,
                        true /* read */,
                        requestDeliveryReport,
                        mThreadId);
                }
            } catch (SQLiteException e) {
                SqliteWrapper.checkSQLiteException(mContext, e);
            }
        }
        // Notify the SmsReceiverService to send the message out
        // add for gemini
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            Intent sentIt = new Intent(SmsReceiverService.ACTION_SEND_MESSAGE,
                null,
                mContext,
                SmsReceiver.class);
            sentIt.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            mContext.sendBroadcast(sentIt);
        } else {
            mContext.sendBroadcast(new Intent(SmsReceiverService.ACTION_SEND_MESSAGE,
                null,
                mContext,
                SmsReceiver.class));
        }
        return false;
    }

    /**
     * Get the service center to use for a reply.
     *
     * The rule from TS 23.040 D.6 is that we send reply messages to
     * the service center of the message to which we're replying, but
     * only if we haven't already replied to that message and only if
     * <code>TP-Reply-Path</code> was set in that message.
     *
     * Therefore, return the service center from the most recent
     * message in the conversation, but only if it is a message from
     * the other party, and only if <code>TP-Reply-Path</code> is set.
     * Otherwise, return null.
     */
    private String getOutgoingServiceCenter(long threadId) {
        Cursor cursor = null;

        try {
            cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
                            Sms.CONTENT_URI, SERVICE_CENTER_PROJECTION,
                            "thread_id = " + threadId + " AND type = "+Sms.MESSAGE_TYPE_INBOX, null, "date DESC");

            if ((cursor == null) || !cursor.moveToFirst()) {
                return null;
            }

            boolean replyPathPresent = (1 == cursor.getInt(COLUMN_REPLY_PATH_PRESENT));
            return replyPathPresent ? cursor.getString(COLUMN_SERVICE_CENTER) : null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void log(String msg) {
        Log.d(LogTag.TAG, "[SmsMsgSender] " + msg);
    }


    // add for gemini
    // 2.2  no used
    public boolean sendMessageGemini(long token, int simId) throws MmsException {
        return false;
    }
  
}
