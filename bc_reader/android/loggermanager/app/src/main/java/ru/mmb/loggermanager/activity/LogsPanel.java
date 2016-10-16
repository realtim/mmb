package ru.mmb.loggermanager.activity;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.filedialog.FileDialog;
import com.filedialog.SelectionMode;

import java.io.File;

import ru.mmb.loggermanager.R;
import ru.mmb.loggermanager.conf.Configuration;

import static ru.mmb.loggermanager.activity.Constants.REQUEST_CODE_SAVE_DIR_DIALOG;

public class LogsPanel {

    private final MainActivity owner;

    private Button selectSaveDirButton;
    private TextView saveDirLabel;
    private Button getLogButton;
    private Button getDebugButton;

    private EditText getLogLineEdit;
    private Button getLogLineButton;
    private EditText getDebugLineEdit;
    private Button getDebugLineButton;
    private Button clearDeviceButton;

    public LogsPanel(MainActivity owner) {
        this.owner = owner;
        initialize();
    }

    private void initialize() {
        selectSaveDirButton = (Button) owner.findViewById(R.id.main_selectSaveDirButton);
        selectSaveDirButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(owner.getBaseContext(), FileDialog.class);
                String startPath = extractStartPath();
                if (startPath != null) {
                    intent.putExtra(FileDialog.START_PATH, startPath);
                }
                intent.putExtra(FileDialog.CAN_SELECT_DIR, true);
                intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
                owner.startActivityForResult(intent, REQUEST_CODE_SAVE_DIR_DIALOG);
            }
        });

        saveDirLabel = (TextView) owner.findViewById(R.id.main_saveDirLabel);
        String saveDir = Configuration.getInstance().getSaveDir();
        if (saveDir != null) {
            saveDirLabel.setText(saveDir);
        }

        getLogButton = (Button) owner.findViewById(R.id.main_getLogButton);
        getLogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                owner.loadLogsFromLogger();
            }
        });

        getDebugButton = (Button) owner.findViewById(R.id.main_getDebugButton);

        getDebugButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                owner.loadDebugFromLogger();
            }
        });

        getLogLineEdit = (EditText) owner.findViewById(R.id.main_getLogLineEditText);
        getDebugLineEdit = (EditText) owner.findViewById(R.id.main_getDebugLineEditText);

        getLogLineButton = (Button) owner.findViewById(R.id.main_getLogLineButton);
        getLogLineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String lineNumber = getLogLineEdit.getText().toString();
                owner.sendLogsCommand("GET#L" + lineNumber + "\n");
            }
        });

        getDebugLineButton = (Button) owner.findViewById(R.id.main_getDebugLineButton);
        getDebugLineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String lineNumber = getDebugLineEdit.getText().toString();
                owner.sendLogsCommand("GET#D" + lineNumber + "\n");
            }
        });

        clearDeviceButton = (Button) owner.findViewById(R.id.main_clearDeviceButton);
        clearDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                owner.sendLogsCommand("DELLOG\n");
            }
        });
        clearDeviceButton.setEnabled(false);
    }

    private String extractStartPath() {
        String result = null;
        String prevSaveDir = saveDirLabel.getText().toString();
        if (!isEmpty(prevSaveDir)) {
            File saveDir = new File(prevSaveDir);
            File saveDirParent = new File(saveDir.getParent());
            if (saveDirParent.exists()) {
                result = saveDirParent.getPath();
            }
        }
        return result;
    }

    public void setControlsEnabled(boolean value) {
        selectSaveDirButton.setEnabled(value);
        saveDirLabel.setEnabled(value);
        if (Configuration.getInstance().isAdminMode()) {
            clearDeviceButton.setEnabled(value);
        } else {
            clearDeviceButton.setEnabled(false);
        }
        getLogLineEdit.setEnabled(value);
        getDebugLineEdit.setEnabled(value);
        getLogLineButton.setEnabled(value);
        getDebugLineButton.setEnabled(value);

        boolean enableSaveActions = value && !isEmpty(Configuration.getInstance().getSaveDir());
        getLogButton.setEnabled(enableSaveActions);
        getDebugButton.setEnabled(enableSaveActions);
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().length() == 0;
    }

    public void changeSaveDir(String saveDir) {
        File saveDirFile = new File(saveDir);
        if (!saveDirFile.exists() || !saveDirFile.isDirectory()) {
            setControlsEnabled(false);
            String message = owner.getResources().getString(R.string.main_save_dir_error);
            Toast.makeText(owner, message.replace("${value}", saveDir), Toast.LENGTH_SHORT).show();
        } else {
            saveDirLabel.setText(saveDir);
            Configuration.getInstance().setSaveDir(owner, saveDir);
            setControlsEnabled(true);
        }
    }
}
