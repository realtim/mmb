package ru.mmb.loggermanager.activity;

import android.view.KeyEvent;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.mmb.loggermanager.R;
import ru.mmb.loggermanager.bluetooth.DeviceInfo;
import ru.mmb.loggermanager.bluetooth.DevicesLoader;
import ru.mmb.loggermanager.conf.Configuration;

public class AutoUpdatePanel {
    private final MainActivity owner;

    private CheckBox autoUpdateTimeCheck;
    private EditText updatePeriodEdit;
    private TableLayout loggersSelectPanel;

    private List<DeviceInfo> loggers;
    private Map<CheckBox, DeviceInfo> checkBoxLoggers = new HashMap<CheckBox, DeviceInfo>();
    private Map<DeviceInfo, CheckBox> loggerCheckBoxes = new HashMap<DeviceInfo, CheckBox>();

    public AutoUpdatePanel(MainActivity owner) {
        this.owner = owner;
        initialize();
    }

    private void initialize() {
        autoUpdateTimeCheck = (CheckBox) owner.findViewById(R.id.main_autoUpdateTimeCheckBox);
        autoUpdateTimeCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (autoUpdateTimeCheck.isChecked()) {
                    owner.startTimeUpdaterThread();
                } else {
                    owner.stopTimeUpdaterThread();
                }
            }
        });

        updatePeriodEdit = (EditText) owner.findViewById(R.id.main_updatePeriodEditText);
        int updatePeriod = Configuration.getInstance().getUpdatePeriodMinutes();
        updatePeriodEdit.setText(Integer.toString(updatePeriod));

        updatePeriodEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String editorText = updatePeriodEdit.getText().toString();
                int newPeriod = 0;
                try {
                    newPeriod = Integer.parseInt(editorText);
                } catch (Exception e) {

                }
                if (newPeriod > 0) {
                    owner.writeToConsole("setting update period (min): " + newPeriod);
                    Configuration.getInstance().setUpdatePeriodMinutes(owner, newPeriod);
                }
                return false;
            }
        });

        loggers = DevicesLoader.loadPairedDevices();
        loggersSelectPanel = (TableLayout) owner.findViewById(R.id.main_loggersSelectPanel);
        initializeLoggerCheckBoxes();
    }

    private void initializeLoggerCheckBoxes() {
        int loggersCount = loggers.size();
        // create 2 columns of check boxes
        int rowCount = (loggersCount % 2 == 0) ? loggersCount / 2 : loggersCount / 2 + 1;

        List<TableRow> rows = new ArrayList<TableRow>();
        for (int i = 0; i < rowCount; i++) {
            TableRow tableRow = new TableRow(owner);
            rows.add(tableRow);
            loggersSelectPanel.addView(tableRow);
        }

        buildLoggersColumn(0, rows);
        buildLoggersColumn(1, rows);
    }

    private void buildLoggersColumn(int colIndex, List<TableRow> rows) {
        int lastLoggerIndex = (colIndex == 0) ? rows.size() - 1 : loggers.size() / 2 - 1;
        for (int rowIndex = 0; rowIndex <= lastLoggerIndex; rowIndex++) {
            createLoggerCheckBox(colIndex, rowIndex, rows);
        }
    }

    private void createLoggerCheckBox(int colIndex, int rowIndex, List<TableRow> rows) {
        DeviceInfo logger = loggers.get(rows.size() * colIndex + rowIndex);
        CheckBox loggerCheckBox = new CheckBox(owner);
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams();
        layoutParams.weight = 1;
        loggerCheckBox.setLayoutParams(layoutParams);
        loggerCheckBox.setText(logger.getDeviceName());
        loggerCheckBox.setChecked(isUpdateLogger(logger));
        loggerCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DeviceInfo logger = checkBoxLoggers.get(buttonView);
                Configuration.getInstance().changeLoggerState(owner, logger.getDeviceName(), isChecked);
            }
        });
        TableRow tableRow = rows.get(rowIndex);
        tableRow.addView(loggerCheckBox);
        checkBoxLoggers.put(loggerCheckBox, logger);
        loggerCheckBoxes.put(logger, loggerCheckBox);
    }

    public boolean isUpdateLogger(DeviceInfo logger) {
        return Configuration.getInstance().getUpdateLoggers().contains(logger.getDeviceName());
    }
}
