package com.android.phone;


import java.util.List;
import java.util.Date;

import android.content.Context;
import android.text.TextUtils.TruncateAt;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.text.TextUtils;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Telephony.SIMInfo;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.CallerInfoAsyncQuery;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.TelephonyProperties;

import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;
import com.android.internal.telephony.gsm.GSMPhone;

import com.mediatek.telephony.PhoneNumberFormatUtilEx;

public class VTCallStatus extends FrameLayout implements CallerInfoAsyncQuery.OnQueryCompleteListener{
    
    private final static String TAG = "VTCallStatus";
    
    private final static int NUMBER_OF_VIEWS = 4;
    private final static int NUMBER_OF_MODES = 10;

    private ViewGroup mCallStatus0;
    private View[] mViews = new View[NUMBER_OF_VIEWS];
        
    private final int UNKNOWN               = -1;
    private final int SIP_INCOMING_WAITING  = 0;
    private final int SIP_ACTIVE            = 1;
    private final int SIP_HOLD              = 2;
    private final int CELL_INCOMING_WAITING = 3;
    private final int CELL_ACTIVE           = 4;
    private final int CELL_HOLD             = 5;
    private final int SIP_CELL              = 6;
    private final int CELL_SIP              = 7;
    private final int CELL_CELL             = 8;
    private final int SIP_SIP               = 9;
    
    public static final int CLEAR_DISCONNECT_MSG = 1;
    
    private int[][] mVisibleMatrix = {
        // SIP_INCOMING_WAITING
        {View.VISIBLE, View.INVISIBLE, View.INVISIBLE, View.VISIBLE  /*, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE*/},
        // SIP_ACTIVE
        {View.VISIBLE, View.INVISIBLE, View.INVISIBLE, View.VISIBLE  /*, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE*/},
        // SIP_HOLD
        {View.VISIBLE, View.INVISIBLE, View.VISIBLE  , View.VISIBLE  /*, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE*/},
        // CELL_INCOMING_WAITING
        {View.VISIBLE, /*View.INVISIBLE*/View.VISIBLE, View.INVISIBLE, View.VISIBLE  /*, View.VISIBLE  , View.INVISIBLE, View.INVISIBLE, View.INVISIBLE*/},
        // CELL_ACTIVE
        {View.VISIBLE, /*View.INVISIBLE*/View.VISIBLE, View.INVISIBLE, View.VISIBLE  /*, View.VISIBLE  , View.INVISIBLE, View.INVISIBLE, View.INVISIBLE*/},
        // CELL_HOLD
        {View.VISIBLE, /*View.INVISIBLE*/View.VISIBLE, View.VISIBLE  , View.VISIBLE  /*, View.VISIBLE  , View.INVISIBLE, View.INVISIBLE, View.INVISIBLE*/},
        // SIP_CELL
        {View.VISIBLE, View.INVISIBLE, View.VISIBLE  , View.VISIBLE  /*, View.VISIBLE  , View.VISIBLE  , View.VISIBLE  , View.VISIBLE*/  },
        // CELL_SIP
        {View.VISIBLE, View.VISIBLE  , View.VISIBLE  , View.VISIBLE  /*, View.VISIBLE  , View.INVISIBLE, View.INVISIBLE, View.VISIBLE*/  },
        // CELL_CELL
        {View.VISIBLE, View.VISIBLE  , View.VISIBLE  , View.VISIBLE  /*, View.VISIBLE  , View.VISIBLE  , View.VISIBLE  , View.VISIBLE*/  },
        // SIP_SIP
        {View.VISIBLE, View.INVISIBLE, View.VISIBLE  , View.VISIBLE  /*, View.VISIBLE  , View.INVISIBLE, View.VISIBLE  , View.VISIBLE*/  }
    };
    
    private int[] mSimColorMap = {
        R.drawable.incall_status_color0,
        R.drawable.incall_status_color1,
        R.drawable.incall_status_color2,
        R.drawable.incall_status_color3,
        R.drawable.incall_status_color4,
        R.drawable.incall_status_color5,
        R.drawable.incall_status_color6,
        R.drawable.incall_status_color7,
    };
    
