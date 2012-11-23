package com.android.phone;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.internal.telephony.gsm.NetworkInfoWithAcT;
import com.android.phone.sip.SipSettings;

public class NetworkEditor extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    private static final int MENU_SAVE = Menu.FIRST;
    private static final int MENU_DISCARD = Menu.FIRST + 1;

    private static final String BUTTON_NETWORK_PRIORITY = "network_priority_key";
    private static final String BUTTON_NETWORK_SERVICE = "network_mode_key";
    private static final String BUTTON_ADD_PLMN_FROM_LIST = "button_add_plmn_from_list_key";
    private static final String BUTTON_PLMN_NAME = "button_plmn_name_key";
    
    public static final String PLMN_NAME = "plmn_name";
    public static final String PLMN_CODE = "plmn_code";
    public static final String PLMN_PRIORITY = "plmn_priority";
    public static final String PLMN_SERVICE = "plmn_service";
    public static final String PLMN_MAX_PRIORITY = "plmn_max_priority";
    
    public static final int RESULT_DELETE = 2;
    public static final int RESULT_MODIFY = 3;
    
    private NetworkInfoWithAcT mNetwork;
    private Button mRemoveButton;
    
    private int mMaxPriority = 0;
    private ListPreference mListPreference = null;
    private ListPreference mListPreferenceService = null;
    private Preference mButtonName;
    private String[] mEntries;
    private String[] mEntriesValue;
    
    //-2 means user not change the network info
    private int mPriority = -2;  //-1 means delet; -2 means not change
    private int mService = -2;   //-2 means not change
    
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.sip_settings_ui);
        
        addPreferencesFromResource(R.xml.network_editor);
        mListPreference = (ListPreference)this.findPreference(BUTTON_NETWORK_PRIORITY);
        mListPreference.setOnPreferenceChangeListener(this);
        mButtonName = findPreference(BUTTON_PLMN_NAME);
        mListPreferenceService = (ListPreference)findPreference(BUTTON_NETWORK_SERVICE);
        mListPreferenceService.setOnPreferenceChangeListener(this);
        mRemoveButton = (Button)findViewById(R.id.add_remove_account_button);
        mRemoveButton.setText(getString(R.string.cb_menu_delete));
        
        mRemoveButton.setOnClickListener(
            new android.view.View.OnClickListener() {
                public void onClick(View v) {
                    mPriority = -1;
                    setRemovedNetworkAndFinish();
                }
            });
    }
    
    protected void onResume() {
        super.onResume();
        createNetworkInfo(this.getIntent());
        initListPreference();
        mButtonName.setTitle(mNetwork.getOperatorNumeric());
    }
    
    private void initListPreference() {
        this.mEntries = new String[this.mMaxPriority];
        this.mEntriesValue = new String[this.mMaxPriority];
        for (int i = 0; i < this.mMaxPriority; i++) {
            mEntries[i] = String.valueOf(i);
            mEntriesValue[i] = String.valueOf(i);
        }
        mListPreference.setEntries(mEntries);
        mListPreference.setEntryValues(mEntriesValue);
        mListPreference.setValue(String.valueOf(mNetwork.getPriority()));
        mListPreference.setSummary(mListPreference.getEntry());
        
        //set the service selected
        mListPreferenceService.setValue(String.valueOf(covertRilNW2Ap(mNetwork.getAccessTechnology())));
        mListPreferenceService.setSummary(mListPreferenceService.getEntry());
    }
    
    private void createNetworkInfo(Intent intent) {
        String numberName = intent.getStringExtra(PLMN_CODE);
        String operatorName = intent.getStringExtra(PLMN_NAME);
        int priority = intent.getIntExtra(PLMN_PRIORITY, 0);
        mMaxPriority = intent.getIntExtra(PLMN_MAX_PRIORITY, 1);
        int act = intent.getIntExtra(PLMN_SERVICE, 0);
        mNetwork = new NetworkInfoWithAcT(operatorName, numberName, act, priority);
        mPriority = priority;
        mService = act;
    }
    
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mListPreference) {
            int select = Integer.valueOf(objValue.toString());
            if (select != /*(this.mNetwork.getPriority()*/ mPriority) {
                mPriority = select;
                preference.setSummary(objValue.toString());
            }
        } else if (preference == mListPreferenceService) {
            int select = Integer.valueOf(objValue.toString());
            int rilNW = covertApNW2Ril(select);
            if (rilNW != /*this.mNetwork.getAccessTechnology()*/mService) {
                mService = rilNW;
            }
            preference.setSummary(getNWString(covertApNW2Ril(select)));
        }
        return true;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_SAVE, 0, R.string.sip_menu_save)
                .setIcon(android.R.drawable.ic_menu_save);
        menu.add(0, MENU_DISCARD, 0, R.string.sip_menu_discard)
                .setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SAVE:
                validateAndSetResult();
                return true;

            case MENU_DISCARD:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                validateAndSetResult();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private void validateAndSetResult() {
        if ((mNetwork.getPriority() == mPriority) && (mNetwork.getAccessTechnology() == mService)) {
            finish();
            return ;
        }
        Intent intent = new Intent(this, PLMNListPreference.class);
        setResult(RESULT_MODIFY, intent);
        genNetworkInfo(intent, mNetwork);
        finish();
    }
    
    private void genNetworkInfo(Intent intent, NetworkInfoWithAcT info) {
        intent.putExtra(NetworkEditor.PLMN_CODE, info.getOperatorNumeric());
        intent.putExtra(NetworkEditor.PLMN_NAME, info.getOperatorAlphaName());
        intent.putExtra(NetworkEditor.PLMN_PRIORITY, this.mPriority);
        intent.putExtra(NetworkEditor.PLMN_SERVICE, this.mService);
    }
    
    private void setRemovedNetworkAndFinish() {
        Intent intent = new Intent(this, PLMNListPreference.class);
        setResult(RESULT_DELETE, intent);
        mNetwork.setPriority(-1);
        genNetworkInfo(intent, mNetwork);
        finish();
        // do finish() in replaceProfile() in a background thread
    }
    
    static int covertRilNW2Ap(int mode) {
        int result = 0;
        if (mode >= 5) {
            result = 2;
        } else if ((mode & 0x04) != 0) {
            result = 1;
        } else {
            result = 0;
        }
        return result;
    }
    
    static int covertApNW2Ril(int mode) {
        int result = 0;
        if (mode == 2) {
            //dual mode
            result = 0x5;
        } else if (mode == 1) {
            //td-scdma
            result = 0x4;
        } else {
            //gsm or others
            result = 0x1;
        }
        return result;
    }
    
    static String getNWString(int rilNW) {
        int apNW = covertRilNW2Ap(rilNW);
        if (apNW == 2) {
            return "Dual mode";
        } else if (apNW == 1) {
            return "TD-SCDMA";
        } else if (apNW == 0) {
            return "GSM";
        }
        return "";
    }
}