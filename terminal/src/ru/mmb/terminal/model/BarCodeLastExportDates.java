package ru.mmb.terminal.model;

import java.util.HashMap;
import java.util.Map;

public class BarCodeLastExportDates
{
	private static final String RECORD_SEPARATOR = ";";
	private static final String SPLIT_SEPARATOR = ":";

	private final Map<Integer, String> lastExportDates = new HashMap<Integer, String>();

	public void loadFromString(String lastExportDatesString)
	{
		lastExportDates.clear();
		if (lastExportDatesString == null) return;

		String[] records = lastExportDatesString.split(RECORD_SEPARATOR);
		for (String record : records)
		{
			int levelPointId = extractLevelPointId(record);
			String lastExportDate = extractLastExportDate(record);
			lastExportDates.put(new Integer(levelPointId), lastExportDate);
		}
	}

	private int extractLevelPointId(String record)
	{
		int separatorPos = record.indexOf(SPLIT_SEPARATOR);
		if (separatorPos == -1)
		    throw new RuntimeException("BarCodeLastExportDates wrong string format.");
		String levelPointIdString = record.substring(0, separatorPos);
		return Integer.parseInt(levelPointIdString);
	}

	private String extractLastExportDate(String record)
	{
		int separatorPos = record.indexOf(SPLIT_SEPARATOR);
		if (separatorPos == -1)
		    throw new RuntimeException("BarCodeLastExportDates wrong string format.");
		return record.substring(separatorPos + 1, record.length());
	}

	public String saveToString()
	{
		String result = "";
		for (Integer levelPointId : lastExportDates.keySet())
		{
			String record = levelPointId.toString() + SPLIT_SEPARATOR;
			record += lastExportDates.get(levelPointId);
			result += record + RECORD_SEPARATOR;
		}
		return result.length() == 0 ? "" : result.substring(0, result.length() - 1);
	}

	public void put(Integer levelPointId, String lastExportDate)
	{
		lastExportDates.put(levelPointId, lastExportDate);
	}

	public String get(Integer levelPointId)
	{
		return lastExportDates.get(levelPointId);
	}

	public static void main(String[] args)
	{
		BarCodeLastExportDates dates = new BarCodeLastExportDates();
		dates.loadFromString("1:hbvbhsv;2:nvjknvjkdf;3:bkjdsvbkjdsv");
		System.out.println("Dates loaded");
		System.out.println("Dates(1): " + dates.get(1));
		System.out.println("Dates(2): " + dates.get(2));
		System.out.println("Dates(3): " + dates.get(3));
		dates.put(4, "vbfvbhkd");
		dates.put(3, "11111");
		System.out.println("Dates changed");
		System.out.println("Dates(3): " + dates.get(3));
		System.out.println("Dates(4): " + dates.get(4));
		System.out.println("Dates to save");
		System.out.println(dates.saveToString());
	}
}
