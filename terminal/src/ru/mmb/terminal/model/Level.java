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

public class Level implements Serializable, Comparable<Level>
{
	private static final long serialVersionUID = 7275757094995874595L;

	private int levelId;
	private int distanceId;
	private String levelName;
	private int levelOrder;
	private StartType levelStartType;
	private Date levelBegTime = null;
	private Date levelMaxBegTime = null;
	private Date levelMinEndTime = null;
	private Date levelEndTime = null;

	private LevelPoint startPoint;
	private LevelPoint finishPoint;

	private transient Distance distance = null;

	private transient List<Checkpoint> checkpoints = null;
	private transient Map<Integer, Checkpoint> checkpointsByOrder = null;

	public Level()
	{
	}

	public Level(int levelId, int distanceId, String levelName, int levelOrder, StartType levelStartType, Date levelBegTime, Date levelMaxBegTime, Date levelMinEndTime, Date levelEndTime)
	{
		this.levelId = levelId;
		this.distanceId = distanceId;
		this.levelName = levelName;
		this.levelOrder = levelOrder;
		this.levelStartType = levelStartType;
		this.levelBegTime = levelBegTime;
		this.levelMaxBegTime = levelMaxBegTime;
		this.levelMinEndTime = levelMinEndTime;
		this.levelEndTime = levelEndTime;
	}

	public int getLevelId()
	{
		return levelId;
	}

	public int getDistanceId()
	{
		return distanceId;
	}

	public String getLevelName()
	{
		return levelName;
	}

	public int getLevelOrder()
	{
		return levelOrder;
	}

	public StartType getLevelStartType()
	{
		return levelStartType;
	}

	public Date getLevelBegTime()
	{
		return levelBegTime;
	}

	public Date getLevelMaxBegTime()
	{
		return levelMaxBegTime;
	}

	public Date getLevelMinEndTime()
	{
		return levelMinEndTime;
	}

	public Date getLevelEndTime()
	{
		return levelEndTime;
	}

	public LevelPoint getStartPoint()
	{
		return startPoint;
	}

	public LevelPoint getFinishPoint()
	{
		return finishPoint;
	}

	public void setStartPoint(LevelPoint startPoint)
	{
		this.startPoint = startPoint;
	}

	public void setFinishPoint(LevelPoint finishPoint)
	{
		this.finishPoint = finishPoint;
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
			checkpoint.setLevel(this);
		}
		sortCheckpoints();

		startPoint.setLevel(this);
		finishPoint.setLevel(this);
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
	public int compareTo(Level another)
	{
		return new Integer(getLevelOrder()).compareTo(new Integer(another.getLevelOrder()));
	}

	public Checkpoint getCheckpointByOrderNum(int orderNum)
	{
		return getCheckpointsByOrderInstance().get(orderNum);
	}

	public void addCheckpoints(String levelPointNames, String levelPointPenalties)
	{
		String[] pointNames = levelPointNames.split(",");
		String[] pointPenalties = levelPointPenalties.split(",");

		if (pointNames.length != pointPenalties.length)
		    throw new RuntimeException("Checkpoints set incorrect: [names: " + levelPointNames
		            + "; penalties: " + levelPointPenalties + "]");

		for (int i = 0; i < pointNames.length; i++)
		{
			Checkpoint newCheckpoint =
			    new Checkpoint(getLevelId(), i, pointNames[i], Integer.parseInt(pointPenalties[i]));
			addCheckpoint(newCheckpoint);
			newCheckpoint.setLevel(this);
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
}
