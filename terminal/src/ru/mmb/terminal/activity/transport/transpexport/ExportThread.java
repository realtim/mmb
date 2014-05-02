package ru.mmb.terminal.activity.transport.transpexport;

import static ru.mmb.terminal.activity.Constants.KEY_EXPORT_RESULT_MESSAGE;
import ru.mmb.terminal.R;
import ru.mmb.terminal.transport.exporter.ExportFormat;
import ru.mmb.terminal.transport.exporter.ExportMode;
import ru.mmb.terminal.transport.exporter.ExportState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public abstract class ExportThread extends Thread
{
	private final TransportExportActivity activity;
	private final ExportMode exportMode;
	private final ExportState exportState;
	private final ExportFormat exportFormat;
	private final Handler finishHandler;

	protected abstract String exportData() throws Exception;

	public ExportThread(TransportExportActivity activity, Handler finishHandler, ExportMode exportMode, ExportState exportState, ExportFormat exportFormat)
	{
		super();
		this.activity = activity;
		this.finishHandler = finishHandler;
		this.exportMode = exportMode;
		this.exportState = exportState;
		this.exportFormat = exportFormat;
	}

	public ExportMode getExportMode()
	{
		return exportMode;
	}

	public ExportState getExportState()
	{
		return exportState;
	}

	public TransportExportActivity getActivity()
	{
		return activity;
	}

	@Override
	public void run()
	{
		boolean wasError = false;
		String errorMessage = "";
		String fileName = "";
		try
		{
			fileName = exportData();
		}
		catch (Exception e)
		{
			wasError = true;
			errorMessage = e.getClass().getSimpleName() + " - " + e.getMessage();
			e.printStackTrace();
		}

		finishHandler.sendMessage(prepareResultMessage(wasError, errorMessage, fileName));
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

	public ExportFormat getExportFormat()
	{
		return exportFormat;
	}
}
