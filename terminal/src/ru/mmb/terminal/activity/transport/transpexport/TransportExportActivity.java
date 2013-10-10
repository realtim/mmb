package ru.mmb.terminal.activity.transport.transpexport;

import static ru.mmb.terminal.activity.Constants.KEY_LEVEL_SELECT_MODE;
import static ru.mmb.terminal.activity.Constants.REQUEST_CODE_LEVEL_ACTIVITY;
import ru.mmb.terminal.R;
import ru.mmb.terminal.activity.ActivityStateWithTeamAndLevel;
import ru.mmb.terminal.activity.level.LevelSelectMode;
import ru.mmb.terminal.activity.level.SelectLevelActivity;
import ru.mmb.terminal.model.registry.Settings;
import ru.mmb.terminal.transport.exporter.ExportState;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

public class TransportExportActivity extends Activity
{
	private ActivityStateWithTeamAndLevel currentState;

	private LinearLayout progressBarPanel;
	private ExportDataPanel exportDataPanel;
	private ExportBarCodesPanel exportBarCodesPanel;
	private ExportState exportState = null;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Settings.getInstance().setCurrentContext(this);

		currentState = new ActivityStateWithTeamAndLevel("transport.export");
		currentState.initialize(this, savedInstanceState);

		setContentView(R.layout.transp_export);

		progressBarPanel = (LinearLayout) findViewById(R.id.transpExport_progressBarPanel);
		exportDataPanel = new ExportDataPanel(this);
		exportBarCodesPanel = new ExportBarCodesPanel(this);

		setTitle(getResources().getString(R.string.transp_export_title));

		refreshState();
	}

	public void refreshState()
	{
		exportDataPanel.refreshState();
		exportBarCodesPanel.refreshState();
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

	public ActivityStateWithTeamAndLevel getCurrentState()
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

	public void selectLevelPoint()
	{
		Intent intent = new Intent(getApplicationContext(), SelectLevelActivity.class);
		currentState.prepareStartActivityIntent(intent, REQUEST_CODE_LEVEL_ACTIVITY);
		intent.putExtra(KEY_LEVEL_SELECT_MODE, LevelSelectMode.BARCODE);
		startActivityForResult(intent, REQUEST_CODE_LEVEL_ACTIVITY);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case REQUEST_CODE_LEVEL_ACTIVITY:
				onSelectLevelActivityResult(resultCode, data);
				break;
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void onSelectLevelActivityResult(int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK)
		{
			currentState.loadFromIntent(data);
			refreshState();
		}
	}
}
