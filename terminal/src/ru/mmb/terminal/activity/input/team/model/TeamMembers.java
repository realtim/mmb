package ru.mmb.terminal.activity.input.team.model;

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
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < memberNames.size(); i++)
		{
			if (i > 0) sb.append("\n");
			sb.append(memberNames.get(i));
		}
		return sb.toString();
	}

	@Override
	public String getMemberText()
	{
		return getMemberNamesText();
	}
}
