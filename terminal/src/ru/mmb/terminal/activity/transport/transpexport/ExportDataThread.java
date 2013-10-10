package ru.mmb.terminal.activity.transport.transpexport;

import ru.mmb.terminal.transport.exporter.ExportMode;
import ru.mmb.terminal.transport.exporter.ExportState;
import ru.mmb.terminal.transport.exporter.data.DataExporter;
import android.os.Handler;

public class ExportDataThread extends ExportThread
{
	public ExportDataThread(TransportExportActivity activity, Handler finishHandler, ExportMode exportMode, ExportState exportState)
	{
		super(activity, finishHandler, exportMode, exportState);
	}

	@Override
	protected String exportData() throws Exception
	{
		return new DataExporter(getExportMode(), getExportState()).exportData();
	}
}
