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

package com.android.mms.data;

import java.util.List;
import java.util.ArrayList;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.telephony.SmsMessage;
import android.provider.Telephony.MmsSms.WordsTable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.common.userhappiness.UserHappinessSignals;
import com.android.mms.ContentRestrictionException;
import com.android.mms.ExceedMessageSizeException;
import com.android.mms.LogTag;
import com.android.mms.MmsConfig;
import com.android.mms.ResolutionException;
import com.android.mms.RestrictedResolutionException;
import com.android.mms.UnsupportContentTypeException;
import com.android.mms.model.AudioModel;
import com.android.mms.model.FileAttachmentModel;
import com.android.mms.model.ImageModel;
import com.android.mms.model.MediaModel;
import com.android.mms.model.SlideModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.model.TextModel;
import com.android.mms.model.VCardModel;
import com.android.mms.model.VCalendarModel;
import com.android.mms.model.VideoModel;
import com.android.mms.transaction.MessageSender;
import com.android.mms.transaction.MmsMessageSender;
import com.android.mms.transaction.SendTransaction;
import com.android.mms.transaction.SmsMessageSender;
import com.android.mms.transaction.SmsReceiverService;
import com.android.mms.ui.ComposeMessageActivity;
//MTK_OP02_PROTECT_START
import com.android.mms.ui.ComposeMessageActivityMini;
//MTK_OP02_PROTECT_END
import com.android.mms.ui.MessageUtils;
import com.android.mms.ui.MessagingPreferenceActivity;
import com.android.mms.ui.SlideEditorActivity;
import com.android.mms.ui.SlideshowEditor;
import com.android.mms.ui.UriImage;
import com.android.mms.util.Recycler;
import com.google.android.mms.ContentType;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.SendReq;
import com.android.mms.MmsApp;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.provider.Telephony.Threads;
import com.mediatek.xlog.Xlog;
import com.mediatek.xlog.SXlog;


/**
 * Contains all state related to a message being edited by the user.
 */
public class WorkingMessage {
    private static final String TAG = "WorkingMessage";
    private static final boolean DEBUG = false;

    // Public intents
    public static final String ACTION_SENDING_SMS = "android.intent.action.SENDING_SMS";

    // Intent extras
    public static final String EXTRA_SMS_MESSAGE = "android.mms.extra.MESSAGE";
    public static final String EXTRA_SMS_RECIPIENTS = "android.mms.extra.RECIPIENTS";
    public static final String EXTRA_SMS_THREAD_ID = "android.mms.extra.THREAD_ID";

    //Creation mode.
    private final static String CREATION_MODE_RESTRICTED = "RESTRICTED";
    private final static String CREATION_MODE_WARNING    = "WARNING";
    private final static String CREATION_MODE_FREE       = "FREE";

    // Database access stuff
    private final Activity mActivity;
    private final ContentResolver mContentResolver;

    // States that can require us to save or send a message as MMS.
    private static final int RECIPIENTS_REQUIRE_MMS = (1 << 0);     // 1
    private static final int HAS_SUBJECT = (1 << 1);                // 2
    private static final int HAS_ATTACHMENT = (1 << 2);             // 4
    private static final int LENGTH_REQUIRES_MMS = (1 << 3);        // 8
    private static final int FORCE_MMS = (1 << 4);                  // 16

    // A bitmap of the above indicating different properties of the message;
    // any bit set will require the message to be sent via MMS.
    private int mMmsState;

    // Errors from setAttachment()
    public static final int OK = 0;
    public static final int UNKNOWN_ERROR = -1;
    public static final int MESSAGE_SIZE_EXCEEDED = -2;
    public static final int UNSUPPORTED_TYPE = -3;
    public static final int IMAGE_TOO_LARGE = -4;
    public static final int WARNING_TYPE    = -10;
    public static final int RESTRICTED_TYPE = -11;
    public static final int RESTRICTED_RESOLUTION = -12;

    // Attachment types
    public static final int TEXT = 0;
    public static final int IMAGE = 1;
    public static final int VIDEO = 2;
    public static final int AUDIO = 3;
    public static final int SLIDESHOW = 4;
    public static final int ATTACHMENT = 5;
    public static final int VCARD = 6; 
    public static final int VCALENDAR = 7;
    
    public boolean mHasDrmPart = false;
    public boolean mHasDrmRight = false;
    private static final int CLASSNUMBERMINI = 130;
    // Current attachment type of the message; one of the above values.
    private int mAttachmentType;

    // Conversation this message is targeting.
    private Conversation mConversation;

    // Text of the message.
    private CharSequence mText;
    // Slideshow for this message, if applicable.  If it's a simple attachment,
    // i.e. not SLIDESHOW, it will contain only one slide.
    private SlideshowModel mSlideshow;
    // Data URI of an MMS message if we have had to save it.
    private Uri mMessageUri;
    // MMS subject line for this message
    private CharSequence mSubject;

    // Set to true if this message has been discarded.
    private boolean mDiscarded = false;

    // Cached value of mms enabled flag
    private static boolean sMmsEnabled = MmsConfig.getMmsEnabled();

    // Our callback interface
    private final MessageStatusListener mStatusListener;
    private List<String> mWorkingRecipients;

    //Set resizedto true if the image is 
    private boolean mResizeImage = false;
    // Message sizes in Outbox
    private static final String[] MMS_OUTBOX_PROJECTION = {
        Mms._ID,            // 0
        Mms.MESSAGE_SIZE,   // 1
        Mms.STATUS
    };

    private static final int MMS_MESSAGE_SIZE_INDEX  = 1;
    private static final int MMS_MESSAGE_STATUS_INDEX  = 2;

    public static  int sCreationMode  = 0;

    private static int sMessageClassMini = 0;
    /**
     * Callback interface for communicating important state changes back to
     * ComposeMessageActivity.
     */
    public interface MessageStatusListener {
        /**
         * Called when the protocol for sending the message changes from SMS
         * to MMS, and vice versa.
         *
         * @param mms If true, it changed to MMS.  If false, to SMS.
         */
        void onProtocolChanged(boolean mms, boolean needToast);

        /**
         * Called when an attachment on the message has changed.
         */
        void onAttachmentChanged();

        /**
         * Called just before the process of sending a message.
         */
        void onPreMessageSent();

        /**
         * Called once the process of sending a message, triggered by
         * {@link send} has completed. This doesn't mean the send succeeded,
         * just that it has been dispatched to the network.
         */
        void onMessageSent();

        /**
         * Called if there are too many unsent messages in the queue and we're not allowing
         * any more Mms's to be sent.
         */
        void onMaxPendingMessagesReached();

        /**
         * Called if there's an attachment error while resizing the images just before sending.
         */
        void onAttachmentError(int error);
    }

    private WorkingMessage(ComposeMessageActivity activity) {
        mActivity = activity;
        mContentResolver = mActivity.getContentResolver();
        mStatusListener = activity;
        mAttachmentType = TEXT;
        mText = "";
        sCreationMode = checkCreationMode(activity);
    }
  //MTK_OP02_PROTECT_START
    private WorkingMessage(ComposeMessageActivityMini activity) {
        mActivity = activity;
        mContentResolver = mActivity.getContentResolver();
        mStatusListener = activity;
        mAttachmentType = TEXT;
        mText = "";
    }
  //MTK_OP02_PROTECT_END
    /**
     * Creates a new working message.
     */
    public static WorkingMessage createEmpty(ComposeMessageActivity activity) {
        // Make a new empty working message.
        WorkingMessage msg = new WorkingMessage(activity);
        return msg;
    }
  //MTK_OP02_PROTECT_START
    public static WorkingMessage createEmpty(ComposeMessageActivityMini activity) {
        // Make a new empty working message.
        WorkingMessage msg = new WorkingMessage(activity);
        sMessageClassMini = CLASSNUMBERMINI;
        return msg;
    } 
  //MTK_OP02_PROTECT_END 
    public int getMessageClassMini() {
        return sMessageClassMini;
    }
    /**
     * Create a new WorkingMessage from the specified data URI, which typically
     * contains an MMS message.
     */
    public static WorkingMessage load(ComposeMessageActivity activity, Uri uri) {
        // If the message is not already in the draft box, move it there.
        if (!uri.toString().startsWith(Mms.Draft.CONTENT_URI.toString())) {
            PduPersister persister = PduPersister.getPduPersister(activity);
            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                LogTag.debug("load: moving %s to drafts", uri);
            }
            try {
                uri = persister.move(uri, Mms.Draft.CONTENT_URI);
            } catch (MmsException e) {
                LogTag.error("Can't move %s to drafts", uri);
                return null;
            }
        }
        WorkingMessage msg = new WorkingMessage(activity);
        if (msg.loadFromUri(uri)) {
            return msg;
        }

