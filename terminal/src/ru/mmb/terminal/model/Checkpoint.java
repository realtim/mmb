package ru.mmb.terminal.model;

import java.io.Serializable;

public class Checkpoint implements Serializable, Comparable<Checkpoint>
{
	private static final long serialVersionUID = 8167279135428904030L;

	private int levelId;
	private int checkpointOrder;
	private String checkpointName;
	private int checkpointPenalty;

	private transient Level level = null;

	public Checkpoint()
	{
	}

	public Checkpoint(int levelId, int checkpointOrder, String checkpointName, int checkpointPenalty)
	{
		this.levelId = levelId;
		this.checkpointOrder = checkpointOrder;
		this.checkpointName = checkpointName;
		this.checkpointPenalty = checkpointPenalty;
	}

	public int getLevelId()
	{
		return levelId;
	}

	public int getCheckpointOrder()
	{
		return checkpointOrder;
	}

	public String getCheckpointName()
	{
		return checkpointName;
	}

	public int getCheckpointPenalty()
	{
		return checkpointPenalty;
	}

	/*public static Checkpoint parse(String checkpointString)
	{
		if (ParseUtils.isEmpty(checkpointString)) return null;

		String[] splitted = checkpointString.trim().split("\\|");
		int lapId = Integer.parseInt(splitted[0]);
		int orderNum = Integer.parseInt(splitted[1]);
		String name = splitted[2];
		int penalty = Integer.parseInt(splitted[3]);

		return new Checkpoint(lapId, orderNum, name, penalty);
	}*/

	public Level getLevel()
	{
		return level;
	}

	public void setLevel(Level level)
	{
		this.level = level;
	}

	@Override
	public int compareTo(Checkpoint another)
	{
		return new Integer(getCheckpointOrder()).compareTo(new Integer(another.getCheckpointOrder()));
	}
}
