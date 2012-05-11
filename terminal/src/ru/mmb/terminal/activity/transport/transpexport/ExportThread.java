package ru.mmb.terminal.activity.transport.transpexport;

import static ru.mmb.terminal.activity.Constants.KEY_EXPORT_RESULT_MESSAGE;
import ru.mmb.terminal.R;
import ru.mmb.terminal.transport.exporter.ExportMode;
import ru.mmb.terminal.transport.exporter.ExportState;
import ru.mmb.terminal.transport.exporter.Exporter;
import android.os.Bundle;
import android.os.Message;

public class ExportThread extends Thread
{
	private final TransportExportActivity activity;
	private final ExportMode exportMode;
	private final ExportState exportState;

	public ExportThread(TransportExportActivity activity, ExportMode exportMode, ExportState exportState)
	{
		super();
		this.activity = activity;
		this.exportMode = exportMode;
		this.exportState = exportState;
	}

	@Override
	public void run()
	{
		boolean wasError = false;
		String errorMessage = "";
		String fileName = "";
		try
		{
			fileName = new Exporter(exportMode, exportState).exportData();
		}
		catch (Exception e)
		{
			wasError = true;
			errorMessage = e.getClass().getSimpleName() + " - " + e.getMessage();
			e.printStackTrace();
		}

		activity.getExportFinishHandler().sendMessage(prepareResultMessage(wasError, errorMessage, fileName));
	}

	private Message prepareResultMessage(boolean wasError, String errorMessage, String fileName)
	{
		Message msg = new Message();
		Bundle messageBundle = new Bundle();
		messageBundle.putString(KEY_EXPORT_RESULT_MESSAGE, getResultMessageString(wasError, errorMessage, fileName));
		msg.setData(messageBundle);
		return msg;
	}

	private String getResultMessageString(boolean wasError, String errorMessage, String fileName)
	{
		if (!wasError)
		{
			return activity.getResources().getString(R.string.transp_export_success) + "\n"
			        + fileName;
		}
		else
		{
			return activity.getResources().getString(R.string.transp_export_error) + "\n"
			        + errorMessage;
		}
	}
}
