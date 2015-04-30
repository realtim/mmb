package ru.mmb.datacollector.activity.report.team.result;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.model.Team;

public class TeamResultDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        TeamResultActivity activity = (TeamResultActivity) getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogPanel = inflater.inflate(R.layout.report_team_result_web_view, null);
        WebView webView = (WebView) dialogPanel.findViewById(R.id.reportTeamResult_webView);
        webView.loadData(activity.getCurrentState().getResultMessage(), "text/html; charset=UTF-8", null);
        builder.setView(dialogPanel);
        Team team = activity.getCurrentState().getCurrentTeam();
        builder.setTitle(team.getTeamNum() + "    " + team.getTeamName());
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing.
            }
        });
        return builder.create();
    }
}
