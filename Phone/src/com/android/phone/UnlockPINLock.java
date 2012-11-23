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



import android.app.Activity;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;

import com.mediatek.featureoption.FeatureOption;
import android.telephony.TelephonyManager;//To find the SIM card Ready State
import android.text.Editable;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.AsyncResult;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.IccCard;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.provider.Telephony.SIMInfo;

import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.method.DigitsKeyListener;
import android.view.ViewRoot;



public class UnlockPINLock extends Activity
{
    public static final String LOGTAG = "UnlockPINLock ";
    // intent action for launching emergency dialer activity.
    static final String ACTION_EMERGENCY_DIAL = "com.android.phone.EmergencyDialer.DIAL";
    
    public static int iSIMMEUnlockNo = -1;
    public int retryCount  =  -1; //PIN retries left

    
    private static boolean result = false;

	private TextView mDismissButton;
	private TextView mUnlockResultNotify;
	private TextView mUnlockActionNotify;
	private TextView mUnlockRetriesNotify;
	private TextView mUnlockForSIMNotify;
	private TextView mUnlockSIMNameNotify;
	private TextView mUnlockSIMCategoryNotify;
	private ImageButton mbackspace;
	private Button mbtnEmergencyCall;
	public  TextView mPwdDisplay = null;
	private TextView mUnlockEmptyForSingleCard = null;
	public int PwdLength = -1;
	public static String strPwd = null; 
	public String tempstrPwd = null;
	public int mPwdLeftChances = -1;//unlock retries left times
	public ProgressDialog progressDialog;
	
    // size limits for the pin.
    private static final int MIN_PIN_LENGTH = 4;
    private static final int MAX_PIN_LENGTH = 8;

    private static final int GET_SIM_RETRY_EMPTY = -1;
    private static final int MSG1 = 100;
    private static final int MSG2 = 102;
    private static final int MSG3 = 104;
    private static final int MSG4 = 106;
	
	
    private static final int DIALOG_ENTERNUMBER = 120;
    
    public static String strWrongCode = null;
    
    protected void onCreate(Bundle savedInstanceState) {
    	Log.d(LOGTAG, "[onCreate]+");
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);//Set this activity no title display
        setContentView(R.layout.unlocksimlock);
        mPwdDisplay = (TextView) findViewById(R.id.pwdDisplay);
        //mPwdDisplay.setInputType(InputType.TYPE_NULL);
        //mPwdDisplay.setTransformationMethod(PasswordTransformationMethod.getInstance());
        mPwdDisplay.setInputType(InputType.TYPE_CLASS_NUMBER |InputType.TYPE_TEXT_VARIATION_PASSWORD );
        mPwdDisplay.setKeyListener(DigitsKeyListener.getInstance());
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        mbackspace = (ImageButton) findViewById(R.id.backspace);
        mbtnEmergencyCall = (Button) findViewById(R.id.emergencyCall);
        mbtnEmergencyCall.setText(this.getResources().getString(R.string.simlock_emergency));
        
	    Bundle bundle = this.getIntent().getExtras();//get the current SIM Number which need to unlock 
	    if(bundle != null){
	    	Log.d(LOGTAG, "[onCreate][iSIMMEUnlockNo]: "+iSIMMEUnlockNo);
	    	iSIMMEUnlockNo = bundle.getInt("Phone.GEMINI_SIM_ID_KEY",-1);
	    	retryCount = bundle.getInt("PINLEFTRETRIES", -1);
	    	strWrongCode = bundle.getString("WRONGCODE");
	    }
	    Log.d(LOGTAG, "[onCreate][strWrongCode]" + strWrongCode);
        mUnlockResultNotify = (TextView) findViewById(R.id.unlockResultNotify);        
        if ((strWrongCode != null) && (strWrongCode.equals(getString(R.string.simlock_wrongcode)))){
        	 Log.d(LOGTAG, "[onCreate][Set Wrong code String]" + getString(R.string.simlock_wrongcode));
        	mUnlockResultNotify.setText(getString(R.string.simlock_wrongcode));
        } 
        if (findViewById(R.id.keyPad) != null)
		{
        	new TouchInput();
		}
        mDismissButton = (TextView) findViewById(R.id.dismiss);//set the Cancel Button text to Dismiss
        //mDismissButton.setText("Dismiss");  
        mUnlockActionNotify = (TextView) findViewById(R.id.unlockActionNotify);
        mUnlockActionNotify.setText(getString(R.string.simlock_enterpincode)); 
        mUnlockRetriesNotify = (TextView) findViewById(R.id.unlockRetriesNotify);
        //mUnlockRetriesNotify.setText("5 retries");
        mUnlockForSIMNotify = (TextView)findViewById(R.id.unlockForSIMNotify);
        mUnlockSIMNameNotify = (TextView) findViewById(R.id.unlockSIMNameNotify);
        mUnlockSIMCategoryNotify = (TextView) findViewById(R.id.unlockSIMCategoryNotify);
        mUnlockEmptyForSingleCard = (TextView)findViewById(R.id.unlockSIMEmptyForSingleCard);
        if(false == FeatureOption.MTK_GEMINI_SUPPORT){
        	mUnlockForSIMNotify.setVisibility(View.GONE);
        	mUnlockSIMNameNotify.setVisibility(View.GONE);
        	mUnlockEmptyForSingleCard.setVisibility(View.INVISIBLE);
        }else{
            //mUnlockForSIMNotify.setText(this.getResources().getString(R.string.simlock_for));
        }  

