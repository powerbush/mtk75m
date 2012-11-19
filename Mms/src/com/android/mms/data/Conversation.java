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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.WapPush;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.provider.Telephony.Sms.Conversations;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.mms.LogTag;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.transaction.WapPushMessagingNotification;
import com.android.mms.ui.MessageUtils;
import com.android.mms.util.DraftCache;

import com.mediatek.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;
import com.mediatek.xlog.SXlog;
/**
 * An interface for finding information about conversations and/or creating new ones.
 */
public class Conversation {
    private static final String TAG = "Mms/conv";
    private static final boolean DEBUG = false;

    private static final Uri sAllThreadsUri =
        Threads.CONTENT_URI.buildUpon().appendQueryParameter("simple", "true").build();

    private static final String[] ALL_THREADS_PROJECTION = {
        Threads._ID, Threads.DATE, Threads.MESSAGE_COUNT, Threads.RECIPIENT_IDS,
        Threads.SNIPPET, Threads.SNIPPET_CHARSET, Threads.READ, Threads.ERROR,
        Threads.HAS_ATTACHMENT, Threads.TYPE ,Threads.READCOUNT ,Threads.STATUS
    };

    private static final String[] ALL_CHANNEL_PROJECTION = { "_id",
        Telephony.CbSms.CbChannel.NAME,
        Telephony.CbSms.CbChannel.NUMBER };
    private static final String[] ALL_ADDRESS_PROJECTION = { "_id",
        Telephony.CbSms.CanonicalAddressesColumns.ADDRESS };

    private static final String[] UNREAD_PROJECTION = {
        Threads._ID,
        Threads.READ
    };

    
    private static final String UNREAD_SELECTION = "(read=0 OR seen=0)";
    private static final String READ_SELECTION = "(read=1)";
    private static final Uri CHANNEL_ID_URI = Uri.parse("content://cb/channel/#");
    private static final Uri CHANNEL1_ID_URI = Uri.parse("content://cb/channel1/#");
    private static final Uri ADDRESS_ID_URI = Uri.parse("content://cb/addresses/#");
    private static final String[] SEEN_PROJECTION = new String[] {
        "seen"
    };

    private static final int ID             = 0;
    private static final int DATE           = 1;
    private static final int MESSAGE_COUNT  = 2;
    private static final int RECIPIENT_IDS  = 3;
    private static final int SNIPPET        = 4;
    private static final int SNIPPET_CS     = 5;
    private static final int READ           = 6;
    private static final int ERROR          = 7;
    private static final int HAS_ATTACHMENT = 8;
    //wappush: add TYPE, which already exists
    private static final int TYPE           = 9;
    private static final int READCOUNT    = 10;
    private static final int STATUS       = 11;
    private static Context mContext;

    // The thread ID of this conversation.  Can be zero in the case of a
    // new conversation where the recipient set is changing as the user
    // types and we have not hit the database yet to create a thread.
    private long mThreadId;

    private ContactList mRecipients;    // The current set of recipients.
    private long mDate;                 // The last update time.
    private int mMessageCount;          // Number of messages.
    private int mUnreadMessageCount;    // Number of unread message.
    private int mMessageStatus;         // Message Status.
    private String mSnippet;            // Text of the most recent message.
    private boolean mHasUnreadMessages; // True if there are unread messages.
    private boolean mHasAttachment;     // True if any message has an attachment.
    private boolean mHasError;          // True if any message is in an error state.
    //add for cb
    private int mAddressId;          // Number of messages.
    private int mChannelId = -1;            // Text of the most recent message.
    private String mChannelName = null;
    
    //wappush: add mType variable
    private int mType;

    private static ContentValues mReadContentValues;
    private static boolean mLoadingThreads;
    private boolean mMarkAsReadBlocked;
    private Object mMarkAsBlockedSyncer = new Object();

    private Conversation(Context context) {
        mContext = context;
        mRecipients = new ContactList();
        mThreadId = 0;
    }

    private Conversation(Context context, long threadId, boolean allowQuery) {
        mContext = context;
        Xlog.d(TAG,"new Conversation.loadFromThreadId(threadId, allowQuery): threadId = " 
                + threadId + "allowQuery = " + allowQuery);
        if (!loadFromThreadId(threadId, allowQuery)) {
            mRecipients = new ContactList();
            mThreadId = 0;
        }
    }

    private Conversation(Context context, Cursor cursor, boolean allowQuery) {
        mContext = context;
        fillFromCursor(context, this, cursor, allowQuery);
    }

    /**
     * Create a new conversation with no recipients.  {@link #setRecipients} can
     * be called as many times as you like; the conversation will not be
     * created in the database until {@link #ensureThreadId} is called.
     */
    public static Conversation createNew(Context context) {
        return new Conversation(context);
    }
    public static Conversation upDateThread(Context context, long threadId, boolean allowQuery) {
        Cache.remove(threadId);
        return get(context, threadId, allowQuery);
    }
    /**
     * Find the conversation matching the provided thread ID.
     */
    public static Conversation get(Context context, long threadId, boolean allowQuery) {
        Conversation conv = Cache.get(threadId);
        if (conv != null)
            return conv;

        conv = new Conversation(context, threadId, allowQuery);
        try {
            Cache.put(conv);
        } catch (IllegalStateException e) {
            LogTag.error("Tried to add duplicate Conversation to Cache");
        }
        return conv;
    }

