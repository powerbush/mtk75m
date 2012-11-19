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
import com.android.mms.data.VCardEntryList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.pim.vcard.VCardConfig;
import android.pim.vcard.VCardEntry;
import android.pim.vcard.VCardEntry.PhoneData;
import android.pim.vcard.VCardEntry.EmailData;
import android.pim.vcard.VCardEntryConstructor;
import android.pim.vcard.exception.VCardNestedException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class VCardDetailMulti extends Activity implements OnClickListener{
    
    public static final String TAG  = "Mms/MultiVcard";
    
    private Uri mUri;
    private int mVCardType;
    private String mVCardChSet;
    private String mSaveText;
    private LinearLayout mSelectPanel;
    private ListView mListView;
    private TextView mSelectAllText;
    private CheckBox mSelectAllBox;
    private Button mSaveButton;
    private Button mCancelButton;
    private ProgressDialog mProgressDialog;
    private Handler mHandler;
    
    private static List<VCardEntry> mDataList;
    private Set<Integer> mSelectedPositionsSet = new HashSet<Integer>();
    private boolean[] mSelectedPositions;
    int mSelectedCount = 0;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.vcard_detail_multi);
        
        mUri = getIntent().getData();
        mVCardType = getIntent().getIntExtra("type", VCardConfig.VCARD_TYPE_UNKNOWN);
        mVCardChSet = getIntent().getStringExtra("charset");
        mHandler = new Handler();
        initRes();
        showProgressDialog();
        new Thread() {
            public void run() {
                doActuallyReadOneVCard();
                dismissProgressDialog();
                mHandler.post(refreashUI);
            }
        }.start();
    }
    
    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = null;
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            initProgressDialog();
        }
        mProgressDialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mProgressDialog != null && !mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
    
    private void initProgressDialog() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(getString(R.string.please_wait));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    }
    
    private void initRes() {
        mListView = (ListView) findViewById(R.id.vcard_list);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(VCardDetailMulti.this, VCardDetailSingle.class);
                intent.setData(mUri);
                intent.putExtra("item_id", position);
                startActivity(intent);
            }
            
        });
        
        mSelectPanel = (LinearLayout) findViewById(R.id.select_panel);        
        mSelectAllText = (TextView) findViewById(R.id.select_all);
        mSelectAllBox = (CheckBox) findViewById(R.id.select_all_checked);
        mSelectAllBox.setChecked(false);
        mSelectPanel.setOnClickListener(this);
        
        mSaveText = getResources().getString(R.string.save);
        mSaveButton = (Button) findViewById(R.id.save);
        mSaveButton.setOnClickListener(this);
        updateSaveButtonState();
        
        mCancelButton = (Button) findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(this);
    }
    
    private Runnable refreashUI = new Runnable() {
        public void run() {
            VCardAdapter adapter = new VCardAdapter(VCardDetailMulti.this, mDataList);
            mListView.setAdapter(adapter);
        }
        
    };
    
    private Runnable toastAndFinish = new Runnable() {
        public void run() {
            Toast.makeText(VCardDetailMulti.this, getString(R.string.save_message_to_sim_successful),
                Toast.LENGTH_SHORT).show();
            finish();
        }
      
    };
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissProgressDialog();
    }
    
    private void doActuallyReadOneVCard() {
        if (mUri == null)
            return;
        if (mVCardType == VCardConfig.VCARD_TYPE_UNKNOWN) {
            mVCardType = VCardConfig.getVCardTypeFromString(
            "import");
        }
        VCardEntryConstructor builder = new VCardEntryConstructor(mVCardType, null, mVCardChSet);
        final VCardEntryList entryList = new VCardEntryList();
        builder.addEntryHandler(entryList);
        
        try {
            if (!MessageUtils.readOneVCardFile(mUri, mVCardType, builder, false, getContentResolver())) {
                return;
            }
        } catch (VCardNestedException e) {
            Log.e(TAG, "Never reach here.");
        }
        mDataList = entryList.getCreatedUris();
        mSelectedPositions = new boolean[mDataList.size()];
        return;
    }
    
    public static VCardEntry getVCardAt(int id) {
        if (id >= 0 && id < mDataList.size()) {
            return mDataList.get(id);
        }
        return null;
    }
    
    @Override
    public void onClick(View v) {
        if (v == mSelectPanel) {
            mSelectAllBox.setChecked(!mSelectAllBox.isChecked());
            boolean isChecked = mSelectAllBox.isChecked();
            new Thread() {
                @Override
                public void run() {
                    synchronized (mSelectedPositionsSet) {
                        for (int k = 0; k < mSelectedPositions.length; ++k) {
                            mSelectedPositions[k] = (mSelectAllBox.isChecked()) ? true : false;
                            if (mSelectAllBox.isChecked())
                                mSelectedPositionsSet.add(k);
                            else
                                mSelectedPositionsSet.remove(k);
                        }
                    }
                }

            }.start();
            updateCheckBoxes(isChecked);
            updateSaveButtonState();
            return;
        } else if (v == mSaveButton) {
            if (mSelectedCount == 0) {
                finish();
                return;
            } 
            showProgressDialog();
            new Thread() {
                @Override
                public void run() {
                    synchronized (mSelectedPositionsSet) {
                        for (Iterator<Integer> it = mSelectedPositionsSet.iterator(); it.hasNext();) {
                            int position = it.next();
                            getVCardAt(position).pushIntoContentResolver(
                                getContentResolver());
                        }
                    }
                    dismissProgressDialog();
                    mHandler.post(toastAndFinish);
                }

            }.start();
            
        } else if (v == mCancelButton) {
            finish();
        }
    }
    
    private void updateSaveButtonState() {
        mSaveButton.setText(mSaveText + "(" + mSelectedCount + ")");
        if (mSelectedCount == 0) {
            mSaveButton.setEnabled(false);
        } else {
            mSaveButton.setEnabled(true);
        }
    }
    
    private void updateCheckBoxes(boolean markAll) {
        int firstVisiblePosition = mListView.getFirstVisiblePosition();
        int lastVisiblePosition = mListView.getLastVisiblePosition();
        for (int k = firstVisiblePosition; k <= lastVisiblePosition; ++k) {
            RelativeLayout itemView = (RelativeLayout) (mListView
                    .getChildAt(k - firstVisiblePosition));
            ((CheckBox)(itemView.findViewById(R.id.checked_box))).setChecked(markAll);
        }
        mSelectedCount = (markAll) ? mSelectedPositions.length : 0;
    }
    
    private class VCardAdapter extends BaseAdapter {
        private List<VCardEntry> mList;
        private LayoutInflater mInflater;
        
        public VCardAdapter(Context context, List<VCardEntry> list) {
            mList = list;
            mInflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = null;
            VCardItemHolder mHolder;
            final int pos = position;
            if (convertView == null) {
                v = mInflater.inflate(R.layout.vcard_list_item, parent, false);
                mHolder = new VCardItemHolder();
                mHolder.presence = (ImageView) v.findViewById(R.id.presence);
                mHolder.name = (TextView) v.findViewById(R.id.name);
                mHolder.checkbox = (CheckBox) v.findViewById(R.id.checked_box);
            } else {
                v = convertView;
                mHolder = (VCardItemHolder)v.getTag();
            }
            
            final VCardEntry vcard = mList.get(pos);
            if (vcard.getPhotoList() != null) {
                final Bitmap photo = BitmapFactory.decodeByteArray(vcard.getPhotoList().get(0).photoBytes,
                        0, vcard.getPhotoList().get(0).photoBytes.length);
                if (photo != null) {
                     mHolder.presence.setImageBitmap(photo);
                } else {
                     mHolder.presence.setImageResource(R.drawable.ic_contact_picture);
                }
            } else {
                mHolder.presence.setImageResource(R.drawable.ic_contact_picture);
            }   
            mHolder.name.setText(mList.get(pos).getDisplayName());
            mHolder.checkbox.setChecked(mSelectedPositions[pos]);
            mHolder.checkbox.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (mSelectedPositions[pos]) {
                        mSelectedPositions[pos] = false;
                        mSelectedPositionsSet.remove(pos);
                        mSelectedCount = (mSelectedCount < 1) ? 0 : (mSelectedCount - 1);
                    } else {
                        mSelectedPositions[pos] = true;
                        mSelectedPositionsSet.add(pos);
                        mSelectedCount = (mSelectedCount >= mSelectedPositions.length) ? mSelectedPositions.length
                            : (mSelectedCount + 1);
                    }
                    mSelectAllBox.setChecked(mSelectedCount == mSelectedPositions.length);
                    updateSaveButtonState();
                }
                
            });
            v.setTag(mHolder);
            return v;
        }
        
    }
    
    private static class VCardItemHolder {
        public ImageView presence;
        public TextView name;
        public CheckBox checkbox;
    }

}
