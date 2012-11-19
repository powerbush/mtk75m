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

import com.android.internal.telephony.ITelephony;
import com.android.mms.LogTag;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.MmsApp;
import com.android.mms.transaction.CBMessagingNotification;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.transaction.SmsRejectedReceiver;
import com.android.mms.transaction.WapPushMessagingNotification;
import com.android.mms.util.DraftCache;
import com.android.mms.util.Recycler;
import com.google.android.mms.pdu.PduHeaders;
import android.database.sqlite.SqliteWrapper;
import android.provider.Settings;
import android.app.StatusBarManager;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.os.Bundle;
import android.os.Handler;
import android.os.ServiceManager;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.Mms;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony.Sms;
import android.provider.Telephony.WapPush;
import android.telephony.SmsManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.net.Uri;
import android.provider.Telephony.Threads;

import com.mediatek.wappush.SiExpiredCheck;
import com.mediatek.featureoption.FeatureOption;
import java.util.List;
import android.text.TextUtils;
import android.os.SystemProperties; 
import com.mediatek.xlog.Xlog;
import com.mediatek.xlog.SXlog;
/**
 * This activity provides a list view of existing conversations.
 */
public class ConversationList extends ListActivity
            implements DraftCache.OnDraftChangedListener {
    private static final String TAG = "ConversationList";
    private static final String CONV_TAG = "Mms/convList";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG;

    private static final int THREAD_LIST_QUERY_TOKEN       = 1701;
    public static final int DELETE_CONVERSATION_TOKEN      = 1801;
    public static final int HAVE_LOCKED_MESSAGES_TOKEN     = 1802;
    private static final int DELETE_OBSOLETE_THREADS_TOKEN = 1803;

    // IDs of the main menu items.
    public static final int MENU_COMPOSE_NEW          = 0;
    public static final int MENU_SEARCH               = 1;
    public static final int MENU_SIM_SMS              = 2;
    public static final int MENU_DELETE_ALL           = 3;
    public static final int MENU_PREFERENCES          = 4;
    public static final int MENU_OMACP                = 5;

    // IDs of the context menu items for the list of conversations.
    public static final int MENU_DELETE               = 0;
    public static final int MENU_VIEW                 = 1;
    public static final int MENU_VIEW_CONTACT         = 2;
    public static final int MENU_ADD_TO_CONTACTS      = 3;

    private ThreadListQueryHandler mQueryHandler;
    private ConversationListAdapter mListAdapter;
    private SharedPreferences mPrefs;
    private Handler mHandler;
    private boolean mNeedToMarkAsSeen;
    private long mThreadID = -1;
    
    //wappush: indicates the type of thread, this exits already, but has not been used before
    private int mType;
    //wappush: SiExpired Check
    private SiExpiredCheck siExpiredCheck;
    //wappush: wappush TAG
    private static final String WP_TAG = "Mms/WapPush";
    
    private Contact mContact = null;
    private StatusBarManager mStatusBarManager;
    static private final String CHECKED_MESSAGE_LIMITS = "checked_message_limits";

    private static final  Uri UPDATE_THREADS_URI = Uri.parse("content://mms-sms/conversations/obsolete");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.conversation_list_screen);
        
        mStatusBarManager = (StatusBarManager)getSystemService(Context.STATUS_BAR_SERVICE);
        mQueryHandler = new ThreadListQueryHandler(getContentResolver());

        ListView listView = getListView();
        listView.setItemsCanFocus(true);
        View headerView = LayoutInflater.from(this).inflate(R.layout.conversation_header_item, null);   
        listView.addHeaderView(headerView);        
              
        listView.setOnCreateContextMenuListener(mConvListOnCreateContextMenuListener);
        listView.setOnKeyListener(mThreadListKeyListener);
        ConversationListItem.resetDefaultDrawable(this); 
        initListAdapter();


        mHandler = new Handler();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean checkedMessageLimits = mPrefs.getBoolean(CHECKED_MESSAGE_LIMITS, false);
        if (DEBUG) Log.v(TAG, "checkedMessageLimits: " + checkedMessageLimits);
        if (!checkedMessageLimits || DEBUG) {
            runOneTimeStorageLimitCheckForLegacyMessages();
        }
        
        if(FeatureOption.MTK_WAPPUSH_SUPPORT){
            siExpiredCheck = new SiExpiredCheck(this);
            siExpiredCheck.startSiExpiredCheckThread();
        }
    }

    private final ConversationListAdapter.OnContentChangedListener mContentChangedListener =
        new ConversationListAdapter.OnContentChangedListener() {
        public void onContentChanged(ConversationListAdapter adapter) {
            startAsyncQuery();
        }
    };

    private void initListAdapter() {
        mListAdapter = new ConversationListAdapter(this, null);
        mListAdapter.setOnContentChangedListener(mContentChangedListener);
        setListAdapter(mListAdapter);
        getListView().setRecyclerListener(mListAdapter);
    }
    
    @Override
    public void onBackPressed() {
        if (isTaskRoot()) {
            // Instead of stopping, simply push this to the back of the stack.
            // This is only done when running at the top of the stack;
            // otherwise, we have been launched by someone else so need to
            // allow the user to go back to the caller.
            moveTaskToBack(false);
        } else {
            super.onBackPressed();
        }
    }
    
    /**
     * Checks to see if the number of MMS and SMS messages are under the limits for the
     * recycler. If so, it will automatically turn on the recycler setting. If not, it
     * will prompt the user with a message and point them to the setting to manually
     * turn on the recycler.
     */
    public synchronized void runOneTimeStorageLimitCheckForLegacyMessages() {
        if (Recycler.isAutoDeleteEnabled(this)) {
            if (DEBUG) Log.v(TAG, "recycler is already turned on");
            // The recycler is already turned on. We don't need to check anything or warn
            // the user, just remember that we've made the check.
            markCheckedMessageLimit();
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                if (Recycler.checkForThreadsOverLimit(ConversationList.this)) {
                    if (DEBUG) Log.v(TAG, "checkForThreadsOverLimit TRUE");
                    // Dang, one or more of the threads are over the limit. Show an activity
                    // that'll encourage the user to manually turn on the setting. Delay showing
                    // this activity until a couple of seconds after the conversation list appears.
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            Intent intent = new Intent(ConversationList.this,
                                    WarnOfStorageLimitsActivity.class);
                            startActivity(intent);
                        }
                    }, 2000);
                } //else {
