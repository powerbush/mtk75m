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

import java.util.Map;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.drawable.Drawable;
import android.net.MailTo;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Environment;
import android.os.SystemProperties;
import android.provider.Browser;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony.Sms;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AlignmentSpan;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineHeightSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.QuickContactBadge;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.data.WorkingMessage;
import com.android.mms.model.FileAttachmentModel;
import com.android.mms.transaction.Transaction;
import com.android.mms.transaction.TransactionBundle;
import com.android.mms.transaction.TransactionService;
import com.android.mms.util.DownloadManager;
import com.android.mms.util.SmileyParser;
import com.google.android.mms.ContentType;
import com.google.android.mms.pdu.PduHeaders;


//add for gemini
import android.database.Cursor;
import com.google.android.mms.util.SqliteWrapper;
import android.content.ContentValues;
import com.mediatek.featureoption.FeatureOption;
import com.android.internal.telephony.Phone;
import android.telephony.gemini.GeminiSmsManager;
import android.util.Log;
import android.provider.Telephony.TextBasedSmsColumns;
import com.android.internal.telephony.TelephonyProperties;
import android.drm.DrmManagerClient;
import com.mediatek.xlog.Xlog;


/**
 * This class provides view of a message in the messages list.
 */
public class MessageListItem extends LinearLayout implements
        SlideViewInterface, OnClickListener {
    public static final String EXTRA_URLS = "com.android.mms.ExtraUrls";

    private static final String TAG = "MessageListItem";
    private static final StyleSpan STYLE_BOLD = new StyleSpan(Typeface.BOLD);

    static final int MSG_LIST_EDIT_MMS   = 1;
    static final int MSG_LIST_EDIT_SMS   = 2;
    static final int ITEM_CLICK          = 5;
    static final int ITEM_MARGIN         = 50;
    private View mMsgListItem;
    private View mMmsView;
    private View mFileAttachmentView;
    private LinearLayout mMsgListItemLayout;
    private ImageView mImageView;
    private ImageView mLockedIndicator;
    private ImageView mDeliveredIndicator;
    private ImageView mDetailsIndicator;
    private ImageButton mSlideShowButton;
    private TextView mBodyTextView;
    private TextView mTimestamp;
    private TextView mSimStatus;
    private Button mDownloadButton;
    private TextView mDownloadingLabel;
//    private QuickContactBadge mAvatar;
    //add for multi-delete
    private CheckBox mSelectedBox;
    private Handler mHandler;
    private MessageItem mMessageItem;
    private boolean mIsTel = false;

    public MessageListItem(Context context) {
        super(context);
    }

    public MessageListItem(Context context, AttributeSet attrs) {
        super(context, attrs);

        int color = mContext.getResources().getColor(R.color.timestamp_color);
        mColorSpan = new ForegroundColorSpan(color);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mMsgListItem = findViewById(R.id.msg_list_item);
        mMsgListItem.setLongClickable(false);
        mMsgListItemLayout = (LinearLayout) findViewById(R.id.mms_layout_view_parent);
        mMsgListItemLayout.setLongClickable(true);
        mBodyTextView = (TextView) findViewById(R.id.text_view);
        mTimestamp = (TextView) findViewById(R.id.timestamp);
       	mSimStatus = (TextView) findViewById(R.id.sim_status);
        mLockedIndicator = (ImageView) findViewById(R.id.locked_indicator);
        mDeliveredIndicator = (ImageView) findViewById(R.id.delivered_indicator);
        mDetailsIndicator = (ImageView) findViewById(R.id.details_indicator);
        //add for multi-delete
        mSelectedBox = (CheckBox)findViewById(R.id.select_check_box);
    }

    public void bind(MessageListAdapter.AvatarCache avatarCache, MessageItem msgItem, boolean isDeleteMode) {
        Xlog.i(TAG, "MessageListItem.bind() : msgItem.mSimId = " + msgItem.mSimId);
        mMessageItem = msgItem;
        //add for multi-delete
        if (isDeleteMode) {
            mSelectedBox.setVisibility(View.VISIBLE);
            mSelectedBox.setChecked(msgItem.isSelected());
        } else {
            mSelectedBox.setVisibility(View.GONE);
        }
        mMsgListItemLayout.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                onMessageListItemClick();
            }
        });

        switch (msgItem.mMessageType) {
            case PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND:
                bindNotifInd(avatarCache, msgItem);
                break;
            default:
                bindCommonMessage(avatarCache, msgItem);
                break;
        }
    }

    public MessageItem getMessageItem() {
        return mMessageItem;
    }

    public void setMsgListItemHandler(Handler handler) {
        mHandler = handler;
    }

    private void bindNotifInd(final MessageListAdapter.AvatarCache avatarCache, final MessageItem msgItem) {
        hideMmsViewIfNeeded();
        hideFileAttachmentViewIfNeeded();

        String msgSizeText = mContext.getString(R.string.message_size_label)
                                + String.valueOf((msgItem.mMessageSize + 1023) / 1024)
                                + mContext.getString(R.string.kilobyte);

        mBodyTextView.setText(formatMessage(msgItem, msgItem.mContact, null, msgItem.mSubject,
                                            msgSizeText + "\n" + msgItem.mTimestamp,
                                            msgItem.mHighlight, msgItem.mTextContentType));

        mTimestamp.setText(formatTimestamp(msgItem, msgItem.mTimestamp));
        mSimStatus.setText(formatSimStatus(msgItem));
        int state = DownloadManager.getInstance().getState(msgItem.mMessageUri);
        switch (state) {
            case DownloadManager.STATE_DOWNLOADING:
                inflateDownloadControls();
                mDownloadingLabel.setVisibility(View.VISIBLE);
                mDownloadButton.setVisibility(View.GONE);
                findViewById(R.id.text_view).setVisibility(GONE);
                break;
            case DownloadManager.STATE_UNSTARTED:
            case DownloadManager.STATE_TRANSIENT_FAILURE:
            case DownloadManager.STATE_PERMANENT_FAILURE:
            default:
                inflateDownloadControls();
                mDownloadingLabel.setVisibility(View.GONE);
                mDownloadButton.setVisibility(View.VISIBLE);
                findViewById(R.id.text_view).setVisibility(GONE);
                mDownloadButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        //add for multi-delete
                        if (mSelectedBox != null && mSelectedBox.getVisibility() == View.VISIBLE) {
                            return;
                        }

                        // add for gemini
                        int simId = 0;
                        if (FeatureOption.MTK_GEMINI_SUPPORT) {
                            // get sim id by uri
                            Cursor cursor = SqliteWrapper.query(msgItem.mContext, msgItem.mContext.getContentResolver(),
                                msgItem.mMessageUri, new String[] { Mms.SIM_ID }, null, null, null);
                            if (cursor != null) {
                                try {
                                    if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                                        simId = cursor.getInt(0);
                                    }
                                } finally {
                                    cursor.close();
                                }
                            }
                        }
                        
                        String optr = SystemProperties.get("ro.operator.optr");
                        //MTK_OP01_PROTECT_START
                        if (null != optr && optr.equals("OP01")) {
                            // check device memory status
                            if (MmsConfig.getDeviceStorageFullStatus()) {
                                MmsApp.getToastHandler().sendEmptyMessage(MmsApp.MSG_RETRIEVE_FAILURE_DEVICE_MEMORY_FULL);
                                return;
                            }

                            /*
                            // check MMS size by NotifyInd
                            int msgSize = 0;
                            Cursor cr = SqliteWrapper.query(msgItem.mContext, msgItem.mContext.getContentResolver(),
                                                msgItem.mMessageUri, new String[] {Mms.MESSAGE_SIZE}, null, null, null);
                            if (cr != null) {
                                try {
                                    if ((cr.getCount() == 1) && cr.moveToFirst()) {
                                        msgSize = cr.getInt(0);
                                    }
                                } finally {
                                    cr.close();
                                }
                            }

                            String netWorkType = null;
                            int slotId = -1;
                            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                // convert sim id to slot id
                                slotId = SIMInfo.getSlotById(msgItem.mContext, simId);
                                netWorkType = SystemProperties.get(slotId == 0 ? 
                                        TelephonyProperties.PROPERTY_CS_NETWORK_TYPE : TelephonyProperties.PROPERTY_CS_NETWORK_TYPE_2);
                            } else {
                                netWorkType = SystemProperties.get(TelephonyProperties.PROPERTY_CS_NETWORK_TYPE);
                            }

                            boolean bTDNetwork = Integer.parseInt(netWorkType) > 2 ? true : false;
                            if ((!bTDNetwork && MmsConfig.getReceiveMmsLimitFor2G() < msgSize/1024)
                                    || (bTDNetwork && MmsConfig.getReceiveMmsLimitForTD() < msgSize/1024)) {
                                MmsApp.getToastHandler().sendEmptyMessage(MmsApp.MSG_MMS_TOO_BIG_TO_DOWNLOAD);
                                return;
                            }
                            */
                        }
                        //MTK_OP01_PROTECT_END
                        mDownloadingLabel.setVisibility(View.VISIBLE);
                        mDownloadButton.setVisibility(View.GONE);
                        Intent intent = new Intent(mContext, TransactionService.class);
                        intent.putExtra(TransactionBundle.URI, msgItem.mMessageUri.toString());
                        intent.putExtra(TransactionBundle.TRANSACTION_TYPE,
                                Transaction.RETRIEVE_TRANSACTION);
                        // add for gemini
                        intent.putExtra(Phone.GEMINI_SIM_ID_KEY, simId);
                        mContext.startService(intent);
                    }
                });
                break;
        }

        // Hide the indicators.
        if (msgItem.mLocked) {
            mLockedIndicator.setImageResource(R.drawable.ic_lock_message_sms);          
            mLockedIndicator.setVisibility(View.VISIBLE);
        } else {
            mLockedIndicator.setVisibility(View.GONE);
        }
        mDeliveredIndicator.setVisibility(View.GONE);
        mDetailsIndicator.setVisibility(View.GONE);
    }

    private void bindCommonMessage(final MessageListAdapter.AvatarCache avatarCache, final MessageItem msgItem) {
        if (mDownloadButton != null) {
            mDownloadButton.setVisibility(View.GONE);
            mDownloadingLabel.setVisibility(View.GONE);
            mBodyTextView.setVisibility(View.VISIBLE);
        }
        // Since the message text should be concatenated with the sender's
        // address(or name), I have to display it here instead of
        // displaying it by the Presenter.
        mBodyTextView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());

        // Get and/or lazily set the formatted message from/on the
        // MessageItem.  Because the MessageItem instances come from a
        // cache (currently of size ~50), the hit rate on avoiding the
        // expensive formatMessage() call is very high.
        CharSequence formattedMessage = msgItem.getCachedFormattedMessage();
        CharSequence formattedTimestamp = msgItem.getCachedFormattedTimestamp();
        CharSequence formattedSimStatus= msgItem.getCachedFormattedTimestamp();
        if (formattedMessage == null) {
            formattedMessage = formatMessage(msgItem, msgItem.mContact, msgItem.mBody,
                                             msgItem.mSubject, msgItem.mTimestamp,
                                             msgItem.mHighlight, msgItem.mTextContentType);
            formattedTimestamp = formatTimestamp(msgItem, msgItem.mTimestamp);
            formattedSimStatus = formatSimStatus(msgItem);
        }
        mBodyTextView.setText(formattedMessage);
        if (msgItem.isFailedMessage() || (!msgItem.isSending() && TextUtils.isEmpty(msgItem.mTimestamp))) {
            mTimestamp.setVisibility(View.GONE);
        } else {
            mTimestamp.setVisibility(View.VISIBLE);
            mTimestamp.setText(formattedTimestamp);
        }

        if (!msgItem.isSimMsg() && !TextUtils.isEmpty(formattedSimStatus)) {
            mSimStatus.setVisibility(View.VISIBLE);
            mSimStatus.setText(formattedSimStatus);
        } else {
            mSimStatus.setVisibility(View.GONE);
        }

        if (msgItem.isSms()) {
            hideMmsViewIfNeeded();
            hideFileAttachmentViewIfNeeded();
        } else {
            Presenter presenter = PresenterFactory.getPresenter(
                    "MmsThumbnailPresenter", mContext,
                    this, msgItem.mSlideshow);
            presenter.present();

            if (msgItem.mAttachmentType != WorkingMessage.TEXT) {
                if (msgItem.mAttachmentType == WorkingMessage.ATTACHMENT) {
                    // show file attachment view
                    hideMmsViewIfNeeded();
                    showFileAttachmentView(msgItem.mSlideshow.getAttachFiles());
                } else {
                    hideFileAttachmentViewIfNeeded();
                    inflateMmsView();
                    mMmsView.setVisibility(View.VISIBLE);
                    setOnClickListener(msgItem);
                    drawPlaybackButton(msgItem);
                }
            } else {
                hideMmsViewIfNeeded();
                hideFileAttachmentViewIfNeeded();
            }
        }

        drawRightStatusIndicator(msgItem);

        requestLayout();
    }

    private void hideMmsViewIfNeeded() {
        if (mMmsView != null) {
            mMmsView.setVisibility(View.GONE);
        }
    }

    public void startAudio() {
        // TODO Auto-generated method stub
    }

    public void startVideo() {
        // TODO Auto-generated method stub
    }

    public void setAudio(Uri audio, String name, Map<String, ?> extras) {
        // TODO Auto-generated method stub
    }

    public void setImage(String name, Bitmap bitmap) {
        inflateMmsView();

        try {
            if (null == bitmap) {
                bitmap = BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_missing_thumbnail_picture);
            }
            mImageView.setImageBitmap(bitmap);
            mImageView.setVisibility(VISIBLE);
        } catch (java.lang.OutOfMemoryError e) {
            Log.e(TAG, "setImage: out of memory: ", e);
        }
    }

    private void inflateMmsView() {
        if (mMmsView == null) {
            //inflate the surrounding view_stub
            findViewById(R.id.mms_layout_view_stub).setVisibility(VISIBLE);

            mMmsView = findViewById(R.id.mms_view);
            mImageView = (ImageView) findViewById(R.id.image_view);
            mSlideShowButton = (ImageButton) findViewById(R.id.play_slideshow_button);
        }
    }
    

    private void hideFileAttachmentViewIfNeeded() {
        if (mFileAttachmentView != null) {
            mFileAttachmentView.setVisibility(View.GONE);
        }
    }

    private void showFileAttachmentView(ArrayList<FileAttachmentModel> files) {
        // There should be one and only one file
        if (files == null || files.size() < 1) {
            Log.e(TAG, "showFileAttachmentView, oops no attachment files found");
            return;
        }
        if (mFileAttachmentView == null) {
            findViewById(R.id.mms_file_attachment_view_stub).setVisibility(VISIBLE);
            mFileAttachmentView = findViewById(R.id.file_attachment_view);
        }
        mFileAttachmentView.setVisibility(View.VISIBLE);
        final FileAttachmentModel attach = files.get(0);
        mFileAttachmentView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mSelectedBox != null && mSelectedBox.getVisibility() == View.VISIBLE) {         
                    return;
                }
                if (attach.isVCard()) {
                    MessageUtils.previewVCard(mContext, attach.getUri());
                } else if (attach.isVCalendar()) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(attach.getUri(), attach.getContentType().toLowerCase());
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        mContext.startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, "exception caught ", e);
                    }
                }
            }
        });
        final ImageView thumb = (ImageView) mFileAttachmentView.findViewById(R.id.file_attachment_thumbnail);
        final TextView name = (TextView) mFileAttachmentView.findViewById(R.id.file_attachment_name_info);
        String nameText = null;
        int thumbResId = -1;
        if (attach.isVCard()) {
            nameText = mContext.getString(R.string.file_attachment_vcard_name, attach.getSrc());
            thumbResId = R.drawable.ic_vcard_attach;
        } else if (attach.isVCalendar()) {
            nameText = mContext.getString(R.string.file_attachment_vcalendar_name, attach.getSrc());
            thumbResId = R.drawable.ic_calendar_attach;
        }
        name.setText(nameText);
        thumb.setImageResource(thumbResId);
        final TextView size = (TextView) mFileAttachmentView.findViewById(R.id.file_attachment_size_info);
        size.setText(MessageUtils.getHumanReadableSize(attach.getAttachSize()));
    }

    private void inflateDownloadControls() {
        if (mDownloadButton == null) {
            //inflate the download controls
            findViewById(R.id.mms_downloading_view_stub).setVisibility(VISIBLE);
            mDownloadButton = (Button) findViewById(R.id.btn_download_msg);
            mDownloadingLabel = (TextView) findViewById(R.id.label_downloading);
        }
    }

    private LeadingMarginSpan mLeadingMarginSpan;

    private LineHeightSpan mSpan = new LineHeightSpan() {
        public void chooseHeight(CharSequence text, int start,
                int end, int spanstartv, int v, FontMetricsInt fm) {
            fm.ascent -= 10;
        }
    };

    TextAppearanceSpan mTextSmallSpan =
        new TextAppearanceSpan(mContext, android.R.style.TextAppearance_Small);

    ForegroundColorSpan mColorSpan = null;  // set in ctor

    private CharSequence formatMessage(MessageItem msgItem, String contact, String body,
                                       String subject, String timestamp, Pattern highlight,
                                       String contentType) {
        CharSequence template = mContext.getResources().getText(R.string.name_colon);
        SpannableStringBuilder bufMessageBody = new SpannableStringBuilder("");

        boolean hasSubject = !TextUtils.isEmpty(subject);
        if (hasSubject) {
        	SmileyParser parser = SmileyParser.getInstance();
            bufMessageBody.append(mContext.getResources().getString(R.string.inline_subject, subject));
        	bufMessageBody.replace(0, bufMessageBody.length(), parser.addSmileySpans(bufMessageBody));
        }

        if (!TextUtils.isEmpty(body)) {
            // Converts html to spannable if ContentType is "text/html".
            if (contentType != null && ContentType.TEXT_HTML.equals(contentType)) {
                bufMessageBody.append("\n");
                bufMessageBody.append(Html.fromHtml(body));
            } else {
                if (hasSubject) {
                    bufMessageBody.append(" - ");
                }
                SmileyParser parser = SmileyParser.getInstance();
                bufMessageBody.append(parser.addSmileySpans(body));
            }
        }

        if (highlight != null) {
            Matcher m = highlight.matcher(bufMessageBody.toString());
            while (m.find()) {
                bufMessageBody.setSpan(new StyleSpan(Typeface.BOLD), m.start(), m.end(), 0);
            }
        }
        bufMessageBody.setSpan(mLeadingMarginSpan, 0, bufMessageBody.length(), 0);
        return bufMessageBody;
    }

    private CharSequence formatTimestamp(MessageItem msgItem, String timestamp) {
        SpannableStringBuilder buf = new SpannableStringBuilder();
        if (msgItem.isSending()) {
            timestamp = mContext.getResources().getString(R.string.sending_message);
        }

		   buf.append(TextUtils.isEmpty(timestamp) ? " " : timestamp);        
		   buf.setSpan(mSpan, 1, buf.length(), 0);
		   
        //buf.setSpan(mTextSmallSpan, 0, buf.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        // Make the timestamp text not as dark
        buf.setSpan(mColorSpan, 0, buf.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        return buf;
    }

    private CharSequence formatSimStatus(MessageItem msgItem ) {
        SpannableStringBuilder buffer = new SpannableStringBuilder();
        // If we're in the process of sending a message (i.e. pending), then we show a "Sending..."
        // string in place of the timestamp.
        //Add sim info
        int simInfoStart = buffer.length();
        CharSequence simInfo = MessageUtils.getSimInfo(mContext, msgItem.mSimId);
        if(simInfo.length() > 0){
            if (msgItem.mBoxId == TextBasedSmsColumns.MESSAGE_TYPE_INBOX) {
                buffer.append(" ");
                buffer.append(mContext.getString(R.string.via_without_time_for_recieve));
            } else {
                buffer.append(" ");
                buffer.append(mContext.getString(R.string.via_without_time_for_send));
            }
            simInfoStart = buffer.length();
            buffer.append(" ");
            buffer.append(simInfo);
            buffer.append(" ");
        }

        //buffer.setSpan(mTextSmallSpan, 0, buffer.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        // Make the timestamp text not as dark
        buffer.setSpan(mColorSpan, 0, simInfoStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        return buffer;
    }

    private void drawPlaybackButton(MessageItem msgItem) {
        switch (msgItem.mAttachmentType) {
            case WorkingMessage.SLIDESHOW:
            case WorkingMessage.AUDIO:
            case WorkingMessage.VIDEO:
                // Show the 'Play' button and bind message info on it.
                mSlideShowButton.setTag(msgItem);
                mSlideShowButton.setVisibility(View.GONE);
                Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.mms_play_btn); 
                if (msgItem.hasDrmContent()) {
                	if (FeatureOption.MTK_DRM_APP) {
                		Xlog.i(TAG," msgItem hasDrmContent"); 
            	        Drawable front = mContext.getResources().getDrawable(com.mediatek.internal.R.drawable.drm_red_lock);
            	        DrmManagerClient drmManager= new DrmManagerClient(mContext);
            	        Bitmap drmBitmap = drmManager.overlayBitmap(bitmap, front);
                        mSlideShowButton.setImageBitmap(drmBitmap);
                        if (bitmap != null && !bitmap.isRecycled()) {
                	    	bitmap.recycle();
                	    	bitmap = null;
                	    }
                	} else {
                		Xlog.i(TAG," msgItem hasn't DrmContent");
                		mSlideShowButton.setImageBitmap(bitmap);
                	}
                } else {
                	Xlog.i(TAG," msgItem hasn't DrmContent"); 
                	mSlideShowButton.setImageBitmap(bitmap);
                }
                // Set call-back for the 'Play' button.
                mSlideShowButton.setOnClickListener(this);
                mSlideShowButton.setVisibility(View.VISIBLE);
                setLongClickable(true);

                // When we show the mSlideShowButton, this list item's onItemClickListener doesn't
                // get called. (It gets set in ComposeMessageActivity:
                // mMsgListView.setOnItemClickListener) Here we explicitly set the item's
                // onClickListener. It allows the item to respond to embedded html links and at the
                // same time, allows the slide show play button to work.
                setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        onMessageListItemClick();
                    }
                });
                break;
            default:
                mSlideShowButton.setVisibility(View.GONE);
                break;
        }
    }

    // OnClick Listener for the playback button
    public void onClick(View v) {
    	//add for multi-delete
		if (mSelectedBox != null && mSelectedBox.getVisibility() == View.VISIBLE) {			
			return;
		}
        MessageItem mi = (MessageItem) v.getTag();
        switch (mi.mAttachmentType) {
            case WorkingMessage.VIDEO:
            case WorkingMessage.AUDIO:
            case WorkingMessage.SLIDESHOW:
                MessageUtils.viewMmsMessageAttachment(mContext, mi.mMessageUri, null);
                break;
        }
    }

    public void onMessageListItemClick() {
    	//add for multi-delete
		if (mSelectedBox != null && mSelectedBox.getVisibility() == View.VISIBLE) {
			mSelectedBox.setChecked(!mSelectedBox.isChecked()); 
			if (null != mHandler) {
	            Message msg = Message.obtain(mHandler, ITEM_CLICK);
	            msg.arg1 = (int)(mMessageItem.mType.equals("mms")? -mMessageItem.mMsgId : mMessageItem.mMsgId);
	            msg.sendToTarget();
	        }
			return;
		}
    	
        URLSpan[] spans = mBodyTextView.getUrls();
        final java.util.ArrayList<String> urls = MessageUtils.extractUris(spans);
        final String telPrefix = "tel:";
        String url = ""; 
        for(int i=0;i<spans.length;i++) {
            url = urls.get(i);
            if(url.startsWith(telPrefix)) {
                mIsTel = true;
                urls.add("smsto:"+url.substring(telPrefix.length()));
            }
        }

        if (spans.length == 0) {
            // Do nothing.
        } else if (spans.length == 1 && !mIsTel) {
            Uri uri = Uri.parse(spans[0].getURL());
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, mContext.getPackageName());
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            mContext.startActivity(intent);
        } else {
            ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_item, urls) {
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    try {
                        String url = getItem(position).toString();
                        TextView tv = (TextView) v;
                        Drawable d = mContext.getPackageManager().getActivityIcon(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        if (d != null) {
                            d.setBounds(0, 0, d.getIntrinsicHeight(), d.getIntrinsicHeight());
                            tv.setCompoundDrawablePadding(10);
                            tv.setCompoundDrawables(d, null, null, null);
                        }
                        final String telPrefix = "tel:";
                        if (url.startsWith(telPrefix)) {
                            url = PhoneNumberUtils.formatNumber(url.substring(telPrefix.length()));
                        }
                        final String smsPrefix = "smsto:";
                        if (url.startsWith(smsPrefix)) {
                            url = PhoneNumberUtils.formatNumber(url.substring(smsPrefix.length()));
                        }
                        final String mailPrefix ="mailto";
                        if(url.startsWith(mailPrefix))
                        {
                            MailTo mt = MailTo.parse(url);
                            url = mt.getTo();
                        }
                        tv.setText(url);
                    } catch (android.content.pm.PackageManager.NameNotFoundException ex) {
                        ;
                    }
                    return v;
                }
            };

            AlertDialog.Builder b = new AlertDialog.Builder(mContext);

            DialogInterface.OnClickListener click = new DialogInterface.OnClickListener() {
                public final void onClick(DialogInterface dialog, int which) {
                    if (which >= 0) {
                        Uri uri = Uri.parse(urls.get(which));
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        intent.putExtra(Browser.EXTRA_APPLICATION_ID, mContext.getPackageName());
                        if (urls.get(which).startsWith("smsto:")) {
                            intent.setClassName(mContext, "com.android.mms.ui.SendMessageToActivity");
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        mContext.startActivity(intent);
                    }
                    dialog.dismiss();
                }
            };

            b.setTitle(R.string.select_link_title);
            b.setCancelable(true);
            b.setAdapter(adapter, click);

            b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public final void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            b.show();
        }
    }


    private void setOnClickListener(final MessageItem msgItem) {
        switch(msgItem.mAttachmentType) {
        case WorkingMessage.IMAGE:
        case WorkingMessage.VIDEO:
            mImageView.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                	//add for multi-delete
            		if (mSelectedBox != null && mSelectedBox.getVisibility() == View.VISIBLE) {
            			mSelectedBox.setChecked(!mSelectedBox.isChecked()); 
            			if (null != mHandler) {
            	            Message msg = Message.obtain(mHandler, ITEM_CLICK);
            	            msg.arg1 = (int)(mMessageItem.mType.equals("mms")? -mMessageItem.mMsgId : mMessageItem.mMsgId);
            	            msg.sendToTarget();
            	        }
            			return;
            		}
                    MessageUtils.viewMmsMessageAttachment(mContext, msgItem.mMessageUri, msgItem.mSlideshow);
                }
            });
            mImageView.setOnLongClickListener(new OnLongClickListener() {
                public boolean onLongClick(View v) {
                    return v.showContextMenu();
                }
            });
            break;

        default:
            mImageView.setOnClickListener(null);
            break;
        }
    }

    private void setErrorIndicatorClickListener(final MessageItem msgItem) {
        String type = msgItem.mType;
        final int what;
        if (type.equals("sms")) {
            what = MSG_LIST_EDIT_SMS;
        } else {
            what = MSG_LIST_EDIT_MMS;
        }
        mDeliveredIndicator.setClickable(true);
        mDeliveredIndicator.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	//add for multi-delete
        		if (mSelectedBox != null && mSelectedBox.getVisibility() == View.VISIBLE) {			
        			return;
        		}
                if (null != mHandler) {
                    Message msg = Message.obtain(mHandler, what);
                    msg.obj = new Long(msgItem.mMsgId);
                    msg.sendToTarget();
                }
            }
        });
    }

    private void drawRightStatusIndicator(MessageItem msgItem) {
        // Locked icon
        if (msgItem.mLocked) {
            mLockedIndicator.setImageResource(R.drawable.ic_lock_message_sms);
            mLockedIndicator.setVisibility(View.VISIBLE);
        } else {
            mLockedIndicator.setVisibility(View.GONE);
        }

        // Delivery icon
        if (msgItem.isOutgoingMessage() && msgItem.isFailedMessage()) {
            mDeliveredIndicator.setImageResource(R.drawable.ic_list_alert_sms_failed);
            setErrorIndicatorClickListener(msgItem);
            mDeliveredIndicator.setVisibility(View.VISIBLE);
        } else if (msgItem.mDeliveryStatus == MessageItem.DeliveryStatus.FAILED) {
            mDeliveredIndicator.setImageResource(R.drawable.ic_list_alert_sms_failed);
            mDeliveredIndicator.setVisibility(View.VISIBLE);
        } else if (msgItem.mDeliveryStatus == MessageItem.DeliveryStatus.RECEIVED) {
            mDeliveredIndicator.setClickable(false);
            mDeliveredIndicator.setImageResource(R.drawable.ic_sms_mms_delivered);
            mDeliveredIndicator.setVisibility(View.VISIBLE);
        } else {
            mDeliveredIndicator.setVisibility(View.GONE);
        }

        // Message details icon
        if (msgItem.mDeliveryStatus == MessageItem.DeliveryStatus.INFO || msgItem.mReadReport) {
            mDetailsIndicator.setImageResource(R.drawable.ic_sms_mms_details);
            mDetailsIndicator.setVisibility(View.VISIBLE);
        } else {
            mDetailsIndicator.setVisibility(View.GONE);
        }
    }

    public void setImageRegionFit(String fit) {
        // TODO Auto-generated method stub
    }

    public void setImageVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    public void setText(String name, String text) {
        // TODO Auto-generated method stub
    }

    public void setTextVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    public void setVideo(String name, Uri video) {
        inflateMmsView();

        try {
            Bitmap bitmap = VideoAttachmentView.createVideoThumbnail(mContext, video);
            if (null == bitmap) {
                bitmap = BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_missing_thumbnail_video);
            }
            mImageView.setImageBitmap(bitmap);
            mImageView.setVisibility(VISIBLE);
        } catch (java.lang.OutOfMemoryError e) {
            Log.e(TAG, "setVideo: out of memory: ", e);
        }
    }

    public void setVideoVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    public void stopAudio() {
        // TODO Auto-generated method stub
    }

    public void stopVideo() {
        // TODO Auto-generated method stub
    }

    public void reset() {
        if (mImageView != null) {
            mImageView.setVisibility(GONE);
        }
    }

    public void setVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    public void pauseAudio() {
        // TODO Auto-generated method stub

    }

    public void pauseVideo() {
        // TODO Auto-generated method stub

    }

    public void seekAudio(int seekTo) {
        // TODO Auto-generated method stub

    }

    public void seekVideo(int seekTo) {
        // TODO Auto-generated method stub

    }
}
