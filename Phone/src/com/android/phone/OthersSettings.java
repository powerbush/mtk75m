package com.android.phone;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Telephony.SIMInfo;
import com.mediatek.CellConnService.CellConnMgr;
import android.telephony.TelephonyManager;
import com.mediatek.xlog.Xlog;

import com.mediatek.featureoption.FeatureOption;

public class OthersSettings extends PreferenceActivity implements Preference.OnPreferenceChangeListener
{
    private static final String BUTTON_OTHERS_FDN_KEY     = "button_fdn_key";
    private static final String BUTTON_OTHERS_AOC_KEY     = "button_aoc_expand_key";
    private static final String BUTTON_OTHERS_CELL_BROADCAST_KEY    = "button_cell_broadcast_key";
    private static final String BUTTON_OTHERS_MINUTE_REMINDER_KEY    = "minute_reminder_key";
    private static final String BUTTON_OTHERS_PHONE_SETTING_KEY    = "button_phone_setting_key";
    private static final String BUTTON_OTHERS_TTY_KEY = "tty_setting_key";
    private static final String BUTTON_OTHERS_DUAL_MIC_KEY = "dual_mic_key";
    
    private static final String LOG_TAG = "Settings/OthersSettings";
    private Preference mButtonFdn;
    private Preference mButtonAoc;
    private Preference mButtonCb;
    private CheckBoxPreference mButtonMr;
    private Preference mButtonPs;
    private Preference mTtyButton;
    private CheckBoxPreference mButtonDualMic;
    
    private int mSimId = 0;
    private boolean isOnlyOneSim = false;
    PreCheckForRunning preCfr = null;
    
    /*private CellConnMgr mCellConnMgr;
    private ServiceComplete mServiceComplete;
    
    class ServiceComplete implements Runnable {
        public void run() {
            int result = mCellConnMgr.getResult();
            Xlog.d(LOG_TAG, "ServiceComplete with the result = " + CellConnMgr.resultToString(result));
            if (CellConnMgr.RESULT_OK == result) {
                startActivity(intent);
            }
        }
        
        public void setIntent(Intent it)
        {
            intent = it;
        }
        private Intent intent;
    }*/
    
