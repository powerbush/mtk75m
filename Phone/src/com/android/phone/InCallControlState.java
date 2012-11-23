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
 * Copyright (C) 2009 The Android Open Source Project
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

import android.telephony.PhoneNumberUtils;
import android.util.Log;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.CallManager;

//MTK begin:
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;
//MTK end

/**
 * Helper class to keep track of enabledness, visibility, and "on/off"
 * or "checked" state of the various controls available in the in-call
 * UI, based on the current telephony state.
 *
 * This class is independent of the exact UI controls used on any given
 * device.  (Some devices use onscreen touchable buttons, for example, and
 * other devices use menu items.)  To avoid cluttering up the InCallMenu
 * and InCallTouchUi code with logic about which functions are available
 * right now, we instead have that logic here, and provide simple boolean
 * flags to indicate the state and/or enabledness of all possible in-call
 * user operations.
 *
 * (In other words, this is the "model" that corresponds to the "view"
 * implemented by InCallMenu and InCallTouchUi.)
 */
public class InCallControlState {
    private static final String LOG_TAG = "InCallControlState";
    //private static final boolean DBG = (PhoneApp.DBG_LEVEL >= 2);
    private static final boolean DBG = true;

    private InCallScreen mInCallScreen;
    private CallManager mCM;

    //
    // Our "public API": Boolean flags to indicate the state and/or
    // enabledness of all possible in-call user operations:
    //

    public boolean manageConferenceVisible;
    public boolean manageConferenceEnabled;
    //
    public boolean canAddCall;
    //
    public boolean canShowSwap;
    public boolean canSwap;
    public boolean canMerge;
    //
    public boolean bluetoothEnabled;
    public boolean bluetoothIndicatorOn;
    //
    public boolean speakerEnabled;
    public boolean speakerOn;
    //
    public boolean canMute;
    public boolean muteIndicatorOn;
    //
    public boolean dialpadEnabled;
    public boolean dialpadVisible;
    //
    /** True if the "Hold" function is *ever* available on this device */
    public boolean supportsHold;
    /** True if the call is currently on hold */
    public boolean onHold;
    /** True if the "Hold" or "Unhold" function should be available right now */
    // TODO: this name is misleading.  Let's break this apart into
    // separate canHold and canUnhold flags, and have the caller look at
    // "canHold || canUnhold" to decide whether the hold/unhold UI element
    // should be visible.
    public boolean canHold;

    /* Added by xingping.zheng start */
    public boolean contactsEnabled;
    /* Added by xingping.zheng end   */

    public InCallControlState(InCallScreen inCallScreen, CallManager cm) {
        if (DBG) log("InCallControlState constructor...");
        mInCallScreen = inCallScreen;
        mCM = cm;
    }

