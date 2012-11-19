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

package com.android.mms.ui;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Browser;
import android.telephony.PhoneNumberUtils;
import android.text.Html;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.style.BackgroundImageSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineHeightSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.URLSpan;
import android.text.style.AlignmentSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.QuickContactBadge;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Telephony.SIMInfo;

import com.android.mms.MmsApp;
import com.android.mms.R;
import com.android.mms.data.WorkingMessage;
import com.android.mms.transaction.Transaction;
import com.android.mms.transaction.TransactionBundle;
import com.android.mms.transaction.TransactionService;
import com.android.mms.util.DownloadManager;
import com.android.mms.util.SmileyParser;
import com.android.internal.telephony.Phone;
import android.text.style.ClickableSpan;
import com.mediatek.featureoption.FeatureOption;

import com.mediatek.xlog.Xlog;

public class WPMessageListItem extends RelativeLayout {
    public static final String EXTRA_URLS = "com.android.mms.ExtraUrls";

    private static final StyleSpan STYLE_BOLD = new StyleSpan(Typeface.BOLD);

    private View mMsgListItem;
    private View mItemContainer;
    private ImageView mLockedIndicator;
    private ImageView mExpirationIndicator;
    private ImageView mDetailsIndicator;
    private TextView mBodyTextView;
    private TextView mTimestamp;
    private TextView mSimStatus;
    private QuickContactBadge mAvatar;
    private Handler mHandler;
    private WPMessageItem mMessageItem;
    //add for multi-delete
    private CheckBox mSelectedBox;
    
    private int statusIconIndent = 0;
    private static final int DEFAULT_ICON_INDENT = 5;
    
    private static final String WP_TAG = "Mms/WapPush";

    public WPMessageListItem(Context context) {    	
        super(context);
    }

    public WPMessageListItem(Context context, AttributeSet attrs) {
        super(context, attrs);

        int color = mContext.getResources().getColor(R.color.timestamp_color);
        mColorSpan = new ForegroundColorSpan(color);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mMsgListItem = findViewById(R.id.wpms_list_item);
        mItemContainer =  findViewById(R.id.wpms_layout_view_parent);
        mBodyTextView = (TextView) findViewById(R.id.wpms_text_view);
        mTimestamp = (TextView) findViewById(R.id.wpms_timestamp);
        mSimStatus = (TextView) findViewById(R.id.sim_status);
        mLockedIndicator = (ImageView) findViewById(R.id.wpms_locked_indicator);
        mExpirationIndicator = (ImageView) findViewById(R.id.wpms_expiration_indicator);
        mDetailsIndicator = (ImageView) findViewById(R.id.wpms_details_indicator);
        //add for multi-delete
        mSelectedBox = (CheckBox)findViewById(R.id.select_check_box);

    }

    public void bind(WPMessageItem msgItem, boolean isDeleteMode) {
        mMessageItem = msgItem;

       //add for multi-delete
        if (isDeleteMode) {
        	mSelectedBox.setVisibility(View.VISIBLE);
        	mSelectedBox.setChecked(msgItem.isSelected());
        } else {
        	mSelectedBox.setVisibility(View.GONE);
        	
        }
        setLongClickable(false);

        bindCommonMessage(msgItem);
        
        mItemContainer.setOnClickListener(new OnClickListener(){
            public void onClick(View v) {            	
               onMessageListItemClick();
	        }
        });
        
    }

    public WPMessageItem getMessageItem() {
        return mMessageItem;
    }
    
    public View getItemContainer(){
    	return mItemContainer;
    }

    public void setMsgListItemHandler(Handler handler) {
        mHandler = handler;
    }

    private void bindCommonMessage(final WPMessageItem msgItem) {
        // Since the message text should be concatenated with the sender's
        // address(or name), I have to display it here instead of
        // displaying it by the Presenter.
        mBodyTextView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());

        String addr = msgItem.mAddress;


        // Get and/or lazily set the formatted message from/on the
        // MessageItem.  Because the MessageItem instances come from a
        // cache (currently of size ~50), the hit rate on avoiding the
        // expensive formatMessage() call is very high.
//        CharSequence formattedMessage = formatMessage(msgItem, msgItem.mContact, msgItem.mText, msgItem.mURL,
//        		msgItem.mCreate, msgItem.mExpiration, msgItem.mTimestamp, msgItem.mHighlight);
        
