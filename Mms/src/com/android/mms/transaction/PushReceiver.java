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

import static android.provider.Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION;
import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_DELIVERY_IND;
import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;
import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_READ_ORIG_IND;

import com.android.mms.MmsConfig;
import com.google.android.mms.ContentType;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.DeliveryInd;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.ReadOrigInd;
import android.database.sqlite.SqliteWrapper;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Inbox;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony.WapPush;
import android.util.Config;
import android.util.Log;
// add for gemini
import com.mediatek.featureoption.FeatureOption;
import com.android.internal.telephony.Phone;
import android.provider.Telephony.MmsSms.PendingMessages;
import com.android.mms.MmsApp;
import java.util.HashMap;
import com.mediatek.xlog.Xlog;
import com.mediatek.xlog.SXlog;


/**
 * Receives Intent.WAP_PUSH_RECEIVED_ACTION intents and starts the
 * TransactionService by passing the push-data to it.
 */
public class PushReceiver extends BroadcastReceiver {
    private static final String TAG = "PushReceiver";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;

    private PowerManager.WakeLock wl;
    private class ReceivePushTask extends AsyncTask<Intent,Void,Void> {
        private Context mContext;
        public ReceivePushTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(Intent... intents) {
            Intent intent = intents[0];
            Xlog.d(MmsApp.TXN_TAG, "do In Background, slotId=" + intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, 0));
            // Get raw PDU push-data from the message and parse it
            byte[] pushData = intent.getByteArrayExtra("data");
            PduParser parser = new PduParser(pushData);
            GenericPdu pdu = parser.parse();

            if (null == pdu) {
                Log.e(TAG, "Invalid PUSH data");
                return null;
            }

            PduPersister p = PduPersister.getPduPersister(mContext);
            ContentResolver cr = mContext.getContentResolver();
            int type = pdu.getMessageType();
            long threadId = -1;

            try {
                switch (type) {
                    case MESSAGE_TYPE_DELIVERY_IND:
                        Xlog.d(MmsApp.TXN_TAG, "type=MESSAGE_TYPE_DELIVERY_IND");
                    case MESSAGE_TYPE_READ_ORIG_IND: {
                        Xlog.d(MmsApp.TXN_TAG, "type=MESSAGE_TYPE_READ_ORIG_IND");
                        threadId = findThreadId(mContext, pdu, type);
                        if (threadId == -1) {
                            // The associated SendReq isn't found, therefore skip
                            // processing this PDU.
                            break;
                        }

                        Uri uri = p.persist(pdu, Inbox.CONTENT_URI);
                        // Update thread ID for ReadOrigInd & DeliveryInd.
                        ContentValues values = new ContentValues(1);
                        values.put(Mms.THREAD_ID, threadId);
                        SqliteWrapper.update(mContext, cr, uri, values, null, null);
                        break;
                    }
                    case MESSAGE_TYPE_NOTIFICATION_IND: {
                        Xlog.d(MmsApp.TXN_TAG, "type=MESSAGE_TYPE_NOTIFICATION_IND");
                        NotificationInd nInd = (NotificationInd) pdu;

                        if (MmsConfig.getTransIdEnabled()) {
                            byte [] contentLocation = nInd.getContentLocation();
                            if ('=' == contentLocation[contentLocation.length - 1]) {
                                byte [] transactionId = nInd.getTransactionId();
                                byte [] contentLocationWithId = new byte [contentLocation.length
                                                                          + transactionId.length];
                                System.arraycopy(contentLocation, 0, contentLocationWithId,
                                        0, contentLocation.length);
                                System.arraycopy(transactionId, 0, contentLocationWithId,
                                        contentLocation.length, transactionId.length);
                                nInd.setContentLocation(contentLocationWithId);
                            }
                        }

                        if (!isDuplicateNotification(mContext, nInd)) {
                            Uri uri = p.persist(pdu, Inbox.CONTENT_URI);
                            // add for gemini
                            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                // update pdu
                                ContentValues values = new ContentValues(2);
                                SIMInfo si = SIMInfo.getSIMInfoBySlot(mContext, intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, -1));
                                if (null == si) {
                                    Xlog.e(MmsApp.TXN_TAG, "PushReceiver:SIMInfo is null for slot " + intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, -1));
                                    break;
                                }
                                values.put(Mms.SIM_ID, si.mSimId);
                                values.put(WapPush.SERVICE_ADDR, intent.getStringExtra(WapPush.SERVICE_ADDR));
                                SqliteWrapper.update(mContext, cr, uri, values, null, null);
                                Xlog.d(MmsApp.TXN_TAG, "save notification slotId=" + intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, 0) 
                                        + "\tsimId=" + si.mSimId 
                                        + "\tsc=" + intent.getStringExtra(WapPush.SERVICE_ADDR)
                                        + "\taddr=" + intent.getStringExtra(WapPush.ADDR));

                                // update pending messages
                                long msgId = 0;
                                Cursor cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
                                                                uri, new String[] {Mms._ID}, null, null, null);
                                if (cursor != null && cursor.getCount() == 1 && cursor.moveToFirst()) {
                                    try{
                                        msgId = cursor.getLong(0);
                                        Xlog.d(MmsApp.TXN_TAG, "msg id = " + msgId);
                                    }finally{
                                        cursor.close();
                                    }
                                }
                                
