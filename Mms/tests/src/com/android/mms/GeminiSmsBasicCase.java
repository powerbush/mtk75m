package com.android.mms;

import android.provider.Telephony.SIMInfo;
import android.view.KeyEvent;
import android.widget.EditText;

import com.android.mms.util.Reflector;

public class GeminiSmsBasicCase extends SmsBasicCase {
    @Override
    protected boolean checkSims() {
        return SIMInfo.getInsertedSIMCount(mContext) == 2;
    }
    
    @Override
    public void test01Settings() throws Throwable {
        super.test01Settings();
    }
    
    @Override
    protected void setting() throws Throwable {
        inputKeyEventSequence(
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_BACK);
    }
    
    @Override
    public void test02InputRecipientsAndContent() throws Throwable {
        super.test02InputRecipientsAndContent();
    }
    
    @Override
    protected void inputMessageContent() throws Throwable {
        mInstrumentation.invokeMenuActionSync(mActivity, 8, 0);
        delay(DELAY_TIME);
        inputKeyEventSequence(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_CENTER);
        EditText editor = (EditText) Reflector.get(mActivity, "mTextEditor");
        assertFalse(editor.toString().isEmpty());
    }
    
    @Override
    public void test03SmsSentSuccessfully() throws Throwable {
        /* not support */
        return;
    }
}
