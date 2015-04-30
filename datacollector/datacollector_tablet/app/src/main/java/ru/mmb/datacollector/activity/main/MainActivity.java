package ru.mmb.datacollector.activity.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.input.start.StartInputActivity;
import ru.mmb.datacollector.activity.report.ResultsActivity;
import ru.mmb.datacollector.activity.settings.SettingsActivity;
import ru.mmb.datacollector.activity.transport.TransportInputActivity;
import ru.mmb.datacollector.activity.transport.TransportReportActivity;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.util.FillData;

import static ru.mmb.datacollector.activity.Constants.REQUEST_CODE_SETTINGS_ACTIVITY;

public class MainActivity extends Activity {
    private MainActivityState currentState;

    private TextView labDBFile;
    private TextView labConnection;
    private TextView labUserId;
    private TextView labDeviceId;
    private TextView labCurrentRaidId;
    private TextView labApplicationMode;

    private Button btnTransport;
    private Button btnInputData;
    private Button btnResults;
    private Button btnSettings;

    private Button btnGenerate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
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
        labApplicationMode = (TextView) findViewById(R.id.main_applicationModeLabel);

        btnTransport = (Button) findViewById(R.id.main_transportBtn);
        btnInputData = (Button) findViewById(R.id.main_inputDataBtn);
        btnResults = (Button) findViewById(R.id.main_resultsBtn);
        btnSettings = (Button) findViewById(R.id.main_settingsBtn);
        btnGenerate = (Button) findViewById(R.id.main_generateBtn);

        btnTransport.setOnClickListener(new TransportButtonClickListener());
        btnInputData.setOnClickListener(new InputDataClickListener());
        btnResults.setOnClickListener(new ResultsClickListener());
        btnSettings.setOnClickListener(new SettingsClickListener());
        btnGenerate.setOnClickListener(new GenerateClickListener());

        // comment this line to show Generate button
        btnGenerate.setVisibility(View.GONE);

        refreshState();
    }

    private void refreshState() {
        setTitle(getResources().getString(R.string.main_title));

        refreshLabels();
        refreshButtons();
    }

    private void refreshLabels() {
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
        labApplicationMode.setText(currentState.getApplicationModeText(this));
        // Application mode is always defined. It is INPUT by default.
        labApplicationMode.setTextColor(currentState.getColor(this, true));
    }

    private void refreshButtons() {
        boolean enabled = currentState.isEnabled();
        btnTransport.setEnabled(enabled);
        btnInputData.setEnabled(enabled && Settings.getInstance().isApplicationModeInput());
        btnResults.setEnabled(enabled && Settings.getInstance().isApplicationModeReport());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        currentState.save(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SETTINGS_ACTIVITY) {
            refreshState();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class TransportButtonClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (Settings.getInstance().isApplicationModeInput()) {
                Intent intent = new Intent(getApplicationContext(), TransportInputActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(getApplicationContext(), TransportReportActivity.class);
                startActivity(intent);
            }
        }
    }

    private class InputDataClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), StartInputActivity.class);
            startActivity(intent);
        }
    }

    private class ResultsClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), ResultsActivity.class);
            startActivity(intent);
        }
    }

    private class SettingsClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivityForResult(intent, REQUEST_CODE_SETTINGS_ACTIVITY);
        }
    }

	private class GenerateClickListener implements OnClickListener
    {
		@Override
		public void onClick(View v)
		{
			FillData.execute();
		}
	}
}
