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

import static android.content.res.Configuration.KEYBOARDHIDDEN_NO;
import static com.android.mms.ui.WPMessageListAdapter.COLUMN_ID;
import static com.android.mms.ui.WPMessageListAdapter.COLUMN_WPMS_LOCKED;
import static com.android.mms.ui.WPMessageListAdapter.COLUMN_WPMS_TYPE;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.android.mms.LogTag;
import com.android.mms.R;
import com.android.mms.data.CBMessage;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.data.WorkingMessage;
import com.android.mms.transaction.CBMessagingNotification;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.transaction.SmsRejectedReceiver;
import com.android.mms.ui.ConversationList.BaseProgressQueryHandler;
import com.android.mms.util.DraftCache;
import com.android.mms.util.Recycler;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.util.SqliteWrapper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.Mms;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.internal.widget.ContactHeaderWidget;

/**
 * This activity provides a list view of existing conversations.
 */
public class CBMessageListActivity extends Activity
            implements DraftCache.OnDraftChangedListener, View.OnClickListener {
    private static final String TAG = "CBMessageListActivity";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG;

    private static final int THREAD_LIST_QUERY_TOKEN = 1901;
    public static final int DELETE_MESSAGE_TOKEN = 1901;
    public static final int HAVE_LOCKED_MESSAGES_TOKEN = 1903;

    // IDs of the main menu items.
    public static final int MENU_DELETE_THREAD          = 0;

    // IDs of the context menu items for the list of conversations.
    public static final int MENU_DELETE_MSG               = 0;
    
    private MessageListQueryHandler mQueryHandler;
    private CBMessageListAdapter mMsgListAdapter;
    private SharedPreferences mPrefs;
    private Handler mHandler;
    private Conversation mConversation;     // Conversation we are working in
    private ListView mMsgListView;  
    private ContactHeaderWidget mContactHeader;
   //add for multi-delete
    private View mDeletePanel;              // View containing the delete and cancel buttons
    private View mSelectPanel;              // View containing the select all check box
    private Button mDeleteButton;
    private Button mCancelButton;
    private CheckBox mSelectedAll;
    private ContentResolver mContentResolver;
    static private final String CHECKED_MESSAGE_LIMITS = "checked_message_limits";
    
    private static final String FOR_MULTIDELETE         = "ForMultiDelete";
    public static final Uri CONTENT_URI = Uri
    .parse("content://cb/messages/#");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cb_message_activity);

        mQueryHandler = new MessageListQueryHandler(getContentResolver());

       // Initialize members for UI elements.
        mMsgListView =  (ListView) findViewById(R.id.cb_history);
        mMsgListView.setDivider(null);      // no divider so we look like IM conversation.
        mContactHeader = (ContactHeaderWidget) findViewById(R.id.association_header_widget);
        //add for multi-delete
        mDeletePanel = findViewById(R.id.delete_panel);
        mSelectPanel = findViewById(R.id.select_panel);
        mSelectPanel.setOnClickListener(this);
        mSelectedAll = (CheckBox)findViewById(R.id.select_all_checked);
        mCancelButton = (Button)findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(this);
        mDeleteButton = (Button)findViewById(R.id.delete);
        mDeleteButton.setOnClickListener(this);
        mDeleteButton.setEnabled(false);
               
        mContentResolver = getContentResolver();
 
        mMsgListView.setOnCreateContextMenuListener(mConvListOnCreateContextMenuListener);
        mMsgListView.setOnKeyListener(mThreadListKeyListener);
        mMsgListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (view != null) {
                    ((CBMessageListItem) view).onMessageListItemClick();
                }
            }
        });
        initialize(savedInstanceState);
        initListAdapter();       
    }
    
    private void initialize(Bundle savedInstanceState) {
        Intent intent = getIntent();

        // Read parameters or previously saved state of this activity.
        initActivityState(savedInstanceState, intent);

        // Mark the current thread as read. 
        mConversation.markAsRead();
    }
    
    private void bindToContactHeaderWidget(int channelId, int simId) {
        mContactHeader.wipeClean();
        mContactHeader.invalidate();        
        String channelName = mConversation.getChannelName(channelId, simId);
        String title = channelName + "(" + channelId + ")";
        mContactHeader.setDisplayName(title,null);
        mContactHeader.setContactUri(null);      
    }
    
    private void initActivityState(Bundle bundle, Intent intent) {
        if (bundle != null) {
            // TODO process bundle != null
            return;
        }
        // If we have been passed a thread_id, use that to find our
        // conversation.
        long threadId = intent.getLongExtra("thread_id", 0);
        if (threadId > 0) {
            mConversation = Conversation.get(this, threadId, false);
        } else {
            Uri intentData = intent.getData();
            if (intentData != null) {
                // try to get a conversation based on the data URI passed to our intent.
                mConversation = Conversation.get(this, intent.getData(), false);
            } else {
                mConversation = Conversation.createNew(this);
            }
        }
    }
    
    private final CBMessageListAdapter.OnContentChangedListener mContentChangedListener =
        new CBMessageListAdapter.OnContentChangedListener() {
        public void onContentChanged(CBMessageListAdapter adapter) {
            startAsyncQuery();
        }
    };

    private void initListAdapter() {
        mMsgListAdapter = new CBMessageListAdapter(this, null);
        mMsgListAdapter.setOnContentChangedListener(mContentChangedListener);
        mMsgListAdapter.setMsgListItemHandler(mMessageListItemHandler);
        mMsgListView.setAdapter(mMsgListAdapter);
        mMsgListView.setRecyclerListener(mMsgListAdapter);     
    }
    
  //==========================================================
    // Inner classes
    //==========================================================
    private final Handler mMessageListItemHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String type;
            switch (msg.what) {             
            case MessageListItem.ITEM_CLICK: {
                //add for multi-delete
                mMsgListAdapter.changeSelectedState(msg.arg1);
                if (mMsgListAdapter.getSelectedNumber() > 0) {
                    mDeleteButton.setEnabled(true);
                    if (mMsgListAdapter.getSelectedNumber() == mMsgListAdapter.getCount()) {
                        mSelectedAll.setChecked(true);
                        return;
                    }
                } else {
                    mDeleteButton.setEnabled(false);
                }
                mSelectedAll.setChecked(false);
                break;
            }
            case CBMessageListItem.UPDATE_CHANNEL: {
               bindToContactHeaderWidget(msg.arg1, msg.arg2);
            }

            default:                
                return;
            }
        }
    };
    
    @Override
    protected void onNewIntent(Intent intent) {
        // Handle intents that occur after the activity has already been created.
        setIntent(intent);
        initialize(null);      
        privateOnStart();
    }

    @Override
    protected void onStart() {
        super.onStart();

        CBMessage.cleanup(this);

        DraftCache.getInstance().addOnDraftChangedListener(this);

        // We used to refresh the DraftCache here, but
        // refreshing the DraftCache each time we go to the ConversationList seems overly
        // aggressive. We already update the DraftCache when leaving CMA in onStop() and
        // onNewIntent(), and when we delete threads or delete all in CMA or this activity.
        // I hope we don't have to do such a heavy operation each time we enter here.

        privateOnStart();

        // we invalidate the contact cache here because we want to get updated presence
        // and any contact changes. We don't invalidate the cache by observing presence and contact
        // changes (since that's too untargeted), so as a tradeoff we do it here.
        // If we're in the middle of the app initialization where we're loading the conversation
        // threads, don't invalidate the cache because we're in the process of building it.
        // TODO: think of a better way to invalidate cache more surgically or based on actual
        // TODO: changes we care about
        if (!CBMessage.loadingMessages()) {
            // TODO donot need here.
        }
    }

    protected void privateOnStart() {
        startAsyncQuery();
    }


    @Override
    protected void onStop() {
        super.onStop();

        DraftCache.getInstance().removeOnDraftChangedListener(this);
        mMsgListAdapter.changeCursor(null);
    }

    public void onDraftChanged(final long threadId, final boolean hasDraft) {
        // Run notifyDataSetChanged() on the main thread.
        mQueryHandler.post(new Runnable() {
            public void run() {
                if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                    log("onDraftChanged: threadId=" + threadId + ", hasDraft=" + hasDraft);
                }
                mMsgListAdapter.notifyDataSetChanged();
            }
        });
    }

    private void startAsyncQuery() {
        try {            
            long threadId = mConversation.getThreadId();
            CBMessage.startQueryForThreadId(mQueryHandler, threadId, THREAD_LIST_QUERY_TOKEN);
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mMsgListAdapter.mIsDeleteMode) {
            return true;
        }
        menu.clear();
        if (mMsgListAdapter.getCount() > 0) {
            menu.add(0, MENU_DELETE_THREAD, 0, R.string.menu_delete).setIcon(
                    android.R.drawable.ic_menu_delete);
        }
        return true;
    }

    @Override
    public boolean onSearchRequested() {
        startSearch(null, false, null /*appData*/, false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Cursor cursor = mMsgListAdapter.getCursor();
        if (cursor.getPosition() >= 0) {
            CBMessage message = CBMessage.from(this, cursor);
            long messageId = message.getMessageId();
            long threadId = message.getThreadId();
            switch (item.getItemId()) {
            case MENU_DELETE_THREAD:
                // TODO, should change to delete one message.
                //confirmDeleteThread(threadId, mQueryHandler, this);
                //add for multi-delete
                changeDeleteMode();
                break;
            default:
                return true;
            }
        }
        return false;
    }

    private final OnCreateContextMenuListener mConvListOnCreateContextMenuListener =
        new OnCreateContextMenuListener() {
        public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenuInfo menuInfo) {
            Cursor cursor = mMsgListAdapter.getCursor();
            if (cursor.getPosition() < 0) {
                return;
            }
            CBMessage message = CBMessage.from(CBMessageListActivity.this, cursor);
            // String channelName = conv.getChannelName();
            menu.setHeaderTitle(R.string.message_options);
            
            AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) menuInfo;
            if (info.position >= 0) {
                menu.add(0, MENU_DELETE_MSG, 0, R.string.delete_message);
            }
        }
    };

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Cursor cursor = mMsgListAdapter.getCursor();
        if (cursor.getPosition() >= 0) {
            CBMessage Message = CBMessage.from(CBMessageListActivity.this, cursor);
            long threadId = Message.getThreadId();
            long messageId = Message.getMessageId();
            switch (item.getItemId()) {
            case MENU_DELETE_MSG: {
                confirmDeleteMessage(messageId, mQueryHandler, this);
                break;
            }
            default:
                break;
            }
        }
        return super.onContextItemSelected(item);
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
    public static void confirmDeleteMessage(long messageId, AsyncQueryHandler handler, Context context) {
        confirmDeleteMessageDialog(new DeleteMessageListener(messageId, handler,
                context), messageId == -1, false,
                context);
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
    public static void confirmDeleteMessageDialog(final DeleteMessageListener listener,
            boolean deleteAll,
            boolean hasLockedMessages,
            Context context) {
        View contents = View.inflate(context, R.layout.delete_thread_dialog_view, null);
        TextView msg = (TextView)contents.findViewById(R.id.message);
        msg.setText(R.string.confirm_delete_message);
        final CheckBox checkbox = (CheckBox)contents.findViewById(R.id.delete_locked);
        if (!hasLockedMessages) {
            checkbox.setVisibility(View.GONE);
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
                        // TODO, need change (maybe)
                        long id = mMsgListView.getSelectedItemId();
                        if (id > 0) {
                            confirmDeleteMessage(id, mQueryHandler, CBMessageListActivity.this);
                        }
                        return true;
                    }
                }
            }
            return false;
        }
    };
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {          
            case KeyEvent.KEYCODE_BACK:
                //add for multi-delete
                if (mMsgListAdapter.mIsDeleteMode) {
                    changeDeleteMode();
                   return true;
                }
        }

        return super.onKeyDown(keyCode, event);
    }
    
    public static class DeleteMessageListener implements OnClickListener {
        private final long mMessageId;
        private final AsyncQueryHandler mHandler;
        private final Context mContext;
        private boolean mDeleteLockedMessages;
        private final Uri mDeleteUri;
        
        public DeleteMessageListener(long messageId, AsyncQueryHandler handler, Context context) {
            mMessageId = messageId;
            mHandler = handler;
            mContext = context;
            mDeleteUri = ContentUris.withAppendedId(CONTENT_URI, messageId);
        }

        public void setDeleteLockedMessage(boolean deleteLockedMessages) {
            mDeleteLockedMessages = deleteLockedMessages;
        }

        public void onClick(DialogInterface dialog, final int whichButton) {
            // TODO do What ??
            mHandler.startDelete(DELETE_MESSAGE_TOKEN,
                    null, mDeleteUri, null, null);
        }
    }

    private final class MessageListQueryHandler extends BaseProgressQueryHandler {
        public MessageListQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            switch (token) {
            case THREAD_LIST_QUERY_TOKEN:
                //add for multi-delete
                mMsgListAdapter.initListMap(cursor);

                int i = cursor.getCount();
                mMsgListAdapter.changeCursor(cursor);

                break;

            case HAVE_LOCKED_MESSAGES_TOKEN:
                long threadId = (Long)cookie;
                confirmDeleteMessageDialog(new DeleteMessageListener(threadId, mQueryHandler,
                        CBMessageListActivity.this), threadId == -1,
                        cursor != null && cursor.getCount() > 0,
                        CBMessageListActivity.this);
                break;

            default:
                Log.e(TAG, "onQueryComplete called with unknown token " + token);
            }
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            switch (token) {
            case DELETE_MESSAGE_TOKEN:
                // Make sure the conversation cache reflects the threads in the DB.
                CBMessage.init(CBMessageListActivity.this);
                CBMessagingNotification.updateNewMessageIndicator(CBMessageListActivity.this);
                startAsyncQuery();
                onContentChanged();
                if (mMsgListAdapter.mIsDeleteMode) {
                    changeDeleteMode();
                }
                if (progress()) {
                    dismissProgressDialog();
                }
                break;
            }
            // If we're deleting the whole conversation, throw away
            // our current working message and bail.
            if (token == ConversationList.DELETE_CONVERSATION_TOKEN) {
                if (progress()) {
                    dismissProgressDialog();
                }
                Conversation.init(CBMessageListActivity.this);
                finish();
            }
        }
    }

  //add for multi-delete
    private void markCheckedState(boolean checkedState) {
        mMsgListAdapter.setItemsValue(checkedState, null);
        mDeleteButton.setEnabled(checkedState);
        int count = mMsgListView.getChildCount();     
        ViewGroup layout = null;
        int childCount = 0;
        for (int i = 0; i < count; i++) {
            layout = (ViewGroup)mMsgListView.getChildAt(i);
            childCount = layout.getChildCount();
            View view = null;
            for (int j = 0; j < childCount; j++) {
                view = layout.getChildAt(j);
                if (view instanceof CheckBox) {
                    if (!mMsgListAdapter.mIsDeleteMode) {
                        ((CheckBox)view).setChecked(false);
                        ((CheckBox)view).setVisibility(View.GONE);
                    } else {
                        ((CheckBox)view).setVisibility(View.VISIBLE);
                        ((CheckBox)view).setChecked(checkedState);
                    }
                    break;
                }
            }
        }
    }

    private void changeDeleteMode() {
        mMsgListAdapter.mIsDeleteMode = !mMsgListAdapter.mIsDeleteMode;
        markCheckedState(false);
        if (mMsgListAdapter.mIsDeleteMode) {
            mSelectPanel.setVisibility(View.VISIBLE);
            mDeletePanel.setVisibility(View.VISIBLE);
            mSelectedAll.setChecked(false);
        } else {
            mSelectPanel.setVisibility(View.GONE);
            mDeletePanel.setVisibility(View.GONE);
        }
        if (!mMsgListAdapter.mIsDeleteMode) {
           mMsgListAdapter.clearList();
        }
    }
    private void log(String format, Object... args) {
        String s = String.format(format, args);
        Log.d(TAG, "[" + Thread.currentThread().getId() + "] " + s);
    }
    
    public static Intent createIntent(Context context, long threadId) {
        Intent intent = new Intent(context, CBMessageListActivity.class);
        if (threadId > 0) {
            intent.setData(Conversation.getUri(threadId));
        }
        return intent;
   }

  //add for multi-delete
    public void onClick(View v) {
        if (v == mDeleteButton) { 
            if (mMsgListAdapter.getSelectedNumber() >= mMsgListAdapter.getCount()) {
                ConversationList.confirmDeleteThreadDialog(
                        new ConversationList.DeleteThreadListener(mConversation.getThreadId(),
                                mQueryHandler, CBMessageListActivity.this),
                            false, false,  CBMessageListActivity.this);
            } else {
                confirmMultiDelete();
            }                
        } else if (v == mCancelButton) {
            changeDeleteMode();
        } else if (v == mSelectPanel) {
            boolean newStatus = !mSelectedAll.isChecked();
            mSelectedAll.setChecked(newStatus);
            markCheckedState(newStatus);
        }
    }
    
    private void confirmMultiDelete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_dialog_title);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setCancelable(true);
        builder.setMessage(R.string.confirm_delete_selected_messages);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
             public void onClick(DialogInterface dialog, int which) {
                 mQueryHandler.setProgressDialog(DeleteProgressDialogUtil.getProgressDialog(
                         CBMessageListActivity.this));
                 mQueryHandler.showProgressDialog();
                 new Thread(new Runnable() {
                     public void run() {
                         
                         Iterator iter = mMsgListAdapter.getItemList().entrySet().iterator();
                         Uri deleteCbUri = null;
                         String[] argsCb = new String[mMsgListAdapter.getSelectedNumber()];
                         int i = 0;
                         while (iter.hasNext()) {
                             @SuppressWarnings("unchecked")
                             Map.Entry<Long, Boolean> entry = (Entry<Long, Boolean>) iter.next();
                             if (entry.getValue()) {
                                 if (entry.getKey() > 0){
                                     Log.i(TAG, "Cb");
                                     argsCb[i] = Long.toString(entry.getKey());
                                     Log.i(TAG, "argsCb[i]" + argsCb[i]);
                                     //deleteSmsUri = ContentUris.withAppendedId(Sms.CONTENT_URI, entry.getKey());
                                     deleteCbUri = CONTENT_URI;
                                     i++;
                                 }
                             }                             
                         }
                         //Uri deleteUri = ContentUris.withAppendedId(CONTENT_URI, entry.getKey());
                        mQueryHandler.startDelete(DELETE_MESSAGE_TOKEN,
                                null, deleteCbUri, FOR_MULTIDELETE, argsCb);
                     }
                 }).start();
             }
         });
        builder.setNegativeButton(R.string.no, null);
        builder.show();
     }
}

