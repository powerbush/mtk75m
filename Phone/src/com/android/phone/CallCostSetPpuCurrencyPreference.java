/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

import java.text.NumberFormat;
import android.text.method.DialerKeyListener;
import android.widget.TextView;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandException.Error;
import com.android.phone.ChangeIccPinScreen;
import com.android.phone.PhoneApp;
import android.os.SystemProperties;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;
import java.util.Locale;
import static com.android.phone.TimeConsumingPreferenceActivity.EXCEPTION_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.RESPONSE_ERROR;
import com.mediatek.xlog.Xlog;

/* Fion add start */
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;
/* Fion add end */

public class CallCostSetPpuCurrencyPreference extends Preference {
    private static final String LOG_TAG = "Settings/CallCostSetPpuCurrencyPreference";
    private static final boolean DBG = true;// (PhoneApp.DBG_LEVEL >= 2);
    private static final int MESSAGE_GET_PPU_CURRENCY = 100;
    private static final int MESSAGE_SET_PPU_CURRENCY = 101;
    private static final int PPU_LENGTH_1 = 5; // ppu,before "."
    private static final int PPU_LENGTH_2 = 4; // ppu after "."
    private static final double PPU_MAX = 100000.0;
    
    private static final String COUNTRY_CODE_BR="BR";
    private static final String COUNTRY_CODE_RU="RU";
    private static final String COUNTRY_CODE_ES="ES";

    private MyHandler mHandler = new MyHandler();
    private Phone phone;
    private TimeConsumingPreferenceListener mListener;

/* Fion add start */
    public static final int DEFAULT_SIM = 2; /* 0: SIM1, 1: SIM2 */

    private int mSimId = DEFAULT_SIM;
/* Fion add end */

    public CallCostSetPpuCurrencyPreference(Context context) {
        this(context, null);
    }

    public CallCostSetPpuCurrencyPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        phone = PhoneFactory.getDefaultPhone();
    }

    private void showInputDialog(String pinRetryCount) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        final View setView = inflater.inflate(R.layout.pref_set_ppu_currency,
                null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(setView);
        builder.setTitle(R.string.set_ppu_currency);

        TextView pinTextView = (TextView) setView
                .findViewById(R.id.showPinTitle);
        if (pinRetryCount != null) {
            String pinTitle = pinTextView.getText().toString() + "\n( "
                    + pinRetryCount + " )";
            pinTextView.setText(pinTitle);
        }
        EditText get_ppu2 = (EditText) setView.findViewById(R.id.get_ppu);
        get_ppu2.setKeyListener(CallCostKeyListener.getInstance());        
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        EditText get_currency = (EditText) setView
                                .findViewById(R.id.get_currency);
                        EditText get_ppu = (EditText) setView
                                .findViewById(R.id.get_ppu);
                        EditText get_pin = (EditText) setView
                                .findViewById(R.id.get_pin);
                        String currency = get_currency.getText().toString();
                        String ppu = get_ppu.getText().toString();
                        String pin = get_pin.getText().toString();

                        String errorInfo = doChecking(currency, ppu, pin);
                        if (errorInfo.equals("")) {
                            setPpuCurrency(currency, ppu, pin);
                            if (mListener != null)
                                mListener.onStarted(
                                        CallCostSetPpuCurrencyPreference.this,
                                        false);
                        } else
                            displayMessage(errorInfo);
                    }
                });
        AlertDialog dialog=builder.create();
        Window window = dialog.getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE |
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        dialog.show();
    }

    // if the result of checking is right, will return "", else will return a
    // message String
    private String doChecking(String currency, String ppu, String pin) {
        String errorInfo = "";
        if (!checkCurrency(currency)) {
            errorInfo += getContext().getString(
                    R.string.call_cost_input_currency_error_msg);
            errorInfo += "\n";
        }
        if (!checkPpu(ppu,true)) {
            errorInfo += getContext().getString(
                    R.string.call_cost_input_ppu_error_msg);
            errorInfo += "\n";
        }
        if (!checkPin(pin)) {
            errorInfo += getContext().getString(
                    R.string.call_cost_input_pin_error_msg);
        }
        return errorInfo;
    }

    private Context getPrefContext() {
        return this.getContext();
    }

    private void checkPinRetryAndShowDialog() {
    	int mPinEnterLimit = 0;
        if(CallSettings.isMultipleSim())
        {
        	if (mSimId == Phone.GEMINI_SIM_1)
        	{
        	    mPinEnterLimit = SystemProperties.getInt("gsm.sim.retry.pin2", -1);
        	}
        	else
        	{
        		mPinEnterLimit = SystemProperties.getInt("gsm.sim.retry.pin2.2", -1);
        	}
        }
        else
        {
        	mPinEnterLimit = SystemProperties.getInt("gsm.sim.retry.pin2", -1);
        }
        if (mPinEnterLimit >= 1 && mPinEnterLimit <= 3) {
            int resId = 0;
             String temp;
             if (mPinEnterLimit == 1){
                 resId = R.string.one_retry_left;
             	temp=getPrefContext().getString(resId);
             }
             else{
                 resId = R.string.retries_left;
                 temp=getPrefContext().getString(resId,mPinEnterLimit);
             }
            showInputDialog(temp);
        } else if (mPinEnterLimit == 0) {
            String errorTitle = getPrefContext()
                    .getString(R.string.error_title);
            String errorMsg = getPrefContext().getString(
                    R.string.pin_retry_error_info);
            showTipDialog(errorTitle, errorMsg);
        } else {
            showInputDialog(null);
            Xlog.i(LOG_TAG, "can not get the pin2 retries left");
        }
    }

    @Override
    protected void onClick() {
        super.onClick();
        checkPinRetryAndShowDialog();
    }

    private void setPpuCurrency(String currency, String ppu, String pin2) {
        Message msg = new Message();
        msg = mHandler.obtainMessage(MESSAGE_SET_PPU_CURRENCY, 0,
                MESSAGE_SET_PPU_CURRENCY, null);
        currency = currency.toUpperCase();
        ppu = formatPpu(ppu,true);
/* Fion add start */
        if (CallSettings.isMultipleSim())
        {
            ((GeminiPhone)phone).setPpuAndCurrencyGemini(currency, ppu, pin2, msg, mSimId);
        }
        else
        {
            phone.setPpuAndCurrency(currency, ppu, pin2, msg);
        }
/* Fion add end */

        Xlog.d(LOG_TAG, "setPpuCurrency:send msg to system,currency="+currency+"  ppu="+ppu+" pin2="+pin2);
    }

    // reason=true means get the ppu when init.
    // reason=false means get the ppu after setting the Max call cost
    private void getPpuCurrency(boolean reason) {
        Message msg = new Message();
        if (reason) {
            msg = mHandler.obtainMessage(MESSAGE_GET_PPU_CURRENCY, 0,
                    MESSAGE_GET_PPU_CURRENCY, null);
        } else {
            msg = mHandler.obtainMessage(MESSAGE_GET_PPU_CURRENCY, 0,
                    MESSAGE_SET_PPU_CURRENCY, null);
        }

/* Fion add start */
        if (CallSettings.isMultipleSim())
        {
            ((GeminiPhone)phone).getPpuAndCurrencyGemini(msg, mSimId);
        }
        else
        {
            phone.getPpuAndCurrency(msg);
        }
/* Fion add end */


        Xlog.d(LOG_TAG, "getPpuCurrency: send msg to system to get data.");
    }

/* Fion add start */
    public void init(TimeConsumingPreferenceListener listener,
            boolean skipReading, int simId) {

        Xlog.d(LOG_TAG,"init, simId = "+simId);

        mListener = listener;
        mSimId = simId;
		
        if (!skipReading) {
            getPpuCurrency(true);
            if (mListener != null) {
                mListener.onStarted(this, true);
            }
        }
    }
