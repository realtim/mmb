package ru.mmb.datacollector.activity.report;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.report.global.ReportGlobalResultActivity;
import ru.mmb.datacollector.activity.report.team.result.TeamResultActivity;
import ru.mmb.datacollector.activity.report.team.search.start.TeamSearchStartActivity;
import ru.mmb.datacollector.model.registry.Settings;

public class ResultsActivity extends Activity {
    private Button btnTeamSearch;
    private Button btnTeamReport;
    private Button btnGroupReport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getInstance().setCurrentContext(this);

        setContentView(R.layout.results);

        btnTeamSearch = (Button) findViewById(R.id.results_searchTeamOnPoint);
        btnTeamReport = (Button) findViewById(R.id.results_showTeamReport);
        btnGroupReport = (Button) findViewById(R.id.results_showGroupReport);

        btnTeamSearch.setOnClickListener(new TeamSearchClickListener());
        btnTeamReport.setOnClickListener(new TeamReportClickListener());
        btnGroupReport.setOnClickListener(new GroupReportClickListener());

        setTitle(getResources().getString(R.string.results_title));
    }

    private class TeamSearchClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), TeamSearchStartActivity.class);
            startActivity(intent);
        }
    }

    private class TeamReportClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), TeamResultActivity.class);
            startActivity(intent);
        }
    }

    private class GroupReportClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), ReportGlobalResultActivity.class);
            startActivity(intent);
        }
    }
}
