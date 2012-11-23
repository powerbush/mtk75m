/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

package com.android.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View.MeasureSpec;
import android.view.View;
import android.view.ViewGroup;

/**
 * Create a 4x3 grid of dial buttons.
 *
 * It was easier and more efficient to do it this way than use
 * standard layouts. It's perfectly fine (and actually encouraged) to
 * use custom layouts rather than piling up standard layouts.
 *
 * The horizontal and vertical spacings between buttons are controlled
 * by the amount of padding (attributes on the ButtonGridLayout element):
 *   - horizontal = left + right padding and
 *   - vertical = top + bottom padding.
 *
 * This class assumes that all the buttons have the same size.
 * The buttons will be bottom aligned in their view on layout.
 *
 * Invocation: onMeasure is called first by the framework to know our
 * size. Then onLayout is invoked to layout the buttons.
 */
// TODO: Blindly layout the buttons w/o checking if we overrun the
// bottom-right corner.
// TODO: This class and the one in the Contacts app are almost
// duplicates. The Contacts instance does not have a
// setChildrenBackground method.

public class ButtonGridLayout extends ViewGroup {
    static private final String TAG = "ButtonGridLayout";
    static private final int COLUMNS = 3;
    static private final int ROWS = 4;
    static private final int NUM_CHILDREN = ROWS * COLUMNS;

    private View[] mButtons = new View[NUM_CHILDREN];

    // This what the fields represent (height is similar):
    // PL: mPaddingLeft
    // BW: mButtonWidth
    // PR: mPaddingRight
    //
    //        mWidthInc
    // <-------------------->
    //   PL      BW      PR
    // <----><--------><---->
    //        --------
    //       |        |
    //       | button |
    //       |        |
    //        --------
    //
    // We assume mPaddingLeft == mPaddingRight == 1/2 padding between
    // buttons.
    //
    // mWidth == COLUMNS x mWidthInc

    // Width and height of a button
    private int mButtonWidth;
    private int mButtonHeight;

    // Width and height of a button + padding.
    private int mWidthInc;
    private int mHeightInc;

    // Height of the dialpad. Used to align it at the bottom of the
    // view.
    private int mWidth;
    private int mHeight;


    public ButtonGridLayout(Context context) {
        super(context);
    }

    public ButtonGridLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ButtonGridLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Cache the buttons in a member array for faster access.  Compute
     * the measurements for the width/height of buttons.  The inflate
     * sequence is called right after the constructor and before the
     * measure/layout phase.
     */
    @Override
    protected void onFinishInflate () {
        super.onFinishInflate();
        final View[] buttons = mButtons;
        for (int i = 0; i < NUM_CHILDREN; i++) {
            buttons[i] = getChildAt(i);
            // Measure the button to get initialized.
            buttons[i].measure(MeasureSpec.UNSPECIFIED , MeasureSpec.UNSPECIFIED);
        }

        // Cache the measurements.
        final View child = buttons[0];
        mButtonWidth = child.getMeasuredWidth();
        mButtonHeight = child.getMeasuredHeight();
        mWidthInc = mButtonWidth + mPaddingLeft + mPaddingRight;
        mHeightInc = mButtonHeight + mPaddingTop + mPaddingBottom;
        mWidth = COLUMNS * mWidthInc;
        mHeight = ROWS * mHeightInc;
    }

    /**
     * Set the background of all the children. Typically a selector to
     * change the background based on some combination of the button's
     * attributes (e.g pressed, enabled...)
     * @param resid Is a resource id to be used for each button's background.
     */
    public void setChildrenBackgroundResource(int resid) {
        final View[] buttons = mButtons;
        for (int i = 0; i < NUM_CHILDREN; i++) {
            buttons[i].setBackgroundResource(resid);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final View[] buttons = mButtons;
        final int paddingLeft = mPaddingLeft;
        final int buttonWidth = mButtonWidth;
        final int buttonHeight = mButtonHeight;
        final int widthInc = mWidthInc;
        final int heightInc = mHeightInc;

        int i = 0;
        // The last row is bottom aligned.
        int y = (bottom - top) - mHeight + mPaddingTop;
        for (int row = 0; row < ROWS; row++) {
            int x = paddingLeft;
            for (int col = 0; col < COLUMNS; col++) {
                buttons[i].layout(x, y, x + buttonWidth, y + buttonHeight);
                x += widthInc;
                i++;
            }
            y += heightInc;
        }
    }

    /**
     * This method is called twice in practice. The first time both
     * with and height are constraint by AT_MOST. The second time, the
     * width is still AT_MOST and the height is EXACTLY. Either way
     * the full width/height should be in mWidth and mHeight and we
     * use 'resolveSize' to do the right thing.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int width = resolveSize(mWidth, widthMeasureSpec);
        final int height = resolveSize(mHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }
}
