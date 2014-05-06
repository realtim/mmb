package ru.mmb.terminal.activity.report.team.search.model;

import ru.mmb.terminal.model.Team;

public interface TeamListRecord
{
	int getTeamId();

	int getTeamNumber();

	String getTeamName();

	String getMemberText();

	Team getTeam();
}