                                Uri.Builder uriBuilder = PendingMessages.CONTENT_URI.buildUpon();
                                uriBuilder.appendQueryParameter("protocol", "mms");
                                uriBuilder.appendQueryParameter("message", String.valueOf(msgId));
                                Cursor pendingCs = SqliteWrapper.query(mContext, mContext.getContentResolver(),
                                        uriBuilder.build(), null, null, null, null);
                                if (pendingCs != null) {
                                    try {
                                        if ((pendingCs.getCount() == 1) && pendingCs.moveToFirst()) {
                                            ContentValues valuesforPending = new ContentValues();
                                            valuesforPending.put(PendingMessages.SIM_ID, si.mSimId);
                                            int columnIndex = pendingCs.getColumnIndexOrThrow(PendingMessages._ID);
                                            long id = pendingCs.getLong(columnIndex);
                                            SqliteWrapper.update(mContext, mContext.getContentResolver(),
                                                            PendingMessages.CONTENT_URI,
                                                            valuesforPending, PendingMessages._ID + "=" + id, null);
                                        }else{
                                            Xlog.w(MmsApp.TXN_TAG, "can not find message to set pending sim id, msgId="+msgId);
                                        }
                                    }finally {
                                        pendingCs.close();
                                    }
                                }
                            }
                        
                            // Start service to finish the notification transaction.
                            Intent svc = new Intent(mContext, TransactionService.class);
                            svc.putExtra(TransactionBundle.URI, uri.toString());
                            svc.putExtra(TransactionBundle.TRANSACTION_TYPE,
                                    Transaction.NOTIFICATION_TRANSACTION);
                            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                SIMInfo si = SIMInfo.getSIMInfoBySlot(mContext, intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, -1));
                                if (null == si) {
                                    Xlog.e(MmsApp.TXN_TAG, "PushReceiver: SIMInfo is null for slot " + intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, -1));
                                    break;
                                }
                                int simId = (int)si.mSimId;
                                svc.putExtra(Phone.GEMINI_SIM_ID_KEY, simId);
                                //svc.putExtra(Phone.GEMINI_SIM_ID_KEY, intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, 0));
                            }
                            mContext.startService(svc);
                        } else if (LOCAL_LOGV) {
                            Log.v(TAG, "Skip downloading duplicate message: "
                                    + new String(nInd.getContentLocation()));
                        }
                        break;
                    }
                    default:
                        Log.e(TAG, "Received unrecognized PDU.");
                }
            } catch (MmsException e) {
                Log.e(TAG, "Failed to save the data from PUSH: type=" + type, e);
            } catch (RuntimeException e) {
                Log.e(TAG, "Unexpected RuntimeException.", e);
            }  finally {
                raisePriority(mContext, false);
                Xlog.d(MmsApp.TXN_TAG, "Normal priority");
            }

            if (LOCAL_LOGV) {
                Log.v(TAG, "PUSH Intent processed.");
            }

            return null;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(WAP_PUSH_RECEIVED_ACTION)
                && ContentType.MMS_MESSAGE.equals(intent.getType())) {
            if (LOCAL_LOGV) {
                Log.v(TAG, "Received PUSH Intent: " + intent);
            }

            // raise priority
            Xlog.d(MmsApp.TXN_TAG, "raise priority");
            raisePriority(context, true);

            // Hold a wake lock for 5 seconds, enough to give any
            // services we start time to take their own wake locks.
            PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            if (wl !=null && wl.isHeld()) {
            	wl.release();
            }
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                            "MMS PushReceiver");
            wl.acquire(5000);
            new ReceivePushTask(context).execute(intent);
        }
    }

    private void raisePriority(Context context, boolean raise){
        final Intent it = new Intent();
        it.setAction("android.intent.action.BOOST_DOWNLOADING");
        it.putExtra("package_name", "com.android.mms");
        it.putExtra("enabled", raise);
        context.sendBroadcast(it);
    }

    private static long findThreadId(Context context, GenericPdu pdu, int type) {
        String messageId;

        if (type == MESSAGE_TYPE_DELIVERY_IND) {
            messageId = new String(((DeliveryInd) pdu).getMessageId());
        } else {
            messageId = new String(((ReadOrigInd) pdu).getMessageId());
        }

        StringBuilder sb = new StringBuilder('(');
        sb.append(Mms.MESSAGE_ID);
        sb.append('=');
        sb.append(DatabaseUtils.sqlEscapeString(messageId));
        sb.append(" AND ");
        sb.append(Mms.MESSAGE_TYPE);
        sb.append('=');
        sb.append(PduHeaders.MESSAGE_TYPE_SEND_REQ);
        // TODO ContentResolver.query() appends closing ')' to the selection argument
        // sb.append(')');

        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                            Mms.CONTENT_URI, new String[] { Mms.THREAD_ID },
                            sb.toString(), null, null);
        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    return cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        }

        return -1;
    }

    private static boolean isDuplicateNotification(
            Context context, NotificationInd nInd) {
        byte[] rawLocation = nInd.getContentLocation();
        if (rawLocation != null) {
            String location = new String(rawLocation);
            String selection = Mms.CONTENT_LOCATION + " = ?";
            String[] selectionArgs = new String[] { location };
            Cursor cursor = SqliteWrapper.query(
                    context, context.getContentResolver(),
                    Mms.CONTENT_URI, new String[] { Mms._ID },
                    selection, selectionArgs, null);
            if (cursor != null) {
                try {
                    if (cursor.getCount() > 0) {
                        // We already received the same notification before.
                        return true;
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return false;
    }
}
