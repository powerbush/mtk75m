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

import android.app.Activity;
import android.content.Intent;
import android.content.ContentUris;
import android.os.Bundle;
import android.provider.Telephony.Mms;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.net.Uri;
import android.media.MediaFile;
import android.webkit.MimeTypeMap;

import com.google.android.mms.ContentType;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduPart;
import com.google.android.mms.MmsException;
import java.util.ArrayList;

import com.android.mms.model.MediaModel;
import com.android.mms.model.SlideModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.model.SmilHelper;
import com.mediatek.xlog.Xlog;
/**
 * This activity provides a list view of existing conversations.
 */
public class PreviewActivity extends Activity {
    private static final String TAG = "Mms/PreviewActivity";
    private PreviewListAdapter mListAdapter;
    private ListView mPreviewList;
    private ArrayList<PreviewListItemData> mListItem;
    private SlideshowModel mSlideshowModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preview);
        this.setTitle(R.string.preview);
        mPreviewList = (ListView) findViewById(R.id.item_list);
        mPreviewList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mListItem = mListAdapter.getItemList();
                final PreviewListItemData item = mListItem.get(position);
                String type = new String(item.getPduPart().getContentType());
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(item.getDataUri(),type);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            }
        });
        
        Intent i = getIntent();  
        long msgId = -1;
        if (i != null && i.hasExtra("msgid")) {
            msgId = i.getLongExtra("msgid", -1);
        }
        initListAdapter(msgId); 
    }
    
    private void initListAdapter(long  msgId) {
        PduBody body = ComposeMessageActivity.getPduBody(PreviewActivity.this, msgId);
        if (body == null) {
            Xlog.i(TAG, "initListAdapter, oops, getPduBody returns null");
            return;
        }
        
        ArrayList<PreviewListItemData> attachments = new ArrayList<PreviewListItemData>();
        
        try {
            Uri uri = ContentUris.withAppendedId(Mms.CONTENT_URI, msgId);
            mSlideshowModel = SlideshowModel.createFromMessageUri(PreviewActivity.this, uri);
        } catch (MmsException e) {
            Xlog.e(TAG, "Create SlideshowModel failed!", e);
            finish();
            return;
        }
        
        if (mSlideshowModel != null) {
            for (int i = 0; i < mSlideshowModel.size(); i++) {
                for (int j = 0; j < mSlideshowModel.get(i).size(); j++) {
                    PduPart part = body
                            .getPartByContentLocation(mSlideshowModel.get(i)
                                    .get(j).getSrc());
                    if (part != null) {
                        final String type = new String(part.getContentType());
                        if (ContentType.isImageType(type)
                                || ContentType.isVideoType(type)
                                || ContentType.isAudioType(type)) {
                            attachments.add(new PreviewListItemData(
                                    PreviewActivity.this, part, msgId, i + 1));
                        }
                    }
                }
            }
        }
        
        attachments.trimToSize();

        mListAdapter = new PreviewListAdapter(this, attachments);
        mPreviewList.setAdapter(mListAdapter);
    }
}
