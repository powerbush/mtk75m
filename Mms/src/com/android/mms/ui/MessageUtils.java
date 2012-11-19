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

import com.android.internal.telephony.ITelephony;
import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.LogTag;
import com.android.mms.data.WorkingMessage;
import com.android.mms.model.MediaModel;
import com.android.mms.model.SlideModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.transaction.MmsMessageSender;
import com.android.mms.util.AddressUtils;
import com.google.android.mms.ContentType;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.CharacterSets;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.MultimediaMessagePdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPart;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.RetrieveConf;
import com.google.android.mms.pdu.SendReq;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.WapPush;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.Bitmap.CompressFormat;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Message;
import android.os.RemoteException;
import android.os.Handler;
import android.os.ServiceManager;
import android.os.StatFs;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.Telephony.Mms;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony.Sms;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundImageSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.style.URLSpan;
import android.util.Log;
import android.widget.Toast;

import android.pim.vcard.VCardConfig;
import android.pim.vcard.VCardEntryConstructor;
import android.pim.vcard.VCardEntryCounter;
import android.pim.vcard.VCardInterpreter;
import android.pim.vcard.VCardInterpreterCollection;
import android.pim.vcard.VCardParser;
import android.pim.vcard.VCardParser_V21;
import android.pim.vcard.VCardParser_V30;
import android.pim.vcard.VCardSourceDetector;
import android.pim.vcard.exception.VCardException;
import android.pim.vcard.exception.VCardNestedException;
import android.pim.vcard.exception.VCardNotSupportedException;
import android.pim.vcard.exception.VCardVersionException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.mediatek.featureoption.FeatureOption;
import com.mediatek.telephony.TelephonyManagerEx;
import android.drm.DrmManagerClient;
import com.android.internal.telephony.Phone;
import com.mediatek.xlog.Xlog;
import com.mediatek.xlog.SXlog;

/**
 * An utility class for managing messages.
 */
public class MessageUtils {
    interface ResizeImageResultCallback {
        void onResizeResult(PduPart part, boolean append);
    }

    private static final String TAG = LogTag.TAG;
    private static String sLocalNumber;
    private static String sLocalNumber2;

    // Cache of both groups of space-separated ids to their full
    // comma-separated display names, as well as individual ids to
    // display names.
    // TODO: is it possible for canonical address ID keys to be
    // re-used?  SQLite does reuse IDs on NULL id_ insert, but does
    // anything ever delete from the mmssms.db canonical_addresses
    // table?  Nothing that I could find.
    private static final Map<String, String> sRecipientAddress =
            new ConcurrentHashMap<String, String>(20 /* initial capacity */);


    /**
     * MMS address parsing data structures
     */
    // allowable phone number separators
    private static final char[] NUMERIC_CHARS_SUGAR = {
        '-', '.', ',', '(', ')', ' ', '/', '\\', '*', '#', '+'
    };

    private static HashMap numericSugarMap = new HashMap (NUMERIC_CHARS_SUGAR.length);

    static {
        for (int i = 0; i < NUMERIC_CHARS_SUGAR.length; i++) {
            numericSugarMap.put(NUMERIC_CHARS_SUGAR[i], NUMERIC_CHARS_SUGAR[i]);
        }
    }


    private MessageUtils() {
        // Forbidden being instantiated.
    }


    public static String getMessageDetails(Context context, MessageItem msgItem) {
        if (msgItem == null) {
            return null;
        }

        if ("mms".equals(msgItem.mType)) {
            int type = msgItem.mMessageType;
            switch (type) {
                case PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND:
                    return getNotificationIndDetails(context, msgItem);
                case PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF:
                case PduHeaders.MESSAGE_TYPE_SEND_REQ:
                    return getMultimediaMessageDetails(context, msgItem);
                default:
                    Log.w(TAG, "No details could be retrieved.");
                    return "";
            }
        } else {
            return getTextMessageDetails(context, msgItem);
        }
    }

    private static String getNotificationIndDetails(Context context, MessageItem msgItem) {
        StringBuilder details = new StringBuilder();
        Resources res = context.getResources();

        Uri uri = ContentUris.withAppendedId(Mms.CONTENT_URI, msgItem.mMsgId);
        NotificationInd nInd;

        try {
            nInd = (NotificationInd) PduPersister.getPduPersister(
                    context).load(uri);
        } catch (MmsException e) {
            Log.e(TAG, "Failed to load the message: " + uri, e);
            return context.getResources().getString(R.string.cannot_get_details);
        }

        // Message Type: Mms Notification.
        details.append(res.getString(R.string.message_type_label));
        details.append(res.getString(R.string.multimedia_notification));

        // sc
        details.append('\n');
        details.append(res.getString(R.string.service_center_label));
        details.append(!TextUtils.isEmpty(msgItem.mServiceCenter)? msgItem.mServiceCenter: "");

        // From: ***
        String from = extractEncStr(context, nInd.getFrom());
        details.append('\n');
        details.append(res.getString(R.string.from_label));
        details.append(!TextUtils.isEmpty(from)? from:
                                 res.getString(R.string.hidden_sender_address));

        // Date: ***
        details.append('\n');
        details.append(res.getString(
                R.string.expire_on, MessageUtils.formatTimeStampString(
                        context, nInd.getExpiry() * 1000L, true)));

        // Subject: ***
        details.append('\n');
        details.append(res.getString(R.string.subject_label));

        EncodedStringValue subject = nInd.getSubject();
        if (subject != null) {
            details.append(subject.getString());
        }

        // Message class: Personal/Advertisement/Infomational/Auto
        details.append('\n');
        details.append(res.getString(R.string.message_class_label));
        details.append(new String(nInd.getMessageClass()));

        // Message size: *** KB
        details.append('\n');
        details.append(res.getString(R.string.message_size_label));
        details.append(String.valueOf((nInd.getMessageSize() + 1023) / 1024));
        details.append(context.getString(R.string.kilobyte));

        return details.toString();
    }