        CharSequence formattedMessage = formatMessage(msgItem, msgItem.mContact, msgItem.mText, msgItem.mURL, msgItem.mTimestamp, msgItem.mExpiration, msgItem.mHighlight);

        mBodyTextView.setText(formattedMessage);
        
        CharSequence timestamp = formatTimestamp(msgItem,msgItem.mTimestamp);
		CharSequence simStatus = formatSimStatus(msgItem);
         
        mTimestamp.setText(timestamp);
        mSimStatus.setText(simStatus);
        drawRightStatusIndicator(msgItem);
        
        //set padding to create space to draw statusIcon
        mTimestamp.setPadding(statusIconIndent, 0, 0, 0);

        requestLayout();
    }
    


    private LineHeightSpan mSpan = new LineHeightSpan() {
        public void chooseHeight(CharSequence text, int start,
                int end, int spanstartv, int v, FontMetricsInt fm) {
            fm.ascent -= 10;
        }
    };

    TextAppearanceSpan mTextSmallSpan =
        new TextAppearanceSpan(mContext, android.R.style.TextAppearance_Small);

    ForegroundColorSpan mColorSpan = null;  // set in ctor

    
    private ClickableSpan mLinkSpan = new ClickableSpan(){
    	public void onClick(View widget){
    	}
    };
    
