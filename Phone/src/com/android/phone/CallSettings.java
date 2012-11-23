package com.android.phone;

import java.util.List;
import android.content.Intent;
import android.net.sip.SipManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Telephony.SIMInfo;
import android.telephony.ServiceState;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.featureoption.FeatureOption;
import com.android.internal.telephony.gemini.GeminiPhone;
import android.telephony.TelephonyManager;

public class CallSettings extends PreferenceActivity
{
    Preference mVTSetting = null;
    Preference mVoiceSetting = null;
    Preference mSipCallSetting = null;
    //true means: only one sim insert && the slot support wcdma
    boolean mOnlyVt = false;
    private int VT_CARD_SLOT = 0;
    
    @Override
    protected void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.call_feature_setting);
        mVTSetting = this.findPreference("button_vedio_call_key");
        mVoiceSetting = this.findPreference("button_voice_call_key");
        
        boolean voipSupported = SipManager.isVoipSupported(this);
        if (!voipSupported)
        {
            this.getPreferenceScreen().removePreference(findPreference("button_internet_call_key"));
        }

        //If this video telephony feature is not supported, remove the setting
        if (!FeatureOption.MTK_VT3G324M_SUPPORT)
        {
            getPreferenceScreen().removePreference(mVTSetting);
            mVTSetting = null;
        }
    }
    
     public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)
     {
         if (preference == mVTSetting)
         {
             Intent intent = new Intent();
             if (!mOnlyVt)
             {
                 intent.setClass(this, VTAdvancedSettingEx.class);
             }else
             {
                 intent.setClass(this, VTAdvancedSetting.class);
             }
             startActivity(intent);
             return true;
         }
     
         return false;
     }
     
     @Override
        public void onResume() {
         super.onResume();
         if (CallSettings.isMultipleSim()) {
             List<SIMInfo> insertSim = SIMInfo.getInsertedSIMList(this);
             
             if (insertSim.size() == 0)
             {
                 //Anyway, no sim information, we disable the voice/video call setting items
                 if (mVTSetting != null)  mVTSetting.setEnabled(false);
                 if (mVoiceSetting != null) mVoiceSetting.setEnabled(false);
                 
                 return ;
             }
             
             //init VT_CARD_SLOT, ensure this can be worked on the project with enable 3G modem switch
             if (PhoneUtils.isSupportFeature("3G_SWITCH")) {
                 VT_CARD_SLOT = PhoneApp.getInstance().phoneMgr.get3GCapabilitySIM();
             }
             
             //In the foreseeable future, we only the slot == 0 supports WCDMA
             if (insertSim != null && insertSim.size() == 1 && insertSim.get(0).mSlot == VT_CARD_SLOT)
             {
                 mOnlyVt = true;
             }
    
             if (null == SIMInfo.getSIMInfoBySlot(this, VT_CARD_SLOT))
             {
                 //no sim in wcdma slot, so disable the vt setting
                 if (mVTSetting != null)  mVTSetting.setEnabled(false);
             }
             
             //For current testing, add this to strict the condition
             if (!isMultipleSim())
             {
                 mOnlyVt = true;
             }
         } else {
             boolean bIccExist = TelephonyManager.getDefault().hasIccCard();
             mOnlyVt = true;
             if (!bIccExist) {
                 if (mVTSetting != null)  mVTSetting.setEnabled(false);
                 if (mVoiceSetting != null) mVoiceSetting.setEnabled(false);
             }
         }
     }
     
     public static boolean isMultipleSim()
     {
         return FeatureOption.MTK_GEMINI_SUPPORT;
     }
     
     public static boolean isRadioOn(int slot) {
         boolean isRadioOn = false;
         Phone phone = PhoneFactory.getDefaultPhone();
         if (CallSettings.isMultipleSim() && phone instanceof GeminiPhone) {
             GeminiPhone dualPhone = (GeminiPhone) phone;
             isRadioOn = dualPhone.isRadioOnGemini(slot);
         }else {
             isRadioOn = phone.getServiceState().getState() != ServiceState.STATE_POWER_OFF;
         }
         
         return isRadioOn;
     }
}