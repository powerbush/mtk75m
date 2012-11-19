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
 */

package com.android.mms.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Presence;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import android.util.Log;

import com.android.mms.data.Contact.UpdateListener;
import com.android.mms.LogTag;
import com.android.mms.ui.MessageUtils;
import com.mediatek.xlog.Xlog;

public class ContactList extends ArrayList<Contact> {
	private static final long serialVersionUID = 1L;
	
	private static final String TAG = "Mms/ContactList";
	// query params for contact lookup by number
	private static final Uri PHONES_WITH_PRESENCE_URI = Data.CONTENT_URI;

	private static final String[] CALLER_ID_PROJECTION = new String[] {
			Phone.NUMBER, // 0
			Phone.LABEL, // 1
			Phone.DISPLAY_NAME, // 2
			Phone.CONTACT_ID, // 3
			Phone.CONTACT_PRESENCE, // 4
			Phone.CONTACT_STATUS, // 5
	};

	private static final int PHONE_NUMBER_COLUMN = 0;
	private static final int PHONE_LABEL_COLUMN = 1;
	private static final int CONTACT_NAME_COLUMN = 2;
	private static final int CONTACT_ID_COLUMN = 3;
	private static final int CONTACT_PRESENCE_COLUMN = 4;
	private static final int CONTACT_STATUS_COLUMN = 5;

	public static ContactList getByNumbers(Iterable<String> numbers, boolean canBlock) {
		ContactList list = new ContactList();
		for (String number : numbers) {
			if (!TextUtils.isEmpty(number)) {
				list.add(Contact.get(number, canBlock));
			}
		}
		return list;
	}

	public static ContactList getByNumbers(String semiSepNumbers,
			boolean canBlock, boolean replaceNumber) {
		ContactList list = new ContactList();
		for (String number : semiSepNumbers.split(";")) {
			if (!TextUtils.isEmpty(number)) {
				Contact contact = Contact.get(number, canBlock);
				if (replaceNumber) {
					contact.setNumber(number);
				}
				list.add(contact);
			}
		}
		return list;
	}

	public static ContactList getByNumbers(Context mContext,
			String semiSepNumbers, boolean canBlock, boolean replaceNumber) {
		ContactList list = new ContactList();
		if (semiSepNumbers.contains(";")) {
			String selection = Phone.NUMBER + " in ('"
					+ semiSepNumbers.replaceAll(";", "','") + "')";
			selection = selection.replaceAll("-", "");
			LinkedList<String> numberList = new LinkedList<String>(Arrays.asList(semiSepNumbers.split(";")));
			Xlog.v(TAG, "ContactList:getByNumber()--selection = \"" + selection + "\"");
			selection = DatabaseUtils.sqlEscapeString(selection);
			Cursor cursor = mContext.getContentResolver().query(
					PHONES_WITH_PRESENCE_URI, CALLER_ID_PROJECTION, selection,
					null, null);
			Xlog.v(TAG, "ContactList:getByNumber()-1");
			if (cursor == null) {
				Xlog.w(TAG, "ContactList.getByNumbers(" + semiSepNumbers
						+ ") returned NULL cursor! contact uri used "
						+ PHONES_WITH_PRESENCE_URI);
				return list;
			}
			Xlog.v(TAG, "ContactList:getByNumber()-2");
			try {
				while (cursor.moveToNext()) {
					String number = cursor.getString(PHONE_NUMBER_COLUMN);
					String label = cursor.getString(PHONE_LABEL_COLUMN);
					String name = cursor.getString(CONTACT_NAME_COLUMN);
					String nameAndNumber = Contact.formatNameAndNumber(name, number);
					long personId = cursor.getLong(CONTACT_ID_COLUMN);
					int presence = cursor.getInt(CONTACT_PRESENCE_COLUMN);
					String presenceText = cursor.getString(CONTACT_STATUS_COLUMN);
					Contact entry = new Contact(number, label, name, nameAndNumber, personId,
							presence, presenceText);

					// byte[] data = loadAvatarData(entry);
					// synchronized (entry) {
					// entry.mAvatarData = data;
					// }
					list.add(entry);
					numberList.remove(number);
				}
			} finally {
				cursor.close();
			}
			Xlog.v(TAG, "ContactList:getByNumber()-3");
			if (numberList.size() > 0) {
				for (String number : numberList) {
					Contact entry = Contact.get(number, false);
					list.add(entry);
				}
			}
			return list;
		} else {
		    // only one recipient, query with block
		    canBlock = true;
			return getByNumbers(semiSepNumbers, canBlock, replaceNumber);
		}
	}

