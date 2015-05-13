package ru.mmb.datacollector.activity.report.team.search.start;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.ActivityStateWithTeamAndScanPoint;
import ru.mmb.datacollector.activity.input.scanpoint.SelectScanPointActivity;
import ru.mmb.datacollector.activity.report.team.search.search.TeamSearchActivity;
import ru.mmb.datacollector.model.registry.Settings;

import static ru.mmb.datacollector.activity.Constants.REQUEST_CODE_SCAN_POINT_ACTIVITY;
import static ru.mmb.datacollector.activity.Constants.REQUEST_CODE_TEAM_SEARCH_ACTIVITY;

public class TeamSearchStartActivity extends Activity {
    private ActivityStateWithTeamAndScanPoint currentState;

    private Button btnSelectScanPoint;
    private Button btnSearchTeamOnScanPoint;
    private TextView labScanPoint;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Settings.getInstance().setCurrentContext(this);

        currentState = new ActivityStateWithTeamAndScanPoint("report.team.search.start");
        currentState.initialize(this, savedInstanceState);

        setContentView(R.layout.report_team_search_start);

        btnSelectScanPoint = (Button) findViewById(R.id.teamSearchStart_selectScanPointBtn);
        btnSearchTeamOnScanPoint = (Button) findViewById(R.id.teamSearchStart_searchTeamOnScanPointBtn);
        labScanPoint = (TextView) findViewById(R.id.teamSearchStart_scanPointLabel);

        btnSelectScanPoint.setOnClickListener(new SelectScanPointClickListener());
        btnSearchTeamOnScanPoint.setOnClickListener(new SearchTeamOnScanPointClickListener());

        refreshState();
    }

    private void refreshState()
    {
        setTitle(currentState.getScanPointText(this));

        if (currentState.getCurrentScanPoint() != null)
            labScanPoint.setText(currentState.getCurrentScanPoint().getScanPointName());

        btnSearchTeamOnScanPoint.setEnabled(currentState.isScanPointSelected());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case REQUEST_CODE_SCAN_POINT_ACTIVITY:
                onSelectScanPointActivityResult(resultCode, data);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void onSelectScanPointActivityResult(int resultCode, Intent data)
    {
        if (resultCode == RESULT_OK)
        {
            currentState.loadFromIntent(data);
            refreshState();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        currentState.save(outState);
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        currentState.saveToSharedPreferences(getPreferences(MODE_PRIVATE));
    }

    private class SelectScanPointClickListener implements View.OnClickListener
    {
        @Override
        public void onClick(View v)
        {
            Intent intent = new Intent(getApplicationContext(), SelectScanPointActivity.class);
            currentState.prepareStartActivityIntent(intent, REQUEST_CODE_SCAN_POINT_ACTIVITY);
            startActivityForResult(intent, REQUEST_CODE_SCAN_POINT_ACTIVITY);
        }
    }

    private class SearchTeamOnScanPointClickListener implements View.OnClickListener
    {
        @Override
        public void onClick(View v)
        {
            Intent intent = new Intent(getApplicationContext(), TeamSearchActivity.class);
            currentState.prepareStartActivityIntent(intent, REQUEST_CODE_TEAM_SEARCH_ACTIVITY);
            startActivity(intent);
        }
    }
}
