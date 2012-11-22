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

package com.android.camera.ui;

import javax.microedition.khronos.opengles.GL11;

abstract class BasicTexture implements Texture {

    protected static final int UNSPECIFIED = -1;

    public static final int STATE_UNLOADED = 0;
    public static final int STATE_LOADED = 1;
    public static final int STATE_ERROR = -1;

    protected GL11 mGL;

    protected int mId;
    protected int mState;

    protected int mWidth = UNSPECIFIED;
    protected int mHeight = UNSPECIFIED;

    protected int mTextureWidth;
    protected int mTextureHeight;

    protected BasicTexture(GL11 gl, int id, int state) {
        mGL = gl;
        mId = id;
        mState = state;
    }

    protected BasicTexture() {
        this(null, 0, STATE_UNLOADED);
    }

    protected void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    /**
     * Sets the size of the texture. Due to the limit of OpenGL, the texture
     * size must be of power of 2, the size of the content may not be the size
     * of the texture.
     */
    protected void setTextureSize(int width, int height) {
        mTextureWidth = width;
        mTextureHeight = height;
    }

    public int getId() {
        return mId;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public void deleteFromGL() {
        if (mState == STATE_LOADED) {
            mGL.glDeleteTextures(1, new int[]{mId}, 0);
        }
        mState = STATE_UNLOADED;
    }

    public void draw(GLRootView root, int x, int y) {
        root.drawTexture(this, x, y, mWidth, mHeight);
    }

    public void draw(GLRootView root, int x, int y, int w, int h) {
        root.drawTexture(this, x, y, w, h);
    }

    abstract protected boolean bind(GLRootView root, GL11 gl);
}
