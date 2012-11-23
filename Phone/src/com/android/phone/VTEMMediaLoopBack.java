package com.android.phone;

import android.os.Handler;
import com.mediatek.vt.VTManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.SurfaceHolder.Callback;
import android.widget.Button;
import com.mediatek.featureoption.FeatureOption;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.gemini.*;

public class VTEMMediaLoopBack extends Activity implements View.OnClickListener, SurfaceHolder.Callback {
    /** Called when the activity is first created. */
	
	private SurfaceView mVTHighVideo;
    private SurfaceView mVTLowVideo;
    private Button mVTDial;
    private Button mVTEnd;
    
    private boolean mHighVideoChanged = false;
    private boolean mLowVideoChanged = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.vt_em_medialoopback);
        
        VTManager.getInstance().registerVTListener(new Handler());
        
        mVTHighVideo = (SurfaceView) findViewById(R.id.VTEMHighVideo);
        mVTLowVideo = (SurfaceView) findViewById(R.id.VTEMLowVideo);
        
        Log.d("VTEMMediaLoopBack", "mVTHighVideo's holder is " + mVTHighVideo.getHolder().toString());
        Log.d("VTEMMediaLoopBack", "mVTLowVideo's holder is " + mVTLowVideo.getHolder().toString());
        
        mVTHighVideo.getHolder().addCallback( this);
        mVTLowVideo.getHolder().addCallback( this);
        
        Log.d("VTEMMediaLoopBack", "set type - SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS ! ");
        mVTHighVideo.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mVTLowVideo.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        mVTDial = (Button)findViewById(R.id.VTEMStart);
        mVTDial.setText("Dial");
        mVTDial.setOnClickListener(this);
        mVTDial.setEnabled(false);
        
        mVTEnd = (Button)findViewById(R.id.VTEMEnd);
        mVTEnd.setText("End");
        mVTEnd.setOnClickListener(this);
        mVTEnd.setEnabled(false);
        
    }
    
    protected void onPause()
    {
    	Log.d("VTEMMediaLoopBack", "onPause! -> finish itself !" );
    	super.onPause();
    	this.finish();
    }
    
    protected void onResume()
    {
    	super.onResume();
    	Log.d("VTEMMediaLoopBack", "onResume!" );
    }
    
    protected void onStop()
    {
    	super.onStop();
    	Log.d("VTEMMediaLoopBack", "onStop!" );
    }
    
    protected void onStart()
    {
    	super.onStart();
    	Log.d("VTEMMediaLoopBack", "onStart!" );
    }

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		
		Log.d("VTEMMediaLoopBack", "surfaceChanged : " + holder.toString());
		
		if(holder == mVTHighVideo.getHolder() && mHighVideoChanged == false)
			mHighVideoChanged = true;
		
		if(holder == mVTLowVideo.getHolder() && mLowVideoChanged == false)
			mLowVideoChanged = true;
		
		if(mHighVideoChanged && mLowVideoChanged)
		{
			Log.d("VTEMMediaLoopBack", "surfaceChanged : VTManager.getInstance().setDisplay(mVTLowVideo.getHolder(), mVTHighVideo.getHolder())");
			VTManager.getInstance().setDisplay(mVTLowVideo.getHolder(), mVTHighVideo.getHolder());
			mVTDial.setEnabled(true);
			mVTEnd.setEnabled(true);
		}
		
	}

	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.d("VTEMMediaLoopBack", "surfaceCreated : " + holder.toString());
		
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.d("VTEMMediaLoopBack", "surfaceDestroyed : " + holder.toString());
		
	}

	public void onClick(View v) {
		// TODO Auto-generated method stub
		
		if(R.id.VTEMStart == v.getId())
		{
			Log.d("VTEMMediaLoopBack", "VT EM Media loopback : dial ! ");
			
			Log.d("VTEMMediaLoopBack", "VTSettingUtils.getInstance().updateVTEngineerModeValues() start ! ");
			VTSettingUtils.getInstance().updateVTEngineerModeValues();
			Log.d("VTEMMediaLoopBack", "VTSettingUtils.getInstance().updateVTEngineerModeValues() end ! ");
			
			Log.d("VTEMMediaLoopBack", "VTManager.getInstance().setVTOpen() start ! ");
			if (FeatureOption.MTK_GEMINI_SUPPORT) {
	    		VTManager.getInstance().setVTOpen(PhoneApp.getInstance().getBaseContext(), PhoneApp.getInstance().mCMGemini);
			}else{
				VTManager.getInstance().setVTOpen(PhoneApp.getInstance().getBaseContext(), PhoneApp.getInstance().mCM);
			}
			Log.d("VTEMMediaLoopBack", "VTManager.getInstance().setVTOpen() end ! ");
			
			Log.d("VTEMMediaLoopBack", "VTManager.getInstance().setVTReady() start ! ");
			VTManager.getInstance().setVTReady();
			Log.d("VTEMMediaLoopBack", "VTManager.getInstance().setVTReady() end ! ");
			
			Log.d("VTEMMediaLoopBack", "VTManager.getInstance().onConnected() start ! ");
			VTManager.getInstance().onConnected();
			Log.d("VTEMMediaLoopBack", "VTManager.getInstance().onConnected() end ! ");
			
		}else if(R.id.VTEMEnd == v.getId()){
			Log.d("VTEMMediaLoopBack", "VT EM Media loopback : hangup ! ");
			
			Log.d("VTEMMediaLoopBack", "VTManager.getInstance().onConnected() start ! ");
			VTManager.getInstance().onDisconnected();
			Log.d("VTEMMediaLoopBack", "VTManager.getInstance().onConnected() end ! ");
			
			Log.d("VTEMMediaLoopBack", "VTManager.getInstance().onConnected() start ! ");
			VTManager.getInstance().setVTClose();
			Log.d("VTEMMediaLoopBack", "VTManager.getInstance().onConnected() end ! ");
		}
		
	}
	

}