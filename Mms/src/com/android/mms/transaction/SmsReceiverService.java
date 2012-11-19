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

import static android.content.Intent.ACTION_BOOT_COMPLETED;
import static android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION;

import com.android.mms.data.Contact;
import com.android.mms.ui.ClassZeroActivity;
import com.android.mms.ui.MessagingPreferenceActivity;
import com.android.mms.util.Recycler;
import com.android.mms.util.SendingProgressTokenManager;
import com.google.android.mms.MmsException;
import android.database.sqlite.SqliteWrapper;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony.Sms.Inbox;
import android.provider.Telephony.Sms.Intents;
import android.provider.Telephony.Sms.Outbox;
import android.provider.Telephony;
import android.telephony.gemini.GeminiSmsManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.TelephonyIntents;
import com.android.mms.R;
import com.android.mms.LogTag;
import com.android.mms.util.ThreadCountManager;
import com.mediatek.featureoption.FeatureOption;
import android.provider.Telephony.Mms;
import com.android.internal.telephony.Phone;
import com.android.mms.MmsApp;
import com.mediatek.xlog.Xlog;
import com.mediatek.xlog.SXlog;
import android.os.SystemProperties;

/**
 * This service essentially plays the role of a "worker thread", allowing us to store
 * incoming messages to the database, update notifications, etc. without blocking the
 * main thread that SmsReceiver runs on.
 */
public class SmsReceiverService extends Service {
    private static final String TAG = "SmsReceiverService";

    private ServiceHandler mServiceHandler;
    private Looper mServiceLooper;
    private boolean mSending;

    public static final String MESSAGE_SENT_ACTION =
        "com.android.mms.transaction.MESSAGE_SENT";
    public static boolean sSmsSent = true;

    // Indicates next message can be picked up and sent out.
    public static final String EXTRA_MESSAGE_SENT_SEND_NEXT ="SendNextMsg";

    // Indicates this is a concatenation sms
    public static final String EXTRA_MESSAGE_CONCATENATION = "ConcatenationMsg";

    public static final String ACTION_SEND_MESSAGE =
        "com.android.mms.transaction.SEND_MESSAGE";

