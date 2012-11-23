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

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ServiceManager;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;

import android.telephony.TelephonyManager;

public class DualSimCallSetting extends TabActivity {

    private TabHost mTabHost;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        final ITelephony iTelephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
//        boolean sim1Insert = iTelephony.isSimInsert(Phone.GEMINI_SIM_1);
//        boolean sim2Insert = iTelephony.isSimInsert(Phone.GEMINI_SIM_2);
        Intent intent = new Intent(this, CallFeaturesSetting.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(Phone.GEMINI_SIM_ID_KEY, Phone.GEMINI_SIM_1);
        mTabHost = this.getTabHost();
        mTabHost.addTab(mTabHost.newTabSpec("SIM_1")
        		.setIndicator(getString(R.string.sim1), 
        				getResources().getDrawable(R.drawable.tab_manage_sim1))
        		.setContent(intent));
        
        Intent intent2 = new Intent(this, CallFeaturesSetting.class);
        intent2.setAction(Intent.ACTION_VIEW);
        intent2.putExtra(Phone.GEMINI_SIM_ID_KEY, Phone.GEMINI_SIM_2);
        mTabHost.addTab(mTabHost.newTabSpec("SIM_2")
        		.setIndicator(getString(R.string.sim2), 
        				getResources().getDrawable(R.drawable.tab_manage_sim2))
        		.setContent(intent2));

        Intent invokeIntent = getIntent();
        int simId = invokeIntent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, Phone.GEMINI_SIM_1);

        if(simId == Phone.GEMINI_SIM_2){
    	    mTabHost.setCurrentTab(Phone.GEMINI_SIM_2);
        }else if(TelephonyManager.getDefault().hasIccCardGemini(Phone.GEMINI_SIM_1)){
    	    mTabHost.setCurrentTab(Phone.GEMINI_SIM_1);
        }else{
    	    mTabHost.setCurrentTab(Phone.GEMINI_SIM_2);
        }

    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
    }

}
