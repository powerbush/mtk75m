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

/**
 * Copyright (c) 2009, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mms.ui;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.mms.MmsConfig;
import com.android.mms.MmsApp;
import com.android.mms.R;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;

import android.provider.Telephony;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.mms.data.Contact;
import com.android.mms.data.Contact.UpdateListener;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.util.DraftCache;

import android.provider.Telephony.Threads;
import android.util.Log;
import com.mediatek.xlog.Xlog;
import com.mediatek.featureoption.FeatureOption;

/***
 * Presents a List of search results.  Each item in the list represents a thread which
 * matches.  The item contains the contact (or phone number) as the "title" and a
 * snippet of what matches, below.  The snippet is taken from the most recent part of
 * the conversation that has a match.  Each match within the visible portion of the
 * snippet is highlighted.
 */

public class SearchActivity extends ListActivity implements DraftCache.OnDraftChangedListener {
    private AsyncQueryHandler mQueryHandler;

    // Track which TextView's show which Contact objects so that we can update
    // appropriately when the Contact gets fully loaded.
    private HashMap<Contact, TextView> mContactMap = new HashMap<Contact, TextView>();


    /*
     * Subclass of TextView which displays a snippet of text which matches the full text and
     * highlights the matches within the snippet.
     */
    
    private static final String WP_TAG = "Mms/WapPush";
    private static final String TAG = "SearchActivity";
    private String searchString;
    public static class TextViewSnippet extends TextView {
        private static String sEllipsis = "\u2026";

        private static int sTypefaceHighlight = Typeface.BOLD;

        private String mFullText;
        private String mTargetString;
        private Pattern mPattern;

        public TextViewSnippet(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public TextViewSnippet(Context context) {
            super(context);
        }

        public TextViewSnippet(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        /**
         * We have to know our width before we can compute the snippet string.  Do that
         * here and then defer to super for whatever work is normally done.
         */
        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            String fullTextLower = mFullText.toLowerCase();
            String targetStringLower = mTargetString.toLowerCase();

            int startPos = 0;
            int searchStringLength = targetStringLower.length();
            int bodyLength = fullTextLower.length();

            Matcher m = mPattern.matcher(mFullText);
            if (m.find(0)) {
                startPos = m.start();
            }

            TextPaint tp = getPaint();

            float searchStringWidth = tp.measureText(mTargetString);
            float textFieldWidth = getWidth();

            String snippetString = null;
            if (searchStringWidth > textFieldWidth) {
                snippetString = mFullText.substring(startPos, startPos + searchStringLength);
            } else {
                float ellipsisWidth = tp.measureText(sEllipsis);
                textFieldWidth -= (2F * ellipsisWidth); // assume we'll need one on both ends

                int offset = -1;
                int start = -1;
                int end = -1;
                /* TODO: this code could be made more efficient by only measuring the additional
                 * characters as we widen the string rather than measuring the whole new
                 * string each time.
                 */
                int newstart = 0;
                int newend = 0;
                while (true) {
                    offset += 1;

                    newstart = Math.max(0, startPos - offset);
                    newend = Math.min(bodyLength, startPos + searchStringLength + offset);

                    if (newstart == start && newend == end) {
                        // if we couldn't expand out any further then we're done
                        break;
                    }
                    start = newstart;
                    end = newend;

                    // pull the candidate string out of the full text rather than body
                    // because body has been toLower()'ed
                    String candidate = mFullText.substring(start, end);
                    if (tp.measureText(candidate) > textFieldWidth) {
                        // if the newly computed width would exceed our bounds then we're done
                        // do not use this "candidate"
                        
                        if (snippetString == null) {
                            /*
                             * ALPS00074310, logic error.
                             * It is null when the loop break at the first time. Logic is error when
                             * textFieldWidth - 2 * ellipsis < targetWidth < textFieldWidth
                             * set the snippetString as same as when longer than textFieldWidth.
                             */
                            snippetString = mFullText.substring(startPos, startPos + searchStringLength);
                        }
                        break;
                    }

                    snippetString = String.format(
                            "%s%s%s",
                            start == 0 ? "" : sEllipsis,
                            candidate,
                            end == bodyLength ? "" : sEllipsis);
                }
            }

            SpannableString spannable = new SpannableString(snippetString);
            int start = 0;

            m = mPattern.matcher(snippetString);
            while (m.find(start)) {
                spannable.setSpan(new StyleSpan(sTypefaceHighlight), m.start(), m.end(), 0);
                start = m.end();
            }
            setText(spannable);

            // do this after the call to setText() above
            super.onLayout(changed, left, top, right, bottom);
        }

        public void setText(String fullText, String target) {
            // Use a regular expression to locate the target string
            // within the full text.  The target string must be
            // found as a word start so we use \b which matches
            // word boundaries.
            String patternString = "\\b" + Pattern.quote(target);
            mPattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);

            mFullText = fullText;
            mTargetString = target;
            requestLayout();
        }
    }

