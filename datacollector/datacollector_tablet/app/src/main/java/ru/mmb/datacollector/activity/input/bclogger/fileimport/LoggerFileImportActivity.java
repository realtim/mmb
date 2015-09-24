package ru.mmb.datacollector.activity.input.bclogger.fileimport;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.filedialog.FileDialog;
import com.filedialog.SelectionMode;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.bluetooth.ThreadMessageTypes;
import ru.mmb.datacollector.db.SQLiteDatabaseAdapter;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.widget.ConsoleMessagesAppender;

import static ru.mmb.datacollector.activity.Constants.REQUEST_CODE_FILE_DIALOG;
import static ru.mmb.datacollector.activity.input.bclogger.fileimport.LoggerFileImportActivityState.STATE_FILE_SELECTED;
import static ru.mmb.datacollector.activity.input.bclogger.fileimport.LoggerFileImportActivityState.STATE_IMPORT_RUNNING;
import static ru.mmb.datacollector.activity.input.bclogger.fileimport.LoggerFileImportActivityState.STATE_NO_FILE_SELECTED;

public class LoggerFileImportActivity extends Activity {
    protected LoggerFileImportActivityState currentState;

    protected Button btnSelectFile;
    protected TextView labFileName;
    private Button btnImportFile;
    private Button btnClearConsole;
    private TextView areaConsole;

    private ConsoleMessagesAppender consoleAppender;
    private Handler fileImportHandler;

    private LoggerFileImportRunner importRunner;
    private Thread importThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getInstance().setCurrentContext(this);

        currentState = new LoggerFileImportActivityState();
        currentState.initialize(this, savedInstanceState);

        setContentView(R.layout.input_bclogger_fileimport);

        btnSelectFile = (Button) findViewById(R.id.inputBCLoggerFileImport_selectFile);
        labFileName = (TextView) findViewById(R.id.inputBCLoggerFileImport_fileName);
        btnImportFile = (Button) findViewById(R.id.inputBCLoggerFileImport_importFileButton);
        btnClearConsole = (Button) findViewById(R.id.inputBCLoggerFileImport_clearConsoleButton);
        areaConsole = (TextView) findViewById(R.id.inputBCLoggerFileImport_consoleTextView);

        btnSelectFile.setOnClickListener(new SelectFileClickListener());
        btnImportFile.setOnClickListener(new ImportFileClickListener());
        btnClearConsole.setOnClickListener(new ClearConsoleClickListener());

        consoleAppender = new ConsoleMessagesAppender(areaConsole);
        fileImportHandler = new FileImportHandler(this, consoleAppender);

        setTitle(currentState.getCurrentScanPoint().getScanPointName());

        refreshState();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_FILE_DIALOG:
                if (resultCode == Activity.RESULT_OK) {
                    String fileName = data.getStringExtra(FileDialog.RESULT_PATH);
                    currentState.setFileName(fileName);
                    currentState.setState(STATE_FILE_SELECTED);
                } else {
                    currentState.setFileName(null);
                    currentState.setState(STATE_NO_FILE_SELECTED);
                }
                refreshState();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void refreshFileName() {
        if (currentState.getFileName() == null) {
            labFileName.setText(getResources().getString(R.string.input_bclogger_fileimport_no_file_selected));
        } else {
            labFileName.setText(currentState.getFileName());
        }
    }

    private void refreshState() {
        refreshFileName();
        btnSelectFile.setEnabled(currentState.getState() != STATE_IMPORT_RUNNING);
        btnImportFile.setEnabled(currentState.getState() == STATE_FILE_SELECTED);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (importThread != null) {
            importRunner.terminate();
            importThread.interrupt();
            importThread = null;
        }
    }

    private class SelectFileClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getBaseContext(), FileDialog.class);
            intent.putExtra(FileDialog.START_PATH, Settings.getInstance().getDatalogDir());
            intent.putExtra(FileDialog.CAN_SELECT_DIR, false);
            intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
            intent.putExtra(FileDialog.FORMAT_FILTER, new String[]{".txt"});
            startActivityForResult(intent, REQUEST_CODE_FILE_DIALOG);
        }
    }

    private class ImportFileClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            currentState.setState(STATE_IMPORT_RUNNING);
            refreshState();

            // backup database before logger data import
            SQLiteDatabaseAdapter dbAdapter = SQLiteDatabaseAdapter.getConnectedInstance();
            dbAdapter.backupDatabase(LoggerFileImportActivity.this);

            importRunner = new LoggerFileImportRunner(currentState.getFileName(), fileImportHandler);
            importThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    importRunner.importFile();
                }
            });
            importThread.start();
        }
    }

    private class ClearConsoleClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            consoleAppender.clear();
        }
    }

    private static class FileImportHandler extends Handler {
        private final LoggerFileImportActivity owner;
        private final ConsoleMessagesAppender consoleAppender;

        private FileImportHandler(LoggerFileImportActivity owner, ConsoleMessagesAppender consoleAppender) {
            super();
            this.owner = owner;
            this.consoleAppender = consoleAppender;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ThreadMessageTypes.MSG_CONSOLE) {
                consoleAppender.appendMessage((String) msg.obj);
            } else if (msg.what == ThreadMessageTypes.MSG_FINISHED_SUCCESS ||
                    msg.what == ThreadMessageTypes.MSG_FINISHED_ERROR) {
                owner.importThread = null;
                owner.currentState.setState(STATE_NO_FILE_SELECTED);
                owner.currentState.setFileName(null);

                // backup database after successful logger data import
                if (msg.what == ThreadMessageTypes.MSG_FINISHED_SUCCESS) {
                    SQLiteDatabaseAdapter dbAdapter = SQLiteDatabaseAdapter.getConnectedInstance();
                    dbAdapter.backupDatabase(owner);
                }

                owner.refreshState();
            }
        }
    }
}