        mbackspace.setOnClickListener(new ImageButton.OnClickListener(){
        	public void onClick(View view){
            	if (mPwdDisplay != null){
        		    sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
        		    strPwd = mPwdDisplay.getText().toString();
            		Log.d(LOGTAG, "[mbackspace][onClickListener][strPwd)]" + strPwd);
            	}           	
        	}
        });
        
        mbackspace.setOnLongClickListener(new ImageButton.OnLongClickListener(){
			public boolean onLongClick(View view) {
				// TODO Auto-generated method stub
				if (mPwdDisplay != null){
            		Log.d(LOGTAG, "[mbackspace][onLongClickListener][strPwd)]" + strPwd);
            		strPwd = null;
					mPwdDisplay.setText("");//delete all digits to display on the Pwd edittext
				}
				return false;
			}
        });
        
        mbtnEmergencyCall.setOnClickListener(new Button.OnClickListener(){
        	public void onClick(View view){
        		int state = TelephonyManager.getDefault().getCallState();
                if ((state == TelephonyManager.CALL_STATE_OFFHOOK)
        			|| (state == TelephonyManager.CALL_STATE_RINGING)){
                    //Need to add: if already exist a call 
                    ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                    try {
                        if (phone != null){
                        	phone.showCallScreen();
                        }
                    } catch (RemoteException e) {
                        // What can we do?
                    }
                } 
                else {
                    Intent intent = new Intent(ACTION_EMERGENCY_DIAL);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                   startActivity(intent);
                }
            }          	
        });
                

        
        Log.d(LOGTAG, "[unlock][onCreate]-");
    }//onCreat End

    

		  


@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}






@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		unregisterReceiver(mReceiver);
		super.onPause();
	}




	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Log.d(LOGTAG, "[onResume]+");
		
		updateEmergencyCallButtonState(mbtnEmergencyCall);
		//register ACTION_AIRPLANE_MODE_CHANGED
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
		//Refresh Left Retries
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
           		mUnlockRetriesNotify.setText(" ("  + getString(R.string.simlock_retriesleft) +String.valueOf(retryCount)+ " )");
			   }else{
				   UnlockPINLock.this.finish();
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
               		mUnlockRetriesNotify.setText(" (" + getString(R.string.simlock_retriesleft) +String.valueOf(retryCount) + " )");
 			   }else{
 				   UnlockPINLock.this.finish();
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
           		mUnlockRetriesNotify.setText(" ("  + getString(R.string.simlock_retriesleft) +String.valueOf(retryCount)+ " )");
 			   }else{
 				   UnlockPINLock.this.finish();
 				   return;
 			   }
    		}
       	   
           }
	       if(false == FeatureOption.MTK_GEMINI_SUPPORT){

	       }else{
			   mUnlockSIMNameNotify.setText(getOptrNameBySlotId(iSIMMEUnlockNo));
			   mUnlockSIMNameNotify.setBackgroundDrawable(getOptrDrawableBySlotId(iSIMMEUnlockNo));
		   }
		
	}


    public void sendDownUpKeyEvents(int keyEventCode) {
        long eventTime = SystemClock.uptimeMillis();
        Handler handler = mPwdDisplay.getHandler();
        if (handler != null){
            handler.sendMessage(handler.obtainMessage(ViewRoot.DISPATCH_KEY,
                new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyEventCode, 0, 0, 0, 0,
                    KeyEvent.FLAG_SOFT_KEYBOARD|KeyEvent.FLAG_KEEP_TOUCH_MODE)));
            handler.sendMessage(handler.obtainMessage(ViewRoot.DISPATCH_KEY,
                new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyEventCode, 0, 0, 0, 0,
                        KeyEvent.FLAG_SOFT_KEYBOARD|KeyEvent.FLAG_KEEP_TOUCH_MODE)));        
        }
    }

