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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.InputStream;

public class MockImage implements IImage {
    private final long mId;
    private final long mTakenDate;
    private IImageList mContainer;

    public MockImage(long id, long takenDate) {
        mId = id;
        mTakenDate = takenDate;
    }

    public int getDegreesRotated() {
        return 0;
    }

    protected void setContainer(IImageList container) {
        this.mContainer = container;
    }

    public Bitmap fullSizeBitmap(int minSideLength, int maxNumberOfPixels) {
        return null;
    }

    public Bitmap fullSizeBitmap(int minSideLength, int maxNumberOfPixels,
            boolean rotateAsNeeded) {
        return null;
    }

    public Bitmap fullSizeBitmap(int minSideLength, int maxNumberOfPixels,
            boolean rotateAsNeeded, boolean useNative) {
        return null;
    }

    public InputStream fullSizeImageData() {
        return null;
    }

    public long fullSizeImageId() {
        return mId;
    }

    public Uri fullSizeImageUri() {
        return null;
    }

    public IImageList getContainer() {
        return mContainer;
    }

    public String getDataPath() {
        return null;
    }

    public long getDateTaken() {
        return mTakenDate;
    }

    public String getDisplayName() {
        return null;
    }

    public int getHeight() {
        return 0;
    }

    public String getMimeType() {
        return null;
    }

    public String getTitle() {
        return null;
    }

    public int getWidth() {
        return 0;
    }

    public boolean isDrm() {
        return false;
    }

    public boolean isReadonly() {
        return false;
    }

    public Bitmap miniThumbBitmap() {
        return null;
    }

    public boolean rotateImageBy(int degrees) {
        return false;
    }

    public void setTitle(String name) {
    }

    public Bitmap thumbBitmap(boolean rotateAsNeeded) {
        return null;
    }

    public Uri thumbUri() {
        return null;
    }
}
