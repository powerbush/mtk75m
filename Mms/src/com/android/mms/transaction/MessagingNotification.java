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

import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;
import static com.google.android.mms.pdu.PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF;

import com.android.mms.R;
import com.android.mms.LogTag;
import com.android.mms.data.Contact;
import com.android.mms.data.Conversation;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.ui.ConversationList;
import com.android.mms.ui.MessagingPreferenceActivity;
import com.android.mms.util.AddressUtils;
import com.android.mms.util.DownloadManager;

import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPersister;
import com.mediatek.featureoption.FeatureOption;
import android.database.sqlite.SqliteWrapper;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.NotificationPlus;
import android.app.NotificationManagerPlus;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.widget.Toast;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class is used to update the notification indicator. It will check whether
 * there are unread messages. If yes, it would show the notification indicator,
 * otherwise, hide the indicator.
 */
public class MessagingNotification {
    private static final String TAG = "MessagingNotification";
    public static Uri sMessageUri = null;
    private static final int NOTIFICATION_ID = 123;
    public static final int MESSAGE_FAILED_NOTIFICATION_ID = 789;
    public static final int DOWNLOAD_FAILED_NOTIFICATION_ID = 531;
    public static final int CLASS_ZERO_NOTIFICATION_ID = 5566;

    // This must be consistent with the column constants below.
    private static final String[] MMS_STATUS_PROJECTION = new String[] {
        Mms.THREAD_ID, Mms.DATE, Mms._ID, Mms.SUBJECT, Mms.SUBJECT_CHARSET };

    // This must be consistent with the column constants below.
    private static final String[] SMS_STATUS_PROJECTION = new String[] {
        Sms.THREAD_ID, Sms.DATE, Sms.ADDRESS, Sms.SUBJECT, Sms.BODY };

    // These must be consistent with MMS_STATUS_PROJECTION and
    // SMS_STATUS_PROJECTION.
    private static final int COLUMN_THREAD_ID   = 0;
    private static final int COLUMN_DATE        = 1;
    private static final int COLUMN_MMS_ID      = 2;
    private static final int COLUMN_SMS_ADDRESS = 2;
    private static final int COLUMN_SUBJECT     = 3;
    private static final int COLUMN_SUBJECT_CS  = 4;
    private static final int COLUMN_SMS_BODY    = 4;

    private static final String NEW_INCOMING_SM_CONSTRAINT =
            "(" + Sms.TYPE + " = " + Sms.MESSAGE_TYPE_INBOX
            + " AND " + Sms.SEEN + " = 0)";

    private static final String NEW_DELIVERY_SM_CONSTRAINT =
        "((" + Sms.TYPE + " = " + Sms.MESSAGE_TYPE_SENT
        + " OR " + Sms.TYPE + " = " + Sms.MESSAGE_TYPE_OUTBOX + ")"
        + " AND " + "(" + Sms.STATUS + " = " + Sms.STATUS_COMPLETE
        + " OR " + Sms.STATUS + " = " + Sms.STATUS_PENDING + "))";

    private static final String NEW_INCOMING_MM_CONSTRAINT =
            "(" + Mms.MESSAGE_BOX + "=" + Mms.MESSAGE_BOX_INBOX
            + " AND " + Mms.SEEN + "=0"
            + " AND (" + Mms.MESSAGE_TYPE + "=" + MESSAGE_TYPE_NOTIFICATION_IND
            + " OR " + Mms.MESSAGE_TYPE + "=" + MESSAGE_TYPE_RETRIEVE_CONF + "))";

    private static final MmsSmsNotificationInfoComparator INFO_COMPARATOR =
            new MmsSmsNotificationInfoComparator();

    private static final Uri UNDELIVERED_URI = Uri.parse("content://mms-sms/undelivered");


    private final static String NOTIFICATION_DELETED_ACTION =
            "com.android.mms.NOTIFICATION_DELETED_ACTION";

