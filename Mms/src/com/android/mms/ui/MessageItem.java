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

package com.android.mms.ui;

import java.util.regex.Pattern;

import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.model.SlideModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.model.TextModel;
import com.android.mms.ui.MessageListAdapter.ColumnsMap;
import com.android.mms.util.AddressUtils;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.MultimediaMessagePdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.RetrieveConf;
import com.google.android.mms.pdu.SendReq;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
//add for gemini
import com.mediatek.featureoption.FeatureOption;
/**
 * Mostly immutable model for an SMS/MMS message.
 *
 * <p>The only mutable field is the cached formatted message member,
 * the formatting of which is done outside this model in MessageListItem.
 */
public class MessageItem {
    private static String TAG = "MessageItem";

    public enum DeliveryStatus  { NONE, INFO, FAILED, PENDING, RECEIVED }

    final Context mContext;
    final String mType;
    final long mMsgId;
    final int mBoxId;

    DeliveryStatus mDeliveryStatus;
    boolean mReadReport;
    boolean mLocked;            // locked to prevent auto-deletion
    boolean mSimMsg = false;

    String mTimestamp;
    String mAddress;
    String mContact;
    String mBody; // Body of SMS, first text of MMS.
    String mTextContentType; // ContentType of text of MMS.
    Pattern mHighlight; // portion of message to highlight (from search)
    //add for gemini
    int mSimId;
    // The only non-immutable field.  Not synchronized, as access will
    // only be from the main GUI thread.  Worst case if accessed from
    // another thread is it'll return null and be set again from that
    // thread.
    CharSequence mCachedFormattedMessage;
    CharSequence mCachedFormattedTimestamp;
    CharSequence mCachedFormattedSimStatus;
    // The last message is cached above in mCachedFormattedMessage. In the latest design, we
    // show "Sending..." in place of the timestamp when a message is being sent. mLastSendingState
    // is used to keep track of the last sending state so that if the current sending state is
    // different, we can clear the message cache so it will get rebuilt and recached.
    boolean mLastSendingState;

    long mSmsDate = 0;
    String mServiceCenter = null;

