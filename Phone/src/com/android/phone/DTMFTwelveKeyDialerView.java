/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

package com.android.phone;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Button;

import java.util.ArrayList;

import com.android.internal.telephony.CallManager;

/**
 * DTMFTwelveKeyDialerView is the view logic that the DTMFDialer uses.
 * This is really a thin wrapper around Linear Layout that intercepts
 * some user interactions to provide the correct UI behaviour for the
 * dialer.
 */
class DTMFTwelveKeyDialerView extends LinearLayout {

    private static final String LOG_TAG = "PHONE/DTMFTwelveKeyDialerView";
    private static final boolean DBG = false;

    private DTMFTwelveKeyDialer mDialer;
    private ButtonGridLayout mButtonGrid;

    /* Added by xingping.zheng start */
    private Button mEndCallButton;
    private Button mHideButton;
    
    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        
        public void onClick(View v) {
            // TODO Auto-generated method stub
            switch(v.getId()) {
                case R.id.endCall:
                    PhoneUtils.hangup(PhoneApp.getInstance().mCM);
                    break;
                case R.id.hideDialpad:
                    if (mDialer.isOpened()) {
                        mDialer.closeDialer(true);
                    }
                    mDialer.setHandleVisible(true);
                    break;
            }
        }
    };
    /* Added by xingping.zheng end */
    
    public DTMFTwelveKeyDialerView (Context context) {
        super(context);
    }

    public DTMFTwelveKeyDialerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void setDialer (DTMFTwelveKeyDialer dialer) {
        mDialer = dialer;
        mButtonGrid = (ButtonGridLayout)findViewById(R.id.dialpad);
        /* Added by xingping.zheng start */
        setupDtmfButtonGroup();
        /* Added by xingping.zheng end */
    }

    /* Added by xingping.zheng start */
    void setupDtmfButtonGroup() {
        mEndCallButton = (Button)findViewById(R.id.endCall);
        mHideButton = (Button)findViewById(R.id.hideDialpad);
        mEndCallButton.setOnClickListener(mOnClickListener);
        mHideButton.setOnClickListener(mOnClickListener);
    }
    /* Added by xingping.zheng end */
    
    /**
     * Normally we ignore everything except for the BACK and CALL keys.
     * For those, we pass them to the model (and then the InCallScreen).
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (DBG) log("dispatchKeyEvent(" + event + ")...");

        int keyCode = event.getKeyCode();
        if (mDialer != null) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                case KeyEvent.KEYCODE_CALL:
                    return event.isDown() ? mDialer.onKeyDown(keyCode, event) :
                        mDialer.onKeyUp(keyCode, event);
            }
        }

        if (DBG) log("==> dispatchKeyEvent: forwarding event to the DTMFDialer");
        return super.dispatchKeyEvent(event);
    }

    /**
     * Set the background of all the dialpad keys. Typically a selector to
     * change the background based on some combination of the
     * attributes.
     * @param resid Is a resource id to be used for each button's background.
     */
    public void setKeysBackgroundResource(int resid) {
        mButtonGrid.setChildrenBackgroundResource(resid);
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

}
