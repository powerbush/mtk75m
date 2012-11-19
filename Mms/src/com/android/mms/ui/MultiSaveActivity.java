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

import com.android.mms.R;
import com.android.mms.model.FileAttachmentModel;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.net.Uri;

import com.google.android.mms.ContentType;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduPart;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import com.mediatek.xlog.Xlog;
/**
 * This activity provides a list view of existing conversations.
 */
public class MultiSaveActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "Mms/MultiSaveActivity";
    private MultiSaveListAdapter mListAdapter;
    private ListView mMultiSaveList;
    private MultiSaveHeaderItem mHeaderView;
    private Button mSaveButton;
    private Button mCancelButton;
    private ContentResolver mContentResolver;
    private boolean needQuit = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.save);
        mContentResolver = getContentResolver();
        setContentView(R.layout.multi_save);
        mCancelButton = (Button)findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(this);
        mSaveButton = (Button)findViewById(R.id.done);
        mSaveButton.setOnClickListener(this);
        
        mMultiSaveList = (ListView) findViewById(R.id.item_list);
        mHeaderView = (MultiSaveHeaderItem) findViewById(R.id.header_view);
        mHeaderView.setOnClickListener(this);
        mMultiSaveList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (view != null) {
                    ((MultiSaveListItem) view).clickListItem();
                    mListAdapter.changeSelectedState(position);               		
                    if (mListAdapter.getSelectedNumber() > 0) {
                        mSaveButton.setEnabled(true);
                        if (mListAdapter.isAllSelected()) {
                            mHeaderView.setCheckBoxState(true);
                        } else {
                            mHeaderView.setCheckBoxState(false);
                        }
                    } else {
                        mSaveButton.setEnabled(false);
                        mHeaderView.setCheckBoxState(false);
                    }
                }
            }
        });
        
        Intent i = getIntent();
        long msgId = -1;
        if (i != null && i.hasExtra("msgid")) {
            msgId = i.getLongExtra("msgid", -1);
        }
        initListAdapter(msgId); 
        initActivityState(savedInstanceState);

    }

    private void initActivityState(Bundle savedInstanceState) {
    	if (savedInstanceState != null) {
    		boolean selectedAll = savedInstanceState.getBoolean("is_all_selected");
    		if (selectedAll) {
    			mListAdapter.setItemsValue(true, null);
    			return;
    		} 
    		
    		int [] selectedItems = savedInstanceState.getIntArray("select_list");
    		if (selectedItems != null) {
    			mListAdapter.setItemsValue(true, selectedItems);
    		}
    		
    	} else {
            Xlog.i(TAG, "initActivityState, fresh start select all");
            mHeaderView.setCheckBoxState(true);
            markCheckedState(true);
            mSaveButton.setEnabled(true);
            mListAdapter.setItemsValue(true, null);
        }
    }
    
    private void initListAdapter(long  msgId) {
        PduBody body = ComposeMessageActivity.getPduBody(MultiSaveActivity.this, msgId);
        if (body == null) {
            Xlog.e(TAG, "initListAdapter, oops, getPduBody returns null");
            return;
        }

        int partNum = body.getPartsNum();
        ArrayList<MultiSaveListItemData> attachments = new ArrayList<MultiSaveListItemData>(partNum);
        for(int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            final String type = new String(part.getContentType());
            if (ContentType.isImageType(type) || ContentType.isVideoType(type) ||
                    ContentType.isAudioType(type) || FileAttachmentModel.isSupportedFile(part)) {
                attachments.add(new MultiSaveListItemData(this, part, msgId));
            }
        }
        attachments.trimToSize();

        mListAdapter = new MultiSaveListAdapter(this, attachments);
        mMultiSaveList.setAdapter(mListAdapter);
    }
   
    @Override
    public void onSaveInstanceState(Bundle outState) {
        Xlog.v(TAG, "onSaveInstanceState, with bundle " + outState);
        super.onSaveInstanceState(outState);      
        if (mListAdapter != null) {
        	if (mListAdapter.isAllSelected()) {
        		outState.putBoolean("is_all_selected", true);
        	} else if (mListAdapter.getSelectedNumber() == 0) {
        		return;
        	} else { 
                int[] checkedArray = new int[mListAdapter.getSelectedNumber()];
                ArrayList<MultiSaveListItemData> list = mListAdapter.getItemList();
                for (int i = 0; i < checkedArray.length; i++) {
                    if (list.get(i).isSelected()) {
                        checkedArray[i] = i;
                    }
                }
		    	outState.putIntArray("select_list", checkedArray);
        	}
        }     
    }
    
    private void markCheckedState(boolean checkedState) {
        int count = mMultiSaveList.getChildCount();     
        Xlog.v(TAG, "markCheckState count is " + count + ", state is " + checkedState);
        for (int i = 0; i < count; i++) {
            RelativeLayout layout = (RelativeLayout)mMultiSaveList.getChildAt(i);
            Xlog.v(TAG, "markCheckedState for view" + layout);
            int childCount = layout.getChildCount();
            
            for (int j = 0; j < childCount; j++) {
                View view = layout.getChildAt(j);
                Xlog.v(TAG, "markCheckedState for view" + view);
                if (view instanceof CheckBox) {
                    ((CheckBox)view).setChecked(checkedState);
                    break;
                }
            }
        }
    }

    public void onClick(View v) {
    	if (v == mSaveButton){ 
            boolean succeeded = false;
            succeeded = copyMedia();
            Intent i = new Intent();
            i.putExtra("multi_save_result", succeeded);
            setResult(RESULT_OK, i);
            finish();
    	} else if (v == mCancelButton) {
    		finish();
    	} else if (v == mHeaderView) {
            boolean enabled = !mHeaderView.isChecked();
            mHeaderView.setCheckBoxState(enabled);
            markCheckedState(enabled);
            mSaveButton.setEnabled(enabled);
            mListAdapter.setItemsValue(enabled, null);
        }
    } 
	
    /**
     * Copies media from an Mms to the "download" directory on the SD card
     * @param msgId
     */
    private boolean copyMedia() {
        boolean result = true;

        ArrayList<MultiSaveListItemData> list = mListAdapter.getItemList();
        int size = list.size();
        for(int i = 0; i < size; i++) {
            if (!list.get(i).isSelected()) {
                continue;
            }
            PduPart part = list.get(i).getPduPart();
            final String filename = list.get(i).getName();
            final String type = new String(part.getContentType());
            if (ContentType.isImageType(type) || ContentType.isVideoType(type) || ContentType.isAudioType(type)
                    || FileAttachmentModel.isSupportedFile(part)) {
                result &= copyPart(part, filename);   // all parts have to be successful for a valid result.
            }
        }
        return result;
    }

    private boolean copyPart(PduPart part, String filename) {
        Uri uri = part.getDataUri();
        Xlog.i(TAG, "copyPart, copy part into sdcard uri " + uri);

        InputStream input = null;
        FileOutputStream fout = null;
        try {
            input = mContentResolver.openInputStream(uri);
            if (input instanceof FileInputStream) {
                FileInputStream fin = (FileInputStream) input;
                // Depending on the location, there may be an
                // extension already on the name or not
                final String dir = Environment.getExternalStorageDirectory() + "/"
                                + Environment.DIRECTORY_DOWNLOADS  + "/";
                Xlog.i(TAG, "copyPart,  file full path is " + dir + filename);
                File file = getUniqueDestination(dir + filename);

                // make sure the path is valid and directories created for this file.
                File parentFile = file.getParentFile();
                if (!parentFile.exists() && !parentFile.mkdirs()) {
                    Xlog.e(TAG, "[MMS] copyPart: mkdirs for " + parentFile.getPath() + " failed!");
                    return false;
                }

                fout = new FileOutputStream(file);

                byte[] buffer = new byte[8000];
                int size = 0;
                while ((size=fin.read(buffer)) != -1) {
                    fout.write(buffer, 0, size);
                }

                // Notify other applications listening to scanner events
                // that a media file has been added to the sd card
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.fromFile(file)));
            }
        } catch (IOException e) {
            // Ignore
            Xlog.e(TAG, "IOException caught while opening or reading stream", e);
            return false;
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    // Ignore
                    Xlog.e(TAG, "IOException caught while closing stream", e);
                    return false;
                }
            }
            if (null != fout) {
                try {
                    fout.close();
                } catch (IOException e) {
                    // Ignore
                    Xlog.e(TAG, "IOException caught while closing stream", e);
                    return false;
                }
            }
        }
        return true;
    }

    private File getUniqueDestination(String fileName) {
        final int index = fileName.indexOf(".");
        final String extension = fileName.substring(index + 1, fileName.length());
        final String base = fileName.substring(0, index);
        File file = new File(base + "." + extension);
        for (int i = 2; file.exists(); i++) {
            file = new File(base + "_" + i + "." + extension);
        }
        return file;
    }
}
