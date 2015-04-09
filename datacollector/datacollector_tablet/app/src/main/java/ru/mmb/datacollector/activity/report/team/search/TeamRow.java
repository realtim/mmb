package ru.mmb.datacollector.activity.report.team.search;

import ru.mmb.datacollector.R;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.widget.Checkable;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TeamRow extends LinearLayout implements Checkable
{
	private final int defaultColor;
	private final int selectedColor = Color.YELLOW;

	private final TextView tvNum;
	private final TextView tvTeam;
	private final TextView tvMember;

	private boolean checked = false;

	public TeamRow(Context context)
	{
		super(context);
		LayoutInflater.from(context).inflate(R.layout.report_team_search_row, this, true);
		tvNum = (TextView) findViewById(R.id.reportTeamRow_numText);
		tvTeam = (TextView) findViewById(R.id.reportTeamRow_teamText);
		tvMember = (TextView) findViewById(R.id.reportTeamRow_memberText);
		defaultColor = tvNum.getTextColors().getColorForState(ENABLED_STATE_SET, Color.LTGRAY);
	}

	@Override
	public boolean isChecked()
	{
		return checked;
	}

	@Override
	public void setChecked(boolean value)
	{
		if (checked != value)
		{
			checked = value;
			updateTextColor();
		}
	}

	private void updateTextColor()
	{
		if (checked)
		{
			tvNum.setTextColor(selectedColor);
			tvTeam.setTextColor(selectedColor);
			tvMember.setTextColor(selectedColor);
		}
		else
		{
			tvNum.setTextColor(defaultColor);
			tvTeam.setTextColor(defaultColor);
			tvMember.setTextColor(defaultColor);
		}
	}

	@Override
	public void toggle()
	{
		checked = !checked;
		updateTextColor();
	}
}