	/**
	 * Returns a ContactList for the corresponding recipient ids passed in. This
	 * method will create the contact if it doesn't exist, and would inject the
	 * recipient id into the contact.
	 */
	public static ContactList getByIds(String spaceSepIds, boolean canBlock) {
		ContactList list = new ContactList();
		for (RecipientIdCache.Entry entry : RecipientIdCache
				.getAddresses(spaceSepIds)) {
			if (entry != null && !TextUtils.isEmpty(entry.number)) {
				Contact contact = Contact.get(entry.number, canBlock);
				contact.setRecipientId(entry.id);
				list.add(contact);
			}
		}
		return list;
	}

	public int getPresenceResId() {
		// We only show presence for single contacts.
		if (size() != 1)
			return 0;

		return get(0).getPresenceResId();
	}
	
	public String getFirstName(String separator) {
	    return this.get(0).getName() + " & " + (size()-1);
	}

	public String formatNames(String separator) {
		String[] names = new String[size()];
		int i = 0;
		for (Contact c : this) {
			names[i++] = c.getName();
		}
		return TextUtils.join(separator, names);
	}

	public String formatNamesAndNumbers(String separator) {
		String[] nans = new String[size()];
		int i = 0;
		for (Contact c : this) {
			nans[i++] = c.getNameAndNumber();
		}
		return TextUtils.join(separator, nans);
	}

	public String serialize() {
		return TextUtils.join(";", getNumbers());
	}

	public boolean containsEmail() {
		for (Contact c : this) {
			if (c.isEmail()) {
				return true;
			}
		}
		return false;
	}

	public String[] getNumbers() {
		return getNumbers(false /* don't scrub for MMS address */);
	}

	public String[] getNumbers(boolean scrubForMmsAddress) {
		List<String> numbers = new ArrayList<String>();
		String number;
		for (Contact c : this) {
			number = c.getNumber();

			if (scrubForMmsAddress) {
				// parse/scrub the address for valid MMS address. The returned
				// number
				// could be null if it's not a valid MMS address. We don't want
				// to send
				// a message to an invalid number, as the network may do its own
				// stripping,
				// and end up sending the message to a different number!
				number = MessageUtils.parseMmsAddress(number);
			}

			// Don't add duplicate numbers. This can happen if a contact name
			// has a comma.
			// Since we use a comma as a delimiter between contacts, the code
			// will consider
			// the same recipient has been added twice. The recipients UI still
			// works correctly.
			// It's easiest to just make sure we only send to the same recipient
			// once.
			if (!TextUtils.isEmpty(number) && !numbers.contains(number)) {
				numbers.add(number);
			}
		}
		return numbers.toArray(new String[numbers.size()]);
	}

	@Override
	public boolean equals(Object obj) {
		try {
			ContactList other = (ContactList) obj;
			// If they're different sizes, the contact
			// set is obviously different.
			if (size() != other.size()) {
				return false;
			}

			// Make sure all the individual contacts are the same.
			for (Contact c : this) {
				if (!other.contains(c)) {
					return false;
				}
			}

			return true;
		} catch (ClassCastException e) {
			return false;
		}
	}

	private void log(String msg) {
		Log.d(LogTag.TAG, "[ContactList] " + msg);
	}
}
