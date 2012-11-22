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

package com.android.camera.gallery;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Images.Media;

/**
 * Represents an ordered collection of Image objects. Provides an API to add
 * and remove an image.
 */
public class ImageList extends BaseImageList implements IImageList {

    @SuppressWarnings("unused")
    private static final String TAG = "ImageList";

    private static final String[] ACCEPTABLE_IMAGE_TYPES =
            new String[] { "image/jpeg", "image/png", "image/gif" };

    /**
     * ImageList constructor.
     */
    public ImageList(ContentResolver resolver, Uri imageUri,
            int sort, String bucketId) {
        super(resolver, imageUri, sort, bucketId);
    }

    private static final String WHERE_CLAUSE =
            "(" + Media.MIME_TYPE + " in (?, ?, ?))";
    private static final String WHERE_CLAUSE_WITH_BUCKET_ID =
            WHERE_CLAUSE + " AND " + Media.BUCKET_ID + " = ?";

    protected String whereClause() {
        return mBucketId == null ? WHERE_CLAUSE : WHERE_CLAUSE_WITH_BUCKET_ID;
    }

    protected String[] whereClauseArgs() {
        // TODO: Since mBucketId won't change, we should keep the array.
        if (mBucketId != null) {
            int count = ACCEPTABLE_IMAGE_TYPES.length;
            String[] result = new String[count + 1];
            System.arraycopy(ACCEPTABLE_IMAGE_TYPES, 0, result, 0, count);
            result[count] = mBucketId;
            return result;
        }
        return ACCEPTABLE_IMAGE_TYPES;
    }

    @Override
    protected Cursor createCursor() {
        Cursor c = Media.query(
                mContentResolver, mBaseUri, IMAGE_PROJECTION,
                whereClause(), whereClauseArgs(), sortOrder());
        return c;
    }

    static final String[] IMAGE_PROJECTION = new String[] {
            Media._ID,
            Media.DATE_TAKEN,
            Media.MINI_THUMB_MAGIC,
            Media.ORIENTATION,
            Media.DATE_MODIFIED};

    private static final int INDEX_ID = 0;
    private static final int INDEX_DATE_TAKEN = 1;
    private static final int INDEX_MINI_THUMB_MAGIC = 2;
    private static final int INDEX_ORIENTATION = 3;
    private static final int INDEX_DATE_MODIFIED = 4;

    @Override
    protected BaseImage loadImageFromCursor(Cursor cursor) {
        long id = cursor.getLong(INDEX_ID);
        long dateTaken = cursor.getLong(INDEX_DATE_TAKEN);
        if (dateTaken == 0) {
            dateTaken = cursor.getLong(INDEX_DATE_MODIFIED) * 1000;
        }
        long miniThumbMagic = cursor.getLong(INDEX_MINI_THUMB_MAGIC);
        int orientation = cursor.getInt(INDEX_ORIENTATION);
        return new Image(mContentResolver, id,
                contentUri(id), miniThumbMagic, dateTaken,
                orientation);
    }
}
