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

package com.android.mms.transaction;

import com.android.mms.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.provider.Telephony;
import com.android.mms.ui.ConversationList;
import android.util.Log;
import com.android.mms.MmsApp;
import com.mediatek.xlog.Xlog;
import com.mediatek.xlog.SXlog;


/**
 * Receive Intent.SMS_REJECTED.  Handle notification that received SMS messages are being
 * rejected. This can happen when the device is out of storage.
 */
public class SmsRejectedReceiver extends BroadcastReceiver {

    public static final int SMS_REJECTED_NOTIFICATION_ID = 239;

    @Override
    public void onReceive(Context context, Intent intent) {
        Xlog.d(MmsApp.TXN_TAG, "SmsRejectedReceiver: onReceive()  intent=" + intent);
        if (Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.DEVICE_PROVISIONED, 0) == 1 &&
                Telephony.Sms.Intents.SMS_REJECTED_ACTION.equals(intent.getAction())) {

            int reason = intent.getIntExtra("result", -1);
            Xlog.d(MmsApp.TXN_TAG, "Sms Rejected, reason=" + reason);
            boolean outOfMemory = reason == Telephony.Sms.Intents.RESULT_SMS_OUT_OF_MEMORY;
            if (!outOfMemory) {
                // Right now, the only user-level rejection we show to the user is out-of-memory.
                return;
            }

            NotificationManager nm = (NotificationManager)
            context.getSystemService(Context.NOTIFICATION_SERVICE);

            Intent viewConvIntent = new Intent(context, ConversationList.class);
            viewConvIntent.setAction(Intent.ACTION_VIEW);
            viewConvIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, viewConvIntent, 0);

            Notification notification = new Notification();

            // TODO: need appropriate icons
            notification.icon = R.drawable.stat_sys_no_sim;
            int titleId;
            int bodyId;
            if (outOfMemory) {
                titleId = R.string.sms_full_title;
                bodyId = R.string.sms_full_body;
            } else {
                titleId = R.string.sms_rejected_title;
                bodyId = R.string.sms_rejected_body;
            }
            notification.tickerText = context.getString(titleId);
            notification.defaults = Notification.DEFAULT_ALL;

            notification.setLatestEventInfo(
                    context, context.getString(titleId),
                    context.getString(bodyId),
                    pendingIntent);
            nm.notify(SMS_REJECTED_NOTIFICATION_ID, notification);
        }
    }

}
