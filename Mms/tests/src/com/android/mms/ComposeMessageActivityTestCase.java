package com.android.mms;

import android.content.Intent;
import android.database.Cursor;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import com.android.mms.MmsOperationTestRunner.MmsOperationTestCase;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.ui.MessageListView;
import com.android.mms.util.Reflector;

public class ComposeMessageActivityTestCase extends MmsOperationTestCase {
    private static final String ADDRESS = "12345";
    private static ComposeMessageActivity sActivity;
    private static String sContent;

    private void prepareMessage() throws Throwable {
        long threadId = Threads.getOrCreateThreadId(mContext, ADDRESS);
        Sms.addMessageToUri(mContext.getContentResolver(), Sms.CONTENT_URI,
                ADDRESS, "Test message content", "Test", System.currentTimeMillis(),
                true, false, threadId, (int) mSim.mSimId);
    }

    private void startComposeMessageActivity() throws Throwable {
        Intent intent = ComposeMessageActivity.createIntent(mContext,
                Threads.getOrCreateThreadId(mContext, ADDRESS));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        sActivity = (ComposeMessageActivity) mInstrumentation.startActivitySync(intent);
        delay(DELAY_TIME);
    }

    private void finishComposeMessageActivity() throws Throwable {
        if (sActivity != null) {
            sActivity.finish();
            sActivity = null;
        }
    }

    public void test01OptionMenuAddSubject() throws Throwable {
        BasicCaseRunner.clearThreads(mInstrumentation);
        prepareMessage();
        startComposeMessageActivity();
        mInstrumentation.invokeMenuActionSync(sActivity, 0, 0);
        delay(DELAY_TIME);
        assertTrue(((EditText) Reflector.get(sActivity, "mSubjectTextEditor")).getVisibility() == View.VISIBLE);
    }

    public void test02OptionMenuDelete() throws Throwable {
        mInstrumentation.invokeMenuActionSync(sActivity, 1, 0);
        delay(DELAY_TIME);
        sendKeysDownUpSync(KeyEvent.KEYCODE_BACK);
    }

    public void test03OptionMenuQuickText() throws Throwable {
        mInstrumentation.invokeMenuActionSync(sActivity, 8, 0);
        delay(DELAY_TIME);
        sendKeysDownUpSync(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_CENTER);
        EditText text = (EditText) Reflector.get(sActivity, "mTextEditor");
        sContent = text.getText().toString();
        assertFalse(sContent.isEmpty());
    }
    
    public void test04OptionMenuAttach() throws Throwable {
        mInstrumentation.invokeMenuActionSync(sActivity, 2, 0);
        delay(DELAY_TIME);
        assertNotNull(Reflector.get(sActivity, "mAttachmentTypeSelectorAdapter"));
        sendKeysDownUpSync(KeyEvent.KEYCODE_BACK);
    }
    
    public void test05OptionMenuInsertSmiley() throws Throwable {
        mInstrumentation.invokeMenuActionSync(sActivity, 26, 0);
        delay(DELAY_TIME);
        sendKeysDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_CENTER);
        EditText text = (EditText) Reflector.get(sActivity, "mTextEditor");
        assertFalse(sContent.equals(text.getText().toString()));
    }
    
    public void test06OptionMenuAllThread() throws Throwable {
        mInstrumentation.invokeMenuActionSync(sActivity, 6, 0);
        delay(DELAY_TIME);
        startComposeMessageActivity();
    }
    
    public void test07OptionMenuNewContact() throws Throwable {
        mInstrumentation.invokeMenuActionSync(sActivity, 27, 0);
        delay(DELAY_TIME);
        sendKeysDownUpSync(KeyEvent.KEYCODE_BACK);
    }

    private void invokeContextMenuView(int id) throws Throwable {
        final MessageListView msgListView = (MessageListView) Reflector.get(sActivity, "mMsgListView");
        sActivity.runOnUiThread(new Runnable() {
            public void run() {
                msgListView.requestFocus();
            }
        });
        delay(DELAY_TIME);
        mInstrumentation.invokeContextMenuAction(sActivity, id, 0);
        delay(DELAY_TIME);
    }

    public void test08TapLockMessage() throws Throwable {
        invokeContextMenuView(28);
        Cursor c = sActivity.mMsgListAdapter.getCursor();
        boolean lock = Reflector.getBoolean(
                sActivity.mMsgListAdapter.getCachedMessageItem(c.getString(0), c.getLong(1), c),
                "mLocked");
        assertTrue(lock);
    }

    public void test09TapForward() throws Throwable {
        invokeContextMenuView(21);
        sendKeysDownUpSync(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_DPAD_CENTER);
    }

    public void test10TapViewDetails() throws Throwable {
        invokeContextMenuView(17);
        sendKeysDownUpSync(KeyEvent.KEYCODE_BACK);
    }

    public void test11TapSaveToSim() throws Throwable {
        Object obj = Reflector.get(sActivity, "mSaveMsgThread");
        invokeContextMenuView(31);
        if (SIMInfo.getInsertedSIMCount(mContext) == 2) {
            sendKeysDownUpSync(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_CENTER);
        }
        assertTrue(obj != Reflector.get(sActivity, "mSaveMsgThread"));
    }

    public void test12TapDelete() throws Throwable {
        try {
            invokeContextMenuView(18);
            sendKeysDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_CENTER);
        } finally {
            finishComposeMessageActivity();
            delay(DELAY_TIME);
            sendKeysDownUpSync(KeyEvent.KEYCODE_BACK);
            BasicCaseRunner.clearThreads(mInstrumentation);
        }
    }
}

