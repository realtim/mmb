package ru.mmb.datacollector.activity.report.team.search.search;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import java.util.List;

import ru.mmb.datacollector.R;

public class TeamsAdapter extends ArrayAdapter<TeamListRecord> {
    private TeamFilter filter = null;

    public TeamsAdapter(Context context, int textViewResourceId, List<TeamListRecord> items) {
        super(context, textViewResourceId, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = new TeamRow(getContext());
        }
        TeamListRecord item = getItem(position);
        if (item != null) {
            TextView tvArrival = (TextView) view.findViewById(R.id.teamSearchRow_arrivalText);
            TextView tvNum = (TextView) view.findViewById(R.id.teamSearchRow_numText);
            TextView tvTeam = (TextView) view.findViewById(R.id.teamSearchRow_teamText);
            TextView tvMembers = (TextView) view.findViewById(R.id.teamSearchRow_membersText);
            if (tvArrival != null) tvArrival.setText(item.getArrivalText());
            if (tvNum != null) tvNum.setText(Integer.toString(item.getTeamNumber()));
            if (tvTeam != null) tvTeam.setText(item.getTeamName());
            if (tvMembers != null) tvMembers.setText(item.getMembersText());
        }
        return view;
    }

    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new TeamFilter(this);
        }
        return filter;
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getTeamId();
    }
}
