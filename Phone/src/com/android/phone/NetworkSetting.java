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
 * Copyright (C) 2006 The Android Open Source Project
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

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.gsm.NetworkInfo;
import com.android.internal.telephony.PhoneStateIntentReceiver;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.ServiceConnection;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.telephony.ServiceState;
import android.util.Log;

import com.android.internal.telephony.CommandException;
import com.mediatek.featureoption.FeatureOption;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.gsm.NetworkInfo;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.gemini.GeminiPhone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * "Networks" settings UI for the Phone app.
 */
public class NetworkSetting extends PreferenceActivity
        implements DialogInterface.OnCancelListener {

    private static final String LOG_TAG = "phone";
    private static final boolean DBG = true;

    private static final int EVENT_NETWORK_SCAN_COMPLETED = 100;
    private static final int EVENT_NETWORK_SCAN_COMPLETED_2 = 101;
    private static final int EVENT_NETWORK_SELECTION_DONE = 200;
    private static final int EVENT_AUTO_SELECT_DONE = 300;
    private static final int EVENT_SERVICE_STATE_CHANGED = 400;

    //dialog ids
    private static final int DIALOG_NETWORK_SELECTION = 100;
    private static final int DIALOG_NETWORK_LIST_LOAD = 200;
    private static final int DIALOG_NETWORK_AUTO_SELECT = 300;

    //String keys for preference lookup
    private static final String LIST_NETWORKS_KEY = "list_networks_key";
    private static final String BUTTON_SRCH_NETWRKS_KEY = "button_srch_netwrks_key";
    private static final String BUTTON_AUTO_SELECT_KEY = "button_auto_select_key";

    //map of network controls to the network data.
    private HashMap<Preference, NetworkInfo> mNetworkMap;

    Phone mPhone;
    protected boolean mIsForeground = false;
	private GeminiPhone mGeminiPhone;

	private static final int SIM_CARD_1 = 0;
	private static final int SIM_CARD_2 = 1;
	private int mSimId = SIM_CARD_1;
	private static final int SIM_CARD_UNDEFINED = -1;
	private boolean _GEMINI_PHONE = false;
    /** message for network selection */
    String mNetworkSelectMsg;
    private String mTitleName = null;

    //preference objects
    private PreferenceGroup mNetworkList;
    private Preference mSearchButton;
    private Preference mAutoSelect;
    private boolean mAirplaneModeEnabled;
    private int mDualSimMode = -1;
    
    //added by mtk80908, to control progress Dialogs
    private static final int MAX_DIALOG_NUM = 3;
    private Dialog[] mDialogs = new Dialog[MAX_DIALOG_NUM];
    private static final int DLGNETWORKSCAN = 2;
    private static final int DLGNETWORKSELECTION = 0;
    private static final int DLGAUTOSELECTION = 1;
    
    private PhoneStateIntentReceiver mPhoneStateReceiver;
    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); //Added by vend_am00015 2010-06-07
        	if(action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
        		mAirplaneModeEnabled = intent.getBooleanExtra("state", false);
        		getPreferenceScreen().setEnabled(!mAirplaneModeEnabled && (mDualSimMode!=0));
        	}else if(action.equals(Intent.ACTION_DUAL_SIM_MODE_CHANGED)){
                mDualSimMode = intent.getIntExtra(Intent.EXTRA_DUAL_SIM_MODE, -1);
                getPreferenceScreen().setEnabled(!mAirplaneModeEnabled && (mDualSimMode!=0));
            }
        }
    };

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
            //mtk80908 begin, changed for Gemini phone
                case EVENT_NETWORK_SCAN_COMPLETED:
                	log("EVENT_NETWORK_SCAN_COMPLETED"+" ||mSimId:"+mSimId);
                	//see if we need to do any work
                	if(_GEMINI_PHONE && mSimId == SIM_CARD_2){
                		return;
                	}
                    networksListLoaded ((List<NetworkInfo>) msg.obj, msg.arg1);
                    break;
                case EVENT_NETWORK_SCAN_COMPLETED_2:
                	log("EVENT_NETWORK_SCAN_COMPLETED_2"+" ||mSimId:"+mSimId);
                	//see if we need to do any work
                	if (_GEMINI_PHONE && mSimId == SIM_CARD_1){
                		return;
                	}
                	networksListLoaded ((List<NetworkInfo>) msg.obj, msg.arg1);
                	break;
            //mtk80908 end 
                case EVENT_NETWORK_SELECTION_DONE:
                    if (DBG) log("hideProgressPanel");
                    //changed by mtk80908
                    if(mDialogs[DLGNETWORKSELECTION] !=null && mDialogs[DLGNETWORKSELECTION].isShowing())
                    	mDialogs[DLGNETWORKSELECTION].dismiss();
                    
                    getPreferenceScreen().setEnabled(true);

                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        if (DBG) log("manual network selection: failed!");
                        displayNetworkSelectionFailed(ar.exception);
                    } else {
                        if (DBG) log("manual network selection: succeeded!");
                        displayNetworkSelectionSucceeded();
                    }
                    break;
                case EVENT_AUTO_SELECT_DONE:
                    if (DBG) log("hideProgressPanel");

                    //changed by mtk80908
                    if(mDialogs[DLGAUTOSELECTION] != null && mDialogs[DLGAUTOSELECTION].isShowing())
                            mDialogs[DLGAUTOSELECTION].dismiss();
                    
                    getPreferenceScreen().setEnabled(true);

                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        if (DBG) log("automatic network selection: failed!");
                        displayNetworkSelectionFailed(ar.exception);
                    } else {
                        if (DBG) log("automatic network selection: succeeded!");
                        displayNetworkSelectionSucceeded();
                    }
                    break;
                case EVENT_SERVICE_STATE_CHANGED:
                   {
                	log("EVENT_SERVICE_STATE_CHANGED");
                   }
                    break;
            }

            return;
        }
    };

    /**
     * Service connection code for the NetworkQueryService.
     * Handles the work of binding to a local object so that we can make
     * the appropriate service calls.
     */

    /** Local service interface */
    private INetworkQueryService mNetworkQueryService = null;

    /** Service connection */
    private final ServiceConnection mNetworkQueryServiceConnection = new ServiceConnection() {

        /** Handle the task of binding the local object to the service */
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (DBG) log("connection created, binding local service.");
            mNetworkQueryService = ((NetworkQueryService.LocalBinder) service).getService();
            // as soon as it is bound, run a query.
            loadNetworksList();
        }

        /** Handle the task of cleaning up the local binding */
        public void onServiceDisconnected(ComponentName className) {
            if (DBG) log("connection disconnected, cleaning local binding.");
            mNetworkQueryService = null;
        }
    };

    /**
     * This implementation of INetworkQueryServiceCallback is used to receive
     * callback notifications from the network query service.
     */
    private final INetworkQueryServiceCallback mCallback = new INetworkQueryServiceCallback.Stub() {

        /** place the message on the looper queue upon query completion. */
        public void onQueryComplete(List<NetworkInfo> networkInfoArray, int status) {
            if (DBG) log("notifying message loop of query completion.");
            
            Message msg;
            //changed by mtk80908 for Gemini phone
            if(mSimId == SIM_CARD_2)
                    msg = mHandler.obtainMessage(EVENT_NETWORK_SCAN_COMPLETED_2,status, 0, networkInfoArray);
            else
                    msg = mHandler.obtainMessage(EVENT_NETWORK_SCAN_COMPLETED,status, 0, networkInfoArray);
            msg.sendToTarget();
        }
    };

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean handled = false;

        if (preference == mSearchButton) {
            loadNetworksList();
            handled = true;
        } else if (preference == mAutoSelect) {
            selectNetworkAutomatic();
            handled = true;
        } else {
            Preference selectedCarrier = preference;

            String networkStr = selectedCarrier.getTitle().toString();
            if (DBG) log("selected network: " + networkStr);

            Message msg = mHandler.obtainMessage(EVENT_NETWORK_SELECTION_DONE);
			if (!_GEMINI_PHONE) {
				mPhone.selectNetworkManually(mNetworkMap.get(selectedCarrier),
						msg);
			} else {
				mGeminiPhone.selectNetworkManuallyGemini(mNetworkMap
						.get(selectedCarrier), msg, mSimId);
			}

            displayNetworkSeletionInProgress(networkStr);

            handled = true;
        }

        return handled;
    }

    //implemented for DialogInterface.OnCancelListener
    public void onCancel(DialogInterface dialog) {
        // request that the service stop the query with this callback object.
        try {
            mNetworkQueryService.stopNetworkQuery(mCallback);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        finish();
    }

    public String getNormalizedCarrierName(NetworkInfo ni) {
        if (ni != null) {
            return ni.getOperatorAlphaLong() + " (" + ni.getOperatorNumeric() + ")";
        }
        return null;
    }

	public void GeminiPhoneInit() {
		if (CallSettings.isMultipleSim()) {
			Intent it = getIntent();
			mSimId = it.getIntExtra(Phone.GEMINI_SIM_ID_KEY, SIM_CARD_UNDEFINED);
			mGeminiPhone = (GeminiPhone) PhoneApp.getInstance().phone;
			_GEMINI_PHONE = true;
		} 
	}
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.carrier_select);
		mNetworkList = (PreferenceGroup) getPreferenceScreen()
				.findPreference(LIST_NETWORKS_KEY);
		mSearchButton = getPreferenceScreen().findPreference(
				BUTTON_SRCH_NETWRKS_KEY);
		mAutoSelect = getPreferenceScreen().findPreference(
				BUTTON_AUTO_SELECT_KEY);
		
		mTitleName = getIntent().getStringExtra(MultipleSimActivity.SUB_TITLE_NAME);

        mPhone = PhoneApp.getInstance().phone;
		GeminiPhoneInit();

		if (DBG) log("It's a GeminiPhone ? = " + _GEMINI_PHONE + "SIM_ID = " + mSimId);

        mNetworkMap = new HashMap<Preference, NetworkInfo>();
        // Start the Network Query service, and bind it.
        // The OS knows to start he service only once and keep the instance around (so
        // long as startService is called) until a stopservice request is made.  Since
        // we want this service to just stay in the background until it is killed, we
        // don't bother stopping it from our end.
	Intent i = new Intent(this, NetworkQueryService.class);
	i.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
	startService(i);
	bindService(i, mNetworkQueryServiceConnection, Context.BIND_AUTO_CREATE);

        mIntentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED); 
        if(_GEMINI_PHONE){
            mIntentFilter.addAction(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
        }
        mPhoneStateReceiver = new PhoneStateIntentReceiver(this, mHandler);
        mPhoneStateReceiver.notifyServiceState(EVENT_SERVICE_STATE_CHANGED);
    }

    //@Override
    /*public void onPause() {
        super.onPause();
        mIsForeground = false;
    }*/

    /**
     * Override onDestroy() to unbind the query service, avoiding service
     * leak exceptions.
     */
    @Override
    protected void onDestroy() {
        // unbind the service.
    	
    	log("[onDestroy]Call onDestroy. unbindService");
        unbindService(mNetworkQueryServiceConnection);

        super.onDestroy();
    }

    @Override
    protected Dialog onCreateDialog(int id) {

        if ((id == DIALOG_NETWORK_SELECTION) || (id == DIALOG_NETWORK_LIST_LOAD) ||
                (id == DIALOG_NETWORK_AUTO_SELECT)) {
            ProgressDialog dialog = new ProgressDialog(this);
            switch (id) {
                case DIALOG_NETWORK_SELECTION:
                    // It would be more efficient to reuse this dialog by moving
                    // this setMessage() into onPreparedDialog() and NOT use
                    // removeDialog().  However, this is not possible since the
                    // message is rendered only 2 times in the ProgressDialog -
                    // after show() and before onCreate.
                    dialog.setMessage(mNetworkSelectMsg);
                    dialog.setCancelable(false);
                    dialog.setIndeterminate(true);
                    break;
                case DIALOG_NETWORK_AUTO_SELECT:
                    dialog.setMessage(getResources().getString(R.string.register_automatically));
                    dialog.setCancelable(false);
                    dialog.setIndeterminate(true);
                    break;
                case DIALOG_NETWORK_LIST_LOAD:
                default:
                    // reinstate the cancelablity of the dialog.
                    dialog.setMessage(getResources().getString(R.string.load_networks_progress));
                    dialog.setCancelable(true);
                    dialog.setOnCancelListener(this);
                    break;
            }
	        log("[onCreateDialog] create dialog id is "+id);
            return dialog;
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        if ((id == DIALOG_NETWORK_SELECTION) || (id == DIALOG_NETWORK_LIST_LOAD) ||
                (id == DIALOG_NETWORK_AUTO_SELECT)) {
        	log("show Dialog ID is "+id+" || mNetworkSelectMsg is "+mNetworkSelectMsg);
        	switch(id){
        	case DIALOG_NETWORK_SELECTION:{
        		mDialogs[DLGNETWORKSELECTION] = dialog;
        		((ProgressDialog)dialog).setMessage(mNetworkSelectMsg);
        	}
        		break;
        	case DIALOG_NETWORK_LIST_LOAD:
        		mDialogs[DLGNETWORKSCAN] = dialog;
        		break;
        	case DIALOG_NETWORK_AUTO_SELECT:
        		mDialogs[DLGAUTOSELECTION] = dialog;
        		default:
        			return;
        	}
            // when the dialogs come up, we'll need to indicate that
            // we're in a busy state to dissallow further input.
            getPreferenceScreen().setEnabled(false);
        }
    }

    private void displayEmptyNetworkList(boolean flag) {
    	if(flag){
    		log("SET empty network list title");
    		setTitle(R.string.empty_networks_list);
    		mNetworkList.setTitle(R.string.empty_networks_list);
    	}else{
    		if(CallSettings.isMultipleSim()){
    		    if (mTitleName != null) {
    		        setTitle(mTitleName);
    		        mNetworkList.setTitle(mTitleName);
    		    } else {
    		        setTitle(getString(R.string.label_available));
                    mNetworkList.setTitle(getString(R.string.label_available));
    		    }
    			/*if(mSimId == SIM_CARD_1){
    				log("SET SIM1 Title");
    				setTitle(getString(R.string.label_available));
    				mNetworkList.setTitle("SIM1 " + getString(R.string.label_available));
    			}else if(mSimId == SIM_CARD_2){
    				log("SET SIM2 Title");
    				setTitle(getString(R.string.label_available));
    				mNetworkList.setTitle("SIM2 " + getString(R.string.label_available));
    			}else{
    				log("SET default SIM Title");
    				setTitle(R.string.label_available);
    				mNetworkList.setTitle(R.string.label_available);
    			}*/
    		}else{
    			log("SET SIM Title");
    			setTitle(R.string.label_available);
    			mNetworkList.setTitle(R.string.label_available);
    		}
    	}
    }

    private void displayNetworkSeletionInProgress(String networkStr) {
        // TODO: use notification manager?
        mNetworkSelectMsg = getResources().getString(R.string.register_on_network, networkStr);

        if (mIsForeground) {
            showDialog(DIALOG_NETWORK_SELECTION);
        }
    }

    private void displayNetworkQueryFailed(int error) {
        String status = getResources().getString(R.string.network_query_error);

        NotificationMgr.getDefault().postTransientNotification(
                        NotificationMgr.NETWORK_SELECTION_NOTIFICATION, status);
    }

    private void displayNetworkSelectionFailed(Throwable ex) {
        String status;

        if ((ex != null && ex instanceof CommandException) &&
                ((CommandException)ex).getCommandError()
                  == CommandException.Error.ILLEGAL_SIM_OR_ME)
        {
            status = getResources().getString(R.string.not_allowed);
        } else {
            status = getResources().getString(R.string.connect_later);
        }

        NotificationMgr.getDefault().postTransientNotification(
                        NotificationMgr.NETWORK_SELECTION_NOTIFICATION, status);
    }

    private void displayNetworkSelectionSucceeded() {
        String status = getResources().getString(R.string.registration_done);

        NotificationMgr.getDefault().postTransientNotification(
                        NotificationMgr.NETWORK_SELECTION_NOTIFICATION, status);

        mHandler.postDelayed(new Runnable() {
            public void run() {
                finish();
            }
        }, 3000);
    }

    private void loadNetworksList() {
        if (DBG) log("load networks list...");

        if (mIsForeground) {
            showDialog(DIALOG_NETWORK_LIST_LOAD);
        }

        // delegate query request to the service.
        try {
            mNetworkQueryService.startNetworkQuery(mCallback);
        } catch (RemoteException e) {
        }

        displayEmptyNetworkList(false);
    }

    /**
     * networksListLoaded has been rewritten to take an array of
     * NetworkInfo objects and a status field, instead of an
     * AsyncResult.  Otherwise, the functionality which takes the
     * NetworkInfo array and creates a list of preferences from it,
     * remains unchanged.
     */
    private void networksListLoaded(List<NetworkInfo> result, int status) {
        if (DBG) log("networks list loaded");

        // update the state of the preferences.
        if (DBG) log("hideProgressPanel");

        //for some case such as changing language, this activity would reset 
        //and it may load the old dialog. But when calling onCreate function,
        //it would show a new DIALOG_NETWORK_LIST_LOAD dialog, and cause that 
        //old dialog cannot be dismissed. So, we dismiss all dialogs here.  
    	for(int i = 0 ; i < MAX_DIALOG_NUM ; i++){
    		if(mDialogs[i] != null && mDialogs[i].isShowing())
    			mDialogs[i].dismiss();
        }

        getPreferenceScreen().setEnabled(!mAirplaneModeEnabled && (mDualSimMode!=0));
        clearList();

        if (status != NetworkQueryService.QUERY_OK) {
            if (DBG) log("error while querying available networks");
            displayNetworkQueryFailed(status);
            displayEmptyNetworkList(true);
        } else {
            if (result != null){
                displayEmptyNetworkList(false);

                // create a preference for each item in the list.
                // just use the operator name instead of the mildly
                // confusing mcc/mnc.
                for (NetworkInfo ni : result) {
                    Preference carrier = new Preference(this, null);
                    String opName = ni.getOperatorAlphaLong();
                    if (ni.getState() == NetworkInfo.State.FORBIDDEN) {
                        opName = opName + "(" + getResources().getString(R.string.network_forbidden) + ")";
                    }
                    carrier.setTitle(opName);
                    carrier.setPersistent(false);
                    mNetworkList.addPreference(carrier);
                    mNetworkMap.put(carrier, ni);

                    if (DBG) log("  " + ni);
                }

            } else {
                displayEmptyNetworkList(true);
            }
            
        }
        try {
            mNetworkQueryService.stopNetworkQuery(mCallback);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private void clearList() {
        for (Preference p : mNetworkMap.keySet()) {
            mNetworkList.removePreference(p);
        }
        mNetworkMap.clear();
    }

    private void selectNetworkAutomatic() {
        if (DBG) log("select network automatically...");
        if (mIsForeground) {
            showDialog(DIALOG_NETWORK_AUTO_SELECT);
        }

        Message msg = mHandler.obtainMessage(EVENT_AUTO_SELECT_DONE);
		if (!_GEMINI_PHONE) {
        mPhone.setNetworkSelectionModeAutomatic(msg);
		} else {
			mGeminiPhone.setNetworkSelectionModeAutomaticGemini(msg, mSimId);
		}
    }

    private void log(String msg) {
        Log.d(LOG_TAG, "[NetworksList] " + msg);
    }
    
    @Override    
    protected void onResume() {
        super.onResume();
        mIsForeground = true;
        mPhoneStateReceiver.registerIntent(); 
        registerReceiver(mReceiver, mIntentFilter);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        boolean isCallStateIdle = telephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE;
        
        ServiceState serviceState = mPhoneStateReceiver.getServiceState();
        mAirplaneModeEnabled = serviceState.getState() == ServiceState.STATE_POWER_OFF;
        
        if(_GEMINI_PHONE) {
            mDualSimMode = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.DUAL_SIM_MODE_SETTING, -1);
            Log.d(LOG_TAG, "NetworkSettings.onResume(), mDualSimMode="+mDualSimMode);
        }

        getPreferenceScreen().setEnabled(isCallStateIdle && (!mAirplaneModeEnabled) && (mDualSimMode!=0));
    } 
    
    @Override
    protected void onPause() {
        super.onPause();
        mIsForeground = false;        
        mPhoneStateReceiver.unregisterIntent();
        unregisterReceiver(mReceiver);
    }
    
}

