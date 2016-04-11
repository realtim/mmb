package ru.mmb.loggermanager.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

import ru.mmb.loggermanager.R;
import ru.mmb.loggermanager.bluetooth.BluetoothAdapterEnableActivity;
import ru.mmb.loggermanager.bluetooth.DeviceInfo;
import ru.mmb.loggermanager.widget.ConsoleMessagesAppender;

public class MainActivity extends BluetoothAdapterEnableActivity {

    private LinearLayout globalContainerPanel;

    private SelectLoggerPanel selectLoggerPanel;
    private DeviceInfo selectedLogger = null;

    private ToggleButton panelsToggle;
    private ViewFlipper panelsFlipper;

    private EditText loggerIdEdit;
    private EditText scanpointEdit;
    private EditText patternEdit;
    private CheckBox checkLengthCheckBox;
    private CheckBox onlyDigitsCheckBox;
    private TextView loggerTimeLabel;

    private TextView consoleTextView;
    private ConsoleMessagesAppender consoleAppender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        globalContainerPanel = (LinearLayout) findViewById(R.id.main_globalContainerPanel);
        panelsToggle = (ToggleButton) findViewById(R.id.main_switchPanelsToggle);
        panelsToggle.setOnClickListener(new PanelsSwitchListener());
        panelsFlipper = (ViewFlipper) findViewById(R.id.main_panelsFlipper);

        selectLoggerPanel = new SelectLoggerPanel(this);

        loggerIdEdit = (EditText) findViewById(R.id.main_loggerIdEditText);
        scanpointEdit = (EditText) findViewById(R.id.main_scanpointEditText);
        patternEdit = (EditText) findViewById(R.id.main_patternEditText);
        checkLengthCheckBox = (CheckBox) findViewById(R.id.main_checkLengthCheckBox);
        onlyDigitsCheckBox = (CheckBox) findViewById(R.id.main_onlyDigitsCheckBox);
        loggerTimeLabel = (TextView) findViewById(R.id.main_loggerTimeLabel);

        consoleTextView = (TextView) findViewById(R.id.main_consoleTextView);
        consoleAppender = new ConsoleMessagesAppender(consoleTextView);
    }

    @Override
    protected void onAdapterStateChanged() {
        refreshState();
    }

    private void refreshState() {
        if (!isAdapterEnabled()) {
            globalContainerPanel.setEnabled(false);
            return;
        } else if (!globalContainerPanel.isEnabled()) {
            globalContainerPanel.setEnabled(true);
        }

        setControlsEnabled(false);
        clearControls();
        if (selectedLogger != null) {
            reloadSelectedLoggerSettings();
            setControlsEnabled(true);
        }
    }

    private void clearControls() {
        loggerIdEdit.setText("");
        scanpointEdit.setText("");
        patternEdit.setText("");
        checkLengthCheckBox.setChecked(false);
        onlyDigitsCheckBox.setChecked(false);
        loggerTimeLabel.setText("");
    }

    private void reloadSelectedLoggerSettings() {

    }

    public void selectedLoggerChanged(DeviceInfo selectedLogger) {
        this.selectedLogger = selectedLogger;
        refreshState();
    }

    private void setControlsEnabled(boolean value) {
        panelsToggle.setEnabled(value);
        panelsFlipper.setEnabled(value);
    }

    private class PanelsSwitchListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (panelsToggle.isChecked()) {
                panelsFlipper.showNext();
            } else {
                panelsFlipper.showPrevious();
            }
        }
    }
}
