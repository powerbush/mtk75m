/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

package com.android.phone;
import static com.android.phone.TimeConsumingPreferenceActivity.EXCEPTION_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.RESPONSE_ERROR;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.telephony.Phone;
import android.os.SystemProperties;
import com.android.internal.telephony.PhoneFactory;
import android.os.AsyncResult;
import android.preference.Preference.OnPreferenceClickListener;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import android.telephony.TelephonyManager;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;

/* Fion add start */
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;
/* Fion add end */

public class CellBroadcastCheckBox extends CheckBoxPreference {
    private static final String LOG_TAG = "Settings/CellBroadcastCheckBox";
    private static final boolean DBG = (PhoneApp.DBG_LEVEL >= 2);
    private static final int MESSAGE_GET_STATE = 100;
    private static final int MESSAGE_SET_STATE = 101;

    private TimeConsumingPreferenceListener mListener;
    private MyHandler mHandler = new MyHandler();
    private Phone mPhone;
    private boolean lastCheckStatus;

/* Fion add start */
    int mSimId;;
/* Fion add end */

    public CellBroadcastCheckBox(Context context) {
        this(context, null);
    }

    public CellBroadcastCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPhone = PhoneFactory.getDefaultPhone();
    }

    private Context getPrefContext() {
        return getContext();
    }

    @Override
    protected void onClick() {
        super.onClick();
        boolean state = isChecked();
        lastCheckStatus = !state;
        setCBState(state ? 0 : 1);
        setChecked(!state);
        //updateSummary(state);
    }
    
    public void updateSummary(boolean isChecked) {
        if (isChecked) {
            this.setSummary(R.string.sum_cell_broadcast_control_off);
        }else {
            this.setSummary(R.string.sum_cell_broadcast_control_on);
        }
    }

/* Fion add start */
    void init(TimeConsumingPreferenceListener listener, boolean skipReading, int simId) {
        Xlog.d(LOG_TAG,"init, simId = "+simId);
        mListener = listener;
        mSimId = simId;
        
        if (!skipReading) {
            TelephonyManager telephonyManager = (TelephonyManager) ((CellBroadcastActivity)listener).getSystemService(Context.TELEPHONY_SERVICE);
        /* Fion add start */        
                boolean hasIccCard;
                if (CallSettings.isMultipleSim())
                {
                    hasIccCard=telephonyManager.hasIccCardGemini(mSimId);
                }
                else
                {
                    hasIccCard=telephonyManager.hasIccCard();
                }
        
        /* Fion add end */

            if (hasIccCard) {
                getCBState(true);
            } else {
                setChecked(false);
                setEnabled(false);
            }
        } else {
            // TODO
        }
    }
