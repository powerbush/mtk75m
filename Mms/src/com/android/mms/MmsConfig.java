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

package com.android.mms;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Config;
import android.util.Log;
import android.os.SystemProperties;


public class MmsConfig {
    private static final String TAG = "MmsConfig";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;

    private static final String DEFAULT_HTTP_KEY_X_WAP_PROFILE = "x-wap-profile";
    private static final String DEFAULT_USER_AGENT = "Android-Mms/2.0";

    private static final int MAX_IMAGE_HEIGHT = 480;
    private static final int MAX_IMAGE_WIDTH  = 640;
    private static final int RECIPIENTS_LIMIT = 50;
    private static final int MAX_TEXT_LENGTH = 2000;
    //MTK_OP01_PROTECT_START
    private static final int MAX_CMCC_TEXT_LENGTH = 3100;
    //MTK_OP01_PROTECT_END

    /**
     * Whether to hide MMS functionality from the user (i.e. SMS only).
     */
    private static boolean mTransIdEnabled = false;
    private static int mMmsEnabled = 1;                         // default to true
    private static int mMaxMessageSize = 300 * 1024;            // default to 300k max size
    private static int mUserSetMmsSizeLimit = 300;              // Mms size limit, default 300K.
    private static int mReceiveMmsSizeLimitFor2G = 200;         // Receive Mms size limit for 2G network
    private static int mReceiveMmsSizeLimitForTD = 400;         // Receive Mms size limit for TD network
    private static String mUserAgent = DEFAULT_USER_AGENT;
    private static String mUaProfTagName = DEFAULT_HTTP_KEY_X_WAP_PROFILE;
    private static String mUaProfUrl = null;
    private static String mHttpParams = null;
    private static String mHttpParamsLine1Key = null;
    private static String mEmailGateway = null;
    private static int mMaxImageHeight = MAX_IMAGE_HEIGHT;      // default value
    private static int mMaxImageWidth = MAX_IMAGE_WIDTH;        // default value
    private static int mMaxRestrictedImageHeight = 1200;        // default value
    private static int mMaxRestrictedImageWidth = 1600;         // default value
    private static int mMmsRecipientLimit = 20;                 // default value
    private static int mSmsRecipientLimit = 100;                // default value
    private static int mDefaultSMSMessagesPerThread = 200;      // default value
    private static int mDefaultMMSMessagesPerThread = 20;       // default value
    private static int mMinMessageCountPerThread = 2;           // default value
    private static int mMaxMessageCountPerThread = 5000;        // default value
    private static int mHttpSocketTimeout = 60*1000;            // default to 1 min
    private static int mMinimumSlideElementDuration = 7;        // default to 7 sec
    private static boolean mNotifyWapMMSC = false;
    private static boolean mAllowAttachAudio = true;

    private static int mSmsToMmsTextThreshold = 4;
    //MTK_OP01_PROTECT_START
    private static int mOP01SmsToMmsThreshold = 11;
    //MTK_OP01_PROTECT_END
    //MTK_OP02_PROTECT_START
    private static int mOP02SmsToMmsThreshold = 10;
    //MTK_OP02_PROTECT_END

    private static boolean mEnableMultipartSMS = true;
    private static boolean mEnableSlideDuration = true;
    private static int mMaxTextLength = -1;
    private static boolean mDeviceStorageFull = false;

    // This is the max amount of storage multiplied by mMaxMessageSize that we
    // allow of unsent messages before blocking the user from sending any more
    // MMS's.
    private static int mMaxSizeScaleForPendingMmsAllowed = 4;       // default value

    // Email gateway alias support, including the master switch and different rules
    private static boolean mAliasEnabled = false;
    private static int mAliasRuleMinChars = 2;
    private static int mAliasRuleMaxChars = 48;

    public static void init(Context context) {
        if (LOCAL_LOGV) {
            Log.v(TAG, "MmsConfig.init()");
        }

        loadMmsSettings(context);
    }

