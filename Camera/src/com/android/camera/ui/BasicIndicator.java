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
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.camera.ui;

import android.content.Context;

import com.android.camera.IconListPreference;
import com.android.camera.R;
import com.android.camera.Util;
import com.android.camera.ui.GLListView.OnItemSelectedListener;

class BasicIndicator extends AbstractIndicator {
    private static final int COLOR_OPTION_ITEM_HIGHLIGHT = 0xFF181818;

    private final ResourceTexture mIcon[];
    private final IconListPreference mPreference;
    protected int mIndex;
    private GLListView mPopupContent;
    private PreferenceAdapter mModel;
    private String mOverride;

    public BasicIndicator(Context context, IconListPreference preference) {
        super(context);
        mPreference = preference;
        mIcon = new ResourceTexture[preference.getLargeIconIds().length];
        mIndex = preference.findIndexOfValue(preference.getValue());
    }

    // Set the override and/or reload the value from preferences.
    private void updateContent(String override, boolean reloadValue) {
        if (!reloadValue && Util.equals(mOverride, override)) return;
        IconListPreference pref = mPreference;
        mOverride = override;
        int index = pref.findIndexOfValue(
                override == null ? pref.getValue() : override);
        if (mIndex != index) {
            mIndex = index;
            invalidate();
        }
    }

    @Override
    public void overrideSettings(String key, String settings) {
        IconListPreference pref = mPreference;
        if (!pref.getKey().equals(key)) return;
        updateContent(settings, false);
    }

    @Override
    public void reloadPreferences() {
        if (mModel != null) mModel.reload();
        updateContent(null, true);
    }

    @Override
    public GLView getPopupContent() {
        if (mPopupContent == null) {
            Context context = getGLRootView().getContext();
            mPopupContent = new GLListView(context);
            mPopupContent.setHighLight(
                    new ColorTexture(COLOR_OPTION_ITEM_HIGHLIGHT));
            mPopupContent.setScroller(new NinePatchTexture(
                    context, R.drawable.scrollbar_handle_vertical));
            mModel = new PreferenceAdapter(context, mPreference);
            mPopupContent.setOnItemSelectedListener(new MyListener(mModel));
            mPopupContent.setDataModel(mModel);
        }
        mModel.overrideSettings(mOverride);
        return mPopupContent;
    }

    protected void onPreferenceChanged(int newIndex) {
        if (newIndex == mIndex) return;
        mIndex = newIndex;
        invalidate();
    }

    private class MyListener implements OnItemSelectedListener {

        private final PreferenceAdapter mAdapter;

        public MyListener(PreferenceAdapter adapter) {
            mAdapter = adapter;
        }

        public void onItemSelected(GLView view, int position) {
            mAdapter.onItemSelected(view, position);
            onPreferenceChanged(position - 1);
        }
    }

    @Override
    protected ResourceTexture getIcon() {
        int index = mIndex;
        if (mIcon[index] == null) {
            Context context = getGLRootView().getContext();
            mIcon[index] = new ResourceTexture(
                    context, mPreference.getLargeIconIds()[index]);
        }
        return mIcon[index];
    }
}
