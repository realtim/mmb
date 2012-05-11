package ru.mmb.terminal.activity.input.team;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

import java.util.ArrayList;
import java.util.List;

import ru.mmb.terminal.R;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FilterPanel
{
	private final SearchTeamActivityState currentState;
	private final SearchTeamActivity searchTeamActivity;

	private final Button btnRefresh;
	private final TextView labFilterStatus;
	private final Button btnClearFilter;
	private final Button btnHideFilter;

	private final EditText editFilterNumber;
	private final CheckBox chkFilterNumberExact;

	private final EditText editFilterTeam;
	private final EditText editFilterMember;

	private final LinearLayout panelFilterNumber;
	private final LinearLayout panelFilterNumberDigits;
	private final LinearLayout panelFilterTeamAndMember;

	private final List<Button> digitButtons = new ArrayList<Button>();
	private final Button btnClearDigit;

	public FilterPanel(SearchTeamActivity context, SearchTeamActivityState currentState)
	{
		this.currentState = currentState;
		this.searchTeamActivity = context;

		btnRefresh = (Button) context.findViewById(R.id.inputTeam_refreshDataButton);
		btnClearFilter = (Button) context.findViewById(R.id.inputTeam_filterClearButton);
		btnHideFilter = (Button) context.findViewById(R.id.inputTeam_filterHideButton);
		labFilterStatus = (TextView) context.findViewById(R.id.inputTeam_filterStatusTextView);

		editFilterNumber = (EditText) context.findViewById(R.id.inputTeam_filterNumberEdit);
		chkFilterNumberExact =
		    (CheckBox) context.findViewById(R.id.inputTeam_filterNumberExactCheck);

		editFilterTeam = (EditText) context.findViewById(R.id.inputTeam_filterTeamEdit);
		editFilterMember = (EditText) context.findViewById(R.id.inputTeam_filterMemberEdit);

		panelFilterNumber = (LinearLayout) context.findViewById(R.id.inputTeam_filterNumberPanel);
		panelFilterNumberDigits =
		    (LinearLayout) context.findViewById(R.id.inputTeam_filterNumberDigitsPanel);
		panelFilterTeamAndMember =
		    (LinearLayout) context.findViewById(R.id.inputTeam_filterTeamAndMemberPanel);

		initDigitButtons(context);
		btnClearDigit = (Button) context.findViewById(R.id.inputTeam_numberCButton);

		btnHideFilter.setOnClickListener(new HideFilterClickListener());
		chkFilterNumberExact.setOnClickListener(new FilterNumberExactClickListener());
		btnRefresh.setOnClickListener(new RefreshClickListener());
		btnClearFilter.setOnClickListener(new ClearFilterClickListener());
		btnClearDigit.setOnClickListener(new ClearDigitClickListener());

		if (context.getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE)
		    btnHideFilter.setEnabled(false);

		refreshFilterVisible();
		refreshFilterNumberExact();
		refreshFilterStatus();
	}

	private void initDigitButtons(SearchTeamActivity context)
	{
		digitButtons.add((Button) context.findViewById(R.id.inputTeam_number0Button));
		digitButtons.add((Button) context.findViewById(R.id.inputTeam_number1Button));
		digitButtons.add((Button) context.findViewById(R.id.inputTeam_number2Button));
		digitButtons.add((Button) context.findViewById(R.id.inputTeam_number3Button));
		digitButtons.add((Button) context.findViewById(R.id.inputTeam_number4Button));
		digitButtons.add((Button) context.findViewById(R.id.inputTeam_number5Button));
		digitButtons.add((Button) context.findViewById(R.id.inputTeam_number6Button));
		digitButtons.add((Button) context.findViewById(R.id.inputTeam_number7Button));
		digitButtons.add((Button) context.findViewById(R.id.inputTeam_number8Button));
		digitButtons.add((Button) context.findViewById(R.id.inputTeam_number9Button));
		for (int i = 0; i < 10; i++)
		{
			digitButtons.get(i).setTag(R.id.digit_button_tag, new Integer(i));
			digitButtons.get(i).setOnClickListener(new DigitClickListener());
		}
	}

	public void refreshFilterVisible()
	{
		if (searchTeamActivity.getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE)
		    return;

		switch (currentState.getFilterState())
		{
			case HIDE_FILTER:
				panelFilterNumber.setVisibility(View.GONE);
				panelFilterNumberDigits.setVisibility(View.GONE);
				panelFilterTeamAndMember.setVisibility(View.GONE);
				btnHideFilter.setText(searchTeamActivity.getResources().getString(R.string.input_team_filter_show_number));
				break;
			case SHOW_JUST_NUMBER:
				panelFilterNumber.setVisibility(View.VISIBLE);
				panelFilterNumberDigits.setVisibility(View.VISIBLE);
				panelFilterTeamAndMember.setVisibility(View.GONE);
				btnHideFilter.setText(searchTeamActivity.getResources().getString(R.string.input_team_filter_show_full));
				break;
			case SHOW_FULL:
				panelFilterNumber.setVisibility(View.VISIBLE);
				panelFilterNumberDigits.setVisibility(View.VISIBLE);
				panelFilterTeamAndMember.setVisibility(View.VISIBLE);
				btnHideFilter.setText(searchTeamActivity.getResources().getString(R.string.input_team_filter_hide));
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

	private void onRefreshClick()
	{
		updateCurrentState();
		refreshFilterStatus();
		searchTeamActivity.refreshTeams();
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

	private class RefreshClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			onRefreshClick();
		}
	}

	private class ClearFilterClickListener implements OnClickListener
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

	private class ClearDigitClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			String prevText = editFilterNumber.getText().toString();
			if (prevText.length() > 0)
			{
				editFilterNumber.setText(prevText.substring(0, prevText.length() - 1));
			}
		}
	}

	private class DigitClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			String prevText = editFilterNumber.getText().toString();
			if (prevText.length() >= 4) return;

			Button digitButton = (Button) v;
			Integer digitValue = (Integer) digitButton.getTag(R.id.digit_button_tag);
			String digit = Integer.toString(digitValue);
			editFilterNumber.setText(prevText + digit);
		}
	}
}
