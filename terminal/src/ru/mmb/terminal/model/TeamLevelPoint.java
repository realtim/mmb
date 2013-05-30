package ru.mmb.terminal.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ru.mmb.terminal.model.checkpoints.CheckedState;
import ru.mmb.terminal.util.PrettyDateFormat;

public class TeamLevelPoint implements Comparable<TeamLevelPoint>
{
	private final int teamId;
	private final int userId;
	private final int deviceId;
	private final int levelPointId;
	private final String takenCheckpointNames;
	private final Date checkDateTime;
	private final Date recordDateTime;

	private Team team = null;
	private LevelPoint levelPoint = null;

	private final List<Checkpoint> takenCheckpoints = new ArrayList<Checkpoint>();

	@SuppressWarnings("unused")
	private String missedCheckpointsText;
	private String takenCheckpointsText;

	public TeamLevelPoint(int teamId, int userId, int deviceId, int levelPointId, String takenCheckpointNames, Date checkDateTime, Date recordDateTime)
	{
		this.teamId = teamId;
		this.userId = userId;
		this.deviceId = deviceId;
		this.levelPointId = levelPointId;
		this.takenCheckpointNames = takenCheckpointNames;
		this.checkDateTime = checkDateTime;
		this.recordDateTime = recordDateTime;
	}

	public Team getTeam()
	{
		return team;
	}

	public void setTeam(Team team)
	{
		this.team = team;
	}

	public int getTeamId()
	{
		return teamId;
	}

	public int getUserId()
	{
		return userId;
	}

	public int getDeviceId()
	{
		return deviceId;
	}

	public List<Checkpoint> getTakenCheckpoints()
	{
		return takenCheckpoints;
	}

	public Date getCheckDateTime()
	{
		return checkDateTime;
	}

	public Date getRecordDateTime()
	{
		return recordDateTime;
	}

	public LevelPoint getLevelPoint()
	{
		return levelPoint;
	}

	public void setLevelPoint(LevelPoint levelPoint)
	{
		this.levelPoint = levelPoint;
	}

	public int getLevelPointId()
	{
		return levelPointId;
	}

	public void initTakenCheckpoints()
	{
		if (levelPoint == null) return;
		Level level = levelPoint.getLevel();
		if (level == null) return;

		takenCheckpoints.clear();

		String[] pointNames = takenCheckpointNames.split(",");
		for (int i = 0; i < pointNames.length; i++)
		{
			Checkpoint checkpoint = level.getCheckpointByName(pointNames[i]);
			if (checkpoint == null) continue;
			takenCheckpoints.add(checkpoint);
		}

		initCheckpointsTexts();
	}

	private void initCheckpointsTexts()
	{
		if (levelPoint.getPointType() == PointType.FINISH)
		{
			CheckedState checkedState = new CheckedState();
			checkedState.setLevel(levelPoint.getLevel());
			checkedState.loadTakenCheckpoints(takenCheckpoints);
			missedCheckpointsText = checkedState.getMissedCheckpointsText();
			takenCheckpointsText = checkedState.getTakenCheckpointsText();
		}
		else
		{
			missedCheckpointsText = "";
			takenCheckpointsText = "";
		}
	}

	public String getTakenCheckpointNames()
	{
		return takenCheckpointNames;
	}

	@Override
	public int compareTo(TeamLevelPoint another)
	{
		int result = recordDateTime.compareTo(another.recordDateTime);
		if (result == 0)
		{
			result = (new Integer(userId)).compareTo(new Integer(another.userId));
		}
		return result;
	}

	@Override
	public String toString()
	{
		return "TeamLevelPoint [teamId=" + teamId + ", userId=" + userId + ", deviceId=" + deviceId
		        + ", levelPointId=" + levelPointId + ", takenCheckpointNames="
		        + takenCheckpointNames + ", checkDateTime=" + checkDateTime + ", recordDateTime="
		        + recordDateTime + "]";
	}

	public String buildInfoText()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(PrettyDateFormat.format(checkDateTime));
		if (levelPoint.getPointType() == PointType.FINISH)
		    sb.append("\n").append(takenCheckpointsText);
		return sb.toString();
	}
}
