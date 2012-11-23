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
 * Copyright (C) 2006 The Android Open Source Project
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

import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.view.Window;

/**
 * Profiling utilities for the Phone app.
 */
public class Profiler {
    private static final String LOG_TAG = PhoneApp.LOG_TAG;

    // Let the compiler optimize all this code out unless we're actively
    // doing profiling runs.
    // TODO: Instead of doing all these "if (PROFILE)" checks here, every
    // place that *calls* any of these methods should check the value of
    // Profiler.PROFILE first, so the method calls will get optimized out
    // too.
    private static final boolean PROFILE = false;

    static long sTimeCallScreenRequested;
    static long sTimeCallScreenOnCreate;
    static long sTimeCallScreenCreated;

    // TODO: Clean up any usage of these times.  (There's no "incoming call
    // panel" in the Phone UI any more; incoming calls just go straight to the
    // regular in-call UI.)
    static long sTimeIncomingCallPanelRequested;
    static long sTimeIncomingCallPanelOnCreate;
    static long sTimeIncomingCallPanelCreated;

    /** This class is never instantiated. */
    private Profiler() {
    }

    static void profileViewCreate(Window win, String tag) {
        if (false) {
            ViewParent p = (ViewParent) win.getDecorView();
            while (p instanceof View) {
                p = ((View) p).getParent();
            }
            //((ViewRoot)p).profile();
            //((ViewRoot)p).setProfileTag(tag);
        }
    }

    static void callScreenRequested() {
        if (PROFILE) {
            sTimeCallScreenRequested = SystemClock.uptimeMillis();
        }
    }

    static void callScreenOnCreate() {
        if (PROFILE) {
            sTimeCallScreenOnCreate = SystemClock.uptimeMillis();
        }
    }

    static void callScreenCreated() {
        if (PROFILE) {
            sTimeCallScreenCreated = SystemClock.uptimeMillis();
            dumpCallScreenStat();
        }
    }

    private static void dumpCallScreenStat() {
        if (PROFILE) {
            log(">>> call screen perf stats <<<");
            log(">>> request -> onCreate = " +
                    (sTimeCallScreenOnCreate - sTimeCallScreenRequested));
            log(">>> onCreate -> created = " +
                    (sTimeCallScreenCreated - sTimeCallScreenOnCreate));
        }
    }

    static void incomingCallPanelRequested() {
        if (PROFILE) {
            sTimeIncomingCallPanelRequested = SystemClock.uptimeMillis();
        }
    }

    static void incomingCallPanelOnCreate() {
        if (PROFILE) {
            sTimeIncomingCallPanelOnCreate = SystemClock.uptimeMillis();
        }
    }

    static void incomingCallPanelCreated() {
        if (PROFILE) {
            sTimeIncomingCallPanelCreated = SystemClock.uptimeMillis();
            dumpIncomingCallPanelStat();
        }
    }

    private static void dumpIncomingCallPanelStat() {
        if (PROFILE) {
            log(">>> incoming call panel perf stats <<<");
            log(">>> request -> onCreate = " +
                    (sTimeIncomingCallPanelOnCreate - sTimeIncomingCallPanelRequested));
            log(">>> onCreate -> created = " +
                    (sTimeIncomingCallPanelCreated - sTimeIncomingCallPanelOnCreate));
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, "[Profiler] " + msg);
    }
}
