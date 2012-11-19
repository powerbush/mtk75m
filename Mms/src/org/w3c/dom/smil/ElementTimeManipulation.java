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
 *  This interface support use-cases commonly associated with animation.  
 * "accelerate" and "decelerate" are float values in the timing draft and 
 * percentage values even in this draft if both of them represent a 
 * percentage. 
 */
public interface ElementTimeManipulation {
    /**
     *  Defines the playback  speed of element time. The value is specified as 
     * a multiple of normal (parent time container) play speed.  Legal values 
     * are signed floating point values.  Zero values are not allowed.  The 
     * default is <code>1.0</code> (no modification of speed). 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public float getSpeed();
    public void setSpeed(float speed)
                            throws DOMException;

    /**
     *  The percentage value of the  simple acceleration of time for the 
     * element. Allowed values are from <code>0</code> to <code>100</code> . 
     * Default value is <code>0</code> (no acceleration). 
     * <br> The sum of the values for accelerate and decelerate must not exceed
     *  100. If it does, the deceleration value will be reduced to make the 
     * sum legal. 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public float getAccelerate();
    public void setAccelerate(float accelerate)
                            throws DOMException;

    /**
     *  The percentage value of the  simple decelerate of time for the 
     * element. Allowed values are from <code>0</code> to <code>100</code> . 
     * Default value is <code>0</code> (no deceleration). 
     * <br> The sum of the values for accelerate and decelerate must not exceed
     *  100. If it does, the deceleration value will be reduced to make the 
     * sum legal. 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public float getDecelerate();
    public void setDecelerate(float decelerate)
                            throws DOMException;

    /**
     *  The  autoReverse attribute controls the "play forwards then backwards" 
     * functionality. Default value is <code>false</code> . 
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly. 
     */
    public boolean getAutoReverse();
    public void setAutoReverse(boolean autoReverse)
                            throws DOMException;

}

