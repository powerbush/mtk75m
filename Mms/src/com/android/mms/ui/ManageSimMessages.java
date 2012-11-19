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

package com.android.mms.ui;

import java.util.ArrayList;

import com.android.mms.R;
import android.database.sqlite.SqliteWrapper;
import com.android.mms.transaction.MessagingNotification;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Browser;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.Telephony.Sms;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.telephony.gemini.GeminiSmsManager;
import android.telephony.SmsMemoryStatus;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.widget.Toast;
import android.provider.Telephony.SIMInfo;
import android.content.ContentUris;
import android.os.SystemProperties;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.MmsApp;
import com.android.mms.util.Recycler;
import com.mediatek.featureoption.FeatureOption;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.xlog.Xlog;
import com.mediatek.xlog.SXlog;

/**
 * Displays a list of the SMS messages stored on the ICC.
 */
public class ManageSimMessages extends Activity
        implements View.OnCreateContextMenuListener {
    private static final int DIALOG_REFRESH = 1;        
    private static Uri ICC_URI;
    private static final String TAG = "ManageSimMessages";
    private static final int MENU_COPY_TO_PHONE_MEMORY = 0;
    private static final int MENU_DELETE_FROM_SIM = 1;
    private static final int MENU_FORWARD = 2;
    private static final int MENU_REPLY =3;
    private static final int MENU_ADD_TO_BOOKMARK      = 4;
    private static final int MENU_CALL_BACK            = 5;
    private static final int MENU_SEND_EMAIL           = 6;
    private static final int MENU_ADD_ADDRESS_TO_CONTACTS = 7;
    private static final int MENU_SEND_SMS              = 9;
    private static final int MENU_ADD_CONTACT           = 10;
    
    private static final int OPTION_MENU_DELETE_ALL = 0;

    private static final int SHOW_LIST = 0;
    private static final int SHOW_EMPTY = 1;
    private static final int SHOW_BUSY = 2;
    private int mState;
    ProgressDialog dialog;
    private static final String ALL_SMS = "999999"; 
    private static final String FOR_MULTIDELETE = "ForMultiDelete";
    private int currentSlotId = 0;

    private ContentResolver mContentResolver;
    private Cursor mCursor = null;
    private ListView mSimList;
    private TextView mMessage;
    private MessageListAdapter mListAdapter = null;
    private AsyncQueryHandler mQueryHandler = null;

    public static final int SIM_FULL_NOTIFICATION_ID = 234;
    public boolean isQuerying = false;
    public boolean isDeleting = false;    
    private boolean isInit = false;
    public static int observerCount = 0; 
    //extract telephony number ...
    private ArrayList<String> mURLs = new ArrayList<String>();
    private ContactList mContactList;
    
    private final ContentObserver simChangeObserver =
            new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfUpdate) {
        Log.e(TAG, "onChange, selfUpdate = "+ selfUpdate);
            if(!isQuerying){
            refreshMessageList();
            }else{
                if(isDeleting == false){
                    observerCount ++;
                }
                Log.e(TAG, "observerCount = " + observerCount);                
            }
        }
    };
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        Intent it = getIntent();
        currentSlotId = it.getIntExtra("SlotId", 0);
        Log.i(TAG, "Got slot id is : " + currentSlotId);
        if (currentSlotId == Phone.GEMINI_SIM_1) {
            ICC_URI = Uri.parse("content://sms/icc");
            setContentView(R.layout.sim_list);
        } else if (currentSlotId == Phone.GEMINI_SIM_2) {
            ICC_URI = Uri.parse("content://sms/icc2");
            setContentView(R.layout.sim2_list);
        }

        mContentResolver = getContentResolver();
        mQueryHandler = new QueryHandler(mContentResolver, this);
        
        mSimList = (ListView) findViewById(R.id.messages);
        mMessage = (TextView) findViewById(R.id.empty_message);

        ITelephony iTelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        if(FeatureOption.MTK_GEMINI_SUPPORT == true &&
           !TelephonyManager.getDefault().hasIccCardGemini(currentSlotId)){
            mSimList.setVisibility(View.GONE);
            if (currentSlotId == Phone.GEMINI_SIM_1) {
                mMessage.setText(R.string.no_sim_1);
            } else if (currentSlotId == Phone.GEMINI_SIM_2) {
            	mMessage.setText(R.string.no_sim_2);
            }
            mMessage.setVisibility(View.VISIBLE);
            setTitle(getString(R.string.sim_manage_messages_title));
            setProgressBarIndeterminateVisibility(false);
        }else if(FeatureOption.MTK_GEMINI_SUPPORT == true){
            try{
            	boolean mIsSim1Ready = iTelephony.isRadioOnGemini(currentSlotId);
                
                if(!mIsSim1Ready){
                    mSimList.setVisibility(View.GONE);
                    mMessage.setText(com.mediatek.R.string.sim_close);
                    mMessage.setVisibility(View.VISIBLE);
                    setTitle(getString(R.string.sim_manage_messages_title));
                    setProgressBarIndeterminateVisibility(false);
                }else{
                    isInit = true;
                }
            }catch(RemoteException e){
                Log.i(TAG, "RemoteException happens......");
            }
        }else{
            isInit = true;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);

