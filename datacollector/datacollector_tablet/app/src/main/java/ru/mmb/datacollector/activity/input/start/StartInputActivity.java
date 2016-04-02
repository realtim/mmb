package ru.mmb.datacollector.activity.input.start;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.ActivityStateWithTeamAndScanPoint;
import ru.mmb.datacollector.activity.input.bclogger.start.StartWorkWithBCLoggerActivity;
import ru.mmb.datacollector.activity.input.data.history.HistoryActivity;
import ru.mmb.datacollector.activity.input.scanpoint.SelectScanPointActivity;
import ru.mmb.datacollector.model.registry.Settings;

import static ru.mmb.datacollector.activity.Constants.REQUEST_CODE_INPUT_BCLOGGER_START_ACTIVITY;
import static ru.mmb.datacollector.activity.Constants.REQUEST_CODE_INPUT_HISTORY_ACTIVITY;
import static ru.mmb.datacollector.activity.Constants.REQUEST_CODE_SCAN_POINT_ACTIVITY;

public class StartInputActivity extends Activity {
    private ActivityStateWithTeamAndScanPoint currentState;

    private Button btnSelectScanPoint;
    private Button btnWorkWithLogger;
    private Button btnProceedInput;
    private TextView labScanPoint;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getInstance().setCurrentContext(this);

        currentState = new ActivityStateWithTeamAndScanPoint("input.start");
        currentState.initialize(this, savedInstanceState);

        setContentView(R.layout.input_start);

        btnSelectScanPoint = (Button) findViewById(R.id.inputStart_selectScanPointBtn);
        btnWorkWithLogger = (Button) findViewById(R.id.inputStart_workWithLoggerBtn);
        btnProceedInput = (Button) findViewById(R.id.inputStart_proceedInputBtn);
        labScanPoint = (TextView) findViewById(R.id.inputStart_scanPointLabel);

        btnSelectScanPoint.setOnClickListener(new SelectScanPointClickListener());
        btnWorkWithLogger.setOnClickListener(new WorkWithLoggerClickListener());
        btnProceedInput.setOnClickListener(new ProceedInputClickListener());

        refreshState();
    }

    private void refreshState() {
        setTitle(currentState.getScanPointText(this));

        if (currentState.getCurrentScanPoint() != null)
            labScanPoint.setText(currentState.getCurrentScanPoint().getScanPointName());

        btnWorkWithLogger.setEnabled(currentState.isScanPointSelected());
        btnProceedInput.setEnabled(currentState.isScanPointSelected());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SCAN_POINT_ACTIVITY:
                onSelectScanPointActivityResult(resultCode, data);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void onSelectScanPointActivityResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            currentState.loadFromIntent(data);
            refreshState();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        currentState.save(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        currentState.saveToSharedPreferences(getPreferences(MODE_PRIVATE));
    }

    private class SelectScanPointClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), SelectScanPointActivity.class);
            currentState.prepareStartActivityIntent(intent, REQUEST_CODE_SCAN_POINT_ACTIVITY);
            startActivityForResult(intent, REQUEST_CODE_SCAN_POINT_ACTIVITY);
        }
    }

    private class WorkWithLoggerClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), StartWorkWithBCLoggerActivity.class);
            currentState.prepareStartActivityIntent(intent, REQUEST_CODE_INPUT_BCLOGGER_START_ACTIVITY);
            startActivity(intent);
        }
    }

    private class ProceedInputClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), HistoryActivity.class);
            currentState.prepareStartActivityIntent(intent, REQUEST_CODE_INPUT_HISTORY_ACTIVITY);
            startActivity(intent);
        }
    }
}