/**
 * Inner Class TouchInput
 * Helper class to handle input from touch dialer.  Only relevant when
 * the keyboard is shut.
 */
class TouchInput implements View.OnClickListener,View.OnTouchListener {
	
	int digit = -1;
    private TextView mZero;
    private TextView mOne;
    private TextView mTwo;
    private TextView mThree;
    private TextView mFour;
    private TextView mFive;
    private TextView mSix;
    private TextView mSeven;
    private TextView mEight;
    private TextView mNine;
    private TextView mOk;
    private TextView mCancelButton;

    TouchInput() {
        mZero = (TextView) findViewById(R.id.zero);
        mOne = (TextView) findViewById(R.id.one);
        mTwo = (TextView) findViewById(R.id.two);
        mThree = (TextView) findViewById(R.id.three);
        mFour = (TextView) findViewById(R.id.four);
        mFive = (TextView) findViewById(R.id.five);
        mSix = (TextView) findViewById(R.id.six);
        mSeven = (TextView) findViewById(R.id.seven);
        mEight = (TextView) findViewById(R.id.eight);
        mNine = (TextView) findViewById(R.id.nine);
        mCancelButton = (TextView) findViewById(R.id.dismiss);
        mOk = (TextView) findViewById(R.id.ok);

        mZero.setText("0");
        mOne.setText("1");
        mTwo.setText("2");
        mThree.setText("3");
        mFour.setText("4");
        mFive.setText("5");
        mSix.setText("6");
        mSeven.setText("7");
        mEight.setText("8");
        mNine.setText("9");

        mZero.setOnClickListener(this);
        mOne.setOnClickListener(this);
        mTwo.setOnClickListener(this);
        mThree.setOnClickListener(this);
        mFour.setOnClickListener(this);
        mFive.setOnClickListener(this);
        mSix.setOnClickListener(this);
        mSeven.setOnClickListener(this);
        mEight.setOnClickListener(this);
        mNine.setOnClickListener(this);
        mOk.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);
        
