package ru.mmb.terminal.activity.input.history.list;

import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.history.HistoryInfo;

public class HistoryListRecord implements Comparable<HistoryListRecord>
{
	private final HistoryInfo historyInfo;

	public HistoryListRecord(HistoryInfo historyInfo)
	{
		this.historyInfo = historyInfo;
	}

	public int getTeamNumber()
	{
		return historyInfo.getTeam().getTeamNum();
	}

	public String getTeamLevelPointText()
	{
		return historyInfo.buildLevelPointInfoText();
	}

	public Integer getUserId()
	{
		return historyInfo.getLevelPointUserId();
	}

	@Override
	public int compareTo(HistoryListRecord another)
	{
		return historyInfo.compareTo(another.historyInfo);
	}

	public Team getTeam()
	{
		return historyInfo.getTeam();
	}

	public String getMembersText()
	{
		return historyInfo.buildMembersInfo();
	}

	public String getDismissedText()
	{
		return historyInfo.buildDismissedInfo();
	}
}
