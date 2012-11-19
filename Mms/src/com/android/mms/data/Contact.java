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

package com.android.mms.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Presence;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.Telephony.Mms;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import android.database.sqlite.SqliteWrapper;
import com.android.mms.ui.MessageUtils;
import com.android.mms.LogTag;
import com.mediatek.xlog.Xlog;

public class Contact {
    private static final String TAG = "Contact";
    private static final boolean V = false;
    private static ContactsCache sContactCache;
    private boolean mIsValid = false;

//    private static final ContentObserver sContactsObserver = new ContentObserver(new Handler()) {
//        @Override
//        public void onChange(boolean selfUpdate) {
//            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
//                log("contact changed, invalidate cache");
//            }
//            invalidateCache();
//        }
//    };

    private static final ContentObserver sPresenceObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfUpdate) {
            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                log("presence changed, invalidate cache");
            }
            invalidateCache();
        }
    };

    private final static HashSet<UpdateListener> mListeners = new HashSet<UpdateListener>();

    private String mNumber;
    private String mName;
    private String mNameAndNumber;   // for display, e.g. Fred Flintstone <670-782-1123>
    private boolean mNumberIsModified; // true if the number is modified

    private long mRecipientId;       // used to find the Recipient cache entry
    private String mLabel;
    private long mPersonId;
    private int mPresenceResId;      // TODO: make this a state instead of a res ID
    private String mPresenceText;
    private BitmapDrawable mAvatar;
    protected byte [] mAvatarData;
    private boolean mIsStale;
    private boolean mQueryPending;
    public static final int STATIC_KEY_BUFFER_MAXIMUM_LENGTH = 10;
    public static final int NORMAL_NUMBER_MAX_LENGTH = 15; // Normal number length. For example: +8613012345678

    public interface UpdateListener {
        public void onUpdate(Contact updated);
    }

    /*
     * Make a basic contact object with a phone number.
     */
    private Contact(String number) {
        mName = "";
        setNumber(number);
        mNumberIsModified = false;
        mLabel = "";
        mPersonId = 0;
        mPresenceResId = 0;
        mIsStale = true;
    }

    protected Contact(String number, String label, String name, String nameAndNumber, long personId, int presence, String presenceText) {
    	setNumber(number);
    	mLabel = label;
    	mName = name;
    	mNameAndNumber = nameAndNumber;
        mPersonId = personId;
        mPresenceResId = sContactCache.getPresenceIconResourceId(presence);
        mPresenceText = presenceText;
        mNumberIsModified = false;
        mIsStale = true;
    }

    @Override
    public String toString() {
        return String.format("{ number=%s, name=%s, nameAndNumber=%s, label=%s, person_id=%d, hash=%d }",
                (mNumber != null ? mNumber : "null"),
                (mName != null ? mName : "null"),
                (mNameAndNumber != null ? mNameAndNumber : "null"),
                (mLabel != null ? mLabel : "null"),
                mPersonId, hashCode());
    }

    private static void logWithTrace(String msg, Object... format) {
        Thread current = Thread.currentThread();
        StackTraceElement[] stack = current.getStackTrace();

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(current.getId());
        sb.append("] ");
        sb.append(String.format(msg, format));

        sb.append(" <- ");
        int stop = stack.length > 7 ? 7 : stack.length;
        for (int i = 3; i < stop; i++) {
            String methodName = stack[i].getMethodName();
            sb.append(methodName);
            if ((i+1) != stop) {
                sb.append(" <- ");
            }
        }

        Log.d(TAG, sb.toString());
    }

    public static Contact get(String number, boolean canBlock) {
        return sContactCache.get(number, canBlock);
    }

    public static void invalidateCache() {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("invalidateCache");
        }

        // While invalidating our local Cache doesn't remove the contacts, it will mark them
        // stale so the next time we're asked for a particular contact, we'll return that
        // stale contact and at the same time, fire off an asyncUpdateContact to update
        // that contact's info in the background. UI elements using the contact typically
        // call addListener() so they immediately get notified when the contact has been
        // updated with the latest info. They redraw themselves when we call the
        // listener's onUpdate().
        sContactCache.invalidate();
    }

    private static String emptyIfNull(String s) {
        return (s != null ? s : "");
    }

    public static String formatNameAndNumber(String name, String number) {
        // Format like this: Mike Cleron <(650) 555-1234>
        //                   Erick Tseng <(650) 555-1212>
        //                   Tutankhamun <tutank1341@gmail.com>
        //                   (408) 555-1289
        String formattedNumber = number;
        if (!Mms.isEmailAddress(number)) {
            formattedNumber = PhoneNumberUtils.formatNumber(number);
        }

        if (!TextUtils.isEmpty(name) && !name.equals(number)) {
            return name + " <" + formattedNumber + ">";
        } else {
            return formattedNumber;
        }
    }

    public synchronized void reload() {
        mIsStale = true;
        sContactCache.get(mNumber, false);
    }

    public synchronized void reload(boolean isBlock) {
        mIsStale = true;
        sContactCache.get(mNumber, isBlock);
    }

    public synchronized String getNumber() {
        return mNumber;
    }

    public synchronized void setNumber(String number) {
        mNumber = number;
        notSynchronizedUpdateNameAndNumber();
        mNumberIsModified = true;
    }

    public boolean isNumberModified() {
        return mNumberIsModified;
    }

    public void setIsNumberModified(boolean flag) {
        mNumberIsModified = flag;
    }

    public synchronized String getName() {
        if (TextUtils.isEmpty(mName)) {
            return mNumber;
        } else {
            return mName;
        }
    }

    public synchronized String getNameAndNumber() {
        return mNameAndNumber;
    }

    private synchronized void updateNameAndNumber() {
       notSynchronizedUpdateNameAndNumber();
    }

    private void notSynchronizedUpdateNameAndNumber() {
        mNameAndNumber = formatNameAndNumber(mName, mNumber);
    }

    public synchronized long getRecipientId() {
        return mRecipientId;
    }

    public synchronized void setRecipientId(long id) {
        mRecipientId = id;
    }

    public synchronized String getLabel() {
        return mLabel;
    }

    public synchronized Uri getUri() {
        return ContentUris.withAppendedId(Contacts.CONTENT_URI, mPersonId);
    }

    public synchronized int getPresenceResId() {
        return mPresenceResId;
    }

    public synchronized boolean existsInDatabase() {
        return (mPersonId > 0);
    }

    public static void addListener(UpdateListener l) {
        synchronized (mListeners) {
            mListeners.add(l);
        }
    }

    public static void removeListener(UpdateListener l) {
        synchronized (mListeners) {
            mListeners.remove(l);
        }
    }

    public static synchronized void dumpListeners() {
        int i = 0;
        Log.i(TAG, "[Contact] dumpListeners; size=" + mListeners.size());
        for (UpdateListener listener : mListeners) {
            Log.i(TAG, "["+ (i++) + "]" + listener);
        }
    }

    public synchronized boolean isEmail() {
        return Mms.isEmailAddress(mNumber);
    }

    public String getPresenceText() {
        return mPresenceText;
    }

    public synchronized Drawable getAvatar(Context context, Drawable defaultValue) {
        if (mAvatar == null) {
            if (mAvatarData != null) {
                Bitmap b = BitmapFactory.decodeByteArray(mAvatarData, 0, mAvatarData.length);
                mAvatar = new BitmapDrawable(context.getResources(), b);
            }
        }
        return mAvatar != null ? mAvatar : defaultValue;
    }

    public static void init(final Context context) {
        sContactCache = new ContactsCache(context);

        RecipientIdCache.init(context);

        // it maybe too aggressive to listen for *any* contact changes, and rebuild MMS contact
        // cache each time that occurs. Unless we can get targeted updates for the contacts we
        // care about(which probably won't happen for a long time), we probably should just
        // invalidate cache peoridically, or surgically.
        /*
        context.getContentResolver().registerContentObserver(
                Contacts.CONTENT_URI, true, sContactsObserver);
        */
    }

    public static void dump() {
        sContactCache.dump();
    }

    private static class ContactsCache {
        private final TaskStack mTaskQueue = new TaskStack();
        private static final String SEPARATOR = ";";

        // query params for caller id lookup
        private static final String CALLER_ID_SELECTION = "PHONE_NUMBERS_EQUAL(" + Phone.NUMBER
                + ",?) AND " + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'"
                + " AND " + Data.RAW_CONTACT_ID + " IN "
                        + "(SELECT raw_contact_id "
                        + " FROM phone_lookup"
                        + " WHERE normalized_number GLOB('+*'))";

        // Utilizing private API
        private static final Uri PHONES_WITH_PRESENCE_URI = Data.CONTENT_URI;

        private static final String[] CALLER_ID_PROJECTION = new String[] {
                Phone.NUMBER,                   // 0
                Phone.LABEL,                    // 1
                Phone.DISPLAY_NAME,             // 2
                Phone.CONTACT_ID,               // 3
                Phone.CONTACT_PRESENCE,         // 4
                Phone.CONTACT_STATUS,           // 5
        };

        private static final int PHONE_NUMBER_COLUMN = 0;
        private static final int PHONE_LABEL_COLUMN = 1;
        private static final int CONTACT_NAME_COLUMN = 2;
        private static final int CONTACT_ID_COLUMN = 3;
        private static final int CONTACT_PRESENCE_COLUMN = 4;
        private static final int CONTACT_STATUS_COLUMN = 5;

        // query params for contact lookup by email
        private static final Uri EMAIL_WITH_PRESENCE_URI = Data.CONTENT_URI;

        private static final String EMAIL_SELECTION = "UPPER(" + Email.DATA + ")=UPPER(?) AND "
                + Data.MIMETYPE + "='" + Email.CONTENT_ITEM_TYPE + "'";

        private static final String[] EMAIL_PROJECTION = new String[] {
                Email.DISPLAY_NAME,           // 0
                Email.CONTACT_PRESENCE,       // 1
                Email.CONTACT_ID,             // 2
                Phone.DISPLAY_NAME,           //
        };
        private static final int EMAIL_NAME_COLUMN = 0;
        private static final int EMAIL_STATUS_COLUMN = 1;
        private static final int EMAIL_ID_COLUMN = 2;
        private static final int EMAIL_CONTACT_NAME_COLUMN = 3;

        private final Context mContext;

        private final HashMap<String, ArrayList<Contact>> mContactsHash =
            new HashMap<String, ArrayList<Contact>>();

        private ContactsCache(Context context) {
            mContext = context;
        }

        void dump() {
            synchronized (ContactsCache.this) {
                Log.d(TAG, "**** Contact cache dump ****");
                for (String key : mContactsHash.keySet()) {
                    ArrayList<Contact> alc = mContactsHash.get(key);
                    for (Contact c : alc) {
                        Log.d(TAG, key + " ==> " + c.toString());
                    }
                }
            }
        }

        private static class TaskStack {
            Thread mWorkerThread;
            private final ArrayList<Runnable> mThingsToLoad;

            public TaskStack() {
                mThingsToLoad = new ArrayList<Runnable>();
                mWorkerThread = new Thread(new Runnable() {
                    public void run() {
                        while (true) {
                            Runnable r = null;
                            synchronized (mThingsToLoad) {
                                if (mThingsToLoad.size() == 0) {
                                    try {
                                        mThingsToLoad.wait();
                                    } catch (InterruptedException ex) {
                                        // nothing to do
                                    }
                                }
                                if (mThingsToLoad.size() > 0) {
                                    r = mThingsToLoad.remove(0);
                                }
                            }
                            if (r != null) {
                                r.run();
                            }
                        }
                    }
                });
                mWorkerThread.start();
            }

            public void push(Runnable r) {
                synchronized (mThingsToLoad) {
                    mThingsToLoad.add(r);
                    mThingsToLoad.notify();
                }
            }
        }

        public void pushTask(Runnable r) {
            mTaskQueue.push(r);
        }

        private Object obj = new Object();
        public Contact get(String number, boolean canBlock) {
            if (V) logWithTrace("get(%s, %s)", number, canBlock);

            if (TextUtils.isEmpty(number)) {
                number = "";        // In some places (such as Korea), it's possible to receive
                                    // a message without the sender's address. In this case,
                                    // all such anonymous messages will get added to the same
                                    // thread.
            }

            // Always return a Contact object, if if we don't have an actual contact
            // in the contacts db.
            Contact contact = get(number);
            Runnable r = null;

            synchronized (contact) {
                // If there's a query pending and we're willing to block then
                // wait here until the query completes.

                // make sure the block can update contact immediately
                if (canBlock) {
                    contact.mIsStale = true;
                }
                // If we're stale and we haven't already kicked off a query then kick
                // it off here.
                if (contact.mIsStale) {
                    contact.mIsStale = false;

                    if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                        log("async update for " + contact.toString() + " canBlock: " + canBlock +
                                " isStale: " + contact.mIsStale);
                    }

                    final Contact c = contact;
                    r = new Runnable() {
                        public void run() {
                            updateContact(c);
                            synchronized (obj) {
                                obj.notifyAll();
                            }
                        }
                    };
                }
            }
            // do this outside of the synchronized so we don't hold up any
            // subsequent calls to "get" on other threads
            if (r != null) {
                if (canBlock) {
                    pushTask(r);

                    synchronized (obj) {
                        try {
                            int waitTime = 35;
                            obj.wait(waitTime);
                        } catch (InterruptedException ex) {
                            // do nothing
                        }
                    }
                } else {
                    pushTask(r);
                }
            }
            return contact;
        }

        private boolean contactChanged(Contact orig, Contact newContactData) {
            // The phone number should never change, so don't bother checking.
            // TODO: Maybe update it if it has gotten longer, i.e. 650-234-5678 -> +16502345678?

            String oldName = emptyIfNull(orig.mName);
            String newName = emptyIfNull(newContactData.mName);
            if (!oldName.equals(newName)) {
                if (V) Log.d(TAG, String.format("name changed: %s -> %s", oldName, newName));
                return true;
            }

            String oldLabel = emptyIfNull(orig.mLabel);
            String newLabel = emptyIfNull(newContactData.mLabel);
            if (!oldLabel.equals(newLabel)) {
                if (V) Log.d(TAG, String.format("label changed: %s -> %s", oldLabel, newLabel));
                return true;
            }

            if (orig.mPersonId != newContactData.mPersonId) {
                if (V) Log.d(TAG, "person id changed");
                return true;
            }

            if (orig.mPresenceResId != newContactData.mPresenceResId) {
                if (V) Log.d(TAG, "presence changed");
                return true;
            }

            if (!Arrays.equals(orig.mAvatarData, newContactData.mAvatarData)) {
                if (V) Log.d(TAG, "avatar changed");
                return true;
            }

            return false;
        }

        private void updateContact(final Contact c) {
            if (c == null) {
                log("Contact.updateContact(): Contact for query is null.");
                return;
            }

            Contact entry = getContactInfo(c.mNumber);
            synchronized (c) {
                if (contactChanged(c, entry)) {
                    if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                        log("updateContact: contact changed for " + entry.mName);
                    }

                    c.mNumber = entry.mNumber;
                    c.mLabel = entry.mLabel;
                    c.mPersonId = entry.mPersonId;
                    c.mPresenceResId = entry.mPresenceResId;
                    c.mPresenceText = entry.mPresenceText;
                    c.mAvatarData = entry.mAvatarData;
                    c.mAvatar = entry.mAvatar;
                    c.mName = entry.mName;
                    c.mIsValid = true;
                    c.notSynchronizedUpdateNameAndNumber();

                    // clone the list of listeners in case the onUpdate call turns around and
                    // modifies the list of listeners
                    // access to mListeners is synchronized on ContactsCache
                    HashSet<UpdateListener> iterator;
                    synchronized (mListeners) {
                        iterator = (HashSet<UpdateListener>)Contact.mListeners.clone();
                    }
                    for (UpdateListener l : iterator) {
                        if (V) Log.d(TAG, "updating " + l);
                        l.onUpdate(c);
                    }
                }
            }
        }

        /**
         * Returns the caller info in Contact.
         */
        public Contact getContactInfo(String numberOrEmail) {
            if (Mms.isEmailAddress(numberOrEmail)) {
                return getContactInfoForEmailAddress(numberOrEmail);
            } else {
                return getContactInfoForPhoneNumber(numberOrEmail);
            }
        }

        /**
         * Queries the caller id info with the phone number.
         * @return a Contact containing the caller id info corresponding to the number.
         */
        private Contact getContactInfoForPhoneNumber(String number) {
            number = PhoneNumberUtils.stripSeparators(number);
            Contact entry = new Contact(number);

            //if (LOCAL_DEBUG) log("queryContactInfoByNumber: number=" + number);

            // We need to include the phone number in the selection string itself rather then
            // selection arguments, because SQLite needs to see the exact pattern of GLOB
            // to generate the correct query plan
            String selection = CALLER_ID_SELECTION.replace("+",
                    PhoneNumberUtils.toCallerIDMinMatch(number));
            Cursor cursor = mContext.getContentResolver().query(
                    PHONES_WITH_PRESENCE_URI,
                    CALLER_ID_PROJECTION,
                    selection,
                    new String[] { number },
                    null);

            if (cursor == null) {
                Log.w(TAG, "queryContactInfoByNumber(" + number + ") returned NULL cursor!" +
                        " contact uri used " + PHONES_WITH_PRESENCE_URI);
                return entry;
            }

            try {
                if (cursor.moveToFirst()) {
                    synchronized (entry) {
                        entry.mLabel = cursor.getString(PHONE_LABEL_COLUMN);
                        entry.mName = cursor.getString(CONTACT_NAME_COLUMN);
                        entry.mPersonId = cursor.getLong(CONTACT_ID_COLUMN);
                        entry.mPresenceResId = getPresenceIconResourceId(
                                cursor.getInt(CONTACT_PRESENCE_COLUMN));
                        entry.mPresenceText = cursor.getString(CONTACT_STATUS_COLUMN);
                        entry.mIsValid = true;
                        if (V) {
                            log("queryContactInfoByNumber: name=" + entry.mName +
                                    ", number=" + number + ", presence=" + entry.mPresenceResId);
                        }
                    }

                    byte[] data = loadAvatarData(entry);

                    synchronized (entry) {
                        entry.mAvatarData = data;
                    }

                }
            } finally {
                cursor.close();
            }

            return entry;
        }

        /*
         * Load the avatar data from the cursor into memory.  Don't decode the data
         * until someone calls for it (see getAvatar).  Hang onto the raw data so that
         * we can compare it when the data is reloaded.
         * TODO: consider comparing a checksum so that we don't have to hang onto
         * the raw bytes after the image is decoded.
         */
        protected byte[] loadAvatarData(Contact entry) {
            byte [] data = null;

            if (entry.mPersonId == 0 || entry.mAvatar != null) {
                return null;
            }

            Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, entry.mPersonId);

            InputStream avatarDataStream = Contacts.openContactPhotoInputStream(
                        mContext.getContentResolver(),
                        contactUri);
            try {
                if (avatarDataStream != null) {
                    data = new byte[avatarDataStream.available()];
                    avatarDataStream.read(data, 0, data.length);
                }
            } catch (IOException ex) {
                //
            } finally {
                try {
                    if (avatarDataStream != null) {
                        avatarDataStream.close();
                    }
                } catch (IOException e) {
                }
            }

            return data;
        }

        private int getPresenceIconResourceId(int presence) {
            // TODO: must fix for SDK
            if (presence != Presence.OFFLINE) {
                return Presence.getPresenceIconResourceId(presence);
            }

            return 0;
        }

        /**
         * Query the contact email table to get the name of an email address.
         */
        private Contact getContactInfoForEmailAddress(String email) {
            Contact entry = new Contact(email);

            Cursor cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
                    EMAIL_WITH_PRESENCE_URI,
                    EMAIL_PROJECTION,
                    EMAIL_SELECTION,
                    new String[] { email },
                    null);

            if (cursor != null) {
                try {
                	boolean found;
                    while (cursor.moveToNext()) {
                        found = false;

                        synchronized (entry) {
                            entry.mPresenceResId = getPresenceIconResourceId(
                                    cursor.getInt(EMAIL_STATUS_COLUMN));
                            entry.mPersonId = cursor.getLong(EMAIL_ID_COLUMN);

                            String name = cursor.getString(EMAIL_NAME_COLUMN);
                            if (TextUtils.isEmpty(name)) {
                                name = cursor.getString(EMAIL_CONTACT_NAME_COLUMN);
                            }
                            if (!TextUtils.isEmpty(name)) {
                                entry.mName = name;
                                if (V) {
                                    log("getContactInfoForEmailAddress: name=" + entry.mName +
                                            ", email=" + email + ", presence=" +
                                            entry.mPresenceResId);
                                }
                                found = true;
                                entry.mIsValid = true;
                            }
                        }

                        if (found) {
                            byte[] data = loadAvatarData(entry);
                            synchronized (entry) {
                                entry.mAvatarData = data;
                            }

                            break;
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
            return entry;
        }

        // Invert and truncate to five characters the phoneNumber so that we
        // can use it as the key in a hashtable.  We keep a mapping of this
        // key to a list of all contacts which have the same key.
        private String key(String phoneNumber, CharBuffer keyBuffer) {
            if (!PhoneNumberUtils.isWellFormedSmsAddress(phoneNumber)) {
                Xlog.d(TAG, "key(): It's not a well form sms address!");
                return phoneNumber;
            } else if (!phoneNumber.startsWith("+") &&
                    phoneNumber.trim().replaceAll("-", "").length() > NORMAL_NUMBER_MAX_LENGTH) {
                Xlog.d(TAG, "key(): The number is not start with '+'!");
                return phoneNumber;
            }

            keyBuffer.clear();
            keyBuffer.mark();
            int position = phoneNumber.length();
            int resultCount = 0;
            while (--position >= 0) {
                keyBuffer.put(phoneNumber.charAt(position));
                if (++resultCount == STATIC_KEY_BUFFER_MAXIMUM_LENGTH) {
                    break;
                }
            }
            keyBuffer.reset();
            if (resultCount > 0) {
                return keyBuffer.toString();
            } else {
                // there were no usable digits in the input phoneNumber
                return phoneNumber;
            }
        }

        // Reuse this so we don't have to allocate each time we go through this
        // "get" function.
        static CharBuffer sStaticKeyBuffer = CharBuffer.allocate(STATIC_KEY_BUFFER_MAXIMUM_LENGTH);

        public Contact get(String numberOrEmail) {
            synchronized (ContactsCache.this) {
                // See if we can find "number" in the hashtable.
                // If so, just return the result.
                final boolean isNotRegularPhoneNumber = Mms.isEmailAddress(numberOrEmail) ||
                        MessageUtils.isAlias(numberOrEmail);
                final String key = isNotRegularPhoneNumber ?
                        numberOrEmail : key(numberOrEmail, sStaticKeyBuffer);

                ArrayList<Contact> candidates = mContactsHash.get(key);
                if (candidates != null) {
                    int length = candidates.size();
                    Contact c = null;
                    for (int i = 0; i < length; i++) {
                        c = candidates.get(i);
                        if (isNotRegularPhoneNumber) {
                            if (numberOrEmail.equals(c.mNumber)) {
                                return c;
                            }
                        } else {
                            if (PhoneNumberUtils.compare(numberOrEmail, c.mNumber)) {
                                return c;
                            }
                        }
                    }
                } else {
                    candidates = new ArrayList<Contact>();
                    // call toString() since it may be the static CharBuffer
                    mContactsHash.put(key, candidates);
                }
                Contact c = new Contact(numberOrEmail);
                candidates.add(c);
                return c;
            }
        }

        void invalidate() {
            // Don't remove the contacts. Just mark them stale so we'll update their
            // info, particularly their presence.
            synchronized (ContactsCache.this) {
                for (ArrayList<Contact> alc : mContactsHash.values()) {
                    for (Contact c : alc) {
                        synchronized (c) {
                            c.mIsStale = true;
                        }
                    }
                }
            }
        }
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
