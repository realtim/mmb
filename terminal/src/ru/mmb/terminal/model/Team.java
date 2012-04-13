package ru.mmb.terminal.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Team implements Serializable
{
	private static final long serialVersionUID = 8964164964196286726L;

	private int teamId;
	private int distanceId;
	private int teamNum;
	private String teamName;

	private transient List<Participant> members = null;

	public Team()
	{
	}

	public Team(int teamId, int distanceId, int teamNum, String teamName)
	{
		this.teamId = teamId;
		this.distanceId = distanceId;
		this.teamNum = teamNum;
		this.teamName = teamName;
	}

	public int getTeamId()
	{
		return teamId;
	}

	public int getDistanceId()
	{
		return distanceId;
	}

	public int getTeamNum()
	{
		return teamNum;
	}

	public String getTeamName()
	{
		return teamName;
	}

	private List<Participant> getMembersInstance()
	{
		if (members == null) members = new ArrayList<Participant>();
		return members;
	}

	public List<Participant> getMembers()
	{
		return Collections.unmodifiableList(getMembersInstance());
	}

	public void addMember(Participant participant)
	{
		getMembersInstance().add(participant);
	}

	/*public static Team parse(String teamString, Map<Integer, List<Participant>> teamParticipants)
	{
		if (ParseUtils.isEmpty(teamString)) return null;

		String[] strings = teamString.trim().split("\\|");
		int id = Integer.parseInt(strings[0]);
		Integer idInteger = new Integer(id);
		int distanceId = Integer.parseInt(strings[1]);
		int number = Integer.parseInt(strings[2]);
		String name = strings[3];
		Team result = new Team(id, distanceId, number, name);

		if (teamParticipants.containsKey(idInteger))
		{
			for (Participant participant : teamParticipants.get(idInteger))
			{
				result.addMember(participant);
				participant.setTeam(result);
			}
		}

		return result;
	}*/

	private void writeObject(ObjectOutputStream s) throws IOException
	{
		s.defaultWriteObject();

		s.writeInt(members.size());
		for (Participant participant : members)
		{
			s.writeObject(participant);
		}
	}

	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException
	{
		s.defaultReadObject();

		int membersSize = s.readInt();
		for (int i = 0; i < membersSize; i++)
		{
			Participant participant = (Participant) s.readObject();
			addMember(participant);
			participant.setTeam(this);
		}
	}

	public Participant getMember(int participantId)
	{
		for (Participant member : members)
		{
			if (member.getUserId() == participantId) return member;
		}
		return null;
	}
}