    @Override
    protected void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.others_settings);
        
        mButtonFdn = findPreference(BUTTON_OTHERS_FDN_KEY);
        mButtonAoc = findPreference(BUTTON_OTHERS_AOC_KEY);
        mButtonCb = findPreference(BUTTON_OTHERS_CELL_BROADCAST_KEY);
        mButtonMr = (CheckBoxPreference)findPreference(BUTTON_OTHERS_MINUTE_REMINDER_KEY);
        mButtonDualMic = (CheckBoxPreference)findPreference(BUTTON_OTHERS_DUAL_MIC_KEY);
        mTtyButton = findPreference(BUTTON_OTHERS_TTY_KEY);
        
        if (!PhoneUtils.isSupportFeature("TTY")) {
            this.getPreferenceScreen().removePreference(mTtyButton);
        }
        
        if (!PhoneUtils.isSupportFeature("DUAL_MIC")) {
            this.getPreferenceScreen().removePreference(mButtonDualMic);
        }
		
		if (true == FeatureOption.MTK_VT3G324M_SUPPORT) {
			// MTK_OP01_PROTECT_START
			if (!"OP01".equals(PhoneUtils.getOptrProperties()))
				// MTK_OP01_PROTECT_END
				this.getPreferenceScreen().removePreference(
						findPreference("auto_reject_setting_key"));
		} else {
			this.getPreferenceScreen().removePreference(
					findPreference("auto_reject_setting_key"));
		}
        
        if (mButtonMr != null)
        {
            mButtonMr.setOnPreferenceChangeListener(this);
        }
        
        if (mButtonDualMic != null)
        {
            mButtonDualMic.setOnPreferenceChangeListener(this);
        }
        
        mButtonPs = findPreference(this.BUTTON_OTHERS_PHONE_SETTING_KEY);
        
        /*mServiceComplete = new ServiceComplete();
        mCellConnMgr = new CellConnMgr(mServiceComplete);
        mCellConnMgr.register(getApplicationContext());*/
        preCfr = new PreCheckForRunning(this);
        
        if (CallSettings.isMultipleSim()) {
            List<SIMInfo> list = SIMInfo.getInsertedSIMList(this);
            if (list.size() == 1) {
                this.isOnlyOneSim = true;
                this.mSimId = list.get(0).mSlot;
            }
        } else {
            this.isOnlyOneSim = true;
            this.mSimId = 0;
        }
        
        preCfr.byPass = !isOnlyOneSim;
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (!CallSettings.isMultipleSim()) {
            if (preference == mButtonFdn) {
                Intent intent = new Intent(this, FdnSetting2.class);
                preCfr.checkToRun(intent, this.mSimId, 302);
                return true;
            } else if (preference == mButtonAoc) {
                Intent intent = new Intent(this, CallCostSettings.class);
                preCfr.checkToRun(intent, this.mSimId, 302);
                return true;
            } else if (preference == mButtonCb) {
                Intent intent = new Intent(this, CellBroadcastActivity.class);
                preCfr.checkToRun(intent, this.mSimId, 302);
                return true;
            }
            return false;
        }
        
        if (preference == mButtonFdn) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
            intent.putExtra(MultipleSimActivity.intentKey, "PreferenceScreen");
            intent.putExtra(MultipleSimActivity.targetClassKey, "com.android.phone.FdnSetting2");
            //this.startActivity(intent);
            preCfr.checkToRun(intent, this.mSimId, 302);
            return true;
        }else if (preference == mButtonAoc) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
            intent.putExtra(MultipleSimActivity.intentKey, "PreferenceScreen");
            intent.putExtra(MultipleSimActivity.targetClassKey, "com.android.phone.CallCostSettings");
            //this.startActivity(intent);
            preCfr.checkToRun(intent, this.mSimId, 302);
            return true;
        }else if (preference == mButtonCb) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
            intent.putExtra(MultipleSimActivity.intentKey, "PreferenceScreen");
            intent.putExtra(MultipleSimActivity.targetClassKey, "com.android.phone.CellBroadcastActivity");
            //this.startActivity(intent);
            preCfr.checkToRun(intent, this.mSimId, 302);
            return true;
        }
        
        return false;
    }
    
     public boolean onPreferenceChange(Preference preference, Object objValue) {
         if (preference == mButtonMr) {
             if (mButtonMr.isChecked()){
                 Xlog.d("OthersSettings", "onPreferenceChange mButtonReminder turn on"); 
                 mButtonMr.setSummary(getString(R.string.minutereminder_turnon));
             }else{
                 Xlog.d("OthersSettings", "onPreferenceChange mButtonReminder turn off"); 
                 mButtonMr.setSummary(getString(R.string.minutereminder_turnoff));
             }
         } else if (preference == mButtonDualMic) {
             if (mButtonDualMic.isChecked()){
                 Xlog.d(LOG_TAG, "onPreferenceChange mButtonDualmic turn on"); 
                 //mButtonDualMic.setSummary(getString(R.string.dual_mic_turnoff));
                 PhoneUtils.setDualMicMode("0");
             }else{
                 Xlog.d(LOG_TAG, "onPreferenceChange mButtonDualmic turn off"); 
                 //mButtonDualMic.setSummary(getString(R.string.dual_mic_turnon));
                 PhoneUtils.setDualMicMode("1");
             }
         }
         
         return true;
     }
     
     public void onResume() {
         super.onResume();
         boolean isRadioOn = CallSettings.isRadioOn(mSimId);
         if (isOnlyOneSim && !isRadioOn) { 
             mButtonFdn.setEnabled(false);
             mButtonAoc.setEnabled(false);
             mButtonCb.setEnabled(false);
         }
         
         if (CallSettings.isMultipleSim()) {
             List<SIMInfo> insertSim = SIMInfo.getInsertedSIMList(this);
             if (insertSim.size() == 0) {
                 mButtonFdn.setEnabled(false);
                 mButtonAoc.setEnabled(false);
                 mButtonCb.setEnabled(false);
             }
         } else {
             boolean bIccExist = TelephonyManager.getDefault().hasIccCard();
             if (!bIccExist) {
                 if (mButtonFdn != null)  mButtonFdn.setEnabled(false);
                 if (mButtonAoc != null) mButtonAoc.setEnabled(false);
                 if (mButtonCb != null) mButtonCb.setEnabled(false);
             }
         }
     }
     
     protected void onDestroy() {
         super.onDestroy();
         //mCellConnMgr.unregister();
         if (preCfr != null) {
             preCfr.deRegister();
         }
     }
}