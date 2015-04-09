package ru.mmb.datacollector.report;

import java.util.ArrayList;
import java.util.List;

import ru.mmb.datacollector.db.DatacollectorDB;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.TeamResult;
import ru.mmb.datacollector.model.report.Level;
import ru.mmb.datacollector.model.report.LevelsRegistry;
import ru.mmb.datacollector.model.report.TeamLevel;
import ru.mmb.datacollector.model.report.TeamReport;

public class TeamReportBuilder
{
	private final Team team;

	public TeamReportBuilder(Team team)
	{
		this.team = team;
	}

	public String buildFullReportString()
	{
		return buildReport().toFullHtml();
	}

	public TeamReport buildReport()
	{
		List<TeamResult> teamResults = DatacollectorDB.getConnectedInstance().loadTeamResults(team);
		teamResults = removeDuplicateDBRecords(teamResults);
		return buildReport(teamResults);
	}

	private List<TeamResult> removeDuplicateDBRecords(List<TeamResult> teamResults)
	{
		List<TeamResult> result = new ArrayList<TeamResult>();

		if (teamResults.isEmpty()) return result;

		int currScanPointId = -1;
		for (int i = 0; i < teamResults.size(); i++)
		{
			TeamResult teamResult = teamResults.get(i);
			if (teamResult.getScanPointId() != currScanPointId)
			{
				if (i > 0)
				{
					result.add(teamResults.get(i - 1));
				}
				currScanPointId = teamResult.getScanPointId();
			}
		}
		// Last record not added to list. It must be added in any case.
		TeamResult lastResult = teamResults.get(teamResults.size() - 1);
		if (!result.contains(lastResult))
		{
			result.add(lastResult);
		}
		return result;
	}

	private TeamReport buildReport(List<TeamResult> teamResults)
	{
		List<TeamLevel> teamLevels = createTeamLevels();
		TeamReport teamReport = new TeamReport(team, teamLevels);
		teamReport.processTeamResults(teamResults);
		return teamReport;
	}

	private List<TeamLevel> createTeamLevels()
	{
		List<TeamLevel> result = new ArrayList<TeamLevel>();
		List<Level> levels = LevelsRegistry.getLevels(team.getDistanceId());
		for (Level level : levels)
		{
			TeamLevel teamLevel = new TeamLevel(level);
			result.add(teamLevel);
		}
		return result;
	}
}
