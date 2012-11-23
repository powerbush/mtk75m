package com.android.phone;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import com.android.internal.telephony.Phone;
import android.preference.Preference;

public class CellBroadcastActivity extends TimeConsumingPreferenceActivity
{
	private static final String BUTTON_CB_CHECKBOX_KEY     = "enable_cellBroadcast";
	private static final String BUTTON_CB_SETTINGS_KEY     = "cbsettings";
	int mSimId = 0;
	
    CellBroadcastCheckBox cbCheckBox = null;
    Preference cbSetting = null;
    @Override
    protected void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.cell_broad_cast);
        mSimId = getIntent().getIntExtra(Phone.GEMINI_SIM_ID_KEY, 0);
        
        cbCheckBox = (CellBroadcastCheckBox)findPreference(BUTTON_CB_CHECKBOX_KEY);
        cbSetting = findPreference(BUTTON_CB_SETTINGS_KEY);
        cbCheckBox.setSummary(cbCheckBox.isChecked()? R.string.sum_cell_broadcast_control_on : R.string.sum_cell_broadcast_control_off);
        
        if (null != getIntent().getStringExtra(MultipleSimActivity.SUB_TITLE_NAME))
        {
            setTitle(getIntent().getStringExtra(MultipleSimActivity.SUB_TITLE_NAME));
        }
        
        if (cbCheckBox != null) {
            cbCheckBox.init(this, false, mSimId);
        }
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        
        if (preference == cbSetting)
        {
            Intent intent = new Intent(this, CellBroadcastSettings.class);
            intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            this.startActivity(intent);
            return true;
        }
        return false;
    }
}