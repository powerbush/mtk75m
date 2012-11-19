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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.android.internal.telephony.ITelephony;
import com.android.mms.LogTag;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.ui.ConversationList.BaseProgressQueryHandler;
import com.android.mms.util.DraftCache;
import com.android.mms.util.Recycler;
import android.database.sqlite.SqliteWrapper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.os.Bundle;
import android.os.Handler;
import android.os.ServiceManager;
import android.preference.PreferenceManager;
import android.provider.Browser;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.telephony.SmsManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.WeakList;
import android.net.Uri;
import android.provider.Telephony.Threads;
import com.mediatek.featureoption.FeatureOption;

/**
 * This activity provides a list view of existing conversations.
 */
public class MultiDeleteActivity extends Activity
          implements View.OnClickListener, DraftCache.OnDraftChangedListener {
    public static final WeakList<MultiDeleteActivity> INSTANCES = new WeakList<MultiDeleteActivity>(2);
    private static final String TAG = "MultiDeleteActivity";
    private ThreadListQueryHandler mQueryHandler;
    private MultiDeleteListAdapter mListAdapter;
    private ListView mMultiDeleteList;
    private long mThreadID = -1;
    private MultiDeleteHeaderItem mHeaderView;
    private Button mDeleteButton;
    private Button mCancelButton;
    private ContentResolver mContentResolver;
    private boolean needQuit = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setTitle(R.string.delete);
        mContentResolver = getContentResolver();
        setContentView(R.layout.multi_delete);
        mQueryHandler = new ThreadListQueryHandler(mContentResolver);
        mCancelButton = (Button)findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(this);
        mDeleteButton = (Button)findViewById(R.id.delete);
        mDeleteButton.setOnClickListener(this);
        mDeleteButton.setEnabled(false);
        
        mMultiDeleteList = (ListView) findViewById(R.id.item_list);
        mHeaderView = (MultiDeleteHeaderItem) findViewById(R.id.header_item);
        if (FeatureOption.MTK_THEMEMANAGER_APP) {
            mHeaderView.setThemeColor("mms_selected_all_bg_color", 0xffe2e3e2);
        } else {
            mHeaderView.setBackgroundColor(0xffe2e3e2);
        }

        mHeaderView.setOnClickListener(this);
        mMultiDeleteList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (view != null) {
                    ((MultiDeleteListItem) view).clickListItem();
                    long itemdId = -1;                		                		
                    itemdId = ((MultiDeleteListItem) view).getMultiDeleteHeader().getThreadId();               		
                    mListAdapter.changeSelectedState(itemdId);               		
                    if (mListAdapter.getSelectedNumber() > 0) {
                        mDeleteButton.setEnabled(true);
                        if (mListAdapter.isAllSelected()) {
                            mHeaderView.SetCheckBoxState(true);
                        } else {
                            mHeaderView.SetCheckBoxState(false);
                        }
                    } else {
                        mDeleteButton.setEnabled(false);
                        mHeaderView.SetCheckBoxState(false);
                    }
                }
            }
        });
        
        initListAdapter(); 
        initActivityState(savedInstanceState);
        
        synchronized (INSTANCES) {
            INSTANCES.add(this);
        }
    }

    private void initActivityState(Bundle savedInstanceState) {
    	if (savedInstanceState != null) {
    		boolean selectedAll = savedInstanceState.getBoolean("is_all_selected");
    		if (selectedAll) {
    			mListAdapter.setItemsValue(true, null);
    			return;
    		} 
    		
    		long [] selectedItems = savedInstanceState.getLongArray("select_list");
    		if (selectedItems != null) {
    			mListAdapter.setItemsValue(true, selectedItems);
    		}
    		
    	}
    	
    }
    
    private final MultiDeleteListAdapter.OnContentChangedListener mContentChangedListener =
        new MultiDeleteListAdapter.OnContentChangedListener() {
		public void onContentChanged(MultiDeleteListAdapter adapter) {
			startAsyncQuery(MultiDeleteListAdapter.THREAD_LIST_QUERY_TOKEN);
			
		}          
    };

    private void initListAdapter() {
    	Intent it = getIntent();
    	mThreadID = it.getLongExtra("threadId", -1);
        mListAdapter = new MultiDeleteListAdapter(this, null);
        mListAdapter.setOnContentChangedListener(mContentChangedListener);
        mMultiDeleteList.setAdapter(mListAdapter);
        mMultiDeleteList.setRecyclerListener(mListAdapter);
    }
   
    @Override
    protected void onNewIntent(Intent intent) {
        // Handle intents that occur after the activity has already been created.
    	startAsyncQuery(MultiDeleteListAdapter.THREAD_LIST_QUERY_TOKEN);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);      
        if (mListAdapter != null) {
        	if (mListAdapter.isAllSelected()) {
        		outState.putBoolean("is_all_selected", true);
        	} else if (mListAdapter.getSelectedNumber() == 0) {
        		return;
        	}
        	else {
        		long [] checkedArray = new long[mListAdapter.getSelectedNumber()];
        		Iterator iter = mListAdapter.getItemList().entrySet().iterator();
        		int i = 0;
		    	while (iter.hasNext()) {
		    		@SuppressWarnings("unchecked")
					Map.Entry<Long, Boolean> entry = (Entry<Long, Boolean>) iter.next();
		    		if (entry.getValue()) {		    			
		    			checkedArray[i] = entry.getKey();
		    			i++;
		    		}
		    	}	
		    	outState.putLongArray("select_list", checkedArray);
        	}
        	
        }     
    }
    
    @Override
    protected void onResume() {   
        startAsyncQuery(MultiDeleteListAdapter.THREAD_LIST_QUERY_TOKEN);   
        super.onResume();
    }
    
    @Override
    protected void onStart() {
        super.onStart();

        DraftCache.getInstance().addOnDraftChangedListener(this);
        if (!Conversation.loadingThreads()) {
            Contact.invalidateCache();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        DraftCache.getInstance().removeOnDraftChangedListener(this);
        mListAdapter.changeCursor(null);
              
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        synchronized (INSTANCES) {
            INSTANCES.remove(this);
        }
    }

    public void onDraftChanged(final long threadId, final boolean hasDraft) {
        // Run notifyDataSetChanged() on the main thread.
        mQueryHandler.post(new Runnable() {
            public void run() {
                mListAdapter.notifyDataSetChanged();
            }
        });
    }

    private void startAsyncQuery(int token) {
    	setProgressBarIndeterminateVisibility(true);
    	switch (token) {
    	case MultiDeleteListAdapter.THREAD_LIST_QUERY_TOKEN: {
    		try {
                Conversation.startQueryForAll(mQueryHandler, MultiDeleteListAdapter.THREAD_LIST_QUERY_TOKEN);
            } catch (SQLiteException e) {
                SqliteWrapper.checkSQLiteException(this, e);
            }
    	}
    	break;
    	default:
    		Log.e(TAG, "invalid token");
    		
    	}
        
    }
      
    @Override
    public boolean onSearchRequested() {
        startSearch(null, false, null /*appData*/, false);
        return true;
    }  

    private final class ThreadListQueryHandler extends BaseProgressQueryHandler {
        public ThreadListQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
        	setProgressBarIndeterminateVisibility(false);
            switch (token) {
            case MultiDeleteListAdapter.THREAD_LIST_QUERY_TOKEN:
                mListAdapter.initListMap(cursor);
                mListAdapter.changeCursor(cursor);            
                if (mListAdapter.isAllSelected()) {
                	mHeaderView.SetCheckBoxState(true);
                } else {
                	mHeaderView.SetCheckBoxState(false);
                }
                return;
            default:
                Log.e(TAG, "onQueryComplete called with unknown token " + token);
            }
            startManagingCursor(cursor);
        }    
        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            switch (token) {
            case ConversationList.DELETE_CONVERSATION_TOKEN:              
            	Log.i(TAG, "thread delete complete,thread id = " + mThreadID);
            	if (mListAdapter.isAllSelected() || needQuit) {
            		finish();
            	}
            	if (progress()) {
            	    dismissProgressDialog();
            	}
                break;                          
            default:
            	Log.e(TAG, "invaild token");	        
            }
        }
    }

    private void markCheckedState(boolean checkedState) {
        int count = mMultiDeleteList.getChildCount();  
        RelativeLayout layout = null;
        int childCount = 0;
        View view = null;
        for (int i = 0; i < count; i++) {
        	layout = (RelativeLayout)mMultiDeleteList.getChildAt(i);
            childCount = layout.getChildCount();
            
            for (int j = 0; j < childCount; j++) {
            	view = layout.getChildAt(j);
                if (view instanceof CheckBox) {
                    ((CheckBox)view).setChecked(checkedState);
                    break;
                }
            }
        }
    }

    public void onClick(View v) {
    	if (v == mDeleteButton){ 
			needQuit = false;
    		if (mListAdapter.isAllSelected()) {
    			ConversationList.confirmDeleteThreadDialog(
    					new ConversationList.DeleteThreadListener(-1, mQueryHandler, MultiDeleteActivity.this),
    					true, false, MultiDeleteActivity.this); 
    		} else {
    			confirmMultiDelete();
    		}
    	} else if (v == mCancelButton) {
    		finish();
    	} else if (v == mHeaderView) {
            boolean enabled = !mHeaderView.isChecked();
            mHeaderView.SetCheckBoxState(enabled);
            markCheckedState(enabled);
            mDeleteButton.setEnabled(enabled);
            mListAdapter.setItemsValue(enabled, null);
        }
    } 
	
	private void confirmMultiDelete() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_dialog_title);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setCancelable(true);
        builder.setMessage(R.string.confirm_delete_selected_theads);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			    mQueryHandler.setProgressDialog(DeleteProgressDialogUtil.getProgressDialog(
			            MultiDeleteActivity.this));
			    mQueryHandler.showProgressDialog();
				new Thread(new Runnable() {
		            public void run() {
		                mQueryHandler.setMax(mListAdapter.getSelectedNumber());
		            	Iterator iter = mListAdapter.getItemList().entrySet().iterator();
				    	while (iter.hasNext()) {
				    		@SuppressWarnings("unchecked")
				    		Map.Entry<Long, Boolean> entry = (Entry<Long, Boolean>) iter.next();
				    		if (entry.getValue()) {				    							    							    			
				    			long threadId = entry.getKey();
				    			markThreadRead(threadId);                    
				    			Conversation.startDelete(mQueryHandler, 
				    					ConversationList.DELETE_CONVERSATION_TOKEN, true, threadId);		                		
				    		}
				    	needQuit = true;
				    	}					    			    	
		            }
		        }).start();  	
			}
		});
        builder.setNegativeButton(R.string.no, null);
        builder.show();
	}

	private void markThreadRead(long threadId){
		//mark the thread as read
    	Uri threadUri = ContentUris.withAppendedId(Threads.CONTENT_URI, threadId);
    	ContentValues readContentValues = new ContentValues(1);
    	readContentValues.put("read", 1);
    	mContentResolver.update(threadUri, readContentValues,
                "read=0", null);
	}
}

/**
 * The common code about delete progress dialogs.
 */
class DeleteProgressDialogUtil {
    
    /**
     * Gets a delete progress dialog.
     * @param context the activity context.
     * @return the delete progress dialog.
     */
    public static ProgressDialog getProgressDialog(Context context) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage(context.getString(R.string.deleting));
        dialog.setMax(1); /* default is one complete */
        return dialog;
    }
}
