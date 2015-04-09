package ru.mmb.datacollector.activity.report.global;

import static ru.mmb.datacollector.activity.Constants.KEY_REPORT_GLOBAL_RESULT_MESSAGE;
import ru.mmb.datacollector.R;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Toast;

public class ReportGlobalResultActivity extends Activity
{
	private ReportGlobalResultActivityState currentState;

	private RadioButton radioAllTeams;
	private RadioButton radioSelectedTeams;
	private EditText editSelectedTeams;
	private Button btnStart;
	private ProgressBar progressBar;

	private Handler buildReportFinishHandler;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		currentState = new ReportGlobalResultActivityState();
		currentState.initialize(this, savedInstanceState);

		setContentView(R.layout.report_global_result);

		radioAllTeams = (RadioButton) findViewById(R.id.reportGlobal_allTeamsRadio);
		radioSelectedTeams = (RadioButton) findViewById(R.id.reportGlobal_selectedTeamsRadio);
		editSelectedTeams = (EditText) findViewById(R.id.reportGlobal_selectedTeamsEdit);
		btnStart = (Button) findViewById(R.id.reportGlobal_startBuilderBtn);

		progressBar = (ProgressBar) findViewById(R.id.reportGlobal_progressBar);
		progressBar.setVisibility(View.GONE);

		RadioClickListener radioClickListener = new RadioClickListener();
		radioAllTeams.setOnClickListener(radioClickListener);
		radioSelectedTeams.setOnClickListener(radioClickListener);

		btnStart.setOnClickListener(new ButtonStartClickListener());

		editSelectedTeams.addTextChangedListener(new SelectedTeamsTextWatcher());

		buildReportFinishHandler = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				String resultMessage = msg.getData().getString(KEY_REPORT_GLOBAL_RESULT_MESSAGE);
				Toast.makeText(getApplicationContext(), resultMessage, Toast.LENGTH_LONG).show();
				onFinishBuildReportThread();
			}
		};

		setTitle(getResources().getString(R.string.report_global_title));

		refreshState();
	}

	public void refreshState()
	{
		btnStart.setEnabled(true);
		radioAllTeams.setEnabled(true);
		radioSelectedTeams.setEnabled(true);
		editSelectedTeams.setText(currentState.getSelectedTeams());
		if (currentState.getReportMode() == GlobalReportMode.ALL_TEAMS)
		{
			radioAllTeams.setChecked(true);
			editSelectedTeams.setEnabled(false);
		}
		else
		{
			radioSelectedTeams.setChecked(true);
			editSelectedTeams.setEnabled(true);
		}
	}

	public ReportGlobalResultActivityState getCurrentState()
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
	protected void onStop()
	{
		super.onStop();
		currentState.saveToSharedPreferences(getPreferences(MODE_PRIVATE));
	}

	@Override
	protected void onPause()
	{
		updateSelectedTeamsState();
		super.onPause();
	}

	private void updateSelectedTeamsState()
	{
		currentState.setSelectedTeams(editSelectedTeams.getText().toString());
	}

	private void startBuildReportThread()
	{
		onStartBuildResultThread();
		BuildGlobalReportThread thread =
		    new BuildGlobalReportThread(this, buildReportFinishHandler, currentState.getReportMode(), currentState.getSelectedTeams());
		thread.start();
	}

	private void onStartBuildResultThread()
	{
		progressBar.setVisibility(View.VISIBLE);
		radioAllTeams.setEnabled(false);
		radioSelectedTeams.setEnabled(false);
		editSelectedTeams.setEnabled(false);
		btnStart.setEnabled(false);
	}

	private void onFinishBuildReportThread()
	{
		progressBar.setVisibility(View.GONE);
		refreshState();
	}

	private class RadioClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			if (radioAllTeams.isChecked())
			{
				currentState.setReportMode(GlobalReportMode.ALL_TEAMS);
			}
			else
			{
				currentState.setReportMode(GlobalReportMode.SELECTED_TEAMS);
			}
			refreshState();
		}
	}

	private class ButtonStartClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			startBuildReportThread();
		}
	}

	private class SelectedTeamsTextWatcher implements TextWatcher
	{
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count)
		{
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after)
		{
		}

		@Override
		public void afterTextChanged(Editable s)
		{
			updateSelectedTeamsState();
		}
	}
}
