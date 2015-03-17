package ru.mmb.datacollector.activity.input.data.checkpoints;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import ru.mmb.datacollector.util.PrettyDateFormat;

public class DateRecord implements Serializable
{
	private static final long serialVersionUID = -3532015001709602344L;

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");

	private int year;
	private int month;
	private int day;
	private int hour;
	private int minute;

	public DateRecord()
	{
		this(new Date());
	}

	public DateRecord(int year, int month, int day, int hour, int minute)
	{
		this.year = year;
		this.month = month;
		this.day = day;
		this.hour = hour;
		this.minute = minute;
	}

	public DateRecord(Date date)
	{
		setDatePart(date);
		setTimePart(date);
	}

	public void setDatePart(Date date)
	{
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		year = calendar.get(Calendar.YEAR);
		month = calendar.get(Calendar.MONTH);
		day = calendar.get(Calendar.DAY_OF_MONTH);
	}

	public void setTimePart(Date date)
	{
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		hour = calendar.get(Calendar.HOUR_OF_DAY);
		minute = calendar.get(Calendar.MINUTE);
	}

	public Date toDate()
	{
		Calendar calendar = new GregorianCalendar(Locale.getDefault());
		//Log.d("date record", calendar.getTimeZone().getDisplayName());
		calendar.set(year, month, day, hour, minute);
		return calendar.getTime();
	}

	public int getYear()
	{
		return year;
	}

	public void setYear(int year)
	{
		this.year = year;
	}

	public int getMonth()
	{
		return month;
	}

	public void setMonth(int month)
	{
		this.month = month;
	}

	public int getDay()
	{
		return day;
	}

	public void setDay(int day)
	{
		this.day = day;
	}

	public int getHour()
	{
		return hour;
	}

	public void setHour(int hour)
	{
		this.hour = hour;
	}

	public int getMinute()
	{
		return minute;
	}

	public void setMinute(int minute)
	{
		this.minute = minute;
	}

	@Override
	public String toString()
	{
		return "DateRecord [year=" + year + ", month=" + month + ", day=" + day + ", hour=" + hour
		        + ", minute=" + minute + "]";
	}

	public String toPrettyString()
	{
		return PrettyDateFormat.format(toDate());
	}

	public String saveToString()
	{
		return sdf.format(toDate());
	}

	public static DateRecord parseString(String value) throws ParseException
	{
		return new DateRecord(sdf.parse(value));
	}
}
