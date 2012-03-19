package ru.mmb.terminal.activity.main;

import static ru.mmb.terminal.activity.Constants.KEY_SEARCH_TEAM_ACTIVITY_MODE;
import static ru.mmb.terminal.activity.Constants.REQUEST_CODE_LOGIN_ACTIVITY;
import static ru.mmb.terminal.activity.Constants.REQUEST_CODE_SEARCH_TEAM_ACTIVITY;
import ru.mmb.terminal.R;
import ru.mmb.terminal.activity.input.start.StartInputActivity;
import ru.mmb.terminal.activity.input.team.ActivityMode;
import ru.mmb.terminal.activity.input.team.SearchTeamActivity;
import ru.mmb.terminal.activity.login.LoginActivity;
import ru.mmb.terminal.db.TerminalDB;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity
{
	private MainActivityState currentState;

	private Button btnInputData;
	private Button btnSearchTeam;
	private Button btnLogin;

	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		currentState = new MainActivityState();
		currentState.initialize(this, savedInstanceState);

		setContentView(R.layout.main);

		btnInputData = (Button) findViewById(R.id.main_inputDataBtn);
		btnSearchTeam = (Button) findViewById(R.id.main_searchTeamBtn);
		btnLogin = (Button) findViewById(R.id.main_loginBtn);

		btnLogin.setOnClickListener(new LoginClickListener());
		btnInputData.setOnClickListener(new InputDataClickListener());
		btnSearchTeam.setOnClickListener(new SearchTeamClickListener());

		// TODO remove test code
		TerminalDB.getInstance();

		refreshState();
	}

	private void refreshState()
	{
		boolean userLoggedIn = currentState.getActiveUser() != null;
		String userName = getResources().getString(R.string.main_no_active_user);
		if (userLoggedIn) userName = currentState.getActiveUser().getName();

		setTitle(userName);

		btnInputData.setEnabled(userLoggedIn);
		btnSearchTeam.setEnabled(userLoggedIn);

		btnLogin.setEnabled(true);
		if (userLoggedIn)
		{
			btnLogin.setText(R.string.main_change_user);
		}
		else
		{
			btnLogin.setText(R.string.main_login);
		}
	}

	private class LoginClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
			currentState.prepareStartActivityIntent(intent, REQUEST_CODE_LOGIN_ACTIVITY);
			startActivityForResult(intent, REQUEST_CODE_LOGIN_ACTIVITY);
		}
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
			case REQUEST_CODE_LOGIN_ACTIVITY:
				onLoginActivityResult(resultCode, data);
				break;
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void onLoginActivityResult(int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK)
		{
			currentState.loadFromIntent(data);
			refreshState();
		}
	}

	private class InputDataClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			if (currentState.getActiveUser() != null)
			{
				Intent intent = new Intent(getApplicationContext(), StartInputActivity.class);
				currentState.prepareStartActivityIntent(intent, REQUEST_CODE_LOGIN_ACTIVITY);
				startActivity(intent);
			}
			else
			{
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.main_must_login), Toast.LENGTH_SHORT).show();
			}
		}
	}

	private class SearchTeamClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			if (currentState.getActiveUser() != null)
			{
				Intent intent = new Intent(getApplicationContext(), SearchTeamActivity.class);
				currentState.prepareStartActivityIntent(intent, REQUEST_CODE_SEARCH_TEAM_ACTIVITY);
				intent.putExtra(KEY_SEARCH_TEAM_ACTIVITY_MODE, ActivityMode.SEARCH_TEAM);
				startActivity(intent);
			}
			else
			{
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.main_must_login), Toast.LENGTH_SHORT).show();
			}
		}
	}
}
