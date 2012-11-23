package com.android.phone.tests;

//import com.android.contacts.DialtactsActivity;
import com.mediatek.android.performance.BasePerformanceTestCase;
import android.view.KeyEvent;
import com.android.phone.InCallScreen;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.CallManager;
import com.android.phone.PhoneApp;
import android.util.Log;
import com.android.internal.telephony.ITelephony;
import android.os.ServiceManager;
import android.os.RemoteException;

public class CallPerformanceTestCase extends BasePerformanceTestCase {

	private InCallScreen mInCallScreen;
	private Instrumentation mInstrumentation;
	private Context mContext;
	private static final String TAG = "CallPerformanceTestCase";

	protected void setUp() throws Exception {
		super.setUp();
		mInstrumentation = getInstrumentation();
		mContext = mInstrumentation.getTargetContext();
		Log.d(TAG, "End setUp()");
	}

	private InCallScreen getInCallActivity() {
		Log.d(TAG, "getInCallActivity() launch InCallScreen");
		Intent intent = new Intent(mContext, InCallScreen.class);
		intent.setData(Uri.fromParts("tel", "10086", null));
		intent.setAction(Intent.ACTION_CALL_PRIVILEGED);
		intent.putExtra(Phone.GEMINI_SIM_ID_KEY, 0);
		intent.putExtra("launch_from_dialer", true);
		intent.putExtra("launch_from_SipCallHandlerEx", false);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		return (InCallScreen) mInstrumentation.startActivitySync(intent);
	}

