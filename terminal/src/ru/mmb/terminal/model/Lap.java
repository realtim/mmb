package ru.mmb.terminal.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.mmb.terminal.model.registry.Parented;

public class Lap implements Serializable, Comparable<Lap>, Parented
{
	private static final long serialVersionUID = 7275757094995874595L;

	private int id;
	private int distanceId;
	private String name;
	private int orderNum;
	private StartType startType;
	private Date commonStartTime = null;
	private Date closeStartTime = null;
	private Date closeFinishTime = null;

	private transient Distance distance = null;

	private transient List<Checkpoint> checkpoints = null;
	private transient Map<Integer, Checkpoint> checkpointsByOrder = null;

	public Lap()
	{
	}

	public Lap(int id, int distanceId, String name, int orderNum, StartType startType, Date commonStartTime, Date closeStartTime, Date closeFinishTime)
	{
		this.id = id;
		this.distanceId = distanceId;
		this.name = name;
		this.orderNum = orderNum;
		this.startType = startType;
		this.commonStartTime = commonStartTime;
		this.closeStartTime = closeStartTime;
		this.closeFinishTime = closeFinishTime;
	}

	public int getId()
	{
		return id;
	}

	public int getDistanceId()
	{
		return distanceId;
	}

	public String getName()
	{
		return name;
	}

	public int getOrderNum()
	{
		return orderNum;
	}

	public StartType getStartType()
	{
		return startType;
	}

	public Date getCommonStartTime()
	{
		return commonStartTime;
	}

	public Date getCloseStartTime()
	{
		return closeStartTime;
	}

	public Date getCloseFinishTime()
	{
		return closeFinishTime;
	}

	private List<Checkpoint> getCheckpointsInstance()
	{
		if (checkpoints == null) checkpoints = new ArrayList<Checkpoint>();
		return checkpoints;
	}

	private Map<Integer, Checkpoint> getCheckpointsByOrderInstance()
	{
		if (checkpointsByOrder == null) checkpointsByOrder = new HashMap<Integer, Checkpoint>();
		return checkpointsByOrder;
	}

	public List<Checkpoint> getCheckpoints()
	{
		return Collections.unmodifiableList(getCheckpointsInstance());
	}

	public void addCheckpoint(Checkpoint checkpoint)
	{
		getCheckpointsInstance().add(checkpoint);
		getCheckpointsByOrderInstance().put(checkpoint.getOrderNum(), checkpoint);
	}

	private void sortCheckpoints()
	{
		Collections.sort(getCheckpointsInstance());
	}

	public static Lap parse(String lapString, Map<Integer, List<Checkpoint>> lapCheckpoints)
	{
		if (ParseUtils.isEmpty(lapString)) return null;

		String[] splitted = lapString.trim().split("\\|");
		int id = Integer.parseInt(splitted[0]);
		int distanceId = Integer.parseInt(splitted[1]);
		String name = splitted[2];
		int orderNum = Integer.parseInt(splitted[3]);
		int startTypeId = Integer.parseInt(splitted[4]);
		StartType startType = StartType.getTypeById(startTypeId);
		Date commonStartTime = null;
		if (!ParseUtils.isNull(splitted[5])) commonStartTime = ParseUtils.parseDate(splitted[5]);
		Date startCloseTime = null;
		if (!ParseUtils.isNull(splitted[6])) startCloseTime = ParseUtils.parseDate(splitted[6]);
		Date finishCloseTime = null;
		if (!ParseUtils.isNull(splitted[7])) finishCloseTime = ParseUtils.parseDate(splitted[7]);
		Lap result =
		    new Lap(id, distanceId, name, orderNum, startType, commonStartTime, startCloseTime, finishCloseTime);

		addLapCheckpoints(result, lapCheckpoints);

		return result;
	}

	private static void addLapCheckpoints(Lap lap, Map<Integer, List<Checkpoint>> lapCheckpoints)
	{
		Integer idInteger = new Integer(lap.getId());
		if (lapCheckpoints.containsKey(idInteger))
		{
			for (Checkpoint checkpoint : lapCheckpoints.get(idInteger))
			{
				lap.addCheckpoint(checkpoint);
				checkpoint.setLap(lap);
			}
		}
		lap.sortCheckpoints();
	}

	private void writeObject(ObjectOutputStream s) throws IOException
	{
		s.defaultWriteObject();

		s.writeInt(checkpoints.size());
		for (Checkpoint checkpoint : checkpoints)
		{
			s.writeObject(checkpoint);
		}
	}

	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException
	{
		s.defaultReadObject();

		int checkpointsSize = s.readInt();
		for (int i = 0; i < checkpointsSize; i++)
		{
			Checkpoint checkpoint = (Checkpoint) s.readObject();
			addCheckpoint(checkpoint);
			checkpoint.setLap(this);
		}
		sortCheckpoints();
	}

	public Distance getDistance()
	{
		return distance;
	}

	public void setDistance(Distance distance)
	{
		this.distance = distance;
	}

	@Override
	public int compareTo(Lap another)
	{
		return new Integer(getOrderNum()).compareTo(new Integer(another.getOrderNum()));
	}

	@Override
	public Integer getParentId()
	{
		return new Integer(getDistanceId());
	}

	@Override
	public String toString()
	{
		return "Lap [id=" + id + ", distanceId=" + distanceId + ", name=" + name + ", orderNum="
		        + orderNum + ", startType=" + startType + ", commonStartTime=" + commonStartTime
		        + ", closeStartTime=" + closeStartTime + ", closeFinishTime=" + closeFinishTime
		        + ", checkpoints=" + checkpoints + "]";
	}

	public Checkpoint getCheckpointByOrderNum(int orderNum)
	{
		return getCheckpointsByOrderInstance().get(orderNum);
	}
}
