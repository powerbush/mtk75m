package com.android.mms;

import java.io.File;
import java.io.FileNotFoundException;

import android.net.Uri;
import android.provider.Telephony.SIMInfo;
import android.view.KeyEvent;
import android.widget.Button;

import com.android.mms.BasicCaseRunner.BasicCase;
import com.android.mms.BasicCaseRunner.MessageStatusListenerDecorator;
import com.android.mms.data.WorkingMessage;
import com.android.mms.ui.MessageListView;
import com.android.mms.util.Reflector;

public class MmsBasicCase extends BasicCase {
    protected boolean checkSims() {
        return SIMInfo.getInsertedSIMCount(mContext) == 1;
    }
    
    public void test01MmsSendSuccessfully() throws Throwable {
        if (!checkSims()) {
            return;
        }
        
        startComposeMessageActivitySync(NEW_THREAD_ID);
        mInstrumentation.sendStringSync(TEST_ADDRESS);
        delay(DELAY_TIME);
        attach(attachment());
        delay(DELAY_TIME);
        MessageStatusListenerDecorator decorator = interceptWorkingMessageSend();
        sendMms();
        long timestamp = System.currentTimeMillis();
        while (!decorator.onMessageSent) {
            if (System.currentTimeMillis() - timestamp > SEND_DURATION_LIMIT) {
                throw new Exception("Too long time for sending");
            }
            delay();
        }
    }
    
    protected void sendMms() throws Throwable {
        final Button send = (Button) Reflector.get(mActivity, "mSendButton");
        runTestOnUiThread(new Runnable() {
            public void run() {
                send.performClick();
            }
        });
    }
    
    protected Uri attachment() throws FileNotFoundException {
        File file = new File("/data/data/com.android.mms.tests/test_100kb.png");
        if (!file.exists()) {
            throw new FileNotFoundException(file.toString());
        }
        return Uri.fromFile(file);
    }
    
    protected void attach(Uri uri) throws Throwable {
        final WorkingMessage message = (WorkingMessage) Reflector.get(mActivity, "mWorkingMessage");
        message.setAttachment(WorkingMessage.IMAGE, uri, false);
    }
    
    public void test02SavedIntoSdcard() throws Throwable {
        if (!checkSims()) {
            return;
        }
        
        /* the file can be saved into sdcard */
        if (!new File("/sdcard/").exists()) {
            throw new FileNotFoundException("No sdcard");
        }
        final MessageListView list = (MessageListView) Reflector.get(mActivity, "mMsgListView");
        runTestOnUiThread(new Runnable() {
            public void run() {
                list.requestFocus();
            }
        });
        delay(DELAY_TIME);
        mInstrumentation.invokeContextMenuAction(mActivity, 25, 0);
        delay(DELAY_TIME);
        inputKeyEventSequence(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_CENTER);
    }
    
    public void test03DeletedFromInbox() throws Throwable {
        if (!checkSims()) {
            return;
        }
        
        try {
            /* the message will be deleted from the inbox */
            final MessageListView list = (MessageListView) Reflector.get(mActivity, "mMsgListView");
            runTestOnUiThread(new Runnable() {
                public void run() {
                    list.requestFocus();
                }
            });
            delay(DELAY_TIME);
            mInstrumentation.invokeContextMenuAction(mActivity, 18, 0);
            delay(DELAY_TIME);
            inputKeyEventSequence(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_CENTER);
        } finally {
            BasicCaseRunner.clearThreads(mInstrumentation);
        }
    }
}
