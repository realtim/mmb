package ru.mmb.terminal.model.barcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.mmb.terminal.db.TerminalDB;
import ru.mmb.terminal.model.BarCodeScan;
import ru.mmb.terminal.model.LevelPoint;
import ru.mmb.terminal.model.exception.DataStorageException;

public class DataStorage
{
	private static DataStorage instance = null;

	/**
	 * DataStorage is recreated for level point.<br>
	 * Initialization load must be performed only once on bar code activity
	 * creation.
	 * 
	 * @param levelPoint
	 * @return
	 */
	public static DataStorage getInstance(LevelPoint levelPoint)
	{
		if (instance == null
		        || instance.getLevelPoint().getLevelPointId() != levelPoint.getLevelPointId())
		{
			instance = new DataStorage(levelPoint);
		}
		return instance;
	}

	public static void reset()
	{
		instance = null;
	}

	private final LevelPoint levelPoint;
	// For selected level point each team would have only one bar code scan.
	private final Map<Integer, BarCodeScan> levelPointScans = new HashMap<Integer, BarCodeScan>();

	private DataStorage(LevelPoint levelPoint)
	{
		this.levelPoint = levelPoint;
		initBarCodeScans();
	}

	private void initBarCodeScans()
	{
		List<BarCodeScan> loadedScans =
		    TerminalDB.getConnectedInstance().loadBarCodeScans(levelPoint);
		for (BarCodeScan loadedScan : loadedScans)
		{
			levelPointScans.put(new Integer(loadedScan.getTeamId()), loadedScan);
		}
	}

	public LevelPoint getLevelPoint()
	{
		return levelPoint;
	}

	private Map<Integer, BarCodeScan> getLevelPointScans()
	{
		return levelPointScans;
	}

	public List<BarCodeScanInfo> getBarCodeScans()
	{
		List<BarCodeScanInfo> result = new ArrayList<BarCodeScanInfo>();
		for (BarCodeScan barCodeScan : levelPointScans.values())
		{
			result.add(new BarCodeScanInfo(barCodeScan));
		}
		return result;
	}

	public static void putBarCodeScan(BarCodeScan barCodeScan)
	{
		if (instance.getLevelPoint().getLevelPointId() == barCodeScan.getLevelPointId())
		{
			instance.getLevelPointScans().put(new Integer(barCodeScan.getTeamId()), barCodeScan);
		}
		else
		{
			String message =
			    "Fatal error." + "\n" + "Current BAR_CODE data storage level point ["
			            + instance.getLevelPoint() + "]" + "\n"
			            + "Putting new team level point to [" + barCodeScan.getLevelPoint() + "]";
			throw new DataStorageException(message);
		}
	}

	public int size()
	{
		return levelPointScans.size();
	}
}
