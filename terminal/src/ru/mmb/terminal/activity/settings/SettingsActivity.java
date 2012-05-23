package ru.mmb.terminal.activity.settings;

import ru.mmb.terminal.R;
import ru.mmb.terminal.model.registry.Settings;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends Activity
{
	private EditText editUserId;
	private EditText editDeviceId;
	private EditText editCurrentRaidId;
	private EditText editLastExportDate;
	private EditText editTranspUserId;
	private EditText editTranspUserPassword;
	private CheckBox checkTeamClearFilter;
	private EditText editCheckboxesPerLine;

	private Button btnSave;

	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.settings);

		Settings.getInstance().refresh();

		editUserId = (EditText) findViewById(R.id.settings_userIdEdit);
		editDeviceId = (EditText) findViewById(R.id.settings_deviceIdEdit);
		editCurrentRaidId = (EditText) findViewById(R.id.settings_currentRaidIdEdit);
		editLastExportDate = (EditText) findViewById(R.id.settings_lastExportDateEdit);
		editTranspUserId = (EditText) findViewById(R.id.settings_transpUserIdEdit);
		editTranspUserPassword = (EditText) findViewById(R.id.settings_transpUserPasswordEdit);
		checkTeamClearFilter = (CheckBox) findViewById(R.id.settings_teamClearFilterCheckbox);
		editCheckboxesPerLine = (EditText) findViewById(R.id.settings_checkboxesPerLineEdit);

		btnSave = (Button) findViewById(R.id.settings_saveBtn);

		btnSave.setOnClickListener(new SaveClickListener());

		refreshState();
	}

	private void refreshState()
	{
		setTitle(getResources().getString(R.string.settings_title));

		editUserId.setText(Integer.toString(Settings.getInstance().getUserId()));
		editDeviceId.setText(Integer.toString(Settings.getInstance().getDeviceId()));
		editCurrentRaidId.setText(Integer.toString(Settings.getInstance().getCurrentRaidId()));
		editLastExportDate.setText(Settings.getInstance().getLastExportDate());
		editTranspUserId.setText(Integer.toString(Settings.getInstance().getTranspUserId()));
		editTranspUserPassword.setText(Settings.getInstance().getTranspUserPassword());
		checkTeamClearFilter.setChecked(Settings.getInstance().isTeamClearFilterAfterOk());
		editCheckboxesPerLine.setText(Settings.getInstance().getCheckboxesPerLine());
	}

	private void saveSettings()
	{
		Settings.getInstance().setUserId(editUserId.getText().toString());
		Settings.getInstance().setDeviceId(editDeviceId.getText().toString());
		Settings.getInstance().setCurrentRaidId(editCurrentRaidId.getText().toString());
		Settings.getInstance().setTranspUserId(editTranspUserId.getText().toString());
		Settings.getInstance().setTranspUserPassword(editTranspUserPassword.getText().toString());
		Settings.getInstance().setTeamClearFilterAfterOk(Boolean.toString(checkTeamClearFilter.isChecked()));
		Settings.getInstance().setCheckboxesPerLine(editCheckboxesPerLine.getText().toString());

		Toast.makeText(getApplicationContext(), getResources().getText(R.string.settings_applied), Toast.LENGTH_SHORT).show();
	}

	private class SaveClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			saveSettings();
		}
	}
}
