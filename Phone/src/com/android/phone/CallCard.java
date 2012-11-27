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
 * Copyright (C) 2006 The Android Open Source Project
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

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.pim.ContactsAsyncHelper;
import android.provider.ContactsContract.Contacts;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.WindowManager;
import android.view.Display;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.CallerInfoAsyncQuery;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.CallManager;

import java.util.List;

/* Fion add start */
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;
import com.mediatek.telephony.PhoneNumberFormatUtilEx;
/* Fion add end */


/**
 * "Call card" UI element: the in-call screen contains a tiled layout of call
 * cards, each representing the state of a current "call" (ie. an active call,
 * a call on hold, or an incoming call.)
 */
public class CallCard extends FrameLayout
        implements CallTime.OnTickListener, CallerInfoAsyncQuery.OnQueryCompleteListener,
                   ContactsAsyncHelper.OnImageLoadCompleteListener, View.OnClickListener {
    private static final String LOG_TAG = "CallCard";
    private static final boolean DBG = true;//(PhoneApp.DBG_LEVEL >= 2);

    /**
     * Reference to the InCallScreen activity that owns us.  This may be
     * null if we haven't been initialized yet *or* after the InCallScreen
     * activity has been destroyed.
     */
    private InCallScreen mInCallScreen;

    // Phone app instance
    private PhoneApp mApplication;

    // Top-level subviews of the CallCard
    //private ViewGroup mPrimaryCallInfo;
//    launch performance start
//    private ViewGroup mSecondaryCallInfo;
//    launch performance end

    // Title and elapsed-time widgets
//    launch performance start
//    private TextView mUpperTitle;
//    private TextView mElapsedTime;
//    launch performance end

    // Text colors, used for various labels / titles
//    launch performance start
//    private int mTextColorDefaultPrimary;
    private int mTextColorDefaultSecondary;
//    private int mTextColorConnected;
//    private int mTextColorConnectedBluetooth;
//    private int mTextColorEnded;
//    private int mTextColorOnHold;
//    launch performance end
    private int mTextColorCallTypeSip;

    // The main block of info about the "primary" or "active" call,
    // including photo / name / phone number / etc.
    ImageView mPhoto;
//    launch performance start
//    private Button mManageConferencePhotoButton;
//    launch performance end
    /* Added by xingping.zheng start */
    private Button mManageConferenceUiButton;
    /* Added by xingping.zheng end */
    private TextView mName;
//    launch performance start
//    private TextView mPhoneNumber;
//    private TextView mLabel;
//    launch performance end
    private TextView mLabelAndNumber;
    private TextView mCallTypeLabel;
    private TextView mSocialStatus;

    // Info about the "secondary" call, which is the "call on hold" when
    // two lines are in use.
    /*
    launch performance start
    private TextView mSecondaryCallName;
    private TextView mSecondaryCallStatus;
    private ImageView mSecondaryCallPhoto;
    launch performance end
    */

    // Menu button hint
    private TextView mMenuButtonHint;

    // Onscreen hint for the incoming call RotarySelector widget.
    private int mRotarySelectorHintTextResId;
    private int mRotarySelectorHintColorResId;

    private CallTime mCallTime;
    // When Locale changed the boolean will be true;
    static private boolean mLocaleChanged = false; 
    static private boolean mLCforUserData = false;
    // Track the state for the photo.
    private ContactsAsyncHelper.ImageTracker mPhotoTracker;

    // Cached DisplayMetrics density.
    private float mDensity;
    private StringBuilder mStringBuilder = new StringBuilder();
    // QVGA Width & Height
    private static final int QVGA_WIDTH   = 320;
    private static final int QVGA_HEIGHT  = 240;
    public CallCard(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (DBG) log("CallCard constructor...");
        if (DBG) log("- this = " + this);
        if (DBG) log("- context " + context + ", attrs " + attrs);

        // Inflate the contents of this CallCard, and add it (to ourself) as a child.
//        LayoutInflater inflater = LayoutInflater.from(context);
//        inflater.inflate(
//                R.layout.call_card,  // resource
//                this,                // root
//                true);

        mApplication = PhoneApp.getInstance();

        mCallTime = new CallTime(this);

        // create a new object to track the state for the photo.
        mPhotoTracker = new ContactsAsyncHelper.ImageTracker();

        mDensity = getResources().getDisplayMetrics().density;
        if (DBG) log("- Density: " + mDensity);
    }

    void setInCallScreenInstance(InCallScreen inCallScreen) {
        mInCallScreen = inCallScreen;
    }

    public void onTickForCallTimeElapsed(long timeElapsed) {
        // While a call is in progress, update the elapsed time shown
        // onscreen.
        updateElapsedTimeWidget(timeElapsed);
        /* Added by xingping.zheng start */
        
        if (mInCallScreen != null && mInCallScreen.mCallStatus != null) {
            mInCallScreen.mCallStatus.updateElapsedTimeWidgetOfCallStatus(timeElapsed);
        } else {
            if (DBG) Log.d("onTickForCallTimeElapsed: ", "mInCallScreen is not ready!");
        }
        /* Added by xingping.zheng end */
    }
    
    void setVtCallTimerConnection(Connection c) {
        mCallTime.vtConnection = c;
    }
    
    Connection getVtCallTimerConnection() {
        return mCallTime.vtConnection;
    }
    
    void startVtCallTimer(Connection c) {
        if (this.mCallTime != null) {
            if (mCallTime.vtConnection != c) {
                //cancel the pre-connection
                mCallTime.stopCallTimer();
                mCallTime.startCallTimer(true, 0);
                mCallTime.vtConnection = c;
            }
        }
    }
    
    void stopVtCallTimer(Connection c) {
        if (this.mCallTime != null) {
            if (c != mCallTime.vtConnection) {
                return ;
            }
            mCallTime.stopCallTimer();
            mCallTime.vtConnection = null;
        }
    }
    
    void cancelVtCallTimer() {
        if (this.mCallTime != null) {
            mCallTime.stopCallTimer();
            mCallTime.vtConnection = null;
            mCallTime.mCallTimer.cancel();
        }
    }

    /* package */
    void stopTimer() {
        CallManager cm = PhoneApp.getInstance().mCM;
        
        //Check if there is active call exist
        //ALPS00098100 : update the call reminder when InCallScreen not in foreground.
        if (cm != null && cm.hasActiveFgCall()) {
            mCallTime.cancelTimer(false);
        } else {
            mCallTime.cancelTimer(true);
        }
    }
    
    void clearReminder() {
        if (mCallTime != null) {
            mCallTime.clearReminder();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (DBG) log("CallCard onFinishInflate(this = " + this + ")...");

        //mPrimaryCallInfo = this;//(ViewGroup) findViewById(R.id.primaryCallInfo);

//        launch performance start
//        mSecondaryCallInfo = (ViewGroup) findViewById(R.id.secondaryCallInfo);
//        mSecondaryCallInfo.setVisibility(View.GONE);
//        launch performance end

        // "Upper" and "lower" title widgets
//        launch performance start
//        mUpperTitle = (TextView) findViewById(R.id.upperTitle);
//        mElapsedTime = (TextView) findViewById(R.id.elapsedTime);
//        launch performance end

        // Text colors
//        launch performance start
//        mTextColorDefaultPrimary =  // corresponds to textAppearanceLarge
//                getResources().getColor(android.R.color.primary_text_dark);
        mTextColorDefaultSecondary =  // corresponds to textAppearanceSmall
                getResources().getColor(android.R.color.secondary_text_dark);
//        mTextColorConnected = getResources().getColor(R.color.incall_textConnected);
//        mTextColorConnectedBluetooth =
//                getResources().getColor(R.color.incall_textConnectedBluetooth);
//        mTextColorEnded = getResources().getColor(R.color.incall_textEnded);
//        mTextColorOnHold = getResources().getColor(R.color.incall_textOnHold);
//        launch performance end
        mTextColorCallTypeSip = getResources().getColor(R.color.incall_callTypeSip);

        // "Caller info" area, including photo / name / phone numbers / etc
        mPhoto = (ImageView) findViewById(R.id.photo);
        //mManageConferencePhotoButton = (Button) findViewById(R.id.manageConferencePhotoButton);
        /* Added by xingping.zheng start */
        //mManageConferencePhotoButton.setOnClickListener(this);
        mManageConferenceUiButton = (Button) findViewById(R.id.manageConferenceUiButton);
        if(mManageConferenceUiButton != null)
            mManageConferenceUiButton.setOnClickListener(this);
        /* Added by xingping.zheng end */
        mName = (TextView) findViewById(R.id.name);
//        launch performance start
//        mPhoneNumber = (TextView) findViewById(R.id.phoneNumber);
//        mLabel = (TextView) findViewById(R.id.label);
//        launch performance end
        mLabelAndNumber = (TextView) findViewById(R.id.labelAndNumber);
        mCallTypeLabel = (TextView) findViewById(R.id.callTypeLabel);
        mSocialStatus = (TextView) findViewById(R.id.socialStatus);

        // "Other call" info area
//        launch performance start
//        mSecondaryCallName = (TextView) findViewById(R.id.secondaryCallName);
//        mSecondaryCallStatus = (TextView) findViewById(R.id.secondaryCallStatus);
//        mSecondaryCallPhoto = (ImageView) findViewById(R.id.secondaryCallPhoto);
//        launch performance end

        // Menu Button hint
        mMenuButtonHint = (TextView) findViewById(R.id.menuButtonHint);
    }

    // When language changed should call this function.
    public void updateForLanguageChange() {
        // Update String "Press Menu for call options".
        // mMenuButtonHint.setText(R.string.menuButtonHint);
        mLocaleChanged = true;
		mLCforUserData = true;
    }

//    launch performance start
//    private void updateSecondaryCallStatus() {
//        mSecondaryCallStatus.setText(R.string.onHold);
//    }
//    launch performance end

    /**
     * Updates the state of all UI elements on the CallCard, based on the
     * current state of the phone.
     */
    void updateState(CallManager cm) {
        if (DBG) log("updateState(" + cm + ")...");
        // if Language changed, the secondary status need change to other language.
        // launch performance start
        // updateSecondaryCallStatus();
        // launch performance end
        // Update some internal state based on the current state of the phone.

        // TODO: clean up this method to just fully update EVERYTHING in
        // the callcard based on the current phone state: set the overall
        // type of the CallCard, load up the main caller info area, and
        // load up and show or hide the "other call" area if necessary.

        Phone.State state = cm.getState();  // IDLE, RINGING, or OFFHOOK
        Call ringingCall = cm.getFirstActiveRingingCall();
        Call fgCall = cm.getActiveFgCall();
        Call bgCall = cm.getFirstActiveBgCall();

        // If the FG call is dialing/alerting, we should display for that call
        // and ignore the ringing call. This case happens when the telephony
        // layer rejects the ringing call while the FG call is dialing/alerting,
        // but the incoming call *does* briefly exist in the DISCONNECTING or
        // DISCONNECTED state.
        if ((ringingCall.getState() != Call.State.IDLE)
                && !fgCall.getState().isDialing()) {
            // A phone call is ringing, call waiting *or* being rejected
            // (ie. another call may also be active as well.)
            updateRingingCall(cm);
        } else if ((fgCall.getState() != Call.State.IDLE)
                || (bgCall.getState() != Call.State.IDLE)) {
            // We are here because either:
            // (1) the phone is off hook. At least one call exists that is
            // dialing, active, or holding, and no calls are ringing or waiting,
            // or:
            // (2) the phone is IDLE but a call just ended and it's still in
            // the DISCONNECTING or DISCONNECTED state. In this case, we want
            // the main CallCard to display "Hanging up" or "Call ended".
            // The normal "foreground call" code path handles both cases.
            updateForegroundCall(cm);
        } else {
            // We don't have any DISCONNECTED calls, which means
            // that the phone is *truly* idle.
            //
            // It's very rare to be on the InCallScreen at all in this
            // state, but it can happen in some cases:
            // - A stray onPhoneStateChanged() event came in to the
            //   InCallScreen *after* it was dismissed.
            // - We're allowed to be on the InCallScreen because
            //   an MMI or USSD is running, but there's no actual "call"
            //   to display.
            // - We're displaying an error dialog to the user
            //   (explaining why the call failed), so we need to stay on
            //   the InCallScreen so that the dialog will be visible.
            //
            // In these cases, put the callcard into a sane but "blank" state:
            updateNoCall(cm);
        }
    }

    /**
     * Updates the UI for the state where the phone is in use, but not ringing.
     */
    private void updateForegroundCall(CallManager cm) {
        if (DBG) log("updateForegroundCall()...");
        // if (DBG) PhoneUtils.dumpCallManager();

        Call fgCall = cm.getActiveFgCall();
        Call bgCall = cm.getFirstActiveBgCall();

        if (fgCall.getState() == Call.State.IDLE) {
            if (DBG) log("updateForegroundCall: no active call, show holding call");
            // TODO: make sure this case agrees with the latest UI spec.

            // Display the background call in the main info area of the
            // CallCard, since there is no foreground call.  Note that
            // displayMainCallStatus() will notice if the call we passed in is on
            // hold, and display the "on hold" indication.
            fgCall = bgCall;

            // And be sure to not display anything in the "on hold" box.
            bgCall = null;
        }

        displayMainCallStatus(cm, fgCall);
        // launch performance start
        /*
        Phone phone = fgCall.getPhone();
        if(false) {
            int phoneType = phone.getPhoneType();
            if (phoneType == Phone.PHONE_TYPE_CDMA) {
                if ((mApplication.cdmaPhoneCallState.getCurrentCallState()
                        == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE)
                        && mApplication.cdmaPhoneCallState.IsThreeWayCallOrigStateDialing()) {
                    displayOnHoldCallStatus(cm, fgCall);
                } else {
                    //This is required so that even if a background call is not present
                    // we need to clean up the background call area.
                    displayOnHoldCallStatus(cm, bgCall);
                }
            } else if ((phoneType == Phone.PHONE_TYPE_GSM)
                    || (phoneType == Phone.PHONE_TYPE_SIP)) {
                displayOnHoldCallStatus(cm, bgCall);
            }
        } else if ((phoneType == Phone.PHONE_TYPE_GSM)
                || (phoneType == Phone.PHONE_TYPE_SIP)) {
            displayOnHoldCallStatus(cm, bgCall);
        }
        */
        // launch performance end
    }

    /**
     * Updates the UI for the state where an incoming call is ringing (or
     * call waiting), regardless of whether the phone's already offhook.
     */
    private void updateRingingCall(CallManager cm) {
        if (DBG) log("updateRingingCall()...");

        Call ringingCall = cm.getFirstActiveRingingCall();

        // Display caller-id info and photo from the incoming call:
        displayMainCallStatus(cm, ringingCall);

        // And even in the Call Waiting case, *don't* show any info about
        // the current ongoing call and/or the current call on hold.
        // (Since the caller-id info for the incoming call totally trumps
        // any info about the current call(s) in progress.)
        // launch performance start
        // displayOnHoldCallStatus(cm, null);
        // launch performance end
    }

    /**
     * Updates the UI for the state where the phone is not in use.
     * This is analogous to updateForegroundCall() and updateRingingCall(),
     * but for the (uncommon) case where the phone is
     * totally idle.  (See comments in updateState() above.)
     *
     * This puts the callcard into a sane but "blank" state.
     */
    private void updateNoCall(CallManager cm) {
        if (DBG) log("updateNoCall()...");

        displayMainCallStatus(cm, null);
        // launch performance start
        //displayOnHoldCallStatus(cm, null);
        // launch performance end
    }

    /**
     * Updates the main block of caller info on the CallCard
     * (ie. the stuff in the primaryCallInfo block) based on the specified Call.
     */
    private void displayMainCallStatus(CallManager cm, Call call) {
        if (DBG) log("displayMainCallStatus(phone " + cm
                     + ", call " + call + ")...");

        if (call == null) {
            // There's no call to display, presumably because the phone is idle.
            hideCallCardElements();
            return;
        }
        //mPrimaryCallInfo.setVisibility(View.VISIBLE);

        Call.State state = call.getState();
        if (DBG) log("  - call.state: " + call.getState());
        Connection c = call.getLatestConnection();
        
        switch (state) {
            case ACTIVE:
                if (c != null && c.isVideo()) {
                    startVtCallTimer(c);
                }
            case DISCONNECTING:
                // update timer field
                if (DBG) log("displayMainCallStatus: start periodicUpdateTimer");
                mCallTime.setActiveCallMode(call);
                mCallTime.reset();
                mCallTime.periodicUpdateTimer();

                break;

            case HOLDING:
                // update timer field
                mCallTime.cancelTimer(true);

                break;

            case DISCONNECTED:
                // Stop getting timer ticks from this call
                mCallTime.cancelTimer(true);

                break;

            case DIALING:
            case ALERTING:
                // Stop getting timer ticks from a previous call
                mCallTime.cancelTimer(true);

                break;

            case INCOMING:
            case WAITING:
                // Stop getting timer ticks from a previous call
                mCallTime.cancelTimer(true);

                break;

            case IDLE:
                // The "main CallCard" should never be trying to display
                // an idle call!  In updateState(), if the phone is idle,
                // we call updateNoCall(), which means that we shouldn't
                // have passed a call into this method at all.
                if(DBG) Log.w(LOG_TAG, "displayMainCallStatus: IDLE call in the main call card!");

                // (It is possible, though, that we had a valid call which
                // became idle *after* the check in updateState() but
                // before we get here...  So continue the best we can,
                // with whatever (stale) info we can get from the
                // passed-in Call object.)

                break;

            default:
                if(DBG) Log.w(LOG_TAG, "displayMainCallStatus: unexpected call state: " + state);
                break;
        }
        // launch performance start
        // updateCardTitleWidgets(call.getPhone(), call);
        // launch performance end
        
        if (PhoneUtils.isConferenceCall(call)) {
            // Update onscreen info for a conference call.
            updateDisplayForConference(call);
        } else {
            // Update onscreen info for a regular call (which presumably
            // has only one connection.)
            Connection conn = null;
            int phoneType = call.getPhone().getPhoneType();
            if (phoneType == Phone.PHONE_TYPE_CDMA) {
                conn = call.getLatestConnection();
            } else if ((phoneType == Phone.PHONE_TYPE_GSM)
                  || (phoneType == Phone.PHONE_TYPE_SIP)) {
                conn = call.getEarliestConnection();
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }

            if (conn == null) {
                if (DBG) log("displayMainCallStatus: connection is null, using default values.");
                // if the connection is null, we run through the behaviour
                // we had in the past, which breaks down into trivial steps
                // with the current implementation of getCallerInfo and
                // updateDisplayForPerson.
                CallerInfo info = PhoneUtils.getCallerInfo(getContext(), null /* conn */);
                updateDisplayForPerson(info, Connection.PRESENTATION_ALLOWED, false, call);
            } else {
                if (DBG) log("  - CONN: " + conn + ", state = " + conn.getState());
                int presentation = conn.getNumberPresentation();

                // make sure that we only make a new query when the current
                // callerinfo differs from what we've been requested to display.
                boolean runQuery = true;
                Object o = conn.getUserData();
                if (o instanceof PhoneUtils.CallerInfoToken) {
                    runQuery = mPhotoTracker.isDifferentImageRequest(
                            ((PhoneUtils.CallerInfoToken) o).currentInfo);
                } else {
                    runQuery = mPhotoTracker.isDifferentImageRequest(conn);
                }

                // Adding a check to see if the update was caused due to a Phone number update
                // or CNAP update. If so then we need to start a new query
                if (phoneType == Phone.PHONE_TYPE_CDMA) {
                    Object obj = conn.getUserData();
                    String updatedNumber = conn.getAddress();
                    String updatedCnapName = conn.getCnapName();
                    CallerInfo info = null;
                    if (obj instanceof PhoneUtils.CallerInfoToken) {
                        info = ((PhoneUtils.CallerInfoToken) o).currentInfo;
                    } else if (o instanceof CallerInfo) {
                        info = (CallerInfo) o;
                    }

                    if (info != null) {
                        if (updatedNumber != null && !updatedNumber.equals(info.phoneNumber)) {
                            if (DBG) log("- displayMainCallStatus: updatedNumber = "
                                    + updatedNumber);
                            runQuery = true;
                        }
                        if (updatedCnapName != null && !updatedCnapName.equals(info.cnapName)) {
                            if (DBG) log("- displayMainCallStatus: updatedCnapName = "
                                    + updatedCnapName);
                            runQuery = true;
                        }
                    }
                }

                if (runQuery) {
                    if (DBG) log("- displayMainCallStatus: starting CallerInfo query...");
                    if (mLCforUserData)
                    {                    
                        if (DBG) log("- displayMainCallStatus: the language changed to clear userdata");
                    	conn.clearUserData();
                        mLCforUserData = false;
                    }
                    PhoneUtils.CallerInfoToken info =
                            PhoneUtils.startGetCallerInfo(getContext(), conn, this, call);
                    updateDisplayForPerson(info.currentInfo, presentation, !info.isFinal, call);
                } else {
                    // No need to fire off a new query.  We do still need
                    // to update the display, though (since we might have
                    // previously been in the "conference call" state.)
                    if (DBG) log("- displayMainCallStatus: using data we already have...");
                    if (o instanceof CallerInfo) {
                        CallerInfo ci = (CallerInfo) o;
                        // Update CNAP information if Phone state change occurred
                        ci.cnapName = conn.getCnapName();
                        ci.numberPresentation = conn.getNumberPresentation();
                        ci.namePresentation = conn.getCnapNamePresentation();
                        if (DBG) log("- displayMainCallStatus: CNAP data from Connection: "
                                + "CNAP name=" + ci.cnapName
                                + ", Number/Name Presentation=" + ci.numberPresentation);
                        if (DBG) log("   ==> Got CallerInfo; updating display: ci = " + ci);
                        updateDisplayForPerson(ci, presentation, false, call);
                    } else if (o instanceof PhoneUtils.CallerInfoToken){
                        CallerInfo ci = ((PhoneUtils.CallerInfoToken) o).currentInfo;
                        if (DBG) log("- displayMainCallStatus: CNAP data from Connection: "
                                + "CNAP name=" + ci.cnapName
                                + ", Number/Name Presentation=" + ci.numberPresentation);
                        if (DBG) log("   ==> Got CallerInfoToken; updating display: ci = " + ci);
                        updateDisplayForPerson(ci, presentation, true, call);
                    } else {
                        Log.w(LOG_TAG, "displayMainCallStatus: runQuery was false, "
                              + "but we didn't have a cached CallerInfo object!  o = " + o);
                        // TODO: any easy way to recover here (given that
                        // the CallCard is probably displaying stale info
                        // right now?)  Maybe force the CallCard into the
                        // "Unknown" state?
                    }
                }
            }
        }

        // In some states we override the "photo" ImageView to be an
        // indication of the current state, rather than displaying the
        // regular photo as set above.
        updatePhotoForCallState(call);

        // One special feature of the "number" text field: For incoming
        // calls, while the user is dragging the RotarySelector widget, we
        // use mPhoneNumber to display a hint like "Rotate to answer".
        if (mRotarySelectorHintTextResId != 0) {
            // Display the hint!
            mLabelAndNumber.setText(mRotarySelectorHintTextResId);
            mLabelAndNumber.setTextColor(getResources().getColor(mRotarySelectorHintColorResId));
            if(PhoneUtils.isDMLocked())
                mLabelAndNumber.setVisibility(View.GONE);
            else
                mLabelAndNumber.setVisibility(View.VISIBLE);
        }
        // If we don't have a hint to display, just don't touch
        // mPhoneNumber and mLabel. (Their text / color / visibility have
        // already been set correctly, by either updateDisplayForPerson()
        // or updateDisplayForConference().)
    }

    /**
     * Implemented for CallerInfoAsyncQuery.OnQueryCompleteListener interface.
     * refreshes the CallCard data when it called.
     */
    public void onQueryComplete(int token, Object cookie, CallerInfo ci) {
        if (DBG) log("onQueryComplete: token " + token + ", cookie " + cookie + ", ci " + ci);
        
        //mtk71029 update for alps00040521. the query implements may be so time consuming, then the call state has changed.
        //Especially, Phone state from ringing to offhook. RingingCall is idle, it may lead display name 'unknown'
        //So, now check the call state is idle or not, if call state is idle, we do nothing.
        if (cookie instanceof Call && !((Call)cookie).isIdle() ) {
            // grab the call object and update the display for an individual call,
            // as well as the successive call to update image via call state.
            // If the object is a textview instead, we update it as we need to.
            if (DBG) log("callerinfo query complete, updating ui from displayMainCallStatus()");
            Call call = (Call) cookie;
            Connection conn = null;
            int phoneType = call.getPhone().getPhoneType();
            if (phoneType == Phone.PHONE_TYPE_CDMA) {
                conn = call.getLatestConnection();
            } else if ((phoneType == Phone.PHONE_TYPE_GSM)
                  || (phoneType == Phone.PHONE_TYPE_SIP)) {
                conn = call.getEarliestConnection();
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }
            PhoneUtils.CallerInfoToken cit =
                   PhoneUtils.startGetCallerInfo(getContext(), conn, this, null);

            int presentation = Connection.PRESENTATION_ALLOWED;
            if (conn != null) presentation = conn.getNumberPresentation();
            if (DBG) log("- onQueryComplete: presentation=" + presentation
                    + ", contactExists=" + ci.contactExists);

            // Depending on whether there was a contact match or not, we want to pass in different
            // CallerInfo (for CNAP). Therefore if ci.contactExists then use the ci passed in.
            // Otherwise, regenerate the CIT from the Connection and use the CallerInfo from there.
            if (ci.contactExists) {
                updateDisplayForPerson(ci, Connection.PRESENTATION_ALLOWED, false, call);
            } else {
                updateDisplayForPerson(cit.currentInfo, presentation, false, call);
            }
            updatePhotoForCallState(call);

        } else if (cookie instanceof TextView){
            if (DBG) log("callerinfo query complete, updating ui from ongoing or onhold");
            ((TextView) cookie).setText(PhoneUtils.getCompactNameFromCallerInfo(ci, mContext));
        }
    }

    /**
     * Implemented for ContactsAsyncHelper.OnImageLoadCompleteListener interface.
     * make sure that the call state is reflected after the image is loaded.
     */
    public void onImageLoadComplete(int token, Object cookie, ImageView iView,
            boolean imagePresent){
        if (cookie != null) {
            updatePhotoForCallState((Call) cookie);
        }
    }

    private void updateCardTitleWidgets(Phone phone, Call call) {
        long duration = 0;
        Call.State state = call.getState();
        if(state == Call.State.ACTIVE || state == Call.State.DISCONNECTING || state == Call.State.DISCONNECTED) {
            duration = CallTime.getCallDuration(call);
            updateElapsedTimeWidget(duration / 1000);
            mInCallScreen.mCallStatus.updateElapsedTimeWidgetOfCallStatus(duration / 1000);
        }
    }

    /**
     * Updates the "card title" (and also elapsed time widget) based on
     * the current state of the call.
     */
    // TODO: it's confusing for updateCardTitleWidgets() and
    // getTitleForCallCard() to be separate methods, since they both
    // just list out the exact same "phone state" cases.
    // Let's merge the getTitleForCallCard() logic into here.
    /*
    private void updateCardTitleWidgets(Phone phone, Call call) {
        if (DBG) log("updateCardTitleWidgets(call " + call + ")...");
        Call.State state = call.getState();
        Context context = getContext();

        // TODO: Still need clearer spec on exactly how title *and* status get
        // set in all states.  (Then, given that info, refactor the code
        // here to be more clear about exactly which widgets on the card
        // need to be set.)

        String cardTitle;
        int phoneType = phone.getPhoneType();
        if (phoneType == Phone.PHONE_TYPE_CDMA) {
            if (!PhoneApp.getInstance().notifier.getIsCdmaRedialCall()) {
                cardTitle = getTitleForCallCard(call);  // Normal "foreground" call card
            } else {
                cardTitle = context.getString(R.string.card_title_redialing);
            }
        } else if ((phoneType == Phone.PHONE_TYPE_GSM)
                || (phoneType == Phone.PHONE_TYPE_SIP)) {
            cardTitle = getTitleForCallCard(call);
        } else {
            throw new IllegalStateException("Unexpected phone type: " + phoneType);
        }
        if (DBG) log("updateCardTitleWidgets: " + cardTitle);

        // Update the title and elapsed time widgets based on the current call state.
        switch (state) {
            case ACTIVE:
            case DISCONNECTING:
                final boolean bluetoothActive = mApplication.showBluetoothIndication();
                int ongoingCallIcon = bluetoothActive ? R.drawable.ic_incall_ongoing_bluetooth
                        : R.drawable.ic_incall_ongoing;
                int connectedTextColor = bluetoothActive
                        ? mTextColorConnectedBluetooth : mTextColorConnected;

                if (phoneType == Phone.PHONE_TYPE_CDMA) {
                    // In normal operation we don't use an "upper title" at all,
                    // except for a couple of special cases:
                    if (mApplication.cdmaPhoneCallState.IsThreeWayCallOrigStateDialing()) {
                        // Display "Dialing" while dialing a 3Way call, even
                        // though the foreground call state is still ACTIVE.
                        setUpperTitle(cardTitle, mTextColorDefaultPrimary, state);
                    } else if (PhoneUtils.isPhoneInEcm(phone)) {
                        // In emergency callback mode (ECM), use a special title
                        // that shows your own phone number.
                        cardTitle = getECMCardTitle(context, phone);
                        setUpperTitle(cardTitle, mTextColorDefaultPrimary, state);
                    } else {
                        // Normal "ongoing call" state; don't use any "title" at all.
                        clearUpperTitle();
                    }
                } else if ((phoneType == Phone.PHONE_TYPE_GSM)
                        || (phoneType == Phone.PHONE_TYPE_SIP)) {
                    // While in the DISCONNECTING state we display a
                    // "Hanging up" message in order to make the UI feel more
                    // responsive.  (In GSM it's normal to see a delay of a
                    // couple of seconds while negotiating the disconnect with
                    // the network, so the "Hanging up" state at least lets
                    // the user know that we're doing something.)
                    // TODO: consider displaying the "Hanging up" state for
                    // CDMA also if the latency there ever gets high enough.
                    if (state == Call.State.DISCONNECTING) {
                        // Display the brief "Hanging up" indication.
                        setUpperTitle(cardTitle, mTextColorDefaultPrimary, state);
                    } else {  // state == Call.State.ACTIVE
                        // Normal "ongoing call" state; don't use any "title" at all.
                        clearUpperTitle();
                    }
                }

                // Use the elapsed time widget to show the current call duration.
                // mElapsedTime.setVisibility(View.VISIBLE);
                // mElapsedTime.setTextColor(connectedTextColor);
                
               
                
                long duration = CallTime.getCallDuration(call);  // msec
                updateElapsedTimeWidget(duration / 1000);
                mInCallScreen.mCallStatus.updateElapsedTimeWidgetOfCallStatus(duration / 1000);
                // Also see onTickForCallTimeElapsed(), which updates this
                // widget once per second while the call is active.
                break;

            case DISCONNECTED:
                // Display "Call ended" (or possibly some error indication;
                // see getCallFailedString()) in the upper title, in red.

                // TODO: display a "call ended" icon somewhere, like the old
                // R.drawable.ic_incall_end?

                setUpperTitle(cardTitle, mTextColorEnded, state);

                // In the "Call ended" state, leave the mElapsedTime widget
                // visible, but don't touch it (so  we continue to see the elapsed time of
                // the call that just ended.)
                long duration2 = CallTime.getCallDuration(call);
                updateElapsedTimeWidget(duration2 / 1000);
                mInCallScreen.mCallStatus.updateElapsedTimeWidgetOfCallStatus(duration2 / 1000);
                // mElapsedTime.setVisibility(View.VISIBLE);
                // mElapsedTime.setTextColor(mTextColorEnded);
                break;

            case HOLDING:
                // For a single call on hold, display the title "On hold" in
                // orange.
                // (But since the upper title overlaps the label of the
                // Hold/Unhold button, we actually use the elapsedTime widget
                // to display the title in this case.)

                // TODO: display an "On hold" icon somewhere, like the old
                // R.drawable.ic_incall_onhold?

                clearUpperTitle();
                //mElapsedTime.setText(cardTitle);
                // While on hold, the elapsed time widget displays an
                // "on hold" indication rather than an amount of time.
                //mElapsedTime.setVisibility(View.VISIBLE);
                //mElapsedTime.setTextColor(mTextColorOnHold);
                break;

            default:
                // All other states (DIALING, INCOMING, etc.) use the "upper title":
                setUpperTitle(cardTitle, mTextColorDefaultPrimary, state);
                // ...and we don't show the elapsed time.
                //mElapsedTime.setVisibility(View.INVISIBLE);
                break;
        }
    }
    */

    /**
     * Updates mElapsedTime based on the specified number of seconds.
     * A timeElapsed value of zero means to not show an elapsed time at all.
     */
    private void updateElapsedTimeWidget(long timeElapsed) {
        if (DBG) log("updateElapsedTimeWidget: " + timeElapsed);
        if( FeatureOption.MTK_VT3G324M_SUPPORT == true &&
        		null != mInCallScreen && null != mInCallScreen.mVTCallStatus )
        	mInCallScreen.mVTCallStatus.updateElapsedTimeWidgetOfCallStatus(timeElapsed);
    }

    /**
     * Returns the "card title" displayed at the top of a foreground
     * ("active") CallCard to indicate the current state of this call, like
     * "Dialing" or "In call" or "On hold".  A null return value means that
     * there's no title string for this state.
     */
    private String getTitleForCallCard(Call call) {
        String retVal = null;
        Call.State state = call.getState();
        Context context = getContext();

        if (DBG) log("- getTitleForCallCard(Call " + call + ")...");

        switch (state) {
            case IDLE:
                break;

            case ACTIVE:
                // Title is "Call in progress".  (Note this appears in the
                // "lower title" area of the CallCard.)
                int phoneType = call.getPhone().getPhoneType();
                if (phoneType == Phone.PHONE_TYPE_CDMA) {
                    if (mApplication.cdmaPhoneCallState.IsThreeWayCallOrigStateDialing()) {
                        retVal = context.getString(R.string.card_title_dialing);
                    } else {
                        retVal = context.getString(R.string.card_title_in_progress);
                    }
                } else if ((phoneType == Phone.PHONE_TYPE_GSM)
                        || (phoneType == Phone.PHONE_TYPE_SIP)) {
                    retVal = context.getString(R.string.card_title_in_progress);
                } else {
                    throw new IllegalStateException("Unexpected phone type: " + phoneType);
                }
                break;

            case HOLDING:
            	if(mInCallScreen.getSwappingCalls())retVal = "";
            	else retVal = context.getString(R.string.card_title_on_hold);
                // TODO: if this is a conference call on hold,
                // maybe have a special title here too?
                break;

            case DIALING:
            case ALERTING:
                retVal = context.getString(R.string.card_title_dialing);
                break;

            case INCOMING:
            case WAITING:
            	if( FeatureOption.MTK_VT3G324M_SUPPORT == true )
            	{
            		if( PhoneApp.getInstance().isVTRinging() )
            			retVal = context.getString(R.string.vt_incoming_call);
            		else
            			retVal = context.getString(R.string.card_title_incoming_call);
            	}
            	else 
            	{
            		retVal = context.getString(R.string.card_title_incoming_call);
            	}
                break;

            case DISCONNECTING:
                retVal = context.getString(R.string.card_title_hanging_up);
                break;

            case DISCONNECTED:
                retVal = getCallFailedString(call);
                break;
        }

        if (DBG) log("  ==> result: " + retVal);
        return retVal;
    }

    /**
     * Updates the "on hold" box in the "other call" info area
     * (ie. the stuff in the secondaryCallInfo block)
     * based on the specified Call.
     * Or, clear out the "on hold" box if the specified call
     * is null or idle.
     */
// launch performance start
//    private void displayOnHoldCallStatus(CallManager cm, Call call) {
//        if (DBG) log("displayOnHoldCallStatus(call =" + call + ")...");
//
//        if ((call == null) || (PhoneApp.getInstance().isOtaCallInActiveState())) {
//            //mSecondaryCallInfo.setVisibility(View.GONE);
//            return;
//        }
//
//        boolean showSecondaryCallInfo = false;
//        Call.State state = call.getState();
//        switch (state) {
//            case HOLDING:
//                // Ok, there actually is a background call on hold.
//                // Display the "on hold" box.
//
//                // Note this case occurs only on GSM devices.  (On CDMA,
//                // the "call on hold" is actually the 2nd connection of
//                // that ACTIVE call; see the ACTIVE case below.)
//
//                if (PhoneUtils.isConferenceCall(call)) {
//                    if (DBG) log("==> conference call.");
//                    mSecondaryCallName.setText(getContext().getString(R.string.confCall));
//                    showImage(mSecondaryCallPhoto, R.drawable.picture_conference);
//                } else {
//                    // perform query and update the name temporarily
//                    // make sure we hand the textview we want updated to the
//                    // callback function.
//                    if (DBG) log("==> NOT a conf call; call startGetCallerInfo...");
//                    PhoneUtils.CallerInfoToken infoToken = PhoneUtils.startGetCallerInfo(
//                            getContext(), call, this, mSecondaryCallName);
//                    if(PhoneUtils.isDMLocked()){
//                    	mSecondaryCallName.setText("");
//                    }else
//                    mSecondaryCallName.setText(
//                            PhoneUtils.getCompactNameFromCallerInfo(infoToken.currentInfo,
//                                                                    getContext()));
//
//                    // Also pull the photo out of the current CallerInfo.
//                    // (Note we assume we already have a valid photo at
//                    // this point, since *presumably* the caller-id query
//                    // was already run at some point *before* this call
//                    // got put on hold.  If there's no cached photo, just
//                    // fall back to the default "unknown" image.)
//                    if (infoToken.isFinal && !PhoneUtils.isDMLocked()) {
//                        showCachedImage(mSecondaryCallPhoto, infoToken.currentInfo);
//                    } else {
//                        showImage(mSecondaryCallPhoto, R.drawable.picture_unknown);
//                    }
//                }
//                break;
//
//            case ACTIVE:
//                // CDMA: This is because in CDMA when the user originates the second call,
//                // although the Foreground call state is still ACTIVE in reality the network
//                // put the first call on hold.
//                if (mApplication.phone.getPhoneType() == Phone.PHONE_TYPE_CDMA) {
//                    List<Connection> connections = call.getConnections();
//                    if (connections.size() > 2) {
//                        // This means that current Mobile Originated call is the not the first 3-Way
//                        // call the user is making, which in turn tells the PhoneApp that we no
//                        // longer know which previous caller/party had dropped out before the user
//                        // made this call.
//                    	if(PhoneUtils.isDMLocked()){
//                        	mSecondaryCallName.setText("");
//                        }else
//                        mSecondaryCallName.setText(
//                                getContext().getString(R.string.card_title_in_call));
//                        showImage(mSecondaryCallPhoto, R.drawable.picture_unknown);
//                    } else {
//                        // This means that the current Mobile Originated call IS the first 3-Way
//                        // and hence we display the first callers/party's info here.
//                        Connection conn = call.getEarliestConnection();
//                        PhoneUtils.CallerInfoToken infoToken = PhoneUtils.startGetCallerInfo(
//                                getContext(), conn, this, mSecondaryCallName);
//
//                        // Get the compactName to be displayed, but then check that against
//                        // the number presentation value for the call. If it's not an allowed
//                        // presentation, then display the appropriate presentation string instead.
//                        CallerInfo info = infoToken.currentInfo;
//
//                        String name = PhoneUtils.getCompactNameFromCallerInfo(info, getContext());
//                        boolean forceGenericPhoto = false;
//                        if (info != null && info.numberPresentation !=
//                                Connection.PRESENTATION_ALLOWED) {
//                            name = getPresentationString(info.numberPresentation);
//                            forceGenericPhoto = true;
//                        }
//                        if(PhoneUtils.isDMLocked()){
//                        	mSecondaryCallName.setText("");
//                        }else
//                        mSecondaryCallName.setText(name);
//
//                        // Also pull the photo out of the current CallerInfo.
//                        // (Note we assume we already have a valid photo at
//                        // this point, since *presumably* the caller-id query
//                        // was already run at some point *before* this call
//                        // got put on hold.  If there's no cached photo, just
//                        // fall back to the default "unknown" image.)
//                        if (!forceGenericPhoto && infoToken.isFinal && !PhoneUtils.isDMLocked()) {
//                            showCachedImage(mSecondaryCallPhoto, info);
//                        } else {
//                            showImage(mSecondaryCallPhoto, R.drawable.picture_unknown);
//                        }
//                    }
//                    showSecondaryCallInfo = true;
//
//                } else {
//                    // We shouldn't ever get here at all for non-CDMA devices.
//                    Log.w(LOG_TAG, "displayOnHoldCallStatus: ACTIVE state on non-CDMA device");
//                    showSecondaryCallInfo = false;
//                }
//                break;
//
//            default:
//                // There's actually no call on hold.  (Presumably this call's
//                // state is IDLE, since any other state is meaningless for the
//                // background call.)
//                showSecondaryCallInfo = false;
//                break;
//        }
//
//        if (showSecondaryCallInfo) {
//            // Ok, we have something useful to display in the "secondary
//            // call" info area.
//            mSecondaryCallInfo.setVisibility(View.VISIBLE);
//
//            // Watch out: there are some cases where we need to display the
//            // secondary call photo but *not* the two lines of text above it.
//            // Specifically, that's any state where the CallCard "upper title" is
//            // in use, since the title (e.g. "Dialing" or "Call ended") might
//            // collide with the secondaryCallStatus and secondaryCallName widgets.
//            //
//            // We detect this case by simply seeing whether or not there's any text
//            // in mUpperTitle.  (This is much simpler than detecting all possible
//            // telephony states where the "upper title" is used!  But note it does
//            // rely on the fact that updateCardTitleWidgets() gets called *earlier*
//            // than this method, in the CallCard.updateState() sequence...)
//            boolean okToShowLabels = TextUtils.isEmpty(mUpperTitle.getText());
//            mSecondaryCallName.setVisibility(okToShowLabels ? View.VISIBLE : View.INVISIBLE);
//            mSecondaryCallStatus.setVisibility(okToShowLabels ? View.VISIBLE : View.INVISIBLE);
//        } else {
//            // Hide the entire "secondary call" info area.
//            mSecondaryCallInfo.setVisibility(View.GONE);
//        }
//    }
// launch performance end

    private String getCallFailedString(Call call) {
        Connection c = call.getEarliestConnection();
        int resID;

        if (c == null) {
            if (DBG) log("getCallFailedString: connection is null, using default values.");
            // if this connection is null, just assume that the
            // default case occurs.
            resID = R.string.card_title_call_ended;
        } else {

            Connection.DisconnectCause cause = c.getDisconnectCause();

            // TODO: The card *title* should probably be "Call ended" in all
            // cases, but if the DisconnectCause was an error condition we should
            // probably also display the specific failure reason somewhere...

            switch (cause) {
                case BUSY:
                    resID = R.string.callFailed_userBusy;
                    break;

                case CONGESTION:
                case BEARER_NOT_AVAIL:
                case NO_CIRCUIT_AVAIL:
                    resID = R.string.callFailed_congestion;
                    break;

                case TIMED_OUT:
                    resID = R.string.callFailed_timedOut;
                    break;

                case SERVER_UNREACHABLE:
                    resID = R.string.callFailed_server_unreachable;
                    break;

                case NUMBER_UNREACHABLE:
                    resID = R.string.callFailed_number_unreachable;
                    break;

                case INVALID_CREDENTIALS:
                    resID = R.string.callFailed_invalid_credentials;
                    break;

                case SERVER_ERROR:
                    resID = R.string.callFailed_server_error;
                    break;

                case OUT_OF_NETWORK:
                    resID = R.string.callFailed_out_of_network;
                    break;

                case LOST_SIGNAL:
                case CDMA_DROP:
                    resID = R.string.callFailed_noSignal;
                    break;

                case LIMIT_EXCEEDED:
                    resID = R.string.callFailed_limitExceeded;
                    break;

                case POWER_OFF:
                    resID = R.string.callFailed_powerOff;
                    break;

                case ICC_ERROR:
                    resID = R.string.callFailed_simError;
                    break;

                case OUT_OF_SERVICE:
                    resID = R.string.callFailed_outOfService;
                    break;

                case INVALID_NUMBER:
                case UNOBTAINABLE_NUMBER:
                    resID = R.string.callFailed_unobtainable_number;
                    break;

                default:
                    resID = R.string.card_title_call_ended;
                    break;
            }
        }
        return getContext().getString(resID);
    }

    /**
     * Updates the name / photo / number / label fields on the CallCard
     * based on the specified CallerInfo.
     *
     * If the current call is a conference call, use
     * updateDisplayForConference() instead.
     */
    /*private*/ void updateDisplayForPerson(CallerInfo info,
                                        int presentation,
                                        boolean isTemporary,
                                        Call call) {
        if (DBG) log("updateDisplayForPerson(" + info + ")\npresentation:" +
                     presentation + " isTemporary:" + isTemporary);

        // inform the state machine that we are displaying a photo.
        mPhotoTracker.setPhotoRequest(info);
        mPhotoTracker.setPhotoState(ContactsAsyncHelper.ImageTracker.DISPLAY_IMAGE);

        // The actual strings we're going to display onscreen:
        String displayName = null;
        String displayNumber = null;
        String label = null;
        Uri personUri = null;
        String socialStatusText = null;
        Drawable socialStatusBadge = null;

        if (info != null) {
            // It appears that there is a small change in behaviour with the
            // PhoneUtils' startGetCallerInfo whereby if we query with an
            // empty number, we will get a valid CallerInfo object, but with
            // fields that are all null, and the isTemporary boolean input
            // parameter as true.

            // In the past, we would see a NULL callerinfo object, but this
            // ends up causing null pointer exceptions elsewhere down the
            // line in other cases, so we need to make this fix instead. It
            // appears that this was the ONLY call to PhoneUtils
            // .getCallerInfo() that relied on a NULL CallerInfo to indicate
            // an unknown contact.

            // when language changed if in emergency call, the String 
            // "Emergency number" should be updated.
            if(mLocaleChanged == true) {
                if(info.isEmergencyNumber() == true) {
                    info.phoneNumber = getContext().getString(
                            com.android.internal.R.string.emergency_call_dialog_number_for_display);
                }
                mLocaleChanged = false;
            }

            // Currently, info.phoneNumber may actually be a SIP address, and
            // if so, it might sometimes include the "sip:" prefix.  That
            // prefix isn't really useful to the user, though, so strip it off
            // if present.  (For any other URI scheme, though, leave the
            // prefix alone.)
            // TODO: It would be cleaner for CallerInfo to explicitly support
            // SIP addresses instead of overloading the "phoneNumber" field.
            // Then we could remove this hack, and instead ask the CallerInfo
            // for a "user visible" form of the SIP address.
            String number = info.phoneNumber;
            if ((number != null) && number.startsWith("sip:")) {
                number = number.substring(4);
            }

            if (TextUtils.isEmpty(info.name)) {
                // No valid "name" in the CallerInfo, so fall back to
                // something else.
                // (Typically, we promote the phone number up to the "name"
                // slot onscreen, and leave the "number" slot empty.)
                if (TextUtils.isEmpty(number)) {
                    displayName =  getPresentationString(presentation);
                } else if (presentation != Connection.PRESENTATION_ALLOWED) {
                    // This case should never happen since the network should never send a phone #
                    // AND a restricted presentation. However we leave it here in case of weird
                    // network behavior
                    displayName = getPresentationString(presentation);
                } else if (!TextUtils.isEmpty(info.cnapName)) {
                    displayName = info.cnapName;
                    info.name = info.cnapName;
                    // CR:129572
                    if(number != null)
                        displayNumber = PhoneNumberFormatUtilEx.formatNumber(number);//PhoneNumberUtils.formatNumber(number);


                } else {
                    // CR:129572
                    if(number != null) {
                        if(!PhoneUtils.isEccCall(call))
                            displayName = PhoneNumberFormatUtilEx.formatNumber(number);
                        else
                            displayName = number;
                    }
                }
            } else {
                // We do have a valid "name" in the CallerInfo.  Display that
                // in the "name" slot, and the phone number in the "number" slot.
                if (presentation != Connection.PRESENTATION_ALLOWED) {
                    // This case should never happen since the network should never send a name
                    // AND a restricted presentation. However we leave it here in case of weird
                    // network behavior
                    displayName = getPresentationString(presentation);
                } else {
                    displayName = info.name;
                    // CR:129572
                    if(number != null)
                        displayNumber = PhoneNumberFormatUtilEx.formatNumber(number);//PhoneNumberUtils.formatNumber(number);
                    label = info.phoneLabel;
                }
            }
            personUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, info.person_id);
            if (DBG) log("- got personUri: '" + personUri
                         + "', based on info.person_id: " + info.person_id);
        } else {
            displayName =  getPresentationString(presentation);
        }

        // when the number is an emergency number, the voice recorder is disabled, so do not need to limit the max width
        if(info.isEmergencyNumber())
            mName.setMaxWidth(Integer.MAX_VALUE);
        else
            mName.setMaxWidth(getResources().getDimensionPixelSize(R.dimen.incall_card_name_max_length));

        if (call.isGeneric()) {
            mName.setText(R.string.card_title_in_call);
        } else {
            mName.setText(displayName);
        }

        if(PhoneUtils.isDMLocked())
            mName.setVisibility(View.GONE);
        else
            mName.setVisibility(View.VISIBLE);

        // Update mPhoto
        // if the temporary flag is set, we know we'll be getting another call after
        // the CallerInfo has been correctly updated.  So, we can skip the image
        // loading until then.

        // If the photoResource is filled in for the CallerInfo, (like with the
        // Emergency Number case), then we can just set the photo image without
        // requesting for an image load. Please refer to CallerInfoAsyncQuery.java
        // for cases where CallerInfo.photoResource may be set.  We can also avoid
        // the image load step if the image data is cached.
        if (isTemporary && (info == null || !info.isCachedPhotoCurrent)) {
            mPhoto.setVisibility(View.INVISIBLE);
        } else if (info != null && info.photoResource != 0){
            showImage(mPhoto, info.photoResource);
        } else if (!showCachedImage(mPhoto, info)) {
            // Load the image with a callback to update the image state.
            // Use the default unknown picture while the query is running.
            ContactsAsyncHelper.updateImageViewWithContactPhotoAsync(
                info, 0, this, call, getContext(), mPhoto, personUri, R.drawable.picture_unknown);
        }
        // And no matter what, on all devices, we never see the "manage
        // conference" button in this state.
//        launch performance start
//        mManageConferencePhotoButton.setVisibility(View.INVISIBLE);
//        launch performance end
        if(mManageConferenceUiButton != null)
            mManageConferenceUiButton.setVisibility(View.INVISIBLE);

        // label and number
        mStringBuilder.delete(0, mStringBuilder.length());
        if(!PhoneUtils.isDMLocked()) {
            if (label != null && !call.isGeneric()) {
                if (label.length() > 10)
                    mStringBuilder.append(label, 0, 9);
                else
                    mStringBuilder.append(label);
            }
            if(mStringBuilder.length() > 0)
                mStringBuilder.append("   ");
            if (displayNumber != null && !call.isGeneric())
                mStringBuilder.append(displayNumber);
            mLabelAndNumber.setText(mStringBuilder.toString());
            mLabelAndNumber.setVisibility(View.VISIBLE);
        } else
            mLabelAndNumber.setVisibility(View.GONE);

        // Other text fields:
        updateCallTypeLabel(call);
        updateSocialStatus(socialStatusText, socialStatusBadge, call);  // Currently unused

        if (FeatureOption.MTK_VT3G324M_SUPPORT && null != mInCallScreen) {
            if ((mInCallScreen.getVTScreenMode() == VTCallUtils.VTScreenMode.VT_SCREEN_OPEN) && (info != null)) {
                String vtPhoneNumber = "";
                if (info.name != null && info.name.length() > 0)
                    vtPhoneNumber += mName.getText().toString();
                
                if (label != null && label.length() > 0)
                    vtPhoneNumber += " " + label;
                
                if (info.phoneNumber != null && info.phoneNumber.length() > 0)
                    vtPhoneNumber += " " + PhoneNumberFormatUtilEx.formatNumber(info.phoneNumber);
                vtPhoneNumber = "\u202D" + vtPhoneNumber + "\u202C";
                mInCallScreen.updateVTPhoneNumber(vtPhoneNumber);
            }
       }
    }

    private String getPresentationString(int presentation) {
        String name = getContext().getString(R.string.unknown);
        if (presentation == Connection.PRESENTATION_RESTRICTED) {
            name = getContext().getString(R.string.private_num);
        } else if (presentation == Connection.PRESENTATION_PAYPHONE) {
            name = getContext().getString(R.string.payphone);
        }
        return name;
    }

    /**
     * Updates the name / photo / number / label fields
     * for the special "conference call" state.
     *
     * If the current call has only a single connection, use
     * updateDisplayForPerson() instead.
     */
    private void updateDisplayForConference(Call call) {
        if (DBG) log("updateDisplayForConference()...");

        int phoneType = call.getPhone().getPhoneType();
        if (phoneType == Phone.PHONE_TYPE_CDMA) {
            // This state corresponds to both 3-Way merged call and
            // Call Waiting accepted call.
            // In this case we display the UI in a "generic" state, with
            // the generic "dialing" icon and no caller information,
            // because in this state in CDMA the user does not really know
            // which caller party he is talking to.
            showImage(mPhoto, R.drawable.picture_dialing);
            mName.setText(R.string.card_title_in_call);
        } else if ((phoneType == Phone.PHONE_TYPE_GSM)
                || (phoneType == Phone.PHONE_TYPE_SIP)) {
//            launch performance start
//            if (mInCallScreen.isTouchUiEnabled()) {
//                // Display the "manage conference" button in place of the photo.
//                mManageConferencePhotoButton.setVisibility(View.VISIBLE);
//                if(mManageConferenceUiButton != null)
//                    mManageConferenceUiButton.setVisibility(View.VISIBLE);
//                mPhoto.setVisibility(View.INVISIBLE);  // Not GONE, since that would break
//                                                       // other views in our RelativeLayout.
//                mLabel.setText(mInCallScreen.getString(R.string.incall_conference_members, call.getConnections().size()));
//
//            } else {
//                // Display the "conference call" image in the photo slot,
//                // with no other information.
//                showImage(mPhoto, R.drawable.picture_conference);
//            }
//            launch performance end
            showImage(mPhoto, R.drawable.picture_conference);
            mLabelAndNumber.setText(mInCallScreen.getString(R.string.incall_conference_members, call.getConnections().size()));
            if (mManageConferenceUiButton != null)
                mManageConferenceUiButton.setVisibility(View.VISIBLE);
            mName.setText(R.string.card_title_conf_call);
        } else {
            throw new IllegalStateException("Unexpected phone type: " + phoneType);
        }

        int maxLength = (int)getContext().getResources().getDimension(R.dimen.incall_card_name_max_length);
        mName.setMaxWidth(maxLength);

        if(PhoneUtils.isDMLocked())mName.setVisibility(View.GONE);
        else mName.setVisibility(View.VISIBLE);

        // TODO: For a conference call, the "phone number" slot is specced
        // to contain a summary of who's on the call, like "Bill Foldes
        // and Hazel Nutt" or "Bill Foldes and 2 others".
        // But for now, just hide it:
        //mPhoneNumber.setVisibility(View.GONE);
        /* Added by xingping.zheng start */
        //mLabel.setVisibility(View.GONE);
        mLabelAndNumber.setVisibility(View.VISIBLE);
        /* Added by xingping.zheng end   */

        // Other text fields:
        updateCallTypeLabel(call);
        updateSocialStatus(null, null, null);  // socialStatus is never visible in this state

        // TODO: for a GSM conference call, since we do actually know who
        // you're talking to, consider also showing names / numbers /
        // photos of some of the people on the conference here, so you can
        // see that info without having to click "Manage conference".  We
        // probably have enough space to show info for 2 people, at least.
        //
        // To do this, our caller would pass us the activeConnections
        // list, and we'd call PhoneUtils.getCallerInfo() separately for
        // each connection.
    }

    /**
     * Updates the CallCard "photo" IFF the specified Call is in a state
     * that needs a special photo (like "busy" or "dialing".)
     *
     * If the current call does not require a special image in the "photo"
     * slot onscreen, don't do anything, since presumably the photo image
     * has already been set (to the photo of the person we're talking, or
     * the generic "picture_unknown" image, or the "conference call"
     * image.)
     */
    private void updatePhotoForCallState(Call call) {
        if (DBG) log("updatePhotoForCallState(" + call + ")...");
        int photoImageResource = 0;

        // Check for the (relatively few) telephony states that need a
        // special image in the "photo" slot.
        Call.State state = call.getState();
        switch (state) {
            case DISCONNECTED:
                // Display the special "busy" photo for BUSY or CONGESTION.
                // Otherwise (presumably the normal "call ended" state)
                // leave the photo alone.
                Connection c = call.getEarliestConnection();
                // if the connection is null, we assume the default case,
                // otherwise update the image resource normally.
                if (c != null) {
                    Connection.DisconnectCause cause = c.getDisconnectCause();
                    if ((cause == Connection.DisconnectCause.BUSY)
                        || (cause == Connection.DisconnectCause.CONGESTION)
                        || (cause == Connection.DisconnectCause.BEARER_NOT_AVAIL)
                        || (cause == Connection.DisconnectCause.NO_CIRCUIT_AVAIL)) {
                        photoImageResource = R.drawable.picture_busy;
                    }
                } else if (DBG) {
                    log("updatePhotoForCallState: connection is null, ignoring.");
                }

                // TODO: add special images for any other DisconnectCauses?
                break;

            case ALERTING:
            case DIALING:
            default:
                // Leave the photo alone in all other states.
                // If this call is an individual call, and the image is currently
                // displaying a state, (rather than a photo), we'll need to update
                // the image.
                // This is for the case where we've been displaying the state and
                // now we need to restore the photo.  This can happen because we
                // only query the CallerInfo once, and limit the number of times
                // the image is loaded. (So a state image may overwrite the photo
                // and we would otherwise have no way of displaying the photo when
                // the state goes away.)

                // if the photoResource field is filled-in in the Connection's
                // caller info, then we can just use that instead of requesting
                // for a photo load.

                // look for the photoResource if it is available.
                CallerInfo ci = null;
                {
                    Connection conn = null;
                    int phoneType = call.getPhone().getPhoneType();
                    if (phoneType == Phone.PHONE_TYPE_CDMA) {
                        conn = call.getLatestConnection();
                    } else if ((phoneType == Phone.PHONE_TYPE_GSM)
                            || (phoneType == Phone.PHONE_TYPE_SIP)) {
                        conn = call.getEarliestConnection();
                    } else {
                        throw new IllegalStateException("Unexpected phone type: " + phoneType);
                    }

                    if (conn != null) {
                        Object o = conn.getUserData();
                        if (o instanceof CallerInfo) {
                            ci = (CallerInfo) o;
                        } else if (o instanceof PhoneUtils.CallerInfoToken) {
                            ci = ((PhoneUtils.CallerInfoToken) o).currentInfo;
                        }
                    }
                }

                //via liaobz start
                Log.i(LOG_TAG,"start:"+ci);
                Log.i(LOG_TAG,"start:"+(ci!=null));
                byte[] imageBytes=null;
                if (ci != null) {
                    //1.according phoneNumber,find id or null from emergencyphb
                    //2.if id not null.find avatar from emergencyphb
                    //3.if id is null.according num find contact_id from sys db(dont know what is person_id)
                    //4.if id not null.get avatar from sys db
                    long contactId = -1;
                    SQLiteDatabase db = null;
                    Cursor cursor = null;
                    String databaseFilename = "/data/data/com.az.Main/databases/emergencyphb.db";
                    try{
                        //step 1
                        try{
                            db = getContext().openOrCreateDatabase(databaseFilename,Context.MODE_WORLD_WRITEABLE + Context.MODE_WORLD_READABLE,null);
                            Log.i(LOG_TAG,"step 1:" + db);
                            cursor=db.query("emerphb", new String[]{"_id"}, " phonenum = '" + ci.phoneNumber + "' ", null, null, null, null);
                            Log.i(LOG_TAG,"step 1:" + cursor);
                            if(cursor!=null){
                                if(cursor.moveToFirst()){
                                    contactId = cursor.getInt(cursor.getColumnIndex("_id"));
                                }
                                cursor.close();
                            }
                        } catch (Exception excp) {}
                        Log.i(LOG_TAG,"step 1:" + contactId);
                        //step 2
                        if(contactId > 0){
                            Log.i(LOG_TAG,"step 2:" + contactId);
                            databaseFilename = "/data/data/com.android.contacts/databases/contactphoto.db";
                            db = getContext().openOrCreateDatabase(databaseFilename,Context.MODE_WORLD_WRITEABLE + Context.MODE_WORLD_READABLE,null);
                            cursor=db.rawQuery("select image from emergencyinfo where contact_id=" + contactId, null);
                            Log.i(LOG_TAG,"step 2:" + cursor);
                            if(cursor!=null){
                                if(cursor.moveToFirst()){
                                    imageBytes=cursor.getBlob(cursor.getColumnIndex("image"));
                                }
                            }
                            cursor.close();
                        }
                        else{
                            //step 3
                            Log.i(LOG_TAG,"step 3:" + contactId);
                            cursor=getContext().getContentResolver().query(Contacts.CONTENT_URI, new String[]{"_id"}, null, null, null);
                            if(cursor!=null){
                                if(cursor.moveToFirst()){
                                    contactId = cursor.getInt(cursor.getColumnIndex("_id"));
                                }
                                cursor.close();
                            }
                            Log.i(LOG_TAG,"step 3:" + contactId);
                            if(contactId > 0){
                                //step 4
                                databaseFilename = "/data/data/com.android.contacts/databases/contactphoto.db";
                                db = getContext().openOrCreateDatabase(databaseFilename,Context.MODE_WORLD_WRITEABLE + Context.MODE_WORLD_READABLE,null);
                                cursor=db.rawQuery("select image from contacttbl where contact_id=" + ci.person_id, null);
                                Log.i(LOG_TAG,"step 4:" + cursor);
                                if(cursor!=null){
                                    if(cursor.moveToFirst()){
                                        imageBytes=cursor.getBlob(cursor.getColumnIndex("image"));
                                        Log.i(LOG_TAG,"step 4:" + imageBytes);
                                    }
                                }
                                cursor.close();
                            }
                        }
                    } catch(Exception e) {
                        e.printStackTrace();
                    } finally {
                        try{db.close();}catch(Exception ex){}
                    }
                    //photoImageResource = ci.photoResource;
                    //via liaobz end
                }

                // If no photoResource found, check to see if this is a conference call. If
                // it is not a conference call:
                //   1. Try to show the cached image
                //   2. If the image is not cached, check to see if a load request has been
                //      made already.
                //   3. If the load request has not been made [DISPLAY_DEFAULT], start the
                //      request and note that it has started by updating photo state with
                //      [DISPLAY_IMAGE].
                // Load requests started in (3) use a placeholder image of -1 to hide the
                // image by default.  Please refer to CallerInfoAsyncQuery.java for cases
                // where CallerInfo.photoResource may be set.
                if (photoImageResource == 0) {
                    if (!PhoneUtils.isConferenceCall(call)) {
                        if (!showCachedImage(mPhoto, ci) && (mPhotoTracker.getPhotoState() ==
                                ContactsAsyncHelper.ImageTracker.DISPLAY_DEFAULT)) {
                            ContactsAsyncHelper.updateImageViewWithContactPhotoAsync(ci,
                                    getContext(), mPhoto, mPhotoTracker.getPhotoUri(), -1);
                            mPhotoTracker.setPhotoState(
                                    ContactsAsyncHelper.ImageTracker.DISPLAY_IMAGE);
                        }
                    }
                } else {
                    showImage(mPhoto, photoImageResource);
                    mPhotoTracker.setPhotoState(ContactsAsyncHelper.ImageTracker.DISPLAY_IMAGE);
                    //return;
                }
                // liaobz
                if(imageBytes != null)
                    mPhoto.setImageBitmap(BitmapFactory.decodeByteArray(imageBytes,0,imageBytes.length));
                break;
        }

        if (photoImageResource != 0) {
            if (DBG) log("- overrriding photo image: " + photoImageResource);
            showImage(mPhoto, photoImageResource);
            // Track the image state.
            mPhotoTracker.setPhotoState(ContactsAsyncHelper.ImageTracker.DISPLAY_DEFAULT);
        }
    }

    /**
     * Try to display the cached image from the callerinfo object.
     *
     *  @return true if we were able to find the image in the cache, false otherwise.
     */
    private static final boolean showCachedImage(ImageView view, CallerInfo ci) {
        if ((ci != null) && ci.isCachedPhotoCurrent) {
            if (ci.cachedPhoto != null) {
                showImage(view, ci.cachedPhoto);
            } else {
                showImage(view, R.drawable.picture_unknown);
            }
            return true;
        }
        showImage(view, R.drawable.picture_unknown);
        return false;
    }

    /** Helper function to display the resource in the imageview AND ensure its visibility.*/
    private static final void showImage(ImageView view, int resource) {
        view.setImageResource(resource);
        view.setVisibility(View.VISIBLE);
    }

    /** Helper function to display the drawable in the imageview AND ensure its visibility.*/
    private static final void showImage(ImageView view, Drawable drawable) {
        view.setImageDrawable(drawable);
        view.setVisibility(View.VISIBLE);
    }

    /**
     * Returns the "Menu button hint" TextView (which is manipulated
     * directly by the InCallScreen.)
     * @see InCallScreen.updateMenuButtonHint()
     */
    /* package */ TextView getMenuButtonHint() {
        return mMenuButtonHint;
    }

    /**
     * Sets the left and right margins of the specified ViewGroup (whose
     * LayoutParams object which must inherit from
     * ViewGroup.MarginLayoutParams.)
     *
     * TODO: Is there already a convenience method like this somewhere?
     */
    private void setSideMargins(ViewGroup vg, int margin) {
        ViewGroup.MarginLayoutParams lp =
                (ViewGroup.MarginLayoutParams) vg.getLayoutParams();
        // Equivalent to setting android:layout_marginLeft/Right in XML
        lp.leftMargin = margin;
        lp.rightMargin = margin;
        vg.setLayoutParams(lp);
    }

    /**
     * Sets the CallCard "upper title".  Also, depending on the passed-in
     * Call state, possibly display an icon along with the title.
     */
//    launch performance start
//    private void setUpperTitle(String title, int color, Call.State state) {
//        mUpperTitle.setText(title);
//        mUpperTitle.setTextColor(color);
//
//        int bluetoothIconId = 0;
//        if (!TextUtils.isEmpty(title)
//                && ((state == Call.State.INCOMING) || (state == Call.State.WAITING))
//                && mApplication.showBluetoothIndication()) {
//            // Display the special bluetooth icon also, if this is an incoming
//            // call and the audio will be routed to bluetooth.
//            bluetoothIconId = R.drawable.ic_incoming_call_bluetooth;
//        }
//
//        mUpperTitle.setCompoundDrawablesWithIntrinsicBounds(bluetoothIconId, 0, 0, 0);
//        if (bluetoothIconId != 0) mUpperTitle.setCompoundDrawablePadding((int) (mDensity * 5));
//
//        //xingping.zheng add
//        if(PhoneApp.getInstance().isQVGAPlusQwerty())
//        {
//            if(TextUtils.isEmpty(title))
//                mUpperTitle.setVisibility(View.GONE);
//            else
//                mUpperTitle.setVisibility(View.VISIBLE);
//        }
//    }
//    launch performance end

    /**
     * Clears the CallCard "upper title", for states (like a normal
     * ongoing call) where we don't use any "title" at all.
     */
//    launch performance start
//    private void clearUpperTitle() {
//        setUpperTitle("", 0, Call.State.IDLE);  // Use dummy values for "color" and "state"
//    }
//    launch performance end

    /**
     * Returns the special card title used in emergency callback mode (ECM),
     * which shows your own phone number.
     */
    private String getECMCardTitle(Context context, Phone phone) {
        String rawNumber = phone.getLine1Number();  // may be null or empty
        String formattedNumber;
        if (!TextUtils.isEmpty(rawNumber)) {
            formattedNumber = PhoneNumberUtils.formatNumber(rawNumber);
        } else {
            formattedNumber = context.getString(R.string.unknown);
        }
        String titleFormat = context.getString(R.string.card_title_my_phone_number);
        return String.format(titleFormat, formattedNumber);
    }
    
    public boolean isSpecialWidthAndHeight(int width, int height) {
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display mDisplay = windowManager.getDefaultDisplay();
        final int mWidth = mDisplay.getWidth();
        final int mHeight = mDisplay.getHeight();
        return ((width == mWidth) && (height == mHeight)) || ((height == mWidth) && (width == mHeight));
    }

    /**
     * Updates the "Call type" label, based on the current foreground call.
     * This is a special label and/or branding we display for certain
     * kinds of calls.
     *
     * (So far, this is used only for SIP calls, which get an
     * "Internet call" label.  TODO: But eventually, the telephony
     * layer might allow each pluggable "provider" to specify a string
     * and/or icon to be displayed here.)
     */
    private void updateCallTypeLabel(Call call) {
        int phoneType = (call != null) ? call.getPhone().getPhoneType() : Phone.PHONE_TYPE_NONE;
        if (phoneType == Phone.PHONE_TYPE_SIP && !isSpecialWidthAndHeight(QVGA_WIDTH, QVGA_HEIGHT)) {
        	try
        	{
        		mCallTypeLabel.setVisibility(View.VISIBLE);
        		mCallTypeLabel.setText(R.string.incall_call_type_label_sip);
        		mCallTypeLabel.setTextColor(mTextColorCallTypeSip);
        		// If desired, we could also display a "badge" next to the label, as follows:
        		//   mCallTypeLabel.setCompoundDrawablesWithIntrinsicBounds(
        		//           callTypeSpecificBadge, null, null, null);
        		//   mCallTypeLabel.setCompoundDrawablePadding((int) (mDensity * 6));
        	}catch(Exception ex)
        	{
        		if (DBG) log("updateCallTypeLabel : can not find the mCallTypeLabel !");
        	}
        } else {
        	try
        	{
        		mCallTypeLabel.setVisibility(View.GONE);
        	}catch(Exception ex)
        	{
        		if (DBG) log("updateCallTypeLabel : can not find the mCallTypeLabel !");
        	}
        }
    }

    /**
     * Updates the "social status" label with the specified text and
     * (optional) badge.
     */
    private void updateSocialStatus(String socialStatusText,
                                    Drawable socialStatusBadge,
                                    Call call) {
        // The socialStatus field is *only* visible while an incoming call
        // is ringing, never in any other call state.
        if ((socialStatusText != null)
                && (call != null)
                && call.isRinging()
                && !call.isGeneric()) {
            mSocialStatus.setVisibility(View.VISIBLE);
            mSocialStatus.setText(socialStatusText);
            mSocialStatus.setCompoundDrawablesWithIntrinsicBounds(
                    socialStatusBadge, null, null, null);
            mSocialStatus.setCompoundDrawablePadding((int) (mDensity * 6));
        } else {
            mSocialStatus.setVisibility(View.GONE);
        }
    }

    /**
     * Hides the top-level UI elements of the call card:  The "main
     * call card" element representing the current active or ringing call,
     * and also the info areas for "ongoing" or "on hold" calls in some
     * states.
     *
     * This is intended to be used in special states where the normal
     * in-call UI is totally replaced by some other UI, like OTA mode on a
     * CDMA device.
     *
     * To bring back the regular CallCard UI, just re-run the normal
     * updateState() call sequence.
     */
    public void hideCallCardElements() {
        //mPrimaryCallInfo.setVisibility(View.GONE);
        mPhoto.setVisibility(View.GONE);
        mManageConferenceUiButton.setVisibility(View.GONE);
        mName.setVisibility(View.GONE);
        mLabelAndNumber.setVisibility(View.GONE);
//        launch performance start
//        mSecondaryCallInfo.setVisibility(View.GONE);
//        launch performance end
    }

    /*
     * Updates the hint (like "Rotate to answer") that we display while
     * the user is dragging the incoming call RotarySelector widget.
     */
    /* package */ void setRotarySelectorHint(int hintTextResId, int hintColorResId) {
        mRotarySelectorHintTextResId = hintTextResId;
        mRotarySelectorHintColorResId = hintColorResId;
    }

    // View.OnClickListener implementation
    public void onClick(View view) {
        int id = view.getId();
        if (DBG) log("onClick(View " + view + ", id " + id + ")...");

        switch (id) {
            case R.id.manageConferencePhotoButton:
            /* Added by xingping.zheng start */
            case R.id.manageConferenceUiButton:
            /* Added by xingping.zheng end */
                // A click on anything here gets forwarded
                // straight to the InCallScreen.
                mInCallScreen.handleOnscreenButtonClick(id);
                break;

            default:
                if(DBG) Log.w(LOG_TAG, "onClick: unexpected click: View " + view + ", id " + id);
                break;
        }
    }

    // Accessibility event support.
    // Since none of the CallCard elements are focusable, we need to manually
    // fill in the AccessibilityEvent here (so that the name / number / etc will
    // get pronounced by a screen reader, for example.)
    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
//        dispatchPopulateAccessibilityEvent(event, mUpperTitle);
        dispatchPopulateAccessibilityEvent(event, mPhoto);
        dispatchPopulateAccessibilityEvent(event, mName);
        dispatchPopulateAccessibilityEvent(event, mSocialStatus);
//        launch performance start
//        dispatchPopulateAccessibilityEvent(event, mManageConferencePhotoButton);
//        dispatchPopulateAccessibilityEvent(event, mPhoneNumber);
//        dispatchPopulateAccessibilityEvent(event, mLabel);
//        dispatchPopulateAccessibilityEvent(event, mSecondaryCallName);
//        dispatchPopulateAccessibilityEvent(event, mSecondaryCallStatus);
//        dispatchPopulateAccessibilityEvent(event, mSecondaryCallPhoto);
//        launch performance end
        return true;
    }

    private void dispatchPopulateAccessibilityEvent(AccessibilityEvent event, View view) {
        List<CharSequence> eventText = event.getText();
        int size = eventText.size();
        view.dispatchPopulateAccessibilityEvent(event);
        // if no text added write null to keep relative position
        if (size == eventText.size()) {
            eventText.add(null);
        }
    }


    // Debugging / testing code

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
