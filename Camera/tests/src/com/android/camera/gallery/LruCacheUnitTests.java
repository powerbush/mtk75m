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

package com.android.camera.gallery;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

public class LruCacheUnitTests extends AndroidTestCase {

    @SmallTest
    public void testPut() {
        LruCache<Integer, Integer> cache = new LruCache<Integer, Integer>(2);
        Integer key = Integer.valueOf(1);
        Integer value = Integer.valueOf(3);
        cache.put(key, value);
        assertEquals(value, cache.get(key));
    }

    @SmallTest
    public void testTracingInUsedObject() {
        LruCache<Integer, Integer> cache = new LruCache<Integer, Integer>(2);
        Integer key = Integer.valueOf(1);
        Integer value = new Integer(3);
        cache.put(key, value);
        for (int i = 0; i < 3; ++i) {
            cache.put(i + 10, i * i);
        }
        System.gc();
        assertEquals(value, cache.get(key));
    }

    @SmallTest
    public void testLruAlgorithm() {
        LruCache<Integer, Integer> cache = new LruCache<Integer, Integer>(2);
        cache.put(0, new Integer(0));
        for (int i = 0; i < 3; ++i) {
            cache.put(i + 1, i * i);
            cache.get(0);
        }
        System.gc();
        assertEquals(Integer.valueOf(0), cache.get(0));
    }

    private static final int TEST_COUNT = 10000;

    static class Accessor extends Thread {
        private final LruCache<Integer,Integer> mMap;

        public Accessor(LruCache<Integer, Integer> map) {
            mMap = map;
        }

        @Override
        public void run() {
            Log.v("TAG", "start get " + this);
            for (int i = 0; i < TEST_COUNT; ++i) {
                mMap.get(i % 2);
            }
            Log.v("TAG", "finish get " + this);
        }
    }

    @SuppressWarnings("unchecked")
    public void testConcurrentAccess() throws Exception {
        LruCache<Integer, Integer> cache = new LruCache<Integer, Integer>(4);
        cache.put(0, 0);
        cache.put(1, 1);
        Accessor accessor[] = new Accessor[4];
        for (int i = 0; i < accessor.length; ++i) {
            accessor[i] = new Accessor(cache);
        }
        for (int i = 0; i < accessor.length; ++i) {
            accessor[i].start();
        }
        for (int i = 0; i < accessor.length; ++i) {
            accessor[i].join();
        }
    }
}
