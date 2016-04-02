package ru.mmb.datacollector.activity.transport.transpexport;

import android.os.Handler;

import ru.mmb.datacollector.transport.exporter.ExportFormat;
import ru.mmb.datacollector.transport.exporter.ExportState;
import ru.mmb.datacollector.transport.exporter.data.DataExporter;

public class ExportDataThread extends ExportThread {
    public ExportDataThread(TransportExportActivity activity, Handler finishHandler, ExportState exportState, ExportFormat exportFormat) {
        super(activity, finishHandler, exportState, exportFormat);
    }

    @Override
    protected String exportData() throws Exception {
        return new DataExporter(getExportState(), getExportFormat()).exportData();
    }
}
