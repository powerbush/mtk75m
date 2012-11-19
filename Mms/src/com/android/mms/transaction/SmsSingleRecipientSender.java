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

package com.android.mms.transaction;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.telephony.SmsManager;
import android.util.Log;

import com.android.mms.LogTag;
import com.android.mms.MmsConfig;
import com.google.android.mms.MmsException;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony.Sms;
import com.android.mms.ui.MessageUtils;
// add for gemini
import com.mediatek.featureoption.FeatureOption;
import android.telephony.gemini.GeminiSmsManager;
import com.android.internal.telephony.Phone;
import com.android.mms.MmsApp;
import com.mediatek.xlog.Xlog;
import com.mediatek.xlog.SXlog;


public class SmsSingleRecipientSender extends SmsMessageSender {

    private final boolean mRequestDeliveryReport;
    private String mDest;
    private Uri mUri;

    public SmsSingleRecipientSender(Context context, String dest, String msgText, long threadId,
            boolean requestDeliveryReport, Uri uri) {
        super(context, null, msgText, threadId);
        mRequestDeliveryReport = requestDeliveryReport;
        mDest = dest;
        mUri = uri;
    }

    // add for gemini
    public SmsSingleRecipientSender(Context context, String dest, String msgText, long threadId,
            boolean requestDeliveryReport, Uri uri, int simId) {
        super(context, null, msgText, threadId, simId);
        mRequestDeliveryReport = requestDeliveryReport;
        mDest = dest;
        mUri = uri;
    }

