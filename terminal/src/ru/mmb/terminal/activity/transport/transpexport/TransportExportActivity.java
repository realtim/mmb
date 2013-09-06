package ru.mmb.terminal.activity.transport.transpexport;

import static ru.mmb.terminal.activity.Constants.KEY_EXPORT_RESULT_MESSAGE;
import ru.mmb.terminal.R;
import ru.mmb.terminal.model.registry.Settings;
import ru.mmb.terminal.transport.exporter.ExportMode;
import ru.mmb.terminal.transport.exporter.ExportState;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class TransportExportActivity extends Activity
{
	private TextView labLastExportDate;
	private Button btnFullExport;
	private Button btnIncrementalExport;
	private ProgressBar progressBar;

	private ExportState exportState = null;
	private Handler exportFinishHandler;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Settings.getInstance().setCurrentContext(this);

		setContentView(R.layout.transp_export);

		labLastExportDate = (TextView) findViewById(R.id.transpExport_lastExportTextView);
		btnFullExport = (Button) findViewById(R.id.transpExport_fullExportBtn);
		btnIncrementalExport = (Button) findViewById(R.id.transpExport_incrementalExportBtn);
		progressBar = (ProgressBar) findViewById(R.id.transpExport_progressBar);

		btnFullExport.setOnClickListener(new FullClickListener());
		btnIncrementalExport.setOnClickListener(new IncrementalClickListener());

		exportFinishHandler = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				if (exportState != null && !exportState.isTerminated())
				{
					String resultMessage = msg.getData().getString(KEY_EXPORT_RESULT_MESSAGE);
					Toast.makeText(getApplicationContext(), resultMessage, Toast.LENGTH_LONG).show();
				}
				exportState = null;
				refreshState();
			}
		};

		setTitle(getResources().getString(R.string.transp_export_title));

		refreshState();
	}

	private void refreshState()
	{
		refreshLastExportDate();
		buttonsSetEnabled();
		refreshProgressBarVisible();
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
		btnFullExport.setEnabled(exportState == null);
		btnIncrementalExport.setEnabled(exportState == null);
	}

	private void refreshProgressBarVisible()
	{
		if (exportState != null)
		{
			progressBar.setVisibility(View.VISIBLE);
		}
		else
		{
			progressBar.setVisibility(View.GONE);
		}
	}

	private void runExport(ExportMode exportMode)
	{
		exportState = new ExportState();
		refreshState();

		ExportThread thread = new ExportThread(this, exportMode, exportState);
		thread.start();
	}

	private void terminateExport()
	{
		if (exportState != null)
		{
			exportState.setTerminated(true);
		}
	}

	public Handler getExportFinishHandler()
	{
		return exportFinishHandler;
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
		terminateExport();
	}
}
