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
 *  This interface define the set of animation extensions for SMIL.  The  
 * attributes will go in a XLink interface. 
 */
public interface SMILAnimation extends SMILElement, ElementTargetAttributes, ElementTime, ElementTimeControl {
    // additiveTypes
    public static final short ADDITIVE_REPLACE          = 0;
    public static final short ADDITIVE_SUM              = 1;

    /**
     *  A code representing the value of the  additive attribute, as defined 
     * above. Default value is <code>ADDITIVE_REPLACE</code> . 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public short getAdditive();
    public void setAdditive(short additive)
                                throws DOMException;

    // accumulateTypes
    public static final short ACCUMULATE_NONE           = 0;
    public static final short ACCUMULATE_SUM            = 1;

    /**
     *  A code representing the value of the  accumulate attribute, as defined 
     * above. Default value is <code>ACCUMULATE_NONE</code> . 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public short getAccumulate();
    public void setAccumulate(short accumulate)
                                throws DOMException;

    // calcModeTypes
    public static final short CALCMODE_DISCRETE         = 0;
    public static final short CALCMODE_LINEAR           = 1;
    public static final short CALCMODE_PACED            = 2;
    public static final short CALCMODE_SPLINE           = 3;

    /**
     *  A code representing the value of the  calcMode attribute, as defined 
     * above. 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public short getCalcMode();
    public void setCalcMode(short calcMode)
                                throws DOMException;

    /**
     *  A <code>DOMString</code> representing the value of the  keySplines 
     * attribute.  Need an interface a point (x1,y1,x2,y2) 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public String getKeySplines();
    public void setKeySplines(String keySplines)
                                throws DOMException;

    /**
     *  A list of the time value of the  keyTimes attribute. 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public TimeList getKeyTimes();
    public void setKeyTimes(TimeList keyTimes)
                                throws DOMException;

    /**
     *  A <code>DOMString</code> representing the value of the  values 
     * attribute. 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public String getValues();
    public void setValues(String values)
                                throws DOMException;

    /**
     *  A <code>DOMString</code> representing the value of the  from attribute.
     *  
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public String getFrom();
    public void setFrom(String from)
                                throws DOMException;

    /**
     *  A <code>DOMString</code> representing the value of the  to attribute. 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public String getTo();
    public void setTo(String to)
                                throws DOMException;

    /**
     *  A <code>DOMString</code> representing the value of the  by attribute. 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public String getBy();
    public void setBy(String by)
                                throws DOMException;

}

