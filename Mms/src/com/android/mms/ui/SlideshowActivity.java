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

/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.mms.ui;

import com.android.mms.R;
import com.android.mms.dom.AttrImpl;
import com.android.mms.dom.smil.SmilDocumentImpl;
import com.android.mms.dom.smil.SmilPlayer;
import com.android.mms.dom.smil.parser.SmilXmlSerializer;
import com.android.mms.model.LayoutModel;
import com.android.mms.model.RegionModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.model.SmilHelper;
import com.google.android.mms.MmsException;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.smil.SMILDocument;
import org.w3c.dom.smil.SMILElement;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Config;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;

import java.io.ByteArrayOutputStream;
import android.widget.Toast;
import android.content.ActivityNotFoundException;
import java.io.ByteArrayOutputStream;
import android.content.Context;
import android.content.res.Configuration;
import com.mediatek.xlog.Xlog;
/**
 * Plays the given slideshow in full-screen mode with a common controller.
 */
public class SlideshowActivity extends Activity implements EventListener {
    private static final String TAG = "SlideshowActivity";
    private static final String M_TAG = "Mms/SlideshowActivity";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;

    private static boolean bNeedResume = false;
    private MediaController mMediaController;
    private SmilPlayer mSmilPlayer;
    private AudioManager mAudioManager; // add for replay the music

    private Handler mHandler;

    private SMILDocument mSmilDoc;

