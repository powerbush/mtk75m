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
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.mediatek.xlog.Xlog;

public class PreviewListAdapter extends BaseAdapter {
    private static final String TAG = "Mms/PreviewListAdapter";
    private final LayoutInflater mFactory;
    private ArrayList<PreviewListItemData> mListItem;
    private int mAllCount;
    private Context mContext;
 
    public PreviewListAdapter(Context context, ArrayList<PreviewListItemData> listItem) {
        mFactory = LayoutInflater.from(context);
        mListItem = listItem;
        mAllCount = mListItem.size();
        mContext = context;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        Xlog.i(TAG, "getView, for position " + position + ", view " + convertView);
        if (position < 0 || position >= mAllCount) {
            Xlog.e(TAG, "getView, oops indexoutofbound pos: " + position);
            return null;
        }
        View itemView;
        if (convertView == null) {
            itemView = mFactory.inflate(R.layout.preview_item, null);
        } else {
            itemView = convertView;
        }
       
        ImageView thumb = (ImageView) itemView.findViewById(R.id.thumbnail);
        final PreviewListItemData item = mListItem.get(position);
        Bitmap t = item.getThumbnail();
        if (t == null) {
            thumb.setImageResource(R.drawable.ic_contact_picture);
        } else {
            thumb.setImageBitmap(t);
        }
        TextView name = (TextView) itemView.findViewById(R.id.name);
        TextView info = (TextView) itemView.findViewById(R.id.info);
        name.setText(item.getName());
        if(item.getSlideNum()!= -1) {
            String slideString = mContext.getString(
                    R.string.slide_number).replace("%s", ""+item.getSlideNum())+" ("+item.getSize()+")";
            info.setText(slideString);
        } else {
            info.setText(item.getSize());
        }
        
        return itemView;
    }

    public long getItemId(int position) {
        return position;
    }
       
    public Object getItem(int position) {
        return null;
    }
       
    public int getCount() {
        return mAllCount;
    }
       
    public ArrayList<PreviewListItemData> getItemList() {
    	return mListItem;
    }
}
