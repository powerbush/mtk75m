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
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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
import com.android.mms.ui.ManageSimMessages;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.provider.Telephony;
import com.mediatek.featureoption.FeatureOption;
import com.android.internal.telephony.Phone;
import android.provider.Telephony.SIMInfo;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.util.Log;

import com.mediatek.telephony.TelephonyManagerEx;
import com.android.mms.ui.MessageUtils;
/**
 * Receive Intent.SIM_FULL_ACTION.  Handle notification that SIM is full.
 */
public class SimFullReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Settings.Secure.getInt(context.getContentResolver(),
            Settings.Secure.DEVICE_PROVISIONED, 0) == 1 &&
            Telephony.Sms.Intents.SIM_FULL_ACTION.equals(intent.getAction())) {
            
            NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
            Intent viewSimIntent;
            int slotId ;
            viewSimIntent = new Intent(context, ManageSimMessages.class);
            viewSimIntent.putExtra("SlotId", Phone.GEMINI_SIM_1);
            if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
            	slotId = intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, Phone.GEMINI_SIM_1);
                if (slotId == Phone.GEMINI_SIM_2) {
            	    viewSimIntent = new Intent(context, ManageSimMessages.class);
            	    viewSimIntent.putExtra("SlotId", Phone.GEMINI_SIM_2);
                } 
            }   
            
            //viewSimIntent.setAction(Intent.ACTION_VIEW);
            viewSimIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, viewSimIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Notification notification = new Notification();
            notification.icon = R.drawable.stat_sys_no_sim;
            // add for gemini
            if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
                SIMInfo simInfo = SIMInfo.getSIMInfoBySlot(context, slotId);
                if (simInfo != null) {
                    notification.simId = simInfo.mSimId;
            	    notification.simInfoType = 1;
                }
            }
            notification.tickerText = context.getString(R.string.sim_full_title);
            
            notification.defaults = Notification.DEFAULT_ALL;

            notification.setLatestEventInfo(
                    context, notification.tickerText, 
                    context.getString(R.string.sim_full_body),
                    pendingIntent);
            nm.notify(ManageSimMessages.SIM_FULL_NOTIFICATION_ID, notification);
       }
    }

}
