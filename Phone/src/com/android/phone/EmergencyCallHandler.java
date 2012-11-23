/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.phone;

import java.util.List;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.telephony.ServiceState;
import android.util.Log;
import android.view.WindowManager;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;

/**
 * Helper class used by the InCallScreen to handle certain special
 * cases when making an emergency call.
 *
 * Specifically, if the user tries to dial an emergency number but the
 * radio is off, e.g. if the device is in airplane mode, this class is
 * responsible for turning the radio back on and retrying the call.
 *
 * This class is initially launched using the same intent originally
 * passed to the InCallScreen (presumably an ACTION_CALL_EMERGENCY intent)
 * but with this class explicitly set as the className/component.  Later,
 * we retry the emergency call by firing off that same intent, with the
 * component cleared, and using an integer extra called
 * EMERGENCY_CALL_RETRY_KEY to convey information about the current state.
 */
public class EmergencyCallHandler extends Activity {
    private static final String TAG = "EmergencyCallHandler";
    private static final boolean DBG = true;  // OK to have this on by default

    /** the key used to get the count from our Intent's extra(s) */
    public static final String EMERGENCY_CALL_RETRY_KEY = "emergency_call_retry_count";
    
    /** count indicating an initial attempt at the call should be made. */
    public static final int INITIAL_ATTEMPT = -1;
    
    private static boolean dialing_ecc = false;
    
    /** number of times to retry the call and the time spent in between attempts*/
    public static final int NUMBER_OF_RETRIES = 0;
    public static final int TIME_BETWEEN_RETRIES_MS = 5000;
    
    // constant events
    private static final int EVENT_SERVICE_STATE_CHANGED = 100;
    private static final int EVENT_TIMEOUT_EMERGENCY_CALL = 200;
    
    private static final int EVENT_SERVICE_STATE_CHANGED2 = 300;

	private static EmergencyCallInfo sEci = null;

    /**
     * Package holding information needed for the callback.
     */
    private static class EmergencyCallInfo {
        public Phone phone;
        public Intent intent;
        public ProgressDialog dialog;
        public Application app;
    }
    
    /**
     * static handler class, used to handle the two relevent events. 
     */
    private static EmergencyCallEventHandler sHandler;
    private static class EmergencyCallEventHandler extends Handler {
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case EVENT_SERVICE_STATE_CHANGED: 
                case EVENT_SERVICE_STATE_CHANGED2: {
                        // make the initial call attempt after the radio is turned on.
                        ServiceState state = (ServiceState) ((AsyncResult) msg.obj).result;
                        if (DBG) Log.d(TAG, "EVENT_SERVICE_STATE_CHANGED: state = " + state);
                        if (state.getState() != ServiceState.STATE_POWER_OFF) {
                            EmergencyCallInfo eci = 
                                (EmergencyCallInfo) ((AsyncResult) msg.obj).userObj;
                            // deregister for the service state change events. 
                            // turn the radio on and listen for it to complete.
                            if (FeatureOption.MTK_GEMINI_SUPPORT == true)
                            {
                                if (dialing_ecc==true)
                                {
                                    return;
                                }
                                
                                ((GeminiPhone)eci.phone).unregisterForServiceStateChangedGemini(this, Phone.GEMINI_SIM_1);
                                ((GeminiPhone)eci.phone).unregisterForServiceStateChangedGemini(this, Phone.GEMINI_SIM_2);                            

                                dialing_ecc=true;
                                
                                Log.w(TAG, "get service state change msg...");
    							
                                if (msg.what == EVENT_SERVICE_STATE_CHANGED)
                                {
                                    Log.w(TAG, "service state change msg for sim 1...");

                                    eci.intent.putExtra(Phone.GEMINI_SIM_ID_KEY, Phone.GEMINI_SIM_1);                            
                                }
                                else if (msg.what == EVENT_SERVICE_STATE_CHANGED2)
                                {
                                    Log.w(TAG, "service state change msg for sim 2...");

                                    eci.intent.putExtra(Phone.GEMINI_SIM_ID_KEY, Phone.GEMINI_SIM_2);                            
                                }
                            }
                            else
                            {
                            eci.phone.unregisterForServiceStateChanged(this);
                            }
                            eci.dialog.dismiss();

                            if (DBG) Log.d(TAG, "About to (re)launch InCallScreen: " + eci.intent);
                            eci.app.startActivity(eci.intent);
							sEci = null;
                        }
                    }
                    break;

