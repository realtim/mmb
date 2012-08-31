package ru.mmb.terminal.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ru.mmb.terminal.db.TerminalDB;
import ru.mmb.terminal.model.Distance;
import ru.mmb.terminal.model.Level;
import ru.mmb.terminal.model.LevelPoint;
import ru.mmb.terminal.model.Participant;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.registry.DistancesRegistry;
import ru.mmb.terminal.model.registry.TeamsRegistry;

public class FillData
{
	public static void execute()
	{
		List<Distance> distances = DistancesRegistry.getInstance().getDistances();
		for (Distance distance : distances)
		{
			generatePoints(distance);
			generateDismiss(distance);
		}
	}

	private static void generatePoints(Distance distance)
	{
		List<Team> teams = TeamsRegistry.getInstance().getTeams(distance.getDistanceId());
		for (Level level : distance.getLevels())
		{
			String takenCheckpoints = level.getCheckpoints().get(0).getCheckpointName();
			LevelPoint levelFinish = level.getFinishPoint();
			Date finishDate = level.getLevelEndTime();
			for (Team team : teams)
			{
				TerminalDB.getInstance().saveInputData(levelFinish, team, finishDate, takenCheckpoints);
			}
		}
	}

	private static void generateDismiss(Distance distance)
	{
		List<Team> teams = TeamsRegistry.getInstance().getTeams(distance.getDistanceId());

		List<Level> levels = distance.getLevels();
		int preLastIndex = levels.size() - 2;
		Level level = levels.get(preLastIndex);
		LevelPoint finishPoint = level.getFinishPoint();
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
			TerminalDB.getInstance().saveWithdrawnMembers(finishPoint, level, team, withdrawnMembers);
		}
	}
}
