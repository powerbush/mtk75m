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

package com.android.camera.stress;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Debug;
import android.os.Environment;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import java.io.FileWriter;
import java.io.BufferedWriter;

/**
 * Test cases to measure the camera and video recorder startup time.
 */
public class CameraStartUp extends InstrumentationTestCase {

    private static final int TOTAL_NUMBER_OF_STARTUP = 20;

    private String TAG = "CameraStartUp";
    private static final String CAMERA_TEST_OUTPUT_FILE =
            Environment.getExternalStorageDirectory().toString() + "/mediaStressOut.txt";
    private static final String CAMERA_PACKAGE_NAME = "com.google.android.camera";
    private static final String CAMERA_ACTIVITY_NAME = "com.android.camera.Camera";
    private static final String VIDEORECORDER_ACTIVITY_NAME = "com.android.camera.VideoCamera";
    private static int WAIT_TIME_FOR_PREVIEW = 1500; //1.5 second

    private long launchCamera() {
        long startupTime = 0;
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(CAMERA_PACKAGE_NAME, CAMERA_ACTIVITY_NAME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            long beforeStart = System.currentTimeMillis();
            Instrumentation inst = getInstrumentation();
            Activity cameraActivity = inst.startActivitySync(intent);
            long cameraStarted = System.currentTimeMillis();
            cameraActivity.finish();
            startupTime = cameraStarted - beforeStart;
            Thread.sleep(1000);
            Log.v(TAG, "camera startup time: " + startupTime);
        } catch (Exception e) {
            Log.v(TAG, e.toString());
            fail("Fails to get the output file");
        }
        return startupTime;
    }

    private long launchVideo() {
        long startupTime = 0;

        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(CAMERA_PACKAGE_NAME, VIDEORECORDER_ACTIVITY_NAME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            long beforeStart = System.currentTimeMillis();
            Instrumentation inst = getInstrumentation();
            Activity recorderActivity = inst.startActivitySync(intent);
            long cameraStarted = System.currentTimeMillis();
            recorderActivity.finish();
            startupTime = cameraStarted - beforeStart;
            Log.v(TAG, "Video Startup Time = " + startupTime);
            // wait for 1s to make sure it reach a clean stage
            Thread.sleep(WAIT_TIME_FOR_PREVIEW);
            Log.v(TAG, "video startup time: " + startupTime);
        } catch (Exception e) {
            Log.v(TAG, e.toString());
            fail("Fails to launch video output file");
        }
        return startupTime;
    }

    private void writeToOutputFile(long totalStartupTime,
            String individualStartupTime, boolean firstStartUp, String Type) throws Exception {
        // TODO (yslau) : Need to integrate the output data with central
        // dashboard
        try {
            FileWriter fstream = null;
            fstream = new FileWriter(CAMERA_TEST_OUTPUT_FILE, true);
            BufferedWriter out = new BufferedWriter(fstream);
            if (firstStartUp) {
                out.write("First " + Type + " Startup: " + totalStartupTime + "\n");
            } else {
                long averageStartupTime = totalStartupTime / (TOTAL_NUMBER_OF_STARTUP -1);
                out.write(Type + "startup time: " + "\n");
                out.write("Number of loop: " + (TOTAL_NUMBER_OF_STARTUP -1)  + "\n");
                out.write(individualStartupTime + "\n\n");
                out.write(Type + " average startup time: " + averageStartupTime + " ms\n\n");
            }
            out.close();
            fstream.close();
        } catch (Exception e) {
            fail("Camera write output to file");
        }
    }

    @LargeTest
    public void testLaunchVideo() throws Exception {
        String individualStartupTime;
        individualStartupTime = "Individual Video Startup Time = ";
        long totalStartupTime = 0;
        long startupTime = 0;
        for (int i = 0; i < TOTAL_NUMBER_OF_STARTUP; i++) {
            if (i == 0) {
                // Capture the first startup time individually
                long firstStartUpTime = launchVideo();
                writeToOutputFile(firstStartUpTime, "na", true, "Video");
            } else {
                startupTime = launchVideo();
                totalStartupTime += startupTime;
                individualStartupTime += startupTime + " ,";
            }
        }
        Log.v(TAG, "totalStartupTime =" + totalStartupTime);
        writeToOutputFile(totalStartupTime, individualStartupTime, false, "Video");
    }

    @LargeTest
    public void testLaunchCamera() throws Exception {
        String individualStartupTime;
        individualStartupTime = "Individual Camera Startup Time = ";
        long totalStartupTime = 0;
        long startupTime = 0;
        for (int i = 0; i < TOTAL_NUMBER_OF_STARTUP; i++) {
            if (i == 0) {
                // Capture the first startup time individually
                long firstStartUpTime = launchCamera();
                writeToOutputFile(firstStartUpTime, "na", true, "Camera");
            } else {
                startupTime = launchCamera();
                totalStartupTime += startupTime;
                individualStartupTime += startupTime + " ,";
            }
        }
        Log.v(TAG, "totalStartupTime =" + totalStartupTime);
        writeToOutputFile(totalStartupTime,
                individualStartupTime, false, "Camera");
    }
}