package ru.mmb.terminal.activity.report.team.search.model;

import java.util.ArrayList;
import java.util.List;

import ru.mmb.terminal.model.Participant;
import ru.mmb.terminal.model.Team;

public class TeamMembers extends TeamInfo implements TeamListRecord
{
	private final List<String> memberNames = new ArrayList<String>();

	public TeamMembers(Team team)
	{
		super(team);
		for (Participant participant : team.getMembers())
		{
			memberNames.add(participant.getUserName());
		}
	}

	public List<String> getMemberNames()
	{
		return memberNames;
	}

	public String getMemberNamesText()
	{
		return getTeam().getMembersText();
	}

	@Override
	public String getMemberText()
	{
		return getMemberNamesText();
	}
}