//        init();
    }

    private void init() {
        updateState(SHOW_BUSY);
        startQuery();
    }

    private class QueryHandler extends AsyncQueryHandler {
        private final ManageSimMessages mParent;

        public QueryHandler(
                ContentResolver contentResolver, ManageSimMessages parent) {
            super(contentResolver);
            mParent = parent;
        }

        @Override
        protected void onQueryComplete(
                int token, Object cookie, Cursor cursor) {
            Log.d(TAG, "onQueryComplete");                                    
            if(FeatureOption.MTK_GEMINI_SUPPORT == true){            
                removeDialog(DIALOG_REFRESH);
            }                
            if(isDeleting){
                isDeleting = false;
            }
            if(observerCount > 0){
                ManageSimMessages.this.startQuery();
                observerCount = 0;
                return;
            }else{
                isQuerying = false;
            }
            if (mCursor != null && !mCursor.isClosed()) {
                mCursor.close();
            }
            mCursor = cursor;
            if (mCursor != null) {
                if (!mCursor.moveToFirst()) {
                    // Let user know the SIM is empty
                    updateState(SHOW_EMPTY);
                } else if (mListAdapter == null) {
                    // Note that the MessageListAdapter doesn't support auto-requeries. If we
                    // want to respond to changes we'd need to add a line like:
                    //   mListAdapter.setOnDataSetChangedListener(mDataSetChangedListener);
                    // See ComposeMessageActivity for an example.
                    mListAdapter = new MessageListAdapter(
                            mParent, mCursor, mSimList, false, null);
                    mSimList.setAdapter(mListAdapter);
                    mSimList.setOnCreateContextMenuListener(mParent);
                    updateState(SHOW_LIST);
                } else {
                    mListAdapter.changeCursor(mCursor);
                    updateState(SHOW_LIST);
                }
                startManagingCursor(mCursor);
                registerSimChangeObserver();
            } else {
                // Let user know the SIM is empty
                updateState(SHOW_EMPTY);
            }
        }
    }


    @Override 
    protected Dialog onCreateDialog(int id){
        switch(id){
            case DIALOG_REFRESH: {
                dialog = new ProgressDialog(this);
                dialog.setIndeterminate(true);
                dialog.setCancelable(false);
                dialog.setMessage(getString(R.string.refreshing));
                return dialog;
            }
        }
        return null;
    }

    private void startQuery() {
        Log.d(TAG, "startQuery");                            
        if(FeatureOption.MTK_GEMINI_SUPPORT == true){
            showDialog(DIALOG_REFRESH);
        }

        try {
            isQuerying = true;
            mQueryHandler.startQuery(0, null, ICC_URI, null, null, null, null);
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    private void refreshMessageList() {
        Log.d(TAG, "refreshMessageList");                                    
        updateState(SHOW_BUSY);
        if (mCursor != null) {
            stopManagingCursor(mCursor);
            // mCursor.close();
        }
        startQuery();
    }

    @Override
    public void onCreateContextMenu(
            ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle(R.string.message_options);
        //MTK_OP02_PROTECT_START
        String optr = SystemProperties.get("ro.operator.optr");
        if ("OP02".equals(optr)) {
            AdapterView.AdapterContextMenuInfo info = null;
            try {
                 info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            } catch (ClassCastException exception) {
                Log.e(TAG, "Bad menuInfo.", exception);
            }
            final Cursor cursor = (Cursor) mListAdapter.getItem(info.position);
            addCallAndContactMenuItems(menu, cursor);
            addRecipientToContact(menu, cursor);
            menu.add(0, MENU_FORWARD, 0, R.string.menu_forward);
            menu.add(0, MENU_REPLY, 0, R.string.menu_reply);
        }
        //MTK_OP02_PROTECT_END
        menu.add(0, MENU_COPY_TO_PHONE_MEMORY, 0,
                R.string.sim_copy_to_phone_memory);
        menu.add(0, MENU_DELETE_FROM_SIM, 0, R.string.sim_delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException exception) {
            Log.e(TAG, "Bad menuInfo.", exception);
            return false;
        }

        final Cursor cursor = (Cursor) mListAdapter.getItem(info.position);
        if(cursor == null){
            Xlog.e(TAG, "Bad menuInfo, cursor is null");
            return false;
        }
        switch (item.getItemId()) {
            case MENU_COPY_TO_PHONE_MEMORY:
                copyToPhoneMemory(cursor);
                return true;
            case MENU_DELETE_FROM_SIM:
                final String msgIndex = getMsgIndexByCursor(cursor);
                confirmDeleteDialog(new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        updateState(SHOW_BUSY);
                        new Thread(new Runnable() {
                            public void run() {
                                deleteFromSim(msgIndex);
                            }
                        }, "ManageSimMessages").start();
                        dialog.dismiss();
                    }
                }, R.string.confirm_delete_SIM_message);
                return true;
            //MTK_OP02_PROTECT_START
            case MENU_FORWARD:
                forwardMessage(cursor);
                return true;
            case MENU_REPLY:
                replyMessage(cursor);
                return true;
            case MENU_ADD_TO_BOOKMARK:{
                if (mURLs.size() == 1) {
                    Browser.saveBookmark(ManageSimMessages.this, null, mURLs.get(0));
                } else if(mURLs.size() > 1) {
                    CharSequence[] items = new CharSequence[mURLs.size()];
                    for (int i = 0; i < mURLs.size(); i++) {
                        items[i] = mURLs.get(i);
                    }
                    new AlertDialog.Builder(ManageSimMessages.this)
                        .setTitle(R.string.menu_add_to_bookmark)
                        .setItems(items, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Browser.saveBookmark(ManageSimMessages.this, null, mURLs.get(which));
                                }
                            })
                        .show();
                }
                return true;
             }
            case MENU_ADD_CONTACT:
            	String number = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                addToContact(number);
                return true;
            //MTK_OP02_PROTECT_END
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");                                        
        super.onResume();

        /* If recreate this activity, the dialog and cursor will be restore in method
         * onRestoreInstanceState() which will be invoked between onStart and onResume.
         * Note: The dialog showed before onPause() will be recreated 
         *      (Refer to Activity.onSaveInstanceState() and onRestoreInstanceState()).
         * So, we should initialize in onResume method when it is first time enter this
         * activity, if need.*/
        if (isInit) {
            isInit = false;
            init();
        }
        registerSimChangeObserver();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");                                            
        super.onPause();
        //invalidate cache to refresh contact data
        Contact.invalidateCache();        
        mContentResolver.unregisterContentObserver(simChangeObserver);
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");                                            
        super.onStop();
        if (dialog != null) {
            removeDialog(DIALOG_REFRESH);
        }
    }

    private void registerSimChangeObserver() {
        mContentResolver.registerContentObserver(
                ICC_URI, true, simChangeObserver);
    }

    private void copyToPhoneMemory(Cursor cursor) {
        String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
        String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
        Long date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
        String serviceCenter = cursor.getString(cursor.getColumnIndexOrThrow("service_center_address"));
        Xlog.d(MmsApp.TXN_TAG, "\t address \t=" + address);
        Xlog.d(MmsApp.TXN_TAG, "\t body \t=" + body);
        Xlog.d(MmsApp.TXN_TAG, "\t date \t=" + date);
        Xlog.d(MmsApp.TXN_TAG, "\t sc \t=" + serviceCenter);

        try {
            if (isIncomingMessage(cursor)) {
                Xlog.d(MmsApp.TXN_TAG, "Copy incoming sms to phone");
                if(FeatureOption.MTK_GEMINI_SUPPORT){
                    SIMInfo simInfo = SIMInfo.getSIMInfoBySlot(this, currentSlotId);
                    if (simInfo != null) {
                        Sms.Inbox.addMessage(mContentResolver, address, body, null, serviceCenter, date, true, (int)simInfo.mSimId);
                    } else {
                        Sms.Inbox.addMessage(mContentResolver, address, body, null, serviceCenter, date, true, -1);
                    }
                }else{
                    Sms.Inbox.addMessage(mContentResolver, address, body, null, serviceCenter, date, true);
                }
            } else {
                // outgoing sms has not date info
                date = System.currentTimeMillis();
                Xlog.d(MmsApp.TXN_TAG, "Copy outgoing sms to phone");
                
                if(FeatureOption.MTK_GEMINI_SUPPORT){
                    SIMInfo simInfo = SIMInfo.getSIMInfoBySlot(this, currentSlotId);
                    if (simInfo != null) {
                        Sms.Sent.addMessage(mContentResolver, address, body, null, serviceCenter, date, (int)simInfo.mSimId);
                    } else {
                        Sms.Sent.addMessage(mContentResolver, address, body, null, serviceCenter, date, -1);
                    }
                } else {
                    Sms.Sent.addMessage(mContentResolver, address, body, null, serviceCenter, date);
                }
            }
            Recycler.getSmsRecycler().deleteOldMessages(getApplicationContext());
            
            String msg = getString(R.string.done);
            Toast.makeText(ManageSimMessages.this, msg, Toast.LENGTH_SHORT).show();
            
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    private boolean isIncomingMessage(Cursor cursor) {
        int messageStatus = cursor.getInt(
                cursor.getColumnIndexOrThrow("status"));
        //if(messageStatus == -1) {
        //    return true;
        //}
        Xlog.d(MmsApp.TXN_TAG, "message status:" + messageStatus);
        return (messageStatus == SmsManager.STATUS_ON_ICC_READ) ||
               (messageStatus == SmsManager.STATUS_ON_ICC_UNREAD);
    }

    private String getMsgIndexByCursor(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndexOrThrow("index_on_icc"));
    }

    private void deleteFromSim(String msgIndex) {
        /* 1. Non-Concatenated SMS's message index string is like "1"
         * 2. Concatenated SMS's message index string is like "1;2;3;".
         * 3. If a concatenated SMS only has one segment stored in SIM Card, its message 
         *    index string is like "1;".
         */
        String[] index = msgIndex.split(";");
        Uri simUri = ICC_URI.buildUpon().build();
        if (SqliteWrapper.delete(this, mContentResolver, simUri, FOR_MULTIDELETE, index) == 1) {
            MessagingNotification.cancelNotification(getApplicationContext(),
                    SIM_FULL_NOTIFICATION_ID);
        }
        isDeleting = true;
    }

    private void deleteFromSim(Cursor cursor) {
        String msgIndex = getMsgIndexByCursor(cursor);
        deleteFromSim(msgIndex);
    }

    private void deleteAllFromSim() {
        // For Delete all,MTK FW support delete all using messageIndex = -1, here use 999999 instead of -1;
        String messageIndexString = ALL_SMS;
        //cursor.getString(cursor.getColumnIndexOrThrow("index_on_icc"));
        Uri simUri = ICC_URI.buildUpon().appendPath(messageIndexString).build();
        Log.i(TAG, "delete simUri: " + simUri);
        if (SqliteWrapper.delete(this, mContentResolver, simUri, null, null) == 1) {
            MessagingNotification.cancelNotification(getApplicationContext(),
                    SIM_FULL_NOTIFICATION_ID);
        }
        isDeleting = true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();

        if ((null != mCursor) && (mCursor.getCount() > 0) && mState == SHOW_LIST) {
            menu.add(0, OPTION_MENU_DELETE_ALL, 0, R.string.menu_delete_messages).setIcon(
                    android.R.drawable.ic_menu_delete);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case OPTION_MENU_DELETE_ALL:
                confirmDeleteDialog(new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        updateState(SHOW_BUSY);
                        deleteAllFromSim();
                        dialog.dismiss();
                    }
                }, R.string.confirm_delete_all_SIM_messages);
                break;

        }

        return true;
    }

    private void confirmDeleteDialog(OnClickListener listener, int messageId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_dialog_title);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.yes, listener);
        builder.setNegativeButton(R.string.no, null);
        builder.setMessage(messageId);

        builder.show();
    }

    private void updateState(int state) {
        Log.e(TAG, "updateState, state = "+ state);            
        if (mState == state) {
            return;
        }

        mState = state;
        switch (state) {
            case SHOW_LIST:
                mSimList.setVisibility(View.VISIBLE);
                mSimList.requestFocus();
                mSimList.setSelection(mSimList.getCount()-1);
                mMessage.setVisibility(View.GONE);
                setTitle(getString(R.string.sim_manage_messages_title));
                setProgressBarIndeterminateVisibility(false);
                break;
            case SHOW_EMPTY:
                mSimList.setVisibility(View.GONE);
                mMessage.setVisibility(View.VISIBLE);
                setTitle(getString(R.string.sim_manage_messages_title));
                setProgressBarIndeterminateVisibility(false);
                break;
            case SHOW_BUSY:
                mSimList.setVisibility(View.GONE);
                mMessage.setVisibility(View.GONE);
                setTitle(getString(R.string.refreshing));
                setProgressBarIndeterminateVisibility(true);
                break;
            default:
                Log.e(TAG, "Invalid State");
        }
    }

    private void viewMessage(Cursor cursor) {
        // TODO: Add this.
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode)
    {
        if (null != intent && null != intent.getData()
                && intent.getData().getScheme().equals("mailto")) {
            try {
                super.startActivityForResult(intent, requestCode);
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "Failed to startActivityForResult: " + intent);
                Intent i = new Intent().setClassName("com.android.email", "com.android.email.activity.setup.AccountSetupBasics");
                this.startActivity(i);
                finish();
            } catch (Exception e) {
                Log.e(TAG, "Failed to startActivityForResult: " + intent);
                Toast.makeText(this,getString(R.string.message_open_email_fail),
                      Toast.LENGTH_SHORT).show();
          }
        } else {
            super.startActivityForResult(intent, requestCode);
        }
    }
    
    //MTK_OP02_PROTECT_START
    private void forwardMessage(Cursor cursor) {
        String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
        Intent intent = new Intent();
        intent.setClassName(this, "com.android.mms.ui.ForwardMessageActivity");
        intent.putExtra(ComposeMessageActivity.FORWARD_MESSAGE, true);
        if (body != null) {
            intent.putExtra(ComposeMessageActivity.SMS_BODY, body);
        }
        
        startActivity(intent);
    }
    
    private void replyMessage(Cursor cursor) {
        String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
        Intent intent = new Intent(Intent.ACTION_SENDTO,
                Uri.fromParts("sms", address, null));
        startActivity(intent);
    }

    private final void addCallAndContactMenuItems(ContextMenu menu, Cursor cursor) {
        // Add all possible links in the address & message
        StringBuilder textToSpannify = new StringBuilder();  
        String reciBody = cursor.getString(cursor.getColumnIndexOrThrow("body"));
        String reciNumber = cursor.getString(cursor.getColumnIndexOrThrow("address"));
        textToSpannify.append(reciNumber + ": ");
        textToSpannify.append(reciBody);
        SpannableString msg = new SpannableString(textToSpannify.toString());
        Linkify.addLinks(msg, Linkify.ALL);
        ArrayList<String> uris =
            MessageUtils.extractUris(msg.getSpans(0, msg.length(), URLSpan.class));
        mURLs.clear();
        Log.d(TAG, "addCallAndContactMenuItems uris.size() = " + uris.size());
        while (uris.size() > 0) {
            String uriString = uris.remove(0);
            // Remove any dupes so they don't get added to the menu multiple times
            while (uris.contains(uriString)) {
                uris.remove(uriString);
            }

            int sep = uriString.indexOf(":");
            String prefix = null;
            if (sep >= 0) {
                prefix = uriString.substring(0, sep);
                if ("mailto".equalsIgnoreCase(prefix) || "tel".equalsIgnoreCase(prefix)){
                    uriString = uriString.substring(sep + 1);
                }
            }
            boolean addToContacts = false;
            if ("mailto".equalsIgnoreCase(prefix)) {
                String sendEmailString = getString(R.string.menu_send_email).replace("%s", uriString);
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("mailto:" + uriString));
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                menu.add(0, MENU_SEND_EMAIL, 0, sendEmailString).setIntent(intent);
                addToContacts = !haveEmailContact(uriString);
            } else if ("tel".equalsIgnoreCase(prefix)) {
                String callBackString = getString(R.string.menu_call_back).replace("%s", uriString);
                Intent intent = new Intent(Intent.ACTION_DIAL,Uri.parse("tel:" + uriString));
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                menu.add(0, MENU_CALL_BACK, 0, callBackString).setIntent(intent);
                
                if (reciBody != null && reciBody.replaceAll("\\-", "").contains(uriString)) {
                    String sendSmsString = getString(
                        R.string.menu_send_sms).replace("%s", uriString);
                    Intent intentSms = new Intent(Intent.ACTION_SENDTO,
                        Uri.parse("smsto:" + uriString));
                    intentSms.setClassName(ManageSimMessages.this, "com.android.mms.ui.SendMessageToActivity");
                    intentSms.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    menu.add(0, MENU_SEND_SMS, 0, sendSmsString)
                        .setIntent(intentSms);
                }
                addToContacts = !isNumberInContacts(uriString);
            } else {
                //add URL to book mark
                if (mURLs.size() <= 0) {
                    menu.add(0, MENU_ADD_TO_BOOKMARK, 0, R.string.menu_add_to_bookmark);
                }
                mURLs.add(uriString);
            }
            if (addToContacts) {
                //Intent intent = ConversationList.createAddContactIntent(uriString);
                Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                intent.putExtra(ContactsContract.Intents.Insert.PHONE, uriString);
                String addContactString = getString(
                        R.string.menu_add_address_to_contacts).replace("%s", uriString);
                menu.add(0, MENU_ADD_ADDRESS_TO_CONTACTS, 0, addContactString)
                    .setIntent(intent);
            }
        }
    }
    
    private boolean addRecipientToContact(ContextMenu menu, Cursor cursor){
    	 boolean showAddContact = false;
    	 String reciNumber = cursor.getString(cursor.getColumnIndexOrThrow("address"));
    	 Log.d(TAG, "addRecipientToContact reciNumber = " + reciNumber);
         // if there is at least one number not exist in contact db, should show add.
    	 mContactList = ContactList.getByNumbers(reciNumber, false, true);
         for (Contact contact : mContactList) {
             if (!contact.existsInDatabase()) {
                 showAddContact = true;
                 Log.d(TAG, "not in contact[number:" + contact.getNumber() + ",name:" + contact.getName());
                 break;
             }
         }
         boolean menuAddExist = (menu.findItem(MENU_ADD_CONTACT)!= null);
         if (showAddContact) {
             if (!menuAddExist) {
                 menu.add(0, MENU_ADD_CONTACT, 1, R.string.menu_add_to_contacts).setIcon(R.drawable.ic_menu_contact);
             }
         } else {
             menu.removeItem(MENU_ADD_CONTACT);
         }
         return true;
    }
    private boolean isNumberInContacts(String phoneNumber) {
        return Contact.get(phoneNumber, false).existsInDatabase();
    }
    
    private boolean haveEmailContact(String emailAddress) {
        Cursor cursor = SqliteWrapper.query(this, getContentResolver(),
                Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, Uri.encode(emailAddress)),
                new String[] { Contacts.DISPLAY_NAME }, null, null, null);

        if (cursor != null) {
            try {
                String name;
                while (cursor.moveToNext()) {
                    name = cursor.getString(0);
                    if (!TextUtils.isEmpty(name)) {
                        return true;
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return false;
    }
    
    private void addToContact(String reciNumber){
        int count = mContactList.size();
        switch(count) {
        case 0:
            Log.e(TAG, "add contact, mCount == 0!");
            break;
        case 1:
            MessageUtils.addNumberOrEmailtoContact(reciNumber, 0, this);
            break;
        default:
            break;
        }
    }
    //MTK_OP02_PROTECT_END
}

