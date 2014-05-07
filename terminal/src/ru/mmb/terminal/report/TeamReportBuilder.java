package ru.mmb.terminal.report;

import java.util.ArrayList;
import java.util.List;

import ru.mmb.terminal.db.TerminalDB;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.TeamResult;
import ru.mmb.terminal.model.report.Level;
import ru.mmb.terminal.model.report.LevelsRegistry;
import ru.mmb.terminal.model.report.TeamLevel;
import ru.mmb.terminal.model.report.TeamReport;

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

	private TeamReport buildReport()
	{
		List<TeamResult> teamResults = TerminalDB.getConnectedInstance().loadTeamResults(team);
		teamResults = removeDuplicateDBRecords(teamResults);
		return buildReport(teamResults);
	}

	public String buildCompactReportString()
	{
		return buildReport().toCompactString();
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
