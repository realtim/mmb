package ru.mmb.terminal.activity.login;

import static ru.mmb.terminal.activity.Constants.KEY_ACTIVE_USER;
import ru.mmb.terminal.R;
import ru.mmb.terminal.model.registry.UsersRegistry;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class LoginActivity extends Activity
{
	private UsersRegistry users;

	private LoginActivityState currentState;

	private String selectedUserName = null;

	private Spinner inputLogin;
	private EditText inputPassword;
	private Button btnOk;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		users = UsersRegistry.getInstance();

		currentState = new LoginActivityState();
		currentState.initialize(this, savedInstanceState);

		setContentView(R.layout.login);

		inputLogin = (Spinner) findViewById(R.id.login_loginInput);
		inputPassword = (EditText) findViewById(R.id.login_passwordInput);
		btnOk = (Button) findViewById(R.id.login_okBtn);

		ArrayAdapter<String> adapter =
		    new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, users.getUserNamesArray());
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		inputLogin.setAdapter(adapter);

		inputLogin.setOnItemSelectedListener(new InputLoginOnItemSelectedListener());
		btnOk.setOnClickListener(new OkBtnClickListener());

		refreshState();
	}

	public void refreshState()
	{
		inputPassword.setText("");
		if (currentState.getActiveUser() != null)
		{
			setTitle(getResources().getString(R.string.login_title_change));
			int userIndex = users.findIndexByName(currentState.getActiveUser().getName());
			inputLogin.setSelection(userIndex);
		}
		else
		{
			setTitle(getResources().getString(R.string.login_title_login));
		}
	}

	private class OkBtnClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			if (selectedUserName != null)
			{
				String userPassword = inputPassword.getText().toString();
				if (users.checkUserPassword(selectedUserName, userPassword))
				{
					Intent resultData = new Intent();
					resultData.putExtra(KEY_ACTIVE_USER, users.findUser(selectedUserName));
					setResult(RESULT_OK, resultData);
					finish();
					return;
				}
			}
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.login_status_bad), Toast.LENGTH_SHORT).show();
		}
	}

	private class InputLoginOnItemSelectedListener implements OnItemSelectedListener
	{
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
		{
			selectedUserName = inputLogin.getItemAtPosition(pos).toString();
		}

		@SuppressWarnings("rawtypes")
		@Override
		public void onNothingSelected(AdapterView parent)
		{
			// Do nothing.
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		currentState.save(outState);
	}
}
