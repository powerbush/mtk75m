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
import com.android.internal.telephony.cdma.TtyIntent;
import com.android.phone.R;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import com.mediatek.xlog.Xlog;

//import com.mediatek.audioprofile.AudioProfileImpl;
//import com.mediatek.audioprofile.AudioProfileManagerImpl;

public class TtySetting extends ListActivity {
	private static final String TAG = "Settings/AudioProfilesSetting";
	
	private static final String PREFERENCES = "AudioProfileSettings";
	private static final int RESTOR_TO_DEFAULT = 0;
	private LayoutInflater mFlater;
	private SharedPreferences mPrefs;
	private Handler mHandler;
	private ProfileAdapter mProfileAdapter;
	static String LOG_TAG = "TtySettings";
	private int mPos;
    
	private class ProfileAdapter extends BaseAdapter{

		String[] ttyTitle = getResources().getStringArray(R.array.tty_mode_entries);
		public int selectedPos = mPos;
		
		public int getCount() {
			return ttyTitle.length;
		}

		public Object getItem(int position) {
			return ttyTitle[position];
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(final int position, View convertView, ViewGroup parent) {
			View view;
			if (convertView != null){
				view = convertView;
			} else {
				view = mFlater.inflate(R.layout.tty_profile_item, parent, false);
				
			}
			final RadioButton btn = (RadioButton)view.findViewById(R.id.radiobutton);
			
			btn.setFocusable(false);
			btn.setFocusableInTouchMode(false);
			btn.setClickable(false);
			
			TextView txt = (TextView)view.findViewById(R.id.profiles_text);
			txt.setSelected(false);
			txt.setText(ttyTitle[position]);
			
			if(mPos == position){
				Xlog.i(TAG,""+mPos+" "+position);
				btn.setChecked(true);
			}else{
				btn.setChecked(false);
			}
			return view;
		}
		
		public int getCurrentSelected()
		{
		    return selectedPos;
	}
	
		public void setCurrentSelected(int pos)
		{
		    selectedPos = pos;
		}
	}
	
	protected void onCreate (Bundle icicle){
		super.onCreate(icicle);
		Xlog.i(TAG,"onCreate");
		mFlater = LayoutInflater.from(this);	
		int settingsTtyMode = android.provider.Settings.Secure.getInt(
                getContentResolver(),
                android.provider.Settings.Secure.PREFERRED_TTY_MODE, 0);
        mPos = settingsTtyMode;
		setListAdapter(new ProfileAdapter());
		Xlog.i(TAG, "set ListView");

	}

	protected void onResume(){
        super.onResume();
        setListAdapter(new ProfileAdapter());
    }
	    
	protected void onListItemClick(ListView l, View v, int position, long id) {
	       
	    ProfileAdapter adapter = (ProfileAdapter)getListAdapter();
	    //New selected
	    View view = getListView().getChildAt(position);
	    RadioButton btn = (RadioButton)view.findViewById(R.id.radiobutton);

	    int selectedPos = adapter.getCurrentSelected();
	    View lastView = getListView().getChildAt(selectedPos);
	    RadioButton lastBtn = (RadioButton)lastView.findViewById(R.id.radiobutton);
		
	    if (selectedPos != position)
	    {
	        boolean bSet = android.provider.Settings.Secure.putInt(
	                    getContentResolver(),
	                    android.provider.Settings.Secure.PREFERRED_TTY_MODE, position);
	        //Broadcast the TTY mode changed
	        Intent ttyModeChanged = new Intent(TtyIntent.TTY_PREFERRED_MODE_CHANGE_ACTION);
	        ttyModeChanged.putExtra(TtyIntent.TTY_PREFFERED_MODE, position);
	        sendBroadcast(ttyModeChanged);
	        lastBtn.setChecked(false);
	        btn.setChecked(true);
	        adapter.setCurrentSelected(position);	  
	        mPos = position;
	}

	    Xlog.i(TAG, v.toString() + " : In onListItemClick");
    }
}
