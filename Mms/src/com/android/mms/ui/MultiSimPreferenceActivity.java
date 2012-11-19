package com.android.mms.ui;

import java.util.List;
import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.ui.AdvancedCheckBoxPreference.GetSimInfo;

import android.R.color;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony;
import com.mediatek.telephony.TelephonyManagerEx;

public class MultiSimPreferenceActivity extends PreferenceActivity implements GetSimInfo{
	private static final String TAG = "MultiSimPreferenceActivity";
	
    private AdvancedCheckBoxPreference mSim1;
    private AdvancedCheckBoxPreference mSim2;
    private AdvancedCheckBoxPreference mSim3;
    private AdvancedCheckBoxPreference mSim4;
    
    private int simCount;
	private List<SIMInfo> listSimInfo;
	
	private TelephonyManagerEx mTelephonyManager;

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    	listSimInfo = SIMInfo.getInsertedSIMList(this);
        simCount = listSimInfo.size();

        addPreferencesFromResource(R.xml.multicardselection);    
        Intent intent = getIntent();
        String preference = intent.getStringExtra("preference");
        //translate key to SIM-related key;
        Log.i("MultiSimPreferenceActivity, getIntent:", intent.toString());
        Log.i("MultiSimPreferenceActivity, getpreference:", preference);

