package ru.mmb.datacollector.activity.input.history.list;

import ru.mmb.datacollector.R;
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

	private final TextView tvTeamNum;
	private final TextView tvTeamName;
	private final TextView tvScanPointInfo;
	private final TextView tvMembers;
	private final TextView tvDismissed;

	private boolean checked = false;

	public HistoryRow(Context context)
	{
		super(context);
		LayoutInflater.from(context).inflate(R.layout.input_history_row, this, true);
		tvTeamNum = (TextView) findViewById(R.id.inputHistoryRow_teamNumText);
		tvTeamName = (TextView) findViewById(R.id.inputHistoryRow_teamNameText);
		tvScanPointInfo = (TextView) findViewById(R.id.inputHistoryRow_scanPointInfoText);
		tvMembers = (TextView) findViewById(R.id.inputHistoryRow_membersText);
		tvDismissed = (TextView) findViewById(R.id.inputHistoryRow_dismissedText);
		defaultColor = tvTeamNum.getTextColors().getColorForState(ENABLED_STATE_SET, Color.LTGRAY);
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
			tvTeamNum.setTextColor(selectedColor);
			tvTeamName.setTextColor(selectedColor);
			tvScanPointInfo.setTextColor(selectedColor);
			tvMembers.setTextColor(selectedColor);
			tvDismissed.setTextColor(selectedColor);
		}
		else
		{
			tvTeamNum.setTextColor(defaultColor);
			tvTeamName.setTextColor(tanColor);
			tvScanPointInfo.setTextColor(defaultColor);
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
