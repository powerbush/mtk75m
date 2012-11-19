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

/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.mms;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.gemini.GeminiSmsManager;
import android.telephony.gsm.SmsManager;
import android.util.Log;

import com.mediatek.featureoption.FeatureOption; 
import android.widget.Toast;

public class SendSMSActivity extends Activity{
	private String TAG = "SendSMSActivity";
	private mServiceReceiver mReceiver01;
	
	private String SMS_SEND_ACTION = "SMS_SEND_ACTION";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		int simId = getIntent().getIntExtra("simId", -1);
		String number = (String)getIntent().getExtra("number","");
		String message = (String)getIntent().getExtra("message","");
		Log.i(TAG,"simID:number:message = " + simId + ":"+number + ":" + message);
		Intent itSend = new Intent(SMS_SEND_ACTION);
		
		IntentFilter mFilter01;
		mFilter01 = new IntentFilter(SMS_SEND_ACTION);
		mReceiver01 = new mServiceReceiver();
		registerReceiver(mReceiver01, mFilter01);
		
		if(FeatureOption.MTK_GEMINI_SUPPORT == true){
			if(number.length()>0)
				GeminiSmsManager.sendTextMessageGemini(number, null, message, simId, PendingIntent.getBroadcast(this, 0, itSend, 0), null);
    	}else{
    		
    		SmsManager smsManager = SmsManager.getDefault();
    		if(number.length()>0)
    			smsManager.sendTextMessage(number, null, message, PendingIntent.getBroadcast(this, 0, itSend, 0), null);
    		
    	}
//		this.setVisible(false);
		this.moveTaskToBack(true);
		
//		this.finish();
	}
	
	public class mServiceReceiver extends BroadcastReceiver
	  {

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			// TODO Auto-generated method stub
			
			switch(getResultCode()){
				case Activity.RESULT_OK:
					Toast.makeText(SendSMSActivity.this, R.string.strOk, Toast.LENGTH_LONG).show();
					break;
				default:
					Toast.makeText(SendSMSActivity.this, R.string.strFail, Toast.LENGTH_LONG).show();
					
			}
			SendSMSActivity.this.finish();
			
		}
	  }

}
