package ru.mmb.terminal.transport.exporter;

public enum ExportFormat
{
	TXT,

	JSON;

	public String getFileExtension()
	{
		if (this == ExportFormat.TXT)
		{
			return "txt";
		}
		else
		{
			return "json";
		}
	}
}
