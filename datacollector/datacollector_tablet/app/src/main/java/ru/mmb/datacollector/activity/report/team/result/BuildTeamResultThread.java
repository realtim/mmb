package ru.mmb.datacollector.activity.report.team.result;

import static ru.mmb.datacollector.activity.Constants.KEY_REPORT_TEAM_RESULT_MESSAGE;
import ru.mmb.datacollector.R;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.report.TeamReportBuilder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class BuildTeamResultThread extends Thread
{
	private final TeamResultActivity activity;
	private final Handler finishHandler;
	private final Team team;
	private String teamResult;

	public BuildTeamResultThread(TeamResultActivity activity, Handler finishHandler, Team team)
	{
		super();
		this.activity = activity;
		this.finishHandler = finishHandler;
		this.team = team;
	}

	public TeamResultActivity getActivity()
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
			teamResult = buildTeamResult();
		}
		catch (Exception e)
		{
			wasError = true;
			errorMessage = e.getClass().getSimpleName() + " - " + e.getMessage();
			e.printStackTrace();
		}

		finishHandler.sendMessage(prepareResultMessage(wasError, errorMessage));
	}

	private String buildTeamResult()
	{
		return new TeamReportBuilder(team).buildFullReportString();
	}

	private Message prepareResultMessage(boolean wasError, String errorMessage)
	{
		Message msg = new Message();
		Bundle messageBundle = new Bundle();
		messageBundle.putString(KEY_REPORT_TEAM_RESULT_MESSAGE, getResultMessageString(wasError, errorMessage));
		msg.setData(messageBundle);
		return msg;
	}

	private String getResultMessageString(boolean wasError, String errorMessage)
	{
		if (!wasError)
		{
			return teamResult;
		}
		else
		{
			return "<html><head><meta charset=\"UTF-8\"></head><body>"
			        + activity.getResources().getString(R.string.report_team_result_error) + "\n"
			        + errorMessage + "</body></html>";
		}
	}
}
