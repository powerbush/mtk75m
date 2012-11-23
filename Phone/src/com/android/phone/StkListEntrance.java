package com.android.phone;



import java.util.ArrayList;

import java.util.List;



import android.app.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;


import android.preference.Preference;
import android.preference.PreferenceActivity;

import android.preference.PreferenceScreen;



import android.provider.Telephony.SIMInfo;
import com.mediatek.telephony.TelephonyManagerEx;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.CellConnService.CellConnMgr;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import android.os.SystemProperties;
import android.provider.Settings;







public class StkListEntrance extends PreferenceActivity {


	
    private static final String TAG = "StkListEntrance";
    
    private static String strTargetLoc = null;
    private static String strTargetClass = null;
	private static final int REQUEST_TYPE = 302;


    private Context mContext;
    private IntentFilter mIntentFilter;
    
    private int mTargetClassIndex = -1;
    private TelephonyManagerEx mTelephonyManager;
    


    private static final String baseband =  SystemProperties.get("gsm.baseband.capability");
    
    public static int mSlot = -1;
    
    private static String mDefaultTitle;
    
 
	private BroadcastReceiver mSimReceiver = new BroadcastReceiver() {
		
        @Override
        public void onReceive(Context context, Intent intent) {
        	
            String action = intent.getAction();
            
            if (action.equals(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED)) {
            	
                int slotId = intent.getIntExtra(TelephonyIntents.INTENT_KEY_ICC_SLOT, -1);
                int simStatus = intent.getIntExtra(TelephonyIntents.INTENT_KEY_ICC_STATE, -1);
            	Log.i(TAG, "receive notification of  sim slot = "+slotId+" status = "+simStatus);
                
                if ((slotId>=0)&&(simStatus>=0)) {
                	updateSimState(slotId,simStatus);               	
                }
            	
            } else if(action.equals(TelephonyIntents.ACTION_RADIO_OFF)){
            	Settings.System.putLong(getApplicationContext().getContentResolver(), Settings.System.SIM_LOCK_STATE_SETTING, 0x0L);
            	Log.i(TAG,"MODEM RESET");
            	finish();       	
            }
        }
	};

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "[onCreate] +");
        mCellMgr.register(this);
        mContext = this;
        
        
        
        addPreferencesFromResource(R.xml.stk_sim_indicate);
        mTelephonyManager = TelephonyManagerEx.getDefault();
        
        Log.i(TAG, "baseband is "+baseband);
        //if((baseband != null)&&(baseband.length()!=0)&&(Integer.parseInt(baseband)<=3)) {
        //MTK_OP02_PROTECT_START
        if(PhoneUtils.getOptrProperties().equals("OP02") ) {
            Utils.mSupport3G = true;
        } 
        //MTK_OP02_PROTECT_END
        mIntentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_RADIO_OFF);
		registerReceiver(mSimReceiver, mIntentFilter);
		
        setTitle(getResources().getString(R.string.sim_toolkit));
        
        Log.i(TAG, "[onCreate][addSimInfoPreference] ");
        addSimInfoPreference();

         
    }

    

	

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(mSimReceiver);
		mCellMgr.unregister();
	}





	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Log.i(TAG, "Enter onPreferenceClick function.");
        // TODO Auto-generated method stub
        strTargetLoc = null;
        strTargetClass = null;
        Log.i(TAG, "[onPreferenceTreeClick] +");
    	GeminiPhone mGeminiPhone = (GeminiPhone)PhoneFactory.getDefaultPhone();
    	PackageManager pm = getApplicationContext().getPackageManager();
        Log.i(TAG, "[onPreferenceTreeClick][Click SIM1][SimState] : " + mGeminiPhone.getIccCardGemini(Phone.GEMINI_SIM_1).getState());
        Log.i(TAG, "[onPreferenceTreeClick][Click SIM2][SimState] : " + mGeminiPhone.getIccCardGemini(Phone.GEMINI_SIM_2).getState());
            
            SIMInfo siminfo1 = SIMInfo.getSIMInfoById(this, Integer.valueOf(preference.getKey()));
            if (siminfo1 != null){
		        mSlot = siminfo1.mSlot;
            }
            Log.i(TAG, "[aaa][onPreferenceTreeClick][mSlot] : " + mSlot);
            if (mSlot == 0){            	
				ComponentName cName1 = new ComponentName("com.android.stk",
		           "com.android.stk.StkLauncherActivity");
				if(mGeminiPhone.getIccCardGemini(Phone.GEMINI_SIM_1).getState() != IccCard.State.READY){
					//call Zhiwei
					int nRet1 = mCellMgr.handleCellConn(mSlot, REQUEST_TYPE);
				}else if(pm.getComponentEnabledSetting(cName1) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED){
		        	showTextToast(getString(R.string.activity_not_found));
		        	finish();
		        	return false;
		        }else{
			   		strTargetLoc = "com.android.stk";
			   		strTargetClass = "com.android.stk.StkLauncherActivity"; 
		        }

            }else if(mSlot == 1){
		        ComponentName cName2 = new ComponentName("com.android.stk2",
		           "com.android.stk2.StkLauncherActivity");
				if(mGeminiPhone.getIccCardGemini(Phone.GEMINI_SIM_2).getState() != IccCard.State.READY){
					//call Zhiwei
					int nRet2 = mCellMgr.handleCellConn(mSlot, REQUEST_TYPE);
				}else if(pm.getComponentEnabledSetting(cName2) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED){
		        	showTextToast(getString(R.string.activity_not_found));
		        	finish();
		        	return false;
		        }else{
			   		strTargetLoc = "com.android.stk2";
			   		strTargetClass = "com.android.stk2.StkLauncherActivity"; 
		        }        	
            }
            
           
        
        if(strTargetLoc != null){
        	Intent intent = new Intent();
        	Log.i(TAG, "[aaa][onPreferenceTreeClick][mSlot][strTargetLoc] : " + strTargetLoc);
            intent.setClassName(strTargetLoc, strTargetClass);
            startActivity(intent);
        }
        
        
        Log.i(TAG, "[onPreferenceTreeClick] -");        
        return false;
    }
    

    
	private void addSimInfoPreference() {
		Log.i(TAG, "[addSimInfoPreference]+");
		PreferenceScreen root = getPreferenceScreen();

		if (root != null) {
			List<SIMInfo> simList = SIMInfo.getInsertedSIMList(this);

			for (SIMInfo siminfo : simList) {
	        	int status = mTelephonyManager.getSimIndicatorStateGemini(siminfo.mSlot);
	        	Log.i(TAG, "sim status of slot "+ siminfo.mSlot+ "is "+status);
	        	Log.i(TAG, "[addSimInfoPreference][siminfo.mSimId] :"+siminfo.mSimId);
				SimInfoPreference simInfoPref = new SimInfoPreference(this, siminfo.mDisplayName,
	        			siminfo.mNumber, siminfo.mSlot, status, siminfo.mColor, 
	        			siminfo.mDispalyNumberFormat, siminfo.mSimId, false);

				Log.i(TAG, "[addSimInfoPreference][addPreference]");
				root.addPreference(simInfoPref);
			}

		}

	}

	//deal with SIM status
	private Runnable serviceComplete = new Runnable() {
		public void run() {
			Log.d(TAG, "serviceComplete run");			
			int nRet = mCellMgr.getResult();
			Log.d(TAG, "serviceComplete result = " + CellConnMgr.resultToString(nRet));
			if (mCellMgr.RESULT_ABORT == nRet) {
				//finish();
				return;
			} else {
		        return;
			}
		}
	};

	private CellConnMgr mCellMgr = new CellConnMgr(serviceComplete);

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	    
	}





	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		strTargetLoc = null;
		strTargetClass = null;
		Log.i(TAG, "[onResume]");		 
	    
	    setDefaultSIMIndicate(0);
	    setDefaultSIMIndicate(1);
	}
	
    
    private void updateSimState(int slotID, int state) {
    	Log.i(TAG, "[updateSimState]+");
    	SIMInfo siminfo = SIMInfo.getSIMInfoBySlot(this, slotID);
    	
    	if (siminfo != null) {
    		Log.i(TAG, "[updateSimState][siminfo.mSimId] : " + siminfo.mSimId);
    		SimInfoPreference pref = (SimInfoPreference)findPreference(String.valueOf(siminfo.mSimId));
    		Log.i(TAG, "[updateSimState][setStatus] ");
    		if (pref != null){
    			pref.setStatus(state);
    		}
        	Log.i(TAG, "updateSimState sim = "+siminfo.mSimId+" status = "+state);

    	}
    }

    private void showTextToast(String msg) {
        Toast toast = Toast.makeText(getApplicationContext(), msg,
                Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }
    
    private void setDefaultSIMIndicate(int slotID){
    	Log.i(TAG, "[getSIMState][slotID] : " + slotID);
    	int state = mTelephonyManager.getSimIndicatorStateGemini(slotID);
    	Log.i(TAG, "[getSIMState][state] : " + state);
    	updateSimState(slotID, state);
    }
 

}