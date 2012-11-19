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

package com.android.mms.model;

import com.android.mms.ContentRestrictionException;
import com.android.mms.ExceedMessageSizeException;
import com.android.mms.LogTag;
import com.android.mms.MmsConfig;
import com.android.mms.RestrictedResolutionException;
import com.android.mms.data.WorkingMessage;
import com.android.mms.dom.smil.SmilMediaElementImpl;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.drm.mobile1.DrmException;
import com.android.mms.drm.DrmWrapper;
import com.android.mms.ui.UriImage;
import com.android.mms.ui.MessageUtils;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.PduPart;
import com.google.android.mms.pdu.PduPersister;

import org.w3c.dom.events.Event;
import org.w3c.dom.smil.ElementTime;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import android.drm.DrmManagerClient;
import com.mediatek.xlog.Xlog;

public class ImageModel extends RegionMediaModel {
    private static final String TAG = "Mms/image";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;

    private static final int THUMBNAIL_BOUNDS_LIMIT = 480;

    private int mWidth;
    private int mHeight;
    public boolean mHasDrmContent;
    public boolean mHasDrmRight;
    private SoftReference<Bitmap> mBitmapCache = new SoftReference<Bitmap>(null);

    public ImageModel(Context context, Uri uri, RegionModel region)
            throws MmsException {
        super(context, SmilHelper.ELEMENT_TAG_IMAGE, uri, region);
        initModelFromUri(uri);
        checkContentRestriction();
    }

    public ImageModel(Context context, String contentType, String src,
            Uri uri, RegionModel region) throws DrmException, MmsException {
        super(context, SmilHelper.ELEMENT_TAG_IMAGE,
                contentType, src, uri, region);
        decodeImageBounds();
    }

    public ImageModel(Context context, String contentType, String src,
            DrmWrapper wrapper, RegionModel regionModel) throws IOException {
        super(context, SmilHelper.ELEMENT_TAG_IMAGE, contentType, src,
                wrapper, regionModel);
    }

    private void initModelFromUri(Uri uri) throws MmsException {
    	try {
        UriImage uriImage = new UriImage(mContext, uri);
        mContentType = uriImage.getContentType();
        if (TextUtils.isEmpty(mContentType)) {
            throw new MmsException("Type of media is unknown.");
        }
        mSrc = uriImage.getSrc();
        mWidth = uriImage.getWidth();
        mHeight = uriImage.getHeight();
            if(mWidth > MmsConfig.getMaxRestrictedImageWidth() || mHeight > MmsConfig.getMaxRestrictedImageHeight()){
            	if (WorkingMessage.sCreationMode  == WorkingMessage.RESTRICTED_TYPE){
            		throw new RestrictedResolutionException("Restricted resolution:" + mWidth + "*" + mHeight);
            	} else if (WorkingMessage.sCreationMode  == WorkingMessage.WARNING_TYPE){
            		throw new ContentRestrictionException("Restricted resolution:" + mWidth + "*" + mHeight);
            	}
            }        
        if (LOCAL_LOGV) {
            Log.v(TAG, "New ImageModel created:"
                    + " mSrc=" + mSrc
                    + " mContentType=" + mContentType
                    + " mUri=" + uri);
        }
    	}catch(IllegalArgumentException e){
    		Xlog.e(TAG, "IllegalArgumentException caught while opening or reading stream", e);
    		throw new MmsException("Type of media is unknown.");
    	}
        
    }

    private void decodeImageBounds() throws DrmException {
        UriImage uriImage = new UriImage(mContext, getUriWithDrmCheck());
        mWidth = uriImage.getWidth();
        mHeight = uriImage.getHeight();

        if (LOCAL_LOGV) {
            Log.v(TAG, "Image bounds: " + mWidth + "x" + mHeight);
        }
    }

    // EventListener Interface
    public void handleEvent(Event evt) {
        String mEvtType = evt.getType();
        if (mEvtType.equals(SmilMediaElementImpl.SMIL_MEDIA_START_EVENT)
            || mEvtType.equals(SmilMediaElementImpl.SMIL_MEDIA_PAUSE_EVENT)) {
            mVisible = true;
        } else if (mEvtType.equals(SmilMediaElementImpl.SMIL_MEDIA_END_EVENT)) {
            if (mFill != ElementTime.FILL_FREEZE) {
                mVisible = false;
            }
        }

        notifyModelChanged(false);
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    protected void checkContentRestriction() throws ContentRestrictionException {
        ContentRestriction cr = ContentRestrictionFactory.getContentRestriction();
        cr.checkImageContentType(mContentType);
    }

    public Bitmap getBitmap() {
        return internalGetBitmap(getUri());
    }

    public Bitmap getBitmapWithDrmCheck() throws DrmException {
        return internalGetBitmap(getUriWithDrmCheck());
    }

    private Bitmap internalGetBitmap(Uri uri) {
        Bitmap bm = mBitmapCache.get();
        if (bm == null) {
            try {
                bm = createThumbnailBitmap(THUMBNAIL_BOUNDS_LIMIT, uri);
                if (bm != null) {
                    mBitmapCache = new SoftReference<Bitmap>(bm);
                }
            } catch (OutOfMemoryError ex) {
                // fall through and return a null bitmap. The callers can handle a null
                // result and show R.drawable.ic_missing_thumbnail_picture
            }
        }
        return bm;
    }

    private Bitmap createThumbnailBitmap(int thumbnailBoundsLimit, Uri uri) {
        int outWidth = mWidth;
        int outHeight = mHeight;

        int s = 1;
        while ((outWidth / s > thumbnailBoundsLimit)
                || (outHeight / s > thumbnailBoundsLimit)) {
            s *= 2;
        }
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            Log.v(TAG, "createThumbnailBitmap: scale=" + s + ", w=" + outWidth / s
                    + ", h=" + outHeight / s);
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = s;

        InputStream input = null;
        InputStream inputForRotate = null;
        try {
            input = mContext.getContentResolver().openInputStream(uri);
            inputForRotate = mContext.getContentResolver().openInputStream(uri);
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
            Bitmap b = BitmapFactory.decodeStream(input, null, options);
            Xlog.i(TAG, "image rotation is" + degree + " degree");
            b = UriImage.rotate(b, degree);
            return b;
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        } catch (OutOfMemoryError ex) {
            MessageUtils.writeHprofDataToFile();
            throw ex;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public boolean getMediaResizable() {
        return true;
    }

    @Override
    protected void resizeMedia(int byteLimit, long messageId) throws MmsException {
        UriImage image = new UriImage(mContext, getUri());
        if (image == null) {
            throw new ExceedMessageSizeException("No room to resize picture: " + getUri());
        }
        PduPart part = image.getResizedImageAsPart(
                MmsConfig.getMaxImageWidth(),
                MmsConfig.getMaxImageHeight(),
                byteLimit);

        if (part == null) {
            throw new ExceedMessageSizeException("Not enough memory to turn image into part: " +
                    getUri());
        }

        String src = getSrc();
        byte[] srcBytes = src.getBytes();
        part.setContentLocation(srcBytes);
        int period = src.lastIndexOf(".");
        byte[] contentId = period != -1 ? src.substring(0, period).getBytes() : srcBytes;
        part.setContentId(contentId);

        PduPersister persister = PduPersister.getPduPersister(mContext);
        this.mSize = part.getData().length;
        Uri newUri = persister.persistPart(part, messageId);
        setUri(newUri);
    }
    
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
}