        return null;
    }
  //MTK_OP02_PROTECT_START
    public static WorkingMessage load(ComposeMessageActivityMini activity, Uri uri) {
        // If the message is not already in the draft box, move it there.
        if (!uri.toString().startsWith(Mms.Draft.CONTENT_URI.toString())) {
            PduPersister persister = PduPersister.getPduPersister(activity);
            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                LogTag.debug("load: moving %s to drafts", uri);
            }
            try {
                uri = persister.move(uri, Mms.Draft.CONTENT_URI);
            } catch (MmsException e) {
                LogTag.error("Can't move %s to drafts", uri);
                return null;
            }
        }
        WorkingMessage msg = new WorkingMessage(activity);
        if (msg.loadFromUri(uri)) {
            return msg;
        }

        return null;
    }
  //MTK_OP02_PROTECT_END
    public int getCurrentMessageSize() {
        int currentMessageSize = mSlideshow.getCurrentSlideshowSize();
        return currentMessageSize;
    }

    private void correctAttachmentState() {
        int slideCount = mSlideshow.size();
        int fileAttachCount = mSlideshow.sizeOfFilesAttach();

        // If we get an empty slideshow, tear down all MMS
        // state and discard the unnecessary message Uri.
        if (fileAttachCount == 0) {
            if (slideCount == 0) {
                mAttachmentType = TEXT;
                mSlideshow = null;
                if (mMessageUri != null) {
                    asyncDelete(mMessageUri, null, null);
                    mMessageUri = null;
                }
            } else if (slideCount > 1) {
                mAttachmentType = SLIDESHOW;
            } else {
                SlideModel slide = mSlideshow.get(0);
                if (slide.hasImage()) {
                    mAttachmentType = IMAGE;
                } else if (slide.hasVideo()) {
                    mAttachmentType = VIDEO;
                } else if (slide.hasAudio()) {
                    mAttachmentType = AUDIO;
                }
            }
        } else {
            mAttachmentType = ATTACHMENT;
        }

        updateState(HAS_ATTACHMENT, hasAttachment(), false);
    }

    private boolean loadFromUri(Uri uri) {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) LogTag.debug("loadFromUri %s", uri);
        try {
            mSlideshow = SlideshowModel.createFromMessageUri(mActivity, uri);
            mHasDrmPart = mSlideshow.checkDrmContent();
            mHasDrmRight = mSlideshow.checkDrmRight();
        } catch (MmsException e) {
            LogTag.error("Couldn't load URI %s", uri);
            return false;
        }

        mMessageUri = uri;

        // Make sure all our state is as expected.
        syncTextFromSlideshow();
        correctAttachmentState();

        return true;
    }

    /**
     * Load the draft message for the specified conversation, or a new empty message if
     * none exists.
     */
    public static WorkingMessage loadDraft(ComposeMessageActivity activity,
                                           Conversation conv) {
        WorkingMessage msg = new WorkingMessage(activity);
        if (msg.loadFromConversation(conv)) {
            return msg;
        } else {
            return createEmpty(activity);
        }
    }
  //MTK_OP02_PROTECT_START
    public static WorkingMessage loadDraft(ComposeMessageActivityMini activity,
            Conversation conv) {
        WorkingMessage msg = new WorkingMessage(activity);
        if (msg.loadFromConversation(conv)) {
            return msg;
        } else {
            return createEmpty(activity);
        }
    }
  //MTK_OP02_PROTECT_END
    private boolean loadFromConversation(Conversation conv) {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) LogTag.debug("loadFromConversation %s", conv);

        long threadId = conv.getThreadId();
        if (threadId <= 0) {
            return false;
        }

        // Look for an SMS draft first.
        mText = readDraftSmsMessage(conv);
        if (!TextUtils.isEmpty(mText)) {
            return true;
        }

        // Then look for an MMS draft.
        StringBuilder sb = new StringBuilder();
        Uri uri = readDraftMmsMessage(mActivity, threadId, sb);
        if (uri != null) {
            if (loadFromUri(uri)) {
                // If there was an MMS message, readDraftMmsMessage
                // will put the subject in our supplied StringBuilder.
                if (sb.length() > 0) {
                    setSubject(sb.toString(), false);
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Sets the text of the message to the specified CharSequence.
     */
    public void setText(CharSequence s) {
        mText = s;
        if (mText != null && TextUtils.getTrimmedLength(mText) >= 0) {
            syncTextToSlideshow();
        }
    }

    /**
     * Returns the current message text.
     */
    public CharSequence getText() {
        return mText;
    }

    /**
     * Returns true if the message has any text. A message with just whitespace is not considered
     * to have text.
     * @return
     */
    public boolean hasText() {
        return mText != null && TextUtils.getTrimmedLength(mText) > 0;
    }

    /**
     * Adds an attachment to the message, replacing an old one if it existed.
     * @param type Type of this attachment, such as {@link IMAGE}
     * @param dataUri Uri containing the attachment data (or null for {@link TEXT})
     * @param append true if we should add the attachment to a new slide
     * @return An error code such as {@link UNKNOWN_ERROR} or {@link OK} if successful
     */
    public int setAttachment(int type, Uri dataUri, boolean append) {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            LogTag.debug("setAttachment type=%d uri %s", type, dataUri);
        }
        int result = OK;

        // Make sure mSlideshow is set up and has a slide.
        ensureSlideshow();

        // Change the attachment and translate the various underlying
        // exceptions into useful error codes.
        try {
            if (type >= ATTACHMENT) {
                if (mSlideshow == null) {
                    mSlideshow = SlideshowModel.createNew(mActivity);
                }
                setOrAppendFileAttachment(type, dataUri, append);
            } else {
                // Make sure mSlideshow is set up and has a slide.
                ensureSlideshow();
                if (append) {
                    appendMedia(type, dataUri);
                } else {
                    changeMedia(type, dataUri);
                }
            }
        } catch (MmsException e) {
            result = UNKNOWN_ERROR;
        } catch (UnsupportContentTypeException e) {
            result = UNSUPPORTED_TYPE;
        } catch (ExceedMessageSizeException e) {
            result = MESSAGE_SIZE_EXCEEDED;
        } catch (ResolutionException e) {
            result = IMAGE_TOO_LARGE;
        } catch (ContentRestrictionException e){
            result = sCreationMode;
        } catch (RestrictedResolutionException e){
            result = RESTRICTED_RESOLUTION;
        } catch (IllegalStateException e) {
            result = UNKNOWN_ERROR;
        } catch (IllegalArgumentException e) {
            result = UNKNOWN_ERROR;
        }

        // If we were successful, update mAttachmentType and notify
        // the listener than there was a change.
        if (result == OK) {
            mAttachmentType = type;
            if (mSlideshow == null){
                return UNKNOWN_ERROR;
            }
            if(mSlideshow.size() > 1) {
                mAttachmentType = SLIDESHOW;
            }
            mStatusListener.onAttachmentChanged();
        } else if (append) {
            // We added a new slide and what we attempted to insert on the slide failed.
            // Delete that slide, otherwise we could end up with a bunch of blank slides.
            SlideshowEditor slideShowEditor = new SlideshowEditor(mActivity, mSlideshow);
            if (slideShowEditor == null || mSlideshow == null){
                return UNKNOWN_ERROR;
            }
            slideShowEditor.removeSlide(mSlideshow.size() - 1);
        }

        if (!MmsConfig.getMultipartSmsEnabled()) {
            if (!append && mAttachmentType == TEXT && type == TEXT) {
                int[] params = SmsMessage.calculateLength(getText(), false);
                /* SmsMessage.calculateLength returns an int[4] with:
                *   int[0] being the number of SMS's required,
                *   int[1] the number of code units used,
                *   int[2] is the number of code units remaining until the next message.
                *   int[3] is the encoding type that should be used for the message.
                */
                int msgCount = params[0];

                if (msgCount >= MmsConfig.getSmsToMmsTextThreshold()) {
                    setLengthRequiresMms(true, false);
                } else {
                    updateState(HAS_ATTACHMENT, hasAttachment(), true);
                }
            } else {
                updateState(HAS_ATTACHMENT, hasAttachment(), true);
            }
        } else {
            // Set HAS_ATTACHMENT if we need it.
            updateState(HAS_ATTACHMENT, hasAttachment(), true);
        }
        correctAttachmentState();
        return result;
    }
    public static int checkCreationMode(Context context){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String creationMode = sp.getString(MessagingPreferenceActivity.CREATION_MODE, CREATION_MODE_FREE);
        if(creationMode.equals(CREATION_MODE_WARNING)){
            return WARNING_TYPE;
        }else if(creationMode.equals(CREATION_MODE_RESTRICTED)){
            return RESTRICTED_TYPE;
        }else{
            return OK;
        }       
    }
    /**
     * Returns true if this message contains anything worth saving.
     */
    public boolean isWorthSaving() {
        // If it actually contains anything, it's of course not empty.
        if (hasText() || hasSubject() || hasAttachment() || hasSlideshow()) {
            return true;
        }

        // When saveAsMms() has been called, we set FORCE_MMS to represent
        // sort of an "invisible attachment" so that the message isn't thrown
        // away when we are shipping it off to other activities.
        if (isFakeMmsForDraft()) {
            return true;
        }

        return false;
    }

    /**
     * Returns true if FORCE_MMS is set.
     * When saveAsMms() has been called, we set FORCE_MMS to represent
     * sort of an "invisible attachment" so that the message isn't thrown
     * away when we are shipping it off to other activities.
     */
    public boolean isFakeMmsForDraft() {
        return (mMmsState & FORCE_MMS) > 0;
    }

    /**
     * Makes sure mSlideshow is set up.
     */
    private void ensureSlideshow() {
        if (mSlideshow != null) {
            if (mSlideshow.size() > 0 || mSlideshow.sizeOfFilesAttach() > 0) {
                return;
            } else {
                mSlideshow.add(new SlideModel(mSlideshow));
                return;
            }
        }

        SlideshowModel slideshow = SlideshowModel.createNew(mActivity);
        SlideModel slide = new SlideModel(slideshow);
        slideshow.add(slide);

        mSlideshow = slideshow;
        mHasDrmPart = mSlideshow.checkDrmContent();
        mHasDrmRight = mSlideshow.checkDrmRight();
    }

    /**
     * Change the message's attachment to the data in the specified Uri.
     * Used only for single-slide ("attachment mode") messages.
     */
    private void changeMedia(int type, Uri uri) throws MmsException {
        SlideModel slide = mSlideshow.get(0);
        MediaModel media;

        if (slide == null) {
            Log.w(LogTag.TAG, "[WorkingMessage] changeMedia: no slides!");
            return;
        }

        // If we're changing to text, just bail out.
        if (type == TEXT) {
            if(mSlideshow != null && mSlideshow.size() > 0) {
                mSlideshow.removeAllAttachFiles();
                mSlideshow.clear();
                mSlideshow = null;
                ensureSlideshow();
                if(ismResizeImage()) {
                    // Delete our MMS message, if there is one.
                    if (mMessageUri != null) {
                        asyncDelete(mMessageUri, null, null);
                        setmResizeImage(false);
                    }
                }
            }
            return;
        }
        mHasDrmPart = false;
        mHasDrmRight = false;
        // Make a correct MediaModel for the type of attachment.
        if (type == IMAGE) {
            media = new ImageModel(mActivity, uri, mSlideshow.getLayout().getImageRegion());
            mHasDrmPart = ((ImageModel)media).hasDrmContent();
            mHasDrmRight = ((ImageModel)media).hasDrmRight();
        } else if (type == VIDEO) {
            media = new VideoModel(mActivity, uri, mSlideshow.getLayout().getImageRegion());
            mHasDrmPart = ((VideoModel)media).hasDrmContent();
            mHasDrmRight = ((VideoModel)media).hasDrmRight();
        } else if (type == AUDIO) {
            media = new AudioModel(mActivity, uri);
            mHasDrmPart = ((AudioModel)media).hasDrmContent();
            mHasDrmRight = ((AudioModel)media).hasDrmRight();
        } else {
            throw new IllegalArgumentException("changeMedia type=" + type + ", uri=" + uri);
        }
        if (media.getMediaPackagedSize() < 0) {
            mHasDrmPart = false;
            throw new ExceedMessageSizeException("Exceed message size limitation");
        }
        if (media.getMediaPackagedSize() > mSlideshow.getCurrentSlideshowSize()){			
            mSlideshow.checkMessageSize(media.getMediaPackagedSize() + SlideshowModel.mReserveSize -mSlideshow.getCurrentSlideshowSize());
        }

        // Remove any previous attachments.
        slide.removeImage();
        slide.removeVideo();
        slide.removeAudio();

        // Add it to the slide.
        if (slide.add(media) == false) {
        	mHasDrmPart = false;
        }

        // For video and audio, set the duration of the slide to
        // that of the attachment.
        if (type == VIDEO || type == AUDIO) {
            slide.updateDuration(media.getDuration());
        }
    }

    /**
     * Add the message's attachment to the data in the specified Uri to a new slide.
     */
    private void appendMedia(int type, Uri uri) throws MmsException {

        // If we're changing to text, just bail out.
        if (type == TEXT) {
            return;
        }

        // The first time this method is called, mSlideshow.size() is going to be
        // one (a newly initialized slideshow has one empty slide). The first time we
        // attach the picture/video to that first empty slide. From then on when this
        // function is called, we've got to create a new slide and add the picture/video
        // to that new slide.
        boolean addNewSlide = true;
        if (mSlideshow.size() == 1 && !mSlideshow.isSimple()) {
            addNewSlide = false;
        }
        if (addNewSlide) {
            SlideshowEditor slideShowEditor = new SlideshowEditor(mActivity, mSlideshow);
            if (!slideShowEditor.addNewSlide()) {
                return;
            }
        }
        // Make a correct MediaModel for the type of attachment.
        MediaModel media;
        SlideModel slide = mSlideshow.get(mSlideshow.size() - 1);
        if (type == IMAGE) {
            media = new ImageModel(mActivity, uri, mSlideshow.getLayout().getImageRegion());
            mHasDrmPart = ((ImageModel)media).hasDrmContent();
            mHasDrmRight = ((ImageModel)media).hasDrmRight();
        } else if (type == VIDEO) {
            media = new VideoModel(mActivity, uri, mSlideshow.getLayout().getImageRegion());
            mHasDrmPart = ((VideoModel)media).hasDrmContent();
            mHasDrmRight = ((VideoModel)media).hasDrmRight();
        } else if (type == AUDIO) {
            media = new AudioModel(mActivity, uri);
            mHasDrmPart = ((AudioModel)media).hasDrmContent();
            mHasDrmRight = ((AudioModel)media).hasDrmRight();
        } else {
            throw new IllegalArgumentException("changeMedia type=" + type + ", uri=" + uri);
        }
        if (media.getMediaPackagedSize() < 0) {
        	mHasDrmPart = false;
            throw new ExceedMessageSizeException("Exceed message size limitation");
        }
        // Add it to the slide.
        if (slide.add(media) == false) {
        	mHasDrmPart = false;
        }

        // For video and audio, set the duration of the slide to
        // that of the attachment.
        if (type == VIDEO || type == AUDIO) {
            slide.updateDuration(media.getDuration());
        }
    }
    
    /**
     * When attaching vCard or vCalendar, we generated a *.vcs or *.vcf file locally.
     * After calling PduPersister.persist(), the files are stored into PduPart and 
     * these file generated become redundant and useless. Therefore, need to delete 
     * them after persisting.
     */
    private void deleteFileAttachmentTempFile() {
        // delete all legacy vcard files
        final String[] filenames = mActivity.fileList();
        for (String file : filenames) {
            if (file.endsWith(".vcf")) {
                if (!mActivity.deleteFile(file)) {
                    Log.d(TAG, "delete temp file, cannot delete file '" + file + "'");
                } else {
                    Log.d(TAG, "delete temp file, deleted file '" + file + "'");
                }
            }
        }
    }

    private void setOrAppendFileAttachment(int type, Uri uri, boolean append) throws MmsException {
        FileAttachmentModel fileAttach = null;
        if (type == VCARD) {
            Log.i(TAG, "WorkingMessage.setOrAppendFileAttachment(): for vcard " + uri);
            fileAttach = new VCardModel(mActivity.getApplication(), uri);
        } else if (type == VCALENDAR) {
            Log.i(TAG, "WorkingMessage.setOrAppendFileAttachment(): for vcalendar " + uri);
            fileAttach = new VCalendarModel(mActivity.getApplication(), uri);
        } else {
            throw new IllegalArgumentException("setOrAppendFileAttachment type=" + type + ", uri=" + uri);
        }
        if (fileAttach == null) {
            throw new IllegalStateException("setOrAppendFileAttachment failedto create FileAttachmentModel " 
                    + type + " uri = " + uri);
        }

        // Remove any previous attachments.
        // NOTE: here cannot use clear(), it will remove the text too.
        SlideModel slide = mSlideshow.get(0);
        slide.removeImage();
        slide.removeVideo();
        slide.removeAudio();

        // Add file attachments
        if (append) {
            mSlideshow.addFileAttachment(fileAttach);
        } else {
            // reset file attachment, so that this is the only one
            mSlideshow.removeAllAttachFiles();
            mSlideshow.addFileAttachment(fileAttach);
        }
    }

    /**
     * Returns true if the message has an attachment (including slideshows).
     */
    public boolean hasAttachment() {
        return (mAttachmentType > TEXT);
    }

    /**
     * Returns the slideshow associated with this message.
     */
    public SlideshowModel getSlideshow() {
        return mSlideshow;
    }

    /**
     * Returns true if the message has a real slideshow, as opposed to just
     * one image attachment, for example.
     */
    public boolean hasSlideshow() {
        return (mSlideshow != null && mSlideshow.size() > 1);
    }

    /**
     * Sets the MMS subject of the message.  Passing null indicates that there
     * is no subject.  Passing "" will result in an empty subject being added
     * to the message, possibly triggering a conversion to MMS.  This extra
     * bit of state is needed to support ComposeMessageActivity converting to
     * MMS when the user adds a subject.  An empty subject will be removed
     * before saving to disk or sending, however.
     */
    public void setSubject(CharSequence s, boolean notify) {

        boolean flag = ((s != null) && (s.length()>0));
        if (flag) {
            mSubject = s;
            updateState(HAS_SUBJECT, flag, notify);
        } else {
            updateState(HAS_SUBJECT, flag, notify);
        }
    }

    /**
     * Returns the MMS subject of the message.
     */
    public CharSequence getSubject() {
        return mSubject;
    }

    /**
     * Returns true if this message has an MMS subject. A subject has to be more than just
     * whitespace.
     * @return
     */
    public boolean hasSubject() {
        return mSubject != null && TextUtils.getTrimmedLength(mSubject) > 0;
    }

    /**
     * Moves the message text into the slideshow.  Should be called any time
     * the message is about to be sent or written to disk.
     */
    private void syncTextToSlideshow() {
        if (mSlideshow == null || mSlideshow.size() != 1)
            return;

        SlideModel slide = mSlideshow.get(0);
        TextModel text;
        // Add a TextModel to slide 0 if one doesn't already exist
        text = new TextModel(mActivity, ContentType.TEXT_PLAIN, "text_0.txt",
                                           mSlideshow.getLayout().getTextRegion(),
                                           TextUtils.getTrimmedLength(mText)>=0 ? (mText.toString()).getBytes() : null);
        try {
            //klocwork issue pid:18444
            if (slide != null) {
                slide.add(text);
            }
        } catch (ExceedMessageSizeException e) {
            return;
        }
    }

    /**
     * Sets the message text out of the slideshow.  Should be called any time
     * a slideshow is loaded from disk.
     */
    private void syncTextFromSlideshow() {
        // Don't sync text for real slideshows.
        if (mSlideshow.size() != 1) {
            return;
        }

        SlideModel slide = mSlideshow.get(0);
        if (slide == null || !slide.hasText()) {
            return;
        }

        mText = slide.getText().getText();
    }

    /**
     * Removes the subject if it is empty, possibly converting back to SMS.
     */
    private void removeSubjectIfEmpty(boolean notify) {
        if (!hasSubject()) {
            setSubject(null, notify);
        }
    }

    /**
     * Gets internal message state ready for storage.  Should be called any
     * time the message is about to be sent or written to disk.
     */
    private void prepareForSave(boolean notify) {
        // Make sure our working set of recipients is resolved
        // to first-class Contact objects before we save.
        syncWorkingRecipients();

        if (requiresMms()) {
            ensureSlideshow();
            syncTextToSlideshow();
            removeSubjectIfEmpty(notify);
        }
    }

    /**
     * Resolve the temporary working set of recipients to a ContactList.
     */
    public void syncWorkingRecipients() {
        if (mWorkingRecipients != null) {
            ContactList recipients = ContactList.getByNumbers(mWorkingRecipients, false);
            String workingRecips = recipients.serialize();
            mConversation.setRecipients(recipients);    // resets the threadId to zero

            mWorkingRecipients = null;
        }
    }

    public String getWorkingRecipients() {
        // this function is used for DEBUG only
        if (mWorkingRecipients == null) {
            return null;
        }
        ContactList recipients = ContactList.getByNumbers(mWorkingRecipients, false);
        return recipients.serialize();
    }

    // Call when we've returned from adding an attachment. We're no longer forcing the message
    // into a Mms message. At this point we either have the goods to make the message a Mms
    // or we don't. No longer fake it.
    public void removeFakeMmsForDraft() {
        updateState(FORCE_MMS, false, false);
    }

    /**
     * Force the message to be saved as MMS and return the Uri of the message.
     * Typically used when handing a message off to another activity.
     */
    public Uri saveAsMms(boolean notify) {
        if (DEBUG) LogTag.debug("save mConversation=%s", mConversation);

        if (mDiscarded) {
            throw new IllegalStateException("save() called after discard()");
        }

        // FORCE_MMS behaves as sort of an "invisible attachment", making
        // the message seem non-empty (and thus not discarded).  This bit
        // is sticky until the last other MMS bit is removed, at which
        // point the message will fall back to SMS.
        updateState(FORCE_MMS, true, notify);

        // Collect our state to be written to disk.
        prepareForSave(true /* notify */);

        // Make sure we are saving to the correct thread ID.
        mConversation.ensureThreadId();
        mConversation.setDraftState(true);

        PduPersister persister = PduPersister.getPduPersister(mActivity);
        SendReq sendReq = makeSendReq(mConversation, mSubject);

        // If we don't already have a Uri lying around, make a new one.  If we do
        // have one already, make sure it is synced to disk.
        if (mMessageUri == null) {
            mMessageUri = createDraftMmsMessage(persister, sendReq, mSlideshow);
        } else {
            updateDraftMmsMessage(mMessageUri, persister, mSlideshow, sendReq);
        }

        return mMessageUri;
    }

    /**
     * Save this message as a draft in the conversation previously specified
     * to {@link setConversation}.
     */
    public void saveDraft() {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            LogTag.debug("saveDraft");
        }
        LogTag.debug("saveDraft");
        // If we have discarded the message, just bail out.
        if (mDiscarded) {
            return;
        }

        // Make sure setConversation was called.
        if (mConversation == null) {
            throw new IllegalStateException("saveDraft() called with no conversation");
        }

        // Get ready to write to disk. But don't notify message status when saving draft
        prepareForSave(false /* notify */);

        if (requiresMms()) {
            asyncUpdateDraftMmsMessage(mConversation);
            // Update state of the draft cache.
            mConversation.setDraftState(true);
        } else {
            String content = mText.toString();

            // bug 2169583: don't bother creating a thread id only to delete the thread
            // because the content is empty. When we delete the thread in updateDraftSmsMessage,
            // we didn't nullify conv.mThreadId, causing a temperary situation where conv
            // is holding onto a thread id that isn't in the database. If a new message arrives
            // and takes that thread id (because it's the next thread id to be assigned), the
            // new message will be merged with the draft message thread, causing confusion!
            if (!TextUtils.isEmpty(content)) {
                asyncUpdateDraftSmsMessage(mConversation, content);
            }
        }       
    }

    synchronized public void discard() {
        discard(true);
    }

    synchronized public void discard(boolean isDiscard) {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            LogTag.debug("discard");
        }

        if (mDiscarded == true) {
            return;
        }

        //For CU lemei,don't save draft.
        mDiscarded = isDiscard;
        
        // Delete our MMS message, if there is one.
        if (mMessageUri != null) {
            asyncDelete(mMessageUri, null, null);
        }

        clearConversation(mConversation);
    }

    synchronized public void discardMini() {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            LogTag.debug("discard");
        }

        if (mDiscarded == true) {
            return;
        }

        //For CU lemei,don't save draft.
        mDiscarded = true;

        // Delete our MMS message, if there is one.
        if (mMessageUri != null) {
            asyncDelete(mMessageUri, null, null);
        }

        clearConversation(mConversation);
    }

    public void unDiscard() {
        if (DEBUG) LogTag.debug("unDiscard");

        mDiscarded = false;
    }

    /**
     * Returns true if discard() has been called on this message.
     */
    public boolean isDiscarded() {
        return mDiscarded;
    }

    /**
     * To be called from our Activity's onSaveInstanceState() to give us a chance
     * to stow our state away for later retrieval.
     *
     * @param bundle The Bundle passed in to onSaveInstanceState
     */
    public void writeStateToBundle(Bundle bundle) {
        if (hasSubject()) {
            bundle.putString("subject", mSubject.toString());
        }

        if (mMessageUri != null) {
            bundle.putParcelable("msg_uri", mMessageUri);
        } else if (hasText()) {
            bundle.putString("sms_body", mText.toString());
        }
    }

    /**
     * To be called from our Activity's onCreate() if the activity manager
     * has given it a Bundle to reinflate
     * @param bundle The Bundle passed in to onCreate
     */
    public void readStateFromBundle(Bundle bundle) {
        if (bundle == null) {
            return;
        }

        String subject = bundle.getString("subject");
        setSubject(subject, false);

        Uri uri = (Uri)bundle.getParcelable("msg_uri");
        if (uri != null) {
            loadFromUri(uri);
            return;
        } else {
            String body = bundle.getString("sms_body");
            if (null  == body ) {
                mText = "";
            } else {
                mText = body;
            }
        }
    }

    /**
     * Update the temporary list of recipients, used when setting up a
     * new conversation.  Will be converted to a ContactList on any
     * save event (send, save draft, etc.)
     */
    public void setWorkingRecipients(List<String> numbers) {
        mWorkingRecipients = numbers;
        Log.i(TAG, "setWorkingRecipients");
    }

    private void dumpWorkingRecipients() {
        Log.i(TAG, "-- mWorkingRecipients:");

        if (mWorkingRecipients != null) {
            int count = mWorkingRecipients.size();
            for (int i=0; i<count; i++) {
                Log.i(TAG, "   [" + i + "] " + mWorkingRecipients.get(i));
            }
            Log.i(TAG, "");
        }
    }

    public void dump() {
        Log.i(TAG, "WorkingMessage:");
        dumpWorkingRecipients();
        if (mConversation != null) {
            Log.i(TAG, "mConversation: " + mConversation.toString());
        }
    }

    /**
     * Set the conversation associated with this message.
     */
    public void setConversation(Conversation conv) {
        if (DEBUG) LogTag.debug("setConversation %s -> %s", mConversation, conv);

        mConversation = conv;

        // Convert to MMS if there are any email addresses in the recipient list.
        setHasEmail(conv.getRecipients().containsEmail(), false);
    }

    /**
     * Hint whether or not this message will be delivered to an
     * an email address.
     */
    public void setHasEmail(boolean hasEmail, boolean notify) {
        Xlog.v(TAG, "WorkingMessage.setHasEmail(" + hasEmail + ", " + notify + ")");
        if (MmsConfig.getEmailGateway() != null) {
            updateState(RECIPIENTS_REQUIRE_MMS, false, notify);
        } else {
            updateState(RECIPIENTS_REQUIRE_MMS, hasEmail, notify);
        }
    }

    /**
     * Returns true if this message would require MMS to send.
     */
    public boolean requiresMms() {
        return (mMmsState > 0);
    }

    /**
     * Set whether or not we want to send this message via MMS in order to
     * avoid sending an excessive number of concatenated SMS messages.
     * @param: mmsRequired is the value for the LENGTH_REQUIRES_MMS bit.
     * @param: notify Whether or not to notify the user.
     */
    public void setLengthRequiresMms(boolean mmsRequired, boolean notify) {
        updateState(LENGTH_REQUIRES_MMS, mmsRequired, notify);
    }

    private static String stateString(int state) {
        if (state == 0)
            return "<none>";

        StringBuilder sb = new StringBuilder();
        if ((state & RECIPIENTS_REQUIRE_MMS) > 0)
            sb.append("RECIPIENTS_REQUIRE_MMS | ");
        if ((state & HAS_SUBJECT) > 0)
            sb.append("HAS_SUBJECT | ");
        if ((state & HAS_ATTACHMENT) > 0)
            sb.append("HAS_ATTACHMENT | ");
        if ((state & LENGTH_REQUIRES_MMS) > 0)
            sb.append("LENGTH_REQUIRES_MMS | ");
        if ((state & FORCE_MMS) > 0)
            sb.append("FORCE_MMS | ");

        sb.delete(sb.length() - 3, sb.length());
        return sb.toString();
    }

    /**
     * Sets the current state of our various "MMS required" bits.
     *
     * @param state The bit to change, such as {@link HAS_ATTACHMENT}
     * @param on If true, set it; if false, clear it
     * @param notify Whether or not to notify the user
     */
    private void updateState(int state, boolean on, boolean notify) {
        Xlog.v(TAG, "WorkingMessage.updateState(" + state + ", " + on + ", " + notify + ")");
        if (!sMmsEnabled) {
            // If Mms isn't enabled, the rest of the Messaging UI should not be using any
            // feature that would cause us to to turn on any Mms flag and show the
            // "Converting to multimedia..." message.
            return;
        }
        int oldState = mMmsState;
        if (on) {
            mMmsState |= state;
        } else {
            mMmsState &= ~state;
        }

        // If we are clearing the last bit that is not FORCE_MMS,
        // expire the FORCE_MMS bit.
        if (mMmsState == FORCE_MMS && ((oldState & ~FORCE_MMS) > 2)) {
            mMmsState = 0;
        }

        // Notify the listener if we are moving from SMS to MMS
        // or vice versa.
        if (notify) {
            if (oldState == 0 && mMmsState != 0) {
                mStatusListener.onProtocolChanged(true, true);
            } else if (oldState != 0 && mMmsState == 0) {
                mStatusListener.onProtocolChanged(false, true);
            }
        }

        if (oldState != mMmsState) {
            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) LogTag.debug("updateState: %s%s = %s",
                    on ? "+" : "-",
                    stateString(state), stateString(mMmsState));
        }
    }

    /**
     * Send this message over the network.  Will call back with onMessageSent() once
     * it has been dispatched to the telephony stack.  This WorkingMessage object is
     * no longer useful after this method has been called.
     */
    public void send(final String recipientsInUI) {
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            LogTag.debug("send");
        }

        // Begin -------- debug code
