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

import com.android.common.NetworkConnectivityListener;
import com.android.mms.R;
import com.android.mms.LogTag;
import com.android.mms.MmsApp;
import com.android.mms.util.DownloadManager;
import com.android.mms.util.RateController;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.android.internal.telephony.Phone;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.provider.Telephony.SIMInfo;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;
//add for 81452
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

// add for gemini
import android.content.ContentResolver;
import android.content.ContentValues;
import android.provider.Settings;
import android.database.sqlite.SqliteWrapper;
import android.provider.Settings;
import com.mediatek.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;
import com.mediatek.xlog.SXlog;


/**
 * The TransactionService of the MMS Client is responsible for handling requests
 * to initiate client-transactions sent from:
 * <ul>
 * <li>The Proxy-Relay (Through Push messages)</li>
 * <li>The composer/viewer activities of the MMS Client (Through intents)</li>
 * </ul>
 * The TransactionService runs locally in the same process as the application.
 * It contains a HandlerThread to which messages are posted from the
 * intent-receivers of this application.
 * <p/>
 * <b>IMPORTANT</b>: This is currently the only instance in the system in
 * which simultaneous connectivity to both the mobile data network and
 * a Wi-Fi network is allowed. This makes the code for handling network
 * connectivity somewhat different than it is in other applications. In
 * particular, we want to be able to send or receive MMS messages when
 * a Wi-Fi connection is active (which implies that there is no connection
 * to the mobile data network). This has two main consequences:
 * <ul>
 * <li>Testing for current network connectivity ({@link android.net.NetworkInfo#isConnected()} is
 * not sufficient. Instead, the correct test is for network availability
 * ({@link android.net.NetworkInfo#isAvailable()}).</li>
 * <li>If the mobile data network is not in the connected state, but it is available,
 * we must initiate setup of the mobile data connection, and defer handling
 * the MMS transaction until the connection is established.</li>
 * </ul>
 */
public class TransactionService extends Service implements Observer {
    private static final String TAG = "TransactionService";

    /**
     * Used to identify notification intents broadcasted by the
     * TransactionService when a Transaction is completed.
     */
    public static final String TRANSACTION_COMPLETED_ACTION =
            "android.intent.action.TRANSACTION_COMPLETED_ACTION";

    /**
     * Action for the Intent which is sent by Alarm service to launch
     * TransactionService.
     */
    public static final String ACTION_ONALARM = "android.intent.action.ACTION_ONALARM";

    /**
     * Used as extra key in notification intents broadcasted by the TransactionService
     * when a Transaction is completed (TRANSACTION_COMPLETED_ACTION intents).
     * Allowed values for this key are: TransactionState.INITIALIZED,
     * TransactionState.SUCCESS, TransactionState.FAILED.
     */
    public static final String STATE = "state";

    /**
     * Used as extra key in notification intents broadcasted by the TransactionService
     * when a Transaction is completed (TRANSACTION_COMPLETED_ACTION intents).
     * Allowed values for this key are any valid content uri.
     */
    public static final String STATE_URI = "uri";

    /**
     * Used to identify notification intents broadcasted by the
     * TransactionService when a Transaction is Start. add for gemini smart 
     */
    public static final String TRANSACTION_START = "com.android.mms.transaction.START";

    /**
     * Used to identify notification intents broadcasted by the
     * TransactionService when a Transaction is Stop. add for gemini smart
     */
    public static final String TRANSACTION_STOP = "com.android.mms.transaction.STOP";

    private static final int EVENT_TRANSACTION_REQUEST = 1;
    private static final int EVENT_DATA_STATE_CHANGED = 2;
    private static final int EVENT_CONTINUE_MMS_CONNECTIVITY = 3;
    private static final int EVENT_HANDLE_NEXT_PENDING_TRANSACTION = 4;
    //add for time out mechanism
    private static final int EVENT_PENDING_TIME_OUT = 5;
    //add for 81452
    private static final int EVENT_SCAN_PENDING_MMS = 6;
    private static final int EVENT_QUIT = 100;

    private static final int TOAST_MSG_QUEUED = 1;
    private static final int TOAST_DOWNLOAD_LATER = 2;
    private static final int TOAST_NONE = -1;

    private static final int FAILE_TYPE_PERMANENT = 1;
    private static final int FAILE_TYPE_TEMPORARY = 2;

    private static final int REQUEST_SIM_NONE = -1;

    // temp for distinguish smart switch or dialog
    private static final boolean SMART = true;

    // 
    private boolean bWaitingConxn = false;

    //avoid stop TransactionService incorrectly.
    private boolean mNeedWait = false; 
    
    // How often to extend the use of the MMS APN while a transaction
    // is still being processed.
    private static final int APN_EXTENSION_WAIT = 8 * 30 * 1000;

    private ServiceHandler mServiceHandler;
    private Looper mServiceLooper;
    private final ArrayList<Transaction> mProcessing  = new ArrayList<Transaction>();
    private final ArrayList<Transaction> mPending  = new ArrayList<Transaction>();
    private ConnectivityManager mConnMgr;
    private NetworkConnectivityListener mConnectivityListener;
    private PowerManager.WakeLock mWakeLock;

    //add for 81452
    private PhoneStateListener mPhoneStateListener;
    private PhoneStateListener mPhoneStateListener2;
    //phone state in single mode, in gemini mode slot0 state
    private int mPhoneState = TelephonyManager.CALL_STATE_IDLE;
    //phone state of slot1 in gemini mode
    private int mPhoneState2 = TelephonyManager.CALL_STATE_IDLE;
    private Object mPhoneStateLock = new Object();
    private int mLastIdleSlot = Phone.GEMINI_SIM_1;
    private Object mIdleLock = new Object();
    private boolean mEnableCallbackIdle = false;

    private long triggerMsgId = 0;
    //Add for time out mechanism
    private final long REQUEST_CONNECTION_TIME_OUT_LENGTH = 3*60*1000;
    //this member is used to ignore status message sent by framework between time out happened and
    //a new data connection request which need wait.
    private boolean mIgnoreMsg = false;

    private int mMaxServiceId = Integer.MIN_VALUE;
    
    // add for gemini
    private int mSimIdForEnd = 0;

    // for handling framework sticky intent issue
    private Intent mFWStickyIntent = null;

    public Handler mToastHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String str = null;

            if (msg.what == TOAST_MSG_QUEUED) {
                str = getString(R.string.message_queued);
            } else if (msg.what == TOAST_DOWNLOAD_LATER) {
                str = getString(R.string.download_later);
            }

