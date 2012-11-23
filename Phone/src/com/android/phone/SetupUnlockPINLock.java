/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */





package com.android.phone;

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   SetupUnlockPINLock.java
//   To setup unlock PIN process,mainly query current lock status to decide call UnlockPINLock.java or UnlockPUKLock.java
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


import android.app.Activity;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;


import com.mediatek.featureoption.FeatureOption;
import android.telephony.TelephonyManager;//To find the SIM card Ready State
import android.text.Editable;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.AsyncResult;
//import android.provider.ContactsContract.CommonDataKinds.Phone;
//import com.android.internal.R;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.IccCard;
import android.view.KeyEvent;
import android.view.WindowManager;



public class SetupUnlockPINLock extends Activity
{
    public static final String LOGTAG = "SetupUnlockPINLock ";
    // intent action for launching emergency dialer activity.
    static final String ACTION_EMERGENCY_DIAL = "com.android.phone.EmergencyDialer.DIAL";
    

    public static  String LOCKCATEGORY = "LockCategory";
    private static int lockCategory = -1;
    //public static int iSIMMEUnlockNo = -1;
    public static int iSIMMEUnlockNo = 0;// To unlock SIM1

    
    private static boolean result = false;


	public int PwdLength = -1;
	public int mPwdLeftChances = -1;//unlock retries left times
	public ProgressDialog progressDialog;
	


    private static final int GET_SIM_RETRY_EMPTY = -1;	
	
    
    protected void onCreate(Bundle savedInstanceState) {
    	Log.d(LOGTAG, "[onCreate]+");
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);//Set this activity no title display
        
        Bundle bundle = this.getIntent().getExtras();//get the current Lock Status: 
        if(bundle != null){
        	Log.d(LOGTAG, "[onCreate][iSIMMEUnlockNo]: "+iSIMMEUnlockNo);
        	iSIMMEUnlockNo = bundle.getInt("Phone.GEMINI_SIM_ID_KEY",-1);
        }

        
        Log.d(LOGTAG, "[unlock][onCreate]-");
    }//onCreat End

    

		  


