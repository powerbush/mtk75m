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
import com.android.mms.model.AudioModel;
import com.android.mms.model.ImageModel;
import com.android.mms.model.Model;
import com.android.mms.model.SlideModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.model.VideoModel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.drm.DrmManagerClient;
import android.media.MediaMetadataRetriever;
import com.mediatek.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;

public class MmsThumbnailPresenter extends Presenter {
    private static final String TAG = "MmsThumbnailPresenter";
    private Context mContext;
    private int mSlideCount = 0;
    public MmsThumbnailPresenter(Context context, ViewInterface view, Model model) {
        super(context, view, model);
        mContext = context;
    }

    @Override
    public void present() {
        SlideModel slide = ((SlideshowModel) mModel).get(0);
        mSlideCount = ((SlideshowModel) mModel).size();
        if (slide != null) {
            Xlog.i(TAG, "The first slide is not null.");
            presentFirstSlide((SlideViewInterface) mView, slide);
        }
    }

    private void presentFirstSlide(SlideViewInterface view, SlideModel slide) {
        view.reset();
        boolean imageVisibility = true;
        if (slide.hasImage()) {
            Xlog.i(TAG, "The first slide has image.");
            presentImageThumbnail(view, slide.getImage());
        } else if (slide.hasVideo()) {
            Xlog.i(TAG, "The first slide has video.");
            presentVideoThumbnail(view, slide.getVideo());
        } else if (slide.hasAudio()) {
            Xlog.i(TAG, "The first slide has audio.");
            presentAudioThumbnail(view, slide.getAudio());
        } else {
            Xlog.i(TAG, "The first slide has only text.");
            imageVisibility = false;
        }
        view.setImageVisibility(imageVisibility);
    }

    private void presentVideoThumbnail(SlideViewInterface view, VideoModel video) {
        if (video.isDrmProtected()) {
            showDrmIcon(view, video.getSrc());
        } else {
        	view.setVideo(video.getSrc(), video.getUri());
        }
    }

    private void presentImageThumbnail(SlideViewInterface view, ImageModel image) {
        if (image.isDrmProtected()) {
            showDrmIcon(view, image.getSrc());
        } else {
        	if (FeatureOption.MTK_DRM_APP) {
        	    String extName = image.getSrc().substring(image.getSrc().lastIndexOf('.') + 1);
        	    if (extName.equals("dcf") && mSlideCount == 1) {
            	    Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
            	    		R.drawable.ic_missing_thumbnail_picture);
            	    Drawable front = mContext.getResources().getDrawable(com.mediatek.internal.R.drawable.drm_red_lock);
            	    DrmManagerClient drmManager= new DrmManagerClient(mContext);
            	    Bitmap drmBitmap = drmManager.overlayBitmap(bitmap, front);
        		    view.setImage(image.getSrc(), drmBitmap);
        		    if (bitmap != null && !bitmap.isRecycled()) {
            	    	bitmap.recycle();
            	    	bitmap = null;
            	    }
        	    } else {
                    view.setImage(image.getSrc(), image.getBitmap());
        	    }
        	} else {
        		view.setImage(image.getSrc(), image.getBitmap());
        	}
        }
    }

    protected void presentAudioThumbnail(SlideViewInterface view, AudioModel audio) {
        if (audio.isDrmProtected()) {
            showDrmIcon(view, audio.getSrc());
        } else {
        	view.setAudio(audio.getUri(), audio.getSrc(), audio.getExtras());  
        }
    }

    // Show an icon instead of real content in the thumbnail.
    private void showDrmIcon(SlideViewInterface view, String name) {
        try {
            Bitmap bitmap = BitmapFactory.decodeResource(
                    mContext.getResources(), R.drawable.ic_mms_drm_protected);
            view.setImage(name, bitmap);
        } catch (java.lang.OutOfMemoryError e) {
            Log.e(TAG, "showDrmIcon: out of memory: ", e);
        }
    }

    public void onModelChanged(Model model, boolean dataChanged) {
        // TODO Auto-generated method stub
    }
}
