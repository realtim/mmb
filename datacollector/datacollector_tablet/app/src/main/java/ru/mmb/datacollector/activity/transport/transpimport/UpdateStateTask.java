package ru.mmb.datacollector.activity.transport.transpimport;

import java.util.TimerTask;

import android.os.Message;

public class UpdateStateTask extends TimerTask
{
	private final TransportImportActivity activity;

	public UpdateStateTask(TransportImportActivity activity)
	{
		this.activity = activity;
	}

	@Override
	public void run()
	{
		activity.getRefreshHandler().sendMessage(new Message());
	}
}
