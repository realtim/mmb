package ru.mmb.datacollector.activity.input.data.history.list;

import java.util.ArrayList;
import java.util.List;

import ru.mmb.datacollector.model.history.DataStorage;
import ru.mmb.datacollector.model.history.HistoryInfo;

public class DataProvider
{
	public static List<HistoryListRecord> getHistoryRecords(DataStorage dataStorage)
	{
		List<HistoryListRecord> result = new ArrayList<HistoryListRecord>();
		for (HistoryInfo historyInfo : dataStorage.getHistory())
		{
			result.add(new HistoryListRecord(historyInfo));
		}
		return result;
	}
}