    private static String getMultimediaMessageDetails(Context context, MessageItem msgItem) {
        if (msgItem.mMessageType == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND) {
            return getNotificationIndDetails(context, msgItem);
        }

        StringBuilder details = new StringBuilder();
        Resources res = context.getResources();

        Uri uri = ContentUris.withAppendedId(Mms.CONTENT_URI, msgItem.mMsgId);
        MultimediaMessagePdu msg;

        try {
            msg = (MultimediaMessagePdu) PduPersister.getPduPersister(
                    context).load(uri);
        } catch (MmsException e) {
            Log.e(TAG, "Failed to load the message: " + uri, e);
            return context.getResources().getString(R.string.cannot_get_details);
        }

        // Message Type: Text message.
        details.append(res.getString(R.string.message_type_label));
        details.append(res.getString(R.string.multimedia_message));

        if (msg instanceof RetrieveConf) {
            // From: ***
            String from = extractEncStr(context, ((RetrieveConf) msg).getFrom());
            details.append('\n');
            details.append(res.getString(R.string.from_label));
            details.append(!TextUtils.isEmpty(from)? from:
                                  res.getString(R.string.hidden_sender_address));
        }

        // To: ***
        details.append('\n');
        details.append(res.getString(R.string.to_address_label));
        EncodedStringValue[] to = msg.getTo();
        if (to != null) {
            details.append(EncodedStringValue.concat(to));
        }
        else {
            Log.w(TAG, "recipient list is empty!");
        }


        // Bcc: ***
        if (msg instanceof SendReq) {
            EncodedStringValue[] values = ((SendReq) msg).getBcc();
            if ((values != null) && (values.length > 0)) {
                details.append('\n');
                details.append(res.getString(R.string.bcc_label));
                details.append(EncodedStringValue.concat(values));
            }
        }

        // Date: ***
        details.append('\n');
        int msgBox = msgItem.mBoxId;
        if (msgBox == Mms.MESSAGE_BOX_DRAFTS) {
            details.append(res.getString(R.string.saved_label));
        } else if (msgBox == Mms.MESSAGE_BOX_INBOX) {
            details.append(res.getString(R.string.received_label));
        } else {
            details.append(res.getString(R.string.sent_label));
        }

        details.append(MessageUtils.formatTimeStampString(
                context, msg.getDate() * 1000L, true));

        // Subject: ***
        details.append('\n');
        details.append(res.getString(R.string.subject_label));

        int size = msgItem.mMessageSize;
        EncodedStringValue subject = msg.getSubject();
        if (subject != null) {
            String subStr = subject.getString();
            // Message size should include size of subject.
            size += subStr.length();
            details.append(subStr);
        }

        // Priority: High/Normal/Low
        details.append('\n');
        details.append(res.getString(R.string.priority_label));
        details.append(getPriorityDescription(context, msg.getPriority()));

        // Message size: *** KB
        details.append('\n');
        details.append(res.getString(R.string.message_size_label));
        details.append((size - 1)/1024 + 1);
        details.append(res.getString(R.string.kilobyte));

        return details.toString();
    }

    private static String getTextMessageDetails(Context context, MessageItem msgItem) {
        StringBuilder details = new StringBuilder();
        Resources res = context.getResources();

        // Message Type: Text message.
        details.append(res.getString(R.string.message_type_label));
        details.append(res.getString(R.string.text_message));

        // Address: ***
        details.append('\n');
        int smsType = msgItem.mBoxId;
        if (Sms.isOutgoingFolder(smsType)) {
            details.append(res.getString(R.string.to_address_label));
        } else {
            details.append(res.getString(R.string.from_label));
        }
        details.append(msgItem.mAddress);

        // Date: ***
        if (msgItem.mSmsDate > 0l) {
            details.append('\n');
            if (smsType == Sms.MESSAGE_TYPE_DRAFT) {
                details.append(res.getString(R.string.saved_label));
            } else if (smsType == Sms.MESSAGE_TYPE_INBOX) {
                details.append(res.getString(R.string.received_label));
            } else {
                details.append(res.getString(R.string.sent_label));
            }
            details.append(MessageUtils.formatTimeStampString(context, msgItem.mSmsDate, true));
        }
        
        // Message Center: ***
        if (smsType == Sms.MESSAGE_TYPE_INBOX) {
            details.append('\n');
            details.append(res.getString(R.string.service_center_label));
            details.append(msgItem.mServiceCenter);
        }

        // Error code: ***
        int errorCode = msgItem.mErrorCode;
        if (errorCode != 0) {
            details.append('\n')
                .append(res.getString(R.string.error_code_label))
                .append(errorCode);
        }

        return details.toString();
    }

    static private String getPriorityDescription(Context context, int PriorityValue) {
        Resources res = context.getResources();
        switch(PriorityValue) {
            case PduHeaders.PRIORITY_HIGH:
                return res.getString(R.string.priority_high);
            case PduHeaders.PRIORITY_LOW:
                return res.getString(R.string.priority_low);
            case PduHeaders.PRIORITY_NORMAL:
            default:
                return res.getString(R.string.priority_normal);
        }
    }

    public static int getAttachmentType(SlideshowModel model) {
        if (model == null) {
            return WorkingMessage.TEXT;
        }

        int numberOfSlides = model.size();
        if (numberOfSlides > 1) {
            return WorkingMessage.SLIDESHOW;
        } else if (numberOfSlides == 1) {
            if (model.sizeOfFilesAttach() > 0) {
                return WorkingMessage.ATTACHMENT;
            }

            // Only one slide in the slide-show.
            SlideModel slide = model.get(0);
            if (slide.hasVideo()) {
                return WorkingMessage.VIDEO;
            }

            if (slide.hasAudio() && slide.hasImage()) {
                return WorkingMessage.SLIDESHOW;
            }

            if (slide.hasAudio()) {
                return WorkingMessage.AUDIO;
            }

            if (slide.hasImage()) {
                return WorkingMessage.IMAGE;
            }

            if (slide.hasText()) {
                return WorkingMessage.TEXT;
            }
        }
        

        if (model.sizeOfFilesAttach() > 0) {
            return WorkingMessage.ATTACHMENT;
        }

        return WorkingMessage.TEXT;
    }

    public static String formatTimeStampString(Context context, long when) {
        return formatTimeStampString(context, when, false);
    }

