package ru.mmb.datacollector.model.checkpoints;

public class Interval
{
	private int beginNum;
	private int endNum;

	public Interval(int beginNum, int endNum)
	{
		this.beginNum = beginNum;
		this.endNum = endNum;
	}

	public int getBeginNum()
	{
		return beginNum;
	}

	public void setBeginNum(int beginNum)
	{
		this.beginNum = beginNum;
	}

	public int getEndNum()
	{
		return endNum;
	}

	public void setEndNum(int endNum)
	{
		this.endNum = endNum;
	}

	public boolean isSingleElement()
	{
		return beginNum == endNum;
	}
}
