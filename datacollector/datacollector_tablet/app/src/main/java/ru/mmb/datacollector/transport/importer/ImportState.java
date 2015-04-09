package ru.mmb.datacollector.transport.importer;

import java.util.ArrayList;
import java.util.List;

public class ImportState
{
	private boolean terminated = false;
	private boolean finished = false;
	private String currentTable;
	private int totalRows;
	private int rowsProcessed;
	private final List<String> messages = new ArrayList<String>();

	public ImportState()
	{
	}

	public synchronized String getCurrentTable()
	{
		return currentTable;
	}

	public synchronized void setCurrentTable(String currentTable)
	{
		this.currentTable = currentTable;
	}

	public synchronized int getTotalRows()
	{
		return totalRows;
	}

	public synchronized void setTotalRows(int totalRows)
	{
		this.totalRows = totalRows;
	}

	public synchronized int getRowsProcessed()
	{
		return rowsProcessed;
	}

	public synchronized void setRowsProcessed(int rowsProcessed)
	{
		this.rowsProcessed = rowsProcessed;
	}

	public synchronized List<String> extractMessages()
	{
		List<String> result = new ArrayList<String>(messages);
		messages.clear();
		return result;
	}

	public synchronized void appendMessages(List<String> newMessages)
	{
		messages.addAll(newMessages);
	}

	public synchronized void appendMessage(String newMessage)
	{
		messages.add(newMessage);
	}

	public synchronized void incRowsProcessed()
	{
		rowsProcessed++;
	}

	public synchronized boolean isTerminated()
	{
		return terminated;
	}

	public synchronized void setTerminated(boolean terminated)
	{
		messages.add("Import terminated.");
		this.terminated = terminated;
	}

	public synchronized void setFinished(boolean finished)
	{
		this.finished = finished;
	}

	public synchronized boolean isFinished()
	{
		return finished;
	}

	public String getProcessedRowsText()
	{
		return getRowsProcessed() + "/" + getTotalRows();
	}
}
