package ru.mmb.terminal.activity.input.barcode.list;

import ru.mmb.terminal.R;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BarCodeAdapter extends ArrayAdapter<BarCodeListRecord>
{
	private static final int[] ENABLED_STATE_SET = new int[] { android.R.attr.state_enabled };

	private BarCodeFilter filter = null;

	private boolean defaultColorDefined = false;
	private int defaultColor = -1;
	private final int tanColor;

	public BarCodeAdapter(Context context, int textViewResourceId)
	{
		super(context, textViewResourceId);
		tanColor = context.getResources().getColor(R.color.Tan);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View view = convertView;
		if (view == null)
		{
			view = new BarCodeRow(getContext());
		}
		BarCodeListRecord item = getItem(position);
		if (item != null)
		{
			LinearLayout rowPanel = (LinearLayout) view.findViewById(R.id.inputBarCodeRow_rowPanel);
			if (rowPanel != null) setPanelBackground(rowPanel, position);
			TextView tvCheckTime = (TextView) view.findViewById(R.id.inputBarCodeRow_checkTimeText);
			if (tvCheckTime != null) tvCheckTime.setText(item.getCheckTimeText());
			TextView tvTeamNum = (TextView) view.findViewById(R.id.inputBarCodeRow_teamNumText);
			if (tvTeamNum != null) tvTeamNum.setText(item.getTeamNumberText());
			TextView tvTeamName = (TextView) view.findViewById(R.id.inputBarCodeRow_teamNameText);
			if (tvTeamName != null) tvTeamName.setText(item.getTeamName());
			setRecordTextColors(position, tvCheckTime, tvTeamNum, tvTeamName);
		}
		return view;
	}

	private void setPanelBackground(LinearLayout rowPanel, int position)
	{
		if (position % 2 == 1)
		{
			rowPanel.setBackgroundColor(getContext().getResources().getColor(R.color.Black));
		}
		else
		{
			rowPanel.setBackgroundColor(getContext().getResources().getColor(R.color.PaleBlack));
		}
	}

	private void setRecordTextColors(int position, TextView tvCheckTime, TextView tvTeamNum,
	        TextView tvTeamName)
	{
		if (tvCheckTime == null || tvTeamNum == null || tvTeamName == null) return;

		if (!defaultColorDefined)
		{
			defineDefaultColor(tvCheckTime);
		}
		if (!defaultColorDefined) return;

		if (position == 0)
		{
			tvCheckTime.setTextColor(Color.YELLOW);
			tvTeamNum.setTextColor(Color.YELLOW);
			tvTeamName.setTextColor(Color.YELLOW);
		}
		else
		{
			tvCheckTime.setTextColor(defaultColor);
			tvTeamNum.setTextColor(defaultColor);
			tvTeamName.setTextColor(tanColor);
		}
	}

	private void defineDefaultColor(TextView textView)
	{
		defaultColor = textView.getTextColors().getColorForState(ENABLED_STATE_SET, Color.LTGRAY);
		defaultColorDefined = true;
	}

	@Override
	public Filter getFilter()
	{
		if (filter == null)
		{
			filter = new BarCodeFilter(this);
		}
		return filter;
	}
}
