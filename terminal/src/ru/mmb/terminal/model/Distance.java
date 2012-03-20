package ru.mmb.terminal.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Distance implements Serializable
{
	private static final long serialVersionUID = 1645090862525665007L;

	private int id;
	private int mmbId;
	private String name;

	private transient List<Lap> laps = null;

	public Distance()
	{
	}

	public Distance(int id, int mmbId, String name)
	{
		this.id = id;
		this.mmbId = mmbId;
		this.name = name;
	}

	public int getId()
	{
		return id;
	}

	public int getMmbId()
	{
		return mmbId;
	}

	public String getName()
	{
		return name;
	}

	private List<Lap> getLapsInstance()
	{
		if (laps == null) laps = new ArrayList<Lap>();
		return laps;
	}

	public List<Lap> getLaps()
	{
		return Collections.unmodifiableList(getLapsInstance());
	}

	public void addLap(Lap lap)
	{
		getLapsInstance().add(lap);
	}

	private void sortLaps()
	{
		Collections.sort(getLapsInstance());
	}

	public static Distance parse(String distanceString, Map<Integer, List<Lap>> distanceLaps)
	{
		if (ParseUtils.isEmpty(distanceString)) return null;

		String[] splitted = distanceString.trim().split("\\|");
		int id = Integer.parseInt(splitted[0]);
		int mmbId = Integer.parseInt(splitted[1]);
		String name = splitted[2];
		Distance result = new Distance(id, mmbId, name);

		addDistanceLaps(result, distanceLaps);

		return result;
	}

	private static void addDistanceLaps(Distance distance, Map<Integer, List<Lap>> distanceLaps)
	{
		Integer idInteger = new Integer(distance.getId());
		if (distanceLaps.containsKey(idInteger))
		{
			for (Lap lap : distanceLaps.get(idInteger))
			{
				distance.addLap(lap);
				lap.setDistance(distance);
			}
		}
		distance.sortLaps();
	}

	private void writeObject(ObjectOutputStream s) throws IOException
	{
		s.defaultWriteObject();

		s.writeInt(laps.size());
		for (Lap lap : laps)
		{
			s.writeObject(lap);
		}
	}

	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException
	{
		s.defaultReadObject();

		int lapsSize = s.readInt();
		for (int i = 0; i < lapsSize; i++)
		{
			Lap lap = (Lap) s.readObject();
			addLap(lap);
			lap.setDistance(this);
		}
		sortLaps();
	}

	@Override
	public String toString()
	{
		return "Distance [id=" + id + ", mmbId=" + mmbId + ", name=" + name + ", laps=" + laps
		        + "]";
	}

	public Lap getLapById(int lapId)
	{
		for (Lap lap : laps)
		{
			if (lap.getId() == lapId) return lap;
		}
		return null;
	}

	public String[] getLapNamesArray()
	{
		String[] result = new String[laps.size()];
		for (int i = 0; i < laps.size(); i++)
		{
			result[i] = laps.get(i).getName();
		}
		return result;
	}

	public int getLapIndex(Lap lap)
	{
		return laps.indexOf(lap);
	}

	public Lap getLapByIndex(int index)
	{
		return laps.get(index);
	}
}
