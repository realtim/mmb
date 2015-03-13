package ru.mmb.datacollector.activity.main;

import static ru.mmb.datacollector.activity.Constants.REQUEST_CODE_SETTINGS_ACTIVITY;
import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.input.start.StartInputActivity;
import ru.mmb.datacollector.activity.report.ResultsActivity;
import ru.mmb.datacollector.activity.settings.SettingsActivity;
import ru.mmb.datacollector.activity.transport.transpexport.TransportExportActivity;
import ru.mmb.datacollector.activity.transport.transpimport.TransportImportActivity;
import ru.mmb.datacollector.activity.transport.transpimport.TransportImportBarcodeActivity;
import ru.mmb.datacollector.model.registry.Settings;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity
{
	private MainActivityState currentState;

	private TextView labDBFile;
	private TextView labConnection;
	private TextView labUserId;
	private TextView labDeviceId;
	private TextView labCurrentRaidId;

	private Button btnInputData;
	private Button btnImportData;
	private Button btnImportBarcodeData;
	private Button btnExportData;
	private Button btnResults;
	private Button btnSettings;

	//	private Button btnGenerate;

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
		labUserId = (TextView) findViewById(R.id.main_userIDLabel);
		labDeviceId = (TextView) findViewById(R.id.main_deviceIDLabel);
		labCurrentRaidId = (TextView) findViewById(R.id.main_currentRaidIDLabel);

		btnInputData = (Button) findViewById(R.id.main_inputDataBtn);
		btnImportData = (Button) findViewById(R.id.main_importDataBtn);
		btnImportBarcodeData = (Button) findViewById(R.id.main_importBarcodeDataBtn);
		btnExportData = (Button) findViewById(R.id.main_exportDataBtn);
		btnResults = (Button) findViewById(R.id.main_resultsBtn);
		btnSettings = (Button) findViewById(R.id.main_settingsBtn);
		//		btnGenerate = (Button) findViewById(R.id.main_generateBtn);

		btnInputData.setOnClickListener(new InputDataClickListener());
		btnImportData.setOnClickListener(new ImportDataClickListener());
		btnImportBarcodeData.setOnClickListener(new ImportBarcodeDataClickListener());
		btnExportData.setOnClickListener(new ExportDataClickListener());
		btnResults.setOnClickListener(new ResultsClickListener());
		btnSettings.setOnClickListener(new SettingsClickListener());
		//		btnGenerate.setOnClickListener(new GenerateClickListener());

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
		labUserId.setText(currentState.getUserIDText(this));
		labUserId.setTextColor(currentState.getColor(this, currentState.isUserIdSelected()));
		labDeviceId.setText(currentState.getDeviceIDText(this));
		labDeviceId.setTextColor(currentState.getColor(this, currentState.isDeviceIdSelected()));
		labCurrentRaidId.setText(currentState.getCurrentRaidIDText(this));
		labCurrentRaidId.setTextColor(currentState.getColor(this, currentState.isCurrentRaidIdSelected()));
	}

	private void refreshButtons()
	{
		boolean enabled = currentState.isEnabled();
		btnInputData.setEnabled(enabled);
		btnImportData.setEnabled(enabled);
		btnImportBarcodeData.setEnabled(enabled);
		btnExportData.setEnabled(enabled);
		btnResults.setEnabled(enabled);
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

	private class ImportBarcodeDataClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent =
			    new Intent(getApplicationContext(), TransportImportBarcodeActivity.class);
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

	private class ResultsClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(getApplicationContext(), ResultsActivity.class);
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

	/*private class GenerateClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			FillBarCodeData.execute();
		}
	}*/
}
