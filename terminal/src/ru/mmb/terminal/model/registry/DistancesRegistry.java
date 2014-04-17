package ru.mmb.terminal.model.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.mmb.terminal.db.TerminalDB;
import ru.mmb.terminal.model.Distance;

public class DistancesRegistry
{
	private static DistancesRegistry instance = null;

	private List<Distance> distances = null;

	public static DistancesRegistry getInstance()
	{
		if (instance == null)
		{
			instance = new DistancesRegistry();
		}
		return instance;
	}

	private DistancesRegistry()
	{
		refresh();
	}

	public void refresh()
	{
		try
		{
			distances = TerminalDB.getConnectedInstance().loadDistances(CurrentRaid.getId());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			distances = new ArrayList<Distance>();
		}
	}

	public List<Distance> getDistances()
	{
		return Collections.unmodifiableList(distances);
	}

	public Distance getDistanceByIndex(int index)
	{
		return distances.get(index);
	}

	public Distance getDistanceById(int id)
	{
		for (Distance distance : distances)
		{
			if (distance.getDistanceId() == id) return distance;
		}
		return null;
	}

	public String[] getDistanceNamesArray()
	{
		String[] result = new String[distances.size()];
		for (int i = 0; i < distances.size(); i++)
		{
			result[i] = distances.get(i).getDistanceName();
		}
		return result;
	}

	public int getDistanceIndex(Distance distance)
	{
		return distances.indexOf(distance);
	}
}
