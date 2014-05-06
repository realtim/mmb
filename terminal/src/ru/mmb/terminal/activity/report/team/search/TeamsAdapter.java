package ru.mmb.terminal.activity.report.team.search;

import java.util.List;

import ru.mmb.terminal.R;
import ru.mmb.terminal.activity.report.team.search.model.TeamListRecord;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

public class TeamsAdapter extends ArrayAdapter<TeamListRecord>
{
	private TeamFilter filter = null;

	public TeamsAdapter(Context context, int textViewResourceId, List<TeamListRecord> items)
	{
		super(context, textViewResourceId, items);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View view = convertView;
		if (view == null)
		{
			view = new TeamRow(getContext());
		}
		TeamListRecord item = getItem(position);
		if (item != null)
		{
			TextView tvNum = (TextView) view.findViewById(R.id.reportTeamRow_numText);
			TextView tvTeam = (TextView) view.findViewById(R.id.reportTeamRow_teamText);
			TextView tvMember = (TextView) view.findViewById(R.id.reportTeamRow_memberText);
			if (tvNum != null) tvNum.setText(Integer.toString(item.getTeamNumber()));
			if (tvTeam != null) tvTeam.setText(item.getTeamName());
			if (tvMember != null) tvMember.setText(item.getMemberText());
		}
		return view;
	}

	@Override
	public Filter getFilter()
	{
		if (filter == null)
		{
			filter = new TeamFilter(this);
		}
		return filter;
	}

	@Override
	public long getItemId(int position)
	{
		return getItem(position).getTeamId();
	}
}
