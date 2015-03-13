package ru.mmb.datacollector.activity.report.team.search;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import ru.mmb.datacollector.R;
import ru.mmb.datacollector.widget.EditTextWithSoftKeyboardSupport;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FilterPanel
{
	private final SearchTeamActivityState currentState;
	private final SearchTeamActivity searchTeamActivity;

	private final TextView labFilterStatus;
	private final Button btnClearFilter;
	private final Button btnHideFilter;

	private final EditTextWithSoftKeyboardSupport editFilterNumber;
	private final CheckBox chkFilterNumberExact;

	private final EditTextWithSoftKeyboardSupport editFilterTeam;
	private final EditTextWithSoftKeyboardSupport editFilterMember;

	private final LinearLayout panelFilterNumber;
	private final LinearLayout panelFilterTeamAndMember;

	public FilterPanel(SearchTeamActivity context, SearchTeamActivityState currentState)
	{
		this.currentState = currentState;
		this.searchTeamActivity = context;
		FilterChangeListener filterChangeListener = new FilterChangeListener();

		btnClearFilter = (Button) context.findViewById(R.id.reportTeam_filterClearButton);
		btnHideFilter = (Button) context.findViewById(R.id.reportTeam_filterHideButton);
		labFilterStatus = (TextView) context.findViewById(R.id.reportTeam_filterStatusTextView);

		editFilterNumber =
		    (EditTextWithSoftKeyboardSupport) context.findViewById(R.id.reportTeam_filterNumberEdit);
		chkFilterNumberExact =
		    (CheckBox) context.findViewById(R.id.reportTeam_filterNumberExactCheck);

		editFilterTeam =
		    (EditTextWithSoftKeyboardSupport) context.findViewById(R.id.reportTeam_filterTeamEdit);
		editFilterMember =
		    (EditTextWithSoftKeyboardSupport) context.findViewById(R.id.reportTeam_filterMemberEdit);

		panelFilterNumber = (LinearLayout) context.findViewById(R.id.reportTeam_filterNumberPanel);
		panelFilterTeamAndMember =
		    (LinearLayout) context.findViewById(R.id.reportTeam_filterTeamAndMemberPanel);

		btnHideFilter.setOnClickListener(new HideFilterClickListener());
		chkFilterNumberExact.setOnClickListener(new FilterNumberExactClickListener());
		btnClearFilter.setOnClickListener(new ClearFilterClickListener());

		editFilterNumber.addTextChangedListener(filterChangeListener);

		editFilterTeam.addTextChangedListener(filterChangeListener);

		editFilterMember.addTextChangedListener(filterChangeListener);

		if (context.getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE)
		    btnHideFilter.setEnabled(false);

		btnHideFilter.setText(searchTeamActivity.getResources().getString(R.string.report_team_filter_hide));

		refreshFilterVisible();
		refreshFilterNumberExact();
		refreshFilterStatus();
	}

	public void refreshFilterVisible()
	{
		if (searchTeamActivity.getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE)
		    return;

		switch (currentState.getFilterState())
		{
			case HIDE_FILTER:
				panelFilterNumber.setVisibility(View.GONE);
				panelFilterTeamAndMember.setVisibility(View.GONE);
				btnHideFilter.setText(searchTeamActivity.getResources().getString(R.string.report_team_filter_show_number));
				break;
			case SHOW_JUST_NUMBER:
				panelFilterNumber.setVisibility(View.VISIBLE);
				panelFilterTeamAndMember.setVisibility(View.GONE);
				btnHideFilter.setText(searchTeamActivity.getResources().getString(R.string.report_team_filter_show_full));
				break;
			case SHOW_FULL:
				panelFilterNumber.setVisibility(View.VISIBLE);
				panelFilterTeamAndMember.setVisibility(View.VISIBLE);
				btnHideFilter.setText(searchTeamActivity.getResources().getString(R.string.report_team_filter_hide));
				break;
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

	private void refreshFilter()
	{
		updateCurrentState();
		refreshFilterStatus();
		searchTeamActivity.refreshTeams();
	}

	private void clearFilterControls()
	{
		editFilterNumber.setText("");
		editFilterTeam.setText("");
		editFilterMember.setText("");
	}

	public void reset()
	{
		clearFilterControls();
		refreshFilter();
	}

	public void focusNumberInputAndShowKeyboard()
	{
		editFilterNumber.requestFocus();
		InputMethodManager imm =
		    (InputMethodManager) searchTeamActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
	}

	private class HideFilterClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			FilterState nextState = FilterState.getNextState(currentState.getFilterState());
			currentState.setFilterState(nextState);
			refreshFilterVisible();
		}
	}

	private class FilterNumberExactClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			currentState.setFilterNumberExact(!currentState.isFilterNumberExact());
			refreshFilterNumberExact();
			refreshFilter();
		}
	}

	private class ClearFilterClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			clearFilterControls();
			refreshFilter();
		}
	}

	public void setEnabled(boolean value)
	{
		panelFilterNumber.setEnabled(value);
		panelFilterTeamAndMember.setEnabled(value);
		btnClearFilter.setEnabled(value);
		if (searchTeamActivity.getResources().getConfiguration().orientation != ORIENTATION_LANDSCAPE)
		{
			btnHideFilter.setEnabled(value);
		}
	}

	private class FilterChangeListener implements TextWatcher
	{
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count)
		{
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after)
		{
		}

		@Override
		public void afterTextChanged(Editable s)
		{
			refreshFilter();
		}
	}
}
