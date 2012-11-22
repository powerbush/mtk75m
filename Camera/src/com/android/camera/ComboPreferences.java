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

package com.android.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ComboPreferences implements SharedPreferences, OnSharedPreferenceChangeListener {
    private SharedPreferences mPrefGlobal;  // global preferences
    private SharedPreferences mPrefLocal;  // per-camera preferences
    private CopyOnWriteArrayList<OnSharedPreferenceChangeListener> mListeners;
    private static WeakHashMap<Context, ComboPreferences> sMap =
            new WeakHashMap<Context, ComboPreferences>();

    public ComboPreferences(Context context) {
        mPrefGlobal = PreferenceManager.getDefaultSharedPreferences(context);
        mPrefGlobal.registerOnSharedPreferenceChangeListener(this);
        synchronized (sMap) {
            sMap.put(context, this);
        }
        mListeners = new CopyOnWriteArrayList<OnSharedPreferenceChangeListener>();
    }

    public static ComboPreferences get(Context context) {
        synchronized (sMap) {
            return sMap.get(context);
        }
    }

    public void setLocalId(Context context, int cameraId) {
        String prefName = context.getPackageName() + "_preferences_" + cameraId;
        if (mPrefLocal != null) {
            mPrefLocal.unregisterOnSharedPreferenceChangeListener(this);
        }
        mPrefLocal = context.getSharedPreferences(
                prefName, Context.MODE_PRIVATE);
        mPrefLocal.registerOnSharedPreferenceChangeListener(this);
    }

    public SharedPreferences getGlobal() {
        return mPrefGlobal;
    }

    public SharedPreferences getLocal() {
        return mPrefLocal;
    }

    public Map<String, ?> getAll() {
        throw new UnsupportedOperationException(); // Can be implemented if needed.
    }

    private static boolean isGlobal(String key) {
        return key.equals(CameraSettings.KEY_CAMERA_ID)
                || key.equals(CameraSettings.KEY_RECORD_LOCATION);
    }

    public String getString(String key, String defValue) {
        if (isGlobal(key) || !mPrefLocal.contains(key)) {
            return mPrefGlobal.getString(key, defValue);
        } else {
            return mPrefLocal.getString(key, defValue);
        }
    }

    public int getInt(String key, int defValue) {
        if (isGlobal(key) || !mPrefLocal.contains(key)) {
            return mPrefGlobal.getInt(key, defValue);
        } else {
            return mPrefLocal.getInt(key, defValue);
        }
    }

    public long getLong(String key, long defValue) {
        if (isGlobal(key) || !mPrefLocal.contains(key)) {
            return mPrefGlobal.getLong(key, defValue);
        } else {
            return mPrefLocal.getLong(key, defValue);
        }
    }

    public float getFloat(String key, float defValue) {
        if (isGlobal(key) || !mPrefLocal.contains(key)) {
            return mPrefGlobal.getFloat(key, defValue);
        } else {
            return mPrefLocal.getFloat(key, defValue);
        }
    }

    public boolean getBoolean(String key, boolean defValue) {
        if (isGlobal(key) || !mPrefLocal.contains(key)) {
            return mPrefGlobal.getBoolean(key, defValue);
        } else {
            return mPrefLocal.getBoolean(key, defValue);
        }
    }

    // This method is not used.
    public Set<String> getStringSet(String key, Set<String> defValues) {
        throw new UnsupportedOperationException();
    }

    public boolean contains(String key) {
        if (mPrefLocal.contains(key)) return true;
        if (mPrefGlobal.contains(key)) return true;
        return false;
    }

    private class MyEditor implements Editor {
        private Editor mEditorGlobal;
        private Editor mEditorLocal;

        MyEditor() {
            mEditorGlobal = mPrefGlobal.edit();
            mEditorLocal = mPrefLocal.edit();
        }

        public boolean commit() {
            boolean result1 = mEditorGlobal.commit();
            boolean result2 = mEditorLocal.commit();
            return result1 && result2;
        }

        public void apply() {
            mEditorGlobal.apply();
            mEditorLocal.apply();
        }

        // Note: clear() and remove() affects both local and global preferences.
        public Editor clear() {
            mEditorGlobal.clear();
            mEditorLocal.clear();
            return this;
        }

        public Editor remove(String key) {
            mEditorGlobal.remove(key);
            mEditorLocal.remove(key);
            return this;
        }

        public Editor putString(String key, String value) {
            if (isGlobal(key)) {
                mEditorGlobal.putString(key, value);
            } else {
                mEditorLocal.putString(key, value);
            }
            return this;
        }

        public Editor putInt(String key, int value) {
            if (isGlobal(key)) {
                mEditorGlobal.putInt(key, value);
            } else {
                mEditorLocal.putInt(key, value);
            }
            return this;
        }

        public Editor putLong(String key, long value) {
            if (isGlobal(key)) {
                mEditorGlobal.putLong(key, value);
            } else {
                mEditorLocal.putLong(key, value);
            }
            return this;
        }

        public Editor putFloat(String key, float value) {
            if (isGlobal(key)) {
                mEditorGlobal.putFloat(key, value);
            } else {
                mEditorLocal.putFloat(key, value);
            }
            return this;
        }

        public Editor putBoolean(String key, boolean value) {
            if (isGlobal(key)) {
                mEditorGlobal.putBoolean(key, value);
            } else {
                mEditorLocal.putBoolean(key, value);
            }
            return this;
        }

        // This method is not used.
        public Editor putStringSet(String key, Set<String> values) {
            throw new UnsupportedOperationException();
        }
    }

    // Note the remove() and clear() of the returned Editor may not work as
    // expected because it doesn't touch the global preferences at all.
    public Editor edit() {
        return new MyEditor();
    }

    public void registerOnSharedPreferenceChangeListener(
            OnSharedPreferenceChangeListener listener) {
        mListeners.add(listener);
    }

    public void unregisterOnSharedPreferenceChangeListener(
            OnSharedPreferenceChangeListener listener) {
        mListeners.remove(listener);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        for (OnSharedPreferenceChangeListener listener : mListeners) {
            listener.onSharedPreferenceChanged(this, key);
        }
    }
}
