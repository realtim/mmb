package ru.mmb.datacollector.activity.report.team.search.search;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import ru.mmb.datacollector.R;

public class TeamRow extends LinearLayout
{
    private final TextView tvArrival;
	private final TextView tvNum;
	private final TextView tvTeam;
	private final TextView tvMembers;

	public TeamRow(Context context)
	{
		super(context);
		LayoutInflater.from(context).inflate(R.layout.report_team_search_row, this, true);
        tvArrival = (TextView) findViewById(R.id.teamSearchRow_arrivalText);
		tvNum = (TextView) findViewById(R.id.teamSearchRow_numText);
		tvTeam = (TextView) findViewById(R.id.teamSearchRow_teamText);
		tvMembers = (TextView) findViewById(R.id.teamSearchRow_membersText);
	}
}
