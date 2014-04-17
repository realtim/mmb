package ru.mmb.terminal.model.history;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TreeMap;

import ru.mmb.terminal.model.Participant;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.TeamDismiss;
import ru.mmb.terminal.model.User;
import ru.mmb.terminal.model.registry.UsersRegistry;

public class TeamDismissedState
{
	private Date lastRecordDateTime;
	private final Team team;
	private final Map<User, Boolean> teamDismissedState = new TreeMap<User, Boolean>();

	public TeamDismissedState(Team team)
	{
		this.team = team;
		initState();
		initDate();
	}

	private void initState()
	{
		for (Participant participant : team.getMembers())
		{
			User user = UsersRegistry.getInstance().getUserById(participant.getUserId());
			teamDismissedState.put(user, false);
		}
	}

	private void initDate()
	{
		// set date from deep past to record having no dismissed members
		lastRecordDateTime = (new GregorianCalendar(1900, 1, 1, 0, 0)).getTime();
	}

	public void add(TeamDismiss teamLevelDismiss)
	{
		User dismissedUser = teamLevelDismiss.getTeamUser();
		teamDismissedState.put(dismissedUser, true);
		updateLastRecordDateTime(teamLevelDismiss);
	}

	private void updateLastRecordDateTime(TeamDismiss teamLevelDismiss)
	{
		Date recordDateTime = teamLevelDismiss.getRecordDateTime();
		if (lastRecordDateTime == null || lastRecordDateTime.before(recordDateTime))
		{
			lastRecordDateTime = recordDateTime;
		}
	}

	public String buildMembersInfo()
	{
		StringBuilder sb = new StringBuilder();
		int index = 0;
		for (User user : teamDismissedState.keySet())
		{
			if (index > 0) sb.append("\n");
			sb.append(user.getUserName());
			index++;
		}
		return sb.toString();
	}

	public String buildDismissedInfo()
	{
		StringBuilder sb = new StringBuilder();
		int index = 0;
		for (Boolean dismissed : teamDismissedState.values())
		{
			if (index > 0) sb.append("\n");
			if (dismissed) sb.append("(-)");
			index++;
		}
		return sb.toString();
	}

	public Date getLastRecordDateTime()
	{
		return lastRecordDateTime;
	}
}
