package ru.mmb.datacollector.model.report;

import static ru.mmb.datacollector.model.report.LevelCalcResult.COMPLETE;
import static ru.mmb.datacollector.model.report.LevelCalcResult.EMPTY;
import static ru.mmb.datacollector.model.report.LevelCalcResult.FAIL;
import static ru.mmb.datacollector.model.report.LevelCalcResult.NOT_FINISHED;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.mmb.datacollector.model.Checkpoint;
import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.LevelPointDiscount;
import ru.mmb.datacollector.model.TeamResult;
import ru.mmb.datacollector.model.checkpoints.CheckedState;

public class TeamLevel
{
	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

	private final Level level;

	private final Map<LevelPoint, Date> levelPointTimes = new LinkedHashMap<LevelPoint, Date>();
	private final Map<LevelPoint, CheckedState> levelPointChecked =
	    new LinkedHashMap<LevelPoint, CheckedState>();
	private long durationMinutes = 0;
	private long penaltyMinutes = 0;
	private int lastVisitedPointOrder = -1;
	private LevelCalcResult calcResult;

	public TeamLevel(Level level)
	{
		this.level = level;
		initInnerStructures();
		calcResult = EMPTY;
	}

	private void initInnerStructures()
	{
		for (LevelPoint levelPoint : level.getLevelPoints())
		{
			levelPointTimes.put(levelPoint, null);
			levelPointChecked.put(levelPoint, null);
		}
	}

	public int getLastVisitedPointOrder()
	{
		return lastVisitedPointOrder;
	}

	public void setLastVisitedPointOrder(int lastVisitedPointOrder)
	{
		this.lastVisitedPointOrder = lastVisitedPointOrder;
	}

	public LevelCalcResult getCalcResult()
	{
		return calcResult;
	}

	public void setCalcResult(LevelCalcResult calcResult)
	{
		this.calcResult = calcResult;
	}

	public Level getLevel()
	{
		return level;
	}

	public void addTeamResult(LevelPoint levelPoint, TeamResult teamResult)
	{
		levelPointTimes.put(levelPoint, teamResult.getCheckDateTime());
		if (levelPoint.getPointType().isFinish())
		{
			CheckedState checkedState = new CheckedState();
			checkedState.setLevelPoint(levelPoint);
			checkedState.loadTakenCheckpoints(teamResult.getTakenCheckpoints());
			levelPointChecked.put(levelPoint, checkedState);
		}
	}

	public void processData()
	{
		if (level.getLevelPoints().isEmpty()) return;

		fillCommonStartTime();
		// Check level point times consistency.
		updateCalcResult();
		updateLastVisitedPoint();
		if (calcResult == COMPLETE)
		{
			calculateDuration();
			calculatePenalty();
		}
	}

	private void fillCommonStartTime()
	{
		boolean notEmpty = false;
		for (Map.Entry<LevelPoint, Date> entry : levelPointTimes.entrySet())
		{
			if (entry.getValue() != null)
			{
				notEmpty = true;
			}
		}
		if (notEmpty)
		{
			LevelPoint startPoint = level.getStartPoint();
			if (startPoint.isCommonStart())
			{
				levelPointTimes.put(startPoint, startPoint.getLevelPointMinDateTime());
			}
		}
	}

	private void updateCalcResult()
	{
		List<Date> times = new ArrayList<Date>(levelPointTimes.values());
		int emptyCount = 0;
		int lastEmptyCount = 0;
		for (int i = 0; i < times.size(); i++)
		{
			if (times.get(i) == null)
			{
				emptyCount++;
				lastEmptyCount++;
			}
			else
			{
				lastEmptyCount = 0;
			}
		}
		if (emptyCount == times.size())
		{
			calcResult = EMPTY;
		}
		else if (emptyCount == 0)
		{
			calcResult = COMPLETE;
		}
		else if (emptyCount == lastEmptyCount)
		{
			calcResult = NOT_FINISHED;
		}
		else
		{
			calcResult = FAIL;
		}
	}

	private void updateLastVisitedPoint()
	{
		if (isAcceptable())
		{
			List<Date> times = new ArrayList<Date>(levelPointTimes.values());
			int firstEmptyIndex = 0;
			// If level is COMPLETE, then there is no null times in list.
			while (firstEmptyIndex < times.size() && times.get(firstEmptyIndex) != null)
			{
				firstEmptyIndex++;
			}
			lastVisitedPointOrder =
			    level.getLevelPoints().get(firstEmptyIndex - 1).getLevelPointOrder();
		}
	}

