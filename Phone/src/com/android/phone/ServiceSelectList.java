package com.android.phone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sip.Dialog;

import com.mediatek.xlog.Xlog;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.preference.DialogPreference;
import android.provider.Telephony.SIMInfo;
import android.provider.Settings;
import android.provider.Telephony;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView;
import android.text.TextUtils;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.gemini.GeminiPhone;

public class ServiceSelectList extends DialogPreference 
            implements AdapterView.OnItemClickListener, DialogInterface.OnClickListener {
    
    final static String TAG = "Settings/ServiceSelectList";
    private LayoutInflater mFlater;
    private String mValue;

    //private List<SimItem> mSimItemList;
    private SelectionListAdapter mAdapter;
    private ListView mListView;
    private int mSelected = -1;
    private int mSwitchTo = -1;
    private int mInitValue = -1;    
    private Drawable mIcon;
    private Context mContext;
    private PhoneInterfaceManager phoneMgr = null;
    
    private static final int DISPLAY_NONE = 0;
    private static final int DISPLAY_FIRST_FOUR = 1;
    private static final int DISPLAY_LAST_FOUR = 2;    
    
    private static final int PIN1_REQUEST_CODE = 302;
    
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;

    public ServiceSelectList(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }   
    public ServiceSelectList(Context context,AttributeSet attrs, int defStyle) {
       super(context, attrs);
       
        mContext = context;
        mFlater = LayoutInflater.from(context);
        
        TypedArray a = context.obtainStyledAttributes(attrs,
                com.android.internal.R.styleable.ListPreference, 0, 0);
        mEntries = a.getTextArray(com.android.internal.R.styleable.ListPreference_entries);
        mEntryValues = a.getTextArray(com.android.internal.R.styleable.ListPreference_entryValues);
        a.recycle(); 
        
        phoneMgr = PhoneApp.getInstance().phoneMgr;
    }
    
    @Override
    public void onBindView(View view) {
        super.onBindView(view);
    }

    
    @Override
     protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);
        mAdapter = new SelectionListAdapter(this.getContext());
        mListView = new ListView(mContext);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        this.mSelected = mAdapter.getHas3GService();

        mListView.setItemsCanFocus(false);
        mListView.setCacheColorHint(0);

        builder.setInverseBackgroundForced(true);
        builder.setView(mListView,0,0,0,0);

        builder.setNegativeButton(R.string.cancel, this);
        builder.setPositiveButton(null, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        
        Xlog.i(TAG, "onDialogClosed : mSelected = " + mSelected);
        Xlog.i(TAG, "onDialogClosed : mInitValue = " + mInitValue);

        if (positiveResult) {
            Xlog.i(TAG, "callChangeListener");
            callChangeListener(this.mAdapter.mSimItemList.get(mSelected).mSimID);
            mInitValue = mSelected;
        }

        this.dismissSelf();
    }
    
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            onDialogClosed(true);
            //this.handleSwitch(mAdapter.mSimItemList.get(mSwitchTo).mSlot);
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            /*AlertDialog slectDialog = (AlertDialog)this.getDialog();
            if (slectDialog != null) {
                slectDialog.dismiss();
            }*/
            onDialogClosed(false);
        }
    }

    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        Xlog.i(TAG, "onclick");
        Xlog.i(TAG,"positon is "+position);
        Xlog.i(TAG,"current select is " + mSelected);
        
        if (v.isEnabled() == false) {
            return ;
        } else if (position == mSelected) {
            dismissSelf();
            return ;
        } else {
            SimItem simItem = mAdapter.mSimItemList.get(position);
            if (simItem.mSimID == SimItem.DESCRIPTION_LIST_ITEM_SIMID) {
                return ;
            } else {
                mSelected = position;
                Xlog.i(TAG,"Switch to " + mSelected);
                int msgId = simItem.mSimID == SimItem.OFF_LIST_ITEM_SIMID ? R.string.confirm_3g_switch_to_off : R.string.confirm_3g_switch;
                AlertDialog newDialog = new AlertDialog.Builder(mContext)
                .setTitle(android.R.string.dialog_alert_title)
                .setPositiveButton(R.string.buttonTxtContinue, this)
                .setNegativeButton(R.string.cancel, this)
                .setCancelable(true)
                .setMessage(msgId)
                .create();
                
                newDialog.show();
                this.onDialogClosed(false);
            }
        }
    }
    
    class SelectionListAdapter extends BaseAdapter {
        
        List<SimItem> mSimItemList;
        
        public SelectionListAdapter(List<SimItem> simItemList) {
            mSimItemList = simItemList;
        }
        
        public SelectionListAdapter(Context ctx) {
            mSimItemList = new ArrayList<SimItem>();
            List<SIMInfo> list = SIMInfo.getInsertedSIMList(ctx);
            for (SIMInfo info : list) {
                mSimItemList.add(new SimItem(info));
            }
            String offText = ctx.getResources().getString(R.string.service_3g_off);
            mSimItemList.add(new SimItem(offText, 0, SimItem.OFF_LIST_ITEM_SIMID));
            //mSimItemList.add(new SimItem(ctx.getResources().getString(R.string.modem_switching_tip), 0, SimItem.DESCRIPTION_LIST_ITEM_SIMID));
        }
        
        public int getCount() {
            return mSimItemList.size();
        }

        public Object getItem(int position) {
            return mSimItemList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public int getHas3GService() {
            int index = -1;
            for (int i = 0; i < mSimItemList.size(); ++i) {
                /*SimItem item = mSimItemList.get(i);
                if (item.mIsSim && item.has3GCapability) {
                    index = i;
                    break;
                } else if (!item.mIsSim && item.mSimID == SimItem.OFF_LIST_ITEM_SIMID) {
                    index = i;
                }*/
                SimItem item = mSimItemList.get(i);
                if (item.has3GCapability) {
                    index = i;
                    break;
                }
            }
            return index;
        }

        private HashMap<Integer, View> viewMap = new HashMap<Integer, View>();

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            
            SimItem simItem = (SimItem)getItem(position);
            if ((simItem != null) && (!simItem.mIsSim) && (simItem.mSimID == SimItem.DESCRIPTION_LIST_ITEM_SIMID)) {
                View itemView = mFlater.inflate(R.layout.preference_non_sim_list,null);
                TextView text = (TextView)itemView.findViewById(R.id.switch_indicate);
                text.setText(simItem.mName);
                viewMap.put(position, itemView);
                
            }
            View itemView = viewMap.get(position);

            if (itemView == null) {
                try {
                    itemView = mFlater.inflate(R.layout.preference_sim_list,null);
                    
                    if (itemView == null)
                        return itemView;

                } catch (InflateException e) {
                    return null;

                }

                simItem = (SimItem)getItem(position);
                
                if (simItem == null) {
                    
                    return itemView;
                    
                }

                TextView textName = (TextView) itemView.findViewById(R.id.simNameSel);
                
                if(textName!=null) {
                    
                    if(simItem.mName != null) {
                        textName.setText(simItem.mName);
                    } else {
                        textName.setVisibility(View.GONE);
                    }

                }
                
                TextView textNum = (TextView) itemView.findViewById(R.id.simNumSel);
                if(textNum!=null) {
                    
                    if((simItem.mIsSim == true) &&((simItem.mNumber != null)&&(simItem.mNumber.length() != 0))) {
                        textNum.setText(simItem.mNumber);
                    } else {
                        textNum.setVisibility(View.GONE);
                    }
                }
                RelativeLayout imageSim = (RelativeLayout)itemView.findViewById(R.id.simIconSel);
                
                if (imageSim != null) {
                    
                    if(simItem.mIsSim == true) {
                        
                        int resColor = Utils.getSimColorResource(simItem.mColor);
                        
                        if(resColor >= 0) {
                            imageSim.setBackgroundResource(resColor);
                        }
                        
                        TextView text3G = (TextView) itemView.findViewById(R.id.sim3gSel);
                        
                        if(text3G != null) {
                            text3G.setVisibility(View.GONE);
                            //MTK_OP02_PROTECT_START
                            if("OP02".equals(PhoneUtils.getOptrProperties()) && position == mSelected){
                                Xlog.i(TAG, "Which card has 3G icon? position = " + mSelected+", slot="+simItem.mSlot);
                                text3G.setVisibility(View.VISIBLE);
                            }
                            //MTK_OP02_PROTECT_END
                        }

                        ImageView imageStatus = (ImageView) itemView.findViewById(R.id.simStatusSel);
                        
                        if(imageStatus!=null) {
                            
                            int res = Utils.getStatusResource(simItem.mState);
                           
                           if(res == -1) {
                               imageStatus.setVisibility(View.GONE);
                           } else {
                               imageStatus.setImageResource(res);              
                           }
                        }
                        
                        TextView textNumFormat = (TextView)itemView.findViewById(R.id.simNumFormatSel);
                        if ((textNumFormat != null) && (simItem.mNumber!=null)) {
                            
                            Xlog.i(TAG, "format is " + simItem.mDispalyNumberFormat);
                            switch (simItem.mDispalyNumberFormat) {
                            case DISPLAY_NONE: {
                                textNumFormat.setVisibility(View.GONE);
                                break;
                                
                            }
                            case DISPLAY_FIRST_FOUR: {
                                
                                if (simItem.mNumber.length()>=4) {
                                    textNumFormat.setText(simItem.mNumber.substring(0, 4));
                                } else {
                                    textNumFormat.setText(simItem.mNumber);
                                }
                                break;
                            }
                            case DISPLAY_LAST_FOUR: {
                                
                                if (simItem.mNumber.length()>=4) {
                                    textNumFormat.setText(simItem.mNumber.substring(simItem.mNumber.length()-4));
                                } else {
                                    textNumFormat.setText(simItem.mNumber);
                                }
                                break;
                            }
                        
                        }           
                        }

                    } else if (simItem.mColor == 8){
                        imageSim.setBackgroundResource(com.mediatek.internal.R.drawable.sim_background_sip);

                        TextView text3G = (TextView) itemView
                                .findViewById(R.id.sim3gSel);
                        if (text3G != null) {

                            text3G.setVisibility(View.GONE);
                        }

                    } else {
                        imageSim.setVisibility(View.GONE);
                    }
                }  
                
                RadioButton ckRadioOn = (RadioButton) itemView
                        .findViewById(R.id.Enable_select);

                if (ckRadioOn != null) {

                    ckRadioOn.setChecked(mSelected == position);
                }

                viewMap.put(position, itemView);
            }
            return itemView;
        }

    }
    
    void SetRadioCheched(int index) {
        int listSize = mListView.getCount();
        
        for (int k=0; k<listSize; k++) {
            
            View ItemView = mListView.getChildAt(k);
            RadioButton btn = (RadioButton)ItemView.findViewById(R.id.Enable_select);
            if(btn!=null){
                btn.setChecked((k == index)?true:false);
            }
        }
    }
    
    void setInitValue(int value) {
        mInitValue = value;
        mSelected = value;
    }
    
    class SimItem {
        public final static long DESCRIPTION_LIST_ITEM_SIMID = -2;
        public final static long OFF_LIST_ITEM_SIMID = -1; 
        public boolean has3GCapability = false;
        
        public boolean mIsSim = true;
        public String mName = null;
        public String mNumber = null;
        public int mDispalyNumberFormat = 0;
        public int mColor = -1;
        public int mSlot = -1;
        public long mSimID = -1;
        public int mState = Phone.SIM_INDICATOR_NORMAL;
        
        //Constructor for not real sim
        public SimItem (String name, int color,long simID) {
            mName = name;
            mColor = color;
            mIsSim = false;
            mSimID = simID;
            if (phoneMgr != null) {
                has3GCapability = mSlot == phoneMgr.get3GCapabilitySIM();
            }
        }
        //constructor for sim
        public SimItem (SIMInfo siminfo) {
            mIsSim = true;
            mName = siminfo.mDisplayName;
            mNumber = siminfo.mNumber;
            mDispalyNumberFormat = siminfo.mDispalyNumberFormat;
            mColor = siminfo.mColor;
            mSlot = siminfo.mSlot;
            mSimID = siminfo.mSimId;
            if (phoneMgr != null) {
                has3GCapability = mSlot == phoneMgr.get3GCapabilitySIM();
            }
        }
    }
    
    void dismissSelf() {
        Xlog.d(TAG, "Dismiss the select list.");     
        AlertDialog dialog = (AlertDialog)this.getDialog();
        if (dialog != null) {
            dialog.dismiss();
        }
    }
}
