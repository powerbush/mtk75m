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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.Map;
import android.drm.DrmManagerClient;
import com.mediatek.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;

/**
 * A simplified view of slide in the slides list.
 */
public class SlideListItemView extends LinearLayout implements SlideViewInterface {
    private static final String TAG = "SlideListItemView";

    private TextView mTextPreview;
    private ImageView mImagePreview;
    private TextView mAttachmentName;
    private ImageView mAttachmentIcon;
    private Context mContext;

    public SlideListItemView(Context context) {
        super(context);
        mContext = context;
    }

    public SlideListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        mTextPreview = (TextView) findViewById(R.id.text_preview);
        mTextPreview.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        mImagePreview = (ImageView) findViewById(R.id.image_preview);
        mAttachmentName = (TextView) findViewById(R.id.attachment_name);
        mAttachmentIcon = (ImageView) findViewById(R.id.attachment_icon);
    }

    public void startAudio() {
        // Playing audio is not needed in this view.
    }

    public void startVideo() {
        // Playing audio is not needed in this view.
    }

    public void setAudio(Uri audio, String name, Map<String, ?> extras) {
        if (name != null) {
            mAttachmentName.setText(name);
            mAttachmentIcon.setImageResource(R.drawable.ic_mms_music);
        } else {
            mAttachmentName.setText("");
            mAttachmentIcon.setImageDrawable(null);
        }
        Xlog.i(TAG, "Audio name is " + name);
        String extName = name.substring(name.lastIndexOf('.') + 1);
        if (extName.equals("dcf")) {
        	Xlog.i(TAG, "contain drm audio");
        	if (FeatureOption.MTK_DRM_APP) {
        	    Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_launcher_slideshow_add_sms);
        	    Drawable front = getResources().getDrawable(com.mediatek.internal.R.drawable.drm_red_lock);
        	    DrmManagerClient drmManager= new DrmManagerClient(mContext);
        	    Bitmap drmBitmap = drmManager.overlayBitmap(bitmap, front);
        	    mImagePreview.setImageBitmap(drmBitmap);
        	    if (bitmap != null && !bitmap.isRecycled()) {
        	    	bitmap.recycle();
        	    	bitmap = null;
        	    }
        	}
        }
    }

    public void setImage(String name, Bitmap bitmap) {
        try {
            if (null == bitmap) {
                bitmap = BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_missing_thumbnail_picture);
            }
            Xlog.i(TAG, "Image name is " + name);
            String extName = name.substring(name.lastIndexOf('.') + 1);
            if (extName.equals("dcf")) {
            	if (FeatureOption.MTK_DRM_APP) {
            	    Drawable front = getResources().getDrawable(com.mediatek.internal.R.drawable.drm_red_lock);
            	    DrmManagerClient drmManager= new DrmManagerClient(mContext);
            	    Bitmap drmBitmap = drmManager.overlayBitmap(bitmap, front);
            	    mImagePreview.setImageBitmap(drmBitmap);
            	    if (bitmap != null && !bitmap.isRecycled()) {
            	    	bitmap.recycle();
            	    	bitmap = null;
            	    }
            	} else {
            		mImagePreview.setImageBitmap(bitmap);
            	}
            } else {
                mImagePreview.setImageBitmap(bitmap);
            }
        } catch (java.lang.OutOfMemoryError e) {
            Log.e(TAG, "setImage: out of memory: ", e);
        }
    }

    public void setImageRegionFit(String fit) {
        // TODO Auto-generated method stub
    }

    public void setImageVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    public void setText(String name, String text) {
        mTextPreview.setText(text);
        mTextPreview.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
    }

    public void setTextVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    public void setVideo(String name, Uri video) {
        if (name != null) {
            mAttachmentName.setText(name);
            mAttachmentIcon.setImageResource(R.drawable.movie);
        } else {
            mAttachmentName.setText("");
            mAttachmentIcon.setImageDrawable(null);
        }

        MediaPlayer mp = new MediaPlayer();
        try {
            Xlog.i(TAG, "Video name is " + name);
            String extName = name.substring(name.lastIndexOf('.') + 1);
            if (extName.equals("dcf")) {
            	if (FeatureOption.MTK_DRM_APP) {
            		Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
            				R.drawable.ic_missing_thumbnail_video);         
            	    Drawable front = getResources().getDrawable(com.mediatek.internal.R.drawable.drm_red_lock);
            	    DrmManagerClient drmManager= new DrmManagerClient(mContext);
            	    Bitmap drmBitmap = drmManager.overlayBitmap(bitmap, front);
            	    mImagePreview.setImageBitmap(drmBitmap);
            	    if (bitmap != null && !bitmap.isRecycled()) {
            	    	bitmap.recycle();
            	    	bitmap = null;
            	    }
            	} else {
            		mImagePreview.setImageBitmap(createVideoThumbnail(mContext, video));
            	}
            } else {
            	mp.setDataSource(mContext, video);
                mImagePreview.setImageBitmap(createVideoThumbnail(mContext, video));
            }
        } catch (IOException e) {
            Log.e(TAG, "Unexpected IOException.", e);
        } finally {
            mp.release();
        }
    }

	/**
	 * get the thumbnail of a video.
	 */
	private Bitmap createVideoThumbnail(Context context, Uri uri) {
		Bitmap bitmap = null;
		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		try {
			retriever.setDataSource(context, uri);
			bitmap = retriever.getFrameAtTime(1000);
		} catch (IllegalArgumentException ex) {
			// Assume this is a corrupt video file
			Xlog.e(TAG, ex.getMessage());
		} catch (RuntimeException ex) {
			Xlog.e(TAG, ex.getMessage());
		}
		finally {
			try {
				retriever.release();
			} catch (RuntimeException ex) {
				// Ignore failures while cleaning up.
			}
		}
		return bitmap;
	}
    public void setVideoVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    public void stopAudio() {
        // Stopping audio is not needed in this view.
    }

    public void stopVideo() {
        // Stopping video is not needed in this view.
    }

    public void reset() {
        // TODO Auto-generated method stub
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
