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

import com.google.android.mms.ContentType;
import com.google.android.mms.MmsException;
import com.android.mms.model.AudioModel;
import com.android.mms.model.ImageModel;
import com.android.mms.model.RegionModel;
import com.android.mms.model.SlideModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.model.TextModel;
import com.android.mms.model.VideoModel;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import com.android.mms.ExceedMessageSizeException;
/**
 * An utility to edit contents of a slide.
 */
public class SlideshowEditor {
    private static final String TAG = "Mms:slideshow";

    public static final int MAX_SLIDE_NUM = 20;

    private final Context mContext;
    private final SlideshowModel mModel;

    public SlideshowEditor(Context context, SlideshowModel model) {
        mContext = context;
        mModel = model;
    }

    /**
     * Add a new slide to the end of message.
     *
     * @return true if success, false if reach the max slide number.
     */
    public boolean addNewSlide() {
        int position = mModel.size();
        return addNewSlide(position);
    }

    /**
     * Add a new slide at the specified position in the message.
     *
     * @return true if success, false if reach the max slide number.
     * @throws IndexOutOfBoundsException - if position is out of range
     *         (position < 0 || position > size()).
     */
    public boolean addNewSlide(int position) {
        int size = mModel.size();
        if (size < MAX_SLIDE_NUM) {
            SlideModel slide = new SlideModel(mModel);

            TextModel text = new TextModel(
                    mContext, ContentType.TEXT_PLAIN, "text_" + size + ".txt",
                    mModel.getLayout().getTextRegion());
            try{
            slide.add(text);

            mModel.add(position, slide);
            }catch(ExceedMessageSizeException e){
            	return false;
            	
            }
            
            return true;
        } else {
            Log.w(TAG, "The limitation of the number of slides is reached.");
            return false;
        }
    }

    /**
     * Remove one slide.
     *
     * @param position
     */
    public void removeSlide(int position) {
        mModel.remove(position);
    }

    /**
     * Remove all slides.
     */
    public void removeAllSlides() {
        while (mModel.size() > 0) {
            removeSlide(0);
        }
    }

    /**
     * Remove the text of the specified slide.
     *
     * @param position index of the slide
     * @return true if success, false if no text in the slide.
     */
    public boolean removeText(int position) {
        return mModel.get(position).removeText();
    }

    public boolean removeImage(int position) {
        return mModel.get(position).removeImage();
    }

    public boolean removeVideo(int position) {
        return mModel.get(position).removeVideo();
    }

    public boolean removeAudio(int position) {
        return mModel.get(position).removeAudio();
    }

    public void changeText(int position, String newText) {
        if (newText != null) {
            SlideModel slide = mModel.get(position);
            TextModel text = slide.getText();
            int oldLength ;
            int newLength = newText.getBytes().length;
            if (text == null) {
            	oldLength = 0;
                text = new TextModel(mContext,
                        ContentType.TEXT_PLAIN, "text_" + position + ".txt",
                        mModel.getLayout().getTextRegion(),
                        TextUtils.getTrimmedLength(newText)>=0 ? newText.getBytes() : null);
                text.setText(newText);
                slide.add(text);
            } else if (!newText.equals(text.getText())) {
            	oldLength = text.getText().getBytes().length;
                if (newLength > oldLength) {
                	mModel.checkMessageSize(newLength - oldLength);
                    mModel.increaseSlideshowSize(newLength - oldLength);
                } else {
                	mModel.decreaseSlideshowSize(oldLength - newLength);
                }
                text.setText(newText);
                text.changeSizeOnlyForText(newText);
            }
        }
    }

    public void changeImage(int position, Uri newImage) throws MmsException {
        mModel.get(position).add(new ImageModel(
                mContext, newImage, mModel.getLayout().getImageRegion()));
    }

    public void changeAudio(int position, Uri newAudio) throws MmsException {
        AudioModel audio = new AudioModel(mContext, newAudio);
        SlideModel slide = mModel.get(position);
        slide.add(audio);
        slide.updateDuration(audio.getDuration());
    }

    public void changeVideo(int position, Uri newVideo) throws MmsException {
        VideoModel video = new VideoModel(mContext, newVideo,
                mModel.getLayout().getImageRegion());
        SlideModel slide = mModel.get(position);
        slide.add(video);
        slide.updateDuration(video.getDuration());
    }

    public void moveSlideUp(int position) {
            mModel.addNoCheckSize(position - 1, mModel.remove(position));
    }

    public void moveSlideDown(int position) {
        mModel.addNoCheckSize(position + 1, mModel.remove(position));
    }

    public void changeDuration(int position, int dur) {
        if (dur >= 0) {
            mModel.get(position).setDuration(dur);
        }
    }

    public void changeLayout(int layout) {
        mModel.getLayout().changeTo(layout);
    }

    public RegionModel getImageRegion() {
        return mModel.getLayout().getImageRegion();
    }

    public RegionModel getTextRegion() {
        return mModel.getLayout().getTextRegion();
    }
}
