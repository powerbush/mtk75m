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

package com.android.phone;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import com.android.phone.R;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;
import com.android.internal.telephony.Phone;
import android.provider.Telephony.SIMInfo;
import com.mediatek.xlog.Xlog;
import com.mediatek.CellConnService.CellConnMgr;

public class VTAdvancedSettingEx extends PreferenceActivity{

    private static final String BUTTON_VT_REPLACE_KEY     = "button_vt_replace_expand_key";
    private static final String BUTTON_VT_ENABLE_BACK_CAMERA_KEY     = "button_vt_enable_back_camera_key";
    private static final String BUTTON_VT_PEER_BIGGER_KEY     = "button_vt_peer_bigger_key";
    private static final String BUTTON_VT_MO_LOCAL_VIDEO_DISPLAY_KEY     = "button_vt_mo_local_video_display_key";
    private static final String BUTTON_VT_MT_LOCAL_VIDEO_DISPLAY_KEY     = "button_vt_mt_local_video_display_key";
    
    private static final String BUTTON_CALL_FWD_KEY    = "button_cf_expand_key";
    private static final String BUTTON_CALL_BAR_KEY    = "button_cb_expand_key";
    private static final String BUTTON_CALL_ADDITIONAL_KEY    = "button_more_expand_key";
    
    private static final String BUTTON_VT_PEER_REPLACE_KEY = "button_vt_replace_peer_expand_key";
    private static final String BUTTON_VT_ENABLE_PEER_REPLACE_KEY = "button_vt_enable_peer_replace_key";
    private static final String BUTTON_VT_AUTO_DROPBACK_KEY = "button_vt_auto_dropback_key";
    
    private static final String BUTTON_VT_RINGTONE_KEY    = "button_vt_ringtone_key";
    private static final String SELECT_My_PICTURE         = "2";
    
    private static final String SELECT_DEFAULT_PICTURE    = "0";
    
    private static final String SELECT_DEFAULT_PICTURE2    = "0";
    private static final String SELECT_MY_PICTURE2         = "1";

    /** The launch code when picking a photo and the raw data is returned */
    public static final int REQUESTCODE_PICTRUE_PICKED_WITH_DATA = 3021;
    
    private Preference mButtonVTEnablebackCamer;
    private Preference mButtonVTReplace;
    private Preference mButtonVTPeerBigger;
    private Preference mButtonVTMoVideo;
    private Preference mButtonVTMtVideo;
    private Preference mButtonCallFwd;
    private Preference mButtonCallBar;
    private Preference mButtonCallAdditional;    
    
    private Preference mButtonVTPeerReplace;
    private Preference mButtonVTEnablePeerReplace;
    private Preference mButtonVTAutoDropBack;
    
    private int mSimId = VTAdvancedSetting.VT_CARD_SLOT;  //cardSlot which support vt
    long simIds[] = new long[1];

    // debug data
    private static final String LOG_TAG = "Settings/VTAdvancedSetting";
    private static final boolean DBG = true; // (PhoneApp.DBG_LEVEL >= 2);
    
    private PreCheckForRunning preCfr = null;
    private boolean isOnlyOneSim = false;
    
    private static void log(String msg) {
        Xlog.d(LOG_TAG, msg);
    }
    
     
    protected void onCreate(Bundle icicle) {
        
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.vt_advanced_setting_ex);
        
        SIMInfo info = null;
        if (PhoneUtils.isSupportFeature("3G_SWITCH")) {
            this.mSimId = PhoneApp.getInstance().phoneMgr.get3GCapabilitySIM();
            info = SIMInfo.getSIMInfoBySlot(this, mSimId);
        } else {
            info = SIMInfo.getSIMInfoBySlot(this, VTAdvancedSetting.VT_CARD_SLOT);
        }
        preCfr = new PreCheckForRunning(this);
        List<SIMInfo> list = SIMInfo.getInsertedSIMList(this);
        if (list.size() == 1) {
            this.isOnlyOneSim = true;
            this.mSimId = list.get(0).mSlot;
        }
        preCfr.byPass = !isOnlyOneSim;
        
        if (info != null) 
        {
            simIds[0] = info.mSimId;
        }else {
            //In normal case, we can't get here, if this happen, we just give a null ids and let's
            //Multiple sim activity handle this by using the default sim list
            simIds = null;
        }
        mButtonVTReplace = findPreference(BUTTON_VT_REPLACE_KEY);
        