//                    if (DEBUG) Log.v(TAG, "checkForThreadsOverLimit silently turning on recycler");
//                    // No threads were over the limit. Turn on the recycler by default.
//                    runOnUiThread(new Runnable() {
//                        public void run() {
//                            SharedPreferences.Editor editor = mPrefs.edit();
//                            editor.putBoolean(MessagingPreferenceActivity.AUTO_DELETE, true);
//                            editor.apply();
//                        }
//                    });
//                }
                // Remember that we don't have to do the check anymore when starting MMS.
                runOnUiThread(new Runnable() {
                    public void run() {
                        markCheckedMessageLimit();
                    }
                });
            }
        }).start();
    }

    /**
     * Mark in preferences that we've checked the user's message limits. Once checked, we'll
     * never check them again, unless the user wipe-data or resets the device.
     */
    private void markCheckedMessageLimit() {
        if (DEBUG) Log.v(TAG, "markCheckedMessageLimit");
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(CHECKED_MESSAGE_LIMITS, true);
        editor.apply();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // Handle intents that occur after the activity has already been created.
        privateOnStart();
    }

    @Override
    protected void onResume() {
        ComposeMessageActivity.mDestroy = true;
        
            try{
                SqliteWrapper.delete(this,getContentResolver(),UPDATE_THREADS_URI,null,null);
            }catch(SQLiteDiskIOException ex){
                Xlog.e(CONV_TAG, " " + ex.getMessage());
            }
            finally{
                mNeedToMarkAsSeen = true;
                startAsyncQuery();

                if(FeatureOption.MTK_WAPPUSH_SUPPORT){
                    siExpiredCheck.startExpiredCheck();
                }
                super.onResume();
            }
            
        // show SMS indicator
        Handler mShowIndicatorHandler = new Handler();
        final ComponentName name = getComponentName();
        		mStatusBarManager.hideSIMIndicator(name);
                mStatusBarManager.showSIMIndicator(name, Settings.System.SMS_SIM_SETTING);
    }

    @Override
    protected void onPause() {
    	mStatusBarManager.hideSIMIndicator(getComponentName());
    	super.onPause();
    }
    @Override
    protected void onStart() {
        super.onStart();
        getListView().setSelectionFromTop(0, 0);
        MessagingNotification.cancelNotification(getApplicationContext(),
                SmsRejectedReceiver.SMS_REJECTED_NOTIFICATION_ID);

        DraftCache.getInstance().addOnDraftChangedListener(this);

        //mNeedToMarkAsSeen = true;
        startAsyncQuery();
        // we invalidate the contact cache here because we want to get updated presence
        // and any contact changes. We don't invalidate the cache by observing presence and contact
        // changes (since that's too untargeted), so as a tradeoff we do it here.
        // If we're in the middle of the app initialization where we're loading the conversation
        // threads, don't invalidate the cache because we're in the process of building it.
        // TODO: think of a better way to invalidate cache more surgically or based on actual
        // TODO: changes we care about
        if (!Conversation.loadingThreads()) {
            Contact.invalidateCache();
        }
    }

    protected void privateOnStart() {
        startAsyncQuery();
    }

    @Override
    protected void onStop() {
        super.onStop();

        DraftCache.getInstance().removeOnDraftChangedListener(this);
        //mListAdapter.changeCursor(null);
        
        //wappush: stop expiration check when exit
        Xlog.i(WP_TAG, "ConversationList: " + "stopExpiredCheck");
        if(FeatureOption.MTK_WAPPUSH_SUPPORT){
        	siExpiredCheck.stopExpiredCheck();
        }
    }
    
    @Override
    protected void onDestroy() {
    	//stop the si expired check thread
        if(FeatureOption.MTK_WAPPUSH_SUPPORT){
        	siExpiredCheck.stopSiExpiredCheckThread();
        }
        super.onDestroy();
    }

    public void onDraftChanged(final long threadId, final boolean hasDraft) {
        // Run notifyDataSetChanged() on the main thread.
        mQueryHandler.post(new Runnable() {
            public void run() {
                if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                    log("onDraftChanged: threadId=" + threadId + ", hasDraft=" + hasDraft);
                }
                mListAdapter.notifyDataSetChanged();
            }
        });
    }

    private void startAsyncQuery() {
        try {
            setTitle(getString(R.string.refreshing));
            setProgressBarIndeterminateVisibility(true);

            Conversation.startQueryForAll(mQueryHandler, THREAD_LIST_QUERY_TOKEN);
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();

        menu.add(0, MENU_COMPOSE_NEW, 0, R.string.menu_compose_new).setIcon(
                com.android.internal.R.drawable.ic_menu_compose);
        // Check OMACP should display or not: flag got from OmacpMessageList.class
        // show OMACP item, warning: don't change the sequence of these item, 
        // otherwise, you will get a surprise;
        Context otherAppContext = null;
        try{
            otherAppContext = this.createPackageContext("com.mediatek.omacp", 
                    Context.CONTEXT_IGNORE_SECURITY);
        }
        catch(Exception e){
            Xlog.e(CONV_TAG, "ConversationList NotFoundContext");
        }
        if (null != otherAppContext) {
            SharedPreferences sp = otherAppContext.getSharedPreferences("omacp", MODE_WORLD_READABLE);
            boolean omaCpShow = sp.getBoolean("configuration_msg_exist", false);
            if(omaCpShow){  
     	        menu.add(0, MENU_OMACP, 0, R.string.menu_omacp).setIcon(
     	                R.drawable.ic_menu_omacp);
            }
        } 
        if (mListAdapter.getCount() > 0) {
            menu.add(0, MENU_DELETE_ALL, 0, R.string.menu_delete_all).setIcon(
                    android.R.drawable.ic_menu_delete);
        }
        String optr = SystemProperties.get("ro.operator.optr");
        //MTK_OP02_PROTECT_START
        if ("OP02".equals(optr)) {
        	menu.add(0, MENU_SIM_SMS, 0, R.string.menu_sim_sms).setIcon(
                    R.drawable.ic_menu_sim_sms);
        	MenuItem item = menu.findItem(MENU_SIM_SMS);
        	List<SIMInfo> listSimInfo = SIMInfo.getInsertedSIMList(this);
        	if(listSimInfo == null || listSimInfo.isEmpty()){
        		item.setEnabled(false);
        		Xlog.d(CONV_TAG, "onPrepareOptionsMenu MenuItem setEnabled(false)");
	        }
        }
        //MTK_OP02_PROTECT_END

        menu.add(0, MENU_SEARCH, 0, android.R.string.search_go).
            setIcon(android.R.drawable.ic_menu_search).
            setAlphabeticShortcut(android.app.SearchManager.MENU_KEY);

        menu.add(0, MENU_PREFERENCES, 0, R.string.menu_preferences).setIcon(
                android.R.drawable.ic_menu_preferences);

        return true;
    }

    @Override
    public boolean onSearchRequested() {
        startSearch(null, false, null /*appData*/, false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case MENU_COMPOSE_NEW:
                createNewMessage();
                break;
            case MENU_SEARCH:
                onSearchRequested();
                break;
            case MENU_DELETE_ALL:
                // The invalid threadId of -1 means all threads here.
                //confirmDeleteThread(-1L, mQueryHandler);
            	Intent it = new Intent(this, MultiDeleteActivity.class);
            	it.putExtra("token", 1901);
            	startActivity(it);
                break;
            case MENU_PREFERENCES: {
                Intent intent = new Intent(this, MessagingPreferenceActivity.class);
                startActivityIfNeeded(intent, -1);
                break;
            }
            case MENU_OMACP:
            	Intent intent = new Intent();
            	intent.setClassName("com.mediatek.omacp", "com.mediatek.omacp.message.OmacpMessageList");
            	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityIfNeeded(intent, -1);
            	break;
            //MTK_OP02_PROTECT_START
            case MENU_SIM_SMS:
            	if(FeatureOption.MTK_GEMINI_SUPPORT == true){
    	            List<SIMInfo> listSimInfo = SIMInfo.getInsertedSIMList(this);
    	            if (listSimInfo.size() > 1) { 
    	            	Intent simSmsIntent = new Intent();
    	            	simSmsIntent.setClass(this, SelectCardPreferenceActivity.class);
    	            	simSmsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	            	simSmsIntent.putExtra("preference", MessagingPreferenceActivity.SMS_MANAGE_SIM_MESSAGES);
    	            	startActivity(simSmsIntent);
    	            } else {  
    	            	Intent simSmsIntent = new Intent();
    	            	simSmsIntent.setClass(this, ManageSimMessages.class);
    	            	simSmsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	            	simSmsIntent.putExtra("SlotId", listSimInfo.get(0).mSlot); 
    	            	startActivity(simSmsIntent);
    	            }
                } else { 
                	startActivity(new Intent(this, ManageSimMessages.class));
                }
            	break;
            //MTK_OP02_PROTECT_END
            default:
                return true;
        }
        return false;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
            // Note: don't read the thread id data from the ConversationListItem view passed in.
            // It's unreliable to read the cached data stored in the view because the ListItem
            // can be recycled, and the same view could be assigned to a different position
            // if you click the list item fast enough. Instead, get the cursor at the position
            // clicked and load the data from the cursor.
            // (ConversationListAdapter extends CursorAdapter, so getItemAtPosition() should
            // return the cursor object, which is moved to the position passed in)
            Cursor cursor  = (Cursor) getListView().getItemAtPosition(position);
            //klocwork issue pid:18182
            if (cursor == null) {
            	return;
            }
            Conversation conv = Conversation.from(this, cursor);
            long tid = conv.getThreadId();

            ConversationListItem headerView = (ConversationListItem) v;
            ConversationListItemData ch = headerView.getConversationHeader();
            
	        //wappush: modify the calling of openThread, add one parameter
            if (LogTag.VERBOSE) {
                Log.d(TAG, "onListItemClick: pos=" + position + ", view=" + v + ", tid=" + tid);
            }
	        Xlog.i(WP_TAG, "ConversationList: " + "ch.getType() is : " + ch.getType());
	        openThread(tid, ch.getType());
    }

    private void createNewMessage() {
        startActivity(ComposeMessageActivity.createIntent(this, 0));
    }
    
    private void openThread(long threadId, int type) {
    	if(FeatureOption.MTK_WAPPUSH_SUPPORT == true){
    		//wappush: add opptunities for starting wappush activity if it is a wappush thread 
        	//type: Threads.COMMON_THREAD, Threads.BROADCAST_THREAD and Threads.WAP_PUSH
        	if(type == Threads.WAPPUSH_THREAD){
            	Xlog.d(WP_TAG, "ConversationList: " + "Start WPMessageActivity.");
            	startActivity(WPMessageActivity.createIntent(this, threadId));
            } else if (type == Threads.CELL_BROADCAST_THREAD) {
            	startActivity(CBMessageListActivity.createIntent(this, threadId));            	
            } else {
            	startActivity(ComposeMessageActivity.createIntent(this, threadId));
            }
    	}else{
    		if (type == Threads.CELL_BROADCAST_THREAD) {
            	startActivity(CBMessageListActivity.createIntent(this, threadId));            	
            } else {
            	startActivity(ComposeMessageActivity.createIntent(this, threadId));
            }
    	}
    }

    public static Intent createAddContactIntent(String address) {
        // address must be a single recipient
        Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.setType(Contacts.CONTENT_ITEM_TYPE);
        if (Mms.isEmailAddress(address)) {
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, address);
        } else {
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, address);
            intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        return intent;
    }

    private final OnCreateContextMenuListener mConvListOnCreateContextMenuListener =
        new OnCreateContextMenuListener() {
        public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenuInfo menuInfo) {
            Cursor cursor = mListAdapter.getCursor();
            if (cursor == null || cursor.getPosition() < 0) {
                return;
            }
            Conversation conv = Conversation.from(ConversationList.this, cursor);
            mThreadID = conv.getThreadId();
            
            //wappush: get the added mType value
            mType = conv.getType();
            Xlog.i(WP_TAG, "ConversationList: " + "mType is : " + mType);   
         
            if(conv != null && conv.getRecipients() != null && conv.getRecipients().size() > 0) {
            	mContact = conv.getRecipients().get(0);
            }
            ContactList recipients = conv.getRecipients();
            menu.setHeaderTitle(recipients.formatNames(","));

            AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) menuInfo;
            if (info.position > 0) {
                menu.add(0, MENU_VIEW, 0, R.string.menu_view);

                // Only show if there's a single recipient
                if (recipients.size() == 1) {
                    // do we have this recipient in contacts?
                    if (recipients.get(0).existsInDatabase()) {
                        menu.add(0, MENU_VIEW_CONTACT, 0, R.string.menu_view_contact);
                    } else {
                        menu.add(0, MENU_ADD_TO_CONTACTS, 0, R.string.menu_add_to_contacts);
                    }
                }
                if(conv.getMessageStatus() == 0){
                    menu.add(0, MENU_DELETE, 0, R.string.menu_delete);
                }       
            }
        }
    };

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Cursor cursor = mListAdapter.getCursor();
        if (mThreadID >= 0 ) {
//            Conversation conv = Conversation.from(ConversationList.this, cursor);
            long threadId = mThreadID;
            switch (item.getItemId()) {
            case MENU_DELETE: {
                confirmDeleteThread(threadId, mQueryHandler);
                break;
            }
            case MENU_VIEW: {
                openThread(threadId, mType);
                break;
            }
            case MENU_VIEW_CONTACT: {
            	if (mContact == null){
            		break;
            	}
                Contact contact = mContact;
                Intent intent = new Intent(Intent.ACTION_VIEW, contact.getUri());
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivity(intent);
                break;
            }
            case MENU_ADD_TO_CONTACTS: {
            	if (mContact == null){
            		break;
            	}
            	  String address = mContact.getNumber();
  				   /*   final Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                intent.putExtra(ContactsContract.Intents.Insert.PHONE, address);
				        startActivity(intent);*/
			     onAddContactButtonClickInt(address);
              //startActivity(createAddContactIntent(address));
                break;
            }
            default:
                break;
            }
        }
        return super.onContextItemSelected(item);
    }

	  public void onAddContactButtonClickInt(final String number) {
        if(!TextUtils.isEmpty(number)) {
            String message = this.getResources().getString(R.string.add_contact_dialog_message, number);
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                                                         .setTitle(number)
                                                         .setMessage(message);
            
            AlertDialog dialog = builder.create();
            
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, this.getResources().getString(R.string.add_contact_dialog_existing), new DialogInterface.OnClickListener() {
                
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                    intent.setType(Contacts.CONTENT_ITEM_TYPE);
                    intent.putExtra(ContactsContract.Intents.Insert.PHONE, number);
						startActivity(intent);
                }
            });

            dialog.setButton(AlertDialog.BUTTON_NEGATIVE, this.getResources().getString(R.string.add_contact_dialog_new), new DialogInterface.OnClickListener() {
                
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    final Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                    intent.putExtra(ContactsContract.Intents.Insert.PHONE, number);
						startActivity(intent);
                }
                
            });
            dialog.show();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // We override this method to avoid restarting the entire
        // activity when the keyboard is opened (declared in
        // AndroidManifest.xml).  Because the only translatable text
        // in this activity is "New Message", which has the full width
        // of phone to work with, localization shouldn't be a problem:
        // no abbreviated alternate words should be needed even in
        // 'wide' languages like German or Russian.

        super.onConfigurationChanged(newConfig);
        if (DEBUG) Log.v(TAG, "onConfigurationChanged: " + newConfig);
    }

    /**
     * Start the process of putting up a dialog to confirm deleting a thread,
     * but first start a background query to see if any of the threads or thread
     * contain locked messages so we'll know how detailed of a UI to display.
     * @param threadId id of the thread to delete or -1 for all threads
     * @param handler query handler to do the background locked query
     */
    public static void confirmDeleteThread(long threadId, AsyncQueryHandler handler) {
        Conversation.startQueryHaveLockedMessages(handler, threadId,
                HAVE_LOCKED_MESSAGES_TOKEN);
    }

    /**
     * Build and show the proper delete thread dialog. The UI is slightly different
     * depending on whether there are locked messages in the thread(s) and whether we're
     * deleting a single thread or all threads.
     * @param listener gets called when the delete button is pressed
     * @param deleteAll whether to show a single thread or all threads UI
     * @param hasLockedMessages whether the thread(s) contain locked messages
     * @param context used to load the various UI elements
     */
    public static void confirmDeleteThreadDialog(final DeleteThreadListener listener,
            boolean deleteAll,
            boolean hasLockedMessages,
            Context context) {
        View contents = View.inflate(context, R.layout.delete_thread_dialog_view, null);
        TextView msg = (TextView)contents.findViewById(R.id.message);
        msg.setText(deleteAll
                ? R.string.confirm_delete_all_conversations
                        : R.string.confirm_delete_conversation);
        final CheckBox checkbox = (CheckBox)contents.findViewById(R.id.delete_locked);
        if (!hasLockedMessages) {
            checkbox.setVisibility(View.GONE);
            listener.setDeleteLockedMessage(true);
        } else {
            listener.setDeleteLockedMessage(checkbox.isChecked());
            checkbox.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    listener.setDeleteLockedMessage(checkbox.isChecked());
                }
            });
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.confirm_dialog_title)
            .setIcon(android.R.drawable.ic_dialog_alert)
        .setCancelable(true)
        .setPositiveButton(R.string.delete, listener)
        .setNegativeButton(R.string.no, null)
        .setView(contents)
        .show();
    }

    private final OnKeyListener mThreadListKeyListener = new OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DEL: {
                        long id = getListView().getSelectedItemId();
                        if (id > 0) {
                            confirmDeleteThread(id, mQueryHandler);
                        }
                        return true;
                    }
                }
            }
            return false;
        }
    };

    public static class DeleteThreadListener implements OnClickListener {
        private final long mThreadId;
        private final AsyncQueryHandler mHandler;
        private final Context mContext;
        private boolean mDeleteLockedMessages;

        public DeleteThreadListener(long threadId, AsyncQueryHandler handler, Context context) {
            mThreadId = threadId;
            mHandler = handler;
            mContext = context;
        }

        public void setDeleteLockedMessage(boolean deleteLockedMessages) {
            mDeleteLockedMessages = deleteLockedMessages;
        }
        private void markAsRead(final long threadId){
        	Uri threadUri = ContentUris.withAppendedId(Threads.CONTENT_URI, mThreadId);

        	ContentValues readContentValues = new ContentValues(1);
        	readContentValues.put("read", 1);
        	mContext.getContentResolver().update(threadUri, readContentValues,
                    "read=0", null);
        }
        public void onClick(DialogInterface dialog, final int whichButton) {
            MessageUtils.handleReadReport(mContext, mThreadId,
                    PduHeaders.READ_STATUS__DELETED_WITHOUT_BEING_READ, new Runnable() {
                public void run() {
                    showProgressDialog();
                    int token = DELETE_CONVERSATION_TOKEN;
                    if (mThreadId == -1) {
                        //wappush: do not need modify the code here, but delete function in provider has been modified.
                        Conversation.startDeleteAll(mHandler, token, mDeleteLockedMessages);
                        DraftCache.getInstance().refresh();
                    } else {
                    	markAsRead(mThreadId);
                    	//wappush: do not need modify the code here, but delete function in provider has been modified.
                        Conversation.startDelete(mHandler, token, mDeleteLockedMessages,
                                mThreadId);
                        DraftCache.getInstance().setDraftState(mThreadId, false);
                    }
                }
                
                private void showProgressDialog() {
                    if (mHandler instanceof BaseProgressQueryHandler) {
                        ((BaseProgressQueryHandler) mHandler).setProgressDialog(
                                DeleteProgressDialogUtil.getProgressDialog(mContext));
                        ((BaseProgressQueryHandler) mHandler).showProgressDialog();
                    }
                }
            });
            dialog.dismiss();
        }
    }
    
    /**
     * The base class about the handler with progress dialog function.
     */
    public static abstract class BaseProgressQueryHandler extends AsyncQueryHandler {
        private ProgressDialog dialog;
        private int progress;
        
        public BaseProgressQueryHandler(ContentResolver resolver) {
            super(resolver);
        }
        
        /**
         * Sets the progress dialog.
         * @param dialog the progress dialog.
         */
        public void setProgressDialog(ProgressDialog dialog) {
            this.dialog = dialog;
        }
        
        /**
         * Sets the max progress.
         * @param max the max progress.
         */
        public void setMax(int max) {
            if (dialog != null) {
                Xlog.d(CONV_TAG, "dialog.setMax max = " + max);
                dialog.setMax(max);
            }
        }
        
        /**
         * Shows the progress dialog. Must be in UI thread.
         */
        public void showProgressDialog() {
            if (dialog != null) {
                dialog.show();
            }
        }
        
        /**
         * Rolls the progress as + 1.
         * @return if progress >= max.
         */
        protected boolean progress() {
            if (dialog != null) {
                Xlog.d(CONV_TAG, "progress = " + progress+";dialog.getMax = "+dialog.getMax());
                return ++progress >= dialog.getMax();
            } else {
                return false;
            }
        }
        
        /**
         * Dismisses the progress dialog.
         */
        protected void dismissProgressDialog() {
            dismissProgressDialog(0);
            if (dialog != null) {
                dismissProgressDialog(0);
            }
        }
        
        private void dismissProgressDialog(int i) {
            if (i != 0) {
                return;
            }
            try {
                dialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (!dialog.isShowing()) {
                    dialog = null;
                }
            }
        }
    }

    private final class ThreadListQueryHandler extends BaseProgressQueryHandler {
        public ThreadListQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            switch (token) {
            case THREAD_LIST_QUERY_TOKEN:
                if (cursor != null) {
                    /* After deleting a conversation, The ListView may refresh again.
                     * Because the cursor is not changed before query again, it may
                     * cause that the deleted threads's data is added in the cache again
                     * by ConversationListAdapter::bindView().
                     * We need to remove the not existed conversation in cache*/
                    Conversation.removeInvalidCache(cursor);
                    mListAdapter.changeCursor(cursor);
                }
                setTitle(getString(R.string.app_label));
                setProgressBarIndeterminateVisibility(false);

                if (mNeedToMarkAsSeen) {
                    mNeedToMarkAsSeen = false;
                    Conversation.markAllConversationsAsSeen(getApplicationContext());

                    // Delete any obsolete threads. Obsolete threads are threads that aren't
                    // referenced by at least one message in the pdu or sms tables.
                    //Conversation.asyncDeleteObsoleteThreads(mQueryHandler,
                    //        DELETE_OBSOLETE_THREADS_TOKEN);
                }
                break;

            case HAVE_LOCKED_MESSAGES_TOKEN:
                long threadId = (Long)cookie;
                confirmDeleteThreadDialog(new DeleteThreadListener(threadId, mQueryHandler,
                        ConversationList.this), threadId == -1,
                        cursor != null && cursor.getCount() > 0,
                        ConversationList.this);
                break;

            default:
                Log.e(TAG, "onQueryComplete called with unknown token " + token);
            }
	        startManagingCursor(cursor);
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            // When this callback is called after deleting, token is 1803(DELETE_OBSOLETE_THREADS_TOKEN)
            // not 1801(DELETE_CONVERSATION_TOKEN)
            CBMessagingNotification.updateNewMessageIndicator(ConversationList.this);
            switch (token) {
            case DELETE_CONVERSATION_TOKEN:
                // Make sure the conversation cache reflects the threads in the DB.
                Conversation.init(ConversationList.this);

                try {
                    ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                    if(phone != null) {
                        if(phone.isTestIccCard()) {
                            Xlog.d(CONV_TAG, "All threads has been deleted, send notification..");
                            SmsManager.getDefault().setSmsMemoryStatus(true);
                        }
                    } else {
                        Xlog.d(CONV_TAG, "Telephony service is not available!");
                    }
                } catch(Exception ex) {
                    Xlog.e(CONV_TAG, " " + ex.getMessage());
                }
                // Update the notification for new messages since they
                // may be deleted.
                MessagingNotification.nonBlockingUpdateNewMessageIndicator(ConversationList.this,
                        false, false);
                // Update the notification for failed messages since they
                // may be deleted.
                MessagingNotification.updateSendFailedNotification(ConversationList.this);
                MessagingNotification.updateDownloadFailedNotification(ConversationList.this);

                //Update the notification for new WAP Push messages
                if(FeatureOption.MTK_WAPPUSH_SUPPORT){
                	WapPushMessagingNotification.nonBlockingUpdateNewMessageIndicator(ConversationList.this,false);
                }

                // Make sure the list reflects the delete
//                startAsyncQuery();
//                onContentChanged();
                if (progress()) {
                    Xlog.d(CONV_TAG, "onDeleteComplete progress() == true");
                    dismissProgressDialog();
                }
                break;

            case DELETE_OBSOLETE_THREADS_TOKEN:
                // Nothing to do here.
                break;
            }
        }
    }

    private void log(String format, Object... args) {
        String s = String.format(format, args);
        Log.d(TAG, "[" + Thread.currentThread().getId() + "] " + s);
    }
}

class ConversationHeaderItem extends RelativeLayout {  
    private Button mNewMessageButton;
    private Context mContext;

    public ConversationHeaderItem(Context context) {
        super(context);
        mContext = context;
    }
    
    public ConversationHeaderItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }
   
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mNewMessageButton = (Button) findViewById(R.id.new_message);
        mNewMessageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mContext.startActivity(ComposeMessageActivity.createIntent(mContext, 0));			
			}
        	
        });
    } 
}
