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
 * Copyright (c) 2000 World Wide Web Consortium,
 * (Massachusetts Institute of Technology, Institut National de
 * Recherche en Informatique et en Automatique, Keio University). All
 * Rights Reserved. This program is distributed under the W3C's Software
 * Intellectual Property License. This program is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See W3C License http://www.w3.org/Consortium/Legal/ for more
 * details.
 */

package org.w3c.dom.smil;

import org.w3c.dom.DOMException;

/**
 *  Declares media content. 
 */
public interface SMILMediaElement extends ElementTime, SMILElement {
    /**
     *  See the  abstract attribute from  . 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public String getAbstractAttr();
    public void setAbstractAttr(String abstractAttr)
                              throws DOMException;

    /**
     *  See the  alt attribute from  . 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public String getAlt();
    public void setAlt(String alt)
                              throws DOMException;

    /**
     *  See the  author attribute from  . 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public String getAuthor();
    public void setAuthor(String author)
                              throws DOMException;

    /**
     *  See the  clipBegin attribute from  . 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public String getClipBegin();
    public void setClipBegin(String clipBegin)
                              throws DOMException;

    /**
     *  See the  clipEnd attribute from  . 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public String getClipEnd();
    public void setClipEnd(String clipEnd)
                              throws DOMException;

    /**
     *  See the  copyright attribute from  . 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public String getCopyright();
    public void setCopyright(String copyright)
                              throws DOMException;

    /**
     *  See the  longdesc attribute from  . 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public String getLongdesc();
    public void setLongdesc(String longdesc)
                              throws DOMException;

    /**
     *  See the  port attribute from  . 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public String getPort();
    public void setPort(String port)
                              throws DOMException;

    /**
     *  See the  readIndex attribute from  . 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public String getReadIndex();
    public void setReadIndex(String readIndex)
                              throws DOMException;

    /**
     *  See the  rtpformat attribute from  . 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public String getRtpformat();
    public void setRtpformat(String rtpformat)
                              throws DOMException;

    /**
     *  See the  src attribute from  . 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public String getSrc();
    public void setSrc(String src)
                              throws DOMException;

    /**
     *  See the  stripRepeat attribute from  . 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public String getStripRepeat();
    public void setStripRepeat(String stripRepeat)
                              throws DOMException;

    /**
     *  See the  title attribute from  . 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public String getTitle();
    public void setTitle(String title)
                              throws DOMException;

    /**
     *  See the  transport attribute from  . 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public String getTransport();
    public void setTransport(String transport)
                              throws DOMException;

    /**
     *  See the  type attribute from  . 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public String getType();
    public void setType(String type)
                              throws DOMException;

}