    // This must match the column IDs below.
    private static final String[] SEND_PROJECTION = new String[] {
        Sms._ID,        //0
        Sms.THREAD_ID,  //1
        Sms.ADDRESS,    //2
        Sms.BODY,       //3
        Sms.STATUS,     //4

    };
    private static final  Uri UPDATE_THREADS_URI = Uri.parse("content://mms-sms/conversations/status");
    private static final int FDN_CHECK_FAIL = 2;
    private static final int RADIO_NOT_AVILABLE = 1;
    public Handler mToastHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RADIO_NOT_AVILABLE:
                    Toast.makeText(SmsReceiverService.this,
                        getString(R.string.message_queued), Toast.LENGTH_SHORT)
                       .show();
                    break;
                case FDN_CHECK_FAIL:
                    Toast.makeText(SmsReceiverService.this, R.string.fdn_enabled,
                         Toast.LENGTH_LONG).show();
                    break;
                default :
                    break;
            }
        }
    };

    // This must match SEND_PROJECTION.
    private static final int SEND_COLUMN_ID         = 0;
    private static final int SEND_COLUMN_THREAD_ID  = 1;
    private static final int SEND_COLUMN_ADDRESS    = 2;
    private static final int SEND_COLUMN_BODY       = 3;
    private static final int SEND_COLUMN_STATUS     = 4;

    private int mResultCode;

    @Override
    public void onCreate() {
        // Temporarily removed for this duplicate message track down.
//        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
//            Log.v(TAG, "onCreate");
//        }

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.
        HandlerThread thread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Temporarily removed for this duplicate message track down.
//        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
//            Log.v(TAG, "onStart: #" + startId + ": " + intent.getExtras());
//        }
        if (MESSAGE_SENT_ACTION.equals(intent.getAction())) {
            mResultCode = intent != null ? intent.getIntExtra("result", 0) : 0;
            Xlog.d(MmsApp.TXN_TAG, "Message Sent Result Code = " + mResultCode);
        }

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        // Temporarily removed for this duplicate message track down.
//        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
//            Log.v(TAG, "onDestroy");
//        }
        mServiceLooper.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        /**
         * Handle incoming transaction requests.
         * The incoming requests are initiated by the MMSC Server or by the MMS Client itself.
         */
        @Override
        public void handleMessage(Message msg) {
            Xlog.d(MmsApp.TXN_TAG, "Sms handleMessage :" + msg);
            int serviceId = msg.arg1;
            Intent intent = (Intent)msg.obj;
            if (intent != null) {
                String action = intent.getAction();

                int error = intent.getIntExtra("errorCode", 0);

                if (MESSAGE_SENT_ACTION.equals(intent.getAction())) {
                    handleSmsSent(intent, error);
                } else if (SMS_RECEIVED_ACTION.equals(action)) {
                    handleSmsReceived(intent, error);
                } else if (ACTION_BOOT_COMPLETED.equals(action)) {
                    handleBootCompleted();
                } else if (TelephonyIntents.ACTION_SERVICE_STATE_CHANGED.equals(action)) {
                    handleServiceStateChanged(intent);
                } else if (ACTION_SEND_MESSAGE.endsWith(action)) {
                    handleSendMessage(intent);
                }
            }
            
            sSmsSent = true;
            // NOTE: We MUST not call stopSelf() directly, since we need to
            // make sure the wake lock acquired by AlertReceiver is released.
            SmsReceiver.finishStartingService(SmsReceiverService.this, serviceId);
        }
    }

    private void handleServiceStateChanged(Intent intent) {
        Xlog.v(MmsApp.TXN_TAG, "Sms handleServiceStateChanged");
        // If service just returned, start sending out the queued messages
        ServiceState serviceState = ServiceState.newFromBundle(intent.getExtras());
        if (serviceState.getState() == ServiceState.STATE_IN_SERVICE) {
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                // convert slot id to sim id
                int slotId = intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, -1);
                SIMInfo si = SIMInfo.getSIMInfoBySlot(this, slotId);
                if (null == si) {
                    Xlog.e(MmsApp.TXN_TAG, "handleServiceStateChanged:SIMInfo is null for slot " + slotId);
                    return;
                }
                sendFirstQueuedMessageGemini((int)si.mSimId);
            } else {
                sendFirstQueuedMessage();
            }
        }
    }

    private void handleSendMessage(Intent intent) {
        Xlog.d(MmsApp.TXN_TAG, "handleSendMessage() simId=" + intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, -1));
        if (!mSending) {
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                sendFirstQueuedMessageGemini(intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, -1));
            } else {
                sendFirstQueuedMessage();
            }
        }
    }

    public synchronized void sendFirstQueuedMessage() {
        Xlog.d(MmsApp.TXN_TAG, "sendFirstQueuedMessage()");
        boolean success = true;
        // get all the queued messages from the database
        final Uri uri = Uri.parse("content://sms/queued");
        ContentResolver resolver = getContentResolver();
        Cursor c = SqliteWrapper.query(this, resolver, uri,
                        SEND_PROJECTION, null, null, "date ASC");   // date ASC so we send out in
                                                                    // same order the user tried
                                                                    // to send messages.
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    String msgText = c.getString(SEND_COLUMN_BODY);
                    String address = c.getString(SEND_COLUMN_ADDRESS);
                    int threadId = c.getInt(SEND_COLUMN_THREAD_ID);
                    int status = c.getInt(SEND_COLUMN_STATUS);

                    int msgId = c.getInt(SEND_COLUMN_ID);
                    Uri msgUri = ContentUris.withAppendedId(Sms.CONTENT_URI, msgId);

                    SmsMessageSender sender = new SmsSingleRecipientSender(this,
                            address, msgText, threadId, status == Sms.STATUS_PENDING,
                            msgUri);

                    if (LogTag.VERBOSE || Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "sendFirstQueuedMessage " + msgUri +
                                ", address: " + address +
                                ", threadId: " + threadId +
                                ", body: " + msgText);
                    }

                    try {
                        sender.sendMessage(SendingProgressTokenManager.NO_TOKEN);;
                        mSending = true;
                    } catch (MmsException e) {
                        Log.e(TAG, "sendFirstQueuedMessage: failed to send message " + msgUri
                                + ", caught ", e);
                        success = false;
                    }
                }
            } finally {
                if (!success) {
                    int msgId = c.getInt(SEND_COLUMN_ID);
                    Uri msgUri = ContentUris.withAppendedId(Sms.CONTENT_URI, msgId);
                    messageFailedToSend(msgUri, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
                    sendFirstQueuedMessage();
                }
                c.close();
            }
        }
        if (success) {
            // We successfully sent all the messages in the queue. We don't need to
            // be notified of any service changes any longer.
            unRegisterForServiceStateChanges();
        }
    }

    private void updateSizeForSentMessage(Intent intent){
        Uri uri = intent.getData();
        int messageSize = intent.getIntExtra("pdu_size", -1);
        Xlog.d(MmsApp.TXN_TAG, "update size for sent sms, size=" + messageSize);
        Cursor cursor = SqliteWrapper.query(this, getContentResolver(),
                                            uri, null, null, null, null);
        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    ContentValues sizeValue = new ContentValues();
                    sizeValue.put(Mms.MESSAGE_SIZE, messageSize);
                    SqliteWrapper.update(this, getContentResolver(),
                                        uri, sizeValue, null, null);
                }
            } finally {
                cursor.close();
            }
        }
    }
    
    private void handleSmsSent(Intent intent, int error) {
        Xlog.d(MmsApp.TXN_TAG, "handleSmsSent(), errorcode=" + error);
        Uri uri = intent.getData();
        mSending = false;
        boolean sendNextMsg = intent.getBooleanExtra(EXTRA_MESSAGE_SENT_SEND_NEXT, false);

        boolean beConcatenationMsg = intent.getBooleanExtra(EXTRA_MESSAGE_CONCATENATION, false);

        // set message size
        updateSizeForSentMessage(intent);

        if (mResultCode == Activity.RESULT_OK) {
            Xlog.d(MmsApp.TXN_TAG, "handleSmsSent(), result is RESULT_OK");
            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "handleSmsSent sending uri: " + uri);
            }
            if (sendNextMsg) {//this is the last part of a sms.a long sms's part is sent ordered.
                Cursor cursor = SqliteWrapper.query(this, getContentResolver(),
                                                    uri, new String[] {Sms.TYPE}, null, null, null);
                if (cursor != null) {
                    try {
                        if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                            int smsType = 0;
                            smsType = cursor.getInt(0);
                            //if smsType is failed, that means at least one part of this long sms is sent failed.
                            // then this long sms is sent failed.
                            //so we shouldn't move it to other boxes.just keep it in failed box.
                            if (smsType != Sms.MESSAGE_TYPE_FAILED) {
                                //move sms from out box to sent box
                                if (!Sms.moveMessageToFolder(this, uri, Sms.MESSAGE_TYPE_SENT, error)) {
                                    Log.e(TAG, "handleSmsSent: failed to move message " + uri + " to sent folder");
                                }
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }

            //if (!Sms.moveMessageToFolder(this, uri, Sms.MESSAGE_TYPE_SENT, error)) {
            //    Log.e(TAG, "handleSmsSent: failed to move message " + uri + " to sent folder");
            //}
            if (sendNextMsg) {
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    // convert slot id to sim id
                    int slotId = intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, -1);
                    SIMInfo si = SIMInfo.getSIMInfoBySlot(this, slotId);
                    if (null == si) {
                        Xlog.e(MmsApp.TXN_TAG, "SmsReceiver:SIMInfo is null for slot " + slotId);
                        return;
                    }
                    sendFirstQueuedMessageGemini((int)si.mSimId);
                }else{
                    sendFirstQueuedMessage();
                }
            }

            // Update the notification for failed messages since they may be deleted.
            MessagingNotification.updateSendFailedNotification(this);
        } else if ((mResultCode == SmsManager.RESULT_ERROR_RADIO_OFF) ||
                (mResultCode == SmsManager.RESULT_ERROR_NO_SERVICE)) {
            if (mResultCode == SmsManager.RESULT_ERROR_RADIO_OFF) {
                Xlog.d(MmsApp.TXN_TAG, "handleSmsSent(), result is RESULT_ERROR_RADIO_OFF");
            } else {
                Xlog.d(MmsApp.TXN_TAG, "handleSmsSent(), result is RESULT_ERROR_NO_SERVICE");
            }
            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "handleSmsSent: no service, queuing message w/ uri: " + uri);
            }
            // We got an error with no service or no radio. Register for state changes so
            // when the status of the connection/radio changes, we can try to send the
            // queued up messages.
            registerForServiceStateChanges();
            // We couldn't send the message, put in the queue to retry later.
            Sms.moveMessageToFolder(this, uri, Sms.MESSAGE_TYPE_QUEUED, error);
            mToastHandler.post(new Runnable() {
                public void run() {
                    Toast.makeText(SmsReceiverService.this, getString(R.string.message_queued),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            if (mResultCode == SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE) {
                mToastHandler.post(new Runnable() {
                    public void run() {
                        Toast.makeText(SmsReceiverService.this, getString(R.string.fdn_check_failure),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
            messageFailedToSend(uri, error);
            if (sendNextMsg) {
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    // convert slot id to sim id
                    int slotId = intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, -1);
                    SIMInfo si = SIMInfo.getSIMInfoBySlot(this, slotId);
                    if (null == si) {
                        Xlog.e(MmsApp.TXN_TAG, "SmsReceiver:SIMInfo is null for slot " + slotId);
                        return;
                    }
                    sendFirstQueuedMessageGemini((int)si.mSimId);
                } else {
                    sendFirstQueuedMessage();
                }
            }
        }
    }

    private void messageFailedToSend(Uri uri, int error) {
        Xlog.d(MmsApp.TXN_TAG, "messageFailedToSend(),uri=" + uri + "\terror=" + error);
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "messageFailedToSend msg failed uri: " + uri);
        }
        Sms.moveMessageToFolder(this, uri, Sms.MESSAGE_TYPE_FAILED, error);
        
        // add by gx, update sms status when failed. this Sms.STATUS is used for delivery report.
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(Sms.STATUS, Sms.STATUS_FAILED);
        SqliteWrapper.update(this, this.getContentResolver(), uri, contentValues, null, null);
        
        MessagingNotification.notifySendFailed(getApplicationContext(), true);
    }

    private void handleSmsReceived(Intent intent, int error) {
        SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
        //Uri messageUri = insertMessage(this, msgs, error);
        Uri messageUri = insertMessage(this, intent, error);

        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            SmsMessage sms = msgs[0];
            Log.v(TAG, "handleSmsReceived" + (sms.isReplace() ? "(replace)" : "") +
                    " messageUri: " + messageUri +
                    ", address: " + sms.getOriginatingAddress() +
                    ", body: " + sms.getMessageBody());
        }
        SmsMessage tmpsms = msgs[0];
        Xlog.d(MmsApp.TXN_TAG, "handleSmsReceived" + (tmpsms.isReplace() ? "(replace)" : "") 
            + " messageUri: " + messageUri 
            + ", address: " + tmpsms.getOriginatingAddress() 
            + ", body: " + tmpsms.getMessageBody());

        if (messageUri != null) {
            // Called off of the UI thread so ok to block.
            MessagingNotification.blockingUpdateNewMessageIndicator(this, true, false);
        } else {
            SmsMessage sms = msgs[0];
            SmsMessage msg = SmsMessage.createFromPdu(sms.getPdu());
            CharSequence messageChars = msg.getMessageBody();
            String message = messageChars.toString();
            if (!TextUtils.isEmpty(message)) {
                MessagingNotification.notifyClassZeroMessage(this, msgs[0]
                        .getOriginatingAddress());
            }
        }
    }

    private void handleBootCompleted() {
        moveOutboxMessagesToQueuedBox();
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            ;
        } else {
            sendFirstQueuedMessage();
        }

        // Called off of the UI thread so ok to block.
        MessagingNotification.blockingUpdateNewMessageIndicator(this, true, false);
    }

    private void moveOutboxMessagesToQueuedBox() {
        ContentValues values = new ContentValues(1);

        values.put(Sms.TYPE, Sms.MESSAGE_TYPE_QUEUED);
        try {
            SqliteWrapper.update(
                    getApplicationContext(), getContentResolver(), Outbox.CONTENT_URI,
                    values, "type = " + Sms.MESSAGE_TYPE_OUTBOX, null);
        } catch (SQLiteDiskIOException e) {
            // Ignore
            Xlog.e(MmsApp.TXN_TAG, "SQLiteDiskIOException caught while move outbox message to queue box", e);
        }
    }

    public static final String CLASS_ZERO_BODY_KEY = "CLASS_ZERO_BODY";

    // This must match the column IDs below.
    private final static String[] REPLACE_PROJECTION = new String[] {
        Sms._ID,
        Sms.ADDRESS,
        Sms.PROTOCOL
    };

    // add for gemini
    private final static String[] REPLACE_PROJECTION_GEMINI = new String[] {
        Sms._ID,
        Sms.ADDRESS,
        Sms.PROTOCOL,
        Sms.SIM_ID
    };

    // This must match REPLACE_PROJECTION.
    private static final int REPLACE_COLUMN_ID = 0;

    /**
     * If the message is a class-zero message, display it immediately
     * and return null.  Otherwise, store it using the
     * <code>ContentResolver</code> and return the
     * <code>Uri</code> of the thread containing this message
     * so that we can use it for notification.
     */
     private Uri insertMessage(Context context, Intent intent, int error) {
        // Build the helper classes to parse the messages.
        SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
        SmsMessage sms = msgs[0];

        // convert slot id to sim id
        int slotId = intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, -1);
        SIMInfo si = SIMInfo.getSIMInfoBySlot(context, slotId);
        if (null == si) {
            Xlog.e(MmsApp.TXN_TAG, "insertMessage:SIMInfo is null for slot " + slotId);
            return null;
        }
        int simId = (int)si.mSimId;
        intent.putExtra(Phone.GEMINI_SIM_ID_KEY, simId);
        Xlog.d(MmsApp.TXN_TAG, "Sms insert message,\tslot id=" + slotId + "\tsim id=" + simId);

        if (sms.getMessageClass() == SmsMessage.MessageClass.CLASS_0) {
            SmsMessage msg = SmsMessage.createFromPdu(sms.getPdu());
            CharSequence messageChars = msg.getMessageBody();
            String message = messageChars.toString();
            
            //if (! TextUtils.isEmpty(message)) {
                displayClassZeroMessage(context, intent);
            //}
            return null;
        } else if (sms.isReplace()) {
            return replaceMessage(context, msgs, error);
        } else {
            return storeMessage(context, msgs, error);
        }
    }
