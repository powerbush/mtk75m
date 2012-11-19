package com.android.mms.ui;

import static android.content.res.Configuration.KEYBOARDHIDDEN_NO;

import java.util.List;

import com.android.mms.R;
import com.android.mms.ui.AdvancedEditorPreference.GetSimInfo;

import android.R.color;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.Phone;
import android.provider.Telephony;
import android.provider.Telephony.SIMInfo;
import com.mediatek.telephony.TelephonyManagerEx;
import com.android.mms.ui.MessagingPreferenceActivity;
import com.mediatek.xlog.Xlog;

public class SelectCardPreferenceActivity extends PreferenceActivity implements GetSimInfo{
    private static final String TAG = "Mms/SelectCardPreferenceActivity";
    
    private AdvancedEditorPreference mSim1;
    private AdvancedEditorPreference mSim2;
    private AdvancedEditorPreference mSim3;
    private AdvancedEditorPreference mSim4;
    
    private String mSim1Number;
    private String mSim2Number;
    private String mSim3Number;
    private String mSim4Number;
    
    private int simCount;
    
    private int currentSim = -1;
    private TelephonyManagerEx mTelephonyManager;
    private EditText mNumberText;
    private AlertDialog mNumberTextDialog;
    private List<SIMInfo> listSimInfo;
    String intentPreference;
    private static Handler mSMSHandler = new Handler();
    private final int MAX_EDITABLE_LENGTH = 20;
    
