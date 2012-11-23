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

package com.android.phone;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import android.os.RemoteException;
import android.util.Log;
import android.app.ActivityManagerNative;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.android.internal.telephony.Connection;

public class VTCallUtils {
	
	private static final String LOG_TAG = "VTCallUtils";
    private static final boolean DBG = true;// (PhoneApp.DBG_LEVEL >= 2);
    
    /**
     * Video Call will control some resource, such as Camera, Media.
     * So Phone App will broadcast Intent to other APPs before acquire and after release the resource.
     * Intent action:
     * Before - "android.phone.extra.VT_CALL_START"
     * After - "android.phone.extra.VT_CALL_END"
     */
    public static final String VT_CALL_START = "android.phone.extra.VT_CALL_START";
    public static final String VT_CALL_END = "android.phone.extra.VT_CALL_END";
    
    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
	
    /**
     * for InCallScreen to update VT UI
     * In old code, there are more than 2 modes.
     * But change to 2 mode OPEN/CLOSE.
     * In fact, it can be handled by InCallScreen.mVTInCallCanvas's visible/invisible.
     * But we also use VTScreenMode, because if need more than 2 modes in future, we can add it easily. 
     */
	static enum VTScreenMode{
		VT_SCREEN_CLOSE,
		VT_SCREEN_OPEN
	}
	
	static void showVTIncomingCallUi() {
        if (DBG) log("showVTIncomingCallUi()...");
        
        VTSettingUtils.getInstance().updateVTEngineerModeValues();
        
        PhoneApp app = PhoneApp.getInstance();

        try {
            ActivityManagerNative.getDefault().closeSystemDialogs("call");
        } catch (RemoteException e) {
        }

        app.preventScreenOn(true);
        app.requestWakeState(PhoneApp.WakeState.FULL);

        if (DBG) log("- updating notification from showVTIncomingCall()...");
        NotificationMgr.getDefault().updateInCallNotification();
        app.displayVTCallScreen();
    }
	
