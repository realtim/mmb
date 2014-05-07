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

public class LevelPoint implements Serializable
{
	private static final long serialVersionUID = -7882002727049857975L;

	private int levelPointId;
	private PointType pointType;
	private int distanceId;
	private int scanPointId;
	private int levelPointOrder;
	private Date levelPointMinDateTime = null;
	private Date levelPointMaxDateTime = null;

	private transient Distance distance = null;
	private transient ScanPoint scanPoint = null;
	private transient List<LevelPointDiscount> levelPointDiscounts = null;

	private transient List<Checkpoint> checkpoints = null;
	private transient Map<Integer, Checkpoint> checkpointsByOrder = null;

	public LevelPoint()
	{
	}

	public LevelPoint(int levelPointId, PointType pointType, int distanceId, int scanPointId, int levelPointOrder, Date levelPointMinDateTime, Date levelPointMaxDateTime)
	{
		this.levelPointId = levelPointId;
		this.pointType = pointType;
		this.distanceId = distanceId;
		this.scanPointId = scanPointId;
		this.levelPointOrder = levelPointOrder;
		this.levelPointMinDateTime = levelPointMinDateTime;
		this.levelPointMaxDateTime = levelPointMaxDateTime;
	}

	public PointType getPointType()
	{
		return pointType;
	}

	public int getLevelPointId()
	{
		return levelPointId;
	}

	public Distance getDistance()
	{
		return distance;
	}

	public void setDistance(Distance distance)
	{
		this.distance = distance;
	}

	public ScanPoint getScanPoint()
	{
		return scanPoint;
	}

	public void setScanPoint(ScanPoint scanPoint)
	{
		this.scanPoint = scanPoint;
	}

	public int getDistanceId()
	{
		return distanceId;
	}

	public int getScanPointId()
	{
		return scanPointId;
	}

	public int getLevelPointOrder()
	{
		return levelPointOrder;
	}

	public Date getLevelPointMinDateTime()
	{
		return levelPointMinDateTime;
	}

	public Date getLevelPointMaxDateTime()
	{
		return levelPointMaxDateTime;
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
		getCheckpointsByOrderInstance().put(checkpoint.getCheckpointOrder(), checkpoint);
	}

	private void sortCheckpoints()
	{
		Collections.sort(getCheckpointsInstance());
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
			checkpoint.setLevelPoint(this);
		}
		sortCheckpoints();
	}

	public Checkpoint getCheckpointByOrderNum(int orderNum)
	{
		return getCheckpointsByOrderInstance().get(orderNum);
	}

	public void addCheckpoints(List<String> levelPointNames, List<Integer> levelPointPenalties)
	{
		if (levelPointNames.size() != levelPointPenalties.size())
		    throw new RuntimeException("Checkpoints set incorrect: [names: " + levelPointNames
		            + "; penalties: " + levelPointPenalties + "]");

		for (int i = 0; i < levelPointNames.size(); i++)
		{
			Checkpoint newCheckpoint =
			    new Checkpoint(getLevelPointId(), i + 1, levelPointNames.get(i), levelPointPenalties.get(i));
			addCheckpoint(newCheckpoint);
			newCheckpoint.setLevelPoint(this);
		}
		sortCheckpoints();
	}

	public Checkpoint getCheckpointByName(String checkpointName)
	{
		for (Checkpoint checkpoint : getCheckpoints())
		{
			if (checkpoint.getCheckpointName().equalsIgnoreCase(checkpointName)) return checkpoint;
		}
		return null;
	}

	private List<LevelPointDiscount> getLevelPointDiscountsInstance()
	{
		if (levelPointDiscounts == null) levelPointDiscounts = new ArrayList<LevelPointDiscount>();
		return levelPointDiscounts;
	}

	public boolean containsLevelPointDiscount(LevelPointDiscount levelPointDiscount)
	{
		if (checkpoints == null || checkpoints.isEmpty()) return false;

		int firstNum = checkpoints.get(0).getCheckpointOrder();
		int lastNum = checkpoints.get(checkpoints.size() - 1).getCheckpointOrder();
		return firstNum <= levelPointDiscount.getLevelPointDiscountStart()
		        && lastNum >= levelPointDiscount.getLevelPointDiscountFinish();
	}

	public void addLevelPointDiscount(LevelPointDiscount levelPointDiscount)
	{
		List<LevelPointDiscount> discounts = getLevelPointDiscountsInstance();
		if (!discounts.contains(levelPointDiscount))
		{
			discounts.add(levelPointDiscount);
		}
	}

	public List<LevelPointDiscount> getLevelPointDiscounts()
	{
		return Collections.unmodifiableList(getLevelPointDiscountsInstance());
	}

	public boolean isCommonStart()
	{
		if (pointType != PointType.START) return false;
		return levelPointMinDateTime.equals(levelPointMaxDateTime);
	}
}
