package ru.mmb.datacollector.activity.transport.transpexport;

import ru.mmb.datacollector.transport.exporter.ExportFormat;
import ru.mmb.datacollector.transport.exporter.ExportMode;
import ru.mmb.datacollector.transport.exporter.ExportState;
import ru.mmb.datacollector.transport.exporter.data.DataExporter;
import android.os.Handler;

public class ExportDataThread extends ExportThread
{
	public ExportDataThread(TransportExportActivity activity, Handler finishHandler, ExportMode exportMode, ExportState exportState, ExportFormat exportFormat)
	{
		super(activity, finishHandler, exportMode, exportState, exportFormat);
	}

	@Override
	protected String exportData() throws Exception
	{
		return new DataExporter(getExportMode(), getExportState(), getExportFormat()).exportData();
	}
}