        mZero.setOnTouchListener(this);
        mOne.setOnTouchListener(this);
        mTwo.setOnTouchListener(this);
        mThree.setOnTouchListener(this);
        mFour.setOnTouchListener(this);
        mFive.setOnTouchListener(this);
        mSix.setOnTouchListener(this);
        mSeven.setOnTouchListener(this);
        mEight.setOnTouchListener(this);
        mNine.setOnTouchListener(this);
        mOk.setOnTouchListener(this);
        mCancelButton.setOnTouchListener(this);
    }

    //To change button color while touch the button
    public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
    	Resources resources = getBaseContext().getResources();
		if(event.getAction() == MotionEvent.ACTION_DOWN){
			v.setBackgroundDrawable(resources.getDrawable(R.drawable.btnkeyboradclick));
		}else if(event.getAction() == MotionEvent.ACTION_UP){
			if(v == mOne || v == mTwo){
				v.setBackgroundDrawable(resources.getDrawable(R.drawable.btnnokeyboradunclicka));
			}else if(v == mThree){
				v.setBackgroundDrawable(resources.getDrawable(R.drawable.btnnokeyboradunclicke));
			}else if (v == mFour || v == mFive){
				v.setBackgroundDrawable(resources.getDrawable(R.drawable.btnnokeyboradunclickb));
			}else if(v == mSix){
				v.setBackgroundDrawable(resources.getDrawable(R.drawable.btnnokeyboradunclickf));
			}else if(v == mSeven || v == mEight){
				v.setBackgroundDrawable(resources.getDrawable(R.drawable.btnnokeyboradunclickc));
			}else if(v == mNine){
				v.setBackgroundDrawable(resources.getDrawable(R.drawable.btnnokeyboradunclickg));
			}else if(v == mOk){
				v.setBackgroundDrawable(resources.getDrawable(R.drawable.btnnokeyboradunclickj));
			}else if(v == mZero){
				v.setBackgroundDrawable(resources.getDrawable(R.drawable.btnnokeyboradunclickd));
			}else if(v == mCancelButton){
				v.setBackgroundDrawable(resources.getDrawable(R.drawable.btnnokeyboradunclickh));
			}else{
			
			}
		}
		return false;
    }
    

    public void onClick(View v) {
    	Log.d(LOGTAG, "[onClick]+" );
    	if (mPwdDisplay == null){
            Log.d(LOGTAG, "[onClick][mPwdDisplay] : null");    		
    	}else{
            Log.d(LOGTAG, "[onClick][mPwdDisplay] : not null");    		    		
    	}

        if (v == mCancelButton) {
        	Log.d(LOGTAG, "[onClick][Cancel Button]" );
        	sendVerifyResult(VERIFY_TYPE_PIN,false);
	    		UnlockPINLock.this.finish();
	    		return;
        }

        if (v == mOk) {  
        	Log.d(LOGTAG, "[onClick][OK Button]" );
        	if (mPwdDisplay != null){
        		Log.d(LOGTAG, "[onClick][OK Button][mPwdDisplay] : not null " );
    		    strPwd = mPwdDisplay.getText().toString();
    		    PwdLength = strPwd.length();  
    		    if(bStringValid(strPwd) == false){
    		    	mUnlockResultNotify.setText(getString(R.string.simlock_input4_8digits)); 
    		    }else{
	    		    if((PwdLength >= 4) && (PwdLength <= 8)){
	    		    	Log.d(LOGTAG, "[onClick][OK Button][strPwd] : " + Integer.valueOf(strPwd));
	    		    	//progressDialog =ProgressDialog.show(PowerOnUnlockSIMLock.this,"","Unlocking....");
	    		    	progressDialog = new ProgressDialog(UnlockPINLock.this);
	    		    	progressDialog.setMessage(getString(R.string.simlock_unlocking));
	    		    	progressDialog.setIndeterminate(true);
	    		    	progressDialog.setCancelable(false);
	    		    	progressDialog.getWindow().setType(
	    	                    WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
	    		    	/*progressDialog.getWindow().setFlags(
	                            WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
	                            WindowManager.LayoutParams.FLAG_BLUR_BEHIND);*/
	    		    	progressDialog.show();
	    		    	
	    		    	Log.d(LOGTAG, "[onClick][OK Button][Start Unlock Process]");
	    	            if(false == FeatureOption.MTK_GEMINI_SUPPORT)//Single Card Unlock
	    	            {
	    	            	unlockPIN(strPwd);
	    	            }else{//Gemini Card Unlock
	    	            	if (iSIMMEUnlockNo == 0){//SIM1 Unlock
	    	            		unlockPIN(iSIMMEUnlockNo,strPwd);
	    	            	}else{//SIM2 Unlock
	    	            		unlockPIN(iSIMMEUnlockNo,strPwd);
	    	            	}
	    	            }
	    		    	Log.d(LOGTAG, "[onClick][OK Button][Finish Unlock Process]");
	    		    }else{
	    		    	//Notify the user to input 4---8 digits
	    		    	mUnlockResultNotify.setText(getString(R.string.simlock_input4_8digits)); 
	    		    }
    		    }
    		    strPwd = null;//delete number entered before
    		    mPwdDisplay.setText("");
        	
        	}//UnlockPINLock.this.finish();// temp handle this 
        }//if (v == mOK)  End
               
        int keyEventCode = checkDigit(v);
        if (keyEventCode >= 0) {
        	if (mPwdDisplay != null){
        		    strPwd = mPwdDisplay.getText().toString();
        		    PwdLength = strPwd.length();
        		    if(PwdLength < 8){
        		    	strPwd += String.valueOf(digit);
        		    	sendDownUpKeyEvents(keyEventCode);
        		    }
        		    	
        		    //mPwdDisplay.setText(strPwd);//add digit to display on the Pwd edittext
        		
        	    }
        	//showAlertDialog(DIALOG_ENTERNUMBER);//Show dialog of pressed number key for test
        	digit = -1;       	
        	}
        
    }//onClick() End
    


    private int checkDigit(View v) {
        int keyEventCode = -1;
        if (v == mZero) {
            digit = 0;
            keyEventCode = KeyEvent.KEYCODE_0;
        } else if (v == mOne) {
            digit = 1;
            keyEventCode = KeyEvent.KEYCODE_1;
        } else if (v == mTwo) {
            digit = 2;
            keyEventCode = KeyEvent.KEYCODE_2;
        } else if (v == mThree) {
            digit = 3;
            keyEventCode = KeyEvent.KEYCODE_3;
        } else if (v == mFour) {
            digit = 4;
            keyEventCode = KeyEvent.KEYCODE_4;
        } else if (v == mFive) {
            digit = 5;
            keyEventCode = KeyEvent.KEYCODE_5;
        } else if (v == mSix) {
            digit = 6;
            keyEventCode = KeyEvent.KEYCODE_6;
        } else if (v == mSeven) {
            digit = 7;
            keyEventCode = KeyEvent.KEYCODE_7;
        } else if (v == mEight) {
            digit = 8;
            keyEventCode = KeyEvent.KEYCODE_8;
        } else if (v == mNine) {
            digit = 9;
            keyEventCode = KeyEvent.KEYCODE_9;
        }
        return keyEventCode;
    }//checkDigit() end
    
    protected void  showAlertDialog(int id){
    	if (id == DIALOG_ENTERNUMBER){
    		AlertDialog.Builder builder = new AlertDialog.Builder(UnlockPINLock.this);
    		switch(id){
    		case DIALOG_ENTERNUMBER: //Press Numbers
    			builder.setIcon(android.R.drawable.ic_dialog_alert)
    	    	.setMessage(String.valueOf(digit))
    	    	.setNegativeButton("OK", new DialogInterface.OnClickListener() 
    	    	{			
    				public void onClick(DialogInterface dialog, int id) 
    				{
    					dialog.cancel();
    				}
    			}).show();
    	    	break;
    		}//switch() End
    	}//if() End
    	
    }//showAlertDialog() funciton End
    
  }// Inner Class End