    private AlertDialog mSaveLocDialog;//for sms save location
    private SharedPreferences spref;
    
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        listSimInfo = SIMInfo.getInsertedSIMList(this);
        simCount = listSimInfo.size();
        spref = PreferenceManager.getDefaultSharedPreferences(this);
        addPreferencesFromResource(R.xml.multicardeditorpreference);    
        Intent intent = getIntent();
        intentPreference = intent.getStringExtra("preference");
        changeMultiCardKeyToSimRelated(intentPreference);
    }
    
    private void changeMultiCardKeyToSimRelated(String preference) {

        mSim1 = (AdvancedEditorPreference) findPreference("pref_key_sim1");
        mSim1.init(this, 0);
        mSim2 = (AdvancedEditorPreference) findPreference("pref_key_sim2");
        mSim2.init(this, 1);
        mSim3 = (AdvancedEditorPreference) findPreference("pref_key_sim3");
        mSim3.init(this, 2);
        mSim4 = (AdvancedEditorPreference) findPreference("pref_key_sim4");
        mSim4.init(this, 3);
        //get the stored value
        SharedPreferences sp = getSharedPreferences("com.android.mms_preferences", MODE_WORLD_READABLE);
        if (simCount == 1) {
            getPreferenceScreen().removePreference(mSim2);
            getPreferenceScreen().removePreference(mSim3);
            getPreferenceScreen().removePreference(mSim4);           
        } else if (simCount == 2) {
            getPreferenceScreen().removePreference(mSim3);
            getPreferenceScreen().removePreference(mSim4);   
        } else if (simCount == 3) {
            getPreferenceScreen().removePreference(mSim4);
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        int currentId = 0;
        if (preference == mSim1) {
            currentId = 0;
        } else if (preference == mSim2) {
            currentId = 1;
        } else if (preference == mSim3) {
            currentId = 2;
        } else if (preference == mSim4) {
            currentId = 3;
        }
        if (intentPreference.equals(MessagingPreferenceActivity.SMS_MANAGE_SIM_MESSAGES)) {
            startManageSimMessages(currentId);
        } else if (intentPreference.equals(MessagingPreferenceActivity.SMS_SERVICE_CENTER)) {
            currentSim = listSimInfo.get(currentId).mSlot;
            setServiceCenter(currentSim);
        } else if(intentPreference.equals(MessagingPreferenceActivity.SMS_SAVE_LOCATION)){
        	setSaveLocation(currentId);
        }
        
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
    
    private void setSaveLocation(long id){
    	Xlog.d(TAG, "currentSlot is: " + id);
       	//the key value for each saveLocation
       	final String [] saveLocation = getResources().getStringArray(R.array.pref_sms_save_location_values);
       	//the diplayname for each saveLocation
    	final String [] saveLocationDisp = getResources().getStringArray(R.array.pref_sms_save_location_choices);
       	if(saveLocation == null || saveLocationDisp == null){
       		Xlog.d(TAG, "setSaveLocation is null");
       		return;
       	}
       	final String saveLocationKey = Long.toString(id) + "_" + MessagingPreferenceActivity.SMS_SAVE_LOCATION;
       	int pos = getSelectedPosition(saveLocationKey, saveLocation);
       	mSaveLocDialog = new AlertDialog.Builder(this)
    	.setTitle(R.string.sms_save_location)
        .setNegativeButton(R.string.Cancel, new NegativeButtonListener())
        .setSingleChoiceItems(saveLocationDisp, pos, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				Xlog.d(TAG, "setSaveLocation whichButton = "+whichButton);
				SharedPreferences.Editor editor = spref.edit();
				editor.putString(saveLocationKey, saveLocation[whichButton]);
				editor.commit();
				mSaveLocDialog.dismiss();
				mSaveLocDialog = null;
			}
		})
        .show();
    }
    
    //get the position which is selected before
    private int getSelectedPosition(String inputmodeKey, String [] modes){
        String res = spref.getString(inputmodeKey, "Phone");
        Xlog.d(TAG, "getSelectedPosition found the res = "+res);
        for(int i = 0; i < modes.length; i++){
        	if(res.equals(modes[i])){
        		Xlog.d(TAG, "getSelectedPosition found the position = "+i);
        		return i;
        	}
        }
        Xlog.d(TAG, "getSelectedPosition not found the position");

        return 0; 
    }
    
    public void setServiceCenter(int id){
        mNumberText = new EditText(this);
        mNumberText.setHint(R.string.type_to_compose_text_enter_to_send);
        mNumberText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_EDITABLE_LENGTH)});
        //mNumberText.setKeyListener(new DigitsKeyListener(false, true));
        mNumberText.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_CLASS_PHONE);
        mNumberText.computeScroll();
        Xlog.d(TAG, "currentSlot is: " + id);
        String scNumber = getServiceCenter(id);
        Xlog.d(TAG, "getScNumber is: " + scNumber);
        mNumberText.setText(scNumber);
        mNumberTextDialog = new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setTitle(R.string.sms_service_center)
        .setView(mNumberText)
        .setPositiveButton(R.string.OK, new PositiveButtonListener())
        .setNegativeButton(R.string.Cancel, new NegativeButtonListener())
        .show();
    }

    public void startManageSimMessages(int id){
        if(listSimInfo == null || id >= listSimInfo.size()){
            Xlog.e(TAG, "startManageSimMessages listSimInfo is null ");
            return;
        }
        Intent it = new Intent();
         int slotId = listSimInfo.get(id).mSlot;
         Xlog.d(TAG, "currentSlot is: " + slotId);
         Xlog.d(TAG, "currentSlot name is: " + listSimInfo.get(0).mDisplayName);
         it.setClass(this, ManageSimMessages.class);
         it.putExtra("SlotId", slotId);
         startActivity(it);
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
    
    public boolean is3G(int id)    {
        mTelephonyManager = TelephonyManagerEx.getDefault();
        //int slotId = SIMInfo.getSlotById(this, listSimInfo.get(id).mSimId);
        int slotId = listSimInfo.get(id).mSlot;
        Xlog.d(TAG, "SIMInfo.getSlotById id: " + id + " slotId: " + slotId);
        if (slotId == MessageUtils.get3GCapabilitySIM()) {
            return true;
        }
        return false;
    }
    
    private String getServiceCenter(int id) {
        mTelephonyManager = TelephonyManagerEx.getDefault();
        return mTelephonyManager.getScAddress(id);    
    }
    
    private boolean setServiceCenter(String SCnumber, int id) {
        mTelephonyManager = TelephonyManagerEx.getDefault();
        Xlog.d(TAG, "setScAddress is: " + SCnumber);
        return mTelephonyManager.setScAddress(SCnumber, id);
    }
    
    private void tostScOK() {
        Toast.makeText(this, R.string.set_service_center_OK, 0);
    }
    
    private void tostScFail() {
        Toast.makeText(this, R.string.set_service_center_fail, 0);
    }
    
    private class PositiveButtonListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
            mTelephonyManager = TelephonyManagerEx.getDefault();
            String scNumber = mNumberText.getText().toString();
            Xlog.d(TAG, "setScNumber is: " + scNumber);
            Xlog.d(TAG, "currentSim is: " + currentSim);
            //setServiceCenter(scNumber, currentSim);
            new Thread(new Runnable() {
                public void run() {
                    mTelephonyManager.setScAddress(mNumberText.getText().toString(), currentSim);
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
}
