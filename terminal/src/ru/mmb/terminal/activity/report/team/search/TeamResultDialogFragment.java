package ru.mmb.terminal.activity.report.team.search;

import ru.mmb.terminal.model.Team;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class TeamResultDialogFragment extends DialogFragment
{
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		SearchTeamActivity activity = (SearchTeamActivity) getActivity();
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		Team team = activity.getCurrentState().getCurrentTeam();
		builder.setTitle(team.getTeamNum() + "    " + team.getTeamName());
		builder.setMessage(activity.getCurrentState().getResultMessage());
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				// Do nothing.
			}
		});
		return builder.create();
	}
}
