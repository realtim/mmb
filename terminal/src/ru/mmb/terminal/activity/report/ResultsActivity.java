package ru.mmb.terminal.activity.report;

import ru.mmb.terminal.R;
import ru.mmb.terminal.activity.report.team.search.SearchTeamActivity;
import ru.mmb.terminal.model.registry.Settings;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ResultsActivity extends Activity
{
	private Button btnTeamReport;
	private Button btnGroupReport;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Settings.getInstance().setCurrentContext(this);

		setContentView(R.layout.results);

		btnTeamReport = (Button) findViewById(R.id.results_showTeamReport);
		btnGroupReport = (Button) findViewById(R.id.results_showGroupReport);

		btnTeamReport.setOnClickListener(new TeamReportClickListener());
		btnGroupReport.setOnClickListener(new GroupReportClickListener());
	}

	private class TeamReportClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(getApplicationContext(), SearchTeamActivity.class);
			startActivity(intent);
		}
	}

	private class GroupReportClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			// Intent intent = new Intent(getApplicationContext(), SearchTeamActivity.class);
			// startActivity(intent);
		}
	}
}
