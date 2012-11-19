package com.android.mms;

import android.content.Intent;
import android.net.Uri;
import android.view.KeyEvent;

import com.android.mms.MmsOperationTestRunner.MmsOperationTestCase;
import com.android.mms.data.WorkingMessage;
import com.android.mms.model.LayoutModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.ui.SlideEditorActivity;
import com.android.mms.util.Reflector;

public class SlideEditorTestCase extends MmsOperationTestCase {
    private static SlideEditorActivity sActivity;

    private void startSlideEditorActivity() throws Throwable {
        Intent intent = ComposeMessageActivity.createIntent(mContext, 0);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ComposeMessageActivity cma = (ComposeMessageActivity) mInstrumentation.startActivitySync(intent);
        try {
            WorkingMessage msg = (WorkingMessage) Reflector.get(cma, "mWorkingMessage");
            Uri uri = msg.saveAsMms(false);
            intent = new Intent(mContext, SlideEditorActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            intent.setData(uri);
            sActivity = (SlideEditorActivity) mInstrumentation.startActivitySync(intent);
            delay(DELAY_TIME);
        } finally {
            cma.finish();
        }
    }

    private void finishSlideEditorActivity() throws Throwable {
        if (sActivity != null) {
            sActivity.finish();
            sActivity = null;
        }
    }

    private void invokeOptionMenu(int id) throws InterruptedException {
        mInstrumentation.invokeMenuActionSync(sActivity, id, 0);
        delay(DELAY_TIME);
    }

    public void test1OptionMenuPreview() throws Throwable {
        startSlideEditorActivity();
        invokeOptionMenu(11);
        sendKeysDownUpSync(KeyEvent.KEYCODE_BACK);
    }

    public void test2OptionMenuAddSlide() throws Throwable {
        int p1 = Reflector.getInt(sActivity, "mPosition");
        invokeOptionMenu(7);
        int p2 = Reflector.getInt(sActivity, "mPosition");
        assertEquals(p2 - p1, 1);
    }

    public void test3OptionMenuDuration() throws Throwable {
        int position = Reflector.getInt(sActivity, "mPosition");
        invokeOptionMenu(10);
        sendKeysDownUpSync(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_CENTER);
        SlideshowModel model = (SlideshowModel) Reflector.get(
                Reflector.get(sActivity, "mSlideshowEditor"), "mModel");
        assertEquals(model.get(position).getDuration(), 1000);
    }

    public void test4OptionMenuLayout() throws Throwable {
        try {
            invokeOptionMenu(9);
            sendKeysDownUpSync(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_CENTER);
            assertEquals(LayoutModel.getLayoutType(), LayoutModel.LAYOUT_TOP_TEXT);
        } finally {
            finishSlideEditorActivity();
        }
    }
}