public void onAttachedToWindow(){
	this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD);
	super.onAttachedToWindow();
}



public boolean onKeyDown(int keyCode, KeyEvent event) {
	// TODO Auto-generated method stub
	Log.d(LOGTAG, "[onKeyDown][Pressed invalid Key][keyCode()]:" + keyCode );
	switch(keyCode)
	{
		case KeyEvent.KEYCODE_BACK:
		case KeyEvent.KEYCODE_MENU:
		case KeyEvent.KEYCODE_HOME:
			Log.d(LOGTAG, "[onKeyDown][Pressed invalid Key]" );
			return true;
	}
	Log.d(LOGTAG, "[onKey][Pressed invalid Key]-" );
	return false;
}

public Handler mHandler = new Handler(){
	public void handleMessage(Message msg){
		switch(msg.what){
		case MSG2:
        	//Process Unlock SIM Lock

    		Log.d(LOGTAG, "[mHandler][MSG2] +");
    		Log.d(LOGTAG, "[mHandler][MSG2][Result] : " + result);
     		if (result == true){//unlock PIN succeed
     			//sendVerifyResult(VERIFY_TYPE_PIN,true);
    			UnlockPINLock.this.finish();
    			
    		}else{//need to check current status
    			
    			Phone phone = PhoneFactory.getDefaultPhone();
    			if(phone.getIccCard().getState() == IccCard.State.PIN_REQUIRED){//Need to unlock PIN
        			retryCount = getRetryPinCount(Phone.GEMINI_SIM_1);
        			Log.d(LOGTAG, "[mHandler][MSG2][Single Card][PIN_REQUIRED][New Retry Count] : " + retryCount);
        			if (retryCount > 0){//still have chances to unlock PIN
           				Intent intent = new Intent(UnlockPINLock.this, UnlockPINLock.class);  
                		Bundle bundle = new Bundle();
                		bundle.putInt("PUKLEFTRETRIES", retryCount);
                		bundle.putInt("Phone.GEMINI_SIM_ID_KEY", iSIMMEUnlockNo);
                		bundle.putString("WRONGCODE", getString(R.string.simlock_wrongcode));
                		intent.putExtras(bundle);
                		startActivity(intent);
                		UnlockPINLock.this.finish();
                		
        			}    	    	            				
    			}else{
       				Intent intent = new Intent(UnlockPINLock.this, UnlockPUKLock.class);  
            		Bundle bundle = new Bundle();
            		int retryPUKCount = getRetryPukCount(Phone.GEMINI_SIM_1);
            		bundle.putString("PUKPHASE", "1");//first step to unlock PUK
            		bundle.putInt("PUKLEFTRETRIES", retryPUKCount);
            		bundle.putInt("Phone.GEMINI_SIM_ID_KEY", iSIMMEUnlockNo);
            		intent.putExtras(bundle);
            		startActivity(intent);
            		UnlockPINLock.this.finish();
            		
    			}
    		}
			break;
		case MSG3:
        	//Process Unlock SIM Lock

    		Log.d(LOGTAG, "[mHandler][MSG3] +");
    		Log.d(LOGTAG, "[mHandler][MSG3][Result] : " + result);
     		if (result == true){//unlock PIN succeed
     			//sendVerifyResult(VERIFY_TYPE_PIN,true);
    			UnlockPINLock.this.finish();
    			
    		}else{//need to check current status
    			
    			GeminiPhone mGeminiPhone = (GeminiPhone)PhoneFactory.getDefaultPhone();
    			Log.d(LOGTAG, "[mHandler][MSG3][Gemini Card][SIM1][UnlockPinFail]+");
    			if (mGeminiPhone.getIccCardGemini(Phone.GEMINI_SIM_1).getState() == IccCard.State.PIN_REQUIRED){
    				Log.d(LOGTAG, "[mHandler][MSG3][Gemini Card][SIM1][Still PIN_REQUIRED]");
        			retryCount = getRetryPinCount(Phone.GEMINI_SIM_1);
        			Log.d(LOGTAG, "[mHandler][MSG3][Gemini Card][SIM1][New Retry Count] : " + retryCount);
        			if (retryCount > 0){//still have chances to unlock PIN
           				Intent intent = new Intent(UnlockPINLock.this, UnlockPINLock.class);  
                		Bundle bundle = new Bundle();
                		bundle.putInt("PUKLEFTRETRIES", retryCount);
                		bundle.putInt("Phone.GEMINI_SIM_ID_KEY", iSIMMEUnlockNo);
                		bundle.putString("WRONGCODE", getString(R.string.simlock_wrongcode));
                		intent.putExtras(bundle);
                		startActivity(intent);
                		UnlockPINLock.this.finish();
                		
        			}    	    	            				
    			}else{
       				Intent intent = new Intent(UnlockPINLock.this, UnlockPUKLock.class);  
            		Bundle bundle = new Bundle();
            		int retryPUKCount = getRetryPukCount(Phone.GEMINI_SIM_1);
            		bundle.putString("PUKPHASE", "1");//first step to unlock PUK
            		bundle.putInt("PUKLEFTRETRIES", retryPUKCount);
            		bundle.putInt("Phone.GEMINI_SIM_ID_KEY", iSIMMEUnlockNo);
            		intent.putExtras(bundle);
            		startActivity(intent);
            		UnlockPINLock.this.finish();
            		
    			}
    		}
			break;
		case MSG4:
        	//Process Unlock SIM Lock

    		Log.d(LOGTAG, "[mHandler][MSG4] +");
    		Log.d(LOGTAG, "[mHandler][MSG4][Result] : " + result);
     		if (result == true){//unlock PIN succeed
     			//sendVerifyResult(VERIFY_TYPE_PIN,true);
    			UnlockPINLock.this.finish();
    			
    		}else{//need to check current status
    			
    			GeminiPhone mGeminiPhone = (GeminiPhone)PhoneFactory.getDefaultPhone();
    			Log.d(LOGTAG, "[mHandler][MSG3][Gemini Card][SIM2][PIN_REQUIRED][UnlockPinFail]+");
    			if (mGeminiPhone.getIccCardGemini(Phone.GEMINI_SIM_2).getState() == IccCard.State.PIN_REQUIRED){
    				Log.d(LOGTAG, "[mHandler][MSG3][Gemini Card][SIM2][PIN_REQUIRED][Still PIN_REQUIRED]");
        			retryCount = getRetryPinCount(Phone.GEMINI_SIM_2);
        			Log.d(LOGTAG, "[mHandler][MSG3][Gemini Card][SIM2][New Retry Count] : " + retryCount);
        			if (retryCount > 0){//still have chances to unlock PIN
           				Intent intent = new Intent(UnlockPINLock.this, UnlockPINLock.class);  
                		Bundle bundle = new Bundle();
                		bundle.putInt("PUKLEFTRETRIES", retryCount);
                		bundle.putInt("Phone.GEMINI_SIM_ID_KEY", iSIMMEUnlockNo);
                		bundle.putString("WRONGCODE", getString(R.string.simlock_wrongcode));
                		intent.putExtras(bundle);
                		startActivity(intent);
                		UnlockPINLock.this.finish();
                		
        			}    	    	            				
    			}else{
       				Intent intent = new Intent(UnlockPINLock.this, UnlockPUKLock.class);  
            		Bundle bundle = new Bundle();
            		int retryPUKCount = getRetryPukCount(Phone.GEMINI_SIM_2);
            		bundle.putString("PUKPHASE", "1");//first step to unlock PUK
            		bundle.putInt("PUKLEFTRETRIES", retryPUKCount);
            		bundle.putInt("Phone.GEMINI_SIM_ID_KEY", iSIMMEUnlockNo);
            		intent.putExtras(bundle);
            		startActivity(intent);
            		UnlockPINLock.this.finish();
            		
    			}
    		}
			break;
		}
	super.handleMessage(msg);
	progressDialog.dismiss();
	}
};


