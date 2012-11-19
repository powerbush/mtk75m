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

import static com.android.mms.transaction.TransactionState.FAILED;
import static com.android.mms.transaction.TransactionState.INITIALIZED;
import static com.android.mms.transaction.TransactionState.SUCCESS;
import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF;
import static com.google.android.mms.pdu.PduHeaders.STATUS_DEFERRED;
import static com.google.android.mms.pdu.PduHeaders.STATUS_REJECTED;
import static com.google.android.mms.pdu.PduHeaders.STATUS_RETRIEVED;
import static com.google.android.mms.pdu.PduHeaders.STATUS_UNRECOGNIZED;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.ui.MessagingPreferenceActivity;
import com.android.mms.util.DownloadManager;
import com.android.mms.util.Recycler;
import com.google.android.mms.InvalidHeaderValueException;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.NotifyRespInd;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import android.database.sqlite.SqliteWrapper;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Inbox;
import android.provider.Telephony.SIMInfo;
import android.telephony.TelephonyManager;
import android.util.Config;
import android.util.Log;

import java.io.IOException;
// add for gemini
import com.mediatek.featureoption.FeatureOption;
import android.content.ContentResolver;
import android.content.ContentValues;
import com.android.mms.MmsApp;
import java.util.Date;
import android.os.SystemProperties;
import com.android.internal.telephony.TelephonyProperties;
import com.mediatek.xlog.Xlog;
import com.mediatek.xlog.SXlog;



/**
 * The NotificationTransaction is responsible for handling multimedia
 * message notifications (M-Notification.ind).  It:
 *
 * <ul>
 * <li>Composes the notification response (M-NotifyResp.ind).
 * <li>Sends the notification response to the MMSC server.
 * <li>Stores the notification indication.
 * <li>Notifies the TransactionService about succesful completion.
 * </ul>
 *
 * NOTE: This MMS client handles all notifications with a <b>deferred
 * retrieval</b> response.  The transaction service, upon succesful
 * completion of this transaction, will trigger a retrieve transaction
 * in case the client is in immediate retrieve mode.
 */
public class NotificationTransaction extends Transaction implements Runnable {
    private static final String TAG = "NotificationTransaction";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;

    private Uri mUri;
    private NotificationInd mNotificationInd;
    private String mContentLocation;

    public NotificationTransaction(
            Context context, int serviceId,
            TransactionSettings connectionSettings, String uriString) {
        super(context, serviceId, connectionSettings);

        mUri = Uri.parse(uriString);

        try {
            mNotificationInd = (NotificationInd)
                    PduPersister.getPduPersister(context).load(mUri);
        } catch (MmsException e) {
            Log.e(TAG, "Failed to load NotificationInd from: " + uriString, e);
            throw new IllegalArgumentException();
        }

        mId = new String(mNotificationInd.getTransactionId());
        mContentLocation = new String(mNotificationInd.getContentLocation());

        // Attach the transaction to the instance of RetryScheduler.
        attach(RetryScheduler.getInstance(context));
    }

    // add for gemini
    public NotificationTransaction(
            Context context, int serviceId, int simId,
            TransactionSettings connectionSettings, String uriString) {
        super(context, serviceId, connectionSettings);

        mUri = Uri.parse(uriString);

        try {
            mNotificationInd = (NotificationInd)
                    PduPersister.getPduPersister(context).load(mUri);
        } catch (MmsException e) {
            Log.e(TAG, "Failed to load NotificationInd from: " + uriString, e);
            throw new IllegalArgumentException();
        }

        mId = new String(mNotificationInd.getTransactionId());
        mSimId = simId;
        mContentLocation = new String(mNotificationInd.getContentLocation());

        // Attach the transaction to the instance of RetryScheduler.
        attach(RetryScheduler.getInstance(context));
    }

    /**
     * This constructor is only used for test purposes.
     */
    public NotificationTransaction(
            Context context, int serviceId,
            TransactionSettings connectionSettings, NotificationInd ind) {
        super(context, serviceId, connectionSettings);

        try {
            mUri = PduPersister.getPduPersister(context).persist(
                        ind, Inbox.CONTENT_URI);
        } catch (MmsException e) {
            Log.e(TAG, "Failed to save NotificationInd in constructor.", e);
            throw new IllegalArgumentException();
        }

        mNotificationInd = ind;
        mId = new String(ind.getTransactionId());
    }

    // add for gemini
    public NotificationTransaction(
            Context context, int serviceId, int simId,
            TransactionSettings connectionSettings, NotificationInd ind) {
        super(context, serviceId, connectionSettings);

        try {
            mUri = PduPersister.getPduPersister(context).persist(
                        ind, Inbox.CONTENT_URI);
        } catch (MmsException e) {
            Log.e(TAG, "Failed to save NotificationInd in constructor.", e);
            throw new IllegalArgumentException();
        }

        mNotificationInd = ind;
        mId = new String(ind.getTransactionId());
        mSimId = simId;
    }

    /*
     * (non-Javadoc)
     * @see com.google.android.mms.pdu.Transaction#process()
     */
    @Override
    public void process() {
        new Thread(this).start();
    }