    /**
     * Find the conversation matching the provided recipient set.
     * When called with an empty recipient list, equivalent to {@link #createNew}.
     */
    public static Conversation get(Context context, ContactList recipients, boolean allowQuery) {
        // If there are no recipients in the list, make a new conversation.
        if (recipients.size() < 1) {
            return createNew(context);
        }

        Conversation conv = Cache.get(recipients);
        if (conv != null) {
            //if messgeCount = 0 and no draft, update this conversation. For the case,
            //send SMS from calllog, don't back to ConversationList, then send again,
            //the mssageCount = 0 and have action in the intent, it will init RecipientEditor
            //in ComposeMessageActivity. The CR is 96268
            if (conv.getThreadId() > 0 && conv.getMessageCount() == 0 && !conv.hasDraft()) {
                long threadId = conv.getThreadId();
                Cache.remove(threadId);
                conv = new Conversation(context, threadId, allowQuery);
                try {
                    Cache.put(conv);
                } catch (IllegalStateException e) {
                    Xlog.e(TAG, "Tried to add duplicate Conversation to Cache");
                }
            }
            return conv;
        }

        long threadId = getOrCreateThreadId(context, recipients);
        conv = new Conversation(context, threadId, allowQuery);
        Log.d(TAG, "Conversation.get: created new conversation " + /*conv.toString()*/ "xxxxxxx");

        if (!conv.getRecipients().equals(recipients)) {
            Log.e(TAG, "Conversation.get: new conv's recipients don't match input recpients "
                    + /*recipients*/ "xxxxxxx");
        }

        try {
            Cache.put(conv);
        } catch (IllegalStateException e) {
            LogTag.error("Tried to add duplicate Conversation to Cache");
        }

        return conv;
    }

    /**
     * Find the conversation matching in the specified Uri.  Example
     * forms: {@value content://mms-sms/conversations/3} or
     * {@value sms:+12124797990}.
     * When called with a null Uri, equivalent to {@link #createNew}.
     */
    public static Conversation get(Context context, Uri uri, boolean allowQuery) {
        if (uri == null) {
            return createNew(context);
        }

        if (DEBUG) Log.v(TAG, "Conversation get URI: " + uri);

        // Handle a conversation URI
        if (uri.getPathSegments().size() >= 2) {
            try {
                String threadIdStr = uri.getPathSegments().get(1);
                threadIdStr = threadIdStr.replaceAll("-","");
                //long threadId = Long.parseLong(uri.getPathSegments().get(1));
                long threadId = Long.parseLong(threadIdStr);
                if (DEBUG) {
                    Log.v(TAG, "Conversation get threadId: " + threadId);
                }
                return get(context, threadId, allowQuery);
            } catch (NumberFormatException exception) {
                LogTag.error("Invalid URI: " + uri);
            }
        }

        String recipient = getRecipients(uri);
        return get(context, ContactList.getByNumbers(context, recipient, false /* don't block */, true /* replace number */), allowQuery);
    }

    public static List<String> getNumbers(String recipient) {
        int len = recipient.length();
        List<String> list = new ArrayList<String>();

        int start = 0;
        int i = 0;
        char c;
        while (i < len + 1) {
            if ((i == len) || ((c = recipient.charAt(i)) == ',') || (c == ';')) {
                if (i > start) {
                    list.add(recipient.substring(start, i));

                    // calculate the recipients total length. This is so if the name contains
                    // commas or semis, we'll skip over the whole name to the next
                    // recipient, rather than parsing this single name into multiple
                    // recipients.
                    int spanLen = recipient.substring(start, i).length();
                    if (spanLen > i) {
                        i = spanLen;
                    }
                }

                i++;

                while ((i < len) && (recipient.charAt(i) == ' ')) {
                    i++;
                }

                start = i;
            } else {
                i++;
            }
        }

        return list;
    }

    /**
     * Returns true if the recipient in the uri matches the recipient list in this
     * conversation.
     */
    public boolean sameRecipient(Uri uri) {
        int size = mRecipients.size();
        if (size > 1) {
            return false;
        }
        if (uri == null) {
            return size == 0;
        }
        if (uri.getPathSegments().size() >= 2) {
            return false;       // it's a thread id for a conversation
        }
        String recipient = getRecipients(uri);
        ContactList incomingRecipient = ContactList.getByNumbers(recipient,
                false /* don't block */, false /* don't replace number */);
        return mRecipients.equals(incomingRecipient);
    }