    public static String formatTimeStampString(Context context, long when, boolean fullFormat) {
        Time then = new Time();
        then.set(when);
        Time now = new Time();
        now.setToNow();

        // Basic settings for formatDateTime() we want for all cases.
        int format_flags = DateUtils.FORMAT_NO_NOON_MIDNIGHT |
                           DateUtils.FORMAT_ABBREV_ALL |
                           DateUtils.FORMAT_CAP_AMPM;

        // If the message is from a different year, show the date and year.
        if (then.year != now.year) {
            format_flags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        } else if (then.yearDay != now.yearDay) {
            // If it is from a different day than today, show only the date.
            format_flags |= DateUtils.FORMAT_SHOW_DATE;
        } else {
            // Otherwise, if the message is from today, show the time.
            format_flags |= DateUtils.FORMAT_SHOW_TIME;
        }

        // If the caller has asked for full details, make sure to show the date
        // and time no matter what we've determined above (but still make showing
        // the year only happen if it is a different year from today).
        if (fullFormat) {
            format_flags |= (DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
        }

        return DateUtils.formatDateTime(context, when, format_flags);
    }

    /**
     * @parameter recipientIds space-separated list of ids
     */
    public static String getRecipientsByIds(Context context, String recipientIds,
                                            boolean allowQuery) {
        String value = sRecipientAddress.get(recipientIds);
        if (value != null) {
            return value;
        }
        if (!TextUtils.isEmpty(recipientIds)) {
            StringBuilder addressBuf = extractIdsToAddresses(
                    context, recipientIds, allowQuery);
            if (addressBuf == null) {
                // temporary error?  Don't memoize.
                return "";
            }
            value = addressBuf.toString();
        } else {
            value = "";
        }
        sRecipientAddress.put(recipientIds, value);
        return value;
    }

    private static StringBuilder extractIdsToAddresses(Context context, String recipients,
                                                       boolean allowQuery) {
        StringBuilder addressBuf = new StringBuilder();
        String[] recipientIds = recipients.split(" ");
        boolean firstItem = true;
        for (String recipientId : recipientIds) {
            String value = sRecipientAddress.get(recipientId);

            if (value == null) {
                if (!allowQuery) {
                    // when allowQuery is false, if any value from sRecipientAddress.get() is null,
                    // return null for the whole thing. We don't want to stick partial result
                    // into sRecipientAddress for multiple recipient ids.
                    return null;
                }

                Uri uri = Uri.parse("content://mms-sms/canonical-address/" + recipientId);
                Cursor c = SqliteWrapper.query(context, context.getContentResolver(),
                                               uri, null, null, null, null);
                if (c != null) {
                    try {
                        if (c.moveToFirst()) {
                            value = c.getString(0);
                            sRecipientAddress.put(recipientId, value);
                        }
                    } finally {
                        c.close();
                    }
                }
            }
            if (value == null) {
                continue;
            }
            if (firstItem) {
                firstItem = false;
            } else {
                addressBuf.append(";");
            }
            addressBuf.append(value);
        }

        return (addressBuf.length() == 0) ? null : addressBuf;
    }

    public static void selectAudio(Context context, int requestCode) {
        if (context instanceof Activity) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        	intent.addCategory(Intent.CATEGORY_DEFAULT);
        	intent.addCategory(Intent.CATEGORY_OPENABLE);
        	intent.setType(ContentType.AUDIO_UNSPECIFIED);
        	intent.setType(ContentType.AUDIO_OGG);
        	intent.setType("application/x-ogg");  
        	if (FeatureOption.MTK_DRM_APP) {
        	    intent.putExtra(android.drm.DrmStore.DrmExtra.EXTRA_DRM_LEVEL, android.drm.DrmStore.DrmExtra.DRM_LEVEL_SD);
        	} 
            try {
                ((Activity) context).startActivityForResult(intent, requestCode);
            } catch (ActivityNotFoundException e) {
                Intent mChooserIntent = Intent.createChooser(intent, null);
                ((Activity) context).startActivityForResult(mChooserIntent, requestCode);
            }
        	
        }
    }

