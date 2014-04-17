package ru.mmb.terminal.test.model.history;

import java.util.List;

import junit.framework.TestCase;
import ru.mmb.terminal.model.ScanPoint;
import ru.mmb.terminal.model.history.DataStorage;
import ru.mmb.terminal.model.history.HistoryInfo;
import ru.mmb.terminal.model.registry.ScanPointsRegistry;

public class DataStorageTest extends TestCase
{
	// Last test level without intersection with UI.
	public void testLoadDataFromDB()
	{
		ScanPoint scanPoint = ScanPointsRegistry.getInstance().getScanPointById(3);

		DataStorage dataStorage = DataStorage.getInstance(scanPoint);

		@SuppressWarnings("unused")
		List<HistoryInfo> history = dataStorage.getHistory();

		/*for (HistoryInfo historyInfo : history)
		{
			System.out.println("LevelPointInfo: "+historyInfo.buildLevelPointInfoText());
		}*/
	}
}
