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

import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.ThrottleManager;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Telephony.SIMInfo;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;

import android.content.BroadcastReceiver;
import android.telephony.TelephonyManager;
import android.content.Context;
import com.mediatek.CellConnService.CellConnMgr;

import android.database.ContentObserver;

/**
 * List of Phone-specific settings screens.
 */
public class Settings extends PreferenceActivity implements DialogInterface.OnClickListener,
        DialogInterface.OnDismissListener, Preference.OnPreferenceChangeListener{

    public static final int WCDMA_CARD_SLOT = 0;
    
    // debug data
    private static final String LOG_TAG = "NetworkSettings";
    private static final boolean DBG = true;
    public static final int REQUEST_CODE_EXIT_ECM = 17;

    //String keys for preference lookup
    private static final String BUTTON_DATA_ENABLED_KEY = "button_data_enabled_key";
    private static final String BUTTON_DATA_USAGE_KEY = "button_data_usage_key";
    private static final String BUTTON_PREFERED_NETWORK_MODE = "preferred_network_mode_key";
    private static final String BUTTON_ROAMING_KEY = "button_roaming_key";
    private static final String BUTTON_CDMA_ROAMING_KEY = "cdma_roaming_mode_key";

    private static final String BUTTON_GSM_UMTS_OPTIONS = "gsm_umts_options_key";
    private static final String BUTTON_CDMA_OPTIONS = "cdma_options_key";
    private static final String BUTTON_APN = "button_apn_key";
    private static final String BUTTON_CARRIER_SEL = "button_carrier_sel_key";
    
    private static final String BUTTON_3G_SERVICE = "button_3g_service_key";
    private static final String BUTTON_PLMN_LIST = "button_plmn_key";

    static final int preferredNetworkMode = Phone.PREFERRED_NT_MODE;

    //UI objects
    //For current platform, mButtonPreferredNetworkMode is used for RAT selection
    private ListPreference mButtonPreferredNetworkMode;
    private Preference mPreferredNetworkMode;
    private CheckBoxPreference mButtonDataRoam;
    private CheckBoxPreference mButtonDataEnabled;
    private Preference mPreference3GSwitch = null;
    private Preference mPLMNPreference = null;

    private Preference mButtonDataUsage;
    private DataUsageListener mDataUsageListener;
    private static final String iface = "rmnet0"; //TODO: this will go away
    private GeminiPhone mGeminiPhone;
    private Phone mPhone;
    private MyHandler mHandler;
    private boolean mOkClicked;
    private static final int SIM_CARD_1 = 0;
        private static final int SIM_CARD_2 = 1;
        private static final int SIM_CARD_SIGNAL = 2;
        
        private static boolean _GEMINI_PHONE = false;
        private int mSimId;
        private PreferenceScreen mApnPref;
        private PreferenceScreen mCarrierSelPref;
    private boolean mIsRadioOn = true;
    
    private boolean isOnlyOneSim = false;
    private PreCheckForRunning preCfr = null;
    private ProgressDialog pd = null;

    long simIds[] = new long[1];
    //GsmUmts options and Cdma options
    GsmUmtsOptions mGsmUmtsOptions;
    CdmaOptions mCdmaOptions;

    private boolean mAirplaneModeEnabled = false;
    private int mDualSimMode = -1;
    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); //Added by vend_am00015 2010-06-07
            if(action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                mAirplaneModeEnabled = intent.getBooleanExtra("state", false);
                getPreferenceScreen().setEnabled((!mAirplaneModeEnabled) && mIsRadioOn && (mDualSimMode!=0));
                update3GPrefStatus();
            } else if (action.equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED) && isChangeData) {
                Log.d(LOG_TAG, "catch data change!");
                String reason = intent.getStringExtra(Phone.STATE_CHANGE_REASON_KEY);
                Phone.DataState state = getMobileDataState(intent);
                Log.d(LOG_TAG, "reason : " + reason + "  state = " + state);
                if ((state == Phone.DataState.CONNECTED) || (state == Phone.DataState.DISCONNECTED)) {
                    mH.removeMessages(DATA_STATE_CHANGE_TIMEOUT);
                    if (pd != null && pd.isShowing()) {
                        try {
                            pd.dismiss();
                        } catch (Exception e) {
                            Log.d(LOG_TAG, e.toString());
                        }
                        pd = null;
                    }
                    isChangeData = false;
                }
            }else if(action.equals(Intent.ACTION_DUAL_SIM_MODE_CHANGED)){
                mDualSimMode = intent.getIntExtra(Intent.EXTRA_DUAL_SIM_MODE, -1);
                getPreferenceScreen().setEnabled(( !mAirplaneModeEnabled) && mIsRadioOn && (mDualSimMode!=0));
            }
        }
    };
    private Preference mClickedPreference;

    private ContentObserver mContentObserver;


    //This is a method implemented for DialogInterface.OnClickListener.
    //  Used to dismiss the dialogs when they come up.
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (CallSettings.isMultipleSim()) {
            if (DBG) log("getDataRoamingEnabledGemini" + " do nothing");
                //mGeminiPhone.setDataRoamingEnabledGemini(true, mSimId);
            } else {
            mPhone.setDataRoamingEnabled(true);
              }
            mOkClicked = true;
            mButtonDataRoam.setChecked(true);
        } else {
            // Reset the toggle
            mButtonDataRoam.setChecked(false);
        }
    }

    public void onDismiss(DialogInterface dialog) {
        // Assuming that onClick gets called first
        if (!mOkClicked) {
            mButtonDataRoam.setChecked(false);
        }
    }

    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceActivity's implementation.  Used to make sure we track the
     * preference click events.
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        /** TODO: Refactor and get rid of the if's using subclasses */
        
        if (preference == mButtonDataUsage)
        {
            if (CallSettings.isMultipleSim())
            {
                Intent intent = new Intent(this, MultipleSimActivity.class);
                //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
                intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
                  intent.putExtra(MultipleSimActivity.intentKey, "PreferenceScreen");
                  intent.putExtra(MultipleSimActivity.targetClassKey, "com.android.phone.DataUsage");
                //this.startActivity(intent);
                  preCfr.checkToRun(intent, this.mSimId, 302);
                return true;
            }else {
                return false;
            }
        }
        
        if (preference == this.mPLMNPreference) {
            if (CallSettings.isMultipleSim()) {
                Intent intent = new Intent(this, MultipleSimActivity.class);
                //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
                intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
                intent.putExtra(MultipleSimActivity.intentKey, "PreferenceScreen");
                intent.putExtra(MultipleSimActivity.targetClassKey, "com.android.phone.PLMNListPreference");
                //this.startActivity(intent);
                preCfr.checkToRun(intent, this.mSimId, 302);
                return true;
            }else {
                return false;
            }
        }
        
        if (preference == mPreferredNetworkMode)
        {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            intent.putExtra(MultipleSimActivity.intentKey, "ListPreference");
            CharSequence[] entries = this.getResources().getStringArray(R.array.gsm_umts_network_preferences_choices);
            intent.putExtra(MultipleSimActivity.initArray, entries);
            intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
            intent.putExtra(MultipleSimActivity.LIST_TITLE, getResources().getString(R.string.gsm_umts_network_preferences_title));
            intent.putExtra(MultipleSimActivity.initFeatureName, "NETWORK_MODE");
            intent.putExtra(MultipleSimActivity.initSimId, simIds);
            intent.putExtra(MultipleSimActivity.initBaseKey, "preferred_network_mode_key@");
            CharSequence[] entriesValue = this.getResources().getStringArray(R.array.gsm_umts_network_preferences_values);
            intent.putExtra(MultipleSimActivity.initArrayValue, entriesValue);
            //this.startActivity(intent);
            preCfr.checkToRun(intent, this.mSimId, 302);
            return true;
        }
        
        if (mGsmUmtsOptions != null &&
                mGsmUmtsOptions.preferenceTreeClick(preference) == true) {
            return true;
        } else if (mCdmaOptions != null &&
                   mCdmaOptions.preferenceTreeClick(preference) == true) {
            if (Boolean.parseBoolean(
                    SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {

                mClickedPreference = preference;

                // In ECM mode launch ECM app dialog
                startActivityForResult(
                    new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                    REQUEST_CODE_EXIT_ECM);
            }
            return true;
        } else if (preference == mButtonPreferredNetworkMode) {
            //displays the value taken from the Settings.System
            int settingsNetworkMode = android.provider.Settings.Secure.getInt(mPhone.getContext().
                    getContentResolver(), android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                    preferredNetworkMode);
            mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
            return true;
        } else if (preference == mButtonDataRoam) {
            if (DBG) log("onPreferenceTreeClick: preference == mButtonDataRoam.");

            //normally called on the toggle click
            if (mButtonDataRoam.isChecked()) {
                // First confirm with a warning dialog about charges
                mOkClicked = false;
                new AlertDialog.Builder(this).setMessage(
                        getResources().getString(R.string.roaming_warning))
                        .setTitle(android.R.string.dialog_alert_title)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, this)
                        .setNegativeButton(android.R.string.no, this)
                        .show()
                        .setOnDismissListener(this);
            }
            else {
        if (CallSettings.isMultipleSim()) {
            //mGeminiPhone.setDataRoamingEnabledGemini(false, mSimId);
        } else {
            mPhone.setDataRoamingEnabled(false);
        }
            }
            return true;
        } else if (preference == mButtonDataEnabled) {
            this.isChangeData = true;
            if (DBG) log("onPreferenceTreeClick: preference == mButtonDataEnabled.");
            ConnectivityManager cm =
                    (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            this.showProgressDialog();
            cm.setMobileDataEnabled(mButtonDataEnabled.isChecked());
            mH.sendMessageDelayed(mH.obtainMessage(DATA_STATE_CHANGE_TIMEOUT), 30000);
            return true;
        }
        if (CallSettings.isMultipleSim()) {
            Intent it = new Intent();
            it.setAction("android.intent.action.MAIN");
                if (preference == mApnPref) {
                        it.setClassName("com.android.phone",
                                "com.android.phone.MultipleSimActivity");
                        it.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
                        it.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
                        it.putExtra(MultipleSimActivity.intentKey, "PreferenceScreen");
                        it.putExtra(MultipleSimActivity.targetClassKey, "com.android.settings.ApnSettings");
                        //startActivity(it);
                        preCfr.checkToRun(it, this.mSimId, 302);
                        return true;
                    } else if (preference == mCarrierSelPref) {
                        it.setClassName("com.android.phone",
                                "com.android.phone.MultipleSimActivity");
                        //it.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
                        it.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
                        it.putExtra(MultipleSimActivity.intentKey, "PreferenceScreen");
                        it.putExtra(MultipleSimActivity.targetClassKey, "com.android.phone.NetworkSetting");
                        //startActivity(it);
                        preCfr.checkToRun(it, this.mSimId, 302);
                        return true;
                    }
        } else {
            // if the button is anything but the simple toggle preference,
            // we'll need to disable all preferences to reject all click
            // events until the sub-activity's UI comes up.
            preferenceScreen.setEnabled(false);
            // Let the intents be launched by the Preference manager
            return false;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.network_setting);
        SIMInfo info = SIMInfo.getSIMInfoBySlot(this, WCDMA_CARD_SLOT);
        simIds[0] = info != null ? info.mSimId : 0;
        //getPhoneType();
        mPhone = PhoneFactory.getDefaultPhone();
        if (CallSettings.isMultipleSim())
        {
            mGeminiPhone = (GeminiPhone)mPhone;
        }
        mHandler = new MyHandler();
        mIntentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED); 
        mIntentFilter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        if(FeatureOption.MTK_GEMINI_SUPPORT){
            mIntentFilter.addAction(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
        }
        
        preCfr = new PreCheckForRunning(this);
        List<SIMInfo> list = SIMInfo.getInsertedSIMList(this);
        if (list.size() == 1) {
            this.isOnlyOneSim = true;
            this.mSimId = list.get(0).mSlot;
        }
        preCfr.byPass = !isOnlyOneSim;
        //get UI object references
        PreferenceScreen prefSet = getPreferenceScreen();

        mButtonDataEnabled = (CheckBoxPreference) prefSet.findPreference(BUTTON_DATA_ENABLED_KEY);
        mButtonDataRoam = (CheckBoxPreference) prefSet.findPreference(BUTTON_ROAMING_KEY);
         if (CallSettings.isMultipleSim()) {
            prefSet.removePreference(mButtonDataEnabled);
            prefSet.removePreference(mButtonDataRoam);
        }
        mButtonPreferredNetworkMode = (ListPreference) prefSet.findPreference(
                BUTTON_PREFERED_NETWORK_MODE);
        mButtonDataUsage = prefSet.findPreference(BUTTON_DATA_USAGE_KEY);
        
        mPreference3GSwitch = prefSet.findPreference(BUTTON_3G_SERVICE);
        mPLMNPreference = prefSet.findPreference(BUTTON_PLMN_LIST);
        prefSet.removePreference(mPLMNPreference);
        // MTK_OP01_PROTECT_START
        if (("OP01".equals(PhoneUtils.getOptrProperties()) && FeatureOption.MTK_CTA_SUPPORT)) {
            prefSet.addPreference(mPLMNPreference);
        }
        // MTK_OP01_PROTECT_START

        if (getResources().getBoolean(R.bool.world_phone) == true) {
            // set the listener for the mButtonPreferredNetworkMode list preference so we can issue
            // change Preferred Network Mode.
            mButtonPreferredNetworkMode.setOnPreferenceChangeListener(this);

            //Get the networkMode from Settings.System and displays it
            int settingsNetworkMode = android.provider.Settings.Secure.getInt(mPhone.getContext().
                    getContentResolver(),android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                    preferredNetworkMode);
            //mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
            mCdmaOptions = new CdmaOptions(this, prefSet);
            mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet);
        } else {
            prefSet.removePreference(mButtonPreferredNetworkMode);
            int phoneType = mPhone.getPhoneType();
            if (phoneType == Phone.PHONE_TYPE_CDMA) {
                mCdmaOptions = new CdmaOptions(this, prefSet);
            } else if (phoneType == Phone.PHONE_TYPE_GSM) {
                mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet);
                mApnPref = (PreferenceScreen) prefSet
                .findPreference(BUTTON_APN);
                
                mButtonPreferredNetworkMode = (ListPreference)prefSet.findPreference("gsm_umts_preferred_network_mode_key");
                mPreferredNetworkMode = prefSet.findPreference("button_network_mode_ex_key");
                
              //Get the networkMode from Settings.System and displays it
                int settingsNetworkMode = android.provider.Settings.Secure.getInt(mPhone.getContext().
                        getContentResolver(),android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                        preferredNetworkMode);
                if (settingsNetworkMode > 2) {
                    settingsNetworkMode = preferredNetworkMode;
                    android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                            android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                            settingsNetworkMode);
                }
                mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
                
                //This needed further modification in future, currently we think only slot one support wcdma
                if (isSupport3G(Settings.WCDMA_CARD_SLOT) && isAllowedbyCustomization())
                {
                    //The platform supports 3G, next check Gemini/Single
                    /*if (CallSettings.isMultipleSim())
                    {
                        if (mSimId != Phone.GEMINI_SIM_1)
                        {
                            //We supports 3G only on sim1
                            prefSet.removePreference(mButtonPreferredNetworkMode);
                            mButtonPreferredNetworkMode = null;
                        }
                    }*/
                }
                else
                {
                    //For MT6516, we doesn't support the RAT selection
                    prefSet.removePreference(mButtonPreferredNetworkMode);
                    prefSet.removePreference(mPreferredNetworkMode);
                    mPreferredNetworkMode = null;
                    mButtonPreferredNetworkMode = null;
                }
                
                if (isUsedGeneralPreference())
                {
                    if (mButtonPreferredNetworkMode != null) prefSet.removePreference(mButtonPreferredNetworkMode);
                    mButtonPreferredNetworkMode = null;
                }else
                {
                    if (mPreferredNetworkMode != null) prefSet.removePreference(mPreferredNetworkMode);
                    mPreferredNetworkMode = null;
                }
                
                if (PhoneUtils.isSupportFeature("3G_SWITCH")) {
                    if (mButtonPreferredNetworkMode != null) {
                        prefSet.removePreference(mButtonPreferredNetworkMode);
                        mButtonPreferredNetworkMode = null;
                    }
                    if (mPreferredNetworkMode != null) {
                        prefSet.removePreference(mPreferredNetworkMode);
                        mPreferredNetworkMode = null;
                    }
                    //for CU & Gemini load, move 3G switch service item into SIM management
                    if("OP02".equals(PhoneUtils.getOptrProperties()) && CallSettings.isMultipleSim()){
                        if(mPreference3GSwitch!=null){
                            log("For Gemini CU load, move 3g switch Service item into SIM management");
                            prefSet.removePreference(mPreference3GSwitch);
                            mPreference3GSwitch = null;
                        }
                    }
                } else {
                    prefSet.removePreference(mPreference3GSwitch);
                    mPreference3GSwitch = null;
                }
                
                if (mButtonPreferredNetworkMode != null)
                {
                    mButtonPreferredNetworkMode.setOnPreferenceChangeListener(this);
                }
                mCarrierSelPref = (PreferenceScreen) prefSet
                .findPreference(BUTTON_CARRIER_SEL);
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }
        }