//    private CharSequence formatMessage(WPMessageItem msgItem, String contact, String text, String URL, String create,
//                                       String expiration, String timestamp, Pattern highlight) {
    private CharSequence formatMessage(WPMessageItem msgItem, String contact, String mText, String mURL,
    		                             String timestamp, String expiration, Pattern highlight) {
        SpannableStringBuilder buf = new SpannableStringBuilder();

        if (!TextUtils.isEmpty(mText)) {
        	// Converts html to spannable if ContentType is "text/html".
            // buf.append(Html.fromHtml(body));
            SmileyParser parser = SmileyParser.getInstance();
            buf.append(parser.addSmileySpans(mText));
            buf.append("\n");
        }
    	/*
    	 * Fix the bug that *.inc will be not be treated as URL  
    	 */
        if(!TextUtils.isEmpty(mURL)){
            int urlStart = buf.length();
            buf.append(mURL);
            //new URLSpan(mURL);//it doesn't work
            buf.setSpan(mLinkSpan, urlStart, buf.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (highlight != null) {
            Matcher m = highlight.matcher(buf.toString());
            while (m.find()) {
                buf.setSpan(new StyleSpan(Typeface.BOLD), m.start(), m.end(), 0);
            }
        }
        
        return buf;
    }

    private CharSequence formatTimestamp(WPMessageItem msgItem,String timestamp){
 
        SpannableStringBuilder buf = new SpannableStringBuilder();       
        buf.append(TextUtils.isEmpty(timestamp) ? " " : timestamp);
        buf.setSpan(mSpan, 1, buf.length(), 0);
        
        //Add sim info
       /* int simInfoStart = buf.length();
        CharSequence simInfo = MessageUtils.getSimInfo(mContext, mMessageItem.mSimId);
        if(simInfo.length() > 0){
            buf.append(" ");
            buf.append(mContext.getString(R.string.via_without_time_for_recieve));
            simInfoStart = buf.length();
            buf.append(" ");
            buf.append(simInfo);
        }
        
        buf.setSpan(mTextSmallSpan, 0, buf.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);*/
        // Make the timestamp text not as dark
        buf.setSpan(mColorSpan, 0, buf.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        return buf;
    }

	private CharSequence formatSimStatus(WPMessageItem msgItem){
 
        SpannableStringBuilder buf = new SpannableStringBuilder();       
       /* buf.append(TextUtils.isEmpty(timestamp) ? " " : timestamp);
        buf.setSpan(mSpan, 1, buf.length(), 0);*/
        
        //Add sim info
        int simInfoStart = buf.length();
        CharSequence simInfo = MessageUtils.getSimInfo(mContext, mMessageItem.mSimId);
        if(simInfo.length() > 0){
            buf.append(" ");
            buf.append(mContext.getString(R.string.via_without_time_for_recieve));
            simInfoStart = buf.length();
            buf.append(" ");
            buf.append(simInfo);
        }
        
       // buf.setSpan(mTextSmallSpan, 0, buf.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        // Make the timestamp text not as dark
        buf.setSpan(mColorSpan, 0, simInfoStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        return buf;
    }

    private void drawRightStatusIndicator(WPMessageItem msgItem) {
    	
    	statusIconIndent = DEFAULT_ICON_INDENT;
    	
        //Locked icon
        if (msgItem.mLocked) {
            mLockedIndicator.setImageResource(R.drawable.ic_lock_message_sms);
            mLockedIndicator.setVisibility(View.VISIBLE);
            statusIconIndent += mLockedIndicator.getDrawable().getIntrinsicWidth();
        } else {
            mLockedIndicator.setVisibility(View.GONE);
        }
        
        //Expiration icon
        if (1 == msgItem.isExpired) {
        	mExpirationIndicator.setImageResource(R.drawable.alert_wappush_si_expired);
        	mExpirationIndicator.setVisibility(View.VISIBLE);
        	statusIconIndent += mExpirationIndicator.getDrawable().getIntrinsicWidth();
        } else {
        	mExpirationIndicator.setVisibility(View.GONE);
        }

    }  
    
    public void onMessageListItemClick() {
    	//add for multi-delete
		if (mSelectedBox != null && mSelectedBox.getVisibility() == View.VISIBLE) {
			mSelectedBox.setChecked(!mSelectedBox.isChecked()); 
			if (null != mHandler) {
	            Message msg = Message.obtain(mHandler, MessageListItem.ITEM_CLICK);
	            msg.arg1 = (int) mMessageItem.mMsgId;
	            msg.sendToTarget();
	        }
			return;
		}
		// TODO Auto-generated method stub
    	if(null == mMessageItem.mURL){
    		return;
    	}else{
    		Xlog.i(WP_TAG, "WPMessageListItem: " + "mMessageItem.mURL is : " + mMessageItem.mURL);
    		
            final java.util.ArrayList<String> urls = new ArrayList<String>();
            //add http manually if the URL does not contain this            
            urls.add(MessageUtils.CheckAndModifyUrl(mMessageItem.mURL));            

            ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_item, urls) {
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    try {
                        String url = getItem(position).toString();
                        TextView tv = (TextView) v;
                        Drawable d = mContext.getPackageManager().getActivityIcon(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        if (d != null) {
                            d.setBounds(0, 0, d.getIntrinsicHeight(), d.getIntrinsicHeight());
                            tv.setCompoundDrawablePadding(10);
                            tv.setCompoundDrawables(d, null, null, null);
                        }
                        tv.setText(url);
                    } catch (android.content.pm.PackageManager.NameNotFoundException ex) {
                        ;
                    }
                    return v;
                }
            };

            AlertDialog.Builder b = new AlertDialog.Builder(mContext);

            DialogInterface.OnClickListener click = new DialogInterface.OnClickListener() {
                public final void onClick(DialogInterface dialog, int which) {
                    if (which >= 0) {
                        Uri uri = Uri.parse(urls.get(which));
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        intent.putExtra(Browser.EXTRA_APPLICATION_ID, mContext.getPackageName());
                        //MTK_OP01_PROTECT_START
                        String optr = SystemProperties.get("ro.operator.optr");
                        if (null != optr && optr.equals("OP01")) {
                            intent.putExtra(Browser.APN_SELECTION, Browser.APN_MOBILE);
                        }
                        //MTK_OP01_PROTECT_END
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        try{
                        	mContext.startActivity(intent);
                        }catch(ActivityNotFoundException ex){
                        	//mContext.startActivity(new Intent(com.android.fallback.Fallback.class));
                        	Toast.makeText(mContext, R.string.error_unsupported_scheme, Toast.LENGTH_LONG).show();
                        	Xlog.e(WP_TAG,"Scheme " + uri.getScheme() + "is not supported!" );
                        }
                    }
                }
            };

            b.setTitle(R.string.select_link_title);
            b.setCancelable(true);
            b.setAdapter(adapter, click);

            b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public final void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            b.show();
        }
	}
}