    /**
     * Updates all our public boolean flags based on the current state of
     * the Phone.
     */
    public void update() {
/*Google:final Call fgCall = mCM.getActiveFgCall();
        final Call.State fgCallState = fgCall.getState();
        final boolean hasActiveForegroundCall = (fgCallState == Call.State.ACTIVE);
        final boolean hasHoldingCall = mCM.hasActiveBgCall(); */
//MTK begin:
        final Call fgCall;
        final Call.State fgCallState;
        final boolean hasActiveForegroundCall;
        final boolean hasHoldingCall;
        
        fgCall = mCM.getActiveFgCall();
        hasHoldingCall = mCM.hasActiveBgCall();
        if (null != fgCall) {
            fgCallState = fgCall.getState();
            hasActiveForegroundCall = (fgCallState == Call.State.ACTIVE);
        } else {
        	fgCallState = Call.State.IDLE;
        	hasActiveForegroundCall = false;
        }
//MTK end
        
        /* Added by xingping.zheng start */
        if ((fgCallState == Call.State.DIALING) || (fgCallState == Call.State.ALERTING))
            contactsEnabled = false;
        else
            contactsEnabled = true;
        /* Added by xingping.zheng end   */
        
        // Manage conference:
        if (Call.State.IDLE != fgCall.getState() && TelephonyCapabilities.supportsConferenceCallManagement(fgCall.getPhone())) {
            // This item is visible only if the foreground call is a
            // conference call, and it's enabled unless the "Manage
            // conference" UI is already up.
            manageConferenceVisible = PhoneUtils.isConferenceCall(fgCall);
            manageConferenceEnabled =
                    manageConferenceVisible && !mInCallScreen.isManageConferenceMode();
        } else if (hasHoldingCall && TelephonyCapabilities.supportsConferenceCallManagement(mCM.getBgPhone())) {
        	manageConferenceVisible = PhoneUtils.isConferenceCall(mCM.getBgPhone().getBackgroundCall());
        	manageConferenceEnabled =
                manageConferenceVisible && !mInCallScreen.isManageConferenceMode();
        } else {
            // This device has no concept of managing a conference call.
            manageConferenceVisible = false;
            manageConferenceEnabled = false;
        }

        // "Add call":
        canAddCall = PhoneUtils.okToAddCall(mCM);

        // Swap / merge calls
        canShowSwap = PhoneUtils.okToShowSwapButton(mCM);
        canSwap = PhoneUtils.okToSwapCalls(mCM);
        canMerge = PhoneUtils.okToMergeCalls(mCM);

        // "Bluetooth":
        if (mInCallScreen.isBluetoothAvailable()) {
            bluetoothEnabled = true;
            bluetoothIndicatorOn = mInCallScreen.isBluetoothAudioConnectedOrPending();
        } else {
            bluetoothEnabled = false;
            bluetoothIndicatorOn = false;
        }

        // "Speaker": always enabled.
        // The current speaker state comes from the AudioManager.
        speakerEnabled = true;
        speakerOn = PhoneUtils.isSpeakerOn(mInCallScreen);

        // "Mute": only enabled when the foreground call is ACTIVE.
        // (It's meaningless while on hold, or while DIALING/ALERTING.)
        // It's also explicitly disabled during emergency calls or if
        // emergency callback mode (ECM) is active.
        Connection c;
        if (null != fgCall) {
            c = fgCall.getLatestConnection();
        } else {
            c = null;
        }
        boolean isEmergencyCall = false;
        if (c != null) isEmergencyCall = PhoneNumberUtils.isEmergencyNumber(c.getAddress());
        boolean isECM = PhoneUtils.isPhoneInEcm(fgCall.getPhone());
        if (isEmergencyCall || isECM) {  // disable "Mute" item
            canMute = false;
            muteIndicatorOn = false;
        } else {
            canMute = hasActiveForegroundCall;
            muteIndicatorOn = PhoneUtils.getMute();
        }

        // "Dialpad": Enabled only when it's OK to use the dialpad in the
        // first place.
        dialpadEnabled = mInCallScreen.okToShowDialpad();

        // Also keep track of whether the dialpad is currently "opened"
        // (i.e. visible).
        dialpadVisible = mInCallScreen.isDialerOpened();

        // "Hold:
        if (null != fgCall && TelephonyCapabilities.supportsHoldAndUnhold(fgCall.getPhone())) {
            // This phone has the concept of explicit "Hold" and "Unhold" actions.
            supportsHold = true;
            // "On hold" means that there's a holding call and
            // *no* foreground call.  (If there *is* a foreground call,
            // that's "two lines in use".)
            onHold = hasHoldingCall && (fgCallState == Call.State.IDLE) && !PhoneUtils.holdAndActiveFromDifPhone(mCM);
            // The "Hold" control is disabled entirely if there's
            // no way to either hold or unhold in the current state.
            boolean okToHold = hasActiveForegroundCall && !hasHoldingCall;
            boolean okToUnhold = onHold;
            canHold = okToHold || okToUnhold;
        } else {
            // This device has no concept of "putting a call on hold."
            supportsHold = false;
            onHold = false;
            canHold = false;
        }

        if (DBG) dumpState();
    }

    public void dumpState() {
        log("InCallControlState:");
        log("  manageConferenceVisible: " + manageConferenceVisible);
        log("  manageConferenceEnabled: " + manageConferenceEnabled);
        log("  canAddCall: " + canAddCall);
        log("  canSwap: " + canSwap);
        log("  canMerge: " + canMerge);
        log("  bluetoothEnabled: " + bluetoothEnabled);
        log("  bluetoothIndicatorOn: " + bluetoothIndicatorOn);
        log("  speakerEnabled: " + speakerEnabled);
        log("  speakerOn: " + speakerOn);
        log("  canMute: " + canMute);
        log("  muteIndicatorOn: " + muteIndicatorOn);
        log("  dialpadEnabled: " + dialpadEnabled);
        log("  dialpadVisible: " + dialpadVisible);
        log("  onHold: " + onHold);
        log("  canHold: " + canHold);
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
