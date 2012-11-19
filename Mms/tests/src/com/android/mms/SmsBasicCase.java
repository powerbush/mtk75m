package com.android.mms;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.database.Cursor;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony.Sms;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;

import com.android.mms.BasicCaseRunner.BasicCase;
import com.android.mms.BasicCaseRunner.MessageStatusListenerDecorator;
import com.android.mms.ui.MessagingPreferenceActivity;
import com.android.mms.ui.RecipientsEditor;
import com.android.mms.util.Reflector;

public class SmsBasicCase extends BasicCase {
    protected boolean checkSims() {
        return SIMInfo.getInsertedSIMCount(mContext) == 1;
    }
    
    protected void clearSms(long threadId) throws Throwable {
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
    
    public void test01Settings() throws Throwable {
        if (!checkSims()) {
            return;
        }
        
        Intent intent = new Intent();
        intent.setFlags(TEST_INTENT_FLAG);
        intent.setClass(mContext, MessagingPreferenceActivity.class);
        MessagingPreferenceActivity activity = (MessagingPreferenceActivity) mInstrumentation.startActivitySync(intent);
        try {
            delay(DELAY_TIME);
            mInstrumentation.invokeMenuActionSync(activity, 1, 0);
            delay(DELAY_TIME);
            setting();
        } finally {
            activity.finish();
        }
    }
    
    protected void setting() throws Throwable {
        inputKeyEventSequence(
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_CENTER);
    }
    
    public void test02InputRecipientsAndContent() throws Throwable {
        if (!checkSims()) {
            return;
        }
        
        startComposeMessageActivitySync(NEW_THREAD_ID);
        delay(DELAY_TIME);
        mInstrumentation.sendStringSync(TEST_ADDRESS);
        delay(DELAY_TIME);
        inputKeyEventSequence(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_DOWN);
        RecipientsEditor recipients = (RecipientsEditor) Reflector.get(mActivity, "mRecipientsEditor");
        assertEquals(TEST_ADDRESS, recipients.getNumbers().get(0));
        inputMessageContent();
    }
    
    protected void inputMessageContent() throws Throwable {
        final StringBuilder sb = new StringBuilder("Test");
        sb.append('\u5B59');
        sb.append("!@#$");
        sb.append("1234");
        final EditText editor = (EditText) Reflector.get(mActivity, "mTextEditor");
        runTestOnUiThread(new Runnable() {
            public void run() {
                editor.setText(sb);
            }
        });
        delay(DELAY_TIME);
        assertEquals(sb.toString(), editor.getText().toString());
    }
    
    public void test03SmsSentSuccessfully() throws Throwable {
        if (!checkSims()) {
            return;
        }
        
        /* the sms can be sent successfully */
        final MessageStatusListenerDecorator decorator = interceptWorkingMessageSend();
        sendSms();
        /* assert, wait a moment */
        long timestamp = System.currentTimeMillis();
        while (!decorator.onMessageSent) {
            if (System.currentTimeMillis() - timestamp > SEND_DURATION_LIMIT) {
                /* too long time */
                throw new Exception("Too long time for sending");
            }
            delay();
        }
        delay(DELAY_TIME);
    }
    
    protected void sendSms() throws Throwable {
        final Button send = (Button) Reflector.get(mActivity, "mSendButton");
        runTestOnUiThread(new Runnable() {
            public void run() {
                send.performClick();
            }
        });
    }
    
    public void test04DiscardTextBoxShow() throws Throwable {
        if (!checkSims()) {
            return;
        }
        
        finishComposeMessageActivity();
        clearSms(getThreadId());
        startComposeMessageActivitySync(NEW_THREAD_ID);
        delay(DELAY_TIME);
        final EditText editor = (EditText) Reflector.get(mActivity, "mTextEditor");
        runTestOnUiThread(new Runnable() {
            public void run() {
                editor.setText("Test");
            }
        });
        delay(DELAY_TIME);
        inputKeyEventSequence(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BACK);
    }
        
    public void test05SaveAsDraft() throws Throwable {
        if (!checkSims()) {
            return;
        }
        
        try {
            inputKeyEventSequence(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_UP);
            mInstrumentation.sendStringSync(TEST_ADDRESS);
            delay(DELAY_TIME);
            runTestOnUiThread(new Runnable() {
                public void run() {
                    mActivity.onKeyDown(KeyEvent.KEYCODE_BACK, null);
                }
            });
            delay(DELAY_TIME);
        } finally {
            BasicCaseRunner.clearThreads(mInstrumentation);
        }
    }
}
