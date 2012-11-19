package com.android.mms;

import android.provider.Telephony.SIMInfo;
import android.view.KeyEvent;

public class GeminiMmsBasicCase extends MmsBasicCase {
    @Override
    protected boolean checkSims() {
        return SIMInfo.getInsertedSIMCount(mContext) == 2;
    }
    
    @Override
    public void test01MmsSendSuccessfully() throws Throwable {
        super.test01MmsSendSuccessfully();
    }
    
    @Override
    protected void sendMms() throws Throwable {
        super.sendMms();
        inputKeyEventSequence(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_CENTER);
    }
}
