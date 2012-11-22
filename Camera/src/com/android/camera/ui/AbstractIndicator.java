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
import android.graphics.Matrix;
import android.graphics.Rect;
import android.view.animation.AlphaAnimation;
import android.view.animation.Transformation;

import javax.microedition.khronos.opengles.GL11;

abstract class AbstractIndicator extends GLView {
    private static final int DEFAULT_PADDING = 3;
    private int mOrientation = 0;

    abstract protected BitmapTexture getIcon();

    public AbstractIndicator(Context context) {
        int padding = GLRootView.dpToPixel(context, DEFAULT_PADDING);
        setPaddings(padding, 0, padding, 0);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        BitmapTexture icon = getIcon();
        new MeasureHelper(this)
               .setPreferredContentSize(icon.getWidth(), icon.getHeight())
               .measure(widthSpec, heightSpec);
    }

    @Override
    protected void render(GLRootView root, GL11 gl) {
        BitmapTexture icon = getIcon();
        if (icon != null) {
            Rect p = mPaddings;
            int width = getWidth() - p.left - p.right;
            int height = getHeight() - p.top - p.bottom;
            if (mOrientation != 0) {
                Transformation trans = root.pushTransform();
                Matrix matrix = trans.getMatrix();
                matrix.preTranslate(p.left + width / 2, p.top + height / 2);
                matrix.preRotate(-mOrientation);
                icon.draw(root, -icon.getWidth() / 2, -icon.getHeight() / 2);
                root.popTransform();
            } else {
                icon.draw(root,
                        p.left + (width - icon.getWidth()) / 2,
                        p.top + (height - icon.getHeight()) / 2);
            }
        }
    }

    public void setOrientation(int orientation) {
        if (orientation % 90 != 0) throw new IllegalArgumentException();
        orientation = orientation % 360;
        if (orientation < 0) orientation += 360;

        if (mOrientation == orientation) return;
        mOrientation = orientation;

        if (getGLRootView() != null) {
            AlphaAnimation anim = new AlphaAnimation(0.2f, 1);
            anim.setDuration(200);
            startAnimation(anim);
        }
    }

    abstract public GLView getPopupContent();

    abstract public void overrideSettings(String key, String settings);

    abstract public void reloadPreferences();
}