    private int[] mSimLightColorMap = {
        R.drawable.layer_color0,
        R.drawable.layer_color1,
        R.drawable.layer_color2,
        R.drawable.layer_color3,
        R.drawable.layer_color4,
        R.drawable.layer_color5,
        R.drawable.layer_color6,
        R.drawable.layer_color7,
    };
    
    private int mCallStatusMode;
    private Call.State mCallState;
    
    private InCallScreen mInCallScreen;
    
    private SIMInfo mSimInfo;
    private int mTextViewSize0;
    private int mTextViewSize1;
    private int mTextViewSize2;

    public VTCallStatus(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Inflate the contents of this CallCard, and add it (to ourself) as a child.
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.vt_call_status, this, true);
    }
    
    @Override
    public void onFinishInflate() {
        
        mViews[0] = findViewById(R.id.vtView0);
        mViews[1] = findViewById(R.id.vtView1);
        mViews[2] = findViewById(R.id.vtView2);
        mViews[3] = findViewById(R.id.vtView3);
        mCallStatus0 = (ViewGroup)findViewById(R.id.vtCallStatus0);

    }
    
    public void setInCallScreenInstance(InCallScreen inCallScreen) {
        mInCallScreen = inCallScreen;
        mTextViewSize0 = dip2px(100f);
        mTextViewSize1 = dip2px(80f);
        mTextViewSize2 = dip2px(160f);
    }
    
    public void updateState(CallManager cm) {
        
        String status = null;

        // If the FG call is dialing/alerting, we should display for that call
        // and ignore the ringing call. This case happens when the telephony
        // layer rejects the ringing call while the FG call is dialing/alerting,
        // but the incoming call *does* briefly exist in the DISCONNECTING or
        // DISCONNECTED state.
        Phone.State state = cm.getState();  // IDLE, RINGING, or OFFHOOK
        Call ringingCall = cm.getFirstActiveRingingCall();
        Call fgCall = cm.getActiveFgCall();
        if ((ringingCall.getState() != Call.State.IDLE)
                && fgCall.getState().isDialing()) {
            return;
        }

        mCallStatusMode = getCallStatusMode(cm);
        log("updateState mCallStatusMode = "+mCallStatusMode);

        updateSimInfo();

        if(FeatureOption.MTK_GEMINI_SUPPORT) {
            if(mCallStatusMode == UNKNOWN) {
                log("mCallStatusMode is UNKNOWN, bail out...");
                mCallStatus0.setVisibility(View.INVISIBLE);
                return;
            } else if((mCallStatusMode > SIP_HOLD && mCallStatusMode < SIP_SIP) && mSimInfo == null) {
                log("mSimInfo is null, bail out...");
                /* buggy
                 * give chance to show call ended string
                 */
                if(mCallState != Call.State.DISCONNECTING && mCallState != Call.State.DISCONNECTED)
                    mCallStatus0.setVisibility(View.INVISIBLE);
                else {
                    TextView textView = (TextView)mViews[3];
                    textView.setText(this.getCallFailedString(cm.getActiveFgCall()));
                }
                return;
            } else {
                mCallStatus0.setVisibility(View.VISIBLE);
            }
        } else {
        	((TextView)mViews[0]).setMaxWidth(dip2px(176f));
        	((TextView)mViews[3]).setMaxWidth(dip2px(80f));
            if(mCallStatusMode == UNKNOWN) {
                log("mCallStatusMode is UNKNOWN, bail out...");
                mCallStatus0.setVisibility(View.INVISIBLE);
                return;
            } else
                mCallStatus0.setVisibility(View.VISIBLE);
        }

        int[] visibles = mVisibleMatrix[mCallStatusMode];
        
        for(int i=0; i<NUMBER_OF_VIEWS; i++) {
            mViews[i].setVisibility(visibles[i]);
            log("mViews["+i+"] = "+mViews[i].getVisibility());
        }

        updateCallStatusBackground(mCallStatusMode);

        updateTextViewStyle(mCallStatusMode);

        switch(mCallStatusMode) {
            
            case SIP_INCOMING_WAITING:
                ((TextView)mViews[0]).setText(R.string.incall_call_type_label_sip);
                ((TextView)mViews[3]).setText(R.string.call_status_ringing);
                break;
                
            case SIP_ACTIVE:
                ((TextView)mViews[0]).setText(R.string.incall_call_type_label_sip);
                status = getTitleForCallStatus(cm.getActiveFgCall());
                /* buggy */
                if(status != null) {
                    ((TextView)mViews[3]).setText(status);
                    mViews[2].setVisibility(View.INVISIBLE);
                } else {
                    ((ImageView)mViews[2]).setImageResource(R.drawable.incall_status_active);
                    mViews[2].setVisibility(View.VISIBLE);
                }
                    
                break;
            
            case SIP_HOLD:
                ((TextView)mViews[0]).setText(R.string.incall_call_type_label_sip);
                ((TextView)mViews[3]).setText(R.string.menu_hold);
                ((ImageView)mViews[2]).setImageResource(R.drawable.incall_status_hold);
                break;
                
            case CELL_INCOMING_WAITING:
                ((TextView)mViews[0]).setText(getNetworkOperatorName());
                ((TextView)mViews[3]).setText("");

                if(FeatureOption.MTK_GEMINI_SUPPORT) {
                    ((TextView)mViews[1]).setText(mSimInfo.mDisplayName);
                }
                else
                    mViews[1].setVisibility(View.INVISIBLE);
                break;
                
            case CELL_ACTIVE:
// MTK_OP01_PROTECT_START
                String optr = SystemProperties.get("ro.operator.optr");
                if( null != optr && optr.equals("OP01")
                    && Call.State.DISCONNECTED == mCallState ){
                    ((TextView)mViews[0]).setEllipsize(null);
                } else
// MTK_OP01_PROTECT_END
                {
                    ((TextView)mViews[0]).setText(getNetworkOperatorName());
                }

                status = getTitleForCallStatus(cm.getActiveFgCall());
                /* buggy */
                if(status != null) { // Dialing status
                    ((TextView)mViews[3]).setText(status);
                    mViews[2].setVisibility(View.INVISIBLE);
                } else { // Other status
                    ((ImageView)mViews[2]).setImageResource(R.drawable.incall_status_active);
                    mViews[2].setVisibility(View.VISIBLE);
                }
                if(FeatureOption.MTK_GEMINI_SUPPORT) {
                    ((TextView)mViews[1]).setText(mSimInfo.mDisplayName);
                }
                else
                    mViews[1].setVisibility(View.INVISIBLE);
                break;
              
            case CELL_HOLD:
                ((TextView)mViews[0]).setText(getNetworkOperatorName());
                ((TextView)mViews[3]).setText(R.string.menu_hold);
                ((ImageView)mViews[2]).setImageResource(R.drawable.incall_status_hold);
                if(FeatureOption.MTK_GEMINI_SUPPORT) {
                    ((TextView)mViews[1]).setText(mSimInfo.mDisplayName);
                }
                else
                    mViews[1].setVisibility(View.INVISIBLE);
                break;
                
            case SIP_CELL:
                ((TextView)mViews[0]).setText(R.string.incall_call_type_label_sip);
                ((ImageView)mViews[2]).setImageResource(R.drawable.incall_status_hold);
                ((TextView)mViews[3]).setText(R.string.menu_hold);                
                break;
                
            case CELL_SIP:
                if(cm.getFirstActiveBgCall().isMultiparty())
                    ((TextView)mViews[0]).setText(R.string.card_title_conf_call);
                else /* start query */ {
                    PhoneUtils.CallerInfoToken info = PhoneUtils.startGetCallerInfo(mInCallScreen,
                                                                                    cm.getFirstActiveBgCall(),
                                                                                    this,
                                                                                    (TextView)mViews[0]);
                    displayCallerInfo(info.currentInfo, (TextView)mViews[0]);
                }

                if(FeatureOption.MTK_GEMINI_SUPPORT)
                    ((TextView)mViews[1]).setText(mSimInfo.mDisplayName);
                else
                    mViews[1].setVisibility(View.INVISIBLE);
                
                ((ImageView)mViews[2]).setImageResource(R.drawable.incall_status_hold);
                ((TextView)mViews[3]).setText(R.string.menu_hold);                
                break;
                
            case SIP_SIP:
                ((TextView)mViews[0]).setText(R.string.incall_call_type_label_sip);
                ((ImageView)mViews[2]).setImageResource(R.drawable.incall_status_hold);
                ((TextView)mViews[3]).setText(R.string.menu_hold);
                
                break;
                
            case CELL_CELL:
                if(cm.getFirstActiveBgCall().isMultiparty())
                    ((TextView)mViews[0]).setText(R.string.card_title_conf_call);
                else /* start query */ {
                    PhoneUtils.CallerInfoToken info = PhoneUtils.startGetCallerInfo(mInCallScreen,
                                                                                    cm.getFirstActiveBgCall(),
                                                                                    this,
                                                                                    (TextView)mViews[0]);
                    displayCallerInfo(info.currentInfo, (TextView)mViews[0]);
                }

                if(FeatureOption.MTK_GEMINI_SUPPORT)
                    ((TextView)mViews[1]).setText(mSimInfo.mDisplayName);
                else
                    mViews[1].setVisibility(View.INVISIBLE);
                
                ((ImageView)mViews[2]).setImageResource(R.drawable.incall_status_hold);
                ((TextView)mViews[3]).setText(R.string.menu_hold);
                break;
        }
    }
    
    void updateElapsedTimeWidgetOfCallStatus(long timeElapsed) {
    	log("updateElapsedTimeWidgetOfCallStatus() is called, timeElapsed is " + timeElapsed );
        log("VT_TIMING_SPECIAL is " + ( VTCallUtils.VTTimingMode.VT_TIMING_SPECIAL == PhoneApp.getInstance().getVTTimingMode() ? "true" : "false" ));
    	if( VTCallUtils.VTTimingMode.VT_TIMING_SPECIAL == PhoneApp.getInstance().getVTTimingMode()
    		&& false == VTInCallScreenFlags.getInstance().mVTInTiming )
    		return;
        TextView elapsedView = (TextView)mViews[3];
        String timeString = null;    
        
        if(VTCallUtils.VTTimingMode.VT_TIMING_NONE == PhoneApp.getInstance().getVTTimingMode())
        	timeString = "";
        else if(timeElapsed == 0)
            timeString = "00:00";
        else
            timeString = DateUtils.formatElapsedTime(timeElapsed);
        
        if(elapsedView != null) {
            elapsedView.setText(timeString);
            if(elapsedView.getVisibility() != View.VISIBLE)
                elapsedView.setVisibility(View.VISIBLE);
        }
    }
    
    private String getTitleForCallStatus(Call call) {
        String retVal = null;

        if(call == null) {
            return null;
        }

        Call.State state = call.getState();
        Context context = getContext();
        int resId;

        switch (state) {
            case IDLE:
                /* buggy */
                retVal = "";
                break;

            case ACTIVE:
           		if ( null != VTInCallScreenFlags.getInstance() &&
           			 false == VTInCallScreenFlags.getInstance().mVTIsMT &&
           			 false == VTInCallScreenFlags.getInstance().mVTInTiming &&
        			 VTCallUtils.VTTimingMode.VT_TIMING_SPECIAL == PhoneApp.getInstance().getVTTimingMode()) {
           			retVal = context.getString(R.string.card_title_dialing);
           		}
                break;

            case HOLDING:
                break;

            case DIALING:
            case ALERTING:
                retVal = context.getString(R.string.card_title_dialing);
                break;

            case INCOMING:
            case WAITING:
                retVal = context.getString(R.string.card_title_incoming_call);
                break;

            case DISCONNECTING:
                retVal = context.getString(R.string.card_title_hanging_up);
                break;

            case DISCONNECTED:
                retVal = getCallFailedString(call);
                mHandler.sendMessageDelayed(Message.obtain(mHandler, CLEAR_DISCONNECT_MSG), 1500);
                break;
        }
        return retVal;
    }
    
    private String getCallFailedString(Call call) {
        Connection c = call.getEarliestConnection();
        int resID;

        if (c == null) {
            // if this connection is null, just assume that the
            // default case occurs.
            resID = R.string.card_title_call_ended;
        } else {

            Connection.DisconnectCause cause = c.getDisconnectCause();

            // TODO: The card *title* should probably be "Call ended" in all
            // cases, but if the DisconnectCause was an error condition we should
            // probably also display the specific failure reason somewhere...

            switch (cause) {
                case BUSY:
                    resID = R.string.callFailed_userBusy;
                    break;

                case CONGESTION:
                    resID = R.string.callFailed_congestion;
                    break;

                case TIMED_OUT:
                    resID = R.string.callFailed_timedOut;
                    break;

                case SERVER_UNREACHABLE:
                    resID = R.string.callFailed_server_unreachable;
                    break;

                case NUMBER_UNREACHABLE:
                    resID = R.string.callFailed_number_unreachable;
                    break;

                case INVALID_CREDENTIALS:
                    resID = R.string.callFailed_invalid_credentials;
                    break;

                case SERVER_ERROR:
                    resID = R.string.callFailed_server_error;
                    break;

                case OUT_OF_NETWORK:
                    resID = R.string.callFailed_out_of_network;
                    break;

                case LOST_SIGNAL:
                case CDMA_DROP:
                    resID = R.string.callFailed_noSignal;
                    break;

                case LIMIT_EXCEEDED:
                    resID = R.string.callFailed_limitExceeded;
                    break;

                case POWER_OFF:
                    resID = R.string.callFailed_powerOff;
                    break;

                case ICC_ERROR:
                    resID = R.string.callFailed_simError;
                    break;

                case OUT_OF_SERVICE:
                    resID = R.string.callFailed_outOfService;
                    break;

                case INVALID_NUMBER:
                case UNOBTAINABLE_NUMBER:
                    resID = R.string.callFailed_unobtainable_number;
                    break;

                default:
                    resID = R.string.card_title_call_ended;
                    break;
            }
        }
        return getContext().getString(resID);
    }
    
    private boolean isMMIRunning(CallManager cm) {
        int count = 0;
        List<Phone> array = cm.getAllPhones();
        for (Phone phone : array) {
            if (phone instanceof GSMPhone) {
                count = phone.getPendingMmiCodes().size();
                if (count != 0)
                   break;
            }
        }
        return count != 0;
    }
    
    private int getCallStatusMode(CallManager cm) {
        log("getCallStatusMode");
        PhoneUtils.dumpCallManager();
        Call fgCall = cm.getActiveFgCall();
        Call bgCall = cm.getFirstActiveBgCall();
        Call ringingCall = cm.getFirstActiveRingingCall();
        
        Call.State fgCallState = fgCall == null ? Call.State.IDLE : fgCall.getState();
        Call.State bgCallState = bgCall == null ? Call.State.IDLE : bgCall.getState();
        
        int fgCallType = fgCall.getPhone().getPhoneType();
        int bgCallType = bgCall.getPhone().getPhoneType();
        int retVal = UNKNOWN;
        mCallState = Call.State.IDLE;
        if(ringingCall != null && ringingCall.getState() != Call.State.IDLE) {
            mCallState = ringingCall.getState();
            if(ringingCall.getPhone().getPhoneType() == Phone.PHONE_TYPE_SIP)
                retVal = SIP_INCOMING_WAITING;
            else
                retVal = CELL_INCOMING_WAITING;
            return retVal;
        }

        if(fgCallState != Call.State.IDLE && bgCallState != Call.State.IDLE) {
            mCallState = fgCallState;
            if(fgCallType == Phone.PHONE_TYPE_SIP) {
                if(bgCallType == Phone.PHONE_TYPE_SIP)
                    retVal = SIP_SIP;
                else
                    retVal = CELL_SIP;
            } else {
                if(bgCallType == Phone.PHONE_TYPE_SIP)
                    retVal = SIP_CELL;
                else
                    retVal = CELL_CELL;
            }
        } else if(fgCallState != Call.State.IDLE) {
            mCallState = fgCallState;
            if(fgCallType == Phone.PHONE_TYPE_SIP)
                retVal = SIP_ACTIVE;
            else
                retVal = CELL_ACTIVE;
        } else if(bgCallState != Call.State.IDLE) {
            mCallState = bgCallState;
            if(bgCallType == Phone.PHONE_TYPE_SIP)
                retVal = SIP_HOLD;
            else
                retVal = CELL_HOLD;
        } else {
            if(isMMIRunning(cm)) {
                log("isMMIRunning");
                retVal = CELL_ACTIVE;
            }
        }
        return retVal;
    }
    
    public void onQueryComplete(int token, Object cookie, CallerInfo ci) {
        log("onQueryComplete : CallerInfo = " + ci);

        displayCallerInfo(ci, (TextView)cookie);
    }
    
    void displayCallerInfo(CallerInfo ci, TextView nameTextView) {
     
        String callerName = "";
        if (ci != null) {
            callerName = ci.name;
            if (TextUtils.isEmpty(callerName)) {
                callerName = ci.phoneNumber;
                if (TextUtils.isEmpty(callerName)) {
                    callerName = mInCallScreen.getString(R.string.unknown);
                } else {
                    if(PhoneUtils.isEccCall(PhoneApp.getInstance().mCM.getActiveFgCall())) {
                        callerName = ci.phoneNumber;
                        log("displayCallerInfo ecc");
                    }
                    else
                        callerName = PhoneNumberFormatUtilEx.formatNumber(ci.phoneNumber);
                }
            }
        }
        log("displayCallerInfo : callerName = callerName");
        // set the caller name
        nameTextView.setText(callerName);
    }
    
    void updateSimInfo() {
        if(FeatureOption.MTK_GEMINI_SUPPORT) {
            GeminiPhone phone = (GeminiPhone) PhoneApp.getInstance().phone;
            int slot = -1;
            
            if(phone.getStateGemini(Phone.GEMINI_SIM_2) != Phone.State.IDLE) {
                slot = Phone.GEMINI_SIM_2;
            } else if(phone.getStateGemini(Phone.GEMINI_SIM_1) != Phone.State.IDLE) {
                slot = Phone.GEMINI_SIM_1;
            }
            
            if (slot == -1) {
                if (phone.getPendingMmiCodesGemini(Phone.GEMINI_SIM_1).size() != 0) {
                    slot = Phone.GEMINI_SIM_1;
                }else if (phone.getPendingMmiCodesGemini(Phone.GEMINI_SIM_2).size() != 0) {
                    slot = Phone.GEMINI_SIM_2;
                }
                mSimInfo = null;
                log("updateSimInfo, running mmi, slot = "+slot);
            } else {
                mSimInfo = SIMInfo.getSIMInfoBySlot(mInCallScreen, slot);
                if(mSimInfo != null) {
                    log("updateSimInfo slot = "+slot+" mSimInfo :");
                    log("    displayName = "+mSimInfo.mDisplayName);
                    log("    color       = "+mSimInfo.mColor);
                }
            }
        }
    } 
    
    /* single talk only */
    String getNetworkOperatorName() {
        String operatorName = null;
        if(FeatureOption.MTK_GEMINI_SUPPORT) {
            GeminiPhone phone = (GeminiPhone) PhoneApp.getInstance().phone;
            if(phone.getStateGemini(Phone.GEMINI_SIM_2) != Phone.State.IDLE) {
                operatorName = SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_ALPHA_2);
            } else if(phone.getStateGemini(Phone.GEMINI_SIM_1) != Phone.State.IDLE) {
                operatorName = SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_ALPHA);
            }
            //Give a chance for get mmi information
            if (operatorName == null && PhoneApp.getInstance().mCM.getState() == Phone.State.IDLE)
            {
            	if (phone.getPendingMmiCodesGemini(Phone.GEMINI_SIM_1).size() != 0)
            	{
            		operatorName = SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_ALPHA);
            	}else if (phone.getPendingMmiCodesGemini(Phone.GEMINI_SIM_2).size() != 0)
            	{
            		operatorName = SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_ALPHA_2);
            	}
            }
        } else {
            operatorName = SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_ALPHA);
        }
        log("operatorName = "+operatorName);
        return operatorName;
    }

    void updateTextViewStyle(int callStatusMode) {
        TextView view0 = (TextView)mViews[0];
        TextView view3 = (TextView)mViews[3];
        view0.setEllipsize(TruncateAt.END);
        view3.setEllipsize(TruncateAt.END);
    }

    int dip2px(float dip) {
        float density = mInCallScreen.getResources().getDisplayMetrics().density;
        return (int)(dip * density + 0.5f);
    }

    void updateCallStatusBackground(int callStatusMode) {
        if(FeatureOption.MTK_GEMINI_SUPPORT) {
            if (mSimInfo != null && (mSimInfo.mColor < 0 || mSimInfo.mColor >= mSimColorMap.length)) {
                log("mSimInfo.mColor invalid, do not update call status background");
                return;
            }
             switch(callStatusMode) {
                case UNKNOWN:
                case SIP_INCOMING_WAITING:
                case SIP_ACTIVE:
                case SIP_HOLD:
                    mCallStatus0.setBackgroundResource(R.drawable.incall_status_color8);
                    break;
                case CELL_INCOMING_WAITING:
                case CELL_ACTIVE:
                case CELL_HOLD:
                    /* TBD */
                    mCallStatus0.setBackgroundResource(mSimColorMap[mSimInfo.mColor]);
                    break;
                case SIP_CELL:
                    mCallStatus0.setBackgroundResource(R.drawable.layer_color8);
                    break;
                case CELL_SIP:
                    mCallStatus0.setBackgroundResource(mSimLightColorMap[mSimInfo.mColor]);
                    break;
                case SIP_SIP:
                    mCallStatus0.setBackgroundResource(R.drawable.layer_color8);
                    break;
                case CELL_CELL:
                    mCallStatus0.setBackgroundResource(mSimLightColorMap[mSimInfo.mColor]);
                    break;
            }
        } else {
            switch(callStatusMode) {
                case SIP_SIP:
                case SIP_CELL:
                case CELL_SIP:
                case CELL_CELL:
                    mCallStatus0.setBackgroundResource(R.drawable.layer_color3);
                    break;
                default:
                    mCallStatus0.setBackgroundResource(R.drawable.incall_status_color3);
                    break;
            }
        }
    }
    
    public void setStartTime( long startTime ){
    	if( 0 < startTime ){
	    	String sTime = getResources().getString(R.string.vt_start_time_from) + " " +
		   		DateFormat.getTimeFormat( mInCallScreen ).format( new Date( startTime ));
			TextView textView = (TextView)mViews[0];
			textView.setText(sTime);
    	} else {
            ((TextView)mViews[0]).setEllipsize(TruncateAt.END);
    		((TextView)mViews[0]).setText(getNetworkOperatorName());
    	}
    }
    
    void log(String msg) {
        Log.d(TAG, msg);
    }
    
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CLEAR_DISCONNECT_MSG:
                    ((TextView)mViews[3]).setText("");
                    break;
            }
        }
    };
}
