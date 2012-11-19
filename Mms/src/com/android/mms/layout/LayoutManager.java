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

package com.android.mms.layout;

import android.content.Context;
import android.content.res.Configuration;
import android.util.Config;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

/**
 * MMS presentation layout management.
 */
public class LayoutManager {
    private static final String TAG = "LayoutManager";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;

    private final Context mContext;
    private LayoutParameters mLayoutParams;

    private static LayoutManager sInstance;

    private LayoutManager(Context context) {
        mContext = context;
        initLayoutParameters(context.getResources().getConfiguration());
    }

    private void initLayoutParameters(Configuration configuration) {
        mLayoutParams = getLayoutParameters(
                configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                ? LayoutParameters.HVGA_PORTRAIT
                : LayoutParameters.HVGA_LANDSCAPE);

        if (LOCAL_LOGV) {
            Log.v(TAG, "LayoutParameters: " + mLayoutParams.getTypeDescription()
                    + ": " + mLayoutParams.getWidth() + "x" + mLayoutParams.getHeight());
        }
    }

    private static LayoutParameters getLayoutParameters(int displayType) {
        switch (displayType) {
            case LayoutParameters.HVGA_LANDSCAPE:
                return new HVGALayoutParameters(LayoutParameters.HVGA_LANDSCAPE);
            case LayoutParameters.HVGA_PORTRAIT:
                return new HVGALayoutParameters(LayoutParameters.HVGA_PORTRAIT);
        }

        throw new IllegalArgumentException(
                "Unsupported display type: " + displayType);
    }

    public static void init(Context context) {
        if (LOCAL_LOGV) {
            Log.v(TAG, "DefaultLayoutManager.init()");
        }

        if (sInstance != null) {
            Log.w(TAG, "Already initialized.");
        }
        sInstance = new LayoutManager(context);
    }

    public static LayoutManager getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("Uninitialized.");
        }
        return sInstance;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (LOCAL_LOGV) {
            Log.v(TAG, "-> LayoutManager.onConfigurationChanged().");
        }
        initLayoutParameters(newConfig);
    }

    public int getLayoutType() {
        return mLayoutParams.getType();
    }

    public int getLayoutWidth() {
        return mLayoutParams.getWidth();
    }

    public int getLayoutHeight() {
        return mLayoutParams.getHeight();
    }

    public LayoutParameters getLayoutParameters() {
        return mLayoutParams;
    }
}