    public void run() {
        Xlog.d(MmsApp.TXN_TAG, "NotificationTransaction: run()");
        DownloadManager downloadManager = DownloadManager.getInstance();
        //boolean autoDownload = downloadManager.isAuto();
        //boolean dataSuspended = (MmsApp.getApplication().getTelephonyManager().getDataState() ==
        //        TelephonyManager.DATA_SUSPENDED);
        boolean autoDownload = false;
        boolean dataSuspended = false;
        // add for gemini
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            autoDownload = downloadManager.isAuto(mSimId);
            int datastate = MmsApp.getApplication().getTelephonyManager().getDataStateGemini(SIMInfo.getSlotById(mContext, mSimId));
            dataSuspended = (datastate == TelephonyManager.DATA_SUSPENDED || datastate == TelephonyManager.DATA_DISCONNECTED);
        } else {
            autoDownload = downloadManager.isAuto();
            dataSuspended = (MmsApp.getApplication().getTelephonyManager().getDataState() == TelephonyManager.DATA_SUSPENDED);
        }
        
        try {
            if (LOCAL_LOGV) {
                Log.v(TAG, "Notification transaction launched: " + this);
            }

            // By default, we set status to STATUS_DEFERRED because we
            // should response MMSC with STATUS_DEFERRED when we cannot
            // download a MM immediately.
            int status = STATUS_DEFERRED;
            
            // Check expiry state
            Date CurrentDate = new Date(System.currentTimeMillis());
            Date ExpiryDate = new Date(mNotificationInd.getExpiry()*1000);
            Xlog.d(MmsApp.TXN_TAG, "expiry time=" + ExpiryDate.toLocaleString()
                    +"\t current=" + CurrentDate.toLocaleString());
            
            //MTK_OP01_PROTECT_START
            /*
            String optr = SystemProperties.get("ro.operator.optr");
            if (optr.equals("OP01")) {
                // Check Message size
                int msgSize = 0;
                Cursor cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
                                 mUri, new String[] {Mms.MESSAGE_SIZE}, null, null, null);
                if (cursor != null && cursor.getCount() == 1 && cursor.moveToFirst()) {
                    try{
                        msgSize = cursor.getInt(0);
                        Xlog.v(MmsApp.TXN_TAG, "msg Size = " + msgSize);
                    }finally{
                        cursor.close();
                    }
                }

                String netWorkType = null;
                int slotId = -1;
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    // convert sim id to slot id
                    slotId = SIMInfo.getSlotById(mContext, mSimId);
                    netWorkType = SystemProperties.get(slotId == 0 ? 
                            TelephonyProperties.PROPERTY_CS_NETWORK_TYPE : TelephonyProperties.PROPERTY_CS_NETWORK_TYPE_2);
                } else {
                    netWorkType = SystemProperties.get(TelephonyProperties.PROPERTY_CS_NETWORK_TYPE);
                }
                
                boolean bTDNetwork = Integer.parseInt(netWorkType) > 2 ? true : false;
                if ((!bTDNetwork && MmsConfig.getReceiveMmsLimitFor2G() < msgSize/1024)
                        || (bTDNetwork && MmsConfig.getReceiveMmsLimitForTD() < msgSize/1024)) {
                    Xlog.v(MmsApp.TXN_TAG, "Message size exceed limitation, rejected.");

                    status = STATUS_REJECTED;
                    sendNotifyRespInd(status);
                    return;
                }
            }
            */
            //MTK_OP01_PROTECT_END
            // Don't try to download when data is suspended, as it will fail, so defer download
            if (!autoDownload || dataSuspended) {
                Xlog.d(MmsApp.TXN_TAG, "Not autoDownload! autoDonwload=" 
                        + autoDownload + ", dataSuspended=" + dataSuspended);
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    downloadManager.markState(mUri, DownloadManager.STATE_UNSTARTED, mSimId);
                } else {
                    downloadManager.markState(mUri, DownloadManager.STATE_UNSTARTED);                
                }
                sendNotifyRespInd(status);
                return;
            }

            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                downloadManager.markState(mUri, DownloadManager.STATE_DOWNLOADING, mSimId);
            } else {
                downloadManager.markState(mUri, DownloadManager.STATE_DOWNLOADING);
            }

            if (LOCAL_LOGV) {
                Log.v(TAG, "Content-Location: " + mContentLocation);
            }

            byte[] retrieveConfData = null;
            // We should catch exceptions here to response MMSC
            // with STATUS_DEFERRED.
            try {
                Xlog.d(MmsApp.TXN_TAG, "NotificationTransaction: before getpdu");
                retrieveConfData = getPdu(mContentLocation);
                Xlog.d(MmsApp.TXN_TAG, "NotificationTransaction: after getpdu");
            } catch (IOException e) {
                mTransactionState.setState(FAILED);
            }

            if (retrieveConfData != null) {
                GenericPdu pdu = new PduParser(retrieveConfData).parse();
                if ((pdu == null) || (pdu.getMessageType() != MESSAGE_TYPE_RETRIEVE_CONF)) {
                    Log.e(TAG, "Invalid M-RETRIEVE.CONF PDU.");
                    mTransactionState.setState(FAILED);
                    status = STATUS_UNRECOGNIZED;
                } else {
                    // Save the received PDU (must be a M-RETRIEVE.CONF).
                    PduPersister p = PduPersister.getPduPersister(mContext);
                    Uri uri = p.persist(pdu, Inbox.CONTENT_URI);
                    Xlog.d(MmsApp.TXN_TAG, "PDU Saved, Uri=" + uri + "\nDelete Notify Ind, Uri=" + mUri);
                    // add for gemini
                    if (FeatureOption.MTK_GEMINI_SUPPORT) {
                        ContentResolver cr = mContext.getContentResolver();
                        ContentValues values = new ContentValues(1);
                        values.put(Mms.SIM_ID, mSimId);
                        SqliteWrapper.update(mContext, cr, uri, values, null, null);
                    }

                    // set message size
                    int messageSize = retrieveConfData.length;
                    ContentValues sizeValue = new ContentValues();
                    sizeValue.put(Mms.MESSAGE_SIZE, messageSize);
                    SqliteWrapper.update(mContext, mContext.getContentResolver(), uri, sizeValue, null, null);
                
                    // We have successfully downloaded the new MM. Delete the
                    // M-NotifyResp.ind from Inbox.
                    String notifId = mUri.getLastPathSegment();
                    String msgId = uri.getLastPathSegment();
                    if (!notifId.equals(msgId)) {
                        SqliteWrapper.delete(mContext, mContext.getContentResolver(),
                                             mUri, null, null);
                    }
                    // Notify observers with newly received MM.
                    mUri = uri;
                    status = STATUS_RETRIEVED;
                }
            }else{
                Xlog.e(MmsApp.TXN_TAG, "retrieveConfData is null");
                mTransactionState.setState(FAILED);
                status = STATUS_UNRECOGNIZED;                
            }

            // Check the status and update the result state of this Transaction.
            switch (status) {
                case STATUS_RETRIEVED:
                    mTransactionState.setState(SUCCESS);
                    break;
                case STATUS_DEFERRED:
                    // STATUS_DEFERRED, may be a failed immediate retrieval.
                    if (mTransactionState.getState() == INITIALIZED) {
                        mTransactionState.setState(SUCCESS);
                    }
                    break;
            }
            // if the status is STATUS_UNRECOGNIZED, this happened when we don't get mms pdu from server,
            //this may be a server problem or network problem.
            //our policy is will retry later, so we must response a deferred status not this one.
            //otherwise the server may delete this mms, and when we retry it, it's another mms created by server to 
            //inform us that the old mms is not exist yet.
            if (status == STATUS_UNRECOGNIZED) {
                status = STATUS_DEFERRED;
            }
            sendNotifyRespInd(status);

            // Make sure this thread isn't over the limits in message count.
            Recycler.getMmsRecycler().deleteOldMessagesInSameThreadAsMessage(mContext, mUri);
        } catch (Throwable t) {
            Log.e(TAG, Log.getStackTraceString(t));
            if (null != mUri){
                Recycler.getMmsRecycler().deleteOldMessagesInSameThreadAsMessage(mContext, mUri);
            }
        } finally {
            mTransactionState.setContentUri(mUri);
            if (!autoDownload /*|| dataSuspended*//*comment this case for 81452*/) {
                // Always mark the transaction successful for deferred
                // download since any error here doesn't make sense.
                mTransactionState.setState(SUCCESS);
            }
            if (mTransactionState.getState() != SUCCESS) {
                mTransactionState.setState(FAILED);
                Xlog.w(MmsApp.TXN_TAG, "NotificationTransaction failed.");
            }
            notifyObservers();
        }
    }

    private void sendNotifyRespInd(int status) throws MmsException, IOException {
        // Create the M-NotifyResp.ind
        Xlog.v(MmsApp.TXN_TAG, "NotificationTransaction: sendNotifyRespInd()");
        NotifyRespInd notifyRespInd = new NotifyRespInd(
                PduHeaders.CURRENT_MMS_VERSION,
                mNotificationInd.getTransactionId(),
                status);

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
                notifyRespInd.setReportAllowed(reportAllowed ? PduHeaders.VALUE_YES : PduHeaders.VALUE_NO);
            } catch(InvalidHeaderValueException ihve) {
                // do nothing here
                Xlog.e(MmsApp.TXN_TAG, "notifyRespInd.setReportAllowed Failed !!");
            }
        }
        //MTK_OP01_PROTECT_END

        // Pack M-NotifyResp.ind and send it
        if(MmsConfig.getNotifyWapMMSC()) {
            sendPdu(new PduComposer(mContext, notifyRespInd).make(), mContentLocation);
        } else {
            sendPdu(new PduComposer(mContext, notifyRespInd).make());
        }
    }

    @Override
    public int getType() {
        return NOTIFICATION_TRANSACTION;
    }

    // add for gemini
    public Uri getNotTrxnUri(){
        return mUri;
    }
}
