package ru.mmb.terminal.activity.report.global;

import static ru.mmb.terminal.activity.Constants.KEY_REPORT_GLOBAL_RESULT_MESSAGE;

import java.io.IOException;

import ru.mmb.terminal.R;
import ru.mmb.terminal.report.GlobalReportBuilder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class BuildGlobalReportThread extends Thread
{
	private final ReportGlobalResultActivity activity;
	private final Handler finishHandler;
	private final GlobalReportMode reportMode;
	private final String selectedTeams;
	private String reportResult;

	public BuildGlobalReportThread(ReportGlobalResultActivity activity, Handler finishHandler, GlobalReportMode reportMode, String selectedTeams)
	{
		super();
		this.activity = activity;
		this.finishHandler = finishHandler;
		this.reportMode = reportMode;
		this.selectedTeams = selectedTeams;
	}

	public ReportGlobalResultActivity getActivity()
	{
		return activity;
	}

	@Override
	public void run()
	{
		boolean wasError = false;
		String errorMessage = "";
		try
		{
			reportResult = buildGlobalReport();
		}
		catch (Exception e)
		{
			wasError = true;
			errorMessage = e.getClass().getSimpleName() + " - " + e.getMessage();
			e.printStackTrace();
		}

		finishHandler.sendMessage(prepareResultMessage(wasError, errorMessage));
	}

	private String buildGlobalReport() throws IOException
	{
		return new GlobalReportBuilder(reportMode, selectedTeams).buildReport();
	}

	private Message prepareResultMessage(boolean wasError, String errorMessage)
	{
		Message msg = new Message();
		Bundle messageBundle = new Bundle();
		messageBundle.putString(KEY_REPORT_GLOBAL_RESULT_MESSAGE, getResultMessageString(wasError, errorMessage));
		msg.setData(messageBundle);
		return msg;
	}

	private String getResultMessageString(boolean wasError, String errorMessage)
	{
		if (!wasError)
		{
			return reportResult;
		}
		else
		{
			return activity.getResources().getString(R.string.report_global_result_error) + "\n"
			        + errorMessage;
		}
	}
}
