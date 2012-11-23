package com.android.phone;

import com.android.internal.telephony.Phone;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.MediaStore.Streaming.Setting;
import android.provider.Telephony.SIMInfo;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.provider.Settings;;


public class IpPrefixPreference extends PreferenceActivity implements OnPreferenceChangeListener, TextWatcher {
    private static final String IP_PREFIX_NUMBER_EDIT_KEY = "button_ip_prefix_edit_key";
    private EditTextPreference mButtonIpPrefix = null;
    private int mSlot = -1;
    private String initTitle = null;
    protected void onCreate(Bundle icicle) {
        
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.ip_prefix_setting);
        mButtonIpPrefix = (EditTextPreference)this.findPreference(IP_PREFIX_NUMBER_EDIT_KEY);
        mButtonIpPrefix.setOnPreferenceChangeListener(this);
        
        mSlot = getIntent().getIntExtra(Phone.GEMINI_SIM_ID_KEY, -1);
        
        initTitle = getIntent().getStringExtra(MultipleSimActivity.SUB_TITLE_NAME);
        if (initTitle != null) {
            this.setTitle(initTitle);
        }
    }
    
    protected void onResume() {
        super.onResume();
        String preFix = this.getIpPrefix(mSlot);
        if ((preFix != null) && (!"".equals(preFix))) {
            mButtonIpPrefix.setSummary(preFix);
            mButtonIpPrefix.setText(preFix);
        } else {
            mButtonIpPrefix.setSummary(R.string.ip_prefix_edit_default_sum);
            mButtonIpPrefix.setText("");
        }
    }
    
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        this.mButtonIpPrefix.setSummary(newValue.toString());
        mButtonIpPrefix.setText(newValue.toString());
        if (newValue == null || "".equals(newValue)) {
            mButtonIpPrefix.setSummary(R.string.ip_prefix_edit_default_sum);
        }
        saveIpPrefix(newValue.toString());
        return false;
    }
    
    private void saveIpPrefix(String str) {
        String key = "ipprefix";
        if (CallSettings.isMultipleSim()) {
            SIMInfo info = SIMInfo.getSIMInfoBySlot(this, mSlot);
            if (info != null) {
                key += Long.valueOf(info.mSimId).toString();
            }
        }
        if (!Settings.System.putString(this.getContentResolver(), key, str)) {
            Log.d("IpPrefixPreference", "Store ip prefix error!");
        }
    }
    
    private String getIpPrefix(int slot) {
        String key = "ipprefix";
        if (CallSettings.isMultipleSim()) {
            SIMInfo info = SIMInfo.getSIMInfoBySlot(this, mSlot);
            if (info != null) {
                key += Long.valueOf(info.mSimId).toString();
            }
        }
        
        return Settings.System.getString(this.getContentResolver(),key);
    }
    
    public void beforeTextChanged(CharSequence s, int start,
            int count, int after) {
        
    }
    
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        
    }
    
    public void afterTextChanged(Editable s) {
        
    }
    
}