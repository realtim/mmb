package ru.mmb.terminal.activity.input.team.model;

import ru.mmb.terminal.model.Team;

public class TeamMember extends TeamInfo implements TeamListRecord
{
	private final String memberName;

	public TeamMember(Team team, String memberName)
	{
		super(team);
		this.memberName = memberName;
	}

	public String getMemberName()
	{
		return memberName;
	}

	@Override
	public String getMemberText()
	{
		return getMemberName();
	}
}
