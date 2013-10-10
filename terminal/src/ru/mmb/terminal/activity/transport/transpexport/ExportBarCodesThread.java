package ru.mmb.terminal.activity.transport.transpexport;

import ru.mmb.terminal.transport.exporter.ExportMode;
import ru.mmb.terminal.transport.exporter.ExportState;
import ru.mmb.terminal.transport.exporter.barcode.BarCodeExporter;
import android.os.Handler;

public class ExportBarCodesThread extends ExportThread
{
	private final int levelPointId;

	public ExportBarCodesThread(TransportExportActivity activity, Handler finishHandler, ExportMode exportMode, ExportState exportState)
	{
		super(activity, finishHandler, exportMode, exportState);
		this.levelPointId = activity.getCurrentState().getCurrentLevelPoint().getLevelPointId();
	}

	@Override
	protected String exportData() throws Exception
	{
		return new BarCodeExporter(getExportMode(), getExportState(), levelPointId).exportData();
	}
}