    Contact.UpdateListener mContactListener = new Contact.UpdateListener() {
        public void onUpdate(Contact updated) {
            TextView tv = mContactMap.get(updated);
            if (tv != null) {
                tv.setText(updated.getNameAndNumber());
            }
        }
    };

    @Override
    public void onStop() {
        super.onStop();
        Xlog.d(TAG,"onStop");
        Contact.removeListener(mContactListener);
        DraftCache.getInstance().removeOnDraftChangedListener(this);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Xlog.d(TAG,"onCreate");
        setContentView(R.layout.search_activity);

        String searchStringParameter = getIntent().getStringExtra(SearchManager.QUERY);
        if (searchStringParameter == null) {
            searchStringParameter = getIntent().getStringExtra("intent_extra_data_key" /*SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA*/);
        }
        searchString = searchStringParameter != null ? searchStringParameter.trim() : searchStringParameter;
        ContentResolver cr = getContentResolver();

        searchStringParameter = searchStringParameter.trim();
        final ListView listView = getListView();
        listView.setItemsCanFocus(true);
        listView.setFocusable(true);
        listView.setClickable(true);

        // I considered something like "searching..." but typically it will
        // flash on the screen briefly which I found to be more distracting
        // than beneficial.
        // This gets updated when the query completes.
        setTitle("");

        Contact.addListener(mContactListener);

        // When the query completes cons up a new adapter and set our list adapter to that.
        mQueryHandler = new AsyncQueryHandler(cr) {
            protected void onQueryComplete(int token, Object cookie, Cursor c) {
                if (c == null) {
                    return;
                }
                final int threadIdPos = c.getColumnIndex("thread_id");
                final int addressPos  = c.getColumnIndex("address");
                final int bodyPos     = c.getColumnIndex("body");
                final int rowidPos    = c.getColumnIndex("_id");

                int cursorCount = c.getCount();
                Xlog.d(TAG, "cursorCount =: " + cursorCount);
                setTitle(getResources().getQuantityString(
                        R.plurals.search_results_title,
                        cursorCount,
                        cursorCount,
                        searchString));

                // Note that we're telling the CursorAdapter not to do auto-requeries. If we
                // want to dynamically respond to changes in the search results,
                // we'll have have to add a setOnDataSetChangedListener().
                setListAdapter(new CursorAdapter(SearchActivity.this,
                        c, false /* no auto-requery */) {
                    @Override
                    public void bindView(View view, Context context, Cursor cursor) {
                        final TextView title = (TextView)(view.findViewById(R.id.title));
                        final TextViewSnippet snippet = (TextViewSnippet)(view.findViewById(R.id.subtitle));

                        String address = cursor.getString(addressPos);
                        Contact contact = address != null ? Contact.get(address, false) : null;

                        String titleString = contact != null ? contact.getNameAndNumber() : "";
                        title.setText(titleString);

                        snippet.setText(cursor.getString(bodyPos), searchString);

                        // if the user touches the item then launch the compose message
                        // activity with some extra parameters to highlight the search
                        // results and scroll to the latest part of the conversation
                        // that has a match.
                        final long threadId = cursor.getLong(threadIdPos);
                        final long rowid = cursor.getLong(rowidPos);

                        view.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                            	Intent onClickIntent= null;
                            	if(FeatureOption.MTK_WAPPUSH_SUPPORT == true){
                            		//wappush: add type
                                	int threadType = 0;
                                	String where = Threads._ID + "=" + threadId;
                                    String[] projection = new String[] { Threads.TYPE };
                                    Cursor queryCursor = getContentResolver().query(
                                    		Threads.CONTENT_URI.buildUpon().appendQueryParameter("simple", "true").build(),
                                            projection, where, null, null);
                                    if(null != queryCursor){
                                    	try {
	                                        if (queryCursor.moveToFirst()) {
	                                    	    threadType = queryCursor.getInt(0);
	                                            Xlog.i(WP_TAG, "SearchActivity: " + "threadType is : " + threadType);
	                                        }
                                    	} finally {
                                    		queryCursor.close();
                                    	}
                                    }else{
                                    	Xlog.w(WP_TAG, "SearchActivity: " + "Thread Id queryCursor is null.");
                                    }
                                    if(Threads.WAPPUSH_THREAD == threadType){
                                    	Xlog.i(WP_TAG, "SearchActivity: " + "onClickIntent WPMessageActivity.");
                                    	onClickIntent = new Intent(SearchActivity.this, WPMessageActivity.class);
                                    }else{
                                    	Xlog.i(WP_TAG, "SearchActivity: " + "onClickIntent ComposeMessageActivity.");
                                    	onClickIntent = new Intent(SearchActivity.this, ComposeMessageActivity.class);
                                    }
                            	}else{
                            		onClickIntent = new Intent(SearchActivity.this, ComposeMessageActivity.class);
                            	}                            	
                                onClickIntent.putExtra("thread_id", threadId);
                                onClickIntent.putExtra("highlight", searchString);
                                onClickIntent.putExtra("select_id", rowid);
                                startActivity(onClickIntent);
                            }
                        });
                    }

                    @Override
                    public View newView(Context context, Cursor cursor, ViewGroup parent) {
                        LayoutInflater inflater = LayoutInflater.from(context);
                        View v = inflater.inflate(R.layout.search_item, parent, false);
                        return v;
                    }

                });

