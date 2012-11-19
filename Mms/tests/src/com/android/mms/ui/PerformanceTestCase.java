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
 */

/*
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

package com.android.mms.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings.Secure;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.android.internal.telephony.Phone;
import com.android.mms.data.Conversation;
import com.android.mms.data.WorkingMessage;
import com.android.mms.data.WorkingMessage.MessageStatusListener;
import com.android.mms.ui.ConversationList.BaseProgressQueryHandler;
import com.android.mms.ui.ConversationList.DeleteThreadListener;
import com.android.mms.util.Reflector;
import com.mediatek.android.performance.BasePerformanceTestCase;

/**
 * Mms performance test case.
 * @author Xin Gang Sun
 *
 */
public class PerformanceTestCase extends BasePerformanceTestCase {
    private static final String TEST_ADDRESS = "10010";
    private static final String DEFAULT_SUBJECT = "Test";
    private static final long DEFAULT_FREQUENCY_DELAY_TIME = 10;
    private static final long NEW_THREAD_ID = 0;
    private static final int TEST_INTENT_FLAG = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
    private static final int MAX_SEND_TIME_LIMIT = 10 * 1000;

    private Instrumentation mInstrumentation;
    private Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mContext = mInstrumentation.getTargetContext();
    }

    private SIMInfo getSIMInfo() {
        try {
            return SIMInfo.getInsertedSIMList(mContext).get(0);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalStateException("No inserted sim found");
        }
    }

    private long getThreadId() throws Throwable {
        return Threads.getOrCreateThreadId(mContext, TEST_ADDRESS);
    }

    private long prepareSms(long threadId, int count) throws Throwable {
        clearSms(threadId);
        for (;count > 0; --count) {
            Sms.addMessageToUri(mContext.getContentResolver(), Sms.CONTENT_URI,
                    TEST_ADDRESS, getMessageAsLength(140), DEFAULT_SUBJECT,
                    System.currentTimeMillis(), true, false, threadId, (int) getSIMInfo().mSimId);
        }
        return threadId;
    }

    private void clearSms(long threadId) throws Throwable {
        Cursor cursor = Sms.query(mContext.getContentResolver(), new String[] {Sms._ID},
                Sms.THREAD_ID + "=" + threadId, null);
        try {
            List<Long> list = new ArrayList<Long>();
            while (cursor.moveToNext()) {
                list.add(cursor.getLong(0));
            }
            if (!list.isEmpty()) {
                String where = Sms._ID + " IN " + list;
                where = where.replace('[', '(');
                where = where.replace(']', ')');
                mContext.getContentResolver().delete(Sms.CONTENT_URI, where, null);
            }
        } finally {
            cursor.close();
        }
    }

    private static void delay() throws InterruptedException {
        delay(DEFAULT_FREQUENCY_DELAY_TIME);
    }

    private static void delay(long time) throws InterruptedException {
        Thread.sleep(time);
    }

    private void dump(String description, long timestamp) {
        mBindHelper.setDescription(description);
        mBindHelper.dumpResult(description, (int) (System.currentTimeMillis() - timestamp));
    }

    /*
     * Test cases must be named test\hXxxXxx to ensure the runing sequence, because the
     * sequence when run them is order by the name alphabet.
     */

    /**
     * Tests launch time.
     * @throws Throwable if error occurs.
     */
    public void test1LaunchTime() throws Throwable {
        mBindHelper.setDescription("testLaunchTime");
        Intent intent = new Intent();
        intent.setFlags(TEST_INTENT_FLAG);
        intent.setClass(mContext, ConversationList.class);
        launchTest("testLaunchTime", intent);
    }

    /**
     * Tests sms thread which contains 20 messages open time.
     * @throws Throwable if error occurs.
     */
    public void test2SmsThreadOpenTime() throws Throwable {
        long threadId = prepareSms(getThreadId(), 20);
        try {
            Intent intent = ComposeMessageActivity.createIntent(mContext, threadId);
            intent.setFlags(TEST_INTENT_FLAG);
            ComposeMessageActivity activity = (ComposeMessageActivity) mInstrumentation.startActivitySync(intent);
            try {
                /* ignore the first sample */
                delay(8000);
            } finally {
                activity.finish();
            }
            delay(1000);
            long timestamp = System.currentTimeMillis();
            activity = (ComposeMessageActivity) mInstrumentation.startActivitySync(intent);
            try {
                while (activity.mMsgListAdapter.getCount() < 20) {
                    delay();
                }
                dump("testSmsThreadOpenTime", timestamp);
            } finally {
                activity.finish();
            }
        } finally {
            clearSms(threadId);
        }
    }

    private static class MessageStatusListenerDecorator implements MessageStatusListener {
        /* The MessageStatusListener proxy decorator. Intercept the invokes to the decoratee. */

        private final MessageStatusListener decoratee;
        private volatile boolean onMessageSent;

        public MessageStatusListenerDecorator(MessageStatusListener decoratee) {
            this.decoratee = decoratee;
        }

        public void onProtocolChanged(boolean mms, boolean needToast) {
            decoratee.onProtocolChanged(mms, needToast);
        }

        public void onPreMessageSent() {
            decoratee.onPreMessageSent();
        }

        public void onMessageSent() {
            decoratee.onMessageSent();
            onMessageSent = true;
        }

        public void onMaxPendingMessagesReached() {
            decoratee.onMaxPendingMessagesReached();
        }

        public void onAttachmentError(int error) {
            decoratee.onAttachmentError(error);
        }

        public void onAttachmentChanged() {
            decoratee.onAttachmentChanged();
        }
    }

    private void testSmsSendOutTime(String description, final List<String> addresses, final int length) throws Throwable {
        try {
            Intent intent = ComposeMessageActivity.createIntent(mContext, NEW_THREAD_ID);
            intent.setFlags(TEST_INTENT_FLAG);
            final ComposeMessageActivity activity = (ComposeMessageActivity) mInstrumentation.startActivitySync(intent);
            try {
                delay(1000);
                final RecipientsEditor recipients = (RecipientsEditor) Reflector.get(activity, "mRecipientsEditor");
                final MessageStatusListenerDecorator decorator = interceptWorkingMessageSend(activity);
                final EditText editor = (EditText) Reflector.get(activity, "mTextEditor");
                final Button send = (Button) Reflector.get(activity, "mSendButton");
                runTestOnUiThread(new Runnable() {
                    public void run() {
                        recipients.setNumbers(addresses);
                        recipients.populate(recipients.constructContactsFromInput());
                        editor.setText(getMessageAsLength(length));
                        send.performClick();
                    }
                });
                long timestamp = System.currentTimeMillis();
                while (!decorator.onMessageSent) {
                    delay();
                    if (System.currentTimeMillis() - timestamp > MAX_SEND_TIME_LIMIT) {
                        throw new Exception("Too long time for sending");
                    }
                }
                dump(description, timestamp);
            } finally {
                activity.finish();
            }
        } finally {
            delay(2000);
            clearSms(Threads.getOrCreateThreadId(mContext, new TreeSet<String>(addresses)));
        }
    }

    private MessageStatusListenerDecorator interceptWorkingMessageSend(ComposeMessageActivity activity)
    throws Throwable {
        MessageStatusListenerDecorator decorator = new MessageStatusListenerDecorator(activity);
        final WorkingMessage message = (WorkingMessage) Reflector.get(activity, "mWorkingMessage");
        /* set the interceptor to the final field */
        Reflector.set(message, "mStatusListener", decorator);
        return decorator;
    }

    private static String getMessageAsLength(int length) {
        StringBuilder string = new StringBuilder("Just for test");
        for (int i = string.length(); i < length; ++i) {
            string.append('.');
        }
        return string.toString();
    }

    /**
     * Tests 140B sms send out time.
     * @throws Throwable if error occurs.
     */
    public void test3SmsSendOutTime() throws Throwable {
        List<String> addresses = new ArrayList<String>();
        addresses.add(TEST_ADDRESS);
        testSmsSendOutTime("testSmsSendOutTime", addresses, 140);
    }

    /**
     * Tests 300B concatenated sms send out time.
     * @throws Throwable if error occurs.
     */
    public void test4ConcatenatedSmsSendOutTime() throws Throwable {
        List<String> addresses = new ArrayList<String>();
        addresses.add(TEST_ADDRESS);
        testSmsSendOutTime("testConcatenatedSmsSendOutTime", addresses, 300);
    }

    /**
     * Tests group sms which send to 20 recipients send out time.
     * @throws Throwable if error occurs.
     */
    public void test5GroupSmsSendOutTime() throws Throwable {
        List<String> addresses = new ArrayList<String>();
        /* 20 addresses, ++TEST_ADDRESS */
        int base = Integer.parseInt(TEST_ADDRESS);
        for (int i = 0; i < 20; ++i) {
            addresses.add(String.valueOf(base++));
        }
        testSmsSendOutTime("testGroupSmsSendOutTime", addresses, 140);
    }

    /**
     * Tests insert attachment time.
     * @throws Throwable if error occurs.
     */
    public void test6InsertAttachmentTime() throws Throwable {
        Intent intent = ComposeMessageActivity.createIntent(mContext, NEW_THREAD_ID);
        intent.setFlags(TEST_INTENT_FLAG);
        final ComposeMessageActivity activity = (ComposeMessageActivity) mInstrumentation.startActivitySync(intent);
        try {
            final WorkingMessage message = (WorkingMessage) Reflector.get(activity, "mWorkingMessage");
            final Uri uri = getTestImage(100);
            long timestamp = System.currentTimeMillis();
            runTestOnUiThread(new Runnable() {
                public void run() {
                    message.setAttachment(WorkingMessage.IMAGE, uri, false);
                }
            });
            while (!message.hasAttachment()) {
                delay();
            }
            dump("testInsertAttachmentTime", timestamp);
        } finally {
            activity.finish();
        }
    }

    private Uri getTestImage(int kb) throws Exception {
        File file = new File("/data/data/com.android.mms.tests/test_100kb.png");
        if (!file.exists()) {
            throw new FileNotFoundException(file.toString());
        }
        return Uri.fromFile(file);
    }

    /**
     * Tests delete only one message time.
     * @throws Throwable if error occurs.
     */
    public void test7DeleteMessageTime() throws Throwable {
        testDeleteMessagesTime(1);
    }

    /**
     * Tests delete 10 meesages time.
     * @throws Throwable if error occurs.
     */
    public void test8DeleteMessagesTime() throws Throwable {
        testDeleteMessagesTime(10);
    }

    private void testDeleteMessagesTime(int count) throws Throwable {
        final long threadId = prepareSms(getThreadId(), count);
        try {
            Intent intent = ComposeMessageActivity.createIntent(mContext, threadId);
            intent.setFlags(TEST_INTENT_FLAG);
            intent.setClass(mContext, ComposeMessageActivity.class);
            final ComposeMessageActivity activity = (ComposeMessageActivity) mInstrumentation.startActivitySync(intent);
            try {
                final BaseProgressQueryHandler handler = (BaseProgressQueryHandler) Reflector.get(activity, "mBackgroundQueryHandler");
                runTestOnUiThread(new Runnable() {
                    public void run() {
                        ConversationList.confirmDeleteThreadDialog(
                                new DeleteThreadListener(threadId, handler, activity),
                                false, false, activity);
                    }
                });
                Thread.yield();
                mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
                delay(1000);
                mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
                long timestamp = System.currentTimeMillis();
                while (Reflector.getInt(handler, "progress") != 1) {
                    delay();
                }
                dump("testDeleteMessage" + (count == 1 ? "" : "s") + "Time", timestamp);
            } finally {
                activity.finish();
            }
        } finally {
            clearSms(threadId);
        }
    }

    private void insertAttachmentForSend(ComposeMessageActivity activity, final Uri attachment) throws Throwable {
        final RecipientsEditor recipients = (RecipientsEditor) Reflector.get(activity, "mRecipientsEditor");
        final WorkingMessage message = (WorkingMessage) Reflector.get(activity, "mWorkingMessage");
        final List<String> addresses = new ArrayList<String>();
        addresses.add(TEST_ADDRESS);
        runTestOnUiThread(new Runnable() {
            public void run() {
                recipients.setNumbers(addresses);
                recipients.populate(recipients.constructContactsFromInput());
                message.setAttachment(WorkingMessage.IMAGE, attachment, false);
            }
        });
    }

    private void prepareMmsDraft() throws Throwable {
        Intent intent = ComposeMessageActivity.createIntent(mContext, NEW_THREAD_ID);
        intent.setFlags(TEST_INTENT_FLAG);
        final ComposeMessageActivity activity = (ComposeMessageActivity) mInstrumentation.startActivitySync(intent);
        try {
            delay(1000);
            insertAttachmentForSend(activity, getTestImage(300));
            delay(2000);
        } finally {
            activity.onKeyDown(KeyEvent.KEYCODE_BACK, null);
            delay(2000);
        }
    }

    private void waitForAttachmentShown(ComposeMessageActivity activity) throws Throwable {
        AttachmentEditor editor = (AttachmentEditor) Reflector.get(activity, "mAttachmentEditor");
        ImageAttachmentView view = (ImageAttachmentView) Reflector.get(editor, "mView");
        while (view.getVisibility() != View.VISIBLE ||
                ((ImageView) Reflector.get(view, "mImageView")).getDrawable() == null) {
            delay();
        }
    }

    /**
     * Tests mms thread open time.
     * @throws Throwable if error occurs.
     */
    public void test9MmsThreadOpenTime() throws Throwable {
        prepareMmsDraft();
        Intent intent = ComposeMessageActivity.createIntent(mContext, getThreadId());
        intent.setFlags(TEST_INTENT_FLAG);
        ComposeMessageActivity activity = (ComposeMessageActivity) mInstrumentation.startActivitySync(intent);
        try {
            /* ignore the first sample */
            delay(8000);
        } finally {
            activity.finish();
        }
        delay(1000);
        long timestamp = System.currentTimeMillis();
        activity = (ComposeMessageActivity) mInstrumentation.startActivitySync(intent);
        WorkingMessage message = (WorkingMessage) Reflector.get(activity, "mWorkingMessage");
        try {
            waitForAttachmentShown(activity);
            dump("testMmsThreadOpenTime", timestamp);
        } finally {
            message.discard();
            activity.finish();
        }
    }

    private int changeNetworkMode(int mode) throws Throwable {
        int preview = Secure.getInt(mContext.getContentResolver(), Secure.PREFERRED_NETWORK_MODE);
        if (preview != mode) {
            Secure.putInt(mContext.getContentResolver(), Secure.PREFERRED_NETWORK_MODE, mode);
            delay(4000);
        }
        return preview;
    }

    /**
     * Tests mms send out time by WCDMA network.
     * @throws Throwable if error occurs.
     */
    public void testAMmsSendOutTimeByWcdma() throws Throwable {
        testMmsSendOutTimeByNetworkMode("testMmsSendOutTimeByWcdma", Phone.NT_MODE_WCDMA_ONLY);
    }

    /**
     * Tests mms send out time by GSM network.
     * @throws Throwable if error occurs.
     */
    public void testBMmsSendOutTimeByGsm() throws Throwable {
        testMmsSendOutTimeByNetworkMode("testMmsSendOutTimeByGsm", Phone.NT_MODE_GSM_ONLY);
    }

    private void testMmsSendOutTimeByNetworkMode(String description, int mode) throws Throwable {
        int preview = changeNetworkMode(mode);
        try {
            Intent intent = ComposeMessageActivity.createIntent(mContext, NEW_THREAD_ID);
            intent.setFlags(TEST_INTENT_FLAG);
            final ComposeMessageActivity activity = (ComposeMessageActivity) mInstrumentation.startActivitySync(intent);
            delay(1000);
            final BaseProgressQueryHandler handler = (BaseProgressQueryHandler) Reflector.get(activity, "mBackgroundQueryHandler");
            try {
                insertAttachmentForSend(activity, getTestImage(100));
                delay(2000);
                MessageStatusListenerDecorator decorator = interceptWorkingMessageSend(activity);
                final Button send = (Button) Reflector.get(activity, "mSendButton");
                runTestOnUiThread(new Runnable() {
                    public void run() {
                        send.performClick();
                    }
                });
                long timestamp = System.currentTimeMillis();
                while (!decorator.onMessageSent) {
                    delay();
                    if (System.currentTimeMillis() - timestamp > MAX_SEND_TIME_LIMIT) {
                        throw new Exception("Too long time for sending");
                    }
                }
                dump(description, timestamp);
            } finally {
                delay(1000);
                /* avoid dismiss the dialog that has not shown */
                handler.setMax(2);
                Conversation.startDelete(handler, ConversationList.DELETE_CONVERSATION_TOKEN, true, getThreadId());
                delay(1000);
                activity.finish();
            }
        } finally {
            changeNetworkMode(preview);
        }
    }
}
