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

package com.android.mms.ui;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.Browser;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.telephony.PhoneNumberUtils;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.Layout.Alignment;
import android.text.method.HideReturnsTransformationMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LineHeightSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.mms.MmsApp;
import com.android.mms.R;
import com.android.mms.data.WorkingMessage;
import com.android.mms.transaction.Transaction;
import com.android.mms.transaction.TransactionBundle;
import com.android.mms.transaction.TransactionService;
import com.android.mms.util.DownloadManager;
import com.android.mms.util.SmileyParser;
import com.google.android.mms.pdu.PduHeaders;
import com.android.internal.telephony.Phone;
import com.mediatek.featureoption.FeatureOption;


/**
 * This class provides view of a message in the messages list.
 */
public class CBMessageListItem extends RelativeLayout {
    public static final String EXTRA_URLS = "com.android.mms.ExtraUrls";
    public static final int  UPDATE_CHANNEL   = 15;
    private static final String TAG = "CBMessageListItem";
    private static final StyleSpan STYLE_BOLD = new StyleSpan(Typeface.BOLD);

    private View mMsgListItem;
    private TextView mBodyTextView;
    private Handler mHandler;
    private CBMessageItem mMessageItem;
    private TextView mTimestamp;
    private View mItemContainer;
    //add for multi-delete
    private CheckBox mSelectedBox;
    private static final int DEFAULT_ICON_INDENT = 5;
    private static final Uri CHANNEL_ID_URI = Uri.parse("content://cb/channel/#");
    private ImageView mRightStatusIndicator;
    public CBMessageListItem(Context context) {
        super(context);
    }

    public CBMessageListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        int color = mContext.getResources().getColor(R.color.timestamp_color);
        mColorSpan = new ForegroundColorSpan(color);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mMsgListItem = findViewById(R.id.msg_list_item);
        mBodyTextView = (TextView) findViewById(R.id.text_view);
        mTimestamp = (TextView) findViewById(R.id.wpms_timestamp);
        mItemContainer =  findViewById(R.id.wpms_layout_view_parent);
        mRightStatusIndicator = (ImageView) findViewById(R.id.cbms_locked_indicator);
        //add for multi-delete
        mSelectedBox = (CheckBox)findViewById(R.id.select_check_box);
    }

    public void bind(Context context, CBMessageItem msgItem, boolean isDeleteMode) {
        mMessageItem = msgItem;
        mContext = context;
      //add for multi-delete
        if (isDeleteMode) {
        	mSelectedBox.setVisibility(View.VISIBLE);
        	mSelectedBox.setChecked(msgItem.isSelected());
        } else {
        	mSelectedBox.setVisibility(View.GONE);
        	
        }
        setLongClickable(false);
        mItemContainer.setOnClickListener(new OnClickListener(){
            public void onClick(View v) {            	
            	onMessageListItemClick();
	        }
        });
        bindCommonMessage(msgItem);
    }
    
    public void onMessageListItemClick() {
    	//add for multi-delete
		if (mSelectedBox != null && mSelectedBox.getVisibility() == View.VISIBLE) {
			mSelectedBox.setChecked(!mSelectedBox.isChecked()); 
			if (null != mHandler) {
	            Message msg = Message.obtain(mHandler, MessageListItem.ITEM_CLICK);
	            msg.arg1 = (int) mMessageItem.getMessageId();
	            msg.sendToTarget();
	        }
			return;
		}
    }
    
    public void unbind() {
        // do nothing 
    }

    public CBMessageItem getMessageItem() {
        return mMessageItem;
    }

    public void setMsgListItemHandler(Handler handler) {
        mHandler = handler;
    }

    private void bindCommonMessage(final CBMessageItem msgItem) {
        // Since the message text should be concatenated with the sender's
        // address(or name), I have to display it here instead of
        // displaying it by the Presenter.
        mBodyTextView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());

        // Get and/or lazily set the formatted message from/on the
        // MessageItem.  Because the MessageItem instances come from a
        // cache (currently of size ~50), the hit rate on avoiding the
        // expensive formatMessage() call is very high.
        CharSequence formattedMessage = msgItem.getCachedFormattedMessage();
        if (formattedMessage == null) {
            formattedMessage = formatMessage(msgItem.getSubject(), msgItem.getDate(), null);
              msgItem.setCachedFormattedMessage(formattedMessage);
        }
        mBodyTextView.setText(formattedMessage);
        CharSequence timestamp = formatTimestamp(msgItem.getDate());
        
        mTimestamp.setText(timestamp);
                
        //set padding to create space to draw statusIcon
        mTimestamp.setPadding(DEFAULT_ICON_INDENT, 0, 0, 0);
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

    AlignmentSpan.Standard mAlignRight = new AlignmentSpan.Standard(Alignment.ALIGN_OPPOSITE);
     
    ForegroundColorSpan mColorSpan = null;  // set in ctor

    
    private ClickableSpan mLinkSpan = new ClickableSpan(){
    	public void onClick(View widget){
    	}
    };
       
    private CharSequence formatMessage(String body, String timestamp, Pattern highlight) {
    	SpannableStringBuilder buf = new SpannableStringBuilder();

    	if (!TextUtils.isEmpty(body)) {
    		// Converts html to spannable if ContentType is "text/html".
    		// buf.append(Html.fromHtml(body));
    		SmileyParser parser = SmileyParser.getInstance();
    		buf.append(parser.addSmileySpans(body));
    		buf.append("\n");
    	}
    
    	if (highlight != null) {
    		Matcher m = highlight.matcher(buf.toString());
    		while (m.find()) {
    			buf.setSpan(new StyleSpan(Typeface.BOLD), m.start(), m.end(), 0);
    		}
    	}

    	return buf;
    }
   
    private CharSequence formatTimestamp(String timestamp){
    	 
        SpannableStringBuilder buf = new SpannableStringBuilder();       
        buf.append(TextUtils.isEmpty(timestamp) ? " " : timestamp);
        buf.setSpan(mSpan, 1, buf.length(), 0);
        
        //Add sim info
        int simInfoStart = buf.length();
        CharSequence simInfo = MessageUtils.getSimInfo(mContext, mMessageItem.getSimId());
        if(simInfo.length() > 0){
        	buf.append(" ");
            buf.append(mContext.getString(R.string.via_without_time_for_recieve));
            simInfoStart = buf.length();
            buf.append(" ");
            buf.append(simInfo);
            buf.append(" ");
        }
        
        buf.setSpan(mTextSmallSpan, 0, buf.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        // Make the timestamp text not as dark
        buf.setSpan(mColorSpan, 0, simInfoStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        return buf;
    }
    
//    private void drawRightStatusIndicator(CBMessageItem msgItem) {
//    	
//    	statusIconIndent = DEFAULT_ICON_INDENT;
//    	
//        //Locked icon
//        if (msgItem.mLocked) {
//            mLockedIndicator.setImageResource(R.drawable.ic_lock_message_sms);
//            mLockedIndicator.setVisibility(View.VISIBLE);
//            statusIconIndent += mLockedIndicator.getDrawable().getIntrinsicWidth();
//        } else {
//            mLockedIndicator.setVisibility(View.GONE);
//        }      
//    }  
}
