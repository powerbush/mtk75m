/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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
import com.android.mms.ui.CBMessageListActivity;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.ui.ConversationList;
import com.android.mms.ui.MessagingPreferenceActivity;
import android.provider.Telephony;
import com.android.mms.util.AddressUtils;
import com.android.mms.util.DownloadManager;

import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.util.SqliteWrapper;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Telephony.CbSms;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;

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
public class CBMessagingNotification {
    private static final String TAG = LogTag.APP;

    private static final int NOTIFICATION_ID = 125;

    private static final Uri URL_MESSAGES = Telephony.CbSms.CONTENT_URI;
    
    // This must be consistent with the column constants below.
    private static final String[] CB_STATUS_PROJECTION = new String[] {
            Telephony.CbSms.THREAD_ID, Telephony.CbSms.DATE,
            Telephony.CbSms.CHANNEL_ID, Telephony.CbSms.BODY };

    private static final int COLUMN_THREAD_ID   = 0;
    private static final int COLUMN_DATE        = 1;
    private static final int COLUMN_CB_CHANNEL = 2;
    private static final int COLUMN_CB_BODY    = 3;

    private static final String NEW_INCOMING_SM_CONSTRAINT =
            "(" + Telephony.CbSms.SEEN + " = 0)";

    private static final CBNotificationInfoComparator INFO_COMPARATOR =
            new CBNotificationInfoComparator();

    private CBMessagingNotification() {
    }

    /**
     * Checks to see if there are any unread messages or delivery
     * reports.  Shows the most recent notification if there is one.
     *
     * @param context the context to use
     */
    public static void updateNewMessageIndicator(Context context) {
        updateNewMessageIndicator(context, false);
    }

    /**
     * Checks to see if there are any unread messages or delivery
     * reports.  Shows the most recent notification if there is one.
     *
     * @param context the context to use
     * @param isNew if notify a new message comes, it should be true, otherwise, false.
     */
    public static void updateNewMessageIndicator(Context context, boolean isNew) {
        SortedSet<CBNotificationInfo> accumulator =
                new TreeSet<CBNotificationInfo>(INFO_COMPARATOR);
        Set<Long> threads = new HashSet<Long>(4);
        int count = 0;
        count += accumulateNotificationInfo(
                accumulator, getCBNewMessageNotificationInfo(context, threads));

        cancelNotification(context, NOTIFICATION_ID);
        if (!accumulator.isEmpty()) {
            accumulator.first().deliver(context, isNew, count, threads.size());
        }
    }

    /**
     * Updates all pending notifications, clearing or updating them as
     * necessary.  This task is completed in the background on a worker
     * thread.
     */
    public static void updateAllNotifications(final Context context) {
        new Thread(new Runnable() {
            public void run() {
                updateNewMessageIndicator(context);
            }
        }).start();
    }
    
    private static final int accumulateNotificationInfo(
            SortedSet set, CBNotificationInfo info) {
        if (info != null) {
            set.add(info);
            return info.mCount;
        }
        return 0;
    }

    private static final class CBNotificationInfo {
        public Intent mClickIntent;
        public String mDescription;
        public int mIconResourceId;
        public CharSequence mTicker;
        public long mTimeMillis;
        public String mTitle;
        public int mCount;

        public CBNotificationInfo(
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
            updateNotification(
                    context, mClickIntent, mDescription, mIconResourceId, isNew,
                    (isNew? mTicker : null), // only display the ticker if the message is new
                    mTimeMillis, mTitle, count, uniqueThreads);
        }

        public long getTime() {
            return mTimeMillis;
        }
    }

    private static final class CBNotificationInfoComparator
            implements Comparator<CBNotificationInfo> {
        public int compare(
                CBNotificationInfo info1, CBNotificationInfo info2) {
            return Long.signum(info2.getTime() - info1.getTime());
        }
    }

    public static final CBNotificationInfo getCBNewMessageNotificationInfo(
            Context context, Set<Long> threads) {
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = SqliteWrapper.query(context, resolver, URL_MESSAGES,
                            CB_STATUS_PROJECTION, NEW_INCOMING_SM_CONSTRAINT,
                            null, Telephony.CbSms.DATE + " desc");

        if (cursor == null) {
            return null;
        }
        
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            // TODO get the description by query the channel table.
            int channel_id = cursor.getInt(COLUMN_CB_CHANNEL);
            String body = cursor.getString(COLUMN_CB_BODY);
            long threadId = cursor.getLong(COLUMN_THREAD_ID);
            long timeMillis = cursor.getLong(COLUMN_DATE);
            String address = Conversation.getChannelNameFromId(context, channel_id);
            address = address + "(" + channel_id + ")";
            CBNotificationInfo info = getNewMessageNotificationInfo(
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

    private static final CBNotificationInfo getNewMessageNotificationInfo(
            String address,
            String body,
            Context context,
            int iconResourceId,
            String subject,
            long threadId,
            long timeMillis,
            int count) {
        Intent clickIntent = CBMessageListActivity.createIntent(context, threadId);
        clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        String senderInfo = buildTickerMessage(
                context, address, null, null).toString();
        String senderInfoName = senderInfo.substring(
                0, senderInfo.length() - 2);
        CharSequence ticker = buildTickerMessage(
                context, address, subject, body);

        return new CBNotificationInfo(
                clickIntent, body, iconResourceId, ticker, timeMillis,
                senderInfoName, count);
    }

    public static void cancelNotification(Context context, int notificationId) {
        NotificationManager nm = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);

        nm.cancel(notificationId);
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
            title = context.getString(R.string.notification_multiple_cb_title);
            clickIntent = new Intent(Intent.ACTION_MAIN);
            clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            clickIntent.setType("vnd.android.cursor.dir/cb-messages");
        }
        
        // If there is more than one message, change the description (which
        // would normally be a snippet of the individual message text) to
        // a string indicating how many unread messages there are.
        if (messageCount > 1) {
            description = context.getString(R.string.notification_multiple_cb,
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
            MessagingNotification.processNotificationSound(context, notification, ringtone);
        }

        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        notification.ledARGB = 0xff00ff00;
        notification.ledOnMS = 500;
        notification.ledOffMS = 2000;

        NotificationManager nm = (NotificationManager)
            context.getSystemService(Context.NOTIFICATION_SERVICE);

        nm.notify(NOTIFICATION_ID, notification);
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
    
    // threadIdResult[0] contains the thread id of the first message.
    // threadIdResult[1] is nonzero if the thread ids of all the messages are the same.
    // You can pass in null for threadIdResult.
    // You can pass in a threadIdResult of size 1 to avoid the comparison of each thread id.
    private static int getUndeliveredMessageCount(Context context, long[] threadIdResult) {
        Cursor undeliveredCursor = SqliteWrapper.query(context, context.getContentResolver(),
                URL_MESSAGES, new String[] { Telephony.CbSms.THREAD_ID }, "read=0", null, null);
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
    
    public static boolean isFailedToDeliver(Intent intent) {
        return (intent != null) && intent.getBooleanExtra("undelivered_flag", false);
    }

    public static boolean isFailedToDownload(Intent intent) {
        return (intent != null) && intent.getBooleanExtra("failed_download_flag", false);
    }
}
