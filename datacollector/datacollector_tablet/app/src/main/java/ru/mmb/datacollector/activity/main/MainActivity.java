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
import ru.mmb.datacollector.activity.report.team.search.start.TeamSearchStartActivity;
import ru.mmb.datacollector.activity.settings.SettingsActivity;
import ru.mmb.datacollector.activity.transport.TransportInputActivity;
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

    private Button btnTransport;
    private Button btnInputData;
    private Button btnSearchTeam;
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

        btnTransport = (Button) findViewById(R.id.main_transportBtn);
        btnInputData = (Button) findViewById(R.id.main_inputDataBtn);
        btnSearchTeam = (Button) findViewById(R.id.main_searchTeamBtn);
        btnSettings = (Button) findViewById(R.id.main_settingsBtn);
        btnGenerate = (Button) findViewById(R.id.main_generateBtn);

        btnTransport.setOnClickListener(new TransportButtonClickListener());
        btnInputData.setOnClickListener(new InputDataClickListener());
        btnSearchTeam.setOnClickListener(new SearchTeamClickListener());
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
    }

    private void refreshButtons() {
        boolean enabled = currentState.isEnabled();
        btnTransport.setEnabled(enabled);
        btnInputData.setEnabled(enabled);
        btnSearchTeam.setEnabled(enabled);
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
            Intent intent = new Intent(getApplicationContext(), TransportInputActivity.class);
            startActivity(intent);
        }
    }

    private class InputDataClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), StartInputActivity.class);
            startActivity(intent);
        }
    }

    private class SearchTeamClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), TeamSearchStartActivity.class);
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

    private class GenerateClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            FillData.execute();
        }
    }
}
