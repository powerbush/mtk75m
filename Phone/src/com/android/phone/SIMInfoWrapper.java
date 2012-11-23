package com.android.phone;

import java.util.HashMap;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.CursorAdapter;

import android.provider.Telephony.SIMInfo;

import com.android.internal.telephony.TelephonyIntents;

// copy from Contacts package
public class SIMInfoWrapper {
    private final static String TAG = "SIMInfoWrapper";
    private final static boolean DBG = false;
    private Context mContext;
    private List<SIMInfo> mAllSimInfoList = null;
    private List<SIMInfo> mInsertedSimInfoList = null;
    private HashMap<Integer,SIMInfo> mAllSimInfoMap = null;
    private HashMap<Integer,SIMInfo> mInsertedSimInfoMap = null;
    private boolean mInsertSim = false;
    private int mAllSimCount = 0;
    private int mInsertedSimCount = 0;      
    private CursorAdapter mAdapter = null;
    private static SIMInfoWrapper sSIMInfoWrapper;
    /**
     * Receiver used to update cached simInfo if changes.  
     */
    
    private final BroadcastReceiver mReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent) {
               String action = intent.getAction();
               if(action.equals(Intent.SIM_SETTINGS_INFO_CHANGED)){
                   updateSimInfo(mContext);
               }else if (action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE) ||
                       action.equals(TelephonyIntents.ACTION_SIM_NAME_UPDATE)){//Deal with Modem Reset
                   if(DBG) log("[updateSimInfo][ACTION_RADIO_OFF]");
                   updateSimInfo(mContext);
                   if (mAdapter != null) {
                       if (mAdapter != null && mAdapter instanceof CursorAdapter) {
                           mAdapter.notifyDataSetChanged();
                       }
                       if(DBG) log("[]adapter" + mAdapter);
                   }
               }
            }           
    };
        
    private void updateSimInfo(Context context){
        
        if (mAllSimInfoList != null) {
            mAllSimInfoList = SIMInfo.getAllSIMList(context);
            if (mAllSimInfoList !=null) {
                mAllSimCount = mAllSimInfoList.size();
                mAllSimInfoMap = new HashMap<Integer,SIMInfo>();
                for(SIMInfo item: mAllSimInfoList){
                    int mSimId = getCheckedSimId(item);
                    if( mSimId != -1)
                        mAllSimInfoMap.put(mSimId, item);
                    
                }
                if(DBG) log("[updateSimInfo] update mAllSimInfoList");
            } else {
                if(DBG) log("[updateSimInfo] updated mAllSimInfoList is null");
                return;
            }
        }
        
        if (mInsertedSimInfoList != null) {
            
            mInsertedSimInfoList = SIMInfo.getInsertedSIMList(context);
            if (mInsertedSimInfoList !=null) {
                mInsertedSimCount = mInsertedSimInfoList.size();
                mInsertedSimInfoMap = new HashMap<Integer,SIMInfo>();
                for(SIMInfo item: mInsertedSimInfoList){
                    int simId = getCheckedSimId(item);
                    if( simId != -1)
                        mInsertedSimInfoMap.put(simId, item);
                }
                if(DBG) log("[updateSimInfo] update mInsertedSimInfoList");                 
            } else {
                if(DBG) log("[updateSimInfo] updated mInsertedSimInfoList is null");
                return;
            }
        }
    }
    
    /**
     * SIMInfo wrapper constructor. Build simInfo according to input type
     * @param context
     * @param isInsertSimOrAll
     */
    private SIMInfoWrapper(Context context){
        mContext = context;
        mAllSimInfoList = SIMInfo.getAllSIMList(context);
        mInsertedSimInfoList = SIMInfo.getInsertedSIMList(context);
        
        if (mAllSimInfoList == null || mInsertedSimInfoList == null) {
            log("[SIMInfoWrapper] mSimInfoList OR mInsertedSimInfoList is nulll");
            return;
        }
        
        mAllSimCount = mAllSimInfoList.size();
        mInsertedSimCount = mInsertedSimInfoList.size();
        
        mAllSimInfoMap = new HashMap<Integer,SIMInfo>();
        mInsertedSimInfoMap = new HashMap<Integer,SIMInfo>();
        
        for(SIMInfo item: mAllSimInfoList){
            int simId  = getCheckedSimId(item);
            if (simId != -1)
                mAllSimInfoMap.put(simId, item);
        }
        
        for(SIMInfo item: mInsertedSimInfoList){
            int simId  = getCheckedSimId(item);
            if (simId != -1)
                mInsertedSimInfoMap.put(simId, item);
        }
        
        IntentFilter iFilter=new IntentFilter();
        iFilter.addAction(Intent.SIM_SETTINGS_INFO_CHANGED);
        iFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        iFilter.addAction(TelephonyIntents.ACTION_SIM_NAME_UPDATE);
        mContext.registerReceiver(mReceiver, iFilter);
    }
    
    private int getCheckedSimId(SIMInfo item) {
        if (item != null && item.mSimId > 0) {
            return (int)item.mSimId;
        } else {
            if(DBG) log("[getCheckedSimId]Wrong simId is " + (item == null?-1:item.mSimId));
            return -1;
        }
    }
    /**
     * SIMInfo wrapper constructor. Build simInfo according to input type
     * @param context
     * @param isInsertSimOrAll
     */
    private SIMInfoWrapper(Context context,boolean isInsertSim){
        mContext = context;
        mInsertSim = isInsertSim;
        if(isInsertSim) {
            mInsertedSimInfoList = SIMInfo.getInsertedSIMList(context);
            if (mInsertedSimInfoList != null) {
                mInsertedSimCount = mInsertedSimInfoList.size();
                mInsertedSimInfoMap = new HashMap<Integer,SIMInfo>();
                for(SIMInfo item: mInsertedSimInfoList){
                    int simId = getCheckedSimId(item);
                    if( simId != -1)
                        mInsertedSimInfoMap.put(simId, item);
                }
            } else {
                if(DBG) log("[SIMInfoWrapper] mInsertedSimInfoList is null");
                return;
            }
        } else {
            mAllSimInfoList = SIMInfo.getAllSIMList(context);
            if (mAllSimInfoList !=null) {
                mAllSimCount = mAllSimInfoList.size();
                mAllSimInfoMap = new HashMap<Integer,SIMInfo>();
                for(SIMInfo item: mAllSimInfoList){
                    int simId = getCheckedSimId(item);
                    if( simId != -1)
                        mAllSimInfoMap.put(simId, item);
                }
            } else {
                if(DBG) log("[SIMInfoWrapper] mAllSimInfoList is null");
                return;
            }
        }
        
        IntentFilter iFilter=new IntentFilter();
        iFilter.addAction(Intent.SIM_SETTINGS_INFO_CHANGED);
        iFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        mContext.registerReceiver(mReceiver, iFilter);
    }
    
    /**
     * Public API to get SIMInfoWrapper instance
     * @param context
     * @param isInsertSim
     * @return SIMInfoWrapper instance
     */
    static SIMInfoWrapper getDefault() {
        if(sSIMInfoWrapper == null)
            sSIMInfoWrapper = new SIMInfoWrapper(PhoneApp.getInstance());
        return sSIMInfoWrapper;
    }

    /**
     * Unregister context receiver
     * Should called when the context is end of life.
     */
    public void release(){
        if(mContext != null)
            mContext.unregisterReceiver(mReceiver);
    }
    
    
    public void setListNotifyDataChanged(CursorAdapter adapter) {
        if (adapter != null)
            mAdapter = adapter;
    }
    /**
     * get cached SIM info list
     * @return 
     */
    public List<SIMInfo> getSimInfoList(){
        if (mInsertSim) {
            return mInsertedSimInfoList;
        } else {
            return mAllSimInfoList;
        }
    }
    
    /**
     * get cached SIM info list
     * @return 
     */
    public List<SIMInfo> getAllSimInfoList(){
        return mAllSimInfoList;
    }
    /**
     * get cached SIM info list
     * @return 
     */
    public List<SIMInfo> getInsertedSimInfoList(){
        return mInsertedSimInfoList;
    }
    
    /**
     * get SimInfo cached HashMap
     * @return 
     */
    public HashMap<Integer,SIMInfo> getSimInfoMap(){
        return mAllSimInfoMap;
    }
    
    
    /**
     * get SimInfo cached HashMap
     * @return 
     */
    public HashMap<Integer,SIMInfo> getInsertedSimInfoMap(){
        return mInsertedSimInfoMap;
    }
    
    /**
     * get cached SimInfo from HashMap 
     * @param id
     * @return
     */
    public SIMInfo getSimInfoById(int id){
        return mAllSimInfoMap.get(id);
    }

    public SIMInfo getSimInfoBySlot(int slot) {
        SIMInfo simInfo = null;
        for(int i=0; i<mInsertedSimCount; i++) {
            simInfo = mInsertedSimInfoList.get(i);
            if(simInfo.mSlot == slot)
                return simInfo;
        }
        return simInfo;
    }

    /**
     * get SIM color according to input id
     * @param id
     * @return
     */
    public int getSimColorById(int id){
        SIMInfo simInfo = mAllSimInfoMap.get(id);
        return (simInfo == null)?-1:simInfo.mColor;
    }
    
    /**
     * get SIM display name according to input id 
     * @param id
     * @return
     */
    public String getSimDisplayNameById(int id){
        SIMInfo simInfo = mAllSimInfoMap.get(id);
        return (simInfo == null)?null:simInfo.mDisplayName;
    }
    
    /**
     * get SIM slot according to input id
     * @param id
     * @return 
     */
    public int getSimSlotById(int id){
        SIMInfo simInfo = mAllSimInfoMap.get(id);
        return (simInfo == null)?-1:simInfo.mSlot;
    }
    
    /**
     * get cached SimInfo from HashMap 
     * @param id
     * @return
     */
    public SIMInfo getInsertedSimInfoById(int id){
        return mInsertedSimInfoMap.get(id);
    }
    
    /**
     * get SIM color according to input id
     * @param id
     * @return
     */
    public int getInsertedSimColorById(int id){
        SIMInfo simInfo = mInsertedSimInfoMap.get(id);
        return (simInfo == null)?-1:simInfo.mColor;
    }
    
    /**
     * get SIM display name according to input id 
     * @param id
     * @return
     */
    public String getInsertedSimDisplayNameById(int id){
        SIMInfo simInfo = mInsertedSimInfoMap.get(id);
        return (simInfo == null)?null:simInfo.mDisplayName;
    }
    
    /**
     * get SIM slot according to input id
     * @param id
     * @return 
     */
    public int getInsertedSimSlotById(int id){
        SIMInfo simInfo = mInsertedSimInfoMap.get(id);
        return (simInfo == null)?-1:simInfo.mSlot;
    }
    
    /**
     * get all SIM count according to Input
     * @return 
     */
    public int getAllSimCount(){
        return mAllSimCount;
    }
    
    /**
     * get inserted SIM count according to Input
     * @return 
     */
    public int getInsertedSimCount(){
        return mInsertedSimCount;
    }
    
    
    private void notifyDataChange() {
        
    }
    
    private void log (String msg) {
        Log.i(TAG,msg);
    }
 
}