/* Fion add end */

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_GET_PPU_CURRENCY:
                handleGetPpuCurrencyResponse(msg);
                break;
            case MESSAGE_SET_PPU_CURRENCY:
                handleSetPpuCurrencyResponse(msg);
                break;
            }
        }

        private void handleGetPpuCurrencyResponse(Message msg) {
        	Xlog.d(LOG_TAG, "handleGetPpuCurrencyResponse: request get from system.");
            if (msg.arg2 == MESSAGE_GET_PPU_CURRENCY) {
                if (mListener != null)
                    mListener.onFinished(CallCostSetPpuCurrencyPreference.this,
                            true);
            } else {
                if (mListener != null)
                    mListener.onFinished(CallCostSetPpuCurrencyPreference.this,
                            false);
            }
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception != null) {
                if (mListener != null)
                    mListener.onError(CallCostSetPpuCurrencyPreference.this,
                            EXCEPTION_ERROR);
                if (msg.arg2 == MESSAGE_GET_PPU_CURRENCY) {
                    setEnabled(false);
                    setSummary(R.string.disable);
                }
                if (DBG)
                    Xlog.d(LOG_TAG,
                            "handleGetPpuCurrencyResponse: ar.exception="
                                    + ar.exception);
                if (mListener != null)
                    mListener.onError(CallCostSetPpuCurrencyPreference.this,
                            EXCEPTION_ERROR);
            } else {
                if (ar.userObj instanceof Throwable) {
                    if (mListener != null)
                        mListener.onError(
                                CallCostSetPpuCurrencyPreference.this,
                                RESPONSE_ERROR);
                } else {
                    String str = "";
                    String currency = ((String[]) ar.result)[0];
                    String ppu = ((String[]) ar.result)[1];
                    if (checkPpu(ppu,false) && checkCurrency(currency)) {
                        str += currency + "   " + spiltPppu(formatPpu(ppu,false));
                        setSummary(str.subSequence(0, str.length()));
                    } else {
                        if (DBG)
                            Xlog.d(LOG_TAG, "handleGetPpuCurrencyResponse: data format error");
                        setSummary(R.string.disable);
                    }
                }
            }
        }

        private void handleSetPpuCurrencyResponse(Message msg) {
        	Xlog.d(LOG_TAG, "handleSetPpuCurrencyResponse: request get from system.");
            if (msg.arg2 == MESSAGE_SET_PPU_CURRENCY) {
                if (mListener != null)
                    mListener.onFinished(CallCostSetPpuCurrencyPreference.this,
                            false);
            }
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception != null) {
                if (DBG)
                    Xlog.d(LOG_TAG,
                            "handleSetPpuCurrencyResponse: ar.exception="
                                    + ar.exception);
                handleException(ar);
            } else {
                getPpuCurrency(false);
                Xlog
                        .i(LOG_TAG,
                                "Function:handleSetPpuCurrencyResponse :system retutn ok   ");
            }
        }
    }

    private void  handleException(AsyncResult ar){
    	Error errorCode=((CommandException)(ar.exception)).getCommandError();
        Xlog.i(LOG_TAG,"= loyee = CallCostSetPpuCurrencyPreference errorCode="+errorCode);
        switch(errorCode){
		case PASSWORD_INCORRECT:
			showExceptionTipDialog(getPrefContext().getString(R.string.error_title),getPrefContext().getString(R.string.pin2_invalid));
			break;
		/*
		 * TODO add the other Exceptions handle here
		 */
		default:
			if (mListener != null)
				mListener.onError(CallCostSetPpuCurrencyPreference.this,EXCEPTION_ERROR);
			break;
        }
    }

    private void showExceptionTipDialog(String title, String msg) {
        new AlertDialog.Builder(getPrefContext())
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
    
    private boolean checkPin(String pin) {
        if (pin == null || pin.length() > 8 || pin.length() < 4)
            return false;
        else
            return true;
    }

    private void showTipDialog(String title, String msg) {
        new AlertDialog.Builder(getPrefContext())
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(
                        R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                gotoUnlockPin2Screen();
                            }
                        }).show();
    }

    private boolean checkCurrency(String currency) {
        if (currency == null || currency.length() != 3) {
            return false;
        }
        for (int i = 0; i < currency.length(); i++) {
            if ((currency.charAt(i) >= 'a' && currency.charAt(i) <= 'z')
                    || (currency.charAt(i) >= 'A' && currency.charAt(i) <= 'Z'))
                continue;
            else
                return false;
        }
        return true;
    }

    private boolean checkPpu(String ppu,boolean down) {
        if (ppu == null || ppu.length() <= 0)
            return false;
		if (down) {
			// if ES,PT,RU
			if (specialLanguage()) {
				// the number of ','
				int flag = 0;
				int length = ppu.length();
				for (int i = 0; i < length; i++) {
					if (ppu.charAt(i) == ',')
						flag++;
				}
				if (flag >= 2)
					return false;
				ppu = ppu.replace(".", "");
				ppu = ppu.replace(",", ".");
			} else {
				// the number of '.'
				int flag = 0;
				int length = ppu.length();
				for (int i = 0; i < length; i++) {
					if (ppu.charAt(i) == '.')
						flag++;
				}
				if (flag >= 2)
					return false;
				ppu = ppu.replace(",", "");
			}
		}
        if (ppu == null || ppu.length() <= 0)
            return false;
        if (ppu.equals("."))
            return false;
        Double tempPpu = Double.valueOf(ppu);
        double temp = tempPpu.doubleValue();
        if (temp < 0 || temp >  PPU_MAX)
            return false;
        return true;
    }

    private final void displayMessage(int strId) {
        Toast.makeText(getPrefContext(), getPrefContext().getString(strId),
                Toast.LENGTH_SHORT).show();
    }

    private final void displayMessage(String str) {
        Toast.makeText(getPrefContext(), str, Toast.LENGTH_SHORT).show();
    }

    private String formatPpu(String ppu,boolean down) {
		if (down) {
			// if ES,PT,RU
			if (specialLanguage()) {
				ppu = ppu.replace(".", "");
				ppu = ppu.replace(",", ".");
			} else {
				ppu = ppu.replace(",", "");
			}
		}
        Double tempPpu = Double.valueOf(ppu);
        String temp = String.format("%.4f", tempPpu);
        temp = temp.replace(",",".");
        int location = temp.indexOf(".");
        for (int j = 4; j >= 1; j--) {
            if (temp.charAt(location + j) == '0') {
                temp = temp.substring(0, temp.length() - 1);
            } else {
        		if (!down) {
        			// if ES,PT,RU
        			if (specialLanguage()) {
        				temp = temp.replace(".", ",");
        			}
        		}
                return temp;
            }
        }
        if (temp.charAt(temp.length() - 1) == '.')
            temp = temp.substring(0, temp.length() - 1);
        // if ES,PT,RU,then repalce "."  to ","
		if (!down) {
			// if ES,PT,RU
			if (specialLanguage()) {
				temp = temp.replace(".", ",");
			}
		}
        return temp;
    }
    
    private  void gotoUnlockPin2Screen(){
        Intent intent = new Intent();
        intent.putExtra("pin2", true);
        intent.setClass(getPrefContext(), ChangeIccPinScreen.class);
        if (CallSettings.isMultipleSim())
        {
        	intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
        }
        getPrefContext().startActivity(intent);
    }
    
    private static class CallCostKeyListener extends DialerKeyListener {
        private static CallCostKeyListener keyListener;
        /**
         * The characters that are used.
         * 
         * @see KeyEvent#getMatch
         * @see #getAcceptedChars
         */
        public static final char[] CHARACTERS = new char[] { '0', '1', '2',
                '3', '4', '5', '6', '7', '8', '9', '.', ','};

        @Override
        protected char[] getAcceptedChars() {
            return CHARACTERS;
        }

        public static CallCostKeyListener getInstance() {
            if (keyListener == null) {
                keyListener = new CallCostKeyListener();
            }
            return keyListener;
        }
    }
    
    private boolean specialLanguage(){
    	String str = Locale.getDefault().getCountry();
    	if( str.equals(COUNTRY_CODE_BR) || str.equals(COUNTRY_CODE_RU) || str.equals(COUNTRY_CODE_ES) )
    		return true;
		else
			return false;
    }
    
    private String spiltPppu(String ppu){
    	String point;
    	String split;
    	if(specialLanguage()){
    		point = ",";
    		split = ".";
    	}else{
    		point = ".";
    		split = ",";
    	}
    	int location=ppu.indexOf(point);
    	if(location == -1){
    		location = ppu.length();
    	}
    	for(int i = location-1;i-3>=0;){
    		i = i-3;
    		String tail = ppu.substring(i+1,ppu.length());
    		String head = ppu.substring(0,i+1);
    		ppu = head + split + tail;
    	}
    	return ppu;
    }
}
