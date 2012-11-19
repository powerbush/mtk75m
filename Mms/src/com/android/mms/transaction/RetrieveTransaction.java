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

import com.android.mms.MmsConfig;
import com.android.mms.ui.MessageUtils;
import com.android.mms.ui.MessagingPreferenceActivity;
import com.android.mms.util.DownloadManager;
import com.android.mms.util.Recycler;
import com.google.android.mms.InvalidHeaderValueException;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.AcknowledgeInd;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.NotifyRespInd;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.RetrieveConf;
import com.google.android.mms.pdu.EncodedStringValue;
import android.database.sqlite.SqliteWrapper;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Inbox;
import android.util.Config;
import android.util.Log;

import java.io.IOException;
// add for gemini
import com.mediatek.featureoption.FeatureOption;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.SystemProperties;
import com.android.mms.MmsApp;
import static com.google.android.mms.pdu.PduHeaders.STATUS_EXPIRED;
import com.mediatek.xlog.Xlog;
import com.mediatek.xlog.SXlog;


/**
 * The RetrieveTransaction is responsible for retrieving multimedia
 * messages (M-Retrieve.conf) from the MMSC server.  It:
 *
 * <ul>
 * <li>Sends a GET request to the MMSC server.
 * <li>Retrieves the binary M-Retrieve.conf data and parses it.
 * <li>Persists the retrieve multimedia message.
 * <li>Determines whether an acknowledgement is required.
 * <li>Creates appropriate M-Acknowledge.ind and sends it to MMSC server.
 * <li>Notifies the TransactionService about succesful completion.
 * </ul>
 */
public class RetrieveTransaction extends Transaction implements Runnable {
    private static final String TAG = "RetrieveTransaction";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;

    private final Uri mUri;
    private final String mContentLocation;
    private boolean mLocked;
    private boolean mExpiry = false;

    static final String[] PROJECTION = new String[] {
        Mms.CONTENT_LOCATION,
        Mms.LOCKED
    };

    // The indexes of the columns which must be consistent with above PROJECTION.
    static final int COLUMN_CONTENT_LOCATION      = 0;
    static final int COLUMN_LOCKED                = 1;

    public RetrieveTransaction(Context context, int serviceId,
            TransactionSettings connectionSettings, String uri)
            throws MmsException {
        super(context, serviceId, connectionSettings);

        if (uri.startsWith("content://")) {
            mUri = Uri.parse(uri); // The Uri of the M-Notification.ind
            mId = mContentLocation = getContentLocation(context, mUri);
            if (LOCAL_LOGV) {
                Log.v(TAG, "X-Mms-Content-Location: " + mContentLocation);
            }
        } else {
            throw new IllegalArgumentException(
                    "Initializing from X-Mms-Content-Location is abandoned!");
        }

        // Attach the transaction to the instance of RetryScheduler.
        attach(RetryScheduler.getInstance(context));
    }

    // add for gemini
    public RetrieveTransaction(Context context, int serviceId, int simId,
            TransactionSettings connectionSettings, String uri)
            throws MmsException {
        super(context, serviceId, connectionSettings);
        mSimId = simId;

        if (uri.startsWith("content://")) {
            mUri = Uri.parse(uri); // The Uri of the M-Notification.ind
            mId = mContentLocation = getContentLocation(context, mUri);
            if (LOCAL_LOGV) {
                Log.v(TAG, "X-Mms-Content-Location: " + mContentLocation);
            }
        } else {
            throw new IllegalArgumentException(
                    "Initializing from X-Mms-Content-Location is abandoned!");
        }

        // Attach the transaction to the instance of RetryScheduler.
        attach(RetryScheduler.getInstance(context));
    }

