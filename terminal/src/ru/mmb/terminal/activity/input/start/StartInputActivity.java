package ru.mmb.terminal.activity.input.start;

import static ru.mmb.terminal.activity.Constants.REQUEST_CODE_INPUT_HISTORY_ACTIVITY;
import static ru.mmb.terminal.activity.Constants.REQUEST_CODE_SCAN_POINT_ACTIVITY;
import ru.mmb.terminal.R;
import ru.mmb.terminal.activity.ActivityStateWithTeamAndScanPoint;
import ru.mmb.terminal.activity.input.history.HistoryActivity;
import ru.mmb.terminal.activity.scanpoint.SelectScanPointActivity;
import ru.mmb.terminal.model.registry.Settings;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class StartInputActivity extends Activity
{
	private ActivityStateWithTeamAndScanPoint currentState;

	private Button btnSelectScanPoint;
	private Button btnProceedInput;
	private TextView labScanPoint;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Settings.getInstance().setCurrentContext(this);

		currentState = new ActivityStateWithTeamAndScanPoint("input.start");
		currentState.initialize(this, savedInstanceState);

		setContentView(R.layout.input_start);

		btnSelectScanPoint = (Button) findViewById(R.id.inputStart_selectScanPointBtn);
		btnProceedInput = (Button) findViewById(R.id.inputStart_proceedInputBtn);
		labScanPoint = (TextView) findViewById(R.id.inputStart_scanPointLabel);

		btnSelectScanPoint.setOnClickListener(new SelectScanPointClickListener());
		btnProceedInput.setOnClickListener(new ProceedInputClickListener());

		refreshState();
	}

	private void refreshState()
	{
		setTitle(currentState.getScanPointText(this));

		if (currentState.getCurrentScanPoint() != null)
		    labScanPoint.setText(currentState.getCurrentScanPoint().getScanPointName());

		btnProceedInput.setEnabled(currentState.isScanPointSelected());
	}

	private class SelectScanPointClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(getApplicationContext(), SelectScanPointActivity.class);
			currentState.prepareStartActivityIntent(intent, REQUEST_CODE_SCAN_POINT_ACTIVITY);
			startActivityForResult(intent, REQUEST_CODE_SCAN_POINT_ACTIVITY);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case REQUEST_CODE_SCAN_POINT_ACTIVITY:
				onSelectScanPointActivityResult(resultCode, data);
				break;
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void onSelectScanPointActivityResult(int resultCode, Intent data)
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
			Intent intent = new Intent(getApplicationContext(), HistoryActivity.class);
			currentState.prepareStartActivityIntent(intent, REQUEST_CODE_INPUT_HISTORY_ACTIVITY);
			startActivity(intent);
		}
	}
}
