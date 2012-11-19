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

import com.android.mms.util.SendingProgressTokenManager;

import android.content.Context;
import android.content.ContentUris;
import android.net.Uri;
import android.net.ConnectivityManager;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import com.android.mms.MmsApp;
import com.mediatek.xlog.Xlog;
import com.mediatek.xlog.SXlog;


/**
 * Transaction is an abstract class for notification transaction, send transaction
 * and other transactions described in MMS spec.
 * It provides the interfaces of them and some common methods for them.
 */
public abstract class Transaction extends Observable {
    private final int mServiceId;

    protected Context mContext;
    protected String mId;
    protected TransactionState mTransactionState;
    protected TransactionSettings mTransactionSettings;
    protected int mSimId;

    /**
     * Identifies push requests.
     */
    public static final int NOTIFICATION_TRANSACTION = 0;
    /**
     * Identifies deferred retrieve requests.
     */
    public static final int RETRIEVE_TRANSACTION     = 1;
    /**
     * Identifies send multimedia message requests.
     */
    public static final int SEND_TRANSACTION         = 2;
    /**
     * Identifies send read report requests.
     */
    public static final int READREC_TRANSACTION      = 3;

    public Transaction(Context context, int serviceId,
            TransactionSettings settings) {
        mContext = context;
        mTransactionState = new TransactionState();
        mServiceId = serviceId;
        mTransactionSettings = settings;
    }

    /**
     * Returns the transaction state of this transaction.
     *
     * @return Current state of the Transaction.
     */
    @Override
    public TransactionState getState() {
        return mTransactionState;
    }

    /**
     * An instance of Transaction encapsulates the actions required
     * during a MMS Client transaction.
     */
    public abstract void process();

    /**
     * Used to determine whether a transaction is equivalent to this instance.
     *
     * @param transaction the transaction which is compared to this instance.
     * @return true if transaction is equivalent to this instance, false otherwise.
     */
/*    public boolean isEquivalent(Transaction transaction) {
        return getClass().equals(transaction.getClass())
                && mId.equals(transaction.mId);
    }
*/

    public boolean isEquivalent(Transaction transaction) {
        Xlog.d(MmsApp.TXN_TAG, "Transaction: isEquivalent mId=" + mId 
            + "\t transaction.mId=" + transaction.mId);
        String Id1 = Uri.parse(mId).getLastPathSegment();
        String Id2 = Uri.parse(transaction.mId).getLastPathSegment();
        Xlog.d(MmsApp.TXN_TAG, "Transaction: isEquivalent Id1=" + Id1 + "\t Id2=" + Id2);

        return getClass().equals(transaction.getClass())
                && Uri.parse(mId).getLastPathSegment().equals(Uri.parse(transaction.mId).getLastPathSegment());
    }

    /**
     * Get the service-id of this transaction which was assigned by the framework.
     * @return the service-id of the transaction
     */
    public int getServiceId() {
        return mServiceId;
    }

    public TransactionSettings getConnectionSettings() {
        return mTransactionSettings;
    }
    public void setConnectionSettings(TransactionSettings settings) {
        mTransactionSettings = settings;
    }

    /**
     * A common method to send a PDU to MMSC.
     *
     * @param pdu A byte array which contains the data of the PDU.
     * @return A byte array which contains the response data.
     *         If an HTTP error code is returned, an IOException will be thrown.
     * @throws IOException if any error occurred on network interface or
     *         an HTTP error code(>=400) returned from the server.
     */
    protected byte[] sendPdu(byte[] pdu) throws IOException {
        return sendPdu(SendingProgressTokenManager.NO_TOKEN, pdu,
                mTransactionSettings.getMmscUrl());
    }

    /**
     * A common method to send a PDU to MMSC.
     *
     * @param pdu A byte array which contains the data of the PDU.
     * @param mmscUrl Url of the recipient MMSC.
     * @return A byte array which contains the response data.
     *         If an HTTP error code is returned, an IOException will be thrown.
     * @throws IOException if any error occurred on network interface or
     *         an HTTP error code(>=400) returned from the server.
     */
    protected byte[] sendPdu(byte[] pdu, String mmscUrl) throws IOException {
        return sendPdu(SendingProgressTokenManager.NO_TOKEN, pdu, mmscUrl);
    }

