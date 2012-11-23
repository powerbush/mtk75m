package com.android.phone;

import android.content.Context;
import android.util.Log;
import com.android.internal.telephony.Phone;
import android.provider.Telephony;


/**
 * Contains utility functions for getting framework resource
 */
public class Utils {


	static int getStatusResource(int state) {
		
		Log.i("Utils gemini", "!!!!!!!!!!!!!state is "+state);
		switch (state) {
		case Phone.SIM_INDICATOR_RADIOOFF:
			return com.mediatek.internal.R.drawable.sim_radio_off;
		case Phone.SIM_INDICATOR_LOCKED:
			return com.mediatek.internal.R.drawable.sim_locked;
		case Phone.SIM_INDICATOR_INVALID:
			return com.mediatek.internal.R.drawable.sim_invalid;
		case Phone.SIM_INDICATOR_SEARCHING:
			return com.mediatek.internal.R.drawable.sim_searching;
		case Phone.SIM_INDICATOR_ROAMING:
			return com.mediatek.internal.R.drawable.sim_roaming;
		case Phone.SIM_INDICATOR_CONNECTED:
			return com.mediatek.internal.R.drawable.sim_connected;
		case Phone.SIM_INDICATOR_ROAMINGCONNECTED:
			return com.mediatek.internal.R.drawable.sim_roaming_connected;
		default:
			return -1;
		}
	}
	
	static int getSimColorResource(int color) {
		
		if((color>=0)&&(color<=7)) {
			return Telephony.SIMBackgroundRes[color];
		} else {
			return -1;
		}

		
	}
	
	static boolean mSupport3G = false;
	static final int TYPE_VOICECALL = 1;
	static final int TYPE_VIDEOCALL = 2;
	static final int TYPE_SMS = 3;
	static final int TYPE_GPRS = 4;

}
