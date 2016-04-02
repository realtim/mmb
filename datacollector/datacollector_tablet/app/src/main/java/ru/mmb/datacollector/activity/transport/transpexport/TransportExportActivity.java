package ru.mmb.datacollector.activity.transport.transpexport;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.transport.exporter.ExportFormat;
import ru.mmb.datacollector.transport.exporter.ExportState;

import static ru.mmb.datacollector.activity.Constants.KEY_EXPORT_RESULT_MESSAGE;

public class TransportExportActivity extends Activity {
    private LinearLayout progressBarPanel;
    private Button btnFullExport;
    private ExportState exportState = null;

    private Handler exportFinishHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getInstance().setCurrentContext(this);

        setContentView(R.layout.transp_export);

        progressBarPanel = (LinearLayout) findViewById(R.id.transpExport_progressBarPanel);
        btnFullExport = (Button) findViewById(R.id.transpExportData_fullExportBtn);

        btnFullExport.setOnClickListener(new FullExportClickListener());

        exportFinishHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (getExportState() != null && !getExportState().isTerminated()) {
                    String resultMessage = msg.getData().getString(KEY_EXPORT_RESULT_MESSAGE);
                    Toast.makeText(getApplicationContext(), resultMessage, Toast.LENGTH_LONG).show();
                }
                setExportState(null);
                refreshState();
            }
        };

        setTitle(getResources().getString(R.string.transp_export_title));

        refreshState();
    }

    public void refreshState() {
        btnFullExport.setEnabled(getExportState() == null);
        refreshProgressBarPanelVisible();
    }

    private void refreshProgressBarPanelVisible() {
        if (exportState != null) {
            progressBarPanel.setVisibility(View.VISIBLE);
        } else {
            progressBarPanel.setVisibility(View.GONE);
        }
    }

    public ExportState getExportState() {
        return exportState;
    }

    public void setExportState(ExportState exportState) {
        this.exportState = exportState;
    }

    private void runExport() {
        setExportState(new ExportState());
        refreshState();

        ExportDataThread thread =
                new ExportDataThread(this, exportFinishHandler, getExportState(), ExportFormat.TXT_TO_SITE);
        thread.start();
    }

    private void terminateExport() {
        if (exportState != null) {
            exportState.setTerminated(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        terminateExport();
    }

    @Override
    protected void onStop() {
        super.onStop();
        terminateExport();
    }

    private class FullExportClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            runExport();
        }
    }
}