/* Fion add end */

    public final void displayMessage(int strId) {
        Toast.makeText(getPrefContext(), getPrefContext().getString(strId),
                Toast.LENGTH_SHORT).show();
    }

    public final void displayMessage(String str) {
        Toast.makeText(getPrefContext(), str, Toast.LENGTH_SHORT).show();
    }

    private void getCBState(boolean reason) {
        Message msg;
        if (reason)
            msg = mHandler.obtainMessage(MESSAGE_GET_STATE, 0,MESSAGE_GET_STATE, null);
        else {
            msg = mHandler.obtainMessage(MESSAGE_GET_STATE, 0,MESSAGE_SET_STATE, null);
        }
/* Fion add start */        
        if (CallSettings.isMultipleSim())
        {
            ((GeminiPhone)mPhone).queryCellBroadcastSmsActivationGemini(msg, mSimId);
        }
        else
        {
            mPhone.queryCellBroadcastSmsActivation(msg);
        }

/* Fion add end */
        if (reason) {
            if (mListener != null && msg.arg2 == MESSAGE_SET_STATE)
                mListener.onStarted(CellBroadcastCheckBox.this, reason);
        }
    }

    private void setCBState(int state) {
        Message msg;
        msg = mHandler.obtainMessage(MESSAGE_SET_STATE, 0, MESSAGE_SET_STATE,null);
/* Fion add start */        
        if (CallSettings.isMultipleSim())
        {
            ((GeminiPhone)mPhone).activateCellBroadcastSmsGemini(state,msg, mSimId);
        }
        else
        {
            mPhone.activateCellBroadcastSms(state,msg);
        }

/* Fion add end */

        if (mListener != null)
            mListener.onStarted(CellBroadcastCheckBox.this, false);
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_GET_STATE:
                handleGetStateResponse(msg);
                break;
            case MESSAGE_SET_STATE:
                handleSetStateResponse(msg);
                break;
            }
        }

        private void handleGetStateResponse(Message msg) {
            if (msg.arg2 == MESSAGE_GET_STATE) {
                if (mListener != null){
                    //mListener.onFinished(CellBroadcastCheckBox.this, true);
                    if (DBG) Xlog.d(LOG_TAG, "For init query, there's no reading dialog!");
                }
            } else {
                if (mListener != null)
                {
                    mListener.onFinished(CellBroadcastCheckBox.this, false);
                    if (false == lastCheckStatus)
                    {
                        RecoverChannelSettings setting = new RecoverChannelSettings(mSimId, getContext().getContentResolver());
                        setting.updateChannelStatus();
                    }
                }
            }
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar == null) {
                Xlog.i(LOG_TAG, "handleGetStateResponse,ar is null");
                if (msg.arg2 == MESSAGE_GET_STATE) {
                    CellBroadcastCheckBox.this.setChecked(false);
                    CellBroadcastCheckBox.this.setEnabled(false);
                }else{
                    if (mListener != null)
                        mListener.onError(CellBroadcastCheckBox.this,EXCEPTION_ERROR);
                }
                return;
            }
            if (ar.exception != null) {
                if (DBG)
                    Xlog.d(LOG_TAG, "handleGetStateResponse: ar.exception="+ ar.exception);
                if (msg.arg2 == MESSAGE_GET_STATE) {
                    CellBroadcastCheckBox.this.setChecked(false);
                    CellBroadcastCheckBox.this.setEnabled(false);
                }else{
                      if (mListener != null)
                          mListener.onError(CellBroadcastCheckBox.this,EXCEPTION_ERROR);
                }
                return;
            } else {
                if (ar.userObj instanceof Throwable) {
                    if (msg.arg2 == MESSAGE_GET_STATE) {
                        CellBroadcastCheckBox.this.setChecked(false);
                        CellBroadcastCheckBox.this.setEnabled(false);
                    }else{
                        if (mListener != null)
                            mListener.onError(CellBroadcastCheckBox.this,RESPONSE_ERROR);
                    }
                    return;
                } else {
                    if (ar.result != null) {
                        Boolean state = (Boolean) ar.result;
                        CellBroadcastCheckBox.this.setChecked(state.booleanValue());
                    } else {
                        if (msg.arg2 == MESSAGE_GET_STATE) {
                            CellBroadcastCheckBox.this.setChecked(false);
                            CellBroadcastCheckBox.this.setEnabled(false);
                        }else{
                            if (mListener != null)
                                mListener.onError(CellBroadcastCheckBox.this,RESPONSE_ERROR);
                        }
                        return;
                    }
                }
            }
        }

        private void handleSetStateResponse(Message msg) {
            if (msg.arg2 == MESSAGE_SET_STATE) {
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar == null) {
                    Xlog.i(LOG_TAG, "handleSetStateResponse,ar is null");
                    mListener.onError(CellBroadcastCheckBox.this,EXCEPTION_ERROR);
                    return;
                }
                if (ar.exception != null) {
                    if (DBG)
                        Xlog.d(LOG_TAG, "handleSetStateResponse: ar.exception="+ ar.exception);
                    if (mListener != null)
                        mListener.onError(CellBroadcastCheckBox.this,EXCEPTION_ERROR);
                } else {
                        Xlog.i(LOG_TAG, "handleSetStateResponse: re get ok");
                        getCBState(false);
                }
            }
        }
    }
}

class RecoverChannelSettings extends Handler {
    
    private static final int MESSAGE_GET_CONFIG = 100;
    private static final int MESSAGE_SET_CONFIG = 101;
    private static final String LOG_TAG = "RecoverChannelSettings";
    private static final String KEYID = "_id";
    private static final String NAME = "name";
    private static final String NUMBER = "number";
    private static final String ENABLE = "enable";
    private static final Uri CHANNEL_URI = Uri.parse("content://cb/channel");
    private static final Uri CHANNEL_URI1 = Uri.parse("content://cb/channel1");
    
    private Uri mUri = CHANNEL_URI;
    private int mSimId;    
    Phone mPhone = null;
    private ContentResolver resolver = null;
    
    public RecoverChannelSettings(int simId, ContentResolver resolver)
    {
        mSimId = simId;
        mPhone = PhoneFactory.getDefaultPhone();
        this.resolver = resolver;
        
        if (CallSettings.isMultipleSim())
        {
            if (mSimId == Phone.GEMINI_SIM_2)
            {
                mUri = CHANNEL_URI1;
            }
        }
    }
    
    private ArrayList<CellBroadcastChannel> mChannelArray = new ArrayList<CellBroadcastChannel>();
    