        mButtonVTEnablebackCamer = findPreference(BUTTON_VT_ENABLE_BACK_CAMERA_KEY);
        mButtonVTPeerBigger = findPreference(BUTTON_VT_PEER_BIGGER_KEY);
        mButtonVTMoVideo = findPreference(BUTTON_VT_MO_LOCAL_VIDEO_DISPLAY_KEY);
        mButtonVTMtVideo = findPreference(BUTTON_VT_MT_LOCAL_VIDEO_DISPLAY_KEY);
        
        mButtonCallAdditional = findPreference(BUTTON_CALL_ADDITIONAL_KEY);
        mButtonCallFwd =  findPreference(BUTTON_CALL_FWD_KEY);
        mButtonCallBar = findPreference(BUTTON_CALL_BAR_KEY);
        
        mButtonVTPeerReplace = findPreference(BUTTON_VT_PEER_REPLACE_KEY);
        mButtonVTEnablePeerReplace = findPreference(BUTTON_VT_ENABLE_PEER_REPLACE_KEY);
        mButtonVTAutoDropBack = findPreference(BUTTON_VT_AUTO_DROPBACK_KEY);
        
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        
        if (preference == mButtonCallFwd)
        {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
            intent.putExtra(MultipleSimActivity.initFeatureName, "VT");
            intent.putExtra(MultipleSimActivity.intentKey, "PreferenceScreen");
            intent.putExtra(MultipleSimActivity.initSimId, simIds);
            intent.putExtra(MultipleSimActivity.targetClassKey, "com.android.phone.GsmUmtsCallForwardOptions");
            //this.startActivity(intent);
            preCfr.checkToRun(intent, this.mSimId, 302);
            return true;
        }else if (preference == mButtonCallBar) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            intent.putExtra("ISVT", true);
            intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
            intent.putExtra(MultipleSimActivity.initFeatureName, "VT");
            intent.putExtra(MultipleSimActivity.intentKey, "PreferenceScreen");
            intent.putExtra(MultipleSimActivity.initSimId, simIds);
            intent.putExtra(MultipleSimActivity.targetClassKey, "com.android.phone.CallBarring");
            //this.startActivity(intent);
            preCfr.checkToRun(intent, this.mSimId, 302);
            return true;
        }else if (preference == mButtonCallAdditional) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
            intent.putExtra(MultipleSimActivity.initFeatureName, "VT");
            intent.putExtra(MultipleSimActivity.intentKey, "PreferenceScreen");
            intent.putExtra(MultipleSimActivity.initSimId, simIds);
            intent.putExtra(MultipleSimActivity.targetClassKey, "com.android.phone.GsmUmtsAdditionalCallOptions");
            //this.startActivity(intent);
            preCfr.checkToRun(intent, this.mSimId, 302);
            return true;
        }else if (preference == mButtonVTEnablebackCamer) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
                //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            intent.putExtra(MultipleSimActivity.initFeatureName, "VT");
            intent.putExtra(MultipleSimActivity.initSimId, simIds);
            intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
            intent.putExtra(MultipleSimActivity.initBaseKey, "button_vt_enable_back_camera_key@");
              intent.putExtra(MultipleSimActivity.intentKey, "CheckBoxPreference");
              //intent.putExtra(MultipleSimActivity.targetClassKey, "com.android.phone.CellBroadcastActivity");
            this.startActivity(intent);
            return true;
        }else if (preference == this.mButtonVTReplace) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
            intent.putExtra(MultipleSimActivity.initFeatureName, "VT");
            intent.putExtra(MultipleSimActivity.intentKey, "ListPreference");
            intent.putExtra(MultipleSimActivity.LIST_TITLE, getResources().getString(R.string.vt_pic_replace_local));
            CharSequence[] entries = this.getResources().getStringArray(R.array.vt_replace_local_video_entries);
            intent.putExtra(MultipleSimActivity.initArray, entries);
            intent.putExtra(MultipleSimActivity.initSimId, simIds);
            if (getKeyValue("button_vt_replace_expand_key") == null)
            {
                setKeyValue("button_vt_replace_expand_key", "0");
            }
            intent.putExtra(MultipleSimActivity.initBaseKey, "button_vt_replace_expand_key@");
            CharSequence[] entriesValue = this.getResources().getStringArray(R.array.vt_replace_local_video_values);
            intent.putExtra(MultipleSimActivity.initArrayValue, entriesValue);
            this.startActivity(intent);
            return true;
        }else if (preference == mButtonVTPeerBigger) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
                //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            intent.putExtra(MultipleSimActivity.initFeatureName, "VT");
            intent.putExtra(MultipleSimActivity.initSimId, simIds);
            intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
              intent.putExtra(MultipleSimActivity.intentKey, "CheckBoxPreference");
              intent.putExtra(MultipleSimActivity.initBaseKey, "button_vt_peer_bigger_key@");
              //intent.putExtra(MultipleSimActivity.targetClassKey, "com.android.phone.CellBroadcastActivity");
            this.startActivity(intent);
        }else if (preference == mButtonVTMoVideo) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
                //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            intent.putExtra(MultipleSimActivity.initFeatureName, "VT");
            intent.putExtra(MultipleSimActivity.initSimId, simIds);
            intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
              intent.putExtra(MultipleSimActivity.intentKey, "CheckBoxPreference");
              intent.putExtra(MultipleSimActivity.initBaseKey, "button_vt_mo_local_video_display_key@");
              //intent.putExtra(MultipleSimActivity.targetClassKey, "com.android.phone.CellBroadcastActivity");
            this.startActivity(intent);
        }else if (preference == mButtonVTMtVideo) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
            intent.putExtra(MultipleSimActivity.initFeatureName, "VT");
            intent.putExtra(MultipleSimActivity.LIST_TITLE, getResources().getString(R.string.vt_incoming_call));
            intent.putExtra(MultipleSimActivity.intentKey, "ListPreference");
            CharSequence[] entries = this.getResources().getStringArray(R.array.vt_mt_local_video_display_entries);
            intent.putExtra(MultipleSimActivity.initArray, entries);
            intent.putExtra(MultipleSimActivity.initSimId, simIds);
            if (getKeyValue("button_vt_mt_local_video_display_key") == null)
            {
                setKeyValue("button_vt_mt_local_video_display_key", "0");
            }
            intent.putExtra(MultipleSimActivity.initBaseKey, "button_vt_mt_local_video_display_key@");
            CharSequence[] entriesValue = this.getResources().getStringArray(R.array.vt_mt_local_video_display_values);
            intent.putExtra(MultipleSimActivity.initArrayValue, entriesValue);
            this.startActivity(intent);
            return true;
        }else if(preference == mButtonVTPeerReplace){
        	Intent intent = new Intent(this, MultipleSimActivity.class);
            //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
            intent.putExtra(MultipleSimActivity.initFeatureName, "VT");
            intent.putExtra(MultipleSimActivity.intentKey, "ListPreference");
            intent.putExtra(MultipleSimActivity.LIST_TITLE, getResources().getString(R.string.vt_peer_video_rep));
            CharSequence[] entries = this.getResources().getStringArray(R.array.vt_replace_local_video_entries2);
            intent.putExtra(MultipleSimActivity.initArray, entries);
            intent.putExtra(MultipleSimActivity.initSimId, simIds);
            if (getKeyValue("button_vt_replace_peer_expand_key") == null)
            {
                setKeyValue("button_vt_replace_peer_expand_key", "0");
            }
            intent.putExtra(MultipleSimActivity.initBaseKey, "button_vt_replace_peer_expand_key@");
            CharSequence[] entriesValue = this.getResources().getStringArray(R.array.vt_replace_local_video_values2);
            intent.putExtra(MultipleSimActivity.initArrayValue, entriesValue);
            this.startActivity(intent);
            return true;
        }else if(preference == mButtonVTEnablePeerReplace){
        	Intent intent = new Intent(this, MultipleSimActivity.class);
            //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
        	intent.putExtra(MultipleSimActivity.initFeatureName, "VT");
        	intent.putExtra(MultipleSimActivity.initSimId, simIds);
        	intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
        	intent.putExtra(MultipleSimActivity.intentKey, "CheckBoxPreference");
        	intent.putExtra(MultipleSimActivity.initBaseKey, "button_vt_enable_peer_replace_key@");
        	//intent.putExtra(MultipleSimActivity.targetClassKey, "com.android.phone.CellBroadcastActivity");
        	this.startActivity(intent);
        }else if(preference == mButtonVTAutoDropBack){
        	Intent intent = new Intent(this, MultipleSimActivity.class);
            //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
        	intent.putExtra(MultipleSimActivity.initFeatureName, "VT");
        	intent.putExtra(MultipleSimActivity.initSimId, simIds);
        	intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
        	intent.putExtra(MultipleSimActivity.intentKey, "CheckBoxPreference");
        	intent.putExtra(MultipleSimActivity.initBaseKey, "button_vt_auto_dropback_key@");
        	//intent.putExtra(MultipleSimActivity.targetClassKey, "com.android.phone.CellBroadcastActivity");
        	this.startActivity(intent);
        }
        
        return false;
    }
    
    private String getKeyValue(String key)
    {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        return sp.getString(key, null);
    }
    
    private void setKeyValue(String key, String value)
    {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, value);
        editor.commit();
    }

    protected void onDestroy() {
        super.onDestroy();
        if (preCfr != null) {
            preCfr.deRegister();
        }
    }
}