    // Fields for MMS only.
    Uri mMessageUri;
    int mMessageType;
    int mAttachmentType;
    String mSubject;
    SlideshowModel mSlideshow;
    int mMessageSize;
    int mErrorType;
    int mErrorCode;
    private boolean mItemSelected = false;
    private boolean mHasDrmContent = false;
    MessageItem(Context context, String type, Cursor cursor,
            ColumnsMap columnsMap, Pattern highlight) throws MmsException {
        mContext = context;
        if (cursor == null){
            throw new MmsException("Get the null cursor");
        }
        mMsgId = cursor.getLong(columnsMap.mColumnMsgId);
        mHighlight = highlight;
        mType = type;

        mServiceCenter = cursor.getString(columnsMap.mColumnSmsServiceCenter);

        //to filter SIM Message
        mSimId = -1;
        if ("sms".equals(type)) {
            mReadReport = false; // No read reports in sms
            long status = cursor.getLong(columnsMap.mColumnSmsStatus);
            if (status == SmsManager.STATUS_ON_ICC_READ
                    || status == SmsManager.STATUS_ON_ICC_UNREAD
                    || status == SmsManager.STATUS_ON_ICC_SENT
                    || status == SmsManager.STATUS_ON_ICC_UNSENT) {
                mSimMsg = true;
            }
            if (status >= Sms.STATUS_FAILED) {
                // Failure
                mDeliveryStatus = DeliveryStatus.FAILED;
            } else if (status >= Sms.STATUS_PENDING) {
                // Pending
                mDeliveryStatus = DeliveryStatus.PENDING;
            } else if (status >= Sms.STATUS_COMPLETE && !mSimMsg) {
                // Success
                mDeliveryStatus = DeliveryStatus.RECEIVED;
            } else {
                mDeliveryStatus = DeliveryStatus.NONE;
            }

            mMessageUri = ContentUris.withAppendedId(Sms.CONTENT_URI, mMsgId);
            // Set contact and message body
            if (mSimMsg) {
                if (status == SmsManager.STATUS_ON_ICC_SENT 
                        || status == SmsManager.STATUS_ON_ICC_UNSENT) {
                    mBoxId = Sms.MESSAGE_TYPE_SENT;
                } else {
                    mBoxId = Sms.MESSAGE_TYPE_INBOX;
                }
            } else {
                mBoxId = cursor.getInt(columnsMap.mColumnSmsType);
            }
            mAddress = cursor.getString(columnsMap.mColumnSmsAddress);
            
            //add for gemini
            if (FeatureOption.MTK_GEMINI_SUPPORT){
                mSimId = cursor.getInt(columnsMap.mColumnSmsSimId);
            }
            
            if (Sms.isOutgoingFolder(mBoxId) && !mSimMsg) {
                String meString = context.getString(
                        R.string.messagelist_sender_self);

                mContact = meString;
            } else {
                // For incoming messages, the ADDRESS field contains the sender.
                if(!TextUtils.isEmpty(mAddress)) {
                    mContact = Contact.get(mAddress, true).getName();
                } else {
                    mContact = context.getString(android.R.string.unknownName);
                }
            }
            
            if (mSimMsg) {
                mBody = mContact + " : " + cursor.getString(columnsMap.mColumnSmsBody);
            } else {
                mBody = cursor.getString(columnsMap.mColumnSmsBody);
            }

            if (!isOutgoingMessage()) {
                // Set "sent" time stamp
                long date = cursor.getLong(columnsMap.mColumnSmsDate);
                mSmsDate = date;
                if (date != 0) {
                    if (isReceivedMessage()){
                        mTimestamp = String.format(context.getString(R.string.received_on),
                                 MessageUtils.formatTimeStampString(context, date));
                    } else{
                        mTimestamp = String.format(context.getString(R.string.sent_on),
                                MessageUtils.formatTimeStampString(context, date));
                    }
                } else {
                    mTimestamp = "";
                }
            }

            mLocked = cursor.getInt(columnsMap.mColumnSmsLocked) != 0;
            mErrorCode = cursor.getInt(columnsMap.mColumnSmsErrorCode);
        } else if ("mms".equals(type)) {
            mMessageUri = ContentUris.withAppendedId(Mms.CONTENT_URI, mMsgId);
            mBoxId = cursor.getInt(columnsMap.mColumnMmsMessageBox);
            mMessageType = cursor.getInt(columnsMap.mColumnMmsMessageType);
            mErrorType = cursor.getInt(columnsMap.mColumnMmsErrorType);
            String subject = cursor.getString(columnsMap.mColumnMmsSubject);
            if (!TextUtils.isEmpty(subject)) {
                EncodedStringValue v = new EncodedStringValue(
                        cursor.getInt(columnsMap.mColumnMmsSubjectCharset),
                        PduPersister.getBytes(subject));
                mSubject = v.getString();
            }
            mLocked = cursor.getInt(columnsMap.mColumnMmsLocked) != 0;

            //add for gemini
            if (FeatureOption.MTK_GEMINI_SUPPORT == true){
            	mSimId = cursor.getInt(columnsMap.mColumnMmsSimId);
            }
            
            long timestamp = 0L;
            PduPersister p = PduPersister.getPduPersister(mContext);
            if (PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND == mMessageType) {
                mDeliveryStatus = DeliveryStatus.NONE;
                NotificationInd notifInd = (NotificationInd) p.load(mMessageUri);
                interpretFrom(notifInd.getFrom(), mMessageUri);
                // Borrow the mBody to hold the URL of the message.
                mBody = new String(notifInd.getContentLocation());
                mMessageSize = (int) notifInd.getMessageSize();
                timestamp = notifInd.getExpiry() * 1000L;
            } else {
                MultimediaMessagePdu msg = (MultimediaMessagePdu) p.load(mMessageUri);
                mSlideshow = SlideshowModel.createFromPduBody(context, msg.getBody());
                mAttachmentType = MessageUtils.getAttachmentType(mSlideshow);
                mHasDrmContent = false;
                mHasDrmContent = mSlideshow.checkDrmContent();
                if (mMessageType == PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF) {
                    RetrieveConf retrieveConf = (RetrieveConf) msg;
                    interpretFrom(retrieveConf.getFrom(), mMessageUri);
                    timestamp = retrieveConf.getDate() * 1000L;
                } else {
                    // Use constant string for outgoing messages
                    mContact = mAddress = context.getString(R.string.messagelist_sender_self);
                    timestamp = ((SendReq) msg).getDate() * 1000L;
                }


                String report = cursor.getString(columnsMap.mColumnMmsDeliveryReport);
                if ((report == null) || !mAddress.equals(context.getString(
                        R.string.messagelist_sender_self))) {
                    mDeliveryStatus = DeliveryStatus.NONE;
                } else {
                    int reportInt;
                    try {
                        reportInt = Integer.parseInt(report);
                        if (reportInt == PduHeaders.VALUE_YES) {
                            mDeliveryStatus = DeliveryStatus.RECEIVED;
                        } else {
                            mDeliveryStatus = DeliveryStatus.NONE;
                        }
                    } catch (NumberFormatException nfe) {
                        Log.e(TAG, "Value for delivery report was invalid.");
                        mDeliveryStatus = DeliveryStatus.NONE;
                    }
                }

                report = cursor.getString(columnsMap.mColumnMmsReadReport);
                if ((report == null) || !mAddress.equals(context.getString(
                        R.string.messagelist_sender_self))) {
                    mReadReport = false;
                } else {
                    int reportInt;
                    try {
                        reportInt = Integer.parseInt(report);
                        mReadReport = (reportInt == PduHeaders.VALUE_YES);
                    } catch (NumberFormatException nfe) {
                        Log.e(TAG, "Value for read report was invalid.");
                        mReadReport = false;
                    }
                }

                SlideModel slide = mSlideshow.get(0);
                if ((slide != null) && slide.hasText()) {
                    TextModel tm = slide.getText();
                    if (tm.isDrmProtected()) {
                        mBody = mContext.getString(R.string.drm_protected_text);
                    } else {
                        mBody = tm.getText();
                    }
                    mTextContentType = tm.getContentType();
                }

                mMessageSize = mSlideshow.getCurrentSlideshowSize();
            }

            if (!isOutgoingMessage()) {
                mTimestamp = context.getString(getTimestampStrId(),
                        MessageUtils.formatTimeStampString(context, timestamp));
            }
        } else {
            throw new MmsException("Unknown type of the message: " + type);
        }
    }

