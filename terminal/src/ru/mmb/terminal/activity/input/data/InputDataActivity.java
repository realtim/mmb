package ru.mmb.terminal.activity.input.data;

import static ru.mmb.terminal.activity.Constants.REQUEST_CODE_WITHDRAW_MEMBER_ACTIVITY;

import java.util.Date;

import ru.mmb.terminal.R;
import ru.mmb.terminal.activity.StateChangeListener;
import ru.mmb.terminal.activity.input.withdraw.WithdrawMemberActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class InputDataActivity extends Activity implements StateChangeListener
{
	private InputDataActivityState currentState;

	private TextView labTeamName;
	private TextView labTeamNumber;
	private TextView labResult;
	private Button btnOk;
	private Button btnWithdraw;

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

		labTeamName = (TextView) findViewById(R.id.inputData_teamNameTextView);
		labTeamNumber = (TextView) findViewById(R.id.inputData_teamNumberTextView);
		labResult = (TextView) findViewById(R.id.inputData_resultTextView);
		btnOk = (Button) findViewById(R.id.inputData_okButton);
		btnWithdraw = (Button) findViewById(R.id.inputData_withdrawButton);

		setTitle(currentState.getTitleText(this));
		if (currentState.getCurrentTeam() != null)
		{
			labTeamName.setText(currentState.getCurrentTeam().getTeamName());
			labTeamNumber.setText(Integer.toString(currentState.getCurrentTeam().getTeamNum()));
		}
		labResult.setText(currentState.getResultText(this));

		btnOk.setOnClickListener(new OkBtnClickListener());
		btnWithdraw.setOnClickListener(new WithdrawMemberClickListener());

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
			Date recordDateTime = new Date();
			currentState.saveInputDataToDB(recordDateTime);
			currentState.putTeamLevelPointToDataStorage(recordDateTime);
			setResult(RESULT_OK);
			finish();
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		datePanel.refreshDateControls();
	}

	private class WithdrawMemberClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(getApplicationContext(), WithdrawMemberActivity.class);
			currentState.prepareStartActivityIntent(intent, REQUEST_CODE_WITHDRAW_MEMBER_ACTIVITY);
			startActivity(intent);
		}
	}
}
