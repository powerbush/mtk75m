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
 * Copyright (C) 2007 The Android Open Source Project
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

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.SystemProperties;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import android.content.DialogInterface;
import android.widget.Button;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;


/**
 * FDN List UI for the Phone app.
 */
public class FdnList extends ADNList {
    private static final int MENU_ADD = 1;
    private static final int MENU_EDIT = 2;
    private static final int MENU_DELETE = 3;

    private static final String INTENT_EXTRA_INDEX = "index";
    private static final String INTENT_EXTRA_NAME = "name";
    private static final String INTENT_EXTRA_NUMBER = "number";
    private static final String INTENT_EXTRA_ADD = "addcontact";

    @Override
    protected Uri resolveIntent() {
        Intent intent = getIntent();
        if (mSimId == Phone.GEMINI_SIM_1) {
            intent.setData(Uri.parse("content://icc/fdn1"));
        } else if (mSimId == Phone.GEMINI_SIM_2) {
            intent.setData(Uri.parse("content://icc/fdn2"));
        } else {
            intent.setData(Uri.parse("content://icc/fdn"));
        }
        return intent.getData();
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mSimId = getIntent().getIntExtra(Phone.GEMINI_SIM_ID_KEY, -1);
        Log.i("FdnList", "Sim id " + mSimId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        Resources r = getResources();

        // Added the icons to the context menu
		menu.add(0, MENU_ADD, 0, r.getString(R.string.menu_add)).setIcon(
				android.R.drawable.ic_menu_add);
		menu.add(0, MENU_EDIT, 0, r.getString(R.string.menu_edit)).setIcon(
				android.R.drawable.ic_menu_edit);
		menu.add(0, MENU_DELETE, 0, r.getString(R.string.menu_delete)).setIcon(
				android.R.drawable.ic_menu_delete);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean hasSelection = (getSelectedItemPosition() >= 0);

        menu.findItem(MENU_ADD).setVisible(true);
        menu.findItem(MENU_EDIT).setVisible(hasSelection);
        menu.findItem(MENU_DELETE).setVisible(hasSelection);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ADD:
                addContact();
                return true;

            case MENU_EDIT:
                editSelected();
                return true;

            case MENU_DELETE:
                deleteSelected();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // TODO: is this what we really want?
        editSelected(position);
    }

    private void addContact() {
        // if we don't put extras "name" when starting this activity, then
        // EditFdnContactScreen treats it like add contact.
        ListView list = this.getListView();
        if(list.getCount() < 10){
            Intent intent = new Intent();
            intent.setClass(this, EditFdnContactScreen.class);
            intent.putExtra(INTENT_EXTRA_ADD, true);
            intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            startActivity(intent);
        } else {
            new AlertDialog.Builder(FdnList.this)
            .setMessage(R.string.fdn_contact_max)
            .setPositiveButton(
                R.string.ok,
                new DialogInterface.OnClickListener() {         
                    public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub        
                    }
                }
            )   
            .show();    
        }
    }

    /**
	 * Overloaded to call editSelected with the current selection by default.
	 * This method may have a problem with touch UI since touch UI does not
	 * really have a concept of "selected" items.
     */
    private void editSelected() {
        editSelected(getSelectedItemPosition());
    }

    /**
     * Edit the item at the selected position in the list.
     */
    private void editSelected(int position) {
        if (mCursor.moveToPosition(position)) {
        	String index = mCursor.getString(INDEX_COLUMN);
            String name = mCursor.getString(NAME_COLUMN);
            String number = mCursor.getString(NUMBER_COLUMN);

            Intent intent = new Intent();
            intent.setClass(this, EditFdnContactScreen.class);
            intent.putExtra(INTENT_EXTRA_INDEX, index);
            intent.putExtra(INTENT_EXTRA_NAME, name);
            intent.putExtra(INTENT_EXTRA_NUMBER, number);
            intent.putExtra(INTENT_EXTRA_ADD, false);
            intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            startActivity(intent);
        }
    }

    private void deleteSelected() {
        if (mCursor.moveToPosition(getSelectedItemPosition())) {
        	String index = mCursor.getString(INDEX_COLUMN);
            String name = mCursor.getString(NAME_COLUMN);
            String number = mCursor.getString(NUMBER_COLUMN);

            Intent intent = new Intent();
            intent.setClass(this, DeleteFdnContactScreen.class);
            intent.putExtra(INTENT_EXTRA_INDEX, index);
            intent.putExtra(INTENT_EXTRA_NAME, name);
            intent.putExtra(INTENT_EXTRA_NUMBER, number);
            intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            startActivity(intent);
        }
    }

    private int getRetryPin2() {
        if (mSimId == Phone.GEMINI_SIM_2) {
            return SystemProperties.getInt("gsm.sim.retry.pin2.2", -1);
        }
        return SystemProperties.getInt("gsm.sim.retry.pin2", -1);
    }
	@Override
	protected void onResume() {
		super.onResume();
		checkPhoneBookState();
		if (getRetryPin2() == 0) {
			finish();
		}
	}
	
	//if Phone book is busy ,then finish this activity,else go on
    private void checkPhoneBookState(){
    	Phone phone=PhoneFactory.getDefaultPhone();
    	/*boolean isPhoneBookReady=false;
         //TODO need Support Dual SIM
    	isPhoneBookReady=phone.getIccCard().isPhbReady();
    	if (!isPhoneBookReady) {
    		showTipToast(getString(R.string.error_title),getString(R.string.fdn_phone_book_busy));
			finish();
    	}*/
    	boolean isPhoneBookReady=false;
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
        	isPhoneBookReady = ((GeminiPhone)phone).getIccCardGemini(mSimId).isPhbReady();
        } else {
    	isPhoneBookReady=phone.getIccCard().isPhbReady();
        }
    	if (!isPhoneBookReady) {
    		showTipToast(getString(R.string.error_title),getString(R.string.fdn_phone_book_busy));
			finish();
    	}
    }
    
    public void showTipToast(String title,String msg ) {
    	Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
     }
}
