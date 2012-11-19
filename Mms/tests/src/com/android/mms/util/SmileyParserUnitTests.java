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

package com.android.mms.util;

import java.nio.IntBuffer;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;

import com.android.internal.widget.Smileys;

/**
 * This is a series of unit tests for the SmileyParser class.
 * 
 * This is just unit tests of the SmileyParser - the activity is not instantiated
 */
@SmallTest
public class SmileyParserUnitTests extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        SmileyParser.init(getContext());
    }

    /**
     * Test of smiley strings.
     */
    public void testSmileyParser() {
        SmileyParser parser = SmileyParser.getInstance();
        SpannableStringBuilder buf = new SpannableStringBuilder();
        
        // Put a string that looks kind of like a smiley in between two valid smileys.
        buf.append(parser.addSmileySpans(":-):-:-("));

        ImageSpan[] spans = buf.getSpans(0, buf.length(), ImageSpan.class);

        assertTrue("Smiley (happy) bitmaps aren't equal",
                compareImageSpans(new ImageSpan(mContext,
                        Smileys.getSmileyResource(Smileys.HAPPY)), spans[0]));

        assertTrue("Smiley (sad) bitmaps aren't equal",
                compareImageSpans(new ImageSpan(mContext,
                        Smileys.getSmileyResource(Smileys.SAD)), spans[1]));
    }
    
    private boolean compareImageSpans(ImageSpan span1, ImageSpan span2) {
        BitmapDrawable bitmapDrawable1 = (BitmapDrawable)span1.getDrawable();
        BitmapDrawable bitmapDrawable2 = (BitmapDrawable)span2.getDrawable();
        Bitmap bitmap1 = bitmapDrawable1.getBitmap();
        Bitmap bitmap2 = bitmapDrawable2.getBitmap();

        int rowBytes1 = bitmap1.getRowBytes();
        int rowBytes2 = bitmap2.getRowBytes();
        if (rowBytes1 != rowBytes2) {
            return false;
        }
        int height1 = bitmap1.getHeight();
        int height2 = bitmap2.getHeight();
        if (height1 != height2) {
            return false;
        }
        int size = height1 * rowBytes1;
        int[] intArray1 = new int[size];
        int[] intArray2 = new int[size];
        IntBuffer intBuf1 = IntBuffer.wrap(intArray1);
        IntBuffer intBuf2 = IntBuffer.wrap(intArray2);

        bitmap1.copyPixelsToBuffer(intBuf1);
        bitmap2.copyPixelsToBuffer(intBuf2);
        
        for (int i = 0; i < size; i++) {
            if (intArray1[i] != intArray2[i]) {
                return false;
            }
        }
        return true;
    }

}
