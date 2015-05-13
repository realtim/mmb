package ru.mmb.datacollector.transport.exporter.method;

public interface ExportDataMethod {
	public static final boolean EXPORT_WITH_RAW = true;

	void exportData(boolean exportWithRaw) throws Exception;
}
