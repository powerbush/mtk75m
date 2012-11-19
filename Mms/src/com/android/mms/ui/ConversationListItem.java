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

import java.util.List;

import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import android.os.Handler;
import android.provider.ContactsContract.Intents;
import android.provider.Telephony.Mms;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.QuickContactBadge;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mediatek.featureoption.FeatureOption;
import android.provider.Telephony.Threads;

/**
 * This class manages the view for given conversation.
 */
public class ConversationListItem extends RelativeLayout implements Contact.UpdateListener {
    private static final String TAG = "ConversationListItem";
    private static final boolean DEBUG = false;

    private TextView mSubjectView;
    private TextView mFromView;
    private TextView mDateView;
    private View mAttachmentView;
    private ImageView mErrorIndicator;
    private ImageView mPresenceView;
    private QuickContactBadge mAvatarView;
    private Context mContext;
    static private Drawable sDefaultContactImage;

    // For posting UI update Runnables from other threads:
    private Handler mHandler = new Handler();

    private ConversationListItemData mConversationHeader;

    private static final StyleSpan STYLE_BOLD = new StyleSpan(Typeface.BOLD);

    public ConversationListItem(Context context) {
        super(context);
        mContext = context;
    }

    public ConversationListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        if (sDefaultContactImage == null) {
            sDefaultContactImage = context.getResources().getDrawable(R.drawable.ic_contact_picture);
        }
    }
    //for change theme to update the Drawable resource
    public static void resetDefaultDrawable(Context context){
        sDefaultContactImage = context.getResources().getDrawable(R.drawable.ic_contact_picture);
    }
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mFromView = (TextView) findViewById(R.id.from);
        mSubjectView = (TextView) findViewById(R.id.subject);

        mDateView = (TextView) findViewById(R.id.date);
        mAttachmentView = findViewById(R.id.attachment);
        mErrorIndicator = (ImageView)findViewById(R.id.error);
        mPresenceView = (ImageView) findViewById(R.id.presence);
        mAvatarView = (QuickContactBadge) findViewById(R.id.avatar);
    }

    public void setPresenceIcon(int iconId) {
        if (iconId == 0) {
            
        } else {
            mPresenceView.setImageResource(iconId);
            mPresenceView.setVisibility(View.VISIBLE);
        }
    }

    public ConversationListItemData getConversationHeader() {
        return mConversationHeader;
    }

    private void setConversationHeader(ConversationListItemData header) {
        mConversationHeader = header;
    }

    /**
     * Only used for header binding.
     */
    public void bind(String title, String explain) {
        mFromView.setText(title);
        mSubjectView.setText(explain);
    }

    private CharSequence formatMessage(ConversationListItemData ch) {
        final int size = android.R.style.TextAppearance_Small;
        final int color = android.R.styleable.Theme_textColorSecondary;
        String from = ch.getFrom();
        
        if (TextUtils.isEmpty(from)){
        	from = mContext.getString(android.R.string.unknownName);
        }
        SpannableStringBuilder buf = new SpannableStringBuilder(from);
        if (/*ch.getType() == Threads.WAPPUSH_THREAD ||*/ (ch.hasDraft() && ch.getMessageCount() == 0)) {
        	// only draft or WapPush
        } else {
            if (ch.getUnreadMessageCount() > 0) {
                buf.append(" (" + ch.getUnreadMessageCount() + "/");
            } else {
                buf.append(" (" + "0/");
            }
            //if (ch.getMessageCount() > 1) {
                buf.append(ch.getMessageCount() + ") ");
            //}
        }

        int before = buf.length();
        if (ch.hasDraft()) {
            buf.append(" ");
            buf.append(mContext.getResources().getString(R.string.has_draft));
            buf.setSpan(new TextAppearanceSpan(mContext, size, color), before,
                    buf.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            buf.setSpan(new ForegroundColorSpan(
                    mContext.getResources().getColor(R.drawable.text_color_red)),
                    before, buf.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }

        // Unread messages are shown in bold
        if (!ch.isRead()) {
            buf.setSpan(STYLE_BOLD, 0, buf.length(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        return buf;
    }

    private void updateAvatarView() {
        ConversationListItemData ch = mConversationHeader;

        Drawable avatarDrawable;
        if (ch.getContacts().size() == 1) {
            Contact contact = ch.getContacts().get(0);
            avatarDrawable = contact.getAvatar(mContext, sDefaultContactImage);

            if (contact.existsInDatabase()) {
                mAvatarView.assignContactUri(contact.getUri());
            } else {
                if (Mms.isEmailAddress(contact.getNumber())) {
                    mAvatarView.assignContactFromEmail(contact.getNumber(), true);
                } else {
                    mAvatarView.assignContactFromPhone(contact.getNumber(), true);
                }
            }
        } else {
            // TODO get a multiple recipients asset (or do something else)
            avatarDrawable = sDefaultContactImage;
            mAvatarView.assignContactUri(null);
        }
        mAvatarView.setImageDrawable(avatarDrawable);
        mAvatarView.setVisibility(View.VISIBLE);
    }

    private void updateFromView() {
        ConversationListItemData ch = mConversationHeader;
        ch.updateRecipients();
        mFromView.setText(formatMessage(ch));
        setPresenceIcon(ch.getContacts().getPresenceResId());
        updateAvatarView();
    }

    public void onUpdate(Contact updated) {
        mHandler.post(new Runnable() {
            public void run() {
                updateFromView();
            }
        });
    }

    public final void bind(Context context, final ConversationListItemData ch) {
        //if (DEBUG) Log.v(TAG, "bind()");

        setConversationHeader(ch);

        Drawable background = ch.isRead()?
                mContext.getResources().getDrawable(R.drawable.conversation_item_background_read) :
                mContext.getResources().getDrawable(R.drawable.conversation_item_background_unread);

        setBackgroundDrawable(background);

//        LayoutParams attachmentLayout = (LayoutParams)mAttachmentView.getLayoutParams();
        boolean hasError = ch.hasError();
        // When there's an error icon, the attachment icon is left of the error icon.
        // When there is not an error icon, the attachment icon is left of the date text.
        // As far as I know, there's no way to specify that relationship in xml.
//        if (hasError) {
//            attachmentLayout.addRule(RelativeLayout.LEFT_OF, R.id.error);
//        } else {
//            attachmentLayout.addRule(RelativeLayout.LEFT_OF, R.id.date);
//        }

        boolean hasAttachment = ch.hasAttachment();
        mAttachmentView.setVisibility(hasAttachment ? VISIBLE : GONE);

        // Date
        mDateView.setText(ch.getDate());

        // From.
        mFromView.setText(formatMessage(ch));

        // Register for updates in changes of any of the contacts in this conversation.
        ContactList contacts = ch.getContacts();

        if (DEBUG) Log.v(TAG, "bind: contacts.addListeners " + this);
        Contact.addListener(this);
        setPresenceIcon(contacts.getPresenceResId());

        // Subject
        mSubjectView.setText(ch.getSubject());
//        LayoutParams subjectLayout = (LayoutParams)mSubjectView.getLayoutParams();
//        // We have to make the subject left of whatever optional items are shown on the right.
//        subjectLayout.addRule(RelativeLayout.LEFT_OF, hasAttachment ? R.id.attachment :
//            (hasError ? R.id.error : R.id.date));

        // Transmission error indicator.
        mErrorIndicator.setVisibility(hasError ? VISIBLE : GONE);
        
        //wappush: modify the picture
        if(true == FeatureOption.MTK_WAPPUSH_SUPPORT){
        	if(mConversationHeader.getType() == Threads.WAPPUSH_THREAD){
        		mErrorIndicator.setImageResource(R.drawable.alert_wappush_si_expired);
        	}else{
        		mErrorIndicator.setImageResource(R.drawable.ic_list_alert_sms_failed);
        	}
        }else{
        	mErrorIndicator.setImageResource(R.drawable.ic_list_alert_sms_failed);
        }

        updateAvatarView();
    }

    public final void unbind() {
        if (DEBUG) Log.v(TAG, "unbind: contacts.removeListeners " + this);
        // Unregister contact update callbacks.
        Contact.removeListener(this);
    }
}
