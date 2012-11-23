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

/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.phone;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;   

import com.mediatek.xlog.Xlog;
import com.android.internal.telephony.Phone;
/**
 * Pin2 entry screen.
 */
public class GetPin2Screen extends Activity {
    private static final String LOG_TAG = "Settings/"+PhoneApp.LOG_TAG;
    private static final int MIN_PIN_LENGTH = 4;
    private static final int MAX_PIN_LENGTH = 8;
	private static final int GET_PIN_RETRY_EMPTY = -1;
    private final BroadcastReceiver mReceiver = new GetPin2ScreenBroadcastReceiver();

    private EditText mPin2Field;
    private TextView mPin2Title;
    private TextView mPin2RetryLabel;
    private TextView mPin2InvalidInfoLabel;
    private Button mButton;
    
    private int mSimId;
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mSimId = getIntent().getIntExtra(Phone.GEMINI_SIM_ID_KEY, -1);
        setContentView(R.layout.get_pin2_screen);
       
        setupView();
        IntentFilter intentFilter = new IntentFilter(
                Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
        if (mPin2Field != null) {
        mPin2Field.addTextChangedListener(new TextWatcher(){
            CharSequence tempStr;
            int startPos;
            int endPos;
            public void afterTextChanged(Editable s) {
               startPos = mPin2Field.getSelectionStart();
               endPos =  mPin2Field.getSelectionEnd();
               if(tempStr.length()>MAX_PIN_LENGTH){
                     s.delete(startPos-1,endPos);
                     mPin2Field.setText(s);
                     mPin2Field.setSelection(s.length());
   			      }              
            }
           public void beforeTextChanged(CharSequence s, int start, int count,int after) {
              tempStr=s;
           }
           public void onTextChanged(CharSequence s, int start, int before,int count) {
           }
           } );
    }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPin2Field.requestFocus();
        if (getPin2RetryNumber() == 0) {
            finish();
        } else {
            mPin2RetryLabel.setText(getRetryPin2());
        }
    }
    /**
     * Reflect the changes in the layout that force the user to open
     * the keyboard. 
     */
    private void setupView() {
        mPin2Field = (EditText) findViewById(R.id.pin);
        if (mPin2Field != null) {
            mPin2Field.setKeyListener(DigitsKeyListener.getInstance());
            //MTK allow the edit text have the cursor
            //mPin2Field.setMovementMethod(null);
            
            //mPin2Field.setOnClickListener(mClicked);
            
        }
        mPin2Title = (TextView) findViewById(R.id.get_pin2_title);
        if (mPin2Title != null){
            mPin2Title.append(getString(R.string.pin_length_indicate));
        }
        mPin2RetryLabel = (TextView) findViewById(R.id.pin2_retry_info_label);
        if (mPin2RetryLabel != null){
            mPin2RetryLabel.setText(getRetryPin2());
        }
        mPin2InvalidInfoLabel = (TextView) findViewById(R.id.pin2_invalid_info_label);
        mButton = (Button) findViewById(R.id.button_get_pin2_ok);
        if (mButton != null){
        	mButton.setOnClickListener(mClicked);
        }
        
    }
	private int getPin2RetryNumber() {
//		return SystemProperties.getInt("gsm.sim.retry.pin2",
//				GET_PIN_RETRY_EMPTY);
		if (mSimId == Phone.GEMINI_SIM_2) {
            return SystemProperties.getInt("gsm.sim.retry.pin2.2", GET_PIN_RETRY_EMPTY);
        }
        return SystemProperties.getInt("gsm.sim.retry.pin2", GET_PIN_RETRY_EMPTY);
	}
	private String getRetryPin2() {
		int retryCount = getPin2RetryNumber();
		switch (retryCount) {
		case GET_PIN_RETRY_EMPTY:
			return " ";
		case 1:
			return getString(R.string.one_retry_left);
		default:
			return getString(R.string.retries_left,retryCount);
		}
	}

    private String getPin2() {
        return mPin2Field.getText().toString();
    }

    private void returnResult() {
        Bundle map = new Bundle();
        map.putString("pin2", getPin2());

        Intent intent = getIntent();
        Uri uri = intent.getData();

        Intent action = new Intent();
        if (uri != null) action.setAction(uri.toString());
        setResult(RESULT_OK, action.putExtras(map));
        finish();
    }

    private boolean invalidatePin(String pin) {
        // check validity
        if (pin == null || pin.length() < MIN_PIN_LENGTH || pin.length() > MAX_PIN_LENGTH) {
            return true;
        } else {
            return false;
        }
    }

    private Button.OnClickListener mClicked = new Button.OnClickListener() {
        public void onClick(View v) {
            if (invalidatePin(mPin2Field.getText().toString())) {
                if (mPin2InvalidInfoLabel != null) {
                    mPin2InvalidInfoLabel.setVisibility(View.VISIBLE);
                }
                mPin2Field.setText("");
                return;
            }

            returnResult();
        }
    };

    private void log(String msg) {
        Xlog.d(LOG_TAG, "[GetPin2] " + msg);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private class GetPin2ScreenBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                finish();
            }
        }
    }
}
