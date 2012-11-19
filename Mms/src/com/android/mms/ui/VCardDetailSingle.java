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
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.mms.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.pim.vcard.VCardConfig;
import android.pim.vcard.VCardEntry;
import android.pim.vcard.VCardEntry.PhoneData;
import android.pim.vcard.VCardEntry.EmailData;
import android.pim.vcard.VCardEntry.ImData;
import android.pim.vcard.VCardEntry.PostalData;
import android.pim.vcard.VCardEntry.OrganizationData;
import android.pim.vcard.VCardEntryConstructor;
import android.pim.vcard.exception.VCardNestedException;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;

import com.android.mms.R;
import com.android.mms.data.VCardEntryList;

public class VCardDetailSingle extends Activity {
    public static final String TAG   = "Mms/VCardDetail";
    
    private ImageView mContactPhoto;
    private TextView mContactName;
    private TextView mNoDetailsTextView;
    private ViewGroup mScrollView;
    private View mBottonPanel;
    private Button mSaveButton;
    private Button mCancelButton;
    private ProgressDialog mProgressDialog;
    private VCardEntry mVCard;
    private LayoutInflater mInflater;
    
    private Uri mUri;
    private int mItemId;
    private int mVCardType;
    private String mVCardChSet;
    private Handler mHandler;
    
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.vcard_detail_single);
        
        initRes();
        mInflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        mHandler = new Handler();
        mUri = getIntent().getData();
        mItemId = getIntent().getIntExtra("item_id", 0);
        boolean need_parse = getIntent().getBooleanExtra("need_parse", false);
        if (need_parse) {
            mVCardType = getIntent().getIntExtra("type", VCardConfig.VCARD_TYPE_UNKNOWN);
            mVCardChSet = getIntent().getStringExtra("charset");
            showProgressDialog();
            new Thread() {
                public void run() {
                    doActuallyReadOneVCard();
                    dismissProgressDialog();
                    fillContent();
                }
            }.start();
        } else {
            mBottonPanel.setVisibility(View.GONE);
            mVCard = VCardDetailMulti.getVCardAt(mItemId);
            if (mVCard != null) {
                fillContent();
            }
        }
    }
    
    private void initProgressDialog() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(getString(R.string.please_wait));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    }
    
    private void initRes() {
        mContactPhoto = (ImageView) findViewById(R.id.contact_photo);
        mContactName = (TextView) findViewById(R.id.contact_name);
        mNoDetailsTextView = (TextView) findViewById(R.id.vcard_no_details);
        mScrollView = (ViewGroup) findViewById(R.id.content);
        mBottonPanel = (View) findViewById(R.id.bottom_panel);
        mSaveButton = (Button) findViewById(R.id.save);
        mSaveButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mVCard != null) {
                    showProgressDialog();
                    new Thread() {
                        @Override
                        public void run() {
                            mVCard.pushIntoContentResolver(getContentResolver());
                            dismissProgressDialog();
                            mHandler.post(toastAndFinish);
                        }

                    }.start();
                } else 
                    finish();
            }
            
        });
        mCancelButton = (Button) findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(new OnClickListener () {
            public void onClick(View v) {
                finish();
            }
        });
    }
    
    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = null;
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            initProgressDialog();
        }
        mProgressDialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mProgressDialog != null && !mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissProgressDialog();
    }
    
    private void doActuallyReadOneVCard() {
        if (mUri == null) {
            return;
        }
        if (mVCardType == VCardConfig.VCARD_TYPE_UNKNOWN) {
            mVCardType = VCardConfig.getVCardTypeFromString(
            "import");
        }
        VCardEntryConstructor builder = new VCardEntryConstructor(mVCardType, null, mVCardChSet);
        final VCardEntryList entryList = new VCardEntryList();
        builder.addEntryHandler(entryList);
        
        try {
            if (!MessageUtils.readOneVCardFile(mUri, mVCardType, builder, false, getContentResolver())) {
                return;
            }
        } catch (VCardNestedException e) {
            Log.e(TAG, "Never reach here.");
        }
        mVCard = entryList.getCreatedUris().get(mItemId);
        return;
    }
    
    private Runnable toastAndFinish = new Runnable() {
        public void run() {
            Toast.makeText(VCardDetailSingle.this, getString(R.string.save_message_to_sim_successful),
                Toast.LENGTH_SHORT).show();
            finish();
        }
      
    };
    
    private void fillContent() {
        new Thread () {
            public void run() {
                boolean noContactDetails = true;
                if (mVCard.getPhotoList() != null) {
                    final Bitmap photo = BitmapFactory.decodeByteArray(mVCard.getPhotoList().get(0).photoBytes,
                        0, mVCard.getPhotoList().get(0).photoBytes.length);
                    if (photo != null) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mContactPhoto.setImageBitmap(photo);
                                mContactName.setText(mVCard.getDisplayName());
                            }   
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mContactPhoto.setImageBitmap(BitmapFactory.decodeResource(
                                    getResources(), R.drawable.ic_contact_picture));
                                mContactName.setText(mVCard.getDisplayName());
                            }   
                        });
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mContactPhoto.setImageBitmap(BitmapFactory.decodeResource(
                                getResources(), R.drawable.ic_contact_picture));
                            mContactName.setText(mVCard.getDisplayName());
                        }
                    });
                }   
                
                if (mVCard.getPhoneList() != null) {
                    noContactDetails = false;
                    for (PhoneData phoneData : mVCard.getPhoneList()) {
                        final View view = mInflater.inflate(R.layout.vcard_item_term, null);
                        TextView label = (TextView) view.findViewById(R.id.vcard_item_term_label);
                        TextView detail = (TextView) view.findViewById(R.id.vcard_item_term_detail);
                        ImageView icon = (ImageView) view.findViewById(R.id.vcard_item_term_icon);
                        icon.setImageResource(R.drawable.sym_call);
                        label.setText(getPhoneTypeLabelResource(phoneData.type));
                        detail.setText((phoneData.data));
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mScrollView.addView(view);
                            } 
                        });
                    }
                }
                
                if (mVCard.getEmailList() != null) {
                    noContactDetails = false;
                    for (EmailData emailData : mVCard.getEmailList()) {
                        final View view = mInflater.inflate(R.layout.vcard_item_term, null);
                        TextView label = (TextView) view.findViewById(R.id.vcard_item_term_label);
                        TextView detail = (TextView) view.findViewById(R.id.vcard_item_term_detail);
                        ImageView icon = (ImageView) view.findViewById(R.id.vcard_item_term_icon);
                        icon.setImageResource(R.drawable.sym_email);
                        label.setText(getEmailTypeLabelResource(emailData.type));
                        detail.setText(emailData.data);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mScrollView.addView(view);
                            } 
                        });
                    }
                }
                
                if (mVCard.getWebsiteList() != null) {
                    noContactDetails = false;
                    for (String webString : mVCard.getWebsiteList()) {
                        final View view = mInflater.inflate(R.layout.vcard_item_term, null);
                        TextView label = (TextView) view.findViewById(R.id.vcard_item_term_label);
                        TextView detail = (TextView) view.findViewById(R.id.vcard_item_term_detail);
                        ImageView icon = (ImageView) view.findViewById(R.id.vcard_item_term_icon);
                        icon.setImageResource(R.drawable.sym_email);
                        label.setText(R.string.website);
                        detail.setText(webString);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mScrollView.addView(view);
                            } 
                        });
                    }
                }

                if (mVCard.getImList() != null) {
                    noContactDetails = false;
                    for (ImData im : mVCard.getImList()) {
                        final View view = mInflater.inflate(R.layout.vcard_item_term, null);
                        TextView label = (TextView) view.findViewById(R.id.vcard_item_term_label);
                        TextView detail = (TextView) view.findViewById(R.id.vcard_item_term_detail);
                        ImageView icon = (ImageView) view.findViewById(R.id.vcard_item_term_icon);
                        icon.setImageResource(R.drawable.sym_chat);
                        label.setText(getImTypeLabelResource(im.protocol));
                        detail.setText(im.data);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mScrollView.addView(view);
                            } 
                        });
                    }
                }

                if (mVCard.getPostalList() != null) {
                    noContactDetails = false;
                    for (PostalData postal : mVCard.getPostalList()) {
                        final View view = mInflater.inflate(R.layout.vcard_item_term, null);
                        TextView label = (TextView) view.findViewById(R.id.vcard_item_term_label);
                        TextView detail = (TextView) view.findViewById(R.id.vcard_item_term_detail);
                        ImageView icon = (ImageView) view.findViewById(R.id.vcard_item_term_icon);
                        icon.setImageResource(R.drawable.sym_map);
                        label.setText(getPostalTypeLabelResource(postal.type));
                        detail.setText(postal.getFormattedAddress(mVCardType));
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mScrollView.addView(view);
                            } 
                        });
                    }
                }

                if (mVCard.getOrganizationList() != null) {
                    noContactDetails = false;
                    for (OrganizationData organization : mVCard.getOrganizationList()) {
                        final View view = mInflater.inflate(R.layout.vcard_item_term, null);
                        TextView label = (TextView) view.findViewById(R.id.vcard_item_term_label);
                        TextView detail = (TextView) view.findViewById(R.id.vcard_item_term_detail);
                        ImageView icon = (ImageView) view.findViewById(R.id.vcard_item_term_icon);
                        icon.setImageResource(R.drawable.sym_organization);
                        label.setText(R.string.organization);
                        detail.setText(organization.getFormattedString());
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mScrollView.addView(view);
                            } 
                        });
                    }
                }

                if (mVCard.getNotes() != null) {
                    noContactDetails = false;
                    for (String note : mVCard.getNotes()) {
                        final View view = mInflater.inflate(R.layout.vcard_item_term, null);
                        TextView label = (TextView) view.findViewById(R.id.vcard_item_term_label);
                        TextView detail = (TextView) view.findViewById(R.id.vcard_item_term_detail);
                        ImageView icon = (ImageView) view.findViewById(R.id.vcard_item_term_icon);
                        icon.setImageResource(R.drawable.sym_note);
                        label.setText(R.string.note);
                        detail.setText(note);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mScrollView.addView(view);
                            } 
                        });
                    }
                }

                if (mVCard.getNickNameList() != null) {
                    noContactDetails = false;
                    for (String nickName : mVCard.getNickNameList()) {
                        final View view = mInflater.inflate(R.layout.vcard_item_term, null);
                        TextView label = (TextView) view.findViewById(R.id.vcard_item_term_label);
                        TextView detail = (TextView) view.findViewById(R.id.vcard_item_term_detail);
                        ImageView icon = (ImageView) view.findViewById(R.id.vcard_item_term_icon);
                        icon.setImageResource(R.drawable.sym_contact);
                        label.setText(R.string.nickname);
                        detail.setText(nickName);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mScrollView.addView(view);
                            } 
                        });
                    }
                }

                if (!TextUtils.isEmpty(mVCard.getBirthday())) {
                    noContactDetails = false;
                    final View view = mInflater.inflate(R.layout.vcard_item_term, null);
                    TextView label = (TextView) view.findViewById(R.id.vcard_item_term_label);
                    TextView detail = (TextView) view.findViewById(R.id.vcard_item_term_detail);
                    ImageView icon = (ImageView) view.findViewById(R.id.vcard_item_term_icon);
                    icon.setImageResource(R.drawable.ic_menu_call);
                    label.setText("Birthday:");
                    detail.setText(mVCard.getBirthday());
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mScrollView.addView(view);
                        } 
                    });
                }

                if (noContactDetails) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mNoDetailsTextView.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        }.start();
    }

    private int getPhoneTypeLabelResource(Integer type) {
        if (type == null) return R.string.call_other;
        switch (type) {
            case Phone.TYPE_HOME: return R.string.call_home;
            case Phone.TYPE_MOBILE: return R.string.call_mobile;
            case Phone.TYPE_WORK: return R.string.call_work;
            case Phone.TYPE_FAX_WORK: return R.string.call_fax_work;
            case Phone.TYPE_FAX_HOME: return R.string.call_fax_home;
            case Phone.TYPE_PAGER: return R.string.call_pager;
            case Phone.TYPE_OTHER: return R.string.call_other;
            case Phone.TYPE_CALLBACK: return R.string.call_callback;
            case Phone.TYPE_CAR: return R.string.call_car;
            case Phone.TYPE_COMPANY_MAIN: return R.string.call_company_main;
            case Phone.TYPE_ISDN: return R.string.call_isdn;
            case Phone.TYPE_MAIN: return R.string.call_main;
            case Phone.TYPE_OTHER_FAX: return R.string.call_other_fax;
            case Phone.TYPE_RADIO: return R.string.call_radio;
            case Phone.TYPE_TELEX: return R.string.call_telex;
            case Phone.TYPE_TTY_TDD: return R.string.call_tty_tdd;
            case Phone.TYPE_WORK_MOBILE: return R.string.call_work_mobile;
            case Phone.TYPE_WORK_PAGER: return R.string.call_work_pager;
            case Phone.TYPE_ASSISTANT: return R.string.call_assistant;
            case Phone.TYPE_MMS: return R.string.call_mms;
            default: return R.string.call_custom;
        }
    }

    private int getEmailTypeLabelResource(Integer type) {
        if (type == null) return R.string.email;
        switch (type) {
            case Email.TYPE_HOME: return R.string.email_home;
            case Email.TYPE_WORK: return R.string.email_work;
            case Email.TYPE_OTHER: return R.string.email_other;
            case Email.TYPE_MOBILE: return R.string.email_mobile;
            default: return R.string.email_custom;
        }
    }

    private int getPostalTypeLabelResource(Integer type) {
        if (type == null) return R.string.map_other;
        switch (type) {
            case StructuredPostal.TYPE_HOME: return R.string.map_home;
            case StructuredPostal.TYPE_WORK: return R.string.map_work;
            case StructuredPostal.TYPE_OTHER: return R.string.map_other;
            default: return R.string.map_custom;
        }
    }

    private int getImTypeLabelResource(Integer type) {
        if (type == null) return R.string.chat;
        switch (type) {
            case Im.PROTOCOL_AIM: return R.string.chat_aim;
            case Im.PROTOCOL_MSN: return R.string.chat_msn;
            case Im.PROTOCOL_YAHOO: return R.string.chat_yahoo;
            case Im.PROTOCOL_SKYPE: return R.string.chat_skype;
            case Im.PROTOCOL_QQ: return R.string.chat_qq;
            case Im.PROTOCOL_GOOGLE_TALK: return R.string.chat_gtalk;
            case Im.PROTOCOL_ICQ: return R.string.chat_icq;
            case Im.PROTOCOL_JABBER: return R.string.chat_jabber;
            case Im.PROTOCOL_NETMEETING: return R.string.chat;
            case Im.PROTOCOL_OTHER: return R.string.chat;
            default: return R.string.chat;
        }
    }
}