    /**
     * Returns a temporary Conversation (not representing one on disk) wrapping
     * the contents of the provided cursor.  The cursor should be the one
     * returned to your AsyncQueryHandler passed in to {@link #startQueryForAll}.
     * The recipient list of this conversation can be empty if the results
     * were not in cache.
     */
    public static Conversation from(Context context, Cursor cursor) {
        // First look in the cache for the Conversation and return that one. That way, all the
        // people that are looking at the cached copy will get updated when fillFromCursor() is
        // called with this cursor.
        long threadId = cursor.getLong(ID);
        if (threadId > 0) {
            Conversation conv = Cache.get(threadId);
            if (conv != null) {
                fillFromCursor(context, conv, cursor, false);   // update the existing conv in-place
                return conv;
            }
        }
        Conversation conv = new Conversation(context, cursor, false);
        try {
            Cache.put(conv);
        } catch (IllegalStateException e) {
            LogTag.error("Tried to add duplicate Conversation to Cache");
        }
        return conv;
    }

    private void buildReadContentValues() {
        if (mReadContentValues == null) {
            mReadContentValues = new ContentValues(2);
            mReadContentValues.put("read", 1);
            mReadContentValues.put("seen", 1);
        }
    }
    
   //add for cb
    public synchronized int getChannelId() {
        if(mChannelId == -1) {
            setChannelIdFromDatabase();
        }
        return mChannelId;
    }
    
    private void setChannelIdFromDatabase() {
        Uri uri = ContentUris.withAppendedId(ADDRESS_ID_URI, mAddressId);
        Cursor c = mContext.getContentResolver().query(uri,
                ALL_ADDRESS_PROJECTION, null, null, null);
        try {
            if (c == null || c.getCount() == 0) {
                mChannelId = -1;
            } else {
                c.moveToFirst();
                // Name
                mChannelId = c.getInt(1);
            }
        } finally {
            if(c != null) {
                c.close();
            }
        }
    }
        
//    public synchronized String getDisplayName() {
//        String displayName = getChannelName();
//        if(displayName.equals(mContext.getString(R.string.cb_default_channel_name))) {
//            displayName = displayName + " " + getChannelId();
//        }
//        return displayName;
//    }
    
    public synchronized String getChannelName(int channelId, int simId) {
         setChannelNameFromDatabase(channelId, simId);
        return mChannelName;
    }