    public static int getSmsToMmsTextThreshold() {
        String optr = SystemProperties.get("ro.operator.optr");
        //MTK_OP01_PROTECT_START
        if ("OP01".equals(optr)) {
            return mOP01SmsToMmsThreshold;
        }
        //MTK_OP01_PROTECT_END
        
        //MTK_OP02_PROTECT_START
        if ("OP02".equals(optr)) {
            return mOP02SmsToMmsThreshold;
        }
        //MTK_OP02_PROTECT_END
        return mSmsToMmsTextThreshold;
    }

    public static boolean getMmsEnabled() {
        return mMmsEnabled == 1 ? true : false;
    }

    public static int getUserSetMmsSizeLimit(boolean isBytes) {
        if (true == isBytes) {
            return mUserSetMmsSizeLimit*1024;
        } else {
        	return mUserSetMmsSizeLimit;
        }
    }
    
    public static void setUserSetMmsSizeLimit(int limit) {
    	mUserSetMmsSizeLimit = limit;
    }
    public static int getMaxMessageSize() {
        return mMaxMessageSize;
    }

    /**
     * This function returns the value of "enabledTransID" present in mms_config file.
     * In case of single segment wap push message, this "enabledTransID" indicates whether
     * TransactionID should be appended to URI or not.
     */
    public static boolean getTransIdEnabled() {
        return mTransIdEnabled;
    }

    public static String getUserAgent() {
        return mUserAgent;
    }

    public static String getUaProfTagName() {
        return mUaProfTagName;
    }

    public static String getUaProfUrl() {
        return mUaProfUrl;
    }

    public static String getHttpParams() {
        return mHttpParams;
    }

    public static String getHttpParamsLine1Key() {
        return mHttpParamsLine1Key;
    }

    public static String getEmailGateway() {
        return mEmailGateway;
    }

    public static int getMaxImageHeight() {
        return mMaxImageHeight;
    }

    public static int getMaxImageWidth() {
        return mMaxImageWidth;
    }

    public static int getMaxRestrictedImageHeight() {
        return mMaxRestrictedImageHeight;
    }

    public static int getMaxRestrictedImageWidth() {
        return mMaxRestrictedImageWidth;
    }

    public static int getMmsRecipientLimit() {
        return mMmsRecipientLimit;
    }

    public static int getSmsRecipientLimit() {
        return mSmsRecipientLimit;
    }

    public static int getMaxTextLimit() {
        String optr = SystemProperties.get("ro.operator.optr");
        //MTK_OP01_PROTECT_START
        if ("OP01".equals(optr)) {
            return MAX_CMCC_TEXT_LENGTH;
        }
        //MTK_OP01_PROTECT_END
        return mMaxTextLength > -1 ? mMaxTextLength : MAX_TEXT_LENGTH;
    }

    public static int getDefaultSMSMessagesPerThread() {
        return mDefaultSMSMessagesPerThread;
    }

    public static int getDefaultMMSMessagesPerThread() {
        return mDefaultMMSMessagesPerThread;
    }

    public static int getMinMessageCountPerThread() {
        return mMinMessageCountPerThread;
    }

    public static int getMaxMessageCountPerThread() {
        return mMaxMessageCountPerThread;
    }

    public static int getHttpSocketTimeout() {
        return mHttpSocketTimeout;
    }

    public static int getMinimumSlideElementDuration() {
        return mMinimumSlideElementDuration;
    }

    public static boolean getMultipartSmsEnabled() {
        return mEnableMultipartSMS;
    }

    public static boolean getSlideDurationEnabled() {
        return mEnableSlideDuration;
    }

    public static boolean getNotifyWapMMSC() {
        return mNotifyWapMMSC;
    }

    public static int getMaxSizeScaleForPendingMmsAllowed() {
        return mMaxSizeScaleForPendingMmsAllowed;
    }

    public static boolean isAliasEnabled() {
        return mAliasEnabled;
    }

    public static int getAliasMinChars() {
        return mAliasRuleMinChars;
    }

    public static int getAliasMaxChars() {
        return mAliasRuleMaxChars;
    }

    public static boolean getAllowAttachAudio() {
        return mAllowAttachAudio;
    }

