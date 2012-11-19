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

import static android.content.res.Configuration.KEYBOARDHIDDEN_NO;
import static com.android.mms.transaction.ProgressCallbackEntity.PROGRESS_ABORT;
import static com.android.mms.transaction.ProgressCallbackEntity.PROGRESS_COMPLETE;
import static com.android.mms.transaction.ProgressCallbackEntity.PROGRESS_START;
import static com.android.mms.transaction.ProgressCallbackEntity.PROGRESS_STATUS_ACTION;
import static com.android.mms.ui.MessageListAdapter.COLUMN_ID;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MMS_LOCKED;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MSG_TYPE;
import static com.android.mms.ui.MessageListAdapter.PROJECTION;

import com.android.internal.telephony.ITelephony;
import com.android.internal.widget.ContactHeaderWidget;
import com.android.mms.transaction.SmsReceiverService;
import com.android.mms.util.ThreadCountManager;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.StatusBarManager;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.drm.mobile1.DrmException;
import android.drm.mobile1.DrmRawContent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.CamcorderProfile;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Browser;
import android.provider.Browser.BookmarkColumns;
import android.provider.ContactsContract;
import android.provider.DrmStore;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Telephony;
import android.provider.Telephony.SIMInfo;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.format.DateFormat;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Config;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RecipientsView.OnRecipientsChangeListener;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CursorAdapter;
import android.view.ViewGroup;
import com.android.mms.transaction.SendTransaction;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.mms.ExceedMessageSizeException;
import com.android.mms.LogTag;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.data.WorkingMessage;
import com.android.mms.data.WorkingMessage.MessageStatusListener;
import com.google.android.mms.ContentType;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduPart;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.SendReq;
import com.android.mms.model.FileAttachmentModel;
import com.android.mms.model.SlideModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.ui.ConversationList.BaseProgressQueryHandler;
import com.android.mms.ui.MessageUtils.ResizeImageResultCallback;
import com.android.mms.ui.RecipientsEditor.RecipientContextMenuInfo;
import com.android.mms.util.SendingProgressTokenManager;
import com.android.mms.util.SmileyParser;
import android.provider.ContactsContract.Data;

//add for gemini
import android.app.ProgressDialog;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.telephony.gemini.GeminiSmsManager;
import com.android.internal.telephony.IccCard;
import com.mediatek.CellConnService.CellConnMgr;
import com.mediatek.featureoption.FeatureOption;
import com.mediatek.telephony.TelephonyManagerEx;
import com.android.mms.MmsApp;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.PhoneFactory;
import android.text.InputFilter.LengthFilter;
import android.pim.vcard.VCardComposer;
import android.pim.vcard.exception.VCardException;
import android.pim.vcard.VCardConfig;
import com.android.mms.transaction.MmsSystemEventReceiver.forShutDownSaveDraft;
import com.mediatek.xlog.Xlog;
import com.mediatek.xlog.SXlog;

/**
 * This is the main UI for:
 * 1. Composing a new message;
 * 2. Viewing/managing message history of a conversation.
 *
 * This activity can handle following parameters from the intent
 * by which it's launched.
 * thread_id long Identify the conversation to be viewed. When creating a
 *         new message, this parameter shouldn't be present.
 * msg_uri Uri The message which should be opened for editing in the editor.
 * address String The addresses of the recipients in current conversation.
 * exit_on_sent boolean Exit this activity after the message is sent.
 */
