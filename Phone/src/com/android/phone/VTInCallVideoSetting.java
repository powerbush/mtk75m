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

import java.util.ArrayList;
import java.util.List;

import com.mediatek.vt.VTManager;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

public class VTInCallVideoSetting extends PreferenceActivity 
			implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener{

	public static final String OPEN_LOCAL_ZOOM 							= "OPEN_LOCAL_ZOOM";
	public static final String OPEN_LOCAL_BRIGHTNESS 					= "OPEN_LOCAL_BRIGHTNESS";
	
	private static final String BUTTON_VT_LOCAL_VIDEO_ZOOM_KEY 			= "vt_local_video_zoom";
	private static final String BUTTON_VT_LOCAL_VIDEO_BRIGHTNESS_KEY 	= "vt_local_video_brightness";
	private static final String BUTTON_VT_LOCAL_VIDEO_EFFECT_KEY 		= "vt_local_video_effect";
	private static final String BUTTON_VT_LOCAL_VIDEO_NIGHT_MODE_KEY 	= "vt_local_video_night_mode";
	private static final String BUTTON_VT_PEER_QUALITY_KEY 				= "vt_peer_video_quality";
	
	private PreferenceScreen mLocalVideoZoom;
	private PreferenceScreen mLocalVideoBrightness;
	private ListPreference mLocalEffect;
	private CheckBoxPreference mLocalVideoNightMode;
	private ListPreference mPeerVideoQuality;
	
    // debug data
    private static final String LOG_TAG = "VTInCallVideoSetting";
    private static final boolean DBG = true; // (PhoneApp.DBG_LEVEL >= 2);
    
    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
	
	@Override 
	protected void onCreate(Bundle icicle) {
		
		if(DBG)log("onCreate!!");
			
		super.onCreate(icicle);
		addPreferencesFromResource(R.xml.vt_incall_video_setting);
		
		mLocalVideoZoom = (PreferenceScreen)findPreference(BUTTON_VT_LOCAL_VIDEO_ZOOM_KEY);
		mLocalVideoZoom.setOnPreferenceClickListener(this);
		
		mLocalVideoBrightness = (PreferenceScreen)findPreference(BUTTON_VT_LOCAL_VIDEO_BRIGHTNESS_KEY);
		mLocalVideoBrightness.setOnPreferenceClickListener(this);
		
		mLocalEffect = (ListPreference)findPreference(BUTTON_VT_LOCAL_VIDEO_EFFECT_KEY);
		mLocalEffect.setOnPreferenceChangeListener(this);
		mLocalEffect.setEnabled(loadSupportedEffects());
		
		mLocalVideoNightMode =  (CheckBoxPreference)findPreference(BUTTON_VT_LOCAL_VIDEO_NIGHT_MODE_KEY);
		mLocalVideoNightMode.setOnPreferenceChangeListener(this);
		mLocalVideoNightMode.setEnabled(VTManager.getInstance().isSupportNightMode());
		
		mPeerVideoQuality = (ListPreference)findPreference(BUTTON_VT_PEER_QUALITY_KEY);
		mPeerVideoQuality.setOnPreferenceChangeListener(this);
	}
	
	private boolean loadSupportedEffects()
	{
        CharSequence[] entryValues = getResources().getStringArray(R.array.vt_incall_setting_local_video_effect_values);
        CharSequence[] entries = getResources().getStringArray(R.array.vt_incall_setting_local_video_effect_entries);  

        List<String> supportEntryValues = VTManager.getInstance().getSupportedColorEffects();
        
        int total = supportEntryValues.size();
        if(total <= 0)return false;
        
        ArrayList<CharSequence> entryValues2 = new ArrayList<CharSequence>();
        ArrayList<CharSequence> entries2 = new ArrayList<CharSequence>();
        
        for(int i = 0 , len = entryValues.length ; i < len ; i++)
        {
        	if( supportEntryValues.indexOf( entryValues[i].toString() ) >= 0 )
        	{
        		entryValues2.add( entryValues[i] );
        		entries2.add( entries[i] );
        	}
        }
        
        int size = entryValues2.size();
        
        mLocalEffect.setEntryValues( entryValues2.toArray(new CharSequence[size]) );
        mLocalEffect.setEntries( entries2.toArray(new CharSequence[size]) );
        
        return true;
	}
	
    @Override
    protected void onResume() {
        if (DBG) log("onResume()...");
        super.onResume();
        
        //get the current values from VTManager:
        mLocalEffect.setValue( VTManager.getInstance().getColorEffect() );
    	mLocalVideoNightMode.setChecked( VTManager.getInstance().getNightMode() );
    	if( VTManager.VT_VQ_SHARP == VTManager.getInstance().getVideoQuality() )
    	{
    		mPeerVideoQuality.setValue("Sharp");
    	}
    	else if ( VTManager.VT_VQ_NORMAL == VTManager.getInstance().getVideoQuality() )
    	{
    		mPeerVideoQuality.setValue("Normal");
    	}
    	else
    	{
    		if (DBG) log("VTManager.getInstance().getVideoQuality() is not VTManager.VT_VQ_SHARP or VTManager.VT_VQ_NORMAL , error ! ");
    	}
    	
        
    	if( VTInCallScreenFlags.getInstance().mVTHideMeNow )
    	{
    		if (DBG) log("onResume() : hide me now, so all local items disabled...");
    		mLocalVideoZoom.setEnabled(false);
    		mLocalVideoBrightness.setEnabled(false);
    		mLocalEffect.setEnabled(false);
    		mLocalVideoNightMode.setEnabled(false);
    	}
        
    }
    
    @Override
    public void onPause() {
    	if (DBG) log("onPause()...,and finish() itself...");
    	super.onPause();
        finish();
    }


	public boolean onPreferenceChange(Preference preference, Object objValue) {
		// TODO Auto-generated method stub
		
		if( preference ==  mLocalEffect){
			VTManager.getInstance().setColorEffect( objValue.toString() );
			finish();
			return true;
		}
		else if( preference == mLocalVideoNightMode )
		{
			if (DBG) log("onPreferenceChange : mLocalVideoNightMode = "+objValue.toString());
			VTManager.getInstance().setNightMode(objValue.toString().equals("true"));
			finish();
			return true;
		}
		else if( preference ==  mPeerVideoQuality)
		{
			if(objValue.toString().equals("Normal"))
			{
				if (DBG) log("onPreferenceChange : mPeerVideoQuality = Normal");
				if (DBG) log(" - VTManager.getInstance().setVideoQuality(VTManager.VT_VQ_NORMAL) ! ");
				VTManager.getInstance().setVideoQuality( VTManager.VT_VQ_NORMAL );
				finish();
				return true;
			}
			else if(objValue.toString().equals("Sharp"))
			{
				if (DBG) log("onPreferenceChange : mPeerVideoQuality = Sharp");
				if (DBG) log(" - VTManager.getInstance().setVideoQuality( VTManager.VT_VQ_SHARP ) ! ");
				VTManager.getInstance().setVideoQuality( VTManager.VT_VQ_SHARP );
				finish();
				return true;
			}
		}
		
		return true;
	}


	public boolean onPreferenceClick(Preference preference) {
		// TODO Auto-generated method stub
		Intent intent;
		
		if(preference == mLocalVideoZoom)
		{
			if(DBG)log("onPreferenceClick : mLocalVideoZoom !");
			intent = new Intent(this, InCallScreen.class);
			intent.setAction(InCallScreen.ACTION_UNDEFINED);
			intent.putExtra(OPEN_LOCAL_ZOOM, true);
			this.startActivity(intent);
			//finish();
			return true;
		}else if(preference == mLocalVideoBrightness)
		{
			if(DBG)log("onPreferenceClick : mLocalVideoBrightness !");
			intent = new Intent(this, InCallScreen.class);
			intent.setAction(InCallScreen.ACTION_UNDEFINED);
			intent.putExtra(OPEN_LOCAL_BRIGHTNESS, true);
			this.startActivity(intent);
			//finish();
			return true;
		}
		
		return true;
	}

	
}
