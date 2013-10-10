package ru.mmb.terminal.activity.settings;

import static ru.mmb.terminal.activity.Constants.REQUEST_CODE_SETTINGS_DB_FILE_DIALOG;
import static ru.mmb.terminal.activity.Constants.REQUEST_CODE_SETTINGS_DEVICE_JSON_DIALOG;

import java.io.File;

import ru.mmb.terminal.R;
import ru.mmb.terminal.model.registry.Settings;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.filedialog.FileDialog;
import com.filedialog.SelectionMode;

public class SettingsActivity extends FragmentActivity
{
	private TextView labelPathToTerminalDB;
	private Button btnSelectTerminalDBFile;

	private Button btnImportDeviceJson;

	private EditText editUserId;
	private EditText editDeviceId;
	private EditText editCurrentRaidId;
	private EditText editTranspUserId;
	private EditText editTranspUserPassword;
	private TextEditorActionListener textEditorActionListener;
	private TextEditorFocusChangeListener textEditorFocusChangeListener;

	private EditText currentEditor = null;

	private DeviceJsonImporter deviceJsonImporter = null;
	private boolean needStartDeviceDialog = false;

	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Settings.getInstance().setCurrentContext(this);

		setContentView(R.layout.settings);

		labelPathToTerminalDB = (TextView) findViewById(R.id.settings_pathToTerminalDBLabel);
		btnSelectTerminalDBFile = (Button) findViewById(R.id.settings_selectTerminalDBBtn);
		btnSelectTerminalDBFile.setOnClickListener(new SelectTerminalDBFileClickListener());

		btnImportDeviceJson = (Button) findViewById(R.id.settings_importDeviceJsonBtn);
		btnImportDeviceJson.setOnClickListener(new ImportDeviceJsonClickListener());

		textEditorActionListener = new TextEditorActionListener();
		textEditorFocusChangeListener = new TextEditorFocusChangeListener();

		editUserId = (EditText) findViewById(R.id.settings_userIdEdit);
		hookTextEditor(editUserId, EditorInfo.IME_ACTION_NEXT);
		editDeviceId = (EditText) findViewById(R.id.settings_deviceIdEdit);
		hookTextEditor(editDeviceId, EditorInfo.IME_ACTION_NEXT);
		editCurrentRaidId = (EditText) findViewById(R.id.settings_currentRaidIdEdit);
		hookTextEditor(editCurrentRaidId, EditorInfo.IME_ACTION_NEXT);
		editTranspUserId = (EditText) findViewById(R.id.settings_transpUserIdEdit);
		hookTextEditor(editTranspUserId, EditorInfo.IME_ACTION_NEXT);
		editTranspUserPassword = (EditText) findViewById(R.id.settings_transpUserPasswordEdit);
		hookTextEditor(editTranspUserPassword, EditorInfo.IME_ACTION_DONE);

