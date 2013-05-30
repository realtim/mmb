package ru.mmb.terminal.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Distance implements Serializable
{
	private static final long serialVersionUID = 1645090862525665007L;

	private int distanceId;
	private int raidId;
	private String distanceName;

	private transient List<Level> levels = null;

	public Distance()
	{
	}

	public Distance(int distanceId, int raidId, String distanceName)
	{
		this.distanceId = distanceId;
		this.raidId = raidId;
		this.distanceName = distanceName;
	}

	public int getDistanceId()
	{
		return distanceId;
	}

	public int getRaidId()
	{
		return raidId;
	}

	public String getDistanceName()
	{
		return distanceName;
	}

	private List<Level> getLevelsInstance()
	{
		if (levels == null) levels = new ArrayList<Level>();
		return levels;
	}

	public List<Level> getLevels()
	{
		return Collections.unmodifiableList(getLevelsInstance());
	}

	public void addLevel(Level level)
	{
		getLevelsInstance().add(level);
	}

	private void sortLevels()
	{
		Collections.sort(getLevelsInstance());
	}

	private void writeObject(ObjectOutputStream s) throws IOException
	{
		s.defaultWriteObject();

		s.writeInt(levels.size());
		for (Level level : levels)
		{
			s.writeObject(level);
		}
	}

	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException
	{
		s.defaultReadObject();

		int levelsSize = s.readInt();
		for (int i = 0; i < levelsSize; i++)
		{
			Level level = (Level) s.readObject();
			addLevel(level);
			level.setDistance(this);
		}
		sortLevels();
	}

	public Level getLevelById(int levelId)
	{
		for (Level level : levels)
		{
			if (level.getLevelId() == levelId) return level;
		}
		return null;
	}

	public String[] getLevelNamesArray()
	{
		String[] result = new String[levels.size()];
		for (int i = 0; i < levels.size(); i++)
		{
			result[i] = levels.get(i).getLevelName();
		}
		return result;
	}

	public int getLevelIndex(Level level)
	{
		return levels.indexOf(level);
	}

	public Level getLevelByIndex(int index)
	{
		return levels.get(index);
	}

	public void addLoadedLevels(List<Level> levels)
	{
		for (Level level : levels)
		{
			addLevel(level);
			level.setDistance(this);
		}
		sortLevels();
	}
}