//        if (LogTag.SEVERE_WARNING) {
//            Log.i(TAG, "##### send #####");
//            Log.i(TAG, "   mConversation (beginning of send): " + mConversation.toString());
//            Log.i(TAG, "   recipientsInUI: " + recipientsInUI);
//        }
        // End -------- debug code

        // Get ready to write to disk.
        prepareForSave(true /* notify */);

        // Begin -------- debug code
//        String newRecipients = mConversation.getRecipients().serialize();
//        if (!TextUtils.isEmpty(recipientsInUI) && !newRecipients.equals(recipientsInUI)) {
//            if (LogTag.SEVERE_WARNING) {
//                LogTag.warnPossibleRecipientMismatch("send() after newRecipients() changed recips from: "
//                        + recipientsInUI + " to " + newRecipients, mActivity);
//                dumpWorkingRecipients();
//            } else {
//                Log.w(TAG, "send() after newRecipients() changed recips from: "
//                        + recipientsInUI + " to " + newRecipients);
//            }
//        }
        // End -------- debug code

        // We need the recipient list for both SMS and MMS.
        final Conversation conv = mConversation;
        String msgTxt = mText.toString();

        if (requiresMms() || addressContainsEmailToMms(conv, msgTxt)) {
            // Make local copies of the bits we need for sending a message,
            // because we will be doing it off of the main thread, which will
            // immediately continue on to resetting some of this state.
            final Uri mmsUri = mMessageUri;
            final PduPersister persister = PduPersister.getPduPersister(mActivity);

            final SlideshowModel slideshow = mSlideshow;
            final SendReq sendReq = makeSendReq(conv, mSubject);

            // Do the dirty work of sending the message off of the main UI thread.
            new Thread(new Runnable() {
                public void run() {
                    // Make sure the text in slide 0 is no longer holding onto a reference to
                    // the text in the message text box.
                    slideshow.prepareForSend();
                    sendMmsWorker(conv, mmsUri, persister, slideshow, sendReq);
                }
            }).start();
        } else {
            // Same rules apply as above.
            final String msgText = mText.toString();
            new Thread(new Runnable() {
                public void run() {
                    preSendSmsWorker(conv, msgText, recipientsInUI);
                }
            }).start();
        }

        // update the Recipient cache with the new to address, if it's different
        RecipientIdCache.updateNumbers(conv.getThreadId(), conv.getRecipients());
    }

    private boolean addressContainsEmailToMms(Conversation conv, String text) {
        if (MmsConfig.getEmailGateway() != null) {
            String[] dests = conv.getRecipients().getNumbers();
            int length = dests.length;
            for (int i = 0; i < length; i++) {
                if (Mms.isEmailAddress(dests[i]) || MessageUtils.isAlias(dests[i])) {
                    String mtext = dests[i] + " " + text;
                    int[] params = SmsMessage.calculateLength(mtext, false);
                    if (params[0] > 1) {
                        updateState(RECIPIENTS_REQUIRE_MMS, true, true);
                        ensureSlideshow();
                        syncTextToSlideshow();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Message sending stuff

    private void preSendSmsWorker(Conversation conv, String msgText, String recipientsInUI) {
        // If user tries to send the message, it's a signal the inputted text is what they wanted.
        UserHappinessSignals.userAcceptedImeText(mActivity);

        mStatusListener.onPreMessageSent();

        // Mark the message as discarded because it is "off the market" after being sent.
        mDiscarded = true;

        long origThreadId = conv.getThreadId();

        // Make sure we are still using the correct thread ID for our
        // recipient set.
        long threadId = conv.ensureThreadId();

        final String semiSepRecipients = conv.getRecipients().serialize();

        // recipientsInUI can be empty when the user types in a number and hits send
        if (LogTag.SEVERE_WARNING && ((origThreadId != 0 && origThreadId != threadId) ||
               (!semiSepRecipients.equals(recipientsInUI) && !TextUtils.isEmpty(recipientsInUI)))) {
            String msg = origThreadId != 0 && origThreadId != threadId ?
                    "WorkingMessage.preSendSmsWorker threadId changed or " +
                    "recipients changed. origThreadId: " +
                    origThreadId + " new threadId: " + threadId +
                    " also mConversation.getThreadId(): " +
                    mConversation.getThreadId()
                :
                    "Recipients in window: \"" +
                    recipientsInUI + "\" differ from recipients from conv: \"" +
                    semiSepRecipients + "\"";

            LogTag.warnPossibleRecipientMismatch(msg, mActivity);
        }

        // just do a regular send. We're already on a non-ui thread so no need to fire
        // off another thread to do this work.
        sendSmsWorker(msgText, semiSepRecipients, threadId);

        // Be paranoid and clean any draft SMS up.
        deleteDraftSmsMessage(threadId);
    }

    private void sendSmsWorker(String msgText, String semiSepRecipients, long threadId) {
        String[] dests = TextUtils.split(semiSepRecipients, ";");
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            LogTag.debug("sendSmsWorker sending message");
        }
        MessageSender sender = new SmsMessageSender(mActivity, dests, msgText, threadId);
        try {
            sender.sendMessage(threadId);

            // Make sure this thread isn't over the limits in message count
            Recycler.getSmsRecycler().deleteOldMessagesByThreadId(mActivity, threadId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS message, threadId=" + threadId, e);
        }

        mStatusListener.onMessageSent();
        SmsReceiverService.sSmsSent = false;
    }

    private void sendMmsWorker(Conversation conv, Uri mmsUri, PduPersister persister,
                               SlideshowModel slideshow, SendReq sendReq) {
        // If user tries to send the message, it's a signal the inputted text is what they wanted.
        UserHappinessSignals.userAcceptedImeText(mActivity);

        // First make sure we don't have too many outstanding unsent message.
        Cursor cursor = null;
        try {
            cursor = SqliteWrapper.query(mActivity, mContentResolver,
                    Mms.Outbox.CONTENT_URI, MMS_OUTBOX_PROJECTION, null, null, null);
            if (cursor != null) {
                long maxMessageSize = MmsConfig.getMaxSizeScaleForPendingMmsAllowed() *
                    MmsConfig.getUserSetMmsSizeLimit(true);
                long totalPendingSize = 0;
                while (cursor.moveToNext()) {
                    if (PduHeaders.STATUS_UNREACHABLE != cursor.getLong(MMS_MESSAGE_STATUS_INDEX)) {
                        totalPendingSize += cursor.getLong(MMS_MESSAGE_SIZE_INDEX);
                    }
                }
                if (totalPendingSize >= maxMessageSize) {
                    unDiscard();    // it wasn't successfully sent. Allow it to be saved as a draft.
                    mStatusListener.onMaxPendingMessagesReached();
                    return;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        mStatusListener.onPreMessageSent();

        // Mark the message as discarded because it is "off the market" after being sent.
        mDiscarded = true;

        // Make sure we are still using the correct thread ID for our
        // recipient set.
        long threadId = conv.ensureThreadId();

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            LogTag.debug("sendMmsWorker: update draft MMS message " + mmsUri);
        }

        if (mmsUri == null) {
            // Create a new MMS message if one hasn't been made yet.
            mmsUri = createDraftMmsMessage(persister, sendReq, slideshow);
        } else {
            // Otherwise, sync the MMS message in progress to disk.
            updateDraftMmsMessage(mmsUri, persister, slideshow, sendReq);
        }

        // Be paranoid and clean any draft SMS up.
        deleteDraftSmsMessage(threadId);

        // Resize all the resizeable attachments (e.g. pictures) to fit
        // in the remaining space in the slideshow.

        
        try {
            MessageSender sender = new MmsMessageSender(mActivity, mmsUri,
                    slideshow.getCurrentSlideshowSize());
            if (!sender.sendMessage(threadId)) {
                // The message was sent through SMS protocol, we should
                // delete the copy which was previously saved in MMS drafts.
                SqliteWrapper.delete(mActivity, mContentResolver, mmsUri, null, null);
            }

            // Make sure this thread isn't over the limits in message count
            Recycler.getMmsRecycler().deleteOldMessagesByThreadId(mActivity, threadId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send message: " + mmsUri + ", threadId=" + threadId, e);
        }

        mStatusListener.onMessageSent();
        SendTransaction.sMMSSent = false;
    }

    private void markMmsMessageWithError(Uri mmsUri) {
        try {
            PduPersister p = PduPersister.getPduPersister(mActivity);
            // Move the message into MMS Outbox. A trigger will create an entry in
            // the "pending_msgs" table.
            p.move(mmsUri, Mms.Outbox.CONTENT_URI);

            // Now update the pending_msgs table with an error for that new item.
            ContentValues values = new ContentValues(1);
            values.put(PendingMessages.ERROR_TYPE, MmsSms.ERR_TYPE_GENERIC_PERMANENT);
            long msgId = ContentUris.parseId(mmsUri);
            SqliteWrapper.update(mActivity, mContentResolver,
                    PendingMessages.CONTENT_URI,
                    values, PendingMessages._ID + "=" + msgId, null);
        } catch (MmsException e) {
            // Not much we can do here. If the p.move throws an exception, we'll just
            // leave the message in the draft box.
            Log.e(TAG, "Failed to move message to outbox and mark as error: " + mmsUri, e);
        }
    }

    // Draft message stuff

    private static final String[] MMS_DRAFT_PROJECTION = {
        Mms._ID,                // 0
        Mms.SUBJECT,            // 1
        Mms.SUBJECT_CHARSET     // 2
    };

    private static final int MMS_ID_INDEX         = 0;
    private static final int MMS_SUBJECT_INDEX    = 1;
    private static final int MMS_SUBJECT_CS_INDEX = 2;

    private static Uri readDraftMmsMessage(Context context, long threadId, StringBuilder sb) {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            LogTag.debug("readDraftMmsMessage tid=%d", threadId);
        }
        Cursor cursor;
        ContentResolver cr = context.getContentResolver();

        final String selection = Mms.THREAD_ID + " = " + threadId;
        cursor = SqliteWrapper.query(context, cr,
                Mms.Draft.CONTENT_URI, MMS_DRAFT_PROJECTION,
                selection, null, null);

        Uri uri;
        try {
            if (cursor.moveToFirst()) {
                uri = ContentUris.withAppendedId(Mms.Draft.CONTENT_URI,
                        cursor.getLong(MMS_ID_INDEX));
                String subject = MessageUtils.extractEncStrFromCursor(cursor, MMS_SUBJECT_INDEX,
                        MMS_SUBJECT_CS_INDEX);
                if (subject != null) {
                    sb.append(subject);
                }
                return uri;
            }
        } finally {
            cursor.close();
        }

        return null;
    }

    /**
     * makeSendReq should always return a non-null SendReq, whether the dest addresses are
     * valid or not.
     */
    private static SendReq makeSendReq(Conversation conv, CharSequence subject) {
        String[] dests = conv.getRecipients().getNumbers(false /*don't scrub for MMS address */);

        SendReq req = new SendReq();
        EncodedStringValue[] encodedNumbers = EncodedStringValue.encodeStrings(dests);
        if (encodedNumbers != null) {
            req.setTo(encodedNumbers);
        }

        if (!TextUtils.isEmpty(subject)) {
            req.setSubject(new EncodedStringValue(subject.toString()));
        }

        req.setDate(System.currentTimeMillis() / 1000L);

        return req;
    }

    private Uri createDraftMmsMessage(PduPersister persister, SendReq sendReq,
            SlideshowModel slideshow) {
        if (slideshow == null){
            return null;
        }
        try {
            PduBody pb = slideshow.toPduBody();
            sendReq.setBody(pb);
            Uri res = persister.persist(sendReq, Mms.Draft.CONTENT_URI);
            deleteFileAttachmentTempFile();
            slideshow.sync(pb);
            return res;
        } catch (MmsException e) {
            return null;
        } catch (IllegalArgumentException e){
            return null;
        }
    }

    private void asyncUpdateDraftMmsMessage(final Conversation conv) {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            LogTag.debug("asyncUpdateDraftMmsMessage conv=%s mMessageUri=%s", conv, mMessageUri);
        }

        final PduPersister persister = PduPersister.getPduPersister(mActivity);
        final SendReq sendReq = makeSendReq(conv, mSubject);

        new Thread(new Runnable() {
            public void run() {
                conv.ensureThreadId();
                conv.setDraftState(true);
                if (mMessageUri == null) {
                    mMessageUri = createDraftMmsMessage(persister, sendReq, mSlideshow);
                } else {
                    updateDraftMmsMessage(mMessageUri, persister, mSlideshow, sendReq);
                }

                // Be paranoid and delete any SMS drafts that might be lying around. Must do
                // this after ensureThreadId so conv has the correct thread id.
                asyncDeleteDraftSmsMessage(conv);
            }
        }).start();
    }

    private static void updateDraftMmsMessage(Uri uri, PduPersister persister,
            SlideshowModel slideshow, SendReq sendReq) {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            LogTag.debug("updateDraftMmsMessage uri=%s", uri);
        }
        if (uri == null) {
            Log.e(TAG, "updateDraftMmsMessage null uri");
            return;
        }

        try{    
            persister.updateHeaders(uri, sendReq);
        }catch (IllegalArgumentException e){
            Xlog.e(TAG, "updateDraftMmsMessage: cannot update message " + uri);
        }    

        final PduBody pb = slideshow.toPduBody();

        try {
            persister.updateParts(uri, pb);
        } catch (MmsException e) {
            Log.e(TAG, "updateDraftMmsMessage: cannot update message " + uri);
        }

        slideshow.sync(pb);
    }

    private static final String SMS_DRAFT_WHERE = Sms.TYPE + "=" + Sms.MESSAGE_TYPE_DRAFT;
    private static final String[] SMS_BODY_PROJECTION = { Sms.BODY };
    private static final int SMS_BODY_INDEX = 0;

    /**
     * Reads a draft message for the given thread ID from the database,
     * if there is one, deletes it from the database, and returns it.
     * @return The draft message or an empty string.
     */
    private String readDraftSmsMessage(Conversation conv) {
        long thread_id = conv.getThreadId();
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            LogTag.debug("readDraftSmsMessage tid=%d", thread_id);
        }
        // If it's an invalid thread or we know there's no draft, don't bother.
        if (thread_id <= 0 || !conv.hasDraft()) {
            return "";
        }

        Uri thread_uri = ContentUris.withAppendedId(Sms.Conversations.CONTENT_URI, thread_id);
        String body = "";

        Cursor c = SqliteWrapper.query(mActivity, mContentResolver,
                        thread_uri, SMS_BODY_PROJECTION, SMS_DRAFT_WHERE, null, null);
        boolean haveDraft = false;
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    body = c.getString(SMS_BODY_INDEX);
                    haveDraft = true;
                }
            } finally {
                c.close();
            }
        }

        // We found a draft, and if there are no messages in the conversation,
        // that means we deleted the thread, too. Must reset the thread id
        // so we'll eventually create a new thread.
        if (haveDraft && conv.getMessageCount() == 0) {
            // Clean out drafts for this thread -- if the recipient set changes,
            // we will lose track of the original draft and be unable to delete
            // it later.  The message will be re-saved if necessary upon exit of
            // the activity.
            clearConversation(conv);
        }

        return body;
    }

    public void clearConversation(final Conversation conv) {
        asyncDeleteDraftSmsMessage(conv);
        //add for bug ALPS00047256
        int MessageCount=0;
        final Uri sAllThreadsUri =  Threads.CONTENT_URI.buildUpon().appendQueryParameter("simple", "true").build();
 
        if (conv.getMessageCount()==0){
            Cursor cursor = SqliteWrapper.query(
                mActivity,
                mContentResolver,
                sAllThreadsUri,
                new String[] {Threads.MESSAGE_COUNT, Threads._ID} ,
                Threads._ID + "=" + conv.getThreadId(), null, null);
            if (cursor !=null) {
                try {
                    if (cursor.moveToFirst()) {
                        MessageCount = cursor.getInt(cursor.getColumnIndexOrThrow(Threads.MESSAGE_COUNT));
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        if ((conv.getMessageCount() == 0)&&(MessageCount == 0)) {
            if (DEBUG) LogTag.debug("clearConversation calling clearThreadId");
            conv.clearThreadId();
        }

        conv.setDraftState(false);
    }

    private void asyncUpdateDraftSmsMessage(final Conversation conv, final String contents) {
        new Thread(new Runnable() {
            public void run() {
                long threadId = conv.ensureThreadId();
                conv.setDraftState(true);
                updateDraftSmsMessage(threadId, contents);
            }
        }).start();
    }

    private void updateDraftSmsMessage(long thread_id, String contents) {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            LogTag.debug("updateDraftSmsMessage tid=%d, contents=\"%s\"", thread_id, contents);
        }

        // If we don't have a valid thread, there's nothing to do.
        if (thread_id <= 0) {
            return;
        }

        ContentValues values = new ContentValues(3);
        values.put(Sms.THREAD_ID, thread_id);
        values.put(Sms.BODY, contents);
        values.put(Sms.TYPE, Sms.MESSAGE_TYPE_DRAFT);
        SqliteWrapper.insert(mActivity, mContentResolver, Sms.CONTENT_URI, values);
        asyncDeleteDraftMmsMessage(thread_id);
    }

    private void asyncDelete(final Uri uri, final String selection, final String[] selectionArgs) {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            LogTag.debug("asyncDelete %s where %s", uri, selection);
        }
        new Thread(new Runnable() {
            public void run() {
                deleteFileAttachmentTempFile();
                if (uri != null){
                    int result = -1; // if delete is unsuccessful, the delete() method will return -1
                    int retryCount = 0;
                    int maxRetryCount = 10;
                    while (result == -1 && retryCount < maxRetryCount) {
                        try {
                            result = SqliteWrapper.delete(mActivity, mContentResolver, uri, selection, selectionArgs);
                            retryCount++;
                        } catch (SQLiteDiskIOException e) {
                            retryCount++;
                            Xlog.e(TAG, "asyncDelete(): SQLiteDiskIOException: delete thread unsuccessful! Try time=" + retryCount);
                        }
                    }
                }
            }
        }).start();
    }

    private void asyncDeleteDraftSmsMessage(Conversation conv) {
        long threadId = conv.getThreadId();
        if (threadId > 0) {
            asyncDelete(ContentUris.withAppendedId(Sms.Conversations.CONTENT_URI, threadId),
                SMS_DRAFT_WHERE, null);
        }
    }

    private void deleteDraftSmsMessage(long threadId) {
        SqliteWrapper.delete(mActivity, mContentResolver,
                ContentUris.withAppendedId(Sms.Conversations.CONTENT_URI, threadId),
                SMS_DRAFT_WHERE, null);
    }

    private void asyncDeleteDraftMmsMessage(long threadId) {
        final String where = Mms.THREAD_ID + " = " + threadId;
        asyncDelete(Mms.Draft.CONTENT_URI, where, null);
        /* Reset MMS's message URI because the MMS is deleted */
        mMessageUri = null;
    }

    public void asyncDeleteOldDraftMmsMessage(long threadId){
        if( mMessageUri != null && threadId > 0 ) {
            String pduId = mMessageUri.getLastPathSegment();
            final String where = Mms.THREAD_ID + " = " + threadId + " and " + WordsTable.ID + " != " + pduId;
            asyncDelete(Mms.Draft.CONTENT_URI, where, null);
        }
    }

    public boolean ismResizeImage() {
        return mResizeImage;
    }

    public void setmResizeImage(boolean mResizeImage) {
        this.mResizeImage = mResizeImage;
    }

    public void resetMessageUri(){
        if (mAttachmentType == TEXT){
            asyncDelete(mMessageUri, null, null);
            mMessageUri = null;
            clearConversation(mConversation);
        }
    }

    // add for gemini

    private int mSimId = 0;

    /**
     * Send this message over the network.  Will call back with onMessageSent() once
     * it has been dispatched to the telephony stack.  This WorkingMessage object is
     * no longer useful after this method has been called.
     */
    public void sendGemini(int simId) {
        Xlog.d(MmsApp.TXN_TAG, "Enter sendGemini(). SIM_ID = " + Integer.toString(simId, 10));
        
        // keep this sim id
        mSimId = simId;

        // Get ready to write to disk.
        prepareForSave(true /* notify */);

        // We need the recipient list for both SMS and MMS.
        final Conversation conv = mConversation;
        String msgTxt = mText.toString();

        if (requiresMms() || addressContainsEmailToMms(conv, msgTxt)) {
            // Make local copies of the bits we need for sending a message,
            // because we will be doing it off of the main thread, which will
            // immediately continue on to resetting some of this state.
            final Uri mmsUri = mMessageUri;
            final PduPersister persister = PduPersister.getPduPersister(mActivity);

            final SlideshowModel slideshow = mSlideshow;
            final SendReq sendReq = makeSendReq(conv, mSubject);

            // Make sure the text in slide 0 is no longer holding onto a reference to the text
            // in the message text box.
            slideshow.prepareForSend();

            // Do the dirty work of sending the message off of the main UI thread.
            new Thread(new Runnable() {
                public void run() {
                    sendMmsWorkerGemini(conv, mmsUri, mSimId, persister, slideshow, sendReq);
                }
            }).start();
        } else {
            // Same rules apply as above.
            final String msgText = mText.toString();
            new Thread(new Runnable() {
                public void run() {
                    //sendSmsWorkerGemini(conv, msgText, mSimId);
                    preSendSmsWorkerGemini(conv, msgText, mSimId);
                }
            }).start();
        }

        // update the Recipient cache with the new to address, if it's different
        RecipientIdCache.updateNumbers(conv.getThreadId(), conv.getRecipients());
    }

    // Dual SIM Message sending stuff
    //2.2
    private void preSendSmsWorkerGemini(Conversation conv, String msgText, int simId) {
        Xlog.d(MmsApp.TXN_TAG, "preSendSmsWorkerGemini. SIM ID = " + simId);
        // If user tries to send the message, it's a signal the inputted text is what they wanted.
        UserHappinessSignals.userAcceptedImeText(mActivity);

        mStatusListener.onPreMessageSent();

        // Mark the message as discarded because it is "off the market" after being sent.
        mDiscarded = true;

        // Make sure we are still using the correct thread ID for our
        // recipient set.
        long threadId = conv.ensureThreadId();

        final String semiSepRecipients = conv.getRecipients().serialize();

        // just do a regular send. We're already on a non-ui thread so no need to fire
        // off another thread to do this work.
        sendSmsWorkerGemini(msgText, semiSepRecipients, threadId, simId);

        // Be paranoid and clean any draft SMS up.
        deleteDraftSmsMessage(threadId);
    }

    private void sendSmsWorkerGemini(String msgText, String semiSepRecipients, long threadId, int simId) {
        Xlog.d(MmsApp.TXN_TAG, "sendSmsWorkerGemini. SIM ID = " + simId);
        String[] dests = TextUtils.split(semiSepRecipients, ";");
        MessageSender sender = new SmsMessageSender(mActivity, dests, msgText, threadId, simId);
        try {
            sender.sendMessage(threadId);

            // Make sure this thread isn't over the limits in message count
            Recycler.getSmsRecycler().deleteOldMessagesByThreadId(mActivity, threadId);
        } catch (Exception e) {
            Xlog.e(TAG, "Failed to send SMS message, threadId=" + threadId, e);
        }

        mStatusListener.onMessageSent();
        SmsReceiverService.sSmsSent = false;
    }

    // add for gemini 2.2
    private void sendMmsWorkerGemini(Conversation conv, Uri mmsUri, int simId, 
                                                PduPersister persister, SlideshowModel slideshow, 
                                                SendReq sendReq) {
        Xlog.d(MmsApp.TXN_TAG, "Enter sendMmsWorkerGemini(). SIM_ID = " + Integer.toString(simId, 10) 
            + "  mmsUri=" + mmsUri);

        // If user tries to send the message, it's a signal the inputted text is what they wanted.
        UserHappinessSignals.userAcceptedImeText(mActivity);

        // First make sure we don't have too many outstanding unsent message.
        Cursor cursor = null;
        try {
            //cursor = SqliteWrapper.query(mActivity, mContentResolver,
            //        Mms.Outbox.CONTENT_URI, MMS_OUTBOX_PROJECTION, null, null, null);
            String[] selectionArgs = new String[] {Integer.toString(simId)};
            cursor = SqliteWrapper.query(mActivity, 
                                        mContentResolver, 
                                        Mms.Outbox.CONTENT_URI, 
                                        MMS_OUTBOX_PROJECTION, 
                                        Mms.SIM_ID + " = ?", 
                                        selectionArgs, 
                                        null);
            if (cursor != null) {
                long maxMessageSize = MmsConfig.getMaxSizeScaleForPendingMmsAllowed() *
                    MmsConfig.getUserSetMmsSizeLimit(true);
                long totalPendingSize = 0;
                while (cursor.moveToNext()) {
                    if (PduHeaders.STATUS_UNREACHABLE != cursor.getLong(MMS_MESSAGE_STATUS_INDEX)) {
                        totalPendingSize += cursor.getLong(MMS_MESSAGE_SIZE_INDEX);
                    }
                }
                if (totalPendingSize >= maxMessageSize) {
                    Xlog.d(MmsApp.TXN_TAG, "sendMmsWorkerGemini(). total Pending Size >= max Message Size");
                    unDiscard();    // it wasn't successfully sent. Allow it to be saved as a draft.
                    mStatusListener.onMaxPendingMessagesReached();
                    return;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        mStatusListener.onPreMessageSent();

        // Mark the message as discarded because it is "off the market" after being sent.
        mDiscarded = true;

        // Make sure we are still using the correct thread ID for our
        // recipient set.
        long threadId = conv.ensureThreadId();

        if (mmsUri == null) {
            Xlog.d(MmsApp.TXN_TAG, "create Draft Mms Message()");
            // Create a new MMS message if one hasn't been made yet.
            mmsUri = createDraftMmsMessage(persister, sendReq, slideshow);
        } else {
            Xlog.d(MmsApp.TXN_TAG, "update Draft Mms Message()");
            // Otherwise, sync the MMS message in progress to disk.
            updateDraftMmsMessage(mmsUri, persister, slideshow, sendReq);
        }
        Xlog.d(MmsApp.TXN_TAG, "sendMmsWorkerGemini(). mmsUri="+mmsUri);

        // Be paranoid and clean any draft SMS up.
        deleteDraftSmsMessage(threadId);
    
        // Resize all the resizeable attachments (e.g. pictures) to fit
        // in the remaining space in the slideshow.
    
        MessageSender sender = new MmsMessageSender(mActivity, mmsUri,
                slideshow.getCurrentSlideshowSize());
        if (CLASSNUMBERMINI == getMessageClassMini()){
            ((MmsMessageSender)sender).setLeMeiFlag(true);
        }
        try {
            if (!sender.sendMessageGemini(threadId, simId)) {
                // The message was sent through SMS protocol, we should
                // delete the copy which was previously saved in MMS drafts.
                SqliteWrapper.delete(mActivity, mContentResolver, mmsUri, null, null);
            }

            // Make sure this thread isn't over the limits in message count
            Recycler.getMmsRecycler().deleteOldMessagesByThreadId(mActivity, threadId);
        } catch (Exception e) {
            Xlog.e(TAG, "Failed to send message: " + mmsUri + ", threadId=" + threadId, e);
        }

        mStatusListener.onMessageSent();
        SendTransaction.sMMSSent = false;
    }

    public Uri getMessageUri() {
        return this.mMessageUri;
    }
}
