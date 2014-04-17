package ru.mmb.terminal.activity.input.withdraw;

import java.util.Date;
import java.util.List;

import ru.mmb.terminal.R;
import ru.mmb.terminal.activity.StateChangeListener;
import ru.mmb.terminal.activity.input.withdraw.list.MembersAdapter;
import ru.mmb.terminal.activity.input.withdraw.list.TeamMemberRecord;
import ru.mmb.terminal.model.registry.Settings;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class WithdrawMemberActivity extends Activity implements StateChangeListener
{
	private WithdrawMemberActivityState currentState;
	private TextView labTeamName;
	private TextView labTeamNumber;
	private TextView labResult;
	private Button btnOk;
	private ListView lvMembers;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Settings.getInstance().setCurrentContext(this);

		currentState = new WithdrawMemberActivityState();
		currentState.initialize(this, savedInstanceState);

		setContentView(R.layout.input_withdraw);

		lvMembers = (ListView) findViewById(R.id.inputWithdraw_withdrawList);
		initListAdapter();

		labTeamName = (TextView) findViewById(R.id.inputWithdraw_teamNameTextView);
		labTeamNumber = (TextView) findViewById(R.id.inputWithdraw_teamNumberTextView);
		labResult = (TextView) findViewById(R.id.inputWithdraw_resultTextView);
		btnOk = (Button) findViewById(R.id.inputWithdraw_okButton);

		setTitle(currentState.getScanPointText(this));

		labTeamName.setText(currentState.getCurrentTeam().getTeamName());
		labTeamNumber.setText(Integer.toString(currentState.getCurrentTeam().getTeamNum()));
		labResult.setText(currentState.getResultText(this));

		btnOk.setOnClickListener(new OkBtnClickListener());

		currentState.addStateChangeListener(this);
	}

	private void initListAdapter()
	{
		List<TeamMemberRecord> items = currentState.getMemberRecords();
		MembersAdapter adapter =
		    new MembersAdapter(this, R.layout.input_withdraw_row, items, currentState);
		lvMembers.setAdapter(adapter);
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

	private class OkBtnClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Date recordDateTime = new Date();
			currentState.saveCurrWithdrawnToDB(recordDateTime);
			currentState.putCurrWithdrawnToDataStorage(recordDateTime);
			setResult(RESULT_OK);
			finish();
		}
	}
}
