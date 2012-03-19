package ru.mmb.terminal.model.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ru.mmb.terminal.model.Checkpoint;
import ru.mmb.terminal.model.Distance;
import ru.mmb.terminal.model.Lap;
import ru.mmb.terminal.util.ExternalStorage;

public class DistancesRegistry extends AbstractRegistry
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
		load();
	}

	private void load()
	{
		try
		{
			List<Checkpoint> checkpoints = loadCheckpoints();
			Map<Integer, List<Checkpoint>> lapCheckpoints = groupChildrenByParentId(checkpoints);
			List<Lap> laps = loadLaps(lapCheckpoints);
			Map<Integer, List<Lap>> distanceLaps = groupChildrenByParentId(laps);
			distances = loadDistances(distanceLaps);
		}
		catch (Exception e)
		{
			distances = new ArrayList<Distance>();
		}
	}

	private List<Checkpoint> loadCheckpoints() throws Exception
	{
		return loadStoredElements(ExternalStorage.getDir() + "/mmb/model/checkpoints.csv", Checkpoint.class);
	}

	private List<Lap> loadLaps(Map<Integer, List<Checkpoint>> lapCheckpoints) throws Exception
	{
		return loadStoredElements(ExternalStorage.getDir() + "/mmb/model/laps.csv", Lap.class, lapCheckpoints);
	}

	private List<Distance> loadDistances(Map<Integer, List<Lap>> distanceLaps) throws Exception
	{
		return loadStoredElements(ExternalStorage.getDir() + "/mmb/model/distances.csv", Distance.class, distanceLaps);
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
			if (distance.getId() == id) return distance;
		}
		return null;
	}

	public String[] getDistanceNamesArray()
	{
		String[] result = new String[distances.size()];
		for (int i = 0; i < distances.size(); i++)
		{
			result[i] = distances.get(i).getName();
		}
		return result;
	}

	public int getDistanceIndex(Distance distance)
	{
		return distances.indexOf(distance);
	}
}
