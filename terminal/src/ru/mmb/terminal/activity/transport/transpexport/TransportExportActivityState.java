package ru.mmb.terminal.activity.transport.transpexport;

import static ru.mmb.terminal.activity.Constants.KEY_CURRENT_EXPORT_FORMAT;
import ru.mmb.terminal.activity.ActivityStateWithTeamAndScanPoint;
import ru.mmb.terminal.transport.exporter.ExportFormat;
import android.content.SharedPreferences;
import android.os.Bundle;

public class TransportExportActivityState extends ActivityStateWithTeamAndScanPoint
{
	private ExportFormat exportFormat = ExportFormat.TXT;

	public TransportExportActivityState()
	{
		super("transport.export");
	}

	public ExportFormat getExportFormat()
	{
		return exportFormat;
	}

	public void setExportFormat(ExportFormat exportFormat)
	{
		this.exportFormat = exportFormat;
		System.out.println("set export fromat: " + exportFormat);
	}

	@Override
	public void save(Bundle savedInstanceState)
	{
		super.save(savedInstanceState);
		savedInstanceState.putSerializable(KEY_CURRENT_EXPORT_FORMAT, exportFormat);
	}

	@Override
	public void load(Bundle savedInstanceState)
	{
		super.load(savedInstanceState);
		if (savedInstanceState.containsKey(KEY_CURRENT_EXPORT_FORMAT))
		{
			exportFormat =
			    (ExportFormat) savedInstanceState.getSerializable(KEY_CURRENT_EXPORT_FORMAT);
			System.out.println("loaded export fromat from saved bundle: " + exportFormat);
		}
	}

	@Override
	public void saveToSharedPreferences(SharedPreferences preferences)
	{
		super.saveToSharedPreferences(preferences);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(getPrefix() + "." + KEY_CURRENT_EXPORT_FORMAT, exportFormat.name());
		editor.commit();
	}

	@Override
	public void loadFromSharedPreferences(SharedPreferences preferences)
	{
		super.loadFromSharedPreferences(preferences);
		String formatName =
		    preferences.getString(getPrefix() + "." + KEY_CURRENT_EXPORT_FORMAT, null);
		System.out.println("loaded format name from preferences: " + formatName);
		if (formatName != null)
		{
			exportFormat = ExportFormat.valueOf(formatName);
		}
	}
}
