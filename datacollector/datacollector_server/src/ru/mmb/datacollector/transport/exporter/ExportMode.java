package ru.mmb.datacollector.transport.exporter;

public enum ExportMode
{
	FULL("FULL"),

	INCREMENTAL("INC");

	private final String shortName;

	private ExportMode(String shortName)
	{
		this.shortName = shortName;
	}

	public String getShortName()
	{
		return shortName;
	}
}