	public void testDTMFResponseTime() {
		mBindHelper.setDescription("The response time since tapping number on the DTMF dialpad until the number shows up on the screen");
		try {
			Thread.sleep(30000);
		} catch (Exception e) {
			//
		}
		mInCallScreen = getInCallActivity();
		try {
			Thread.sleep(18000);
		} catch (Exception e) {
			//
		}
		mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
		try {
			Thread.sleep(300);
		} catch (Exception e) {
			//
		}
		mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
		try {
			Thread.sleep(300);
		} catch (Exception e) {
			//
		}
		mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
		try {
			Thread.sleep(300);
		} catch (Exception e) {
			//
		}
		mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
		try {
			Thread.sleep(300);
		} catch (Exception e) {
			//
		}
		mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER);
		try {
			Thread.sleep(800);
		} catch (Exception e) {
			//
		}
		mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
		try {
			Thread.sleep(300);
		} catch (Exception e) {
			//
		}
		long DTMFtimeStart = System.currentTimeMillis();
		mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
		// mInstrumentation.waitForIdleSync();
		// long timeEnd = System.currentTimeMillis();
		long DTMFtimeEnd = mBindHelper.getTimeResult();
		long DTMFduration = DTMFtimeEnd - DTMFtimeStart;
		mBindHelper.dumpResultStr("DTMF_button_click_response_time\t"
				+ DTMFduration);
                try {
			Thread.sleep(800);
		} catch (Exception e) {
			//
		}
		// Button endButton = (Button) findViewById(R.id.endButton);
		// mInCallScreen.handleOnscreenButtonClick(com.android.phone.R.id.endButton);
		// mInCallScreen.internalHangup();
		// CallManager cm = PhoneApp.getInstance().mCM;
		// PhoneUtils.hangup(cm);
/*		try {
			Thread.sleep(800);
		} catch (Exception e) {
			//
		}
		mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
		try {
			Thread.sleep(300);
		} catch (Exception e) {
			//
		}
		mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
		try {
			Thread.sleep(300);
		} catch (Exception e) {
			//
		}
		mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
		try {
			Thread.sleep(300);
		} catch (Exception e) {
			//
		}

		mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
		try {
			Thread.sleep(300);
		} catch (Exception e) {
			//
		}
		mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER);

		try {
			Thread.sleep(300);
		} catch (Exception e) {
			//
		}

		mBindHelper.setDescription("The response time since tapping 'End Call' button until the in call screen dismisses");

		try {
			Thread.sleep(300);
		} catch (Exception e) {
			//
		}
		mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);

		try {
			Thread.sleep(300);
		} catch (Exception e) {
			//
		}
		mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);

		try {
			Thread.sleep(500);
		} catch (Exception e) {
			//
		}

		long endCalltimeStart = System.currentTimeMillis();
		Log.d(TAG, "test EndCall Screen Dismiss Time, send ENDCALL KEY");
		mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER);
		mInstrumentation.waitForIdleSync();
		long endCalltimeEnd = System.currentTimeMillis();
		// long endCalltimeEnd = mBindHelper.getTimeResult();
		long endCallduration = endCalltimeEnd - endCalltimeStart;
		mBindHelper.dumpResultStr("End_Call_screen_dismiss_time\t"
				+ endCallduration);

		try {
			Thread.sleep(500);
		} catch (Exception e) {
			//
		}*/
                try {
			ITelephony phone = ITelephony.Stub.asInterface(ServiceManager
					.checkService("phone"));
			if (phone != null) phone.endCall();
		} catch (RemoteException e) {
			Log.w(TAG, "RemoteException:" + e);
		}
		mInCallScreen.finishForTest(); 

	}

        public void testEndCallScreenDismissTime() {
            	mBindHelper.setDescription("The response time since tapping 'End Call' button until the in call screen dismisses");
		try {
			Thread.sleep(30000);
		} catch (Exception e) {
			//
		}
		mInCallScreen = getInCallActivity();
		try {
			Thread.sleep(2000);
		} catch (Exception e) {
			//
		}
		// setActivity(mInCallScreen);
		mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
		mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
		mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
                try {
			Thread.sleep(800);
		} catch (Exception e) {
			//
		}
		long endCalltimeStart = System.currentTimeMillis();
		Log.d(TAG, "test EndCall Screen Dismiss Time, send ENDCALL KEY");
		mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER);
		mInstrumentation.waitForIdleSync();
		long endCalltimeEnd = System.currentTimeMillis();
		// long endCalltimeEnd = mBindHelper.getTimeResult();
		long endCallduration = endCalltimeEnd - endCalltimeStart;
		mBindHelper.dumpResultStr("End_Call_screen_dismiss_time\t"
				+ endCallduration);
                try {
			Thread.sleep(1000);
		} catch (Exception e) {
			//
		}
        }

	/*
	 * public void testEndCallScreenDismissTime() {mBindHelper.setDescription(
	 * "The response time since tapping 'End Call' button until the in call screen dismisses 2"
	 * ); try{ Thread.sleep(12000); } catch(Exception e) { // } Log.d(TAG,
	 * "testEndCallScreenDismissTime(), begin to start InCallScreen:");
	 * mInCallScreen = getInCallActivity(); Log.d(TAG,
	 * "testEndCallScreenDismissTime(), mInCallScreen:" + mInCallScreen); try{
	 * Thread.sleep(6000); } catch(Exception e) { // }
	 * mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN); try{
	 * Thread.sleep(500); } catch(Exception e) { // }
	 * mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN); try{
	 * Thread.sleep(500); } catch(Exception e) { // }
	 * mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN); try{
	 * Thread.sleep(500); } catch(Exception e) { // } try{ Thread.sleep(2000); }
	 * catch(Exception e) { // } long timeStart = System.currentTimeMillis();
	 * mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENDCALL); Log.d(TAG,
	 * "testEndCallScreenDismissTime(), send ENDCALL KEY");
	 * //mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
	 * //mInstrumentation.waitForIdleSync(); //long timeEnd =
	 * System.currentTimeMillis(); long timeEnd = mBindHelper.getTimeResult();
	 * long duration = timeEnd - timeStart;
	 * mBindHelper.dumpResultStr("End_Call_screen_dismiss_time\t" + duration); }
	 */

	protected void tearDown() throws Exception {
		mInstrumentation = null;
		mContext = null;
		mInCallScreen = null;
		super.tearDown();
	}

}