private void unlockPIN(final String strPwd){
    //Single Card:
	Phone phone = PhoneFactory.getDefaultPhone();
	if(phone.getIccCard().getState() == IccCard.State.PIN_REQUIRED){//Need to unlock PIN
		//unlock PIN Code
		int retryCount = getRetryPinCount(Phone.GEMINI_SIM_1);
		if (retryCount > 0){
		    	
	    	Log.d(LOGTAG, "[unlockPIN][new Thread][Start]");
	    	//start a new Thread for unlock PIN
	    	new Thread(){
	    		public void run(){
	    			Log.d(LOGTAG, "[unlockPIN][new Thread][Run]+");
            		try{
            			Log.d(LOGTAG, "[unlockPIN][new Thread][TogetResult][strPwd]" + strPwd);
                                if (ITelephony.Stub.asInterface(ServiceManager.checkService("phone")) != null){
                                    result = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).supplyPin(strPwd);
                                }
            			
            		}catch(RemoteException re){
            			re.printStackTrace();
            		}
            		Log.d(LOGTAG, "[unlockPIN][new Thread][Result] : " + result);
            		Message msg3 = Message.obtain(mHandler, MSG2);
	    			msg3.sendToTarget();
	    			Log.d(LOGTAG, "[unlockPIN][new Thread][Run]-");
	    		}
	    		
	    	}.start();
	    	Log.d(LOGTAG, "[unlockPIN][new Thread][Finsish]");
		}else{//no chance to unlock pin
			
		}

		
	}

}


