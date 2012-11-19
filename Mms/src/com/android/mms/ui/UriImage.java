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
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2008 Esmertec AG.
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

package com.android.mms.ui;

import com.android.mms.model.ImageModel;
import com.android.mms.LogTag;
import com.google.android.mms.pdu.PduPart;
import android.database.sqlite.SqliteWrapper;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.Telephony.Mms.Part;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import android.media.ExifInterface;
import android.drm.DrmManagerClient;
import com.mediatek.xlog.Xlog;

public class UriImage {
    private static final String TAG = "Mms/image";
    private static final boolean DEBUG = true;
    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;

    private final Context mContext;
    private final Uri mUri;
    private String mContentType;
    private String mPath;
    private String mSrc;
    private int mWidth;
    private int mHeight;

    public UriImage(Context context, Uri uri) {
        if ((null == context) || (null == uri)) {
            throw new IllegalArgumentException();
        }

        String scheme = uri.getScheme();
        if (scheme.equals("content")) {
            initFromContentUri(context, uri);
        } else if (uri.getScheme().equals("file")) {
            initFromFile(context, uri);
        }

        mSrc = mPath.substring(mPath.lastIndexOf('/') + 1);

        if(mSrc.startsWith(".") && mSrc.length() > 1) {
            mSrc = mSrc.substring(1);
        }

        // Some MMSCs appear to have problems with filenames
        // containing a space.  So just replace them with
        // underscores in the name, which is typically not
        // visible to the user anyway.
        mSrc = mSrc.replace(' ', '_');
        Xlog.i(TAG, "ImageModel got mSrc: " + mSrc);
        mContext = context;
        mUri = uri;

        decodeBoundsInfo();
    }

    private void initFromFile(Context context, Uri uri) {
        mPath = uri.getPath();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String extension = MimeTypeMap.getFileExtensionFromUrl(mPath).toLowerCase();
        if (TextUtils.isEmpty(extension)) {
            // getMimeTypeFromExtension() doesn't handle spaces in filenames nor can it handle
            // urlEncoded strings. Let's try one last time at finding the extension.
            int dotPos = mPath.lastIndexOf('.');
            if (0 <= dotPos) {
                extension = mPath.substring(dotPos + 1);
                extension = extension.toLowerCase();
            }
        }
        Xlog.i(TAG, "ImageModel got file path: " + mPath);
        Xlog.i(TAG, "ImageModel got file extension: " + extension);
        mContentType = mimeTypeMap.getMimeTypeFromExtension(extension);
        if (mContentType == null && extension.equals("dcf")) {
        	DrmManagerClient drmManager= new DrmManagerClient(mContext);
        	mContentType = drmManager.getOriginalMimeType(mPath);
        	Xlog.i(TAG, "ImageModel got drm content, mContentType: " + mContentType);
        }
        Xlog.i(TAG, "ImageModel got mContentType: " + mContentType);
        // It's ok if mContentType is null. Eventually we'll show a toast telling the
        // user the picture couldn't be attached.
    }

    private void initFromContentUri(Context context, Uri uri) {
        Cursor c = SqliteWrapper.query(context, context.getContentResolver(),
                            uri, null, null, null, null);

        if (c == null) {
            throw new IllegalArgumentException(
                    "Query on " + uri + " returns null result.");
        }

        try {
            if ((c.getCount() != 1) || !c.moveToFirst()) {
                throw new IllegalArgumentException(
                        "Query on " + uri + " returns 0 or multiple rows.");
            }

            String filePath;
            if (ImageModel.isMmsUri(uri)) {
                filePath = c.getString(c.getColumnIndexOrThrow(Part.FILENAME));
                if (TextUtils.isEmpty(filePath)) {
                    filePath = c.getString(
                            c.getColumnIndexOrThrow(Part._DATA));
                }
                mContentType = c.getString(
                        c.getColumnIndexOrThrow(Part.CONTENT_TYPE));
            } else {
                filePath = uri.getPath();
                mContentType = c.getString(
                        c.getColumnIndexOrThrow(Images.Media.MIME_TYPE));
            }
            mPath = filePath;
            Xlog.i(TAG, "ImageModel got file path: " + mPath);
            Xlog.i(TAG, "ImageModel got mContentType: " + mContentType);
        } finally {
            c.close();
        }
    }