    private SlideView mSlideView;
    private boolean mRotate = false;
    /**
     * @return whether the Smil has MMS conformance layout.
     * Refer to MMS Conformance Document OMA-MMS-CONF-v1_2-20050301-A
     */
    private static final boolean isMMSConformance(SMILDocument smilDoc) {
        SMILElement head = smilDoc.getHead();
        if (head == null) {
            // No 'head' element
            return false;
        }
        NodeList children = head.getChildNodes();
        if (children == null || children.getLength() != 1) {
            // The 'head' element should have only one child.
            return false;
        }
        Node layout = children.item(0);
        if (layout == null || !"layout".equals(layout.getNodeName())) {
            // The child is not layout element
            return false;
        }
        NodeList layoutChildren = layout.getChildNodes();
        if (layoutChildren == null) {
            // The 'layout' element has no child.
            return false;
        }
        int num = layoutChildren.getLength();
        if (num <= 0) {
            // The 'layout' element has no child.
            return false;
        }
        for (int i = 0; i < num; i++) {
            Node layoutChild = layoutChildren.item(i);
            if (layoutChild == null) {
                // The 'layout' child is null.
                return false;
            }
            String name = layoutChild.getNodeName();
            if ("root-layout".equals(name)) {
                continue;
            } else if ("region".equals(name)) {
                NamedNodeMap map = layoutChild.getAttributes();
                for (int j = 0; j < map.getLength(); j++) {
                    Node node = map.item(j);
                    if (node == null) {
                        return false;
                    }
                    String attrName = node.getNodeName();
                    // The attr should be one of left, top, height, width, fit and id
                    if ("left".equals(attrName) || "top".equals(attrName) ||
                            "height".equals(attrName) || "width".equals(attrName) ||
                            "fit".equals(attrName)) {
                        continue;
                    } else if ("id".equals(attrName)) {
                        String value;
                        if (node instanceof AttrImpl) {
                            value = ((AttrImpl)node).getValue();
                        } else {
                            return false;
                        }
                        if ("Text".equals(value) || "Image".equals(value)) {
                            continue;
                        } else {
                            // The id attr is not 'Text' or 'Image'
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            } else {
                // The 'layout' element has the child other than 'root-layout' or 'region'
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	// TODO Auto-generated method stub
    	super.onConfigurationChanged(newConfig);
    	if ((mSmilPlayer != null) && (mMediaController != null)) {
            mMediaController.hide();
        }
    }
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mHandler = new Handler();

        // Play slide-show in full-screen mode.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        setContentView(R.layout.slideshow);

        Intent intent = getIntent();
        Uri msg = intent.getData();
        final SlideshowModel model;

        try {
            model = SlideshowModel.createFromMessageUri(this, msg);
        } catch (MmsException e) {
            Log.e(TAG, "Cannot present the slide show.", e);
            finish();
            return;
        }

        mSlideView = (SlideView) findViewById(R.id.slide_view);
        PresenterFactory.getPresenter("SlideshowPresenter", this, mSlideView, model);

        Xlog.d(M_TAG, "onCreate(): get AudioManager from system service.");
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE); // add for replay the music
        mRotate = true;
        mSmilPlayer = SmilPlayer.getPlayer();
        initMediaController();
        mSlideView.setMediaController(mMediaController);
        // Use SmilHelper.getDocument() to ensure rebuilding the
        // entire SMIL document.
        mSmilDoc = SmilHelper.getDocument(model);
        mHandler.post(new Runnable() {
            private boolean isRotating() {
                return mSmilPlayer.isPausedState()
                        || mSmilPlayer.isPlayingState()
                        || mSmilPlayer.isPlayedState();
            }

            public void run() {
                if (isMMSConformance(mSmilDoc)) {
                    int imageLeft = 0;
                    int imageTop = 0;
                    int textLeft = 0;
                    int textTop = 0;
                    LayoutModel layout = model.getLayout();
                    if (layout != null) {
                        RegionModel imageRegion = layout.getImageRegion();
                        if (imageRegion != null) {
                            imageLeft = imageRegion.getLeft();
                            imageTop = imageRegion.getTop();
                        }
                        RegionModel textRegion = layout.getTextRegion();
                        if (textRegion != null) {
                            textLeft = textRegion.getLeft();
                            textTop = textRegion.getTop();
                        }
                    }
                    mSlideView.enableMMSConformanceMode(textLeft, textTop, imageLeft, imageTop);
                }
                if (DEBUG) {
                    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
                    SmilXmlSerializer.serialize(mSmilDoc, ostream);
                    if (LOCAL_LOGV) {
                        Log.v(TAG, ostream.toString());
                    }
                }

                // Add event listener.
                ((EventTarget) mSmilDoc).addEventListener(
                        SmilDocumentImpl.SMIL_DOCUMENT_END_EVENT,
                        SlideshowActivity.this, false);

                mSmilPlayer.init(mSmilDoc);
                if (isRotating()) {
                    mSmilPlayer.reload();
                } else {
                    mSmilPlayer.play();
                }
            }
        });
    }

    private void initMediaController() {
        mMediaController = new MediaController(SlideshowActivity.this, false);
        mMediaController.setMediaPlayer(new SmilPlayerController(mSmilPlayer));
        mMediaController.setAnchorView(findViewById(R.id.slide_view));
        mMediaController.setPrevNextListeners(
            new OnClickListener() {
              public void onClick(View v) {
            	  if ((mSmilPlayer != null) && (mMediaController != null)) {
                      mMediaController.show();
                  }
                  mSmilPlayer.next();
              }
            },
            new OnClickListener() {
              public void onClick(View v) {
            	  if ((mSmilPlayer != null) && (mMediaController != null)) {
                      mMediaController.show();
                  }
            	  if (mSmilPlayer.getCurrentSlide() <= 1){
            		  return;
            	  }
                  mSmilPlayer.prev();
              }
            });
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
    	
        if ((ev.getAction() == MotionEvent.ACTION_UP) && (mSmilPlayer != null) && (mMediaController != null)) {
            mMediaController.show();
        }
        return false;
    }

    // fix exception when press email add
    @Override
    public void startActivityForResult(Intent intent, int requestCode)
    {
        // requestCode >= 0 means the activity in question is a sub-activity.

        if (null != intent && null != intent.getData()
                && intent.getData().getScheme().equals("mailto")) {
            try {
                super.startActivityForResult(intent, requestCode);
            } catch (ActivityNotFoundException e) {
                Xlog.w(TAG, "Failed to startActivityForResult: " + intent);
                Intent i = new Intent().setClassName("com.android.email", "com.android.email.activity.setup.AccountSetupBasics");
                this.startActivity(i);
                finish();
            } catch (Exception e) {
                Xlog.e(TAG, "Failed to startActivityForResult: " + intent);
                Toast.makeText(this,getString(R.string.message_open_email_fail),
                      Toast.LENGTH_SHORT).show();
          }
        } else {
            super.startActivityForResult(intent, requestCode);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (mSmilDoc != null) {
            ((EventTarget) mSmilDoc).removeEventListener(
                    SmilDocumentImpl.SMIL_DOCUMENT_END_EVENT, this, false);
        }
        if ((null != mSmilPlayer)) {
        	if (mSmilPlayer.isPlayingState()) {
        		mSmilPlayer.pause();
        		bNeedResume = true;
        	} else if (mSmilPlayer.isPausedState()) {
        		bNeedResume = false;
        	}
        }   
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	if ((null != mSmilPlayer)) {
            if (isFinishing()) {
                mSmilPlayer.stop();
            } 
            if (mMediaController != null) {
                // Must do this so we don't leak a window.
                mMediaController.hide();
            }
        }
        Xlog.d(M_TAG, "onStop(): AudioManager abandonAudioFocus.");
    	mAudioManager.abandonAudioFocus(mAudioFocusListener);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
        		break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                break;
            case KeyEvent.KEYCODE_BACK:
                if ((mSmilPlayer != null) &&
                        (mSmilPlayer.isPausedState()
                        || mSmilPlayer.isPlayingState()
                        || mSmilPlayer.isPlayedState())) {
                    mSmilPlayer.stop();
                }
                break;
            default:
                if ((mSmilPlayer != null) && (mMediaController != null)) {
                    mMediaController.show();
                }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Xlog.d(M_TAG, "onStart(): AudioManager requestAudioFocus.");
        mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        if(mMediaController != null){
            //mMediaController.setMdragging(false);
            mMediaController.hide();
        }
        if (mSmilDoc != null) {
            ((EventTarget) mSmilDoc).addEventListener(
                    SmilDocumentImpl.SMIL_DOCUMENT_END_EVENT,
                    SlideshowActivity.this, false);
        }
        
        if(!bNeedResume) {
            mRotate = false;
            return;
        }
        if(null == mSmilPlayer) {
            mSmilPlayer = SmilPlayer.getPlayer();
        }
        if(null != mSmilPlayer) {
            if(!isFinishing()) {
                if(mSmilPlayer.isPausedState()) {
                    if(mRotate) {
                        // if need resume the player, set the state playing.
                        mSmilPlayer.setStateStart();
                    } else {
                        mSmilPlayer.start();
                    }
                }
            }
        }
        mRotate = false;
    }
    private class SmilPlayerController implements MediaPlayerControl {
        private final SmilPlayer mPlayer;

        public SmilPlayerController(SmilPlayer player) {
            mPlayer = player;
        }

        public int getBufferPercentage() {
            // We don't need to buffer data, always return 100%.
            return 100;
        }

        public int getCurrentPosition() {
        	if (mPlayer != null) {
        		return mPlayer.getCurrentPosition();
        	} else {
                return 0;
        	}
        }

        public int getDuration() {
            return mPlayer.getDuration();
        }

        public boolean isPlaying() {
            return mPlayer != null ? mPlayer.isPlayingState() : false;
        }

        public void pause() {
            if (mPlayer != null) {
                mPlayer.pause();
                
            }
        }

        public void seekTo(int pos) {
            // Don't need to support.
        }

        public void start() {
            if (mPlayer != null) {
                mPlayer.start();
            }
        }

        public boolean canPause() {
            return true;
        }

        public boolean canSeekBackward() {
            return true;
        }

        public boolean canSeekForward() {
            return true;
        }
    }

    public void handleEvent(Event evt) {
        final Event event = evt;
        mHandler.post(new Runnable() {
            public void run() {
                String type = event.getType();
                if(type.equals(SmilDocumentImpl.SMIL_DOCUMENT_END_EVENT)) {
                    finish();
                }
            }
        });
    }

    private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (mSmilPlayer == null) {
                return;
            }

            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS: {
                    mSmilPlayer.pause();
                    break;
                }

                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: {
                    if (mSmilPlayer.isPlayingState()) {
                        mSmilPlayer.pause();
                    }
                    break;
                }

                case AudioManager.AUDIOFOCUS_GAIN: {
                    if (mSmilPlayer.isPausedState() || mSmilPlayer.isPlayedState()) {
                        mSmilPlayer.play();
                    }
                    break;
                }
                default:
                    break;
            }
        }
    };
}
