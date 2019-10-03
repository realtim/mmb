package ru.mmb.sportiduinomanager;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ru.mmb.sportiduinomanager.model.Records;
import ru.mmb.sportiduinomanager.model.Teams;

/**
 * Provides the list of teams punched at a station.
 */
public class TeamListAdapter extends RecyclerView.Adapter<TeamListAdapter.TeamHolder> {
    /**
     * Interface for list item click processing.
     */
    private final OnTeamClicked mOnClick;
    /**
     * All teams registered for the raid.
     */
    private final Teams mTeams;
    /**
     * All team punches at connected station (sorted, one last punch per team).
     */
    private final Records mRecords;

    /**
     * Last clicked position in team list.
     */
    private int mSelectedPos;

    /**
     * Adapter constructor.
     *
     * @param onClick Interface for click processing in calling activity.
     * @param teams   List of all registered teams from ControlPointActivity
     * @param records List of all team punches from ControlPointActivity
     */
    TeamListAdapter(final OnTeamClicked onClick, final Teams teams, final Records records) {
        super();
        mOnClick = onClick;
        mTeams = teams;
        mRecords = records;
        mSelectedPos = 0;
    }

    /**
     * Create new views (invoked by the layout manager).
     */
    @NonNull
    @Override
    public TeamListAdapter.TeamHolder onCreateViewHolder(@NonNull final ViewGroup viewGroup,
                                                         final int viewType) {
        final View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.team_list_item, viewGroup, false);
        return new TeamListAdapter.TeamHolder(view);
    }

    /**
     * Replace the contents of a view (invoked by the layout manager).
     */
    @Override
    public void onBindViewHolder(@NonNull final TeamHolder holder, final int position) {
        // Get index of element of mFlash list to display at this position
        int index = mRecords.size() - position - 1;
        if (index < 0) {
            index = 0;
        }
        // Get team number at this position
        final int teamNumber = mRecords.getTeamNumber(index);
        // Get team name for this number
        String teamName;
        if (mTeams == null) {
            teamName = "";
        } else {
            teamName = mTeams.getTeamName(teamNumber);
            if (teamName == null) {
                teamName = holder.itemView.getResources().getString(R.string.unknown);
            }
        }
        // Get members count and team time
        final int teamMask = mRecords.getTeamMask(index);
        int teamMembersCount;
        if (teamMask < 0) {
            teamMembersCount = 0;
        } else {
            teamMembersCount = Teams.getMembersCount(teamMask);
        }
        // Update the contents of the view with that team
        holder.mName.setText(holder.itemView.getResources().getString(R.string.cp_team_name,
                teamNumber, teamName));
        holder.mCount.setText(holder.itemView.getResources().getString(R.string.list_team_count,
                teamMembersCount));
        holder.mTime.setText(holder.itemView.getResources().getString(R.string.list_team_time,
                Records.printTime(mRecords.getTeamTime(index), "dd.MM  HH:mm:ss")));
        // Highlight row if it is selected
        holder.itemView.setSelected(mSelectedPos == position);
        // Set my listener for all elements of list item
        holder.itemView.setOnClickListener(view -> mOnClick.onTeamClick(holder.getAdapterPosition()));
    }

    /**
     * Return the size of team list (invoked by the layout manager).
     */
    @Override
    public int getItemCount() {
        return mRecords.size();
    }

    /**
     * Get position of selected item in RecyclerView list.
     *
     * @return Current selected item index
     */
    int getPosition() {
        return mSelectedPos;
    }

    /**
     * Set new selected item in RecyclerView list.
     *
     * @param position New selected item index
     */
    void setPosition(final int position) {
        mSelectedPos = position;
    }

    /**
     * Declare interface for click processing.
     */
    public interface OnTeamClicked {
        /**
         * Implemented in BluetoothActivity class.
         *
         * @param position Position of clicked device in the list of discovered devices
         */
        void onTeamClick(int position);
    }

    /**
     * Custom ViewHolder for team_list_item layout.
     */
    final class TeamHolder extends RecyclerView.ViewHolder {
        /**
         * Team number and name.
         */
        private final TextView mName;
        /**
         * Current number of members computed from team mask.
         */
        private final TextView mCount;
        /**
         * Time of last punch for the team.
         */
        private final TextView mTime;

        /**
         * Holder for list element containing checkbox with team member name.
         *
         * @param view View of list item
         */
        private TeamHolder(final View view) {
            super(view);
            mName = view.findViewById(R.id.list_team_name);
            mCount = view.findViewById(R.id.list_team_count);
            mTime = view.findViewById(R.id.list_team_time);
        }
    }
}
