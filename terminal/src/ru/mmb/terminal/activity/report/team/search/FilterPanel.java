package ru.mmb.terminal.activity.report.team.search;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

import java.util.HashMap;
import java.util.Map;

import ru.mmb.terminal.R;
import ru.mmb.terminal.widget.EditTextWithSoftKeyboardSupport;
import android.content.Context;
import android.database.DataSetObserver;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class FilterPanel extends ModeSwitchable
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

	private final Map<TeamsAdapter, AdapterObserver> adapterObservers =
	    new HashMap<TeamsAdapter, AdapterObserver>();

	public FilterPanel(SearchTeamActivity context, SearchTeamActivityState currentState)
	{
		super(currentState);
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

		editFilterNumber.setOnEditorActionListener(filterChangeListener);
		editFilterNumber.setSoftKeyboardBackListener(filterChangeListener);
		editFilterNumber.setOnFocusChangeListener(filterChangeListener);

		editFilterTeam.setOnEditorActionListener(filterChangeListener);
		editFilterTeam.setSoftKeyboardBackListener(filterChangeListener);
		editFilterTeam.setOnFocusChangeListener(filterChangeListener);

		editFilterMember.setOnEditorActionListener(filterChangeListener);
		editFilterMember.setSoftKeyboardBackListener(filterChangeListener);
		editFilterMember.setOnFocusChangeListener(filterChangeListener);

		if (context.getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE)
		    btnHideFilter.setEnabled(false);

		if (currentState.isTeamFastSelect()) switchToFastMode();

		refreshFilterVisible();
		refreshFilterNumberExact();
		refreshFilterStatus();
	}

	@Override
	protected void switchToFastMode()
	{
		currentState.setFilterState(FilterState.SHOW_JUST_NUMBER);
		currentState.setFilterNumberExact(true);

		chkFilterNumberExact.setEnabled(false);
		editFilterTeam.setEnabled(false);
		editFilterMember.setEnabled(false);
		btnHideFilter.setEnabled(false);
		btnHideFilter.setText(searchTeamActivity.getResources().getString(R.string.report_team_filter_hide));

		refreshFilterVisible();
		refreshFilterNumberExact();
	}

	@Override
	protected void switchToUsualMode()
	{
		chkFilterNumberExact.setEnabled(true);
		editFilterTeam.setEnabled(true);
		editFilterMember.setEnabled(true);
		btnHideFilter.setEnabled(true);
		btnHideFilter.setText(searchTeamActivity.getResources().getString(R.string.report_team_filter_hide));

		refreshFilterVisible();
		refreshFilterNumberExact();
	}

	public void refreshFilterVisible()
	{
		if (searchTeamActivity.getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE)
		    return;

		if (currentState.isTeamFastSelect())
		{
			panelFilterNumber.setVisibility(View.VISIBLE);
			panelFilterTeamAndMember.setVisibility(View.GONE);
			return;
		}

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

	public void addObserversToAdapters()
	{
		addAdapterObserver(SortColumn.NUMBER);
	}

	private void addAdapterObserver(SortColumn sortColumn)
	{
		TeamsAdapter adapter = searchTeamActivity.getAdapterBySortColumn(sortColumn);
		AdapterObserver observer = new AdapterObserver();
		adapter.registerDataSetObserver(observer);
		adapterObservers.put(adapter, observer);
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
		if (!currentState.isTeamFastSelect()) return;

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

	private class FilterChangeListener implements OnEditorActionListener, OnFocusChangeListener
	{
		@Override
		public void onFocusChange(View v, boolean hasFocus)
		{
			if (!hasFocus)
			{
				refreshFilter();
			}
		}

		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
		{
			if (actionId == EditorInfo.IME_ACTION_UNSPECIFIED
			        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
			{
				if (currentState.isTeamFastSelect())
				{
					TeamsAdapter adapter = searchTeamActivity.getCurrentAdapter();
					AdapterObserver observer = adapterObservers.get(adapter);
					if (observer != null) observer.setActive(true);
				}
				refreshFilter();
			}
			return false;
		}
	}

	private class AdapterObserver extends DataSetObserver
	{
		private boolean active = false;

		@Override
		public void onChanged()
		{
			if (active && currentState.getNumberFilter() != null)
			    searchTeamActivity.selectTeamAndShowReport();
			active = false;
		}

		public void setActive(boolean active)
		{
			this.active = active;
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
}
