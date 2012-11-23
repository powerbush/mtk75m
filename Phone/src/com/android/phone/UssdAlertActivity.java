package com.android.phone;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.content.Intent;
import android.widget.Button;
import android.os.Bundle;
import android.util.Log;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.mediatek.featureoption.FeatureOption;

import android.view.View;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.gemini.GeminiPhone;

public class UssdAlertActivity extends AlertActivity implements 
    DialogInterface.OnClickListener {
    public static int USSD_DIALOG_REQUEST = 1;
    public static int USSD_DIALOG_NOTIFICATION = 2;
    public static String USSD_MESSAGE_EXTRA = "ussd_message";
    public static String USSD_TYPE_EXTRA = "ussd_type";
    public static String USSD_SLOT_ID = "slot_id";
    
    private TextView mMsg = null;
    EditText inputText = null;
    
    private String mText = null;
    private int mType = USSD_DIALOG_REQUEST;
    private int slotId = 0;
    Phone phone = null;
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the "dialog"
        final AlertController.AlertParams p = mAlertParams;
        
        phone = PhoneApp.getInstance().phone;
        Intent intent = getIntent();
        mText = intent.getStringExtra(USSD_MESSAGE_EXTRA);
        mType = intent.getIntExtra(USSD_TYPE_EXTRA, USSD_DIALOG_REQUEST);
        slotId = intent.getIntExtra(USSD_SLOT_ID, 0);
        //p.mIconId = android.R.drawable.ic_dialog_alert;
        //p.mTitle = getString(R.string.bt_enable_title);
        //p.mTitle = "USSD";
        p.mView = createView();
        if (mType == USSD_DIALOG_REQUEST) {
            p.mPositiveButtonText = getString(R.string.send_button);
            p.mNegativeButtonText = getString(R.string.cancel);
        } else {
            p.mPositiveButtonText = getString(R.string.ok);
        }
        
        p.mPositiveButtonListener = this;
        p.mNegativeButtonListener = this;
        
//        if (mType == USSD_DIALOG_NOTIFICATION) {
//            
//        }
        PhoneUtils.mUssdActivity = this;
        setupAlert();
    }

    private View createView() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_ussd_response, null);
        mMsg = (TextView)dialogView.findViewById(R.id.msg);
        inputText = (EditText) dialogView.findViewById(R.id.input_field);

        if (mMsg != null) {
            mMsg.setText(mText);
        }
        
        if (mType == USSD_DIALOG_NOTIFICATION) {
            inputText.setVisibility(View.GONE);
        }
        
        return dialogView;
    }

    public void onClick(DialogInterface dialog, int which) {
        PhoneUtils.mDialog = null;
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                if (mType == USSD_DIALOG_REQUEST) {
                    sendUssd();
                }
                finish();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                PhoneUtils.cancelUssdDialog();
                finish();
                break;
        }
    }
    
    private void sendUssd() {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            ((GeminiPhone)phone).sendUssdResponseGemini(inputText.getText().toString(), slotId);
        } else {
            phone.sendUssdResponse(inputText.getText().toString());
        }
    }
}