    public static int getReceiveMmsLimitFor2G() {
        return mReceiveMmsSizeLimitFor2G;
    }

    public static int getReceiveMmsLimitForTD() {
        return mReceiveMmsSizeLimitForTD;
    }

    public static final void beginDocument(XmlPullParser parser, String firstElementName) throws XmlPullParserException, IOException
    {
        int type;
        while ((type=parser.next()) != parser.START_TAG
                   && type != parser.END_DOCUMENT) {
            ;
        }

        if (type != parser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        if (!parser.getName().equals(firstElementName)) {
            throw new XmlPullParserException("Unexpected start tag: found " + parser.getName() +
                    ", expected " + firstElementName);
        }
    }

    public static final void nextElement(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        int type;
        while ((type=parser.next()) != parser.START_TAG
                   && type != parser.END_DOCUMENT) {
            ;
        }
    }
    
    private static void loadMmsSettings(Context context) {
        XmlResourceParser parser = context.getResources().getXml(R.xml.mms_config);

        try {
            beginDocument(parser, "mms_config");

            while (true) {
                nextElement(parser);
                String tag = parser.getName();
                if (tag == null) {
                    break;
                }
                String name = parser.getAttributeName(0);
                String value = parser.getAttributeValue(0);
                String text = null;
                if (parser.next() == XmlPullParser.TEXT) {
                    text = parser.getText();
                }

                if (DEBUG) {
                    Log.v(TAG, "tag: " + tag + " value: " + value);
                }
                if ("name".equalsIgnoreCase(name)) {
                    if ("bool".equals(tag)) {
                        // bool config tags go here
                        if ("enabledMMS".equalsIgnoreCase(value)) {
                            mMmsEnabled = "true".equalsIgnoreCase(text) ? 1 : 0;
                        } else if ("enabledTransID".equalsIgnoreCase(value)) {
                            mTransIdEnabled = "true".equalsIgnoreCase(text);
                        } else if ("enabledNotifyWapMMSC".equalsIgnoreCase(value)) {
                            mNotifyWapMMSC = "true".equalsIgnoreCase(text);
                        } else if ("aliasEnabled".equalsIgnoreCase(value)) {
                            mAliasEnabled = "true".equalsIgnoreCase(text);
                        } else if ("allowAttachAudio".equalsIgnoreCase(value)) {
                            mAllowAttachAudio = "true".equalsIgnoreCase(text);
                        } else if ("enableMultipartSMS".equalsIgnoreCase(value)) {
                            mEnableMultipartSMS = "true".equalsIgnoreCase(text);
                        } else if ("enableSlideDuration".equalsIgnoreCase(value)) {
                            mEnableSlideDuration = "true".equalsIgnoreCase(text);
                        }
                    } else if ("int".equals(tag)) {
                        // int config tags go here
                        if ("maxMessageSize".equalsIgnoreCase(value)) {
                            mMaxMessageSize = Integer.parseInt(text);
                        } else if ("maxImageHeight".equalsIgnoreCase(value)) {
                            mMaxImageHeight = Integer.parseInt(text);
                        } else if ("maxImageWidth".equalsIgnoreCase(value)) {
                            mMaxImageWidth = Integer.parseInt(text);
                        } else if ("maxRestrictedImageHeight".equalsIgnoreCase(value)) {
                        	mMaxRestrictedImageHeight = Integer.parseInt(text);
                        }else if ("maxRestrictedImageWidth".equalsIgnoreCase(value)) {
                        	mMaxRestrictedImageWidth = Integer.parseInt(text);
                        } else if ("defaultSMSMessagesPerThread".equalsIgnoreCase(value)) {
                            mDefaultSMSMessagesPerThread = Integer.parseInt(text);
                        } else if ("defaultMMSMessagesPerThread".equalsIgnoreCase(value)) {
                            mDefaultMMSMessagesPerThread = Integer.parseInt(text);
                        } else if ("minMessageCountPerThread".equalsIgnoreCase(value)) {
                            mMinMessageCountPerThread = Integer.parseInt(text);
                        } else if ("maxMessageCountPerThread".equalsIgnoreCase(value)) {
                            mMaxMessageCountPerThread = Integer.parseInt(text);
                        } else if ("smsToMmsTextThreshold".equalsIgnoreCase(value)) {
                            mSmsToMmsTextThreshold = Integer.parseInt(text);
                        } else if ("recipientLimit".equalsIgnoreCase(value)) {
                            String optr = SystemProperties.get("ro.operator.optr");
                            //MTK_OP01_PROTECT_START
                            if ("OP01".equals(optr)) {
                                mMmsRecipientLimit = RECIPIENTS_LIMIT;
                            } else {
                            //MTK_OP01_PROTECT_END
                                mMmsRecipientLimit = Integer.parseInt(text);
                                if (mMmsRecipientLimit <= 0) {
                                    mMmsRecipientLimit = RECIPIENTS_LIMIT;
                                }
                            //MTK_OP01_PROTECT_START
                            }
                            //MTK_OP01_PROTECT_END
                        } else if ("httpSocketTimeout".equalsIgnoreCase(value)) {
                            mHttpSocketTimeout = Integer.parseInt(text);
                        } else if ("minimumSlideElementDuration".equalsIgnoreCase(value)) {
                            mMinimumSlideElementDuration = Integer.parseInt(text);
                        } else if ("maxSizeScaleForPendingMmsAllowed".equalsIgnoreCase(value)) {
                            mMaxSizeScaleForPendingMmsAllowed = Integer.parseInt(text);
                        } else if ("aliasMinChars".equalsIgnoreCase(value)) {
                            mAliasRuleMinChars = Integer.parseInt(text);
                        } else if ("aliasMaxChars".equalsIgnoreCase(value)) {
                            mAliasRuleMaxChars = Integer.parseInt(text);
                        } else if ("smsToMmsTextThreshold".equalsIgnoreCase(value)) {
                            mSmsToMmsTextThreshold = Integer.parseInt(text);
                        } else if ("maxMessageTextSize".equalsIgnoreCase(value)) {
                            mMaxTextLength = Integer.parseInt(text);
                        }
                    } else if ("string".equals(tag)) {
                        // string config tags go here
                        if ("userAgent".equalsIgnoreCase(value)) {
                            mUserAgent = text;
                        } else if ("uaProfTagName".equalsIgnoreCase(value)) {
                            mUaProfTagName = text;
                        } else if ("uaProfUrl".equalsIgnoreCase(value)) {
                            mUaProfUrl = text;
                        } else if ("httpParams".equalsIgnoreCase(value)) {
                            mHttpParams = text;
                        } else if ("httpParamsLine1Key".equalsIgnoreCase(value)) {
                            mHttpParamsLine1Key = text;
                        } else if ("emailGatewayNumber".equalsIgnoreCase(value)) {
                            mEmailGateway = text;
                        }
                    }
                }
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "loadMmsSettings caught ", e);
        } catch (NumberFormatException e) {
            Log.e(TAG, "loadMmsSettings caught ", e);
        } catch (IOException e) {
            Log.e(TAG, "loadMmsSettings caught ", e);
        } finally {
            parser.close();
        }

        String errorStr = null;

        if (getMmsEnabled() && mUaProfUrl == null) {
            errorStr = "uaProfUrl";
        }

        if (errorStr != null) {
            String err =
                String.format("MmsConfig.loadMmsSettings mms_config.xml missing %s setting",
                        errorStr);
            Log.e(TAG, err);
            throw new ContentRestrictionException(err);
        }
    }
    
    /*
     * Notes:for CMCC customization,whether to enable SL automatically lanuch.
     * default set false
     */
    private static boolean mSlAutoLanuchEnabled = false;
    public static boolean getSlAutoLanuchEnabled(){
        return mSlAutoLanuchEnabled;
    }

    public static void setDeviceStorageFullStatus(boolean bFull) {
        mDeviceStorageFull = bFull;
    }

    public static boolean getDeviceStorageFullStatus() {
        return mDeviceStorageFull;
    }

}
