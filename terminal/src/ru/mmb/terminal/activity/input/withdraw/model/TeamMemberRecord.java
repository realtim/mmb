package ru.mmb.terminal.activity.input.withdraw.model;

import ru.mmb.terminal.model.Participant;

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
		return member.getId();
	}

	public CharSequence getMemberName()
	{
		return member.getName();
	}

	@Override
	public int compareTo(TeamMemberRecord another)
	{
		return member.compareTo(another.getMember());
	}
}
