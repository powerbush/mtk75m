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

import java.util.ArrayList;

import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventException;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import android.util.Log;

public class EventTargetImpl implements EventTarget {
    private static final String TAG = "EventTargetImpl";
    private ArrayList<EventListenerEntry> mListenerEntries;
    private EventTarget mNodeTarget;

    static class EventListenerEntry
    {
        final String mType;
        final EventListener mListener;
        final boolean mUseCapture;

        EventListenerEntry(String type, EventListener listener, boolean useCapture)
        {
            mType = type;
            mListener = listener;
            mUseCapture = useCapture;
        }
    }

    public EventTargetImpl(EventTarget target) {
        mNodeTarget = target;
    }

    public void addEventListener(String type, EventListener listener, boolean useCapture) {
        if ((type == null) || type.equals("") || (listener == null)) {
            return;
        }

        // Make sure we have only one entry
        removeEventListener(type, listener, useCapture);

        if (mListenerEntries == null) {
            mListenerEntries = new ArrayList<EventListenerEntry>();
        }
        mListenerEntries.add(new EventListenerEntry(type, listener, useCapture));
    }

    public boolean dispatchEvent(Event evt) throws EventException {
        // We need to use the internal APIs to modify and access the event status
        EventImpl eventImpl = (EventImpl)evt;

        if (!eventImpl.isInitialized()) {
            throw new EventException(EventException.UNSPECIFIED_EVENT_TYPE_ERR,
                    "Event not initialized");
        } else if ((eventImpl.getType() == null) || eventImpl.getType().equals("")) {
            throw new EventException(EventException.UNSPECIFIED_EVENT_TYPE_ERR,
                    "Unspecified even type");
        }

        // Initialize event status
        eventImpl.setTarget(mNodeTarget);

        // TODO: At this point, to support event capturing and bubbling, we should
        // establish the chain of EventTargets from the top of the tree to this
        // event's target.

        // TODO: CAPTURING_PHASE skipped

        // Handle AT_TARGET
        // Invoke handleEvent of non-capturing listeners on this EventTarget.
        eventImpl.setEventPhase(Event.AT_TARGET);
        eventImpl.setCurrentTarget(mNodeTarget);
        if (!eventImpl.isPropogationStopped() && (mListenerEntries != null)) {
            for (int i = 0; i < mListenerEntries.size(); i++) {
                EventListenerEntry listenerEntry = mListenerEntries.get(i);
                if (!listenerEntry.mUseCapture
                        && listenerEntry.mType.equals(eventImpl.getType())) {
                    try {
                        listenerEntry.mListener.handleEvent(eventImpl);
                    }
                    catch (Exception e) {
                        // Any exceptions thrown inside an EventListener will
                        // not stop propagation of the event
                        Log.w(TAG, "Catched EventListener exception", e);
                    }
                }
            }
        }

        if (eventImpl.getBubbles()) {
            // TODO: BUBBLING_PHASE skipped
        }

        return eventImpl.isPreventDefault();
    }

    public void removeEventListener(String type, EventListener listener,
            boolean useCapture) {
        if (null == mListenerEntries) {
            return;
        }
        for (int i = 0; i < mListenerEntries.size(); i ++) {
            EventListenerEntry listenerEntry = mListenerEntries.get(i);
            if ((listenerEntry.mUseCapture == useCapture)
                    && (listenerEntry.mListener == listener)
                    && listenerEntry.mType.equals(type)) {
                mListenerEntries.remove(i);
                break;
            }
        }
    }

}
