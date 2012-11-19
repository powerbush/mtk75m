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

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;

public class ElementImpl extends NodeImpl implements Element {
    private String mTagName;
    private NamedNodeMap mAttributes = new NamedNodeMapImpl();

    /*
     * Internal methods
     */

    protected ElementImpl(DocumentImpl owner, String tagName) {
        super(owner);
        mTagName = tagName;
    }

    /*
     *  Element Interface methods
     */

    public String getAttribute(String name) {
        Attr attrNode = getAttributeNode(name);
        String attrValue = "";
        if (attrNode != null) {
            attrValue = attrNode.getValue();
        }
        return attrValue;
    }

    public String getAttributeNS(String namespaceURI, String localName) {
        // TODO Auto-generated method stub
        return null;
    }

    public Attr getAttributeNode(String name) {
        return (Attr)mAttributes.getNamedItem(name);
    }

    public Attr getAttributeNodeNS(String namespaceURI, String localName) {
        // TODO Auto-generated method stub
        return null;
    }

    public NodeList getElementsByTagName(String name) {
        return new NodeListImpl(this, name, true);
    }

    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
        // TODO Auto-generated method stub
        return null;
    }

    public String getTagName() {
        return mTagName;
    }

    public boolean hasAttribute(String name) {
        return (getAttributeNode(name) != null);
    }

    public boolean hasAttributeNS(String namespaceURI, String localName) {
        // TODO Auto-generated method stub
        return false;
    }

    public void removeAttribute(String name) throws DOMException {
        // TODO Auto-generated method stub

    }

    public void removeAttributeNS(String namespaceURI, String localName)
            throws DOMException {
        // TODO Auto-generated method stub

    }

    public Attr removeAttributeNode(Attr oldAttr) throws DOMException {
        // TODO Auto-generated method stub
        return null;
    }

    public void setAttribute(String name, String value) throws DOMException {
        Attr attribute = getAttributeNode(name);
        if (attribute == null) {
            attribute = mOwnerDocument.createAttribute(name);
        }
        attribute.setNodeValue(value);
        mAttributes.setNamedItem(attribute);
    }

    public void setAttributeNS(String namespaceURI, String qualifiedName,
            String value) throws DOMException {
        // TODO Auto-generated method stub

    }

    public Attr setAttributeNode(Attr newAttr) throws DOMException {
        // TODO Auto-generated method stub
        return null;
    }

    public Attr setAttributeNodeNS(Attr newAttr) throws DOMException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * Node Interface methods
     */

    @Override
    public short getNodeType() {
        return ELEMENT_NODE;
    }

    @Override
    public String getNodeName() {
        // The value of nodeName is tagName when Node is an Element
        return mTagName;
    }

    @Override
    public NamedNodeMap getAttributes() {
        return mAttributes;
    }

    @Override
    public boolean hasAttributes() {
        return (mAttributes.getLength() > 0);
    }

    public TypeInfo getSchemaTypeInfo() {
        return null;
    }

    public void setIdAttribute(String name, boolean isId) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, null);
    }

    public void setIdAttributeNS(String namespaceURI, String localName,
            boolean isId) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, null);
    }

    public void setIdAttributeNode(Attr idAttr, boolean isId)
            throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, null);
    }
}
