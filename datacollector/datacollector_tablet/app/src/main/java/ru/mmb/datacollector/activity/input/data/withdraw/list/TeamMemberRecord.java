package ru.mmb.datacollector.activity.input.data.withdraw.list;

import ru.mmb.datacollector.model.Participant;

public class TeamMemberRecord implements Comparable<TeamMemberRecord>
{
	private final Participant member;
	private final boolean prevWithdrawn;

	public TeamMemberRecord(Participant member, boolean prevWithdrawn)
	{
		this.member = member;
		this.prevWithdrawn = prevWithdrawn;
	}

	public Participant getMember()
	{
		return member;
	}

	public boolean isPrevWithdrawn()
	{
		return prevWithdrawn;
	}

	public int getMemberId()
	{
		return member.getUserId();
	}

	public CharSequence getMemberName()
	{
		return member.getUserName();
	}

	@Override
	public int compareTo(TeamMemberRecord another)
	{
		return member.compareTo(another.getMember());
	}
}
