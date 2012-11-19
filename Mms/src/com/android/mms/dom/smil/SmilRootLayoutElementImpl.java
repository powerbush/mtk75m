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
 * Copyright (C) 2007 Esmertec AG.
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.mms.dom.smil;

import org.w3c.dom.DOMException;
import org.w3c.dom.smil.SMILRootLayoutElement;

public class SmilRootLayoutElementImpl extends SmilElementImpl implements
        SMILRootLayoutElement {

    private static final String WIDTH_ATTRIBUTE_NAME = "width";
    private static final String HEIGHT_ATTRIBUTE_NAME = "height";
    private static final String BACKGROUND_COLOR_ATTRIBUTE_NAME = "backgroundColor";
    private static final String TITLE_ATTRIBUTE_NAME = "title";

    SmilRootLayoutElementImpl(SmilDocumentImpl owner, String tagName) {
        super(owner, tagName);
    }

    public String getBackgroundColor() {
        return this.getAttribute(BACKGROUND_COLOR_ATTRIBUTE_NAME);
    }

    public int getHeight() {
        String heightString = this.getAttribute(HEIGHT_ATTRIBUTE_NAME);
        return parseAbsoluteLength(heightString);
    }

    public String getTitle() {
        return this.getAttribute(TITLE_ATTRIBUTE_NAME);
    }

    public int getWidth() {
        String widthString = this.getAttribute(WIDTH_ATTRIBUTE_NAME);
        return parseAbsoluteLength(widthString);
    }

    public void setBackgroundColor(String backgroundColor) throws DOMException {
        this.setAttribute(BACKGROUND_COLOR_ATTRIBUTE_NAME, backgroundColor);
    }

    public void setHeight(int height) throws DOMException {
        this.setAttribute(HEIGHT_ATTRIBUTE_NAME, String.valueOf(height) + "px");

    }

    public void setTitle(String title) throws DOMException {
        this.setAttribute(TITLE_ATTRIBUTE_NAME, title);
    }

    public void setWidth(int width) throws DOMException {
        this.setAttribute(WIDTH_ATTRIBUTE_NAME, String.valueOf(width) + "px");
    }

    /*
     * Internal Interface
     */

    private int parseAbsoluteLength(String length) {
        if (length.endsWith("px")) {
            length = length.substring(0, length.indexOf("px"));
        }
        try {
            return Integer.parseInt(length);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
