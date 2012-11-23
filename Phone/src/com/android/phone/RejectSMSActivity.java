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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceScreen;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class RejectSMSActivity extends Activity implements OnItemClickListener
{
    private static final String LOG_TAG = "RejectSMSActivity";
	private ListView mSMSList;
    private ArrayAdapter<String> mMessageAdapter;
    private List<String> mList;
    private String number;
    private int simId;
    private Phone mPhone;
    private static final String REJECT_SMS_PREFS_NAME  = "reject_message";
    String[] keys= {
         	"message1","message2","message3","message4","message5",
         	"message6","message7","message8","message9","message10"
        };
    
	private AlertDialog newDialog;
    private static final int MAX_WORDS = 140;
    private boolean eidtBeforeSending;
    

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		int flags = WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
		getWindow().addFlags(flags);
		
		number = (String) getIntent().getExtra("number","");
		simId = (Integer) getIntent().getExtra("simId",-1);
		mPhone = PhoneApp.getInstance().phone;
		
		setContentView(R.layout.reject_sms_list);
		mSMSList = (ListView)findViewById(R.id.sms_list);
		SharedPreferences setting = getSharedPreferences(REJECT_SMS_PREFS_NAME,Context.MODE_PRIVATE);
        
		if(!setting.contains(keys[0])){
        	initSetting();
        }
		SharedPreferences sp = PhoneApp.getInstance().getApplicationContext()
			.getSharedPreferences("com.android.phone_preferences" , Context.MODE_PRIVATE);
		eidtBeforeSending = sp.getBoolean("edit_message_before_sending", false);
		Log.v(LOG_TAG,"eidtBeforeSending :" + eidtBeforeSending);
        
        mList = new ArrayList<String>();
        for(String key:keys){
        	String message = setting.getString(key,null);
        	if(message!=null && message.length()>0)mList.add(message);
        }
        mMessageAdapter = new ArrayAdapter<String>(this, R.layout.message, mList);
        mSMSList.setAdapter(mMessageAdapter);
        mSMSList.setOnItemClickListener(this);
        
	}

	private void initSetting(){
		SharedPreferences setting = getSharedPreferences(REJECT_SMS_PREFS_NAME,Context.MODE_PRIVATE);
		Editor mEditor = setting.edit();
		mEditor.clear();
		mEditor.putString(keys[0], "I am busy now, I will call you later.");
		mEditor.commit();
	}
	
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// TODO Auto-generated method stub
		String message = mList.get(position);
		if(eidtBeforeSending){
			Intent i = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("sms", number, null));
			i.putExtra("sms_body",message);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
			startActivity(i);
			
			this.finish();
		}else{
			Intent i = new Intent("android.intent.action.SENDSMS");//
			i.putExtra("number", number);
			i.putExtra("simId",simId);
			i.putExtra("message",message);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
			startActivity(i);
			this.finish();
		}
		
	}
	

}
