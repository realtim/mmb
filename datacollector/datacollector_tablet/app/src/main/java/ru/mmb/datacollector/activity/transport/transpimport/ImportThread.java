package ru.mmb.datacollector.activity.transport.transpimport;

import android.os.Message;

import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.registry.DistancesRegistry;
import ru.mmb.datacollector.model.registry.ScanPointsRegistry;
import ru.mmb.datacollector.model.registry.TeamsRegistry;
import ru.mmb.datacollector.model.registry.UsersRegistry;
import ru.mmb.datacollector.model.report.LevelsRegistry;
import ru.mmb.datacollector.transport.importer.ImportState;
import ru.mmb.datacollector.transport.importer.Importer;

public class ImportThread extends Thread
{
	private final ImportState importState;
	private final String fileName;
	private final ScanPoint scanPoint;
	private final TransportImportActivity activity;

	public ImportThread(String fileName, ImportState importState, ScanPoint scanPoint, TransportImportActivity activity)
	{
		super();
		this.fileName = fileName;
		this.importState = importState;
		this.scanPoint = scanPoint;
		this.activity = activity;
	}

	@Override
	public void run()
	{
		boolean needRefreshRegistries = false;
		try
		{
			new Importer(importState, scanPoint).importPackageFromFile(fileName);
			importState.setFinished(true);
			needRefreshRegistries = !importState.isTerminated();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			importState.appendMessage("Import failed for " + fileName);
			importState.appendMessage("ERROR: " + e.getClass().getSimpleName() + " - "
			        + e.getMessage());
			importState.setFinished(true);
		}
		importState.appendMessage("Import finished.");
		activity.getRefreshHandler().sendMessage(new Message());

		if (needRefreshRegistries)
		{
			DistancesRegistry.getInstance().refresh();
			ScanPointsRegistry.getInstance().refresh();
			TeamsRegistry.getInstance().refresh();
			UsersRegistry.getInstance().refresh();
            LevelsRegistry.getInstance().refresh();

			// update data storages after import
			ru.mmb.datacollector.model.history.DataStorage.reset();
		}

		System.out.println("ImportThread finished.");
	}
}
