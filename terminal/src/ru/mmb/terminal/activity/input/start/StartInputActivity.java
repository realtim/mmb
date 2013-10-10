package ru.mmb.terminal.activity.input.start;

import static ru.mmb.terminal.activity.Constants.REQUEST_CODE_INPUT_BARCODE_ACTIVITY;
import static ru.mmb.terminal.activity.Constants.REQUEST_CODE_INPUT_HISTORY_ACTIVITY;
import static ru.mmb.terminal.activity.Constants.REQUEST_CODE_LEVEL_ACTIVITY;
import ru.mmb.terminal.R;
import ru.mmb.terminal.activity.ActivityStateWithTeamAndLevel;
import ru.mmb.terminal.activity.LevelPointType;
import ru.mmb.terminal.activity.input.barcode.BarCodeActivity;
import ru.mmb.terminal.activity.input.history.HistoryActivity;
import ru.mmb.terminal.activity.level.SelectLevelActivity;
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
	private ActivityStateWithTeamAndLevel currentState;

	private Button btnSelectLevel;
	private Button btnProceedInput;
	private Button btnScanBarCodes;
	private TextView labDistance;
	private TextView labLevel;
	private TextView labInputMode;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Settings.getInstance().setCurrentContext(this);

		currentState = new ActivityStateWithTeamAndLevel("input.start");
		currentState.initialize(this, savedInstanceState);

		setContentView(R.layout.input_start);

		btnSelectLevel = (Button) findViewById(R.id.inputStart_selectLevelBtn);
		btnProceedInput = (Button) findViewById(R.id.inputStart_proceedInputBtn);
		btnScanBarCodes = (Button) findViewById(R.id.inputStart_scanBarCodesBtn);
		labDistance = (TextView) findViewById(R.id.inputStart_distanceLabel);
		labLevel = (TextView) findViewById(R.id.inputStart_levelLabel);
		labInputMode = (TextView) findViewById(R.id.inputStart_modeLabel);

		btnSelectLevel.setOnClickListener(new SelectLevelClickListener());
		btnProceedInput.setOnClickListener(new ProceedInputClickListener());
		btnScanBarCodes.setOnClickListener(new ScanBarCodesClickListener());

		refreshState();
	}

	private void refreshState()
	{
		setTitle(currentState.getLevelPointText(this));

		if (currentState.getCurrentDistance() != null)
		    labDistance.setText(currentState.getCurrentDistance().getDistanceName());
		if (currentState.getCurrentLevel() != null)
		    labLevel.setText(currentState.getCurrentLevel().getLevelName());
		if (currentState.getCurrentLevelPointType() != null)
		    labInputMode.setText(getResources().getString(currentState.getCurrentLevelPointType().getDisplayNameId()));

		btnProceedInput.setEnabled(currentState.isLevelSelected());
		btnScanBarCodes.setEnabled(currentState.isLevelSelected()
		        && currentState.getCurrentLevelPointType() == LevelPointType.FINISH);
	}

	private class SelectLevelClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(getApplicationContext(), SelectLevelActivity.class);
			currentState.prepareStartActivityIntent(intent, REQUEST_CODE_LEVEL_ACTIVITY);
			startActivityForResult(intent, REQUEST_CODE_LEVEL_ACTIVITY);
		}
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

	private class ScanBarCodesClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(getApplicationContext(), BarCodeActivity.class);
			currentState.prepareStartActivityIntent(intent, REQUEST_CODE_INPUT_BARCODE_ACTIVITY);
			startActivity(intent);
		}
	}
}
