package ru.mmb.sportiduinomanager;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import ru.mmb.sportiduinomanager.model.Chips;
import ru.mmb.sportiduinomanager.model.Station;
import ru.mmb.sportiduinomanager.model.Teams;

/**
 * Provides ability to get chip events from station, mark team members as absent,
 * update team members mask in a chip and save this data in local database.
 */
public final class ActivePointActivity extends MainActivity {
    /**
     * Main application thread with persistent data.
     */
    private MainApplication mMainApplication;

    /**
     * Station which was previously paired via Bluetooth.
     */
    private Station mStation;

    /**
     * List of teams and team members from local database.
     */
    private Teams mTeams;

    /**
     * Chips events received from all stations.
     */
    private Chips mChips;

    /**
     * Filtered list of events with teams visiting connected station at current point.
     */
    private Chips mVisits;

    @Override
    protected void onCreate(final Bundle instanceState) {
        super.onCreate(instanceState);
        mMainApplication = (MainApplication) getApplication();
        setContentView(R.layout.activity_activepoint);
        updateMenuItems(mMainApplication, R.id.active_point);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Set selection in drawer menu to current mode
        getMenuItem(R.id.active_point).setChecked(true);
        // Disable startup animation
        overridePendingTransition(0, 0);
        // Get connected station from main application thread
        mStation = mMainApplication.getStation();
        // Get teams and members from main application thread
        mTeams = mMainApplication.getTeams();
        //Get chips events from main application thread
        mChips = mMainApplication.getChips();
        // Create filtered chip events for current station and point
        if (mChips != null) {
            mVisits = mChips.getChipsAtPoint(mStation.getNumber(), mStation.getMACasLong());
        }
        // Update activity layout
        updateLayout();
    }

    /**
     * Update layout according to activity state.
     */
    private void updateLayout() {
        // Update number of teams visited this active point
        // (station clock will be updated in background thread)
        ((TextView) findViewById(R.id.ap_total_teams)).setText(getResources()
                .getString(R.string.ap_total_teams, mVisits.size()));
        // Hide last team block when no teams have been visited the station
        if (mVisits == null || mVisits.size() == 0) {
            findViewById(R.id.ap_team_data).setVisibility(View.GONE);
            return;
        }
        // Update last team number and name
        final int teamNumber = mVisits.getLastTeamN();
        if (teamNumber <= 0 || mTeams == null) {
            findViewById(R.id.ap_team_data).setVisibility(View.GONE);
            return;
        }
        ((TextView) findViewById(R.id.ap_team_name)).setText(getResources()
                .getString(R.string.ap_team_name, teamNumber, mTeams.getTeamName(teamNumber)));
        // Update last team time
        final long teamTime = mVisits.getLastTeamTime();
        if (teamTime <= 0) {
            findViewById(R.id.ap_team_data).setVisibility(View.GONE);
            return;
        }
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(teamTime * 1000);
        final DateFormat format = new SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault());
        ((TextView) findViewById(R.id.ap_team_time)).setText(format.format(calendar.getTime()));
        // Update number of team members
        final int teamMask = mVisits.getLastTeamMask();
        if (teamMask <= 0) {
            findViewById(R.id.ap_team_data).setVisibility(View.GONE);
            return;
        }
        ((TextView) findViewById(R.id.ap_members_count)).setText(getResources()
                .getString(R.string.ap_members_count, Teams.getMembersCount(teamMask)));
        // Show last team block
        findViewById(R.id.ap_team_data).setVisibility(View.VISIBLE);
    }

}
