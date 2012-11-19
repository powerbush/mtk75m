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

import com.android.mms.MmsConfig;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.ui.RecipientsEditor;

import android.content.Context;
import android.provider.Telephony.Mms;
import android.telephony.PhoneNumberUtils;
import android.text.Annotation;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.RecipientsView;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import com.mediatek.xlog.Xlog;

/**
 * Provide UI for editing the recipients of multi-media messages.
 */
public class RecipientsEditor extends RecipientsView {
	private static final String TAG = "Mms/RecipientsEditor";
	private int mLongPressedPosition = -1;
	private final RecipientsEditorTokenizer mTokenizer;
	private char mLastSeparator = ',';

	public RecipientsEditor(Context context, AttributeSet attrs) {
		super(context, attrs, android.R.attr.autoCompleteTextViewStyle);
		mTokenizer = new RecipientsEditorTokenizer(context, this);
		setTokenizer(mTokenizer);
		
		// For the focus to move to the message body when soft Next is pressed
		setImeOptions(EditorInfo.IME_ACTION_NEXT);

		/*
		 * The point of this TextWatcher is that when the user chooses an
		 * address completion from the AutoCompleteTextView menu, it is marked
		 * up with Annotation objects to tie it back to the address book entry
		 * that it came from. If the user then goes back and edits that part of
		 * the text, it no longer corresponds to that address book entry and
		 * needs to have the Annotations claiming that it does removed.
		 */
	}

	@Override
	public boolean enoughToFilter() {
		if (!super.enoughToFilter()) {
			return false;
		}
		// If the user is in the middle of editing an existing recipient, don't
		// offer the
		// auto-complete menu. Without this, when the user selects an
		// auto-complete menu item,
		// it will get added to the list of recipients so we end up with the old
		// before-editing
		// recipient and the new post-editing recipient. As a precedent, gmail
		// does not show
		// the auto-complete menu when editing an existing recipient.
		int end = getSelectionEnd();
		int len = getText().length();

		return end == len;

	}

	public int getRecipientCount() {
		List<String> numberList = mTokenizer.getNumbers();
		return numberList.isEmpty()? 0: numberList.size();
	}

	public List<String> getNumbers() {
		return mTokenizer.getNumbers();
	}

	public void setNumbers(List<String> numberList) {
		mTokenizer.setNumbers(numberList);
	}

	public String allNumberToString() {
		return mTokenizer.allNumberToString();
	}

	public boolean isDuplicateNumber(String number) {
		return mTokenizer.getNumbers().contains(number);
	}

	public ContactList constructContactsFromInput() {
		List<String> numbers = mTokenizer.getNumbers();
		ContactList list = new ContactList();
		for (String number : numbers) {
			Contact contact = Contact.get(number, false);
			contact.setNumber(number);
			list.add(contact);
		}
		return list;
	}

	private boolean isValidAddress(String number, boolean isMms) {
		if (isMms) {
			return MessageUtils.isValidMmsAddress(number);
		} else {
			// TODO: PhoneNumberUtils.isWellFormedSmsAddress() only check if the
			// number is a valid
			// GSM SMS address. If the address contains a dialable char, it
			// considers it a well
			// formed SMS addr. CDMA doesn't work that way and has a different
			// parser for SMS
			// address (see CdmaSmsAddress.parse(String address)). We should
			// definitely fix this!!!
			return PhoneNumberUtils.isWellFormedSmsAddress(number.replaceAll("-", ""))
					|| Mms.isEmailAddress(number);
		}
	}

	public boolean hasValidRecipient(boolean isMms) {
		for (String number : mTokenizer.getNumbers()) {
			if (isValidAddress(number, isMms))
				return true;
		}
		return false;
	}

	public boolean hasInvalidRecipient(boolean isMms) {
		for (String number : mTokenizer.getNumbers()) {
			if (!isValidAddress(number, isMms)) {
				if (MmsConfig.getEmailGateway() == null) {
					return true;
				} else if (!MessageUtils.isAlias(number)) {
					return true;
				}
			}
		}
		return false;
	}

	public String formatInvalidNumbers(boolean isMms) {
		StringBuilder sb = new StringBuilder();
		for (String number : mTokenizer.getNumbers()) {
			if (!isValidAddress(number, isMms)) {
				if (sb.length() != 0) {
					sb.append(", ");
				}
				sb.append(number);
			}
		}
		return sb.toString();
	}