	public static void checkVTFile()
	{
		if (DBG) log("start checkVTFile() ! ");
		if( !(new File( VTAdvancedSetting.getPicPathDefault() ).exists()) )
    	{
			if (DBG) log("checkVTFile() : the default pic file not exists , create it ! ");
    		
    		try {
    			Bitmap btp1 = BitmapFactory.decodeResource(PhoneApp.getInstance().getResources(), R.drawable.vt_incall_pic_qcif);
    			VTCallUtils.saveMyBitmap( VTAdvancedSetting.getPicPathDefault() , btp1 );
    			btp1.recycle();
    			if (DBG) log(" - Bitmap.isRecycled() : " + btp1.isRecycled() );
			} catch (IOException e) {
				e.printStackTrace();
			}
   		
    	}
    	
    	if( !(new File( VTAdvancedSetting.getPicPathUserselect() ).exists()) )
    	{
    		if (DBG) log("checkVTFile() : the default user select pic file not exists , create it ! ");
    		
    		try {
    			Bitmap btp2 = BitmapFactory.decodeResource(PhoneApp.getInstance().getResources(), R.drawable.vt_incall_pic_qcif);
        		VTCallUtils.saveMyBitmap( VTAdvancedSetting.getPicPathUserselect() , btp2 );
        		btp2.recycle();
        		if (DBG) log(" - Bitmap.isRecycled() : " + btp2.isRecycled() );
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	
    	if( !(new File( VTAdvancedSetting.getPicPathDefault2() ).exists()) )
    	{
			if (DBG) log("checkVTFile() : the default pic2 file not exists , create it ! ");
    		
    		try {
    			Bitmap btp3 = BitmapFactory.decodeResource(PhoneApp.getInstance().getResources(), R.drawable.vt_incall_pic_qcif);
    			VTCallUtils.saveMyBitmap( VTAdvancedSetting.getPicPathDefault2() , btp3 );
    			btp3.recycle();
    			if (DBG) log(" - Bitmap.isRecycled() : " + btp3.isRecycled() );
			} catch (IOException e) {
				e.printStackTrace();
			}
   		
    	}
    	
    	if( !(new File( VTAdvancedSetting.getPicPathUserselect2() ).exists()) )
    	{
			if (DBG) log("checkVTFile() : the default user select pic2 file not exists , create it ! ");
    		
    		try {
    			Bitmap btp4 = BitmapFactory.decodeResource(PhoneApp.getInstance().getResources(), R.drawable.vt_incall_pic_qcif);
    			VTCallUtils.saveMyBitmap( VTAdvancedSetting.getPicPathUserselect2() , btp4 );
    			btp4.recycle();
    			if (DBG) log(" - Bitmap.isRecycled() : " + btp4.isRecycled() );
			} catch (IOException e) {
				e.printStackTrace();
			}
   		
    	}
    	
    	if (DBG) log("end checkVTFile() ! ");
	}
	
	public static void saveMyBitmap(String bitName, Bitmap bitmap) throws IOException {
	
		if (DBG) log("saveMyBitmap()...");
		
        File f = new File( bitName );
        f.createNewFile();
        FileOutputStream fOut = null;
        
        try {
                fOut = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
                e.printStackTrace();
        }
        
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
        try {
                fOut.flush();
        } catch (IOException e) {
                e.printStackTrace();
        }
        
        try {
                fOut.close();
        } catch (IOException e) {
                e.printStackTrace();
        }
	}
	
	/**
	 * VT needs special timing method for different number:
	 * In MT call, the timing method is the same as voice call:
	 * starting timing when the call state turn to "ACTIVE".
	 * In MO call, because of the multimedia ringtone, the timing method is different 
	 * which we starting timing when receive the Message - VTManager.VT_MSG_START_COUNTER.
	 * Because when the multimedia ringtone is playing, the call state is already "ACTIVE",
	 * but then we are connecting with the multimedia ringtone server but not the number we dialing.
	 * So we must wait to start timing when connected with the number we dialing.
	 * The Message VTManager.VT_MSG_START_COUNTER is to tell us that we have connected with the number we dialing.
	 * But it is not to follow this method for all numbers in MO call.
	 * Some numbers don't need timing - vtNumbers_none [].
	 * Some numbers need timing with the voice call method - vtNumbers_default [].
	 * You can UPDATE the numbers in them here.
	 * 
	 */
	
	final static String vtNumbers_none [] = {"12531","+8612531"};
	final static String vtNumbers_default [] = {"12535","13800100011","+8612535","+8613800100011"};
	
	static enum VTTimingMode {
		VT_TIMING_NONE, VT_TIMING_SPECIAL, VT_TIMING_DEFAULT
	}

	public static VTTimingMode checkVTTimingMode(String number) {
		if (DBG)
			log("checkVTTimingMode - number:" + number);

		ArrayList<String> mArrayList_none = new ArrayList<String>(Arrays
				.asList(vtNumbers_none));
		ArrayList<String> mArrayList_default = new ArrayList<String>(Arrays
				.asList(vtNumbers_default));

		if (mArrayList_none.indexOf(number) >= 0) {
			if (DBG)
				log("checkVTTimingMode - return:" + VTTimingMode.VT_TIMING_NONE);
			return VTTimingMode.VT_TIMING_NONE;
		}

		if (mArrayList_default.indexOf(number) >= 0) {
			if (DBG)
				log("checkVTTimingMode - return:"
						+ VTTimingMode.VT_TIMING_DEFAULT);
			return VTTimingMode.VT_TIMING_DEFAULT;
		}

		if (DBG)
			log("checkVTTimingMode - return:" + VTTimingMode.VT_TIMING_SPECIAL);
		return VTTimingMode.VT_TIMING_SPECIAL;
	}

}