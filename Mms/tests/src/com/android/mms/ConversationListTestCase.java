package com.android.mms;

import android.content.Intent;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.view.KeyEvent;

import com.android.mms.MmsOperationTestRunner.MmsOperationTestCase;
import com.android.mms.ui.ConversationList;

public class ConversationListTestCase extends MmsOperationTestCase {
    private static final String ADDRESS = "12345";
    private static ConversationList sActivity;

    private void prepareMessage() throws Throwable {
        long threadId = Threads.getOrCreateThreadId(mContext, ADDRESS);
        Sms.addMessageToUri(mContext.getContentResolver(), Sms.CONTENT_URI,
                ADDRESS, "Test message content", "TEST", System.currentTimeMillis(),
                true, false, threadId, (int) mSim.mSimId);
    }

    private void startConversationList() throws Throwable {
        Intent intent = new Intent();
        intent.setClass(mContext, ConversationList.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        sActivity = (ConversationList) mInstrumentation.startActivitySync(intent);
        delay(DELAY_TIME);
    }

    private void finishConversationList() throws Throwable {
        if (sActivity != null) {
            sActivity.finish();
            sActivity = null;
        }
    }

    public void test1OptionMenuDelete() throws Throwable {
        BasicCaseRunner.clearThreads(mInstrumentation);
        prepareMessage();
        startConversationList();
        mInstrumentation.invokeMenuActionSync(sActivity, ConversationList.MENU_DELETE_ALL, 0);
        delay(DELAY_TIME);
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        delay(DELAY_TIME);
    }

    public void test2OptionMenuSettings() throws Throwable {
        mInstrumentation.invokeMenuActionSync(sActivity, ConversationList.MENU_PREFERENCES, 0);
        delay(DELAY_TIME);
        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        delay(DELAY_TIME);
    }
    
    public void test3OptionMenuSearch() throws Throwable {
        mInstrumentation.invokeMenuActionSync(sActivity, ConversationList.MENU_SEARCH, 0);
        delay(DELAY_TIME);
        mInstrumentation.sendStringSync("Test");
        delay(DELAY_TIME);
        sendKeysDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_BACK);
    }
    
    public void test4OptionMenuCompose() throws Throwable {
        mInstrumentation.invokeMenuActionSync(sActivity, ConversationList.MENU_COMPOSE_NEW, 0);
        delay(DELAY_TIME);
        sendKeysDownUpSync(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BACK);
    }

    public void invokeContextMenuView(int menu) throws Throwable {
        sendKeysDownUpSync(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN);
        mInstrumentation.invokeContextMenuAction(sActivity, menu, 0);
        delay(DELAY_TIME);
    }

    public void test5ContextMenuView() throws Throwable {
        invokeContextMenuView(ConversationList.MENU_VIEW);
        sendKeysDownUpSync(KeyEvent.KEYCODE_BACK);
    }

    public void test6ContextMenuNewContact() throws Throwable {
        invokeContextMenuView(ConversationList.MENU_ADD_TO_CONTACTS);
        sendKeysDownUpSync(KeyEvent.KEYCODE_BACK);
    }

    public void test7TapEnterThread() throws Throwable {
        sendKeysDownUpSync(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_BACK);
    }

    public void test8ContextMenuDelete() throws Throwable {
        try {
            invokeContextMenuView(ConversationList.MENU_DELETE);
            sendKeysDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
        } finally {
            finishConversationList();
            delay(DELAY_TIME);
            BasicCaseRunner.clearThreads(mInstrumentation);
        }
    }
}

