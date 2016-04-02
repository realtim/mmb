package ru.mmb.datacollector.activity.transport.transpexport;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.transport.exporter.ExportFormat;
import ru.mmb.datacollector.transport.exporter.ExportState;

import static ru.mmb.datacollector.activity.Constants.KEY_EXPORT_RESULT_MESSAGE;

public abstract class ExportThread extends Thread {
    private final TransportExportActivity activity;
    private final ExportState exportState;
    private final ExportFormat exportFormat;
    private final Handler finishHandler;

    protected abstract String exportData() throws Exception;

    public ExportThread(TransportExportActivity activity, Handler finishHandler, ExportState exportState, ExportFormat exportFormat) {
        super();
        this.activity = activity;
        this.finishHandler = finishHandler;
        this.exportState = exportState;
        this.exportFormat = exportFormat;
    }

    public ExportState getExportState() {
        return exportState;
    }

    public TransportExportActivity getActivity() {
        return activity;
    }

    @Override
    public void run() {
        boolean wasError = false;
        String errorMessage = "";
        String fileName = "";
        try {
            fileName = exportData();
        } catch (Exception e) {
            wasError = true;
            errorMessage = e.getClass().getSimpleName() + " - " + e.getMessage();
            e.printStackTrace();
        }

        finishHandler.sendMessage(prepareResultMessage(wasError, errorMessage, fileName));
    }

    private Message prepareResultMessage(boolean wasError, String errorMessage, String fileName) {
        Message msg = new Message();
        Bundle messageBundle = new Bundle();
        messageBundle.putString(KEY_EXPORT_RESULT_MESSAGE, getResultMessageString(wasError, errorMessage, fileName));
        msg.setData(messageBundle);
        return msg;
    }

    private String getResultMessageString(boolean wasError, String errorMessage, String fileName) {
        if (!wasError) {
            return activity.getResources().getString(R.string.transp_export_success) + "\n"
                    + fileName;
        } else {
            return activity.getResources().getString(R.string.transp_export_error) + "\n"
                    + errorMessage;
        }
    }

    public ExportFormat getExportFormat() {
        return exportFormat;
    }
}
