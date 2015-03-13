package ru.mmb.datacollector.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ru.mmb.datacollector.db.DatacollectorDB;
import ru.mmb.datacollector.model.Distance;
import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.Participant;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.registry.DistancesRegistry;
import ru.mmb.datacollector.model.registry.ScanPointsRegistry;
import ru.mmb.datacollector.model.registry.TeamsRegistry;

public class FillData
{
	private static List<ScanPoint> scanPoints;

	public static void execute()
	{
		List<Distance> distances = DistancesRegistry.getInstance().getDistances();
		scanPoints = ScanPointsRegistry.getInstance().getScanPoints();
		for (Distance distance : distances)
		{
			generatePoints(distance);
			generateDismiss(distance);
		}
	}

	private static void generatePoints(Distance distance)
	{
		List<Team> teams = TeamsRegistry.getInstance().getTeams(distance.getDistanceId());
		for (ScanPoint scanPoint : scanPoints)
		{
			LevelPoint levelPoint = scanPoint.getLevelPointByDistance(distance.getDistanceId());
			if (levelPoint.getPointType().isStart())
			{
				String takenCheckpoints = "";
				Date startDate = levelPoint.getLevelPointMinDateTime();
				for (Team team : teams)
				{
					DatacollectorDB.getConnectedInstance().saveTeamResult(levelPoint, team, startDate, takenCheckpoints, new Date());
				}
			}
			else if (levelPoint.getPointType().isFinish())
			{
				String takenCheckpoints = levelPoint.getCheckpoints().get(0).getCheckpointName();
				Date finishDate = levelPoint.getLevelPointMaxDateTime();
				for (Team team : teams)
				{
					DatacollectorDB.getConnectedInstance().saveTeamResult(levelPoint, team, finishDate, takenCheckpoints, new Date());
				}
			}
		}
	}

	private static void generateDismiss(Distance distance)
	{
		List<Team> teams = TeamsRegistry.getInstance().getTeams(distance.getDistanceId());

		LevelPoint finishPoint =
		    scanPoints.get(scanPoints.size() - 1).getLevelPointByDistance(distance.getDistanceId());
		for (Team team : teams)
		{
			List<Participant> members = team.getMembers();
			if (members.size() == 1) continue;
			List<Participant> withdrawnMembers = new ArrayList<Participant>();
			for (int i = 0; i < members.size(); i++)
			{
				if (i == 0) continue;
				withdrawnMembers.add(members.get(i));
			}
			DatacollectorDB.getConnectedInstance().saveDismissedMembers(finishPoint, team, withdrawnMembers, new Date());
		}
	}
}