public class ComposeMessageActivity extends Activity
        implements View.OnClickListener, TextView.OnEditorActionListener,
        MessageStatusListener, Contact.UpdateListener, OnRecipientsChangeListener,
        forShutDownSaveDraft {
    public static final int NO_REQUEST_CODE               =  0;
    public static final int REQUEST_CODE_ATTACH_IMAGE     = 10;
    public static final int REQUEST_CODE_TAKE_PICTURE     = 11;
    public static final int REQUEST_CODE_ATTACH_VIDEO     = 12;
    public static final int REQUEST_CODE_TAKE_VIDEO       = 13;
    public static final int REQUEST_CODE_ATTACH_SOUND     = 14;
    public static final int REQUEST_CODE_RECORD_SOUND     = 15;
    public static final int REQUEST_CODE_CREATE_SLIDESHOW = 16;
    public static final int REQUEST_CODE_ECM_EXIT_DIALOG  = 17;
    public static final int REQUEST_CODE_ADD_CONTACT      = 18;
    public static final int REQUEST_CODE_PICK_CONTACT     = 19;
    public static final int REQUEST_CODE_ATTACH_RINGTONE  = 20;
    public static final int REQUEST_CODE_ATTACH_VCARD     = 21;
    public static final int REQUEST_CODE_TEXT_VCARD       = 22;
    public static final int REQUEST_CODE_MULTI_SAVE       = 23;
    public static final int REQUEST_CODE_LOAD_DRAFT       = 24;
    public static final int REQUEST_CODE_ATTACH_VCALENDAR = 25;

    private static final String TAG = "Mms/compose";

    private static final boolean DEBUG = false;
    private static final boolean TRACE = false;
    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;

    // Menu ID
    private static final int MENU_ADD_SUBJECT           = 0;
    private static final int MENU_DELETE_THREAD         = 1;
    private static final int MENU_ADD_ATTACHMENT        = 2;
    private static final int MENU_DISCARD               = 3;
    private static final int MENU_SEND                  = 4;
    private static final int MENU_CALL_RECIPIENT        = 5;
    private static final int MENU_CONVERSATION_LIST     = 6;
    private static final int MENU_ADD_QUICK_TEXT        = 8;
    private static final int MENU_ADD_TEXT_VCARD        = 9;

    // Context menu ID
    private static final int MENU_VIEW_CONTACT          = 12;
    private static final int MENU_ADD_TO_CONTACTS       = 13;
    private static final int MENU_EDIT_MESSAGE          = 14;
    private static final int MENU_VIEW_SLIDESHOW        = 16;
    private static final int MENU_VIEW_MESSAGE_DETAILS  = 17;
    private static final int MENU_DELETE_MESSAGE        = 18;
    private static final int MENU_SEARCH                = 19;
    private static final int MENU_DELIVERY_REPORT       = 20;
    private static final int MENU_FORWARD_MESSAGE       = 21;
    private static final int MENU_CALL_BACK             = 22;
    private static final int MENU_SEND_EMAIL            = 23;
    private static final int MENU_COPY_MESSAGE_TEXT     = 24;
    private static final int MENU_COPY_TO_SDCARD        = 25;
    private static final int MENU_INSERT_SMILEY         = 26;
    private static final int MENU_ADD_ADDRESS_TO_CONTACTS = 27;
    private static final int MENU_LOCK_MESSAGE          = 28;
    private static final int MENU_UNLOCK_MESSAGE        = 29;
    private static final int MENU_COPY_TO_DRM_PROVIDER  = 30;
    private static final int MENU_SAVE_MESSAGE_TO_SIM   = 31;
    private static final int MENU_PREVIEW = 32;
    private static final int MENU_SEND_SMS              = 33;
    private static final int MENU_IMPORT_VCARD          = 34;
    private static final int MENU_ADD_TO_BOOKMARK       = 35;
    private static final int RECIPIENTS_MAX_LENGTH      = 5000/*312*/;
    private static final int MESSAGE_LIST_QUERY_TOKEN   = 9527;
    private static final int DELETE_MESSAGE_TOKEN       = 9700;
    private static final int CHARS_REMAINING_BEFORE_COUNTER_SHOWN = 10;
    private static final int DEFAULT_LENGTH             = 40;
    private static final int RECIPIENTS_LIMIT_FOR_SMS   = MmsConfig.getSmsRecipientLimit();
    private static final long NO_DATE_FOR_DIALOG        = -1L;
    private static final String EXIT_ECM_RESULT         = "exit_ecm_result";
    private static final String VCARD_INTENT            = "com.android.contacts.pickphoneandemail";
    private static final String FOR_MULTIDELETE         = "ForMultiDelete";
    private static final String NUMBER_ADD_CONTACT_ACTION ="android.intent.action.INSERT_OR_EDIT";

    // for save message to sim card
    private static final int SIM_SELECT_FOR_SEND_MSG                    = 1;
    private static final int SIM_SELECT_FOR_SAVE_MSG_TO_SIM             = 2;
    private static final int MSG_QUIT_SAVE_MESSAGE_THREAD               = 100;
    private static final int MSG_SAVE_MESSAGE_TO_SIM                    = 102;
    private static final int MSG_SAVE_MESSAGE_TO_SIM_AFTER_SELECT_SIM   = 104;
    private static final int MSG_SAVE_MESSAGE_TO_SIM_SUCCEED            = 106;
    private static final int MSG_SAVE_MESSAGE_TO_SIM_FAILED_GENERIC     = 108;
    private static final int MSG_SAVE_MESSAGE_TO_SIM_FAILED_SIM_FULL    = 110;
    private static final String SELECT_TYPE                             = "Select_type";
    
    public static final int MIN_SIZE_FOR_CAPTURE_VIDEO    = 1024 * 10;  // 10K

    private ContentResolver mContentResolver;
    private BackgroundQueryHandler mBackgroundQueryHandler;
    private Conversation mConversation;     // Conversation we are working in

    private boolean mExitOnSent;            // Should we finish() after sending a message?

    private View mTopPanel;                 // View containing the recipient and subject editors
    private View mBottomPanel;              // View containing the text editor, send button, et.
    //add for multi-delete
    private View mDeletePanel;              // View containing the delete and cancel buttons
    private View mSelectPanel;              // View containing the select all check box
    private Button mDeleteButton;
    private Button mCancelButton;
    private CheckBox mSelectedAll;
    
    private EditText mTextEditor;           // Text editor to type your message into
    private TextView mTextCounter;          // Shows the number of characters used in text editor
    private Button mSendButton;             // Press to detonate

    //add for gemini
    private int mSelectedSimId;
    
    //add for Gemini Enhancement
    private View mRecipients;               // View containing the recipient editors, request contacts icon
    private View mRecipientsAvatar;         // View Recipients stub
    private ContactHeaderWidget mContactHeader;
    private ImageButton mPickContacts;      // title bar button
    private ImageButton mJumpToContacts;    // title bar button
    private int mSimCount;
    private List<SIMInfo> mSimInfoList;
    private int mAssociatedSimId;
    private long mMessageSimId;
    private long mDataConnectionSimId;
    private StatusBarManager mStatusBarManager;
    private ComponentName mComponentName;
    
    private EditText mSubjectTextEditor;    // Text editor for MMS subject

    private AttachmentEditor mAttachmentEditor;

    private MessageListView mMsgListView;        // ListView for messages in this conversation
    public MessageListAdapter mMsgListAdapter;  // and its corresponding ListAdapter

    private RecipientsEditor mRecipientsEditor;  // UI control for editing recipients

    private boolean mIsKeyboardOpen;             // Whether the hardware keyboard is visible
    private boolean mIsLandscape;                // Whether we're in landscape mode

    private boolean mPossiblePendingNotification;   // If the message list has changed, we may have
                                                    // a pending notification to deal with.
    private boolean mIsTooManyRecipients;   // Whether the recipients are too many

    private boolean mToastForDraftSave;   // Whether to notify the user that a draft is being saved

    private boolean mSentMessage;       // true if the user has sent a message while in this
                                        // activity. On a new compose message case, when the first
                                        // message is sent is a MMS w/ attachment, the list blanks
                                        // for a second before showing the sent message. But we'd
                                        // think the message list is empty, thus show the recipients
                                        // editor thinking it's a draft message. This flag should
                                        // help clarify the situation.
    private boolean isInitRecipientsEditor = true;    // true, init mRecipientsEditor and add recipients; 
                                                      // false, init mRecipientsEditor, but recipients

    boolean mClickCanResponse = true; // can click button or some view items

    // State variable indicating an image is being compressed, which may take a while.
    private boolean mCompressingImage = false;
    
    private boolean mAppendAttachmentSign = false;
    
    private WorkingMessage mWorkingMessage;         // The message currently being composed.

    private AlertDialog mSIMSelectDialog;
    private AlertDialog mQuickTextDialog;
    private AlertDialog mSmileyDialog;
    private AlertDialog mDetailDialog;
    private AlertDialog mSendDialog;
    private  AlertDialog  mProgressDialog; //The dialog  show for the progress of sharing pictures.
    private boolean mWaitingForSubActivity;
    private int mLastRecipientCount;            // Used for warning the user on too many recipients.
    private AttachmentTypeSelectorAdapter mAttachmentTypeSelectorAdapter;
    private static final String STR_RN = "\\r\\n"; // for "\r\n"
    private static final String STR_CN = "\n"; // the char value of '\n'
    public static boolean mDestroy = false;
    private boolean misPickContatct = false;
    private ThreadCountManager mThreadCountManager = ThreadCountManager.getInstance();
    private Long mThreadId = -1l;

    private boolean mSendingMessage;    // Indicates the current message is sending, and shouldn't send again.
    private boolean mWaitingForSendMessage;

    private Intent mAddContactIntent;   // Intent used to add a new contact
    private String mDebugRecipients;
    private ArrayList<String> mURLs = new ArrayList<String>();
    private String mSizeLimitTemp;
    private int mMmsSizeLimit;
    private final String ARABIC = "ar";
    private static CellConnMgr mCellMgr = null;
    private static int mCellMgrRegisterCount = 0;
    private Handler mIndicatorHandler;
    private static Activity sCompose = null;
    private Handler mSaveMsgHandler = null;
    private Thread mSaveMsgThread = null;
    private boolean mSendButtonCanResponse = true;
    public static final String SMS_ADDRESS = "sms_address"; 
    public static final String SMS_BODY = "sms_body"; 
    public static final String FORWARD_MESSAGE = "forwarded_message";
    
    private Runnable mAddAttachmentRunnable = null;
    
    private int mToastCountForResizeImage = 0; // For indicate whether show toast message for resize image or not. If
                                               // mToastCountForResizeImage equals 0, show toast.
    
    private Handler mUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_SAVE_MESSAGE_TO_SIM_SUCCEED:
                Toast.makeText(ComposeMessageActivity.this, R.string.save_message_to_sim_successful, Toast.LENGTH_SHORT).show();
                break;

            case MSG_SAVE_MESSAGE_TO_SIM_FAILED_GENERIC:
                Toast.makeText(ComposeMessageActivity.this, R.string.save_message_to_sim_unsuccessful, Toast.LENGTH_SHORT).show();
                break;

            case MSG_SAVE_MESSAGE_TO_SIM_FAILED_SIM_FULL:
                Toast.makeText(ComposeMessageActivity.this, R.string.save_message_to_sim_unsuccessful, Toast.LENGTH_SHORT).show();
                break;

            case MSG_SAVE_MESSAGE_TO_SIM:
                String type = (String)msg.obj;
                long msgId = msg.arg1;
                saveMessageToSim(type, msgId);
                break;

            default:
                Log.d(TAG, "inUIHandler msg unhandled.");
                break;
            }
        }
    };

    @SuppressWarnings("unused")
    private static void log(String logMsg) {
        Thread current = Thread.currentThread();
        long tid = current.getId();
        StackTraceElement[] stack = current.getStackTrace();
        String methodName = stack[3].getMethodName();
        // Prepend current thread ID and name of calling method to the message.
        logMsg = "[" + tid + "] [" + methodName + "] " + logMsg;
        Log.d(TAG, logMsg);
    }

    //==========================================================
    // Inner classes
    //==========================================================

    private void editSlideshow() {
        Uri dataUri = mWorkingMessage.saveAsMms(false);
        Intent intent = new Intent(this, SlideshowEditActivity.class);
        intent.setData(dataUri);
        startActivityForResult(intent, REQUEST_CODE_CREATE_SLIDESHOW);
    }

    private final class SaveMsgThread extends Thread {
        private String msgType = null;
        private long msgId = 0;
        public SaveMsgThread(String type, long id) {
            msgType = type;
            msgId = id;
        }
        public void run() {
            Looper.prepare();
            if (null != Looper.myLooper()) {
                mSaveMsgHandler = new SaveMsgHandler(Looper.myLooper());
            }
            Message msg = mSaveMsgHandler.obtainMessage(MSG_SAVE_MESSAGE_TO_SIM);
            msg.arg1 = (int)msgId;
            msg.obj = msgType;
            if (FeatureOption.MTK_GEMINI_SUPPORT && mSimCount > 1) {
                mUiHandler.sendMessage(msg);
            } else {
                mSaveMsgHandler.sendMessage(msg);
            }
            Looper.loop();
        }
    }

    private final class SaveMsgHandler extends Handler {
        public SaveMsgHandler(Looper looper) {
            super(looper);
        }
        
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_QUIT_SAVE_MESSAGE_THREAD: {
                    Xlog.v(MmsApp.TXN_TAG, "exit save message thread");
                    getLooper().quit();
                    break;
                }

                case MSG_SAVE_MESSAGE_TO_SIM: {
                    String type = (String)msg.obj;
                    long msgId = msg.arg1;
                    //saveMessageToSim(type, msgId);
                    getMessageAndSaveToSim(type, msgId);
                    break;
                }

                case MSG_SAVE_MESSAGE_TO_SIM_AFTER_SELECT_SIM: {
                    Intent it = (Intent)msg.obj;
                    getMessageAndSaveToSim(it);
                    break;
                }

                default:
                    break;
            }
        }
    }

    private final Handler mAttachmentEditorHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AttachmentEditor.MSG_EDIT_SLIDESHOW: {
                    editSlideshow();
                    break;
                }

                case AttachmentEditor.MSG_SEND_SLIDESHOW: {
                    if (isPreparedForSending()) {
                        checkRecipientsCount();
                    }
                    break;
                }
                case AttachmentEditor.MSG_VIEW_IMAGE:
                case AttachmentEditor.MSG_PLAY_VIDEO:
                case AttachmentEditor.MSG_PLAY_AUDIO:
                case AttachmentEditor.MSG_PLAY_SLIDESHOW:
                    if (mClickCanResponse) {
                        mClickCanResponse = false;
                        MessageUtils.viewMmsMessageAttachment(ComposeMessageActivity.this, mWorkingMessage);
                    }
                    hideInputMethod();
                    break;

                case AttachmentEditor.MSG_REPLACE_IMAGE:
                    getSharedPreferences("SetDefaultLayout", 0).edit().putBoolean("SetDefaultLayout", false).commit();
                case AttachmentEditor.MSG_REPLACE_VIDEO:
                case AttachmentEditor.MSG_REPLACE_AUDIO:
                    hideInputMethod();
                    showAddAttachmentDialog(false);
                    break;

                case AttachmentEditor.MSG_REMOVE_ATTACHMENT:
                    mWorkingMessage.setAttachment(WorkingMessage.TEXT, null, false);
                    mWorkingMessage.resetMessageUri();
                    break;

                default:
                    break;
            }
        }
    };

    private final Handler mMessageListItemHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String type;
            switch (msg.what) {
                case MessageListItem.MSG_LIST_EDIT_MMS:
                    type = "mms";
                    break;
                case MessageListItem.MSG_LIST_EDIT_SMS:
                    type = "sms";
                    break;
                case MessageListItem.ITEM_CLICK: {
                    //add for multi-delete
                    mMsgListAdapter.changeSelectedState(msg.arg1);
                    if (mMsgListAdapter.getSelectedNumber() > 0) {
                        mDeleteButton.setEnabled(true);
                        if (mMsgListAdapter.getSelectedNumber() == mMsgListAdapter.getCount()) {
                            mSelectedAll.setChecked(true);
                            return;
                        }
                    } else {
                        mDeleteButton.setEnabled(false);
                    }
                    mSelectedAll.setChecked(false);
                    return;
                }
                    
                default:
                    Log.w(TAG, "Unknown message: " + msg.what);
                    return;
            }

            MessageItem msgItem = getMessageItem(type, (Long) msg.obj, false);
            if (msgItem != null) {
                editMessageItem(msgItem);
                drawBottomPanel();
            }
        }
    };

    private final OnKeyListener mSubjectKeyListener = new OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }

            // When the subject editor is empty, press "DEL" to hide the input field.
            if ((keyCode == KeyEvent.KEYCODE_DEL) && (mSubjectTextEditor.length() == 0)) {
                showSubjectEditor(false);
                mWorkingMessage.setSubject(null, true);
                resetCounter();
                return true;
            }

            return false;
        }
    };

    /**
     * Return the messageItem associated with the type ("mms" or "sms") and message id.
     * @param type Type of the message: "mms" or "sms"
     * @param msgId Message id of the message. This is the _id of the sms or pdu row and is
     * stored in the MessageItem
     * @param createFromCursorIfNotInCache true if the item is not found in the MessageListAdapter's
     * cache and the code can create a new MessageItem based on the position of the current cursor.
     * If false, the function returns null if the MessageItem isn't in the cache.
     * @return MessageItem or null if not found and createFromCursorIfNotInCache is false
     */
    private MessageItem getMessageItem(String type, long msgId,
            boolean createFromCursorIfNotInCache) {
        return mMsgListAdapter.getCachedMessageItem(type, msgId,
                createFromCursorIfNotInCache ? mMsgListAdapter.getCursor() : null);
    }


    private void resetCounter() {
        mTextEditor.setText(mWorkingMessage.getText());
        // once updateCounter.
        updateCounter(mWorkingMessage.getText(), 0, 0, 0);
        if (mWorkingMessage.requiresMms()) {
            mTextCounter.setVisibility(View.GONE);
        } else {
            mTextCounter.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateCounter(CharSequence text, int start, int before, int count) {
        int[] params = SmsMessage.calculateLength(text, false);
            /* SmsMessage.calculateLength returns an int[4] with:
             *   int[0] being the number of SMS's required,
             *   int[1] the number of code units used,
             *   int[2] is the number of code units remaining until the next message.
             *   int[3] is the encoding type that should be used for the message.
             */
        int msgCount = params[0];
        int unitesUsed = params[1];
        int remainingInCurrentMessage = params[2];
        String optr = SystemProperties.get("ro.operator.optr");
        mWorkingMessage.setLengthRequiresMms(
                msgCount >= MmsConfig.getSmsToMmsTextThreshold(), true);
       //MTK_OP02_PROTECT_END
        // Show the counter
        // Update the remaining characters and number of messages required.
        if (mWorkingMessage.requiresMms()) {
            mTextCounter.setVisibility(View.GONE);
        } else {
            mTextCounter.setVisibility(View.VISIBLE);
        }
        String counterText = remainingInCurrentMessage + "/" + msgCount;
        mTextCounter.setText(counterText);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode)
    {
        // requestCode >= 0 means the activity in question is a sub-activity.
        if (requestCode >= 0) {
            mWaitingForSubActivity = true;
        }
        if (null != intent && null != intent.getData()
                && intent.getData().getScheme().equals("mailto")) {
            try {
                super.startActivityForResult(intent, requestCode);
            } catch (ActivityNotFoundException e) {
                Xlog.e(TAG, "Failed to startActivityForResult: " + intent);
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

    private void toastConvertInfo(boolean toMms) {
        final int resId = toMms ? R.string.converting_to_picture_message
                : R.string.converting_to_text_message;
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    private class DeleteMessageListener implements OnClickListener {
        private final Uri mDeleteUri;
        private final boolean mDeleteLocked;

        public DeleteMessageListener(Uri uri, boolean deleteLocked) {
            mDeleteUri = uri;
            mDeleteLocked = deleteLocked;
        }

        public DeleteMessageListener(long msgId, String type, boolean deleteLocked) {
            if ("mms".equals(type)) {
                mDeleteUri = ContentUris.withAppendedId(Mms.CONTENT_URI, msgId);
            } else {
                mDeleteUri = ContentUris.withAppendedId(Sms.CONTENT_URI, msgId);
            }
            mDeleteLocked = deleteLocked;
        }

        public void onClick(DialogInterface dialog, int whichButton) {
            mBackgroundQueryHandler.startDelete(DELETE_MESSAGE_TOKEN,
                    null, mDeleteUri, mDeleteLocked ? null : "locked=0", null);
            dialog.dismiss();
        }
    }

    private class DiscardDraftListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int whichButton) {
            try {
                mWorkingMessage.discard();
                dialog.dismiss();
                finish();
            } catch(IllegalStateException e) {
                Xlog.e(TAG, e.getMessage());
            }
            
        }
    }

    private class SendIgnoreInvalidRecipientListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int whichButton) {
            checkConditionsAndSendMessage(true);
            dialog.dismiss();
        }
    }

    private class CancelSendingListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int whichButton) {
            if (isRecipientsEditorVisible()) {
                mRecipientsEditor.requestFocus();
            }
            dialog.dismiss();
            updateSendButtonState(true);
        }
    }

    private void confirmSendMessageIfNeeded() {
        if (!isRecipientsEditorVisible()) {
            checkConditionsAndSendMessage(true);
            return;
        }

        boolean isMms = mWorkingMessage.requiresMms();
        if (mRecipientsEditor.hasInvalidRecipient(isMms)) {
            updateSendButtonState();
            String title = getResourcesString(R.string.has_invalid_recipient, 
                    mRecipientsEditor.formatInvalidNumbers(isMms));
            new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(R.string.invalid_recipient_message)
                .setPositiveButton(R.string.try_to_send, new SendIgnoreInvalidRecipientListener())
                .setNegativeButton(R.string.no, new CancelSendingListener())
                .show();
        } else {
            checkConditionsAndSendMessage(true);
        }
    }
    
    private final TextWatcher mRecipientsWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // This is a workaround for bug 1609057.  Since onUserInteraction() is
            // not called when the user touches the soft keyboard, we pretend it was
            // called when textfields changes.  This should be removed when the bug
            // is fixed.
            onUserInteraction();
        }

        public void afterTextChanged(Editable s) {
            // Bug 1474782 describes a situation in which we send to
            // the wrong recipient.  We have been unable to reproduce this,
            // but the best theory we have so far is that the contents of
            // mRecipientList somehow become stale when entering
            // ComposeMessageActivity via onNewIntent().  This assertion is
            // meant to catch one possible path to that, of a non-visible
            // mRecipientsEditor having its TextWatcher fire and refreshing
            // mRecipientList with its stale contents.
            if (!isRecipientsEditorVisible()) {
                // Make sure the crash is uploaded to the service so we
                // can see if this is happening in the field.
                Log.w(TAG,
                     "RecipientsWatcher: afterTextChanged called with invisible mRecipientsEditor");
                return;
            }
            // If we have gone to zero recipients, disable send button.
            updateSendButtonState();
        }
    };

    private final OnCreateContextMenuListener mRecipientsMenuCreateListener =
        new OnCreateContextMenuListener() {
        public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenuInfo menuInfo) {
            if (menuInfo != null) {
                Contact c = ((RecipientContextMenuInfo) menuInfo).recipient;
                RecipientsMenuClickListener l = new RecipientsMenuClickListener(c);

                menu.setHeaderTitle(c.getName());

                if (c.existsInDatabase()) {
                    menu.add(0, MENU_VIEW_CONTACT, 0, R.string.menu_view_contact)
                            .setOnMenuItemClickListener(l);
                } else if (canAddToContacts(c)){
                    menu.add(0, MENU_ADD_TO_CONTACTS, 0, R.string.menu_add_to_contacts)
                            .setOnMenuItemClickListener(l);
                }
            }
        }
    };

    private final class RecipientsMenuClickListener implements MenuItem.OnMenuItemClickListener {
        private final Contact mRecipient;

        RecipientsMenuClickListener(Contact recipient) {
            mRecipient = recipient;
        }

        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                // Context menu handlers for the recipients editor.
                case MENU_VIEW_CONTACT: {
                    Uri contactUri = mRecipient.getUri();
                    Intent intent = new Intent(Intent.ACTION_VIEW, contactUri);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    startActivity(intent);
                    return true;
                }
                case MENU_ADD_TO_CONTACTS: {
                    // mAddContactIntent = ConversationList.createAddContactIntent(
                    // mRecipient.getNumber());
                    /*
                    mAddContactIntent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                    mAddContactIntent.putExtra(ContactsContract.Intents.Insert.PHONE, mRecipient.getNumber());
                    ComposeMessageActivity.this.startActivityForResult(mAddContactIntent,
                            REQUEST_CODE_ADD_CONTACT);
                    */
                    MessageUtils.addNumberOrEmailtoContact(mRecipient.getNumber(), REQUEST_CODE_ADD_CONTACT, ComposeMessageActivity.this);
                    return true;
                }
            }
            return false;
        }
    }

    private boolean canAddToContacts(Contact contact) {
        // There are some kind of automated messages, like STK messages, that we don't want
        // to add to contacts. These names begin with special characters, like, "*Info".
        final String name = contact.getName();
        if (!TextUtils.isEmpty(contact.getNumber())) {
            char c = contact.getNumber().charAt(0);
            if (isSpecialChar(c)) {
                return false;
            }
        }
        if (!TextUtils.isEmpty(name)) {
            char c = name.charAt(0);
            if (isSpecialChar(c)) {
                return false;
            }
        }
        if (!(Mms.isEmailAddress(name) || (Mms.isPhoneNumber(name) || isPhoneNumber(name)) ||
                MessageUtils.isLocalNumber(contact.getNumber()))) {     // Handle "Me"
            return false;
        }
        return true;
    }

    private boolean isPhoneNumber(String num) {
        num = num.trim();
        if (TextUtils.isEmpty(num)) {
            return false;
        }
        final char[] digits = num.toCharArray();
        for (char c : digits) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    private boolean isSpecialChar(char c) {
        return c == '*' || c == '%' || c == '$';
    }

    private void addPositionBasedMenuItems(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;

        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo");
            return;
        }
        final int position = info.position;

        addUriSpecificMenuItems(menu, v, position);
    }

    private Uri getSelectedUriFromMessageList(ListView listView, int position) {
        // If the context menu was opened over a uri, get that uri.
        MessageListItem msglistItem = (MessageListItem) listView.getChildAt(position);
        if (msglistItem == null) {
            // FIXME: Should get the correct view. No such interface in ListView currently
            // to get the view by position. The ListView.getChildAt(position) cannot
            // get correct view since the list doesn't create one child for each item.
            // And if setSelection(position) then getSelectedView(),
            // cannot get corrent view when in touch mode.
            return null;
        }

        TextView textView;
        CharSequence text = null;
        int selStart = -1;
        int selEnd = -1;

        //check if message sender is selected
        textView = (TextView) msglistItem.findViewById(R.id.text_view);
        if (textView != null) {
            text = textView.getText();
            selStart = textView.getSelectionStart();
            selEnd = textView.getSelectionEnd();
        }

        if (selStart == -1) {
            //sender is not being selected, it may be within the message body
            textView = (TextView) msglistItem.findViewById(R.id.body_text_view);
            if (textView != null) {
                text = textView.getText();
                selStart = textView.getSelectionStart();
                selEnd = textView.getSelectionEnd();
            }
        }

        // Check that some text is actually selected, rather than the cursor
        // just being placed within the TextView.
        if (selStart != selEnd) {
            int min = Math.min(selStart, selEnd);
            int max = Math.max(selStart, selEnd);

            URLSpan[] urls = ((Spanned) text).getSpans(min, max,
                                                        URLSpan.class);

            if (urls.length == 1) {
                return Uri.parse(urls[0].getURL());
            }
        }

        //no uri was selected
        return null;
    }

    private void addUriSpecificMenuItems(ContextMenu menu, View v, int position) {
        Uri uri = getSelectedUriFromMessageList((ListView) v, position);

        if (uri != null) {
            Intent intent = new Intent(null, uri);
            intent.addCategory(Intent.CATEGORY_SELECTED_ALTERNATIVE);
            menu.addIntentOptions(0, 0, 0,
                    new android.content.ComponentName(this, ComposeMessageActivity.class),
                    null, intent, 0, null);
        }
    }

    private final void addCallAndContactMenuItems(
            ContextMenu menu, MsgListMenuClickListener l, MessageItem msgItem) {
        // Add all possible links in the address & message
        StringBuilder textToSpannify = new StringBuilder();
        if (msgItem.mBoxId == Mms.MESSAGE_BOX_INBOX) {
            textToSpannify.append(msgItem.mAddress + ": ");
        }
        textToSpannify.append(msgItem.mBody);

        SpannableString msg = new SpannableString(textToSpannify.toString());
        Linkify.addLinks(msg, Linkify.ALL);
        ArrayList<String> uris =
            MessageUtils.extractUris(msg.getSpans(0, msg.length(), URLSpan.class));
        mURLs.clear();
        while (uris.size() > 0) {
            String uriString = uris.remove(0);
            // Remove any dupes so they don't get added to the menu multiple times
            while (uris.contains(uriString)) {
                uris.remove(uriString);
            }

            int sep = uriString.indexOf(":");
            String prefix = null;
            if (sep >= 0) {
                prefix = uriString.substring(0, sep);
                if ("mailto".equalsIgnoreCase(prefix) || "tel".equalsIgnoreCase(prefix)){
                    uriString = uriString.substring(sep + 1);
                }
            }
            boolean addToContacts = false;
            if ("mailto".equalsIgnoreCase(prefix)) {
                String sendEmailString = getString(
                        R.string.menu_send_email).replace("%s", uriString);
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("mailto:" + uriString));
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                menu.add(0, MENU_SEND_EMAIL, 0, sendEmailString)
                    .setOnMenuItemClickListener(l)
                    .setIntent(intent);
                addToContacts = !haveEmailContact(uriString);
            } else if ("tel".equalsIgnoreCase(prefix)) {
                String callBackString = getString(
                        R.string.menu_call_back).replace("%s", uriString);
                Intent intent = new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:" + uriString));
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                menu.add(0, MENU_CALL_BACK, 0, callBackString)
                    .setOnMenuItemClickListener(l)
                    .setIntent(intent);
                
                if (msgItem.mBody != null && msgItem.mBody.replaceAll("\\-", "").contains(uriString)) {
                    String sendSmsString = getString(
                        R.string.menu_send_sms).replace("%s", uriString);
                    Intent intentSms = new Intent(Intent.ACTION_SENDTO,
                        Uri.parse("smsto:" + uriString));
                    intentSms.setClassName(ComposeMessageActivity.this, "com.android.mms.ui.SendMessageToActivity");
                    intentSms.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    menu.add(0, MENU_SEND_SMS, 0, sendSmsString)
                        .setOnMenuItemClickListener(l)
                        .setIntent(intentSms);
                }
                addToContacts = !isNumberInContacts(uriString);
            } else {
                //add URL to book mark
                if (msgItem.isSms() && mURLs.size() <= 0) {
                    menu.add(0, MENU_ADD_TO_BOOKMARK, 0, R.string.menu_add_to_bookmark)
                    .setOnMenuItemClickListener(l);
                }
                mURLs.add(uriString);
            }
            if (addToContacts) {
                //Intent intent = ConversationList.createAddContactIntent(uriString);
                Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                intent.putExtra(ContactsContract.Intents.Insert.PHONE, uriString);
                String addContactString = getString(
                        R.string.menu_add_address_to_contacts).replace("%s", uriString);
                menu.add(0, MENU_ADD_ADDRESS_TO_CONTACTS, 0, addContactString)
                    .setOnMenuItemClickListener(l)
                    .setIntent(intent);
            }
        }
    }

    private boolean haveEmailContact(String emailAddress) {
        Cursor cursor = SqliteWrapper.query(this, getContentResolver(),
                Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, Uri.encode(emailAddress)),
                new String[] { Contacts.DISPLAY_NAME }, null, null, null);

        if (cursor != null) {
            try {
                String name;
                while (cursor.moveToNext()) {
                    name = cursor.getString(0);
                    if (!TextUtils.isEmpty(name)) {
                        return true;
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return false;
    }

    private boolean isNumberInContacts(String phoneNumber) {
        return Contact.get(phoneNumber, false).existsInDatabase();
    }

    private MessageItem mMsgItem = null;
    private final OnCreateContextMenuListener mMsgListMenuCreateListener =
        new OnCreateContextMenuListener() {

        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            //add for multi-delete
            if (mMsgListAdapter.mIsDeleteMode) {
                return;
            }

            Cursor cursor = mMsgListAdapter.getCursor();
            String type = cursor.getString(COLUMN_MSG_TYPE);
            long msgId = cursor.getLong(COLUMN_ID);
            Xlog.i(TAG, "onCreateContextMenu(): msgId=" + msgId);
            addPositionBasedMenuItems(menu, v, menuInfo);

            mMsgItem = mMsgListAdapter.getCachedMessageItem(type, msgId, cursor);
            if (mMsgItem == null) {
                Log.e(TAG, "Cannot load message item for type = " + type
                        + ", msgId = " + msgId);
                return;
            }

            menu.setHeaderTitle(R.string.message_options);

            MsgListMenuClickListener l = new MsgListMenuClickListener();

            if (mMsgItem.mLocked) {
                menu.add(0, MENU_UNLOCK_MESSAGE, 0, R.string.menu_unlock)
                    .setOnMenuItemClickListener(l);
            } else {
                menu.add(0, MENU_LOCK_MESSAGE, 0, R.string.menu_lock)
                    .setOnMenuItemClickListener(l);
            }

            if (mMsgItem.isMms()) {
                switch (mMsgItem.mBoxId) {
                    case Mms.MESSAGE_BOX_INBOX:
                        break;
                    case Mms.MESSAGE_BOX_OUTBOX:
                        // Since we currently break outgoing messages to multiple
                        // recipients into one message per recipient, only allow
                        // editing a message for single-recipient conversations.
//                        if (getRecipients().size() == 1) {
//                            menu.add(0, MENU_EDIT_MESSAGE, 0, R.string.menu_edit)
//                                    .setOnMenuItemClickListener(l);
//                        }
                        break;
                }
                switch (mMsgItem.mAttachmentType) {
                    case WorkingMessage.TEXT:
                        break;
                    case WorkingMessage.ATTACHMENT:
                        final ArrayList<FileAttachmentModel> files = mMsgItem.mSlideshow.getAttachFiles();
                        if (files == null || files.size() < 1) {
                            break;
                        }
                        final FileAttachmentModel attach = files.get(0);
                        if (attach.isVCard()) {
                            menu.add(0, MENU_IMPORT_VCARD, 0, R.string.file_attachment_import_vcard)
                                .setOnMenuItemClickListener(l);
                        }
                        if (haveSomethingToCopyToSDCard(mMsgItem.mMsgId)) {
                            menu.add(0, MENU_COPY_TO_SDCARD, 0, R.string.copy_to_sdcard)
                            .setOnMenuItemClickListener(l);
                        }
                        break;
                    case WorkingMessage.VIDEO:
                    case WorkingMessage.IMAGE:
                        if (haveSomethingToCopyToSDCard(mMsgItem.mMsgId)) {
                            menu.add(0, MENU_COPY_TO_SDCARD, 0, R.string.copy_to_sdcard)
                            .setOnMenuItemClickListener(l);
                        }
                        break;
                    case WorkingMessage.SLIDESHOW:
                        menu.add(0, MENU_VIEW_SLIDESHOW, 0, R.string.view_slideshow)
                        .setOnMenuItemClickListener(l);
                        if (haveSomethingToCopyToSDCard(mMsgItem.mMsgId)) {
                            menu.add(0, MENU_PREVIEW, 0, R.string.preview)
                            .setOnMenuItemClickListener(l);
                            menu.add(0, MENU_COPY_TO_SDCARD, 0, R.string.copy_to_sdcard)
                            .setOnMenuItemClickListener(l);
                        }
                        break;
                    default:
                        if (haveSomethingToCopyToSDCard(mMsgItem.mMsgId)) {
                            menu.add(0, MENU_COPY_TO_SDCARD, 0, R.string.copy_to_sdcard)
                            .setOnMenuItemClickListener(l);
                        }
                        if (haveSomethingToCopyToDrmProvider(mMsgItem.mMsgId)) {
                            menu.add(0, MENU_COPY_TO_DRM_PROVIDER, 0,
                                    getDrmMimeMenuStringRsrc(mMsgItem.mMsgId))
                            .setOnMenuItemClickListener(l);
                        }
                        break;
                }
            } else {
                // Message type is sms. Only allow "edit" if the message has a single recipient
                if (getRecipients().size() == 1 && mMsgItem.mBoxId == Sms.MESSAGE_TYPE_FAILED) {
                    menu.add(0, MENU_EDIT_MESSAGE, 0, R.string.menu_edit)
                            .setOnMenuItemClickListener(l);
                }
            }

            addCallAndContactMenuItems(menu, l, mMsgItem);

            // Forward is not available for undownloaded messages.
            if (mMsgItem.isDownloaded()) {
                menu.add(0, MENU_FORWARD_MESSAGE, 0, R.string.menu_forward)
                        .setOnMenuItemClickListener(l);
            }

            // It is unclear what would make most sense for copying an MMS message
            // to the clipboard, so we currently do SMS only.
            if (mMsgItem.isSms()) {
                menu.add(0, MENU_COPY_MESSAGE_TEXT, 0, R.string.copy_message_text)
                        .setOnMenuItemClickListener(l);
                if (mSimCount > 0 && !mMsgItem.isSending()) {
                    menu.add(0, MENU_SAVE_MESSAGE_TO_SIM, 0, R.string.save_message_to_sim)
                        .setOnMenuItemClickListener(l);
                }
            }

            menu.add(0, MENU_VIEW_MESSAGE_DETAILS, 0, R.string.view_message_details)
                    .setOnMenuItemClickListener(l);
            menu.add(0, MENU_DELETE_MESSAGE, 0, R.string.delete_message)
                    .setOnMenuItemClickListener(l);
            if (mMsgItem.mDeliveryStatus != MessageItem.DeliveryStatus.NONE || mMsgItem.mReadReport) {
                menu.add(0, MENU_DELIVERY_REPORT, 0, R.string.view_delivery_report)
                        .setOnMenuItemClickListener(l);
            }
        }
    };

    // edit fail message item
    private void editMessageItem(MessageItem msgItem) {
        if ("sms".equals(msgItem.mType)) {
            editSmsMessageItem(msgItem);
        } else {
            editMmsMessageItem(msgItem);
        }
        if ((msgItem.isFailedMessage() || msgItem.isSending()) && mMsgListAdapter.getCount() <= 1 ) {
            // For messages with bad addresses, let the user re-edit the recipients.
            initRecipientsEditor();
            isInitRecipientsEditor = true;
            mRecipientsAvatar.setVisibility(View.GONE);
            mMsgListAdapter.changeCursor(null);
        }
    }

    private void editSmsMessageItem(MessageItem msgItem) {
        // When the message being edited is the only message in the conversation, the delete
        // below does something subtle. The trigger "delete_obsolete_threads_pdu" sees that a
        // thread contains no messages and silently deletes the thread. Meanwhile, the mConversation
        // object still holds onto the old thread_id and code thinks there's a backing thread in
        // the DB when it really has been deleted. Here we try and notice that situation and
        // clear out the thread_id. Later on, when Conversation.ensureThreadId() is called, we'll
        // create a new thread if necessary.
        synchronized(mConversation) {
            if (mMsgListAdapter.getCursor().getCount() <= 1) {
                mConversation.clearThreadId();
            }
        }
        // Delete the old undelivered SMS and load its content.
        Uri uri = ContentUris.withAppendedId(Sms.CONTENT_URI, msgItem.mMsgId);
        SqliteWrapper.delete(ComposeMessageActivity.this,
                mContentResolver, uri, null, null);
        mWorkingMessage.setText(msgItem.mBody);
    }

    private void editMmsMessageItem(MessageItem msgItem) {
        // Discard the current message in progress.
        mWorkingMessage.discard();

        // Load the selected message in as the working message.
        mWorkingMessage = WorkingMessage.load(this, msgItem.mMessageUri);
        if (mWorkingMessage == null){
            return;
        }
        mWorkingMessage.setConversation(mConversation);

        mAttachmentEditor.update(mWorkingMessage);
        drawTopPanel();

        // WorkingMessage.load() above only loads the slideshow. Set the
        // subject here because we already know what it is and avoid doing
        // another DB lookup in load() just to get it.
        mWorkingMessage.setSubject(msgItem.mSubject, false);

        if (mWorkingMessage.hasSubject()) {
            showSubjectEditor(true);
        }
    }

    private void copyToClipboard(String str) {
        ClipboardManager clip =
            (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        clip.setText(str);
    }

    private void forwardMessage(MessageItem msgItem) {
        Intent intent = createIntent(this, 0);

        intent.putExtra("exit_on_sent", true);
        intent.putExtra(FORWARD_MESSAGE, true);

        if (msgItem.mType.equals("sms")) {
            intent.putExtra(SMS_BODY, msgItem.mBody);
        } else {
            SendReq sendReq = new SendReq();
            String subject = getString(R.string.forward_prefix);
            if (msgItem.mSubject != null) {
                subject += msgItem.mSubject;
            }
            sendReq.setSubject(new EncodedStringValue(subject));
            sendReq.setBody(msgItem.mSlideshow.makeCopy(
                    ComposeMessageActivity.this));

            Uri uri = null;
            try {
                PduPersister persister = PduPersister.getPduPersister(this);
                // Copy the parts of the message here.
                uri = persister.persist(sendReq, Mms.Draft.CONTENT_URI);
            } catch (MmsException e) {
                Log.e(TAG, "Failed to copy message: " + msgItem.mMessageUri, e);
                Toast.makeText(ComposeMessageActivity.this,
                        R.string.cannot_save_message, Toast.LENGTH_SHORT).show();
                return;
            }

            intent.putExtra("msg_uri", uri);
            intent.putExtra("subject", subject);
        }
        // ForwardMessageActivity is simply an alias in the manifest for ComposeMessageActivity.
        // We have to make an alias because ComposeMessageActivity launch flags specify
        // singleTop. When we forward a message, we want to start a separate ComposeMessageActivity.
        // The only way to do that is to override the singleTop flag, which is impossible to do
        // in code. By creating an alias to the activity, without the singleTop flag, we can
        // launch a separate ComposeMessageActivity to edit the forward message.
        intent.setClassName(this, "com.android.mms.ui.ForwardMessageActivity");
        startActivity(intent);
    }

    /**
     * Context menu handlers for the message list view.
     */
    private final class MsgListMenuClickListener implements MenuItem.OnMenuItemClickListener {
        public boolean onMenuItemClick(MenuItem item) {

            if (mMsgItem == null) {
                return false;
            }

            switch (item.getItemId()) {
                case MENU_EDIT_MESSAGE:
                    editMessageItem(mMsgItem);
                    drawBottomPanel();
                    return true;

                case MENU_COPY_MESSAGE_TEXT:
                	if (mMsgItem.mBody != null) {
                        String copyBody = mMsgItem.mBody.replaceAll(STR_RN, STR_CN);
                        copyToClipboard(copyBody);
                	} else {
                		Xlog.i(TAG, "onMenuItemClick, mMsgItem.mBody == null");
                		return false;
                	}
                    return true;

                case MENU_FORWARD_MESSAGE:
                    final MessageItem mRestrictedItem = mMsgItem;
                    if (WorkingMessage.sCreationMode == 0 || !isRestrictedType(mMsgItem.mMsgId)) {
                    	new Thread(new Runnable() {
                            public void run() {
                            	forwardMessage(mRestrictedItem);
                            }
                        }, "ForwardMessage").start();
                        
                    } else if(WorkingMessage.sCreationMode == WorkingMessage.WARNING_TYPE) {
                        new AlertDialog.Builder(ComposeMessageActivity.this)
                        .setTitle(R.string.restricted_forward_title)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.restricted_forward_message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public final void onClick(DialogInterface dialog, int which) {
                                int createMode = WorkingMessage.sCreationMode;
                                WorkingMessage.sCreationMode = 0;
                                new Thread(new Runnable() {
                                    public void run() {
                                    	forwardMessage(mRestrictedItem);
                                    }
                                }, "ForwardMessage").start();
                                WorkingMessage.sCreationMode = createMode;
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                    }
                    return true;
                    
                case MENU_IMPORT_VCARD: {
                    // There should be one and only one file
                    final ArrayList<FileAttachmentModel> files = mMsgItem.mSlideshow.getAttachFiles();
                    if (files == null || files.size() != 1) {
                        return false;
                    }
                    final FileAttachmentModel attach = files.get(0);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(attach.getUri(), attach.getContentType().toLowerCase());
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                    return true;
                }

                case MENU_VIEW_SLIDESHOW:
                    if (mClickCanResponse) {
                        mClickCanResponse = false;
                        MessageUtils.viewMmsMessageAttachment(ComposeMessageActivity.this, ContentUris.withAppendedId(
                            Mms.CONTENT_URI, mMsgItem.mMsgId), null);
                    }
                    return true;

                case MENU_VIEW_MESSAGE_DETAILS: {
                    String messageDetails = MessageUtils.getMessageDetails(
                            ComposeMessageActivity.this, mMsgItem);
                    mDetailDialog = new AlertDialog.Builder(ComposeMessageActivity.this)
                            .setTitle(R.string.message_details_title)
                            .setMessage(messageDetails)
                            .setPositiveButton(android.R.string.ok, null)
                            .setCancelable(true)
                            .show();
                    return true;
                }
                case MENU_DELETE_MESSAGE: {
                    DeleteMessageListener l = new DeleteMessageListener(
                            mMsgItem.mMessageUri, mMsgItem.mLocked);
                    String where = Telephony.Mms._ID + "=" + mMsgItem.mMsgId;
                    String[] projection = new String[] { Sms.Inbox.THREAD_ID };
                    Xlog.d(TAG, "where:" + where);
                    Cursor queryCursor = Sms.query(getContentResolver(),
                            projection, where, null);
                    if (queryCursor.moveToFirst()) {
                        mThreadId = queryCursor.getLong(0);
                        queryCursor.close();
                    }
                    confirmDeleteDialog(l, mMsgItem.mLocked);
                    return true;
                }
                case MENU_DELIVERY_REPORT:
                    showDeliveryReport(mMsgItem.mMsgId, mMsgItem.mType);
                    return true;

                case MENU_COPY_TO_SDCARD: {
                    final long iMsgId = mMsgItem.mMsgId;
                    Intent i = new Intent(ComposeMessageActivity.this, MultiSaveActivity.class);
                    i.putExtra("msgid", iMsgId);
                    startActivityForResult(i, REQUEST_CODE_MULTI_SAVE);
                    return true;
                }
                
                case MENU_PREVIEW: {
                    final long iMsgId = mMsgItem.mMsgId;
                    Intent i = new Intent(ComposeMessageActivity.this, PreviewActivity.class);
                    i.putExtra("msgid", iMsgId);
                    startActivity(i);
                    return true;
                }

                case MENU_COPY_TO_DRM_PROVIDER: {
                    int resId = getDrmMimeSavedStringRsrc(mMsgItem.mMsgId, copyToDrmProvider(mMsgItem.mMsgId));
                    Toast.makeText(ComposeMessageActivity.this, resId, Toast.LENGTH_SHORT).show();
                    return true;
                }

                case MENU_SAVE_MESSAGE_TO_SIM: {
                    mSaveMsgThread = new SaveMsgThread(mMsgItem.mType, mMsgItem.mMsgId);
                    mSaveMsgThread.start();
                    return true;
                }

                case MENU_LOCK_MESSAGE: {
                    lockMessage(mMsgItem, true);
                    return true;
                }

                case MENU_UNLOCK_MESSAGE: {
                    lockMessage(mMsgItem, false);
                    return true;
                }
                case MENU_ADD_TO_BOOKMARK: {
                    if (mURLs.size() == 1) {
                        Browser.saveBookmark(ComposeMessageActivity.this, null, mURLs.get(0));
                    } else if(mURLs.size() > 1) {
                        CharSequence[] items = new CharSequence[mURLs.size()];
                        for (int i = 0; i < mURLs.size(); i++) {
                            items[i] = mURLs.get(i);
                        }
                        new AlertDialog.Builder(ComposeMessageActivity.this)
                            .setTitle(R.string.menu_add_to_bookmark)
                            .setIcon(R.drawable.ic_dialog_menu_generic)
                            .setItems(items, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Browser.saveBookmark(ComposeMessageActivity.this, null, mURLs.get(which));
                                    }
                                })
                            .show();
                    }
                    return true;
                }
                case MENU_ADD_ADDRESS_TO_CONTACTS: {
                    mAddContactIntent = item.getIntent();
                    MessageUtils.addNumberOrEmailtoContact(mAddContactIntent.getStringExtra(ContactsContract.Intents.Insert.PHONE), 
                            REQUEST_CODE_ADD_CONTACT, ComposeMessageActivity.this);
                    return true;
                }
                default:
                    return false;
            }
        }
    }

    private void saveMessageToSim(String msgType, long msgId) {
        Xlog.d(MmsApp.TXN_TAG, "save message to sim, message type:" + msgType 
                + "; message id:" + msgId + "; sim count:" + mSimCount);

        Intent intent = new Intent();
        intent.putExtra("message_type", msgType);
        intent.putExtra("message_id", msgId);
        intent.putExtra(SELECT_TYPE, SIM_SELECT_FOR_SAVE_MSG_TO_SIM);
        showSimSelectedDialog(intent);
    }

    private void getMessageAndSaveToSim(Intent intent) {
        Xlog.v(MmsApp.TXN_TAG, "get message and save to sim, selected sim id = " + mSelectedSimId);
        String msgType = intent.getStringExtra("message_type");
        long msgId = intent.getLongExtra("message_id", 0);
        if (msgType == null) {
            //mSaveMsgHandler.sendEmptyMessage(MSG_SAVE_MESSAGE_TO_SIM_FAILED_GENERIC);
            mUiHandler.sendEmptyMessage(MSG_SAVE_MESSAGE_TO_SIM_FAILED_GENERIC);            
            return;
        }
        getMessageAndSaveToSim(msgType, msgId);
    }

    private void getMessageAndSaveToSim(String msgType, long msgId){
        int result = 0;
        MessageItem msgItem = getMessageItem(msgType, msgId, true);
        if (msgItem == null || msgItem.mBody == null) {
            Xlog.e(MmsApp.TXN_TAG, "getMessageAndSaveToSim, can not get Message Item.");
            return;
        }
        
        String scAddress = null;
   
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> messages = null;
        messages = smsManager.divideMessage(msgItem.mBody);

        int smsStatus = 0;
        long timeStamp = 0;
        if (msgItem.isReceivedMessage()) {
            smsStatus = SmsManager.STATUS_ON_ICC_READ;
            timeStamp = msgItem.mSmsDate;
            scAddress = msgItem.getServiceCenter();
        } else if (msgItem.isSentMessage()) {
            smsStatus = SmsManager.STATUS_ON_ICC_SENT;
        } else if (msgItem.isFailedMessage()) {
            smsStatus = SmsManager.STATUS_ON_ICC_UNSENT;
        } else {
            Xlog.w(MmsApp.TXN_TAG, "Unknown sms status");
        }

        if (scAddress == null) {
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                scAddress = TelephonyManagerEx.getDefault().getScAddress(SIMInfo.getSlotById(this, mSelectedSimId));
            } else {
                scAddress = TelephonyManagerEx.getDefault().getScAddress(0);
            }
        }

        Xlog.d(MmsApp.TXN_TAG, "\t scAddress\t= " + scAddress);
        Xlog.d(MmsApp.TXN_TAG, "\t Address\t= " + msgItem.mAddress);
        Xlog.d(MmsApp.TXN_TAG, "\t msgBody\t= " + msgItem.mBody);
        Xlog.d(MmsApp.TXN_TAG, "\t smsStatus\t= " + smsStatus);
        Xlog.d(MmsApp.TXN_TAG, "\t timeStamp\t= " + timeStamp);


        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            int slotId = -1;
            if (mSimCount == 1) {
                slotId = mSimInfoList.get(0).mSlot;
            } else {
                slotId = SIMInfo.getSlotById(this, mSelectedSimId);
            }
            Xlog.d(MmsApp.TXN_TAG, "\t slot Id\t= " + slotId);

            result = GeminiSmsManager.copyTextMessageToIccCardGemini(scAddress, 
                    msgItem.mAddress, messages, smsStatus, timeStamp, slotId);
        } else {
            
            result = SmsManager.getDefault().copyTextMessageToIccCard(scAddress, 
                    msgItem.mAddress, messages, smsStatus, timeStamp);
        }

        if (result == SmsManager.RESULT_ERROR_SUCCESS) {
            Xlog.d(MmsApp.TXN_TAG, "save message to sim succeed.");
            mUiHandler.sendEmptyMessage(MSG_SAVE_MESSAGE_TO_SIM_SUCCEED);            
        } else if (result == SmsManager.RESULT_ERROR_SIM_MEM_FULL) {
            Xlog.w(MmsApp.TXN_TAG, "save message to sim failed: sim memory full.");
            mUiHandler.sendEmptyMessage(MSG_SAVE_MESSAGE_TO_SIM_FAILED_SIM_FULL);
        } else {
            Xlog.w(MmsApp.TXN_TAG, "save message to sim failed: generic error.");
            mUiHandler.sendEmptyMessage(MSG_SAVE_MESSAGE_TO_SIM_FAILED_GENERIC);
        }
        mSaveMsgHandler.sendEmptyMessageDelayed(MSG_QUIT_SAVE_MESSAGE_THREAD, 5000);
    }

    private void lockMessage(MessageItem msgItem, boolean locked) {
        Uri uri;
        if ("sms".equals(msgItem.mType)) {
            uri = Sms.CONTENT_URI;
        } else {
            uri = Mms.CONTENT_URI;
        }
        final Uri lockUri = ContentUris.withAppendedId(uri, msgItem.mMsgId);

        final ContentValues values = new ContentValues(1);
        values.put("locked", locked ? 1 : 0);

        new Thread(new Runnable() {
            public void run() {
                getContentResolver().update(lockUri,
                        values, null, null);
            }
        }, "lockMessage").start();
    }

    /**
     * Looks to see if there are any valid parts of the attachment that can be copied to a SD card.
     * @param msgId
     */
    private boolean haveSomethingToCopyToSDCard(long msgId) {
        PduBody body = PduBodyCache.getPduBody(this,
                ContentUris.withAppendedId(Mms.CONTENT_URI, msgId));
        if (body == null) {
            return false;
        }

        boolean result = false;
        int partNum = body.getPartsNum();
        for(int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            String type = new String(part.getContentType());

            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                log("[CMA] haveSomethingToCopyToSDCard: part[" + i + "] contentType=" + type);
            }

            if (ContentType.isImageType(type) || ContentType.isVideoType(type) ||
                    ContentType.isAudioType(type)) {
                result = true;
                break;
            }

            if (FileAttachmentModel.isSupportedFile(part)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * Looks to see if there are any drm'd parts of the attachment that can be copied to the
     * DrmProvider. Right now we only support saving audio (e.g. ringtones).
     * @param msgId
     */
    private boolean haveSomethingToCopyToDrmProvider(long msgId) {
        String mimeType = getDrmMimeType(msgId);
        return isAudioMimeType(mimeType);
    }

    /**
     * Simple cache to prevent having to load the same PduBody again and again for the same uri.
     */
    private static class PduBodyCache {
        private static PduBody mLastPduBody;
        private static Uri mLastUri;

        static public PduBody getPduBody(Context context, Uri contentUri) {
            if (contentUri.equals(mLastUri)) {
                return mLastPduBody;
            }
            try {
                mLastPduBody = SlideshowModel.getPduBody(context, contentUri);
                mLastUri = contentUri;
             } catch (MmsException e) {
                 Log.e(TAG, e.getMessage(), e);
                 return null;
             }
             return mLastPduBody;
        }
    };

    /* package */ static PduBody getPduBody(Context context, long msgid) {
        return  PduBodyCache.getPduBody(context,
                ContentUris.withAppendedId(Mms.CONTENT_URI, msgid));
    }

    /**
     * Copies media from an Mms to the DrmProvider
     * @param msgId
     */
    private boolean copyToDrmProvider(long msgId) {
        boolean result = true;
        PduBody body = PduBodyCache.getPduBody(this,
                ContentUris.withAppendedId(Mms.CONTENT_URI, msgId));
        if (body == null) {
            return false;
        }

        int partNum = body.getPartsNum();
        for(int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            String type = new String(part.getContentType());

            if (ContentType.isDrmType(type)) {
                // All parts (but there's probably only a single one) have to be successful
                // for a valid result.
                result &= copyPartToDrmProvider(part);
            }
        }
        return result;
    }

    private String mimeTypeOfDrmPart(PduPart part) {
        Uri uri = part.getDataUri();
        InputStream input = null;
        try {
            input = mContentResolver.openInputStream(uri);
            if (input instanceof FileInputStream) {
                FileInputStream fin = (FileInputStream) input;

                DrmRawContent content = new DrmRawContent(fin, fin.available(),
                        DrmRawContent.DRM_MIMETYPE_MESSAGE_STRING);
                String mimeType = content.getContentType();
                return mimeType;
            }
        } catch (IOException e) {
            // Ignore
            Log.e(TAG, "IOException caught while opening or reading stream", e);
        } catch (DrmException e) {
            Log.e(TAG, "DrmException caught ", e);
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    // Ignore
                    Log.e(TAG, "IOException caught while closing stream", e);
                }
            }
        }
        return null;
    }

    /**
     * Returns the type of the first drm'd pdu part.
     * @param msgId
     */
    private String getDrmMimeType(long msgId) {
        PduBody body = PduBodyCache.getPduBody(this,
                ContentUris.withAppendedId(Mms.CONTENT_URI, msgId));
        if (body == null) {
            return null;
        }

        int partNum = body.getPartsNum();
        for(int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            String type = new String(part.getContentType());

            if (ContentType.isDrmType(type)) {
                return mimeTypeOfDrmPart(part);
            }
        }
        return null;
    }

    private int getDrmMimeMenuStringRsrc(long msgId) {
        String mimeType = getDrmMimeType(msgId);
        if (isAudioMimeType(mimeType)) {
            return R.string.save_ringtone;
        }
        return 0;
    }

    private int getDrmMimeSavedStringRsrc(long msgId, boolean success) {
        String mimeType = getDrmMimeType(msgId);
        if (isAudioMimeType(mimeType)) {
            return success ? R.string.saved_ringtone : R.string.saved_ringtone_fail;
        }
        return 0;
    }

    private boolean isAudioMimeType(String mimeType) {
        return mimeType != null && mimeType.startsWith("audio/");
    }

    private boolean isImageMimeType(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    private boolean copyPartToDrmProvider(PduPart part) {
        Uri uri = part.getDataUri();

        InputStream input = null;
        try {
            input = mContentResolver.openInputStream(uri);
            if (input instanceof FileInputStream) {
                FileInputStream fin = (FileInputStream) input;

                // Build a nice title
                byte[] location = part.getName();
                if (location == null) {
                    location = part.getFilename();
                }
                if (location == null) {
                    location = part.getContentLocation();
                }

                // Depending on the location, there may be an
                // extension already on the name or not
                String title = new String(location);
                int index;
                if ((index = title.indexOf(".")) == -1) {
                    String type = new String(part.getContentType());
                } else {
                    title = title.substring(0, index);
                }

                // transfer the file to the DRM content provider
                Intent item = DrmStore.addDrmFile(mContentResolver, fin, title);
                if (item == null) {
                    Log.w(TAG, "unable to add file " + uri + " to DrmProvider");
                    return false;
                }
            }
        } catch (IOException e) {
            // Ignore
            Log.e(TAG, "IOException caught while opening or reading stream", e);
            return false;
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    // Ignore
                    Log.e(TAG, "IOException caught while closing stream", e);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Copies media from an Mms to the "download" directory on the SD card
     * @param msgId
     */
    private boolean copyMedia(long msgId) {
        boolean result = true;
        PduBody body = PduBodyCache.getPduBody(this,
                ContentUris.withAppendedId(Mms.CONTENT_URI, msgId));
        if (body == null) {
            return false;
        }

        int partNum = body.getPartsNum();
        for(int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            String type = new String(part.getContentType());

            if (ContentType.isImageType(type) || ContentType.isVideoType(type) ||
                    ContentType.isAudioType(type) || FileAttachmentModel.isSupportedFile(type)) {
                result &= copyPart(part, Long.toHexString(msgId)); // all parts have to be successful for a valid result.
            }
        }
        return result;
    }

    private boolean isRestrictedType(long msgId){
        PduBody body = PduBodyCache.getPduBody(this,
                ContentUris.withAppendedId(Mms.CONTENT_URI, msgId));
        if (body == null) {
            return false;
        }

        int partNum = body.getPartsNum();
        for(int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            int width = 0;
            int height = 0;
            String type = new String(part.getContentType());

            int mediaTypeStringId;
            if (ContentType.isVideoType(type)) {
                mediaTypeStringId = R.string.type_video;
            } else if (ContentType.isAudioType(type)) {
                mediaTypeStringId = R.string.type_audio;
            } else if (ContentType.isImageType(type)) {
                mediaTypeStringId = R.string.type_picture;
                InputStream input = null;
                try {
                    input = this.getContentResolver().openInputStream(part.getDataUri());
                    BitmapFactory.Options opt = new BitmapFactory.Options();
                    opt.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(input, null, opt);
                    width = opt.outWidth;
                    height = opt.outHeight;
                } catch (FileNotFoundException e) {
                    // Ignore
                    Xlog.e(TAG, "FileNotFoundException caught while opening stream", e);
                } finally {
                    if (null != input) {
                        try {
                            input.close();
                        } catch (IOException e) {
                            // Ignore
                            Xlog.e(TAG, "IOException caught while closing stream", e);
                        }
                    }
                }
            } else {
                continue;
            }
            if (!ContentType.isRestrictedType(type) || width > MmsConfig.getMaxRestrictedImageWidth()
                    || height > MmsConfig.getMaxRestrictedImageHeight()){
                if (WorkingMessage.sCreationMode == WorkingMessage.RESTRICTED_TYPE){
                    Resources res = getResources();
                    String mediaType = res.getString(mediaTypeStringId);
                    MessageUtils.showErrorDialog(ComposeMessageActivity.this, res.getString(R.string.unsupported_media_format, mediaType)
                            , res.getString(R.string.select_different_media, mediaType));
                }
            return true;
            }
        }
        return false;
    }

    private boolean copyPart(PduPart part, String fallback) {
        Uri uri = part.getDataUri();

        InputStream input = null;
        FileOutputStream fout = null;
        try {
            input = mContentResolver.openInputStream(uri);
            if (input instanceof FileInputStream) {
                FileInputStream fin = (FileInputStream) input;

                byte[] location = part.getName();
                if (location == null) {
                    location = part.getFilename();
                }
                if (location == null) {
                    location = part.getContentLocation();
                }

                String fileName;
                if (location == null) {
                    // Use fallback name.
                    fileName = fallback;
                } else {
                    fileName = new String(location);
                }
                // Depending on the location, there may be an
                // extension already on the name or not
                String dir = Environment.getExternalStorageDirectory() + "/"
                                + Environment.DIRECTORY_DOWNLOADS  + "/";
                String extension;
                int index;
                if ((index = fileName.lastIndexOf(".")) == -1) {
                    String type = new String(part.getContentType());
                    extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
                    Xlog.i(TAG, "Save part extension name is: " + extension);
                } else {
                    extension = fileName.substring(index + 1, fileName.length());
                    Xlog.i(TAG, "Save part extension name is: " + extension);
                    fileName = fileName.substring(0, index);
                }

                File file = getUniqueDestination(dir + fileName, extension);

                // make sure the path is valid and directories created for this file.
                File parentFile = file.getParentFile();
                if (!parentFile.exists() && !parentFile.mkdirs()) {
                    Log.e(TAG, "[MMS] copyPart: mkdirs for " + parentFile.getPath() + " failed!");
                    return false;
                }

                fout = new FileOutputStream(file);

                byte[] buffer = new byte[8000];
                int size = 0;
                while ((size=fin.read(buffer)) != -1) {
                    fout.write(buffer, 0, size);
                }

                // Notify other applications listening to scanner events
                // that a media file has been added to the sd card
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.fromFile(file)));
            }
        } catch (IOException e) {
            // Ignore
            Log.e(TAG, "IOException caught while opening or reading stream", e);
            return false;
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    // Ignore
                    Log.e(TAG, "IOException caught while closing stream", e);
                    return false;
                }
            }
            if (null != fout) {
                try {
                    fout.close();
                } catch (IOException e) {
                    // Ignore
                    Log.e(TAG, "IOException caught while closing stream", e);
                    return false;
                }
            }
        }
        return true;
    }

    private File getUniqueDestination(String base, String extension) {
        File file = new File(base + "." + extension);

        for (int i = 2; file.exists(); i++) {
            file = new File(base + "_" + i + "." + extension);
        }
        return file;
    }

    private void showDeliveryReport(long messageId, String type) {
        Intent intent = new Intent(this, DeliveryReportActivity.class);
        intent.putExtra("message_id", messageId);
        intent.putExtra("message_type", type);

        startActivity(intent);
    }

    private final IntentFilter mHttpProgressFilter = new IntentFilter(PROGRESS_STATUS_ACTION);
    private final BroadcastReceiver mHttpProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (PROGRESS_STATUS_ACTION.equals(intent.getAction())) {
                long token = intent.getLongExtra("token",
                                    SendingProgressTokenManager.NO_TOKEN);
                if (token != mConversation.getThreadId()) {
                    return;
                }

                int progress = intent.getIntExtra("progress", 0);
                switch (progress) {
                    case PROGRESS_START:
                        setProgressBarVisibility(true);
                        break;
                    case PROGRESS_ABORT:
                    case PROGRESS_COMPLETE:
                        setProgressBarVisibility(false);
                        break;
                    default:
                        setProgress(100 * progress);
                }
            }
        }
    };

    private static ContactList sEmptyContactList;

    private ContactList getRecipients() {
        // If the recipients editor is visible, the conversation has
        // not really officially 'started' yet.  Recipients will be set
        // on the conversation once it has been saved or sent.  In the
        // meantime, let anyone who needs the recipient list think it
        // is empty rather than giving them a stale one.
        if (isRecipientsEditorVisible()) {
            if (sEmptyContactList == null) {
                sEmptyContactList = new ContactList();
            }
            return sEmptyContactList;
        }
        return mConversation.getRecipients();
    }

    private void bindToContactHeaderWidget(ContactList list) {
        mRecipientsAvatar.setVisibility(View.VISIBLE);

        mContactHeader.wipeClean();
        mContactHeader.invalidate();
        switch (list.size()) {
            case 0:
                String recipient = "";
                if (mRecipientsEditor != null) {
                    recipient = mRecipientsEditor.getText().toString();
                }
                mContactHeader.showStar(true);
                mContactHeader.setStarInvisible(true);
                mContactHeader.setDisplayName(recipient, null);
                break;
            case 1:
                final Contact contact = Contact.get(list.get(0).getNumber(), true);
                final String addr = contact.getNumber();
                final String name = list.formatNames("");
                final Uri contactUri = contact.getUri();
                Intent intent;

                if (contact.existsInDatabase()) {
                    Xlog.i(TAG, "compose.bindToContactHeaderWidget(): has contact");
                    mContactHeader.showStar(true);
                    mContactHeader.setStarInvisible(true);
                    mContactHeader.setDisplayName(name, addr);
                    mContactHeader.setContactUri(contactUri, true);
                    intent = new Intent(Intent.ACTION_VIEW, contactUri);
                } else {
                    Xlog.i(TAG, "compose.bindToContactHeaderWidget(): doesn't have contact");
                    // mContactHeader.setDisplayName(addr,null);
                    mContactHeader.showStar(true);
                    mContactHeader.setStarInvisible(true);
                    if (Mms.isEmailAddress(addr)) {
                        mContactHeader.bindFromEmail(addr);
                    } else {
                        mContactHeader.bindFromPhoneNumber(addr);
                    }
                    intent = ConversationList.createAddContactIntent(contact.getNumber());
                }

                // mContactHeader.setContactUri(contactUri,true);
                // set photo
                BitmapDrawable photo = (BitmapDrawable) contact.getAvatar(this, null);
                if (null != photo) {
                    mContactHeader.setPhoto(photo.getBitmap());
                }

                final Intent contactIntent = intent;
                mJumpToContacts.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (contactIntent.getAction() == Intent.ACTION_VIEW) {
                            contactIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                            startActivityForResult(contactIntent, REQUEST_CODE_LOAD_DRAFT);
                        } else {
                            //v.setClickable(false);
                            MessageUtils.addNumberOrEmailtoContact(contact.getNumber(), REQUEST_CODE_ADD_CONTACT, ComposeMessageActivity.this);
                        }
                    }
                });
                final String action = intent.getAction();
                if (Intent.ACTION_VIEW.equals(action)) {
                    Xlog.d(TAG,"ComposeMessageActivity-onResume-action:"+action);
                    mJumpToContacts.setBackgroundResource(R.drawable.add_contact_selector_exist);
                } else if (NUMBER_ADD_CONTACT_ACTION.equals(action)) {
                    Xlog.d(TAG,"ComposeMessageActivity-onResume-action:"+action);
                    mJumpToContacts.setBackgroundResource(R.drawable.add_contact_selector);
                }
                //mJumpToContacts.setClickable(true);
                break;
            default:
                // Handle multiple recipients
                mContactHeader.showStar(true);
                mContactHeader.setStarInvisible(true);
                final ContactList contactList = list;
                mJumpToContacts.setBackgroundResource(R.drawable.contact_button_selector);
               
                mJumpToContacts.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        // TODO: Is there a better way to share data between activities?
                        MultiRecipientsActivity.setContactList(contactList);
                        final Intent i = new Intent(ComposeMessageActivity.this, MultiRecipientsActivity.class);
                        startActivity(i);
                    }
                });
                String moreRecipients = getString(R.string.more_recipients);
                mContactHeader.setDisplayName(list.getFirstName(", ") + " " + moreRecipients, null);
                mContactHeader.setContactUri(null);
                mContactHeader.setPhoto(((BitmapDrawable) getResources().getDrawable(R.drawable.ic_groupchat))
                        .getBitmap());
                mContactHeader.setPresence(0); // clear the presence, too
                break;
        }
    }

    // Get the recipients editor ready to be displayed onscreen.
    private void initRecipientsEditor() {
        if (isRecipientsEditorVisible() && isInitRecipientsEditor) {
            return;
        }
        // Must grab the recipients before the view is made visible because getRecipients()
        // returns empty recipients when the editor is visible.
        ContactList recipients = getRecipients();
        while (!recipients.isEmpty() && recipients.size() > RECIPIENTS_LIMIT_FOR_SMS) {
            recipients.remove(RECIPIENTS_LIMIT_FOR_SMS);
        }

        ViewStub stub = (ViewStub) findViewById(R.id.recipients_editor_stub);
        if (stub != null) {
            mRecipientsEditor = (RecipientsEditor) stub.inflate();
        } else {
            mRecipientsEditor = (RecipientsEditor) findViewById(R.id.recipients_editor);
            mRecipientsEditor.setVisibility(View.VISIBLE);
        }

        mRecipientsEditor.setMaxRecipNumber(RECIPIENTS_LIMIT_FOR_SMS);
        mRecipientsEditor.setAdapter(new RecipientsAdapter(this));
        if (isInitRecipientsEditor) {
            mRecipientsEditor.deleteAllRecipients();
            mRecipientsEditor.populate(recipients);
            mWorkingMessage.setWorkingRecipients(new ArrayList<String>(Arrays.asList(recipients.getNumbers())));
            mWorkingMessage.syncWorkingRecipients();
        }
        mRecipientsEditor.setOnCreateContextMenuListener(mRecipientsMenuCreateListener);
        mRecipientsEditor.setOnRecipChangeListener(this);
        mRecipientsEditor.addTextChangedListener(mRecipientsWatcher);
        mRecipientsEditor.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // After the user selects an item in the pop-up contacts list, move the
                // focus to the text editor if there is only one recipient. This helps
                // the common case of selecting one recipient and then typing a message,
                // but avoids annoying a user who is trying to add five recipients and
                // keeps having focus stolen away.
                if (mRecipientsEditor.getRecipientCount() == 1) {
                    // if we're in extract mode then don't request focus
                    final InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (inputManager == null || !inputManager.isFullscreenMode()) {
                        mTextEditor.requestFocus();
                    }
                }
            }
        });

        mRecipientsEditor.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    RecipientsEditor editor = (RecipientsEditor) v;
                } else {
                    mRecipientsEditor.setSelection(mRecipientsEditor.length());
                }
            }
        });

        mTopPanel.setVisibility(View.VISIBLE);
        mRecipients.setVisibility(View.VISIBLE);
    }

    //==========================================================
    // Activity methods
    //==========================================================

    public static boolean cancelFailedToDeliverNotification(Intent intent, Context context) {
        if (MessagingNotification.isFailedToDeliver(intent)) {
            // Cancel any failed message notifications
            MessagingNotification.cancelNotification(context,
                        MessagingNotification.MESSAGE_FAILED_NOTIFICATION_ID);
            return true;
        }
        return false;
    }

    public static boolean cancelFailedDownloadNotification(Intent intent, Context context) {
        if (MessagingNotification.isFailedToDownload(intent)) {
            // Cancel any failed download notifications
            MessagingNotification.cancelNotification(context,
                        MessagingNotification.DOWNLOAD_FAILED_NOTIFICATION_ID);
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sCompose = ComposeMessageActivity.this;
        initMessageSettings();
        setContentView(R.layout.compose_message_activity);
        setProgressBarVisibility(false);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // Initialize members for UI elements.
        initResourceRefs();

        // SIM indicator manager
        mStatusBarManager = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
        mComponentName = getComponentName();
        mContentResolver = getContentResolver();
        mBackgroundQueryHandler = new BackgroundQueryHandler(mContentResolver);
        mSimCount = 0;

        initialize(savedInstanceState, 0);
        mDestroy = false;
        if (TRACE) {
            android.os.Debug.startMethodTracing("compose");
        }
        if (mCellMgr == null) {
            mCellMgr = new CellConnMgr();
        }
        mCellMgr.register(getApplication());
        mCellMgrRegisterCount++;
    }

    private void initMessageSettings() {
        Context otherAppContext = null;
        SharedPreferences sp = null;
        try {
            otherAppContext = this.createPackageContext("com.android.mms", Context.CONTEXT_IGNORE_SECURITY);
        } catch (Exception e) {
            Xlog.e(TAG, "ConversationList NotFoundContext");
        }
        if (otherAppContext != null) {
            sp = otherAppContext.getSharedPreferences("com.android.mms_preferences", MODE_WORLD_READABLE);
        }
        String mSizeLimitTemp = null;
        int mMmsSizeLimit = 0;
        if (sp != null) {
            mSizeLimitTemp = sp.getString("pref_key_mms_size_limit", "300");
        }
        if (0 == mSizeLimitTemp.compareTo("100")) {
            mMmsSizeLimit = 100;
        } else if (0 == mSizeLimitTemp.compareTo("200")) {
            mMmsSizeLimit = 200;
        } else {
            mMmsSizeLimit = 300;
        }
        MmsConfig.setUserSetMmsSizeLimit(mMmsSizeLimit);
    }

    private void showSubjectEditor(boolean show) {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("showSubjectEditor: " + show);
        }

        if (mSubjectTextEditor == null) {
            // Don't bother to initialize the subject editor if
            // we're just going to hide it.
            if (show == false) {
                return;
            }
            mSubjectTextEditor = (EditText)findViewById(R.id.subject);
            String optr = SystemProperties.get("ro.operator.optr");
            //MTK_OP01_PROTECT_START
            if ("OP01".equals(optr)) {
                mSubjectTextEditor.setFilters(new InputFilter[] { new MyLengthFilter(DEFAULT_LENGTH) });
            }
            //MTK_OP01_PROTECT_END
        }

        mSubjectTextEditor.setOnKeyListener(show ? mSubjectKeyListener : null);

        if (show) {
            mSubjectTextEditor.addTextChangedListener(mSubjectEditorWatcher);
        } else {
            mSubjectTextEditor.removeTextChangedListener(mSubjectEditorWatcher);
        }

        mSubjectTextEditor.setText(mWorkingMessage.getSubject());
        mSubjectTextEditor.setVisibility(show ? View.VISIBLE : View.GONE);
        hideOrShowTopPanel();
    }

    private void hideOrShowTopPanel() {
        boolean anySubViewsVisible = (isSubjectEditorVisible() || isRecipientsEditorVisible());
        mTopPanel.setVisibility(anySubViewsVisible ? View.VISIBLE : View.GONE);
    }

    public void initialize(Bundle savedInstanceState, long originalThreadId) {
        Intent intent = getIntent();

        // Create a new empty working message.
        mWorkingMessage = WorkingMessage.createEmpty(this);

        // Read parameters or previously saved state of this activity. This will load a new
        // mConversation
        initActivityState(savedInstanceState, intent);

        if (LogTag.SEVERE_WARNING && originalThreadId != 0 &&
                originalThreadId == mConversation.getThreadId()) {
            LogTag.warnPossibleRecipientMismatch("ComposeMessageActivity.initialize threadId didn't change" +
                    " from: " + originalThreadId, this);
        }

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("initialize: savedInstanceState = " + savedInstanceState +
                " intent = " + intent +
                " mConversation = " + mConversation);
        }

        if (cancelFailedToDeliverNotification(getIntent(), this)) {
            // Show a pop-up dialog to inform user the message was
            // failed to deliver.
            undeliveredMessageDialog(getMessageDate(null));
        }
        cancelFailedDownloadNotification(getIntent(), this);

        // Set up the message history ListAdapter
        initMessageList();

        // Load the draft for this thread, if we aren't already handling
        // existing data, such as a shared picture or forwarded message.
        boolean isForwardedMessage = false;
        if (!handleSendIntent(intent)) {
            isForwardedMessage = handleForwardedMessage();
            if (!isForwardedMessage && mConversation.hasDraft()) {
                loadDraft();
            }
        }
        
        // Let the working message know what conversation it belongs to
        mWorkingMessage.setConversation(mConversation);

        // Show the recipients editor if we don't have a valid thread. Hide it otherwise.
        if (mConversation.getThreadId() <= 0l || 
                  (mConversation.getMessageCount() <= 0 && (intent.getAction() != null || mConversation.hasDraft()))) {
            // Hide the recipients editor so the call to initRecipientsEditor won't get
            // short-circuited.
            hideRecipientEditor();
            isInitRecipientsEditor = true;
            initRecipientsEditor();

            // Bring up the softkeyboard so the user can immediately enter recipients. This
            // call won't do anything on devices with a hard keyboard.
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        } else {
            hideRecipientEditor();
            mConversation.markAsRead();
        }

        drawTopPanel();
        drawBottomPanel();
        mAttachmentEditor.update(mWorkingMessage);

        Configuration config = getResources().getConfiguration();
        mIsKeyboardOpen = config.keyboardHidden == KEYBOARDHIDDEN_NO;
        mIsLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE;
        onKeyboardStateChanged(mIsKeyboardOpen);

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("initialize: update title, mConversation=" + mConversation.toString());
        }

        if (isForwardedMessage && isRecipientsEditorVisible()) {
            // The user is forwarding the message to someone. Put the focus on the
            // recipient editor rather than in the message editor.
            mRecipientsEditor.requestFocus();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);

        Conversation conversation = null;
        mSentMessage = false;

        // If we have been passed a thread_id, use that to find our
        // conversation.

        // Note that originalThreadId might be zero but if this is a draft and we save the
        // draft, ensureThreadId gets called async from WorkingMessage.asyncUpdateDraftSmsMessage
        // the thread will get a threadId behind the UI thread's back.
        long originalThreadId = mConversation.getThreadId();
        long threadId = intent.getLongExtra("thread_id", 0);
        Uri intentUri = intent.getData();

        boolean sameThread = false;
        if (threadId > 0) {
            conversation = Conversation.get(this, threadId, false);
        } else {
            if (mConversation.getThreadId() == 0) {
                // We've got a draft. See if the new intent's recipient is the same as
                // the draft's recipient. First make sure the working recipients are synched
                // to the conversation.
                mWorkingMessage.syncWorkingRecipients();
                sameThread = mConversation.sameRecipient(intentUri);
            }
            if (!sameThread) {
                // Otherwise, try to get a conversation based on the
                // data URI passed to our intent.
                conversation = Conversation.get(this, intentUri, false);
            }
        }

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("onNewIntent: data=" + intentUri + ", thread_id extra is " + threadId);
            log("     new conversation=" + conversation + ", mConversation=" + mConversation);
        }

        if (conversation != null) {

            // this is probably paranoia to compare both thread_ids and recipient lists,
            // but we want to make double sure because this is a last minute fix for Froyo
            // and the previous code checked thread ids only.
            // (we cannot just compare thread ids because there is a case where mConversation
            // has a stale/obsolete thread id (=1) that could collide against the new thread_id(=1),
            // even though the recipient lists are different)
            sameThread = (conversation.getThreadId() == mConversation.getThreadId() &&
                    conversation.equals(mConversation));
        }

        if (sameThread) {
            log("onNewIntent: same conversation");
        } else {
            // Don't let any markAsRead DB updates occur before we've loaded the messages for
            // the thread.
            conversation.blockMarkAsRead(true);
            
            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                log("onNewIntent: different conversation, initialize...");
            }
            if ((!isRecipientsEditorVisible())
                    || (mRecipientsEditor.hasValidRecipient(mWorkingMessage.requiresMms()))) {
                saveDraft();    // if we've got a draft, save it first
            }

            hideRecipientEditor();
            if (mRecipientsAvatar != null && mRecipientsAvatar.getVisibility() == View.VISIBLE) {
                mRecipientsAvatar.setVisibility(View.GONE);
            }
            mMsgListAdapter = null;
            mConversation = conversation;

            initialize(null, mConversation.getThreadId());
            loadMessageContent();
        }

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (mWorkingMessage.isDiscarded()) {
            // If the message isn't worth saving, don't resurrect it. Doing so can lead to
            // a situation where a new incoming message gets the old thread id of the discarded
            // draft. This activity can end up displaying the recipients of the old message with
            // the contents of the new message. Recognize that dangerous situation and bail out
            // to the ConversationList where the user can enter this in a clean manner.
             mWorkingMessage.unDiscard();    // it was discarded in onStop().
             if (isRecipientsEditorVisible()) {
                goToConversationList();
            } else {
                //loadDraft();
                mWorkingMessage.setConversation(mConversation);
                mAttachmentEditor.update(mWorkingMessage);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        misPickContatct = false;
        mConversation.blockMarkAsRead(true);
        initFocus();

        // Register a BroadcastReceiver to listen on HTTP I/O process.
        registerReceiver(mHttpProgressReceiver, mHttpProgressFilter);
        loadMessageContent();

        // Update the fasttrack info in case any of the recipients' contact info changed
        // while we were paused. This can happen, for example, if a user changes or adds
        // an avatar associated with a contact.
        if (mConversation.getThreadId() == 0) {
            mWorkingMessage.syncWorkingRecipients();
        }

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("onStart: update title, mConversation=" + mConversation.toString());
        }
    }

    public void loadMessageContent() {
        startMsgListQuery();
        updateSendFailedNotification();
        drawBottomPanel();
    }

    private void updateSendFailedNotification() {
        final long threadId = mConversation.getThreadId();
        if (threadId <= 0)
            return;

        // updateSendFailedNotificationForThread makes a database call, so do the work off
        // of the ui thread.
        new Thread(new Runnable() {
            public void run() {
                MessagingNotification.updateSendFailedNotificationForThread(
                        ComposeMessageActivity.this, threadId);
            }
        }, "updateSendFailedNotification").start();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save recipients of this coversation
        if (mRecipientsEditor != null && isRecipientsEditorVisible()) {
//            outState.putString("recipients", mRecipientsEditor.allNumberToString());
            // We are compressing the image, so save the thread id in order to restore the draft when activity
            // restarting.
            if (mCompressingImage) {
                outState.putLong("thread", mConversation.ensureThreadId());
            } else if (mRecipientsEditor.getRecipientCount() < 1) {
                outState.putLong("thread",mConversation.ensureThreadId());
            }
        } else {
         // save the current thread id
            outState.putLong("thread",mConversation.getThreadId());
            Xlog.i(TAG,"saved thread id:" + mConversation.getThreadId());
        }

        mWorkingMessage.writeStateToBundle(outState);

        if (mExitOnSent) {
            outState.putBoolean("exit_on_sent", mExitOnSent);
        }
        outState.putBoolean("compressing_image", mCompressingImage);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mProgressDialog != null && !mProgressDialog.isShowing())  {
            mProgressDialog.show();
        }
        Configuration config = getResources().getConfiguration();
        Xlog.d(TAG, "onResume - config.orientation="+config.orientation);
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Xlog.e(TAG, "onResume Set setSoftInputMode to 0x"+Integer.toHexString(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN));
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
        // OLD: get notified of presence updates to update the titlebar.
        // NEW: we are using ContactHeaderWidget which displays presence, but updating presence
        //      there is out of our control.
        //Contact.startPresenceObserver();

        addRecipientsListeners();
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("onResume: update title, mConversation=" + mConversation.toString());
        }

        // get all SIM info
        mGetSimInfoRunnable.run();
        updateSendButtonState();

        // There seems to be a bug in the framework such that setting the title
        // here gets overwritten to the original title.  Do this delayed as a
        // workaround.
        mMessageListItemHandler.postDelayed(new Runnable() {
            public void run() {
                if (isRecipientsEditorVisible()) {
                    ContactList recipients = mRecipientsEditor.constructContactsFromInput();
                } else {
                    ContactList recipients = getRecipients();
                    if (!recipients.isEmpty()) {
                        for (Contact contact:recipients) {
                            contact.reload(true);
                        }
                    }
                    bindToContactHeaderWidget(recipients);
                }
            }
        }, 10);
        // show SMS indicator
        mStatusBarManager.hideSIMIndicator(mComponentName);
        mStatusBarManager.showSIMIndicator(mComponentName, Settings.System.SMS_SIM_SETTING);
        
        mClickCanResponse = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // OLD: stop getting notified of presence updates to update the titlebar.
        // NEW: we are using ContactHeaderWidget which displays presence, but updating presence
        //      there is out of our control.
        //Contact.stopPresenceObserver();
        if (mDetailDialog != null){
            mDetailDialog.dismiss();
        }
        if (mSendDialog != null){
            mSendDialog.dismiss();
        }
        removeRecipientsListeners();
        clearPendingProgressDialog();
        // hide SIM indicator
        mStatusBarManager.hideSIMIndicator(mComponentName);

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAddAttachmentRunnable != null) {
            mAttachmentEditorHandler.removeCallbacks(mAddAttachmentRunnable);
        }
        if (misPickContatct){
            return;
        }
        // Allow any blocked calls to update the thread's read status.
        mConversation.blockMarkAsRead(false);

        if (mRecipientsEditor != null) {
            CursorAdapter recipientsAdapter = (CursorAdapter)mRecipientsEditor.getAdapter();
            if (recipientsAdapter != null) {
                recipientsAdapter.changeCursor(null);
            }
        }
        
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("onStop: save draft");
        }

        if ((!isRecipientsEditorVisible())
                || (mRecipientsEditor.hasValidRecipient(mWorkingMessage.requiresMms()))) {
            saveDraft();
        }
        mMsgListAdapter.changeCursor(null);
        // Cleanup the BroadcastReceiver.
        unregisterReceiver(mHttpProgressReceiver);
        Xlog.i(TAG, "onStop(): mWorkingMessage.isDiscarded() == " + mWorkingMessage.isDiscarded());
    }

    @Override
    protected void onDestroy() {
        if (TRACE) {
            android.os.Debug.stopMethodTracing();
        }
        if (mCellMgrRegisterCount == 1) {
            mCellMgr.unregister();
        }
        mCellMgrRegisterCount--;
        mDestroy = true;
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (LOCAL_LOGV) {
            Log.v(TAG, "onConfigurationChanged: " + newConfig);
        }

        mIsKeyboardOpen = newConfig.keyboardHidden == KEYBOARDHIDDEN_NO;
        boolean isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
        if (isLandscape) {
            // Hide input Method.
            hideInputMethod();
        }
        if (mIsLandscape != isLandscape) {
            mIsLandscape = isLandscape;
            // Have to re-layout the attachment editor because we have different layouts
            // depending on whether we're portrait or landscape.
            //add for multi-delete
            if (!mMsgListAdapter.mIsDeleteMode) {
                mAttachmentEditor.update(mWorkingMessage);
            }
        }
        onKeyboardStateChanged(mIsKeyboardOpen);
    }

    private void onKeyboardStateChanged(boolean isKeyboardOpen) {
        // If the keyboard is hidden, don't show focus highlights for
        // things that cannot receive input.
        if (isKeyboardOpen) {
            if (mRecipientsEditor != null) {
                mRecipientsEditor.setFocusableInTouchMode(true);
            }
            if (mSubjectTextEditor != null) {
                mSubjectTextEditor.setFocusableInTouchMode(true);
            }
            mTextEditor.setFocusableInTouchMode(true);
            mTextEditor.setHint(R.string.type_to_compose_text_enter_to_send);
        } else {
            if (mRecipientsEditor != null) {
                mRecipientsEditor.setFocusable(false);
            }
            if (mSubjectTextEditor != null) {
                mSubjectTextEditor.setFocusable(false);
            }
            mTextEditor.setFocusable(false);
            mTextEditor.setHint(R.string.open_keyboard_to_compose_message);
        }
    }

    @Override
    public void onUserInteraction() {
        checkPendingNotification();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            checkPendingNotification();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DEL:
                if ((mMsgListAdapter != null) && mMsgListView.isFocused()) {
                    Cursor cursor;
                    try {
                        cursor = (Cursor) mMsgListView.getSelectedItem();
                    } catch (ClassCastException e) {
                        Log.e(TAG, "Unexpected ClassCastException.", e);
                        return super.onKeyDown(keyCode, event);
                    }

                    if (cursor != null) {
                        boolean locked = cursor.getInt(COLUMN_MMS_LOCKED) != 0;
                        DeleteMessageListener l = new DeleteMessageListener(
                                cursor.getLong(COLUMN_ID),
                                cursor.getString(COLUMN_MSG_TYPE),
                                locked);
                        confirmDeleteDialog(l, locked);
                        return true;
                    }
                }
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                break;
            case KeyEvent.KEYCODE_ENTER:
                if (isPreparedForSending()) {
                    //simSelection();
                    checkRecipientsCount();
                    return true;
                } else {
                    if (!isHasRecipientCount()) {
                            new AlertDialog.Builder(this).setIcon(
                                    android.R.drawable.ic_dialog_alert).setTitle(
                                        R.string.cannot_send_message).setMessage(
                                            R.string.cannot_send_message_reason)
                                                .setPositiveButton(R.string.yes,
                                                    new CancelSendingListener()).show();
                    } else {
                        new AlertDialog.Builder(this).setIcon(
                                android.R.drawable.ic_dialog_alert).setTitle(
                                    R.string.cannot_send_message).setMessage(
                                        R.string.cannot_send_message_reason_no_content)
                                        .setPositiveButton(R.string.yes,
                                            new CancelSendingListener()).show();
                    }
                }
                break;
            case KeyEvent.KEYCODE_BACK:
                // add for multi-delete
                if (mMsgListAdapter.mIsDeleteMode) {
                    mMsgListAdapter.mIsDeleteMode = false;
                    drawTopPanel();
                    drawBottomPanel();
                    startMsgListQuery();
                } else {
                    if (isRecipientsEditorVisible()) {
                        mRecipientsEditor.structLastRecipient();
                    }
                    exitComposeMessageActivity(new Runnable() {
                        public void run() {
                            finish();
                        }
                    });
                }
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void exitComposeMessageActivity(final Runnable exit) {
        // If the message is empty, just quit -- finishing the
        // activity will cause an empty draft to be deleted.
        if (!mWorkingMessage.isWorthSaving()) {
            exit.run();
            return;
        }

        if (isRecipientsEditorVisible() &&
                !mRecipientsEditor.hasValidRecipient(mWorkingMessage.requiresMms())) {
            MessageUtils.showDiscardDraftConfirmDialog(this, new DiscardDraftListener());
            return;
        } else {
            //TODO delete old draft.
            mWorkingMessage.asyncDeleteOldDraftMmsMessage(mConversation.getThreadId());
        }
            

        mToastForDraftSave = true;
        exit.run();
    }

    private void goToConversationList() {
        finish();
        startActivity(new Intent(this, ConversationList.class));
    }

    private void hideRecipientEditor() {
        if (mRecipientsEditor != null) {
            mRecipientsEditor.removeTextChangedListener(mRecipientsWatcher);
            mRecipientsEditor.setVisibility(View.GONE);
            hideOrShowTopPanel();
        }
    }

    private boolean isRecipientsEditorVisible() {
        return (null != mRecipientsEditor)
                    && (View.VISIBLE == mRecipientsEditor.getVisibility());
    }

    private boolean isSubjectEditorVisible() {
        return (null != mSubjectTextEditor)
                    && (View.VISIBLE == mSubjectTextEditor.getVisibility());
    }

    public void onAttachmentChanged() {
        // Have to make sure we're on the UI thread. This function can be called off of the UI
        // thread when we're adding multi-attachments
        runOnUiThread(new Runnable() {
            public void run() {
                drawBottomPanel();
                updateSendButtonState();
                mAttachmentEditor.update(mWorkingMessage);
            }
        });
    }

    public void onProtocolChanged(final boolean mms, final boolean needToast) {
        // Have to make sure we're on the UI thread. This function can be called off of the UI
        // thread when we're adding multi-attachments
        runOnUiThread(new Runnable() {
            public void run() {
                /*
                if (mSubjectTextEditor != null && mSubjectTextEditor.getVisibility() == View.VISIBLE){
                    return;
                }*/
                if (mms == true) {
                    mTextCounter.setVisibility(View.GONE);
                } else {
                    mTextCounter.setVisibility(View.VISIBLE);
            	}
                if(needToast == true){
                    toastConvertInfo(mms);
                }
                //setSendButtonText(mms);
            }
        });
    }

    private void setSendButtonText(boolean isMms) {
        Button sendButton = mSendButton;
        sendButton.setText(R.string.send);

        if (isMms) {
            // Create and append the "MMS" text in a smaller font than the "Send" text.
            sendButton.append("\n");
            SpannableString spannable = new SpannableString(getString(R.string.mms));
            int mmsTextSize = (int) (sendButton.getTextSize() * 0.75f);
            spannable.setSpan(new AbsoluteSizeSpan(mmsTextSize), 0, spannable.length(), 0);
            sendButton.append(spannable);
            mTextCounter.setText("");
        }
    }

    Runnable mResetMessageRunnable = new Runnable() {
        public void run() {
            resetMessage();
        }
    };

    Runnable mGetSimInfoRunnable = new Runnable() {
        public void run() {
            getSimInfoList();
        }
    };

    public void onPreMessageSent() {
        runOnUiThread(mResetMessageRunnable);
    }

    public void onMessageSent() {
        mWaitingForSendMessage = false;
        // If we already have messages in the list adapter, it
        // will be auto-requerying; don't thrash another query in.
        if (mMsgListAdapter.getCount() == 0) {
            startMsgListQuery();
        }
    }

    public void onMaxPendingMessagesReached() {
        saveDraft();

        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(ComposeMessageActivity.this, R.string.too_many_unsent_mms,
                        Toast.LENGTH_LONG).show();
                mSendingMessage = false;
                updateSendButtonState();
            }
        });
    }

    public void onAttachmentError(final int error) {
        runOnUiThread(new Runnable() {
            public void run() {
                handleAddAttachmentError(error, R.string.type_picture);
                onMessageSent();        // now requery the list of messages
            }
        });
    }

    // We don't want to show the "call" option unless there is only one
    // recipient and it's a phone number.
    private boolean isRecipientCallable() {
        ContactList recipients = getRecipients();
        return (recipients.size() == 1 && !recipients.containsEmail());
    }

    private void dialRecipient() {
        String number = getRecipients().get(0).getNumber();
        Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
         
        startActivity(dialIntent);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        // add for multi-delete
        if (mMsgListAdapter.mIsDeleteMode) {
            return true;
        }

        if (isRecipientCallable()) {
            menu.add(0, MENU_CALL_RECIPIENT, 0, R.string.menu_call).setIcon(
                    R.drawable.ic_menu_call);
        }

        // Only add the "View contact" menu item when there's a single recipient and that
        // recipient is someone in contacts.
        ContactList recipients = getRecipients();
        if (recipients.size() == 1 && recipients.get(0).existsInDatabase()) {
            menu.add(0, MENU_VIEW_CONTACT, 0, R.string.menu_view_contact).setIcon(
                    R.drawable.ic_menu_contact);
        }

        if (MmsConfig.getMmsEnabled()) {
            if (!isSubjectEditorVisible()) {
                menu.add(0, MENU_ADD_SUBJECT, 0, R.string.add_subject).setIcon(
                        R.drawable.ic_menu_edit);
            }

            if (!mWorkingMessage.hasAttachment()) {
                menu.add(0, MENU_ADD_ATTACHMENT, 0, R.string.add_attachment).setIcon(
                        R.drawable.ic_menu_attachment);
            }
        }

        if (isPreparedForSending()) {
            menu.add(0, MENU_SEND, 0, R.string.send).setIcon(android.R.drawable.ic_menu_send);
        }
        if (mMsgListAdapter.getCount() > 0) {
            // Removed search as part of b/1205708
            //menu.add(0, MENU_SEARCH, 0, R.string.menu_search).setIcon(
            //        R.drawable.ic_menu_search);
            Cursor cursor = mMsgListAdapter.getCursor();
            if ((null != cursor) && (cursor.getCount() > 0)) {
                menu.add(0, MENU_DELETE_THREAD, 0, R.string.delete_message).setIcon(
                    android.R.drawable.ic_menu_delete);
            }
        } else {
            menu.add(0, MENU_DISCARD, 0, R.string.discard).setIcon(android.R.drawable.ic_menu_delete);
        }
        if (!mWorkingMessage.hasSlideshow() || (mSubjectTextEditor != null && mSubjectTextEditor.isFocused())) {
            menu.add(0, MENU_ADD_QUICK_TEXT, 0, R.string.menu_insert_quick_text).setIcon(
                R.drawable.ic_menu_quick_text);
        }
        
        if (!mWorkingMessage.hasSlideshow() || (mSubjectTextEditor != null && mSubjectTextEditor.isFocused())) {
            menu.add(0, MENU_INSERT_SMILEY, 0, R.string.menu_insert_smiley).setIcon(
                R.drawable.ic_menu_emoticons);
        }

        if (!mWorkingMessage.hasSlideshow()){
            menu.add(0, MENU_ADD_TEXT_VCARD, 0, R.string.menu_insert_text_vcard).setIcon(
                    R.drawable.ic_menu_text_vcard);
        }

        menu.add(0, MENU_CONVERSATION_LIST, 0, R.string.all_threads).setIcon(
                R.drawable.ic_menu_friendslist);

        buildAddAddressToContactMenuItem(menu);
        return true;
    }

    private void buildAddAddressToContactMenuItem(Menu menu) {
        // Look for the first recipient we don't have a contact for and create a menu item to
        // add the number to contacts.
        for (Contact c : getRecipients()) {
            if (!c.existsInDatabase() && canAddToContacts(c)) {
                // Intent intent = ConversationList.createAddContactIntent(c.getNumber());
                Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                intent.putExtra(ContactsContract.Intents.Insert.PHONE, c.getNumber());
                menu.add(0, MENU_ADD_ADDRESS_TO_CONTACTS, 0, R.string.menu_add_to_contacts)
                    .setIcon(android.R.drawable.ic_menu_add)
                    .setIntent(intent);
                break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ADD_SUBJECT:
                showSubjectEditor(true);
                mWorkingMessage.setSubject("", true);
                ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(
                    getCurrentFocus(), InputMethodManager.SHOW_IMPLICIT);
                mSubjectTextEditor.requestFocus(); 
                break;
            case MENU_ADD_ATTACHMENT:
                // Launch the add-attachment list dialog
                hideInputMethod();
                showAddAttachmentDialog(true);
//                showAddAttachmentDialog(!mWorkingMessage.hasAttachedFiles());
                break;
            case MENU_ADD_QUICK_TEXT:
                showQuickTextDialog();
                break;

            case MENU_ADD_TEXT_VCARD: {
                Intent intent = new Intent("android.intent.action.CONTACTSMULTICHOICE");
                intent.setType(Phone.CONTENT_ITEM_TYPE);
                intent.putExtra("request_type", 1);
                intent.putExtra("pick_count", 20);
                startActivityForResult(intent, REQUEST_CODE_TEXT_VCARD);
                break;
            }

            case MENU_DISCARD:
                mWorkingMessage.discard();
                finish();
                break;
            case MENU_SEND:
                if (isPreparedForSending()) {
                    // simSelection();
                    checkRecipientsCount();
                } else {
                    if (!isHasRecipientCount()) {
                        new AlertDialog.Builder(this).setIcon(
                            android.R.drawable.ic_dialog_alert).setTitle(
                                R.string.cannot_send_message).setMessage(
                                    R.string.cannot_send_message_reason)
                                        .setPositiveButton(R.string.yes,
                                            new CancelSendingListener()).show();
                    } else {
                        new AlertDialog.Builder(this).setIcon(
                            android.R.drawable.ic_dialog_alert).setTitle(
                                R.string.cannot_send_message).setMessage(
                                    R.string.cannot_send_message_reason_no_content)
                                        .setPositiveButton(R.string.yes,
                                            new CancelSendingListener()).show();
                    }
                }
                break;
            case MENU_SEARCH:
                onSearchRequested();
                break;
            case MENU_DELETE_THREAD:
                // add for multi-delete
                mMsgListAdapter.mIsDeleteMode = true;
                hideInputMethod();
                drawTopPanel();
                drawBottomPanel();
                startMsgListQuery();
                break;
            case MENU_CONVERSATION_LIST:
                exitComposeMessageActivity(new Runnable() {
                    public void run() {
                        goToConversationList();
                    }
                });
                break;
            case MENU_CALL_RECIPIENT:
                dialRecipient();
                break;
            case MENU_INSERT_SMILEY:
                showSmileyDialog();
                break;
            case MENU_VIEW_CONTACT: {
                // View the contact for the first (and only) recipient.
                ContactList list = getRecipients();
                if (list.size() == 1 && list.get(0).existsInDatabase()) {
                    Uri contactUri = list.get(0).getUri();
                    Intent intent = new Intent(Intent.ACTION_VIEW, contactUri);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    startActivityForResult(intent, REQUEST_CODE_LOAD_DRAFT);
                }
                break;
            }
            case MENU_ADD_ADDRESS_TO_CONTACTS:
                // If there is only one, just add it to database through MessageUtils.
                // Get the nonexist list and pass it to MultiRecipientsActivity.
                ContactList nonexist = new ContactList();
                for (Contact c : getRecipients()) {
                    if (!c.existsInDatabase() && canAddToContacts(c)) {
                        nonexist.add(c);
                    }
                }
                if (nonexist.size() > 1) {
                    MultiRecipientsActivity.setContactList(nonexist);
                    final Intent i = new Intent(ComposeMessageActivity.this, MultiRecipientsActivity.class);
                    startActivity(i);
                } else {
                    mAddContactIntent = item.getIntent();
                    MessageUtils.addNumberOrEmailtoContact(mAddContactIntent.getStringExtra(ContactsContract.Intents.Insert.PHONE), 
                        REQUEST_CODE_ADD_CONTACT, ComposeMessageActivity.this);
                }
                break;
        }

        return true;
    }
    
    //add for multi-delete
    private void changeDeleteMode() {
        if (!mMsgListAdapter.mIsDeleteMode) {
            mMsgListAdapter.clearList();
            markCheckedState(false);
        }
    }
    
    private void markCheckedState(boolean checkedState) {
        mMsgListAdapter.setItemsValue(checkedState, null);
        mDeleteButton.setEnabled(checkedState);
        int count = mMsgListView.getChildCount();
        ViewGroup layout = null;
        int childCount = 0;
        View view = null;
        for (int i = 0; i < count; i++) {
            layout = (ViewGroup) mMsgListView.getChildAt(i);
            childCount = layout.getChildCount();

            for (int j = 0; j < childCount; j++) {
                view = layout.getChildAt(j);
                if (view instanceof CheckBox) {
                    ((CheckBox) view).setChecked(checkedState);
                    break;
                }
            }
        }
    }

    private void confirmDeleteThread(long threadId) {
        Conversation.startQueryHaveLockedMessages(mBackgroundQueryHandler,
                threadId, ConversationList.HAVE_LOCKED_MESSAGES_TOKEN);
    }

//    static class SystemProperties { // TODO, temp class to get unbundling working
//        static int getInt(String s, int value) {
//            return value;       // just return the default value or now
//        }
//    }

    private int getVideoCaptureDurationLimit() {
        return CamcorderProfile.get(CamcorderProfile.QUALITY_LOW).duration;
    }

    private void addAttachment(int type, boolean append) {
        // Calculate the size of the current slide if we're doing a replace so the
        // slide size can optionally be used in computing how much room is left for an attachment.
        int currentSlideSize = 0;
        SlideshowModel slideShow = mWorkingMessage.getSlideshow();
        if (append) {
            mAppendAttachmentSign = true;
        } else {
            mAppendAttachmentSign = false;
        }
        if (slideShow != null) {
            SlideModel slide = slideShow.get(0);
            currentSlideSize = slide == null ? 0 : slide.getSlideSize();
        }

        switch (type) {
            case AttachmentTypeSelectorAdapter.ADD_IMAGE:
                MessageUtils.selectImage(this, REQUEST_CODE_ATTACH_IMAGE);
                break;

            case AttachmentTypeSelectorAdapter.TAKE_PICTURE: {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                intent.putExtra(MediaStore.EXTRA_OUTPUT, Mms.ScrapSpace.CONTENT_URI);
                startActivityForResult(intent, REQUEST_CODE_TAKE_PICTURE);
                break;
            }

            case AttachmentTypeSelectorAdapter.ADD_VIDEO:
                MessageUtils.selectVideo(this, REQUEST_CODE_ATTACH_VIDEO);
                break;

            case AttachmentTypeSelectorAdapter.RECORD_VIDEO: {
                
                long sizeLimit = 0;
                if (mAppendAttachmentSign) {
                    sizeLimit = computeAttachmentSizeLimitForAppen(slideShow);
                } else {
                    sizeLimit = computeAttachmentSizeLimit(slideShow, currentSlideSize);
                }
                if (sizeLimit > MIN_SIZE_FOR_CAPTURE_VIDEO) {
                    // MessageUtils.recordVideo(this, REQUEST_CODE_TAKE_VIDEO, sizeLimit);
                    int durationLimit = getVideoCaptureDurationLimit();
                    Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
                    intent.putExtra("android.intent.extra.sizeLimit", sizeLimit);
                    intent.putExtra("android.intent.extra.durationLimit", durationLimit);
                    startActivityForResult(intent, REQUEST_CODE_TAKE_VIDEO);
                } else {
                    Toast.makeText(this,
                        getString(R.string.message_too_big_for_video),
                        Toast.LENGTH_SHORT).show();
                }
//                // Set video size limit. Subtract 1K for some text.
//                long sizeLimit = MmsConfig.getUserSetMmsSizeLimit(true) - SlideshowModel.SLIDESHOW_SLOP;
//                if (slideShow != null) {
//                    sizeLimit -= slideShow.getCurrentSlideshowSize();
//
//                    // We're about to ask the camera to capture some video which will
//                    // eventually replace the content on the current slide. Since the current
//                    // slide already has some content (which was subtracted out just above)
//                    // and that content is going to get replaced, we can add the size of the
//                    // current slide into the available space used to capture a video.
//                    sizeLimit += currentSlideSize;
//                }
//                if (sizeLimit > 0) {
//                    int durationLimit = getVideoCaptureDurationLimit();
//                    Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
//                    intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
//                    intent.putExtra("android.intent.extra.sizeLimit", sizeLimit);
//                    intent.putExtra("android.intent.extra.durationLimit", durationLimit);
//                    startActivityForResult(intent, REQUEST_CODE_TAKE_VIDEO);
//                }
//                else {
//                    Toast.makeText(this,
//                            getString(R.string.message_too_big_for_video),
//                            Toast.LENGTH_SHORT).show();
//                }
            }
            break;

            case AttachmentTypeSelectorAdapter.ADD_SOUND:
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setTitle(getString(R.string.add_music));
                String[] items = new String[2];
                items[0] = getString(R.string.attach_ringtone);
                items[1] = getString(R.string.attach_sound);
                alertBuilder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                MessageUtils.selectRingtone(ComposeMessageActivity.this, REQUEST_CODE_ATTACH_RINGTONE);
                                break;
                            case 1:
                                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                                    Toast.makeText(ComposeMessageActivity.this, getString(R.string.Insert_sdcard), Toast.LENGTH_LONG).show();
                                    return;
                                }
                                MessageUtils.selectAudio(ComposeMessageActivity.this, REQUEST_CODE_ATTACH_SOUND);
                                break;
                        }
                    }
                });
                alertBuilder.create().show();
                break;

            case AttachmentTypeSelectorAdapter.RECORD_SOUND:
                MessageUtils.recordSound(this, REQUEST_CODE_RECORD_SOUND);
                break;

            case AttachmentTypeSelectorAdapter.ADD_SLIDESHOW:
                editSlideshow();
                break;
                
            case AttachmentTypeSelectorAdapter.ADD_VCARD:
                Intent intent = new Intent("android.intent.action.CONTACTSMULTICHOICE");
                intent.setType(Phone.CONTENT_ITEM_TYPE);
                intent.putExtra("request_type", 1);
                intent.putExtra("pick_count", 100);
                startActivityForResult(intent, REQUEST_CODE_ATTACH_VCARD);
                break;
            case AttachmentTypeSelectorAdapter.ADD_VCALENDAR:
                Intent i = new Intent("android.intent.action.CALENDARCHOICE");
                i.setType("text/x-vcalendar");
                i.putExtra("request_type", 0);
                startActivityForResult(i, REQUEST_CODE_ATTACH_VCALENDAR);
                break;
            default:
                break;
        }
    }

    public static long computeAttachmentSizeLimit(SlideshowModel slideShow, int currentSlideSize) {
        // Computer attachment size limit. Subtract 1K for some text.
        long sizeLimit = MmsConfig.getUserSetMmsSizeLimit(true) - SlideshowModel.SLIDESHOW_SLOP;
        if (slideShow != null) {
            sizeLimit -= slideShow.getCurrentSlideshowSize();

            // We're about to ask the camera to capture some video (or the sound recorder
            // to record some audio) which will eventually replace the content on the current
            // slide. Since the current slide already has some content (which was subtracted
            // out just above) and that content is going to get replaced, we can add the size of the
            // current slide into the available space used to capture a video (or audio).
            sizeLimit += currentSlideSize;
        }
        return sizeLimit;
    }

    public static long computeAttachmentSizeLimitForAppen(SlideshowModel slideShow) {
        long sizeLimit = MmsConfig.getUserSetMmsSizeLimit(true) - SlideshowModel.MMS_SLIDESHOW_INIT_SIZE;
        if (slideShow != null) {
            sizeLimit -= slideShow.getCurrentSlideshowSize();
        }
        if (sizeLimit > 0) {
            return sizeLimit;
        }
        return 0;
    }
    
    private void showAddAttachmentDialog(final boolean append) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_dialog_attach);
        builder.setTitle(R.string.add_attachment);

        if (mAttachmentTypeSelectorAdapter == null) {
            int mode = AttachmentTypeSelectorAdapter.MODE_WITH_SLIDESHOW;
            if (MessageUtils.isVCalendarAvailable(ComposeMessageActivity.this)) {
                mode |= AttachmentTypeSelectorAdapter.MODE_WITH_VCALENDAR;
            }
            mAttachmentTypeSelectorAdapter = new AttachmentTypeSelectorAdapter(
                    this, mode);
        }
        builder.setAdapter(mAttachmentTypeSelectorAdapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                addAttachment(mAttachmentTypeSelectorAdapter.buttonToCommand(which), append);
                dialog.dismiss();
            }
        });

        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DEBUG) {
            log("onActivityResult: requestCode=" + requestCode
                    + ", resultCode=" + resultCode + ", data=" + data);
        }
        mWaitingForSubActivity = false; // We're back!
        boolean mNeedAppendAttachment = false;
        if (mAppendAttachmentSign) {
            mNeedAppendAttachment = true;
            mAppendAttachmentSign = false;
        }
        if (mWorkingMessage.isFakeMmsForDraft()) {
            // We no longer have to fake the fact we're an Mms. At this point we are or we aren't,
            // based on attachments and other Mms attrs.
            mWorkingMessage.removeFakeMmsForDraft();
        }

        //When View contact back, load draft.
        if (requestCode == REQUEST_CODE_LOAD_DRAFT) {
            loadDraft();
            mWorkingMessage.setConversation(mConversation);
            mAttachmentEditor.update(mWorkingMessage);
            return;
        }

        // If there's no data (because the user didn't select a picture and
        // just hit BACK, for example), there's nothing to do.
        if (requestCode != REQUEST_CODE_TAKE_PICTURE) {
            if (data == null) {
                return;
            }
        } else if (resultCode != RESULT_OK){
            if (DEBUG) log("onActivityResult: bail due to resultCode=" + resultCode);
            return;
        }

        switch (requestCode) {
            case REQUEST_CODE_CREATE_SLIDESHOW:
                if (data != null) {
                    WorkingMessage newMessage = WorkingMessage.load(this, data.getData());
                    if (newMessage != null) {
                        boolean isMmsBefore = mWorkingMessage.requiresMms();
                        
                        newMessage.setSubject(mWorkingMessage.getSubject(), true);
                        mWorkingMessage = newMessage;
                        mWorkingMessage.setConversation(mConversation);
                        mAttachmentEditor.update(mWorkingMessage);
                        drawTopPanel();
                        updateSendButtonState();
                        
                        boolean isMmsAfter = mWorkingMessage.requiresMms();
                        if (isMmsAfter && !isMmsBefore) {
                            toastConvertInfo(true);
                        } else if (!isMmsAfter && isMmsBefore) {
                            toastConvertInfo(false);
                        }
                    }
                }
                break;

            case REQUEST_CODE_TAKE_PICTURE: {
                // create a file based uri and pass to addImage(). We want to read the JPEG
                // data directly from file (using UriImage) instead of decoding it into a Bitmap,
                // which takes up too much memory and could easily lead to OOM.
                File file = new File(Mms.ScrapSpace.SCRAP_FILE_PATH);
                File tempFile = new File("/sdcard/mms/scrapSpace/.nomedia");
                if (!tempFile.exists()) {
                    try {
                        tempFile.createNewFile();
                    } catch (IOException e) {
                        Xlog.w(TAG, "compose.onActivityResult(): can't create new file '/sdcard/mms/scrapSpace/.nomedia'");
                    }
                }
                Uri uri = Uri.fromFile(file);
                addImageAsync(uri, null, mNeedAppendAttachment);
                break;
            }

            case REQUEST_CODE_PICK_CONTACT: {
                long[] contactsId = data.getLongArrayExtra("com.android.contacts.pickphoneandemail");
                if (contactsId == null || contactsId.length <= 0) {
                    return;
                }

                String selection = "_id in(";
                int contactLength = contactsId.length;
                for (int i = 0; i < contactLength; i++) {
                    if (i == 0) {
                        selection += contactsId[i];
                    } else {
                        selection += "," + contactsId[i];
                    }
                }
                selection += ")";

                Cursor cursor = getContentResolver().query(Data.CONTENT_URI, null, selection, null, null);
                int cannotAdd = -1;
                if (cursor != null) {
                    int multiContactCount = cursor.getCount();
                    int countAdd = recipientCount();
                    cannotAdd = multiContactCount + countAdd - RECIPIENTS_LIMIT_FOR_SMS;
                    List<String> numberList = mRecipientsEditor.getNumbers();
                    Xlog.d(TAG, "onActivityResult() : numberList = " + mRecipientsEditor.allNumberToString());
                    ArrayList<String> displayNameList = new ArrayList<String>();
                    try {
                        while (cursor.moveToNext()) {
                            if (countAdd >= RECIPIENTS_LIMIT_FOR_SMS) {
                                break;
                            }
                            String number = cursor.getString(cursor.getColumnIndexOrThrow(Phone.NUMBER));
                            if (mRecipientsEditor.isDuplicateNumber(number)) {
                                continue;
                            }

                            String displayName = cursor.getString(cursor.getColumnIndexOrThrow(Contacts.DISPLAY_NAME));
                            Xlog.i(TAG, "onActivityResult() : displayName = " + displayName);
                            displayNameList.add(displayName);
                            numberList.add(number);
                            if (Mms.isEmailAddress(number)) {
                                mWorkingMessage.setHasEmail(true, !mWorkingMessage.requiresMms());
                            }
                            Xlog.i(TAG, "onActivityResult() : setNumbers! numberList.size() = " + numberList.size());
                            countAdd++;
                        }
                    } catch (IllegalStateException e) {
                        Xlog.e(TAG, "contact is deleted " + e);
                    } finally {
                        cursor.close();
                    }
                    mRecipientsEditor.addRecipients(displayNameList, displayNameList.size());
                    mRecipientsEditor.setNumbers(numberList);
                    Xlog.d(TAG, "onActivityResult() : numberList = " + mRecipientsEditor.allNumberToString());
                    mWorkingMessage.setWorkingRecipients(numberList);
                    Xlog.d(TAG, "addRecipient-end");
                }

                misPickContatct = false;
                if (cannotAdd > 0) {
                    toastTooManyRecipients(cannotAdd + RECIPIENTS_LIMIT_FOR_SMS);
                }
                return;
            }

            case REQUEST_CODE_ATTACH_IMAGE: {
                if (data != null) {
                    addImageAsync(data.getData(),data.getType(), mNeedAppendAttachment);
                }
                break;
            }

            case REQUEST_CODE_TAKE_VIDEO:
            case REQUEST_CODE_ATTACH_VIDEO:
//                addVideo(data.getData(), false);
                addVideoAsync(data.getData(), mNeedAppendAttachment);      // can handle null videoUri
                break;

            case REQUEST_CODE_ATTACH_SOUND: {
                addAudio(data.getData());
                break;
            }

            case REQUEST_CODE_RECORD_SOUND:
                addAudio(data.getData());
                break;

            case REQUEST_CODE_ECM_EXIT_DIALOG:
                boolean outOfEmergencyMode = data.getBooleanExtra(EXIT_ECM_RESULT, false);
                if (outOfEmergencyMode) {
                    checkConditionsAndSendMessage(false);
                }
                break;

            case REQUEST_CODE_ADD_CONTACT:
                // The user just added a new contact. We saved the contact info in
                // mAddContactIntent. Get the contact and force our cached contact to
                // get reloaded with the new info (such as contact name). After the
                // contact is reloaded, the function onUpdate() in this file will get called
                // and it will update the title bar, etc.
                if (mAddContactIntent != null) {
                    String address = mAddContactIntent.getStringExtra(ContactsContract.Intents.Insert.EMAIL);
                    if (address == null) {
                        address = mAddContactIntent.getStringExtra(ContactsContract.Intents.Insert.PHONE);
                    }
                    if (address != null) {
                        Contact contact = Contact.get(address, false);
                        if (contact != null) {
                            contact.reload();
                        }
                    }
                }
                break;
            case REQUEST_CODE_ATTACH_RINGTONE:// add by mtk71046
                Uri uri = (Uri) data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (Settings.System.DEFAULT_RINGTONE_URI.equals(uri)) {
                    break;
                }
                addAudio(uri);
                break;

            case REQUEST_CODE_TEXT_VCARD:
                long[] contactIds = data.getLongArrayExtra("com.android.contacts.pickcontacts");
                addTextVCard(contactIds);
                misPickContatct = false;
                return;

            case REQUEST_CODE_ATTACH_VCARD:
                long[] contactIdsAttach = data.getLongArrayExtra("com.android.contacts.pickcontacts");
                attachVCardByContactsId(contactIdsAttach);
                misPickContatct = false;
                break;
                
            case REQUEST_CODE_ATTACH_VCALENDAR:
                attachVCalendar(data.getData());
                break;

            case REQUEST_CODE_MULTI_SAVE:
                boolean succeeded = false;
                if (data != null && data.hasExtra("multi_save_result")) {
                    succeeded = data.getBooleanExtra("multi_save_result", false);
                }
                final int resId = succeeded ? R.string.copy_to_sdcard_success : R.string.Insert_sdcard;
                Toast.makeText(ComposeMessageActivity.this, resId, Toast.LENGTH_SHORT).show();
                return;
            default:
                // TODO
                break;
        }
        isInitRecipientsEditor = false;
        // 181 add for 121871
        mWorkingMessage.saveDraft();
    }

    private void waitForCompressing() {
        synchronized (ComposeMessageActivity.this) {
            while (mCompressingImage) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Xlog.e(TAG, "intterrupted exception e ", e);
                }
            }
        }
    }

    private void notifyCompressingDone() {
        synchronized (ComposeMessageActivity.this) {
            mCompressingImage = false;
            notify();
        }
    }

    private final ResizeImageResultCallback mResizeImageCallback = new ResizeImageResultCallback() {
        // TODO: make this produce a Uri, that's what we want anyway
        public void onResizeResult(PduPart part, boolean append) {
            if (part == null) {
                handleAddAttachmentError(WorkingMessage.UNKNOWN_ERROR, R.string.type_picture);
                return;
            }
            mWorkingMessage.setmResizeImage(true);
            Context context = ComposeMessageActivity.this;
            PduPersister persister = PduPersister.getPduPersister(context);
            int result;
            if(mWorkingMessage.isDiscarded()){
                return;
            }

            Uri messageUri = mWorkingMessage.getMessageUri();   
            if (null == messageUri) {
                try {
                    messageUri = mWorkingMessage.saveAsMms(true);
                } catch (IllegalStateException e) {
                    Xlog.e(TAG, e.getMessage() + ", go to ConversationList!");
                    goToConversationList();
                }
            }

            try {
                Uri dataUri = persister.persistPart(part, ContentUris.parseId(messageUri));
                result = mWorkingMessage.setAttachment(WorkingMessage.IMAGE, dataUri, append);
                if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                    log("ResizeImageResultCallback: dataUri=" + dataUri);
                }
            } catch (MmsException e) {
                result = WorkingMessage.UNKNOWN_ERROR;
            } catch (NullPointerException e){
                result = WorkingMessage.UNKNOWN_ERROR;
            }

            handleAddAttachmentError(result, R.string.type_picture);
            if(result == WorkingMessage.OK){
                try {
                    mWorkingMessage.saveAsMms(false);
                } catch (IllegalStateException e) {
                    Xlog.e(TAG, e.getMessage() + ", go to ConversationList!");
                    goToConversationList();
                }
            }
        }
    };

    private void handleAddAttachmentError(final int error, final int mediaTypeStringId) {
        if (error == WorkingMessage.OK) {
            return;
        }

        runOnUiThread(new Runnable() {
            public void run() {
                Resources res = getResources();
                String mediaType = res.getString(mediaTypeStringId);
                String title, message;

                switch(error) {
                    case WorkingMessage.UNKNOWN_ERROR:
                        message = res.getString(R.string.failed_to_add_media, mediaType);
                        Toast.makeText(ComposeMessageActivity.this, message, Toast.LENGTH_SHORT).show();
                        return;
                    case WorkingMessage.UNSUPPORTED_TYPE:
                    case WorkingMessage.RESTRICTED_TYPE:
                        title = res.getString(R.string.unsupported_media_format, mediaType);
                        message = res.getString(R.string.select_different_media, mediaType);
                        break;
                    case WorkingMessage.MESSAGE_SIZE_EXCEEDED:
                        title = res.getString(R.string.exceed_message_size_limitation, mediaType);
                        message = res.getString(R.string.failed_to_add_media, mediaType);
                        break;
                    case WorkingMessage.IMAGE_TOO_LARGE:
                        title = res.getString(R.string.failed_to_resize_image);
                        message = res.getString(R.string.resize_image_error_information);
                        break;
                    case WorkingMessage.RESTRICTED_RESOLUTION:
                        title = res.getString(R.string.select_different_media_type);
                        message = res.getString(R.string.image_resolution_too_large);
                        break;
                default:
                    throw new IllegalArgumentException("unknown error " + error);
                }

                MessageUtils.showErrorDialog(ComposeMessageActivity.this, title, message);
            }
        });
    }

    private Uri mRestrictedMidea = null;
    private boolean mRestrictedAppend = false;
    private int mRestrictedType = WorkingMessage.TEXT;

    private void showConfirmDialog(Uri uri, boolean append, int type, int messageId) {
        mRestrictedMidea = uri;
        mRestrictedAppend = append;
        mRestrictedType = type;
        new AlertDialog.Builder(ComposeMessageActivity.this)
        .setTitle(R.string.unsupport_media_type)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setMessage(messageId)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public final void onClick(DialogInterface dialog, int which) {
                if (mRestrictedMidea == null || mRestrictedType == WorkingMessage.TEXT || mWorkingMessage.isDiscarded()){
                    return;
                }
                int createMode = WorkingMessage.sCreationMode;
                WorkingMessage.sCreationMode = 0;
                int result = mWorkingMessage.setAttachment(mRestrictedType, mRestrictedMidea, mRestrictedAppend);
                if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                    log("Restricted Midea: dataUri=" + mRestrictedMidea);
                }
                if (mRestrictedType == WorkingMessage.IMAGE && (result == WorkingMessage.IMAGE_TOO_LARGE ||
                    result == WorkingMessage.MESSAGE_SIZE_EXCEEDED)) {
                    if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                        log("addImage: resize image " + mRestrictedMidea);
                    }
                    MessageUtils.resizeImageAsync(ComposeMessageActivity.this,
                            mRestrictedMidea, mAttachmentEditorHandler, mResizeImageCallback, mRestrictedAppend);
                    WorkingMessage.sCreationMode = createMode;
                    return;
                }
                WorkingMessage.sCreationMode = createMode;
                int typeId = R.string.type_picture;
                if (mRestrictedType == WorkingMessage.AUDIO) {
                    typeId = R.string.type_audio;
                } else if (mRestrictedType == WorkingMessage.VIDEO) {
                    typeId = R.string.type_video;
                }
                handleAddAttachmentError(result, typeId);
            }
        })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
    }
    
    
    private Uri mCreationUri = null;
    private boolean mCreationAppend = false;

    private void addImageAsync(final Uri uri, final String mimeType, final boolean append) {
        mCompressingImage = true;
        runAsyncWithDialog(new Runnable() {
            public void run() {
                addImage(uri, append);
                mWorkingMessage.saveAsMms(false);
            }
        }, R.string.adding_attachments_title);
    }
    
    private void addImage(Uri uri, boolean append) {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("addImage: append=" + append + ", uri=" + uri);
        }

        int result = mWorkingMessage.setAttachment(WorkingMessage.IMAGE, uri, append);

        if (result == WorkingMessage.IMAGE_TOO_LARGE ||
            result == WorkingMessage.MESSAGE_SIZE_EXCEEDED) {
            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                log("addImage: resize image " + uri);
            }
            // Adjust whether its a DRM IMAGE
            if (FeatureOption.MTK_DRM_APP) {
                if (!MessageUtils.checkUriContainsDrm(this, uri)) {
                    mToastCountForResizeImage++;
                    if (mToastCountForResizeImage == 1) {
                        MessageUtils.resizeImage(this, uri, mAttachmentEditorHandler, mResizeImageCallback, append,
                            true);
                    } else {
                        MessageUtils.resizeImage(this, uri, mAttachmentEditorHandler, mResizeImageCallback, append,
                            false);
                    }
                } else {
                    handleAddAttachmentError(result, R.string.type_picture);
                }
            } else {
                mToastCountForResizeImage++;
                if (mToastCountForResizeImage == 1) {
                    MessageUtils.resizeImage(this, uri, mAttachmentEditorHandler, mResizeImageCallback, append, true);
                } else {
                    MessageUtils.resizeImage(this, uri, mAttachmentEditorHandler, mResizeImageCallback, append, false);
                }
            }
            return;
        } else if (result == WorkingMessage.WARNING_TYPE) {
            mCreationUri = uri;
            mCreationAppend = append;
            runOnUiThread(new Runnable() {
                public void run() {
                    showConfirmDialog(mCreationUri, mCreationAppend, WorkingMessage.IMAGE, R.string.confirm_restricted_image);
                }
            });
            return;
        }
        handleAddAttachmentError(result, R.string.type_picture);
    }

    private void addVideoAsync(final Uri uri, final boolean append) {
        runAsyncWithDialog(new Runnable() {
            public void run() {
                addVideo(uri, append);
                mWorkingMessage.saveDraft();
            }
        }, R.string.adding_attachments_title);
    }

    private void addVideo(Uri uri, boolean append) {
        if (uri != null) {
            int result = mWorkingMessage.setAttachment(WorkingMessage.VIDEO, uri, append);
            if (result == WorkingMessage.WARNING_TYPE){
                showConfirmDialog(uri, append, WorkingMessage.VIDEO, R.string.confirm_restricted_video);
            } else {
                handleAddAttachmentError(result, R.string.type_video);
            }
        }
    }

    private void addAudio(Uri uri) {
        int result = mWorkingMessage.setAttachment(WorkingMessage.AUDIO, uri, false);
        if (result == WorkingMessage.WARNING_TYPE){
            showConfirmDialog(uri, false, WorkingMessage.AUDIO, R.string.confirm_restricted_audio);
            return;
        }
        handleAddAttachmentError(result, R.string.type_audio);
    }
    
    private void addAudio(Uri uri, boolean append) {
        int result = mWorkingMessage.setAttachment(WorkingMessage.AUDIO, uri, append);
        if (result == WorkingMessage.WARNING_TYPE){
            showConfirmDialog(uri, false, WorkingMessage.AUDIO, R.string.confirm_restricted_audio);
            return;
        }
        handleAddAttachmentError(result, R.string.type_audio);
    }

    private void addTextVCard(long[] contactsIds) {
        Xlog.i(TAG, "compose.addTextVCard(): contactsIds.length() = " + contactsIds.length);
        String textVCard = TextUtils.isEmpty(mTextEditor.getText())? "": "\n";
        StringBuilder sb = new StringBuilder("");
        for (long contactId : contactsIds) {
            if (contactId == contactsIds[contactsIds.length-1]) {
                sb.append(contactId);
            } else {
                sb.append(contactId + ",");
            }
        }
        String selection = Data.CONTACT_ID + " in (" + sb.toString() + ")";

        Xlog.i(TAG, "compose.addTextVCard(): selection = " + selection);
        Uri dataUri = Uri.parse("content://com.android.contacts/data");
        Cursor cursor = getContentResolver().query(
            dataUri, // URI
            new String[]{Data.CONTACT_ID, Data.MIMETYPE, Data.DATA1}, // projection
            selection, // selection
            null, // selection args
            RawContacts.SORT_KEY_PRIMARY); // sortOrder
        if (cursor != null) {
            textVCard = getVCardString(cursor, textVCard);
            insertText(mTextEditor, textVCard);
            cursor.close();
        }
    }

    private void setFileAttachment(final String fileName, final int type, final boolean append) {
        final File attachFile = getFileStreamPath(fileName);
        final Resources res = getResources();
        if (attachFile.exists() && attachFile.length() > 0) {
            Uri attachUri = Uri.fromFile(attachFile);
            final int result = mWorkingMessage.setAttachment(type, attachUri, append);
            handleAddAttachmentError(result, R.string.type_common_file);
        } else {
            Toast.makeText(ComposeMessageActivity.this, 
                    res.getString(R.string.failed_to_add_media, fileName), Toast.LENGTH_SHORT).show();
        }
    }

    // the uri must be a vcard uri created by Contacts
    private void attachVCardByUri(Uri uri) {
        if (uri == null) {
            return;
        }
        final String filename = getVCardFileName();
        try {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = getContentResolver().openInputStream(uri);
                out = openFileOutput(filename, Context.MODE_PRIVATE);
                byte[] buf = new byte[8096];
                int size = 0;
                while ((size = in.read(buf)) != -1) {
                    out.write(buf, 0, size);
                }
            } finally {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "exception attachVCardByUri ", e);
        }
        setFileAttachment(filename, WorkingMessage.VCARD, false);
    }

    private void attachVCalendar(final Uri eventUri) {
        if (eventUri == null) {
            return;
        }
        final int result = mWorkingMessage.setAttachment(WorkingMessage.VCALENDAR, eventUri, false);
        handleAddAttachmentError(result, R.string.type_common_file);
    }

    private void attachVCardByContactsId(long[] contactsIds) {
        // make contacts' id string
        StringBuilder contactsIdsStr = new StringBuilder("");
        for (int i = 0; i < contactsIds.length-1; i++) {
            contactsIdsStr.append(contactsIds[i]);
            contactsIdsStr.append(',');
        }
        contactsIdsStr.append(contactsIds[contactsIds.length-1]);
        final String ids = contactsIdsStr.toString();
        if (!ids.equals("")) {
            AttachVCardWorkerThread worker = new AttachVCardWorkerThread(ids);
            worker.run();
        }
    }

    // turn contacts id into *.vcf file attachment
    private class AttachVCardWorkerThread extends Thread {
        private String mContactIds;

        public AttachVCardWorkerThread(String ids) {
            mContactIds = ids;
        }

        @Override
        public void run() {
            final String fileName = getVCardFileName();
            try {
                VCardComposer composer = null;
                try {
                    composer = new VCardComposer(ComposeMessageActivity.this, VCardConfig.VCARD_TYPE_V30_GENERIC, true);
                    composer.addHandler(
                            composer.new HandlerForOutputStream(openFileOutput(fileName, Context.MODE_PRIVATE)));
                    if (!composer.init(Contacts._ID + " IN (" + mContactIds + ")", null)) {
                        // fall through to catch clause
                        throw new VCardException("Canot initialize " + composer.getErrorReason());
                    }
                    while (!composer.isAfterLast()) {
                        if (!composer.createOneEntry()) {
                            throw new VCardException("Canot create entry " + composer.getErrorReason());
                        }
                    }
                } finally {
                    if (composer != null) {
                        composer.terminate();
                    }
                }
            } catch (VCardException e) {
                Log.e(TAG, "export vcard file, vcard exception " + e.getMessage());
            } catch (FileNotFoundException e) {
                Log.e(TAG, "export vcard file, file not found exception " + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "export vcard file, IO exception " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "export vcard file, exception " + e.getMessage());
            }

            setFileAttachment(fileName, WorkingMessage.VCARD, false);
        }
    }

    private String getVCardFileName() {
        final String fileExtension = ".vcf";
        // base on time stamp
        String name = DateFormat.format("yyyyMMdd_hhmmss", new Date(System.currentTimeMillis())).toString();
        name = name.trim();
        return name + fileExtension;
    }

    private boolean handleForwardedMessage() {
        Intent intent = getIntent();

        // If this is a forwarded message, it will have an Intent extra
        // indicating so.  If not, bail out.
        if (intent.getBooleanExtra(FORWARD_MESSAGE, false) == false) {
            return false;
        }

        Uri uri = intent.getParcelableExtra("msg_uri");

        if (Log.isLoggable(LogTag.APP, Log.DEBUG)) {
            log("handle forwarded message " + uri);
        }

        if (uri != null) {
            mWorkingMessage = WorkingMessage.load(this, uri);
            mWorkingMessage.setSubject(intent.getStringExtra("subject"), false);
        } else {
        	String smsAddress = null;
        	if (intent.hasExtra(SMS_ADDRESS)) {
        		smsAddress = intent.getStringExtra(SMS_ADDRESS);
        		if (smsAddress != null){
                    mRecipientsEditor.addRecipient(smsAddress, true);
                }
        	}
            mWorkingMessage.setText(intent.getStringExtra(SMS_BODY));
        }

        // let's clear the message thread for forwarded messages
        mMsgListAdapter.changeCursor(null);

        return true;
    }

    private boolean handleSendIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return false;
        }
        
        //add for saveAsMms
        mWorkingMessage.setConversation(mConversation);

        final String mimeType = intent.getType();
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                final Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
                runAsyncWithDialog(new Runnable() {
                    public void run() {
                        addAttachment(mimeType, uri, false);
                        SlideshowModel mSlideShowModel = mWorkingMessage.getSlideshow();
                        if (mSlideShowModel != null && mSlideShowModel.getCurrentSlideshowSize() > 0) {
                            mWorkingMessage.saveAsMms(false);
                        }
                    }
                }, R.string.adding_attachments_title);
                return true;
            } else if (extras.containsKey(Intent.EXTRA_TEXT)) {
                mWorkingMessage.setText(extras.getString(Intent.EXTRA_TEXT));
                return true;
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) &&
                extras.containsKey(Intent.EXTRA_STREAM)) {
            SlideshowModel slideShow = mWorkingMessage.getSlideshow();
            final ArrayList<Parcelable> uris = extras.getParcelableArrayList(Intent.EXTRA_STREAM);
            int currentSlideCount = slideShow != null ? slideShow.size() : 0;
            int importCount = uris.size();
            if (importCount + currentSlideCount > SlideshowEditor.MAX_SLIDE_NUM) {
                importCount = Math.min(SlideshowEditor.MAX_SLIDE_NUM - currentSlideCount,
                        importCount);
                Toast.makeText(ComposeMessageActivity.this,
                        getString(R.string.too_many_attachments,
                                SlideshowEditor.MAX_SLIDE_NUM, importCount),
                                Toast.LENGTH_LONG).show();
            }
            final int numberToImport = importCount;
            Xlog.i(TAG, "numberToImport: " + numberToImport);
            final WorkingMessage msg = mWorkingMessage;
            runAsyncWithDialog(new Runnable() {
                public void run() {
                    mToastCountForResizeImage = 0;
                    for (int i = 0; i < numberToImport; i++) {
                        Parcelable uri = uris.get(i);
                        String scheme = ((Uri) uri).getScheme();
                        if (scheme != null && scheme.equals("file")) {
                        	// change "file://..." Uri to "Content://...., and attemp to add this attachment"
                        	addFileAttachment(mimeType, (Uri)uri, true);  
                        } else {
                            addAttachment(mimeType, (Uri) uri, true);
                        }
                    }
                    mToastCountForResizeImage = 0;
                    SlideshowModel mSlideShowModel = mWorkingMessage.getSlideshow();
                    if (mSlideShowModel != null && mSlideShowModel.size() > 0) {
                        mWorkingMessage.saveAsMms(false);
                    }
                }
            }, R.string.adding_attachments_title);
            return true;
        }

        return false;
    }

    
    // Shows the activity's progress spinner. Should be canceled if exiting the activity.
    private Runnable mShowProgressDialogRunnable = new Runnable() {
        public void run() {
            if (mProgressDialog != null) {
                mProgressDialog.show();
            }
        }
    };
    /**
     * Asynchronously executes a task while blocking the UI with a progress spinner.
     *
     * Must be invoked by the UI thread.  No exceptions!
     *
     * @param task the work to be done wrapped in a Runnable
     * @param dialogStringId the id of the string to be shown in the dialog
     */
    private void runAsyncWithDialog(final Runnable task, final int dialogStringId) {
        new ModalDialogAsyncTask(dialogStringId).execute(new Runnable[] {task});
    }
    
    /**
     * Asynchronously performs tasks specified by Runnables.
     * Displays a progress spinner while the tasks are running.  The progress spinner
     * will only show if tasks have not finished after a certain amount of time.
     *
     * This AsyncTask must be instantiated and invoked on the UI thread.
     */
    private class ModalDialogAsyncTask extends AsyncTask<Runnable, Void, Void> {
        final int mDialogStringId;

        /**
         * Creates the Task with the specified string id to be shown in the dialog
         */
        public ModalDialogAsyncTask(int dialogStringId) {
            this.mDialogStringId = dialogStringId;
            // lazy initialization of progress dialog for loading attachments
            if (mProgressDialog == null) {
                mProgressDialog = createProgressDialog();
            }
        }

        /**
         * Initializes the progress dialog with its intended settings.
         */
        private ProgressDialog createProgressDialog() {
            ProgressDialog dialog = new ProgressDialog(ComposeMessageActivity.this);
            dialog.setIndeterminate(true);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            dialog.setMessage(ComposeMessageActivity.this.
                    getText(mDialogStringId));
            return dialog;
        }

        /**
         * Activates a progress spinner on the UI.  This assumes the UI has invoked this Task.
         */
        @Override
        protected void onPreExecute() {
            // activate spinner after half a second
            mAttachmentEditorHandler.postDelayed(mShowProgressDialogRunnable, 500);
        }

        /**
         * Perform the specified Runnable tasks on a background thread
         */
        @Override
        protected Void doInBackground(Runnable... params) {
            if (params != null) {
                try {
                    for (int i = 0; i < params.length; i++) {
                        params[i].run();
                    }
                } finally {
                    // Cancel pending display of the progress bar if the image has finished loading.
                    mAttachmentEditorHandler.removeCallbacks(mShowProgressDialogRunnable);
                }
            }
            return null;
        }

        /**
         * Deactivates the progress spinner on the UI. This assumes the UI has invoked this Task.
         */
        @Override
        protected void onPostExecute(Void result) {
            if (mProgressDialog != null) {
                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                mProgressDialog = null;
            }
        }
    }
    
    private void clearPendingProgressDialog() {
        // remove any callback to display a progress spinner
        mAttachmentEditorHandler.removeCallbacks(mShowProgressDialogRunnable);
        // clear the dialog so any pending dialog.dismiss() call can be avoided
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
        
    private void addFileAttachment(String type, Uri uri, boolean append) {
    	
        if (!addFileAttachment(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, type, uri, append)) {
        	if (!addFileAttachment(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, type, uri, append)) {
        		if (!addFileAttachment(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, type, uri, append)) {
        			Xlog.i(TAG, "This file is not in media store(audio, video or image)," +
        					"attemp to add it like file uri");
        			addAttachment(type, (Uri) uri, append);
        		}
        	}
        } 
    }
    
    private boolean addFileAttachment(Uri mediaStoreUri, String type, Uri uri, boolean append) {
    	String path = uri.getPath();
        if (path != null) {
        	Cursor c = getContentResolver().query(mediaStoreUri, 
                    new String[] {MediaStore.MediaColumns._ID, Audio.Media.MIME_TYPE}, MediaStore.MediaColumns.DATA + "=?",
                    new String[] {path}, null);
        	if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        Uri contentUri = Uri.withAppendedPath(mediaStoreUri, c.getString(0));
                        Xlog.i(TAG, "Get id in MediaStore:" + c.getString(0));
                        Xlog.i(TAG, "Get content type in MediaStore:" + c.getString(1));
                        Xlog.i(TAG, "Get uri in MediaStore:" + contentUri);
                        
                        String contentType = c.getString(1);
                        addAttachment(contentType, contentUri, append);
                        return true;
                    } else {
                        Xlog.i(TAG, "MediaStore:" + mediaStoreUri.toString() + " has not this file");
                    }
                } finally {
                    c.close();
                }
        	}
        }
        return false;
    }
 
    // mVideoUri will look like this: content://media/external/video/media
    private static final String mVideoUri = Video.Media.getContentUri("external").toString();
    // mImageUri will look like this: content://media/external/images/media
    private static final String mImageUri = Images.Media.getContentUri("external").toString();
	// mAudioUri will look like this: content://media/external/images/media
	private static final String mAudioUri = Audio.Media.getContentUri("external").toString();
	
    private void addAttachment(String type, Uri uri, boolean append) {
        if (uri != null) {
            // When we're handling Intent.ACTION_SEND_MULTIPLE, the passed in items can be
            // videos, and/or images, and/or some other unknown types we don't handle. When
            // a single attachment is "shared" the type will specify an image or video. When
            // there are multiple types, the type passed in is "*/*". In that case, we've got
            // to look at the uri to figure out if it is an image or video.
            boolean wildcard = "*/*".equals(type);
            Xlog.i(TAG, "Got send intent mimeType :" + type);
            if (type.startsWith("image/") || 
            		(wildcard && uri.toString().startsWith(mImageUri))) {
                addImage(uri, append);
            } else if (type.startsWith("video/") ||
                    (wildcard && uri.toString().startsWith(mVideoUri))) {
                addVideo(uri, append);
            } else if (type.startsWith("audio/") || type.equals("application/ogg")
					        || (wildcard && uri.toString().startsWith(mAudioUri))) {
                addAudio(uri, append);
            } else if (type.equalsIgnoreCase("text/x-vcard")) {
                attachVCardByUri(uri);
            } else if (type.equalsIgnoreCase("text/x-vcalendar")) {
                // add for vcalendar
                attachVCalendar(uri);
            }
        }
    }

    private String getResourcesString(int id, String mediaName) {
        Resources r = getResources();
        return r.getString(id, mediaName);
    }

    private void drawBottomPanel() {
        // Reset the counter for text editor.
        resetCounter();

        if (mWorkingMessage.hasSlideshow() && !mMsgListAdapter.mIsDeleteMode) {
            mBottomPanel.setVisibility(View.GONE);
            mDeletePanel.setVisibility(View.GONE);
            mAttachmentEditor.update(mWorkingMessage);
            mAttachmentEditor.requestFocus();
            return;
        }
        //add for multi-delete
        if (mMsgListAdapter.mIsDeleteMode) {
            mBottomPanel.setVisibility(View.GONE);
            mAttachmentEditor.hideView();
            mDeletePanel.setVisibility(View.VISIBLE);
            return;
        } 

        mDeletePanel.setVisibility(View.GONE);
        mAttachmentEditor.update(mWorkingMessage);
        mBottomPanel.setVisibility(View.VISIBLE);
        CharSequence text = mWorkingMessage.getText();

        // TextView.setTextKeepState() doesn't like null input.
        if (text != null) {
            mTextEditor.setTextKeepState(text);
            mTextEditor.setSelection(text.length());
        } else {
            mTextEditor.setText("");
        }
    }

    private void drawTopPanel() {
        //add for multi-delete
        if (mMsgListAdapter.mIsDeleteMode) {
            mSelectPanel.setVisibility(View.VISIBLE);
            mSelectedAll.setChecked(false);
        } else {
            mSelectPanel.setVisibility(View.GONE);
        }
        showSubjectEditor(mWorkingMessage.hasSubject() && !mMsgListAdapter.mIsDeleteMode);
    }

    //==========================================================
    // Interface methods
    //==========================================================

    public void onClick(View v) {
        if ((v == mSendButton)) {
        	if (mSendButtonCanResponse) {
        		mSendButtonCanResponse = false;
                if (isPreparedForSending()) {
                    // Since sending message here, why not disable button 'Send'??
                	updateSendButtonState(false);
                    //simSelection();
                    checkRecipientsCount();
                    mSendButtonCanResponse = true;
                } else {
                	mSendButtonCanResponse = true;
                    if (!isHasRecipientCount()) {
                        new AlertDialog.Builder(this).setIcon(
                            android.R.drawable.ic_dialog_alert).setTitle(
                                R.string.cannot_send_message).setMessage(
                                    R.string.cannot_send_message_reason)
                                        .setPositiveButton(R.string.yes,
                                            new CancelSendingListener()).show();
                    } else {
                        new AlertDialog.Builder(this).setIcon(
                            android.R.drawable.ic_dialog_alert).setTitle(
                                R.string.cannot_send_message).setMessage(
                                    R.string.cannot_send_message_reason_no_content)
                                        .setPositiveButton(R.string.yes,
                                            new CancelSendingListener()).show();

                    }
                }
        	}

        } else if (v == mDeleteButton){ 
            if (mMsgListAdapter.getSelectedNumber() >= mMsgListAdapter.getCount()) {
                ConversationList.confirmDeleteThreadDialog(
                        new ConversationList.DeleteThreadListener(mConversation.getThreadId(),
                                mBackgroundQueryHandler, ComposeMessageActivity.this),
                                false, false, ComposeMessageActivity.this);
            } else {
                confirmMultiDelete();
            }
        } else if (v == mCancelButton) {
            mMsgListAdapter.mIsDeleteMode = false;
            drawTopPanel();
            drawBottomPanel();
            startMsgListQuery();
        } else if (v == mSelectPanel) {
            boolean newStatus = !mSelectedAll.isChecked();
            mSelectedAll.setChecked(newStatus);
            markCheckedState(newStatus);
        }
    }

    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (event != null) {
            // if shift key is down, then we want to insert the '\n' char in the TextView;
            // otherwise, the default action is to send the message.
            if (!event.isShiftPressed()) {
            if(event.getAction() == KeyEvent.ACTION_DOWN) {
                return false;
            }
            if (isPreparedForSending()) {
                //simSelection();
                checkRecipientsCount();
            } else {
                    if (!isHasRecipientCount()) {
                        new AlertDialog.Builder(this).setIcon(
                            android.R.drawable.ic_dialog_alert).setTitle(
                                R.string.cannot_send_message).setMessage(
                                    R.string.cannot_send_message_reason)
                                        .setPositiveButton(R.string.yes,
                                            new CancelSendingListener()).show();

                    } else {
                        new AlertDialog.Builder(this).setIcon(
                            android.R.drawable.ic_dialog_alert).setTitle(
                                R.string.cannot_send_message).setMessage(
                                    R.string.cannot_send_message_reason_no_content)
                                        .setPositiveButton(R.string.yes,new CancelSendingListener()).show();
                    }
                }
                return true;
            }
            return false;
        }

        if (isPreparedForSending()) {
            //simSelection();
            checkRecipientsCount();
        } else {
            if (!isHasRecipientCount()) {
                new AlertDialog.Builder(this).setIcon(
                    android.R.drawable.ic_dialog_alert).setTitle(
                        R.string.cannot_send_message).setMessage(
                            R.string.cannot_send_message_reason)
                                .setPositiveButton(R.string.yes,
                                    new CancelSendingListener()).show();
            } else {
                new AlertDialog.Builder(this).setIcon(
                    android.R.drawable.ic_dialog_alert).setTitle(
                        R.string.cannot_send_message).setMessage(
                            R.string.cannot_send_message_reason_no_content).setPositiveButton(
                                R.string.yes, new CancelSendingListener()).show();

            }
        }
        return true;
    }

    private final TextWatcher mTextEditorWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // This is a workaround for bug 1609057.  Since onUserInteraction() is
            // not called when the user touches the soft keyboard, we pretend it was
            // called when textfields changes.  This should be removed when the bug
            // is fixed.
            onUserInteraction();

            mWorkingMessage.setText(s);
            mAttachmentEditor.onTextChangeForOneSlide(s);

            updateSendButtonState();

            updateCounter(s, start, before, count);

        }

        public void afterTextChanged(Editable s) {
        }
    };

    /**
     * Ensures that if the text edit box extends past two lines then the
     * button will be shifted up to allow enough space for the character
     * counter string to be placed beneath it.
     */
    private void ensureCorrectButtonHeight() {
        int currentTextLines = mTextEditor.getLineCount();
        if (currentTextLines <= 2) {
            mTextCounter.setVisibility(View.GONE);
        }
        else if (currentTextLines > 2 && mTextCounter.getVisibility() == View.GONE) {
            // Making the counter invisible ensures that it is used to correctly
            // calculate the position of the send button even if we choose not to
            // display the text.
            mTextCounter.setVisibility(View.INVISIBLE);
        }
    }

    private final TextWatcher mSubjectEditorWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mWorkingMessage.setSubject(s, true);
            mTextCounter.setVisibility(View.GONE);
        }

        public void afterTextChanged(Editable s) { }
    };

    //==========================================================
    // Private methods
    //==========================================================

    /**
     * Initialize all UI elements from resources.
     */
    private void initResourceRefs() {
        mMsgListView = (MessageListView) findViewById(R.id.history);
        mMsgListView.setDivider(null);      // no divider so we look like IM conversation.
        mBottomPanel = findViewById(R.id.bottom_panel);
        //add for multi-delete
        mDeletePanel = findViewById(R.id.delete_panel);
        mSelectPanel = findViewById(R.id.select_panel);
        if (FeatureOption.MTK_THEMEMANAGER_APP) {
            mSelectPanel.setThemeColor("mms_selected_all_bg_color", 0xffeeeeee);
        } else {
            mSelectPanel.setBackgroundColor(R.color.read_bgcolor);
        }
        mSelectPanel.setOnClickListener(this);
        mSelectedAll = (CheckBox)findViewById(R.id.select_all_checked);
        mCancelButton = (Button)findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(this);
        mDeleteButton = (Button)findViewById(R.id.delete);
        mDeleteButton.setOnClickListener(this);
        mDeleteButton.setEnabled(false);

        mTextEditor = (EditText) findViewById(R.id.embedded_text_editor);
        //mTextEditor.setOnEditorActionListener(this);
        mTextEditor.addTextChangedListener(mTextEditorWatcher);
        mTextEditor.setFilters(new InputFilter[] {
                new LengthFilter(MmsConfig.getMaxTextLimit())});
        mTextCounter = (TextView) findViewById(R.id.text_counter);

        mSendButton = (Button) findViewById(R.id.send_button);
        mSendButton.setOnClickListener(this);

        mTopPanel = findViewById(R.id.recipients_subject_linear);
        mTopPanel.setFocusable(false);
        mRecipients = findViewById(R.id.recipients_linear);
        mRecipientsAvatar = findViewById(R.id.recipients_avatar);
        mContactHeader = (ContactHeaderWidget) findViewById(R.id.contact_header);
        mPickContacts = (ImageButton) findViewById(R.id.pick_contacts);
        mPickContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recipientCount() >= RECIPIENTS_LIMIT_FOR_SMS) {
                    Toast.makeText(ComposeMessageActivity.this, R.string.cannot_add_recipient, Toast.LENGTH_SHORT).show();
                } else {
                    addContacts(100);
                }
            }
        });
        mJumpToContacts = (ImageButton) findViewById(R.id.jump_to_contacts);

        mAttachmentEditor = (AttachmentEditor) findViewById(R.id.attachment_editor);
        mAttachmentEditor.setHandler(mAttachmentEditorHandler);
    }

    private void confirmDeleteDialog(OnClickListener listener, boolean locked) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(locked ? R.string.confirm_dialog_locked_title :
            R.string.confirm_dialog_title);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setCancelable(true);
        builder.setMessage(locked ? R.string.confirm_delete_locked_message :
                    R.string.confirm_delete_message);
        builder.setPositiveButton(R.string.delete, listener);
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }

    void undeliveredMessageDialog(long date) {
        String body;
        LinearLayout dialog = (LinearLayout) LayoutInflater.from(this).inflate(
                R.layout.retry_sending_dialog, null);

        if (date >= 0) {
            body = getString(R.string.undelivered_msg_dialog_body,
                    MessageUtils.formatTimeStampString(this, date));
        } else {
            // FIXME: we can not get sms retry time.
            body = getString(R.string.undelivered_sms_dialog_body);
        }

        ((TextView) dialog.findViewById(R.id.body_text_view)).setText(body);

        Toast undeliveredDialog = new Toast(this);
        undeliveredDialog.setView(dialog);
        undeliveredDialog.setDuration(Toast.LENGTH_LONG);
        undeliveredDialog.show();
    }

    private void startMsgListQuery() {
        final Uri conversationUri = mConversation.getUri();

        if (conversationUri == null) {
            return;
        }

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("startMsgListQuery for " + conversationUri);
        }

        // Cancel any pending queries
        mBackgroundQueryHandler.cancelOperation(MESSAGE_LIST_QUERY_TOKEN);
        try {
            mBackgroundQueryHandler.postDelayed(new Runnable() {
                public void run() {
                    mBackgroundQueryHandler.startQuery(
                            MESSAGE_LIST_QUERY_TOKEN, null, conversationUri,
                            PROJECTION, null, null, null);
                }
            }, 50);
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    private void initMessageList() {
        if (mMsgListAdapter != null) {
            return;
        }

        String highlightString = getIntent().getStringExtra("highlight");
        Pattern highlight = highlightString == null
            ? null
            : Pattern.compile("\\b" + Pattern.quote(highlightString), Pattern.CASE_INSENSITIVE);

        // Initialize the list adapter with a null cursor.
        mMsgListAdapter = new MessageListAdapter(this, null, mMsgListView, true, highlight);
        mMsgListAdapter.setOnDataSetChangedListener(mDataSetChangedListener);
        mMsgListAdapter.setMsgListItemHandler(mMessageListItemHandler);
        mMsgListView.setAdapter(mMsgListAdapter);
        mMsgListView.setItemsCanFocus(false);
        mMsgListView.setVisibility(View.VISIBLE);
        mMsgListView.setOnCreateContextMenuListener(mMsgListMenuCreateListener);
        mMsgListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (view != null) {
                    ((MessageListItem) view).onMessageListItemClick();
                }
            }
        });
    }

    private void loadDraft() {
        if (mWorkingMessage.isWorthSaving()) {
            Log.w(TAG, "loadDraft() called with non-empty working message");
            return;
        }

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("loadDraft: call WorkingMessage.loadDraft");
        }

        mWorkingMessage = WorkingMessage.loadDraft(this, mConversation);
    }

    private void saveDraft() {
        // TODO: Do something better here.  Maybe make discard() legal
        // to call twice and make isEmpty() return true if discarded
        // so it is caught in the clause above this one?
        if (mWorkingMessage.isDiscarded()) {
            return;
        }

        if (!mWaitingForSubActivity && !mWorkingMessage.isWorthSaving() && !mWaitingForSendMessage) {
            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                log("saveDraft: not worth saving, discard WorkingMessage and bail");
            }
            mWorkingMessage.discard(false);
            return;
        }

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("saveDraft: call WorkingMessage.saveDraft");
        }

        mWorkingMessage.saveDraft();
        if (mToastForDraftSave) {
            Toast.makeText(this, R.string.message_saved_as_draft,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isPreparedForSending() {
        if (isRecipientsEditorVisible()) {
            String recipientText = mRecipientsEditor.getText() == null? "" : mRecipientsEditor.getText().toString();
            return mSimCount> 0 && (recipientText != null && !recipientText.equals("")) && (mWorkingMessage.hasAttachment() || mWorkingMessage.hasText());
        } else {
            return mSimCount> 0 && (mWorkingMessage.hasAttachment() || mWorkingMessage.hasText());
        }
    }

    private boolean isHasRecipientCount(){
        int recipientCount = recipientCount();
        return (recipientCount > 0 && recipientCount < RECIPIENTS_LIMIT_FOR_SMS);
    }

    private int recipientCount() {
        int recipientCount;

        // To avoid creating a bunch of invalid Contacts when the recipients
        // editor is in flux, we keep the recipients list empty.  So if the
        // recipients editor is showing, see if there is anything in it rather
        // than consulting the empty recipient list.
        if (isRecipientsEditorVisible()) {
            recipientCount = mRecipientsEditor.getRecipientCount();
        } else {
            recipientCount = getRecipients().size();
        }
        return recipientCount;
    }

    private String getResourcesString(int id) {
        Resources r = getResources();
        return r.getString(id);
    }

    private void checkConditionsAndSendMessage(boolean bCheckEcmMode){
        // check pin
        // convert sim id to slot id
        int requestType = CellConnMgr.REQUEST_TYPE_SIMLOCK;
        final int slotId;
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            requestType = CellConnMgr.REQUEST_TYPE_ROAMING;
            slotId = SIMInfo.getSlotById(this, mSelectedSimId);
            Xlog.d(MmsApp.TXN_TAG, "check pin and...: simId=" + mSelectedSimId + "\t slotId=" + slotId);
        } else {
            slotId = 0;
        }
        final boolean bCEM = bCheckEcmMode;
        mCellMgr.handleCellConn(slotId, requestType, new Runnable() {
            public void run() {
                int nRet = mCellMgr.getResult();
                Xlog.d(MmsApp.TXN_TAG, "serviceComplete result = " + CellConnMgr.resultToString(nRet));
                if (mCellMgr.RESULT_ABORT == nRet || mCellMgr.RESULT_OK == nRet) {
                    updateSendButtonState();
                    return;
                }
                if (slotId != mCellMgr.getPreferSlot()) {
                    SIMInfo si = SIMInfo.getSIMInfoBySlot(ComposeMessageActivity.this, mCellMgr.getPreferSlot());
                    if (si == null) {
                        Xlog.e(MmsApp.TXN_TAG, "serviceComplete siminfo is null");
                        updateSendButtonState();
                        return;
                    }
                    mSelectedSimId = (int)si.mSimId;
                }
                sendMessage(bCEM);
            }
        });
    }

    private void sendMessage(boolean bCheckEcmMode) {
        if (mWorkingMessage.requiresMms() && (mWorkingMessage.hasSlideshow() || mWorkingMessage.hasAttachment())) {
            if (mWorkingMessage.getCurrentMessageSize() > MmsConfig.getUserSetMmsSizeLimit(true)) {
                MessageUtils.showErrorDialog(ComposeMessageActivity.this,
                        getResourcesString(R.string.exceed_message_size_limitation),
                        getResourcesString(R.string.exceed_message_size_limitation));
                updateSendButtonState();
                return;
            }
        }
        if (bCheckEcmMode) {
            // TODO: expose this in telephony layer for SDK build
            String inEcm = SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE);
            if (Boolean.parseBoolean(inEcm)) {
                try {
                    startActivityForResult(
                            new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                            REQUEST_CODE_ECM_EXIT_DIALOG);
                    return;
                } catch (ActivityNotFoundException e) {
                    // continue to send message
                    Log.e(TAG, "Cannot find EmergencyCallbackModeExitDialog", e);
                }
            }
        }

        ContactList contactList = isRecipientsEditorVisible() ?
                mRecipientsEditor.constructContactsFromInput() : getRecipients();
        mDebugRecipients = contactList.serialize();
        if (!mSendingMessage) {
            if (LogTag.SEVERE_WARNING) {
                String sendingRecipients = mConversation.getRecipients().serialize();
                if (!sendingRecipients.equals(mDebugRecipients)) {
                    String workingRecipients = mWorkingMessage.getWorkingRecipients();
                    if (!mDebugRecipients.equals(workingRecipients)) {
                        LogTag.warnPossibleRecipientMismatch("ComposeMessageActivity.sendMessage recipients in " +
                                "window: \"" +
                                mDebugRecipients + "\" differ from recipients from conv: \"" +
                                sendingRecipients + "\" and working recipients: " +
                                workingRecipients, this);
                    }
                }
            }

            // send can change the recipients. Make sure we remove the listeners first and then add
            // them back once the recipient list has settled.
            removeRecipientsListeners();

            //add for gemini TODO
            if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
                mWorkingMessage.sendGemini(mSelectedSimId);
            } else {
                mWorkingMessage.send(mDebugRecipients);
            }

            mSentMessage = true;
            mSendingMessage = true;
            mWaitingForSendMessage = true;
            isInitRecipientsEditor = false; // when tap fail icon, don't add recipients
            addRecipientsListeners();
            mRecipients.setVisibility(View.GONE);
            mMsgListView.setVisibility(View.VISIBLE);
            bindToContactHeaderWidget(mConversation.getRecipients());
        }
        // But bail out if we are supposed to exit after the message is sent.
        if (mExitOnSent) {
            finish();
        }
    }

    private void resetMessage() {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("resetMessage");
        }

        // Make the attachment editor hide its view.
        mAttachmentEditor.hideView();

        // Hide the subject editor.
        showSubjectEditor(false);

        // Focus to the text editor.
        mTextEditor.requestFocus();

        // We have to remove the text change listener while the text editor gets cleared and
        // we subsequently turn the message back into SMS. When the listener is listening while
        // doing the clearing, it's fighting to update its counts and itself try and turn
        // the message one way or the other.
        mTextEditor.removeTextChangedListener(mTextEditorWatcher);

        // Clear the text box.
        TextKeyListener.clear(mTextEditor.getText());

        mWorkingMessage = WorkingMessage.createEmpty(this);
        mWorkingMessage.setConversation(mConversation);

        hideRecipientEditor();
        drawBottomPanel();

        // "Or not", in this case.
        updateSendButtonState();

        // Our changes are done. Let the listener respond to text changes once again.
        mTextEditor.addTextChangedListener(mTextEditorWatcher);

        // Close the soft on-screen keyboard if we're in landscape mode so the user can see the
        // conversation.
        if (mIsLandscape) {
            hideInputMethod();
        }

        mLastRecipientCount = 0;
        mSendingMessage = false;
   }

   private void updateSendButtonState(final boolean enabled) {
       if (!mWorkingMessage.hasSlideshow()) {
           mSendButton.setEnabled(enabled);
           mSendButton.setFocusable(enabled);
       } else {
           mAttachmentEditor.setCanSend(enabled);
       }
   }

    private void updateSendButtonState() {
        boolean enable = false;
        Xlog.v(TAG, "ComposeMessageActivity.updateSendButtonState(): mSimCount = " + mSimCount);
        if (isPreparedForSending()) {
            if (FeatureOption.MTK_GEMINI_SUPPORT == true && mSimCount > 0) {
                // When the type of attachment is slideshow, we should
                // also hide the 'Send' button since the slideshow view
                // already has a 'Send' button embedded.
                if (!mWorkingMessage.hasSlideshow()) {
                    enable = true;
                } else {
                    mAttachmentEditor.setCanSend(true);
                }
            } else {
                ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                if (phone != null) {
                    try {
                        if (phone.isSimInsert(0)) { // check SIM state
                            if (!mWorkingMessage.hasSlideshow()) {
                                enable = true;
                            } else {
                                mAttachmentEditor.setCanSend(true);
                            }
                        } else {
                            mAttachmentEditor.setCanSend(false);
                        }
                    } catch (RemoteException e) {
                        Xlog.w(TAG, "compose.updateSendButton()_singleSIM");
                    }
                }
            }
        } else {
            mAttachmentEditor.setCanSend(false);
        }
        mSendButton.setEnabled(enable);
        mSendButton.setFocusable(enable);
    }

    private long getMessageDate(Uri uri) {
        if (uri != null) {
            Cursor cursor = SqliteWrapper.query(this, mContentResolver,
                    uri, new String[] { Mms.DATE }, null, null, null);
            if (cursor != null) {
                try {
                    if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                        return cursor.getLong(0) * 1000L;
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return NO_DATE_FOR_DIALOG;
    }

    private void initActivityState(Bundle bundle, Intent intent) {
        mIsTooManyRecipients = false;
        if (bundle != null) {
            mCompressingImage = bundle.getBoolean("compressing_image", false);
            String recipientsStr = bundle.getString("recipients");
            int recipientCount = 0;
            if(recipientsStr != null){
                recipientCount = recipientsStr.split(";").length;
                mConversation = Conversation.get(this,
                    ContactList.getByNumbers(this, recipientsStr,
                            false /* don't block */, true /* replace number */), false);
            } else {
                Long threadId = bundle.getLong("thread", 0);
                mConversation = Conversation.get(this, threadId, false);
            }
            
            mExitOnSent = bundle.getBoolean("exit_on_sent", false);
            mWorkingMessage.readStateFromBundle(bundle);
            if (!mCompressingImage && mConversation.hasDraft() && mConversation.getMessageCount() == 0) {
                mWorkingMessage.clearConversation(mConversation);
            }
            if (recipientCount > RECIPIENTS_LIMIT_FOR_SMS) {
                mIsTooManyRecipients = true;
            }
            mCompressingImage = false;
            return;
        }

        String vCardContactsIds = intent.getStringExtra("multi_export_contacts");
        long[] contactsIds = null;
        if (vCardContactsIds != null && !vCardContactsIds.equals("")) {
            String[] vCardConIds = vCardContactsIds.split(",");
            Xlog.e(TAG, "ComposeMessage.initActivityState(): vCardConIds.length" + vCardConIds.length);
            contactsIds = new long[vCardConIds.length];
            try {
                for (int i = 0; i < vCardConIds.length; i++) {
                    contactsIds[i] = Long.parseLong(vCardConIds[i]);
                }
            } catch (NumberFormatException e) {
                contactsIds = null;
            }
        }
        // If we have been passed a thread_id, use that to find our
        // conversation.
        long threadId = intent.getLongExtra("thread_id", 0);
        if (threadId > 0) {
            mConversation = Conversation.get(this, threadId, false);
        } else if (contactsIds != null && contactsIds.length > 0) {
            addTextVCard(contactsIds);
            mConversation = Conversation.createNew(this);
            return;
        } else {
            Uri intentData = intent.getData();

            String action = intent.getAction();
            if (intentData != null && (TextUtils.isEmpty(action) || !action.equals(Intent.ACTION_SEND))) {
                // group-contact send message
                // try to get a conversation based on the data URI passed to our intent.
                if (intentData.getPathSegments().size() < 2) {
                    mConversation = mConversation.get(this,
                            ContactList.getByNumbers(this,
                                    getStringForMultipleRecipients(intentData.getSchemeSpecificPart()),
                                    false /* don't block */, true /* replace number */),
                            false);
                } else {
                    mConversation = Conversation.get(this, intentData, false);
                    mWorkingMessage.setText(getBody(intentData));
                }
                mWorkingMessage.setText(getBody(intentData));
            } else {
                // special intent extra parameter to specify the address
                String address = intent.getStringExtra("address");
                if (!TextUtils.isEmpty(address)) {
                    mConversation = Conversation.get(this, ContactList.getByNumbers(address,
                            false /* don't block */, true /* replace number */), false);
                } else {
                    mConversation = Conversation.createNew(this);
                }
            }
        }
        mExitOnSent = intent.getBooleanExtra("exit_on_sent", false);
        if (intent.hasExtra("sms_body")) {
            String sms_body = intent.getStringExtra("sms_body");
            if (sms_body != null && sms_body.length() > MmsConfig.getMaxTextLimit()) {
                mWorkingMessage.setText(sms_body.subSequence(0, MmsConfig.getMaxTextLimit()));
            } else {
                mWorkingMessage.setText(sms_body);
            }
        }
        mWorkingMessage.setSubject(intent.getStringExtra("subject"), false);
    }

    private void initFocus() {
        if (!mIsKeyboardOpen) {
            return;
        }

        // If the recipients editor is visible, there is nothing in it,
        // and the text editor is not already focused, focus the
        // recipients editor.
        if (isRecipientsEditorVisible()
                && TextUtils.isEmpty(mRecipientsEditor.getText())
                && !mTextEditor.isFocused()) {
            mRecipientsEditor.requestFocus();
            return;
        }

        // If we decided not to focus the recipients editor, focus the text editor.
        mTextEditor.requestFocus();
    }

    private final MessageListAdapter.OnDataSetChangedListener
                    mDataSetChangedListener = new MessageListAdapter.OnDataSetChangedListener() {
        public void onDataSetChanged(MessageListAdapter adapter) {
            mPossiblePendingNotification = true;
        }

        public void onContentChanged(MessageListAdapter adapter) {
            startMsgListQuery();
        }
    };

    private void checkPendingNotification() {
        if (mPossiblePendingNotification && hasWindowFocus()) {
            mConversation.markAsRead();
            mPossiblePendingNotification = false;
        }
    }

    private final class BackgroundQueryHandler extends BaseProgressQueryHandler {
        public BackgroundQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            switch(token) {
                case MESSAGE_LIST_QUERY_TOKEN:
                    if (cursor != null) {
                        int newSelectionPos = -1;
                        long targetMsgId = getIntent().getLongExtra("select_id", -1);
                        if (targetMsgId != -1) {
                            cursor.moveToPosition(-1);
                            long msgId = 0L;
                            while (cursor.moveToNext()) {
                                msgId = cursor.getLong(COLUMN_ID);
                                if (msgId == targetMsgId) {
                                    newSelectionPos = cursor.getPosition();
                                    break;
                                }
                            }
                        }

                        //add for multi-delete
                        Xlog.i(TAG, "compose.onContentChanged(): onContentChanged()");
                        if (mMsgListAdapter.mIsDeleteMode) {
                            int selectedCount = mMsgListAdapter.getSelectedNumber();
                            int allCount = cursor.getCount();
                            Xlog.i(TAG, "selectedCount=" + selectedCount + ", allCount=" + allCount);
                            if (selectedCount < allCount) {
                                mSelectedAll.setChecked(false);
                            } else if ((selectedCount == allCount) && allCount > 0) {
                                mSelectedAll.setChecked(true);
                            }
                            mMsgListAdapter.initListMap(cursor);
                        }

                        changeDeleteMode();
                        mMsgListAdapter.changeCursor(cursor);
                        if (newSelectionPos != -1) {
                            mMsgListView.setSelection(newSelectionPos);
                        }

                        // Once we have completed the query for the message history, if
                        // there is nothing in the cursor and we are not composing a new
                        // message, we must be editing a draft in a new conversation (unless
                        // mSentMessage is true).
                        // Show the recipients editor to give the user a chance to add
                        // more people before the conversation begins.

                        /* Removed because of ALPS00047456.When touch the icon of a send fail sms,
                         * ComposeMessageActivity come back to a edit screen, in some conditions,
                         * It will create a wrong ThreadId. If user continue touch back menu, will
                         * create a wrong draft sms.
                         */
                        /*
                        if(cursor.getCount() == 0 && !mDestroy){
                            mConversation.clearThreadId();
                            mConversation.ensureThreadId();
                        }
                        */
                        //Modified !isRecipientsEditorVisible() to isRecipientsEditorVisible for "Use Message Number" feature
                        if (cursor.getCount() == 0 && isRecipientsEditorVisible() && !mSentMessage) {
                            initRecipientsEditor();
                            isInitRecipientsEditor = true;
                            if (mRecipientsAvatar != null) {
                                mRecipientsAvatar.setVisibility(View.GONE);
                            }
                        } else {
                            /*
                             * Related to ALPS00049425 & ALPS00051531.
                             * The conversation that thread id <= 0, is a draft-only sms. But a draft-only
                             * mms has a valid thread id, because the draft cache must be related to thread
                             * id.
                             */
                            if (!isRecipientsEditorVisible()) {
                                mRecipientsAvatar.setVisibility(View.VISIBLE);
                            }
                        }

                        // FIXME: freshing layout changes the focused view to an unexpected
                        // one, set it back to TextEditor forcely.
                        if (cursor.getCount() > 0) {
                            if (!(isSubjectEditorVisible() && mSubjectTextEditor.hasFocus())) {
                                mTextEditor.requestFocus();
                            }
                        }

                        // 181 for 121767
                        if (mWorkingMessage.hasSlideshow()){
                            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                                 WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                        }
                        mConversation.blockMarkAsRead(false);
                    }
                    return;

                case ConversationList.HAVE_LOCKED_MESSAGES_TOKEN:
                    long threadId = (Long)cookie;
                    ConversationList.confirmDeleteThreadDialog(
                            new ConversationList.DeleteThreadListener(threadId,
                                mBackgroundQueryHandler, ComposeMessageActivity.this),
                            threadId == -1,
                            cursor != null && cursor.getCount() > 0,
                            ComposeMessageActivity.this);
                    break;
            }
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            switch(token) {
            case DELETE_MESSAGE_TOKEN:
                Xlog.d(TAG, "onDeleteComplete(): before update mConversation, ThreadId = " + mConversation.getThreadId());
                mConversation = Conversation.upDateThread(ComposeMessageActivity.this, mConversation.getThreadId(), false);
                mThreadCountManager.isFull(mThreadId, ComposeMessageActivity.this, 
                        ThreadCountManager.OP_FLAG_DECREASE);
                // Update the notification for new messages since they
                // may be deleted.
                MessagingNotification.nonBlockingUpdateNewMessageIndicator(
                        ComposeMessageActivity.this, false, false);
                // Update the notification for failed messages since they
                // may be deleted.
                updateSendFailedNotification();
                MessagingNotification.updateDownloadFailedNotification(ComposeMessageActivity.this);
                Xlog.d(TAG, "onDeleteComplete(): MessageCount = " + mConversation.getMessageCount() + 
                        ", ThreadId = " + mConversation.getThreadId());
                if (mConversation.getMessageCount() <= 0 || mConversation.getThreadId() <= 0l) {
                    finish();
                }
                if (progress()) {
                    dismissProgressDialog();
                }
                break;
            case ConversationList.DELETE_CONVERSATION_TOKEN:
                try {
                    ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                    if(phone != null) {
                        if(phone.isTestIccCard()) {
                            Xlog.d(TAG, "All messages has been deleted, send notification...");
                            SmsManager.getDefault().setSmsMemoryStatus(true);
                        }
                    } else {
                        Xlog.d(TAG, "Telephony service is not available!");
                    }
                } catch(Exception ex) {
                    Xlog.e(TAG, "" + ex.getMessage());
                }
                // Update the notification for new messages since they
                // may be deleted.
                MessagingNotification.nonBlockingUpdateNewMessageIndicator(
                        ComposeMessageActivity.this, false, false);
                // Update the notification for failed messages since they
                // may be deleted.
                updateSendFailedNotification();
                MessagingNotification.updateDownloadFailedNotification(ComposeMessageActivity.this);
                if (mMsgListAdapter.mIsDeleteMode) {
                    changeDeleteMode();
                }
                if (progress()) {
                    dismissProgressDialog();
                } 
                break;
            }

            // If we're deleting the whole conversation, throw away
            // our current working message and bail.
            if (token == ConversationList.DELETE_CONVERSATION_TOKEN) {
                mWorkingMessage.discard();
                Conversation.init(ComposeMessageActivity.this);
                finish();
            }
        }
    }

    private void showQuickTextDialog() {
        if (mQuickTextDialog == null) {
            List<String> quickTextsList = new ArrayList<String>();

            // add user's quick text
            Cursor cursor = getContentResolver().query(MmsSms.CONTENT_URI_QUICKTEXT, null, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    quickTextsList.add(cursor.getString(1));
                }
                cursor.close();
            }

            // add default quick text
            String[] default_quick_texts = getResources().getStringArray(R.array.default_quick_texts);
            for (int i = 0; i < default_quick_texts.length; i++) {
                quickTextsList.add(default_quick_texts[i]);
            }

            List<Map<String, ?>> entries = new ArrayList<Map<String, ?>>();
            for (String text : quickTextsList) {
                HashMap<String, Object> entry = new HashMap<String, Object>();
                entry.put("text", text);
                entries.add(entry);
            }

            final SimpleAdapter qtAdapter = new SimpleAdapter( this, entries, R.layout.quick_text_list_item, 
                    new String[] {"text"}, new int[] {R.id.quick_text});
            
            AlertDialog.Builder qtBuilder = new AlertDialog.Builder(this);

            qtBuilder.setTitle(getString(R.string.select_quick_text));
            qtBuilder.setCancelable(true);
            qtBuilder.setAdapter(qtAdapter, new DialogInterface.OnClickListener() {
                @SuppressWarnings("unchecked")
                public final void onClick(DialogInterface dialog, int which) {
                    HashMap<String, Object> item = (HashMap<String, Object>) qtAdapter.getItem(which);
                    if (mSubjectTextEditor != null && mSubjectTextEditor.isFocused()){
                        insertText(mSubjectTextEditor, (String)item.get("text"));
                    } else {
                        insertText(mTextEditor, (String)item.get("text"));
                    }
                    dialog.dismiss();
                }
            });
            mQuickTextDialog = qtBuilder.create();
        }
        mQuickTextDialog.show();
    }

    private void showSmileyDialog() {
        if (mSmileyDialog == null) {
            int[] icons = SmileyParser.DEFAULT_SMILEY_RES_IDS;
            String[] names = getResources().getStringArray(
                    SmileyParser.DEFAULT_SMILEY_NAMES);
            final String[] texts = getResources().getStringArray(
                    SmileyParser.DEFAULT_SMILEY_TEXTS);

            final int N = names.length;

            List<Map<String, ?>> entries = new ArrayList<Map<String, ?>>();
            boolean added = false;
            for (int i = 0; i < N; i++) {
                // We might have different ASCII for the same icon, skip it if
                // the icon is already added.
                for (int j = 0; j < i; j++) {
                    if (icons[i] == icons[j]) {
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    HashMap<String, Object> entry = new HashMap<String, Object>();

                    entry.put("icon", icons[i]);
                    entry.put("name", names[i]);
                    entry.put("text", texts[i]);

                    entries.add(entry);
                    added = false;
                }
            }

            final SimpleAdapter a = new SimpleAdapter(
                    this,
                    entries,
                    R.layout.smiley_menu_item,
                    new String[] {"icon", "name", "text"},
                    new int[] {R.id.smiley_icon, R.id.smiley_name, R.id.smiley_text});
            SimpleAdapter.ViewBinder viewBinder = new SimpleAdapter.ViewBinder() {
                public boolean setViewValue(View view, Object data, String textRepresentation) {
                    if (view instanceof ImageView) {
                        Drawable img = getResources().getDrawable((Integer)data);
                        ((ImageView)view).setImageDrawable(img);
                        return true;
                    }
                    return false;
                }
            };
            a.setViewBinder(viewBinder);

            AlertDialog.Builder b = new AlertDialog.Builder(this);

            b.setTitle(getString(R.string.menu_insert_smiley));

            b.setCancelable(true);
            b.setAdapter(a, new DialogInterface.OnClickListener() {
                @SuppressWarnings("unchecked")
                public final void onClick(DialogInterface dialog, int which) {
                    HashMap<String, Object> item = (HashMap<String, Object>) a.getItem(which);
                    if (mSubjectTextEditor != null && mSubjectTextEditor.isFocused()){
                        insertText(mSubjectTextEditor, (String)item.get("text"));
                    }else{
                        insertText(mTextEditor, (String)item.get("text"));
                    }
                    dialog.dismiss();
                }
            });
            mSmileyDialog = b.create();
        }
        mSmileyDialog.show();
    }

    private void insertText(EditText edit, String insertText){
        int where = edit.getSelectionStart();

        if (where == -1) {
            edit.append(insertText);
        } else {
            edit.getText().insert(where, insertText);
        }
    }

    public void onUpdate(final Contact updated) {
        // Using an existing handler for the post, rather than conjuring up a new one.
    }

    private void addRecipientsListeners() {
        Contact.addListener(this);
    }

    private void removeRecipientsListeners() {
        Contact.removeListener(this);
    }

    private boolean isCompleteRecipient(String text) {
        if (text == null || TextUtils.isEmpty(text)) {
            return false;
        }
        String lastChar = text.substring(text.length() - 1);
        if (lastChar.equals(",") || lastChar.equals(";")) {
            return true;
        }
        return false;
    }

    public static Intent createIntent(Context context, long threadId) {
        Intent intent = new Intent(context, ComposeMessageActivity.class);

        if (threadId > 0) {
            intent.setData(Conversation.getUri(threadId));
        }

        return intent;
    }

    private String getBody(Uri uri) {
        if (uri == null) {
            return null;
        }
        String urlStr = uri.getSchemeSpecificPart();
        if (!urlStr.contains("?")) {
            return null;
        }
        urlStr = urlStr.substring(urlStr.indexOf('?') + 1);
        String[] params = urlStr.split("&");
        for (String p : params) {
            if (p.startsWith("body=")) {
                try {
                    return URLDecoder.decode(p.substring(5), "UTF-8");
                } catch (UnsupportedEncodingException e) { }
            }
        }
        return null;
    }

    /**
     * This filter will constrain edits not to make the length of the text
     * greater than the specified length ( eg. 40 Bytes).
     */  
    class MyLengthFilter implements InputFilter {
        public MyLengthFilter(int max) {
            mMax = max;
        }

        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            mMax = DEFAULT_LENGTH;
            if ((dest.toString() + source.subSequence(start, end)).getBytes().length > 
                  (dest.toString() + source.subSequence(start, end)).length()) {
                mMax = 13;
            }
            int keep = mMax - (dest.length() - (dend - dstart));

            if (keep <= 0) {
                return "";
            } else if (keep >= end - start) {
                return null; // keep original
            } else {
                return source.subSequence(start, start + keep);
            }
        }

        private int mMax;
    }

    private void hideInputMethod() {
        InputMethodManager inputMethodManager =
            (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if(this.getWindow()!=null && this.getWindow().getCurrentFocus()!=null){
            inputMethodManager.hideSoftInputFromWindow(this.getWindow().getCurrentFocus().getWindowToken(), 0);
        }
    }

    // toast there are too many recipients.
    private void toastTooManyRecipients(int recipientCount) {
        String tooManyRecipients = getString(R.string.too_many_recipients, recipientCount, RECIPIENTS_LIMIT_FOR_SMS);
        Toast.makeText(ComposeMessageActivity.this, tooManyRecipients, Toast.LENGTH_LONG).show();
    }

    private void addContacts(int pickCount) {
        Intent intent = new Intent("android.intent.action.CONTACTSMULTICHOICE");
        intent.setType(Phone.CONTENT_ITEM_TYPE);
        intent.putExtra("request_email", true);

        intent.putExtra("pick_count", pickCount);
        misPickContatct = true;
        startActivityForResult(intent, REQUEST_CODE_PICK_CONTACT);
    }

    private class TextVCardContact {
        protected String name = "";
        protected List<String> numbers = new ArrayList<String>();
        protected List<String> emails = new ArrayList<String>();
        protected List<String> organizations = new ArrayList<String>();

        protected void reset() {
            name = "";
            numbers.clear();
            emails.clear();
            organizations.clear();
        }
        @Override
        public String toString() {
            String textVCardString = "";
            int i = 1;
            if (name != null && !name.equals("")) {
                textVCardString += getString(R.string.contact_name) + ": " + name + "\n";
            }
            if (!numbers.isEmpty()) {
                if (numbers.size() > 1) {
                    i = 1;
                    for (String number : numbers) {
                        textVCardString += getString(R.string.contact_tel) + i + ": " + number + "\n";
                        i++;
                    }
                } else {
                    textVCardString += getString(R.string.contact_tel) + ": " + numbers.get(0) + "\n";
                }
            }
            if (!emails.isEmpty()) {
                if (emails.size() > 1) {
                    i = 1;
                    for (String email : emails) {
                        textVCardString += getString(R.string.contact_email) + i + ": " + email + "\n";
                        i++;
                    }
                } else {
                    textVCardString += getString(R.string.contact_email) + ": " + emails.get(0) + "\n";
                }
            }
            if (!organizations.isEmpty()) {
                if (organizations.size() > 1) {
                    i = 1;
                    for (String organization : organizations) {
                        textVCardString += getString(R.string.contact_organization) + i + ": " + organization + "\n";
                        i++;
                    }
                } else {
                    textVCardString += getString(R.string.contact_organization) + ": " + organizations.get(0) + "\n";
                }
            }
            return textVCardString;
        }
    }

    // create the String of vCard via Contacts message
    private String getVCardString(Cursor cursor, String textVCard) {
        final int dataContactId     = 0;
        final int dataMimeType      = 1;
        final int dataString        = 2;
        long contactId = 0l;
        long contactCurrentId = 0l;
        int i = 1;
        String mimeType;
        TextVCardContact tvc = new TextVCardContact();
        int j = 0;
        while (cursor.moveToNext()) {
            contactId = cursor.getLong(dataContactId);
            mimeType = cursor.getString(dataMimeType);
            if (contactCurrentId == 0l) {
                contactCurrentId = contactId;
            }

            // put one contact information into textVCard string
            if (contactId != contactCurrentId) {
                contactCurrentId = contactId;
                textVCard += tvc.toString();
                tvc.reset();
            }

            // get cursor data
            if (CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
                tvc.name = cursor.getString(dataString);
            }
            if (CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                tvc.numbers.add(cursor.getString(dataString));
            }
            if (CommonDataKinds.Email.CONTENT_ITEM_TYPE.equals(mimeType)) {
                tvc.emails.add(cursor.getString(dataString));
            }
            if (CommonDataKinds.Organization.CONTENT_ITEM_TYPE.equals(mimeType)) {
                tvc.organizations.add(cursor.getString(dataString));
            }
            // put the last one contact information into textVCard string
            if (cursor.isLast()) {
                textVCard += tvc.toString();
            }
            j++;
            if (j%10 == 0) {
                if (textVCard.length() > MmsConfig.getMaxTextLimit()) {
                    break;
                }
            }
        }
        Xlog.i(TAG, "compose.getVCardString():return string = " + textVCard);
        return textVCard;
    }

    private int getContactSIM(String number) {
        int simId = -1;
        List simIdList = new ArrayList<Integer>();
        Cursor associateSIMCursor = ComposeMessageActivity.this.getContentResolver().query(
                Data.CONTENT_URI, 
                new String[]{Data.SIM_ID}, 
                Data.MIMETYPE + "='" + CommonDataKinds.Phone.CONTENT_ITEM_TYPE 
                    + "' AND (" + Data.DATA1 + "='" + number + "') AND (" + Data.SIM_ID + "!= -1)", 
                null,
                null
        );

        if (null == associateSIMCursor) {
            Xlog.i(TAG, TAG + " queryContactInfo : associateSIMCursor is null");
        } else {
            Xlog.i(TAG, TAG + " queryContactInfo : associateSIMCursor is not null. Count[" + 
                    associateSIMCursor.getCount() + "]");
        }

        if ((null != associateSIMCursor) && (associateSIMCursor.getCount() > 0)) {
            associateSIMCursor.moveToFirst();
            // Get only one record is OK
            simId = (Integer) associateSIMCursor.getInt(0);
        } else {
            simId = -1;
        }
        associateSIMCursor.close();
        return simId;
    }
    
    private void getSimInfoList() {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mSimInfoList = SIMInfo.getInsertedSIMList(this);
            mSimCount = mSimInfoList.isEmpty()? 0: mSimInfoList.size();
            Xlog.v(TAG, "ComposeMessageActivity.getSimInfoList(): mSimCount = " + mSimCount);
        } else { // single SIM
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            if (phone != null) {
                try {
                    mSimCount = phone.isSimInsert(0) ? 1: 0;
                } catch (RemoteException e) {
                    Xlog.e(MmsApp.TXN_TAG, "check sim insert status failed");
                    mSimCount = 0;
                }
            }
        }
    }

    private void checkRecipientsCount() {
        if (isRecipientsEditorVisible()) {
            mRecipientsEditor.structLastRecipient();
        }
        hideInputMethod();
        final int mmsLimitCount = MmsConfig.getMmsRecipientLimit();
        if (mWorkingMessage.requiresMms() && recipientCount() > mmsLimitCount) {
            String message = getString(R.string.max_recipients_message, mmsLimitCount);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.max_recipients_title);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setCancelable(true);
            builder.setMessage(message);
            builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            /*
                             * If entering an existing thread, #mRecipientsEditor never gets initialized.
                             * So, when mRecipientsEditor is not visible, it might be null.
                             */
                            List<String> recipientsList;
                            if (isRecipientsEditorVisible()) {
                                recipientsList = mRecipientsEditor.getNumbers();
                            } else {
                                recipientsList = new ArrayList<String>(Arrays.asList(getRecipients().getNumbers()));
                            }
                            List<String> newRecipientsList = new ArrayList<String>();

                            if (recipientCount() > mmsLimitCount * 2) {
                                for (int i = 0; i < mmsLimitCount; i++) {
                                    newRecipientsList.add(recipientsList.get(i));
                                }
                                mWorkingMessage.setWorkingRecipients(newRecipientsList);
                            } else {
                                for (int i = recipientCount() - 1; i >= mmsLimitCount; i--) {
                                    recipientsList.remove(i);
                                }
                                mWorkingMessage.setWorkingRecipients(recipientsList);
                            }
                            simSelection();
                        }
                    });
                }
            });
            builder.setNegativeButton(R.string.no, null);
            builder.show();
            updateSendButtonState();
        } else {
            /*
             * fix CR ALPS00069541
             * if the message copy from sim card with unknown recipient
             * the recipient will be ""
             */
            if (isRecipientsEditorVisible() && "".equals(mRecipientsEditor.allNumberToString().replaceAll(";", ""))) {
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.cannot_send_message)
                        .setMessage(R.string.cannot_send_message_reason)
                        .setPositiveButton(R.string.yes, new CancelSendingListener())
                        .show();
            } else if (!isRecipientsEditorVisible() && "".equals(mConversation.getRecipients().serialize().replaceAll(";", ""))) {
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.cannot_send_message)
                        .setMessage(R.string.cannot_send_message_reason)
                        .setPositiveButton(R.string.yes, new CancelSendingListener())
                        .show();
            } else {
                simSelection();
            }
        }
    }
    
    private void simSelection() {
        if (FeatureOption.MTK_GEMINI_SUPPORT == false) {
            confirmSendMessageIfNeeded();
        } else if (mSimCount == 0) {
            // SendButton can't click in this case
        } else if (mSimCount == 1) {
            mSelectedSimId = (int) mSimInfoList.get(0).mSimId;
            confirmSendMessageIfNeeded();
        } else if (mSimCount > 1) {
            Intent intent = new Intent();
            intent.putExtra(SELECT_TYPE, SIM_SELECT_FOR_SEND_MSG);
            // getContactSIM
            if (getRecipients().size() == 1/*isOnlyOneRecipient()*/) {
                mAssociatedSimId = getContactSIM(getRecipients().get(0).getNumber()); // 152188888888 is a contact number
            } else {
                mAssociatedSimId = -1;
            }

            // getDefaultSIM()
            mMessageSimId = Settings.System.getLong(getContentResolver(), Settings.System.SMS_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
            if (mMessageSimId == Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK) {
                // always ask, show SIM selection dialog
                showSimSelectedDialog(intent);
                updateSendButtonState();
            } else if (mMessageSimId == Settings.System.DEFAULT_SIM_NOT_SET) {
                /*
                 * not set default SIM: 
                 * if recipients are morn than 2,or there is no associated SIM,
                 * show SIM selection dialog
                 * else send message via associated SIM
                 */
                if (mAssociatedSimId == -1) {
                    showSimSelectedDialog(intent);
                    updateSendButtonState();
                } else {
                    mSelectedSimId = mAssociatedSimId;
                    confirmSendMessageIfNeeded();
                }
            } else {
                /*
                 * default SIM:
                 * if recipients are morn than 2,or there is no associated SIM,
                 * send message via default SIM
                 * else show SIM selection dialog
                 */
                if (mAssociatedSimId == -1 || (mMessageSimId == mAssociatedSimId)) {
                    mSelectedSimId = (int) mMessageSimId;
                    confirmSendMessageIfNeeded();
                } else {
                    showSimSelectedDialog(intent);
                    updateSendButtonState();
                }
            }
        }
    }
    
    private void showSimSelectedDialog(Intent intent) {
        // TODO get default SIM and get contact SIM
        final Intent it = intent;
        List<Map<String, ?>> entries = new ArrayList<Map<String, ?>>();
        for (int i = 0; i < mSimCount; i++) {
            SIMInfo simInfo = mSimInfoList.get(i);
            HashMap<String, Object> entry = new HashMap<String, Object>();

            entry.put("simIcon", simInfo.mSimBackgroundRes);
            int state = MessageUtils.getSimStatus(i, mSimInfoList, TelephonyManagerEx.getDefault());
            entry.put("simStatus", MessageUtils.getSimStatusResource(state));
            String simNumber = "";
            if (!TextUtils.isEmpty(simInfo.mNumber)) {
                switch(simInfo.mDispalyNumberFormat) {
                    //case android.provider.Telephony.SimInfo.DISPLAY_NUMBER_DEFAULT:
                    case android.provider.Telephony.SimInfo.DISPLAY_NUMBER_FIRST:
                        if(simInfo.mNumber.length() <= 4)
                            simNumber = simInfo.mNumber;
                        else
                            simNumber = simInfo.mNumber.substring(0, 4);
                        break;
                    case android.provider.Telephony.SimInfo.DISPLAY_NUMBER_LAST:
                        if(simInfo.mNumber.length() <= 4)
                            simNumber = simInfo.mNumber;
                        else
                            simNumber = simInfo.mNumber.substring(simInfo.mNumber.length() - 4);
                        break;
                    case 0://android.provider.Telephony.SimInfo.DISPLAY_NUMBER_NONE:
                        simNumber = "";
                        break;
                }
            }
            if (!TextUtils.isEmpty(simNumber)) {
                entry.put("simNumberShort",simNumber);
            } else {
                entry.put("simNumberShort", "");
            }

            entry.put("simName", simInfo.mDisplayName);
            if (!TextUtils.isEmpty(simInfo.mNumber)) {
                entry.put("simNumber", simInfo.mNumber);
            } else {
                entry.put("simNumber", "");
            }
            if (mAssociatedSimId == (int) simInfo.mSimId) {
                // if this SIM is contact SIM, set "Suggested"
                entry.put("suggested", getString(R.string.suggested));
            } else {
                entry.put("suggested", "");// not suggested
            }
            if (!MessageUtils.is3G(i, mSimInfoList)) {
                entry.put("sim3g", "");
            } else {
                String optr = SystemProperties.get("ro.operator.optr");
                //MTK_OP02_PROTECT_START
                if ("OP02".equals(optr)) {
                    entry.put("sim3g", "3G");
                } else 
                //MTK_OP02_PROTECT_END
                {
                    entry.put("sim3g", "");
                }
            }
            entries.add(entry);
        }

        final SimpleAdapter a = new SimpleAdapter(
                this,
                entries,
                R.layout.sim_selector,
                new String[] {"simIcon", "simStatus", "simNumberShort", "simName", "simNumber", "suggested", "sim3g"},
                new int[] {R.id.sim_icon, R.id.sim_status, R.id.sim_number_short, 
                        R.id.sim_name, R.id.sim_number, R.id.sim_suggested, R.id.sim3g});
        SimpleAdapter.ViewBinder viewBinder = new SimpleAdapter.ViewBinder() {
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                if (view instanceof ImageView) {
                    if (view.getId() == R.id.sim_icon) {
                        ImageView simicon = (ImageView) view.findViewById(R.id.sim_icon);
                        simicon.setBackgroundResource((Integer) data);
                    } else if (view.getId() == R.id.sim_status) {
                        ImageView simstatus = (ImageView)view.findViewById(R.id.sim_status);
                        if ((Integer)data != com.android.internal.telephony.Phone.SIM_INDICATOR_UNKNOWN
                                && (Integer)data != com.android.internal.telephony.Phone.SIM_INDICATOR_NORMAL) {
                            simstatus.setVisibility(View.VISIBLE);
                            simstatus.setImageResource((Integer)data);
                        } else {
                            simstatus.setVisibility(View.GONE);
                        }
                    }
                    return true;
                }
                return false;
            }
        };
        a.setViewBinder(viewBinder);
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(getString(R.string.sim_selected_dialog_title));
        b.setCancelable(true);
        b.setAdapter(a, new DialogInterface.OnClickListener() {
            @SuppressWarnings("unchecked")
            public final void onClick(DialogInterface dialog, int which) {
                updateSendButtonState(false);
                mSelectedSimId = (int) mSimInfoList.get(which).mSimId;
                if (it.getIntExtra(SELECT_TYPE, -1) == SIM_SELECT_FOR_SEND_MSG) {
                    confirmSendMessageIfNeeded();
                } else if (it.getIntExtra(SELECT_TYPE, -1) == SIM_SELECT_FOR_SAVE_MSG_TO_SIM) {
                    //getMessageAndSaveToSim(it);
                    Message msg = mSaveMsgHandler.obtainMessage(MSG_SAVE_MESSAGE_TO_SIM_AFTER_SELECT_SIM);
                    msg.obj = it;
                    //mSaveMsgHandler.sendMessageDelayed(msg, 60);
                    mSaveMsgHandler.sendMessage(msg);
                }
                dialog.dismiss();
            }
        });
        mSIMSelectDialog = b.create();
        mSIMSelectDialog.show();
    }
    
    //add for multi-delete  
    private void confirmMultiDelete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_dialog_title);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setCancelable(true);
        builder.setMessage(R.string.confirm_delete_selected_messages);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mBackgroundQueryHandler.setProgressDialog(DeleteProgressDialogUtil.getProgressDialog(ComposeMessageActivity.this));
 	    		mBackgroundQueryHandler.showProgressDialog();
                new Thread(new Runnable() {
                    public void run() {
                        Iterator iter = mMsgListAdapter.getItemList().entrySet().iterator();
                        Uri deleteSmsUri = null;
                        Uri deleteMmsUri = null;
                        String[] argsSms = new String[mMsgListAdapter.getSelectedNumber()];
                        String[] argsMms = new String[mMsgListAdapter.getSelectedNumber()];
                        int i = 0;
                        int j = 0;
                        while (iter.hasNext()) {
                            @SuppressWarnings("unchecked")
                            Map.Entry<Long, Boolean> entry = (Entry<Long, Boolean>) iter.next();
                            if (entry.getValue()) {
                                if (entry.getKey() > 0){
                                    Xlog.i(TAG, "sms");		
                                    argsSms[i] = Long.toString(entry.getKey());
                                    Xlog.i(TAG, "argsSms[i]" + argsSms[i]);
                                    //deleteSmsUri = ContentUris.withAppendedId(Sms.CONTENT_URI, entry.getKey());
                                    deleteSmsUri = Sms.CONTENT_URI;
                                    i++;
                                } else {
                                    Xlog.i(TAG, "mms");  				
                                    argsMms[j] = Long.toString(-entry.getKey());
                                    Xlog.i(TAG, "argsMms[j]" + argsMms[j]);
                                    //deleteMmsUri = ContentUris.withAppendedId(Mms.CONTENT_URI, -entry.getKey());
                                    deleteMmsUri = Mms.CONTENT_URI;
                                    j++;
                                }
                            }
                        }
                        mBackgroundQueryHandler.setMax(
                                (deleteSmsUri != null ? 1 : 0) +
                                (deleteMmsUri != null ? 1 : 0));
                        if (deleteSmsUri != null) {
                            mBackgroundQueryHandler.startDelete(DELETE_MESSAGE_TOKEN,
                                    null, deleteSmsUri, FOR_MULTIDELETE, argsSms);
                        }
                        if (deleteMmsUri != null) {
                            mBackgroundQueryHandler.startDelete(DELETE_MESSAGE_TOKEN,
                                    null, deleteMmsUri, FOR_MULTIDELETE, argsMms);
                        }
                        mMsgListAdapter.mIsDeleteMode = false;
                        runOnUiThread(new Runnable() {
                            public void run() {
                                drawTopPanel();
                                drawBottomPanel();
                            }
                        });
                    }
                }).start();
            }
        });
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }

    private String getNumberByContactName(String contactName) {
        String number = contactName;
        long contactId = -1L;
        Uri dataUri = ContactsContract.Contacts.CONTENT_URI;
        Xlog.i(TAG, "compose.getNumberByContactName(): dataUri =  " + dataUri.toString());
        Cursor cursor = getContentResolver().query(dataUri,
                new String[] {Contacts._ID},
                Contacts.DISPLAY_NAME + "=?",
                new String[] {contactName},
                null);
        if (cursor != null && cursor.moveToFirst()) {
            contactId = cursor.getLong(0);
            Xlog.i(TAG, "compose.getNumberByContactName(): cursor.contactId =  " + contactId);
            number = queryPhoneNumbers(contactId, contactName);
        }
        if (cursor != null) {
            cursor.close();
        }

        Xlog.v(TAG, "getNumberByContactName: return number = " + number);
        return number;
    }

    private String queryPhoneNumbers(long contactId, String contactName) {
        Xlog.i(TAG, "compose.queryPhoneNumbers(" + contactId + ", " + contactName + ")");
        Uri baseUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        Uri dataUri = Uri.withAppendedPath(baseUri, Contacts.Data.CONTENT_DIRECTORY);
         String number = contactName;
        // mtk80909 modified for ALPS00023212
        Cursor c = getContentResolver().query(dataUri, new String[] {Phone.NUMBER},
                Data.MIMETYPE + "=?", new String[] {Phone.CONTENT_ITEM_TYPE}, null);
        if (c != null && c.moveToFirst()) {
            number = c.getString(0);
            Xlog.i(TAG, "compose.queryPhoneNumbers(): number = " + number);
        }
        if (c != null) {
            c.close();
        }
        Xlog.e(TAG, "compose.queryPhoneNumbers(): number = " + number);
        return number;
    }

    // add for RecipientsEditor
    public void onAddRecipient(int recipIndex, CharSequence recipName, boolean flag) {
        List<String> numberList = mRecipientsEditor.getNumbers();
        if (flag) {
            if (numberList.size() >= RECIPIENTS_LIMIT_FOR_SMS) {
                // TODO nater:need delete more recipient
                toastTooManyRecipients(numberList.size() + 1);
                return;
            }

            int numberListSize = numberList.size();
            String number = getNumberByContactName(recipName.toString());
            Xlog.v(TAG, "onAddRecipient: Mms.isEmailAddress(number) = " + Mms.isEmailAddress(number));
            if (Mms.isEmailAddress(number)) {
                mWorkingMessage.setHasEmail(true, !mWorkingMessage.requiresMms());
            }
            if (recipIndex > (numberListSize - 1)) {
                numberList.add(number);
            } else {
                numberList.add(numberList.get(numberListSize - 1));
                for (int i = numberListSize - 1; i > recipIndex; i--) {
                    String num = numberList.get(i - 1);
                    numberList.set(i, num);
                }
                numberList.set(recipIndex, number);
            }
            mRecipientsEditor.setNumbers(numberList);
        } else {
            mWorkingMessage.setHasEmail(mRecipientsEditor.containsEmail(), !mWorkingMessage.requiresMms());
        }
        Xlog.d(TAG, "onAddRecipient() : numberList = " + mRecipientsEditor.allNumberToString());
        mWorkingMessage.setWorkingRecipients(numberList);
    }

    public void onDeleteRecipient(int recipIndex, CharSequence recipName) {
        Xlog.v(TAG, "onDeleteRecipient: index = " + recipIndex + ",recipName = " + recipName);
        List<String> numberList = mRecipientsEditor.getNumbers();
        if (numberList.size() > recipIndex) {
            numberList.remove(recipIndex);
        }
        Xlog.v(TAG, "ComposeMessage.onDeleteRecipient(..) : setNumbers! size=" + numberList.size());
        mRecipientsEditor.setNumbers(numberList);
        mWorkingMessage.setWorkingRecipients(numberList);
        boolean hasEmail = mRecipientsEditor.containsEmail();
        mWorkingMessage.setHasEmail(hasEmail, hasEmail != mWorkingMessage.requiresMms());
    }

    public void onRecipientsFull() {
        toastTooManyRecipients(mRecipientsEditor.getNumbers().size() + 1);
    }

    public static Activity getComposeContext() {
        return sCompose;
    }
	
    @Override
    public void saveDraftForShutDown() {
        saveDraft();
    }

    /**
     * Remove the number which is the same as any one before;
     * When the count of recipients over the limit, make a toast and remove the recipients over the limit.
     * @param recipientsString the numbers slipt by ','.
     * @return recipientsString the numbers slipt by ',' after modified.
     */
    private String getStringForMultipleRecipients(String recipientsString) {
        recipientsString = recipientsString.replaceAll(",", ";");
        String[] recipients_all = recipientsString.split(";");
        List<String> recipientsList = new ArrayList<String>();
        for (String recipient : recipients_all) {
            recipientsList.add(recipient);
        }

        Set<String> recipientsSet = new HashSet<String>();
        recipientsSet.addAll(recipientsList);

        if (recipientsSet.size() > RECIPIENTS_LIMIT_FOR_SMS) {
            toastTooManyRecipients(recipients_all.length);
        }

        recipientsList.clear();
        recipientsList.addAll(recipientsSet);

        recipientsString = "";
        int count = recipientsList.size() > RECIPIENTS_LIMIT_FOR_SMS ? RECIPIENTS_LIMIT_FOR_SMS : recipientsList.size();
        for(int i = 0; i < count; i++) {
            if (i == (count - 1)) {
                recipientsString += recipientsList.get(i);
            } else {
                recipientsString += recipientsList.get(i) + ";";
            }
        }
        return recipientsString;
    }
}
