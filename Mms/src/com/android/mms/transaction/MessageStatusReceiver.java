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
 * Copyright (C) 2007 Esmertec AG.
 * Copyright (C) 2007 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Sms;
import android.telephony.SmsMessage;
import android.util.Log;

import android.database.sqlite.SqliteWrapper;
import com.android.mms.LogTag;
import com.android.mms.MmsApp;
import com.mediatek.xlog.Xlog;
import com.mediatek.xlog.SXlog;

public class MessageStatusReceiver extends BroadcastReceiver {
    public static final String MESSAGE_STATUS_RECEIVED_ACTION =
            "com.android.mms.transaction.MessageStatusReceiver.MESSAGE_STATUS_RECEIVED";
    private static final String[] ID_PROJECTION = new String[] { Sms._ID, Sms.STATUS };
    private static final String LOG_TAG = "MessageStatusReceiver";
    private static final Uri STATUS_URI =
            Uri.parse("content://sms/status");
    private Context mContext;
    private static final String MMS_READ_STATE_CHANGE = "MMS_READ_STATE_CHANGE";

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        if (MESSAGE_STATUS_RECEIVED_ACTION.equals(intent.getAction())) {
            SmsMessage message = updateMessageStatus(context, intent);
            // Called on the UI thread so don't block.
            if (message.getStatus() <= Sms.STATUS_PENDING){
                MessagingNotification.nonBlockingUpdateNewMessageIndicator(context,
                    true, message.isStatusReportMessage());
            }
        } else if (MMS_READ_STATE_CHANGE.equals(intent.getAction())) {
            // update notification when read state change 
            MessagingNotification.nonBlockingUpdateNewMessageIndicator(context, false, false);
       }
    }

    private SmsMessage updateMessageStatus(Context context, Intent intent) {
        boolean sendNextMsg = intent.getBooleanExtra(SmsReceiverService.EXTRA_MESSAGE_SENT_SEND_NEXT, false);
        byte[] pdu = (byte[]) intent.getExtra("pdu");
        SmsMessage message = SmsMessage.createFromPdu(pdu);
        Uri messageUri = intent.getData();
        // Create a "status/#" URL and use it to update the
        // message's status in the database.
        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                            messageUri, ID_PROJECTION, null, null, null);

        try {
            if (cursor.moveToFirst()) {
                int messageId = cursor.getInt(0);
                int oldStatus = cursor.getInt(1);
                Uri updateUri = ContentUris.withAppendedId(STATUS_URI, messageId);
                int status = message.getStatus();
                boolean isStatusReport = message.isStatusReportMessage();
                ContentValues contentValues = new ContentValues(1);

                if (Log.isLoggable(LogTag.TAG, Log.DEBUG)) {
                    log("updateMessageStatus: msgUrl=" + messageUri + ", status=" + status +
                            ", isStatusReport=" + isStatusReport);
                }
                Xlog.d(MmsApp.TXN_TAG, "updateMessageStatus: msgUrl=" + messageUri 
                    + ", status=" + status + ", isStatusReport=" + isStatusReport);

                if (oldStatus == Sms.STATUS_FAILED) {
                    //if the status is failed already, this means this is a long sms, and 
                    //at least one part of it is sent failed. so the status report of this long sms is failed overall.
                    //don't record a part's status.
                    // but this part success status is will toasted.
                    Xlog.d(MmsApp.TXN_TAG,"the original status is:"+oldStatus);
                } else {
                    Xlog.d(MmsApp.TXN_TAG,"the original status is:"+oldStatus+"the last part:"+sendNextMsg);
                    //update only if this is the last part of a sms.
                    if (sendNextMsg == true) {
                        contentValues.put(Sms.STATUS, status);
                        SqliteWrapper.update(context, context.getContentResolver(),
                                            updateUri, contentValues, null, null);
                    }
                }
                MessagingNotification.sMessageUri = updateUri;
            } else {
                error("Can't find message for status update: " + messageUri);
                Xlog.w(MmsApp.TXN_TAG, "Can't find message for status update: " + messageUri);

            }
        } finally {
            cursor.close();
        }
        return message;
    }

    private void error(String message) {
        Log.e(LOG_TAG, "[MessageStatusReceiver] " + message);
    }

    private void log(String message) {
        Log.d(LOG_TAG, "[MessageStatusReceiver] " + message);
    }
}
