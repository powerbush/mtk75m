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
import com.android.mms.ExceedMessageSizeException;
import com.android.mms.R;
import com.android.mms.data.WorkingMessage;
import com.android.mms.model.FileAttachmentModel;
import com.android.mms.model.SlideModel;
import com.android.mms.model.MediaModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.MmsConfig;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.mediatek.featureoption.FeatureOption;
import android.drm.DrmManagerClient;
import com.mediatek.xlog.Xlog;
/**
 * This is an embedded editor/view to add photos and sound/video clips
 * into a multimedia message.
 */
public class AttachmentEditor extends LinearLayout {
    private static final String TAG = "AttachmentEditor";

    static final int MSG_EDIT_SLIDESHOW   = 1;
    static final int MSG_SEND_SLIDESHOW   = 2;
    static final int MSG_PLAY_SLIDESHOW   = 3;
    static final int MSG_REPLACE_IMAGE    = 4;
    static final int MSG_REPLACE_VIDEO    = 5;
    static final int MSG_REPLACE_AUDIO    = 6;
    static final int MSG_PLAY_VIDEO       = 7;
    static final int MSG_PLAY_AUDIO       = 8;
    static final int MSG_VIEW_IMAGE       = 9;
    static final int MSG_REMOVE_ATTACHMENT = 10;

    private final Context mContext;
    private Handler mHandler;

    private SlideViewInterface mView;
    private View mFileAttachmentView;
    private WorkingMessage mWorkingMessage;
    private SlideshowModel mSlideshow;
    private Presenter mPresenter;
    private boolean mCanSend;
    private Button mSendButton;
    private boolean mTextIncludedInMms;
    private TextView mMediaSize;
    private ImageView mDrmLock;
    private boolean mFlagMini = false;
    public AttachmentEditor(Context context, AttributeSet attr) {
        super(context, attr);
        mContext = context;
    }

