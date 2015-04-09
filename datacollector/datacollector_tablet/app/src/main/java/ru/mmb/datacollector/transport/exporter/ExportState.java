package ru.mmb.datacollector.transport.exporter;

public class ExportState
{
	private boolean terminated = false;

	public synchronized boolean isTerminated()
	{
		return terminated;
	}

	public synchronized void setTerminated(boolean terminated)
	{
		this.terminated = terminated;
	}
}
