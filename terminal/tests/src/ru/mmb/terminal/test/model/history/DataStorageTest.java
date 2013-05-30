package ru.mmb.terminal.test.model.history;

import java.util.List;

import junit.framework.TestCase;
import ru.mmb.terminal.model.Distance;
import ru.mmb.terminal.model.Level;
import ru.mmb.terminal.model.LevelPoint;
import ru.mmb.terminal.model.history.DataStorage;
import ru.mmb.terminal.model.history.HistoryInfo;
import ru.mmb.terminal.model.registry.DistancesRegistry;

public class DataStorageTest extends TestCase
{
	// Last test level without intersection with UI.
	public void testLoadDataFromDB()
	{
		Distance distance = DistancesRegistry.getInstance().getDistanceById(1);
		Level level = distance.getLevelById(1);
		LevelPoint levelPoint = level.getFinishPoint();

		DataStorage dataStorage = DataStorage.getInstance(levelPoint);

		@SuppressWarnings("unused")
		List<HistoryInfo> history = dataStorage.getHistory();

		/*for (HistoryInfo historyInfo : history)
		{
			System.out.println("LevelPointInfo: "+historyInfo.buildLevelPointInfoText());
		}*/
	}
}