                case EVENT_TIMEOUT_EMERGENCY_CALL: {
                        if (DBG) Log.d(TAG, "EVENT_TIMEOUT_EMERGENCY_CALL...");
                        // repeated call after the timeout period.
                        EmergencyCallInfo eci = (EmergencyCallInfo) msg.obj;
                        eci.dialog.dismiss();

                        if (DBG) Log.d(TAG, "About to (re)launch InCallScreen: " + eci.intent);
                        eci.app.startActivity(eci.intent);
                    }
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        Log.i(TAG, "onCreate()...  intent = " + getIntent());
        super.onCreate(icicle);

        // Watch out: the intent action we get here should always be
        // ACTION_CALL_EMERGENCY, since the whole point of this activity
        // is for it to be launched using the same intent originally
        // passed to the InCallScreen, which will always be
        // ACTION_CALL_EMERGENCY when making an emergency call.
        //
        // If we ever get launched with any other action, especially if it's
        // "com.android.phone.InCallScreen.UNDEFINED" (as in bug 3094858), that
        // almost certainly indicates a logic bug in the InCallScreen.
        if (!Intent.ACTION_CALL_EMERGENCY.equals(getIntent().getAction())) {
            Log.w(TAG, "Unexpected intent action!  Should be ACTION_CALL_EMERGENCY, "
                  + "but instead got: " + getIntent().getAction());
        }

        // setup the phone and get the retry count embedded in the intent.
        Phone phone = PhoneFactory.getDefaultPhone();
        int retryCount = getIntent().getIntExtra(EMERGENCY_CALL_RETRY_KEY, INITIAL_ATTEMPT);
        Log.w(TAG, "oncreate() retryCount is "+retryCount);
        // create a new message object.
        if (null == sEci) {
    		 Log.w(TAG, "oncreate() sEci is null");
        	 sEci = new EmergencyCallInfo();
    		 sEci.dialog = constructDialog(retryCount);
    	}
        sEci.phone = phone;
        sEci.app = getApplication();
        

        // The Intent we're going to fire off to retry the call is the
        // same one that got us here (except that we *don't* explicitly
        // specify this class as the component!)
        sEci.intent = getIntent().setComponent(null);
        // And we'll be firing this Intent from the PhoneApp's context
        // (see the startActivity() calls above) so the
        // FLAG_ACTIVITY_NEW_TASK flag is required.
        sEci.intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (DBG) Log.d(TAG, "- initial eci.intent: " + sEci.intent);
        // create the handler.
        if (sHandler == null) {
            sHandler = new EmergencyCallEventHandler();
        }
        
        // If this is the initial attempt, we need to register for a radio state
        // change and turn the radio on.  Otherwise, this is just a retry, and
        // we simply wait the alloted time before sending the request to try
        // the call again.
        
        // Note: The radio logic ITSELF will try its best to put the emergency
        // call through once the radio is turned on.  The retry we have here 
        // is in case it fails; the current constants we have include making
        // 6 attempts, with a 5 second delay between each.
        if (retryCount == INITIAL_ATTEMPT) {
            // place the number of pending retries in the intent.
            sEci.intent.putExtra(EMERGENCY_CALL_RETRY_KEY, NUMBER_OF_RETRIES);
            
            // turn the radio on and listen for it to complete.
            if (FeatureOption.MTK_GEMINI_SUPPORT == true)
            {
                ((GeminiPhone)phone).registerForServiceStateChangedGemini(sHandler, 
                       EVENT_SERVICE_STATE_CHANGED, sEci, Phone.GEMINI_SIM_1);
            			
                ((GeminiPhone)phone).registerForServiceStateChangedGemini(sHandler, 
                       EVENT_SERVICE_STATE_CHANGED2, sEci, Phone.GEMINI_SIM_2);
            
                dialing_ecc = false;
            }
            else
            {
            phone.registerForServiceStateChanged(sHandler, 
                        EVENT_SERVICE_STATE_CHANGED, sEci);
            }

            // If airplane mode is on, we turn it off the same way that the
            // Settings activity turns it off.
            Intent intent;
            int dualSimMode = 0;
            boolean bOffAirplaneMode = false;
            if (FeatureOption.MTK_GEMINI_SUPPORT)
                dualSimMode = Settings.System.getInt(getContentResolver(),
                        Settings.System.DUAL_SIM_MODE_SETTING,
                        Settings.System.DUAL_SIM_MODE_SETTING_DEFAULT);

            if (DBG)
                Log.d(TAG, "dualSimMode = " + dualSimMode);
            if (Settings.System.getInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) > 0) {
                if (DBG)
                    Log.d(TAG, "Turning off airplane mode...");

                // Change the system setting
                Settings.System.putInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0);

