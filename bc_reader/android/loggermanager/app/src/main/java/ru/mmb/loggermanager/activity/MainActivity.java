package ru.mmb.loggermanager.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

import ru.mmb.loggermanager.R;
import ru.mmb.loggermanager.widget.ConsoleMessagesAppender;

public class MainActivity extends BluetoothAdapterEnableActivity {

    private LinearLayout globalContainerPanel;

    private SelectLoggerPanel selectLoggerPanel;
    private DeviceInfo selectedLogger = null;

    private ToggleButton panelsToggle;
    private ViewFlipper panelsFlipper;

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
        reloadSelectedLoggerSettings();
        setControlsEnabled(true);
    }

    private void reloadSelectedLoggerSettings() {
    }

    public void selectedLoggerChanged(DeviceInfo selectedLogger) {
        if (selectedLogger == null) {
            setControlsEnabled(false);
        } else {
            this.selectedLogger = selectedLogger;
            refreshState();
        }
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
