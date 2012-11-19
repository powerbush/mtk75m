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

import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.ReadRecInd;
import com.google.android.mms.pdu.EncodedStringValue;
import com.android.mms.ui.MessageUtils;

import android.content.Context;
import android.net.Uri;
import android.provider.Telephony.Mms.Sent;
import android.provider.Telephony.SIMInfo;
import android.util.Config;
import android.util.Log;

import java.io.IOException;
import com.android.mms.MmsApp;

import com.mediatek.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;
import com.mediatek.xlog.SXlog;


/**
 * The ReadRecTransaction is responsible for sending read report
 * notifications (M-read-rec.ind) to clients that have requested them.
 * It:
 *
 * <ul>
 * <li>Loads the read report indication from storage (Outbox).
 * <li>Packs M-read-rec.ind and sends it.
 * <li>Notifies the TransactionService about succesful completion.
 * </ul>
 */
public class ReadRecTransaction extends Transaction {
    private static final String TAG = "ReadRecTransaction";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;

    private final Uri mReadReportURI;

    public ReadRecTransaction(Context context,
            int transId,
            TransactionSettings connectionSettings,
            String uri) {
        super(context, transId, connectionSettings);
        mReadReportURI = Uri.parse(uri);
        mId = uri;

        // Attach the transaction to the instance of RetryScheduler.
        attach(RetryScheduler.getInstance(context));
    }

    // add for gemini
    public ReadRecTransaction(Context context,
            int transId, int simId,
            TransactionSettings connectionSettings,
            String uri) {
        super(context, transId, connectionSettings);
        mReadReportURI = Uri.parse(uri);
        mId = uri;
        mSimId = simId;

        // Attach the transaction to the instance of RetryScheduler.
        attach(RetryScheduler.getInstance(context));
    }

    /*
     * (non-Javadoc)
     * @see com.android.mms.Transaction#process()
     */
    @Override
    public void process() {
        Xlog.d(MmsApp.TXN_TAG, "ReadRecTransaction: process()");
        PduPersister persister = PduPersister.getPduPersister(mContext);

        try {
            // Load M-read-rec.ind from outbox
            ReadRecInd readRecInd = (ReadRecInd) persister.load(mReadReportURI);

            // insert the 'from' address per spec
            readRecInd.setFrom(new EncodedStringValue(PduHeaders.FROM_INSERT_ADDRESS_TOKEN_STR));
            byte[] postingData = new PduComposer(mContext, readRecInd).make();
            sendPdu(postingData);

            Uri uri = persister.move(mReadReportURI, Sent.CONTENT_URI);
            mTransactionState.setState(TransactionState.SUCCESS);
            mTransactionState.setContentUri(uri);
        } catch (IOException e) {
            if (LOCAL_LOGV) {
                Log.v(TAG, "Failed to send M-Read-Rec.Ind.", e);
            }
        } catch (MmsException e) {
            if (LOCAL_LOGV) {
                Log.v(TAG, "Failed to load message from Outbox.", e);
            }
        } catch (RuntimeException e) {
            if (LOCAL_LOGV) {
                Log.e(TAG, "Unexpected RuntimeException.", e);
            }
        } finally {
            if (mTransactionState.getState() != TransactionState.SUCCESS) {
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setContentUri(mReadReportURI);
            }
            notifyObservers();
        }
    }

    @Override
    public int getType() {
        return READREC_TRANSACTION;
    }

    // add for gemini
    public Uri getRrecTrxnUri() {
        return mReadReportURI;
    }
}