    private String getContentLocation(Context context, Uri uri)  throws MmsException {
        Xlog.v(MmsApp.TXN_TAG, "RetrieveTransaction: getContentLocation()");
        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                            uri, PROJECTION, null, null, null);
        mLocked = false;

        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    // Get the locked flag from the M-Notification.ind so it can be transferred
                    // to the real message after the download.
                    mLocked = cursor.getInt(COLUMN_LOCKED) == 1;
                    return cursor.getString(COLUMN_CONTENT_LOCATION);
                }
            } finally {
                cursor.close();
            }
        }

        throw new MmsException("Cannot get X-Mms-Content-Location from: " + uri);
    }

    /*
     * (non-Javadoc)
     * @see com.android.mms.transaction.Transaction#process()
     */
    @Override
    public void process() {
        new Thread(this).start();
    }

    public void run() {
        try {
            // Check expiery , this operation must be done before markState function,
            NotificationInd nInd = (NotificationInd) PduPersister.getPduPersister(mContext).load(mUri);
            if (nInd.getExpiry() < System.currentTimeMillis()/1000L) {
                mExpiry = true;
                Xlog.d(MmsApp.TXN_TAG, "The message is expired!");
                try {
                    sendNotifyRespInd(STATUS_EXPIRED);
                } catch (Throwable t) { 
                    // we should run following func to delete expired notification, no matter what happen. so, catch throwable
                    Xlog.e(MmsApp.TXN_TAG, Log.getStackTraceString(t));
                }
            }

            
            // Change the downloading state of the M-Notification.ind.
            DownloadManager.getInstance().markState(
                    mUri, DownloadManager.STATE_DOWNLOADING);

            // if this notification expiry, we should not download message from network
            if (mExpiry) {
                mTransactionState.setState(TransactionState.SUCCESS);
                mTransactionState.setContentUri(mUri);
                return;
            }

            // Send GET request to MMSC and retrieve the response data.
            byte[] resp = getPdu(mContentLocation);

            // Parse M-Retrieve.conf
            RetrieveConf retrieveConf = (RetrieveConf) new PduParser(resp).parse();
            if (null == retrieveConf) {
                Xlog.e(MmsApp.TXN_TAG, "RetrieveTransaction: run(): Invalid M-Retrieve.conf PDU!!!");
                throw new MmsException("Invalid M-Retrieve.conf PDU.");
            }

            Uri msgUri = null;
            if (isDuplicateMessage(mContext, retrieveConf)) {
                Xlog.w(MmsApp.TXN_TAG, "RetrieveTransaction: run, DuplicateMessage");
                // Mark this transaction as failed to prevent duplicate
                // notification to user.
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setContentUri(mUri);
            } else {
                Xlog.d(MmsApp.TXN_TAG, "RetrieveTransaction: run, Store M-Retrieve.conf into Inbox");
                // Store M-Retrieve.conf into Inbox
                PduPersister persister = PduPersister.getPduPersister(mContext);
                msgUri = persister.persist(retrieveConf, Inbox.CONTENT_URI);

                // add for gemini
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    ContentResolver cr = mContext.getContentResolver();
                    ContentValues values = new ContentValues(1);
                    values.put(Mms.SIM_ID, mSimId);
                    SqliteWrapper.update(mContext, cr, msgUri, values, null, null);
                    Xlog.d(MmsApp.TXN_TAG, "save retrieved mms, simId=" + mSimId);
                }

                // set message size
                int messageSize = resp.length;
                ContentValues sizeValue = new ContentValues();
                sizeValue.put(Mms.MESSAGE_SIZE, messageSize);
                SqliteWrapper.update(mContext, mContext.getContentResolver(), msgUri, sizeValue, null, null);

                // The M-Retrieve.conf has been successfully downloaded.
                mTransactionState.setState(TransactionState.SUCCESS);
                mTransactionState.setContentUri(msgUri);
                // Remember the location the message was downloaded from.
                // Since it's not critical, it won't fail the transaction.
                // Copy over the locked flag from the M-Notification.ind in case
                // the user locked the message before activating the download.
                updateContentLocation(mContext, msgUri, mContentLocation, mLocked);
            }

            if (msgUri != null) {
                // Delete the corresponding M-Notification.ind.
                String notifId = mUri.getLastPathSegment();
                String msgId = msgUri.getLastPathSegment();
                if (!notifId.equals(msgId)) {
                    SqliteWrapper.delete(mContext, mContext.getContentResolver(),
                                         mUri, null, null);
                }
                // Have to delete messages over limit *after* the delete above. Otherwise,
                // it would be counted as part of the total.
                Recycler.getMmsRecycler().deleteOldMessagesInSameThreadAsMessage(mContext, msgUri);
            } else {
                // is Duplicate Message, delete notification
                SqliteWrapper.delete(mContext, mContext.getContentResolver(),
                                         mUri, null, null);
            }

            // Send ACK to the Proxy-Relay to indicate we have fetched the
            // MM successfully.
            // Don't mark the transaction as failed if we failed to send it.
            sendAcknowledgeInd(retrieveConf);
        } catch (Throwable t) {
            Log.e(TAG, Log.getStackTraceString(t));
        } finally {
            if (mTransactionState.getState() != TransactionState.SUCCESS) {
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setContentUri(mUri);
                Log.e(TAG, "Retrieval failed.");
            }
            notifyObservers();
        }
    }

    private boolean isDuplicateMessage(Context context, RetrieveConf rc) {
        byte[] rawMessageId = rc.getMessageId();
        if (rawMessageId != null) {
            String messageId = new String(rawMessageId);
            String selection = "(" + Mms.MESSAGE_ID + " = ? AND "
                                   + Mms.MESSAGE_TYPE + " = ?)";
            //each card has it's own mms.
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                selection += " AND " + Mms.SIM_ID + " = " + mSimId;
            }
            String[] selectionArgs = new String[] { messageId,
                    String.valueOf(PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF) };
            Cursor cursor = SqliteWrapper.query(
                    context, context.getContentResolver(),
                    Mms.CONTENT_URI, new String[] { Mms._ID },
                    selection, selectionArgs, null);
            if (cursor != null) {
                try {
                    if (cursor.getCount() > 0) {
                        // We already received the same message before.
                        return true;
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return false;
    }

    private void sendAcknowledgeInd(RetrieveConf rc) throws MmsException, IOException {
        Xlog.v(MmsApp.TXN_TAG, "RetrieveTransaction: sendAcknowledgeInd()");
        // Send M-Acknowledge.ind to MMSC if required.
        // If the Transaction-ID isn't set in the M-Retrieve.conf, it means
        // the MMS proxy-relay doesn't require an ACK.
        byte[] tranId = rc.getTransactionId();
        if (tranId != null) {
            // Create M-Acknowledge.ind
            AcknowledgeInd acknowledgeInd = new AcknowledgeInd(
                    PduHeaders.CURRENT_MMS_VERSION, tranId);

            // insert the 'from' address per spec
            String lineNumber = null;
            // add for gemini
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                lineNumber = MessageUtils.getLocalNumberGemini(mSimId);
            } else {
                lineNumber = MessageUtils.getLocalNumber();
            }
            acknowledgeInd.setFrom(new EncodedStringValue(lineNumber));

            //MTK_OP01_PROTECT_START
            String optr = SystemProperties.get("ro.operator.optr");
            if (optr.equals("OP01")) {
                // X-Mms-Report-Allowed Optional
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                boolean reportAllowed = true;
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    reportAllowed = prefs.getBoolean(Integer.toString(mSimId)+ "_" + 
                            MessagingPreferenceActivity.MMS_ENABLE_TO_SEND_DELIVERY_REPORT,
                            true);
                } else {
                    reportAllowed = prefs.getBoolean(MessagingPreferenceActivity.MMS_ENABLE_TO_SEND_DELIVERY_REPORT, true);
                }

                Xlog.d(MmsApp.TXN_TAG, "reportAllowed: " + reportAllowed);
            
                try {
                    acknowledgeInd.setReportAllowed(reportAllowed ? PduHeaders.VALUE_YES : PduHeaders.VALUE_NO);
                } catch(InvalidHeaderValueException ihve) {
                    // do nothing here
                    Xlog.e(MmsApp.TXN_TAG, "acknowledgeInd.setReportAllowed Failed !!");
                }
            }
            //MTK_OP01_PROTECT_END

            // Pack M-Acknowledge.ind and send it
            if(MmsConfig.getNotifyWapMMSC()) {
                sendPdu(new PduComposer(mContext, acknowledgeInd).make(), mContentLocation);
            } else {
                sendPdu(new PduComposer(mContext, acknowledgeInd).make());
            }
        }
    }

    /**
    * send expired in MM1_notification.Res if the notification expired
    */
    private void sendNotifyRespInd(int status) throws MmsException, IOException {
        // Create the M-NotifyResp.ind
        Xlog.d(MmsApp.TXN_TAG, "RetrieveTransaction: sendNotifyRespInd for expired.");
        NotificationInd notificationInd = (NotificationInd) PduPersister.getPduPersister(mContext).load(mUri);
        NotifyRespInd notifyRespInd = new NotifyRespInd(
                PduHeaders.CURRENT_MMS_VERSION,
                notificationInd.getTransactionId(),
                status);

        // Pack M-NotifyResp.ind and send it
        if(MmsConfig.getNotifyWapMMSC()) {
            sendPdu(new PduComposer(mContext, notifyRespInd).make(), mContentLocation);
        } else {
            sendPdu(new PduComposer(mContext, notifyRespInd).make());
        }
    }

    private static void updateContentLocation(Context context, Uri uri,
                                              String contentLocation,
                                              boolean locked) {
        Xlog.d(MmsApp.TXN_TAG, "RetrieveTransaction: updateContentLocation()");

        ContentValues values = new ContentValues(2);
        values.put(Mms.CONTENT_LOCATION, contentLocation);
        values.put(Mms.LOCKED, locked);     // preserve the state of the M-Notification.ind lock.
        SqliteWrapper.update(context, context.getContentResolver(),
                             uri, values, null, null);
    }

    @Override
    public int getType() {
        return RETRIEVE_TRANSACTION;
    }

    // add for gemini
    public Uri getRtrTrxnUri() {
        return mUri;
    }
}
