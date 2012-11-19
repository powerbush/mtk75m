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

package com.android.mms.dom.events;

import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventTarget;

public class EventImpl implements Event {

    // Event type informations
    private String mEventType;
    private boolean mCanBubble;
    private boolean mCancelable;

    // Flags whether the event type information was set
    // FIXME: Can we use mEventType for this purpose?
    private boolean mInitialized;

    // Target of this event
    private EventTarget mTarget;

    // Event status variables
    private short mEventPhase;
    private boolean mStopPropagation;
    private boolean mPreventDefault;
    private EventTarget mCurrentTarget;
    private int mSeekTo;

    private final long mTimeStamp = System.currentTimeMillis();

    public boolean getBubbles() {
        return mCanBubble;
    }

    public boolean getCancelable() {
        return mCancelable;
    }

    public EventTarget getCurrentTarget() {
        return mCurrentTarget;
    }

    public short getEventPhase() {
        return mEventPhase;
    }

    public EventTarget getTarget() {
        return mTarget;
    }

    public long getTimeStamp() {
        return mTimeStamp;
    }

    public String getType() {
        return mEventType;
    }

    public void initEvent(String eventTypeArg, boolean canBubbleArg,
            boolean cancelableArg) {
        mEventType = eventTypeArg;
        mCanBubble = canBubbleArg;
        mCancelable = cancelableArg;
        mInitialized = true;
    }

    public void initEvent(String eventTypeArg, boolean canBubbleArg, boolean cancelableArg,
            int seekTo) {
        mSeekTo = seekTo;
        initEvent(eventTypeArg, canBubbleArg, cancelableArg);
    }

    public void preventDefault() {
        mPreventDefault = true;
    }

    public void stopPropagation() {
        mStopPropagation = true;
    }

    /*
     * Internal Interface
     */

    boolean isInitialized() {
        return mInitialized;
    }

    boolean isPreventDefault() {
        return mPreventDefault;
    }

    boolean isPropogationStopped() {
        return mStopPropagation;
    }

    void setTarget(EventTarget target) {
        mTarget = target;
    }

    void setEventPhase(short eventPhase) {
        mEventPhase = eventPhase;
    }

    void setCurrentTarget(EventTarget currentTarget) {
        mCurrentTarget = currentTarget;
    }

    public int getSeekTo() {
        return mSeekTo;
    }
}
