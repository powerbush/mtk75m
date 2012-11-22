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
import android.util.AttributeSet;

import com.android.camera.R;

import java.util.List;

/** A {@code ListPreference} where each entry has a corresponding icon. */
public class IconListPreference extends ListPreference {

    private int mIconIds[];
    private int mLargeIconIds[];

    public IconListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.IconListPreference, 0, 0);
        Resources res = context.getResources();
        mIconIds = getIconIds(res, a.getResourceId(
                R.styleable.IconListPreference_icons, 0));
        mLargeIconIds = getIconIds(res, a.getResourceId(
                R.styleable.IconListPreference_largeIcons, 0));
        a.recycle();
    }

    public int[] getLargeIconIds() {
        return mLargeIconIds;
    }

    public int[] getIconIds() {
        return mIconIds;
    }

    public void setLargeIconIds(int[] largeIconIds) {
        mLargeIconIds = largeIconIds;
    }

    public void setIconIds(int[] iconIds) {
        mIconIds = iconIds;
    }

    private int[] getIconIds(Resources res, int iconsRes) {
        if (iconsRes == 0) return null;
        TypedArray array = res.obtainTypedArray(iconsRes);
        int n = array.length();
        int ids[] = new int[n];
        for (int i = 0; i < n; ++i) {
            ids[i] = array.getResourceId(i, 0);
        }
        array.recycle();
        return ids;
    }

    @Override
    public void filterUnsupported(List<String> supported) {
        CharSequence entryValues[] = getEntryValues();
        IntArray iconIds = new IntArray();
        IntArray largeIconIds = new IntArray();

        for (int i = 0, len = entryValues.length; i < len; i++) {
            if (supported.indexOf(entryValues[i].toString()) >= 0) {
                iconIds.add(mIconIds[i]);
                largeIconIds.add(mLargeIconIds[i]);
            }
        }
        int size = iconIds.size();
        mIconIds = iconIds.toArray(new int[size]);
        mLargeIconIds = iconIds.toArray(new int[size]);
        super.filterUnsupported(supported);
    }
}
