package com.android.phone;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;	
import android.app.ProgressDialog;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Telephony.SIMInfo;
import android.view.Window;
import android.view.WindowManager;
import com.mediatek.xlog.Xlog;

import com.android.internal.telephony.gemini.GeminiPhone;

public class Modem3GCapabilitySwitch extends PreferenceActivity implements Preference.OnPreferenceChangeListener{

    public final static String SERVICE_LIST_KEY = "preferred_3g_service_key";
    public final static String NETWORK_MODE_KEY = "preferred_network_mode_key";
    private Preference mServiceList = null;
    private ListPreference mNetworkMode = null;
    private static final boolean DBG = true;
    private static final String TAG = "Settings/Modem3GCapabilitySwitch";
    
    PhoneInterfaceManager phoneMgr = null;
    private GeminiPhone mGeminiPhone;
    private Phone mPhone;
    MyHandler mHandler;
    private StatusBarManager mStatusBarManager = null;
    static final int preferredNetworkMode = Phone.PREFERRED_NT_MODE;
    private ProgressDialog pd = null;
    private ModemSwitchReceiver mslr;
    private ProgressDialog pdSwitching = null;
    
    private static int SIMID_3G_SERVICE_OFF = -1;
    private static int SIMID_3G_SERVICE_NOT_SET = -2;
    
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Xlog.d(TAG,"onCreate()");
        this.addPreferencesFromResource(R.xml.service_3g_setting);
        mServiceList = findPreference(SERVICE_LIST_KEY);
        mNetworkMode = (ListPreference)findPreference(NETWORK_MODE_KEY);
        mServiceList.setOnPreferenceChangeListener(this);
        mNetworkMode.setOnPreferenceChangeListener(this);
        
        mPhone = PhoneFactory.getDefaultPhone();
        if (CallSettings.isMultipleSim())
        {
            mGeminiPhone = (GeminiPhone)mPhone;
        }
        mHandler = new MyHandler();
        phoneMgr = PhoneApp.getInstance().phoneMgr;
        