public void unlockPIN(int simId,final String strPwd){
	GeminiPhone mGeminiPhone = (GeminiPhone)PhoneFactory.getDefaultPhone();
	if (simId == 0)
	{
		 Log.d(LOGTAG, "[unlockPIN][Gemini Card][SIM1]+" );
		
   	     if (mGeminiPhone.getIccCardGemini(Phone.GEMINI_SIM_1).getState() == IccCard.State.PIN_REQUIRED){
   	    	 //card1 need to unlock PIN
   	    	Log.d(LOGTAG, "[unlockPIN][Gemini Card][SIM1][PIN_REQUIRED]+" );
    		int retryCount = getRetryPinCount(Phone.GEMINI_SIM_1);
    		Log.d(LOGTAG, "[unlockPIN][Gemini Card][SIM1][PIN_REQUIRED][retryCount] : " +  retryCount);
    		if (retryCount > 0){
   		    	
		    	Log.d(LOGTAG, "[unlockPIN][Gemini Card][SIM1][new Thread][Start]");
		    	//start a new Thread for unlock PIN
		    	new Thread(){
		    		public void run(){
		    			Log.d(LOGTAG, "[unlockPIN][Gemini Card][SIM1][new Thread][Run]+");
	            		try{
	            			Log.d(LOGTAG, "[unlockPIN][Gemini Card][SIM1][TogetResult][strPwd]" + strPwd);
	            			if (ITelephony.Stub.asInterface(ServiceManager.checkService("phone")) != null){
	            				result = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).supplyPinGemini(strPwd, Phone.GEMINI_SIM_1);
	            			}       			
	            		}catch(RemoteException re){
	            			re.printStackTrace();
	            		}
	            		Log.d(LOGTAG, "[unlockPIN][Gemini Card][SIM1][mHandler][Result] : " + result);
	            		Log.d(LOGTAG, "[unlockPIN][Gemini Card][SIM1][mHandler][Result] -");
	            		Message msg3 = Message.obtain(mHandler, MSG3);
		    			msg3.sendToTarget();
		    		}
		    		
		    	}.start();
		    	Log.d(LOGTAG, "[unlockPIN][Gemini Card][SIM1][new Thread][Finsish]");
    		}else{//no chance to unlock pin
    			
    		}

		 Log.d(LOGTAG, "[unlockPIN][Gemini Card][SIM1]-" );
   	     }
   	}
	else
	{
		Log.d(LOGTAG, "[unlockPIN][Gemini Card][SIM2]+" );
		//Message callback = Message.obtain(mHandler,  UNLOCK_ICC_SML_COMPLETE);
		if (mGeminiPhone.getIccCardGemini(Phone.GEMINI_SIM_2).getState() == IccCard.State.PIN_REQUIRED){
   	    	 //card2 need to unlock PIN
   	    	Log.d(LOGTAG, "[unlockPIN][Gemini Card][SIM2][PIN_REQUIRED]+" );
    		int retryCount = getRetryPinCount(Phone.GEMINI_SIM_2);
    		Log.d(LOGTAG, "[unlockPIN][Gemini Card][SIM2][PIN_REQUIRED][retryCount] : " +  retryCount);
    		if (retryCount > 0){
   		    	
		    	Log.d(LOGTAG, "[unlockPIN][Gemini Card][SIM2][new Thread][Start]");
		    	//start a new Thread for unlock PIN
		    	new Thread(){
		    		public void run(){
		    			Log.d(LOGTAG, "[unlockPIN][Gemini Card][SIM2][new Thread][Run]+");
	            		try{
	            			Log.d(LOGTAG, "[unlockPIN][Gemini Card][SIM2][TogetResult][strPwd]" + strPwd);
	            			if (ITelephony.Stub.asInterface(ServiceManager.checkService("phone")) != null){
	            				result = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).supplyPinGemini(strPwd, Phone.GEMINI_SIM_2);
	            			}     			
	            		}catch(RemoteException re){
	            			re.printStackTrace();
	            		}
	            		Log.d(LOGTAG, "[unlockPIN][Gemini Card][SIM2][mHandler][Result] : " + result);
	            		Log.d(LOGTAG, "[unlockPIN][Gemini Card][SIM2][mHandler][Result] -");
	            		Message msg3 = Message.obtain(mHandler, MSG4);
		    			msg3.sendToTarget();
		    			Log.d(LOGTAG, "[unlockPIN][Gemini Card][SIM2][new Thread][Run]-");
		    		}
		    		
		    	}.start();
		    	Log.d(LOGTAG, "[unlockPIN][Gemini Card][SIM2][new Thread][Finsish]");
    		}else{//no chance to unlock pin
    			
    		}
    	Log.d(LOGTAG, "[Gemini Card][SetSIMLock][SIM 2]-" );
	}
} 
}


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

