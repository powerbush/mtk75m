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

import com.android.mms.LogTag;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;

import com.android.internal.widget.ContactHeaderWidget;
import android.app.Activity;
import android.app.ListActivity;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony.Mms;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import com.mediatek.xlog.Xlog;

/**
 * This activity previews multi recipients in a list.
 */
public class MultiRecipientsActivity extends ListActivity {
    private static final String TAG = "Mms/MultiRecipientsActivity";
    private static final String NUMBER_ADD_CONTACT_ACTION ="android.intent.action.INSERT_OR_EDIT";
    private StatusBarManager mStatusBarManager;
    private ComponentName mComponentName;
    private static ContactList mRecipients;
    private MultiRecipientsAdapter mMultiRecipientsAdapter; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMultiRecipientsAdapter = new MultiRecipientsAdapter(this, mRecipients);
        setListAdapter(mMultiRecipientsAdapter);
        this.getListView().setBackgroundColor(android.graphics.Color.WHITE);
        // SIM indicator manager
        mStatusBarManager = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
        mComponentName = getComponentName();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (!mRecipients.isEmpty()) {
            for (int i=0;i<mRecipients.size();i++) {
                final Contact contt = mRecipients.get(i);
                contt.reload();
            }
        }
        mMultiRecipientsAdapter.notifyDataSetChanged();
    }

    /* package */ static void setContactList(ContactList list) {
        mRecipients = list;
    }

    private class MultiRecipientsAdapter extends BaseAdapter {
        private static final String TAG = "MultiRecipientsAdapter";
        private final LayoutInflater mFactory;
        private ContactList mRecipients;
        private int mAllCount;
        private Context mContext;
     
        public MultiRecipientsAdapter(Context context, ContactList list) {
            mFactory = LayoutInflater.from(context);
            mRecipients = list;
            mAllCount = mRecipients.size();
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
                itemView = mFactory.inflate(R.layout.multi_recipients_list_item, null);
            } else {
                itemView = convertView;
            }
           
            final ContactHeaderWidget contactHeader = 
                (ContactHeaderWidget) itemView.findViewById(R.id.multi_recipients_contact_header);
            final Contact contact = Contact.get(mRecipients.get(position).getNumber(), true);
            final ImageButton jumpToContact = 
                (ImageButton) itemView.findViewById(R.id.multi_recipients_jump_to_contact);
            renderView(contactHeader, contact, jumpToContact);
            return itemView;
        }

        private void renderView(ContactHeaderWidget contactHeader, final Contact contact, ImageButton jumpToContact) {
            final String addr = contact.getNumber();
            final String name = contact.getName();
            final Uri contactUri = contact.getUri();
            Intent intent;

            if (contact.existsInDatabase()) {
                Xlog.i(TAG, "compose.bindToContactHeaderWidget(): has contact");
                contactHeader.showStar(true);
                contactHeader.setStarInvisible(true);
                contactHeader.setDisplayName(name, addr);
                contactHeader.setContactUri(contactUri, true);
                intent = new Intent(Intent.ACTION_VIEW, contactUri);
            } else {
                Xlog.i(TAG, "compose.bindToContactHeaderWidget(): doesn't have contact");
                contactHeader.showStar(true);
                contactHeader.setStarInvisible(true);
                if (Mms.isEmailAddress(addr)) {
                    contactHeader.bindFromEmail(addr);
                } else {
                    contactHeader.setDisplayName(addr,null);
                    contactHeader.bindFromPhoneNumber(addr);
                }
                intent = ConversationList.createAddContactIntent(contact.getNumber());
            }

            BitmapDrawable photo = (BitmapDrawable) contact.getAvatar(MultiRecipientsActivity.this, null);
            if (null != photo) {
                contactHeader.setPhoto(photo.getBitmap());
            } else {
                contactHeader.setPhoto(((BitmapDrawable) getResources().getDrawable(R.drawable.ic_contact_picture))
                        .getBitmap());
            }

            final Intent contactIntent = intent;
            jumpToContact.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (contactIntent.getAction() == Intent.ACTION_VIEW) {
                        contactIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        startActivity(contactIntent);
                    } else {
                        MessageUtils.addNumberOrEmailtoContact(contact.getNumber(), 
                            ComposeMessageActivity.NO_REQUEST_CODE, MultiRecipientsActivity.this);
                    }
                }
            });
            final String action = intent.getAction();
            if (Intent.ACTION_VIEW.equals(action)) {
                Xlog.d(TAG,"ComposeMessageActivity-onResume-action:"+action);
                jumpToContact.setBackgroundResource(R.drawable.add_contact_selector_exist);
            } else if (NUMBER_ADD_CONTACT_ACTION.equals(action)) {
                Xlog.d(TAG,"ComposeMessageActivity-onResume-action:"+action);
                jumpToContact.setBackgroundResource(R.drawable.add_contact_selector);
            }
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
    }
}