    private void decodeBoundsInfo() {
        InputStream input = null;
        try {
            input = mContext.getContentResolver().openInputStream(mUri);
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, opt);
            mWidth = opt.outWidth;
            mHeight = opt.outHeight;
        } catch (FileNotFoundException e) {
            // Ignore
            Log.e(TAG, "FileNotFoundException caught while opening stream", e);
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    // Ignore
                    Log.e(TAG, "IOException caught while closing stream", e);
                }
            }
        }
    }

    public String getContentType() {
        return mContentType;
    }

    public String getSrc() {
        return mSrc;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public PduPart getResizedImageAsPart(int widthLimit, int heightLimit, int byteLimit) {
        PduPart part = new PduPart();

        byte[] data = getResizedImageData(widthLimit, heightLimit, byteLimit);
        if (data == null) {
            if (LOCAL_LOGV) {
                Log.v(TAG, "Resize image failed.");
            }
            return null;
        }

        part.setData(data);
        part.setContentType(getContentType().getBytes());

        return part;
    }

    private static final int NUMBER_OF_RESIZE_ATTEMPTS = 4;

    private byte[] getResizedImageData(int widthLimit, int heightLimit, int byteLimit) {
        int outWidth = mWidth;
        int outHeight = mHeight;

        int scaleFactor = 1;
        while ((outWidth / scaleFactor > widthLimit) || (outHeight / scaleFactor > heightLimit)) {
            scaleFactor *= 2;
        }

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            Log.v(TAG, "getResizedImageData: wlimit=" + widthLimit +
                    ", hlimit=" + heightLimit + ", sizeLimit=" + byteLimit +
                    ", mWidth=" + mWidth + ", mHeight=" + mHeight +
                    ", initialScaleFactor=" + scaleFactor);
        }

        InputStream input = null;
        InputStream inputForRotate = null;
        ByteArrayOutputStream os = null;
        try {
            int attempts = 1;

            do {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = scaleFactor;
                input = mContext.getContentResolver().openInputStream(mUri);
                // Don't know why need two copy of inputStream if we use ExifInterface, 
                // this is only for getting rotation degree
                inputForRotate = mContext.getContentResolver().openInputStream(mUri);
                int orientation = 0;
                int degree = 0;
                try {
                	if (inputForRotate != null) {
                		ExifInterface exif = new ExifInterface(inputForRotate);
                		if (exif != null) {
                            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
                            degree = getExifRotation(orientation);
                        }
                	}   
                } catch (IOException e) {
                	Xlog.e(TAG, e.getMessage(), e);
                } finally {
                    if (inputForRotate != null) {
                        try {
                            inputForRotate.close();
                        } catch (IOException e) {
                            Xlog.e(TAG, e.getMessage(), e);
                        }
                    }
                }
                
                Xlog.i(TAG, "image rotation is" + degree + " degree");
                int quality = MessageUtils.IMAGE_COMPRESSION_QUALITY;
                try {
                    Bitmap b = BitmapFactory.decodeStream(input, null, options);
                    if (b == null) {
                        return null;
                    }
                    // rotate it to final rotation.
                    b = rotate(b, degree);
                    if (options.outWidth > widthLimit || options.outHeight > heightLimit) {
                        // The decoder does not support the inSampleSize option.
                        // Scale the bitmap using Bitmap library.
                        int scaledWidth = outWidth / scaleFactor;
                        int scaledHeight = outHeight / scaleFactor;

                        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                            Log.v(TAG, "getResizedImageData: retry scaling using " +
                                    "Bitmap.createScaledBitmap: w=" + scaledWidth +
                                    ", h=" + scaledHeight);
                        }

                        b = Bitmap.createScaledBitmap(b, outWidth / scaleFactor,
                                outHeight / scaleFactor, false);
                        if (b == null) {
                            return null;
                        }
                    }

                    // Compress the image into a JPG. Start with MessageUtils.IMAGE_COMPRESSION_QUALITY.
                    // In case that the image byte size is still too large reduce the quality in
                    // proportion to the desired byte size. Should the quality fall below
                    // MINIMUM_IMAGE_COMPRESSION_QUALITY skip a compression attempt and we will enter
                    // the next round with a smaller image to start with.
                    os = new ByteArrayOutputStream();
                    b.compress(CompressFormat.JPEG, quality, os);
                    int jpgFileSize = os.size();
                    if (jpgFileSize > byteLimit) {
                        int reducedQuality = quality * byteLimit / jpgFileSize;
                        if (reducedQuality >= MessageUtils.MINIMUM_IMAGE_COMPRESSION_QUALITY) {
                            quality = reducedQuality;

                            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                                Log.v(TAG, "getResizedImageData: compress(2) w/ quality=" + quality);
                            }

                            os = new ByteArrayOutputStream();
                            b.compress(CompressFormat.JPEG, quality, os);
                        }
                    }
                    b.recycle();        // done with the bitmap, release the memory
                } catch (java.lang.OutOfMemoryError e) {
                    Log.w(TAG, "getResizedImageData - image too big (OutOfMemoryError), will try "
                            + " with smaller scale factor, cur scale factor: " + scaleFactor);
                    // fall through and keep trying with a smaller scale factor.
                }
                if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                    Log.v(TAG, "attempt=" + attempts
                            + " size=" + (os == null ? 0 : os.size())
                            + " width=" + outWidth / scaleFactor
                            + " height=" + outHeight / scaleFactor
                            + " scaleFactor=" + scaleFactor
                            + " quality=" + quality);
                }
                scaleFactor *= 2;
                attempts++;
            } while ((os == null || os.size() > byteLimit) && attempts < NUMBER_OF_RESIZE_ATTEMPTS);
            // update the content type of image to "jpeg" ,because of the compression
            mContentType = "image/jpeg";
            return os == null ? null : os.toByteArray();
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        } catch (java.lang.OutOfMemoryError e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Xlog.e(TAG, e.getMessage(), e);
                }
            }
        }
    }

    /**
     * corresponding orientation of EXIF to degrees.
     */
    private int getExifRotation(int orientation) {
        int degrees = 0;
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                degrees = 0;
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                degrees = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                degrees = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                degrees = 270;
                break;
        }
        return degrees;
    }
    
    // Rotates the bitmap by the specified degree.
    // If a new bitmap is created, the original bitmap is recycled.
    public static Bitmap rotate(Bitmap b, int degrees) {
        if (degrees != 0 && b != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees,
                    (float) b.getWidth() / 2, (float) b.getHeight() / 2);
            try {
                Bitmap b2 = Bitmap.createBitmap(
                        b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (b != b2) {
                    b.recycle();
                    b = b2;
                }
            } catch (OutOfMemoryError ex) {
                // We have no memory to rotate. Return the original bitmap.
            }
        }
        return b;
    }
}
