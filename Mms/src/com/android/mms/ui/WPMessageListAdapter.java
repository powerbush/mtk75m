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
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.BaseColumns;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.util.Config;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.CursorAdapter;
import android.widget.ListView;
import com.android.mms.R;

import com.android.mms.R;
import android.provider.Telephony.WapPush;
import android.provider.Telephony.Mms;

import com.mediatek.xlog.Xlog;

public class WPMessageListAdapter extends CursorAdapter {
    private static final String WP_TAG = "Mms/WapPush";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = Config.LOGV && DEBUG;
    //add for multi-delete
    public boolean mIsDeleteMode = false;
    private Map<Long, Boolean> mListItem;

    static final String[] WP_PROJECTION = new String[] {
        // TODO: should move this symbol into com.android.mms.telephony.Telephony.
    	BaseColumns._ID,
    	WapPush.THREAD_ID,
        WapPush.ADDR,
        WapPush.SERVICE_ADDR,
        WapPush.READ,
        WapPush.DATE,
        WapPush.TYPE,
        WapPush.SIID,
        WapPush.URL,
        WapPush.CREATE,
        WapPush.EXPIRATION,
        WapPush.ACTION,
        WapPush.TEXT,
        WapPush.SIM_ID,
        WapPush.LOCKED,
        WapPush.ERROR
    };

    // The indexes of the default columns which must be consistent
    // with above PROJECTION.  
    static final int COLUMN_ID                  = 0;
    static final int COLUMN_THREAD_ID           = 1;
    static final int COLUMN_WPMS_ADDR           = 2;
    static final int COLUMN_WPMS_SERVICE_ADDR   = 3;
    static final int COLUMN_WPMS_READ           = 4;
    static final int COLUMN_WPMS_DATE           = 5;
    static final int COLUMN_WPMS_TYPE           = 6;
    static final int COLUMN_WPMS_SIID           = 7;
    static final int COLUMN_WPMS_URL            = 8;
    static final int COLUMN_WPMS_CREATE         = 9;
    static final int COLUMN_WPMS_EXPIRATION     = 10;
    static final int COLUMN_WPMS_ACTION         = 11;
    static final int COLUMN_WPMS_TEXT           = 12;
    static final int COLUMN_WPMS_SIMID          = 13;
    static final int COLUMN_WPMS_LOCKED         = 14;
    static final int COLUMN_WPMS_ERROR          = 15;

    private static final int CACHE_SIZE         = 50;

    protected LayoutInflater mInflater;
    private final ListView mListView;
    private final LinkedHashMap<Long, WPMessageItem> mMessageItemCache;
    private final WPColumnsMap mColumnsMap;
    private OnDataSetChangedListener mOnDataSetChangedListener;
    private Handler mMsgListItemHandler;
    private Pattern mHighlight;
    private Context mContext;

    public WPMessageListAdapter(
            Context context, Cursor c, ListView listView,
            boolean useDefaultColumnsMap, Pattern highlight) {
        super(context, c, false /* auto-requery */);
        mContext = context;
        mHighlight = highlight;

        mInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mListView = listView;
        mMessageItemCache = new LinkedHashMap<Long, WPMessageItem>(
                    10, 1.0f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > CACHE_SIZE;
            }
        };

        mListItem = new HashMap<Long, Boolean>();
        
