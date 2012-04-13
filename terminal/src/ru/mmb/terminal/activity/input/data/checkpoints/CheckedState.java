package ru.mmb.terminal.activity.input.data.checkpoints;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.mmb.terminal.model.Checkpoint;
import ru.mmb.terminal.model.Level;

public class CheckedState implements Serializable
{
	private static final long serialVersionUID = -1671058824332140814L;

	private final Map<Integer, Boolean> checkedMap = new LinkedHashMap<Integer, Boolean>();
	private Level level;

	public CheckedState()
	{
	}

	public void setLevel(Level level)
	{
		if (level != null)
		{
			if (this.level == null || this.level.getLevelId() != level.getLevelId())
			    rebuildCheckedMap(level);
		}
		else
		{
			checkedMap.clear();
		}
		this.level = level;
	}

	private void rebuildCheckedMap(Level level)
	{
		checkedMap.clear();
		for (Checkpoint checkpoint : level.getCheckpoints())
		{
			checkedMap.put(checkpoint.getCheckpointOrder(), false);
		}
	}

	public Level getLevel()
	{
		return level;
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
			sb.append(level.getCheckpointByOrderNum(checkpointOrderNum).getCheckpointName());
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
			sb.append(level.getCheckpointByOrderNum(interval.getBeginNum()).getCheckpointName());
			if (!interval.isSingleElement())
			    sb.append("-").append(level.getCheckpointByOrderNum(interval.getEndNum()).getCheckpointName());
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
