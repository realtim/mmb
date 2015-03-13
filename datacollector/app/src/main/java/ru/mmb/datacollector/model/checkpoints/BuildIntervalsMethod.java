package ru.mmb.datacollector.model.checkpoints;

import java.util.ArrayList;
import java.util.List;

public class BuildIntervalsMethod
{
	private final List<Integer> checkedList;

	public BuildIntervalsMethod(List<Integer> checkedList)
	{
		this.checkedList = checkedList;
	}

	public List<Interval> execute()
	{
		List<Interval> result = new ArrayList<Interval>();
		Interval current = null;
		for (int i = 0; i < checkedList.size(); i++)
		{
			int orderNum = checkedList.get(i);
			if (current != null && (current.getEndNum() + 1) == orderNum)
			{
				current.setEndNum(orderNum);
			}
			else
			{
				current = new Interval(orderNum, orderNum);
				result.add(current);
			}
		}
		return result;
	}
}
