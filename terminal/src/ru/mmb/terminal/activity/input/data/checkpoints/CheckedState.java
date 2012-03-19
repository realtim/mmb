package ru.mmb.terminal.activity.input.data.checkpoints;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.mmb.terminal.model.Checkpoint;
import ru.mmb.terminal.model.Lap;

public class CheckedState implements Serializable
{
	private static final long serialVersionUID = -1671058824332140814L;

	private final Map<Integer, Boolean> checkedMap = new LinkedHashMap<Integer, Boolean>();
	private Lap lap;

	public CheckedState()
	{
	}

	public void setLap(Lap lap)
	{
		if (lap != null)
		{
			if (this.lap == null || this.lap.getId() != lap.getId()) rebuildCheckedMap(lap);
		}
		else
		{
			checkedMap.clear();
		}
		this.lap = lap;
	}

	private void rebuildCheckedMap(Lap lap)
	{
		checkedMap.clear();
		for (Checkpoint checkpoint : lap.getCheckpoints())
		{
			checkedMap.put(checkpoint.getOrderNum(), false);
		}
	}

	public Lap getLap()
	{
		return lap;
	}

	public void setChecked(int orderNum, boolean checked)
	{
		checkedMap.put(orderNum, checked);
	}

	public boolean isChecked(int orderNum)
	{
		if (checkedMap.containsKey(orderNum))
		{
			return checkedMap.get(orderNum);
		}
		return false;
	}

	public String getTakenCheckpointsText()
	{
		return getCheckpointsText(getCheckedList());
	}

	public String getTakenCheckpointsRawText()
	{
		StringBuilder sb = new StringBuilder();
		for (Integer checkpointOrderNum : getCheckedList())
		{
			sb.append(lap.getCheckpointByOrderNum(checkpointOrderNum).getName());
			sb.append(",");
		}
		return (sb.length() == 0) ? "-" : sb.toString().substring(0, sb.length() - 1);
	}

	public String getMissedCheckpointsText()
	{
		return getCheckpointsText(getNotCheckedList());
	}

	private String getCheckpointsText(List<Integer> checkpoints)
	{
		StringBuilder sb = new StringBuilder();
		List<Interval> intervals = new BuildIntervalsMethod(checkpoints).execute();
		for (Interval interval : intervals)
		{
			sb.append(lap.getCheckpointByOrderNum(interval.getBeginNum()).getName());
			if (!interval.isSingleElement())
			    sb.append("-").append(lap.getCheckpointByOrderNum(interval.getEndNum()).getName());
			sb.append(",");
		}
		return (sb.length() == 0) ? "-" : sb.toString().substring(0, sb.length() - 1);
	}

	public List<Integer> getCheckedList()
	{
		List<Integer> result = new ArrayList<Integer>();
		for (Map.Entry<Integer, Boolean> checkedEntry : checkedMap.entrySet())
		{
			if (checkedEntry.getValue() == true) result.add(checkedEntry.getKey());
		}
		return result;
	}

	public List<Integer> getNotCheckedList()
	{
		List<Integer> result = new ArrayList<Integer>();
		for (Map.Entry<Integer, Boolean> checkedEntry : checkedMap.entrySet())
		{
			if (checkedEntry.getValue() == false) result.add(checkedEntry.getKey());
		}
		return result;
	}

	public void checkAll()
	{
		setAllChecked(true);
	}

	public void uncheckAll()
	{
		setAllChecked(false);
	}

	private void setAllChecked(boolean value)
	{
		for (Integer orderNum : checkedMap.keySet())
		{
			checkedMap.put(orderNum, value);
		}
	}
}
