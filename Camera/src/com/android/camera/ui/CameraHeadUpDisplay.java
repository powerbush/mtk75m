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

import com.android.camera.CameraSettings;
import com.android.camera.IconListPreference;
import com.android.camera.ListPreference;
import com.android.camera.PreferenceGroup;

public class CameraHeadUpDisplay extends HeadUpDisplay {

    private static final String TAG = "CamcoderHeadUpDisplay";

    private OtherSettingsIndicator mOtherSettings;
    private GpsIndicator mGpsIndicator;
    private ZoomIndicator mZoomIndicator;
    private Context mContext;
    private float[] mInitialZoomRatios;
    private int mInitialOrientation;

    public CameraHeadUpDisplay(Context context) {
        super(context);
        mContext = context;
    }

    public void initialize(Context context, PreferenceGroup group,
            float[] initialZoomRatios, int initialOrientation) {
        mInitialZoomRatios = initialZoomRatios;
        mInitialOrientation = initialOrientation;
        super.initialize(context, group);
    }

    @Override
    protected void initializeIndicatorBar(
            Context context, PreferenceGroup group) {
        super.initializeIndicatorBar(context, group);

        ListPreference prefs[] = getListPreferences(group,
                CameraSettings.KEY_FOCUS_MODE,
                CameraSettings.KEY_EXPOSURE,
                CameraSettings.KEY_SCENE_MODE,
                CameraSettings.KEY_PICTURE_SIZE,
                CameraSettings.KEY_JPEG_QUALITY,
                CameraSettings.KEY_COLOR_EFFECT);

        mOtherSettings = new OtherSettingsIndicator(context, prefs);
        mOtherSettings.setOnRestorePreferencesClickedRunner(new Runnable() {
            public void run() {
                if (mListener != null) {
                    mListener.onRestorePreferencesClicked();
                }
            }
        });
        mIndicatorBar.addComponent(mOtherSettings);

        mGpsIndicator = new GpsIndicator(
                context, (IconListPreference)
                group.findPreference(CameraSettings.KEY_RECORD_LOCATION));
        mIndicatorBar.addComponent(mGpsIndicator);

        addIndicator(context, group, CameraSettings.KEY_WHITE_BALANCE);
        addIndicator(context, group, CameraSettings.KEY_FLASH_MODE);

        if (mInitialZoomRatios != null) {
            mZoomIndicator = new ZoomIndicator(mContext);
            mZoomIndicator.setZoomRatios(mInitialZoomRatios);
            mIndicatorBar.addComponent(mZoomIndicator);
        } else {
            mZoomIndicator = null;
        }

        addIndicator(context, group, CameraSettings.KEY_CAMERA_ID);

        mIndicatorBar.setOrientation(mInitialOrientation);
    }

    public void setZoomListener(ZoomControllerListener listener) {
        // The rendering thread won't access listener variable, so we don't
        // need to do concurrency protection here
        mZoomIndicator.setZoomListener(listener);
    }

    public void setZoomIndex(int index) {
        GLRootView root = getGLRootView();
        if (root != null) {
            synchronized (root) {
                mZoomIndicator.setZoomIndex(index);
            }
        } else {
            mZoomIndicator.setZoomIndex(index);
        }
    }

    public void setGpsHasSignal(final boolean hasSignal) {
        GLRootView root = getGLRootView();
        if (root != null) {
            synchronized (root) {
                mGpsIndicator.setHasSignal(hasSignal);
            }
        } else {
            mGpsIndicator.setHasSignal(hasSignal);
        }
    }

    /**
     * Sets the zoom rations the camera driver provides. This methods must be
     * called before <code>setZoomListener()</code> and
     * <code>setZoomIndex()</code>
     */
    public void setZoomRatios(float[] zoomRatios) {
        GLRootView root = getGLRootView();
        if (root != null) {
            synchronized(root) {
                setZoomRatiosLocked(zoomRatios);
            }
        } else {
            setZoomRatiosLocked(zoomRatios);
        }
    }

    private void setZoomRatiosLocked(float[] zoomRatios) {
        mZoomIndicator.setZoomRatios(zoomRatios);
    }
}
