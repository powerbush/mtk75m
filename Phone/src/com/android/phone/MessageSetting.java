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
import java.util.Set;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.CallLog.Calls;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.telephony.SmsManager;
import android.telephony.gemini.GeminiSmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.mediatek.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;

public class MessageSetting extends ListActivity
        {
    private static final String LOG_TAG = "Settings/Callsettings";
    private final boolean DBG = true; 
    private static final int MENU_ADD_MESSAGE = Menu.FIRST;
    private static final int MENU_DELETE_MESSAGE = Menu.FIRST + 1;
    private static final int MENU_EDIT_MESSAGE = Menu.FIRST + 2;
    
    private static final int MAX_ITEMS = 10;
    private static final int MAX_WORDS = 140;
    
    private static final String PREFS_NAME  = "reject_message";

    private AlertDialog  mAddMessageDialog;
    private ArrayAdapter<String> mMessageAdapter;
    private String[] mMessages;
    private List<String> mList;
    private int mPosition;

    private String[] keys= {
    	"message1","message2","message3","message4","message5",
    	"message6","message7","message8","message9","message10"
    };
    
    public static final int DEFAULT_SIM = 2; /* 0: SIM1, 1: SIM2 */
    private int mSimId = DEFAULT_SIM;

    @Override
    protected void onCreate(Bundle icicle) {
		Xlog.v(LOG_TAG,"[MessageSetting]onCreate start...");
        super.onCreate(icicle);
        getListView().setOnCreateContextMenuListener(this);
        
        if (FeatureOption.MTK_GEMINI_SUPPORT == true)
        {
            PhoneApp app = PhoneApp.getInstance();
            mSimId = getIntent().getIntExtra(app.phone.GEMINI_SIM_ID_KEY, -1);
        }
        Xlog.d(LOG_TAG, "[MessageSetting]Sim Id : " + mSimId);
        
        mList = new ArrayList<String>();
        SharedPreferences setting = getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE);
        if(!setting.contains(keys[0])){
        	initSetting();
        }
        for(String key:keys){
        	String message = setting.getString(key,null);
        	if(message!=null)mList.add(message);
        }
        
        mMessages = new String[mList.size()];
        for(int i = 0; i < mList.size(); i ++){
        	mMessages[i] = mList.get(i); 
        }
        mMessageAdapter = new ArrayAdapter<String>(this,R.layout.message, mMessages);
        this.setListAdapter(mMessageAdapter);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Xlog.v(LOG_TAG,"[MessageSetting]onCreateOptionsMenu start...");
		menu.add(0,MENU_ADD_MESSAGE,0, R.string.add_message)
			.setIcon(R.drawable.phonesetting_addmessage_menu);
		return super.onCreateOptionsMenu(menu);
	}
	private AlertDialog newDialog;
	private EditText inputText;
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Xlog.v(LOG_TAG,"[MessageSetting]onOptionsItemSelected start...");
		switch (item.getItemId()) {
        case MENU_ADD_MESSAGE:
        	if(mList.size()>=MAX_ITEMS){
        		Toast.makeText(this, R.string.max_item, Toast.LENGTH_LONG).show();
        		return true;
        	}
        	
        	LayoutInflater inflater = (LayoutInflater) getSystemService(
                     Context.LAYOUT_INFLATER_SERVICE);
        	View dialogView = inflater.inflate(R.xml.reject_add_message_dialog, null);
    		TextView mTitle = (TextView) dialogView.findViewById(R.id.title);
    		mTitle.setText(R.string.add_message);
//            final EditText 
    		inputText = (EditText) dialogView.findViewById(R.id.message_field);
            inputText.addTextChangedListener(new MYTextWatcher());
            
			OnClickListener mListener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					String str = inputText.getText().toString();
					if(str == null || str.length() == 0) return;
					mList.add(str);
					saveSetting();
					mMessages = new String[mList.size()];
			        for(int i = 0; i < mList.size(); i ++){
			        	mMessages[i] = mList.get(i); 
			        }
			        
			        mMessageAdapter = new ArrayAdapter<String>(MessageSetting.this,R.layout.message, mMessages);
					MessageSetting.this.setListAdapter(mMessageAdapter);
				}
			};
			
	        SharedPreferences setting = getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE);
			newDialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setPositiveButton(R.string.ok, mListener)
                    .setNegativeButton(R.string.cancel, null)
                    .setCancelable(true)
                    .create();

            Window window = newDialog.getWindow();
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE |
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            newDialog.getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            newDialog.show();
			Editable str = inputText.getText();
			if(str == null || str.length() == 0 ){
				newDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
			}

            return true;
    }
		return super.onOptionsItemSelected(item);
	}
	private class MYTextWatcher implements TextWatcher {

		public synchronized void afterTextChanged(Editable arg0) {
			try{
				String str = arg0.toString();
			
				if(str == null || str.length() == 0) {
					newDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
				} else {   //have charactor input
				    int strLength = str.length();
                    int strGbkLength = str.getBytes("GBK").length;
                    Xlog.v(LOG_TAG,"[MessageSetting]afterTextChanged ...str_length=" + strLength);
                    Xlog.v(LOG_TAG,"[MessageSetting]afterTextChanged ...str_getBytes=" + strGbkLength);
				
				   if(strLength == strGbkLength || strLength*2 == strGbkLength) { //only  include  english or chinese charactor
				     if (strGbkLength > MAX_WORDS) {
                         newDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                     } else {
                         newDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                     }

                   } else { //include chinese and english charactor
                       if (strLength*2 > MAX_WORDS) {
					newDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
				}else{
					newDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
				}
                   }
                }
			}catch(UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void beforeTextChanged(CharSequence s, int start,
               int count, int after){
			
		}
		public void onTextChanged(CharSequence s, int start, int before, int count){}

    }

	@Override
	protected void onStop() {
		Xlog.v(LOG_TAG,"[MessageSetting]onStop start...");
		// TODO Auto-generated method stub
		saveSetting();
		super.onStop();
	}
	
	private void saveSetting(){
		SharedPreferences setting = getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE);
		Editor mEditor = setting.edit();
		mEditor.clear();
		for(int i = 0; i < mList.size(); i ++){
			mEditor.putString(keys[i], mList.get(i));
		}
		mEditor.commit();
	}

	private void initSetting(){
		SharedPreferences setting = getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE);
		Editor mEditor = setting.edit();
		mEditor.clear();
		mEditor.putString(keys[0], "I am busy now, I will call you later.");
		mEditor.commit();
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		Xlog.v(LOG_TAG,"[MessageSetting]onCreateContextMenu start...");
		AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Xlog.e(LOG_TAG, "[MessageSetting]bad menuInfo", e);
            return;
        }
        mPosition = info.position;       
        Xlog.v(LOG_TAG,"[MessageSetting]mPostion:" + mPosition);
        menu.setHeaderTitle(R.string.message);
		menu.add(0, MENU_EDIT_MESSAGE, 0, R.string.edit_message);
		if(mList.size() > 1)menu.add(0, MENU_DELETE_MESSAGE, 0, R.string.delete_message);
		Xlog.v(LOG_TAG,"[MessageSetting]onCreateContextMenu end...");
		return;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Xlog.v(LOG_TAG,"[MessageSetting]onContextItemSelected start...");
		switch (item.getItemId()) {
        	case MENU_DELETE_MESSAGE: {
        		mList.remove(mPosition);
        		saveSetting();
        		mMessages = new String[mList.size()];
        		for(int i = 0; i < mList.size(); i ++){
        			mMessages[i] = mList.get(i); 
        		}
        		mMessageAdapter = new ArrayAdapter<String>(this,R.layout.message, mMessages);
        		this.setListAdapter(mMessageAdapter);
        		Xlog.v(LOG_TAG,"[MessageSetting]onContextItemSelected end...");
        		return true;
        	}
        	case MENU_EDIT_MESSAGE: {
        		LayoutInflater inflater = (LayoutInflater) getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
        		View dialogView = inflater.inflate(R.xml.reject_add_message_dialog, null);
        		TextView mTitle = (TextView) dialogView.findViewById(R.id.title);
        		mTitle.setText(R.string.edit_message);
//        		final EditText 
        		inputText = (EditText) dialogView.findViewById(R.id.message_field);
        		String strText = mList.get(mPosition);
        		
        		if(strText != null) {
            		inputText.setText(mList.get(mPosition));
            		inputText.setSelection(strText.length());
        		}
        		

                inputText.addTextChangedListener(new MYTextWatcher());
                
   				OnClickListener mListener = new DialogInterface.OnClickListener() {
   					public void onClick(DialogInterface dialog, int which) {
   						String str = inputText.getText().toString();
   						if(str == null || str.length() == 0){
   							mList.remove(mPosition);
   						}
   						else mList.set(mPosition, str);
   						saveSetting();
   						mMessages = new String[mList.size()];
   						for(int i = 0; i < mList.size(); i ++){
   							mMessages[i] = mList.get(i); 
   						}
   			        
   						mMessageAdapter = new ArrayAdapter<String>(MessageSetting.this,R.layout.message, mMessages);
   						MessageSetting.this.setListAdapter(mMessageAdapter);
   					}
   				};
   			
   				SharedPreferences setting = getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE);
   				newDialog = new AlertDialog.Builder(this)
                       .setView(dialogView)
                       .setPositiveButton(R.string.ok, mListener)
                       .setNegativeButton(R.string.cancel, null)
                       .setCancelable(true)
                       .create();
   	            Window window = newDialog.getWindow();
   	            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE |
   	                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
               newDialog.getWindow().addFlags(
                       WindowManager.LayoutParams.FLAG_DIM_BEHIND);

               newDialog.show();
        		return true;
        	}
		}
		return super.onContextItemSelected(item);
	}

}