    public boolean sendMessage(long token) throws MmsException {
        Xlog.d(MmsApp.TXN_TAG, "SmsSingleRecipientSender: sendMessage()");
        if (mMessageText == null) {
            // Don't try to send an empty message, and destination should be just
            // one.
            throw new MmsException("Null message body or have multiple destinations.");
        }
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> messages = null;
        if ((MmsConfig.getEmailGateway() != null) &&
                (Mms.isEmailAddress(mDest) || MessageUtils.isAlias(mDest))) {
            String msgText;
            msgText = mDest + " " + mMessageText;
            mDest = MmsConfig.getEmailGateway();
            messages = smsManager.divideMessage(msgText);
        } else {
           messages = smsManager.divideMessage(mMessageText);
           // remove spaces from destination number (e.g. "801 555 1212" -> "8015551212")
           mDest = mDest.replaceAll(" ", "");
           mDest = mDest.replaceAll("-", "");
           //mDest = mDest.replaceAll("(", "");
           //mDest = mDest.replaceAll(")", "");
        }
        int messageCount = messages.size();
        Xlog.d(MmsApp.TXN_TAG, "SmsSingleRecipientSender: sendMessage(), Message Count=" + messageCount);

        if (messageCount == 0) {
            // Don't try to send an empty message.
            throw new MmsException("SmsSingleRecipientSender.sendMessage: divideMessage returned " +
                    "empty messages. Original message is \"" + mMessageText + "\"");
        }

        boolean moved = Sms.moveMessageToFolder(mContext, mUri, Sms.MESSAGE_TYPE_OUTBOX, 0);
        if (!moved) {
            throw new MmsException("SmsSingleRecipientSender.sendMessage: couldn't move message " +
                    "to outbox: " + mUri);
        }

        ArrayList<PendingIntent> deliveryIntents =  new ArrayList<PendingIntent>(messageCount);
        ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>(messageCount);
        for (int i = 0; i < messageCount; i++) {
            if (mRequestDeliveryReport) {
                // TODO: Fix: It should not be necessary to
                // specify the class in this intent.  Doing that
                // unnecessarily limits customizability.
                Intent intent = new Intent(
                                MessageStatusReceiver.MESSAGE_STATUS_RECEIVED_ACTION,
                                mUri,
                                mContext,
                                MessageStatusReceiver.class);
                //mark the last part of a sms. only the last part sms status report will be record if sent ok.
                if (i == messageCount -1) {
                    intent.putExtra(SmsReceiverService.EXTRA_MESSAGE_SENT_SEND_NEXT, true);
                }
                //the second the parameter is used now! not as the google doc says "currently not used"
                deliveryIntents.add(PendingIntent.getBroadcast(mContext, i, intent, 0));
            }
            Intent intent  = new Intent(SmsReceiverService.MESSAGE_SENT_ACTION,
                    mUri,
                    mContext,
                    SmsReceiver.class);
            if (i == messageCount -1) {
                intent.putExtra(SmsReceiverService.EXTRA_MESSAGE_SENT_SEND_NEXT, true);
            }

            // add for concatenation msg
            if (messageCount > 1) {
                intent.putExtra(SmsReceiverService.EXTRA_MESSAGE_CONCATENATION, true);
            }
            sentIntents.add(PendingIntent.getBroadcast(mContext, i, intent, 0));
        }
        try {
            Xlog.d(MmsApp.TXN_TAG, "\t Destination\t= " + mDest);
            Xlog.d(MmsApp.TXN_TAG, "\t ServiceCenter\t= " + mServiceCenter);
            Xlog.d(MmsApp.TXN_TAG, "\t Message\t= " + messages);
            Xlog.d(MmsApp.TXN_TAG, "\t uri\t= " + mUri);
            smsManager.sendMultipartTextMessage(mDest, mServiceCenter, messages, sentIntents, deliveryIntents);
        } catch (Exception ex) {
            throw new MmsException("SmsSingleRecipientSender.sendMessage: caught " + ex +
                    " from SmsManager.sendTextMessage()");
        }
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            log("SmsSingleRecipientSender: address=" + mDest + ", threadId=" + mThreadId +
                    ", uri=" + mUri + ", msgs.count=" + messageCount);
        }
        return false;
    }

    // add for gemini
    public boolean sendMessageGemini(long token, int simId) throws MmsException {
        // convert sim id to slot id
        int slotId = SIMInfo.getSlotById(mContext, mSimId);
        Xlog.d(MmsApp.TXN_TAG, "SmsSingleRecipientSender: sendMessageGemini() simId=" + simId +"slotId=" + slotId);
        if (mMessageText == null) {
            // Don't try to send an empty message, and destination should be just
            // one.
            throw new MmsException("Null message body or have multiple destinations.");
        }
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> messages = null;
        if ((MmsConfig.getEmailGateway() != null) &&
                (Mms.isEmailAddress(mDest) || MessageUtils.isAlias(mDest))) {
            String msgText;
            msgText = mDest + " " + mMessageText;
            mDest = MmsConfig.getEmailGateway();
            messages = smsManager.divideMessage(msgText);
        } else {
           messages = smsManager.divideMessage(mMessageText);
           // remove spaces from destination number (e.g. "801 555 1212" -> "8015551212")
           mDest = mDest.replaceAll(" ", "");
           // add by gx
           mDest = mDest.replaceAll("-", "");
           //mDest = mDest.replaceAll("(", "");
           //mDest = mDest.replaceAll(")", "");
        }
        int messageCount = messages.size();
        Xlog.d(MmsApp.TXN_TAG, "SmsSingleRecipientSender: sendMessageGemini(), Message Count=" + messageCount);

        if (messageCount == 0) {
            // Don't try to send an empty message.
            throw new MmsException("SmsSingleRecipientSender.sendMessageGemini: divideMessage returned " +
                    "empty messages. Original message is \"" + mMessageText + "\"");
        }

        boolean moved = Sms.moveMessageToFolder(mContext, mUri, Sms.MESSAGE_TYPE_OUTBOX, 0);
        if (!moved) {
            throw new MmsException("SmsSingleRecipientSender.sendMessageGemini: couldn't move message " +
                    "to outbox: " + mUri);
        }

        ArrayList<PendingIntent> deliveryIntents =  new ArrayList<PendingIntent>(messageCount);
        ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>(messageCount);
        for (int i = 0; i < messageCount; i++) {
            if (mRequestDeliveryReport) {
                // TODO: Fix: It should not be necessary to
                // specify the class in this intent.  Doing that
                // unnecessarily limits customizability.
                Intent drIt = new Intent(
                                MessageStatusReceiver.MESSAGE_STATUS_RECEIVED_ACTION,
                                mUri,
                                mContext,
                                MessageStatusReceiver.class);
                drIt.putExtra(Phone.GEMINI_SIM_ID_KEY, slotId/*mSimId*/);
                //mark the last part of a sms. only the last part sms status report will be record if sent ok.
                if (i == messageCount -1) {
                    drIt.putExtra(SmsReceiverService.EXTRA_MESSAGE_SENT_SEND_NEXT, true);
                }
                //the second the parameter is used now! not as the google doc says "currently not used"                
                deliveryIntents.add(PendingIntent.getBroadcast(mContext, i, drIt, 0));
            }
            Intent intent  = new Intent(SmsReceiverService.MESSAGE_SENT_ACTION,
                    mUri,
                    mContext,
                    SmsReceiver.class);
            if (i == messageCount -1) {
                intent.putExtra(SmsReceiverService.EXTRA_MESSAGE_SENT_SEND_NEXT, true);
            }
            
            // add for concatenation msg
            if (messageCount > 1) {
                intent.putExtra(SmsReceiverService.EXTRA_MESSAGE_CONCATENATION, true);
            }
            
            intent.putExtra(Phone.GEMINI_SIM_ID_KEY, slotId/*mSimId*/);
            sentIntents.add(PendingIntent.getBroadcast(mContext, i, intent, 0));
        }
        try {
            Xlog.d(MmsApp.TXN_TAG, "\t Destination\t= " + mDest);
            Xlog.d(MmsApp.TXN_TAG, "\t ServiceCenter\t= " + mServiceCenter);
            Xlog.d(MmsApp.TXN_TAG, "\t Message\t= " + messages);
            Xlog.d(MmsApp.TXN_TAG, "\t uri\t= " + mUri);
            Xlog.d(MmsApp.TXN_TAG, "\t slotId\t= "+ slotId/*mSimId*/);
            GeminiSmsManager.sendMultipartTextMessageGemini(mDest, mServiceCenter, messages, slotId/*mSimId*/, sentIntents, deliveryIntents);
            Xlog.d(MmsApp.TXN_TAG, "\t after sendMultipartTextMessageGemini");
        } catch (Exception ex) {
            throw new MmsException("SmsSingleRecipientSender.sendMessageGemini: caught " + ex +
                    " from SmsManager.sendTextMessage()");
        }
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            log("SmsSingleRecipientSender:sendMessageGemini: address=" + mDest + ", threadId=" + mThreadId +
                    ", uri=" + mUri + ", msgs.count=" + messageCount);
        }
        return false;
    }


    private void log(String msg) {
        Log.d(LogTag.TAG, "[SmsSingleRecipientSender] " + msg);
    }
}