    private void interpretFrom(EncodedStringValue from, Uri messageUri) {
        if (from != null) {
            mAddress = from.getString();
        } else {
            // In the rare case when getting the "from" address from the pdu fails,
            // (e.g. from == null) fall back to a slower, yet more reliable method of
            // getting the address from the "addr" table. This is what the Messaging
            // notification system uses.
            mAddress = AddressUtils.getFrom(mContext, messageUri);
        }
        mContact = TextUtils.isEmpty(mAddress) ? mContext.getString(android.R.string.unknownName) : Contact.get(mAddress, false).getName();
    }

    private int getTimestampStrId() {
        if (PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND == mMessageType) {
            return R.string.expire_on;
        } else {
            if (isReceivedMessage()){
                return R.string.received_on;
            }else{
                return R.string.sent_on;
            }
        }
    }

    public boolean hasDrmContent() {
        return mHasDrmContent;
    }

    public boolean isSimMsg() {
        return mSimMsg;
    }

    public boolean isMms() {
        return mType.equals("mms");
    }

    public boolean isSms() {
        return mType.equals("sms");
    }

    public boolean isDownloaded() {
        return (mMessageType != PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND);
    }

    public boolean isOutgoingMessage() {
        boolean isOutgoingMms = isMms() && (mBoxId == Mms.MESSAGE_BOX_OUTBOX);
        boolean isOutgoingSms = isSms()
                                    && ((mBoxId == Sms.MESSAGE_TYPE_FAILED)
                                            || (mBoxId == Sms.MESSAGE_TYPE_OUTBOX)
                                            || (mBoxId == Sms.MESSAGE_TYPE_QUEUED));
        return isOutgoingMms || isOutgoingSms;
    }

