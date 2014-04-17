package ru.mmb.terminal.model.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.mmb.terminal.db.TerminalDB;
import ru.mmb.terminal.model.Distance;
import ru.mmb.terminal.model.LevelPoint;
import ru.mmb.terminal.model.ScanPoint;

public class ScanPointsRegistry
{
	private static ScanPointsRegistry instance = null;

	private List<ScanPoint> scanPoints = null;
	private DistancesRegistry distancesRegistry;

	public static ScanPointsRegistry getInstance()
	{
		if (instance == null)
		{
			instance = new ScanPointsRegistry();
		}
		return instance;
	}

	private ScanPointsRegistry()
	{
		refresh();
	}

	public void refresh()
	{
		try
		{
			// Init distances registry first.
			distancesRegistry = DistancesRegistry.getInstance();
			// Now load scanPoints and levelPoints.
			scanPoints = TerminalDB.getConnectedInstance().loadScanPoints(CurrentRaid.getId());
			List<LevelPoint> levelPoints =
			    TerminalDB.getConnectedInstance().loadLevelPoints(CurrentRaid.getId());
			updateDistanceForLevelPoints(levelPoints);
			for (ScanPoint scanPoint : scanPoints)
			{
				addLevelPointsToScanPoint(scanPoint, levelPoints);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			scanPoints = new ArrayList<ScanPoint>();
		}
	}

	private void updateDistanceForLevelPoints(List<LevelPoint> levelPoints)
	{
		for (LevelPoint levelPoint : levelPoints)
		{
			Distance distance = distancesRegistry.getDistanceById(levelPoint.getDistanceId());
			levelPoint.setDistance(distance);
		}
	}

	private void addLevelPointsToScanPoint(ScanPoint scanPoint, List<LevelPoint> levelPoints)
	{
		for (LevelPoint levelPoint : levelPoints)
		{
			if (levelPoint.getScanPointId() == scanPoint.getScanPointId())
			{
				scanPoint.addLevelPoint(levelPoint);
				levelPoint.setScanPoint(scanPoint);
			}
		}
	}

	public List<ScanPoint> getScanPoints()
	{
		return Collections.unmodifiableList(scanPoints);
	}

	public ScanPoint getScanPointByIndex(int index)
	{
		return scanPoints.get(index);
	}

	public ScanPoint getScanPointById(int id)
	{
		for (ScanPoint scanPoint : scanPoints)
		{
			if (scanPoint.getScanPointId() == id) return scanPoint;
		}
		return null;
	}

	public String[] getScanPointNamesArray()
	{
		String[] result = new String[scanPoints.size()];
		for (int i = 0; i < scanPoints.size(); i++)
		{
			result[i] = scanPoints.get(i).getScanPointName();
		}
		return result;
	}

	public int getScanPointIndex(ScanPoint scanPoint)
	{
		return scanPoints.indexOf(scanPoint);
	}
}
