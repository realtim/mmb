package ru.mmb.datacollector.activity.transport.transpimport;

import static ru.mmb.datacollector.activity.Constants.REQUEST_CODE_FILE_DIALOG;

import java.util.List;
import java.util.Timer;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.ActivityStateWithTeamAndScanPoint;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.transport.importer.ImportState;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.filedialog.FileDialog;
import com.filedialog.SelectionMode;

public class TransportImportActivity extends Activity
{
	private final int CONSOLE_CHAR_LIMIT = 20000;

	protected ActivityStateWithTeamAndScanPoint currentState;

	private String fileName = null;
	private String messagesText = "";
	private ImportState importState = null;
	private Handler refreshHandler;
	private Timer refreshTimer = null;

	protected Button btnSelectFile;
	protected TextView labFileName;
	protected Button btnStart;
	protected Button btnStop;
	private TextView labCurrentTable;
	private TextView labRowsProcessed;
	private TextView areaMessages;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Settings.getInstance().setCurrentContext(this);

		currentState = new ActivityStateWithTeamAndScanPoint(getCurrentStatePrefix());
		currentState.initialize(this, savedInstanceState);

		setContentView(getFormLayoutResourceId());

		initVisualElementVariables();

		refreshHandler = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				if (importState == null) return;

				if (importState.isFinished())
				{
					if (refreshTimer != null)
					{
						refreshTimer.cancel();
						refreshTimer = null;
					}
					rebuildMessagesText();
					importState = null;
				}
				refreshState();
			}
		};

		setTitle();

		refreshAll();
	}

	protected int getFormLayoutResourceId()
	{
		return R.layout.transp_import;
	}

	protected String getCurrentStatePrefix()
	{
		return "transport.import";
	}

	protected void initVisualElementVariables()
	{
		btnSelectFile = (Button) findViewById(R.id.transpImport_selectFile);
		labFileName = (TextView) findViewById(R.id.transpImport_fileName);
		btnStart = (Button) findViewById(R.id.transpImport_startBtn);
		btnStop = (Button) findViewById(R.id.transpImport_stopBtn);
		labCurrentTable = (TextView) findViewById(R.id.transpImport_currentTable);
		labRowsProcessed = (TextView) findViewById(R.id.transpImport_rowsProcessed);
		areaMessages = (TextView) findViewById(R.id.transpImport_messages);

		btnSelectFile.setOnClickListener(new SelectFileClickListener());
		btnStart.setOnClickListener(new StartClickListener());
		btnStop.setOnClickListener(new StopClickListener());
	}

	protected void setTitle()
	{
		setTitle(getResources().getString(R.string.transp_import_dicts_title));
	}

	protected void refreshAll()
	{
		refreshFileName();
		refreshState();
	}

	public ActivityStateWithTeamAndScanPoint getCurrentState()
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
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case REQUEST_CODE_FILE_DIALOG:
				if (resultCode == Activity.RESULT_OK)
				{
					fileName = data.getStringExtra(FileDialog.RESULT_PATH);
					Settings.getInstance().onImportFileSelected(fileName);
				}
				else
				{
					fileName = null;
				}
				refreshFileName();
				refreshState();
				break;
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void refreshFileName()
	{
		if (fileName == null)
		{
			labFileName.setText(getResources().getString(R.string.transp_import_no_file_selected));
		}
		else
		{
			labFileName.setText(fileName);
		}
	}

	protected void refreshState()
	{
		btnSelectFile.setEnabled(!isImportRunning());
		btnStart.setEnabled(isImportPossible());
		btnStop.setEnabled(isImportRunning());
		refreshInfoLabels();
		if (isImportRunning())
		{
			rebuildMessagesText();
		}
	}

	private void refreshInfoLabels()
	{
		if (isImportRunning())
		{
			labCurrentTable.setText(importState.getCurrentTable());
			labRowsProcessed.setText(importState.getProcessedRowsText());
		}
		else
		{
			labCurrentTable.setText(getResources().getString(R.string.transp_import_no_table));
			labRowsProcessed.setText(getResources().getString(R.string.transp_import_no_rows));
		}
	}

	public boolean isImportRunning()
	{
		return importState != null;
	}

	protected boolean isImportPossible()
	{
		return !isImportRunning() && (fileName != null);
	}

	protected void rebuildMessagesText()
	{
		String toAppend = "";
		List<String> extractedMessages = importState.extractMessages();
		if (extractedMessages.size() == 0) return;
		for (String message : extractedMessages)
		{
			toAppend += "\n" + message;
		}
		messagesText = messagesText.concat(toAppend);
		if (messagesText.length() > CONSOLE_CHAR_LIMIT)
		{
			messagesText =
			    messagesText.substring(messagesText.length() - CONSOLE_CHAR_LIMIT, messagesText.length());
		}
		areaMessages.setText(messagesText);
	}

	private void clearMessages()
	{
		messagesText = "";
		areaMessages.setText(messagesText);
	}

	public void startStateCheckTimer()
	{
		if (refreshTimer != null)
		{
			refreshTimer.cancel();
			refreshTimer = null;
		}
		refreshTimer = new Timer();
		refreshTimer.schedule(new UpdateStateTask(this), 0, 500L);
	}

	public Handler getRefreshHandler()
	{
		return refreshHandler;
	}

	private void terminateImport()
	{
		if (refreshTimer != null)
		{
			refreshTimer.cancel();
			refreshTimer = null;
		}
		if (importState != null)
		{
			importState.setTerminated(true);
			rebuildMessagesText();
			importState = null;
		}
		refreshState();
	}

	private class SelectFileClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(getBaseContext(), FileDialog.class);
			intent.putExtra(FileDialog.START_PATH, Settings.getInstance().getImportDir());
			intent.putExtra(FileDialog.CAN_SELECT_DIR, false);
			intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
			intent.putExtra(FileDialog.FORMAT_FILTER, new String[] { ".json" });
			startActivityForResult(intent, REQUEST_CODE_FILE_DIALOG);
		}
	}

	private class StartClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			importState = new ImportState();
			ImportThread thread =
			    new ImportThread(fileName, importState, currentState.getCurrentScanPoint(), TransportImportActivity.this);
			thread.start();
			clearMessages();
			refreshState();
			startStateCheckTimer();
		}
	}

	private class StopClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			terminateImport();
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		currentState.saveToSharedPreferences(getPreferences(MODE_PRIVATE));
		terminateImport();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		currentState.saveToSharedPreferences(getPreferences(MODE_PRIVATE));
		terminateImport();
	}
}
