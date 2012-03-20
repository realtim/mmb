package ru.mmb.terminal.activity.input.lap;

import static ru.mmb.terminal.activity.Constants.KEY_CURRENT_DISTANCE;
import static ru.mmb.terminal.activity.Constants.KEY_CURRENT_INPUT_MODE;
import static ru.mmb.terminal.activity.Constants.KEY_CURRENT_LAP;
import ru.mmb.terminal.R;
import ru.mmb.terminal.activity.StateChangeListener;
import ru.mmb.terminal.activity.input.InputActivityState;
import ru.mmb.terminal.activity.input.InputMode;
import ru.mmb.terminal.model.Distance;
import ru.mmb.terminal.model.Lap;
import ru.mmb.terminal.model.StartType;
import ru.mmb.terminal.model.registry.DistancesRegistry;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.Spinner;

public class SelectLapActivity extends Activity implements StateChangeListener
{
	private InputActivityState currentState;

	private Spinner inputDistance;
	private Spinner inputLap;
	private RadioButton radioStart;
	private RadioButton radioFinish;
	private Button btnOk;

	private DistancesRegistry distances;

	private int prevSelectedDistancePos = -1;
	private int currSelectedDistancePos = -1;

	private int prevSelectedLapPos = -1;
	private int currSelectedLapPos = -1;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		distances = DistancesRegistry.getInstance();

		currentState = new InputActivityState("input.lap");
		currentState.initialize(this, savedInstanceState);

		setContentView(R.layout.input_lap);

		inputDistance = (Spinner) findViewById(R.id.inputLap_distanceInput);
		inputLap = (Spinner) findViewById(R.id.inputLap_lapInput);
		radioStart = (RadioButton) findViewById(R.id.inputLap_startRadio);
		radioFinish = (RadioButton) findViewById(R.id.inputLap_finishRadio);
		btnOk = (Button) findViewById(R.id.inputLap_okBtn);

		setInputDistanceAdapter();

		inputDistance.setOnItemSelectedListener(new InputDistanceOnItemSelectedListener());
		inputLap.setOnItemSelectedListener(new InputLapOnItemSelectedListener());
		radioStart.setOnCheckedChangeListener(new RadioOnCheckedChangeListener());
		radioFinish.setOnCheckedChangeListener(new RadioOnCheckedChangeListener());
		btnOk.setOnClickListener(new OkBtnClickListener());

		initializeControls();

