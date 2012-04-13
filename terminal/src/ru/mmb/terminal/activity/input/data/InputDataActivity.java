package ru.mmb.terminal.activity.input.data;

import ru.mmb.terminal.R;
import ru.mmb.terminal.activity.StateChangeListener;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class InputDataActivity extends Activity implements StateChangeListener
{
	private InputDataActivityState currentState;

	private TextView labTeam;
	private TextView labResult;
	private Button btnOk;

	private DatePanel datePanel;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		currentState = new InputDataActivityState();
		currentState.initialize(this, savedInstanceState);

		setContentView(R.layout.input_data);

		datePanel = new DatePanel(this, currentState);
		new CheckpointPanel(this, currentState);

		labTeam = (TextView) findViewById(R.id.inputData_teamNameTextView);
		labResult = (TextView) findViewById(R.id.inputData_resultTextView);
		btnOk = (Button) findViewById(R.id.inputData_okButton);

		setTitle(currentState.getTitleText(this));
		labTeam.setText(currentState.getCurrentTeamText(this));
		labResult.setText(currentState.getResultText(this));

		btnOk.setOnClickListener(new OkBtnClickListener());

		currentState.addStateChangeListener(this);
	}

	@Override
	public void onStateChange()
	{
		labResult.setText(currentState.getResultText(this));
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

	private class OkBtnClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			// TODO restore data saving
			// currentState.saveInputDataToDB();
			finish();
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		datePanel.refreshDateControls();
	}
}
