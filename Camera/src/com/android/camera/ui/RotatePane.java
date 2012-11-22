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

import android.graphics.Matrix;
import android.view.MotionEvent;

import javax.microedition.khronos.opengles.GL11;


class RotatePane extends GLView {

    public static final int UP = 0;
    public static final int RIGHT = 1;
    public static final int DOWN = 2;
    public static final int LEFT = 3;

    private int mOrientation = 0;

    private GLView mChild;

    @Override
    protected void onLayout(
            boolean change, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        switch (mOrientation) {
            case UP:
            case DOWN:
                mChild.layout(0, 0, width, height);
                break;
            case LEFT:
            case RIGHT:
                mChild.layout(0, 0, height, width);
                break;
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        GLView c = mChild;
        switch(mOrientation) {
            case UP:
            case DOWN:
                c.measure(widthSpec, heightSpec);
                setMeasuredSize(c.getMeasuredWidth(), c.getMeasuredHeight());
                break;
            case LEFT:
            case RIGHT:
                mChild.measure(heightSpec, widthSpec);
                setMeasuredSize(c.getMeasuredHeight(), c.getMeasuredWidth());
        }
    }

    @Override
    protected void render(GLRootView view, GL11 gl) {

        if (mOrientation == UP) {
            mChild.render(view, gl);
            return;
        }

        view.pushTransform();
        Matrix matrix = view.getTransformation().getMatrix();
        float width = getWidth();
        float height = getHeight();
        switch (mOrientation) {
            case DOWN:
                matrix.preRotate(180, width / 2, height / 2);
                break;
            case LEFT:
                matrix.preRotate(270, height / 2, height / 2);
                break;
            case RIGHT:
                matrix.preRotate(90, width / 2, width / 2);
                break;
        }
        mChild.render(view, gl);
        view.popTransform();
    }

    @Override
    protected boolean dispatchTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        float width = getWidth();
        float height = getHeight();
        switch (mOrientation) {
            case DOWN: event.setLocation(width - x, height - y); break;
            case LEFT: event.setLocation(height - y, x); break;
            case RIGHT: event.setLocation(y, width - x); break;
        }
        boolean result = mChild.dispatchTouchEvent(event);
        event.setLocation(x, y);
        return result;
    }

    public void setOrientation(int orientation) {
        if (mOrientation == orientation) return;
        mOrientation = orientation;
        requestLayout();
    }

    public void setContent(GLView view) {
        if (mChild == view) return;

        if (mChild != null) super.clearComponents();
        mChild = view;
        if (view != null) super.addComponent(view);
        requestLayout();
    }

    @Override
    public void addComponent(GLView view) {
        throw new UnsupportedOperationException("use setContent(GLView)");
    }

    @Override
    public void clearComponents() {
        throw new UnsupportedOperationException("use setContent(null)");
    }
}
