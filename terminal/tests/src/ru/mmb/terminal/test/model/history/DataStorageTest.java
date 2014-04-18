package ru.mmb.terminal.test.model.history;

import java.util.List;

import ru.mmb.terminal.model.ScanPoint;
import ru.mmb.terminal.model.history.DataStorage;
import ru.mmb.terminal.model.history.HistoryInfo;
import ru.mmb.terminal.model.registry.ScanPointsRegistry;
import ru.mmb.terminal.model.registry.Settings;
import android.test.AndroidTestCase;

public class DataStorageTest extends AndroidTestCase
{
	// Last test level without intersection with UI.
	public void testLoadDataFromDB()
	{
		Settings.getInstance().setCurrentContext(getContext());
		// XXX You must know scan point ID in current terminal.db for base application. 
		ScanPoint scanPoint = ScanPointsRegistry.getInstance().getScanPointById(12);
		System.out.println(scanPoint);

		DataStorage dataStorage = DataStorage.getInstance(scanPoint);

		@SuppressWarnings("unused")
		List<HistoryInfo> history = dataStorage.getHistory();

		/*for (HistoryInfo historyInfo : history)
		{
			System.out.println("LevelPointInfo: "+historyInfo.buildLevelPointInfoText());
		}*/
	}
}
