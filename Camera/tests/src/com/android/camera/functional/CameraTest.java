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

package com.android.camera.functional;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Process;
import android.provider.MediaStore;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class CameraTest extends InstrumentationTestCase {
    private static final String TAG = "CameraTest";
    private static final String CAMERA_PACKAGE = "com.google.android.camera";
    private static final String CAMERA_ACTIVITY = "com.android.camera.Camera";
    private static final String CAMCORDER_ACTIVITY = "com.android.camera.VideoCamera";

    @LargeTest
    public void testVideoCaptureIntentFdLeak() throws Exception {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.setClassName(CAMERA_PACKAGE, CAMCORDER_ACTIVITY);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.parse("file://"
                + Environment.getExternalStorageDirectory().toString()
                + "test_fd_leak.3gp"));
        getInstrumentation().startActivitySync(intent).finish();
        // Test if the fd is closed.
        for (File f: new File("/proc/" + Process.myPid() + "/fd").listFiles()) {
            assertEquals(-1, f.getCanonicalPath().indexOf("test_fd_leak.3gp"));
        }
    }

    @LargeTest
    public void testActivityLeak() throws Exception {
        checkActivityLeak(CAMERA_ACTIVITY);
        checkActivityLeak(CAMCORDER_ACTIVITY);
    }

    private void checkActivityLeak(String activityName) throws Exception {
        final int TEST_COUNT = 5;
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(CAMERA_PACKAGE, activityName);
        ArrayList<WeakReference<Activity>> refs =
                new ArrayList<WeakReference<Activity>>();
        for (int i = 0; i < TEST_COUNT; i++) {
            Activity activity = getInstrumentation().startActivitySync(intent);
            refs.add(new WeakReference<Activity>(activity));
            activity.finish();
            getInstrumentation().waitForIdleSync();
            activity = null;
        }
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        int refCount = 0;
        for (WeakReference<Activity> c: refs) {
            if (c.get() != null) refCount++;
        }
        // If applications are leaking activity, every reference is reachable.
        assertTrue(refCount != TEST_COUNT);
    }
}
