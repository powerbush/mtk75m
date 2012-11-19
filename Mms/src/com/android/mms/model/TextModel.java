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

package com.android.mms.model;

import com.android.mms.dom.smil.SmilMediaElementImpl;
import android.drm.mobile1.DrmException;
import com.android.mms.drm.DrmWrapper;
import com.google.android.mms.pdu.CharacterSets;

import org.w3c.dom.events.Event;
import org.w3c.dom.smil.ElementTime;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class TextModel extends RegionMediaModel {
    private static final String TAG = "Mms/text";

    private CharSequence mText;
    private final int mCharset;

    public TextModel(Context context, String contentType, String src, RegionModel region) {
        this(context, contentType, src, CharacterSets.UTF_8, new byte[0], region);
    }
    public TextModel(Context context, String contentType, String src, RegionModel region, byte[] data) {
        this(context, contentType, src, CharacterSets.UTF_8, data, region);
    }
    public TextModel(Context context, String contentType, String src,
            int charset, byte[] data, RegionModel region) {
        super(context, SmilHelper.ELEMENT_TAG_TEXT, contentType, src,
                data != null ? data : new byte[0], region);

        if (charset == CharacterSets.ANY_CHARSET) {
            // By default, we use ISO_8859_1 to decode the data
            // which character set wasn't set.
            charset = CharacterSets.ISO_8859_1;
        }
        mCharset = charset;
        mText = extractTextFromData(data);
    }

    private CharSequence extractTextFromData(byte[] data) {
        if (data != null) {
            try {
                if (CharacterSets.ANY_CHARSET == mCharset) {
                    return new String(data); // system default encoding.
                } else {
                    String name = CharacterSets.getMimeName(mCharset);
                    return new String(data, name);
                }
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Unsupported encoding: " + mCharset, e);
                return new String(data); // system default encoding.
            }
        }
        return "";
    }

    public TextModel(Context context, String contentType, String src, int charset,
            DrmWrapper wrapper, RegionModel regionModel) throws IOException {
        super(context, SmilHelper.ELEMENT_TAG_TEXT, contentType, src, wrapper, regionModel);

        if (charset == CharacterSets.ANY_CHARSET) {
            // By default, we use ISO_8859_1 to decode the data
            // which character set wasn't set.
            charset = CharacterSets.ISO_8859_1;
        }
        mCharset = charset;
    }

    public String getText() {
        if (mText == null) {
            try {
                mText = extractTextFromData(getData());
            } catch (DrmException e) {
                Log.e(TAG, e.getMessage(), e);
                // Display DRM error message in place.
                mText = e.getMessage();
            }
        }

        // If our internal CharSequence is not already a String,
        // re-save it as a String so subsequent calls to getText will
        // be less expensive.
        if (!(mText instanceof String)) {
            mText = mText.toString();
        }

        return mText.toString();
    }

    public void setText(CharSequence text) {
        mText = text;
        notifyModelChanged(true);
    }

    public void cloneText() {
        mText = new String(mText.toString());
    }

    public int getCharset() {
        return mCharset;
    }

    // EventListener Interface
    public void handleEvent(Event evt) {
        if (evt.getType().equals(SmilMediaElementImpl.SMIL_MEDIA_START_EVENT)) {
            mVisible = true;
        } else if (mFill != ElementTime.FILL_FREEZE) {
            mVisible = false;
        }

        notifyModelChanged(false);
    }
}
