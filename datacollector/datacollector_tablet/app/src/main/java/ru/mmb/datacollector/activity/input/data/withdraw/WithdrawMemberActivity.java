package ru.mmb.datacollector.activity.input.data.withdraw;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.input.data.withdraw.list.MembersAdapter;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.registry.ScanPointsRegistry;
import ru.mmb.datacollector.model.registry.Settings;

public class WithdrawMemberActivity extends Activity implements WithdrawStateChangeListener {
    private WithdrawMemberActivityState currentState;
    private TextView labTeamName;
    private TextView labTeamNumber;
    private Spinner comboWithdrawScanPoint;
    private TextView labResult;
    private Button btnOk;
    private ListView lvMembers;

    MembersAdapter lvMembersAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getInstance().setCurrentContext(this);

        currentState = new WithdrawMemberActivityState();
        currentState.initialize(this, savedInstanceState);

        setContentView(R.layout.input_data_withdraw);

        lvMembers = (ListView) findViewById(R.id.inputWithdraw_withdrawList);
        initListAdapter();

        labTeamName = (TextView) findViewById(R.id.inputWithdraw_teamNameTextView);
        labTeamNumber = (TextView) findViewById(R.id.inputWithdraw_teamNumberTextView);
        comboWithdrawScanPoint = (Spinner) findViewById(R.id.inputWithdraw_scanPointSpinner);
        labResult = (TextView) findViewById(R.id.inputWithdraw_resultTextView);
        btnOk = (Button) findViewById(R.id.inputWithdraw_okButton);

        setTitle(currentState.getScanPointText(this));

        setWithdrawScanPointAdapter();
        refreshWithdrawScanPointState();

        labTeamName.setText(currentState.getCurrentTeam().getTeamName());
        labTeamNumber.setText(Integer.toString(currentState.getCurrentTeam().getTeamNum()));
        labResult.setText(currentState.getResultText(this));

        comboWithdrawScanPoint.setOnItemSelectedListener(new WithdrawScanPointOnItemSelectedListener());
        btnOk.setOnClickListener(new OkBtnClickListener());

        currentState.addStateChangeListener(this);
    }

    private void initListAdapter() {
        lvMembersAdapter = new MembersAdapter(this, R.layout.input_data_withdraw_row, currentState.getMemberRecords(), currentState);
        lvMembers.setAdapter(lvMembersAdapter);
    }

    private void setWithdrawScanPointAdapter() {
        String[] limitedScanPointNames = ScanPointsRegistry.getInstance().getScanPointNamesArray(currentState.getCurrentScanPoint());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, limitedScanPointNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        comboWithdrawScanPoint.setAdapter(adapter);
    }

    private void refreshWithdrawScanPointState() {
        if (currentState.getWithdrawScanPoint() == null) {
            if (currentState.getCurrentScanPoint() == null) {
                comboWithdrawScanPoint.setSelection(0);
            } else {
                updateWithdrawScanPointSelection(currentState.getCurrentScanPoint());
            }
        } else {
            updateWithdrawScanPointSelection(currentState.getWithdrawScanPoint());
        }
    }

    private void updateWithdrawScanPointSelection(ScanPoint scanPoint) {
        int pos = ScanPointsRegistry.getInstance().getScanPointIndex(scanPoint);
        if (pos == -1) {
            pos = 0;
        }
        comboWithdrawScanPoint.setSelection(pos);
    }

    private void setControlsEnabled(boolean enabled) {
        comboWithdrawScanPoint.setEnabled(enabled);
        lvMembers.setEnabled(enabled);
        btnOk.setEnabled(enabled);
    }

    @Override
    public void onStateChange() {
        labResult.setText(currentState.getResultText(this));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        currentState.save(outState);
    }

    @Override
    public void onStateReload() {
        Log.d("WITHDRAW", "refresh members list");
        lvMembersAdapter.refresh();
    }

    private class OkBtnClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            Date recordDateTime = new Date();
            currentState.saveCurrWithdrawnToDB(recordDateTime);
            currentState.putCurrWithdrawnToDataStorage(recordDateTime);
            setResult(RESULT_OK);
            finish();
        }
    }

    private class WithdrawScanPointOnItemSelectedListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            ScanPoint newScanPoint = ScanPointsRegistry.getInstance().getScanPointByIndex(position);
            Log.d("WITHDRAW", "withdraw scan point: " + currentState.getWithdrawScanPoint().getScanPointName());
            Log.d("WITHDRAW", "selected scan point: " + newScanPoint.getScanPointName());
            if (!currentState.getWithdrawScanPoint().equals(newScanPoint)) {
                setControlsEnabled(false);
                if (currentState.hasItemsToSave()) {
                    Log.d("WITHDRAW", "saving data");
                    showSavingMessage();
                    Date recordDateTime = new Date();
                    currentState.saveCurrWithdrawnToDB(recordDateTime);
                }
                currentState.setWithdrawScanPoint(newScanPoint);
                setControlsEnabled(true);
            }
        }

        private void showSavingMessage() {
            String message = WithdrawMemberActivity.this.getResources().getString(R.string.input_withdraw_saving);
            Toast.makeText(WithdrawMemberActivity.this, message, Toast.LENGTH_SHORT);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // do nothing
        }
    }
}
