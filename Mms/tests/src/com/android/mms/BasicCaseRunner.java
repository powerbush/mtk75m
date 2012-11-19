package com.android.mms;

import junit.framework.TestSuite;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony.Threads;
import android.test.InstrumentationTestCase;
import android.test.InstrumentationTestRunner;

import com.android.mms.data.Conversation;
import com.android.mms.data.WorkingMessage;
import com.android.mms.data.WorkingMessage.MessageStatusListener;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.ui.ConversationList;
import com.android.mms.ui.ConversationList.BaseProgressQueryHandler;
import com.android.mms.ui.MultiDeleteActivity;
import com.android.mms.util.DraftCache;
import com.android.mms.util.Reflector;

public class BasicCaseRunner extends InstrumentationTestRunner {
    private static final long DELAY_TIME = 4000;
    
    @Override
    public TestSuite getTestSuite() {
        TestSuite suite = new TestSuite();
        switch (SIMInfo.getInsertedSIMCount(getContext())) {
        case 1:
            suite.addTestSuite(SmsBasicCase.class);
            suite.addTestSuite(MmsBasicCase.class);
            break;
        case 2:
            suite.addTestSuite(GeminiSmsBasicCase.class);
            suite.addTestSuite(GeminiMmsBasicCase.class);
            break;
        default:
            break;
        }
        suite.addTestSuite(ConversationListTestCase.class);
        suite.addTestSuite(ComposeMessageActivityTestCase.class);
        suite.addTestSuite(SlideEditorTestCase.class);
        return suite;
    }
    
    public static void clearThreads(Instrumentation instrumentation) throws Throwable {
        Intent intent = new Intent();
        intent.setClass(instrumentation.getTargetContext(), MultiDeleteActivity.class);
        intent.setFlags(BasicCase.TEST_INTENT_FLAG);
        MultiDeleteActivity activity = (MultiDeleteActivity) instrumentation.startActivitySync(intent);
        try {
            BasicCase.delay(DELAY_TIME);
            BaseProgressQueryHandler handler = (BaseProgressQueryHandler) Reflector.get(activity, "mQueryHandler");
            /* avoid dismiss the dialog that has not shown */
            handler.setMax(2);
            Conversation.startDeleteAll(handler, ConversationList.DELETE_CONVERSATION_TOKEN, true);
            BasicCase.delay(DELAY_TIME);
            DraftCache.getInstance().refresh();
        } finally {
            activity.finish();
        }
    }

    public static class AutoTestNotSupportedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    public static class BasicCase extends InstrumentationTestCase {
        protected static final int TEST_INTENT_FLAG = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
        protected static final long NEW_THREAD_ID = 0;
        protected static final String TEST_ADDRESS = "12345";
        protected static final long DEFAULT_FREQUENCY_DELAY_TIME = 10;
        protected static final long DELAY_TIME = 2000;
        protected static final long SEND_DURATION_LIMIT = 20 * 1000;
        protected Instrumentation mInstrumentation;
        protected Context mContext;
        protected static ComposeMessageActivity mActivity;

        @Override
        protected void setUp() throws Exception {
            super.setUp();
            mInstrumentation = getInstrumentation();
            mContext = mInstrumentation.getTargetContext();
        }
        
        protected long getThreadId() throws Throwable {
            return Threads.getOrCreateThreadId(mContext, TEST_ADDRESS);
        }
        
        protected void startComposeMessageActivitySync(long threadId) throws Throwable {
            Intent intent = ComposeMessageActivity.createIntent(mContext, threadId);
            intent.setFlags(TEST_INTENT_FLAG);
            mActivity = (ComposeMessageActivity) mInstrumentation.startActivitySync(intent);
        }
        
        protected void finishComposeMessageActivity() {
            if (mActivity != null) {
                mActivity.finish();
                mActivity = null;
            }
        }
        
        protected static void delay() throws InterruptedException {
            delay(DEFAULT_FREQUENCY_DELAY_TIME);
        }
        
        protected static void delay(long time) throws InterruptedException {
            Thread.sleep(time);
        }
        
        protected void inputKeyEventSequence(int... keys) throws InterruptedException {
            for (int key : keys) {
                mInstrumentation.sendKeyDownUpSync(key);
                delay(DELAY_TIME);
            }
        }
        
        protected MessageStatusListenerDecorator interceptWorkingMessageSend()
        throws Throwable {
            MessageStatusListenerDecorator decorator = new MessageStatusListenerDecorator(mActivity);
            final WorkingMessage message = (WorkingMessage) Reflector.get(mActivity, "mWorkingMessage");
            Reflector.set(message, "mStatusListener", decorator);
            return decorator;
        }
    }
    
    protected static class MessageStatusListenerDecorator implements MessageStatusListener {
        private final MessageStatusListener decoratee;
        protected volatile boolean onMessageSent;
        
        public MessageStatusListenerDecorator(MessageStatusListener decoratee) {
            this.decoratee = decoratee;
        }

        public void onAttachmentChanged() {
            decoratee.onAttachmentChanged();
        }

        public void onAttachmentError(int error) {
            decoratee.onAttachmentError(error);
        }

        public void onMaxPendingMessagesReached() {
            decoratee.onMaxPendingMessagesReached();
        }

        public void onMessageSent() {
            decoratee.onMessageSent();
            onMessageSent = true;
        }

        public void onPreMessageSent() {
            decoratee.onPreMessageSent();
        }

        public void onProtocolChanged(boolean mms, boolean needToast) {
            decoratee.onProtocolChanged(mms, needToast);
        }
    }
}