	public boolean containsEmail() {
		List<String> numbers = mTokenizer.getNumbers();
		for (String number : numbers) {
			if (Mms.isEmailAddress(number)) {
				return true;
			}
		}
		return false;
	}

	public static CharSequence contactToToken(Contact c) {
		Xlog.v(TAG, "RecipientsEditor:contactToToken, contact.getName=" + c.getName());
		SpannableString s = new SpannableString(c.getName());
		int len = s.length();

		if (len == 0) {
			return s;
		}

		s.setSpan(new Annotation("number", c.getName()), 0, len,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		return s;
	}

	public void populate(ContactList list) {
		Xlog.i(TAG, "RecipientsEditor : populate(ContactsList list), list.size() = " + list.size());
		List<String> numberList = new ArrayList<String>();
		for (Contact c : list) {
			if (numberList != null && numberList.size() < MmsConfig.getSmsRecipientLimit()) {
				addRecipient(contactToToken(c), false);
				numberList.add(c.getNumber());
			} else {
				break;
			}
		}
		mTokenizer.setNumbers(numberList);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			InputMethodManager inputMethodManager = (InputMethodManager) getContext()
					.getSystemService(Context.INPUT_METHOD_SERVICE);

			inputMethodManager.showSoftInput(this, 0);
		}
		return super.onKeyUp(keyCode, event);
	}

	private int pointToPosition(int x, int y) {
		x -= getCompoundPaddingLeft();
		y -= getExtendedPaddingTop();

		x += getScrollX();
		y += getScrollY();

		Layout layout = getLayout();
		if (layout == null) {
			return -1;
		}

		int line = layout.getLineForVertical(y);
		int off = layout.getOffsetForHorizontal(line, x);

		return off;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		final int x = (int) ev.getX();
		final int y = (int) ev.getY();

		if (action == MotionEvent.ACTION_DOWN) {
			mLongPressedPosition = pointToPosition(x, y);
		}

		return super.onTouchEvent(ev);
	}

	@Override
	protected ContextMenuInfo getContextMenuInfo() {
		if ((mLongPressedPosition >= 0)) {
			Spanned text = getText();
			if (mLongPressedPosition <= text.length()) {
				int start = mTokenizer.findTokenStart(text,
						mLongPressedPosition);
				int end = mTokenizer.findTokenEnd(text, start);

				if (end != start) {
					String number = getNumberAt(getText(), start, end,
							getContext());
					Contact c = Contact.get(number, false);
					return new RecipientContextMenuInfo(c);
				}
			}
		}
		return null;
	}

	private static String getNumberAt(Spanned sp, int start, int end,
			Context context) {
		return getFieldAt("number", sp, start, end, context);
	}

	private static int getSpanLength(Spanned sp, int start, int end,
			Context context) {
		// TODO: there's a situation where the span can lose its annotations:
		// - add an auto-complete contact
		// - add another auto-complete contact
		// - delete that second contact and keep deleting into the first
		// - we lose the annotation and can no longer get the span.
		// Need to fix this case because it breaks auto-complete contacts with
		// commas in the name.
		Annotation[] a = sp.getSpans(start, end, Annotation.class);
		if (a.length > 0) {
			return sp.getSpanEnd(a[0]);
		}
		return 0;
	}

	private static String getFieldAt(String field, Spanned sp, int start,
			int end, Context context) {
		Annotation[] a = sp.getSpans(start, end, Annotation.class);
		String fieldValue = getAnnotation(a, field);
		if (TextUtils.isEmpty(fieldValue)) {
			fieldValue = TextUtils.substring(sp, start, end);
		}
		return fieldValue;

	}

	private static String getAnnotation(Annotation[] a, String key) {
		for (int i = 0; i < a.length; i++) {
			if (a[i].getKey().equals(key)) {
				return a[i].getValue();
			}
		}

		return "";
	}

