package ru.mmb.terminal.activity.transport.transpexport;

import ru.mmb.terminal.R;
import ru.mmb.terminal.model.registry.Settings;
import ru.mmb.terminal.transport.exporter.ExportState;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

public class TransportExportActivity extends Activity
{
	private TransportExportActivityState currentState;

	private LinearLayout progressBarPanel;
	private ExportDataPanel exportDataPanel;
	private ExportState exportState = null;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Settings.getInstance().setCurrentContext(this);

		currentState = new TransportExportActivityState();
		currentState.initialize(this, savedInstanceState);

		setContentView(R.layout.transp_export);

		progressBarPanel = (LinearLayout) findViewById(R.id.transpExport_progressBarPanel);
		exportDataPanel = new ExportDataPanel(this);

		setTitle(getResources().getString(R.string.transp_export_title));

		refreshState();
	}

	public void refreshState()
	{
		exportDataPanel.refreshState();
		refreshProgressBarPanelVisible();
	}

	private void refreshProgressBarPanelVisible()
	{
		if (exportState != null)
		{
			progressBarPanel.setVisibility(View.VISIBLE);
		}
		else
		{
			progressBarPanel.setVisibility(View.GONE);
		}
	}

	public ExportState getExportState()
	{
		return exportState;
	}

	public void setExportState(ExportState exportState)
	{
		this.exportState = exportState;
	}

	private void terminateExport()
	{
		if (exportState != null)
		{
			exportState.setTerminated(true);
		}
	}

	public TransportExportActivityState getCurrentState()
	{
		return currentState;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		currentState.save(outState);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		terminateExport();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		currentState.saveToSharedPreferences(getPreferences(MODE_PRIVATE));
		terminateExport();
	}
}
