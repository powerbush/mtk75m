package com.android.phone;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.gsm.NetworkInfoWithAcT;
import com.mediatek.xlog.Xlog;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.sip.SipProfile;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class PLMNListPreference extends TimeConsumingPreferenceActivity {
    
    private List<NetworkInfoWithAcT> mPLMNList;
    private int startIndex = 0;
    private int stopIndex = 0;
    private int numbers = 0;
    private Preference mClicked = null;
    private PreferenceCategory mPLMNListContainer;
    private Preference mButtonAddnew;
    private Map<Preference, NetworkInfoWithAcT> mPreferenceMap = new LinkedHashMap<Preference, NetworkInfoWithAcT>();
    
    private static final String LOG_TAG = "Settings/PLMNListPreference";
    private static final boolean DGB = true;
    private static final String BUTTON_PLMN_CONTAINER = "plmn_list";
    private static final String BUTTON_ADD_PLMN = "button_add_plmn_key";
    private static final String BUTTON_ADD_PLMN_FROM_LIST = "button_add_plmn_from_list_key";
    private static final int REQUEST_ADD_OR_EDIT_PLMN = 1;
    private static final boolean DBG = true;
    
    private int mSlotId = 0;
    private Phone mPhone = null;
    private SIMCapability mCapability = new SIMCapability(0, 0, 0, 0);
    
    private MyHandler mHandler = new MyHandler();
    
    ArrayList<String> listPriority = new ArrayList<String>();
    //listPriority.add("1");
    //listPriority.add("2");
    ArrayList<String> listService = new ArrayList<String>();    
    
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.plmn_list);
        mPLMNListContainer = (PreferenceCategory)findPreference(BUTTON_PLMN_CONTAINER);
        mButtonAddnew = findPreference(BUTTON_ADD_PLMN);
        
        mPhone = PhoneFactory.getDefaultPhone();
        mSlotId = getIntent().getIntExtra(Phone.GEMINI_SIM_ID_KEY, 0);
    }
    
    public void onResume() {
        super.onResume();
        getSIMCapability();
        init(this, false, mSlotId);
    }
    
    void init(TimeConsumingPreferenceListener listener, boolean skipReading, int mSlotId) {
        Xlog.d(LOG_TAG, "init with simSlot = " + mSlotId);
        if (!skipReading) {
            if (CallSettings.isMultipleSim()) {
                //getPreferedOperatorListGemini
                GeminiPhone dualPhone = (GeminiPhone)mPhone;
                dualPhone.getPreferedOperatorListGemini(mSlotId, mHandler.obtainMessage(MyHandler.MESSAGE_GET_PLMN_LIST, mSlotId, MyHandler.MESSAGE_GET_PLMN_LIST));
            } else {
                mPhone.getPreferedOperatorList(mHandler.obtainMessage(MyHandler.MESSAGE_GET_PLMN_LIST, mSlotId, MyHandler.MESSAGE_GET_PLMN_LIST));
            }
            
            if (listener != null) {
                listener.onStarted(mPLMNListContainer, true);
            }
        }
    }
    
    private void getSIMCapability() {
        if (CallSettings.isMultipleSim()) {
            //getPreferedOperatorListGemini
            GeminiPhone dualPhone = (GeminiPhone)mPhone;
            dualPhone.getPOLCapabilityGemini(mSlotId, mHandler.obtainMessage(MyHandler.MESSAGE_GET_PLMN_CAPIBILITY, mSlotId, MyHandler.MESSAGE_GET_PLMN_CAPIBILITY));
        } else {
            mPhone.getPOLCapability(mHandler.obtainMessage(MyHandler.MESSAGE_GET_PLMN_CAPIBILITY, mSlotId, MyHandler.MESSAGE_GET_PLMN_CAPIBILITY));
        }
    }
    
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = super.onCreateDialog(id);
        if (null != dialog) {
            dialog.setTitle(R.string.update_plmn_setting);
        }
        return dialog;
    }
    
    private void refreshPreference(ArrayList<NetworkInfoWithAcT> list) {
        if (mPLMNListContainer.getPreferenceCount() != 0) {
            mPLMNListContainer.removeAll();
        }
        if (this.mPreferenceMap != null) {
            mPreferenceMap.clear();
        }
        
        if (this.mPLMNList != null) {
            mPLMNList.clear();
        }
        mPLMNList = list;
        if (list == null || list.size() == 0) {
            if (DBG) Xlog.d(LOG_TAG, "refreshPreference : NULL PLMN list!");
            if (list == null) mPLMNList = new ArrayList<NetworkInfoWithAcT>();
            return ;
        }
        //sorts the network info base on the priority
        Collections.sort(list, new NetworkCompare());
        
        for (NetworkInfoWithAcT network : list) {
            addPLMNPreference(network);
            if (DGB) {
                Xlog.d(LOG_TAG, network.toString());
            }
        }
    }
    
    class NetworkCompare implements Comparator<NetworkInfoWithAcT> {

        public int compare(NetworkInfoWithAcT object1, NetworkInfoWithAcT object2) {
            // TODO Auto-generated method stub
            return (object1.getPriority() - object2.getPriority());
        }
        
    }
    
    void addPLMNPreference(NetworkInfoWithAcT network) {
        PLMNPreference pref = new PLMNPreference(this);
        String plmnName = network.getOperatorAlphaName();
        String extendName = NetworkEditor.getNWString(network.getAccessTechnology());
        pref.setTitle(plmnName + "(" + extendName + ")");
        mPLMNListContainer.addPreference(pref);
        mPreferenceMap.put(pref, network);
    }
    
    void extractInfoFromNetworkInfo(Intent intent, NetworkInfoWithAcT info) {
        
        intent.putExtra(NetworkEditor.PLMN_CODE, info.getOperatorNumeric());
        intent.putExtra(NetworkEditor.PLMN_NAME, info.getOperatorAlphaName());
        intent.putExtra(NetworkEditor.PLMN_PRIORITY, info.getPriority());
        intent.putExtra(NetworkEditor.PLMN_SERVICE, info.getAccessTechnology());
        NetworkInfoWithAcT info2 = mPLMNList.get(mPLMNList.size() -1);
        intent.putExtra(NetworkEditor.PLMN_MAX_PRIORITY, info2.getPriority() + 1);
    }
    
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof PLMNPreference) {
            Intent intent = new Intent(this, NetworkEditor.class);
            NetworkInfoWithAcT info = this.mPreferenceMap.get(preference);
            extractInfoFromNetworkInfo(intent, info);            
            startActivityForResult(intent, REQUEST_ADD_OR_EDIT_PLMN);
            mClicked = preference;
            return true;
        } else if (preference == mButtonAddnew){
            int index = mPLMNList != null ? mPLMNList.size() : 0;
            if (index >= mCapability.lastIndex + 1) {
                new AlertDialog.Builder(this).setMessage("SIM has no space to add new PLMN!")
                        .setTitle(android.R.string.dialog_alert_title)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, this)
                        .show();
                return false;
            }
            showInputDialog("");
            mClicked = preference;
            return true;
        }
        
        return false;
    }
    
    protected void onActivityResult(final int requestCode, final int resultCode,
            final Intent intent) {
        Xlog.d(LOG_TAG, "resultCode = " + resultCode);
        
        if (resultCode <= Activity.RESULT_FIRST_USER) return;
        
        if (NetworkEditor.RESULT_DELETE == resultCode) {
            Xlog.d(LOG_TAG, "onActivityResult delete");
        } else if (NetworkEditor.RESULT_MODIFY == resultCode) {
            Xlog.d(LOG_TAG, "onActivityResult modify");
        }
        NetworkInfoWithAcT newInfo = null;
        newInfo = createNetworkInfo(intent);
        
        this.handlePLMNListChange(newInfo, this.mPreferenceMap.get(mClicked));
    }
    
    private NetworkInfoWithAcT createNetworkInfo(Intent intent) {
        String numberName = intent.getStringExtra(NetworkEditor.PLMN_CODE);
        String operatorName = intent.getStringExtra(NetworkEditor.PLMN_NAME);
        int priority = intent.getIntExtra(NetworkEditor.PLMN_PRIORITY, 0);
        int act = intent.getIntExtra(NetworkEditor.PLMN_SERVICE, 0);
        return new NetworkInfoWithAcT(operatorName, numberName, act, priority);
    }
    
    private class PLMNPreference extends Preference {
        public PLMNPreference(Context context) {
            super(context);
            // TODO Auto-generated constructor stub
        }
    }
    
    void handleSetPLMN(ArrayList<NetworkInfoWithAcT> list) {
        numbers = list.size();
        onStarted(this.mPLMNListContainer, false);
        for (int i = 0; i < list.size(); i++) {
            NetworkInfoWithAcT ni = list.get(i);
            if (CallSettings.isMultipleSim()) {
                GeminiPhone dualPhone = (GeminiPhone)mPhone;
                dualPhone.setPOLEntryGemini(mSlotId, ni,
                        mHandler.obtainMessage(MyHandler.MESSAGE_SET_PLMN_LIST, mSlotId, MyHandler.MESSAGE_SET_PLMN_LIST));
            } else {
                mPhone.setPOLEntry(ni,
                        mHandler.obtainMessage(MyHandler.MESSAGE_SET_PLMN_LIST, mSlotId, MyHandler.MESSAGE_SET_PLMN_LIST));
                if (DBG) {
                    Xlog.d(LOG_TAG, "handleSetPLMN: " + ni.toString());
                }
            }
        }
    }
    
    void handlePLMNListChange(NetworkInfoWithAcT newInfo, NetworkInfoWithAcT oldInfo) {
        
        if (newInfo.getPriority() == -1) {
            //delete this network info
            handleSetPLMN(genDelete(oldInfo));
            
        } else {
            handleSetPLMN(genModifyEx(newInfo, oldInfo));
        }
    }
    
    void handlePLMNListAdd(NetworkInfoWithAcT newInfo) {
        Xlog.d(LOG_TAG, "handlePLMNListAdd: add new network: " + newInfo);
        dumpNetworkInfo(mPLMNList);
        ArrayList<NetworkInfoWithAcT> list = new ArrayList<NetworkInfoWithAcT>();
        for (int i = 0; i < mPLMNList.size(); i++) {
            list.add(mPLMNList.get(i));
        }
        NetworkCompare nc = new NetworkCompare();
        int pos = Collections.binarySearch(mPLMNList, newInfo, nc);
        
        int properPos = -1;
        if (pos < 0) {
            properPos = getPosition(mPLMNList, newInfo);
        }
        if (properPos == -1) {
            list.add(pos, newInfo);
        } else {
            list.add(properPos, newInfo);
        }
        this.adjustPriority(list);
        this.dumpNetworkInfo(list);
        this.handleSetPLMN(list);
    }
    
    private void dumpNetworkInfo(List<NetworkInfoWithAcT> list) {
        if (!DBG) return;
        
        Xlog.d(LOG_TAG, "dumpNetworkInfo : **********start*******");
        for (int i = 0; i < list.size(); i++) {
            Xlog.d(LOG_TAG, "dumpNetworkInfo : " + list.get(i).toString());
        }
        Xlog.d(LOG_TAG, "dumpNetworkInfo : ***********stop*******");
    }
    
    ArrayList<NetworkInfoWithAcT> genModifyEx(NetworkInfoWithAcT newInfo, NetworkInfoWithAcT oldInfo) {
        Xlog.d(LOG_TAG, "genModifyEx: change : " + oldInfo.toString() + "----> " + newInfo.toString());
        dumpNetworkInfo(mPLMNList);
        
        NetworkCompare nc = new NetworkCompare();
        int oldPos = Collections.binarySearch(mPLMNList, oldInfo, nc);
        int newPos = Collections.binarySearch(mPLMNList, newInfo, nc);
        
        ArrayList<NetworkInfoWithAcT> list = new ArrayList<NetworkInfoWithAcT>();
        /*for (int i = 0; i < mPLMNList.size(); i++) {
            list.add(mPLMNList.get(i));
        }*/
        //no index change, only set the name and act
        if (newInfo.getPriority() == oldInfo.getPriority()) {
            list.add(newInfo);
            dumpNetworkInfo(list);
            return list;
        }
        
        for (int i = 0; i < mPLMNList.size(); i++) {
            list.add(mPLMNList.get(i));
        }
        
        //there is no network info at the expected position
        int properPos = -1;
        if (newPos < 0) {
            properPos = getPosition(mPLMNList, newInfo);
            list.add(properPos, newInfo);
            dumpNetworkInfo(list);
            return list;
        }
        
        //found
        int adjustIndex = newPos;
        if (oldPos > newPos) {
            list.remove(oldPos);
            list.add(newPos, newInfo);
        } else if (oldPos < newPos){
            list.add(newPos + 1, newInfo);
            list.remove(oldPos);
            adjustIndex -= 1;
        } else {
            list.remove(oldPos);
            list.add(oldPos, newInfo);
        }
        
        this.adjustPriority(list);
        dumpNetworkInfo(list);
        return list;
    }
    
    int getPosition(List<NetworkInfoWithAcT> list, NetworkInfoWithAcT newInfo) {
        int index = -1;
      //No priority element found
        if (list == null || list.size() == 0) {
            return 0;
        }
        
        if (list.size() == 1) {
            return list.get(0).getPriority() > newInfo.getPriority() ? 0 : 1;
        }
        
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getPriority() > newInfo.getPriority()) {
                if (i == 0) {
                    index = 0;
                } else {
                    index = i -1;
                }
            }
            break;
        }
        //Add this to the last
        if (index == -1) {
            index = list.size();
        }
        return index;
    }
    
    private void adjustPriority(ArrayList<NetworkInfoWithAcT> list) {
        int priority = 0;
        for (NetworkInfoWithAcT info : list) {
            info.setPriority(priority++);
        }
    }
    
    ArrayList<NetworkInfoWithAcT> genDelete(NetworkInfoWithAcT network) {
        Xlog.d(LOG_TAG, "genDelete : " + network.toString());
        dumpNetworkInfo(mPLMNList);
        
        ArrayList<NetworkInfoWithAcT> list = new ArrayList<NetworkInfoWithAcT>();
        NetworkCompare nc = new NetworkCompare();
        int pos = Collections.binarySearch(mPLMNList, network, nc);
        
        //the list before the delete, don't change
        for (int i = 0; i < mPLMNList.size(); i++) {
            list.add(mPLMNList.get(i));
        }
        
        list.remove(pos);
        network.setOperatorNumeric(null);
        list.add(network);
        
        //make sure after this have been removed
        for (int i = list.size(); i < this.mCapability.lastIndex + 1; i++) {
            //network.setPriority(i);
            NetworkInfoWithAcT ni = new NetworkInfoWithAcT("", null, 1, i);
            list.add(ni);
        }
        this.adjustPriority(list);
        dumpNetworkInfo(list);
        
        /*this.startIndex = pos;
        this.stopIndex = mPLMNList.size() - 1;
        NetworkInfoWithAcT ni = mPLMNList.get(startIndex);
        ni.setOperatorNumeric(null);
        //add the network which will be deleted with numberic == null
        list.add(ni);*/
        
        /*NetworkInfoWithAcT infoTemp;
        for (int i = pos + 1; i < mPLMNList.size(); i++) {
            infoTemp = mPLMNList.get(i);
            infoTemp.setPriority(infoTemp.getPriority() - 1);
            list.add(infoTemp);
        }*/
        return list;
    }
    
    private void initStringList() {
        listPriority.clear();
        listService.clear();
        int maxPriority = 0;
        if ((mPLMNList != null) && (mPLMNList.size() != 0)) {
            NetworkInfoWithAcT info = mPLMNList.get(mPLMNList.size()-1);
            maxPriority = info.getPriority() + 1;
        } else {
            maxPriority = 1;
        }
        for (int i = 0; i <= maxPriority; i++) {
            listPriority.add(String.valueOf(i));
        }
        
        listService.add("GSM");
        listService.add("TD-SCDMA");
        listService.add("Dual mode");
    }
    
    private void showInputDialog(String pinRetryLeft) {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View dialogView = inflater.inflate(R.layout.edit_plmn_info,
                null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        //builder.setTitle(R.string.max_call_cost_set);
        initStringList();
        
        final Spinner spPriority = (Spinner)dialogView.findViewById(R.id.sp_priority);
        final Spinner spService = (Spinner)dialogView.findViewById(R.id.sp_service);

        ArrayAdapter<String> adapterPriority = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, this.listPriority);
        ArrayAdapter<String> adapterService = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, this.listService);
        adapterPriority.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapterService.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        spPriority.setAdapter(adapterPriority);
        spService.setAdapter(adapterService);
        
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        EditText mccmnc = (EditText) dialogView.findViewById(R.id.mccmnc);
                        int priority = spPriority.getSelectedItemPosition();
                        int service = spService.getSelectedItemPosition();
                        if (mccmnc == null || mccmnc.length() == 0) {
                            Toast.makeText(PhoneApp.getInstance(), R.string.plmn_mcc_mnc_error, Toast.LENGTH_SHORT)
                            .show();
                            return ;
                        }
                        NetworkInfoWithAcT info = new NetworkInfoWithAcT("", mccmnc.getText().toString(), NetworkEditor.covertApNW2Ril(service), priority);
                        handlePLMNListAdd(info);
                        }
                });
        AlertDialog dialog=builder.create();
        Window window = dialog.getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE |
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.show();
    }
    
    private class MyHandler extends Handler {
        private static final int MESSAGE_GET_PLMN_LIST = 0;
        private static final int MESSAGE_SET_PLMN_LIST = 1;
        private static final int MESSAGE_GET_PLMN_CAPIBILITY = 2;
        
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_PLMN_LIST:
                    handleGetPLMNResponse(msg);
                    break;
                case MESSAGE_SET_PLMN_LIST:
                    handleSetPLMNResponse(msg);
                    break;
                    
                case MESSAGE_GET_PLMN_CAPIBILITY:
                    handleGetPLMNCapibilityResponse(msg);
                    break;
            }
        }
        
        @SuppressWarnings("unchecked")
        public void handleGetPLMNResponse(Message msg) {
            if (DBG) Xlog.d(LOG_TAG, "handleGetPLMNResponse: done");
            
            if (msg.arg2 == MyHandler.MESSAGE_GET_PLMN_LIST) {
                onFinished(mPLMNListContainer, true);
            } else {
                onFinished(mPLMNListContainer, false);
            }
            
            AsyncResult ar = (AsyncResult) msg.obj;
            boolean isUserException = false;
            if (ar.exception != null) {
                Xlog.d(LOG_TAG, "handleGetPLMNResponse with exception = " + ar.exception);
                if (mPLMNList == null) {
                    //avoid the exception because we no chance to update the mPLMNList when first arrive here.
                    mPLMNList = new ArrayList<NetworkInfoWithAcT>();
                }
            } else {
                refreshPreference((ArrayList<NetworkInfoWithAcT>)ar.result);
            }
        }
        
        public void handleSetPLMNResponse(Message msg) {
            if (DBG) Xlog.d(LOG_TAG, "handleSetPLMNResponse: done");
            numbers --;
            /*if (numbers == 0) {
                onFinished(mPLMNListContainer, false);
            }*/
            
            AsyncResult ar = (AsyncResult) msg.obj;
            boolean isUserException = false;
            if (ar.exception != null) {
                Xlog.d(LOG_TAG, "handleSetPLMNResponse with exception = " + ar.exception);
            } else {
                //refreshPreference((ArrayList<NetworkInfoWithAcT>)ar.result);
                if (DBG) Xlog.d(LOG_TAG, "handleSetPLMNResponse: with OK result!");
            }
            
            if (numbers == 0) {
                //init(PLMNListPreference.this, false, mSlotId);
                if (CallSettings.isMultipleSim()) {
                    
                } else {
                    mPhone.getPreferedOperatorList(mHandler.obtainMessage(MyHandler.MESSAGE_GET_PLMN_LIST, mSlotId, MyHandler.MESSAGE_SET_PLMN_LIST));
                }
            }
        }
        
        public void handleGetPLMNCapibilityResponse(Message msg) {
            if (DBG) Xlog.d(LOG_TAG, "handleGetPLMNCapibilityResponse: done");
            
            AsyncResult ar = (AsyncResult) msg.obj;
            
            if (ar.exception != null) {
                Xlog.d(LOG_TAG, "handleGetPLMNCapibilityResponse with exception = " + ar.exception);
            } else {
                mCapability.setCapability((int[])ar.result);
            }
        }
    }
    
    private class SIMCapability {
        int firstIndex;
        int lastIndex;
        int firstFormat;
        int lastFormat;
        
        public SIMCapability(int startIndex, int stopIndex, int startFormat, int stopFormat) {
            firstIndex = startIndex;
            lastIndex = stopIndex;
            firstFormat = startFormat;
            lastFormat = stopFormat;
        }
        
        public void setCapability(int r[]) {
            if (r.length < 4) {
                return;
            }
            
            firstIndex = r[0];
            lastIndex = r[1];
            firstFormat = r[2];
            lastFormat = r[3];
            
        }
    }
}