    private void setChannelNameFromDatabase(int channelId, int simId) {
        Uri uri = null;
        if (simId != 0) {
            uri = ContentUris.withAppendedId(CHANNEL1_ID_URI, channelId);
        } else {
            uri = ContentUris.withAppendedId(CHANNEL_ID_URI, channelId);
        }
        Cursor c = mContext.getContentResolver().query(uri,
                ALL_CHANNEL_PROJECTION, null, null, null);
        try {
            if (c == null || c.getCount() == 0) {
                mChannelName = mContext
                        .getString(R.string.cb_default_channel_name);
            } else {
                c.moveToFirst();
                mChannelName = c.getString(1);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
    
    //wappush: because of different uri
    public void WPMarkAsRead() {
        final Uri threadUri = ContentUris.withAppendedId(
                WapPush.CONTENT_URI_THREAD, mThreadId);
        new Thread(new Runnable() {
            public void run() {
                synchronized (mMarkAsBlockedSyncer) {
                    if (mMarkAsReadBlocked) {
                        try {
                            mMarkAsBlockedSyncer.wait();
                        } catch (InterruptedException e) {
                        }
                    }

                    if (threadUri != null) {
                        buildReadContentValues();

                        // Check the read flag first. It's much faster to do a query than
                        // to do an update. Timing this function show it's about 10x faster to
                        // do the query compared to the update, even when there's nothing to
                        // update.
                        boolean needUpdate = true;

                        Cursor c = mContext.getContentResolver().query(threadUri,
                                UNREAD_PROJECTION, UNREAD_SELECTION, null, null);
                        if (c != null) {
                            try {
                                needUpdate = c.getCount() > 0;
                            } finally {
                                c.close();
                            }
                        }

                        if(needUpdate){
                            mContext.getContentResolver().update(threadUri,mReadContentValues, UNREAD_SELECTION, null);
                            mHasUnreadMessages = false;
                        }
                    }
                }
                WapPushMessagingNotification.updateAllNotifications(mContext);
            }
        }).start();
    }

    /**
     * Marks all messages in this conversation as read and updates
     * relevant notifications.  This method returns immediately;
     * work is dispatched to a background thread.
     */
    public void markAsRead() {
        // If we have no Uri to mark (as in the case of a conversation that
        // has not yet made its way to disk), there's nothing to do.
        final Uri threadUri = getUri();

        new Thread(new Runnable() {
            public void run() {
                synchronized(mMarkAsBlockedSyncer) {
                    if (mMarkAsReadBlocked) {
                        try {
                            mMarkAsBlockedSyncer.wait();
                        } catch (InterruptedException e) {
                        }
                    }

                    if (threadUri != null) {
                        buildReadContentValues();

                        // Check the read flag first. It's much faster to do a query than
                        // to do an update. Timing this function show it's about 10x faster to
                        // do the query compared to the update, even when there's nothing to
                        // update.
                        boolean needUpdate = true;

                        Cursor c = mContext.getContentResolver().query(threadUri,
                                UNREAD_PROJECTION, UNREAD_SELECTION, null, null);
                        if (c != null) {
                            try {
                                needUpdate = c.getCount() > 0;
                            } finally {
                                c.close();
                            }
                        }

                        if (needUpdate) {
                            LogTag.debug("markAsRead: update read/seen for thread uri: " +
                                    threadUri);
                            mContext.getContentResolver().update(threadUri, mReadContentValues,
                                    UNREAD_SELECTION, null);
                        }

                        setHasUnreadMessages(false);
                    }
                }

                // Always update notifications regardless of the read state.
                MessagingNotification.blockingUpdateAllNotifications(mContext);
            }
        }).start();
    }

    public void blockMarkAsRead(boolean block) {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            LogTag.debug("blockMarkAsRead: " + block);
        }

        synchronized(mMarkAsBlockedSyncer) {
            if (block != mMarkAsReadBlocked) {
                mMarkAsReadBlocked = block;
                if (!mMarkAsReadBlocked) {
                    mMarkAsBlockedSyncer.notifyAll();
                }
            }

        }
    }

    /**
     * Returns a content:// URI referring to this conversation,
     * or null if it does not exist on disk yet.
     */
    public synchronized Uri getUri() {
        if (mThreadId <= 0)
            return null;

        return ContentUris.withAppendedId(Threads.CONTENT_URI, mThreadId);
    }

    /**
     * Returns a content:// URI referring to this conversation,
     * or null if it does not exist on disk yet.
     */
    public synchronized Uri getQueryMsgUri() {
        if (mThreadId < 0)
            return null;

        return ContentUris.withAppendedId(Threads.CONTENT_URI, mThreadId);
    }

    /**
     * Return the Uri for all messages in the given thread ID.
     * @deprecated
     */
    public static Uri getUri(long threadId) {
        // TODO: Callers using this should really just have a Conversation
        // and call getUri() on it, but this guarantees no blocking.
        return ContentUris.withAppendedId(Threads.CONTENT_URI, threadId);
    }

    /**
     * Returns the thread ID of this conversation.  Can be zero if
     * {@link #ensureThreadId} has not been called yet.
     */
    public synchronized long getThreadId() {
        return mThreadId;
    }

    /**
     * Guarantees that the conversation has been created in the database.
     * This will make a blocking database call if it hasn't.
     *
     * @return The thread ID of this conversation in the database
     */
    public synchronized long ensureThreadId() {
        return ensureThreadId(false);
    }

    public synchronized long ensureThreadId(boolean scrubForMmsAddress) {
        if (DEBUG) {
            LogTag.debug("ensureThreadId before: " + mThreadId);
        }
        if (mThreadId <= 0) {
            mThreadId = getOrCreateThreadId(mContext, mRecipients, scrubForMmsAddress);
        }
        if (DEBUG) {
            LogTag.debug("ensureThreadId after: " + mThreadId);
        }

        return mThreadId;
    }

    public synchronized void clearThreadId() {
        // remove ourself from the cache
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            LogTag.debug("clearThreadId old threadId was: " + mThreadId + " now zero");
        }
        Cache.remove(mThreadId);
        DraftCache.getInstance().updateDraftStateInCache(mThreadId);
        mThreadId = 0;
    }

    /**
     * Sets the list of recipients associated with this conversation.
     * If called, {@link #ensureThreadId} must be called before the next
     * operation that depends on this conversation existing in the
     * database (e.g. storing a draft message to it).
     */
    public synchronized void setRecipients(ContactList list) {
        // remove the same contacts
        Set<Contact> contactSet = new HashSet<Contact>();
        contactSet.addAll(list);
        list.clear();
        list.addAll(contactSet);

        mRecipients = list;

        // Invalidate thread ID because the recipient set has changed.
        mThreadId = 0;
    }

    /**
     * Returns the recipient set of this conversation.
     */
    public synchronized ContactList getRecipients() {
        return mRecipients;
    }

    /**
     * Returns true if a draft message exists in this conversation.
     */
    public synchronized boolean hasDraft() {
        if (mThreadId <= 0)
            return false;

        return DraftCache.getInstance().hasDraft(mThreadId);
    }

    /**
     * Sets whether or not this conversation has a draft message.
     */
    public synchronized void setDraftState(boolean hasDraft) {
        if (mThreadId <= 0)
            return;

        DraftCache.getInstance().setDraftState(mThreadId, hasDraft);
    }

    /**
     * Returns the time of the last update to this conversation in milliseconds,
     * on the {@link System#currentTimeMillis} timebase.
     */
    public synchronized long getDate() {
        return mDate;
    }

    /**
     * Returns the number of messages in this conversation, excluding the draft
     * (if it exists).
     */
    public synchronized int getMessageCount() {
        return mMessageCount;
    }

    public synchronized int getUnreadMessageCount() {
        return mUnreadMessageCount;
    }
    
    public synchronized int getMessageStatus() {
        return mMessageStatus;
    }
    /**
     * Returns a snippet of text from the most recent message in the conversation.
     */
    public synchronized String getSnippet() {
        return mSnippet;
    }

    /**
     * Returns true if there are any unread messages in the conversation.
     */
    public boolean hasUnreadMessages() {
        synchronized (this) {
            return mHasUnreadMessages;
        }
    }

    private void setHasUnreadMessages(boolean flag) {
        synchronized (this) {
            mHasUnreadMessages = flag;
        }
    }

    /**
     * Returns true if any messages in the conversation have attachments.
     */
    public synchronized boolean hasAttachment() {
        return mHasAttachment;
    }
    
    //wappush: add getType function
    /**
     * Returns type of the thread.
     */
    public synchronized int getType() {
        return mType;
    }

    /**
     * Returns true if any messages in the conversation are in an error state.
     */
    public synchronized boolean hasError() {
        return mHasError;
    }

    private static long getOrCreateThreadId(Context context, ContactList list) {
        return getOrCreateThreadId(context, list, false);
    }

    private static long getOrCreateThreadId(Context context, ContactList list, final boolean scrubForMmsAddress) {
        HashSet<String> recipients = new HashSet<String>();
        Contact cacheContact = null;
        for (Contact c : list) {
            cacheContact = Contact.get(c.getNumber(), false);
            String number;
            if (cacheContact != null) {
                number = cacheContact.getNumber();
            } else {
                number = c.getNumber();
            }
            if (scrubForMmsAddress) {
                number = MessageUtils.parseMmsAddress(number);
            }
            
            if (!TextUtils.isEmpty(number) && !recipients.contains(number)) {
                recipients.add(number);
            }
        }
        long retVal = 0;
        try{
            retVal = Threads.getOrCreateThreadId(context, recipients);
        }catch(IllegalArgumentException e){
            LogTag.error("Can't get or create the thread id");
            return 0;
        }
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            LogTag.debug("[Conversation] getOrCreateThreadId for (%s) returned %d",
                    recipients, retVal);
        }

        return retVal;
    }

