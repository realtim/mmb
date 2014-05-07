package ru.mmb.terminal.model.report;

import static ru.mmb.terminal.model.report.LevelCalcResult.COMPLETE;
import static ru.mmb.terminal.model.report.LevelCalcResult.EMPTY;
import static ru.mmb.terminal.model.report.LevelCalcResult.FAIL;
import static ru.mmb.terminal.model.report.LevelCalcResult.NOT_FINISHED;

import java.util.List;

import ru.mmb.terminal.model.LevelPoint;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.TeamResult;

public class TeamReport implements Comparable<TeamReport>
{
	private final Team team;
	private LevelCalcResult calcResult;
	private int lastVisitedPointOrder;
	private long duration;
	private final List<TeamLevel> teamLevels;

	public TeamReport(Team team, List<TeamLevel> teamLevels)
	{
		this.team = team;
		this.teamLevels = teamLevels;
	}

	public void processTeamResults(List<TeamResult> teamResults)
	{
		prepareTeamLevels(teamResults);
		calculateFinalResult();
		if (calcResult == EMPTY || calcResult == FAIL) return;
		calculateLastVisited();
		calculateTotalDuration();
	}

	private void prepareTeamLevels(List<TeamResult> teamResults)
	{
		int distanceId = team.getDistanceId();
		for (TeamLevel teamLevel : teamLevels)
		{
			for (TeamResult teamResult : teamResults)
			{
				LevelPoint levelPoint =
				    teamResult.getScanPoint().getLevelPointByDistance(distanceId);
				Level level = LevelsRegistry.getLevelByLevelPointId(levelPoint.getLevelPointId());
				if (teamLevel.getLevel() == level)
				{
					teamLevel.addTeamResult(levelPoint, teamResult);
				}
			}
			teamLevel.processData();
		}
	}

	private void calculateFinalResult()
	{
		calcResult = EMPTY;
		int currentPos = 0;
		for (TeamLevel teamLevel : teamLevels)
		{
			LevelCalcResult teamLevelResult = teamLevel.getCalcResult();
			if (currentPos == 0)
			{
				calcResult = teamLevelResult;
			}
			else
			{
				if (teamLevelResult == FAIL)
				{
					calcResult = FAIL;
				}
				else if (calcResult == COMPLETE
				        && ((teamLevelResult == EMPTY || teamLevelResult == NOT_FINISHED)))
				{
					calcResult = NOT_FINISHED;
				}
				else if (calcResult == NOT_FINISHED && teamLevelResult != EMPTY)
				{
					calcResult = FAIL;
				}
			}
			currentPos++;
			if (calcResult == LevelCalcResult.FAIL) break;
		}
	}

	private void calculateLastVisited()
	{
		for (TeamLevel teamLevel : teamLevels)
		{
			if (teamLevel.isAcceptable())
			{
				lastVisitedPointOrder = teamLevel.getLastVisitedPointOrder();
			}
		}
	}

	private void calculateTotalDuration()
	{
		for (TeamLevel teamLevel : teamLevels)
		{
			if (teamLevel.getCalcResult() == COMPLETE)
			{
				duration += teamLevel.getTotalDuration();
			}
		}

	}

	public String toFullHtml()
	{
		if (calcResult == EMPTY)
		    return "<html><head><meta charset=\"UTF-8\"></head><body>NO DATA</body></html>";

		StringBuilder sb = new StringBuilder();
		sb.append("<html><head><meta charset=\"UTF-8\"></head><body>");
		sb.append("<table cellspacing=\"5\"><col width=\"150\"><col width=\"100\"><col width=\"250\">");
		for (TeamLevel teamLevel : teamLevels)
		{
			sb.append(teamLevel.toFullHtml());
		}
		if (calcResult != FAIL)
		{
			sb.append("<tr><td colspan=3 style=\"background-color:grey\">&nbsp;</td></tr>");
			sb.append("<tr><td colspan=3 style=\"background-color:cyan\">");
			sb.append("Total time: " + toHourMinuteString((int) duration));
			sb.append("</td></tr>");
		}
		sb.append("</table></body></html>");
		return sb.toString();
	}

	public static String toHourMinuteString(int duration)
	{
		int hours = duration / 60;
		int minutes = duration % 60;
		return String.format("%d:%02d", hours, minutes);
	}

	@Override
	public int compareTo(TeamReport another)
	{
		if (this.calcResult == FAIL && another.calcResult == FAIL)
		{
			return 0;
		}
		if (this.calcResult == FAIL)
		{
			return -1;
		}
		if (lastVisitedPointOrder != another.lastVisitedPointOrder)
		{
			return new Integer(lastVisitedPointOrder).compareTo(new Integer(another.lastVisitedPointOrder));
		}
		return new Long(duration).compareTo(new Long(another.duration));
	}

	public String toCompactString()
	{
		// TODO Auto-generated method stub
		return null;
	}
}
