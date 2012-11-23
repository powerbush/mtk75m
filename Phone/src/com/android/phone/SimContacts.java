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

import android.accounts.Account;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.os.PowerManager; 
import android.widget.Toast;

// mtk80909 start
import com.android.internal.telephony.ITelephony;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.content.Context;
// mtk80909 end


import java.util.ArrayList;

/**
 * SIM Address Book UI for the Phone app.
 */
public class SimContacts extends ADNList {
    private static final String LOG_TAG = "SimContacts";

    static final ContentValues sEmptyContentValues = new ContentValues();

    private static final int MENU_IMPORT_ONE = 1;
    private static final int MENU_IMPORT_ALL = 2;
    private ProgressDialog mProgressDialog;

    private Account mAccount;
	private ImportAllSimContactsThread mThread;
	private ImportOneSimContactsThread mOneSimThread;//mtk80794 for change feature ALPS00120835
	private int mPosition;//mtk80794 for change feature ALPS00120835

   private class ImportAllSimContactsThread extends Thread
            implements OnCancelListener, OnClickListener {
        public boolean mCanceled = false;
        private PowerManager.WakeLock mWakeLock;
		
        public ImportAllSimContactsThread(int simId) {
            super("ImportAllSimContactsThread");
            mSimId = simId;
			Context context = SimContacts.this;
			PowerManager powerManager = (PowerManager)context.getSystemService(
                    Context.POWER_SERVICE);
            mWakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_DIM_WAKE_LOCK |
                    PowerManager.ON_AFTER_RELEASE, LOG_TAG);
        }

		@Override
        public void finalize() {
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
				
        @Override
        public void run() {
            final ContentValues emptyContentValues = new ContentValues();
            final ContentResolver resolver = getContentResolver();
			mWakeLock.acquire();
            if ( mCursor != null ){
                mCursor.moveToPosition(-1);
                while (!mCanceled && mCursor.moveToNext()) {
                    actuallyImportOneSimContact(mCursor, resolver, mAccount, mSimId);
                    mProgressDialog.incrementProgressBy(1);
                }
            }
			mWakeLock.release();
            mProgressDialog.dismiss();
            finish();  
        }

        public void onCancel(DialogInterface dialog) {
            mCanceled = true;
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_NEGATIVE) {
                mCanceled = true;
                mProgressDialog.dismiss();
            } else {
                Log.e(LOG_TAG, "Unknown button event has come: " + dialog.toString());
            }
        }
    }
    
    private class ImportOneSimContactsThread extends Thread
    implements OnCancelListener, OnClickListener {//mtk80794 for change feature ALPS00120835
    public boolean mCanceled = false;

	public ImportOneSimContactsThread(int position, int indicate) {
	    super("ImportOneSimContactsThread");
	    mPosition = position;
	    switch (indicate) {
        case RawContacts.INDICATE_SIM:
        case RawContacts.INDICATE_USIM:
        case RawContacts.INDICATE_SIM1:
        case RawContacts.INDICATE_USIM1:
        	mSimId = com.android.internal.telephony.Phone.GEMINI_SIM_1;
        	break;
        case RawContacts.INDICATE_SIM2:
        case RawContacts.INDICATE_USIM2:
        	mSimId = com.android.internal.telephony.Phone.GEMINI_SIM_2;
        	break;
        }
	}

	@Override
	public void run() {
	    final ContentValues emptyContentValues = new ContentValues();
	    final ContentResolver resolver = getContentResolver();
	    if ( mCursor != null ){
	        mCursor.moveToPosition(-1);
	        if (mCursor.moveToPosition(mPosition)) {
	            actuallyImportOneSimContact(mCursor, resolver, mAccount, mSimId);
//	            mProgressDialog.incrementProgressBy(1);
	            SimContacts.this.runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(SimContacts.this,
								R.string.import_sim_contact_success, Toast.LENGTH_SHORT).show();
					}
				});
	        }
	        
	    }
