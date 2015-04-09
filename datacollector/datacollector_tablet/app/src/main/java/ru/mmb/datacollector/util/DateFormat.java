package ru.mmb.datacollector.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormat
{
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmm");

	public static String format(Date date)
	{
		return DATE_FORMAT.format(date);
	}

	public static Date parse(String dateString)
	{
		Date result;
		try
		{
			result = DATE_FORMAT.parse(dateString);
		}
		catch (ParseException e)
		{
			throw new RuntimeException("Couldn't parse date: " + dateString);
		}
		return result;
	}
}
