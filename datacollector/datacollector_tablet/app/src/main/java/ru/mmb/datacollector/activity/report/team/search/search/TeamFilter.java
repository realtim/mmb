package ru.mmb.datacollector.activity.report.team.search.search;

import android.widget.Filter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TeamFilter extends Filter {
    private final TeamsAdapter owner;
    private final List<TeamListRecord> sourceItems = new ArrayList<TeamListRecord>();
    private TeamSearchActivityState currentState;

    public TeamFilter(TeamsAdapter owner) {
        this.owner = owner;
    }

    public void initialize(List<TeamListRecord> sourceItems, TeamSearchActivityState currentState) {
        this.sourceItems.addAll(sourceItems);
        this.currentState = currentState;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        List<TeamListRecord> filteredItems = getFilteredRecords();
        FilterResults result = new FilterResults();
        result.values = filteredItems;
        return result;
    }

    private List<TeamListRecord> getFilteredRecords() {
        List<TeamListRecord> result = new ArrayList<TeamListRecord>();
        for (TeamListRecord item : sourceItems) {
            if (matchesFilter(item)) result.add(item);
        }
        return result;
    }

    private boolean matchesFilter(TeamListRecord item) {
        if (!checkNumberFilter(item)) return false;
        if (!checkTeamFilter(item)) return false;
        if (!checkMemberFilter(item)) return false;
        return true;
    }

    private boolean checkNumberFilter(TeamListRecord item) {
        if (currentState.getFilterPanelState().getNumberFilter() == null) return true;

        if (currentState.getFilterPanelState().isFilterNumberExact()) {
            return item.getTeamNumber() ==
                   currentState.getFilterPanelState().getNumberFilterAsInt();
        } else {
            return Integer.toString(item.getTeamNumber()).contains(currentState.getFilterPanelState().getNumberFilter());
        }
    }

    private boolean checkTeamFilter(TeamListRecord item) {
        if (currentState.getFilterPanelState().getTeamFilter() == null) return true;

        return item.getTeamName().toLowerCase().contains(currentState.getFilterPanelState().getTeamFilter().toLowerCase());
    }

    private boolean checkMemberFilter(TeamListRecord item) {
        if (currentState.getFilterPanelState().getMemberFilter() == null) return true;

        return item.getMembersText().toLowerCase().contains(currentState.getFilterPanelState().getMemberFilter().toLowerCase());
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        @SuppressWarnings("unchecked")
        List<TeamListRecord> filterResult = (List<TeamListRecord>) results.values;
        owner.setNotifyOnChange(false);
        owner.clear();
        if (filterResult != null) {
            for (TeamListRecord item : filterResult) {
                owner.add(item);
            }
            owner.sort(new ArrivalComparator());
        }
        owner.notifyDataSetChanged();
    }

    private class ArrivalComparator implements Comparator<TeamListRecord> {
        @Override
        public int compare(TeamListRecord obj1, TeamListRecord obj2) {
            return -1 * obj1.getArrivalText().compareTo(obj2.getArrivalText());
        }
    }
}
