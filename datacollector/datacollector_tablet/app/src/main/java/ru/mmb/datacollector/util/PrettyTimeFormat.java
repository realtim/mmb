package ru.mmb.datacollector.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PrettyTimeFormat
{
	private static final SimpleDateFormat PRETTY_FORMAT = new SimpleDateFormat("MMM dd\tHH:mm");

	public static String format(Date date)
	{
		return PRETTY_FORMAT.format(date);
	}
}