            if (str != null) {
                Toast.makeText(TransactionService.this, str,
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onCreate() {
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "Creating TransactionService");
        }

        Xlog.d(MmsApp.TXN_TAG, "Creating Transaction Service");

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.
        HandlerThread thread = new HandlerThread("TransactionService");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        mConnectivityListener = new NetworkConnectivityListener();
        mConnectivityListener.registerHandler(mServiceHandler, EVENT_DATA_STATE_CHANGED);
        mConnectivityListener.startListening(this);
        mFWStickyIntent = mConnectivityListener.getStickyIntent();
        //add for 81452
        registerPhoneCallListener();
        Xlog.d(MmsApp.TXN_TAG, "Sticky Intent would be received");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Xlog.d(MmsApp.TXN_TAG, "onStartCommand");
        if (intent == null) {
            return Service.START_NOT_STICKY;
        }
        mConnMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean noNetwork = !isNetworkAvailable();

        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "onStart: #" + startId + ": " + intent.getExtras() + " intent=" + intent);
            Log.v(TAG, "    networkAvailable=" + !noNetwork);
        }

        Uri uri = null;
        String str = intent.getStringExtra(TransactionBundle.URI);
        if (null != str) {
            Xlog.d(MmsApp.TXN_TAG, "onStartCommand, URI in Bundle.");
            uri = Uri.parse(str);
            if (null != uri) {
                triggerMsgId = ContentUris.parseId(uri);
                Xlog.d(MmsApp.TXN_TAG, "Trigger Message ID = " + triggerMsgId);
            }
        }
        
        mMaxServiceId = (startId > mMaxServiceId)?startId:mMaxServiceId;

        if (ACTION_ONALARM.equals(intent.getAction()) || (intent.getExtras() == null)) {
            if (ACTION_ONALARM.equals(intent.getAction())) {
                Xlog.d(MmsApp.TXN_TAG, "onStartCommand: ACTION_ONALARM");
            } else {
                Xlog.d(MmsApp.TXN_TAG, "onStartCommand: Intent has no Extras data.");
            }
            // add for gemini
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                if (SMART) {
                    scanPendingMessages(startId, noNetwork, -1, false);
                } else {
                    // 0: no data connect, 1:sim1,  2:sim2
                    int simId = Settings.System.getInt(getContentResolver(), Settings.System.GPRS_CONNECTION_SETTING, Settings.System.GPRS_CONNECTION_SETTING_DEFAULT); 
                    Xlog.d(MmsApp.TXN_TAG, "onStartCommand:  0:no data connect, 1:sim1,  2:sim2,  current="+simId);
                    if (0 != simId) {
                        scanPendingMessages(startId, noNetwork, simId-1, false);
                    }
                }
            } else {
                scanPendingMessages(startId, noNetwork, -1, false);
            }
        } else {
            // For launching NotificationTransaction and test purpose.
            TransactionBundle args = null;
            //add this for sync
            int pendingSize = getPendingSize();
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                args = new TransactionBundle(intent.getIntExtra(TransactionBundle.TRANSACTION_TYPE, 0), 
                                             intent.getStringExtra(TransactionBundle.URI));
                // 1. for gemini, do not cear noNetwork param
                // 2. check URI
                if (null != intent.getStringExtra(TransactionBundle.URI)) {
                    int simId = intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, -1);
                    if (-1 != simId) {
                        launchTransactionGemini(startId, simId, args);
                    } else {
                        // for handling third party 
                        long connectSimId = Settings.System.getLong(getContentResolver(), Settings.System.GPRS_CONNECTION_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET); 
                        Xlog.d(MmsApp.TXN_TAG, "onStartCommand before launch transaction:  current data settings: " + connectSimId);
                        if (Settings.System.DEFAULT_SIM_NOT_SET != connectSimId && Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER != connectSimId) {
                            launchTransactionGemini(startId, (int)connectSimId, args);
                        }  
                    }
                }
            }else {
                args = new TransactionBundle(intent.getExtras());
                launchTransaction(startId, args, noNetwork);
            }
        }
        return Service.START_NOT_STICKY;
    }

    /*    
    * this method is used to scan pending messages in database to re-process them one by one.    
    * startId: useless now.    * noNetwork: whether the network is ok.    
    * simId: for single mode use -1, for gemini mode use -1 means no filter.    
    * scanAll: control scan scope.       
    */
    private void scanPendingMessages(int startId, boolean noNetwork, int simId, boolean scanAll) {
        Xlog.d(MmsApp.TXN_TAG, "scanPendingMessagesGemini: startid=" + startId 
            + ", Request simId=" + simId+ ", noNetwork=" + noNetwork + "scanAll:" + scanAll);
        // Scan database to find all pending operations.
        Cursor cursor = PduPersister.getPduPersister(this).getPendingMessages(
                scanAll?Long.MAX_VALUE:SystemClock.elapsedRealtime());
        if (cursor != null) {
            try {
                int count = cursor.getCount();
                Xlog.d(MmsApp.TXN_TAG, "scanPendingMessages: Pending Message Size=" + count);
                if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                    Log.v(TAG, "scanPendingMessages: cursor.count=" + count);
                }

                if (count == 0 && triggerMsgId == 0) {
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "scanPendingMessages: no pending messages. Stopping service.");
                    }
                    if (scanAll == false) {
                        RetryScheduler.setRetryAlarm(this);
                    }
                    stopSelfIfIdle(startId);
                    return;
                }

                int columnIndexOfMsgId = cursor.getColumnIndexOrThrow(PendingMessages.MSG_ID);
                int columnIndexOfMsgType = cursor.getColumnIndexOrThrow(PendingMessages.MSG_TYPE);
                /*gemini specific*/
                int columnIndexOfSimId = cursor.getColumnIndexOrThrow(PendingMessages.SIM_ID);
                int columnIndexOfErrorType = cursor.getColumnIndexOrThrow(PendingMessages.ERROR_TYPE);                

                if (noNetwork) {
                    // Make sure we register for connection state changes.
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "scanPendingMessages: registerForConnectionStateChanges");
                    }
                    Xlog.d(MmsApp.TXN_TAG, "scanPendingMessagesGemini: registerForConnectionStateChanges");
                    MmsSystemEventReceiver.registerForConnectionStateChanges(
                            getApplicationContext());
                }
                int msgType = 0;
                int transactionType = 0;
                /*gemini specific*/
                int pendingMsgSimId = 0;
                
                while (cursor.moveToNext()) {
                    msgType = cursor.getInt(columnIndexOfMsgType);
                    transactionType = getTransactionType(msgType);
                    if (noNetwork && (FeatureOption.MTK_GEMINI_SUPPORT == false)/*only single card mode show toast*/) {
                        onNetworkUnavailable(startId, transactionType);
                        return;
                    }
                    /*gemini specific*/
                    if (FeatureOption.MTK_GEMINI_SUPPORT) {
                        pendingMsgSimId = cursor.getInt(columnIndexOfSimId);
                        Xlog.d(MmsApp.TXN_TAG, "scanPendingMessagesGemini: pendingMsgSimId=" + pendingMsgSimId);
                        if ((simId != -1) && (simId != pendingMsgSimId)) {
                            Xlog.d(MmsApp.TXN_TAG, "Gemini mode, request only process simId:"+simId+",current simId is:"+pendingMsgSimId);
                            continue;
                        }                        
                        if (!SMART) {
                            if (pendingMsgSimId != simId) {
                                Xlog.d(MmsApp.TXN_TAG, "scanPendingMessagesGemini: pendingMsgSimId!=simId, Continue!");
                                continue;
                            }
                        }
                        if (MmsSms.ERR_TYPE_GENERIC_PERMANENT == cursor.getInt(columnIndexOfErrorType)) {
                            Xlog.d(MmsApp.TXN_TAG, "scanPendingMessagesGemini: Error type = Permanent, Continue!");
                            continue;
                        }
                        if (triggerMsgId == cursor.getLong(columnIndexOfMsgId)) {
                            Xlog.d(MmsApp.TXN_TAG, "scanPendingMessagesGemini: Message ID = Trigger message ID, Continue!");
                            continue;
                        }
                    }
                    
                    switch (transactionType) {
                        case -1:
                            Xlog.d(MmsApp.TXN_TAG, "scanPendingMessagesGemini: transaction Type= -1");
                            break;
                        case Transaction.RETRIEVE_TRANSACTION:
                            Xlog.d(MmsApp.TXN_TAG, "scanPendingMessagesGemini: transaction Type= RETRIEVE");       
                            // If it's a transiently failed transaction,
                            // we should retry it in spite of current
                            // downloading mode.
                            int failureType = cursor.getInt(
                                    cursor.getColumnIndexOrThrow(
                                            PendingMessages.ERROR_TYPE));
                            if (!isTransientFailure(failureType)) {
                                Xlog.d(MmsApp.TXN_TAG, cursor.getLong(columnIndexOfMsgId) +  "this RETRIEVE not transient failure");
                                break;
                            }
                            // fall-through
                        default:
                            Uri uri = ContentUris.withAppendedId(
                                    Mms.CONTENT_URI,
                                    cursor.getLong(columnIndexOfMsgId));
                            Xlog.d(MmsApp.TXN_TAG, "scanPendingMessages: Pending Message uri=" + uri);
                            
                            TransactionBundle args = new TransactionBundle(
                                    transactionType, uri.toString());
                            // FIXME: We use the same startId for all MMs.
                            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                if (SMART) {
                                    if (pendingMsgSimId > 0) {
                                        launchTransactionGemini(startId, pendingMsgSimId, args);
                                    } else {
                                        // for handling third party 
                                        long connectSimId = Settings.System.getLong(getContentResolver(), Settings.System.GPRS_CONNECTION_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET); 
                                        Xlog.v(MmsApp.TXN_TAG, "Scan Pending message:  current data settings: " + connectSimId);
                                        if (Settings.System.DEFAULT_SIM_NOT_SET != connectSimId && Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER != connectSimId) {
                                            launchTransactionGemini(startId, (int)connectSimId, args);
                                        }
                                    }
                                } else {
                                    launchTransactionGemini(startId, simId, args);
                                }
                            } else {
                                launchTransaction(startId, args, false);
                            }
                            break;
                    }
                }
            } finally {
                cursor.close();
            }
        } else {
            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "scanPendingMessages: no pending messages. Stopping service.");
            } 
            Xlog.d(MmsApp.TXN_TAG, "scanPendingMessagesGemini: no pending messages. Stopping service.");
            if (triggerMsgId == 0) {
                if (scanAll == false) {
                    RetryScheduler.setRetryAlarm(this);
                }
                stopSelfIfIdle(startId);
            }
        }
    }

    private void stopSelfIfIdle(int startId) {
        //TransactionService need keep alive to wait call end and process pending mms in db. add for 81452. 
        if (mEnableCallbackIdle) {
            Xlog.d(MmsApp.TXN_TAG, "need wait call end, no stop.");
            return;
        }
        synchronized (mProcessing) {
            if (mProcessing.isEmpty() && mPending.isEmpty() && mNeedWait == false/*avoid incorrectly stop service*/) {
                if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                    Log.v(TAG, "stopSelfIfIdle: STOP!");
                }
                // Make sure we're no longer listening for connection state changes.
                if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                    Log.v(TAG, "stopSelfIfIdle: unRegisterForConnectionStateChanges");
                }
                MmsSystemEventReceiver.unRegisterForConnectionStateChanges(getApplicationContext());

                stopSelf(startId);
            }
        }
    }

    private static boolean isTransientFailure(int type) {
        return (type < MmsSms.ERR_TYPE_GENERIC_PERMANENT) && (type > MmsSms.NO_ERROR);
    }

    private boolean isNetworkAvailable() {
        return mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS).
                isAvailable();
    }

    private int getTransactionType(int msgType) {
        switch (msgType) {
            case PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND:
                return Transaction.RETRIEVE_TRANSACTION;
            case PduHeaders.MESSAGE_TYPE_READ_REC_IND:
                return Transaction.READREC_TRANSACTION;
            case PduHeaders.MESSAGE_TYPE_SEND_REQ:
                return Transaction.SEND_TRANSACTION;
            default:
                Log.w(TAG, "Unrecognized MESSAGE_TYPE: " + msgType);
                return -1;
        }
    }

    private void launchTransaction(int serviceId, TransactionBundle txnBundle, boolean noNetwork) {
        if (noNetwork) {
            Log.w(TAG, "launchTransaction: no network error!");
            onNetworkUnavailable(serviceId, txnBundle.getTransactionType());
            return;
        }
        Message msg = mServiceHandler.obtainMessage(EVENT_TRANSACTION_REQUEST);
        msg.arg1 = serviceId;
        msg.obj = txnBundle;

        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "launchTransaction: sending message " + msg);
        }
        mServiceHandler.sendMessage(msg);
    }

    private void onNetworkUnavailable(int serviceId, int transactionType) {
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "onNetworkUnavailable: sid=" + serviceId + ", type=" + transactionType);
        }

        int toastType = TOAST_NONE;
        if (transactionType == Transaction.RETRIEVE_TRANSACTION) {
            toastType = TOAST_DOWNLOAD_LATER;
        } else if (transactionType == Transaction.SEND_TRANSACTION) {
            toastType = TOAST_MSG_QUEUED;
        }
        if (toastType != TOAST_NONE) {
            mToastHandler.sendEmptyMessage(toastType);
        }
        //change for 81452
        stopSelfIfIdle(serviceId);
    }

    @Override
    public void onDestroy() {
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "Destroying TransactionService");
        }
        Xlog.d(MmsApp.TXN_TAG, "Destroying Transaction Service");
        if (!mPending.isEmpty()) {
            Xlog.w(MmsApp.TXN_TAG, "onDestroy: TransactionService exiting with transaction still pending");
        }

        mConnectivityListener.unregisterHandler(mServiceHandler);
        mConnectivityListener.stopListening();
        mConnectivityListener = null;

        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            bWaitingConxn = false;
            ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE))
                    .listenGemini(mPhoneStateListener,
                            PhoneStateListener.LISTEN_NONE, Phone.GEMINI_SIM_1);
            mPhoneStateListener = null;
            ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE))
                    .listenGemini(mPhoneStateListener2,
                            PhoneStateListener.LISTEN_NONE, Phone.GEMINI_SIM_2);            
            mPhoneStateListener2 = null;
        } else {
            ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE))
                    .listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            mPhoneStateListener = null;
        }

        releaseWakeLock();

        mServiceHandler.sendEmptyMessage(EVENT_QUIT);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Handle status change of Transaction (The Observable).
     */
    public void update(Observable observable) {
        Xlog.d(MmsApp.TXN_TAG, "Transaction Service update");
        Transaction transaction = (Transaction) observable;
        int serviceId = transaction.getServiceId();

        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "update transaction " + serviceId);
        }

        try {
            synchronized (mProcessing) {
                mProcessing.remove(transaction);
                if (mPending.size() > 0) {
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "update: handle next pending transaction...");
                    }
                    Xlog.d(MmsApp.TXN_TAG, "TransactionService: update: mPending.size()=" + mPending.size());
                    Message msg = mServiceHandler.obtainMessage(
                            EVENT_HANDLE_NEXT_PENDING_TRANSACTION,
                            transaction.getConnectionSettings());
                    // add for gemini
                    if (FeatureOption.MTK_GEMINI_SUPPORT) {
                        msg.arg2 = transaction.mSimId;
                    }
                    mServiceHandler.sendMessage(msg);
                }
                //else {
                else if (0 == mProcessing.size()) {
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "update: endMmsConnectivity");
                    }

                    // add for gemini
                    if (FeatureOption.MTK_GEMINI_SUPPORT) {
                        endMmsConnectivityGemini(transaction.mSimId);
                        Xlog.d(MmsApp.TXN_TAG, "update endMmsConnectivityGemini Param = " + transaction.mSimId);
                    } else {
                        Xlog.d(MmsApp.TXN_TAG, "update call endMmsConnectivity");
                        endMmsConnectivity();
                    }
                }
            }

            Intent intent = new Intent(TRANSACTION_COMPLETED_ACTION);
            TransactionState state = transaction.getState();
            int result = state.getState();
            intent.putExtra(STATE, result);

            switch (result) {
                case TransactionState.SUCCESS:
                    Xlog.d(MmsApp.TXN_TAG, "update: result=SUCCESS");
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "Transaction complete: " + serviceId);
                    }

                    intent.putExtra(STATE_URI, state.getContentUri());

                    // Notify user in the system-wide notification area.
                    switch (transaction.getType()) {
                        case Transaction.NOTIFICATION_TRANSACTION:
                        case Transaction.RETRIEVE_TRANSACTION:
                            // We're already in a non-UI thread called from
                            // NotificationTransacation.run(), so ok to block here.
                            MessagingNotification.blockingUpdateNewMessageIndicator(this, true,
                                    false);
                            MessagingNotification.updateDownloadFailedNotification(this);
                            break;
                        case Transaction.SEND_TRANSACTION:
                            RateController.getInstance().update();
                            break;
                    }
                    break;
                case TransactionState.FAILED:
                    Xlog.d(MmsApp.TXN_TAG, "update: result=FAILED");
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "Transaction failed: " + serviceId);
                    }
                    break;
                default:
                    Xlog.d(MmsApp.TXN_TAG, "update: result=default");
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "Transaction state unknown: " +
                                serviceId + " " + result);
                    }
                    break;
            }

            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "update: broadcast transaction result " + result);
            }
            // Broadcast the result of the transaction.
            sendBroadcast(intent);
        } finally {
            transaction.detach(this);
            MmsSystemEventReceiver.unRegisterForConnectionStateChanges(getApplicationContext());
            //add this if to fix a bug. some transaction may be not processed.
            //here there is a precondition: the serviceId[startId] of each transaction got from framewrok is increasing by order.
            //so the max service id is recorded. and must invoked as the last transaction finish. 
            if (transaction.getServiceId() == mMaxServiceId) {
                stopSelfIfIdle(mMaxServiceId);
            }
        }
    }

    private synchronized void createWakeLock() {
        // Create a new wake lock if we haven't made one yet.
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MMS Connectivity");
            mWakeLock.setReferenceCounted(false);
        }
    }

    private void acquireWakeLock() {
        // It's okay to double-acquire this because we are not using it
        // in reference-counted mode.
        mWakeLock.acquire();
    }

    private void releaseWakeLock() {
        // Don't release the wake lock if it hasn't been created and acquired.
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    protected int beginMmsConnectivity() throws IOException {
        // Take a wake lock so we don't fall asleep before the message is downloaded.
        createWakeLock();

        int result = mConnMgr.startUsingNetworkFeature(
                ConnectivityManager.TYPE_MOBILE, Phone.FEATURE_ENABLE_MMS);
        Xlog.d(MmsApp.TXN_TAG, "startUsingNetworkFeature: result=" + result);

        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "beginMmsConnectivity: result=" + result);
        }

        switch (result) {
            case Phone.APN_ALREADY_ACTIVE:
            case Phone.APN_REQUEST_STARTED:
                acquireWakeLock();
                //add this for time out mechanism
                setDataConnectionTimer(result);                
                return result;
            case Phone.APN_TYPE_NOT_AVAILABLE:
            case Phone.APN_REQUEST_FAILED:
                return result;
        }

        throw new IOException("Cannot establish MMS connectivity");
    }

    protected void endMmsConnectivity() {
        try {
            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "endMmsConnectivity");
            }

            // cancel timer for renewal of lease
            mServiceHandler.removeMessages(EVENT_CONTINUE_MMS_CONNECTIVITY);
            if (mConnMgr != null) {
                mConnMgr.stopUsingNetworkFeature(
                        ConnectivityManager.TYPE_MOBILE,
                        Phone.FEATURE_ENABLE_MMS);
                Xlog.d(MmsApp.TXN_TAG, "stopUsingNetworkFeature");
            }
        } finally {
            releaseWakeLock();
            triggerMsgId = 0;
        }
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        /**
         * Handle incoming transaction requests.
         * The incoming requests are initiated by the MMSC Server or by the
         * MMS Client itself.
         */
        @Override
        public void handleMessage(Message msg) {
            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "Handling incoming message: " + msg);
            }
            Xlog.d(MmsApp.TXN_TAG, "handleMessage :" + msg);

            Transaction transaction = null;

            switch (msg.what) {
                case EVENT_QUIT:
                    Xlog.d(MmsApp.TXN_TAG, "EVENT_QUIT");
                    if (FeatureOption.MTK_GEMINI_SUPPORT && SMART) {
                        bWaitingConxn = false;
                    }
                    releaseWakeLock();
                    getLooper().quit();
                    return;

                case EVENT_CONTINUE_MMS_CONNECTIVITY:
                    Xlog.d(MmsApp.TXN_TAG, "EVENT_CONTINUE_MMS_CONNECTIVITY");
                    synchronized (mProcessing) {
                        if (mProcessing.isEmpty()) {
                            return;
                        }
                    }

                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "handle EVENT_CONTINUE_MMS_CONNECTIVITY event...");
                    }

                    try {
                        // add for gemini
                        int result = 0;
                        if (FeatureOption.MTK_GEMINI_SUPPORT) {
                            SIMInfo si = SIMInfo.getSIMInfoBySlot(getApplicationContext(), msg.arg2);
                            if (null == si) {
                                Xlog.e(MmsApp.TXN_TAG, "TransactionService:SIMInfo is null for slot " + msg.arg2);
                                return;
                            }
                            int simId = (int)si.mSimId;
                            result = beginMmsConnectivityGemini(simId/*msg.arg2*/);
                        } else{
                            result = beginMmsConnectivity();
                        }

                        if (result != Phone.APN_ALREADY_ACTIVE) {
                            if (result == Phone.APN_REQUEST_STARTED && mServiceHandler.hasMessages(EVENT_PENDING_TIME_OUT)) {
                                //the timer is not for this case, remove it.
                                mServiceHandler.removeMessages(EVENT_PENDING_TIME_OUT);
                                Xlog.d(MmsApp.TXN_TAG, "remove an invalid timer.");
                            }                            
                            // Just wait for connectivity startup without
                            // any new request of APN switch.
                            return;
                        }
                    } catch (IOException e) {
                        Log.w(TAG, "Attempt to extend use of MMS connectivity failed");
                        return;
                    }

                    //// Restart timer
                    //sendMessageDelayed(obtainMessage(EVENT_CONTINUE_MMS_CONNECTIVITY),
                    //                   APN_EXTENSION_WAIT);
                    if (FeatureOption.MTK_GEMINI_SUPPORT) {
                        // Set a timer to keep renewing our "lease" on the MMS connection
                        sendMessageDelayed(obtainMessage(EVENT_CONTINUE_MMS_CONNECTIVITY, 0, msg.arg2),
                                           APN_EXTENSION_WAIT);
                    } else {
                        // Set a timer to keep renewing our "lease" on the MMS connection
                        sendMessageDelayed(obtainMessage(EVENT_CONTINUE_MMS_CONNECTIVITY),
                                           APN_EXTENSION_WAIT);
                    }
                    return;

                case EVENT_DATA_STATE_CHANGED:
                    Xlog.d(MmsApp.TXN_TAG, "EVENT_DATA_STATE_CHANGED! slot=" + msg.arg2);
                    /*
                     * If we are being informed that connectivity has been established
                     * to allow MMS traffic, then proceed with processing the pending
                     * transaction, if any.
                     */
                    if (mConnectivityListener == null) {
                        Xlog.w(MmsApp.TXN_TAG, "handleMessage : mConnectivityListener == null");
                        return;
                    }

                    // check sticky intent
                    if (mFWStickyIntent != null) {
                        //NetworkInfo stickyInfo = (NetworkInfo)mFWStickyIntent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                        //NetworkInfo nowInfo = mConnectivityListener.getNetworkInfo();
                        //if (stickyInfo != null && nowInfo != null && )
                        Xlog.d(MmsApp.TXN_TAG, "get sticky intent" + mFWStickyIntent);
                        mFWStickyIntent = null;
                        return;
                    }
                    //ignore none mobile mms status.
                    if (msg.arg1 != ConnectivityManager.TYPE_MOBILE_MMS) {
                        Xlog.d(MmsApp.TXN_TAG, "ignore a none mobile mms status message.");
                        return;
                    }
                    //NetworkInfo info = mConnectivityListener.getNetworkInfo();
                    if (mConnMgr == null) {                        
                        mConnMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);                    
                    }                    
                    if (mConnMgr == null) {                        
                        Xlog.d(MmsApp.TXN_TAG, "mConnMgr == null ");                      
                        return;                    
                    }
                    //add for time out mechanism
                    if (mIgnoreMsg == true) {
                        Xlog.d(MmsApp.TXN_TAG, "between time out over and a new connection request, ignore msg.");
                        return;
                    }
                    if (mServiceHandler.hasMessages(EVENT_PENDING_TIME_OUT)) {
                        if (FeatureOption.MTK_GEMINI_SUPPORT) {
                            Transaction trxn = null;
                            synchronized (mProcessing) {
                                if (mPending.size() != 0) {
                                    trxn = mPending.get(0);
                                } else {
                                    Xlog.d(MmsApp.TXN_TAG, "a timer is created but pending is null!");
                                }
                            }
                            /*state change but pending is null, this may happened.
                                this is just for framework abnormal case*/
                            if (trxn == null) {
                                Xlog.d(MmsApp.TXN_TAG, "remove a timer which may be created by EVENT_CONTINUE_MMS_CONNECTIVITY");
                                mServiceHandler.removeMessages(EVENT_PENDING_TIME_OUT);
                                //return;//since the pending is null, we can return. if not, it should ok too.
                                
                            } else {
                                int slotId = SIMInfo.getSlotById(getApplicationContext(), trxn.mSimId);
                                if (slotId == msg.arg2) {
                                    mServiceHandler.removeMessages(EVENT_PENDING_TIME_OUT);
                                    Xlog.d(MmsApp.TXN_TAG, "gemini normal get msg, remove timer.");
                                }
                            }
                        } else {
                            mServiceHandler.removeMessages(EVENT_PENDING_TIME_OUT);
                            Xlog.d(MmsApp.TXN_TAG, "normal get msg, remove timer.");
                        }
                    }
                    NetworkInfo info = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
                    
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "Handle DATA_STATE_CHANGED event: " + info);
                    }
                    Xlog.d(MmsApp.TXN_TAG, "Newwork info: " + info);
                    //add for sync
                    int pendingSize = getPendingSize();
                    // Check connection state : connect or disconnect
                    if (info != null && !info.isConnected()) {
                        if (FeatureOption.MTK_GEMINI_SUPPORT 
                                && (ConnectivityManager.TYPE_MOBILE == info.getType()
                                    ||ConnectivityManager.TYPE_MOBILE_MMS == info.getType())) {
                            if (pendingSize != 0) {
                                if (SMART) {
                                    //change for sync
                                    Transaction trxn = null;
                                    synchronized (mProcessing) {
                                        trxn = mPending.get(0);
                                    }
                                    int slotId = SIMInfo.getSlotById(getApplicationContext(), trxn.mSimId);
                                    if (slotId != msg.arg2) {
                                        return;
                                    }
                                } else {
                                    int simId = Settings.System.getInt(getContentResolver(), Settings.System.GPRS_CONNECTION_SETTING, Settings.System.GPRS_CONNECTION_SETTING_DEFAULT); 
                                    //add for sync
                                    Transaction trxn = null;
                                    synchronized (mProcessing) {
                                        trxn = mPending.get(0);
                                    }
                                    if (trxn.mSimId != simId - 1){
                                        //add for sync
                                        setTransactionFail(removePending(0), FAILE_TYPE_PERMANENT);
                                    }
                                }
                            }
                        }
                        
                        // check type and reason, 
                        if (ConnectivityManager.TYPE_MOBILE_MMS == info.getType() 
                            && Phone.REASON_NO_SUCH_PDP.equals(info.getReason())) {
                            if (0 != pendingSize){
                                //add for sync
                                setTransactionFail(removePending(0), FAILE_TYPE_PERMANENT);
                                return;
                            }
                        } else if (ConnectivityManager.TYPE_MOBILE_MMS == info.getType()
                            && NetworkInfo.State.DISCONNECTED == info.getState()) {
                            if (0 != pendingSize){
                                Xlog.d(MmsApp.TXN_TAG, "setTransactionFail TEMPORARY because NetworkInfo.State.DISCONNECTED");
                                //add for sync
                                setTransactionFail(removePending(0), FAILE_TYPE_TEMPORARY);
                                return;
                            }             
                        } else if ((ConnectivityManager.TYPE_MOBILE_MMS == info.getType() 
                                && Phone.REASON_APN_FAILED.equals(info.getReason())) 
                                || Phone.REASON_RADIO_TURNED_OFF.equals(info.getReason())) {
                            if (0 != pendingSize){
                                //add for sync
                                setTransactionFail(removePending(0), FAILE_TYPE_TEMPORARY);
                                return;
                            }
                            Xlog.d(MmsApp.TXN_TAG, "No pending message.");
                        }
                        return;
                    }

                    if (info != null && Phone.REASON_VOICE_CALL_ENDED.equals(info.getReason())){
                        if (0 != pendingSize){
                            Transaction trxn = null;
                            synchronized (mProcessing) {
                                trxn = mPending.get(0);
                            }
                            // add for gemini
                            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                processPendingTransactionGemini(transaction,trxn.getConnectionSettings(),trxn.mSimId);
                            } else {
                                processPendingTransaction(transaction, trxn.getConnectionSettings());
                            }
                        }
                    }

                    // Check availability of the mobile network.
                    if ((info == null) || (info.getType() != ConnectivityManager.TYPE_MOBILE_MMS)) {
                        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                            Log.v(TAG, "   type is not TYPE_MOBILE_MMS, bail");
                        }
                        Xlog.d(MmsApp.TXN_TAG, "EVENT_DATA_STATE_CHANGED: type is not TYPE_MOBILE_MMS, bail");
                        return;
                    }

                    //TransactionSettings settings = new TransactionSettings(
                            //TransactionService.this, info.getExtraInfo());

                    TransactionSettings settings = null;
                    if (FeatureOption.MTK_GEMINI_SUPPORT) {
                        if (SMART) {
                            if (Phone.GEMINI_SIM_1 == msg.arg2 || Phone.GEMINI_SIM_2 == msg.arg2) {
                                settings = new TransactionSettings(TransactionService.this, info.getExtraInfo(), msg.arg2);
                            } else {
                                return;
                            }
                        } else {
                            int simId = Settings.System.getInt(getContentResolver(), Settings.System.GPRS_CONNECTION_SETTING, Settings.System.GPRS_CONNECTION_SETTING_DEFAULT);
                            Xlog.d(MmsApp.TXN_TAG, "handleMessage:  0:no data connect, 1:sim1,  2:sim2,  current=" + simId);
                            if (0 != simId) {
                                settings = new TransactionSettings(TransactionService.this, info.getExtraInfo(), simId-1);
                            } else {
                                return;
                            }
                        }
                    } else {
                        settings = new TransactionSettings(TransactionService.this, info.getExtraInfo());
                    }

                    // If this APN doesn't have an MMSC, wait for one that does.
                    if (TextUtils.isEmpty(settings.getMmscUrl())) {
                        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                            Log.v(TAG, "   empty MMSC url, bail");
                        }
                        Xlog.d(MmsApp.TXN_TAG, "empty MMSC url, bail");
                        if (0 != pendingSize){
                            setTransactionFail(removePending(0), FAILE_TYPE_TEMPORARY);
                        }
                        return;
                    }

                    //// Set a timer to keep renewing our "lease" on the MMS connection
                    //sendMessageDelayed(obtainMessage(EVENT_CONTINUE_MMS_CONNECTIVITY),
                    //                   APN_EXTENSION_WAIT);
                    if (FeatureOption.MTK_GEMINI_SUPPORT) {
                        if (SMART) {
                            if (Phone.GEMINI_SIM_1 == msg.arg2 || Phone.GEMINI_SIM_2 == msg.arg2) {
                                // Set a timer to keep renewing our "lease" on the MMS connection
                                //sendMessageDelayed(obtainMessage(EVENT_CONTINUE_MMS_CONNECTIVITY, 0, msg.arg2),
                                //                   APN_EXTENSION_WAIT);
                                SIMInfo si = SIMInfo.getSIMInfoBySlot(getApplicationContext(), msg.arg2);
                                if (null == si) {
                                    Xlog.e(MmsApp.TXN_TAG, "TransactionService:SIMInfo is null for slot " + msg.arg2);
                                    return;
                                }
                                int simId = (int)si.mSimId;
                                processPendingTransactionGemini(transaction, settings, simId/*msg.arg2*/);
                            }else {
                                return;
                            }
                        } else {
                            int simId = Settings.System.getInt(getContentResolver(), Settings.System.GPRS_CONNECTION_SETTING, Settings.System.GPRS_CONNECTION_SETTING_DEFAULT);
                            Xlog.d(MmsApp.TXN_TAG, "handleMessage:  0:no data connect, 1:sim1,  2:sim2,  current="+simId);
                            if (0 != simId) {
                                // Set a timer to keep renewing our "lease" on the MMS connection                            
                                //sendMessageDelayed(obtainMessage(EVENT_CONTINUE_MMS_CONNECTIVITY, 0, simId-1), APN_EXTENSION_WAIT);                            
                                processPendingTransactionGemini(transaction, settings, simId-1);                        
                            } else {                            
                                return;                        
                            }
                        }
                    }else {
                        // Set a timer to keep renewing our "lease" on the MMS connection
                        //sendMessageDelayed(obtainMessage(EVENT_CONTINUE_MMS_CONNECTIVITY),
                        //                   APN_EXTENSION_WAIT);
                        processPendingTransaction(transaction, settings);
                    }
                    return;

                case EVENT_TRANSACTION_REQUEST:
                    Xlog.d(MmsApp.TXN_TAG, "EVENT_TRANSACTION_REQUEST");

                    int serviceId = msg.arg1;
                    try {
                        TransactionBundle args = (TransactionBundle) msg.obj;
                        TransactionSettings transactionSettings;

                        // Set the connection settings for this transaction.
                        // If these have not been set in args, load the default settings.
                        String mmsc = args.getMmscUrl();
                        if (mmsc != null) {
                            transactionSettings = new TransactionSettings(
                                    mmsc, args.getProxyAddress(), args.getProxyPort());
                        } else {
                            // add for gemini
                            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                // convert sim id to slot id
                                int slotId = SIMInfo.getSlotById(getApplicationContext(), msg.arg2);
                                transactionSettings = new TransactionSettings(
                                                    TransactionService.this, null, slotId/*msg.arg2*/);
                            } else {
                                transactionSettings = new TransactionSettings(
                                                    TransactionService.this, null);
                            }
                        }

                        int transactionType = args.getTransactionType();

                        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                            Log.v(TAG, "handle EVENT_TRANSACTION_REQUEST: transactionType=" +
                                    transactionType);
                        }

                        // Create appropriate transaction
                        switch (transactionType) {
                            case Transaction.NOTIFICATION_TRANSACTION:
                                String uri = args.getUri();
                                Xlog.d(MmsApp.TXN_TAG, "TRANSACTION REQUEST: NOTIFICATION_TRANSACTION, uri="+uri);
                                if (uri != null) {
                                    // add for gemini
                                    if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                        transaction = new NotificationTransaction(
                                            TransactionService.this, serviceId, msg.arg2,
                                            transactionSettings, uri);
                                    } else {
                                        transaction = new NotificationTransaction(
                                            TransactionService.this, serviceId,
                                            transactionSettings, uri);
                                    }
                                } else {
                                    // Now it's only used for test purpose.
                                    byte[] pushData = args.getPushData();
                                    PduParser parser = new PduParser(pushData);
                                    GenericPdu ind = parser.parse();

                                    int type = PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;
                                    if ((ind != null) && (ind.getMessageType() == type)) {
                                        // add for gemini
                                        if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                            transaction = new NotificationTransaction(
                                                TransactionService.this, serviceId, msg.arg2,
                                                transactionSettings, (NotificationInd) ind);
                                        } else {
                                            transaction = new NotificationTransaction(
                                                TransactionService.this, serviceId,
                                                transactionSettings, (NotificationInd) ind);
                                        }
                                    } else {
                                        Xlog.e(MmsApp.TXN_TAG, "Invalid PUSH data.");
                                        transaction = null;
                                        return;
                                    }
                                }
                                break;
                            case Transaction.RETRIEVE_TRANSACTION:
                                Xlog.d(MmsApp.TXN_TAG, "TRANSACTION REQUEST: RETRIEVE_TRANSACTION uri=" + args.getUri());
                                // add for gemini
                                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                    transaction = new RetrieveTransaction(
                                        TransactionService.this, serviceId, msg.arg2,
                                        transactionSettings, args.getUri());
                                } else {
                                    transaction = new RetrieveTransaction(
                                        TransactionService.this, serviceId,
                                        transactionSettings, args.getUri());
                                }
                                break;
                            case Transaction.SEND_TRANSACTION:
                                Xlog.d(MmsApp.TXN_TAG, "TRANSACTION REQUEST: SEND_TRANSACTION");
                                // add for gemini
                                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                    transaction = new SendTransaction(
                                        TransactionService.this, serviceId, msg.arg2,
                                        transactionSettings, args.getUri());
                                } else {
                                    transaction = new SendTransaction(
                                        TransactionService.this, serviceId,
                                        transactionSettings, args.getUri());
                                }
                                break;
                            case Transaction.READREC_TRANSACTION:
                                Xlog.d(MmsApp.TXN_TAG, "TRANSACTION REQUEST: READREC_TRANSACTION");
                                // add for gemini
                                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                    transaction = new ReadRecTransaction(
                                        TransactionService.this, serviceId, msg.arg2,
                                        transactionSettings, args.getUri());
                                } else {
                                    transaction = new ReadRecTransaction(
                                        TransactionService.this, serviceId,
                                        transactionSettings, args.getUri());
                                }
                                break;
                            default:
                                Xlog.w(MmsApp.TXN_TAG, "Invalid transaction type: " + serviceId);
                                transaction = null;
                                return;
                        }

                        if (!processTransaction(transaction)) {
                            // add for gemini
                            if (FeatureOption.MTK_GEMINI_SUPPORT && null != transaction) {
                                mSimIdForEnd = transaction.mSimId;
                            }
                            transaction = null;
                            return;
                        }

                        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                            Log.v(TAG, "Started processing of incoming message: " + msg);
                        }
                    } catch (Exception ex) {
                        Xlog.e(MmsApp.TXN_TAG, "Exception occurred while handling message: " + msg, ex);

                        if (transaction != null) {
                            // add for gemini
                            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                mSimIdForEnd = transaction.mSimId;
                            }
                            try {
                                transaction.detach(TransactionService.this);
                                //change this sync.
                                synchronized (mProcessing) {
                                    if (mProcessing.contains(transaction)) {
                                        mProcessing.remove(transaction);
                                    }
                                }
                            } catch (Throwable t) {
                                Log.e(TAG, "Unexpected Throwable.", t);
                            } finally {
                                // Set transaction to null to allow stopping the
                                // transaction service.
                                transaction = null;
                            }
                        }
                    } finally {
                        if (transaction == null) {
                            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                                Log.v(TAG, "Transaction was null. Stopping self: " + serviceId);
                            }
                            Xlog.d(MmsApp.TXN_TAG, "finally call endMmsConnectivity");
                            //add this for sync
                            boolean canEnd = false;
                            synchronized (mProcessing) {
                                canEnd = (mProcessing.size() == 0 && mPending.size() == 0);
                            }
                            if (canEnd == true){
                                // add for gemini
                                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                    endMmsConnectivityGemini(mSimIdForEnd);
                                } else {
                                    endMmsConnectivity();
                                }
                            }
                            //change for 81452
                            stopSelfIfIdle(serviceId);
                        }
                    }
                    return;
                case EVENT_HANDLE_NEXT_PENDING_TRANSACTION:
                    Xlog.d(MmsApp.TXN_TAG, "EVENT_HANDLE_NEXT_PENDING_TRANSACTION");
                    // add for gemini
                    if (FeatureOption.MTK_GEMINI_SUPPORT) {
                        processPendingTransactionGemini(transaction, (TransactionSettings) msg.obj, msg.arg2);
                    } else {
                        processPendingTransaction(transaction, (TransactionSettings) msg.obj);
                    }
                    return;
                //add for time out mechanism    
                case EVENT_PENDING_TIME_OUT:
                    //make the pending transaction temporary failed.
                    int pendSize = getPendingSize();
                    if (0 != pendSize){
                        Xlog.d(MmsApp.TXN_TAG, "a pending connection request time out, mark temporary failed.");
                        mIgnoreMsg = true;
                        setTransactionFail(removePending(0), FAILE_TYPE_TEMPORARY);
                    }
                    return;                    
                //add for 81452
                case EVENT_SCAN_PENDING_MMS:
                    {
                        Xlog.d(MmsApp.TXN_TAG, "EVENT_SCAN_PENDING_MMS");
                        if (FeatureOption.MTK_GEMINI_SUPPORT) {
                            int firstSlot = mLastIdleSlot;
                            int secondSlot;
                            if (firstSlot == Phone.GEMINI_SIM_1) {
                                secondSlot = Phone.GEMINI_SIM_2;
                            } else {
                                secondSlot = Phone.GEMINI_SIM_1;
                            }
                            Xlog.d(MmsApp.TXN_TAG, "scan first slot:"+firstSlot+",second slot:"+secondSlot);
                            SIMInfo si = SIMInfo.getSIMInfoBySlot(getApplicationContext(), firstSlot);
                            if (null != si) {
                                scanPendingMessages(1, false, (int)si.mSimId, true);
                            }
                            si = null;
                            si = SIMInfo.getSIMInfoBySlot(getApplicationContext(), secondSlot);
                            if (null != si) {
                                scanPendingMessages(1, false, (int)si.mSimId, true);
                            }
                        } else {
                            scanPendingMessages(1, false, -1, true);
                        }
                    }
                    break;
                default:
                    Xlog.d(MmsApp.TXN_TAG, "handleMessage : default");
                    Log.w(TAG, "what=" + msg.what);
                    return;
            }
        }

        private void processPendingTransaction(Transaction transaction,
                                               TransactionSettings settings) {
            Xlog.v(MmsApp.TXN_TAG, "processPendingTxn: transaction=" + transaction);
            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "processPendingTxn: transaction=" + transaction);
            }

            int numProcessTransaction = 0;
            synchronized (mProcessing) {
                if (mPending.size() != 0) {
                    Xlog.d(MmsApp.TXN_TAG, "processPendingTransaction: mPending.size()=" + mPending.size());
                    transaction = mPending.remove(0);
                    //avoid stop TransactionService incorrectly.
                    mNeedWait = true;
                }
                numProcessTransaction = mProcessing.size();
            }

            if (transaction != null) {
                if (settings != null) {
                    transaction.setConnectionSettings(settings);
                }

                /*
                 * Process deferred transaction
                 */
                try {
                    int serviceId = transaction.getServiceId();

                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "processPendingTxn: process " + serviceId);
                    }

                    if (processTransaction(transaction)) {
                        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                            Log.v(TAG, "Started deferred processing of transaction  "
                                    + transaction);
                        }
                    } else {
                        transaction = null;
                        //change for 81452
                        stopSelfIfIdle(serviceId);
                    }
                } catch (IOException e) {
                    Log.w(TAG, e.getMessage(), e);
                }
            } else {
                if (numProcessTransaction == 0) {
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "processPendingTxn: no more transaction, endMmsConnectivity");
                    }
                    Xlog.d(MmsApp.TXN_TAG, "processPendingTransaction:no more transaction, endMmsConnectivity");
                    endMmsConnectivity();
                }
            }
        }

        // add for gemini
        private void processPendingTransactionGemini(Transaction transaction,
                                               TransactionSettings settings, int simId) {
            Xlog.d(MmsApp.TXN_TAG, "processPendingTxn for Gemini: transaction=" + transaction + " sim ID="+simId);

            int numProcessTransaction = 0;
            synchronized (mProcessing) {
                if (mPending.size() != 0) {
                    Xlog.d(MmsApp.TXN_TAG, "processPendingTxn for Gemini: Pending size=" + mPending.size());
                    Transaction transactiontemp = null;
                    int pendingSize = mPending.size();
                    for (int i = 0; i < pendingSize; ++i){
                        transactiontemp = mPending.remove(0);
                        if (simId == transactiontemp.mSimId){
                            transaction = transactiontemp;
                            Xlog.d(MmsApp.TXN_TAG, "processPendingTxn for Gemini, get transaction with same simId");
                            //avoid stop TransactionService incorrectly.
                            mNeedWait = true;
                            break;
                        }else{
                            mPending.add(transactiontemp);
                            Xlog.d(MmsApp.TXN_TAG, "processPendingTxn for Gemini, diffrent simId, add to tail");
                        }
                    }
                    if (SMART) {
                        if (null == transaction) {
                            transaction = mPending.remove(0);
                            //avoid stop TransactionService incorrectly.
                            mNeedWait = true;
                            endMmsConnectivityGemini(simId);
                            Xlog.d(MmsApp.TXN_TAG, "Another SIM:" + transaction.mSimId);
                        }
                    }
                }
                numProcessTransaction = mProcessing.size();
            }

            if (transaction != null) {
                if (settings != null) {
                    transaction.setConnectionSettings(settings);
                }

                if (FeatureOption.MTK_GEMINI_SUPPORT && SMART) {
                    bWaitingConxn = false;
                }

                try {
                    int serviceId = transaction.getServiceId();
                    Xlog.d(MmsApp.TXN_TAG, "processPendingTxnGemini: process " + serviceId);

                    if (processTransaction(transaction)) {
                        Xlog.d(MmsApp.TXN_TAG, "Started deferred processing of transaction  " + transaction);
                    } else {
                        transaction = null;
                        //change for 81452
                        stopSelfIfIdle(serviceId);
                    }
                } catch (IOException e) {
                    Xlog.e(MmsApp.TXN_TAG, e.getMessage(), e);
                }
            } else {
                if (numProcessTransaction == 0) {
                    Xlog.d(MmsApp.TXN_TAG, "processPendingTxnGemini:no more transaction, endMmsConnectivity");
                    endMmsConnectivityGemini(simId);
                }
            }
        }

        /**
         * Internal method to begin processing a transaction.
         * @param transaction the transaction. Must not be {@code null}.
         * @return {@code true} if process has begun or will begin. {@code false}
         * if the transaction should be discarded.
         * @throws IOException if connectivity for MMS traffic could not be
         * established.
         */
        private boolean processTransaction(Transaction transaction) throws IOException {
            Xlog.v(MmsApp.TXN_TAG, "process Transaction");
            // Check if transaction already processing
            synchronized (mProcessing) {
                //avoid stop TransactionService incorrectly.
                mNeedWait = false;
                for (Transaction t : mPending) {
                    if (t.isEquivalent(transaction)) {
                        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                            Log.v(TAG, "Transaction already pending: " +
                                    transaction.getServiceId());
                        }
                        Xlog.d(MmsApp.TXN_TAG, "Process Transaction: already pending " + transaction.getServiceId());
                        return true;
                    }
                }
                for (Transaction t : mProcessing) {
                    if (t.isEquivalent(transaction)) {
                        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                            Log.v(TAG, "Duplicated transaction: " + transaction.getServiceId());
                        }
                        Xlog.d(MmsApp.TXN_TAG, "Process Transaction: Duplicated transaction" + transaction.getServiceId());
                        return true;
                    }
                }

                // add for gemini
                //if(FeatureOption.MTK_GEMINI_SUPPORT && SMART && (mProcessing.size() > 0 || mPending.size() > 0)){
                if (FeatureOption.MTK_GEMINI_SUPPORT && SMART && (mProcessing.size() > 0 || bWaitingConxn)) {
                    mPending.add(transaction);
                    Xlog.d(MmsApp.TXN_TAG, "add to pending, Processing size=" + mProcessing.size() 
                        + ",is waiting conxn=" + bWaitingConxn);
                    return true;
                }

                /*
                * Make sure that the network connectivity necessary
                * for MMS traffic is enabled. If it is not, we need
                * to defer processing the transaction until
                * connectivity is established.
                */
                if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                    Log.v(TAG, "processTransaction: call beginMmsConnectivity...");
                }
                
                int connectivityResult = 0;
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    connectivityResult = beginMmsConnectivityGemini(transaction.mSimId);
                } else {
                    connectivityResult = beginMmsConnectivity();
                }
                if (connectivityResult == Phone.APN_REQUEST_STARTED) {
                    mPending.add(transaction);
                    if (FeatureOption.MTK_GEMINI_SUPPORT && SMART) {
                        bWaitingConxn = true;
                    }
                    if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "processTransaction: connResult=APN_REQUEST_STARTED, " +
                                "defer transaction pending MMS connectivity");
                    }
                    Xlog.d(MmsApp.TXN_TAG, "mPending.size()=" + mPending.size());
                    return true;
                } 
                // add for gemini and open
                else if (connectivityResult == Phone.APN_TYPE_NOT_AVAILABLE
                        ||connectivityResult == Phone.APN_REQUEST_FAILED){
                    if (transaction instanceof SendTransaction
                        || transaction instanceof RetrieveTransaction){
                        //add for 81452
                        //if failed becaused of call, moniter call end, and go on process.
                        if (isDuringCall()) {
                            synchronized (mIdleLock) {
                                mEnableCallbackIdle = true;
                            }
                            setTransactionFail(transaction, FAILE_TYPE_TEMPORARY);
                        } else {
                            setTransactionFail(transaction, FAILE_TYPE_PERMANENT);
                        }
                        return false;
                    }
                }

                Xlog.d(MmsApp.TXN_TAG, "Adding Processing list: " + transaction);
                mProcessing.add(transaction);
            }

            //// Set a timer to keep renewing our "lease" on the MMS connection
            //sendMessageDelayed(obtainMessage(EVENT_CONTINUE_MMS_CONNECTIVITY),
            //                   APN_EXTENSION_WAIT);
            
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                // Set a timer to keep renewing our "lease" on the MMS connection
                // convert sim id to slot id
                int slotId = SIMInfo.getSlotById(getApplicationContext(), transaction.mSimId);
                sendMessageDelayed(obtainMessage(EVENT_CONTINUE_MMS_CONNECTIVITY, 0, slotId/*transaction.mSimId*/),
                                   APN_EXTENSION_WAIT);
            } else {
                // Set a timer to keep renewing our "lease" on the MMS connection
                sendMessageDelayed(obtainMessage(EVENT_CONTINUE_MMS_CONNECTIVITY),
                                   APN_EXTENSION_WAIT);
            }

            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "processTransaction: starting transaction " + transaction);
            }

            // Attach to transaction and process it
            transaction.attach(TransactionService.this);
            transaction.process();
            return true;
        }
    }


    // add for gemini and open
    private void setTransactionFail(Transaction txn, int failType) {
        Xlog.v(MmsApp.TXN_TAG, "set Transaction Fail. fail Type=" + failType);

        if (FeatureOption.MTK_GEMINI_SUPPORT && SMART) {
            bWaitingConxn = false;
        }
        
        Uri uri = null;
        if (txn instanceof SendTransaction) {
            Xlog.d(MmsApp.TXN_TAG, "set Transaction Fail. :Send");
            uri = ((SendTransaction)txn).getSendReqUri();
        } else if (txn instanceof NotificationTransaction) {
            Xlog.d(MmsApp.TXN_TAG, "set Transaction Fail. :Notification");
            uri = ((NotificationTransaction)txn).getNotTrxnUri();
        } else if (txn instanceof RetrieveTransaction) {
            Xlog.d(MmsApp.TXN_TAG, "set Transaction Fail. :Retrieve");
            uri = ((RetrieveTransaction)txn).getRtrTrxnUri();
        } else if (txn instanceof ReadRecTransaction) {
            Xlog.d(MmsApp.TXN_TAG, "set Transaction Fail. :ReadRec");
            uri = ((ReadRecTransaction)txn).getRrecTrxnUri();
        } else {
            Xlog.d(MmsApp.TXN_TAG, "set Transaction Fail. type cann't be recognised");
        }

        if (null != uri) {
            txn.mTransactionState.setContentUri(uri);
        }

        if (txn instanceof NotificationTransaction) {
            DownloadManager downloadManager = DownloadManager.getInstance();
            boolean autoDownload = false;
            // add for gemini
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                autoDownload = downloadManager.isAuto(txn.mSimId);
            } else {
                autoDownload = downloadManager.isAuto();
            }

            if (!autoDownload) {
                txn.mTransactionState.setState(TransactionState.SUCCESS);
            } else {
                txn.mTransactionState.setState(TransactionState.FAILED);
            }
        } else {
            txn.mTransactionState.setState(TransactionState.FAILED);
        }

        txn.attach(TransactionService.this);
        Xlog.d(MmsApp.TXN_TAG, "attach this transaction.");
        
        long msgId = ContentUris.parseId(uri);

        Uri.Builder uriBuilder = PendingMessages.CONTENT_URI.buildUpon();
        uriBuilder.appendQueryParameter("protocol", "mms");
        uriBuilder.appendQueryParameter("message", String.valueOf(msgId));

        Cursor cursor = SqliteWrapper.query(getApplicationContext(), 
                                            getApplicationContext().getContentResolver(),
                                            uriBuilder.build(), 
                                            null, null, null, null);

        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    // Mark the failed message as unread.
                    ContentValues readValues = new ContentValues(1);
                    readValues.put(Mms.READ, 0);
                    SqliteWrapper.update(getApplicationContext(), getApplicationContext().getContentResolver(),
                                    uri, readValues, null, null);
                            
                    DefaultRetryScheme scheme = new DefaultRetryScheme(getApplicationContext(), 100);
                    
                    ContentValues values = null;
                    if (FAILE_TYPE_PERMANENT == failType) {
                        values = new ContentValues(2);
                        values.put(PendingMessages.ERROR_TYPE,  MmsSms.ERR_TYPE_GENERIC_PERMANENT);
                        values.put(PendingMessages.RETRY_INDEX, scheme.getRetryLimit());

                        int columnIndex = cursor.getColumnIndexOrThrow(PendingMessages._ID);
                        long id = cursor.getLong(columnIndex);
                                            
                        SqliteWrapper.update(getApplicationContext(), 
                                            getApplicationContext().getContentResolver(),
                                            PendingMessages.CONTENT_URI,
                                            values, PendingMessages._ID + "=" + id, null);
                    }
                }
            }finally {
                cursor.close();
            }
        }

        txn.notifyObservers();
    }
    

    // add for gemini
    private void launchTransactionGemini(int serviceId, int simId, TransactionBundle txnBundle) {
        Message msg = mServiceHandler.obtainMessage(EVENT_TRANSACTION_REQUEST);
        msg.arg1 = serviceId;
        msg.arg2 = simId;
        msg.obj = txnBundle;

        Xlog.d(MmsApp.TXN_TAG, "launchTransactionGemini: sending message " + msg);
        mServiceHandler.sendMessage(msg);
    }

    // add for gemini
    protected int beginMmsConnectivityGemini(int simId) throws IOException {
        // Take a wake lock so we don't fall asleep before the message is downloaded.
        createWakeLock();

        // convert sim id to slot id
        int slotId = SIMInfo.getSlotById(getApplicationContext(), simId);

        int result = mConnMgr.startUsingNetworkFeatureGemini(
                ConnectivityManager.TYPE_MOBILE, Phone.FEATURE_ENABLE_MMS, slotId);

        Xlog.d(MmsApp.TXN_TAG, "beginMmsConnectivityGemini: simId=" + simId + "\t slotId=" + slotId + "\t result=" + result);

        switch (result) {
            case Phone.APN_ALREADY_ACTIVE:
            case Phone.APN_REQUEST_STARTED:
                acquireWakeLock();
                if (SMART) {
                    sendBroadcast(new Intent(TRANSACTION_START));
                }
                //add this for time out mechanism
                setDataConnectionTimer(result);                
                return result;
            case Phone.APN_TYPE_NOT_AVAILABLE:
            case Phone.APN_REQUEST_FAILED:
                return result;
            default:
                throw new IOException("Cannot establish MMS connectivity");
        }
    }

    // add for gemini
    protected void endMmsConnectivityGemini(int simId) {
        try {
            // convert sim id to slot id
            int slotId = SIMInfo.getSlotById(getApplicationContext(), simId);
            
            Xlog.d(MmsApp.TXN_TAG, "endMmsConnectivityGemini: slot id = " + slotId);

            // cancel timer for renewal of lease
            mServiceHandler.removeMessages(EVENT_CONTINUE_MMS_CONNECTIVITY);
            if (mConnMgr != null) {
                mConnMgr.stopUsingNetworkFeatureGemini(
                        ConnectivityManager.TYPE_MOBILE,
                        Phone.FEATURE_ENABLE_MMS, slotId);
                if (SMART) {
                    sendBroadcast(new Intent(TRANSACTION_STOP));
                }
            }
        } finally {
            releaseWakeLock();
            triggerMsgId = 0;
        }
    }

    //add for time out mechanism
    private void setDataConnectionTimer(int result) {
        if (result == Phone.APN_REQUEST_STARTED) {
            mIgnoreMsg = false;
            if (mServiceHandler.hasMessages(EVENT_PENDING_TIME_OUT) == false) {
                Xlog.d(MmsApp.TXN_TAG, "a timer is created.");
                Message msg = mServiceHandler.obtainMessage(EVENT_PENDING_TIME_OUT);
                mServiceHandler.sendMessageDelayed(msg, REQUEST_CONNECTION_TIME_OUT_LENGTH);
            }
        }
    }

    private int getPendingSize() {
        int pendingSize = 0;
        synchronized (mProcessing) {
            pendingSize = mPending.size();
        }
        return pendingSize;
    }

    private Transaction removePending(int index) {
        Transaction trxn = null;
        synchronized (mProcessing) {
            trxn = mPending.remove(index);
        }
        return trxn;
    }
    
    //add for 81452
    /*
        check whether the request data connection fail is caused by calling going on.
    */
    private boolean isDuringCall() {
        if(FeatureOption.MTK_GEMINI_SUPPORT == true){
            synchronized (mPhoneStateLock) {
                mPhoneState = ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE)).getCallStateGemini(Phone.GEMINI_SIM_1);
                mPhoneState2 = ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE)).getCallStateGemini(Phone.GEMINI_SIM_2);
            }
            return (mPhoneState != TelephonyManager.CALL_STATE_IDLE)||(mPhoneState2 != TelephonyManager.CALL_STATE_IDLE);
        } else {
            synchronized (mPhoneStateLock) {
                mPhoneState = ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE)).getCallState();
            }
            return mPhoneState != TelephonyManager.CALL_STATE_IDLE;
        }
    }
    
    private void callbackState() {
        if(FeatureOption.MTK_GEMINI_SUPPORT == true){
            if (mPhoneState == TelephonyManager.CALL_STATE_IDLE &&
                mPhoneState2 == TelephonyManager.CALL_STATE_IDLE) {
                synchronized (mIdleLock) {
                    if (mEnableCallbackIdle) {
                        Message msg = mServiceHandler.obtainMessage(EVENT_SCAN_PENDING_MMS);
                        mServiceHandler.sendMessage(msg);
                        mEnableCallbackIdle = false;
                    }
                }
            }
        } else {
            if (mPhoneState == TelephonyManager.CALL_STATE_IDLE) {
                synchronized (mIdleLock) {
                    if (mEnableCallbackIdle) {
                        Message msg = mServiceHandler.obtainMessage(EVENT_SCAN_PENDING_MMS);
                        mServiceHandler.sendMessage(msg);
                        mEnableCallbackIdle = false;
                    }
                }
            }
        }
    }

    /*
    * register phone call listener
    */
    private void registerPhoneCallListener() {
        if(FeatureOption.MTK_GEMINI_SUPPORT == true){
            mPhoneStateListener = new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    synchronized (mPhoneStateLock) {
                        mPhoneState = state;
                    }
                    if (mPhoneState == TelephonyManager.CALL_STATE_IDLE &&
                        mPhoneState2 == TelephonyManager.CALL_STATE_IDLE) {
                        mLastIdleSlot = Phone.GEMINI_SIM_1;
                    }
                    Xlog.d(MmsApp.TXN_TAG, "get slot0 new state:"+state+",slot1 current state:"+mPhoneState2
                        +",mEnableCallbackIdle:"+mEnableCallbackIdle+",mLastIdleSlot:"+mLastIdleSlot);
                    callbackState();
                }
            };
            mPhoneStateListener2 = new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    synchronized (mPhoneStateLock) {
                        mPhoneState2 = state;
                    }
                    if (mPhoneState == TelephonyManager.CALL_STATE_IDLE &&
                        mPhoneState2 == TelephonyManager.CALL_STATE_IDLE) {
                        mLastIdleSlot = Phone.GEMINI_SIM_2;
                    }
                    Xlog.d(MmsApp.TXN_TAG, "get slot1 new state:"+state+",slot0 current state:"+mPhoneState
                        +",mEnableCallbackIdle:"+mEnableCallbackIdle+",mLastIdleSlot:"+mLastIdleSlot);
                    callbackState();
                }
            };
            ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE))
                    .listenGemini(mPhoneStateListener,
                            PhoneStateListener.LISTEN_CALL_STATE, Phone.GEMINI_SIM_1);
            
            ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE))
                    .listenGemini(mPhoneStateListener2,
                            PhoneStateListener.LISTEN_CALL_STATE, Phone.GEMINI_SIM_2);
        }else{
            mPhoneStateListener = new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    synchronized (mPhoneStateLock) {
                        mPhoneState = state;
                    }
                    Xlog.d(MmsApp.TXN_TAG, "get new state:"+state+",mEnableCallbackIdle:"+mEnableCallbackIdle);
                    callbackState();
                }
            };
            ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE))
                    .listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }        
    }
}
