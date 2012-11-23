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

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAudioGateway;
import android.bluetooth.BluetoothAudioGateway.IncomingConnectionInfo;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothUuid;
/* MTK Removed : BEGIN */
//import android.bluetooth.HeadsetBase;
/* MTK Removed : BEND */
import android.bluetooth.IBluetooth;
import android.bluetooth.IBluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import java.util.HashMap;

/*********************************************************/
import android.bluetooth.BluetoothProfileManager;
import android.bluetooth.BluetoothProfileManager.Profile;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;

/**
 * Provides Bluetooth Headset and Handsfree profile, as a service in
 * the Phone application.
 * @hide
 */
public class BluetoothHeadsetService extends Service {
    private static final String TAG = "BT HSHFP";
    private static final boolean DBG = true;

    private static final String PREF_NAME = BluetoothHeadsetService.class.getSimpleName();
    private static final String PREF_LAST_HEADSET = "lastHeadsetAddress";

    private static final int PHONE_STATE_CHANGED = 1;

    private static final String BLUETOOTH_ADMIN_PERM = android.Manifest.permission.BLUETOOTH_ADMIN;
    private static final String BLUETOOTH_PERM = android.Manifest.permission.BLUETOOTH;

    private static boolean sHasStarted = false;

    private BluetoothDevice mDeviceSdpQuery;
    private BluetoothAdapter mAdapter;
    private IBluetooth mBluetoothService;
    private PowerManager mPowerManager;
    private BluetoothAudioGateway mAg;
    private BluetoothHandsfree mBtHandsfree;
    private HashMap<BluetoothDevice, BluetoothRemoteHeadset> mRemoteHeadsets;

