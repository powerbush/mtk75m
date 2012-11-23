package com.android.phone.tests;

import android.content.Intent;
import com.android.phone.InCallScreen;
import android.app.Instrumentation;
import android.view.KeyEvent;
import android.net.Uri;
import android.util.Log;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.ITelephony;
import android.os.ServiceManager;
import android.os.RemoteException;
import com.android.internal.telephony.CallManager;
import android.provider.Settings;

public class InCallScreenTest2 extends
		ActivityInstrumentationTestCase2<InCallScreen> {

	private Instrumentation mInstrumentation;
	private InCallScreen mActivityOne;
	private InCallScreen mActivityTwo;
	private Context mContext;
	private static final String TAG = "ConferenceCallTestCase";

	public InCallScreenTest2() {
		super("com.android.phone", InCallScreen.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
		mInstrumentation = getInstrumentation();
		mContext = mInstrumentation.getTargetContext();
		//
	}

	private InCallScreen getInCallScreenActivity() {
		Log .d( TAG, "===============================================================================");
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

	public void testInCallScreenMenuEndAll() {
		try {
			Thread.sleep(7000);
		} catch (Exception e) {
			//
		}
		mActivityOne = getInCallScreenActivity();
		Log .d( TAG, "===============================================================================");
		Log.d(TAG, "mActivityOne:" + mActivityOne);
		try {
			Thread.sleep(2000);
		} catch (Exception e) {
			//
		}
		mActivityOne.finishForTest();
		try {
			Thread.sleep(2000);
		} catch (Exception e) {
			//
		}
		mActivityTwo = getInCallScreenActivity();
		Log.d(TAG, "===============================================================================");
		Log.d(TAG, "mActivityTwo:" + mActivityTwo);
		try {
			Thread.sleep(2000);
		} catch (Exception e) {
			//
		}
		Log.d(TAG, "===============================================================================");
		Log.d(TAG, "setActivity mActivityTwo:" + mActivityTwo);
		setActivity(mActivityTwo);
		Log.d(TAG, "===============================================================================");
		Log.d(TAG, "sendKeys: KeyEvent.KEYCODE_MENU");
		sendKeys(KeyEvent.KEYCODE_MENU);
		try {
			Thread.sleep(500);
		} catch (Exception e) {
			//
		}
		sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
		sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
		try {
			Thread.sleep(500);
		} catch (Exception e) {
			//
		}
		Log.d(TAG, "===============================================================================");
		Log.d(TAG, "sendKeys: KeyEvent.KEYCODE_DPAD_DOWN");
		sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
		try {
			Thread.sleep(2000);
		} catch (Exception e) {
			//
		}
		boolean isIdle = false;
		try {
			ITelephony phone = ITelephony.Stub.asInterface(ServiceManager
					.checkService("phone"));
			if (phone != null)
				isIdle = phone.isIdle();
		} catch (RemoteException e) {
			Log.w(TAG, "RemoteException:" + e);
		}
		assertEquals(true, isIdle);
		mActivityTwo.finishForTest();
	}

	public void testInCallScreenMenuEndHeld() {
		try {
			Thread.sleep(7000);
		} catch (Exception e) {
			//
		}
		mActivityOne = getInCallScreenActivity();
		Log.d(TAG, "===============================================================================");
		Log.d(TAG, "mActivityOne:" + mActivityOne);
		try {
			Thread.sleep(2000);
		} catch (Exception e) {
			//
		}
		mActivityOne.finishForTest();
		try {
			Thread.sleep(2000);
		} catch (Exception e) {
			//
		}
		mActivityTwo = getInCallScreenActivity();
		Log.d(TAG, "===============================================================================");
		Log.d(TAG, "mActivityTwo:" + mActivityTwo);
		try {
			Thread.sleep(2000);
		} catch (Exception e) {
			//
		}
		Log.d(TAG, "===============================================================================");
		Log.d(TAG, "setActivity mActivityTwo:" + mActivityTwo);
		setActivity(mActivityTwo);
		Log.d(TAG, "===============================================================================");
		Log.d(TAG, "sendKeys: KeyEvent.KEYCODE_MENU");
		sendKeys(KeyEvent.KEYCODE_MENU);
		try {
			Thread.sleep(500);
		} catch (Exception e) {
			//
		}
		sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
		sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
		sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
		try {
			Thread.sleep(500);
		} catch (Exception e) {
			//
		}
		Log.d(TAG, "===============================================================================");
		Log.d(TAG, "sendKeys: KeyEvent.KEYCODE_DPAD_DOWN");
		sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
		try {
			Thread.sleep(2000);
		} catch (Exception e) {
			//
		}
		boolean isOnlyFgCall = false;
		CallManager cm = CallManager.getInstance();
		isOnlyFgCall = cm.hasActiveFgCall() && !cm.hasActiveBgCall();
		assertEquals(true, isOnlyFgCall);
		try {
			Thread.sleep(500);
		} catch (Exception e) {
			//
		}
		try {
			ITelephony phone = ITelephony.Stub.asInterface(ServiceManager
					.checkService("phone"));
			if (phone != null)
				phone.endCall();
		} catch (RemoteException e) {
			Log.w(TAG, "RemoteException:" + e);
		}
		mActivityTwo.finishForTest();
	}

	public void testTurnOnFlightModeEndMO() {
		try {
			Thread.sleep(7000);
		} catch (Exception e) {
			//
		}
		mActivityOne = getInCallScreenActivity();
		Log .d( TAG, "===============================================================================");
		Log.d(TAG, "mActivityOne:" + mActivityOne);
		try {
			Thread.sleep(5000);
		} catch (Exception e) {
			//
		}
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 1);
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", true);
        mContext.sendBroadcast(intent);
		try {
			Thread.sleep(2000);
		} catch (Exception e) {
			//
		}
		boolean isIdle = false;
		try {
			ITelephony phone = ITelephony.Stub.asInterface(ServiceManager
					.checkService("phone"));
			if (phone != null)
				isIdle = phone.isIdle();
		} catch (RemoteException e) {
			Log.w(TAG, "RemoteException:" + e);
		}
		assertEquals(true, isIdle);
		try {
			Thread.sleep(500);
		} catch (Exception e) {
			//
		}
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0);
        Intent intentRadioOn = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intentRadioOn.putExtra("state", false);
        mContext.sendBroadcast(intentRadioOn);
		mActivityOne.finishForTest();
	}

}



