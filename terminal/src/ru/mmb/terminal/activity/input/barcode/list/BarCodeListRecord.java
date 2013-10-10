package ru.mmb.terminal.activity.input.barcode.list;

import ru.mmb.terminal.model.barcode.BarCodeScanInfo;

public class BarCodeListRecord implements Comparable<BarCodeListRecord>
{
	private final BarCodeScanInfo barCodeScanInfo;

	public BarCodeListRecord(BarCodeScanInfo barCodeScanInfo)
	{
		this.barCodeScanInfo = barCodeScanInfo;
	}

	public String getCheckTimeText()
	{
		return barCodeScanInfo.buildCheckTimeText();
	}

	public String getTeamNumberText()
	{
		return barCodeScanInfo.buildTeamNumberText();
	}

	public String getTeamName()
	{
		return barCodeScanInfo.buildTeamName();
	}

	@Override
	public int compareTo(BarCodeListRecord another)
	{
		return barCodeScanInfo.compareTo(another.barCodeScanInfo);
	}
}