    /**
     * A common method to send a PDU to MMSC.
     *
     * @param token The token to identify the sending progress.
     * @param pdu A byte array which contains the data of the PDU.
     * @return A byte array which contains the response data.
     *         If an HTTP error code is returned, an IOException will be thrown.
     * @throws IOException if any error occurred on network interface or
     *         an HTTP error code(>=400) returned from the server.
     */
    protected byte[] sendPdu(long token, byte[] pdu) throws IOException {
        return sendPdu(token, pdu, mTransactionSettings.getMmscUrl());
    }

    /**
     * A common method to send a PDU to MMSC.
     *
     * @param token The token to identify the sending progress.
     * @param pdu A byte array which contains the data of the PDU.
     * @param mmscUrl Url of the recipient MMSC.
     * @return A byte array which contains the response data.
     *         If an HTTP error code is returned, an IOException will be thrown.
     * @throws IOException if any error occurred on network interface or
     *         an HTTP error code(>=400) returned from the server.
     */
    protected byte[] sendPdu(long token, byte[] pdu, String mmscUrl) throws IOException {
        ensureRouteToHost(mmscUrl, mTransactionSettings);
        return HttpUtils.httpConnection(
                mContext, token,
                mmscUrl,
                pdu, HttpUtils.HTTP_POST_METHOD,
                mTransactionSettings.isProxySet(),
                mTransactionSettings.getProxyAddress(),
                mTransactionSettings.getProxyPort());
    }

    /**
     * A common method to retrieve a PDU from MMSC.
     *
     * @param url The URL of the message which we are going to retrieve.
     * @return A byte array which contains the data of the PDU.
     *         If the status code is not correct, an IOException will be thrown.
     * @throws IOException if any error occurred on network interface or
     *         an HTTP error code(>=400) returned from the server.
     */
    protected byte[] getPdu(String url) throws IOException {
        ensureRouteToHost(url, mTransactionSettings);
        return HttpUtils.httpConnection(
                mContext, SendingProgressTokenManager.NO_TOKEN,
                url, null, HttpUtils.HTTP_GET_METHOD,
                mTransactionSettings.isProxySet(),
                mTransactionSettings.getProxyAddress(),
                mTransactionSettings.getProxyPort());
    }

    /**
     * Make sure that a network route exists to allow us to reach the host in the
     * supplied URL, and to the MMS proxy host as well, if a proxy is used.
     * @param url The URL of the MMSC to which we need a route
     * @param settings Specifies the address of the proxy host, if any
     * @throws IOException if the host doesn't exist, or adding the route fails.
     */
    private void ensureRouteToHost(String url, TransactionSettings settings) throws IOException {
        ConnectivityManager connMgr =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        int inetAddr;
        if (settings.isProxySet()) {
            String proxyAddr = settings.getProxyAddress();
            inetAddr = lookupHost(proxyAddr);
            if (inetAddr == -1) {
                throw new IOException("Cannot establish route for " + url + ": Unknown host");
            } else {
                if (!connMgr.requestRouteToHost(ConnectivityManager.TYPE_MOBILE_MMS, inetAddr)) {
                    throw new IOException("Cannot establish route to proxy " + inetAddr);
                }
            }
        } else {
            Uri uri = Uri.parse(url);
            inetAddr = lookupHost(uri.getHost());
            if (inetAddr == -1) {
                throw new IOException("Cannot establish route for " + url + ": Unknown host");
            } else {
                if (!connMgr.requestRouteToHost(ConnectivityManager.TYPE_MOBILE_MMS, inetAddr)) {
                    throw new IOException("Cannot establish route to " + inetAddr + " for " + url);
                }
            }
        }
    }

    /**
     * Look up a host name and return the result as an int. Works if the argument
     * is an IP address in dot notation. Obviously, this can only be used for IPv4
     * addresses.
     * @param hostname the name of the host (or the IP address)
     * @return the IP address as an {@code int} in network byte order
     */
    // TODO: move this to android-common
    public static int lookupHost(String hostname) {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            return -1;
        }
        byte[] addrBytes;
        int addr;
        addrBytes = inetAddress.getAddress();
        addr = ((addrBytes[3] & 0xff) << 24)
                | ((addrBytes[2] & 0xff) << 16)
                | ((addrBytes[1] & 0xff) << 8)
                |  (addrBytes[0] & 0xff);
        return addr;
    }

    @Override
    public String toString() {
        return getClass().getName() + ": serviceId=" + mServiceId;
    }

    /**
     * Get the type of the transaction.
     *
     * @return Transaction type in integer.
     */
    abstract public int getType();
}
