package ru.mmb.terminal.activity.main;

import ru.mmb.terminal.R;
import ru.mmb.terminal.activity.input.start.StartInputActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity
{
	private MainActivityState currentState;

	private Button btnInputData;

	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		currentState = new MainActivityState();
		currentState.initialize(this, savedInstanceState);

		setContentView(R.layout.main);

		btnInputData = (Button) findViewById(R.id.main_inputDataBtn);

		btnInputData.setOnClickListener(new InputDataClickListener());

		refreshState();
	}

	private void refreshState()
	{
		setTitle(getResources().getString(R.string.main_title));
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		currentState.save(outState);
	}

	private class InputDataClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(getApplicationContext(), StartInputActivity.class);
			startActivity(intent);
		}
	}

	/*private class GenerateTeamsClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			FillTeamsAndUsers.execute();
			Toast.makeText(getApplication(), "Teams generated", Toast.LENGTH_LONG).show();
		}
	}*/
}
