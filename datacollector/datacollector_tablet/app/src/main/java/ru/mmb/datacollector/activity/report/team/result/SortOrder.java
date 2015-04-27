package ru.mmb.datacollector.activity.report.team.result;

public enum SortOrder
{
	ASC,

	DESC;

	public static SortOrder getByName(String sortOrderName)
	{
		for (SortOrder sortOrder : values())
		{
			if (sortOrder.name().equalsIgnoreCase(sortOrderName)) return sortOrder;
		}
		throw new RuntimeException("SortOrder not found for name: " + sortOrderName);
	}
}
