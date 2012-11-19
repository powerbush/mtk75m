package com.android.mms;

import junit.framework.TestSuite;
import android.app.Instrumentation;
import android.content.Context;
import android.provider.Telephony.SIMInfo;
import android.test.InstrumentationTestCase;
import android.test.InstrumentationTestRunner;

public class MmsOperationTestRunner extends InstrumentationTestRunner {
    @Override
    public TestSuite getAllTests() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(ComposeMessageActivityTestCase.class);
        suite.addTestSuite(ConversationListTestCase.class);
        suite.addTestSuite(SlideEditorTestCase.class);
        return suite;
    }
    
    public static abstract class MmsOperationTestCase extends InstrumentationTestCase {
        protected static final long DELAY_TIME = 2000;
        protected SIMInfo mSim;
        protected Instrumentation mInstrumentation;
        protected Context mContext;
        
        @Override
        protected void setUp() throws Exception {
            super.setUp();
            mInstrumentation = getInstrumentation();
            mContext = mInstrumentation.getTargetContext();
            mSim = SIMInfo.getInsertedSIMList(mContext).get(0);
        }

        protected static void delay(long time) throws InterruptedException {
            Thread.sleep(time);
        }

        protected void sendKeysDownUpSync(int... keys) throws InterruptedException {
            for (int key : keys) {
                mInstrumentation.sendKeyDownUpSync(key);
                delay(DELAY_TIME);
            }
        }
    }
}
