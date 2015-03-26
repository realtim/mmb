package ru.mmb.datacollector.activity.input.bclogger.select;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.StateChangeListener;
import ru.mmb.datacollector.activity.input.bclogger.ActivityStateWithScanPointAndLogger;
import ru.mmb.datacollector.bluetooth.LoggerInfo;
import ru.mmb.datacollector.model.registry.Settings;

import static ru.mmb.datacollector.activity.Constants.KEY_CURRENT_LOGGER_INFO;

/*
 * To start this activity bluetooth adapter MUST be enabled.
 * Check enabled state in calling activity.
 */
public class SelectBCLoggerActivity extends Activity implements StateChangeListener {
    private ActivityStateWithScanPointAndLogger currentState;

    private Map<String, LoggerInfo> pairedLoggers;
    private List<String> loggerNames;

    private Spinner inputBCLogger;
    private Button btnOk;

    private String prevSelectedLogger = null;
    private String currSelectedLogger = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getInstance().setCurrentContext(this);

        loadPairedLoggersList();
        buildLoggerNamesArray();

        currentState = new ActivityStateWithScanPointAndLogger("input.bclogger.select");
        currentState.initialize(this, savedInstanceState);

        setContentView(R.layout.input_bclogger_select);

        inputBCLogger = (Spinner) findViewById(R.id.inputBCLoggerSelect_loggerInput);
        btnOk = (Button) findViewById(R.id.inputBCLoggerSelect_okBtn);

        setInputBCLoggerAdapter();

        inputBCLogger.setOnItemSelectedListener(new InputBCLoggerOnItemSelectedListener());
        btnOk.setOnClickListener(new OkBtnClickListener());

        initializeControls();

        currentState.addStateChangeListener(this);
        onStateChange();
    }

    private void loadPairedLoggersList() {
        pairedLoggers = new TreeMap<String, LoggerInfo>();
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> devices = btAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            if (device.getName().contains("LOGGER")) {
                pairedLoggers.put(device.getName(), new LoggerInfo(device.getName(), device.getAddress()));
            }
        }
    }

    private void buildLoggerNamesArray() {
        loggerNames = new ArrayList<String>();
        for (String loggerName : pairedLoggers.keySet()) {
            loggerNames.add(loggerName);
        }
    }

    private void setInputBCLoggerAdapter() {
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, loggerNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        inputBCLogger.setAdapter(adapter);
    }

    private void initializeControls() {
        refreshInputBCLoggerState();

        if (currentState.getCurrentLoggerInfo() == null)
            currentState.setCurrentLoggerInfo(pairedLoggers.get(loggerNames.get(0)));
        LoggerInfo currentLoggerInfo = currentState.getCurrentLoggerInfo();
        setInitialLoggerName(currentLoggerInfo);
    }

    private void refreshInputBCLoggerState() {
        if (currentState.getCurrentLoggerInfo() == null)
        {
            inputBCLogger.setSelection(0);
        }
        else
        {
            int pos = loggerNames.indexOf(currentState.getCurrentLoggerInfo().getLoggerName());
            if (pos == -1) pos = 0;
            inputBCLogger.setSelection(pos);
        }
    }

    private void setInitialLoggerName(LoggerInfo currentLoggerInfo) {
        currSelectedLogger = currentLoggerInfo.getLoggerName();
        prevSelectedLogger = currSelectedLogger;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        currentState.save(outState);
    }

    @Override
    public void onStateChange()
    {
        btnOk.setEnabled(currentState.isLoggerSelected());
    }

    private class InputBCLoggerOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            currSelectedLogger = loggerNames.get(pos);
            if (currSelectedLogger.equals(prevSelectedLogger)) {
                prevSelectedLogger = currSelectedLogger;
                currentState.setCurrentLoggerInfo(pairedLoggers.get(currSelectedLogger));
            }
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void onNothingSelected(AdapterView parent) {
            // Do nothing.
        }
    }

    private class OkBtnClickListener implements View.OnClickListener
    {
        @Override
        public void onClick(View v)
        {
            Intent resultData = new Intent();
            if (currentState.getCurrentLoggerInfo() != null)
                resultData.putExtra(KEY_CURRENT_LOGGER_INFO, currentState.getCurrentLoggerInfo());
            setResult(RESULT_OK, resultData);
            finish();
        }
    }
}
