package ru.mmb.datacollector.activity.report.team.search.model;

import ru.mmb.datacollector.model.Team;

public interface TeamListRecord
{
	int getTeamId();

	int getTeamNumber();

	String getTeamName();

	String getMemberText();

	Team getTeam();
}