        if (useDefaultColumnsMap) {
            mColumnsMap = new WPColumnsMap();
        } else {
            mColumnsMap = new WPColumnsMap(c);
        }
    }

    private OnCreateContextMenuListener mItemOnCreateContextMenuListener;
    
    public void setItemOnCreateContextMenuListener(OnCreateContextMenuListener l){
    	mItemOnCreateContextMenuListener = l;
    }
    
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (view instanceof WPMessageListItem) {
            int type = cursor.getInt(mColumnsMap.mColumnWpmsType);
            long msgId = cursor.getLong(mColumnsMap.mColumnMsgId);

            WPMessageItem msgItem = getCachedMessageItem(type, msgId, cursor);
            if (msgItem != null) {
                WPMessageListItem mli = (WPMessageListItem) view;

              //for multi-delete
                if (mIsDeleteMode) {
                     if (mListItem.get(msgId) == null) {
                     	mListItem.put(msgId, false);
                     } else {
                     	msgItem.setSelectedState(mListItem.get(msgId));
                     }
                } else {
                	msgItem.setSelectedState(false);
                }
                
                mli.bind(msgItem, mIsDeleteMode);
                mli.setMsgListItemHandler(mMsgListItemHandler);
                
                //set Listenner
                View itemContainer = mli.getItemContainer();
                itemContainer.setOnCreateContextMenuListener(mItemOnCreateContextMenuListener);
            }
        }
    }

    public interface OnDataSetChangedListener {
        void onDataSetChanged(WPMessageListAdapter adapter);
        void onContentChanged(WPMessageListAdapter adapter);
    }

    public void setOnDataSetChangedListener(OnDataSetChangedListener l) {
        mOnDataSetChangedListener = l;
    }

    public void setMsgListItemHandler(Handler handler) {
        mMsgListItemHandler = handler;
    }

    //add for multi-delete
    public void changeSelectedState(long listId) {
    	mListItem.put(listId, !mListItem.get(listId));
    	
    }
    public  Map<Long, Boolean> getItemList() {
    	return mListItem;
    	
    }
    
    public void initListMap(Cursor cursor) {
    	if (cursor != null) {
    		long itemId = 0;
    		while (cursor.moveToNext()) {	  			
    			itemId = cursor.getLong(mColumnsMap.mColumnMsgId); 		   
    			if (mListItem.get(itemId) == null) {
    	        	mListItem.put(itemId, false);
    	        }
    		}
    	}  
    }
    
    public void setItemsValue(boolean value, long[] keyArray) {
    	Iterator iter = mListItem.entrySet().iterator();
    	//keyArray = null means set the all item
    	if (keyArray == null) {
    		while (iter.hasNext()) {
        		@SuppressWarnings("unchecked")
    			Map.Entry<Long, Boolean> entry = (Entry<Long, Boolean>) iter.next();
        		entry.setValue(value);
        	}
    	} else {
    		for (int i = 0; i < keyArray.length; i++) {
    			mListItem.put(keyArray[i], value);
    		}
    	}
    }
    
    public void clearList() {
    	setItemsValue(false, null);
    }
    
    public int getSelectedNumber() {
    	Iterator iter = mListItem.entrySet().iterator();
    	int number = 0;
    	while (iter.hasNext()) {
    		@SuppressWarnings("unchecked")
			Map.Entry<Long, Boolean> entry = (Entry<Long, Boolean>) iter.next();
    		if (entry.getValue()) {
    			number++;
    		}
    	}
    	return number;
    }
    
    public void notifyImageLoaded(String address) {
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        if (LOCAL_LOGV) {
            Xlog.v(WP_TAG, "WPMessageListAdapter.notifyDataSetChanged().");
        }

        mListView.setSelection(mListView.getCount());
        mMessageItemCache.clear();

        if (mOnDataSetChangedListener != null) {
            mOnDataSetChangedListener.onDataSetChanged(this);
        }
    }

    @Override
    protected void onContentChanged() {
        if (getCursor() != null && !getCursor().isClosed()) {
            if (mOnDataSetChangedListener != null) {
                mOnDataSetChangedListener.onContentChanged(this);
            }
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.wp_message_list_item, parent, false);
    }

    public WPMessageItem getCachedMessageItem(int type, long msgId, Cursor c) {
        WPMessageItem item = null;        
        try {
            item = mMessageItemCache.get(msgId);
        } catch (IndexOutOfBoundsException e){
            Xlog.e(WP_TAG, "WPMessageListAdapter: " + e.getMessage());
        }
        
        if (item == null) {
        	item = new WPMessageItem(mContext, type, c, mColumnsMap, mHighlight);
        	mMessageItemCache.put(msgId, item);
        }
        return item;
    }


    
    public static class WPColumnsMap {
    	public int mColumnMsgId;
        public int mColumnWpmsThreadId;
        public int mColumnWpmsAddr;
        public int mColumnWpmsServiceAddr;
        public int mColumnWpmsRead;
        public int mColumnWpmsDate;
        public int mColumnWpmsType;
        public int mColumnWpmsSiid;
        public int mColumnWpmsURL;
        public int mColumnWpmsCreate;
        public int mColumnWpmsExpiration;
        public int mColumnWpmsAction;
        public int mColumnWpmsText;        
        public int mColumnWpmsSimId;
        public int mColumnWpmsLocked;
        public int mColumnWpmsError;

        public WPColumnsMap() {
        	mColumnMsgId              = COLUMN_ID;
        	mColumnWpmsThreadId       = COLUMN_THREAD_ID;
            mColumnWpmsAddr           = COLUMN_WPMS_ADDR;
            mColumnWpmsServiceAddr    = COLUMN_WPMS_SERVICE_ADDR;
            mColumnWpmsRead           = COLUMN_WPMS_READ;
            mColumnWpmsDate           = COLUMN_WPMS_DATE;
            mColumnWpmsType           = COLUMN_WPMS_TYPE;
            mColumnWpmsSiid           = COLUMN_WPMS_SIID;
            mColumnWpmsURL            = COLUMN_WPMS_URL;
            mColumnWpmsCreate         = COLUMN_WPMS_CREATE;
            mColumnWpmsExpiration     = COLUMN_WPMS_EXPIRATION;
            mColumnWpmsAction         = COLUMN_WPMS_ACTION;
            mColumnWpmsText           = COLUMN_WPMS_TEXT;
            mColumnWpmsSimId          = COLUMN_WPMS_SIMID;
            mColumnWpmsLocked         = COLUMN_WPMS_LOCKED;
            mColumnWpmsError          = COLUMN_WPMS_ERROR;
        }

        public WPColumnsMap(Cursor cursor) {
            // Ignore all 'not found' exceptions since the custom columns
            // may be just a subset of the default columns.            
            try {
                mColumnMsgId = cursor.getColumnIndexOrThrow(BaseColumns._ID);
            } catch (IllegalArgumentException e) {
                Xlog.w("colsMap", e.getMessage());
            }
            
            try {
            	mColumnWpmsThreadId = cursor.getColumnIndexOrThrow(WapPush.THREAD_ID);
            } catch (IllegalArgumentException e) {
                Xlog.w("colsMap", e.getMessage());
            }

            try {
            	mColumnWpmsAddr = cursor.getColumnIndexOrThrow(WapPush.ADDR);
            } catch (IllegalArgumentException e) {
                Xlog.w("colsMap", e.getMessage());
            }

            try {
            	mColumnWpmsServiceAddr = cursor.getColumnIndexOrThrow(WapPush.SERVICE_ADDR);
            } catch (IllegalArgumentException e) {
                Xlog.w("colsMap", e.getMessage());
            }

            try {
            	mColumnWpmsRead = cursor.getColumnIndexOrThrow(WapPush.READ);
            } catch (IllegalArgumentException e) {
                Xlog.w("colsMap", e.getMessage());
            }

            try {
            	mColumnWpmsDate = cursor.getColumnIndexOrThrow(WapPush.DATE);
            } catch (IllegalArgumentException e) {
                Xlog.w("colsMap", e.getMessage());
            }

            try {
            	mColumnWpmsType = cursor.getColumnIndexOrThrow(WapPush.TYPE);
            } catch (IllegalArgumentException e) {
                Xlog.w("colsMap", e.getMessage());
            }

            try {
            	mColumnWpmsSiid = cursor.getColumnIndexOrThrow(WapPush.SIID);
            } catch (IllegalArgumentException e) {
                Xlog.w("colsMap", e.getMessage());
            }

            try {
            	mColumnWpmsURL = cursor.getColumnIndexOrThrow(WapPush.URL);
            } catch (IllegalArgumentException e) {
                Xlog.w("colsMap", e.getMessage());
            }

            try {
            	mColumnWpmsCreate = cursor.getColumnIndexOrThrow(WapPush.CREATE);
            } catch (IllegalArgumentException e) {
                Xlog.w("colsMap", e.getMessage());
            }

            try {
            	mColumnWpmsExpiration = cursor.getColumnIndexOrThrow(WapPush.EXPIRATION);
            } catch (IllegalArgumentException e) {
                Xlog.w("colsMap", e.getMessage());
            }

            try {
            	mColumnWpmsAction = cursor.getColumnIndexOrThrow(WapPush.ACTION);
            } catch (IllegalArgumentException e) {
                Xlog.w("colsMap", e.getMessage());
            }

            try {
            	mColumnWpmsText = cursor.getColumnIndexOrThrow(WapPush.TEXT);
            } catch (IllegalArgumentException e) {
                Xlog.w("colsMap", e.getMessage());
            }

            try {
            	mColumnWpmsSimId = cursor.getColumnIndexOrThrow(WapPush.SIM_ID);
            } catch (IllegalArgumentException e) {
                Xlog.w("colsMap", e.getMessage());
            }
            
            try {
            	mColumnWpmsLocked = cursor.getColumnIndexOrThrow(WapPush.LOCKED);
            } catch (IllegalArgumentException e) {
                Xlog.w("colsMap", e.getMessage());
            }
            
            try {
            	mColumnWpmsError = cursor.getColumnIndexOrThrow(WapPush.ERROR);
            } catch (IllegalArgumentException e) {
                Xlog.w("colsMap", e.getMessage());
            }
        }
    }
}