@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}


	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		Log.d(LOGTAG, "[onResume]+");
		//Refresh Left Retries
		super.onResume();
		   if(false == FeatureOption.MTK_GEMINI_SUPPORT)
           {
			   Log.d(LOGTAG, "[onResume][Single Card]");
               //Single Card:
			   Phone phone = PhoneFactory.getDefaultPhone();
			   if(phone.getIccCard().getState() == IccCard.State.PIN_REQUIRED){
				   Log.d(LOGTAG, "[onResume][Single Card][PIN_REQUIRED]");
           	    //Query retries time left
           		int retryCount = getRetryPinCount(Phone.GEMINI_SIM_1);
           		Log.d(LOGTAG, "[onResume][Single Card][PIN Retries Left] : " + retryCount);
       			if (retryCount > 0){
       				//Go to UnlockPINLock.java with the left retries
       				Intent intent = new Intent(SetupUnlockPINLock.this, UnlockPINLock.class);  
            		Bundle bundle = new Bundle();
            		bundle.putInt("PINLEFTRETRIES", retryCount);
            		bundle.putInt("Phone.GEMINI_SIM_ID_KEY", iSIMMEUnlockNo);
            		intent.putExtras(bundle);
            		startActivity(intent);
            		SetupUnlockPINLock.this.finish();
            		return;
       				//result = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).supplyPin(strPwd);
       			}else{
       			    //no retries to unlock PIN
       				//Go to UnlockPUKLock.java
       				Intent intent = new Intent(SetupUnlockPINLock.this, UnlockPUKLock.class);  
            		Bundle bundle = new Bundle();
            		int retryPUKCount = getRetryPukCount(Phone.GEMINI_SIM_1);
            		bundle.putString("PUKPHASE", "1");//first step to unlock PUK
            		bundle.putInt("PUKLEFTRETRIES", retryPUKCount);
            		bundle.putInt("Phone.GEMINI_SIM_ID_KEY", iSIMMEUnlockNo);
            		intent.putExtras(bundle);
            		startActivity(intent);
            		SetupUnlockPINLock.this.finish();
            		return;
       			}
			   }else if(phone.getIccCard().getState() == IccCard.State.PUK_REQUIRED){//Need to unlock PUK
				    Log.d(LOGTAG, "[onResume][Single Card][PUK_REQUIRED]");
	           		int retryPUKCount = getRetryPukCount(Phone.GEMINI_SIM_1);
	           		Log.d(LOGTAG, "[onResume][Single Card][PUK Retries Left] : " + retryPUKCount);
       				//Go to UnlockPUKLock.java
       				Intent intent = new Intent(SetupUnlockPINLock.this, UnlockPUKLock.class);  
            		Bundle bundle = new Bundle();
            		bundle.putString("PUKPHASE", "1");//first step to unlock PUK
            		bundle.putInt("PUKLEFTRETRIES", retryPUKCount);
            		bundle.putInt("Phone.GEMINI_SIM_ID_KEY", iSIMMEUnlockNo);
            		intent.putExtras(bundle);
            		startActivity(intent);
            		SetupUnlockPINLock.this.finish();
            		return;
			   }else{
				   SetupUnlockPINLock.this.finish();
				   return;
			   }
           }
           else
           {
           	Log.d(LOGTAG, "[onResume][GEMINI Card]" );
           	GeminiPhone mGeminiPhone = (GeminiPhone)PhoneFactory.getDefaultPhone();
           	if (iSIMMEUnlockNo == 0)
           	{
               	Log.d(LOGTAG, "[onResume][GEMINI Card][SIM1]" );
           		if (mGeminiPhone.getIccCardGemini(Phone.GEMINI_SIM_1).getState() == IccCard.State.PIN_REQUIRED){
           			Log.d(LOGTAG, "[onResume][GEMINI Card][SIM1][PIN_REQUIRED]" );
           			//card1 need to unlock PIN
           		int retryCount = getRetryPinCount(Phone.GEMINI_SIM_1);
           			Log.d(LOGTAG, "[onResume][GEMINI Card][SIM1][PIN Retries Left] : " + retryCount);
       			if (retryCount > 0){
       				//Go to UnlockPINLock.java with the left retries
       				Intent intent = new Intent(SetupUnlockPINLock.this, UnlockPINLock.class);  
            		Bundle bundle = new Bundle();
            		bundle.putInt("PINLEFTRETRIES", retryCount);
            		bundle.putInt("Phone.GEMINI_SIM_ID_KEY", iSIMMEUnlockNo);
            		intent.putExtras(bundle);
            		startActivity(intent);
            		SetupUnlockPINLock.this.finish();
            		return;
       				//result = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).supplyPin(strPwd);
       			}else{
       			    //no retries to unlock PIN
       				//Go to UnlockPUKLock.java
       				Intent intent = new Intent(SetupUnlockPINLock.this, UnlockPUKLock.class);  
            		Bundle bundle = new Bundle();
            		int retryPUKCount = getRetryPukCount(Phone.GEMINI_SIM_1);
            		bundle.putString("PUKPHASE", "1");//first step to unlock PUK
            		bundle.putInt("PUKLEFTRETRIES", retryPUKCount);
            		bundle.putInt("Phone.GEMINI_SIM_ID_KEY", iSIMMEUnlockNo);
            		intent.putExtras(bundle);
            		startActivity(intent);
            		SetupUnlockPINLock.this.finish();
            		return;
       			}         		
          	     }else if (mGeminiPhone.getIccCardGemini(Phone.GEMINI_SIM_1).getState() == IccCard.State.PUK_REQUIRED){
          	    	Log.d(LOGTAG, "[onResume][GEMINI Card][SIM1][PUK_REQUIRED]" );
          	    	int retryPUKCount = getRetryPukCount(Phone.GEMINI_SIM_1);
          	    	Log.d(LOGTAG, "[onResume][GEMINI Card][SIM1][PUK Retries Left] : " + retryPUKCount);
       				//Go to UnlockPUKLock.java
       				Intent intent = new Intent(SetupUnlockPINLock.this, UnlockPUKLock.class);  
            		Bundle bundle = new Bundle();
            		bundle.putString("PUKPHASE", "1");//first step to unlock PUK
            		bundle.putInt("PUKLEFTRETRIES", retryPUKCount);
            		bundle.putInt("Phone.GEMINI_SIM_ID_KEY", iSIMMEUnlockNo);
            		intent.putExtras(bundle);
            		startActivity(intent);
            		SetupUnlockPINLock.this.finish();
            		return;
          	     }else{
          	    	 SetupUnlockPINLock.this.finish();
          	    	 return;
          	     }
           		
    		}   
    		else//GEMINI SIM2
    		{
    			Log.d(LOGTAG, "[onResume][GEMINI Card][SIM2]" );
           		if (mGeminiPhone.getIccCardGemini(Phone.GEMINI_SIM_2).getState() == IccCard.State.PIN_REQUIRED){
         	    	 //card1 need to unlock PIN
           			Log.d(LOGTAG, "[onResume][GEMINI Card][SIM2][PIN_REQUIRED]" );
          		int retryCount = getRetryPinCount(Phone.GEMINI_SIM_2);
          		Log.d(LOGTAG, "[onResume][GEMINI Card][SIM2][PIN Retries Left] : " + retryCount);
      			if (retryCount > 0){
      				//Go to UnlockPINLock.java with the left retries
      				Intent intent = new Intent(SetupUnlockPINLock.this, UnlockPINLock.class);  
            		Bundle bundle = new Bundle();
            		bundle.putInt("PINLEFTRETRIES", retryCount);
            		bundle.putInt("Phone.GEMINI_SIM_ID_KEY", iSIMMEUnlockNo);
            		intent.putExtras(bundle);
            		startActivity(intent);
            		SetupUnlockPINLock.this.finish();
            		return;
      				//result = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).supplyPin(strPwd);
      			}else{
      			    //no retries to unlock PIN
      				//Go to UnlockPUKLock.java
      				Intent intent = new Intent(SetupUnlockPINLock.this, UnlockPUKLock.class);  
            		Bundle bundle = new Bundle();
            		int retryPUKCount = getRetryPukCount(Phone.GEMINI_SIM_2);
            		bundle.putString("PUKPHASE", "1");//first step to unlock PUK
            		bundle.putInt("PUKLEFTRETRIES", retryPUKCount);
            		bundle.putInt("Phone.GEMINI_SIM_ID_KEY", iSIMMEUnlockNo);
            		intent.putExtras(bundle);
            		startActivity(intent);
            		SetupUnlockPINLock.this.finish();
            		return;
      			}}else if (mGeminiPhone.getIccCardGemini(Phone.GEMINI_SIM_2).getState() == IccCard.State.PUK_REQUIRED){
      				Log.d(LOGTAG, "[onResume][GEMINI Card][SIM2][PUK_REQUIRED]" );
      				int retryPUKCount = getRetryPukCount(Phone.GEMINI_SIM_2);
	           		Log.d(LOGTAG, "[onResume][GEMINI Card][SIM2][PUK Retries Left] : " + retryPUKCount);
       				//Go to UnlockPUKLock.java
       				Intent intent = new Intent(SetupUnlockPINLock.this, UnlockPUKLock.class);  
            		Bundle bundle = new Bundle();
            		bundle.putString("PUKPHASE", "1");//first step to unlock PUK
            		bundle.putInt("PUKLEFTRETRIES", retryPUKCount);
            		bundle.putInt("Phone.GEMINI_SIM_ID_KEY", iSIMMEUnlockNo);
            		intent.putExtras(bundle);
            		startActivity(intent);
            		SetupUnlockPINLock.this.finish();
            		return;
         	     }else{
          	    	 SetupUnlockPINLock.this.finish();
          	    	 return;
          	     }
    		}
       	   
           }
        
		
	}


