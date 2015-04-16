package ru.mmb.datacollector.transport.importer;


public class ImportState {
	private boolean finished = false;
	private String currentTable;
	private int totalRows;
	private int rowsProcessed;

	public ImportState() {
	}

	public synchronized String getCurrentTable() {
		return currentTable;
	}

	public synchronized void setCurrentTable(String currentTable) {
		this.currentTable = currentTable;
	}

	public synchronized int getTotalRows() {
		return totalRows;
	}

	public synchronized void setTotalRows(int totalRows) {
		this.totalRows = totalRows;
	}

	public synchronized int getRowsProcessed() {
		return rowsProcessed;
	}

	public synchronized void setRowsProcessed(int rowsProcessed) {
		this.rowsProcessed = rowsProcessed;
	}

	public synchronized void incRowsProcessed() {
		rowsProcessed++;
	}

	public synchronized void setFinished(boolean finished) {
		this.finished = finished;
	}

	public synchronized boolean isFinished() {
		return finished;
	}

	public String getProcessedRowsText() {
		return getRowsProcessed() + "/" + getTotalRows();
	}
}