    public void update(WorkingMessage msg, boolean isMini) {
    	mFlagMini = isMini;
    	update(msg);
    }
    public void update(WorkingMessage msg) {
        hideView();
        mView = null;
        mFileAttachmentView = null;
        mWorkingMessage = msg;
        // If there's no attachment, we have nothing to do.
        if (!msg.hasAttachment()) {
            return;
        }

        // Get the slideshow from the message.
        mSlideshow = msg.getSlideshow();
        try{
            // file attachment view and other views are exclusive to each other
            if (mSlideshow.sizeOfFilesAttach() > 0) {
                mFileAttachmentView = createFileAttachmentView(msg);
                return;
            }

        	mView = createView(msg);
        }catch(IllegalArgumentException e){
        	return;
        }

        if ((mPresenter == null) || !mSlideshow.equals(mPresenter.getModel())) {
            mPresenter = PresenterFactory.getPresenter(
                    "MmsThumbnailPresenter", mContext, mView, mSlideshow);
        } else {
            mPresenter.setView(mView);
        }

        mPresenter.present();
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public void setCanSend(boolean enable) {
        if (mCanSend != enable) {
            mCanSend = enable;
            updateSendButton();
        }
    }
    
    private void updateSendButton() {
    	if (null != mSendButton) {
    		mSendButton.setEnabled(mCanSend);
    		mSendButton.setFocusable(mCanSend);
    	}
    }

    public void hideView() {
        if (mView != null) {
            ((View) mView).setVisibility(View.GONE);
        }
        if (mFileAttachmentView != null) {
            mFileAttachmentView.setVisibility(View.GONE);
        }
    }

    private View getStubView(int stubId, int viewId) {
        View view = findViewById(viewId);
        if (view == null) {
            ViewStub stub = (ViewStub) findViewById(stubId);
            view = stub.inflate();
        }

        return view;
    }

    private class MessageOnClick implements OnClickListener {
        private int mWhat;

        public MessageOnClick(int what) {
            mWhat = what;
        }

        public void onClick(View v) {
            Message msg = Message.obtain(mHandler, mWhat);
            msg.sendToTarget();
        }
    }

    private SlideViewInterface createView(WorkingMessage msg) {
        boolean inPortrait = inPortraitMode();
        
        if (mSlideshow.size() > 1) {
            return createSlideshowView(inPortrait, msg);
        } else if (mSlideshow.size() == 1) {

            SlideModel slide = mSlideshow.get(0);
            if (slide.hasImage()) {
                return createMediaView(
                        inPortrait ? R.id.image_attachment_view_portrait_stub
                                : R.id.image_attachment_view_landscape_stub,
                        inPortrait ? R.id.image_attachment_view_portrait
                                : R.id.image_attachment_view_landscape,
                        R.id.view_image_button, R.id.replace_image_button,
                        R.id.remove_image_button, R.id.media_size_info, msg
                                .getCurrentMessageSize(), MSG_VIEW_IMAGE,
                        MSG_REPLACE_IMAGE, MSG_REMOVE_ATTACHMENT, msg);
            } else if (slide.hasVideo()) {
                return createMediaView(
                        inPortrait ? R.id.video_attachment_view_portrait_stub
                                : R.id.video_attachment_view_landscape_stub,
                        inPortrait ? R.id.video_attachment_view_portrait
                                : R.id.video_attachment_view_landscape,
                        R.id.view_video_button, R.id.replace_video_button,
                        R.id.remove_video_button, R.id.media_size_info, msg
                                .getCurrentMessageSize(), MSG_PLAY_VIDEO,
                        MSG_REPLACE_VIDEO, MSG_REMOVE_ATTACHMENT, msg);
            } else if (slide.hasAudio()) {
                return createMediaView(
                        inPortrait ? R.id.audio_attachment_view_portrait_stub
                                : R.id.audio_attachment_view_landscape_stub,
                        inPortrait ? R.id.audio_attachment_view_portrait
                                : R.id.audio_attachment_view_landscape,
                        R.id.play_audio_button, R.id.replace_audio_button,
                        R.id.remove_audio_button, R.id.media_size_info, msg
                                .getCurrentMessageSize(), MSG_PLAY_AUDIO,
                        MSG_REPLACE_AUDIO, MSG_REMOVE_ATTACHMENT, msg);
            }
            throw new IllegalArgumentException();
        }
        return null;
    }

    private View createFileAttachmentView(WorkingMessage msg) {
        // There should be one and only one file
        List<FileAttachmentModel> attachFiles = mSlideshow.getAttachFiles();
        if (attachFiles == null || attachFiles.size() != 1) {
            Log.e(TAG, "createFileAttachmentView, oops no attach files found.");
            return null;
        }
        FileAttachmentModel attach = attachFiles.get(0);
        Log.i(TAG, "createFileAttachmentView, attach " + attach.toString());
        final View view = getStubView(R.id.file_attachment_view_stub, R.id.file_attachment_view);
        view.setVisibility(View.VISIBLE);
        final ImageView thumb = (ImageView) view.findViewById(R.id.file_attachment_thumbnail);
        final TextView name = (TextView) view.findViewById(R.id.file_attachment_name_info);
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
        final TextView size = (TextView) view.findViewById(R.id.file_attachment_size_info);
        size.setText(MessageUtils.getHumanReadableSize(attach.getAttachSize())
                +"/"+MmsConfig.getUserSetMmsSizeLimit(false) + "K");
        final ImageView remove = (ImageView) view.findViewById(R.id.file_attachment_button_remove);
        final ImageView divider = (ImageView) view.findViewById(R.id.file_attachment_divider);
        divider.setVisibility(View.VISIBLE);
        remove.setVisibility(View.VISIBLE);
        remove.setOnClickListener(new MessageOnClick(MSG_REMOVE_ATTACHMENT));
        return view;
    }

    /**
     * What is the current orientation?
     */
    private boolean inPortraitMode() {
        final Configuration configuration = mContext.getResources().getConfiguration();
        return configuration.orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    private SlideViewInterface createMediaView(
            int stub_view_id, int real_view_id,
            int view_button_id, int replace_button_id, int remove_button_id, int size_view_id,
            int msgSize,
            int view_message, int replace_message, int remove_message, WorkingMessage msg) {
        LinearLayout view = (LinearLayout)getStubView(stub_view_id, real_view_id);
        view.setVisibility(View.VISIBLE);

        Button viewButton = (Button) view.findViewById(view_button_id);
        Button replaceButton = (Button) view.findViewById(replace_button_id);
        Button removeButton = (Button) view.findViewById(remove_button_id);
        
        // show Mms Size  
        mMediaSize = (TextView) view.findViewById(size_view_id); 
        int sizeShow = (msgSize - 1)/1024 + 1;
        String info = sizeShow + "K/" + MmsConfig.getUserSetMmsSizeLimit(false) + "K";
        mMediaSize.setText(info); 

        viewButton.setOnClickListener(new MessageOnClick(view_message));
        replaceButton.setOnClickListener(new MessageOnClick(replace_message));
        removeButton.setOnClickListener(new MessageOnClick(remove_message));

        if (mFlagMini) {
        	replaceButton.setVisibility(View.GONE);
        }
        return (SlideViewInterface) view;
    }

    private SlideViewInterface createSlideshowView(boolean inPortrait, WorkingMessage msg) {
    	LinearLayout view;
    	view =(LinearLayout) getStubView(inPortrait ?
    			R.id.slideshow_attachment_view_portrait_stub :
    				R.id.slideshow_attachment_view_landscape_stub,
    				inPortrait ? R.id.slideshow_attachment_view_portrait :
    					R.id.slideshow_attachment_view_landscape);
    	view.setVisibility(View.VISIBLE);

        Button editBtn = (Button) view.findViewById(R.id.edit_slideshow_button);
       
        mSendButton = (Button) view.findViewById(R.id.send_slideshow_button);
        mSendButton.setOnClickListener(new MessageOnClick(MSG_SEND_SLIDESHOW));
        	
        updateSendButton();
        
        final ImageButton playBtn = (ImageButton) view.findViewById(
                R.id.play_slideshow_button);
    	if (FeatureOption.MTK_DRM_APP) {
    		if (msg.mHasDrmPart) {
    			Xlog.i(TAG, "mHasDrmPart");
        	    Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.mms_play_btn);        
        	    Drawable front = mContext.getResources().getDrawable(com.mediatek.internal.R.drawable.drm_red_lock);
        	    DrmManagerClient drmManager= new DrmManagerClient(mContext);
        	    Bitmap drmBitmap = drmManager.overlayBitmap(bitmap, front);
        	    playBtn.setImageBitmap(drmBitmap);
        	    if (bitmap != null && !bitmap.isRecycled()) {
        	    	bitmap.recycle();
        	    	bitmap = null;
        	    }
    		}
    	}

        // show Mms Size  
        mMediaSize = (TextView) view.findViewById(R.id.media_size_info); 
		int sizeShow = (msg.getCurrentMessageSize() - 1)/1024 + 1;
        String info = sizeShow + "K/" + MmsConfig.getUserSetMmsSizeLimit(false) + "K";
        
        mMediaSize.setText(info);

        editBtn.setOnClickListener(new MessageOnClick(MSG_EDIT_SLIDESHOW));
        playBtn.setOnClickListener(new MessageOnClick(MSG_PLAY_SLIDESHOW));

        return (SlideViewInterface) view;
    }

    public void onTextChangeForOneSlide(CharSequence s) throws ExceedMessageSizeException {
    	
    	if (null == mMediaSize || (mWorkingMessage.hasSlideshow() && mWorkingMessage.getSlideshow().size() >1)) {
    	    return;
    	}

    	// borrow this method to get the encoding type
	    int[] params = SmsMessage.calculateLength(s, false);
	    int type = params[3];
	    int totalSize = 0;
	    if (mWorkingMessage.hasAttachment()) {
	        totalSize = mWorkingMessage.getCurrentMessageSize();
	    }
    	// show
		int sizeShow = (totalSize - 1)/1024 + 1;
		String info = sizeShow + "K/" + MmsConfig.getUserSetMmsSizeLimit(false) + "K";
        mMediaSize.setText(info);
    }
}
