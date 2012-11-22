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
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.camera;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.android.camera.R;

/**
 * This class draws an icon which changes according to the mode. For example,
 * The flash icon can have on, off, and auto modes. The user can use
 * {@link #setMode(String)} to change the mode (and the icon).
 */
public class IconIndicator extends ImageView {

    private Drawable[] mIcons;
    private CharSequence[] mModes;

    public IconIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.IconIndicator, defStyle, 0);
        Drawable icons[] = loadIcons(context.getResources(),
                a.getResourceId(R.styleable.IconIndicator_icons, 0));
        CharSequence modes[] =
                a.getTextArray(R.styleable.IconIndicator_modes);
        a.recycle();

        setModesAndIcons(modes, icons);
        setImageDrawable(mIcons.length > 0 ? mIcons[0] : null);
    }

    public IconIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    private Drawable[] loadIcons(Resources resources, int iconsId) {
        TypedArray array = resources.obtainTypedArray(iconsId);
        int n = array.length();
        Drawable drawable[] = new Drawable[n];
        for (int i = 0; i < n; ++i) {
            int id = array.getResourceId(i, 0);
            drawable[i] = id == 0 ? null : resources.getDrawable(id);
        }
        array.recycle();
        return drawable;
    }

    private void setModesAndIcons(CharSequence[] modes, Drawable icons[]) {
        if (modes.length != icons.length || icons.length == 0) {
            throw new IllegalArgumentException();
        }
        mIcons = icons;
        mModes = modes;
    }

    public void setMode(String mode) {
        for (int i = 0, n = mModes.length; i < n; ++i) {
            if (mModes[i].equals(mode)) {
                setImageDrawable(mIcons[i]);
                return;
            }
        }
        throw new IllegalArgumentException("unknown mode: " + mode);
    }
}