    public static void recordSound(Context context, int requestCode) {
        if (context instanceof Activity) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType(ContentType.AUDIO_AMR);
            intent.setClassName("com.android.soundrecorder",
                    "com.android.soundrecorder.SoundRecorder");

            ((Activity) context).startActivityForResult(intent, requestCode);
        }
    }

    public static void selectVideo(Context context, int requestCode) {
        selectMediaByType(context, requestCode, ContentType.VIDEO_UNSPECIFIED);
    }

    public static void selectImage(Context context, int requestCode) {
        selectMediaByType(context, requestCode, ContentType.IMAGE_UNSPECIFIED);
    }

    private static void selectMediaByType(
            Context context, int requestCode, String contentType) {
         if (context instanceof Activity) {

            Intent innerIntent = new Intent(Intent.ACTION_GET_CONTENT);

            innerIntent.setType(contentType);
            if (FeatureOption.MTK_DRM_APP) {
                innerIntent.putExtra(android.drm.DrmStore.DrmExtra.EXTRA_DRM_LEVEL, android.drm.DrmStore.DrmExtra.DRM_LEVEL_SD);
            }
            Intent wrapperIntent = Intent.createChooser(innerIntent, null);

            ((Activity) context).startActivityForResult(wrapperIntent, requestCode);
        }
    }

    public static void viewSimpleSlideshow(Context context, SlideshowModel slideshow) {
        if (!slideshow.isSimple()) {
            throw new IllegalArgumentException(
                    "viewSimpleSlideshow() called on a non-simple slideshow");
        }
        SlideModel slide = slideshow.get(0);
        MediaModel mm = null;
        if (slide.hasImage()) {
            mm = slide.getImage();
        } else if (slide.hasVideo()) {
            mm = slide.getVideo();
        } else if (slide.hasAudio()) {
        	mm = slide.getAudio();
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        String contentType;
        if (mm.isDrmProtected()) {
            contentType = mm.getDrmObject().getContentType();
        } else {
            contentType = mm.getContentType();
        }
        intent.setDataAndType(mm.getUri(), contentType);
        try {
            context.startActivity(intent);
        } catch(ActivityNotFoundException e) {
            Message msg = Message.obtain(MmsApp.getToastHandler());
            msg.what = MmsApp.MSG_MMS_CAN_NOT_OPEN;
            msg.obj = contentType;
            msg.sendToTarget();
            //after user click view, and the error toast is shown, we must make it can press again.
            // tricky code
            if (context instanceof ComposeMessageActivity) {
                ((ComposeMessageActivity)context).mClickCanResponse = true;
            }
        }
    }

    public static void showErrorDialog(Activity activity,
            String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setIcon(R.drawable.ic_sms_mms_not_delivered);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    dialog.dismiss();
                }
            }
        });
        if (!activity.isFinishing()) {
            builder.show();
        }
    }

    /**
     * The quality parameter which is used to compress JPEG images.
     */
    public static final int IMAGE_COMPRESSION_QUALITY = 80;
    /**
     * The minimum quality parameter which is used to compress JPEG images.
     */
    public static final int MINIMUM_IMAGE_COMPRESSION_QUALITY = 50;

    public static Uri saveBitmapAsPart(Context context, Uri messageUri, Bitmap bitmap)
            throws MmsException {

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.JPEG, IMAGE_COMPRESSION_QUALITY, os);

        PduPart part = new PduPart();

        part.setContentType("image/jpeg".getBytes());
        String contentId = "Image" + System.currentTimeMillis();
        part.setContentLocation((contentId + ".jpg").getBytes());
        part.setContentId(contentId.getBytes());
        part.setData(os.toByteArray());

        Uri retVal = PduPersister.getPduPersister(context).persistPart(part,
                        ContentUris.parseId(messageUri));

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("saveBitmapAsPart: persisted part with uri=" + retVal);
        }

        return retVal;
    }

    /**
     * Message overhead that reduces the maximum image byte size.
     * 5000 is a realistic overhead number that allows for user to also include
     * a small MIDI file or a couple pages of text along with the picture.
     */
    public static final int MESSAGE_OVERHEAD = 5000;

    public static void resizeImageAsync(final Context context,
            final Uri imageUri, final Handler handler,
            final ResizeImageResultCallback cb,
            final boolean append) {

        // Show a progress toast if the resize hasn't finished
        // within one second.
        // Stash the runnable for showing it away so we can cancel
        // it later if the resize completes ahead of the deadline.
        final Runnable showProgress = new Runnable() {
            public void run() {
                Toast.makeText(context, R.string.compressing, Toast.LENGTH_SHORT).show();
            }
        };
        
        // Schedule it for one second from now.
        handler.postDelayed(showProgress, 1000);

        new Thread(new Runnable() {
            public void run() {
                final PduPart part;
                try {
                    UriImage image = new UriImage(context, imageUri);
                    part = image.getResizedImageAsPart(
                        MmsConfig.getMaxImageWidth(),
                        MmsConfig.getMaxImageHeight(),
                        MmsConfig.getUserSetMmsSizeLimit(true) - MESSAGE_OVERHEAD);
                    handler.post(new Runnable() {
                        public void run() {
                            cb.onResizeResult(part, append);
                        }
                    });
                } catch (IllegalArgumentException e){
                    Xlog.e(TAG, "Unexpected IllegalArgumentException.", e);
                } 
                finally {
                    // Cancel pending show of the progress toast if necessary.
                    handler.removeCallbacks(showProgress);
                }

                
            }
        }).start();
    }

    public static void resizeImage(final Context context, final Uri imageUri, final Handler handler,
            final ResizeImageResultCallback cb, final boolean append, boolean showToast) {

        // Show a progress toast if the resize hasn't finished
        // within one second.
        // Stash the runnable for showing it away so we can cancel
        // it later if the resize completes ahead of the deadline.
        final Runnable showProgress = new Runnable() {
            public void run() {
                Toast.makeText(context, R.string.compressing, Toast.LENGTH_SHORT).show();
            }
        };
        if (showToast) {
            handler.post(showProgress);
//            handler.postDelayed(showProgress, 1000);
        }
        final PduPart part;
        try {
            UriImage image = new UriImage(context, imageUri);
            part = image.getResizedImageAsPart(MmsConfig.getMaxImageWidth(), MmsConfig.getMaxImageHeight(), MmsConfig
                    .getUserSetMmsSizeLimit(true)
                - MESSAGE_OVERHEAD);
            cb.onResizeResult(part, append);
        } catch (IllegalArgumentException e) {
            Xlog.e(TAG, "Unexpected IllegalArgumentException.", e);
        }finally {
            // Cancel pending show of the progress toast if necessary.
            handler.removeCallbacks(showProgress);
        }
    }

    public static void showDiscardDraftConfirmDialog(Context context,
            OnClickListener listener) {
        new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.discard_message)
                .setMessage(R.string.discard_message_reason)
                .setPositiveButton(R.string.yes, listener)
                .setNegativeButton(R.string.no, null)
                .show();
    }

    public static String getLocalNumber() {
        if (null == sLocalNumber) {
            sLocalNumber = MmsApp.getApplication().getTelephonyManager().getLine1Number();
        }
        return sLocalNumber;
    }

    public static String getLocalNumberGemini(int simId) {
        // convert sim id to slot id
        int slotId = SIMInfo.getSlotById(MmsApp.getApplication().getApplicationContext(), simId);
        if (Phone.GEMINI_SIM_1 == slotId) {
            if (null == sLocalNumber) {
                sLocalNumber = MmsApp.getApplication().getTelephonyManager().getLine1NumberGemini(slotId);
            }
            Xlog.d(MmsApp.TXN_TAG, "getLocalNumberGemini, Sim ID=" + simId + ", slot ID=" + slotId +", lineNumber=" + sLocalNumber);

            return sLocalNumber;
        } else if (Phone.GEMINI_SIM_2 == slotId) {
            if (null == sLocalNumber2) {
                sLocalNumber2 = MmsApp.getApplication().getTelephonyManager().getLine1NumberGemini(slotId);
            }
            Xlog.d(MmsApp.TXN_TAG, "getLocalNumberGemini, Sim ID=" + simId + ", slot ID=" + slotId +", lineNumber=" + sLocalNumber);
            return sLocalNumber2;
        } else {
            Xlog.e(MmsApp.TXN_TAG, "getLocalNumberGemini, illegal slot ID");
            return null;
        }
    }

    public static boolean isLocalNumber(String number) {
        if (number == null) {
            return false;
        }

        // we don't use Mms.isEmailAddress() because it is too strict for comparing addresses like
        // "foo+caf_=6505551212=tmomail.net@gmail.com", which is the 'from' address from a forwarded email
        // message from Gmail. We don't want to treat "foo+caf_=6505551212=tmomail.net@gmail.com" and
        // "6505551212" to be the same.
        if (number.indexOf('@') >= 0) {
            return false;
        }

        // add for gemini
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            return PhoneNumberUtils.compare(number, getLocalNumberGemini(Phone.GEMINI_SIM_1))
                   || PhoneNumberUtils.compare(number, getLocalNumberGemini(Phone.GEMINI_SIM_2));
        } else {
            return PhoneNumberUtils.compare(number, getLocalNumber());
        }
    }

    public static void handleReadReport(final Context context,
            final long threadId,
            final int status,
            final Runnable callback) {
        String selection = Mms.MESSAGE_TYPE + " = " + PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF
            + " AND " + Mms.READ + " = 0"
            + " AND " + Mms.READ_REPORT + " = " + PduHeaders.VALUE_YES;

        if (threadId != -1) {
            selection = selection + " AND " + Mms.THREAD_ID + " = " + threadId;
        }

        final Cursor c = SqliteWrapper.query(context, context.getContentResolver(),
                        Mms.Inbox.CONTENT_URI, new String[] {Mms._ID, Mms.MESSAGE_ID},
                        selection, null, null);

        if (c == null) {
            return;
        }

        final Map<String, String> map = new HashMap<String, String>();
        try {
            if (c.getCount() == 0) {
                if (callback != null) {
                    callback.run();
                }
                return;
            }

            while (c.moveToNext()) {
                Uri uri = ContentUris.withAppendedId(Mms.CONTENT_URI, c.getLong(0));
                map.put(c.getString(1), AddressUtils.getFrom(context, uri));
            }
        } finally {
            c.close();
        }

        OnClickListener positiveListener = new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                for (final Map.Entry<String, String> entry : map.entrySet()) {
                    MmsMessageSender.sendReadRec(context, entry.getValue(),
                                                 entry.getKey(), status);
                }

                if (callback != null) {
                    callback.run();
                }
                dialog.dismiss();
            }
        };

        OnClickListener negativeListener = new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (callback != null) {
                    callback.run();
                }
                dialog.dismiss();
            }
        };

        OnCancelListener cancelListener = new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                if (callback != null) {
                    callback.run();
                }
            }
        };

        confirmReadReportDialog(context, positiveListener,
                                         negativeListener,
                                         cancelListener);
    }

    // add for gemini
    public static void handleReadReportGemini(final Context context,
            final long threadId,
            final int status,
            final Runnable callback) {
        String selection = Mms.MESSAGE_TYPE + " = " + PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF
            + " AND " + Mms.READ + " = 0"
            + " AND " + Mms.READ_REPORT + " = " + PduHeaders.VALUE_YES;

        if (threadId != -1) {
            selection = selection + " AND " + Mms.THREAD_ID + " = " + threadId;
        }

        final Cursor c = SqliteWrapper.query(context, context.getContentResolver(),
                        Mms.Inbox.CONTENT_URI, new String[] {Mms._ID, Mms.MESSAGE_ID, Mms.SIM_ID},
                        selection, null, null);

        if (c == null) {
            return;
        }

        final Map<String, String> map = new HashMap<String, String>();
        try {
            if (c.getCount() == 0) {
                if (callback != null) {
                    callback.run();
                }
                return;
            }

            while (c.moveToNext()) {
                Uri uri = ContentUris.withAppendedId(Mms.CONTENT_URI, c.getLong(0));
                map.put(c.getString(1), AddressUtils.getFrom(context, uri));
            }
        } finally {
            c.close();
        }

        OnClickListener positiveListener = new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                for (final Map.Entry<String, String> entry : map.entrySet()) {
                    MmsMessageSender.sendReadRec(context, entry.getValue(),
                                                 entry.getKey(), status);
                }

                if (callback != null) {
                    callback.run();
                }
            }
        };

        OnClickListener negativeListener = new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (callback != null) {
                    callback.run();
                }
            }
        };

        OnCancelListener cancelListener = new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                if (callback != null) {
                    callback.run();
                }
            }
        };

        confirmReadReportDialog(context, positiveListener,
                                         negativeListener,
                                         cancelListener);
    }

    private static void confirmReadReportDialog(Context context,
            OnClickListener positiveListener, OnClickListener negativeListener,
            OnCancelListener cancelListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        builder.setTitle(R.string.confirm);
        builder.setMessage(R.string.message_send_read_report);
        builder.setPositiveButton(R.string.yes, positiveListener);
        builder.setNegativeButton(R.string.no, negativeListener);
        builder.setOnCancelListener(cancelListener);
        builder.show();
    }

    public static String extractEncStrFromCursor(Cursor cursor,
            int columnRawBytes, int columnCharset) {
        String rawBytes = cursor.getString(columnRawBytes);
        int charset = cursor.getInt(columnCharset);

        if (TextUtils.isEmpty(rawBytes)) {
            return "";
        } else if (charset == CharacterSets.ANY_CHARSET) {
            return rawBytes;
        } else {
            return new EncodedStringValue(charset, PduPersister.getBytes(rawBytes)).getString();
        }
    }

    private static String extractEncStr(Context context, EncodedStringValue value) {
        if (value != null) {
            return value.getString();
        } else {
            return "";
        }
    }

    public static ArrayList<String> extractUris(URLSpan[] spans) {
        int size = spans.length;
        ArrayList<String> accumulator = new ArrayList<String>();

        for (int i = 0; i < size; i++) {
            accumulator.add(spans[i].getURL());
        }
        return accumulator;
    }

    /**
     * Play/view the message attachments.
     * TOOD: We need to save the draft before launching another activity to view the attachments.
     *       This is hacky though since we will do saveDraft twice and slow down the UI.
     *       We should pass the slideshow in intent extra to the view activity instead of
     *       asking it to read attachments from database.
     * @param context
     * @param msgUri the MMS message URI in database
     * @param slideshow the slideshow to save
     * @param persister the PDU persister for updating the database
     * @param sendReq the SendReq for updating the database
     */
    public static void viewMmsMessageAttachment(Context context, Uri msgUri,
            SlideshowModel slideshow) {
    	if (msgUri == null){
    		return;
    	}
        boolean isSimple = (slideshow == null) ? false : slideshow.isSimple();
        SlideModel slide = null;
        if (slideshow != null){
            // If a slideshow was provided, save it to disk first.
                PduPersister persister = PduPersister.getPduPersister(context);
                try {
                    PduBody pb = slideshow.toPduBody();
                    persister.updateParts(msgUri, pb);
                    slideshow.sync(pb);
                } catch (MmsException e) {
                    Log.e(TAG, "Unable to save message for preview");
                    return;
                }
                slide = slideshow.get(0);
            }
        if (isSimple && slide!=null && !slide.hasAudio()) {
            // In attachment-editor mode, we only ever have one slide.
            MessageUtils.viewSimpleSlideshow(context, slideshow);
            
        } else {
            // Launch the slideshow activity to play/view.
            Intent intent = new Intent(context, SlideshowActivity.class);
            intent.setData(msgUri);
            context.startActivity(intent);
        }
    }

    public static void viewMmsMessageAttachmentMini(Context context, Uri msgUri,
            SlideshowModel slideshow) {
    	if (msgUri == null){
    		return;
    	}
        boolean isSimple = (slideshow == null) ? false : slideshow.isSimple();
        if (slideshow != null){
            // If a slideshow was provided, save it to disk first.
                PduPersister persister = PduPersister.getPduPersister(context);
                try {
                    PduBody pb = slideshow.toPduBody();
                    persister.updateParts(msgUri, pb);
                    slideshow.sync(pb);
                } catch (MmsException e) {
                    Xlog.e(TAG, "Unable to save message for preview");
                    return;
                }
            }

            // Launch the slideshow activity to play/view.
            Intent intent = new Intent(context, SlideshowActivity.class);
            intent.setData(msgUri);
            context.startActivity(intent);
        
    }

    public static void viewMmsMessageAttachment(Context context, WorkingMessage msg) {
        SlideshowModel slideshow = msg.getSlideshow();
//        if (slideshow == null) {
//            throw new IllegalStateException("msg.getSlideshow() == null");
//        }
        /**
        if (slideshow.isSimple()) {
            MessageUtils.viewSimpleSlideshow(context, slideshow);
        } else {
            Uri uri = msg.saveAsMms(false);
            viewMmsMessageAttachment(context, uri, slideshow);
        }**/
        Uri uri = msg.saveAsMms(false);
        viewMmsMessageAttachment(context, uri, slideshow);
    }

    /**
     * Debugging
     */
    public static void writeHprofDataToFile(){
        String filename = Environment.getExternalStorageDirectory() + "/mms_oom_hprof_data";
        try {
            android.os.Debug.dumpHprofData(filename);
            Log.i(TAG, "##### written hprof data to " + filename);
        } catch (IOException ex) {
            Log.e(TAG, "writeHprofDataToFile: caught " + ex);
        }
    }

    public static boolean isAlias(String string) {
        if (!MmsConfig.isAliasEnabled()) {
            return false;
        }

        if (TextUtils.isEmpty(string)) {
            return false;
        }

        // TODO: not sure if this is the right thing to use. Mms.isPhoneNumber() is
        // intended for searching for things that look like they might be phone numbers
        // in arbitrary text, not for validating whether something is in fact a phone number.
        // It will miss many things that are legitimate phone numbers.
        if (Mms.isPhoneNumber(string)) {
            return false;
        }

        if (!isAlphaNumeric(string)) {
            return false;
        }

        int len = string.length();

        if (len < MmsConfig.getAliasMinChars() || len > MmsConfig.getAliasMaxChars()) {
            return false;
        }

        return true;
    }

    public static boolean isAlphaNumeric(String s) {
        char[] chars = s.toCharArray();
        for (int x = 0; x < chars.length; x++) {
            char c = chars[x];

            if ((c >= 'a') && (c <= 'z')) {
                continue;
            }
            if ((c >= 'A') && (c <= 'Z')) {
                continue;
            }
            if ((c >= '0') && (c <= '9')) {
                continue;
            }

            return false;
        }
        return true;
    }




    /**
     * Given a phone number, return the string without syntactic sugar, meaning parens,
     * spaces, slashes, dots, dashes, etc. If the input string contains non-numeric
     * non-punctuation characters, return null.
     */
    private static String parsePhoneNumberForMms(String address) {
        StringBuilder builder = new StringBuilder();
        int len = address.length();

        for (int i = 0; i < len; i++) {
            char c = address.charAt(i);

            // accept the first '+' in the address
            if (c == '+' && builder.length() == 0) {
                builder.append(c);
                continue;
            }

            if (Character.isDigit(c)) {
                builder.append(c);
                continue;
            }

            if (numericSugarMap.get(c) == null) {
                return null;
            }
        }
        return builder.toString();
    }

    /**
     * Returns true if the address passed in is a valid MMS address.
     */
    public static boolean isValidMmsAddress(String address) {
        String retVal = parseMmsAddress(address);
        return (retVal != null && !retVal.equals(""));
    }

    /**
     * parse the input address to be a valid MMS address.
     * - if the address is an email address, leave it as is.
     * - if the address can be parsed into a valid MMS phone number, return the parsed number.
     * - if the address is a compliant alias address, leave it as is.
     */
    public static String parseMmsAddress(String address) {
        // if it's a valid Email address, use that.
        if (Mms.isEmailAddress(address)) {
            return address;
        }

        // if we are able to parse the address to a MMS compliant phone number, take that.
        String retVal = parsePhoneNumberForMms(address);
        if (retVal != null) {
            return retVal;
        }

        // if it's an alias compliant address, use that.
        if (isAlias(address)) {
            return address;
        }

        // it's not a valid MMS address, return null
        return null;
    }

    private static void log(String msg) {
        Log.d(TAG, "[MsgUtils] " + msg);
    }
    
    //wappush: add this function to handle the url string, if it does not contain the http or https schema, then add http schema manually.
    public static String CheckAndModifyUrl(String url){
        if(url==null){
            return null;
        }

        Uri uri = Uri.parse(url);
        if(uri.getScheme() != null ){
            return url;
        }

        return "http://" + url;
    }

    public static void selectRingtone(Context context, int requestCode) {
        if (context instanceof Activity) {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_INCLUDE_DRM, false);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
                    context.getString(R.string.select_audio));
            ((Activity) context).startActivityForResult(intent, requestCode);
        }
    }

    public static Map<Integer, CharSequence> simInfoMap = new HashMap<Integer, CharSequence>();
    public static CharSequence getSimInfo(Context context, int simId){
        Xlog.d(TAG, "getSimInfo simId = " + simId);
        if (simInfoMap.containsKey(simId)) {
            Xlog.d(TAG, "MessageUtils.getSimInfo(): getCache");
            return simInfoMap.get(simId);
        }
        //get sim info
        SIMInfo simInfo = SIMInfo.getSIMInfoById(context, simId);
        if(null != simInfo){
            String displayName = simInfo.mDisplayName;

            Xlog.d(TAG, "SIMInfo simId=" + simInfo.mSimId + " mDisplayName=" + displayName);

            if(null == displayName){
                simInfoMap.put(simId, "");
                return "";
            }

            SpannableStringBuilder buf = new SpannableStringBuilder();
            buf.append(" ");
            if (displayName.length() <= 6) {
                buf.append(displayName);
            } else {
                buf.append(displayName.substring(0,3) + ".." + displayName.charAt(displayName.length() - 1));
            }
            buf.append(" ");

            //set background image
            int colorRes = (simInfo.mSlot >= 0) ? simInfo.mSimBackgroundRes : com.mediatek.internal.R.drawable.sim_background_locked;
            Drawable drawable = context.getResources().getDrawable(colorRes);
            buf.setSpan(new BackgroundImageSpan(colorRes, drawable), 0, buf.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //set simInfo color
            int color = context.getResources().getColor(R.color.siminfo_color);
            buf.setSpan(new ForegroundColorSpan(color), 0, buf.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //buf.setSpan(new StyleSpan(Typeface.BOLD),0, buf.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            simInfoMap.put(simId, buf);
            return buf;
        }
        simInfoMap.put(simId, "");
        return "";
    }

    public static void addNumberOrEmailtoContact(final String numberOrEmail, final int REQUEST_CODE,
            final Activity activity) {
        if (!TextUtils.isEmpty(numberOrEmail)) {
            String message = activity.getResources().getString(R.string.add_contact_dialog_message, numberOrEmail);
            AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle(numberOrEmail).setMessage(message);
            AlertDialog dialog = builder.create();
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, activity.getResources().getString(
                    R.string.add_contact_dialog_existing), new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                    intent.setType(Contacts.CONTENT_ITEM_TYPE);
                    if (Mms.isEmailAddress(numberOrEmail)) {
                        intent.putExtra(ContactsContract.Intents.Insert.EMAIL, numberOrEmail);
                    } else {
                        intent.putExtra(ContactsContract.Intents.Insert.PHONE, numberOrEmail);
                    }
                    if (REQUEST_CODE > 0) {
                        activity.startActivityForResult(intent, REQUEST_CODE);
                    } else {
                        activity.startActivity(intent);
                    }
                }
            });

            dialog.setButton(AlertDialog.BUTTON_NEGATIVE, activity.getResources()
                    .getString(R.string.add_contact_dialog_new), new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    final Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                    if (Mms.isEmailAddress(numberOrEmail)) {
                        intent.putExtra(ContactsContract.Intents.Insert.EMAIL, numberOrEmail);
                    } else {
                        intent.putExtra(ContactsContract.Intents.Insert.PHONE, numberOrEmail);
                    }
                    if (REQUEST_CODE > 0) {
                        activity.startActivityForResult(intent, REQUEST_CODE);
                    } else {
                        activity.startActivity(intent);
                    }
                }
            });
            dialog.show();
        } else {
            
        }
    }
    public static boolean checkUriContainsDrm(Context context, Uri uri) {
        String uripath = convertUriToPath(context, uri);
        String extName = uripath.substring(uripath.lastIndexOf('.') + 1);
        if (extName.equals("dcf")) {
        	return true;
        }
        return false;
    }
    private static String convertUriToPath(Context context, Uri uri) {
        String path = null;
        if (null != uri) {
            String scheme = uri.getScheme();
            if (null == scheme || scheme.equals("")
                    || scheme.equals(ContentResolver.SCHEME_FILE)) {
                path = uri.getPath();
            } else if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
                String[] projection = new String[] { MediaStore.MediaColumns.DATA };
                Cursor cursor = null;
                try {
                    cursor = context.getContentResolver().query(uri,
                            projection, null, null, null);
                    if (null == cursor || 0 == cursor.getCount()
                            || !cursor.moveToFirst()) {
                        throw new IllegalArgumentException(
                                "Given Uri could not be found"
                                        + " in media store");
                    }
                    int pathIndex = cursor
                            .getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                    path = cursor.getString(pathIndex);
                } catch (SQLiteException e) {
                    throw new IllegalArgumentException(
                            "Given Uri is not formatted in a way "
                                    + "so that it can be found in media store.");
                } finally {
                    if (null != cursor) {
                        cursor.close();
                    }
                }
            } else {
                throw new IllegalArgumentException(
                        "Given Uri scheme is not supported");
            }
        }
        return path;
    }

    /**
     * Return the current storage status.
     */
    public static String getStorageStatus(Context context) {
        // we need count only
        final String[] PROJECTION = new String[] {
            BaseColumns._ID,
        };
        final ContentResolver cr = context.getContentResolver();
        final Resources res = context.getResources();
        Cursor cursor = null;
        
        StringBuilder buffer = new StringBuilder();
        // Mms count
        cursor = cr.query(Mms.CONTENT_URI, PROJECTION, null, null, null);
        int mmsCount = 0;
        if (cursor != null) {
            mmsCount = cursor.getCount();
            cursor.close();
        }
        buffer.append(res.getString(R.string.storage_dialog_mms, mmsCount));
        buffer.append("\n");
        // Sms count
        cursor = cr.query(Sms.CONTENT_URI, PROJECTION, null, null, null);
        int smsCount = 0;
        if (cursor != null) {
            smsCount = cursor.getCount();
            cursor.close();
        }
        buffer.append(res.getString(R.string.storage_dialog_sms, smsCount));
        buffer.append("\n");
        // Attachment size
        final String sizeTag = getHumanReadableSize(getAttachmentsSize(cr));
        buffer.append(res.getString(R.string.storage_dialog_attachments) + sizeTag);
        buffer.append("\n");
        // Database size
        final File db = new File("/data/data/com.android.providers.telephony/databases/mmssms.db");
        final long dbsize = db.length();
        buffer.append(res.getString(R.string.storage_dialog_database) + getHumanReadableSize(dbsize));
        buffer.append("\n");
        // Available space
        final StatFs datafs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
        final long availableSpace = datafs.getAvailableBlocks() * datafs.getBlockSize();
        buffer.append(res.getString(R.string.storage_dialog_available_space) + getHumanReadableSize(availableSpace));
        return buffer.toString();
    }

    private static long getAttachmentsSize(ContentResolver cr) {
        String[] projs = new String[] {
            Mms.Part._DATA
        };
        // TODO: is there a predefined Uri like Mms.CONTENT_URI?
        final Uri part = Uri.parse("content://mms/part/");
        Cursor cursor = cr.query(part, projs, null, null, null);
        long size = 0;
        if (cursor == null || !cursor.moveToFirst()) {
            Xlog.e(TAG, "getAttachmentsSize, cursor is empty or null");
            return size;
        }
        Xlog.i(TAG, "getAttachmentsSize, count " + cursor.getCount());
        do {
            final String data = cursor.getString(0);
            if (data != null) {
                File file = new File(data);
                size += file.length();
            }
        } while (cursor.moveToNext());
        if (cursor != null) {
            cursor.close();
        }
        return size;
    }

    public static String getHumanReadableSize(long size) {
        String tag;
        float fsize = (float) size;
        if (size < 1024L) {
            tag = String.valueOf(size) + "B";
        } else if (size < 1024L*1024L) {
            fsize /= 1024.0f;
            tag = String.format(Locale.ENGLISH, "%.2f", fsize) + "KB";
        } else {
            fsize /= 1024.0f * 1024.0f;
            tag = String.format(Locale.ENGLISH, "%.2f", fsize) + "MB";
        }
        return tag;
    }
    public static void previewVCard(final Context context, final Uri targetUri) {
        if (targetUri == null) {
            return;
        }
        final ProgressDialog dialog = ProgressDialog.show(context, context.getString(R.string.parse_vcard),
                context.getString(R.string.please_wait), true, true);
        
        final ContentResolver resolver = context.getContentResolver();
        new Thread() {
            boolean parseResult = false;
            final VCardEntryCounter counter = new VCardEntryCounter();
            final VCardSourceDetector detector = new VCardSourceDetector();
            final VCardInterpreterCollection builderCollection =
                    new VCardInterpreterCollection(Arrays.asList(counter, detector));
            public void run() {
                try {
                    try {
                        // We don't know which type should be useld to parse the Uri.
                        // It is possble to misinterpret the vCard, but we expect the parser
                        // lets VCardSourceDetector detect the type before the misinterpretation.
                        parseResult = readOneVCardFile(targetUri, VCardConfig.VCARD_TYPE_UNKNOWN,
                                builderCollection, true, resolver);
                    } catch (VCardNestedException e) {
                        try {
                            // Assume that VCardSourceDetector was able to detect the source.
                            // Try again with the detector.
                            parseResult = readOneVCardFile(targetUri, detector.getEstimatedType(),
                                    counter, false, resolver);
                        } catch (VCardNestedException e2) {
                            parseResult = false;
                        }
                    }
                } finally {
                    dialog.dismiss();
                    if (!parseResult) {
                        Toast.makeText(context, R.string.failed_to_parse_vcard, Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (counter.getCount() == 1) {
                        Intent intent = new Intent(context, VCardDetailSingle.class);
                        intent.setData(targetUri);
                        intent.putExtra("item_id", 0);
                        intent.putExtra("need_parse", true);
                        intent.putExtra("type", detector.getEstimatedType());
                        intent.putExtra("charset", detector.getEstimatedCharset());
                        context.startActivity(intent);
                    } else if (counter.getCount() > 1) {
                        Intent intent = new Intent(context, VCardDetailMulti.class);
                        intent.setData(targetUri);
                        intent.putExtra("type", detector.getEstimatedType());
                        intent.putExtra("charset", detector.getEstimatedCharset());
                        context.startActivity(intent);
                    } else {
                        Toast.makeText(context, R.string.empty_vcard, Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }
        }.start();
    }
    
    public static boolean readOneVCardFile(Uri uri, int vcardType, VCardInterpreter interpreter,
            boolean throwNestedException, ContentResolver resolver) throws VCardNestedException {
        InputStream inputStream = null;
        try {
            inputStream = resolver.openInputStream(uri);
            VCardParser vcardParser = new VCardParser_V21(vcardType);

            try {
                vcardParser.parse(inputStream, interpreter);
            } catch (VCardVersionException e1) {
                // version seems not right, retry with another version
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    // Silent exception when closing stream, because we retry parse later
                }
                // Let the object collector clean up internal temporal objects, so that try again
                if (interpreter instanceof VCardEntryConstructor) {
                    ((VCardEntryConstructor)interpreter).clear();
                } else if (interpreter instanceof VCardInterpreterCollection) {
                    for (VCardInterpreter elem : ((VCardInterpreterCollection) interpreter).getCollection()) {
                        if (elem instanceof VCardEntryConstructor) {
                            ((VCardEntryConstructor)elem).clear();
                        }
                    }
                }

                // reopen the input stream
                inputStream = resolver.openInputStream(uri);
                try {
                    vcardParser = new VCardParser_V30(vcardType);
                    vcardParser.parse(inputStream, interpreter);
                } catch (VCardVersionException e2) {
                    throw new VCardException("vCard with unspported version." + e2.getMessage());
                }
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException was emitted: " + e.getMessage());
            return false;
        } catch (VCardNotSupportedException e) {
            if ((e instanceof VCardNestedException) && throwNestedException) {
                throw (VCardNestedException)e;
            }
            return false;
        } catch (VCardException e) {
            return false;
        }
        return true;
    }

    public static int getSimStatusResource(int state) {
        Xlog.i(TAG, "SIM state is " + state);
        switch (state) {
            /** 1, RADIOOFF : has SIM/USIM inserted but not in use . */
            case Phone.SIM_INDICATOR_RADIOOFF:
                return com.mediatek.internal.R.drawable.sim_radio_off;

            /** 2, LOCKED : has SIM/USIM inserted and the SIM/USIM has been locked. */
            case Phone.SIM_INDICATOR_LOCKED:
                return com.mediatek.internal.R.drawable.sim_locked;

            /** 3, INVALID : has SIM/USIM inserted and not be locked but failed to register to the network. */
            case Phone.SIM_INDICATOR_INVALID:
                return com.mediatek.internal.R.drawable.sim_invalid;

            /** 4, SEARCHING : has SIM/USIM inserted and SIM/USIM state is Ready and is searching for network. */
            case Phone.SIM_INDICATOR_SEARCHING:
                return com.mediatek.internal.R.drawable.sim_searching;

            /** 6, ROAMING : has SIM/USIM inserted and in roaming service(has no data connection). */  
            case Phone.SIM_INDICATOR_ROAMING:
                return com.mediatek.internal.R.drawable.sim_roaming;

            /** 7, CONNECTED : has SIM/USIM inserted and in normal service(not roaming) and data connected. */
            case Phone.SIM_INDICATOR_CONNECTED:
                return com.mediatek.internal.R.drawable.sim_connected;

            /** 8, ROAMINGCONNECTED = has SIM/USIM inserted and in roaming service(not roaming) and data connected.*/
            case Phone.SIM_INDICATOR_ROAMINGCONNECTED:
                return com.mediatek.internal.R.drawable.sim_roaming_connected;

            /** -1, UNKNOWN : invalid value */
            case Phone.SIM_INDICATOR_UNKNOWN:

            /** 0, ABSENT, no SIM/USIM card inserted for this phone */
            case Phone.SIM_INDICATOR_ABSENT:

            /** 5, NORMAL = has SIM/USIM inserted and in normal service(not roaming and has no data connection). */
            case Phone.SIM_INDICATOR_NORMAL:
            default:
                return Phone.SIM_INDICATOR_UNKNOWN;
        }
    }

    public static int getSimStatus(int id, List<SIMInfo> simInfoList, TelephonyManagerEx telephonyManager) {
        int slotId = simInfoList.get(id).mSlot;
        if (slotId != -1) {
            return telephonyManager.getSimIndicatorStateGemini(slotId);
        }
        return -1;
    }

    public static boolean is3G(int id, List<SIMInfo> simInfoList) {
        int slotId = simInfoList.get(id).mSlot;
        Xlog.i(TAG, "SIMInfo.getSlotById id: " + id + " slotId: " + slotId);
        if (slotId == get3GCapabilitySIM()) {
            return true;
        }
        return false;
    }

    public static int get3GCapabilitySIM() {
        int retval = -1;
        if(FeatureOption.MTK_GEMINI_3G_SWITCH) {
            ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
            try {
                retval = ((telephony == null) ? -1 : telephony.get3GCapabilitySIM());
            } catch(RemoteException e) {
                Xlog.e(TAG, "get3GCapabilitySIM(): throw RemoteException");
            }
        }
        return retval;
    }

    public static boolean isVCalendarAvailable(Context context) {
        final Intent intent = new Intent("android.intent.action.CALENDARCHOICE");
        intent.setType("text/x-vcalendar");
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }
}
