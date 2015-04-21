package ru.mmb.datacollector.util;

import java.util.Calendar;
import java.util.Date;

public class DateUtils {
	/*
	 * Code copied from stackoverflow.
	 */
	public static Date trimToMinutes(Date value) {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.setTime(value);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}
}