    /*
     * The primary key of a conversation is its recipient set; override
     * equals() and hashCode() to just pass through to the internal
     * recipient sets.
     */
    @Override
    public synchronized boolean equals(Object obj) {
        try {
            Conversation other = (Conversation)obj;
            return (mRecipients.equals(other.mRecipients));
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public synchronized int hashCode() {
        return mRecipients.hashCode();
    }

    @Override
    public synchronized String toString() {
        return String.format("[%s] (tid %d)", mRecipients.serialize(), mThreadId);
    }

    /**
     * Remove any obsolete conversations sitting around on disk. Obsolete threads are threads
     * that aren't referenced by any message in the pdu or sms tables.
     */
    public static void asyncDeleteObsoleteThreads(AsyncQueryHandler handler, int token) {
        handler.startDelete(token, null, Threads.OBSOLETE_THREADS_URI, null, null);
    }

    /**
     * Start a query for all conversations in the database on the specified
     * AsyncQueryHandler.
     *
     * @param handler An AsyncQueryHandler that will receive onQueryComplete
     *                upon completion of the query
     * @param token   The token that will be passed to onQueryComplete
     */
    public static void startQueryForAll(AsyncQueryHandler handler, int token) {
        handler.cancelOperation(token);
        final int queryToken = token;
        final AsyncQueryHandler queryHandler = handler;
        // This query looks like this in the log:
        // I/Database(  147): elapsedTime4Sql|/data/data/com.android.providers.telephony/databases/
        // mmssms.db|2.253 ms|SELECT _id, date, message_count, recipient_ids, snippet, snippet_cs,
        // read, error, has_attachment FROM threads ORDER BY  date DESC
        queryHandler.postDelayed(new Runnable() {
            public void run() {
                queryHandler.startQuery(
                        queryToken, null, sAllThreadsUri,
                        ALL_THREADS_PROJECTION, null, null, Conversations.DEFAULT_SORT_ORDER);
            }
        }, 10);
        //handler.startQuery(token, null, sAllThreadsUri,
        //        ALL_THREADS_PROJECTION, null, null, Conversations.DEFAULT_SORT_ORDER);
    }

    /**
     * Start a delete of the conversation with the specified thread ID.
     *
     * @param handler An AsyncQueryHandler that will receive onDeleteComplete
     *                upon completion of the conversation being deleted
     * @param token   The token that will be passed to onDeleteComplete
     * @param deleteAll Delete the whole thread including locked messages
     * @param threadId Thread ID of the conversation to be deleted
     */
    public static void startDelete(AsyncQueryHandler handler, int token, boolean deleteAll,
            long threadId) {
        //wappush: do not need modify the code here, but delete function in provider has been modified.
        Uri uri = ContentUris.withAppendedId(Threads.CONTENT_URI, threadId);
        String selection = deleteAll ? null : "locked=0";
        handler.startDelete(token, null, uri, selection, null);
    }

    /**
     * Start deleting all conversations in the database.
     * @param handler An AsyncQueryHandler that will receive onDeleteComplete
     *                upon completion of all conversations being deleted
     * @param token   The token that will be passed to onDeleteComplete
     * @param deleteAll Delete the whole thread including locked messages
     */
    public static void startDeleteAll(AsyncQueryHandler handler, int token, boolean deleteAll) {
        //wappush: do not need modify the code here, but delete function in provider has been modified.
        String selection = deleteAll ? null : "locked=0";
        handler.startDelete(token, null, Threads.CONTENT_URI, selection, null);
    }

    /**
     * Check for locked messages in all threads or a specified thread.
     * @param handler An AsyncQueryHandler that will receive onQueryComplete
     *                upon completion of looking for locked messages
     * @param threadId   The threadId of the thread to search. -1 means all threads
     * @param token   The token that will be passed to onQueryComplete
     */
    public static void startQueryHaveLockedMessages(AsyncQueryHandler handler, long threadId,
            int token) {
        handler.cancelOperation(token);
        Uri uri = MmsSms.CONTENT_LOCKED_URI;
        if (threadId != -1) {
            uri = ContentUris.withAppendedId(uri, threadId);
        }
        handler.startQuery(token, new Long(threadId), uri,
                ALL_THREADS_PROJECTION, null, null, Conversations.DEFAULT_SORT_ORDER);
    }

    /**
     * Fill the specified conversation with the values from the specified
     * cursor, possibly setting recipients to empty if {@value allowQuery}
     * is false and the recipient IDs are not in cache.  The cursor should
     * be one made via {@link #startQueryForAll}.
     */
    private static void fillFromCursor(Context context, Conversation conv,
                                       Cursor c, boolean allowQuery) {
        synchronized (conv) {
            conv.mThreadId = c.getLong(ID);
            conv.mDate = c.getLong(DATE);
            conv.mMessageCount = c.getInt(MESSAGE_COUNT);

            // Replace the snippet with a default value if it's empty.
            String snippet = MessageUtils.extractEncStrFromCursor(c, SNIPPET, SNIPPET_CS);
            if (TextUtils.isEmpty(snippet)) {
                snippet = context.getString(R.string.no_subject_view);
            }
            conv.mSnippet = snippet;

            conv.setHasUnreadMessages(c.getInt(READ) == 0);
            conv.mHasError = (c.getInt(ERROR) != 0);
            conv.mHasAttachment = (c.getInt(HAS_ATTACHMENT) != 0);
            
            //wappush: get the value for mType
            conv.mType = c.getInt(TYPE);
            conv.mMessageStatus = c.getInt(STATUS);
        }
        // Fill in as much of the conversation as we can before doing the slow stuff of looking
        // up the contacts associated with this conversation.
        String recipientIds = c.getString(RECIPIENT_IDS);
        ContactList recipients = ContactList.getByIds(recipientIds, allowQuery);
        synchronized (conv) {
            conv.mRecipients = recipients;
            conv.mUnreadMessageCount = conv.mMessageCount - c.getInt(READCOUNT);
//            if (conv.mType == Threads.WAPPUSH_THREAD) {
//                if(FeatureOption.MTK_WAPPUSH_SUPPORT){
//                    Cursor cc = mContext.getContentResolver().query(WapPush.CONTENT_URI,
//                            UNREAD_PROJECTION,
//                            "(thread_id=" + conv.mThreadId +") AND " + READ_SELECTION,
//                            null,
//                            null);
//                    if (cc!=null) {
//                        conv.mUnreadMessageCount = conv.mMessageCount - cc.getCount();
//                        cc.close();
//                    }
//                }
//            } else {
//                Log.d("this","conv.mType != Threads.WAPPUSH_THREA");
//                Cursor cc = mContext.getContentResolver().query(ContentUris.withAppendedId(Threads.CONTENT_URI, conv.mThreadId),
//                        UNREAD_PROJECTION, READ_SELECTION, null, null);
//                if (cc!=null) {
//                    conv.mUnreadMessageCount = conv.mMessageCount - cc.getCount();
//                    cc.close();
//                }
//            }  
        }

        if (Log.isLoggable(LogTag.THREAD_CACHE, Log.VERBOSE)) {
            LogTag.debug("fillFromCursor: conv=" + conv + ", recipientIds=" + recipientIds);
        }
    }

    /**
     * Private cache for the use of the various forms of Conversation.get.
     */
    private static class Cache {
        private static Cache sInstance = new Cache();
        static Cache getInstance() { return sInstance; }
        private final HashSet<Conversation> mCache;
        private Cache() {
            mCache = new HashSet<Conversation>(10);
        }

        /**
         * Return the conversation with the specified thread ID, or
         * null if it's not in cache.
         */
        static Conversation get(long threadId) {
            synchronized (sInstance) {
                if (Log.isLoggable(LogTag.THREAD_CACHE, Log.VERBOSE)) {
                    LogTag.debug("Conversation get with threadId: " + threadId);
                }
                for (Conversation c : sInstance.mCache) {
                    if (DEBUG) {
                        LogTag.debug("Conversation get() threadId: " + threadId +
                                " c.getThreadId(): " + c.getThreadId());
                    }
                    if (c.getThreadId() == threadId) {
                        return c;
                    }
                }
            }
            return null;
        }

        /**
         * Return the conversation with the specified recipient
         * list, or null if it's not in cache.
         */
        static Conversation get(ContactList list) {
            synchronized (sInstance) {
                if (Log.isLoggable(LogTag.THREAD_CACHE, Log.VERBOSE)) {
                    LogTag.debug("Conversation get with ContactList: " + list);
                }
                for (Conversation c : sInstance.mCache) {
                    if (c.getRecipients().equals(list)) {
                        return c;
                    }
                }
            }
            return null;
        }

        /**
         * Put the specified conversation in the cache.  The caller
         * should not place an already-existing conversation in the
         * cache, but rather update it in place.
         */
        static void put(Conversation c) {
            synchronized (sInstance) {
                // We update cache entries in place so people with long-
                // held references get updated.
                if (Log.isLoggable(LogTag.THREAD_CACHE, Log.VERBOSE)) {
                    LogTag.debug("Conversation.Cache.put: conv= " + c + ", hash: " + c.hashCode());
                }

                if (sInstance.mCache.contains(c)) {
                    throw new IllegalStateException("cache already contains " + c +
                            " threadId: " + c.mThreadId);
                }
                sInstance.mCache.add(c);
            }
        }

        static void remove(long threadId) {
            if (DEBUG) {
                LogTag.debug("remove threadid: " + threadId);
                dumpCache();
            }
            for (Conversation c : sInstance.mCache) {
                if (c.getThreadId() == threadId) {
                    sInstance.mCache.remove(c);
                    return;
                }
            }
        }

        static void dumpCache() {
            synchronized (sInstance) {
                LogTag.debug("Conversation dumpCache: ");
                for (Conversation c : sInstance.mCache) {
                    LogTag.debug("   conv: " + c.toString() + " hash: " + c.hashCode());
                }
            }
        }

        /**
         * Remove all conversations from the cache that are not in
         * the provided set of thread IDs.
         */
        static void keepOnly(Set<Long> threads) {
            synchronized (sInstance) {
                Iterator<Conversation> iter = sInstance.mCache.iterator();
                Conversation c = null;
                while (iter.hasNext()) {
                    c = iter.next();
                    if (!threads.contains(c.getThreadId())) {
                        iter.remove();
                    }
                }
            }
            if (DEBUG) {
                LogTag.debug("after keepOnly");
                dumpCache();
            }
        }
    }

    /**
     * Set up the conversation cache.  To be called once at application
     * startup time.
     */
    public static void init(final Context context) {
        new Thread(new Runnable() {
            public void run() {
                cacheAllThreads(context);
            }
        }).start();
    }

    public static void markAllConversationsAsSeen(final Context context) {
        if (DEBUG) {
            LogTag.debug("Conversation.markAllConversationsAsSeen");
        }

        new Thread(new Runnable() {
            public void run() {
                blockingMarkAllSmsMessagesAsSeen(context);
                blockingMarkAllMmsMessagesAsSeen(context);
                blockingMarkAllCellBroadcastMessagesAsSeen(context);

                // Always update notifications regardless of the read state.
                MessagingNotification.blockingUpdateAllNotifications(context);
                
                if(FeatureOption.MTK_WAPPUSH_SUPPORT){
                    blockingMarkAllWapPushMessagesAsSeen(context);
                    WapPushMessagingNotification.blockingUpdateNewMessageIndicator(context,false);
                }
            }
        }).start();
    }

    private static void blockingMarkAllSmsMessagesAsSeen(final Context context) {
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(Sms.Inbox.CONTENT_URI,
                SEEN_PROJECTION,
                "seen=0",
                null,
                null);

        int count = 0;

        if (cursor != null) {
            try {
                count = cursor.getCount();
            } finally {
                cursor.close();
            }
        }

        if (count == 0) {
            return;
        }

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            Log.d(TAG, "mark " + count + " SMS msgs as seen");
        }

        ContentValues values = new ContentValues(1);
        values.put("seen", 1);

        resolver.update(Sms.Inbox.CONTENT_URI,
                values,
                "seen=0",
                null);
    }

    private static void blockingMarkAllMmsMessagesAsSeen(final Context context) {
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(Mms.Inbox.CONTENT_URI,
                SEEN_PROJECTION,
                "seen=0",
                null,
                null);

        int count = 0;

        if (cursor != null) {
            try {
                count = cursor.getCount();
            } finally {
                cursor.close();
            }
        }

        if (count == 0) {
            return;
        }

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            Log.d(TAG, "mark " + count + " MMS msgs as seen");
        }

        ContentValues values = new ContentValues(1);
        values.put("seen", 1);

        resolver.update(Mms.Inbox.CONTENT_URI,
                values,
                "seen=0",
                null);

    }
    
