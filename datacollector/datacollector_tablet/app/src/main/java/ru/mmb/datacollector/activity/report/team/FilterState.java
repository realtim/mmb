package ru.mmb.datacollector.activity.report.team;

public enum FilterState
{
	SHOW_FULL,

	SHOW_JUST_NUMBER,

	HIDE_FILTER;

	public static FilterState getNextState(FilterState filterState)
	{
		switch (filterState)
		{
			case HIDE_FILTER:
				return SHOW_JUST_NUMBER;
			case SHOW_JUST_NUMBER:
				return SHOW_FULL;
			case SHOW_FULL:
			default:
				return HIDE_FILTER;
		}
	}

	public static FilterState getByName(String filterStateName)
	{
		for (FilterState filterState : values())
		{
			if (filterState.name().equalsIgnoreCase(filterStateName)) return filterState;
		}
		throw new RuntimeException("FilterState not found for name: " + filterStateName);
	}
}
