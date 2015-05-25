package ru.mmb.datacollector.activity.input.data.history;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.input.data.checkpoints.InputDataActivity;
import ru.mmb.datacollector.activity.input.data.history.list.DataProvider;
import ru.mmb.datacollector.activity.input.data.history.list.HistoryAdapter;
import ru.mmb.datacollector.activity.input.data.history.list.HistoryFilter;
import ru.mmb.datacollector.activity.input.data.history.list.HistoryListRecord;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.history.DataStorage;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.model.registry.TeamsRegistry;
import ru.mmb.datacollector.widget.EditTextWithSoftKeyboardSupport;

import static ru.mmb.datacollector.activity.Constants.REQUEST_CODE_INPUT_DATA_ACTIVITY;

public class HistoryActivity extends Activity {
    private HistoryActivityState currentState;

    private HistoryAdapter historyAdapter;
    private DataStorage dataStorage;

    private EditTextWithSoftKeyboardSupport editTeamNumber;

    private TeamNumberChangeListener teamNumberChangeListener;

    private ListView lvHistory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getInstance().setCurrentContext(this);

        currentState = new HistoryActivityState();
        currentState.initialize(this, savedInstanceState);

        setContentView(R.layout.input_data_history);

        editTeamNumber =
                (EditTextWithSoftKeyboardSupport) findViewById(R.id.inputHistory_teamNumberEdit);
        editTeamNumber.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        editTeamNumber.setImeOptions(EditorInfo.IME_ACTION_DONE);

        teamNumberChangeListener = new TeamNumberChangeListener();
        editTeamNumber.setOnEditorActionListener(teamNumberChangeListener);
        editTeamNumber.setSoftKeyboardBackListener(teamNumberChangeListener);

        dataStorage = DataStorage.getInstance(currentState.getCurrentScanPoint());

        lvHistory = (ListView) findViewById(R.id.inputHistory_historyList);
        historyAdapter = new HistoryAdapter(this, R.layout.input_data_history_row);
        lvHistory.setAdapter(historyAdapter);

        lvHistory.setOnItemLongClickListener(new LvHistoryItemLongClickListener());

        setTitle(currentState.getScanPointText(this));

        refreshHistory();
    }

    private void refreshHistory() {
        ((HistoryFilter) historyAdapter.getFilter()).reset(DataProvider.getHistoryRecords(dataStorage));
        historyAdapter.getFilter().filter("");
    }

    private void clearSelectedTeam() {
        editTeamNumber.setText("");
        currentState.setCurrentTeam(null);
    }

    private Team getSelectedTeam() {
        try {
            Integer teamNumber = Integer.parseInt(editTeamNumber.getText().toString());
            return TeamsRegistry.getInstance().getTeamByNumber(teamNumber);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_INPUT_DATA_ACTIVITY:
                onInputActivityResult(resultCode, data);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void onInputActivityResult(int resultCode, Intent data) {
        clearSelectedTeam();
        refreshHistory();
        if (lvHistory.getAdapter().getCount() > 0) {
            lvHistory.setItemChecked(0, true);
            lvHistory.setSelection(0);
        }
        focusNumberInputAndShowKeyboard();
    }

    public void focusNumberInputAndShowKeyboard() {
        editTeamNumber.requestFocus();
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
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

    private void startInputDataActivity() {
        Intent intent = new Intent(getApplicationContext(), InputDataActivity.class);
        currentState.prepareStartActivityIntent(intent, REQUEST_CODE_INPUT_DATA_ACTIVITY);
        startActivityForResult(intent, REQUEST_CODE_INPUT_DATA_ACTIVITY);
    }

    private class LvHistoryItemLongClickListener implements OnItemLongClickListener {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long itemId) {
            HistoryListRecord historyListRecord =
                    ((HistoryAdapter) parent.getAdapter()).getItem(position);
            currentState.setCurrentTeam(historyListRecord.getTeam());

            // hide soft keyboard
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(lvHistory.getApplicationWindowToken(), 0);

            startInputDataActivity();
            return true;
        }
    }

    private class TeamNumberChangeListener implements OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if ((actionId == EditorInfo.IME_ACTION_UNSPECIFIED &&
                 event.getKeyCode() == KeyEvent.KEYCODE_ENTER) ||
                (actionId == EditorInfo.IME_ACTION_DONE)) {
                Team team = getSelectedTeam();
                currentState.setCurrentTeam(team);

                if (team != null) {
                    // hide soft keyboard
                    InputMethodManager imm =
                            (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(lvHistory.getApplicationWindowToken(), 0);

                    startInputDataActivity();
                } else {
                    String message =
                            HistoryActivity.this.getResources().getString(R.string.input_history_team_not_found);
                    message = message.replace("${teamNumber}", editTeamNumber.getText().toString());
                    Toast.makeText(HistoryActivity.this, message, Toast.LENGTH_SHORT).show();
                    editTeamNumber.setText("");

                    InputMethodManager imm =
                            (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
            }

            if (actionId == EditorInfo.IME_ACTION_NONE
                && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                clearSelectedTeam();
            }

            return false;
        }
    }
}