    //mark all wappush message as seen
    private static void blockingMarkAllWapPushMessagesAsSeen(final Context context) {
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(WapPush.CONTENT_URI,
                SEEN_PROJECTION,
                "seen=0",
                null,
                null);

        int count = 0;

        if (cursor != null) {
            try {
                count = cursor.getCount();
            } finally {
                cursor.close();
            }
        }

        if (count == 0) {
            return;
        }

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            Xlog.d(TAG, "mark " + count + " MMS msgs as seen");
        }

        ContentValues values = new ContentValues(1);
        values.put("seen", 1);

        resolver.update(WapPush.CONTENT_URI,
                values,
                "seen=0",
                null);

    }

    //mark all CellBroadcast message as seen
    private static void blockingMarkAllCellBroadcastMessagesAsSeen(final Context context) {
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(Telephony.CbSms.CONTENT_URI,
                SEEN_PROJECTION,
                "seen=0",
                null,
                null);

        int count = 0;

        if (cursor != null) {
            try {
                count = cursor.getCount();
            } finally {
                cursor.close();
            }
        }

        if (count == 0) {
            return;
        }

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            Xlog.d(TAG, "mark " + count + " CB msgs as seen");
        }

        ContentValues values = new ContentValues(1);
        values.put("seen", 1);

        resolver.update(Telephony.CbSms.CONTENT_URI,
                values,
                "seen=0",
                null);
    }
    
    /**
     * Are we in the process of loading and caching all the threads?.
     */
    public static boolean loadingThreads() {
        synchronized (Cache.getInstance()) {
            return mLoadingThreads;
        }
    }

    private static void cacheAllThreads(Context context) {
        if (Log.isLoggable(LogTag.THREAD_CACHE, Log.VERBOSE)) {
            LogTag.debug("[Conversation] cacheAllThreads: begin");
        }
        synchronized (Cache.getInstance()) {
            if (mLoadingThreads) {
                return;
                }
            mLoadingThreads = true;
        }

        // Keep track of what threads are now on disk so we
        // can discard anything removed from the cache.
        HashSet<Long> threadsOnDisk = new HashSet<Long>();

        // Query for all conversations.
        Cursor c = context.getContentResolver().query(sAllThreadsUri,
                ALL_THREADS_PROJECTION, null, null, null);
        try {
            if (c != null) {
                long threadId = 0L;
                while (c.moveToNext()) {
                    threadId = c.getLong(ID);
                    threadsOnDisk.add(threadId);

                    // Try to find this thread ID in the cache.
                    Conversation conv;
                    synchronized (Cache.getInstance()) {
                        conv = Cache.get(threadId);
                    }

                    if (conv == null) {
                        // Make a new Conversation and put it in
                        // the cache if necessary.
                        conv = new Conversation(context, c, true);
                        try {
                            synchronized (Cache.getInstance()) {
                                Cache.put(conv);
                            }
                        } catch (IllegalStateException e) {
                            LogTag.error("Tried to add duplicate Conversation to Cache");
                        }
                    } else {
                        // Or update in place so people with references
                        // to conversations get updated too.
                        fillFromCursor(context, conv, c, true);
                    }
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
            synchronized (Cache.getInstance()) {
                mLoadingThreads = false;
            }
        }

        // Purge the cache of threads that no longer exist on disk.
        Cache.keepOnly(threadsOnDisk);

        if (Log.isLoggable(LogTag.THREAD_CACHE, Log.VERBOSE)) {
            LogTag.debug("[Conversation] cacheAllThreads: finished");
            Cache.dumpCache();
        }
    }

    private boolean loadFromThreadId(long threadId, boolean allowQuery) {
        Cursor c = mContext.getContentResolver().query(sAllThreadsUri, ALL_THREADS_PROJECTION,
                "_id=" + Long.toString(threadId), null, null);
        try {
            if (c.moveToFirst()) {
                fillFromCursor(mContext, this, c, allowQuery);

                if (threadId != mThreadId) {
                    LogTag.error("loadFromThreadId: fillFromCursor returned differnt thread_id!" +
                            " threadId=" + threadId + ", mThreadId=" + mThreadId);
                }
            } else {
                LogTag.error("loadFromThreadId: Can't find thread ID " + threadId);
                return false;
            }
        } finally {
            c.close();
        }
        return true;
    }
    
    static public String getChannelNameFromId(Context context, int channel_id) {
        String name = null;
        Uri uri = ContentUris.withAppendedId(CHANNEL_ID_URI, channel_id);
        Cursor c = context.getContentResolver().query(uri,
                ALL_CHANNEL_PROJECTION, null, null, null);
        try {
            if (c == null || c.getCount() == 0) {
                name = context.getString(R.string.cb_default_channel_name);
            } else {
                c.moveToFirst();
                name = c.getString(1);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return name;
    }

    static public void removeInvalidCache(Cursor cursor) {
        if (cursor != null) {
            synchronized (Cache.getInstance()) {
                HashSet<Long> threadsOnDisk = new HashSet<Long>();

                while (cursor.moveToNext()) {
                    long threadId = cursor.getLong(ID);

                    threadsOnDisk.add(threadId);
                }

                Cache.keepOnly(threadsOnDisk);
            }
        }
    }

    private static String getRecipients(Uri uri) {
        String base = uri.getSchemeSpecificPart();
        int pos = base.indexOf('?');
        return (pos == -1) ? base : base.substring(0, pos);
    }
}
