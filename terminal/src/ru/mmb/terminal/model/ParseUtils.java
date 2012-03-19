package ru.mmb.terminal.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ParseUtils
{
	private static final String NULL_STRING = "<null>";
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmZ");

	static
	{
		sdf.setTimeZone(TimeZone.getTimeZone("GMT+0400"));
	}

	public static boolean isEmpty(String value)
	{
		return value == null || value.trim().length() == 0;
	}

	public static boolean isNull(String value)
	{
		return NULL_STRING.equals(value);
	}

	public static Date parseDate(String value)
	{
		try
		{
			return sdf.parse(value + "+0400");
		}
		catch (ParseException e)
		{
			return null;
		}
	}

	public static String saveDate(Date value)
	{
		return sdf.format(value);
	}
}
