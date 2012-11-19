package com.android.mms.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.os.Bundle;
import static android.provider.BaseColumns._ID;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.text.InputFilter;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.android.mms.R;

public class SmsTemplateEditActivity extends Activity {
	
    private static String TAG = "MMS/SmsTempalteEditor";
    private static String TEXT = "text";
    private static int QUICK_TEXT_HAS_ALREADY = -1;
    private static int QUICK_TEXT_NULL = -2;

    private Integer mQuickTextId;
    private Integer mMaxQuickTextId;
    private String mQuickText;
    
    @SuppressWarnings("unused")
	private AlertDialog mQuicktextAlertDialog;
    private ListView mListView;
    private Button addButton;
    private EditText mNewQuickText;
    private EditText mOldQuickText;
    @SuppressWarnings("unused")
	private TextView textItem;
    
    private Cursor cursor;
    private ArrayAdapter<String> adapter;
    private List<Integer> allQuickTextIds = new ArrayList<Integer>();
    private List<String> allQuickTexts = new ArrayList<String>();
    private final int MAX_EDITABLE_LENGTH = 128;
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sms_template_edit_activity);
        
        addButton = (Button) findViewById(R.id.quickText_add_button);
        addButton.setOnClickListener(new android.view.View.OnClickListener() {
			public void onClick(View v) {
				switch (v.getId()) {
                case R.id.quickText_add_button:
                    addQuickText();
                    break;
                default:
                	break;
                }
			}
        });
        
        mListView = (ListView) findViewById(R.id.quick_text_list);
        mListView.setOnItemClickListener(new OnItemClickListener () {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				InputMethodManager inputM =
                    (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                if(getWindow() != null && getWindow().getCurrentFocus() != null) {
                	inputM.hideSoftInputFromWindow(getWindow().getCurrentFocus().getWindowToken(), 0);
                }
				mQuickTextId = allQuickTextIds.get(new Integer((int) id));
				mQuickText = allQuickTexts.get(new Integer((int) id));
				showEditDialog();
				return;
			}	
        });
        mNewQuickText = (EditText) findViewById(R.id.new_quick_text);
        mNewQuickText.setHint(R.string.type_to_compose_text_enter_to_send);
        textItem = (TextView) findViewById(R.id.new_quick_text);

        updateAllQuicktexts();
    }
    
    private void showEditDialog() {
		mQuicktextAlertDialog = new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(R.string.quick_text_editor)
        .setMessage(mQuickText)
        .setPositiveButton(R.string.edit, new EditButtonListener())
        .setNeutralButton(R.string.delete, new DeleteButtonListener())
        .setNegativeButton(android.R.string.cancel, new CancelButtonListener())
        .show();
	}
    
    // 4ButtonListener
    private class EditButtonListener implements OnClickListener {
		public void onClick(DialogInterface dialog, int which) {
			dialog.dismiss();
			showEditDialog(mQuickText);
		}
    }
    
    private class UpdateButtonListener implements OnClickListener {
		public void onClick(DialogInterface dialog, int which) {
			String newQuicktext = mOldQuickText.getText().toString();
			int i = updateST(mQuickTextId, newQuicktext);
			dialog.dismiss();
			if (i > 0) {
				mQuickTextId = null;
				updateAllQuicktexts();
				makeAToast(R.string.modify_successful);
			} else if (i == QUICK_TEXT_HAS_ALREADY) {
				makeAToast(R.string.already_have_quick_text);
			} else if (i == QUICK_TEXT_NULL) {
				makeAToast(R.string.cannot_save_message);
				showEditDialog(mQuickText);
			} else {
				makeAToast(R.string.modify_unsuccessful);
			}
		}
    }
    
    private class DeleteButtonListener implements OnClickListener {
		public void onClick(DialogInterface dialog, int which) {
			int i = delST(mQuickTextId);
			updateAllQuicktexts();
			dialog.dismiss();
			if (i > 0) {
				mQuickTextId = null;
				makeAToast(R.string.delete_successful);
			} else {
				makeAToast(R.string.delete_unsuccessful);
			}
		}
    }
    
    private class CancelButtonListener implements OnClickListener {
		public void onClick(DialogInterface dialog, int which) {
			dialog.dismiss();
		}
    }
    
    private void addQuickText() {
    	String currentText = mNewQuickText.getText().toString();
        if (currentText.length() != 0) {
        	if (addST(currentText)) {
        		mNewQuickText.setText("");
                updateAllQuicktexts();
                makeAToast(getString(R.string.add_quick_text_successful) + " : \n" + currentText);
        	} else {
        		makeAToast(R.string.already_have_quick_text);
        	}
        } 
        return;
	}

	private boolean addST(String str) {
        // Insert a new record into the Events data source.
        // You would do something similar for delete and update.
    	if (hasQuicktext(str)) {
    		return false;
    	}
        ContentValues values = new ContentValues();
        values.put(_ID, mMaxQuickTextId + 1);
        values.put(TEXT, str);
        getContentResolver().insert(MmsSms.CONTENT_URI_QUICKTEXT, values);
        return true;
    }

	private int delST(Integer id) {
        return getContentResolver().delete(MmsSms.CONTENT_URI_QUICKTEXT, _ID + "=" + id, null);
    }
    
    private int updateST(Integer id, String text) {
    	if (text.trim().equals("")) {
    		return QUICK_TEXT_NULL;
    	} else if (hasQuicktext(text)) {
    		return QUICK_TEXT_HAS_ALREADY;
    	}
        ContentValues values = new ContentValues();
        values.put(_ID, id);
        values.put(TEXT, text);
        return getContentResolver().update(MmsSms.CONTENT_URI_QUICKTEXT, values, _ID + "=" + id, null);
    }

    private Cursor getSTs() {
        // Perform a managed query. The Activity will handle closing
        // and re-querying the cursor when needed.
    	Cursor cursor = getContentResolver().query(MmsSms.CONTENT_URI_QUICKTEXT, null, null, null, null);
        startManagingCursor(cursor);
        return cursor;
    }
    
    
    private boolean hasQuicktext(String str) {
    	for (String s : allQuickTexts) {
    		if (s.equals(str)) {
    			return true;
    		}
    	}
		return false;
	}
    
    private void updateAllQuicktexts() {
    	mMaxQuickTextId = 1;
    	cursor = getSTs();
    	allQuickTextIds.clear();
    	allQuickTexts.clear();
        if (cursor != null) {
        	while (cursor.moveToNext()) {
        		int qtId = cursor.getInt(0);
        		allQuickTextIds.add(qtId);
        		allQuickTexts.add(cursor.getString(1));
        		mMaxQuickTextId = mMaxQuickTextId >= qtId? mMaxQuickTextId : qtId;
        	}
        }
        cursor.close();
        adapter = new ArrayAdapter<String>(this, R.layout.quick_text_edit_item, allQuickTexts);
        mListView.setAdapter(adapter);
	}
    
    private void makeAToast(Integer strId){
    	if (strId != null) {
    		Toast.makeText(this, strId, Toast.LENGTH_LONG).show();
    	}
    }
    
    private void makeAToast(String message){
    	if (message != null && !message.equals("")) {
    		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    	}
    }
    
    private void showEditDialog(String quickText) {
    	mOldQuickText = new EditText(this);
    	mOldQuickText.setHint(R.string.type_to_compose_text_enter_to_send);
    	mOldQuickText.computeScroll();
    	mOldQuickText.setText(quickText);
    	mOldQuickText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_EDITABLE_LENGTH)});
    	mQuicktextAlertDialog = new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(R.string.quick_text_editor)
        .setView(mOldQuickText)
        .setPositiveButton(android.R.string.ok, new UpdateButtonListener())
        .setNegativeButton(android.R.string.cancel, new CancelButtonListener())
        .show();
	}
}