                // ListView seems to want to reject the setFocusable until such time
                // as the list is not empty.  Set it here and request focus.  Without
                // this the arrow keys (and trackball) fail to move the selection.
                listView.setFocusable(true);
                listView.setFocusableInTouchMode(true);
                listView.requestFocus();

                // Remember the query if there are actual results
                if (cursorCount > 0) {
                    SearchRecentSuggestions recent = ((MmsApp)getApplication()).getRecentSuggestions();
                    if (recent != null) {
                        recent.saveRecentQuery(searchString, getString(R.string.search_history, cursorCount, searchString));
                    }
                }
            }
        };

        // don't pass a projection since the search uri ignores it
        Uri uri = Telephony.MmsSms.SEARCH_URI.buildUpon().appendQueryParameter("pattern", searchString).build();

        // kick off a query for the threads which match the search string
        mQueryHandler.startQuery(0, null, uri, null, null, null, null);

    }

    @Override
    protected void onStart(){
        super.onStart();
        DraftCache.getInstance().addOnDraftChangedListener(this);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Xlog.d(TAG, "onResume");
        Uri uri = Telephony.MmsSms.SEARCH_URI.buildUpon().appendQueryParameter("pattern", searchString).build();
        mQueryHandler.startQuery(0, null, uri, null, null, null, null);
    }

    public void onDraftChanged(final long threadId, final boolean hasDraft) {
        Xlog.d(TAG, "onDraftChanged hasDraft = " + hasDraft + " threadId = " + threadId); 
        mQueryHandler.postDelayed(new Runnable() {
            public void run() {
                final Uri uri = Telephony.MmsSms.SEARCH_URI.buildUpon().appendQueryParameter("pattern", searchString).build();
                mQueryHandler.startQuery(0, null, uri, null, null, null, null);
            }
        }, 100);
    }
}