	private void calculateDuration()
	{
		if (calcResult != COMPLETE) return;

		long millisStart = levelPointTimes.get(level.getStartPoint()).getTime();
		long millisEnd = levelPointTimes.get(level.getFinishPoint()).getTime();

		durationMinutes = (millisEnd - millisStart) / 1000 / 60;
	}

	private void calculatePenalty()
	{
		for (LevelPoint levelPoint : level.getLevelPoints())
		{
			if (levelPoint.getPointType().isFinish())
			{
				penaltyMinutes += calculatePointPenalty(levelPoint);
			}
		}
	}

	private long calculatePointPenalty(LevelPoint levelPoint)
	{
		long penalty = 0;
		CheckedState checkedState = levelPointChecked.get(levelPoint);
		for (Checkpoint checkpoint : levelPoint.getCheckpoints())
		{
			if (!checkedState.isChecked(checkpoint.getCheckpointOrder()))
			{
				penalty += checkpoint.getCheckpointPenalty();
			}
		}
		for (LevelPointDiscount discount : levelPoint.getLevelPointDiscounts())
		{
			penalty -= calculateDiscount(levelPoint, checkedState, discount);
		}
		return penalty;
	}

	private long calculateDiscount(LevelPoint levelPoint, CheckedState checkedState,
	        LevelPointDiscount discount)
	{
		int discountValue = discount.getLevelPointDiscountValue();
		// Team can take all checkpoints in discountable area, then discount is ignored.
		// We MUST NOT apply discount to other checkpoints for level point.
		int discountablePenalty = 0;
		for (Checkpoint checkpoint : levelPoint.getCheckpoints())
		{
			if (discount.contains(checkpoint)
			        && !checkedState.isChecked(checkpoint.getCheckpointOrder()))
			{
				discountablePenalty += checkpoint.getCheckpointPenalty();
			}
		}
		if (discountablePenalty >= discountValue)
		{
			return discountValue;
		}
		else
		{
			return discountablePenalty;
		}
	}

	public long getDurationMinutes()
	{
		return durationMinutes;
	}

	public long getPenaltyMinutes()
	{
		return penaltyMinutes;
	}

	public boolean isAcceptable()
	{
		return calcResult == COMPLETE || calcResult == NOT_FINISHED;
	}

	public long getTotalDuration()
	{
		return durationMinutes + penaltyMinutes;
	}

	public String toFullHtml()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<tr><td colspan=3 style=\"background-color:grey\">&nbsp;</td></tr>");
		for (LevelPoint levelPoint : level.getLevelPoints())
		{
			sb.append("<tr><td>");
			sb.append(getFixedLengthName(15, levelPoint));
			sb.append("</td><td>");
			sb.append(getTimeString(levelPoint));
			sb.append("</td><td>");
			if (levelPoint.getPointType().isFinish())
			{
				sb.append("missed ").append(getMissedString(levelPoint));
			}
			sb.append("</td></tr>");
		}
		if (calcResult == COMPLETE)
		{
			sb.append("<tr><td colspan=3>");
			sb.append("duration [" + toHourMinuteString((int) durationMinutes) + "], penalty ["
			        + toHourMinuteString((int) penaltyMinutes) + "]");
			sb.append("</td></tr>");
		}
		return sb.toString();
	}

	private String getFixedLengthName(int fixedLength, LevelPoint levelPoint)
	{
		String result = levelPoint.getScanPoint().getScanPointName();
		if (result.length() > 15)
		{
			result = result.substring(0, 15);
		}
		return result;
	}

	private String getTimeString(LevelPoint levelPoint)
	{
		Date levelPointTime = levelPointTimes.get(levelPoint);
		return (levelPointTime == null) ? "??:??" : TIME_FORMAT.format(levelPointTime);
	}

	private String getMissedString(LevelPoint levelPoint)
	{
		CheckedState checkedState = levelPointChecked.get(levelPoint);
		return (checkedState == null) ? "???" : checkedState.getMissedCheckpointsText();
	}

	private String toHourMinuteString(int duration)
	{
		return TeamReport.toHourMinuteString(duration);
	}

	public String toCompactHtml()
	{
		String timesString = "";
		String missedString = "";
		StringBuilder sb = new StringBuilder();
		sb.append("<tr>");
		for (LevelPoint levelPoint : level.getLevelPoints())
		{
			if (timesString.length() > 0)
			{
				timesString += " - ";
			}
			timesString += getTimeString(levelPoint);
			if (levelPoint.getPointType().isFinish())
			{
				if (missedString.length() > 0)
				{
					missedString += "; ";
				}
				missedString += getMissedString(levelPoint);
			}
		}
		sb.append("<td>").append(timesString).append("</td>");
		sb.append("<td>").append(missedString).append("</td>");
		sb.append("</tr>");
		return sb.toString();
	}
}
