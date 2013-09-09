package ru.mmb.terminal.activity.transport.transpimport;

import ru.mmb.terminal.model.registry.DistancesRegistry;
import ru.mmb.terminal.model.registry.TeamsRegistry;
import ru.mmb.terminal.model.registry.UsersRegistry;
import ru.mmb.terminal.transport.importer.ImportState;
import ru.mmb.terminal.transport.importer.Importer;
import android.os.Message;

public class ImportThread extends Thread
{
	private final ImportState importState;
	private final String fileName;
	private final TransportImportActivity activity;

	public ImportThread(String fileName, ImportState importState, TransportImportActivity activity)
	{
		super();
		this.fileName = fileName;
		this.importState = importState;
		this.activity = activity;
	}

	@Override
	public void run()
	{
		boolean needRefreshRegistries = false;
		try
		{
			new Importer(importState).importPackage(fileName);
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
			TeamsRegistry.getInstance().refresh();
			UsersRegistry.getInstance().refresh();
		}

		System.out.println("ImportThread finished.");
	}
}
