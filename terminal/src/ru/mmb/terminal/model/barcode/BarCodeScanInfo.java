package ru.mmb.terminal.model.barcode;

import ru.mmb.terminal.model.BarCodeScan;
import ru.mmb.terminal.util.PrettyTimeFormat;

public class BarCodeScanInfo implements Comparable<BarCodeScanInfo>
{
	private final BarCodeScan barCodeScan;

	public BarCodeScanInfo(BarCodeScan barCodeScan)
	{
		this.barCodeScan = barCodeScan;
	}

	@Override
	public int compareTo(BarCodeScanInfo another)
	{
		return this.barCodeScan.compareTo(another.barCodeScan);
	}

	public String buildTeamNumberText()
	{
		return Integer.toString(barCodeScan.getTeam().getTeamNum());
	}

	public String buildTeamName()
	{
		return barCodeScan.getTeam().getTeamName();
	}

	public String buildCheckTimeText()
	{
		return PrettyTimeFormat.format(barCodeScan.getCheckDateTime());
	}
}
