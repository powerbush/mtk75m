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
 * Copyright (C) 2007 Esmertec AG.
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.mms.dom;

import java.util.ArrayList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NodeListImpl implements NodeList {
    private ArrayList<Node> mSearchNodes;
    private ArrayList<Node> mStaticNodes;
    private Node mRootNode;
    private String mTagName;
    private boolean mDeepSearch;

    /*
     * Internal Interface
     */

    /**
     * Constructs a NodeList by searching for all descendants or the direct
     * children of a root node with a given tag name.
     * @param rootNode The root <code>Node</code> of the search.
     * @param tagName The tag name to be searched for. If null, all descendants
     *              will be returned.
     * @param deep Limit the search to the direct children of rootNode if false,
     *              to all descendants otherwise.
     */
    public NodeListImpl(Node rootNode, String tagName, boolean deepSearch) {
        mRootNode = rootNode;
        mTagName  = tagName;
        mDeepSearch = deepSearch;
    }

    /**
     * Constructs a NodeList for a given static node list.
     * @param nodes The static node list.
     */
    public NodeListImpl(ArrayList<Node> nodes) {
        mStaticNodes = nodes;
    }

    /*
     * NodeListImpl Interface
     */

    public int getLength() {
        if (mStaticNodes == null) {
            fillList(mRootNode);
            return mSearchNodes.size();
        } else {
            return mStaticNodes.size();
        }
    }

    public Node item(int index) {
        Node node = null;
        if (mStaticNodes == null) {
            fillList(mRootNode);
            try {
                node = mSearchNodes.get(index);
            } catch (IndexOutOfBoundsException e) {
                // Do nothing and return null
            }
        } else {
            try {
                node = mStaticNodes.get(index);
            } catch (IndexOutOfBoundsException e) {
                // Do nothing and return null
            }
        }
        return node;
    }

    /**
     * A preorder traversal is done in the following order:
     * <ul>
     *   <li> Visit root.
     *   <li> Traverse children from left to right in preorder.
     * </ul>
     * This method fills the live node list.
     * @param The root of preorder traversal
     * @return The next match
     */
    private void fillList(Node node) {
        // (Re)-initialize the container if this is the start of the search.
        // Visit the root of this iteration otherwise.
        if (node == mRootNode) {
            mSearchNodes = new ArrayList<Node>();
        } else {
            if ((mTagName == null) || node.getNodeName().equals(mTagName)) {
                mSearchNodes.add(node);
            }
        }

        // Descend one generation...
        node = node.getFirstChild();

        // ...and visit in preorder the children if we are in deep search
        // or directly add the children to the list otherwise.
        while (node != null) {
            if (mDeepSearch) {
                fillList(node);
            } else {
                if ((mTagName == null) || node.getNodeName().equals(mTagName)) {
                    mSearchNodes.add(node);
                }
            }
            node = node.getNextSibling();
        }
    }
}
