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

package com.android.mms.transaction;

import org.apache.http.entity.ByteArrayEntity;

import android.content.Context;
import android.content.Intent;

import java.io.IOException;
import java.io.OutputStream;

public class ProgressCallbackEntity extends ByteArrayEntity {
    private static final int DEFAULT_PIECE_SIZE = 4096;

    public static final String PROGRESS_STATUS_ACTION = "com.android.mms.PROGRESS_STATUS";
    public static final int PROGRESS_START    = -1;
    public static final int PROGRESS_ABORT    = -2;
    public static final int PROGRESS_COMPLETE = 100;

    private final Context mContext;
    private final byte[] mContent;
    private final long mToken;

    public ProgressCallbackEntity(Context context, long token, byte[] b) {
        super(b);

        mContext = context;
        mContent = b;
        mToken = token;
    }

    @Override
    public void writeTo(final OutputStream outstream) throws IOException {
        if (outstream == null) {
            throw new IllegalArgumentException("Output stream may not be null");
        }

        boolean completed = false;
        try {
            broadcastProgressIfNeeded(PROGRESS_START);

            int pos = 0, totalLen = mContent.length;
            int len = 0;
            while (pos < totalLen) {
                len = totalLen - pos;
                if (len > DEFAULT_PIECE_SIZE) {
                    len = DEFAULT_PIECE_SIZE;
                }
                outstream.write(mContent, pos, len);
                outstream.flush();

                pos += len;

                broadcastProgressIfNeeded(100 * pos / totalLen);
            }

            broadcastProgressIfNeeded(PROGRESS_COMPLETE);
            completed = true;
        } finally {
            if (!completed) {
                broadcastProgressIfNeeded(PROGRESS_ABORT);
            }
        }
    }

    private void broadcastProgressIfNeeded(int progress) {
        if (mToken > 0) {
            Intent intent = new Intent(PROGRESS_STATUS_ACTION);
            intent.putExtra("progress", progress);
            intent.putExtra("token", mToken);
            mContext.sendBroadcast(intent);
        }
    }
}
