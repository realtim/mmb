package ru.mmb.terminal.activity.input.team;

public enum SortColumn
{
	NUMBER,

	TEAM,

	MEMBER;

	public static SortColumn getByName(String sortColumnName)
	{
		for (SortColumn sortColumn : values())
		{
			if (sortColumn.name().equalsIgnoreCase(sortColumnName)) return sortColumn;
		}
		throw new RuntimeException("SortColumn not found for name: " + sortColumnName);
	}
}
