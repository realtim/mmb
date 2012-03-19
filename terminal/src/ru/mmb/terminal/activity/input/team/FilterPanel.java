package ru.mmb.terminal.activity.input.team;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import ru.mmb.terminal.R;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class FilterPanel
{
	private final SearchTeamActivityState currentState;
	private final SearchTeamActivity searchTeamActivity;

	private final Button btnRefresh;
	private final TextView labFilterStatus;
	private final Button btnClearFilter;
	private final Button btnHideFilter;
	private final LinearLayout panelFilterEditors;
	private final EditText editFilterNumber;
	private final CheckBox chkFilterNumberExact;
	private final EditText editFilterTeam;
	private final EditText editFilterMember;

	public FilterPanel(SearchTeamActivity context, SearchTeamActivityState currentState)
	{
		this.currentState = currentState;
		this.searchTeamActivity = context;

		btnRefresh = (Button) context.findViewById(R.id.inputTeam_refreshDataButton);
		btnClearFilter = (Button) context.findViewById(R.id.inputTeam_filterClearButton);
		btnHideFilter = (Button) context.findViewById(R.id.inputTeam_filterHideButton);
		labFilterStatus = (TextView) context.findViewById(R.id.inputTeam_filterStatusTextView);
		panelFilterEditors = (LinearLayout) context.findViewById(R.id.inputTeam_filterEditorsPanel);
		editFilterNumber = (EditText) context.findViewById(R.id.inputTeam_filterNumberEdit);
		chkFilterNumberExact =
		    (CheckBox) context.findViewById(R.id.inputTeam_filterNumberExactCheck);
		editFilterTeam = (EditText) context.findViewById(R.id.inputTeam_filterTeamEdit);
		editFilterMember = (EditText) context.findViewById(R.id.inputTeam_filterMemberEdit);

		btnHideFilter.setOnClickListener(new BtnHideFilterClickListener());
		chkFilterNumberExact.setOnClickListener(new ChkFilterNumberExactClickListener());
		btnRefresh.setOnClickListener(new BtnRefreshClickListener());
		btnClearFilter.setOnClickListener(new BtnClearFilterClickListener());

		if (context.getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE)
		    btnHideFilter.setEnabled(false);

		refreshFilterVisible();
		refreshFilterNumberExact();
		refreshFilterStatus();
	}

	public void refreshFilterVisible()
	{
		if (searchTeamActivity.getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE)
		    return;

		if (currentState.isHideFilter())
		{
			panelFilterEditors.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, 0));
			btnHideFilter.setText(searchTeamActivity.getResources().getString(R.string.input_team_filter_show));
		}
		else
		{
			panelFilterEditors.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			btnHideFilter.setText(searchTeamActivity.getResources().getString(R.string.input_team_filter_hide));
		}
	}

	private void refreshFilterNumberExact()
	{
		chkFilterNumberExact.setChecked(currentState.isFilterNumberExact());
	}

	private void refreshFilterStatus()
	{

		labFilterStatus.setText(currentState.getFilterStatusText(searchTeamActivity));
	}

	private void updateCurrentState()
	{
		currentState.setNumberFilter(editFilterNumber.getText().toString());
		currentState.setTeamFilter(editFilterTeam.getText().toString());
		currentState.setMemberFilter(editFilterMember.getText().toString());
	}

	private void onRefreshClick()
	{
		updateCurrentState();
		refreshFilterStatus();
		searchTeamActivity.refreshTeams();
	}

	private class BtnHideFilterClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			currentState.setHideFilter(!currentState.isHideFilter());
			refreshFilterVisible();
		}
	}

	private class ChkFilterNumberExactClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			currentState.setFilterNumberExact(!currentState.isFilterNumberExact());
			refreshFilterNumberExact();
		}
	}

	private class BtnRefreshClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			onRefreshClick();
		}
	}

	private class BtnClearFilterClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			editFilterNumber.setText("");
			editFilterTeam.setText("");
			editFilterMember.setText("");
			onRefreshClick();
		}
	}
}
