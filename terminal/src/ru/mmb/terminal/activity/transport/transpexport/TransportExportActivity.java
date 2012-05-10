package ru.mmb.terminal.activity.transport.transpexport;

import ru.mmb.terminal.R;
import ru.mmb.terminal.model.registry.Settings;
import ru.mmb.terminal.transport.exporter.ExportMode;
import ru.mmb.terminal.transport.exporter.Exporter;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class TransportExportActivity extends Activity
{
	private TextView labLastExportDate;
	private Button btnFullExport;
	private Button btnIncrementalExport;

	private boolean exportRunning = false;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.transp_export);

		labLastExportDate = (TextView) findViewById(R.id.transpExport_lastExportTextView);
		btnFullExport = (Button) findViewById(R.id.transpExport_fullExportBtn);
		btnIncrementalExport = (Button) findViewById(R.id.transpExport_incrementalExportBtn);

		btnFullExport.setOnClickListener(new FullClickListener());
		btnIncrementalExport.setOnClickListener(new IncrementalClickListener());

		setTitle(getResources().getString(R.string.transp_export_title));

		refreshState();
	}

	private void refreshState()
	{
		refreshLastExportDate();
		buttonsSetEnabled();
	}

	private void refreshLastExportDate()
	{
		String lastExportString = Settings.getInstance().getLastExportDate();
		if ("".equals(lastExportString))
		{
			labLastExportDate.setText(getResources().getText(R.string.transp_export_no_last_export));
		}
		else
		{
			labLastExportDate.setText(lastExportString);
		}
	}

	private void buttonsSetEnabled()
	{
		btnFullExport.setEnabled(!exportRunning);
		btnIncrementalExport.setEnabled(!exportRunning);
	}

	private void runExport(ExportMode exportMode)
	{
		exportRunning = true;
		refreshState();

		String exportFileName = null;
		try
		{
			exportFileName = (new Exporter(exportMode)).exportData();
			exportRunning = false;
			refreshState();
			String toastMessage =
			    getResources().getString(R.string.transp_export_success) + "\n" + exportFileName;
			Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_SHORT).show();
		}
		catch (Exception e)
		{
			exportRunning = false;
			refreshState();
			e.printStackTrace();
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.transp_export_error), Toast.LENGTH_SHORT).show();
		}
	}

	private class FullClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			runExport(ExportMode.FULL);
		}
	}

	private class IncrementalClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			runExport(ExportMode.INCREMENTAL);
		}
	}
}
