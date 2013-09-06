package ru.mmb.terminal.activity.main;

import static ru.mmb.terminal.activity.Constants.REQUEST_CODE_SETTINGS_ACTIVITY;
import ru.mmb.terminal.R;
import ru.mmb.terminal.activity.input.start.StartInputActivity;
import ru.mmb.terminal.activity.settings.SettingsActivity;
import ru.mmb.terminal.activity.transport.transpexport.TransportExportActivity;
import ru.mmb.terminal.activity.transport.transpimport.TransportImportActivity;
import ru.mmb.terminal.model.registry.Settings;
import ru.mmb.terminal.util.FillData;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity
{
	private MainActivityState currentState;

	private TextView labDBFile;
	private TextView labConnection;
	private TextView labCurrentRaidId;

	private Button btnInputData;
	private Button btnImportData;
	private Button btnExportData;
	private Button btnSettings;

	// private Button btnFillData;

	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Settings.getInstance().setCurrentContext(this);

		currentState = new MainActivityState();
		currentState.initialize(this, savedInstanceState);

		setContentView(R.layout.main);

		labDBFile = (TextView) findViewById(R.id.main_dataBaseFileLabel);
		labConnection = (TextView) findViewById(R.id.main_dataBaseConnectionLabel);
		labCurrentRaidId = (TextView) findViewById(R.id.main_currentRaidIDLabel);

		btnInputData = (Button) findViewById(R.id.main_inputDataBtn);
		btnImportData = (Button) findViewById(R.id.main_importDataBtn);
		btnExportData = (Button) findViewById(R.id.main_exportDataBtn);
		btnSettings = (Button) findViewById(R.id.main_settingsBtn);
		// btnFillData = (Button) findViewById(R.id.main_fillDataBtn);

		btnInputData.setOnClickListener(new InputDataClickListener());
		btnImportData.setOnClickListener(new ImportDataClickListener());
		btnExportData.setOnClickListener(new ExportDataClickListener());
		btnSettings.setOnClickListener(new SettingsClickListener());
		// btnFillData.setOnClickListener(new FillDataClickListener());

		refreshState();
	}

	private void refreshState()
	{
		setTitle(getResources().getString(R.string.main_title));

		refreshLabels();
		refreshButtons();
	}

	private void refreshLabels()
	{
		labDBFile.setText(currentState.getDBFileText(this));
		labDBFile.setTextColor(currentState.getColor(this, currentState.isDBFileSelected()));
		labConnection.setText(currentState.getConnectionText(this));
		labConnection.setTextColor(currentState.getColor(this, currentState.isConnected()));
		labCurrentRaidId.setText(currentState.getCurrentRaidIDText(this));
		labCurrentRaidId.setTextColor(currentState.getColor(this, currentState.isCurrentRaidSelected()));
	}

	private void refreshButtons()
	{
		boolean enabled = currentState.isEnabled();
		btnInputData.setEnabled(enabled);
		btnImportData.setEnabled(enabled);
		btnExportData.setEnabled(enabled);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		currentState.save(outState);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == REQUEST_CODE_SETTINGS_ACTIVITY)
		{
			refreshState();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private class InputDataClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(getApplicationContext(), StartInputActivity.class);
			startActivity(intent);
		}
	}

	private class ImportDataClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(getApplicationContext(), TransportImportActivity.class);
			startActivity(intent);
		}
	}

	private class ExportDataClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(getApplicationContext(), TransportExportActivity.class);
			startActivity(intent);
		}
	}

	private class SettingsClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
			startActivityForResult(intent, REQUEST_CODE_SETTINGS_ACTIVITY);
		}
	}

	@SuppressWarnings("unused")
	private class FillDataClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			FillData.execute();
			Toast.makeText(getApplication(), "Data generated", Toast.LENGTH_LONG).show();
		}
	}
}
