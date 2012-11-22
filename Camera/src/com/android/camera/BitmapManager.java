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

package com.android.camera;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.util.Log;

import java.io.FileDescriptor;
import java.util.WeakHashMap;

/**
 * Provides utilities to decode bitmap, get thumbnail, and cancel the
 * operations.
 *
 * <p>The function {@link #decodeFileDescriptor(FileDescriptor,
 * BitmapFactory.Options)} is used to decode a bitmap. During decoding another
 * thread can cancel it using the function {@link #cancelThreadDecoding(Thread,
 * ContentResolver)} specifying the {@code Thread} which is in decoding.
 *
 * <p>{@code cancelThreadDecoding(Thread,ContentResolver)} is sticky until
 * {@code allowThreadDecoding(Thread) } is called.
 */
public class BitmapManager {
    private static final String TAG = "BitmapManager";
    private static enum State {CANCEL, ALLOW}
    private static class ThreadStatus {
        public State mState = State.ALLOW;
        public BitmapFactory.Options mOptions;

        @Override
        public String toString() {
            String s;
            if (mState == State.CANCEL) {
                s = "Cancel";
            } else if (mState == State.ALLOW) {
                s = "Allow";
            } else {
                s = "?";
            }
            s = "thread state = " + s + ", options = " + mOptions;
            return s;
        }
    }

    private final WeakHashMap<Thread, ThreadStatus> mThreadStatus =
            new WeakHashMap<Thread, ThreadStatus>();

    private static BitmapManager sManager = null;

    private BitmapManager() {
    }

    /**
     * Get thread status and create one if specified.
     */
    private synchronized ThreadStatus getOrCreateThreadStatus(Thread t) {
        ThreadStatus status = mThreadStatus.get(t);
        if (status == null) {
            status = new ThreadStatus();
            mThreadStatus.put(t, status);
        }
        return status;
    }

    public synchronized boolean canThreadDecoding(Thread t) {
        ThreadStatus status = mThreadStatus.get(t);
        if (status == null) {
            // allow decoding by default
            return true;
        }

        boolean result = (status.mState != State.CANCEL);
        return result;
    }

    /**
     * Gets the thumbnail of the given ID of the original image.
     *
     * <p> This method wraps around @{code getThumbnail} in {@code
     * android.provider.MediaStore}. It provides the ability to cancel it.
     */
    public Bitmap getThumbnail(ContentResolver cr, long origId, int kind,
            BitmapFactory.Options options, boolean isVideo) {
        Thread t = Thread.currentThread();
        ThreadStatus status = getOrCreateThreadStatus(t);

        if (!canThreadDecoding(t)) {
            Log.d(TAG, "Thread " + t + " is not allowed to decode.");
            return null;
        }

        try {
            if (isVideo) {
                return Video.Thumbnails.getThumbnail(cr, origId, t.getId(),
                        kind, null);
            } else {
                return Images.Thumbnails.getThumbnail(cr, origId, t.getId(),
                        kind, null);
            }
        } finally {
            synchronized (status) {
                status.notifyAll();
            }
        }
    }

    public static synchronized BitmapManager instance() {
        if (sManager == null) {
            sManager = new BitmapManager();
        }
        return sManager;
    }
}
