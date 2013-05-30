package ru.mmb.terminal.activity.input.history.list;

import ru.mmb.terminal.R;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.widget.Checkable;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HistoryRow extends LinearLayout implements Checkable
{
	private final int defaultColor;
	private final int tanColor;
	private final int selectedColor = Color.YELLOW;

	private final TextView tvNum;
	private final TextView tvTeamName;
	private final TextView tvLevelPoint;
	private final TextView tvMembers;
	private final TextView tvDismissed;

	private boolean checked = false;

	public HistoryRow(Context context)
	{
		super(context);
		LayoutInflater.from(context).inflate(R.layout.input_history_row, this, true);
		tvNum = (TextView) findViewById(R.id.inputHistoryRow_numText);
		tvTeamName = (TextView) findViewById(R.id.inputHistoryRow_teamNameText);
		tvLevelPoint = (TextView) findViewById(R.id.inputHistoryRow_levelPointText);
		tvMembers = (TextView) findViewById(R.id.inputHistoryRow_membersText);
		tvDismissed = (TextView) findViewById(R.id.inputHistoryRow_dismissedText);
		defaultColor = tvNum.getTextColors().getColorForState(ENABLED_STATE_SET, Color.LTGRAY);
		tanColor = context.getResources().getColor(R.color.Tan);
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
			tvTeamName.setTextColor(selectedColor);
			tvLevelPoint.setTextColor(selectedColor);
			tvMembers.setTextColor(selectedColor);
			tvDismissed.setTextColor(selectedColor);
		}
		else
		{
			tvNum.setTextColor(defaultColor);
			tvTeamName.setTextColor(tanColor);
			tvLevelPoint.setTextColor(defaultColor);
			tvMembers.setTextColor(defaultColor);
			tvDismissed.setTextColor(defaultColor);
		}
	}

	@Override
	public void toggle()
	{
		checked = !checked;
		updateTextColor();
	}
}
