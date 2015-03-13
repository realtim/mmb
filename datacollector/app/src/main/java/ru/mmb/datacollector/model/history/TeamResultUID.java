package ru.mmb.datacollector.model.history;

import java.util.Date;

import ru.mmb.datacollector.model.TeamResult;

/**
 * Record UID helps uniquely identify records to synchronize history map.<br>
 * Also it will help use TreeMap sorted by date correctly.
 * 
 * @author yweiss
 */
public class TeamResultUID implements Comparable<TeamResultUID>
{
	private final Date recordDateTime;
	private final Integer teamId;
	private final Integer userId;

	public TeamResultUID(TeamResult teamResult)
	{
		recordDateTime = teamResult.getRecordDateTime();
		teamId = teamResult.getTeamId();
		userId = teamResult.getUserId();
	}

	@Override
	public int compareTo(TeamResultUID another)
	{
		int result = recordDateTime.compareTo(another.recordDateTime);
		if (result == 0)
		{
			result = teamId.compareTo(another.teamId);
			if (result == 0)
			{
				result = userId.compareTo(another.userId);
			}
		}
		return result;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((recordDateTime == null) ? 0 : recordDateTime.hashCode());
		result = prime * result + ((teamId == null) ? 0 : teamId.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		TeamResultUID other = (TeamResultUID) obj;
		if (recordDateTime == null)
		{
			if (other.recordDateTime != null) return false;
		}
		else if (!recordDateTime.equals(other.recordDateTime)) return false;
		if (teamId == null)
		{
			if (other.teamId != null) return false;
		}
		else if (!teamId.equals(other.teamId)) return false;
		if (userId == null)
		{
			if (other.userId != null) return false;
		}
		else if (!userId.equals(other.userId)) return false;
		return true;
	}
}
