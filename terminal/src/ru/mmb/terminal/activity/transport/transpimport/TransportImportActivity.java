package ru.mmb.terminal.activity.transport.transpimport;

import static ru.mmb.terminal.activity.Constants.REQUEST_CODE_FILE_DIALOG_ACTIVITY;

import java.util.List;
import java.util.Timer;

import ru.mmb.terminal.R;
import ru.mmb.terminal.model.registry.Settings;
import ru.mmb.terminal.transport.importer.ImportState;
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

	private String fileName = null;
	private String messagesText = "";
	private ImportState importState = null;
	private Handler refreshHandler;
	private Timer refreshTimer = null;

	private Button btnSelectFile;
	private TextView labFileName;
	private Button btnStart;
	private Button btnStop;
	private TextView labCurrentTable;
	private TextView labRowsProcessed;
	private TextView areaMessages;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Settings.getInstance().setCurrentContext(this);

		setContentView(R.layout.transp_import);

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

		setTitle(getResources().getString(R.string.transp_import_title));

		refreshFileName();
		refreshState();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case REQUEST_CODE_FILE_DIALOG_ACTIVITY:
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

	private void refreshState()
	{
		if (importState == null)
		{
			btnSelectFile.setEnabled(true);
			btnStart.setEnabled(fileName != null);
			btnStop.setEnabled(false);
			labCurrentTable.setText(getResources().getString(R.string.transp_import_no_table));
			labRowsProcessed.setText(getResources().getString(R.string.transp_import_no_rows));
		}
		else
		{
			btnSelectFile.setEnabled(false);
			btnStart.setEnabled(false);
			btnStop.setEnabled(true);
			labCurrentTable.setText(importState.getCurrentTable());
			labRowsProcessed.setText(importState.getProcessedRowsText());
			rebuildMessagesText();
		}
	}

	private void rebuildMessagesText()
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
			startActivityForResult(intent, REQUEST_CODE_FILE_DIALOG_ACTIVITY);
		}
	}

	private class StartClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			importState = new ImportState();
			ImportThread thread =
			    new ImportThread(fileName, importState, TransportImportActivity.this);
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
		terminateImport();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		terminateImport();
	}
}