// MTK_OP01_PROTECT_START
        if( "OP01".equals( PhoneUtils.getOptrProperties()))
        {
        	// CMCC needs not "Network Mode" preference
            if (null != mButtonPreferredNetworkMode)
            {
            	prefSet.removePreference(mButtonPreferredNetworkMode);
            	mButtonPreferredNetworkMode = null;
            }
        	
            if (null != mPreferredNetworkMode)
            {
            	prefSet.removePreference(mPreferredNetworkMode);
            	mPreferredNetworkMode = null;
            }
        }
// MTK_OP01_PROTECT_END
        ThrottleManager tm = (ThrottleManager) getSystemService(Context.THROTTLE_SERVICE);
        mDataUsageListener = new DataUsageListener(this, mButtonDataUsage, prefSet);

        if (!CallSettings.isMultipleSim()) {
		mContentObserver = new ContentObserver(mHandler){
		    @Override
		    public void onChange(boolean selfChange) {
		        super.onChange(selfChange);
		        int state = android.provider.Settings.Secure.getInt(mPhone.getContext().getContentResolver(),
		                    android.provider.Settings.Secure.MOBILE_DATA,
		                    0);                
		        mButtonDataEnabled.setChecked(state != 0);
		    }
		};
		
		this.getContentResolver().registerContentObserver(
		        android.provider.Settings.Secure.getUriFor(android.provider.Settings.Secure.MOBILE_DATA),
		        false, mContentObserver);
        }
        registerReceiver(mReceiver, mIntentFilter);

    }

    private void getPhoneType() {
        if(FeatureOption.MTK_GEMINI_SUPPORT) {
            Intent it = getIntent();
        mSimId = it.getIntExtra(Phone.GEMINI_SIM_ID_KEY, SIM_CARD_SIGNAL);
            if (1 < mSimId) {
                mSimId = 0;
                int bIccExist = TelephonyManager.getDefault().getSimStateGemini(mSimId);
                if (TelephonyManager.SIM_STATE_READY != bIccExist) {
                    mSimId = 1;
                }       
            }

            _GEMINI_PHONE = true;
            mGeminiPhone = (GeminiPhone)PhoneFactory.getDefaultPhone();
    
        if (mSimId == SIM_CARD_2) {
                        mIsRadioOn = mGeminiPhone.isSimInsert(Phone.GEMINI_SIM_2) && mGeminiPhone.isRadioOnGemini(Phone.GEMINI_SIM_2);
                } else {
                        mIsRadioOn = mGeminiPhone.isSimInsert(Phone.GEMINI_SIM_1) && mGeminiPhone.isRadioOnGemini(Phone.GEMINI_SIM_1);
                }    
        } else {
        mIsRadioOn = true;
    }
    }
    
    private void updateStatus() {
        boolean isEnable = true;
        if (CallSettings.isMultipleSim()) {
            isEnable = !(TelephonyManager.getDefault().getSimStateGemini(Phone.GEMINI_SIM_1) != TelephonyManager.SIM_STATE_READY
                       && TelephonyManager.getDefault().getSimStateGemini(Phone.GEMINI_SIM_2) != TelephonyManager.SIM_STATE_READY);
        } else {
            isEnable = TelephonyManager.getDefault().getSimState() == TelephonyManager.SIM_STATE_READY;
        }
        Log.d(LOG_TAG, "updateStatus(), isEnable="+isEnable);
        if(mApnPref!=null){
            mApnPref.setEnabled(isEnable);
        }
        if(mCarrierSelPref!=null){
            mCarrierSelPref.setEnabled(isEnable);
        }
        if(mButtonDataUsage!=null){
            mButtonDataUsage.setEnabled(isEnable);
        }
        
        if (CallSettings.isMultipleSim()) {
            update3GPrefStatus();
        } else {
            if(mButtonDataEnabled!=null){
                mButtonDataEnabled.setEnabled(isEnable);
            }
            if(mButtonDataRoam!=null){
                mButtonDataRoam.setEnabled(isEnable);
            }
            if (mButtonPreferredNetworkMode != null) {
                mButtonPreferredNetworkMode.setEnabled(isEnable);
            }
        }
    }
    
    private void update3GPrefStatus(){
        Log.d(LOG_TAG, "update3GPrefStatus()");
        if (CallSettings.isMultipleSim()) {
            List<SIMInfo> sims = SIMInfo.getInsertedSIMList(this);
            if (PhoneUtils.isSupportFeature("3G_SWITCH")) {
                //Support 3g switch service, so there is no network mode in this level
                if ((sims != null) && (sims.size() > 0)) {
                    if (mPreference3GSwitch != null) {
                        mPreference3GSwitch.setEnabled(true);
                    }
                } else {
                    if (mPreference3GSwitch != null) {
                        mPreference3GSwitch.setEnabled(false);
                    }
                }
            } else {
                if (mButtonPreferredNetworkMode != null) {
                    //There is only one sim insert, check the 3G slot (slot 0)
                    mButtonPreferredNetworkMode.setEnabled(TelephonyManager.getDefault().getSimStateGemini(Phone.GEMINI_SIM_1) == TelephonyManager.SIM_STATE_READY);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // upon resumption from the sub-activity, make sure we re-enable the
        // preferences.
        getPreferenceScreen().setEnabled(true);

        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        mButtonDataEnabled.setChecked(cm.getMobileDataEnabled());

        // Set UI state in onResume because a user could go home, launch some
        // app to change this setting's backend, and re-launch this settings app
        // and the UI state would be inconsistent with actual state
    if (CallSettings.isMultipleSim()) {
        if(DBG) log("getDataRoamingEnabledGemini" + " do nothing!");
            /*mButtonDataRoam.setChecked(mGeminiPhone.getDataRoamingEnabledGemini(mSimId));
            if(getPreferenceScreen().findPreference(BUTTON_PREFERED_NETWORK_MODE) != null)  {
                mGeminiPhone.getPreferredNetworkTypeGemini(mHandler.obtainMessage(
                        MyHandler.MESSAGE_GET_PREFERRED_NETWORK_TYPE), mSimId);
            }
                
                if (mButtonPreferredNetworkMode != null)
                {
                    if (TelephonyManager.CALL_STATE_IDLE == TelephonyManager.getDefault().getCallStateGemini(Phone.GEMINI_SIM_1))
                    {
                        mButtonPreferredNetworkMode.setEnabled(true);
                    }
                    else
                    {
                        mButtonPreferredNetworkMode.setEnabled(false);
                    }
                }*/
    } else {
        mButtonDataRoam.setChecked(mPhone.getDataRoamingEnabled());

        if (getPreferenceScreen().findPreference(BUTTON_PREFERED_NETWORK_MODE) != null)  {
            mPhone.getPreferredNetworkType(mHandler.obtainMessage(
                    MyHandler.MESSAGE_GET_PREFERRED_NETWORK_TYPE));
        }
            
            if (mButtonPreferredNetworkMode != null)
            {
                if (TelephonyManager.CALL_STATE_IDLE == TelephonyManager.getDefault().getCallState())
                {
                    mButtonPreferredNetworkMode.setEnabled(true);
                }
                else
                {
                    mButtonPreferredNetworkMode.setEnabled(false);
                }
            }
    }
        mDataUsageListener.resume();
       
        //if the phone not idle state or airplane mode, then disable the preferenceScreen
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        boolean isCallStateIdle = telephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE;
        
        if(FeatureOption.MTK_GEMINI_SUPPORT) {
        mDualSimMode = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.DUAL_SIM_MODE_SETTING, -1);
        Log.d(LOG_TAG, "Settings.onResume(), mDualSimMode="+mDualSimMode);
        }
        getPreferenceScreen().setEnabled(isCallStateIdle && (!mAirplaneModeEnabled) && mIsRadioOn && (mDualSimMode!=0));
        
        if (mButtonPreferredNetworkMode != null)
        {
            int settingsNetworkMode = android.provider.Settings.Secure.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Secure.PREFERRED_NETWORK_MODE, preferredNetworkMode);
            
            Log.d(LOG_TAG, "mButtonPreferredNetworkMode != null and the settingsNetworkMode = " + settingsNetworkMode);
            UpdatePreferredNetworkModeSummary(settingsNetworkMode);
            
          //There is only one sim inserted
            SIMInfo info = SIMInfo.getSIMInfoBySlot(this, WCDMA_CARD_SLOT);
            if (info == null)
            {
                mButtonPreferredNetworkMode.setEnabled(false);
            }
        }
        
        if (mPreferredNetworkMode != null)
        {
            int settingsNetworkMode = android.provider.Settings.Secure.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Secure.PREFERRED_NETWORK_MODE, preferredNetworkMode);
            Log.d(LOG_TAG, "mPreferredNetworkMode != null and the settingsNetworkMode = " + settingsNetworkMode);
            
            if (settingsNetworkMode < 0 || settingsNetworkMode > 2) {
                settingsNetworkMode = 0;
                UpdateGeneralPreferredNetworkModeSummary(settingsNetworkMode);
            }
            
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
            //int settingsNetworkMode = Integer.valueOf(sp.getString("preferred_network_mode_key", "-1"));
            SharedPreferences.Editor edit = sp.edit();
            edit.putString("preferred_network_mode_key", String.valueOf(settingsNetworkMode));
            edit.commit();
            //UpdateGeneralPreferredNetworkModeSummary(settingsNetworkMode);
        }
        
        //Please make sure this is the last line!!
        updateStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDataUsageListener.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        if (preCfr != null) {
            preCfr.deRegister();
        }
        
        if ((pd != null) && (pd.isShowing())) {
            try {
                pd.dismiss();
            } catch (Exception e) {
                Log.d(LOG_TAG, e.toString());
            }
        }
        
        if (pd != null) {
            pd = null;
        }
    }
    
    private void showProgressDialog() {
        // TODO Auto-generated method stub
        pd = new ProgressDialog(this);
        pd.setMessage(getText(R.string.updating_settings));
        pd.setCancelable(false);
        pd.setIndeterminate(true);
        pd.show();
    }

    /**
     * Implemented to support onPreferenceChangeListener to look for preference
     * changes specifically on CLIR.
     *
     * @param preference is the preference to be changed, should be mButtonCLIR.
     * @param objValue should be the value of the selection, NOT its localized
     * display value.
     */
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mButtonPreferredNetworkMode) {
            //NOTE onPreferenceChange seems to be called even if there is no change
            //Check if the button value is changed from the System.Setting
            mButtonPreferredNetworkMode.setValue((String) objValue);
            int buttonNetworkMode;
            buttonNetworkMode = Integer.valueOf((String) objValue).intValue();
            int settingsNetworkMode = android.provider.Settings.Secure.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Secure.PREFERRED_NETWORK_MODE, preferredNetworkMode);
            if (buttonNetworkMode != settingsNetworkMode) {
                showProgressDialog();
                int modemNetworkMode;
                switch(buttonNetworkMode) {
                    case Phone.NT_MODE_GLOBAL:
                        modemNetworkMode = Phone.NT_MODE_GLOBAL;
                        break;
                    case Phone.NT_MODE_EVDO_NO_CDMA:
                        modemNetworkMode = Phone.NT_MODE_EVDO_NO_CDMA;
                        break;
                    case Phone.NT_MODE_CDMA_NO_EVDO:
                        modemNetworkMode = Phone.NT_MODE_CDMA_NO_EVDO;
                        break;
                    case Phone.NT_MODE_CDMA:
                        modemNetworkMode = Phone.NT_MODE_CDMA;
                        break;
                    case Phone.NT_MODE_GSM_UMTS:
                        modemNetworkMode = Phone.NT_MODE_GSM_UMTS;
                        break;

                    case Phone.NT_MODE_WCDMA_ONLY:
                        modemNetworkMode = Phone.NT_MODE_WCDMA_ONLY;
                        break;
                    case Phone.NT_MODE_GSM_ONLY:
                        modemNetworkMode = Phone.NT_MODE_GSM_ONLY;
                        break;
                    case Phone.NT_MODE_WCDMA_PREF:
                        modemNetworkMode = Phone.NT_MODE_WCDMA_PREF;
                        break;
                    default:
                        modemNetworkMode = Phone.PREFERRED_NT_MODE;
                }
                UpdatePreferredNetworkModeSummary(buttonNetworkMode);

                android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                        buttonNetworkMode );
                //Set the modem network mode
        if (CallSettings.isMultipleSim()) {
                    mGeminiPhone.setPreferredNetworkTypeGemini(modemNetworkMode, mHandler
                            .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE), mSimId);
        } else {
                mPhone.setPreferredNetworkType(modemNetworkMode, mHandler
                        .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
            }
        }
        }

        // always let the preference setting proceed.
        return true;
    }

    private class MyHandler extends Handler {

        private static final int MESSAGE_GET_PREFERRED_NETWORK_TYPE = 0;
        private static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_PREFERRED_NETWORK_TYPE:
                    handleGetPreferredNetworkTypeResponse(msg);
                    break;

                case MESSAGE_SET_PREFERRED_NETWORK_TYPE:
                    handleSetPreferredNetworkTypeResponse(msg);
                    break;
            }
        }

        private void handleGetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception == null) {
                int modemNetworkMode = ((int[])ar.result)[0];

                if (DBG) {
                    log ("handleGetPreferredNetworkTypeResponse: modemNetworkMode = " +
                            modemNetworkMode);
                }

                int settingsNetworkMode = android.provider.Settings.Secure.getInt(
                        mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                        preferredNetworkMode);

                if (DBG) {
                    log("handleGetPreferredNetworkTypeReponse: settingsNetworkMode = " +
                            settingsNetworkMode);
                }

                //check that modemNetworkMode is from an accepted value
                if (modemNetworkMode == Phone.NT_MODE_WCDMA_PREF ||
                        modemNetworkMode == Phone.NT_MODE_GSM_ONLY ||
                        modemNetworkMode == Phone.NT_MODE_WCDMA_ONLY ||
                        modemNetworkMode == Phone.NT_MODE_GSM_UMTS ||
                        modemNetworkMode == Phone.NT_MODE_CDMA ||
                        modemNetworkMode == Phone.NT_MODE_CDMA_NO_EVDO ||
                        modemNetworkMode == Phone.NT_MODE_EVDO_NO_CDMA ||
                        modemNetworkMode == Phone.NT_MODE_GLOBAL ) {
                    if (DBG) {
                        log("handleGetPreferredNetworkTypeResponse: if 1: modemNetworkMode = " +
                                modemNetworkMode);
                    }

                    //check changes in modemNetworkMode and updates settingsNetworkMode
                    if (modemNetworkMode != settingsNetworkMode) {
                        if (DBG) {
                            log("handleGetPreferredNetworkTypeResponse: if 2: " +
                                    "modemNetworkMode != settingsNetworkMode");
                        }

                        settingsNetworkMode = modemNetworkMode;

                        if (DBG) { log("handleGetPreferredNetworkTypeResponse: if 2: " +
                                "settingsNetworkMode = " + settingsNetworkMode);
                        }

                        //changes the Settings.System accordingly to modemNetworkMode
                        android.provider.Settings.Secure.putInt(
                                mPhone.getContext().getContentResolver(),
                                android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                                settingsNetworkMode );
                    }

                    UpdatePreferredNetworkModeSummary(modemNetworkMode);
                    // changes the mButtonPreferredNetworkMode accordingly to modemNetworkMode
                    mButtonPreferredNetworkMode.setValue(Integer.toString(modemNetworkMode));
                } else {
                    if (DBG) log("handleGetPreferredNetworkTypeResponse: else: reset to default");
                    resetNetworkModeToDefault();
                }
            }
        }

        private void handleSetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            if (pd != null && pd.isShowing()) {
                pd.dismiss();
            }
            if (ar.exception == null) {
                int networkMode = Integer.valueOf(
                        mButtonPreferredNetworkMode.getValue()).intValue();
                android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                        networkMode );
            } else {
            if (CallSettings.isMultipleSim()) {
            mGeminiPhone.getPreferredNetworkTypeGemini(obtainMessage(MESSAGE_GET_PREFERRED_NETWORK_TYPE), mSimId);
            } else {
                mPhone.getPreferredNetworkType(obtainMessage(MESSAGE_GET_PREFERRED_NETWORK_TYPE));
            }
        }
        }

        private void resetNetworkModeToDefault() {
            //set the mButtonPreferredNetworkMode
            mButtonPreferredNetworkMode.setValue(Integer.toString(preferredNetworkMode));
            //set the Settings.System
            android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                        preferredNetworkMode );
            //Set the Modem
        if (CallSettings.isMultipleSim()) {
            mGeminiPhone.setPreferredNetworkTypeGemini(preferredNetworkMode,
                        this.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE), mSimId);
        } else {
            mPhone.setPreferredNetworkType(preferredNetworkMode,
                    this.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
        }
    }
    }
    
    
    private void UpdateGeneralPreferredNetworkModeSummary(int NetworkMode) {
        
        /*int index = -1;
        CharSequence[] entries = this.getResources().getStringArray(R.array.gsm_umts_network_preferences_choices);
        CharSequence[] entriesValue = this.getResources().getStringArray(R.array.gsm_umts_network_preferences_values);
        for (int i = 0; i < entriesValue.length; ++i)
        {
            if (entriesValue[i].toString().equals(String.valueOf(NetworkMode)))
            {
                index = i;
                break;
            }
        }
        
        String sum = entries[index].toString();
        switch(NetworkMode) {
            case Phone.NT_MODE_WCDMA_PREF:
                // TODO T: Make all of these strings come from res/values/strings.xml.
                //mButtonPreferredNetworkMode.setSummary("WCDMA pref");
                //Current GSM/WCDMA auto ==  WCDMA pref
                //mButtonPreferredNetworkMode.setSummary("GSM/WCDMA");
                mPreferredNetworkMode.setSummary(sum);
                //mButtonPreferredNetworkMode.getEntry();
                break;
            case Phone.NT_MODE_GSM_ONLY:
                //mButtonPreferredNetworkMode.setSummary("GSM only");
                mPreferredNetworkMode.setSummary(sum);
                break;
            case Phone.NT_MODE_WCDMA_ONLY:
                //mButtonPreferredNetworkMode.setSummary("WCDMA only");
                mPreferredNetworkMode.setSummary(sum);
                break;
                
            default:
                mPreferredNetworkMode.setSummary("Global");
        }*/
        
        android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                NetworkMode );
    }

    private void UpdatePreferredNetworkModeSummary(int NetworkMode) {
        switch(NetworkMode) {
            case Phone.NT_MODE_WCDMA_PREF:
                // TODO T: Make all of these strings come from res/values/strings.xml.
                //mButtonPreferredNetworkMode.setSummary("WCDMA pref");
                //Current GSM/WCDMA auto ==  WCDMA pref
                //mButtonPreferredNetworkMode.setSummary("GSM/WCDMA");
                mButtonPreferredNetworkMode.setSummary(mButtonPreferredNetworkMode.getEntry());
                //mButtonPreferredNetworkMode.getEntry();
                break;
            case Phone.NT_MODE_GSM_ONLY:
                //mButtonPreferredNetworkMode.setSummary("GSM only");
                mButtonPreferredNetworkMode.setSummary(mButtonPreferredNetworkMode.getEntry());
                break;
            case Phone.NT_MODE_WCDMA_ONLY:
                //mButtonPreferredNetworkMode.setSummary("WCDMA only");
                mButtonPreferredNetworkMode.setSummary(mButtonPreferredNetworkMode.getEntry());
                break;
            case Phone.NT_MODE_GSM_UMTS:
                mButtonPreferredNetworkMode.setSummary("GSM/WCDMA");
                break;
            case Phone.NT_MODE_CDMA:
                mButtonPreferredNetworkMode.setSummary("CDMA / EvDo");
                break;
            case Phone.NT_MODE_CDMA_NO_EVDO:
                mButtonPreferredNetworkMode.setSummary("CDMA only");
                break;
            case Phone.NT_MODE_EVDO_NO_CDMA:
                mButtonPreferredNetworkMode.setSummary("EvDo only");
                break;
            case Phone.NT_MODE_GLOBAL:
            default:
                mButtonPreferredNetworkMode.setSummary("Global");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
        case REQUEST_CODE_EXIT_ECM:
            Boolean isChoiceYes =
                data.getBooleanExtra(EmergencyCallbackModeExitDialog.EXTRA_EXIT_ECM_RESULT, false);
            if (isChoiceYes) {
                // If the phone exits from ECM mode, show the CDMA Options
                mCdmaOptions.showDialog(mClickedPreference);
            } else {
                // do nothing
            }
            break;

        default:
            break;
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
    
    private static boolean isAllowedbyCustomization() {
        if ("OP02".equals(PhoneUtils.getOptrProperties())) {
            return false;
        }
        return true;
    }
    
    
    private boolean isUsedGeneralPreference()
    {
        if (!CallSettings.isMultipleSim())
        {
            return false;
        }
        List<SIMInfo> simList = SIMInfo.getInsertedSIMList(this);
        boolean found3g = false;
        for (SIMInfo info: simList) {
            if (isSupport3G(info.mSlot)) {
                found3g = true;
                break;
            }
        }
        return (simList.size() > 1) && found3g;
    }
    
    public static boolean isSupport3G(int slot)
    {
        //For current, we suppose only two slot support
        if (slot < 0 || slot > 1) {
            return false;
        }
        String propertyKey = "gsm.baseband.capability";
        String capability = null;
        if (slot == 1) {
            propertyKey += "2";
        }
        capability = SystemProperties.get(propertyKey);
        if (capability == null || "".equals(capability)) {
            return false;
        }
        
        int value = 0;
        try {
            value = Integer.valueOf(capability, 16);
        }catch (NumberFormatException ne) {
            return false;
        }
        
        // GPRS: 0x01
        // EDGE: 0x02
        // WCDMA: 0x04
        // TD-SCDMA: 0x08
        // HSDPA: 0x10
        // HSUPA: 0x20
        // HSPA+: 0x40   // Reserve 
        // LTE: 0x80 // Reserve 
        if (value <= 0x3) {
            return false;
        }
        
        return true;
    }

    //handle data state changed
    public static int DATA_STATE_CHANGE_TIMEOUT = 2001;
    private boolean isChangeData = false;
    Handler mH = new Handler() {
        
        public void handleMessage(Message msg) {
            if (msg.what == DATA_STATE_CHANGE_TIMEOUT) {
                if (pd != null && pd.isShowing() && isChangeData) {
                    try {
                        pd.dismiss();
                    } catch (Exception e) {
                        Log.d(LOG_TAG, e.toString());
                    }
                    pd = null;
                    isChangeData = false;
                }
            }
        }
    };
    
    private static Phone.DataState getMobileDataState(Intent intent) {
        String str = intent.getStringExtra(Phone.STATE_KEY);
        if (str != null) {
            return Enum.valueOf(Phone.DataState.class, str);
        } else {
            return Phone.DataState.DISCONNECTED;
        }
    }
}