/*    
    private Uri insertMessage(Context context, SmsMessage[] msgs, int error) {
        // Build the helper classes to parse the messages.
        Xlog.d(MmsApp.TXN_TAG, "Sms insertMessage");
        SmsMessage sms = msgs[0];

        if (sms.getMessageClass() == SmsMessage.MessageClass.CLASS_0) {
            SmsMessage msg = SmsMessage.createFromPdu(sms.getPdu());
            CharSequence messageChars = msg.getMessageBody();
            String message = messageChars.toString();
            
            if (! TextUtils.isEmpty(message)) {
                displayClassZeroMessage(context, sms);
            }
            return null;
        } else if (sms.isReplace()) {
            return replaceMessage(context, msgs, error);
        } else {
            return storeMessage(context, msgs, error);
        }
    }
*/
    /**
     * This method is used if this is a "replace short message" SMS.
     * We find any existing message that matches the incoming
     * message's originating address and protocol identifier.  If
     * there is one, we replace its fields with those of the new
     * message.  Otherwise, we store the new message as usual.
     *
     * See TS 23.040 9.2.3.9.
     */
    private Uri replaceMessage(Context context, SmsMessage[] msgs, int error) {
        Xlog.v(MmsApp.TXN_TAG, "Sms replaceMessage");
        SmsMessage sms = msgs[0];
        ContentValues values = extractContentValues(sms);

        int pduCount = msgs.length;
        if (pduCount == 1) {
            // There is only one part, so grab the body directly.
            values.put(Inbox.BODY, sms.getMessageBody());
        } else {
            // Build up the body from the parts.
            StringBuilder body = new StringBuilder();
            for (int i = 0; i < pduCount; i++) {
                sms = msgs[i];
                body.append(sms.getMessageBody());
            }
            values.put(Inbox.BODY, body.toString());
        }
        values.put(Sms.ERROR_CODE, error);

        ContentResolver resolver = context.getContentResolver();
        String originatingAddress = sms.getOriginatingAddress();
        int protocolIdentifier = sms.getProtocolIdentifier();
        String selection =
                Sms.ADDRESS + " = ? AND " +
                Sms.PROTOCOL + " = ?";
        String[] selectionArgs = null;

        // for gemini we should care the sim id
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            selection = selection + " AND " + Sms.SIM_ID + " = ?";
            // conver slot id to sim id
            SIMInfo si = SIMInfo.getSIMInfoBySlot(context, sms.getMessageSimId());
            if (null == si) {
                Xlog.e(MmsApp.TXN_TAG, "SmsReceiverService:SIMInfo is null for slot " + sms.getMessageSimId());
                return null;
            }
            int simId = (int)si.mSimId;
            selectionArgs = new String[] {originatingAddress, 
                                          Integer.toString(protocolIdentifier), 
                                          Integer.toString(simId/*sms.getMessageSimId()*/)};
        } else {
            selectionArgs = new String[] {originatingAddress, 
                                          Integer.toString(protocolIdentifier)};
        }
        
        //String[] selectionArgs = new String[] {
        //    originatingAddress, Integer.toString(protocolIdentifier)
        //};

        //Cursor cursor = SqliteWrapper.query(context, resolver, Inbox.CONTENT_URI,
        //                    REPLACE_PROJECTION, selection, selectionArgs, null);

        // add for gemini
        Cursor cursor = SqliteWrapper.query(context, resolver, Inbox.CONTENT_URI,
                            FeatureOption.MTK_GEMINI_SUPPORT ? REPLACE_PROJECTION_GEMINI : REPLACE_PROJECTION,
                            selection, 
                            selectionArgs, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    long messageId = cursor.getLong(REPLACE_COLUMN_ID);
                    Uri messageUri = ContentUris.withAppendedId(
                            Sms.CONTENT_URI, messageId);

                    SqliteWrapper.update(context, resolver, messageUri,
                                        values, null, null);
                    return messageUri;
                }
            } finally {
                cursor.close();
            }
        }
        return storeMessage(context, msgs, error);
    }

    private Uri storeMessage(Context context, SmsMessage[] msgs, int error) {
        Xlog.v(MmsApp.TXN_TAG, "Sms storeMessage");
        SmsMessage sms = msgs[0];

        // Store the message in the content provider.
        ContentValues values = extractContentValues(sms);
        values.put(Sms.ERROR_CODE, error);
        int pduCount = msgs.length;

        if (pduCount == 1) {
            // There is only one part, so grab the body directly.
            values.put(Inbox.BODY, sms.getDisplayMessageBody());
        } else {
            // Build up the body from the parts.
            StringBuilder body = new StringBuilder();
            for (int i = 0; i < pduCount; i++) {
                sms = msgs[i];
                body.append(sms.getDisplayMessageBody());
            }
            values.put(Inbox.BODY, body.toString());
        }

        // Make sure we've got a thread id so after the insert we'll be able to delete
        // excess messages.
        Long threadId = values.getAsLong(Sms.THREAD_ID);
        String address = values.getAsString(Sms.ADDRESS);
        if (!TextUtils.isEmpty(address)) {
            Contact cacheContact = Contact.get(address,true);
            if (cacheContact != null) {
                address = cacheContact.getNumber();
            }
        } else {
            address = getString(R.string.unknown_sender);
            values.put(Sms.ADDRESS, address);
        }

        Uri insertedUri = null;
        if (((threadId == null) || (threadId == 0)) && (address != null)) {
            for (int i = 0; (i < 5) && ((threadId == null) || (threadId == 0)); ++i){
                try{
                    threadId = Threads.getOrCreateThreadId(context, address);
                } catch (IllegalArgumentException iae){
                    Xlog.e(MmsApp.TXN_TAG, "getOrCreateThreadId failed for this time:" + i);
                }
            }
            //threadId = Threads.getOrCreateThreadId(context, address);
            values.put(Sms.THREAD_ID, threadId);
            Xlog.d(MmsApp.TXN_TAG, "address=" + address + "\tthread id = " + threadId);
        }

        // update "status" to 1
        Uri statusUri = ContentUris.withAppendedId(UPDATE_THREADS_URI, threadId);
        ContentValues statusContentValues = new ContentValues(1);
        statusContentValues.put(Threads.STATUS, 1);
        context.getContentResolver().update(statusUri, statusContentValues, null, null);

        // for gemini, we should add sim id information
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            // conver slot id to sim id
            SIMInfo si = SIMInfo.getSIMInfoBySlot(context, sms.getMessageSimId());
            if (null == si) {
                Xlog.e(MmsApp.TXN_TAG, "SmsReceiverService:SIMInfo is null for slot " + sms.getMessageSimId());
                return null;
            }
            values.put(Sms.SIM_ID, (int)si.mSimId/*sms.getMessageSimId()*/);
            
            /* MTK note: for FTA test in the GEMINI phone
            * We need to tell SmsManager where the last incoming SMS comes from.
            * This is because the mms APP and Phone APP runs in two different process
            * and mms will use setSmsMemoryStatus to tell modem that the ME storage is full or not.
            * Since We need to dispatch the infomation about ME storage to currect SIM
            * so we should use setLastIncomingSmsSimId here 
            * to tell SmsManager this to let it dispatch the info.
            */
            SmsManager.getDefault().setLastIncomingSmsSimId(sms.getMessageSimId());
        }
        ContentResolver resolver = context.getContentResolver();
        insertedUri = SqliteWrapper.insert(context, resolver, Inbox.CONTENT_URI, values);
        
        // update "status" to 0
        statusContentValues = new ContentValues(1);
        statusContentValues.put(Threads.STATUS, 0);
        context.getContentResolver().update(statusUri, statusContentValues, null, null);
        
        // store on SIM if needed
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String optr = SystemProperties.get("ro.operator.optr");
        String storeLocation = null;
        //MTK_OP02_PROTECT_START
        if ("OP02".equals(optr)) {
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                final String saveLocationKey = Integer.toString(sms.getMessageSimId()) 
                        + "_" + MessagingPreferenceActivity.SMS_SAVE_LOCATION;
                storeLocation = prefs.getString(saveLocationKey, "Phone");
            } else {
                storeLocation = prefs.getString(MessagingPreferenceActivity.SMS_SAVE_LOCATION, "Phone");
            }
        } else {
        //MTK_OP02_PROTECT_END
            storeLocation = prefs.getString(MessagingPreferenceActivity.SMS_SAVE_LOCATION, "Phone");
        //MTK_OP02_PROTECT_START
        }
        //MTK_OP02_PROTECT_END
        if (storeLocation.equals("Sim")) {
            String sc = (null == sms.getServiceCenterAddress()) ? "" : sms.getServiceCenterAddress();
            boolean bSucceed = true;
            for (int i = 0; i < pduCount; i++) {
                sms = msgs[i];
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    bSucceed = GeminiSmsManager.copyMessageToIccGemini(sms.getSmsc(), 
                            sms.getTpdu(), SmsManager.STATUS_ON_ICC_READ, sms.getMessageSimId());
                    Xlog.d(MmsApp.TXN_TAG, "save sms on SIM. part:" + i 
                            + "; result:" + bSucceed + "; sc:" + sc + "; slotId:" + sms.getMessageSimId());
                } else {
                    bSucceed = SmsManager.getDefault().copyMessageToIcc(sms.getSmsc(),sms.getTpdu(), SmsManager.STATUS_ON_ICC_READ);
                    Xlog.d(MmsApp.TXN_TAG, "save sms on SIM. part:" + i + "; result:" + bSucceed + "; sc:" + sc);
                }
            }
        }

        // set sms size
        if (null != insertedUri) {
            int messageSize = 0;
            if (pduCount == 1) {
                messageSize = sms.getPdu().length;
            } else {
                for (int i = 0; i < pduCount; i++) {
                    sms = msgs[i];
                    messageSize += sms.getPdu().length;
                }
            }
            ContentValues sizeValue = new ContentValues();
            sizeValue.put(Mms.MESSAGE_SIZE, messageSize);
            SqliteWrapper.update(this, getContentResolver(), insertedUri, sizeValue, null, null);
        }

        // Now make sure we're not over the limit in stored messages
        try {
            ThreadCountManager.getInstance().isFull(threadId, context, ThreadCountManager.OP_FLAG_INCREASE);
        } catch (Exception oe) {
            Log.e(TAG, oe.getMessage());
        }
        Recycler.getSmsRecycler().deleteOldMessagesByThreadId(getApplicationContext(), threadId);

        return insertedUri;
    }

    /**
     * Extract all the content values except the body from an SMS
     * message.
     */
    private ContentValues extractContentValues(SmsMessage sms) {
        // Store the message in the content provider.
        ContentValues values = new ContentValues();

        values.put(Inbox.ADDRESS, sms.getDisplayOriginatingAddress());

        // Use now for the timestamp to avoid confusion with clock
        // drift between the handset and the SMSC.
        values.put(Inbox.DATE, new Long(System.currentTimeMillis()));
        values.put(Inbox.PROTOCOL, sms.getProtocolIdentifier());
        values.put(Inbox.READ, 0);
        values.put(Inbox.SEEN, 0);
        if (sms.getPseudoSubject().length() > 0) {
            values.put(Inbox.SUBJECT, sms.getPseudoSubject());
        }
        values.put(Inbox.REPLY_PATH_PRESENT, sms.isReplyPathPresent() ? 1 : 0);
        values.put(Inbox.SERVICE_CENTER, sms.getServiceCenterAddress());
        return values;
    }

    /**
     * Displays a class-zero message immediately in a pop-up window
     * with the number from where it received the Notification with
     * the body of the message
     *
     */
     private void displayClassZeroMessage(Context context, Intent intent) {
        // Using NEW_TASK here is necessary because we're calling
        // startActivity from outside an activity.
        Xlog.v(MmsApp.TXN_TAG, "Sms displayClassZeroMessage");

        intent.setComponent(new ComponentName(context, ClassZeroActivity.class))
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        // add for gemini, add sim id info
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            intent.putExtra(Phone.GEMINI_SIM_ID_KEY, intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, -1));
        }

        context.startActivity(intent);
    }