    @Override
    public void onCreate() {
        log("[API] onCreate");
        super.onCreate();
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mBtHandsfree = PhoneApp.getInstance().getBluetoothHandsfree();
        /* MTK Modified : Begin */
        mAg = new BluetoothAudioGateway(mPowerManager, mAdapter);
        /* MTK Modified : End */
        IntentFilter filter = new IntentFilter(
                BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        /* MTK Modified : Begin */
        filter.addAction(BluetoothProfileManager.ACTION_DISABLE_PROFILES);
        /* MTK Modified : End */
        filter.addAction(AudioManager.VOLUME_CHANGED_ACTION);
        filter.addAction(BluetoothDevice.ACTION_UUID);
        registerReceiver(mBluetoothReceiver, filter);
        /* Android2.3 Added : Begin */
        IBinder b = ServiceManager.getService(BluetoothAdapter.BLUETOOTH_SERVICE);
        if (b == null) {
            throw new RuntimeException("Bluetooth service not available");
        }
        mBluetoothService = IBluetooth.Stub.asInterface(b);
        mRemoteHeadsets = new HashMap<BluetoothDevice, BluetoothRemoteHeadset>();
        /* Android2.3 Added : End */
   }

   /* Android2.3 Added : Begin */
   private class BluetoothRemoteHeadset {
       private int mState;
       private int mHeadsetType;
       /* MTK Removed : Begin */
       //private HeadsetBase mHeadset;
       /* MTK Removed : End */
       private IncomingConnectionInfo mIncomingInfo;

       BluetoothRemoteHeadset() {
           mState = BluetoothHeadset.STATE_DISCONNECTED;
           mHeadsetType = BluetoothHandsfree.TYPE_UNKNOWN;
           /* MTK Removed : Begin */
           //mHeadset = null;
           /* MTK Removed : End */
           mIncomingInfo = null;
       }

       BluetoothRemoteHeadset(int headsetType, IncomingConnectionInfo incomingInfo) {
           mState = BluetoothHeadset.STATE_DISCONNECTED;
           mHeadsetType = headsetType;
           /* MTK Removed : Begin */
           //mHeadset = null;
           /* MTK Removed : End */
           mIncomingInfo = incomingInfo;
       }
   }
   /* Android2.3 Added : End */
   /* Android2.3 Added : Begin */
   synchronized private BluetoothDevice getCurrentDevice() {
       for (BluetoothDevice device : mRemoteHeadsets.keySet()) {
           int state = mRemoteHeadsets.get(device).mState;
           if (state == BluetoothHeadset.STATE_CONNECTING ||
               state == BluetoothHeadset.STATE_CONNECTED) {
               return device;
           }
       }
       return null;
   }
   /* Android2.2 Added : End */

    @Override
    public void onStart(Intent intent, int startId) {
        log("[API] onStart");
         if (mAdapter == null) {
            logWarn("Stopping BluetoothHeadsetService: device does not have BT");
            stopSelf();
        } else {
            if (!sHasStarted) {
                if (DBG) log("Starting BluetoothHeadsetService");
                if (mAdapter.isEnabled()) {
                    mAg.start(mIncomingConnectionHandler);
                    mBtHandsfree.onBluetoothEnabled();
                }
                sHasStarted = true;
            }
        }
    }

    private final Handler mIncomingConnectionHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            log("[API] handleMessage : "+String.valueOf(msg.what));
            synchronized(BluetoothHeadsetService.this) {
                /* MTK Modified : Begin */
                //IncomingConnectionInfo info = (IncomingConnectionInfo)msg.obj;
                IncomingConnectionInfo info = null;
                BluetoothDevice device = null;
                BluetoothRemoteHeadset remoteHeadset = null;
                int state = BluetoothHeadset.STATE_DISCONNECTED;
         /*       if(msg.obj != null) {
                    info = (IncomingConnectionInfo)msg.obj;
                    device = info.mRemoteDevice;
                    state = mRemoteHeadsets.get(device).mState;
                }
                */
                /* MTK Modified : End */
                int type = BluetoothHandsfree.TYPE_UNKNOWN;
                if(msg.what == BluetoothAudioGateway.MSG_INCOMING_HEADSET_CONNECTION){
                    type = BluetoothHandsfree.TYPE_HEADSET;
                }else if(msg.what == BluetoothAudioGateway.MSG_INCOMING_HANDSFREE_CONNECTION){
                    type = BluetoothHandsfree.TYPE_HANDSFREE;
                }else{
                    switch(msg.what) {
                    /* MTK Added : Begin */
                    case BluetoothAudioGateway.RFCOMM_ERROR:
                        device = getCurrentDevice();
                        if(device == null) return;
                        remoteHeadset = mRemoteHeadsets.get(device);
                        if(remoteHeadset == null) return;
                        state = remoteHeadset.mState;
                        if (state != BluetoothHeadset.STATE_CONNECTING) {
                            log("RFCOMM_ERROR : mState != BluetoothHeadset.STATE_CONNECTING");
                            return;  // stale events
                        }                
                        setState(device, BluetoothHeadset.STATE_DISCONNECTED, BluetoothHeadset.RESULT_FAILURE);
                        return;
                    case BluetoothAudioGateway.RFCOMM_CONNECTED:
                        device = getCurrentDevice();
                        if(device == null) return;
                        remoteHeadset = mRemoteHeadsets.get(device);
                        if(remoteHeadset == null) return;
                        state = remoteHeadset.mState;
                        if (state != BluetoothHeadset.STATE_CONNECTING) {
                            log("RFCOMM_CONNECTED : mState != BluetoothHeadset.STATE_CONNECTING");
                            return;  // stale events
                        }
                        mBtHandsfree.connectHeadset(mAg, mRemoteHeadsets.get(device).mHeadsetType);
                        setState(device, BluetoothHeadset.STATE_CONNECTED, BluetoothHeadset.RESULT_SUCCESS);
                        return;
                    case BluetoothAudioGateway.RFCOMM_DISCONNECTED:
                        device = getCurrentDevice();
                        if(device != null)
                           setState(device, BluetoothHeadset.STATE_DISCONNECTED, BluetoothHeadset.RESULT_FAILURE);
                        return;
                    case BluetoothAudioGateway.SCO_ACCEPTED:
                    case BluetoothAudioGateway.SCO_CONNECTED:
                    case BluetoothAudioGateway.SCO_CLOSED:
                        if(msg.obj == null) {
                            logWarn("Remote Device is null when receive SCO msg");
                            mBtHandsfree.handleSCOEvent(msg.what, null);
                        }else {
                            mBtHandsfree.handleSCOEvent(msg.what, (BluetoothDevice)msg.obj);
                        }
                        return;
                    default:
                        log("[ERR] unknown msg="+String.valueOf(msg.what));
                        return;
                    /* MTK Added : End */
                    }
                }

                /* MTK Modified : Begin */
                info = (IncomingConnectionInfo)msg.obj;
                /* MTK Modified : End */
                Log.i(TAG, "Incoming rfcomm (" + BluetoothHandsfree.typeToString(type) +
                      ") connection from " + info.mRemoteDevice + "on channel " +
                      info.mRfcommChan);

                int priority = BluetoothHeadset.PRIORITY_OFF;
                /* MTK Removed : Begin */
                //HeadsetBase headset;
                /* MTK Removed : End */
                priority = getPriority(info.mRemoteDevice);
                /* MTK Modified : Begin */
                if (priority == BluetoothHeadset.PRIORITY_OFF) {
                    logInfo("Rejecting incoming connection because priority = " + priority);
                    // SH : reject the connection request
                    mAg.rejectConnection();
                    return;
                }
                /* MTK Modified : End */

                device = getCurrentDevice();

                state = BluetoothHeadset.STATE_DISCONNECTED;
                if (device != null) {
                    remoteHeadset = mRemoteHeadsets.get(device);
                    if(remoteHeadset != null){
                        state = mRemoteHeadsets.get(device).mState;
                    }
                }

                switch (state) {
                case BluetoothHeadset.STATE_DISCONNECTED:
                    // headset connecting us, lets join
                    remoteHeadset = new BluetoothRemoteHeadset(type, info);
                    mRemoteHeadsets.put(info.mRemoteDevice, remoteHeadset);

                    try {
                        mBluetoothService.notifyIncomingConnection(
                            info.mRemoteDevice.getAddress());
                    } catch (RemoteException e) {
                        Log.e(TAG, "notifyIncomingConnection");
                    }
                    break;
                case BluetoothHeadset.STATE_CONNECTING:
                    // It shall be never happened
                    if (!info.mRemoteDevice.equals(device)) {
                        // different headset, ignoring
                        logInfo("Already attempting connect to " + device +
                              ", disconnecting " + info.mRemoteDevice);
                        /* MTK Modified : Begin */
                        //headset = new HeadsetBase(mPowerManager, mAdapter, info.mRemoteDevice,
                        //        info.mSocketFd, info.mRfcommChan, null);
                        //headset.disconnect();
                        mAg.rejectConnection();
                        /* MTK Modified : End */

                        break;
                    }

                    // Incoming and Outgoing connections to the same headset.
                    // The state machine manager will cancel outgoing and accept the incoming one.
                    // Update the state
                    remoteHeadset = mRemoteHeadsets.get(info.mRemoteDevice);
                    if(remoteHeadset != null){
                        remoteHeadset.mHeadsetType = type;
                        remoteHeadset.mIncomingInfo = info;
                    }else {
                        logWarn("mRemoteHeadsets.get("+info.mRemoteDevice+") returns null");
                    }

                    try {
                        mBluetoothService.notifyIncomingConnection(
                            info.mRemoteDevice.getAddress());
                    } catch (RemoteException e) {
                        Log.e(TAG, "notifyIncomingConnection");
                    }
                    break;
                case BluetoothHeadset.STATE_CONNECTED:
                    logInfo("Already connected to " + device + ", disconnecting " +
                            info.mRemoteDevice);
                    /* MTK Modified : Begin */
                    //headset = new HeadsetBase(mPowerManager, mAdapter, info.mRemoteDevice,
                    //          info.mSocketFd, info.mRfcommChan, null);
                    //headset.disconnect();
                    //mAg.rejectConnection();
                    rejectIncomingConnection(info);
                    /* MTK Modified : End */
                    break;
                }
            }
        }
    };

    private void rejectIncomingConnection(IncomingConnectionInfo info) {
            //HeadsetBase headset = new HeadsetBase(mPowerManager, mAdapter,
            //    info.mRemoteDevice, info.mSocketFd, info.mRfcommChan, null);
            mAg.disconnect();
    }

    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            BluetoothDevice currDevice = getCurrentDevice();
            int state = BluetoothHeadset.STATE_DISCONNECTED;
            if (currDevice != null) {
                state = mRemoteHeadsets.get(currDevice).mState;
            }
            log("[Intent] action="+action+", state="+String.valueOf(state));
            if ((state == BluetoothHeadset.STATE_CONNECTED ||
                    state == BluetoothHeadset.STATE_CONNECTING) &&
                    action.equals(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED) &&
                    device.equals(currDevice)) {
                try {
                    mBinder.disconnectHeadset(currDevice);
                } catch (RemoteException e) {}
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                           BluetoothAdapter.ERROR)) {
                case BluetoothAdapter.STATE_ON:
                    adjustPriorities();
                    /* MTK Added : Begin */
                    broadcastHfpState(BluetoothProfileManager.STATE_ENABLING);
                    /* MTK Added : End */
                    mAg.start(mIncomingConnectionHandler);
                    mBtHandsfree.onBluetoothEnabled();
                    /* MTK Added : Begin */
                    broadcastHfpState(BluetoothProfileManager.STATE_ENABLED);
                    /* MTK Added : End */
                    break;
                /* MTK Removed : Begin */
                //case BluetoothAdapter.STATE_TURNING_OFF:
                //    mBtHandsfree.onBluetoothDisabled();
                //    mAg.stop();
                //    if (currDevice != null) {
                //        setState(currDevice, BluetoothHeadset.STATE_DISCONNECTED,
                //                BluetoothHeadset.RESULT_FAILURE,
                //                BluetoothHeadset.LOCAL_DISCONNECT);
                //    }
                //    break;
                /* MTK Removed : End */
                }
            /* MTK Added : Begin */
            } else if(action.equals(BluetoothProfileManager.ACTION_DISABLE_PROFILES)) {
                mBtHandsfree.onBluetoothDisabled();
                mAg.stop();
                if (currDevice != null) {
                    setState(currDevice, BluetoothHeadset.STATE_DISCONNECTED,
                            BluetoothHeadset.RESULT_FAILURE,
                            BluetoothHeadset.LOCAL_DISCONNECT);
                }
                broadcastHfpState(BluetoothProfileManager.STATE_DISABLED);
            /* MTK Added : End */
            } else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                                                   BluetoothDevice.ERROR);
                switch(bondState) {
                case BluetoothDevice.BOND_BONDED:
                    if (getPriority(device) == BluetoothHeadset.PRIORITY_UNDEFINED) {
                        setPriority(device, BluetoothHeadset.PRIORITY_ON);
                    }
                    break;
                case BluetoothDevice.BOND_NONE:
                    setPriority(device, BluetoothHeadset.PRIORITY_UNDEFINED);
                    break;
                }
            } else if (action.equals(AudioManager.VOLUME_CHANGED_ACTION)) {
                int streamType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
                if (streamType == AudioManager.STREAM_BLUETOOTH_SCO) {
                    mBtHandsfree.sendScoGainUpdate(intent.getIntExtra(
                            AudioManager.EXTRA_VOLUME_STREAM_VALUE, 0));
                }

            } else if (action.equals(BluetoothDevice.ACTION_UUID)) {
                if (device.equals(mDeviceSdpQuery) && device.equals(currDevice)) {
                    // We have got SDP records for the device we are interested in.
                    getSdpRecordsAndConnect(device);
                }
            }
        }
    };

    private static final int CONNECT_HEADSET_DELAYED = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONNECT_HEADSET_DELAYED:
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    getSdpRecordsAndConnect(device);
                    break;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // ------------------------------------------------------------------
    // Bluetooth Headset Connect
    // ------------------------------------------------------------------
    private static final int RFCOMM_CONNECTED             = 1;
    private static final int RFCOMM_ERROR                 = 2;

    private long mTimestamp;

    /**
     * Thread for RFCOMM connection
     * Messages are sent to mConnectingStatusHandler as connection progresses.
     */
    /* MTK Removed : Begin */
    /*
    private RfcommConnectThread mConnectThread;
    private class RfcommConnectThread extends Thread {
        private BluetoothDevice device;
        private int channel;
        private int type;

        private static final int EINTERRUPT = -1000;
        private static final int ECONNREFUSED = -111;

        public RfcommConnectThread(BluetoothDevice device, int channel, int type) {
            super();
            this.device = device;
            this.channel = channel;
            this.type = type;
        }

        private int waitForConnect(HeadsetBase headset) {
            // Try to connect for 20 seconds
            int result = 0;
            for (int i=0; i < 40 && result == 0; i++) {
                // waitForAsyncConnect returns 0 on timeout, 1 on success, < 0 on error.
                result = headset.waitForAsyncConnect(500, mConnectedStatusHandler);
                if (isInterrupted()) {
                    headset.disconnect();
                    return EINTERRUPT;
                }
            }
            return result;
        }

        @Override
        public void run() {
            long timestamp;

            timestamp = System.currentTimeMillis();
            HeadsetBase headset = new HeadsetBase(mPowerManager, mAdapter, device, channel);

            int result = waitForConnect(headset);

            if (result != EINTERRUPT && result != 1) {
                if (result == ECONNREFUSED && mDeviceSdpQuery == null) {
                    // The rfcomm channel number might have changed, do SDP
                    // query and try to connect again.
                    mDeviceSdpQuery = getCurrentDevice();
                    device.fetchUuidsWithSdp();
                    mConnectThread = null;
                    return;
                } else {
                    Log.i(TAG, "Trying to connect to rfcomm socket again after 1 sec");
                    try {
                      sleep(1000);  // 1 second
                    } catch (InterruptedException e) {}
                }
                result = waitForConnect(headset);
            }
            mDeviceSdpQuery = null;
            if (result == EINTERRUPT) return;

            if (DBG) log("RFCOMM connection attempt took " +
                  (System.currentTimeMillis() - timestamp) + " ms");
            if (isInterrupted()) {
                headset.disconnect();
                return;
            }
            if (result < 0) {
                Log.w(TAG, "headset.waitForAsyncConnect() error: " + result);
                mConnectingStatusHandler.obtainMessage(RFCOMM_ERROR).sendToTarget();
                return;
            } else if (result == 0) {
                mConnectingStatusHandler.obtainMessage(RFCOMM_ERROR).sendToTarget();
                Log.w(TAG, "mHeadset.waitForAsyncConnect() error: " + result + "(timeout)");
                return;
            } else {
                mConnectingStatusHandler.obtainMessage(RFCOMM_CONNECTED, headset).sendToTarget();
            }
        }
    }
    */
     /* MTK Removed : End */
    /**
     * Receives events from mConnectThread back in the main thread.
     */
    /* MTK Removed : Begin */
    /*
    private final Handler mConnectingStatusHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            BluetoothDevice device = getCurrentDevice();
            if (device == null ||
                mRemoteHeadsets.get(device).mState != BluetoothHeadset.STATE_CONNECTING) {
                return;  // stale events
            }

            switch (msg.what) {
            case RFCOMM_ERROR:
                if (DBG) log("Rfcomm error");
                mConnectThread = null;
                setState(device,
                         BluetoothHeadset.STATE_DISCONNECTED, BluetoothHeadset.RESULT_FAILURE,
                         BluetoothHeadset.LOCAL_DISCONNECT);
                break;
            case RFCOMM_CONNECTED:
                if (DBG) log("Rfcomm connected");
                mConnectThread = null;
                HeadsetBase headset = (HeadsetBase)msg.obj;
                setState(device,
                        BluetoothHeadset.STATE_CONNECTED, BluetoothHeadset.RESULT_SUCCESS);

                mRemoteHeadsets.get(device).mHeadset = headset;
                mBtHandsfree.connectHeadset(headset, mRemoteHeadsets.get(device).mHeadsetType);
                break;
            }
        }
    };
    */
     /* MTK Removed : End */
    /**
     * Receives events from a connected RFCOMM socket back in the main thread.
     */
     /* MTK Removed : Begin */
    /*
    private final Handler mConnectedStatusHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case HeadsetBase.RFCOMM_DISCONNECTED:
                mBtHandsfree.resetAtState();
                BluetoothDevice device = getCurrentDevice();
                if (device != null) {
                    setState(device,
                        BluetoothHeadset.STATE_DISCONNECTED, BluetoothHeadset.RESULT_FAILURE,
                        BluetoothHeadset.REMOTE_DISCONNECT);
                }
                break;
            }
        }
    };
    */
     /* MTK Removed : End */

    private void setState(BluetoothDevice device, int state) {
        setState(device, state, BluetoothHeadset.RESULT_SUCCESS);
    }

    private void setState(BluetoothDevice device, int state, int result) {
        setState(device, state, result, -1);
    }

    private synchronized void setState(BluetoothDevice device,
        int state, int result, int initiator) {
        int prevState = mRemoteHeadsets.get(device).mState;
        if (state != prevState) {
            if (DBG) log("Device: " + device +
                " Headset  state" + prevState + " -> " + state + ", result = " + result);
            if (prevState == BluetoothHeadset.STATE_CONNECTED) {
                mBtHandsfree.disconnectHeadset();
            }
            Intent intent = new Intent(BluetoothHeadset.ACTION_STATE_CHANGED);
            intent.putExtra(BluetoothHeadset.EXTRA_PREVIOUS_STATE, prevState);
            intent.putExtra(BluetoothHeadset.EXTRA_STATE, state);
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
            // Add Extra EXTRA_DISCONNECT_INITIATOR for DISCONNECTED state
            if (state == BluetoothHeadset.STATE_DISCONNECTED) {
                if (initiator == -1) {
                    log("Headset Disconnected Intent without Disconnect Initiator extra");
                } else {
                    intent.putExtra(BluetoothHeadset.EXTRA_DISCONNECT_INITIATOR,
                                    initiator);
                }
                //mRemoteHeadsets.get(device).mHeadset = null;
                mRemoteHeadsets.get(device).mHeadsetType = BluetoothHandsfree.TYPE_UNKNOWN;
            }

            mRemoteHeadsets.get(device).mState = state;

            sendBroadcast(intent, BLUETOOTH_PERM);
            if (state == BluetoothHeadset.STATE_CONNECTED) {
                // Set the priority to AUTO_CONNECT
                setPriority(device, BluetoothHeadset.PRIORITY_AUTO_CONNECT);
                adjustOtherHeadsetPriorities(device);
            }
       }
    }

    private void adjustOtherHeadsetPriorities(BluetoothDevice connectedDevice) {
       for (BluetoothDevice device : mAdapter.getBondedDevices()) {
          if (getPriority(device) >= BluetoothHeadset.PRIORITY_AUTO_CONNECT &&
              !device.equals(connectedDevice)) {
              setPriority(device, BluetoothHeadset.PRIORITY_ON);
          }
       }
    }

    private void setPriority(BluetoothDevice device, int priority) {
        try {
            mBinder.setPriority(device, priority);
        } catch (RemoteException e) {
            Log.e(TAG, "Error while setting priority for: " + device);
        }
    }

    private int getPriority(BluetoothDevice device) {
        try {
            return mBinder.getPriority(device);
        } catch (RemoteException e) {
            Log.e(TAG, "Error while getting priority for: " + device);
        }
        return BluetoothHeadset.PRIORITY_UNDEFINED;
    }

    private void adjustPriorities() {
        // This is to ensure backward compatibility.
        // Only 1 device is set to AUTO_CONNECT
        BluetoothDevice savedDevice = null;
        int max_priority = BluetoothHeadset.PRIORITY_AUTO_CONNECT;
        if (mAdapter.getBondedDevices() != null) {
            for (BluetoothDevice device : mAdapter.getBondedDevices()) {
                int priority = getPriority(device);
                if (priority >= BluetoothHeadset.PRIORITY_AUTO_CONNECT) {
                    setPriority(device, BluetoothHeadset.PRIORITY_ON);
                }
                if (priority >= max_priority) {
                    max_priority = priority;
                    savedDevice = device;
                }
            }
            if (savedDevice != null) {
                setPriority(savedDevice, BluetoothHeadset.PRIORITY_AUTO_CONNECT);
            }
        }
    }

    private synchronized void getSdpRecordsAndConnect(BluetoothDevice device) {
        BluetoothRemoteHeadset remoteHeadset = null;
        log("[API] getSdpRecordsAndConnect");
        if (device == null || !device.equals(getCurrentDevice())) {
            // stale
            return;
        }

        // Check if incoming connection has already connected.
        if(mRemoteHeadsets == null ||
            (remoteHeadset = mRemoteHeadsets.get(device)) == null ||
            remoteHeadset.mState == BluetoothHeadset.STATE_CONNECTED) {
            logWarn("getSdpRecordsAndConnect failed");
            return;
        }

        ParcelUuid[] uuids = device.getUuids();
        int type = BluetoothHandsfree.TYPE_UNKNOWN;
        if (uuids != null) {
            if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Handsfree)) {
                log("SDP UUID: TYPE_HANDSFREE");
                type = BluetoothHandsfree.TYPE_HANDSFREE;
                mRemoteHeadsets.get(device).mHeadsetType = type;
                int channel = device.getServiceChannel(BluetoothUuid.Handsfree);
                /* MTK Modified : Begin */
                //mConnectThread = new RfcommConnectThread(device, channel, type);
                //mConnectThread.start();
                if( mAg.waitForAsyncConnect(device, 20000, type) > 0 )
                {
                    //mHeadsetType = BluetoothHandsfree.TYPE_HANDSFREE;
                }
                else
                {
                    log("[ERR] waitForAsyncConnect failed");
                    setState(device, BluetoothHeadset.STATE_DISCONNECTED,
                             BluetoothHeadset.RESULT_FAILURE,
                             BluetoothHeadset.LOCAL_DISCONNECT);
                }
                /* MTK Modified : End */
                if (getPriority(device) < BluetoothHeadset.PRIORITY_AUTO_CONNECT) {
                    setPriority(device, BluetoothHeadset.PRIORITY_AUTO_CONNECT);
                }
                return;
            } else if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.HSP)) {
                log("SDP UUID: TYPE_HEADSET");
                type = BluetoothHandsfree.TYPE_HEADSET;
                mRemoteHeadsets.get(device).mHeadsetType = type;
                int channel = device.getServiceChannel(BluetoothUuid.HSP);
                /* MTK Modified : Begin */
                //mConnectThread = new RfcommConnectThread(device, channel, type);
                //mConnectThread.start();
                if( mAg.waitForAsyncConnect(device, 20000, type) > 0 )
                {
                    //mHeadsetType = BluetoothHandsfree.TYPE_HANDSFREE;
                }
                else
                {
                    log("[ERR] waitForAsyncConnect failed");
                    setState(device, BluetoothHeadset.STATE_DISCONNECTED,
                             BluetoothHeadset.RESULT_FAILURE,
                             BluetoothHeadset.LOCAL_DISCONNECT);
                }
                /* MTK Modified : End */
                if (getPriority(device) < BluetoothHeadset.PRIORITY_AUTO_CONNECT) {
                    setPriority(device, BluetoothHeadset.PRIORITY_AUTO_CONNECT);
                }
                return;
            }
        }
        log("SDP UUID: TYPE_UNKNOWN");
        mRemoteHeadsets.get(device).mHeadsetType = type;
        setState(device, BluetoothHeadset.STATE_DISCONNECTED,
                BluetoothHeadset.RESULT_FAILURE, BluetoothHeadset.LOCAL_DISCONNECT);
        return;
    }

    /**
     * Handlers for incoming service calls
     */
    private final IBluetoothHeadset.Stub mBinder = new IBluetoothHeadset.Stub() {
        public int getState(BluetoothDevice device) {
            enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
            BluetoothRemoteHeadset headset = mRemoteHeadsets.get(device);
            if (headset == null) {
                return BluetoothHeadset.STATE_DISCONNECTED;
            }
            return headset.mState;
        }
        public BluetoothDevice getCurrentHeadset() {
            enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
            return getCurrentDevice();
        }
        public boolean connectHeadset(BluetoothDevice device) {
            enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                                           "Need BLUETOOTH_ADMIN permission");
            synchronized (BluetoothHeadsetService.this) {
                try {
                    return mBluetoothService.connectHeadset(device.getAddress());
                } catch (RemoteException e) {
                    Log.e(TAG, "connectHeadset");
                    return false;
                }
            }
        }
        public void disconnectHeadset(BluetoothDevice device) {
            enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                                           "Need BLUETOOTH_ADMIN permission");
            synchronized (BluetoothHeadsetService.this) {
                try {
                    mBluetoothService.disconnectHeadset(device.getAddress());
                } catch (RemoteException e) {
                    Log.e(TAG, "disconnectHeadset");
                }
            }
        }
        public boolean isConnected(BluetoothDevice device) {
            enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");

            BluetoothRemoteHeadset headset = mRemoteHeadsets.get(device);
            return headset != null && headset.mState == BluetoothHeadset.STATE_CONNECTED;
        }
        public boolean startVoiceRecognition() {
            enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
            synchronized (BluetoothHeadsetService.this) {
                BluetoothDevice device = getCurrentDevice();

                if (device == null ||
                    mRemoteHeadsets.get(device).mState != BluetoothHeadset.STATE_CONNECTED) {
                    return false;
                }
                return mBtHandsfree.startVoiceRecognition();
            }
        }
        public boolean stopVoiceRecognition() {
            enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
            synchronized (BluetoothHeadsetService.this) {
                BluetoothDevice device = getCurrentDevice();

                if (device == null ||
                    mRemoteHeadsets.get(device).mState != BluetoothHeadset.STATE_CONNECTED) {
                    return false;
                }

                return mBtHandsfree.stopVoiceRecognition();
            }
        }
        public int getBatteryUsageHint() {
            enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
            // SH : porting
            log("[BIND] getBatteryUsageHint");
            return BluetoothAudioGateway.getAtInputCount();
            //return HeadsetBase.getAtInputCount();
        }
        public int getPriority(BluetoothDevice device) {
            enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                "Need BLUETOOTH_ADMIN permission");
            synchronized (BluetoothHeadsetService.this) {
                int priority = Settings.Secure.getInt(getContentResolver(),
                        Settings.Secure.getBluetoothHeadsetPriorityKey(device.getAddress()),
                        BluetoothHeadset.PRIORITY_UNDEFINED);
                return priority;
            }
        }

        public boolean setPriority(BluetoothDevice device, int priority) {
            enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                "Need BLUETOOTH_ADMIN permission");
            synchronized (BluetoothHeadsetService.this) {
                if (!BluetoothAdapter.checkBluetoothAddress(device.getAddress())) {
                        return false;
                }
                if (priority < BluetoothHeadset.PRIORITY_OFF) {
                    return false;
                }
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.getBluetoothHeadsetPriorityKey(device.getAddress()),
                        priority);
                if (DBG) log("Saved priority " + device + " = " + priority);
                return true;
            }
        }
        public boolean createIncomingConnect(BluetoothDevice device) {
            synchronized (BluetoothHeadsetService.this) {
                /* MTK Modified : Begin */
                //HeadsetBase headset;
                //setState(device, BluetoothHeadset.STATE_CONNECTING);

                IncomingConnectionInfo info = mRemoteHeadsets.get(device).mIncomingInfo;
                //headset = new HeadsetBase(mPowerManager, mAdapter, device,
                //        info.mSocketFd, info.mRfcommChan,
                //        mConnectedStatusHandler);

                //mRemoteHeadsets.get(device).mHeadset = headset;

                //mConnectingStatusHandler.obtainMessage(RFCOMM_CONNECTED, headset).sendToTarget();
                //return true;

                // SH : Audiogateway will send this message after connected                
                /* This function might want to connect directly through a known */
                /* Rfcomm channel */
                /* SH PTS fix : Begin */
                if( mAg.acceptConnection() > 0 )
                {
                    //mRemoteDevice = info.mRemoteDevice;
                    setState(device, BluetoothHeadset.STATE_CONNECTING);
                    //mHeadsetType = type;    
                    return true;
                } else {
                    return false;
                }
                /* SH PTS fix : End */
                /* MTK Modified : End */
          }
      }

        public boolean rejectIncomingConnect(BluetoothDevice device) {
            synchronized (BluetoothHeadsetService.this) {
                BluetoothRemoteHeadset headset = mRemoteHeadsets.get(device);
                if (headset != null) {
                    IncomingConnectionInfo info = headset.mIncomingInfo;
                    rejectIncomingConnection(info);
                } else {
                    Log.e(TAG, "Error no record of remote headset");
                }
                return true;
            }
        }
        
        public boolean acceptIncomingConnect(BluetoothDevice device) {
            synchronized (BluetoothHeadsetService.this) {
                /* MTK Modified : Begin */
                //HeadsetBase headset;
                BluetoothRemoteHeadset cachedHeadset = mRemoteHeadsets.get(device);
                if (cachedHeadset == null) {
                    Log.e(TAG, "Cached Headset is Null in acceptIncomingConnect");
                    return false;
                }
                //IncomingConnectionInfo info = cachedHeadset.mIncomingInfo;
                //headset = new HeadsetBase(mPowerManager, mAdapter, device,
                //        info.mSocketFd, info.mRfcommChan, mConnectedStatusHandler);

                //setState(device, BluetoothHeadset.STATE_CONNECTED, BluetoothHeadset.RESULT_SUCCESS);
                // SH : Audiogateway will send this message after connected                
                if( mAg.acceptConnection() > 0 )
                {
                    //mRemoteDevice = info.mRemoteDevice;
                    setState(device, BluetoothHeadset.STATE_CONNECTING);
                    //mHeadsetType = type;    
                    //return true;
                } else {
                    return false;
                }

                //cachedHeadset.mHeadset = headset;
                //mBtHandsfree.connectHeadset(headset, cachedHeadset.mHeadsetType);

                if (DBG) log("Successfully used incoming connection");
                return true;
                /* MTK Modified : End */
            }
        }

        public  boolean cancelConnectThread() {
            synchronized (BluetoothHeadsetService.this) {
                /* MTK Modified : Begin */
                //if (mConnectThread != null) {
                    // cancel the connection thread
                //    mConnectThread.interrupt();
                //    try {
                //        mConnectThread.join();
                //    } catch (InterruptedException e) {
                //        Log.e(TAG, "Connection cancelled twice?", e);
                //    }
                //    mConnectThread = null;
                //}
                /* TODO: we should disconnect inprogress connecting procedure */
                /* connecting is ongoing, cancel it */
                mAg.disconnect();
                return true;
                /* MTK Modified : End */
            }
        }

        public boolean connectHeadsetInternal(BluetoothDevice device) {
            synchronized (BluetoothHeadsetService.this) {
                BluetoothDevice currDevice = getCurrentDevice();
                if (currDevice == null) {
                    BluetoothRemoteHeadset headset = new BluetoothRemoteHeadset();
                    mRemoteHeadsets.put(device, headset);

                    setState(device, BluetoothHeadset.STATE_CONNECTING);
                    if (device.getUuids() == null) {
                        // We might not have got the UUID change notification from
                        // Bluez yet, if we have just paired. Try after 1.5 secs.
                        Message msg = new Message();
                        msg.what = CONNECT_HEADSET_DELAYED;
                        msg.obj = device;
                        mHandler.sendMessageDelayed(msg, 1500);
                    } else {
                        getSdpRecordsAndConnect(device);
                    }
                    return true;
                } else {
                      Log.w(TAG, "connectHeadset(" + device + "): failed: already in state " +
                            mRemoteHeadsets.get(currDevice).mState +
                            " with headset " + currDevice);
                }
                return false;
            }
        }

        public boolean disconnectHeadsetInternal(BluetoothDevice device) {
            synchronized (BluetoothHeadsetService.this) {
                BluetoothRemoteHeadset remoteHeadset = mRemoteHeadsets.get(device);
                if (remoteHeadset == null) return false;

                if (remoteHeadset.mState == BluetoothHeadset.STATE_CONNECTED) {
                    // Send a dummy battery level message to force headset
                    // out of sniff mode so that it will immediately notice
                    // the disconnection. We are currently sending it for
                    // handsfree only.
                    // TODO: Call hci_conn_enter_active_mode() from
                    // rfcomm_send_disc() in the kernel instead.
                    // See http://b/1716887
                    /* MTK Removed : Begin */
                    //HeadsetBase headset = remoteHeadset.mHeadset;
                    //if (remoteHeadset.mHeadsetType == BluetoothHandsfree.TYPE_HANDSFREE) {
                    //    headset.sendURC("+CIEV: 7,3");
                    //}
                    /* MTK Removed : End */
                    /* MTK Modified : Begin */
                    //if (headset != null) {
                    //    headset.disconnect();
                    //    headset = null;
                    //}
                    mAg.disconnect();
                    /* MTK Modified : End */
                    setState(device, BluetoothHeadset.STATE_DISCONNECTED,
                             BluetoothHeadset.RESULT_CANCELED,
                             BluetoothHeadset.LOCAL_DISCONNECT);
                    return true;
                } else if (remoteHeadset.mState == BluetoothHeadset.STATE_CONNECTING) {
                    // The state machine would have canceled the connect thread.
                    // Just set the state here.
                    setState(device, BluetoothHeadset.STATE_DISCONNECTED,
                              BluetoothHeadset.RESULT_CANCELED,
                              BluetoothHeadset.LOCAL_DISCONNECT);
                    return true;
                }
                return false;
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DBG) log("Stopping BluetoothHeadsetService");
        unregisterReceiver(mBluetoothReceiver);
        mBtHandsfree.onBluetoothDisabled();
        mAg.stop();
        sHasStarted = false;
        if (getCurrentDevice() != null) {
            setState(getCurrentDevice(), BluetoothHeadset.STATE_DISCONNECTED,
                 BluetoothHeadset.RESULT_CANCELED,
                 BluetoothHeadset.LOCAL_DISCONNECT);
        }
    }

    private static void log(String msg) {
        Log.d(TAG, "[BT][HFG]"+msg);
    }
    private static void logInfo(String msg) {
        Log.i(TAG, "[BT][HFG]"+msg);
    }
    private static void logWarn(String msg) {
        Log.w(TAG, "[BT][HFG]"+msg);
    }

    /****************************************************************/
    private void broadcastHfpState(int state) {
		BluetoothProfileManager.Profile profile;
		Intent intent = new Intent(BluetoothProfileManager.ACTION_PROFILE_STATE_UPDATE);
		profile = BluetoothProfileManager.Profile.Bluetooth_HEADSET;
		intent.putExtra(BluetoothProfileManager.EXTRA_PROFILE, profile);
		intent.putExtra(BluetoothProfileManager.EXTRA_NEW_STATE, state);
		sendBroadcast(intent, android.Manifest.permission.BLUETOOTH);
    }
}
