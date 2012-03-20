package ru.mmb.terminal.activity.input.start;

import static ru.mmb.terminal.activity.Constants.REQUEST_CODE_INPUT_LAP_ACTIVITY;
import static ru.mmb.terminal.activity.Constants.REQUEST_CODE_INPUT_TEAM_ACTIVITY;
import ru.mmb.terminal.R;
import ru.mmb.terminal.activity.input.InputActivityState;
import ru.mmb.terminal.activity.input.lap.SelectLapActivity;
import ru.mmb.terminal.activity.input.team.SearchTeamActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class StartInputActivity extends Activity
{
	private InputActivityState currentState;

	private Button btnSelectLap;
	private Button btnProceedInput;
	private TextView labDistance;
	private TextView labLap;
	private TextView labInputMode;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		currentState = new InputActivityState("input.start");
		currentState.initialize(this, savedInstanceState);

		setContentView(R.layout.input_start);

		btnSelectLap = (Button) findViewById(R.id.inputStart_selectLapBtn);
		btnProceedInput = (Button) findViewById(R.id.inputStart_proceedInputBtn);
		labDistance = (TextView) findViewById(R.id.inputStart_distanceLabel);
		labLap = (TextView) findViewById(R.id.inputStart_lapLabel);
		labInputMode = (TextView) findViewById(R.id.inputStart_modeLabel);

		btnSelectLap.setOnClickListener(new SelectLapClickListener());
		btnProceedInput.setOnClickListener(new ProceedInputClickListener());

		refreshState();
	}

	private void refreshState()
	{
		setTitle(currentState.getTitleText(this));

		if (currentState.getCurrentDistance() != null)
		    labDistance.setText(currentState.getCurrentDistance().getName());
		if (currentState.getCurrentLap() != null)
		    labLap.setText(currentState.getCurrentLap().getName());
		if (currentState.getCurrentInputMode() != null)
		    labInputMode.setText(getResources().getString(currentState.getCurrentInputMode().getDisplayNameId()));

		btnProceedInput.setEnabled(currentState.isLapSelected());
	}

	private class SelectLapClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(getApplicationContext(), SelectLapActivity.class);
			currentState.prepareStartActivityIntent(intent, REQUEST_CODE_INPUT_LAP_ACTIVITY);
			startActivityForResult(intent, REQUEST_CODE_INPUT_LAP_ACTIVITY);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case REQUEST_CODE_INPUT_LAP_ACTIVITY:
				onSelectLapActivityResult(resultCode, data);
				break;
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void onSelectLapActivityResult(int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK)
		{
			currentState.loadFromIntent(data);
			refreshState();
		}
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

	private class ProceedInputClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(getApplicationContext(), SearchTeamActivity.class);
			currentState.prepareStartActivityIntent(intent, REQUEST_CODE_INPUT_TEAM_ACTIVITY);
			startActivity(intent);
		}
	}
}
