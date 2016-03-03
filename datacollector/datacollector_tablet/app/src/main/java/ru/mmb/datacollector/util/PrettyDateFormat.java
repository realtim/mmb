package ru.mmb.datacollector.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PrettyDateFormat
{
	private static final SimpleDateFormat PRETTY_FORMAT = new SimpleDateFormat("HH:mm dd/MMM/yyyy");

	public static String format(Date date)
	{
		return PRETTY_FORMAT.format(date);
	}
}