                // Post the intent
                intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                intent.putExtra("state", false);
                sendBroadcast(intent);
                
                bOffAirplaneMode = true;
                // Otherwise, for some strange reason the radio is just off, so
                // we just turn it back on.
            } else {
                if (DBG)
                    Log.d(TAG, "Manually powering radio on...");
                if (!FeatureOption.MTK_GEMINI_SUPPORT)
                    phone.setRadioPower(true);
            }

            if (FeatureOption.MTK_GEMINI_SUPPORT 
                && (!bOffAirplaneMode || (bOffAirplaneMode && needSetDualSimMode(dualSimMode)))) {
                //BEGIN mtk03923 [20111006][Skip if for MT6575E2 SMT]
                //if (dualSimMode == 0) {
                //If there is only SIM2, send broadcast with EXTRA_DUAL_SIM_MODE == 1 will not
                //turn on the radio
                int mode = getProperDualSimMode();
                    Settings.System.putInt(this.getContentResolver(),
                            Settings.System.DUAL_SIM_MODE_SETTING, mode);
                    intent = new Intent(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
                    intent.putExtra(Intent.EXTRA_DUAL_SIM_MODE, mode);
                    sendBroadcast(intent);
                //}
                //END   mtk03923 [20111006][Skip if for MT6575E2 SMT]
            }

        } else {
            // decrement and store the number of retries.
            if (DBG) Log.d(TAG, "Retry attempt...  retryCount = " + retryCount);
            sEci.intent.putExtra(EMERGENCY_CALL_RETRY_KEY, (retryCount - 1));
            
            // get the message and attach the data, then wait the alloted
            // time and send.
            Message m = sHandler.obtainMessage(EVENT_TIMEOUT_EMERGENCY_CALL);
            m.obj = sEci;
            sHandler.sendMessageDelayed(m, TIME_BETWEEN_RETRIES_MS);
        }
        finish();
    }
    
    /**
     * create the dialog and hand it back to caller.
     */
    private ProgressDialog constructDialog(int retryCount) {
        Log.w(TAG, "constructDialog the retryCount is " + retryCount);
        // figure out the message to display. 
        int msgId = (retryCount == INITIAL_ATTEMPT) ? 
                R.string.emergency_enable_radio_dialog_message :
                R.string.emergency_enable_radio_dialog_retry;

        // create a system dialog that will persist outside this activity.
        ProgressDialog pd = new ProgressDialog(getApplication());
        pd.setTitle(getText(R.string.emergency_enable_radio_dialog_title));
        pd.setMessage(getText(msgId));
        pd.setIndeterminate(true);
        pd.setCancelable(false);
        pd.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
        pd.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        
        // show the dialog
        pd.show();
        
        return pd;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // We shouldn't ever get here, since we should never be launched in
        // "singleTop" mode in the first place.
        Log.w(TAG, "Unexpected call to onNewIntent(): intent=" + intent);
        super.onNewIntent(intent);
    }
    
    static final int DUALSIM_OFF = 0;
    static final int SIM1_ONLY = 1;
    static final int SIM2_ONLY = 2;
    static final int DUALSIM_ON = 3;
    
    private boolean needSetDualSimMode(int lastMode) {
        
        List<SIMInfo> list = SIMInfo.getInsertedSIMList(this);
        if (list == null || list.size() == 0) {
            return true;
        }
        
        //dual radio off, off airplane mode not open radios
        if (DUALSIM_OFF == lastMode) {
            return true;
        }
        
        //dual radio on, even one sim will be ok
        if (DUALSIM_ON == lastMode) {
            return false;
        }
        
        for (SIMInfo info : list) {
            if (lastMode == info.mSlot + 1) {
                return false;
            }
        }
        
        return true;
    }
    
    private int getProperDualSimMode() {
        int mode = 1;
        List<SIMInfo> list = SIMInfo.getInsertedSIMList(this);
      
        if (list != null && list.size() == 1 && list.get(0).mSlot == Phone.GEMINI_SIM_2) {
            mode = 2;
        }
        
        return mode;
    }
}
