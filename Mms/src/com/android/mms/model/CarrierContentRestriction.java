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

import java.util.ArrayList;

import android.content.ContentResolver;

import com.google.android.mms.ContentType;
import com.android.mms.ContentRestrictionException;
import com.android.mms.ExceedMessageSizeException;
import com.android.mms.MmsConfig;
import com.android.mms.ResolutionException;
import com.android.mms.UnsupportContentTypeException;
import com.android.mms.data.WorkingMessage;

public class CarrierContentRestriction implements ContentRestriction {
    private static final ArrayList<String> sSupportedImageTypes;
    private static final ArrayList<String> sSupportedAudioTypes;
    private static final ArrayList<String> sSupportedVideoTypes;
    private static final ArrayList<String> sSupportedTypes;

    static {
        sSupportedImageTypes = ContentType.getImageTypes();
        sSupportedAudioTypes = ContentType.getAudioTypes();
        sSupportedVideoTypes = ContentType.getVideoTypes();
        sSupportedTypes      = ContentType.getSupportedTypes();
    }

    public CarrierContentRestriction() {
    }

    public void checkMessageSize(int messageSize, int increaseSize, ContentResolver resolver)
            throws ContentRestrictionException {
        if ( (messageSize < 0) || (increaseSize < 0) ) {
            throw new ContentRestrictionException("Negative message size"
                    + " or increase size");
        }
        int newSize = messageSize + increaseSize;

        if ( (newSize < 0) || (newSize > MmsConfig.getUserSetMmsSizeLimit(true)) ) {
            throw new ExceedMessageSizeException("Exceed message size limitation");
        }
    }

    public void checkResolution(int width, int height) throws ContentRestrictionException {
        if ( (width > MmsConfig.getMaxImageWidth()) || (height > MmsConfig.getMaxImageHeight()) ) {
            throw new ResolutionException("content resolution exceeds restriction.");
        }
    }

    public void checkImageContentType(String contentType)
            throws ContentRestrictionException {
        if (null == contentType) {
            throw new ContentRestrictionException("Null content type to be check");
        }

        checkRestrictedContentType(contentType);
        if (!sSupportedImageTypes.contains(contentType)) {
            throw new UnsupportContentTypeException("Unsupported image content type : "
                    + contentType);
        }
    }

    public void checkAudioContentType(String contentType)
            throws ContentRestrictionException {
        if (null == contentType) {
            throw new ContentRestrictionException("Null content type to be check");
        }

        checkRestrictedContentType(contentType);
        if (!sSupportedAudioTypes.contains(contentType)) {
            throw new UnsupportContentTypeException("Unsupported audio content type : "
                    + contentType);
        }
    }

    public void checkVideoContentType(String contentType)
            throws ContentRestrictionException {
        if (null == contentType) {
            throw new ContentRestrictionException("Null content type to be check");
        }

        checkRestrictedContentType(contentType);
        
        if (!sSupportedVideoTypes.contains(contentType)) {
            throw new UnsupportContentTypeException("Unsupported video content type : "
                    + contentType);
        }
    }

    public void checkFileAttachmentContentType(String contentType) throws ContentRestrictionException {
        if (null == contentType) {
            throw new ContentRestrictionException("Null content type to be check");
        }

        if (!sSupportedTypes.contains(contentType)) {
            throw new UnsupportContentTypeException("Unsupported content type : " + contentType);
        }
    }

    private void checkRestrictedContentType(String contentType)
    throws ContentRestrictionException{
    	if (WorkingMessage.sCreationMode != 0 && !ContentType.isRestrictedType(contentType)){
    		throw new ContentRestrictionException("Restricted content type:" + contentType);
    	}
    	
    }
}