    public static class OnDeletedReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                Log.d(TAG, "[MessagingNotification] clear notification: mark all msgs seen");
            }

            Conversation.markAllConversationsAsSeen(context);
        }
    };
    private static OnDeletedReceiver sNotificationDeletedReceiver = new OnDeletedReceiver();
    private static Intent sNotificationOnDeleteIntent;
    private static Handler mToastHandler = new Handler();

    private MessagingNotification() {
    }

    public static void init(Context context) {
        // set up the intent filter for notification deleted action
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NOTIFICATION_DELETED_ACTION);
        context.registerReceiver(sNotificationDeletedReceiver, intentFilter);

        // initialize the notification deleted action
        sNotificationOnDeleteIntent = new Intent(NOTIFICATION_DELETED_ACTION);
    }

    /**
     * Checks to see if there are any "unseen" messages or delivery
     * reports.  Shows the most recent notification if there is one.
     * Does its work and query in a worker thread.
     *
     * @param context the context to use
     */
    public static void nonBlockingUpdateNewMessageIndicator(final Context context,
            final boolean isNew,
            final boolean isStatusMessage) {
        Log.d(TAG, "nonBlockingUpdateNewMessageIndicator, isNew = " + isNew + ", isStatusMessage = " + isStatusMessage) ;
        new Thread(new Runnable() {
            public void run() {
                blockingUpdateNewMessageIndicator(context, isNew, isStatusMessage);
            }
        }).start();
    }

    /**
     * Checks to see if there are any "unseen" messages or delivery
     * reports.  Shows the most recent notification if there is one.
     *
     * @param context the context to use
     * @param isNew if notify a new message comes, it should be true, otherwise, false.
     */
    public static void blockingUpdateNewMessageIndicator(Context context, boolean isNew,
            boolean isStatusMessage) {
        Log.d(TAG, "blockingUpdateNewMessageIndicator, isNew = " + isNew);
        SortedSet<MmsSmsNotificationInfo> accumulator =
                new TreeSet<MmsSmsNotificationInfo>(INFO_COMPARATOR);
        MmsSmsDeliveryInfo delivery = null;
        Set<Long> threads = new HashSet<Long>(4);

        int count = 0;
        count += accumulateNotificationInfo(
                accumulator, getMmsNewMessageNotificationInfo(context, threads));
        count += accumulateNotificationInfo(
                accumulator, getSmsNewMessageNotificationInfo(context, threads));

        cancelNotification(context, NOTIFICATION_ID);
        if (!accumulator.isEmpty()) {
            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                Log.d(TAG, "blockingUpdateNewMessageIndicator: count=" + count +
                        ", isNew=" + isNew);
            }
            accumulator.first().deliver(context, isNew, count, threads.size());
        }

        // And deals with delivery reports (which use Toasts). It's safe to call in a worker
        // thread because the toast will eventually get posted to a handler.
        delivery = getSmsNewDeliveryInfo(context);
        if (delivery != null) {
            delivery.deliver(context, isStatusMessage);
        }
    }

    /**
     * Updates all pending notifications, clearing or updating them as
     * necessary.
     */
    public static void blockingUpdateAllNotifications(final Context context) {
        nonBlockingUpdateNewMessageIndicator(context, false, false);
        updateSendFailedNotification(context);
        updateDownloadFailedNotification(context);
        CBMessagingNotification.updateNewMessageIndicator(context);
    }

    private static final int accumulateNotificationInfo(
            SortedSet set, MmsSmsNotificationInfo info) {
        Log.d(TAG, "accumulateNotificationInfo");
        if (info != null) {
            set.add(info);

            return info.mCount;
        }

        return 0;
    }

    private static final class MmsSmsDeliveryInfo {
        public CharSequence mTicker;
        public long mTimeMillis;

        public MmsSmsDeliveryInfo(CharSequence ticker, long timeMillis) {
            mTicker = ticker;
            mTimeMillis = timeMillis;
        }

        public void deliver(Context context, boolean isStatusMessage) {
            updateDeliveryNotification(
                    context, isStatusMessage, mTicker, mTimeMillis);
        }
    }

    private static final class MmsSmsNotificationInfo {
        public Intent mClickIntent;
        public String mDescription;
        public int mIconResourceId;
        public CharSequence mTicker;
        public long mTimeMillis;
        public String mTitle;
        public int mCount;

        public MmsSmsNotificationInfo(
                Intent clickIntent, String description, int iconResourceId,
                CharSequence ticker, long timeMillis, String title, int count) {
            mClickIntent = clickIntent;
            mDescription = description;
            mIconResourceId = iconResourceId;
            mTicker = ticker;
            mTimeMillis = timeMillis;
            mTitle = title;
            mCount = count;
        }

        public void deliver(Context context, boolean isNew, int count, int uniqueThreads) {
            Log.d(TAG, "deliver");            
            updateNotification(
                    context, mClickIntent, mDescription, mIconResourceId, isNew,
                    (isNew? mTicker : null), // only display the ticker if the message is new
                    mTimeMillis, mTitle, count, uniqueThreads);
        }

        public long getTime() {
            return mTimeMillis;
        }
    }

    private static final class MmsSmsNotificationInfoComparator
            implements Comparator<MmsSmsNotificationInfo> {
        public int compare(
                MmsSmsNotificationInfo info1, MmsSmsNotificationInfo info2) {
            return Long.signum(info2.getTime() - info1.getTime());
        }
    }

    private static final MmsSmsNotificationInfo getMmsNewMessageNotificationInfo(
            Context context, Set<Long> threads) {
        Log.d(TAG, "getMmsNewMessageNotificationInfo");
        ContentResolver resolver = context.getContentResolver();

        // This query looks like this when logged:
        // I/Database(  147): elapsedTime4Sql|/data/data/com.android.providers.telephony/databases/
        // mmssms.db|0.362 ms|SELECT thread_id, date, _id, sub, sub_cs FROM pdu WHERE ((msg_box=1
        // AND seen=0 AND (m_type=130 OR m_type=132))) ORDER BY date desc

        Cursor cursor = SqliteWrapper.query(context, resolver, Mms.CONTENT_URI,
                            MMS_STATUS_PROJECTION, NEW_INCOMING_MM_CONSTRAINT,
                            null, Mms.DATE + " desc");

        if (cursor == null) {
            return null;
        }

        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            long msgId = cursor.getLong(COLUMN_MMS_ID);
            Uri msgUri = Mms.CONTENT_URI.buildUpon().appendPath(
                    Long.toString(msgId)).build();
            String address = AddressUtils.getFrom(context, msgUri);
            if(TextUtils.isEmpty(address)){
                address = context.getText(android.R.string.unknownName).toString();
            }
            String subject = getMmsSubject(
                    cursor.getString(COLUMN_SUBJECT), cursor.getInt(COLUMN_SUBJECT_CS));
            long threadId = cursor.getLong(COLUMN_THREAD_ID);
            long timeMillis = cursor.getLong(COLUMN_DATE) * 1000;

            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                Log.d(TAG, "getMmsNewMessageNotificationInfo: count=" + cursor.getCount() +
                        ", first addr = " + address + ", thread_id=" + threadId);
            }

            MmsSmsNotificationInfo info = getNewMessageNotificationInfo(
                    address, subject, context,
                    R.drawable.stat_notify_sms, null, threadId,
                    timeMillis, cursor.getCount());

            threads.add(threadId);
            while (cursor.moveToNext()) {
                threads.add(cursor.getLong(COLUMN_THREAD_ID));
            }

            return info;
        } finally {
            cursor.close();
        }
    }

    private static final MmsSmsDeliveryInfo getSmsNewDeliveryInfo(Context context) {
        ContentResolver resolver = context.getContentResolver();
        Uri uri = sMessageUri;
        if (uri == null){
            uri = Sms.CONTENT_URI;
        }
        Cursor cursor = SqliteWrapper.query(context, resolver, uri,
                    SMS_STATUS_PROJECTION, NEW_DELIVERY_SM_CONSTRAINT,
                    null, Sms.DATE + " desc");
        sMessageUri = null;
        if (cursor == null)
            return null;

        try {
            if (!cursor.moveToFirst())
            return null;

            String address = cursor.getString(COLUMN_SMS_ADDRESS);
            long timeMillis = 3000;

            return new MmsSmsDeliveryInfo(String.format(
                context.getString(R.string.delivery_toast_body), address),
                timeMillis);

        } finally {
            cursor.close();
        }
    }

    private static final MmsSmsNotificationInfo getSmsNewMessageNotificationInfo(
            Context context, Set<Long> threads) {
        Log.d(TAG, "getSmsNewMessageNotificationInfo");            
        ContentResolver resolver = context.getContentResolver();
        String constraint = NEW_INCOMING_SM_CONSTRAINT;
        Cursor cursor = SqliteWrapper.query(context, resolver, Sms.CONTENT_URI,
                            SMS_STATUS_PROJECTION, constraint,
                            null, Sms.DATE + " desc");

        if (cursor == null) {
            return null;
        }

        try {
            if (!cursor.moveToFirst()) {
                return null;
            }

            String address = cursor.getString(COLUMN_SMS_ADDRESS);
            if(TextUtils.isEmpty(address)){
                address = context.getText(android.R.string.unknownName).toString();
            }
            String body = cursor.getString(COLUMN_SMS_BODY);
            long threadId = cursor.getLong(COLUMN_THREAD_ID);
            long timeMillis = cursor.getLong(COLUMN_DATE);

            //if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) 
            {
                Log.d(TAG, "getSmsNewMessageNotificationInfo: count=" + cursor.getCount() +
                        ", first addr=" + address + ", thread_id=" + threadId);
            }

            MmsSmsNotificationInfo info = getNewMessageNotificationInfo(
                    address, body, context, R.drawable.stat_notify_sms,
                    null, threadId, timeMillis, cursor.getCount());

            threads.add(threadId);
            while (cursor.moveToNext()) {
                threads.add(cursor.getLong(COLUMN_THREAD_ID));
            }

            return info;
        } finally {
            cursor.close();
        }
    }

    private static final MmsSmsNotificationInfo getNewMessageNotificationInfo(
            String address,
            String body,
            Context context,
            int iconResourceId,
            String subject,
            long threadId,
            long timeMillis,
            int count) {
        Log.d(TAG, "getNewMessageNotificationInfo");
        Intent clickIntent = ComposeMessageActivity.createIntent(context, threadId);
        clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        String senderInfo = buildTickerMessage(
                context, address, null, null).toString();
        String senderInfoName = senderInfo.substring(
                0, senderInfo.length() - 2);
        CharSequence ticker = buildTickerMessage(
                context, address, subject, body);

        return new MmsSmsNotificationInfo(
                clickIntent, body, iconResourceId, ticker, timeMillis,
                senderInfoName, count);
    }

    public static void cancelNotification(Context context, int notificationId) {
        Log.d(TAG, "cancelNotification");
        Log.d(TAG, "    notificationId : " + notificationId);        
        NotificationManager nm = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);

        nm.cancel(notificationId);
    }

    private static void updateDeliveryNotification(final Context context,
                                                   boolean isStatusMessage,
                                                   final CharSequence message,
                                                   final long timeMillis) {
        if (!isStatusMessage) {
            return;
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        if (!sp.getBoolean(MessagingPreferenceActivity.NOTIFICATION_ENABLED, true)) {
            return;
        }

        mToastHandler.post(new Runnable() {
            public void run() {
                Toast.makeText(context, message, (int)timeMillis).show();
            }
        });
    }

    private static void updateNotification(
            Context context,
            Intent clickIntent,
            String description,
            int iconRes,
            boolean isNew,
            CharSequence ticker,
            long timeMillis,
            String title,
            int messageCount,
            int uniqueThreadCount) {
        Log.d(TAG, "updateNotification");            
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        if (!sp.getBoolean(MessagingPreferenceActivity.NOTIFICATION_ENABLED, true)) {
            return;
        }

        Notification notification = new Notification(iconRes, ticker, timeMillis);

        // If we have more than one unique thread, change the title (which would
        // normally be the contact who sent the message) to a generic one that
        // makes sense for multiple senders, and change the Intent to take the
        // user to the conversation list instead of the specific thread.
        if (uniqueThreadCount > 1) {
            title = context.getString(R.string.notification_multiple_title);
            clickIntent = new Intent(Intent.ACTION_MAIN);

            clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            clickIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            clickIntent.setClassName("com.android.mms", "com.android.mms.ui.ConversationList");
            //clickIntent.setType("vnd.android-dir/mms-sms");
        }

        // If there is more than one message, change the description (which
        // would normally be a snippet of the individual message text) to
        // a string indicating how many "unseen" messages there are.
        if (messageCount > 1) {
            description = context.getString(R.string.notification_multiple,
                    Integer.toString(messageCount));
        }

        // Make a startActivity() PendingIntent for the notification.
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Update the notification.
        notification.setLatestEventInfo(context, title, description, pendingIntent);

        if (isNew) {
            String ringtoneStr = sp.getString(MessagingPreferenceActivity.NOTIFICATION_RINGTONE, null);
            Uri ringtone = TextUtils.isEmpty(ringtoneStr) ? null : Uri.parse(ringtoneStr);
            processNotificationSound(context, notification, ringtone);
            // add for CTA 5.3.3
            NotificationPlus notiPlus = new NotificationPlus.Builder(context)
                    .setTitle(context.getString(R.string.new_message))
                    .setMessage(context.getString(R.string.notification_multiple, Integer.toString(messageCount)))
                    .setPositiveButton(context.getString(R.string.view), pendingIntent)
                    .create();
            NotificationManagerPlus.notify(1, notiPlus);
        }

        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.defaults |= Notification.DEFAULT_LIGHTS;

        // set up delete intent
        notification.deleteIntent = PendingIntent.getBroadcast(context, 0, sNotificationOnDeleteIntent, 0);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        nm.notify(NOTIFICATION_ID, notification);
    }
    
    protected static void processNotificationSound(Context context, Notification notification, Uri ringtone) {
        int state = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getCallState();
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (FeatureOption.MTK_BRAZIL_CUSTOMIZATION_CLARO && state != TelephonyManager.CALL_STATE_IDLE) {
            /* in call or in ringing */
            
            /* ringtone on, and into music mode */
            notification.audioStreamType = AudioManager.STREAM_MUSIC;
        }
        if (audioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION) == AudioManager.VIBRATE_SETTING_ON) {
            /* vibrate on */
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        }
        notification.sound = ringtone;
    }

    protected static CharSequence buildTickerMessage(
            Context context, String address, String subject, String body) {
        String displayAddress = Contact.get(address, true).getName();

        StringBuilder buf = new StringBuilder(
                displayAddress == null
                ? ""
                : displayAddress.replace('\n', ' ').replace('\r', ' '));
        buf.append(':').append(' ');

        int offset = buf.length();
        if (!TextUtils.isEmpty(subject)) {
            subject = subject.replace('\n', ' ').replace('\r', ' ');
            buf.append(subject);
            buf.append(' ');
        }

        if (!TextUtils.isEmpty(body)) {
            body = body.replace('\n', ' ').replace('\r', ' ');
            buf.append(body);
        }

        SpannableString spanText = new SpannableString(buf.toString());
        spanText.setSpan(new StyleSpan(Typeface.BOLD), 0, offset,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spanText;
    }

    private static String getMmsSubject(String sub, int charset) {
        return TextUtils.isEmpty(sub) ? ""
                : new EncodedStringValue(charset, PduPersister.getBytes(sub)).getString();
    }

    public static void notifyDownloadFailed(Context context, long threadId) {
        Log.d(TAG, "notifyDownloadFailed");                    
        notifyFailed(context, true, threadId, false);
    }

    public static void notifySendFailed(Context context) {
        Log.d(TAG, "notifySendFailed");
        notifyFailed(context, false, 0, false);
    }

    public static void notifySendFailed(Context context, boolean noisy) {
        Log.d(TAG, "notifySendFailed, noisy = " + noisy);
        notifyFailed(context, false, 0, noisy);
    }

    private static void notifyFailed(Context context, boolean isDownload, long threadId,
                                     boolean noisy) {
        // TODO factor out common code for creating notifications
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        boolean enabled = sp.getBoolean(MessagingPreferenceActivity.NOTIFICATION_ENABLED, true);
        if (!enabled) {
            return;
        }

        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Strategy:
        // a. If there is a single failure notification, tapping on the notification goes
        //    to the compose view.
        // b. If there are two failure it stays in the thread view. Selecting one undelivered
        //    thread will dismiss one undelivered notification but will still display the
        //    notification.If you select the 2nd undelivered one it will dismiss the notification.

        long[] msgThreadId = {0};
        int totalFailedCount = getUndeliveredMessageCount(context, msgThreadId);

        Intent failedIntent;
        Notification notification = new Notification();
        String title;
        String description;
        if (totalFailedCount > 1) {
            description = context.getString(R.string.notification_failed_multiple,
                    Integer.toString(totalFailedCount));
            title = context.getString(R.string.notification_failed_multiple_title);

            failedIntent = new Intent(context, ConversationList.class);
        } else {
            title = isDownload ?
                        context.getString(R.string.message_download_failed_title) :
                        context.getString(R.string.message_send_failed_title);

            description = context.getString(R.string.message_failed_body);
            failedIntent = new Intent(context, ComposeMessageActivity.class);
            if (isDownload) {
                // When isDownload is true, the valid threadId is passed into this function.
                failedIntent.putExtra("failed_download_flag", true);
            } else {
                threadId = (msgThreadId[0] != 0 ? msgThreadId[0] : 0);
                failedIntent.putExtra("undelivered_flag", true);
            }
            failedIntent.putExtra("thread_id", threadId);
        }

        failedIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, failedIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notification.icon = R.drawable.stat_notify_sms_failed;

        notification.tickerText = title;

        notification.setLatestEventInfo(context, title, description, pendingIntent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        if (noisy) {
            String ringtoneStr = sp.getString(MessagingPreferenceActivity.NOTIFICATION_RINGTONE, null);
            Uri ringtone = TextUtils.isEmpty(ringtoneStr) ? null : Uri.parse(ringtoneStr);
            processNotificationSound(context, notification, ringtone);
        }

        if (isDownload) {
            nm.notify(DOWNLOAD_FAILED_NOTIFICATION_ID, notification);
        } else {
            nm.notify(MESSAGE_FAILED_NOTIFICATION_ID, notification);
        }
    }

    // threadIdResult[0] contains the thread id of the first message.
    // threadIdResult[1] is nonzero if the thread ids of all the messages are the same.
    // You can pass in null for threadIdResult.
    // You can pass in a threadIdResult of size 1 to avoid the comparison of each thread id.
    private static int getUndeliveredMessageCount(Context context, long[] threadIdResult) {
        Log.d(TAG, "getUndeliveredMessageCount");                    
        Cursor undeliveredCursor = SqliteWrapper.query(context, context.getContentResolver(),
                UNDELIVERED_URI, new String[] { Mms.THREAD_ID }, "read=0", null, null);
        if (undeliveredCursor == null) {
            return 0;
        }
        int count = undeliveredCursor.getCount();
        try {
            if (threadIdResult != null && undeliveredCursor.moveToFirst()) {
                threadIdResult[0] = undeliveredCursor.getLong(0);

                if (threadIdResult.length >= 2) {
                    // Test to see if all the undelivered messages belong to the same thread.
                    long firstId = threadIdResult[0];
                    while (undeliveredCursor.moveToNext()) {
                        if (undeliveredCursor.getLong(0) != firstId) {
                            firstId = 0;
                            break;
                        }
                    }
                    threadIdResult[1] = firstId;    // non-zero if all ids are the same
                }
            }
        } finally {
            undeliveredCursor.close();
        }
        return count;
    }

    public static void updateSendFailedNotification(Context context) {
        Log.d(TAG, "updateSendFailedNotification");
        if (getUndeliveredMessageCount(context, null) < 1) {
            cancelNotification(context, MESSAGE_FAILED_NOTIFICATION_ID);
        } else {
            notifySendFailed(context);      // rebuild and adjust the message count if necessary.
        }
    }

    /**
     *  If all the undelivered messages belong to "threadId", cancel the notification.
     */
    public static void updateSendFailedNotificationForThread(Context context, long threadId) {
        long[] msgThreadId = {0, 0};
        if (getUndeliveredMessageCount(context, msgThreadId) > 0
                && msgThreadId[0] == threadId
                && msgThreadId[1] != 0) {
            cancelNotification(context, MESSAGE_FAILED_NOTIFICATION_ID);
        }
    }

    private static int getDownloadFailedMessageCount(Context context) {
        // Look for any messages in the MMS Inbox that are of the type
        // NOTIFICATION_IND (i.e. not already downloaded) and in the
        // permanent failure state.  If there are none, cancel any
        // failed download notification.
        Cursor c = SqliteWrapper.query(context, context.getContentResolver(),
                Mms.Inbox.CONTENT_URI, null,
                Mms.MESSAGE_TYPE + "=" +
                    String.valueOf(PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND) +
                " AND " + Mms.STATUS + "=" +
                    String.valueOf(DownloadManager.STATE_PERMANENT_FAILURE),
                null, null);
        if (c == null) {
            return 0;
        }
        int count = c.getCount();
        c.close();
        return count;
    }

    public static void updateDownloadFailedNotification(Context context) {
        if (getDownloadFailedMessageCount(context) < 1) {
            cancelNotification(context, DOWNLOAD_FAILED_NOTIFICATION_ID);
        }
    }

    public static boolean isFailedToDeliver(Intent intent) {
        return (intent != null) && intent.getBooleanExtra("undelivered_flag", false);
    }

    public static boolean isFailedToDownload(Intent intent) {
        return (intent != null) && intent.getBooleanExtra("failed_download_flag", false);
    }

    public static boolean notifyClassZeroMessage(Context context, String address) {
        Log.d(TAG, "notifyClassZeroMessage");
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        boolean enabled = sp.getBoolean(MessagingPreferenceActivity.NOTIFICATION_ENABLED, true);
        Log.d(TAG, "notifyClassZeroMessage, enabled = "+enabled);        
        if (!enabled) {
            return false;
        }

        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification();

        String ringtoneStr = sp.getString(MessagingPreferenceActivity.NOTIFICATION_RINGTONE, null);
        Uri ringtone = TextUtils.isEmpty(ringtoneStr) ? null : Uri.parse(ringtoneStr);
        processNotificationSound(context, notification, ringtone);

        notification.tickerText = address;
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.ledARGB = 0xff00ff00;
        notification.ledOnMS = 500;
        notification.ledOffMS = 2000;
        nm.notify(CLASS_ZERO_NOTIFICATION_ID, notification);
        return true;
    }
}