    public boolean isSending() {
        return !isFailedMessage() && isOutgoingMessage();
    }
    
    public boolean isReceivedMessage() {
        boolean isReceivedMms = isMms() && (mBoxId == Mms.MESSAGE_BOX_INBOX);
        boolean isReceivedSms = isSms() && (mBoxId == Sms.MESSAGE_TYPE_INBOX);
        /*(mBoxId == 0 && isSms()) means it's a SIM SMS*/
        return isReceivedMms || isReceivedSms || (mBoxId == 0 && isSms());
    }

    public boolean isSentMessage() {
        boolean isSentMms = isMms() && (mBoxId == Mms.MESSAGE_BOX_SENT);
        boolean isSentSms = isSms() && (mBoxId == Sms.MESSAGE_TYPE_SENT);
        return isSentMms || isSentSms;
    }
    
    public boolean isFailedMessage() {
        boolean isFailedMms = isMms()
                            && (mErrorType >= MmsSms.ERR_TYPE_GENERIC_PERMANENT);
        boolean isFailedSms = isSms()
                            && (mBoxId == Sms.MESSAGE_TYPE_FAILED);
        return isFailedMms || isFailedSms;
    }

    // Note: This is the only mutable field in this class.  Think of
    // mCachedFormattedMessage as a C++ 'mutable' field on a const
    // object, with this being a lazy accessor whose logic to set it
    // is outside the class for model/view separation reasons.  In any
    // case, please keep this class conceptually immutable.
    public void setCachedFormattedMessage(CharSequence formattedMessage) {
        mCachedFormattedMessage = formattedMessage;
    }

    public CharSequence getCachedFormattedMessage() {
        boolean isSending = isSending();
        if (isSending != mLastSendingState) {
            mLastSendingState = isSending;
            mCachedFormattedMessage = null;         // clear cache so we'll rebuild the message
                                                    // to show "Sending..." or the sent date.
        }
        return mCachedFormattedMessage;
    }

    public void setCachedFormattedTimestamp(CharSequence formattedTimestamp) {
    	mCachedFormattedTimestamp = formattedTimestamp;
    }

    public CharSequence getCachedFormattedTimestamp() {
        boolean isSending = isSending();
        if (isSending != mLastSendingState) {
            mLastSendingState = isSending;
            mCachedFormattedTimestamp = null;
        }
        return mCachedFormattedTimestamp;
    }

    public void setCachedFormattedSimStatus(CharSequence formattedSimStatus) {
    	mCachedFormattedSimStatus = formattedSimStatus;
    }

    public CharSequence getCachedFormattedSimStatus() {
        boolean isSending = isSending();
        if (isSending != mLastSendingState) {
            mLastSendingState = isSending;
            mCachedFormattedSimStatus = null;
        }
        return mCachedFormattedSimStatus;
    }

    public int getBoxId() {
        return mBoxId;
    }
    //add for gemini
    public int getSimId() {
        return mSimId;
    }

    public boolean isSelected() {
        return mItemSelected;
    }
    
    public void setSelectedState(boolean isSelected) {
        mItemSelected = isSelected;
    }
    
    public int getFileAttachmentCount() {
        if (mSlideshow != null) {
            return mSlideshow.sizeOfFilesAttach();
        }
        return 0;
    }

    public String getServiceCenter() {
        return mServiceCenter;
    }
    
    @Override
    public String toString() {
    	//add for gemini
    	if (FeatureOption.MTK_GEMINI_SUPPORT == true){
        return "type: " + mType +
            " box: " + mBoxId +
            " sim: " + mSimId +
            " uri: " + mMessageUri +
            " address: " + mAddress +
            " contact: " + mContact +
            " read: " + mReadReport +
            " delivery status: " + mDeliveryStatus;
    }
    	else{
    		return "type: " + mType +
            " box: " + mBoxId +
            " uri: " + mMessageUri +
            " address: " + mAddress +
            " contact: " + mContact +
            " read: " + mReadReport +
            " delivery status: " + mDeliveryStatus;
    	}
        
    }
}