		currentState.addStateChangeListener(this);
		onStateChange();
	}

	private void setInputDistanceAdapter()
	{
		ArrayAdapter<String> adapter =
		    new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, distances.getDistanceNamesArray());
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		inputDistance.setAdapter(adapter);
	}

	private void initializeControls()
	{
		refreshInputDistanceState();

		if (currentState.getCurrentDistance() == null)
		    currentState.setCurrentDistance(distances.getDistanceByIndex(0));
		Distance currentDistance = currentState.getCurrentDistance();
		setInitialDistancePos(currentDistance);

		refreshInputLapState();

		if (currentState.getCurrentLap() == null)
		    currentState.setCurrentLap(currentState.getCurrentDistance().getLapByIndex(0));
		Lap currentLap = currentState.getCurrentLap();
		setInitialLapPos(currentDistance, currentLap);

		refreshInputModeState();
	}

	private void setInitialDistancePos(Distance currentDistance)
	{
		currSelectedDistancePos = distances.getDistanceIndex(currentDistance);
		prevSelectedDistancePos = currSelectedDistancePos;
	}

	private void setInitialLapPos(Distance currentDistance, Lap currentLap)
	{
		currSelectedLapPos = currentDistance.getLapIndex(currentLap);
		prevSelectedLapPos = currSelectedLapPos;
	}

	private void refreshInputDistanceState()
	{
		if (currentState.getCurrentDistance() == null)
		{
			inputDistance.setSelection(0);
		}
		else
		{
			int pos = distances.getDistanceIndex(currentState.getCurrentDistance());
			if (pos == -1) pos = 0;
			inputDistance.setSelection(pos);
		}
	}

	private void refreshInputLapState()
	{
		setInputLapAdapter();

		if (currentState.getCurrentLap() == null || !isLapFromCurrentDistance())
		{
			inputLap.setSelection(0);
		}
		else
		{
			int pos = currentState.getCurrentDistance().getLapIndex(currentState.getCurrentLap());
			if (pos == -1) pos = 0;
			inputLap.setSelection(pos);
		}
	}

	private void setInputLapAdapter()
	{
		ArrayAdapter<String> adapter =
		    new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, currentState.getCurrentDistance().getLapNamesArray());
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		inputLap.setAdapter(adapter);
	}

	private boolean isLapFromCurrentDistance()
	{
		return currentState.getCurrentLap().getDistance() == currentState.getCurrentDistance();
	}

	private void refreshInputModeState()
	{
		Lap lap = currentState.getCurrentLap();

		if (lap == null)
		{
			radioStart.setEnabled(false);
			radioFinish.setEnabled(false);
			currentState.setCurrentInputMode(null);
			return;
		}

		if (lap.getStartType() != StartType.WHEN_READY)
		{
			radioStart.setEnabled(false);
			radioFinish.setEnabled(true);
			radioFinish.setChecked(true);
			currentState.setCurrentInputMode(InputMode.FINISH);
		}
		else
		{
			radioStart.setEnabled(true);
			radioFinish.setEnabled(true);
			if (currentState.getCurrentInputMode() == null)
			{
				radioStart.setChecked(true);
				currentState.setCurrentInputMode(InputMode.START);
			}
			if (currentState.getCurrentInputMode() == InputMode.START) radioStart.setChecked(true);
			if (currentState.getCurrentInputMode() == InputMode.FINISH)
			    radioFinish.setChecked(true);
		}
	}

	private class InputDistanceOnItemSelectedListener implements OnItemSelectedListener
	{
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
		{
			currSelectedDistancePos = pos;
			if (currSelectedDistancePos != prevSelectedDistancePos)
			{
				prevSelectedDistancePos = currSelectedDistancePos;
				currentState.setCurrentDistance(distances.getDistanceByIndex(pos));
				refreshInputLapState();
			}
		}

		@SuppressWarnings("rawtypes")
		@Override
		public void onNothingSelected(AdapterView parent)
		{
			// Do nothing.
		}
	}

	private class InputLapOnItemSelectedListener implements OnItemSelectedListener
	{
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
		{
			currSelectedLapPos = pos;
			if (currSelectedLapPos != prevSelectedLapPos)
			{
				prevSelectedLapPos = currSelectedLapPos;
				Distance distance = currentState.getCurrentDistance();
				currentState.setCurrentLap(distance.getLapByIndex(pos));
				currentState.setCurrentInputMode(null);
				refreshInputModeState();
			}
		}

		@SuppressWarnings("rawtypes")
		@Override
		public void onNothingSelected(AdapterView parent)
		{
			// Do nothing.
		}
	}

	private class RadioOnCheckedChangeListener implements OnCheckedChangeListener
	{
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			if (isChecked)
			{
				if (buttonView == radioStart)
					currentState.setCurrentInputMode(InputMode.START);
				else
					currentState.setCurrentInputMode(InputMode.FINISH);
			}
		}
	}

	private class OkBtnClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent resultData = new Intent();
			if (currentState.getCurrentDistance() != null)
			    resultData.putExtra(KEY_CURRENT_DISTANCE, currentState.getCurrentDistance());
			if (currentState.getCurrentLap() != null)
			    resultData.putExtra(KEY_CURRENT_LAP, currentState.getCurrentLap());
			if (currentState.getCurrentInputMode() != null)
			    resultData.putExtra(KEY_CURRENT_INPUT_MODE, currentState.getCurrentInputMode());
			setResult(RESULT_OK, resultData);
			finish();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		currentState.save(outState);
	}

	@Override
	public void onStateChange()
	{
		setTitle(currentState.getTitleText(this));

		btnOk.setEnabled(currentState.isLapSelected());
	}
}