	private class RecipientsEditorTokenizer implements
			MultiAutoCompleteTextView.Tokenizer {
		private final MultiAutoCompleteTextView mList;
		private final Context mContext;
		private List<String> mNumberList;

		RecipientsEditorTokenizer(Context context,
				MultiAutoCompleteTextView list) {
			mList = list;
			mContext = context;
			mNumberList = new ArrayList<String>();
		}

		/**
		 * Returns the start of the token that ends at offset
		 * <code>cursor</code> within <code>text</code>. It is a method from the
		 * MultiAutoCompleteTextView.Tokenizer interface.
		 */
		public int findTokenStart(CharSequence text, int cursor) {
			return RecipientsEditor.this.findRecipientStart(text, cursor);
		}

		/**
		 * Returns the end of the token (minus trailing punctuation) that begins
		 * at offset <code>cursor</code> within <code>text</code>. It is a
		 * method from the MultiAutoCompleteTextView.Tokenizer interface.
		 */
		public int findTokenEnd(CharSequence text, int cursor) {
			return RecipientsEditor.this.findRecipientEnd(text, cursor);
		}

		/**
		 * Returns <code>text</code>, modified, if necessary, to ensure that it
		 * ends with a token terminator (for example a space or comma). It is a
		 * method from the MultiAutoCompleteTextView.Tokenizer interface.
		 */
		/*
		 * public CharSequence terminateToken(CharSequence text) { int i =
		 * text.length();
		 * 
		 * while (i > 0 && text.charAt(i - 1) == ' ') { i--; }
		 * 
		 * char c; if (i > 0 && ((c = text.charAt(i - 1)) == ',' || c == ';')) {
		 * return text; } else { // Use the same delimiter the user just typed.
		 * // This lets them have a mixture of commas and semicolons in their
		 * list. String separator = mLastSeparator + " "; if (text instanceof
		 * Spanned) { SpannableString sp = new SpannableString(text +
		 * separator); TextUtils.copySpansFrom((Spanned) text, 0, text.length(),
		 * Object.class, sp, 0); return sp; } else { return text + separator; }
		 * } }
		 */
		public CharSequence terminateToken(CharSequence text) {

			// "text" format as Eric <13988887777>, we need only number,
			// if change to show as Eric <13988887777>, remove code below
			CharSequence numText = "";
			if (mNumberList.size() < MmsConfig.getSmsRecipientLimit()) {
				if (text.toString().indexOf("<") > 0 && text.toString().indexOf(">") > 0) {
					int len = text.length();
					while (len > 0) {
						if (text.charAt(len - 1) == '<') {
							len -= 2;
							break;
						}
						len--;
					}
					numText = text.subSequence(0, len);
					mNumberList.add(text.subSequence((len + 2), (text.length() -1)).toString());
				} else {
					numText = text;
					mNumberList.add(text.toString());
				}

				int i = numText.length();
				while (i > 0 && numText.charAt(i - 1) == ' ') {
					i--;
				}

				char c;
				if (i > 0 && ((c = numText.charAt(i - 1)) == ',' || c == ';')) {
					return numText;
				} else {
					// Use the same delimiter the user just typed.
					// This lets them have a mixture of commas and semicolons in
					// their list.
					// String separator = mLastSeparator + " ";
					if (numText instanceof Spanned) {
						SpannableString sp = new SpannableString(numText);
						TextUtils.copySpansFrom((Spanned) numText, 0, numText
								.length(), Object.class, sp, 0);
						return sp;
					} else {
						return numText;
					}
				}
			}
			return numText;
		}

		public String allNumberToString() {
			String allNumberString = "";
			for (String number : mNumberList) {
				allNumberString += number + ";";
			}
			if (!allNumberString.equals("")) {
				allNumberString = allNumberString.substring(0, allNumberString.length()-1);
			}
			return allNumberString;
		}
		
		public List<String> getNumbers() {
			return mNumberList;
		}
		
		public void setNumbers(List<String> numberList) {
			this.mNumberList = numberList;
		}
	}

	static class RecipientContextMenuInfo implements ContextMenuInfo {
		final Contact recipient;

		RecipientContextMenuInfo(Contact r) {
			recipient = r;
		}
	}

	@Override
	public void showDropDown() {
		// TODO Auto-generated method stub
		super.showDropDown();
		((View) (this.getParent().getParent())).invalidate();
	}

	@Override
	protected boolean structLastRecipient() {
		return super.structLastRecipient();
	}
}