public boolean onKeyLongPress(int keyCode, KeyEvent event) {
	// TODO Auto-generated method stub
	Log.d(LOGTAG, "[onKeyLongPress][Long Pressed invalid Key][keyCode()]:" + keyCode );
	if(event.getKeyCode() ==  KeyEvent.KEYCODE_MENU){
		Log.d(LOGTAG, "[onKeyLongPress][Pressed invalid Key]" );
		return true;
	}
	return false;
}

private final BroadcastReceiver mReceiver = new  BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) {
    	Log.d(LOGTAG, "[BroadcastReceiver][onReceiver]+" );
        String action = intent.getAction();
        Log.d(LOGTAG, "[BroadcastReceiver][onReceiver][action] : " +action );
        if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
        	Log.d(LOGTAG, "[BroadcastReceiver][onReceiver][ACTION_AIRPLANE_MODE_CHANGED]" );
            finish();
          //sendVerifyResult(VERIFY_TYPE_PIN,false);
        }
        Log.d(LOGTAG, "[BroadcastReceiver][onReceiver]-" );
    }
};


  //To get OptrName by Slot No
    public String getOptrNameBySlotId(long Slot){
    	Log.d(LOGTAG, "[getOptrNameBySlotId][Slot]: "+  (int)Slot);
    	if(Slot >= 0){
    		SIMInfo info = SIMInfo.getSIMInfoBySlot(this,(int)Slot);
    		Log.d(LOGTAG, "[getOptrNameBySlotId][info]: "+  info);    		
    		if (info.mDisplayName != null){
    			Log.d(LOGTAG, "[getOptrNameBySlotId][OptrName]: "+  info.mDisplayName);
    			return info.mDisplayName;
    		}else{
    			return this.getResources().getString(R.string.simlock_newsim);
    		}		
    	}else{
    		return this.getResources().getString(R.string.simlock_nocard);
    	}
    }

    public Drawable getOptrDrawableBySlotId(long Slot){
        if(Slot >= 0){
            SIMInfo info = SIMInfo.getSIMInfoBySlot(this,(int)Slot);
            Log.d(LOGTAG, "[getOptrDrawableBySlotId][info]: "+  info);
            if(info != null){
                return this.getResources().getDrawable(info.mSimBackgroundRes);    			
            } else
                return null;
        }else{
            return null;
        }
    }

public static boolean bCharNumber(char ch){
	if((ch >= '0') && (ch <= '9')){
		return true;
	}else{
		return false;
	}
}


public static boolean bStringValid(String str){
	for(int i = 0;i < str.length();i++){
		if(bCharNumber(str.charAt(i)) == false){
			return false;
		}
	}
	return true;
	
}

public void updateEmergencyCallButtonState(Button button) {
    int newState = TelephonyManager.getDefault().getCallState();
    int textId;
    if (newState == TelephonyManager.CALL_STATE_OFFHOOK) {
        // show "return to call" text and show phone icon
        button.setText(getString(R.string.simlock_returntocall));
    } else {
        button.setText(getString(R.string.simlock_emergency));
    }
}
}//Main Class End

