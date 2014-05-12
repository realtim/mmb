package ru.mmb.terminal.activity.report.global;

public enum GlobalReportMode
{
	ALL_TEAMS("all"),

	SELECTED_TEAMS("vip");

	private final String shortName;

	private GlobalReportMode(String shortName)
	{
		this.shortName = shortName;
	}

	public String getShortName()
	{
		return shortName;
	}

	public static GlobalReportMode getByName(String reportModeName)
	{
		for (GlobalReportMode reportMode : values())
		{
			if (reportMode.name().equalsIgnoreCase(reportModeName)) return reportMode;
		}
		throw new RuntimeException("GlobalReportMode not found for name: " + reportModeName);
	}
}
