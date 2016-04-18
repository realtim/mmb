package ru.mmb.loggermanager.activity;

import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import ru.mmb.loggermanager.R;

public class SettingsPanel {

    private final MainActivity owner;

    private EditText loggerIdEdit;
    private EditText scanpointEdit;
    private EditText patternEdit;
    private CheckBox checkLengthCheckBox;
    private CheckBox onlyDigitsCheckBox;
    private TextView loggerTimeLabel;

    private Button reloadSettingsButton;

    private Button updateLoggerIdButton;
    private Button updateScanpointButton;
    private Button updatePatternButton;
    private Button updateCheckLengthButton;
    private Button updateOnlyDigitsButton;
    private Button updateLoggerTimeButton;

    public SettingsPanel(MainActivity owner) {
        this.owner = owner;
        initialize();
    }

    private void initialize() {
        loggerIdEdit = (EditText) owner.findViewById(R.id.main_loggerIdEditText);
        scanpointEdit = (EditText) owner.findViewById(R.id.main_scanpointEditText);
        patternEdit = (EditText) owner.findViewById(R.id.main_patternEditText);
        checkLengthCheckBox = (CheckBox) owner.findViewById(R.id.main_checkLengthCheckBox);
        onlyDigitsCheckBox = (CheckBox) owner.findViewById(R.id.main_onlyDigitsCheckBox);
        loggerTimeLabel = (TextView) owner.findViewById(R.id.main_loggerTimeLabel);

        reloadSettingsButton = (Button) owner.findViewById(R.id.main_reloadSettingsButton);
        reloadSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                owner.reloadSelectedLoggerSettings();
            }
        });

        updateLoggerIdButton = (Button) owner.findViewById(R.id.main_updateLoggerIdButton);
        updateLoggerIdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String loggerId = loggerIdEdit.getText().toString();
                if (!validateTwoDigitsNumber(loggerId)) {
                    showValidationError(
                            owner.getResources().getString(R.string.main_two_digits_error),
                            loggerId);
                    return;
                }
                int loggerIdInt = Integer.parseInt(loggerId);
                owner.sendLoggerSettingsCommand("SETI" + String.format("%02d", loggerIdInt) + "\n");
            }
        });

        updateScanpointButton = (Button) owner.findViewById(R.id.main_updateScanpointButton);
        updateScanpointButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String scanpointNum = scanpointEdit.getText().toString();
                if (!validateTwoDigitsNumber(scanpointNum)) {
                    showValidationError(
                            owner.getResources().getString(R.string.main_two_digits_error),
                            scanpointNum);
                    return;
                }
                int scanpointIdInt = Integer.parseInt(scanpointNum);
                owner.sendLoggerSettingsCommand("SETC" + String.format("%02d", scanpointIdInt) + "\n");
            }
        });

        updatePatternButton = (Button) owner.findViewById(R.id.main_updatePatternButton);
        updatePatternButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pattern = patternEdit.getText().toString();
                if (pattern.length() != 8) {
                    showValidationError(
                            owner.getResources().getString(R.string.main_pattern_error),
                            pattern);
                    return;
                }
                owner.sendLoggerSettingsCommand("SETP" + pattern + "\n");
            }
        });

        updateCheckLengthButton = (Button) owner.findViewById(R.id.main_updateCheckLengthButton);
        updateCheckLengthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String checkLength = checkLengthCheckBox.isChecked() ? "Y" : "N";
                owner.sendLoggerSettingsCommand("SETL" + checkLength + "\n");
            }
        });

        updateOnlyDigitsButton = (Button) owner.findViewById(R.id.main_updateOnlyDigitsButton);
        updateOnlyDigitsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String onlyDigits = onlyDigitsCheckBox.isChecked() ? "Y" : "N";
                owner.sendLoggerSettingsCommand("SETN" + onlyDigits + "\n");
            }
        });

        updateLoggerTimeButton = (Button) owner.findViewById(R.id.main_updateLoggerTimeButton);
        updateLoggerTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                owner.updateLoggerTime();
            }
        });
    }

    private void showValidationError(String message, String value) {
        Toast.makeText(owner, message.replace("${value}", value), Toast.LENGTH_SHORT).show();
    }

    private boolean validateTwoDigitsNumber(String value) {
        if (value == null) {
            return false;
        }
        if (value.length() > 2) {
            return false;
        }
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public void clearControls() {
        loggerIdEdit.setText("");
        scanpointEdit.setText("");
        patternEdit.setText("");
        checkLengthCheckBox.setChecked(false);
        onlyDigitsCheckBox.setChecked(false);
        loggerTimeLabel.setText("");
    }

    public void setControlsEnabled(boolean value) {
        loggerIdEdit.setEnabled(value);
        scanpointEdit.setEnabled(value);
        patternEdit.setEnabled(value);
        checkLengthCheckBox.setEnabled(value);
        onlyDigitsCheckBox.setEnabled(value);
        loggerTimeLabel.setEnabled(value);

        reloadSettingsButton.setEnabled(value);

        updateLoggerIdButton.setEnabled(value);
        updateScanpointButton.setEnabled(value);
        updatePatternButton.setEnabled(value);
        updateCheckLengthButton.setEnabled(value);
        updateOnlyDigitsButton.setEnabled(value);
        updateLoggerTimeButton.setEnabled(value);
    }

    public void updateLoggerSettings(LoggerSettings loggerSettings) {
        loggerIdEdit.setText(loggerSettings.getLoggerId());
        scanpointEdit.setText(loggerSettings.getScanpointId());
        patternEdit.setText(loggerSettings.getPattern());
        checkLengthCheckBox.setChecked(loggerSettings.isCheckLength());
        onlyDigitsCheckBox.setChecked(loggerSettings.isOnlyDigits());
        loggerTimeLabel.setText(loggerSettings.getLoggerTime());
    }
}
