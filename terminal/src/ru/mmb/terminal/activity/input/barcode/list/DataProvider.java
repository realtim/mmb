package ru.mmb.terminal.activity.input.barcode.list;

import java.util.ArrayList;
import java.util.List;

import ru.mmb.terminal.model.barcode.BarCodeScanInfo;
import ru.mmb.terminal.model.barcode.DataStorage;

public class DataProvider
{
	public static List<BarCodeListRecord> getBarCodeScanRecords(DataStorage dataStorage)
	{
		List<BarCodeListRecord> result = new ArrayList<BarCodeListRecord>();
		for (BarCodeScanInfo barCodeScanInfo : dataStorage.getBarCodeScans())
		{
			result.add(new BarCodeListRecord(barCodeScanInfo));
		}
		return result;
	}
}
