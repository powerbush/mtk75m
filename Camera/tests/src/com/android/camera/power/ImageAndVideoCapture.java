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

package com.android.camera.power;

import com.android.camera.Camera;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;
import android.view.KeyEvent;
import android.content.Intent;
/**
 * Junit / Instrumentation test case for camera power measurement
 *
 * Running the test suite:
 *
 * adb shell am instrument \
 *    -e com.android.camera.power.ImageAndVideoCapture \
 *    -w com.android.camera.tests/android.test.InstrumentationTestRunner
 *
 */

public class ImageAndVideoCapture extends ActivityInstrumentationTestCase2 <Camera> {
    private String TAG = "ImageAndVideoCapture";
    private static final int TOTAL_NUMBER_OF_IMAGECAPTURE = 5;
    private static final int TOTAL_NUMBER_OF_VIDEOCAPTURE = 5;
    private static final long WAIT_FOR_IMAGE_CAPTURE_TO_BE_TAKEN = 1500;   //1.5 sedconds
    private static final long WAIT_FOR_VIDEO_CAPTURE_TO_BE_TAKEN = 10000; //10 seconds
    private static final long WAIT_FOR_PREVIEW = 1500; //1.5 seconds
    private static final long WAIT_FOR_STABLE_STATE = 2000; //2 seconds

    public ImageAndVideoCapture() {
        super("com.google.android.camera", Camera.class);
    }

    @Override
    protected void setUp() throws Exception {
        getActivity();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @LargeTest
    public void testLaunchCamera() {
        // This test case capture the baseline for the image preview.
        try {
            Thread.sleep(WAIT_FOR_STABLE_STATE);
        } catch (Exception e) {
            Log.v(TAG, e.toString());
            assertTrue("testImageCaptureDoNothing", false);
        }
        assertTrue("testImageCaptureDoNothing", true);
    }

    @LargeTest
    public void testCapture5Image() {
        // This test case will use the default camera setting
        Instrumentation inst = getInstrumentation();
        try {
            for (int i = 0; i < TOTAL_NUMBER_OF_IMAGECAPTURE; i++) {
                Thread.sleep(WAIT_FOR_IMAGE_CAPTURE_TO_BE_TAKEN);
                inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP);
                inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
                Thread.sleep(WAIT_FOR_IMAGE_CAPTURE_TO_BE_TAKEN);
            }
            Thread.sleep(WAIT_FOR_STABLE_STATE);
        } catch (Exception e) {
            Log.v(TAG, e.toString());
            assertTrue("testImageCapture", false);
        }
        assertTrue("testImageCapture", true);
    }

    @LargeTest
    public void testCapture5Videos() {
        // This test case will use the default camera setting
        Instrumentation inst = getInstrumentation();
        try {
            // Switch to the video mode
            Intent intent = new Intent();
            intent.setClassName("com.google.android.camera",
                    "com.android.camera.VideoCamera");
            getActivity().startActivity(intent);
            for (int i = 0; i < TOTAL_NUMBER_OF_VIDEOCAPTURE; i++) {
                Thread.sleep(WAIT_FOR_PREVIEW);
                // record a video
                inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
                Thread.sleep(WAIT_FOR_VIDEO_CAPTURE_TO_BE_TAKEN);
                inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
                Thread.sleep(WAIT_FOR_PREVIEW);
            }
            Thread.sleep(WAIT_FOR_STABLE_STATE);
        } catch (Exception e) {
            Log.v(TAG, e.toString());
            assertTrue("testVideoCapture", false);
        }
        assertTrue("testVideoCapture", true);
    }
}
