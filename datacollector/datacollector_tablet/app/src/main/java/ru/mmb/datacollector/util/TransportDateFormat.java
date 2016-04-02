package ru.mmb.datacollector.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TransportDateFormat
{
	private static final SimpleDateFormat TRANSP_DATE_FORMAT_SHORT =
	    new SimpleDateFormat("yyyy-MM-dd");
	private static final SimpleDateFormat TRANSP_DATE_FORMAT_LONG =
	    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static String formatShort(Date date)
	{
		return TRANSP_DATE_FORMAT_SHORT.format(date);
	}

	public static Date parseShort(String dateString)
	{
		Date result;
		try
		{
			result = TRANSP_DATE_FORMAT_SHORT.parse(dateString);
		}
		catch (ParseException e)
		{
			throw new RuntimeException("Couldn't parse short date: " + dateString);
		}
		return result;
	}

	public static String formatLong(Date date)
	{
		return TRANSP_DATE_FORMAT_LONG.format(date);
	}

	public static Date parseLong(String dateString)
	{
		Date result;
		try
		{
			result = TRANSP_DATE_FORMAT_LONG.parse(dateString);
		}
		catch (ParseException e)
		{
			throw new RuntimeException("Couldn't parse long date: " + dateString);
		}
		return result;
	}
}
