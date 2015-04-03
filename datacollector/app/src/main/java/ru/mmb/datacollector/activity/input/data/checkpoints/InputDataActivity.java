package ru.mmb.datacollector.activity.input.data.checkpoints;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Date;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.StateChangeListener;
import ru.mmb.datacollector.activity.input.data.withdraw.WithdrawMemberActivity;
import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.registry.Settings;

import static ru.mmb.datacollector.activity.Constants.REQUEST_CODE_WITHDRAW_MEMBER_ACTIVITY;

public class InputDataActivity extends Activity implements StateChangeListener {
    private InputDataActivityState currentState;

    private LinearLayout panelExistsIndicator;
    private TextView labTeamName;
    private TextView labTeamNumber;
    private TextView labResult;
    private Button btnOk;
    private Button btnWithdraw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getInstance().setCurrentContext(this);

        currentState = new InputDataActivityState();
        currentState.initialize(this, savedInstanceState);

        setContentView(R.layout.input_data_checkpoints);

        new CheckpointPanel(this, currentState);

        panelExistsIndicator = (LinearLayout) findViewById(R.id.inputData_existsIndicatorPanel);
        labTeamName = (TextView) findViewById(R.id.inputData_teamNameTextView);
        labTeamNumber = (TextView) findViewById(R.id.inputData_teamNumberTextView);
        labResult = (TextView) findViewById(R.id.inputData_resultTextView);
        btnOk = (Button) findViewById(R.id.inputData_okButton);
        btnWithdraw = (Button) findViewById(R.id.inputData_withdrawButton);

        setTitle(currentState.getScanPointText(this));
        if (currentState.getCurrentTeam() != null) {
            labTeamName.setText(currentState.getCurrentTeam().getTeamName());
            labTeamNumber.setText(Integer.toString(currentState.getCurrentTeam().getTeamNum()));
        }
        labResult.setText(currentState.getResultText(this));

        btnOk.setOnClickListener(new OkBtnClickListener());
        btnWithdraw.setOnClickListener(new WithdrawMemberClickListener());

        currentState.addStateChangeListener(this);
        refreshExistsIndicator();
    }

    private void refreshExistsIndicator() {
        if (currentState.isEditingExistingRecord()) {
            //Log.d("input_data_checkpoints", "record exists");
            panelExistsIndicator.setBackgroundResource(R.color.LightSkyBlue);
        } else {
            //Log.d("input_data_checkpoints", "record NOT exists");
            panelExistsIndicator.setBackgroundResource(R.color.Pink);
        }
    }

    @Override
    public void onStateChange() {
        //Log.d("input data", "state change fired");
        labResult.setText(currentState.getResultText(this));
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

    private boolean isFinish() {
        int distanceId = currentState.getCurrentTeam().getDistanceId();
        LevelPoint levelPoint = currentState.getCurrentScanPoint().getLevelPointByDistance(distanceId);
        return levelPoint.getPointType().isFinish();
    }

    private class OkBtnClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (isFinish()) {
                Date recordDateTime = new Date();
                currentState.saveInputDataToDB(recordDateTime);
                currentState.putTeamLevelPointToDataStorage(recordDateTime);
            }
            setResult(RESULT_OK);
            finish();
        }
    }

    private class WithdrawMemberClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), WithdrawMemberActivity.class);
            currentState.prepareStartActivityIntent(intent, REQUEST_CODE_WITHDRAW_MEMBER_ACTIVITY);
            startActivity(intent);
        }
    }
}
