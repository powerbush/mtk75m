package com.android.phone;

import java.util.List;

import android.preference.PreferenceActivity;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.os.Bundle;
import android.content.Intent;
import android.telephony.ServiceState;
import com.mediatek.xlog.Xlog;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony.SimInfo;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.CellConnService.CellConnMgr;

public class CallFeaturesSetting extends PreferenceActivity
implements Preference.OnPreferenceChangeListener {

    //Used for phone's other modules
    // Dtmf tone types
    static final int DTMF_TONE_TYPE_NORMAL = 0;
    static final int DTMF_TONE_TYPE_LONG   = 1;
    public static final String HAC_KEY = "HACSetting";
    public static final String HAC_VAL_ON = "ON";
    public static final String HAC_VAL_OFF = "OFF";
    
 // intent action to bring up voice mail settings
    public static final String ACTION_ADD_VOICEMAIL =
        "com.android.phone.CallFeaturesSetting.ADD_VOICEMAIL";
    // intent action sent by this activity to a voice mail provider
    // to trigger its configuration UI
    public static final String ACTION_CONFIGURE_VOICEMAIL =
        "com.android.phone.CallFeaturesSetting.CONFIGURE_VOICEMAIL";
    // Extra put in the return from VM provider config containing voicemail number to set
    public static final String VM_NUMBER_EXTRA = "com.android.phone.VoicemailNumber";
    // Extra put in the return from VM provider config containing call forwarding number to set
    public static final String FWD_NUMBER_EXTRA = "com.android.phone.ForwardingNumber";
    // Extra put in the return from VM provider config containing call forwarding number to set
    public static final String FWD_NUMBER_TIME_EXTRA = "com.android.phone.ForwardingNumberTime";
    // If the VM provider returns non null value in this extra we will force the user to
    // choose another VM provider
    public static final String SIGNOUT_EXTRA = "com.android.phone.Signout";
    
    private static final String LOG_TAG = "Settings/CallFeaturesSetting";
    private static final boolean DBG = true; // (PhoneApp.DBG_LEVEL >= 2);
    
    private static final String BUTTON_CALL_FWD_KEY    = "button_cf_expand_key";
    private static final String BUTTON_CALL_BAR_KEY    = "button_cb_expand_key";
    private static final String BUTTON_CALL_ADDITIONAL_KEY    = "button_more_expand_key";
    private static final String BUTTON_CALL_VOICEMAIL_KEY    = "button_voicemail_key";
    private static final String BUTTON_IP_PREFIX_KEY = "button_ip_prefix_key";
    
    private Preference mButtonVoiceMail;
    private Preference mButtonCallFwd;
    private Preference mButtonCallBar;
    private Preference mButtonCallAdditional;
    private Preference mButtonIpPrefix;
    
    private int mSimId = 0;
    private boolean isOnlyOneSim = false;
    private PreCheckForRunning preCfr = null;
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        addPreferencesFromResource(R.xml.voice_call_settings);
        addPreferencesFromResource(R.xml.gsm_umts_call_options);
        
        PreferenceScreen prefSet = getPreferenceScreen();
        mButtonCallAdditional = prefSet.findPreference(BUTTON_CALL_ADDITIONAL_KEY);
        mButtonCallFwd =  prefSet.findPreference(BUTTON_CALL_FWD_KEY);
        mButtonCallBar = prefSet.findPreference(BUTTON_CALL_BAR_KEY);
        mButtonVoiceMail = prefSet.findPreference(BUTTON_CALL_VOICEMAIL_KEY);
        mButtonIpPrefix = prefSet.findPreference(BUTTON_IP_PREFIX_KEY);
        
        if (!PhoneUtils.isSupportFeature("IP_DIAL")) {
            prefSet.removePreference(mButtonIpPrefix);
            mButtonIpPrefix = null;
        }
        
        preCfr = new PreCheckForRunning(this);
        if (CallSettings.isMultipleSim()) {
            List<SIMInfo> list = SIMInfo.getInsertedSIMList(this);
            if (list.size() == 1) {
                this.isOnlyOneSim = true;
                this.mSimId = list.get(0).mSlot;
            }
            preCfr.byPass = !isOnlyOneSim;
        } else {
            this.isOnlyOneSim = true;
            this.mSimId = 0;
            preCfr.byPass = !isOnlyOneSim;
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        //There is only one sim inserted(dualsim with one sim or single sim)
        if (isOnlyOneSim) {
            boolean isRadioOn = CallSettings.isRadioOn(this.mSimId);            
            if (!isRadioOn) {
                mButtonCallAdditional.setEnabled(false);
                mButtonCallFwd.setEnabled(false);
                mButtonCallBar.setEnabled(false);
                mButtonVoiceMail.setEnabled(false);
                mButtonIpPrefix.setEnabled(false);
            }
        }
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        
        if (!CallSettings.isMultipleSim())
        {
            if (preference == mButtonVoiceMail) {
                Intent intent = new Intent(this, VoiceMailSetting.class);
                intent.setAction(Intent.ACTION_MAIN);
                preCfr.checkToRun(intent, this.mSimId, 302);
                return true;
            } else if (preference == mButtonCallFwd) {
                Intent intent = new Intent(this, GsmUmtsCallForwardOptions.class);
                intent.setAction(Intent.ACTION_MAIN);
                preCfr.checkToRun(intent, this.mSimId, 302);
                return true;
            } else if (preference == mButtonCallBar) {
                Intent intent = new Intent(this, CallBarring.class);
                intent.setAction(Intent.ACTION_MAIN);
                preCfr.checkToRun(intent, this.mSimId, 302);
                return true;
            } else if (preference == mButtonCallAdditional) {
                Intent intent = new Intent(this, GsmUmtsAdditionalCallOptions.class);
                intent.setAction(Intent.ACTION_MAIN);
                preCfr.checkToRun(intent, this.mSimId, 302);
                return true;
            }
            return false;
        }
        
        if (preference == mButtonVoiceMail) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
            intent.putExtra(MultipleSimActivity.intentKey, "PreferenceScreen");
            intent.putExtra(MultipleSimActivity.targetClassKey, "com.android.phone.VoiceMailSetting");
            preCfr.checkToRun(intent, this.mSimId, 302);
            //this.startActivity(intent);
            return true;
        }else if (preference == mButtonCallFwd) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
            intent.putExtra(MultipleSimActivity.intentKey, "PreferenceScreen");
            intent.putExtra(MultipleSimActivity.targetClassKey, "com.android.phone.GsmUmtsCallForwardOptions");
            //this.startActivity(intent);
            preCfr.checkToRun(intent, this.mSimId, 302);
            return true;
        }else if (preference == mButtonCallBar) {
            Xlog.d(LOG_TAG, "onPreferenceTreeClick , call barring key , simId= " + mSimId);
            Intent intent = new Intent(this, MultipleSimActivity.class);
            //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
            intent.putExtra(MultipleSimActivity.intentKey, "PreferenceScreen");
            intent.putExtra(MultipleSimActivity.targetClassKey, "com.android.phone.CallBarring");
            //this.startActivity(intent);
            preCfr.checkToRun(intent, this.mSimId, 302);
            return true;
        }else if (preference == mButtonCallAdditional) {
            Xlog.d(LOG_TAG, "onPreferenceTreeClick , call cost key , simId= " + mSimId);
            Intent intent = new Intent(this, MultipleSimActivity.class);
              //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
            intent.putExtra(MultipleSimActivity.intentKey, "PreferenceScreen");
            intent.putExtra(MultipleSimActivity.targetClassKey, "com.android.phone.GsmUmtsAdditionalCallOptions");
            //this.startActivity(intent);
            preCfr.checkToRun(intent, this.mSimId, 302);
            return true;
        } else if (mButtonIpPrefix == preference) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
            intent.putExtra(MultipleSimActivity.intentKey, "PreferenceScreen");
            intent.putExtra(MultipleSimActivity.targetClassKey, "com.android.phone.IpPrefixPreference");
            this.startActivity(intent);
            //preCfr.checkToRun(intent, this.mSimId, 302);
            return true;
        }
        
        return false;
    }
    
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        return false;
    }
    
    protected void onDestroy() {
        super.onDestroy();
        if (preCfr != null) {
            preCfr.deRegister();
        }
    }
}