//	    finish();
	}
	
	public void onCancel(DialogInterface dialog) {
	    mCanceled = true;
	}
	
	public void onClick(DialogInterface dialog, int which) {
	    if (which == DialogInterface.BUTTON_NEGATIVE) {
	        mCanceled = true;
            } else {
                Log.e(LOG_TAG, "Unknown button event has come: " + dialog.toString());
            }
        }
    }

    // From HardCodedSources.java in Contacts app.
    // TODO: fix this.
    private static final String ACCOUNT_TYPE_GOOGLE = "com.google";
    private static final String GOOGLE_MY_CONTACTS_GROUP = "System Group: My Contacts";

    private static void actuallyImportOneSimContact(
            final Cursor cursor, final ContentResolver resolver, Account account, int simId) {
        final NamePhoneTypePair namePhoneTypePair =
            new NamePhoneTypePair(cursor.getString(NAME_COLUMN));
        final String name = namePhoneTypePair.name;
        final int phoneType = namePhoneTypePair.phoneType;
        final String phoneNumber = cursor.getString(NUMBER_COLUMN); // mtk80909
        final String emailAddresses = cursor.getString(EMAIL_COLUMN);
        final String[] emailAddressArray;
        String additionalNumber = null;
        boolean isSimUsim = false;
        boolean isSim1Usim = false;
        boolean isSim2Usim = false;
        additionalNumber = cursor.getString(ADDITIONAL_NUMBER_COLUMN);	        
        final String email = cursor.getString(EMAIL_COLUMN);
        if (!TextUtils.isEmpty(emailAddresses)) {
            emailAddressArray = emailAddresses.split(",");
        } else {
            emailAddressArray = null;
        }
        final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
        if (iTel == null) {
        	Log.i(TAG,"iTel is " + iTel);
        	return;
        }

        final ArrayList<ContentProviderOperation> operationList =
            new ArrayList<ContentProviderOperation>();
        ContentProviderOperation.Builder builder =
            ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
        String myGroupsId = null;
        if (account != null) {
            builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
            builder.withValue(RawContacts.ACCOUNT_TYPE, account.type);

            // TODO: temporal fix for "My Groups" issue. Need to be refactored.
            if (ACCOUNT_TYPE_GOOGLE.equals(account.type)) {
                final Cursor tmpCursor = resolver.query(Groups.CONTENT_URI, new String[] {
                        Groups.SOURCE_ID },
                        Groups.TITLE + "=?", new String[] {
                        GOOGLE_MY_CONTACTS_GROUP }, null);
                try {
                    if (tmpCursor != null && tmpCursor.moveToFirst()) {
                        myGroupsId = tmpCursor.getString(0);
                    }
                } finally {
                    if (tmpCursor != null) {
                        tmpCursor.close();
                    }
                }
            }
        } else {
            builder.withValues(sEmptyContentValues);
        }
        int simType = -1;
        try {
        if (com.mediatek.featureoption.FeatureOption.MTK_GEMINI_SUPPORT) {
        	if (simId == com.android.internal.telephony.Phone.GEMINI_SIM_2 && iTel.getIccCardTypeGemini(simId).equals("USIM")) {
        	Log.i(TAG,"sim == 2");
        		isSim2Usim = true;	        
        	} else if (simId == com.android.internal.telephony.Phone.GEMINI_SIM_1 && iTel.getIccCardTypeGemini(simId).equals("USIM")) {
        	Log.i(TAG,"sim == 1");
        		isSim1Usim = true;
	        }
        } else {
        	Log.i(TAG,"sim == 0");
        	if (simId == 0 && iTel.getIccCardType().equals("USIM")) {
        		isSimUsim = true;
	        }
            }
        	} catch (Exception e) {
	        	Log.i(TAG,"In actuallyImportOneSimContact e.getMessage is " + e.getMessage());
	        }
        operationList.add(builder.build());

        if (!TextUtils.isEmpty(phoneNumber)) {
        builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
        builder.withValueBackReference(Phone.RAW_CONTACT_ID, 0);
        builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
        builder.withValue(Phone.TYPE, phoneType);
        if (!TextUtils.isEmpty(phoneNumber)) {
        builder.withValue(Phone.NUMBER, phoneNumber);
        } else {
        	builder.withValue(Phone.NUMBER, null);
        	builder.withValue(Data.IS_PRIMARY, 1);
            }
        operationList.add(builder.build());
        }

        if (!TextUtils.isEmpty(name)) {
        builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
        builder.withValueBackReference(StructuredName.RAW_CONTACT_ID, 0);
        builder.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
	        builder.withValue(StructuredName.GIVEN_NAME, name);		
        operationList.add(builder.build());
	    }

//        if (emailAddresses != null) {
//            for (String emailAddress : emailAddressArray) {
//                builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
//                builder.withValueBackReference(Email.RAW_CONTACT_ID, 0);
//                builder.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
//                builder.withValue(Email.TYPE, Email.TYPE_MOBILE);
//                builder.withValue(Email.DATA, emailAddress);
//                operationList.add(builder.build());
//            }
//        }

        if (myGroupsId != null) {
            builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference(GroupMembership.RAW_CONTACT_ID, 0);
            builder.withValue(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
            builder.withValue(GroupMembership.GROUP_SOURCE_ID, myGroupsId);
            operationList.add(builder.build());
        }

        //if USIM
        if (isSim1Usim || isSim2Usim) {
        	Log.i(TAG,"isSim1Usim is " + isSim1Usim + "isSim2Usim is" + isSim2Usim);
        	if (!TextUtils.isEmpty(email)) {
	        	Log.i(TAG,"In actuallyImportOneSimContact email is " + email);
//	            for (String emailAddress : emailAddressArray) {
	                builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
	                builder.withValueBackReference(Email.RAW_CONTACT_ID, 0);
	                builder.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
	                builder.withValue(Email.TYPE, Email.TYPE_MOBILE);
	                builder.withValue(Email.DATA, email);
	                operationList.add(builder.build());
//	            }
	        }
        	
        	
        	
        if (!TextUtils.isEmpty(additionalNumber)) {
            Log.i(TAG,"additionalNumber is " + additionalNumber);
        	builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
	        builder.withValueBackReference(Phone.RAW_CONTACT_ID, 0);
	        builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
	        builder.withValue(Phone.TYPE, phoneType);			        	
	        builder.withValue(Phone.NUMBER, additionalNumber);
	        builder.withValue(Data.IS_ADDITIONAL_NUMBER, 1);
	        operationList.add(builder.build());
	        }
        } 

        try {
            resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        } catch (OperationApplicationException e) {
            Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        }
    }
    
	// mtk80909 for ALPS00023212
    public static class NamePhoneTypePair {
        public String name;
        public int phoneType;
        public String phoneTypeSuffix;
        public NamePhoneTypePair(String nameWithPhoneType) {
            // Look for /W /H /M or /O at the end of the name signifying the type
            int nameLen = nameWithPhoneType.length();
            if (nameLen - 2 >= 0 && nameWithPhoneType.charAt(nameLen - 2) == '/') {
                char c = Character.toUpperCase(nameWithPhoneType.charAt(nameLen - 1));
                phoneTypeSuffix = String.valueOf(nameWithPhoneType.charAt(nameLen - 1));
                if (c == 'W') {
                    phoneType = Phone.TYPE_WORK;
                } else if (c == 'M' || c == 'O') {
                    phoneType = Phone.TYPE_MOBILE;
                } else if (c == 'H') {
                    phoneType = Phone.TYPE_HOME;
                } else {
                    phoneType = Phone.TYPE_OTHER;
                }
                name = nameWithPhoneType.substring(0, nameLen - 2);
            } else {
            	phoneTypeSuffix = "";
                phoneType = Phone.TYPE_OTHER;
                name = nameWithPhoneType;
            }
        }
    }

//    private void importOneSimContact(int position) {//mtk80794 for change feature ALPS00120835
//        final ContentResolver resolver = getContentResolver();
//        if ( mCursor != null ){
//            mCursor.moveToPosition(-1);
//            while (mCursor.moveToPosition(position)) {
//            	Log.i(LOG_TAG,"Before actuallyImportOneSimContact ");
//                actuallyImportOneSimContact(mCursor, resolver, mAccount);
////                mProgressDialog.incrementProgressBy(1);
//            }
//        } else {
//            Log.e(LOG_TAG, "Failed to move the cursor to the position \"" + position + "\"");
//        }
////        if (mProgressDialog.isShowing()) {
////            mProgressDialog.dismiss();
////        }
////        finish();  
////        if (mCursor.moveToPosition(position)) {
////            actuallyImportOneSimContact(mCursor, resolver, mAccount);
////        } 
//    }

    /* Followings are overridden methods */

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Intent intent = getIntent();
        if (intent != null) {
            final String accountName = intent.getStringExtra("account_name");
            final String accountType = intent.getStringExtra("account_type");
            if (!TextUtils.isEmpty(accountName) && !TextUtils.isEmpty(accountType)) {
                mAccount = new Account(accountName, accountType);
            }
            mIndicate = intent.getIntExtra(RawContacts.INDICATE_PHONE_SIM, -1);
            switch (mIndicate) {
            case RawContacts.INDICATE_SIM:
            case RawContacts.INDICATE_USIM:
            case RawContacts.INDICATE_SIM1:
            case RawContacts.INDICATE_USIM1:
            	mSimId = com.android.internal.telephony.Phone.GEMINI_SIM_1;
            	break;
            case RawContacts.INDICATE_SIM2:
            case RawContacts.INDICATE_USIM2:
            	mSimId = com.android.internal.telephony.Phone.GEMINI_SIM_2;
            	break;
            }
            
//            mSimId = intent.getIntExtra(com.android.internal.telephony.Phone.GEMINI_SIM_ID_KEY, -1);
        }

        registerForContextMenu(getListView());
    }

	// added by mtk80909
    @Override
    protected void onResume() {
        final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
        if (iTel == null) {
            super.onResume();
            return;
        }
        try {
            if (com.mediatek.featureoption.FeatureOption.MTK_GEMINI_SUPPORT) {
                if (!iTel.isRadioOnGemini(mSimId) 
                		|| iTel.isFDNEnabledGemini(mSimId)) {
                    mAirplaneMode = true;
                }
            } else if (!iTel.isRadioOn() || iTel.isFDNEnabled()) {
                mAirplaneMode = true;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        super.onResume();
    }

	@Override
	protected void onPause(){
		super.onPause();
		mAirplaneMode = false;
		
		if (mThread != null && mProgressDialog != null) {
        	mThread.mCanceled = true;
        	mProgressDialog.dismiss();
		}

		/*
		if (mThread != null && mProgressDialog != null){
			mThread.onCancel(mProgressDialog);
		}
		*/
    }

    @Override
    protected CursorAdapter newAdapter() {
        return new SimpleCursorAdapter(this, R.layout.sim_import_list_entry, mCursor,
                new String[] { "name" }, new int[] { android.R.id.text1 });
    }

    @Override
    protected Uri resolveIntent() {
        Intent intent = getIntent();
        if (/*mSimId == com.android.internal.telephony.Phone.GEMINI_SIM_1*/ mIndicate == RawContacts.INDICATE_SIM1) {
            intent.setData(Uri.parse("content://icc/adn1"));
        } else if (/*mSimId == com.android.internal.telephony.Phone.GEMINI_SIM_2*/mIndicate == RawContacts.INDICATE_SIM2) {
            intent.setData(Uri.parse("content://icc/adn2"));
        } else if (mIndicate == RawContacts.INDICATE_USIM) {
        	intent.setData(Uri.parse("content://icc/pbr"));
        } else if (mIndicate == RawContacts.INDICATE_USIM1) {
        	intent.setData(Uri.parse("content://icc/pbr1"));
        } else if (mIndicate == RawContacts.INDICATE_USIM2) {
        	intent.setData(Uri.parse("content://icc/pbr2"));
        } else {
        intent.setData(Uri.parse("content://icc/adn"));
        }
        if (Intent.ACTION_PICK.equals(intent.getAction())) {
            // "index" is 1-based
            mInitialSelection = intent.getIntExtra("index", 0) - 1;
        }
        return intent.getData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_IMPORT_ALL, 0, R.string.importAllSimEntries);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(MENU_IMPORT_ALL);
        if (item != null) {
            item.setVisible(mCursor != null && mCursor.getCount() > 0);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_IMPORT_ALL:
                CharSequence title = getString(R.string.importAllSimEntries);
                CharSequence message = getString(R.string.importingSimContacts);
                // mtk80909 start
                if (mSimId == com.android.internal.telephony.Phone.GEMINI_SIM_1 && mIndicate == RawContacts.INDICATE_SIM1) {
                    message = getString(R.string.importingSim1Contacts);
                } else if (mSimId == com.android.internal.telephony.Phone.GEMINI_SIM_2 && mIndicate == RawContacts.INDICATE_SIM2) {
                    message = getString(R.string.importingSim2Contacts);
                } else if (mSimId == com.android.internal.telephony.Phone.GEMINI_SIM_1 && mIndicate == RawContacts.INDICATE_USIM1) {
                    message = getString(R.string.importingUSim1Contacts);
                } else if (mSimId == com.android.internal.telephony.Phone.GEMINI_SIM_2 && mIndicate == RawContacts.INDICATE_USIM2) {
                    message = getString(R.string.importingUSim2Contacts);
                }
                
                // mtk80909 end
                mThread = new ImportAllSimContactsThread(mSimId);

                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setTitle(title);
                mProgressDialog.setMessage(message);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                        getString(R.string.cancel), mThread);
                mProgressDialog.setProgress(0);
                if ( mCursor != null )
                    mProgressDialog.setMax(mCursor.getCount());
                mProgressDialog.show();

                mThread.start();

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_IMPORT_ONE:
                ContextMenu.ContextMenuInfo menuInfo = item.getMenuInfo();
                if (menuInfo instanceof AdapterView.AdapterContextMenuInfo) {
                    int position = ((AdapterView.AdapterContextMenuInfo)menuInfo).position;
                    Log.i(TAG,"");
                    importOneSimContactDialog(position, mIndicate);     //mtk80794 for change feature ALPS00120835            
//                    importOneSimContact(position);//mtk80794 for change feature ALPS00120835
                    return true;
                }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {
        if (menuInfo instanceof AdapterView.AdapterContextMenuInfo) {
            AdapterView.AdapterContextMenuInfo itemInfo =
                    (AdapterView.AdapterContextMenuInfo) menuInfo;
            TextView textView = (TextView) itemInfo.targetView.findViewById(android.R.id.text1);
            if (textView != null) {	// modified by mtk80909 2010-10-11
            	CharSequence text = textView.getText();
            	if (TextUtils.isEmpty(text)) {
            		text = " ";
            	}
            	// text is added because if the text length is zero, 
            	// menu won't have header title.
                menu.setHeaderTitle(text);
            }
            menu.add(0, MENU_IMPORT_ONE, 0, R.string.importSimEntry);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
//    	Cursor cursor = null;
//    	try{
//	    	cursor = (Cursor) getListAdapter().getItem(position);
//	} catch(Exception e){
//		if(null != cursor) cursor.close();
//		cursor = null;
//	}
//         int indicate = -1;
//         if (cursor != null) {
//             indicate = cursor.getInt(cursor
//                     .getColumnIndexOrThrow(RawContacts.INDICATE_PHONE_SIM));
//         }
    	importOneSimContactDialog(position, mIndicate);//mtk80794 for change feature ALPS00120835
//        importOneSimContact(position);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL: {
                if (mCursor != null && mCursor.moveToPosition(getSelectedItemPosition())) {
                    String phoneNumber = mCursor.getString(NUMBER_COLUMN);
                    if (phoneNumber == null || !TextUtils.isGraphic(phoneNumber)) {
                        // There is no number entered.
                        //TODO play error sound or something...
                        return true;
                    }
                    //TODO Need support dual SIM
                    Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                            Uri.fromParts("tel", phoneNumber, null));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                          | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    startActivity(intent);
                    finish();
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void importOneSimContactDialog(int position, int indicate) {//mtk80794 for change feature ALPS00120835
    	Log.i(LOG_TAG,"In importOneSimContactDialog ");
        mOneSimThread = new ImportOneSimContactsThread(position, indicate);
        mOneSimThread.start();
        
        return;

    }
}
