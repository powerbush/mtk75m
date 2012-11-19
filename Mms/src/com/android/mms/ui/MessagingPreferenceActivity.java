/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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

package com.android.mms.ui;

import java.util.List;
import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.SearchRecentSuggestions;
import android.telephony.TelephonyManager;
import android.text.InputFilter;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import com.android.mms.ui.NumberPickerDialog.NonWrapNumberPicker;
import com.android.mms.util.Recycler;
import com.mediatek.featureoption.FeatureOption;
import com.android.internal.telephony.Phone;
import android.provider.Telephony.SIMInfo;
import com.mediatek.telephony.TelephonyManagerEx;
import android.os.Handler;
import android.view.inputmethod.EditorInfo;
import android.preference.Preference.OnPreferenceChangeListener;
import android.os.SystemProperties;

/**
 * With this activity, users can set preferences for MMS and SMS and
 * can access and manipulate SMS messages stored on the SIM.
 */
public class MessagingPreferenceActivity extends PreferenceActivity
    implements Preference.OnPreferenceChangeListener{
    
    private static final String TAG = "MessagingPreferenceActivity";
    private static final boolean DEBUG = false;
    // Symbolic names for the keys used for preference lookup
    public static final String MMS_DELIVERY_REPORT_MODE = "pref_key_mms_delivery_reports";
    public static final String EXPIRY_TIME              = "pref_key_mms_expiry";
    public static final String PRIORITY                 = "pref_key_mms_priority";
    public static final String READ_REPORT_MODE         = "pref_key_mms_read_reports";
    public static final String SMS_DELIVERY_REPORT_MODE = "pref_key_sms_delivery_reports";
    public static final String NOTIFICATION_ENABLED     = "pref_key_enable_notifications";
    public static final String NOTIFICATION_RINGTONE    = "pref_key_ringtone";
    public static final String AUTO_RETRIEVAL           = "pref_key_mms_auto_retrieval";
    public static final String RETRIEVAL_DURING_ROAMING = "pref_key_mms_retrieval_during_roaming";
    public static final String AUTO_DELETE              = "pref_key_auto_delete";
    public static final String CREATION_MODE            = "pref_key_mms_creation_mode";
    public static final String MMS_SIZE_LIMIT           = "pref_key_mms_size_limit";
    public static final String SMS_QUICK_TEXT_EDITOR    = "pref_key_quick_text_editor";
    public static final String SMS_SERVICE_CENTER       = "pref_key_sms_service_center";
    public static final String SMS_MANAGE_SIM_MESSAGES  = "pref_key_manage_sim_messages";
    public static final String SMS_SAVE_LOCATION        = "pref_key_sms_save_location";
    public static final String MMS_ENABLE_TO_SEND_DELIVERY_REPORT = "pref_key_mms_enable_to_send_delivery_reports";
    // Menu entries
    
    private static final int MENU_RESTORE_DEFAULTS    = 1;
    private final int MAX_EDITABLE_LENGTH = 20;
    private Preference mStorageStatusPref;
    private Preference mSmsLimitPref;
    private Preference mSmsQuickTextEditorPref;
    private Preference mMmsLimitPref;
    private Preference mManageSimPref;
    private Preference mClearHistoryPref;
    private Recycler mSmsRecycler;
    private Recycler mMmsRecycler;
    private Preference mSmsServiceCenterPref;
    
    // all preferences need change key for single sim card
    private CheckBoxPreference mSmsDeliveryReport;
    private CheckBoxPreference mMmsDeliveryReport;
    private CheckBoxPreference mMmsEnableToSendDeliveryReport;
    private CheckBoxPreference mMmsReadReport;
    private CheckBoxPreference mMmsAutoRetrieval;
    private CheckBoxPreference mMmsRetrievalDuringRoaming;
    
    // all preferences need change key for multiple sim card
    private Preference mSmsDeliveryReportMultiSim;
    private Preference mMmsDeliveryReportMultiSim;
    private Preference mMmsEnableToSendDeliveryReportMultiSim;
    private Preference mMmsReadReportMultiSim;
    private Preference mMmsAutoRetrievalMultiSim;
    private Preference mMmsRetrievalDuringRoamingMultiSim;
    private Preference mSmsServiceCenterPrefMultiSim;
    private Preference mManageSimPrefMultiSim;
    private Preference mSmsSaveLoactionMultiSim;
    
    private ListPreference mMmsPriority;
    private ListPreference mSmsLocation;
    private ListPreference mMmsCreationMode;
    private ListPreference mMmsSizeLimit;
    
    private static final int CONFIRM_CLEAR_SEARCH_HISTORY_DIALOG = 3;
    private static final String PRIORITY_HIGH= "High";
    private static final String PRIORITY_LOW= "Low";
    private static final String PRIORITY_NORMAL= "Normal";
    
    private static final String LOCATION_PHONE = "Phone";
    private static final String LOCATION_SIM = "Sim";
    
    private static final String CREATION_MODE_RESTRICTED = "RESTRICTED";
    private static final String CREATION_MODE_WARNING = "WARNING";
    private static final String CREATION_MODE_FREE = "FREE";
    
    private static final String SIZE_LIMIT_100 = "100";
    private static final String SIZE_LIMIT_200 = "200";
    private static final String SIZE_LIMIT_300 = "300";
    
    
    //NumberPickerDialog numberPicker;

    private static Handler mSMSHandler = new Handler();
    private static Handler mMMSHandler = new Handler();
    private EditText mNumberText;
    private AlertDialog mNumberTextDialog;
    private List<SIMInfo> listSimInfo;
    private TelephonyManagerEx mTelephonyManager;
    int slotId;
    private NumberPickerDialog mSmsDisplayLimitDialog;
    private NumberPickerDialog mMmsDisplayLimitDialog;
    private NonWrapNumberPicker mNumberPicker;
    private EditText inputNumber;
    private int currentSimCount = 0;
    @Override
    protected void onPause(){
        super.onPause();
        if (mSmsDisplayLimitDialog != null ) {
            mSmsDisplayLimitDialog.dismiss();
        }
        if (mMmsDisplayLimitDialog != null ) {
            mMmsDisplayLimitDialog.dismiss();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if ((mSmsDisplayLimitDialog != null)
                && (mSmsDisplayLimitDialog.isShowing())) {
            mNumberPicker = (NonWrapNumberPicker) mSmsDisplayLimitDialog
                    .findViewById(R.id.number_picker);
            inputNumber = (EditText) mNumberPicker
                    .findViewById(R.id.timepicker_input);
            String inputString = inputNumber.getText().toString();
            int limitNumber = 0;
            if ((inputString == null || inputString.equals(""))) {
                limitNumber = mSmsRecycler.getMessageMinLimit();
            }else{
                limitNumber = Integer.parseInt(inputString);
            }
            mSmsDisplayLimitDialog.dismiss();
            mSmsDisplayLimitDialog = new NumberPickerDialog(this,
                    mSmsLimitListener, limitNumber,
                    mSmsRecycler.getMessageMinLimit(), mSmsRecycler
                            .getMessageMaxLimit(),
                    R.string.pref_title_sms_delete);
            mSmsDisplayLimitDialog.show();
        }
        if ((mMmsDisplayLimitDialog != null)
                && (mMmsDisplayLimitDialog.isShowing())) {
            mNumberPicker = (NonWrapNumberPicker) mMmsDisplayLimitDialog
                    .findViewById(R.id.number_picker);
            inputNumber = (EditText) mNumberPicker
                    .findViewById(R.id.timepicker_input);
            String inputString = inputNumber.getText().toString();
            int limitNumber = 0;
            if ((inputString == null || inputString.equals(""))) {
                limitNumber = mMmsRecycler.getMessageMinLimit();
            }else{
                limitNumber = Integer.parseInt(inputString);
            } 
            mMmsDisplayLimitDialog.dismiss();
            mMmsDisplayLimitDialog = new NumberPickerDialog(this,
                    mMmsLimitListener, limitNumber,
                    mMmsRecycler.getMessageMinLimit(), mMmsRecycler
                            .getMessageMaxLimit(),
                    R.string.pref_title_mms_delete);
            mMmsDisplayLimitDialog.show();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        //For mMmsPriority;
        String stored = sp.getString(PRIORITY, getString(R.string.priority_normal));
        mMmsPriority.setSummary(getVisualTextName(stored, R.array.pref_key_mms_priority_choices,
                R.array.pref_key_mms_priority_values));
        
        //For mSmsLocation;
        //MTK_OP02_PROTECT_START
        String optr = SystemProperties.get("ro.operator.optr");
        if ("OP02".equals(optr)) {
            if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
                int currentSimCount = SIMInfo.getInsertedSIMCount(this);
                int slotId = 0;
                if (currentSimCount == 1) {
                    slotId = SIMInfo.getInsertedSIMList(this).get(0).mSlot;
                    stored = sp.getString((Long.toString(slotId) + "_" + SMS_SAVE_LOCATION), "Phone");
                }
                Log.d(TAG, "setListPrefSummary stored slotId = "+ slotId + " stored =" + stored);
            } else {
                stored = sp.getString(SMS_SAVE_LOCATION, "Phone");
                Log.d(TAG, "setListPrefSummary stored 2 =" + stored);
            }
        } else {
        //MTK_OP02_PROTECT_END
              stored = sp.getString(SMS_SAVE_LOCATION, "Phone");
              Log.d(TAG, "setListPrefSummary stored 3 =" + stored);
       //MTK_OP02_PROTECT_START
        }
        //MTK_OP02_PROTECT_END
        mSmsLocation.setSummary(getVisualTextName(stored, R.array.pref_sms_save_location_choices,
                R.array.pref_sms_save_location_values));
        
        //For mMmsCreationMode
        stored = sp.getString(CREATION_MODE, "FREE");
        mMmsCreationMode.setSummary(getVisualTextName(stored, R.array.pref_mms_creation_mode_choices,
                R.array.pref_mms_creation_mode_values));
        
        //For mMmsSizeLimit
        stored = sp.getString(MMS_SIZE_LIMIT, "300");
        mMmsSizeLimit.setSummary(getVisualTextName(stored, R.array.pref_mms_size_limit_choices,
                R.array.pref_mms_size_limit_values));
    }
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.i(TAG, "onCreate");
        if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
            Log.i(TAG, "MTK_GEMINI_SUPPORT is true");
            currentSimCount = SIMInfo.getInsertedSIMCount(this);
            Log.i(TAG, "currentSimCount is :" + currentSimCount);
            if (currentSimCount <= 1) {
                addPreferencesFromResource(R.xml.preferences);
                // MTK_OP01_PROTECT_START
                String optr1 = SystemProperties.get("ro.operator.optr");
                if ("OP01".equals(optr1)) {
                mMmsEnableToSendDeliveryReport = (CheckBoxPreference) findPreference(MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
                } else
                // MTK_OP01_PROTECT_END
                {
                mMmsEnableToSendDeliveryReport = (CheckBoxPreference) findPreference(MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
                PreferenceCategory mmsCategory = (PreferenceCategory) findPreference("pref_key_mms_settings");
                mmsCategory.removePreference(mMmsEnableToSendDeliveryReport);
                } 
                
            } else {
                addPreferencesFromResource(R.xml.multicardpreferences);
            }
        } else {
            addPreferencesFromResource(R.xml.preferences);
             // MTK_OP01_PROTECT_START
            String optr1 = SystemProperties.get("ro.operator.optr");
            if ("OP01".equals(optr1)) {
                mMmsEnableToSendDeliveryReport = (CheckBoxPreference) findPreference(MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
            } else
            // MTK_OP01_PROTECT_END
            {
                mMmsEnableToSendDeliveryReport = (CheckBoxPreference) findPreference(MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
                PreferenceCategory mmsCategory = (PreferenceCategory) findPreference("pref_key_mms_settings");
                mmsCategory.removePreference(mMmsEnableToSendDeliveryReport);
            } 
        }
        // MTK_OP01_PROTECT_START
        String optr1 = SystemProperties.get("ro.operator.optr");
        if ("OP01".equals(optr1)) { 
            mStorageStatusPref = findPreference("pref_key_storage_status");
        } else
        // MTK_OP01_PROTECT_END
        { 

            mStorageStatusPref = findPreference("pref_key_storage_status");
            PreferenceCategory storageCategory = (PreferenceCategory) findPreference("pref_key_storage_settings");
            storageCategory.removePreference(mStorageStatusPref);
        } 
        
        mSmsLimitPref = findPreference("pref_key_sms_delete_limit"); 
        mMmsLimitPref = findPreference("pref_key_mms_delete_limit");
        mClearHistoryPref = findPreference("pref_key_mms_clear_history");
        mSmsQuickTextEditorPref = findPreference("pref_key_quick_text_editor");
        
        mMmsPriority = (ListPreference) findPreference("pref_key_mms_priority");
        mMmsPriority.setOnPreferenceChangeListener(this);
        mSmsLocation = (ListPreference) findPreference(SMS_SAVE_LOCATION);
        mSmsLocation.setOnPreferenceChangeListener(this);
        mMmsCreationMode = (ListPreference) findPreference("pref_key_mms_creation_mode");
        mMmsCreationMode.setOnPreferenceChangeListener(this);
        mMmsSizeLimit = (ListPreference) findPreference("pref_key_mms_size_limit");
        mMmsSizeLimit.setOnPreferenceChangeListener(this);

        PreferenceCategory smsCategory =
            (PreferenceCategory)findPreference("pref_key_sms_settings");
        if(FeatureOption.MTK_GEMINI_SUPPORT == true){ 
            if (currentSimCount == 0){
                
                // No SIM card, remove the SIM-related prefs
                //smsCategory.removePreference(mManageSimPref);
                //If there is no SIM, this item will be disabled and can not be accessed.
                mManageSimPref = findPreference(SMS_MANAGE_SIM_MESSAGES);
                mManageSimPref.setEnabled(false);
                //MTK_OP02_PROTECT_START
                String optr = SystemProperties.get("ro.operator.optr");
                if ("OP02".equals(optr)) {
                    smsCategory.removePreference(mManageSimPref);
                }
                //MTK_OP02_PROTECT_END
                mSmsServiceCenterPref = findPreference("pref_key_sms_service_center");
                mSmsServiceCenterPref.setEnabled(false);
            }
        } else {
             if (!MmsApp.getApplication().getTelephonyManager().hasIccCard()) {
                 //smsCategory.removePreference(mManageSimPref);
                 //If there is no SIM, this item will be disabled and can not be accessed.
                 mManageSimPref = findPreference(SMS_MANAGE_SIM_MESSAGES);
                 mManageSimPref.setEnabled(false);
                 //MTK_OP02_PROTECT_START
                 String optr = SystemProperties.get("ro.operator.optr");
                 if ("OP02".equals(optr)) {
                    smsCategory.removePreference(mManageSimPref);
                 }
                 //MTK_OP02_PROTECT_END
                 mSmsServiceCenterPref = findPreference("pref_key_sms_service_center");
                 mSmsServiceCenterPref.setEnabled(false);
             } else {
                 mManageSimPref = findPreference(SMS_MANAGE_SIM_MESSAGES);
                 //MTK_OP02_PROTECT_START
                 String optr = SystemProperties.get("ro.operator.optr");
                 if ("OP02".equals(optr)) {
                     smsCategory.removePreference(mManageSimPref);
                 }
                 //MTK_OP02_PROTECT_END
                 mSmsServiceCenterPref = findPreference("pref_key_sms_service_center");
             }
        }
        if (!MmsConfig.getMmsEnabled()) {
            // No Mms, remove all the mms-related preferences
            PreferenceCategory mmsOptions =
                (PreferenceCategory)findPreference("pref_key_mms_settings");
            getPreferenceScreen().removePreference(mmsOptions);

            PreferenceCategory storageOptions =
                (PreferenceCategory)findPreference("pref_key_storage_settings");
            storageOptions.removePreference(findPreference("pref_key_mms_delete_limit"));
        }
        

        enablePushSetting();
        
        mSmsRecycler = Recycler.getSmsRecycler();
        mMmsRecycler = Recycler.getMmsRecycler();

        // Fix up the recycler's summary with the correct values
        setSmsDisplayLimit();
        setMmsDisplayLimit();
        
        // Change the key to the SIM-related key, if has one SIM card, else set default value.
        if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
            Log.i(TAG, "MTK_GEMINI_SUPPORT is true");
            if (currentSimCount == 1) {
                Log.i(TAG, "single sim");
                changeSingleCardKeyToSimRelated();
            } else if (currentSimCount > 1) {
                setMultiCardPreference();
            }
        }
    }
    
    private void changeSingleCardKeyToSimRelated() {
        // get to know which one
        listSimInfo = SIMInfo.getInsertedSIMList(this);
        SIMInfo singleCardInfo = null;
        if (listSimInfo.size() != 0) {
            singleCardInfo = listSimInfo.get(0);
        }
        if (singleCardInfo == null) {
            return;
        }
        Long simId = listSimInfo.get(0).mSimId;
        Log.i(TAG,"Got simId = " + simId);
        //translate all key to SIM-related key;
        mSmsDeliveryReport = (CheckBoxPreference) findPreference(SMS_DELIVERY_REPORT_MODE);
        mMmsDeliveryReport = (CheckBoxPreference) findPreference(MMS_DELIVERY_REPORT_MODE);
        mMmsReadReport = (CheckBoxPreference) findPreference(READ_REPORT_MODE);
        mMmsAutoRetrieval = (CheckBoxPreference) findPreference(AUTO_RETRIEVAL);
        mMmsRetrievalDuringRoaming = (CheckBoxPreference) findPreference(RETRIEVAL_DURING_ROAMING);
        mSmsServiceCenterPref = findPreference(SMS_SERVICE_CENTER);
        mManageSimPref = findPreference(SMS_MANAGE_SIM_MESSAGES);
        mManageSimPrefMultiSim = null;
        //MTK_OP02_PROTECT_START
        PreferenceCategory smsCategory =
            (PreferenceCategory)findPreference("pref_key_sms_settings");
        String optr = SystemProperties.get("ro.operator.optr");
        if ("OP02".equals(optr)) {
            if (mManageSimPref != null) {
                smsCategory.removePreference(mManageSimPref);
            }
            int slotid = listSimInfo.get(0).mSlot;
            mSmsLocation = (ListPreference) findPreference(SMS_SAVE_LOCATION);
            mSmsLocation.setKey(Long.toString(slotid) + "_" + SMS_SAVE_LOCATION);
            //get the stored value
            SharedPreferences spr = getSharedPreferences("com.android.mms_preferences", MODE_WORLD_READABLE);
            mSmsLocation.setValue(spr.getString((Long.toString(slotid) + "_" + SMS_SAVE_LOCATION), "Phone"));
        }
        //MTK_OP02_PROTECT_END
        mSmsDeliveryReport.setKey(Long.toString(simId) + "_" + SMS_DELIVERY_REPORT_MODE);
        mMmsDeliveryReport.setKey(Long.toString(simId) + "_" + MMS_DELIVERY_REPORT_MODE);  
        mMmsReadReport.setKey(Long.toString(simId) + "_" + READ_REPORT_MODE);
        mMmsAutoRetrieval.setKey(Long.toString(simId) + "_" + AUTO_RETRIEVAL);
        mMmsRetrievalDuringRoaming.setDependency(Long.toString(simId) + "_" + AUTO_RETRIEVAL);
        mMmsRetrievalDuringRoaming.setKey(Long.toString(simId) + "_" + RETRIEVAL_DURING_ROAMING);
        
        //MTK_OP01_PROTECT_START
        if (optr.equals("OP01")) {
            mMmsEnableToSendDeliveryReport = (CheckBoxPreference) findPreference(MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
            mMmsEnableToSendDeliveryReport.setKey(Long.toString(simId) + "_" + MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
        } else
        //MTK_OP01_PROTECT_END
        {
            mMmsEnableToSendDeliveryReport = (CheckBoxPreference) findPreference(MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
            if(mMmsEnableToSendDeliveryReport != null){
                mMmsEnableToSendDeliveryReport.setKey(Long.toString(simId) + "_" + MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
                PreferenceCategory mmsCategory = (PreferenceCategory)findPreference("pref_key_mms_settings");
                mmsCategory.removePreference(mMmsEnableToSendDeliveryReport);
            } 
        }

        //get the stored value
        SharedPreferences sp = getSharedPreferences("com.android.mms_preferences", MODE_WORLD_READABLE);
        if (mSmsDeliveryReport != null) {
            mSmsDeliveryReport.setChecked(sp.getBoolean(mSmsDeliveryReport.getKey(), false));
        }
        if (mMmsDeliveryReport != null) {
            mMmsDeliveryReport.setChecked(sp.getBoolean(mMmsDeliveryReport.getKey(), false));
        }
        if (mMmsEnableToSendDeliveryReport != null) {
            mMmsEnableToSendDeliveryReport.setChecked(sp.getBoolean(mMmsEnableToSendDeliveryReport.getKey(), true));
        }
        if (mMmsReadReport != null) {
            mMmsReadReport.setChecked(sp.getBoolean(mMmsReadReport.getKey(), false));
        }
        if (mMmsAutoRetrieval != null) {
            mMmsAutoRetrieval.setChecked(sp.getBoolean(mMmsAutoRetrieval.getKey(), true));
        }
        if (mMmsRetrievalDuringRoaming != null) {
            mMmsRetrievalDuringRoaming.setChecked(sp.getBoolean(mMmsRetrievalDuringRoaming.getKey(), false));
        }
    }
    
    private void setMultiCardPreference() {    
        mSmsDeliveryReportMultiSim = findPreference(SMS_DELIVERY_REPORT_MODE);
        mMmsDeliveryReportMultiSim = findPreference(MMS_DELIVERY_REPORT_MODE);
        String optr = SystemProperties.get("ro.operator.optr");
        //MTK_OP01_PROTECT_START
        if ("OP01".equals(optr)) {
            mMmsEnableToSendDeliveryReportMultiSim = findPreference(MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
        } else
        //MTK_OP01_PROTECT_END
        {
            mMmsEnableToSendDeliveryReportMultiSim = findPreference(MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
            PreferenceCategory mmsCategory =
                (PreferenceCategory)findPreference("pref_key_mms_settings");
            mmsCategory.removePreference(mMmsEnableToSendDeliveryReportMultiSim);
        }
        mMmsReadReportMultiSim = findPreference(READ_REPORT_MODE);
        mMmsAutoRetrievalMultiSim = findPreference(AUTO_RETRIEVAL);
        mMmsRetrievalDuringRoamingMultiSim = findPreference(RETRIEVAL_DURING_ROAMING);
        mSmsServiceCenterPrefMultiSim = findPreference(SMS_SERVICE_CENTER);
        mManageSimPrefMultiSim = findPreference(SMS_MANAGE_SIM_MESSAGES);
        mManageSimPref = null;
       //MTK_OP02_PROTECT_START
        PreferenceCategory smsCategory =
            (PreferenceCategory)findPreference("pref_key_sms_settings");
        if ("OP02".equals(optr)) {
            if (mManageSimPrefMultiSim != null) {
                smsCategory.removePreference(mManageSimPrefMultiSim);
            }
            if (mSmsLocation != null) {
                smsCategory.removePreference(mSmsLocation);
                Preference saveLocationMultiSim = new Preference(this);
                saveLocationMultiSim.setKey(SMS_SAVE_LOCATION);
                saveLocationMultiSim.setTitle(R.string.sms_save_location);
                saveLocationMultiSim.setSummary(R.string.sms_save_location);
                smsCategory.addPreference(saveLocationMultiSim);
                mSmsSaveLoactionMultiSim = findPreference(SMS_SAVE_LOCATION);
           }
        }
        //MTK_OP02_PROTECT_END
    }
    private void setSmsDisplayLimit() {
        mSmsLimitPref.setSummary(
                getString(R.string.pref_summary_delete_limit,
                        mSmsRecycler.getMessageLimit(this)));
    }

    private void setMmsDisplayLimit() {
        mMmsLimitPref.setSummary(
                getString(R.string.pref_summary_delete_limit,
                        mMmsRecycler.getMessageLimit(this)));
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.clear();
        menu.add(0, MENU_RESTORE_DEFAULTS, 0, R.string.restore_default);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESTORE_DEFAULTS:
                restoreDefaultPreferences();
                return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mStorageStatusPref) {
            final String memoryStatus = MessageUtils.getStorageStatus(getApplicationContext());
            new AlertDialog.Builder(MessagingPreferenceActivity.this)
                    .setTitle(R.string.pref_title_storage_status)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setMessage(memoryStatus)
                    .setPositiveButton(android.R.string.ok, null)
                    .setCancelable(true)
                    .show();
        } else if (preference == mSmsLimitPref) {
            mSmsDisplayLimitDialog = 
            new NumberPickerDialog(this,
                    mSmsLimitListener,
                    mSmsRecycler.getMessageLimit(this),
                    mSmsRecycler.getMessageMinLimit(),
                    mSmsRecycler.getMessageMaxLimit(),
                    R.string.pref_title_sms_delete);
            mSmsDisplayLimitDialog.show();
        } else if (preference == mMmsLimitPref) {
            mMmsDisplayLimitDialog = 
            new NumberPickerDialog(this,
                    mMmsLimitListener,
                    mMmsRecycler.getMessageLimit(this),
                    mMmsRecycler.getMessageMinLimit(),
                    mMmsRecycler.getMessageMaxLimit(),
                    R.string.pref_title_mms_delete);
            mMmsDisplayLimitDialog.show();
        } else if (preference == mManageSimPref) {    
            if(FeatureOption.MTK_GEMINI_SUPPORT == true){
                listSimInfo = SIMInfo.getInsertedSIMList(this);
                int slotId = listSimInfo.get(0).mSlot;
                Log.i(TAG, "slotId is : " + slotId);
                if (slotId != -1) {
                    Intent it = new Intent();
                    it.setClass(this, ManageSimMessages.class);
                    it.putExtra("SlotId", slotId);
                    it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(it);
                }
            } else {
                startActivity(new Intent(this, ManageSimMessages.class));
            }
        } else if (preference == mClearHistoryPref) {
            showDialog(CONFIRM_CLEAR_SEARCH_HISTORY_DIALOG);
            return true;
        } else if (preference == mSmsQuickTextEditorPref) {
            Intent intent = new Intent();
            intent.setClass(this, SmsTemplateEditActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else if (preference == mSmsDeliveryReportMultiSim 
                || preference == mMmsDeliveryReportMultiSim
                || preference == mMmsEnableToSendDeliveryReportMultiSim
                || preference == mMmsReadReportMultiSim 
                || preference == mMmsAutoRetrievalMultiSim 
                || preference == mMmsRetrievalDuringRoamingMultiSim) {
            
            Intent it = new Intent();
            it.setClass(this, MultiSimPreferenceActivity.class);
            it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            it.putExtra("preference", preference.getKey());
            startActivity(it);
        } else if (preference == mSmsServiceCenterPref) {
            mNumberText = new EditText(this);
            mNumberText.setHint(R.string.type_to_compose_text_enter_to_send);
            mNumberText.computeScroll();
            mNumberText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_EDITABLE_LENGTH)});
            //mNumberText.setKeyListener(new DigitsKeyListener(false, true));
            mNumberText.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_CLASS_PHONE);
            mTelephonyManager = TelephonyManagerEx.getDefault();
            String gotScNumber;
            if(FeatureOption.MTK_GEMINI_SUPPORT == true){
                int slotId = listSimInfo.get(0).mSlot;
                gotScNumber = mTelephonyManager.getScAddress(slotId);
            } else {
                gotScNumber = mTelephonyManager.getScAddress(0);
            }
            Log.i(TAG, "gotScNumber is: " + gotScNumber);
            mNumberText.setText(gotScNumber);
            mNumberTextDialog = new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle(R.string.sms_service_center)
            .setView(mNumberText)
            .setPositiveButton(R.string.OK, new PositiveButtonListener())
            .setNegativeButton(R.string.Cancel, new NegativeButtonListener())
            .show();
        } else if (preference == mSmsServiceCenterPrefMultiSim
                || preference == mManageSimPrefMultiSim
                ||(preference == mSmsSaveLoactionMultiSim && currentSimCount > 1
                        && "OP02".equals(SystemProperties.get("ro.operator.optr")))) {
            Intent it = new Intent();
            it.setClass(this, SelectCardPreferenceActivity.class);
            it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            it.putExtra("preference", preference.getKey());
            startActivity(it);
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private class PositiveButtonListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
            // write to the SIM Card.
            mTelephonyManager = TelephonyManagerEx.getDefault();
            if(FeatureOption.MTK_GEMINI_SUPPORT == true){
                slotId = listSimInfo.get(0).mSlot;
            } else {
                slotId = 0;
            }
            new Thread(new Runnable() {
                public void run() {
                    mTelephonyManager.setScAddress(mNumberText.getText().toString(), slotId);
                }
            }).start();
        }
    }
    
    private class NegativeButtonListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
            // cancel
            dialog.dismiss();
        }
    }
    private void restoreDefaultPreferences() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit().clear().apply();
        setPreferenceScreen(null);
        if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
            currentSimCount = SIMInfo.getInsertedSIMCount(this);
            Log.i(TAG, "currentSimCount is :" + currentSimCount);
            if (currentSimCount <= 1) {
                addPreferencesFromResource(R.xml.preferences);
                 // MTK_OP01_PROTECT_START
                String optr1 = SystemProperties.get("ro.operator.optr");
                if ("OP01".equals(optr1)) {
                    mMmsEnableToSendDeliveryReport = (CheckBoxPreference) findPreference(MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
                } else
                // MTK_OP01_PROTECT_END
                {
                    mMmsEnableToSendDeliveryReport = (CheckBoxPreference) findPreference(MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
                    PreferenceCategory mmsCategory = (PreferenceCategory) findPreference("pref_key_mms_settings");
                    mmsCategory.removePreference(mMmsEnableToSendDeliveryReport);
                } 
            } else {
                addPreferencesFromResource(R.xml.multicardpreferences);
            }
        } else {
            addPreferencesFromResource(R.xml.preferences);
             // MTK_OP01_PROTECT_START
            String optr1 = SystemProperties.get("ro.operator.optr");
            if ("OP01".equals(optr1)) {
                mMmsEnableToSendDeliveryReport = (CheckBoxPreference) findPreference(MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
            } else
            // MTK_OP01_PROTECT_END
            {
                mMmsEnableToSendDeliveryReport = (CheckBoxPreference) findPreference(MMS_ENABLE_TO_SEND_DELIVERY_REPORT);
                PreferenceCategory mmsCategory = (PreferenceCategory) findPreference("pref_key_mms_settings");
                mmsCategory.removePreference(mMmsEnableToSendDeliveryReport);
            } 
        }

        //MTK_OP01_PROTECT_START
        String optr1 = SystemProperties.get("ro.operator.optr");
        if ("OP01".equals(optr1)) {
            mStorageStatusPref = findPreference("pref_key_storage_status");
        } else
        //MTK_OP01_PROTECT_END
        {
            mStorageStatusPref = findPreference("pref_key_storage_status");
            PreferenceCategory storageCategory = (PreferenceCategory)findPreference("pref_key_storage_settings");
            storageCategory.removePreference(mStorageStatusPref);
        }
        mManageSimPref = findPreference("pref_key_manage_sim_messages");
        //MTK_OP02_PROTECT_START
        PreferenceCategory smsCategory =
            (PreferenceCategory)findPreference("pref_key_sms_settings");
        String optr = SystemProperties.get("ro.operator.optr");
        if ("OP02".equals(optr)) {
            smsCategory.removePreference(mManageSimPref);
        }
        //MTK_OP02_PROTECT_END
        mSmsLimitPref = findPreference("pref_key_sms_delete_limit");
        mMmsLimitPref = findPreference("pref_key_mms_delete_limit");
        mSmsQuickTextEditorPref = findPreference("pref_key_quick_text_editor");
        mMmsPriority = (ListPreference) findPreference("pref_key_mms_priority");
        mMmsPriority.setOnPreferenceChangeListener(this);
        
        mSmsLocation = (ListPreference) findPreference(SMS_SAVE_LOCATION);
        mSmsLocation.setOnPreferenceChangeListener(this);
        mMmsCreationMode = (ListPreference) findPreference("pref_key_mms_creation_mode");
        mMmsCreationMode.setOnPreferenceChangeListener(this);
        mMmsSizeLimit = (ListPreference) findPreference("pref_key_mms_size_limit");
        mMmsSizeLimit.setOnPreferenceChangeListener(this);
        
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        //For mMmsPriority;
        String stored = sp.getString(PRIORITY, getString(R.string.priority_normal)); 
        mMmsPriority.setSummary(getVisualTextName(stored, R.array.pref_key_mms_priority_choices,
                R.array.pref_key_mms_priority_values));
            //For mSmsLocation;
        stored = sp.getString(SMS_SAVE_LOCATION, "Phone");
        mSmsLocation.setSummary(getVisualTextName(stored, R.array.pref_sms_save_location_choices,
                 R.array.pref_sms_save_location_values));
 
        //For mMmsCreationMode
        stored = sp.getString(CREATION_MODE, "FREE");
        mMmsCreationMode.setSummary(getVisualTextName(stored, R.array.pref_mms_creation_mode_choices,
                R.array.pref_mms_creation_mode_values));
        
        //For mMmsSizeLimit
        stored = sp.getString(MMS_SIZE_LIMIT, "300");
        mMmsSizeLimit.setSummary(getVisualTextName(stored, R.array.pref_mms_size_limit_choices,
                R.array.pref_mms_size_limit_values));
        
        if(FeatureOption.MTK_GEMINI_SUPPORT == true){ 
            if (currentSimCount == 0){
                // No SIM card, remove the SIM-related prefs
                PreferenceCategory smsCategory1 =
                    (PreferenceCategory)findPreference("pref_key_sms_settings");
                //smsCategory.removePreference(mManageSimPref);
                //If there is no SIM, this item will be disabled and can not be accessed.
                mManageSimPref = findPreference(SMS_MANAGE_SIM_MESSAGES);
                 if (null != mManageSimPref) {
                    mManageSimPref.setEnabled(false);
                    //MTK_OP02_PROTECT_START
                if ("OP02".equals(optr)) {
                        smsCategory1.removePreference(mManageSimPref);
                    }}
                //MTK_OP02_PROTECT_END
                mSmsServiceCenterPref = findPreference("pref_key_sms_service_center");
                mSmsServiceCenterPref.setEnabled(false);
            }
        } else {
             if (!MmsApp.getApplication().getTelephonyManager().hasIccCard()) {
                 PreferenceCategory smsCategory1 =
                     (PreferenceCategory)findPreference("pref_key_sms_settings");
                 //smsCategory.removePreference(mManageSimPref);
                 //If there is no SIM, this item will be disabled and can not be accessed.
                 mManageSimPref = findPreference(SMS_MANAGE_SIM_MESSAGES);
                 if (null != mManageSimPref) {
                 mManageSimPref.setEnabled(false);
                 //MTK_OP02_PROTECT_START
                 if ("OP02".equals(optr)) {
                     smsCategory1.removePreference(mManageSimPref);
                 }}
                 //MTK_OP02_PROTECT_END
                 mSmsServiceCenterPref = findPreference("pref_key_sms_service_center");
                 mSmsServiceCenterPref.setEnabled(false);
             } else {
                 mManageSimPref = findPreference(SMS_MANAGE_SIM_MESSAGES);
                //MTK_OP02_PROTECT_START
                 if ("OP02".equals(optr)) {
                     getPreferenceScreen().removePreference(mManageSimPref);
                 }
                 //MTK_OP02_PROTECT_END
                 mSmsServiceCenterPref = findPreference("pref_key_sms_service_center");
             }
        }
        if (!MmsConfig.getMmsEnabled()) {
            // No Mms, remove all the mms-related preferences
            PreferenceCategory mmsOptions =
                (PreferenceCategory)findPreference("pref_key_mms_settings");
            getPreferenceScreen().removePreference(mmsOptions);

            PreferenceCategory storageOptions =
                (PreferenceCategory)findPreference("pref_key_storage_settings");
            storageOptions.removePreference(findPreference("pref_key_mms_delete_limit"));
        }

        enablePushSetting();
        
        mSmsRecycler = Recycler.getSmsRecycler();
        mMmsRecycler = Recycler.getMmsRecycler();

        // Fix up the recycler's summary with the correct values
        setSmsDisplayLimit();
        setMmsDisplayLimit();        
        
        // Change the key to the SIM-related key, if has one SIM card, else set default value.
        if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
            if (currentSimCount == 1 ) {
                Log.i(TAG, "single sim");
                changeSingleCardKeyToSimRelated();
            } else if (currentSimCount > 1) {
                setMultiCardPreference();
            }
        }
    }

    NumberPickerDialog.OnNumberSetListener mSmsLimitListener =
        new NumberPickerDialog.OnNumberSetListener() {
            public void onNumberSet(int limit) { 
                if (limit <= mSmsRecycler.getMessageMinLimit()){
                    limit = mSmsRecycler.getMessageMinLimit();
                }else if( limit >= mSmsRecycler.getMessageMaxLimit()) {
                    limit = mSmsRecycler.getMessageMaxLimit();
                }
                mSmsRecycler.setMessageLimit(MessagingPreferenceActivity.this, limit);
                setSmsDisplayLimit(); 
                mSMSHandler.post(new Runnable() {
                    public void run() {
                        new Thread(new Runnable() {
                            public void run() {
                               Recycler.getSmsRecycler().deleteOldMessages(getApplicationContext());
                               if(FeatureOption.MTK_WAPPUSH_SUPPORT){
                              Recycler.getWapPushRecycler().deleteOldMessages(getApplicationContext());
                               }
                             }
                        }, "DeleteSMSOldMsgAfterSetNum").start();
                    }
                });
            }
    };

    NumberPickerDialog.OnNumberSetListener mMmsLimitListener =
        new NumberPickerDialog.OnNumberSetListener() {
            public void onNumberSet(int limit) {
                if (limit <= mMmsRecycler.getMessageMinLimit()){
                    limit = mMmsRecycler.getMessageMinLimit();
                }else if( limit >= mMmsRecycler.getMessageMaxLimit()) {
                    limit = mMmsRecycler.getMessageMaxLimit();
                } 
                mMmsRecycler.setMessageLimit(MessagingPreferenceActivity.this, limit);
                setMmsDisplayLimit();
                mMMSHandler.post(new Runnable() {
                    public void run() {
                        new Thread(new Runnable() {
                            public void run() {
                               Recycler.getMmsRecycler().deleteOldMessages(getApplicationContext());                            
                           } 
                        }, "DeleteMMSOldMsgAfterSetNum").start();
                    }
                });
            }
    };

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case CONFIRM_CLEAR_SEARCH_HISTORY_DIALOG:
                return new AlertDialog.Builder(MessagingPreferenceActivity.this)
                    .setTitle(R.string.confirm_clear_search_title)
                    .setMessage(R.string.confirm_clear_search_text)
                    .setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            SearchRecentSuggestions recent =
                                ((MmsApp)getApplication()).getRecentSuggestions();
                            if (recent != null) {
                                recent.clearHistory();
                            }
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .create();
        }
        return super.onCreateDialog(id);
    }

    /*
     * Notes: if wap push is not support, wap push setting should be removed
     * 
     */
    private void enablePushSetting(){
        
        PreferenceCategory wapPushOptions =
            (PreferenceCategory)findPreference("pref_key_wappush_settings");
        
        if(FeatureOption.MTK_WAPPUSH_SUPPORT){  
            if(!MmsConfig.getSlAutoLanuchEnabled()){
                wapPushOptions.removePreference(findPreference("pref_key_wappush_sl_autoloading"));
            }
        }else{
            getPreferenceScreen().removePreference(wapPushOptions);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference arg0, Object arg1) {
        final String key = arg0.getKey();
        int slotId = 0;
        if (FeatureOption.MTK_GEMINI_SUPPORT == true &&
                "OP02".equals(SystemProperties.get("ro.operator.optr"))) {
            int currentSimCount = SIMInfo.getInsertedSIMCount(this);
            if (currentSimCount == 1){
                slotId = SIMInfo.getInsertedSIMList(this).get(0).mSlot;
            }
        }
        String stored  = (String)arg1;
        if (PRIORITY.equals(key)) {
            Log.i(TAG, "change priority");
            mMmsPriority.setSummary(getVisualTextName(stored, R.array.pref_key_mms_priority_choices,
                    R.array.pref_key_mms_priority_values));
     
        } else if (CREATION_MODE.equals(key)) {
            mMmsCreationMode.setSummary(getVisualTextName(stored, R.array.pref_mms_creation_mode_choices,
                    R.array.pref_mms_creation_mode_values));
            
        } else if (MMS_SIZE_LIMIT.equals(key)) {
            mMmsSizeLimit.setSummary(getVisualTextName(stored, R.array.pref_mms_size_limit_choices,
                    R.array.pref_mms_size_limit_values));
            
        } else if (SMS_SAVE_LOCATION.equals(key) && !(currentSimCount > 1 &&
                "OP02".equals(SystemProperties.get("ro.operator.optr")))) {
            mSmsLocation.setSummary(getVisualTextName(stored, R.array.pref_sms_save_location_choices,
                    R.array.pref_sms_save_location_values));
        } else if((Long.toString(slotId) + "_" + SMS_SAVE_LOCATION).equals(key)){
            mSmsLocation.setSummary(getVisualTextName(stored, R.array.pref_sms_save_location_choices,
                    R.array.pref_sms_save_location_values));
        }
        return true;
    }
    private CharSequence getVisualTextName(String enumName, int choiceNameResId, int choiceValueResId) {
        CharSequence[] visualNames = getResources().getTextArray(
                choiceNameResId);
        CharSequence[] enumNames = getResources().getTextArray(
                choiceValueResId);

        // Sanity check
        if (visualNames.length != enumNames.length) {
            return "";
        }

        for (int i = 0; i < enumNames.length; i++) {
            if (enumNames[i].equals(enumName)) {
                return visualNames[i];
            }
        }
        return "";
    }

}
