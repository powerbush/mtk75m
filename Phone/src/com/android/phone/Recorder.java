/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

//New file added by delong.liu@archermind.com

package com.android.phone;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

public class Recorder implements OnErrorListener{
	private static final String TAG = "Recorder";

	static final String SAMPLE_PREFIX = "recording";
	static final String SAMPLE_PATH_KEY = "sample_path";
	static final String SAMPLE_LENGTH_KEY = "sample_length";

	public static final int IDLE_STATE = 0;
	public static final int RECORDING_STATE = 1;

	int mState = IDLE_STATE;

	public static final int NO_ERROR = 0;
	public static final int SDCARD_ACCESS_ERROR = 1;
	public static final int INTERNAL_ERROR = 2;

	public interface OnStateChangedListener {
		public void onStateChanged(int state);
		public void onError(int error);
	}

	OnStateChangedListener mOnStateChangedListener = null;

	long mSampleStart = 0; // time at which latest record or play operation started
	long mSampleLength = 0; // length of current sample
	File mSampleFile = null;
	MediaRecorder mRecorder = null;

	public Recorder() {
	}

	public void saveState(Bundle recorderState) {
		recorderState.putString(SAMPLE_PATH_KEY, mSampleFile.getAbsolutePath());
		recorderState.putLong(SAMPLE_LENGTH_KEY, mSampleLength);
	}

	public int getMaxAmplitude() {
		if (mState != RECORDING_STATE)
			return 0;
		return mRecorder.getMaxAmplitude();
	}

	public void restoreState(Bundle recorderState) {
		String samplePath = recorderState.getString(SAMPLE_PATH_KEY);
		if (samplePath == null)
			return;
		long sampleLength = recorderState.getLong(SAMPLE_LENGTH_KEY, -1);
		if (sampleLength == -1)
			return;

		File file = new File(samplePath);
		if (!file.exists())
			return;
		if (mSampleFile != null
				&& mSampleFile.getAbsolutePath().compareTo(
						file.getAbsolutePath()) == 0)
			return;

		delete();
		mSampleFile = file;
		mSampleLength = sampleLength;

		signalStateChanged(IDLE_STATE);
	}

	public void setOnStateChangedListener(OnStateChangedListener listener) {
		mOnStateChangedListener = listener;
	}

	public int state() {
		return mState;
	}

	public int progress() {
		if (mState == RECORDING_STATE )
			return (int) ((System.currentTimeMillis() - mSampleStart) / 1000);
		return 0;
	}

	public long sampleLength() {
		return mSampleLength;
	}

	public File sampleFile() {
		return mSampleFile;
	}

	/**
	 * Resets the recorder state. If a sample was recorded, the file is deleted.
	 */
	public void delete() {
		stop();

		if (mSampleFile != null)
			mSampleFile.delete();
		mSampleFile = null;
		mSampleLength = 0l;

		signalStateChanged(IDLE_STATE);
	}

	/**
	 * Resets the recorder state. If a sample was recorded, the file is left on
	 * disk and will be reused for a new recording.
	 */
	public void clear() {
		stop();

		mSampleLength = 0l;

		signalStateChanged(IDLE_STATE);
	}

	public void startRecording(int outputfileformat, String extension) throws IOException {
        log("startRecording");
        stop();

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
		String prefix = dateFormat.format(new Date());
		File sampleDir = Environment.getExternalStorageDirectory();

		if (!sampleDir.canWrite()) {
			Log.i(TAG, "----- file can't write!! ---");
			// Workaround for broken sdcard support on the device.
			sampleDir = new File("/sdcard/sdcard");
		}

		sampleDir = new File(sampleDir.getAbsolutePath() + "/PhoneRecord");
		if(sampleDir.exists() == false) {
		       sampleDir.mkdirs();
		}

		try {
			mSampleFile = File.createTempFile(prefix, extension, sampleDir);
		} catch (IOException e) {
			setError(SDCARD_ACCESS_ERROR);
			Log.i(TAG, "----***------- can't access sdcard !!");
			throw e;
		}
		
		log("finish creating temp file, start to record");

		mRecorder = new MediaRecorder();
		mRecorder.setOnErrorListener(this);
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(outputfileformat);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		mRecorder.setOutputFile(mSampleFile.getAbsolutePath());

		try {
			mRecorder.prepare();
			mRecorder.start();
			mSampleStart = System.currentTimeMillis();
			setState(RECORDING_STATE);
		} catch (IOException exception) {
            log("startRecording, IOException");
            setError(INTERNAL_ERROR);
			mRecorder.reset();
			mRecorder.release();
			mRecorder = null;
			throw exception;
		} catch (RuntimeException exception) {
            log("startRecording, RuntimeException");
            setError(INTERNAL_ERROR);
			mRecorder.reset();
			mRecorder.release();
			mRecorder = null;
            throw exception;
		}
	}

	public void stopRecording() {
        log("stopRecording");
        if (mRecorder == null){
			return;
		}
        	mSampleLength = System.currentTimeMillis() - mSampleStart;
		mRecorder.stop();
		mRecorder.release();
		mRecorder = null;

		setState(IDLE_STATE);
	}

	public void stop() {
        log("stop");
        stopRecording();
	}

	private void setState(int state) {
		if (state == mState)
			return;

		mState = state;
		signalStateChanged(mState);
	}

	private void signalStateChanged(int state) {
		if (mOnStateChangedListener != null)
			mOnStateChangedListener.onStateChanged(state);
	}

	private void setError(int error) {
		if (mOnStateChangedListener != null)
			mOnStateChangedListener.onError(error);
	}

	/**
	 * error listener
	 */
	// Added by tianxiang.qin@archermind.com
	public void onError(MediaRecorder mp, int what, int extra) {
        log("onError");
        if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
			stop();
			// TODO show hint view
		}
		return;
	}
	
    public void log(String msg) {
        Log.d(TAG, msg);
    }
	// Added by tianxiang.qin@archermind.com end
}