        mslr = new ModemSwitchReceiver();
        IntentFilter intentFilter = new IntentFilter(GeminiPhone.EVENT_3G_SWITCH_LOCK_CHANGED);
        intentFilter.addAction(GeminiPhone.EVENT_PRE_3G_SWITCH);
        intentFilter.addAction(GeminiPhone.EVENT_3G_SWITCH_DONE);
        intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        this.registerReceiver(mslr, intentFilter);
        
    }
    
    protected void onResume() {
        super.onResume();
        long simId = SIMID_3G_SERVICE_NOT_SET;
        int slot = phoneMgr.get3GCapabilitySIM();
        if (slot == SIMID_3G_SERVICE_OFF) {
            simId = slot;
        } else {
            SIMInfo info = SIMInfo.getSIMInfoBySlot(this, slot);
            simId = info != null ? info.mSimId : SIMID_3G_SERVICE_NOT_SET;
        }
        
        updateSummarys(simId);
        updateNetworkMode();
        updateItemStatus();
    }
    
    private void updateNetworkMode() {
        if (mNetworkMode == null) {
            return ;
        }
        int slot = phoneMgr.get3GCapabilitySIM();
        Xlog.d(TAG, "updateNetworkMode(), 3G capability slot="+slot);
        if (CallSettings.isMultipleSim() && slot != -1 && CallSettings.isRadioOn(slot)) {
            mNetworkMode.setEnabled(true);
            Xlog.d(TAG, "Try to get preferred network mode for slot "+slot);
            mGeminiPhone.getPreferredNetworkTypeGemini(mHandler.obtainMessage(MyHandler.MESSAGE_GET_PREFERRED_NETWORK_TYPE), slot);
        }else{
            mNetworkMode.setEnabled(false);
            mNetworkMode.setSummary("");
        }
    }
    
    private void updateSummarys(long simId) {
        if (mServiceList == null) {
            return ;
        }
        Xlog.d(TAG, "updateSummarys(), simId="+simId);
        if (simId == SIMID_3G_SERVICE_OFF) {
            //3G service is off
            mServiceList.setSummary(R.string.service_3g_off);
            if (mNetworkMode != null) {
                mNetworkMode.setEnabled(false);
                mNetworkMode.setSummary("");
            }
        } else if (simId == SIMID_3G_SERVICE_NOT_SET) {
            //Clear the summary
            mServiceList.setSummary("");
            mNetworkMode.setEnabled(false);
            mNetworkMode.setSummary("");
        } else {
            SIMInfo info = SIMInfo.getSIMInfoById(this, simId);
            if (info != null) {
                mServiceList.setSummary(info.mDisplayName);
                //if the 3G service slot is radio off, disable the network mode
                boolean isPowerOn = CallSettings.isRadioOn(info.mSlot);
                Xlog.d(TAG, "updateSummarys(), SIM "+simId+" power status is "+isPowerOn);
                mNetworkMode.setEnabled(isPowerOn);
                if(!isPowerOn){
                    mNetworkMode.setSummary("");
                }
            }
        }
    }
    
    public void changeForNetworkMode(Object objValue ) {
        mNetworkMode.setValue((String) objValue);
        int buttonNetworkMode;
        buttonNetworkMode = Integer.valueOf((String) objValue).intValue();
        int settingsNetworkMode = android.provider.Settings.Secure.getInt(
                mPhone.getContext().getContentResolver(),
                android.provider.Settings.Secure.PREFERRED_NETWORK_MODE, preferredNetworkMode);
        if (buttonNetworkMode != settingsNetworkMode) {
            showProgressDialog();
            int modemNetworkMode;
            switch(buttonNetworkMode) {
                case Phone.NT_MODE_GLOBAL:
                    modemNetworkMode = Phone.NT_MODE_GLOBAL;
                    break;
                case Phone.NT_MODE_EVDO_NO_CDMA:
                    modemNetworkMode = Phone.NT_MODE_EVDO_NO_CDMA;
                    break;
                case Phone.NT_MODE_CDMA_NO_EVDO:
                    modemNetworkMode = Phone.NT_MODE_CDMA_NO_EVDO;
                    break;
                case Phone.NT_MODE_CDMA:
                    modemNetworkMode = Phone.NT_MODE_CDMA;
                    break;
                case Phone.NT_MODE_GSM_UMTS:
                    modemNetworkMode = Phone.NT_MODE_GSM_UMTS;
                    break;
                case Phone.NT_MODE_WCDMA_ONLY:
                    modemNetworkMode = Phone.NT_MODE_WCDMA_ONLY;
                    break;
                case Phone.NT_MODE_GSM_ONLY:
                    modemNetworkMode = Phone.NT_MODE_GSM_ONLY;
                    break;
                case Phone.NT_MODE_WCDMA_PREF:
                    modemNetworkMode = Phone.NT_MODE_WCDMA_PREF;
                    break;
                default:
                    modemNetworkMode = Phone.PREFERRED_NT_MODE;
            }
            UpdatePreferredNetworkModeSummary(buttonNetworkMode);

            android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                    buttonNetworkMode );
            //Set the modem network mode
            int slot = phoneMgr.get3GCapabilitySIM();
            if (CallSettings.isMultipleSim()) {
                mGeminiPhone.setPreferredNetworkTypeGemini(modemNetworkMode, 
                        mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE), slot);
            } else {
                mPhone.setPreferredNetworkType(modemNetworkMode, 
                        mHandler.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
            }
        }
    }
    
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mServiceList) {
            long simId = Long.valueOf(objValue.toString());
            //updateSummarys(simId);
            //updateNetworkMode();
            handleServiceSwitch(simId);
        } else if (preference == mNetworkMode) {
            changeForNetworkMode(objValue);
        }
        return true;
    }

    void showSwitchProgress() {
        if (pdSwitching != null) {
            Xlog.d(TAG, "The progress dialog already exist, so exit!");
            return ;
        }
        
        pdSwitching = new ProgressDialog(this);
        if (pdSwitching != null) {
            pdSwitching.setMessage(getResources().getString(R.string.modem_switching));
        }
        Xlog.d(TAG, "Create and show the progress dialog...");
        pdSwitching.setCancelable(false);
        
        Window win = pdSwitching.getWindow();
        WindowManager.LayoutParams lp = win.getAttributes();
        lp.flags |= WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED;
        win.setAttributes(lp);
        pdSwitching.show();
    }

    void clearAfterSwitch(Intent it) {
        long simId3G = SIMID_3G_SERVICE_NOT_SET;
        Xlog.d(TAG, "clearAfterSwitch(), remove switching dialog");
        disSwitchProgressDialog();
        setStatusBarEnableStatus(true);
        //the slot which supports 3g service after switch
        //then get the simid which inserted to the 3g slot
        int slot3G = it.getIntExtra(GeminiPhone.EXTRA_3G_SIM, SIMID_3G_SERVICE_NOT_SET);
        if (slot3G == SIMID_3G_SERVICE_OFF) {
            simId3G = SIMID_3G_SERVICE_OFF;
        } else {
            SIMInfo info = SIMInfo.getSIMInfoBySlot(this, slot3G);
            if (info != null) {
                simId3G = info.mSimId;
            }
        }

        this.updateSummarys(simId3G);
        this.updateNetworkMode();
    }
    

    private void disSwitchProgressDialog() {
        if (pdSwitching != null && pdSwitching.isShowing()) {
            pdSwitching.dismiss();
            pdSwitching = null;
        }
    }
    
    private void handleServiceSwitch(long simId) {
        if (phoneMgr.is3GSwitchLocked()) {
            Xlog.d(TAG, "Switch has been locked, return");
            return ;
        }
        Xlog.d(TAG, "handleServiceSwitch("+simId+"), show switching dialog first");
        showSwitchProgress();
        setStatusBarEnableStatus(false);
        int slotId = -1;
        if (simId != -1) {
            SIMInfo info = SIMInfo.getSIMInfoById(this, simId);
            slotId = info==null?-1:info.mSlot;
        }
        if (phoneMgr.set3GCapabilitySIM(slotId)) {
            Xlog.d(TAG, "Receive ok for the switch, and starting the waiting...");
        } else {
            Xlog.d(TAG, "Receive error for the switch & Dismiss switching didalog");
            disSwitchProgressDialog();
            setStatusBarEnableStatus(true);
            //maybe: need update the ui if switch fail
            //this.updateSummarys(long simId);
            //this.updateNetworkMode();
        }
    }
    
    private class MyHandler extends Handler {

        private static final int MESSAGE_GET_PREFERRED_NETWORK_TYPE = 0;
        private static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_PREFERRED_NETWORK_TYPE:
                    handleGetPreferredNetworkTypeResponse(msg);
                    break;

                case MESSAGE_SET_PREFERRED_NETWORK_TYPE:
                    handleSetPreferredNetworkTypeResponse(msg);
                    break;
            }
        }

        private void handleGetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception == null) {
                int modemNetworkMode = ((int[])ar.result)[0];

                if (DBG) {
                    Xlog.d(TAG, "handleGetPreferredNetworkTypeResponse: modemNetworkMode = " +
                            modemNetworkMode);
                }

                int settingsNetworkMode = android.provider.Settings.Secure.getInt(
                        mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                        preferredNetworkMode);

                if (DBG) {
                    Xlog.d(TAG, "handleGetPreferredNetworkTypeReponse: settingsNetworkMode = " +
                            settingsNetworkMode);
                }

                //check that modemNetworkMode is from an accepted value
                if (modemNetworkMode == Phone.NT_MODE_WCDMA_PREF ||
                        modemNetworkMode == Phone.NT_MODE_GSM_ONLY ||
                        modemNetworkMode == Phone.NT_MODE_WCDMA_ONLY ||
                        modemNetworkMode == Phone.NT_MODE_GSM_UMTS ||
                        modemNetworkMode == Phone.NT_MODE_CDMA ||
                        modemNetworkMode == Phone.NT_MODE_CDMA_NO_EVDO ||
                        modemNetworkMode == Phone.NT_MODE_EVDO_NO_CDMA ||
                        modemNetworkMode == Phone.NT_MODE_GLOBAL ) {
                    if (DBG) {
                        Xlog.d(TAG, "handleGetPreferredNetworkTypeResponse: if 1: modemNetworkMode = " +
                                modemNetworkMode);
                    }

                    //check changes in modemNetworkMode and updates settingsNetworkMode
                    if (modemNetworkMode != settingsNetworkMode) {
                        if (DBG) {
                            Xlog.d(TAG, "handleGetPreferredNetworkTypeResponse: if 2: " +
                                    "modemNetworkMode != settingsNetworkMode");
                        }

                        settingsNetworkMode = modemNetworkMode;

                        if (DBG) {
                            Xlog.d(TAG, "handleGetPreferredNetworkTypeResponse: if 2: " +
                                "settingsNetworkMode = " + settingsNetworkMode);
                        }

                        //changes the Settings.System accordingly to modemNetworkMode
                        android.provider.Settings.Secure.putInt(
                                mPhone.getContext().getContentResolver(),
                                android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                                settingsNetworkMode );
                    }
                    mNetworkMode.setValue(Integer.toString(modemNetworkMode));
                    UpdatePreferredNetworkModeSummary(modemNetworkMode);
                    
                } else {
                    if (DBG) Xlog.d(TAG, "handleGetPreferredNetworkTypeResponse: else: reset to default");
                    resetNetworkModeToDefault();
                }
            }else{
                Xlog.i(TAG, "handleGetPreferredNetworkTypeResponse(), response exist exception");
            }
        }

        private void handleSetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            if (pd != null && pd.isShowing()) {
                try {
                    pd.dismiss();
                } catch (Exception e) {
                    Xlog.d(TAG, e.toString());
                }
            }
            
            if (pd != null) {
                pd = null;
            }
            
            if (ar.exception == null) {
                int networkMode = Integer.valueOf(
                        mNetworkMode.getValue()).intValue();
                android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                        networkMode );
            } else {
                int slot = phoneMgr.get3GCapabilitySIM();
                if (CallSettings.isMultipleSim()) {
                    mGeminiPhone.getPreferredNetworkTypeGemini(obtainMessage(MESSAGE_GET_PREFERRED_NETWORK_TYPE), slot);
                } else {
                    mPhone.getPreferredNetworkType(obtainMessage(MESSAGE_GET_PREFERRED_NETWORK_TYPE));
                }
            }
        }

        private void resetNetworkModeToDefault() {
            //set the mNetworkMode
            mNetworkMode.setValue(Integer.toString(preferredNetworkMode));
            //set the Settings.System
            android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                        preferredNetworkMode );
            //Set the Modem
            int slot = phoneMgr.get3GCapabilitySIM();
            if (CallSettings.isMultipleSim()) {
                mGeminiPhone.setPreferredNetworkTypeGemini(preferredNetworkMode,
                            this.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE), slot);
            } else {
                mPhone.setPreferredNetworkType(preferredNetworkMode,
                        this.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
            }
        }
    }
    
    private void UpdatePreferredNetworkModeSummary(int NetworkMode) {
        String value = mNetworkMode.getValue();
        switch(NetworkMode) {
            case Phone.NT_MODE_WCDMA_PREF:
                // TODO T: Make all of these strings come from res/values/strings.xml.
                mNetworkMode.setSummary(mNetworkMode.getEntry());
                //mNetworkMode.getEntry();
                break;
            case Phone.NT_MODE_GSM_ONLY:
                //mNetworkMode.setSummary("GSM only");
                mNetworkMode.setSummary(mNetworkMode.getEntry());
                break;
            case Phone.NT_MODE_WCDMA_ONLY:
                //mNetworkMode.setSummary("WCDMA only");
                mNetworkMode.setSummary(mNetworkMode.getEntry());
                break;
            case Phone.NT_MODE_GSM_UMTS:
                mNetworkMode.setSummary("GSM/WCDMA");
                break;
            case Phone.NT_MODE_CDMA:
                mNetworkMode.setSummary("CDMA / EvDo");
                break;
            case Phone.NT_MODE_CDMA_NO_EVDO:
                mNetworkMode.setSummary("CDMA only");
                break;
            case Phone.NT_MODE_EVDO_NO_CDMA:
                mNetworkMode.setSummary("EvDo only");
                break;
            case Phone.NT_MODE_GLOBAL:
            default:
                mNetworkMode.setSummary("Global");
        }
    }
    
    private void showProgressDialog() {
        // TODO Auto-generated method stub
        pd = new ProgressDialog(this);
        pd.setMessage(getText(R.string.updating_settings));
        pd.setCancelable(false);
        pd.setIndeterminate(true);
        pd.show();
    }
    
    private void updateItemStatus() {
        boolean enabled =  phoneMgr.is3GSwitchLocked();
        Xlog.d(TAG, "updateItemStatus(), is3GSwitchLocked()?"+enabled);
        if (this.mServiceList != null) {
            mServiceList.setEnabled(!enabled);
        }
        
        if (this.mNetworkMode != null) {
            //Two sim insert, set simA has 3G service, then power off and remove SimA
            //Power on, not switch 3G to SimB, return the slot which simA has been inserted
            //so check there is sim insert in the 3G service slot
            int cardSlot = phoneMgr.get3GCapabilitySIM();
            SIMInfo info = SIMInfo.getSIMInfoBySlot(this, cardSlot);
            mNetworkMode.setEnabled(!enabled && (info != null) && (CallSettings.isRadioOn(cardSlot)));
            if(!mNetworkMode.isEnabled()){
                mNetworkMode.setSummary("");
            }
        }
    }
    
    protected void onDestroy() {
        super.onDestroy();
        Xlog.d(TAG, "onDestroy()");
        
        if (pd != null && pd.isShowing()) {
            try {
                pd.dismiss();
            } catch (Exception e) {
                Xlog.d(TAG, e.toString());
            }
        }
        if (pd != null) {
            pd = null;
        }
        disSwitchProgressDialog();
        
        if(mslr!=null){
            unregisterReceiver(mslr);
        }
        //restore status bar status after finish, to avoid unexpected event
        setStatusBarEnableStatus(true);
    }
    
    /**
     * When switching modem, the status bar should be disabled
     * @param enabled
     */
    private void setStatusBarEnableStatus(boolean enabled){
        Xlog.i(TAG, "setStatusBarEnableStatus("+enabled+")");
        if(mStatusBarManager == null){
            mStatusBarManager = (StatusBarManager)getSystemService(Context.STATUS_BAR_SERVICE);
        }
        if(mStatusBarManager!=null){
            if(enabled){
                mStatusBarManager.disable(StatusBarManager.DISABLE_NONE);
            }else{
                mStatusBarManager.disable(StatusBarManager.DISABLE_EXPAND);
            }
        }else{
            Xlog.e(TAG, "Fail to get status bar instance");
        }
    }
    
    class ModemSwitchReceiver extends BroadcastReceiver {
        
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (GeminiPhone.EVENT_3G_SWITCH_LOCK_CHANGED.equals(action)) {
                Xlog.d(TAG, "receives EVENT_3G_SWITCH_LOCK_CHANGED...");
                boolean bLocked = intent.getBooleanExtra(GeminiPhone.EXTRA_3G_SWITCH_LOCKED, false);
                updateItemStatus();
            } else if (GeminiPhone.EVENT_PRE_3G_SWITCH.equals(action)) {
                Xlog.d(TAG, "Starting the switch......@" + this);
                showSwitchProgress();
                setStatusBarEnableStatus(false);
            } else if (GeminiPhone.EVENT_3G_SWITCH_DONE.equals(action)) {
                Xlog.d(TAG, "Done the switch......@" + this);
                clearAfterSwitch(intent);
            }else if(Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)){
                boolean airplaneMode = intent.getBooleanExtra("state", false);
                Xlog.d(TAG, "airplaneMode new  state is ["+airplaneMode+"]");
                updateNetworkMode();
            }
        }
    }
}