/*    
    private void displayClassZeroMessage(Context context, SmsMessage sms) {
        // Using NEW_TASK here is necessary because we're calling
        // startActivity from outside an activity.
        Xlog.v(MmsApp.TXN_TAG, "Sms displayClassZeroMessage");
        Intent smsDialogIntent = new Intent(context, ClassZeroActivity.class)
                .putExtra("pdu", sms.getPdu())
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                          | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        // add for gemini, add sim id info
        if(FeatureOption.MTK_GEMINI_SUPPORT == true){
            smsDialogIntent.putExtra(Phone.GEMINI_SIM_ID_KEY, sms.getMessageSimId());
        }

        context.startActivity(smsDialogIntent);
    }
*/
    private void registerForServiceStateChanges() {
        Context context = getApplicationContext();
        unRegisterForServiceStateChanges();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "registerForServiceStateChanges");
        }

        context.registerReceiver(SmsReceiver.getInstance(), intentFilter);
    }

    private void unRegisterForServiceStateChanges() {
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "unRegisterForServiceStateChanges");
        }
        try {
            Context context = getApplicationContext();
            context.unregisterReceiver(SmsReceiver.getInstance());
        } catch (IllegalArgumentException e) {
            // Allow un-matched register-unregister calls
        }
    }


    /**
     * send first queue message, and the messages should have the same SIMID as the last send one.
     * * @param lastSIMID The SIM ID that last message used
     *
     */
    // 2.2
    public synchronized void sendFirstQueuedMessageGemini(int simId) {
        Xlog.d(MmsApp.TXN_TAG, "sendFirstQueuedMessageGemini() simId=" + simId);
        boolean success = true;
        // get all the queued messages from the database
        final Uri uri = Uri.parse("content://sms/queued");
        ContentResolver resolver = getContentResolver();
        Cursor c = SqliteWrapper.query(this, resolver, uri,
                        SEND_PROJECTION, Mms.SIM_ID + "=" + simId, null, "date ASC");   // date ASC so we send out in
                                                                    // same order the user tried
                                                                    // to send messages.
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    String msgText = c.getString(SEND_COLUMN_BODY);
                    String address = c.getString(SEND_COLUMN_ADDRESS);
                    int threadId = c.getInt(SEND_COLUMN_THREAD_ID);
                    int status = c.getInt(SEND_COLUMN_STATUS);

                    int msgId = c.getInt(SEND_COLUMN_ID);
                    Uri msgUri = ContentUris.withAppendedId(Sms.CONTENT_URI, msgId);

                    SmsMessageSender sender = new SmsSingleRecipientSender(this,
                            address, msgText, threadId, status == Sms.STATUS_PENDING,
                            msgUri, simId);

                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "sendFirstQueuedMessage " + msgUri +
                                ", address: " + address +
                                ", threadId: " + threadId +
                                ", body: " + msgText);
                    }
                    Xlog.d(MmsApp.TXN_TAG, "sendFirstQueuedMessage " + msgUri 
                        + ", address: " + address 
                        + ", threadId: " + threadId 
                        + ", body: " + msgText 
                        + ", simId: " + simId);
                    try {
                        sender.sendMessageGemini(SendingProgressTokenManager.NO_TOKEN, simId);
                        mSending = true;
                    } catch (MmsException e) {
                        Log.e(TAG, "sendFirstQueuedMessage: failed to send message " + msgUri
                                + ", caught ", e);
                        success = false;
                    }
                }
            } finally {
                if (!success) {
                    int msgId = c.getInt(SEND_COLUMN_ID);
                    Uri msgUri = ContentUris.withAppendedId(Sms.CONTENT_URI, msgId);
                    messageFailedToSend(msgUri, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
                    sendFirstQueuedMessageGemini(simId);
                }
                c.close();
            }
        }
        //check wether there are more pending messages need to be sent.
        c = null;
        c = SqliteWrapper.query(this, resolver, uri, SEND_PROJECTION, null, null, null);
        Xlog.d(MmsApp.TXN_TAG, "there is pending sms:" + (c == null?false:true));
        if (success && c == null) {
            //only when no pending msgs,we can unregister safely.
            // We successfully sent all the messages in the queue. We don't need to
            // be notified of any service changes any longer.
            unRegisterForServiceStateChanges();
        }
        if (c != null) {
            c.close();
        }
    }

}