		refreshState();
	}

	private void hookTextEditor(EditText textEditor, int imeOptions)
	{
		textEditor.setImeOptions(imeOptions);
		textEditor.setOnEditorActionListener(textEditorActionListener);
		textEditor.setOnFocusChangeListener(textEditorFocusChangeListener);
	}

	public void refreshState()
	{
		setTitle(getResources().getString(R.string.settings_title));

		labelPathToTerminalDB.setText(Settings.getInstance().getPathToTerminalDB());
		editUserId.setText(Integer.toString(Settings.getInstance().getUserId()));
		editDeviceId.setText(Integer.toString(Settings.getInstance().getDeviceId()));
		editCurrentRaidId.setText(Integer.toString(Settings.getInstance().getCurrentRaidId()));
		editTranspUserId.setText(Integer.toString(Settings.getInstance().getTranspUserId()));
		editTranspUserPassword.setText(Settings.getInstance().getTranspUserPassword());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case REQUEST_CODE_SETTINGS_DB_FILE_DIALOG:
				if (resultCode == Activity.RESULT_OK)
				{
					String terminalDBFileName = data.getStringExtra(FileDialog.RESULT_PATH);
					labelPathToTerminalDB.setText(terminalDBFileName);
					Settings.getInstance().setPathToTerminalDB(terminalDBFileName);
				}
				break;
			case REQUEST_CODE_SETTINGS_DEVICE_JSON_DIALOG:
				if (resultCode == Activity.RESULT_OK)
				{
					String deviceJsonName = data.getStringExtra(FileDialog.RESULT_PATH);
					deviceJsonImporter = new DeviceJsonImporter(this);
					if (deviceJsonImporter.prepareJsonObjects(deviceJsonName))
					{
						needStartDeviceDialog = true;
					}
					else
					{
						deviceJsonImporter = null;
					}
				}
				break;
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		// DialogFragment is not created in onActivityResult, 
		// because activity is yet not working there.
		// So we are waiting for resume.
		if (needStartDeviceDialog)
		{
			needStartDeviceDialog = false;
			if (deviceJsonImporter != null) deviceJsonImporter.showImportDialog();
		}
	}

	@Override
	protected void onPause()
	{
		if (currentEditor != null)
		{
			onTextEditorContentsChanged(currentEditor);
		}
		super.onPause();
	}

	private void onTextEditorContentsChanged(View view)
	{
		if (view == editUserId)
		{
			Settings.getInstance().setUserId(editUserId.getText().toString());
		}
		if (view == editDeviceId)
		{
			Settings.getInstance().setDeviceId(editDeviceId.getText().toString());
		}
		if (view == editCurrentRaidId)
		{
			Settings.getInstance().setCurrentRaidId(editCurrentRaidId.getText().toString());
		}
		if (view == editTranspUserId)
		{
			Settings.getInstance().setTranspUserId(editTranspUserId.getText().toString());
		}
		if (view == editTranspUserPassword)
		{
			Settings.getInstance().setTranspUserPassword(editTranspUserPassword.getText().toString());
		}
	}

	private class SelectTerminalDBFileClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(getBaseContext(), FileDialog.class);
			String startPath = extractStartPath();
			if (startPath != null)
			{
				intent.putExtra(FileDialog.START_PATH, startPath);
			}
			intent.putExtra(FileDialog.CAN_SELECT_DIR, false);
			intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
			intent.putExtra(FileDialog.FORMAT_FILTER, new String[] { ".db" });
			startActivityForResult(intent, REQUEST_CODE_SETTINGS_DB_FILE_DIALOG);
		}
	}

	private String extractStartPath()
	{
		String result = null;
		String prevPathToTerminalDB = labelPathToTerminalDB.getText().toString();
		if (!"".equals(prevPathToTerminalDB))
		{
			File dbFile = new File(prevPathToTerminalDB);
			File dbFileDir = new File(dbFile.getParent());
			if (dbFileDir.exists())
			{
				result = dbFileDir.getPath();
			}
		}
		return result;
	}

	private class TextEditorActionListener implements OnEditorActionListener
	{
		@Override
		public boolean onEditorAction(TextView view, int action, KeyEvent event)
		{
			if (action == EditorInfo.IME_ACTION_NEXT || action == EditorInfo.IME_ACTION_DONE)
			{
				onTextEditorContentsChanged(view);
			}
			return false;
		}
	}

	private class TextEditorFocusChangeListener implements OnFocusChangeListener
	{
		@Override
		public void onFocusChange(View v, boolean hasFocus)
		{
			// This listener is attached only to EditText controls.
			// So, here v is always EditText.
			if (!hasFocus)
			{
				onTextEditorContentsChanged(v);
			}
			else
			{
				currentEditor = (EditText) v;
			}
		}
	}

	private class ImportDeviceJsonClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(getBaseContext(), FileDialog.class);
			String startPath = extractStartPath();
			if (startPath != null)
			{
				intent.putExtra(FileDialog.START_PATH, startPath);
			}
			intent.putExtra(FileDialog.CAN_SELECT_DIR, false);
			intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
			intent.putExtra(FileDialog.FORMAT_FILTER, new String[] { ".json" });
			startActivityForResult(intent, REQUEST_CODE_SETTINGS_DEVICE_JSON_DIALOG);
		}
	}
}
