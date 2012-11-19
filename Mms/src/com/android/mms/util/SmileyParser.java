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

/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.mms.util;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;

import com.android.mms.R;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class for annotating a CharSequence with spans to convert textual emoticons
 * to graphical ones.
 */
public class SmileyParser {
    // Singleton stuff
    private static SmileyParser sInstance;
    public static SmileyParser getInstance() { return sInstance; }
    public static void init(Context context) {
        sInstance = new SmileyParser(context);
    }

    private final Context mContext;
    private final String[] mSmileyTexts;
    private final Pattern mPattern;
    private final HashMap<String, Integer> mSmileyToRes;

    private SmileyParser(Context context) {
        mContext = context;
        mSmileyTexts = mContext.getResources().getStringArray(DEFAULT_SMILEY_TEXTS);
        mSmileyToRes = buildSmileyToRes();
        mPattern = buildPattern();
    }

    static class Smileys {
        private static final int[] sIconIds = {
            R.drawable.emo_im_happy,
            R.drawable.emo_im_sad,
            R.drawable.emo_im_winking,
            R.drawable.emo_im_tongue_sticking_out,
            R.drawable.emo_im_surprised,
            R.drawable.emo_im_kissing,
            R.drawable.emo_im_yelling,
            R.drawable.emo_im_cool,
            R.drawable.emo_im_money_mouth,
            R.drawable.emo_im_foot_in_mouth,
            R.drawable.emo_im_embarrassed,
            R.drawable.emo_im_angel,
            R.drawable.emo_im_undecided,
            R.drawable.emo_im_crying,
            R.drawable.emo_im_lips_are_sealed,
            R.drawable.emo_im_laughing,
            R.drawable.emo_im_wtf
        };

        public static int HAPPY = 0;
        public static int SAD = 1;
        public static int WINKING = 2;
        public static int TONGUE_STICKING_OUT = 3;
        public static int SURPRISED = 4;
        public static int KISSING = 5;
        public static int YELLING = 6;
        public static int COOL = 7;
        public static int MONEY_MOUTH = 8;
        public static int FOOT_IN_MOUTH = 9;
        public static int EMBARRASSED = 10;
        public static int ANGEL = 11;
        public static int UNDECIDED = 12;
        public static int CRYING = 13;
        public static int LIPS_ARE_SEALED = 14;
        public static int LAUGHING = 15;
        public static int WTF = 16;

        public static int getSmileyResource(int which) {
            return sIconIds[which];
        }
    }

    // NOTE: if you change anything about this array, you must make the corresponding change
    // to the string arrays: default_smiley_texts and default_smiley_names in res/values/arrays.xml
    public static final int[] DEFAULT_SMILEY_RES_IDS = {
        Smileys.getSmileyResource(Smileys.HAPPY),                //  0
        Smileys.getSmileyResource(Smileys.SAD),                  //  1
        Smileys.getSmileyResource(Smileys.WINKING),              //  2
        Smileys.getSmileyResource(Smileys.TONGUE_STICKING_OUT),  //  3
        Smileys.getSmileyResource(Smileys.SURPRISED),            //  4
        Smileys.getSmileyResource(Smileys.KISSING),              //  5
        Smileys.getSmileyResource(Smileys.YELLING),              //  6
        Smileys.getSmileyResource(Smileys.COOL),                 //  7
        Smileys.getSmileyResource(Smileys.MONEY_MOUTH),          //  8
        Smileys.getSmileyResource(Smileys.FOOT_IN_MOUTH),        //  9
        Smileys.getSmileyResource(Smileys.EMBARRASSED),          //  10
        Smileys.getSmileyResource(Smileys.ANGEL),                //  11
        Smileys.getSmileyResource(Smileys.UNDECIDED),            //  12
        Smileys.getSmileyResource(Smileys.CRYING),               //  13
        Smileys.getSmileyResource(Smileys.LIPS_ARE_SEALED),      //  14
        Smileys.getSmileyResource(Smileys.LAUGHING),             //  15
        Smileys.getSmileyResource(Smileys.WTF),                   //  16
    };

    public static final int DEFAULT_SMILEY_TEXTS = R.array.default_smiley_texts;
    public static final int DEFAULT_SMILEY_NAMES = R.array.default_smiley_names;

    /**
     * Builds the hashtable we use for mapping the string version
     * of a smiley (e.g. ":-)") to a resource ID for the icon version.
     */
    private HashMap<String, Integer> buildSmileyToRes() {
        if (DEFAULT_SMILEY_RES_IDS.length != mSmileyTexts.length) {
            // Throw an exception if someone updated DEFAULT_SMILEY_RES_IDS
            // and failed to update arrays.xml
            throw new IllegalStateException("Smiley resource ID/text mismatch");
        }

        HashMap<String, Integer> smileyToRes =
                            new HashMap<String, Integer>(mSmileyTexts.length);
        for (int i = 0; i < mSmileyTexts.length; i++) {
            smileyToRes.put(mSmileyTexts[i], DEFAULT_SMILEY_RES_IDS[i]);
        }

        return smileyToRes;
    }

    /**
     * Builds the regular expression we use to find smileys in {@link #addSmileySpans}.
     */
    private Pattern buildPattern() {
        // Set the StringBuilder capacity with the assumption that the average
        // smiley is 3 characters long.
        StringBuilder patternString = new StringBuilder(mSmileyTexts.length * 3);

        // Build a regex that looks like (:-)|:-(|...), but escaping the smilies
        // properly so they will be interpreted literally by the regex matcher.
        patternString.append('(');
        for (String s : mSmileyTexts) {
            patternString.append(Pattern.quote(s));
            patternString.append('|');
        }
        // Replace the extra '|' with a ')'
        patternString.replace(patternString.length() - 1, patternString.length(), ")");

        return Pattern.compile(patternString.toString());
    }


    /**
     * Adds ImageSpans to a CharSequence that replace textual emoticons such
     * as :-) with a graphical version.
     *
     * @param text A CharSequence possibly containing emoticons
     * @return A CharSequence annotated with ImageSpans covering any
     *         recognized emoticons.
     */
    public CharSequence addSmileySpans(CharSequence text) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);

        Matcher matcher = mPattern.matcher(text);
        int resId = 0;
        while (matcher.find()) {
        	resId = mSmileyToRes.get(matcher.group());
            builder.setSpan(new ImageSpan(mContext, resId),
                            matcher.start(), matcher.end(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return builder;
    }
}


