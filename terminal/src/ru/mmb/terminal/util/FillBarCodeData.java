package ru.mmb.terminal.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import ru.mmb.terminal.db.TerminalDB;
import ru.mmb.terminal.model.Distance;
import ru.mmb.terminal.model.LevelPoint;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.registry.DistancesRegistry;
import ru.mmb.terminal.model.registry.TeamsRegistry;

public class FillBarCodeData
{
	public static void execute()
	{
		Distance distance = DistancesRegistry.getInstance().getDistances().get(0);
		LevelPoint levelPoint = distance.getLevelById(81).getFinishPoint();
		List<Team> teams = TeamsRegistry.getInstance().getTeams(distance.getDistanceId());
		generateBarCodeData(teams, levelPoint);
	}

	private static void generateBarCodeData(List<Team> teams, LevelPoint levelPoint)
	{
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(new Date());
		calendar.add(Calendar.HOUR_OF_DAY, -3);
		int counter = 0;
		for (Team team : teams)
		{
			Date recordDate = calendar.getTime();
			TerminalDB.getConnectedInstance().saveBarCodeScan(levelPoint, team, recordDate, recordDate);
			counter++;
			if (counter % 10 == 0)
			{
				calendar.add(Calendar.MINUTE, 1);
			}
		}
	}

}