//public void onAttachedToWindow(){
//	this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD);
//	super.onAttachedToWindow();
//}
	static final int VERIFY_TYPE_PIN = 501;
	static final int VERIFY_TYPE_PUK = 502;
	static final int VERIFY_TYPE_SIMMELOCK = 503;
	static final int VERIFY_TYPE_PIN2 = 504;
	static final int VERIFY_TYPE_PUK2 = 505;

	static final String VERIFY_TYPE = "verfiy_type";
	static final String VERIFY_RESULT = "verfiy_result";
	public static final String START_TYPE = "start_type";
	public static final String START_TYPE_REQ = "request";
	public static final String START_TYPE_RSP = "response";

	public void sendVerifyResult(int verifyType, boolean bRet) {
		Log.d(LOGTAG, "sendVerifyResult verifyType = " + verifyType
				+ " bRet = " + bRet);
		Intent retIntent = new Intent(
				"android.intent.action.CELLCONNSERVICE").putExtra(
				START_TYPE, START_TYPE_RSP);

		if (null == retIntent) {
			Log.e(LOGTAG, "sendVerifyResult new retIntent failed");
			return;
		}
		retIntent.putExtra(VERIFY_TYPE, verifyType);

		retIntent.putExtra(VERIFY_RESULT, bRet);

		startService(retIntent);
	}    


private int getRetryPukCount(final int simId) {
	if (simId == Phone.GEMINI_SIM_2)
		return SystemProperties.getInt("gsm.sim.retry.puk1.2",GET_SIM_RETRY_EMPTY);
	else
		return SystemProperties.getInt("gsm.sim.retry.puk1",GET_SIM_RETRY_EMPTY);
	}
private int getRetryPinCount(final int simId) {
	if (simId == Phone.GEMINI_SIM_2)
		return SystemProperties.getInt("gsm.sim.retry.pin1.2",GET_SIM_RETRY_EMPTY);
	else
		return SystemProperties.getInt("gsm.sim.retry.pin1",GET_SIM_RETRY_EMPTY);
	}

private final BroadcastReceiver mReceiver = new  BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) {
    	Log.d(LOGTAG, "[BroadcastReceiver][onReceiver]+" );
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
        	Log.d(LOGTAG, "[BroadcastReceiver][onReceiver][ACTION_AIRPLANE_MODE_CHANGED]" );
            finish();
        }
        Log.d(LOGTAG, "[BroadcastReceiver][onReceiver]-" );
    }
};

}//Main Class End

