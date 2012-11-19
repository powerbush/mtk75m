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

import com.android.mms.R;
import com.android.mms.ui.MessageListAdapter.ColumnsMap;

import android.content.Context;
import android.database.Cursor;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;

/**
 * Various instrumentation tests for ComposeMessageActivity.
 */
public class ComposeMessageActivityTests
extends ActivityInstrumentationTestCase2<ComposeMessageActivity> {

    private Context mContext;

    private TextView mRecipientsView;
    private EditText mTextEditor;
    private MessageListView mMsgListView;
    private MessageListAdapter mMsgListAdapter;
    private ColumnsMap mColumnsMap;

    public ComposeMessageActivityTests() {
        super("com.android.mms", ComposeMessageActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getTargetContext();

        ComposeMessageActivity a = getActivity();
        mRecipientsView = (TextView)a.findViewById(R.id.recipients_editor);
        mTextEditor = (EditText)a.findViewById(R.id.embedded_text_editor);
        mMsgListView = (MessageListView)a.findViewById(R.id.history);
        mMsgListAdapter = (MessageListAdapter)mMsgListView.getAdapter();
    }

    class BoxChecker {
        private int[] mExpectedBoxStates;
        private boolean mDone;
        private String mError;

        public BoxChecker(int[] expectedBoxStates) {
            mExpectedBoxStates = expectedBoxStates;
            mDone = false;
            mError = null;
            mMsgListAdapter.setOnDataSetChangedListener(mDataSetChangedListener);
        }

        private final MessageListAdapter.OnDataSetChangedListener
        mDataSetChangedListener = new MessageListAdapter.OnDataSetChangedListener() {
            public void onDataSetChanged(MessageListAdapter adapter) {
                int count = adapter.getCount();
                if (count > 0) {
                    MessageItem item = getMessageItem(count - 1);   // get most recent

                    int boxId = item.getBoxId();

                    // is boxId a valid box?
                    boolean found = false;
                    boolean isLast = false;
                    for (int i = 0; i < mExpectedBoxStates.length; i++) {
                        if (mExpectedBoxStates[i] == boxId) {
                            found = true;
                            isLast = i == mExpectedBoxStates.length - 1;
                            break;
                        }
                    }
                    if (!found) {
                        setError("Unexpected box state");
                        return;
                    }
                    if (isLast) {
                        mDone = true;
                    }
               }
            }

            public void onContentChanged(MessageListAdapter adapter) {
            }
        };

        private void setError(String error) {
            mError = error;
            mDone = true;
        }

        public String getError() {
            return mError;
        }

        public boolean isDone() {
            return mDone;
        }

        private MessageItem getMessageItem(int index) {
            Cursor cursor = (Cursor)mMsgListAdapter.getItem(index);

            mColumnsMap = new MessageListAdapter.ColumnsMap(cursor);
            String type = cursor.getString(mColumnsMap.mColumnMsgType);
            long msgId = cursor.getLong(mColumnsMap.mColumnMsgId);

            MessageItem msgItem = mMsgListAdapter.getCachedMessageItem(type, msgId, cursor);

            return msgItem;
        }
}

    /**
     * Tests that a simple SMS message is successfully sent.
     */
    @LargeTest
    public void testSendMessage() throws Throwable {
        final ComposeMessageActivity a = getActivity();

        runTestOnUiThread(new Runnable() {
            public void run() {
                checkFocused(mRecipientsView);
                mRecipientsView.setText("2012130903");
                mTextEditor.setText("This is a test message");
                Button send = (Button)a.findViewById(R.id.send_button);
                send.performClick();
            }
        });

        // Now poll while watching the adapter to see if the message got sent
        BoxChecker boxChecker = new BoxChecker(new int[] {4, 2});    // outbox, sent
        long now = System.currentTimeMillis();
        boolean success = true;
        while (!boxChecker.isDone()) {
            Thread.sleep(1000);
            if (System.currentTimeMillis() - now > 10000) {
                // Give up after ten seconds
                success = false;
                break;
            }
        }
        assertTrue(success && boxChecker.getError() == null);
    }

    /**
     * Helper method to verify which field has the focus
     * @param focused The view that should be focused (all others should not have focus)
     */
    @SmallTest
    private void checkFocused(View focused) {
        assertEquals(focused == mRecipientsView, mRecipientsView.isFocused());
        assertEquals(focused == mTextEditor, mTextEditor.isFocused());
    }

}
