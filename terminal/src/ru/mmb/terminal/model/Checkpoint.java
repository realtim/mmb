package ru.mmb.terminal.model;

import java.io.Serializable;

import ru.mmb.terminal.model.registry.Parented;

public class Checkpoint implements Serializable, Comparable<Checkpoint>, Parented
{
	private static final long serialVersionUID = 8167279135428904030L;

	private int lapId;
	private int orderNum;
	private String name;
	private int penalty;

	private transient Lap lap = null;

	public Checkpoint()
	{
	}

	public Checkpoint(int lapId, int orderNum, String name, int penalty)
	{
		this.lapId = lapId;
		this.orderNum = orderNum;
		this.name = name;
		this.penalty = penalty;
	}

	public int getLapId()
	{
		return lapId;
	}

	public int getOrderNum()
	{
		return orderNum;
	}

	public String getName()
	{
		return name;
	}

	public int getPenalty()
	{
		return penalty;
	}

	@Override
	public String toString()
	{
		return "Checkpoint [lapId=" + lapId + ", orderNum=" + orderNum + ", name=" + name
		        + ", penalty=" + penalty + "]";
	}

	public static Checkpoint parse(String checkpointString)
	{
		if (ParseUtils.isEmpty(checkpointString)) return null;

		String[] splitted = checkpointString.trim().split("\\|");
		int lapId = Integer.parseInt(splitted[0]);
		int orderNum = Integer.parseInt(splitted[1]);
		String name = splitted[2];
		int penalty = Integer.parseInt(splitted[3]);

		return new Checkpoint(lapId, orderNum, name, penalty);
	}

	public Lap getLap()
	{
		return lap;
	}

	public void setLap(Lap lap)
	{
		this.lap = lap;
	}

	@Override
	public int compareTo(Checkpoint another)
	{
		return new Integer(getOrderNum()).compareTo(new Integer(another.getOrderNum()));
	}

	@Override
	public Integer getParentId()
	{
		return new Integer(getLapId());
	}
}