        changeMultiCardKeyToSimRelated(preference);
        
    }
    
    private void changeMultiCardKeyToSimRelated(String preference) {

        mSim1 = (AdvancedCheckBoxPreference) findPreference("pref_key_sim1");
        mSim1.init(this, 0);
        mSim2 = (AdvancedCheckBoxPreference) findPreference("pref_key_sim2");
        mSim2.init(this, 1);
        mSim3 = (AdvancedCheckBoxPreference) findPreference("pref_key_sim3");
        mSim3.init(this, 2);
        mSim4 = (AdvancedCheckBoxPreference) findPreference("pref_key_sim4");
        mSim4.init(this, 3);
        //get the stored value
    	SharedPreferences sp = getSharedPreferences("com.android.mms_preferences", MODE_WORLD_READABLE);
    	

    	if (simCount == 1) {
    		getPreferenceScreen().removePreference(mSim2);
    		getPreferenceScreen().removePreference(mSim3);
    		getPreferenceScreen().removePreference(mSim4);
    		mSim1.setKey(Long.toString(listSimInfo.get(0).mSimId) + "_" + preference);
            if (preference.equals(MessagingPreferenceActivity.RETRIEVAL_DURING_ROAMING)) {
                mSim1.setEnabled(sp.getBoolean(Long.toString(listSimInfo.get(0).mSimId)	
                   	+ "_" + MessagingPreferenceActivity.AUTO_RETRIEVAL, true));
            }
    	} else if (simCount == 2) {
    		getPreferenceScreen().removePreference(mSim3);
    		getPreferenceScreen().removePreference(mSim4);
    		
            mSim1.setKey(Long.toString(listSimInfo.get(0).mSimId) + "_" + preference);
            mSim2.setKey(Long.toString(listSimInfo.get(1).mSimId) + "_" + preference);
            if (preference.equals(MessagingPreferenceActivity.RETRIEVAL_DURING_ROAMING)) {
                mSim1.setEnabled(sp.getBoolean(Long.toString(listSimInfo.get(0).mSimId)	
                   	+ "_" + MessagingPreferenceActivity.AUTO_RETRIEVAL, true));
                mSim2.setEnabled(sp.getBoolean(Long.toString(listSimInfo.get(1).mSimId)	
               		+ "_" + MessagingPreferenceActivity.AUTO_RETRIEVAL, true));
            }
    	} else if (simCount == 3) {
    	    getPreferenceScreen().removePreference(mSim4);
    	
            mSim1.setKey(Long.toString(listSimInfo.get(0).mSimId) + "_" + preference);
            mSim2.setKey(Long.toString(listSimInfo.get(1).mSimId) + "_" + preference);
            mSim3.setKey(Long.toString(listSimInfo.get(2).mSimId) + "_" + preference);
            if (preference.equals(MessagingPreferenceActivity.RETRIEVAL_DURING_ROAMING)) {
                mSim1.setEnabled(sp.getBoolean(Long.toString(listSimInfo.get(0).mSimId)	
                   	+ "_" + MessagingPreferenceActivity.AUTO_RETRIEVAL, true));
                mSim2.setEnabled(sp.getBoolean(Long.toString(listSimInfo.get(1).mSimId)	
               		+ "_" + MessagingPreferenceActivity.AUTO_RETRIEVAL, true));
                mSim3.setEnabled(sp.getBoolean(Long.toString(listSimInfo.get(2).mSimId)	
               		+ "_" + MessagingPreferenceActivity.AUTO_RETRIEVAL, true));
            }
    	} else{
            
            mSim1.setKey(Long.toString(listSimInfo.get(0).mSimId) + "_" + preference);
            mSim2.setKey(Long.toString(listSimInfo.get(1).mSimId) + "_" + preference);
            mSim3.setKey(Long.toString(listSimInfo.get(2).mSimId) + "_" + preference);
            mSim4.setKey(Long.toString(listSimInfo.get(3).mSimId) + "_" + preference);
            if (preference.equals(MessagingPreferenceActivity.RETRIEVAL_DURING_ROAMING)) {
                mSim1.setEnabled(sp.getBoolean(Long.toString(listSimInfo.get(0).mSimId)	
                   	+ "_" + MessagingPreferenceActivity.AUTO_RETRIEVAL, true));
                mSim2.setEnabled(sp.getBoolean(Long.toString(listSimInfo.get(1).mSimId)	
               		+ "_" + MessagingPreferenceActivity.AUTO_RETRIEVAL, true));
                mSim3.setEnabled(sp.getBoolean(Long.toString(listSimInfo.get(2).mSimId)	
               		+ "_" + MessagingPreferenceActivity.AUTO_RETRIEVAL, true));
                mSim4.setEnabled(sp.getBoolean(Long.toString(listSimInfo.get(3).mSimId)	
               		+ "_" + MessagingPreferenceActivity.AUTO_RETRIEVAL, true));
            } 
    	}
    	
        if (mSim1 != null) {
            if (preference.equals(MessagingPreferenceActivity.AUTO_RETRIEVAL) 
            		|| preference.equals(MessagingPreferenceActivity.MMS_ENABLE_TO_SEND_DELIVERY_REPORT)) {
    	        mSim1.setChecked(sp.getBoolean(mSim1.getKey(), true));
            } else {
            	mSim1.setChecked(sp.getBoolean(mSim1.getKey(), false));
            }
        }
        if (mSim2 != null) {
        	if (preference.equals(MessagingPreferenceActivity.AUTO_RETRIEVAL) 
            		|| preference.equals(MessagingPreferenceActivity.MMS_ENABLE_TO_SEND_DELIVERY_REPORT)) {
    	        mSim2.setChecked(sp.getBoolean(mSim2.getKey(), true));
            }  else {
            	mSim2.setChecked(sp.getBoolean(mSim2.getKey(), false));
            }
        }
        if (mSim3 != null) {
        	if (preference.equals(MessagingPreferenceActivity.AUTO_RETRIEVAL) 
            		|| preference.equals(MessagingPreferenceActivity.MMS_ENABLE_TO_SEND_DELIVERY_REPORT)) {
    	        mSim3.setChecked(sp.getBoolean(mSim3.getKey(), true));
            } else {
            	mSim3.setChecked(sp.getBoolean(mSim3.getKey(), false));
            }
        }
        if (mSim4 != null) {
        	if (preference.equals(MessagingPreferenceActivity.AUTO_RETRIEVAL) 
            		|| preference.equals(MessagingPreferenceActivity.MMS_ENABLE_TO_SEND_DELIVERY_REPORT)) {
    	        mSim4.setChecked(sp.getBoolean(mSim4.getKey(), true));
            } else {
            	mSim4.setChecked(sp.getBoolean(mSim4.getKey(), false));
            }
        }
    }
    
    public String getSimName(int id) {
    	return listSimInfo.get(id).mDisplayName;
    }

    
    public String getSimNumber(int id) {
    	return listSimInfo.get(id).mNumber;
    }
    
    public int getSimColor(int id) {
    	return listSimInfo.get(id).mSimBackgroundRes;
    }
    
    public int getNumberFormat(int id) {
    	return listSimInfo.get(id).mDispalyNumberFormat;
    }
    
    public int getSimStatus(int id) {
    	mTelephonyManager = TelephonyManagerEx.getDefault();
    	//int slotId = SIMInfo.getSlotById(this,listSimInfo.get(id).mSimId);
    	int slotId = listSimInfo.get(id).mSlot;
    	if (slotId != -1) {
    		return mTelephonyManager.getSimIndicatorStateGemini(slotId);
    	}
    	return -1;
    }
    
    public boolean is3G(int id)	{
    	//int slotId = SIMInfo.getSlotById(this, listSimInfo.get(id).mSimId);
    	int slotId = listSimInfo.get(id).mSlot;
    	Log.i(TAG, "SIMInfo.getSlotById id: " + id + " slotId: " + slotId);
    	if (slotId == MessageUtils.get3GCapabilitySIM()) {
    		return true;
    	}
    	return false;
    }
}