    private boolean updateChannelToDatabase(int index) {
        String[] projection = new String[] { KEYID, NAME, NUMBER, ENABLE };
        this.mChannelArray.get(index);
        final int id = mChannelArray.get(index).getKeyId();
        final String name = mChannelArray.get(index).getChannelName();
        final boolean enable = false;
        final int number = mChannelArray.get(index).getChannelId();
        ContentValues values = new ContentValues();
        values.put(KEYID, id);
        values.put(NAME, name);
        values.put(NUMBER, number);
        values.put(ENABLE, Integer.valueOf(enable ? 1:0));
        String where = KEYID + "=" + mChannelArray.get(index).getKeyId();
        
        try {
            int lines = resolver.update(mUri, values,where, null);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
    
    boolean queryChannelFromDatabase() {
        /*ClearChannel();*/
        String[] projection = new String[] { KEYID, NAME, NUMBER, ENABLE };
        try {
            Cursor cursor = resolver.query(mUri,projection, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    CellBroadcastChannel channel = new CellBroadcastChannel();
                    channel.setChannelId(cursor.getInt(2));
                    channel.setKeyId(cursor.getInt(0));// keyid for delete or edit
                    channel.setChannelName(cursor.getString(1));
                    //channel.setChannelState(false);
                    channel.setChannelState(cursor.getInt(3) == 1);
                    mChannelArray.add(channel);
                }
                cursor.close();
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }
    
    
    void updateChannelStatus()
    {
        if (false == queryChannelFromDatabase())
        {
            return ;
        }
        
        int length = mChannelArray.size();
        for (int i = 0; i < length; i++) {
            int keyId = mChannelArray.get(i).getKeyId();
            String channelName = mChannelArray.get(i).getChannelName();
            int channelId = mChannelArray.get(i).getChannelId();
            boolean channelState = mChannelArray.get(i).getChannelState();
            String title = channelName + "(" + String.valueOf(channelId) + ")";
            //channel.setTitle(title);
            final CellBroadcastChannel oldChannel = new CellBroadcastChannel(keyId, channelId, channelName, channelState);
            if (channelState)
            {
                SmsBroadcastConfigInfo[] objectList = makeChannelConfigArray(oldChannel);
                setCellBroadcastConfig(objectList, i);
                //break;
            }
        }
    }
    
    private void setCellBroadcastConfig(SmsBroadcastConfigInfo[] objectList, int index) {
        Message msg = obtainMessage(MESSAGE_SET_CONFIG, 0, index, null);
        if (CallSettings.isMultipleSim())
        {
            ((GeminiPhone)mPhone).setCellBroadcastSmsConfigGemini(objectList, objectList, msg, mSimId);
        }
        else
        {
            mPhone.setCellBroadcastSmsConfig(objectList, objectList, msg);
        }
    }
    
    private SmsBroadcastConfigInfo[] makeChannelConfigArray(CellBroadcastChannel channel) {
        SmsBroadcastConfigInfo[] objectList = new SmsBroadcastConfigInfo[2];
        int tChannelId=channel.getChannelId();
        objectList[0] = new SmsBroadcastConfigInfo(tChannelId,tChannelId, -1, -1, false);
        objectList[1] = new SmsBroadcastConfigInfo(tChannelId,tChannelId, -1, -1, channel.getChannelState());
        return objectList;
    }
    
    public void handleMessage(Message msg) {
        switch (msg.what) {
        case MESSAGE_GET_CONFIG:
            handleGetCellBroadcastConfigResponse(msg);
            break;
        case MESSAGE_SET_CONFIG:
            handleSetCellBroadcastConfigResponse(msg);
            break;
        }
    }
    
    private void handleGetCellBroadcastConfigResponse(Message msg) {
        
        AsyncResult ar = (AsyncResult) msg.obj;
        if (ar == null) {
            return;
        }
        if (ar.exception != null) {
            Xlog.d(LOG_TAG,"handleGetCellBroadcastConfigResponse: ar.exception="+ ar.exception);
            return;
        } else {
            if (ar != null && ar.userObj instanceof Throwable) {
                Xlog.d(LOG_TAG,"handleGetCellBroadcastConfigResponse: ar.exception="+ ar.exception);
            } else {
                Xlog.d(LOG_TAG,"handleGetCellBroadcastConfigResponse: success!");
            }
        }
    }

    private void handleSetCellBroadcastConfigResponse(Message msg) {
        //if (msg.arg2 == MESSAGE_SET_CONFIG) {
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar == null) {
                Xlog.i(LOG_TAG,"handleSetCellBroadcastConfigResponse,ar is null");
                //onError(mLanguagePreference, RESPONSE_ERROR);
            }
            if (ar.exception != null) {
                //if (DBG)
                this.updateChannelToDatabase(msg.arg2);
                Xlog.d(LOG_TAG,"handleSetCellBroadcastConfigResponse: ar.exception="+ ar.exception);
                //onError(mLanguagePreference, EXCEPTION_ERROR);
            }
            //getCellBroadcastConfig(false);
        //}
    }
    
    private void getCellBroadcastConfig(boolean reason) {
        Message msg;
        if (reason == true) {
            msg = obtainMessage(MESSAGE_GET_CONFIG, 0,MESSAGE_GET_CONFIG, null);
        } else {
            msg = obtainMessage(MESSAGE_GET_CONFIG, 0,MESSAGE_SET_CONFIG, null);
        }
        if (CallSettings.isMultipleSim())
        {
            ((GeminiPhone)mPhone).getCellBroadcastSmsConfigGemini(msg, mSimId);
        }
        else
        {
            mPhone.getCellBroadcastSmsConfig(msg);
        }
    }
    
    public void setSimId(int id)
    {
        mSimId = id;
    }
}