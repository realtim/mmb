package ru.mmb.datacollector.activity.report.team;

import android.app.Activity;
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

import java.util.ArrayList;
import java.util.List;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.widget.EditTextWithSoftKeyboardSupport;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

public class FilterPanel {
    private final List<FilterStateChangeListener> listeners = new ArrayList<FilterStateChangeListener>();

    private final FilterPanelState panelState;
    private final Activity ownerActivity;

    private final TextView labFilterStatus;
    private final Button btnClearFilter;
    private final Button btnHideFilter;

    private final EditTextWithSoftKeyboardSupport editFilterNumber;
    private final CheckBox chkFilterNumberExact;

    private final EditTextWithSoftKeyboardSupport editFilterTeam;
    private final EditTextWithSoftKeyboardSupport editFilterMember;

    private final LinearLayout panelFilterNumber;
    private final LinearLayout panelFilterTeamAndMember;

    public FilterPanel(Activity ownerActivity, FilterPanelState panelState, int[] fieldIds) {
        this.panelState = panelState;
        this.ownerActivity = ownerActivity;
        FilterChangeListener filterChangeListener = new FilterChangeListener();

        btnClearFilter = (Button) ownerActivity.findViewById(fieldIds[0]);
        btnHideFilter = (Button) ownerActivity.findViewById(fieldIds[1]);
        labFilterStatus = (TextView) ownerActivity.findViewById(fieldIds[2]);

        editFilterNumber = (EditTextWithSoftKeyboardSupport) ownerActivity.findViewById(fieldIds[3]);
        chkFilterNumberExact = (CheckBox) ownerActivity.findViewById(fieldIds[4]);

        editFilterTeam = (EditTextWithSoftKeyboardSupport) ownerActivity.findViewById(fieldIds[5]);
        editFilterMember = (EditTextWithSoftKeyboardSupport) ownerActivity.findViewById(fieldIds[6]);

        panelFilterNumber = (LinearLayout) ownerActivity.findViewById(fieldIds[7]);
        panelFilterTeamAndMember = (LinearLayout) ownerActivity.findViewById(fieldIds[8]);

        btnHideFilter.setOnClickListener(new HideFilterClickListener());
        chkFilterNumberExact.setOnClickListener(new FilterNumberExactClickListener());
        btnClearFilter.setOnClickListener(new ClearFilterClickListener());

        editFilterNumber.addTextChangedListener(filterChangeListener);

        editFilterTeam.addTextChangedListener(filterChangeListener);

        editFilterMember.addTextChangedListener(filterChangeListener);

        if (ownerActivity.getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE)
            btnHideFilter.setEnabled(false);

        btnHideFilter.setText(ownerActivity.getResources().getString(R.string.report_team_filter_hide));

        refreshFilterVisible();
        refreshFilterNumberExact();
        refreshFilterStatus();
    }

    public void refreshFilterVisible() {
        if (ownerActivity.getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE)
            return;

        switch (panelState.getFilterState()) {
            case HIDE_FILTER:
                panelFilterNumber.setVisibility(View.GONE);
                panelFilterTeamAndMember.setVisibility(View.GONE);
                btnHideFilter.setText(ownerActivity.getResources().getString(R.string.report_team_filter_show_number));
                break;
            case SHOW_JUST_NUMBER:
                panelFilterNumber.setVisibility(View.VISIBLE);
                panelFilterTeamAndMember.setVisibility(View.GONE);
                btnHideFilter.setText(ownerActivity.getResources().getString(R.string.report_team_filter_show_full));
                break;
            case SHOW_FULL:
                panelFilterNumber.setVisibility(View.VISIBLE);
                panelFilterTeamAndMember.setVisibility(View.VISIBLE);
                btnHideFilter.setText(ownerActivity.getResources().getString(R.string.report_team_filter_hide));
                break;
        }
    }

    private void refreshFilterNumberExact() {
        chkFilterNumberExact.setChecked(panelState.isFilterNumberExact());
    }

    private void refreshFilterStatus() {
        labFilterStatus.setText(panelState.getFilterStatusText(ownerActivity));
    }

    private void updateCurrentState() {
        panelState.setNumberFilter(editFilterNumber.getText().toString());
        panelState.setTeamFilter(editFilterTeam.getText().toString());
        panelState.setMemberFilter(editFilterMember.getText().toString());
    }

    private void refreshFilter() {
        updateCurrentState();
        refreshFilterStatus();
        fireFilterStateChanged();
        //ownerActivity.refreshTeams();
    }

    private void fireFilterStateChanged() {
        for (FilterStateChangeListener listener : listeners) {
            listener.onFilterStateChange();
        }
    }

    private void clearFilterControls() {
        editFilterNumber.setText("");
        editFilterTeam.setText("");
        editFilterMember.setText("");
    }

    public void reset() {
        clearFilterControls();
        refreshFilter();
    }

    public void focusNumberInputAndShowKeyboard() {
        editFilterNumber.requestFocus();
        InputMethodManager imm =
                (InputMethodManager) ownerActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public void addFilterStateChangeListener(FilterStateChangeListener listener)
    {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    private class HideFilterClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            FilterState nextState = FilterState.getNextState(panelState.getFilterState());
            panelState.setFilterState(nextState);
            refreshFilterVisible();
        }
    }

    private class FilterNumberExactClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            panelState.setFilterNumberExact(!panelState.isFilterNumberExact());
            refreshFilterNumberExact();
            refreshFilter();
        }
    }

    private class ClearFilterClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            clearFilterControls();
            refreshFilter();
        }
    }

    public void setEnabled(boolean value) {
        panelFilterNumber.setEnabled(value);
        panelFilterTeamAndMember.setEnabled(value);
        btnClearFilter.setEnabled(value);
        if (ownerActivity.getResources().getConfiguration().orientation != ORIENTATION_LANDSCAPE) {
            btnHideFilter.setEnabled(value);
        }
    }

    private class FilterChangeListener implements TextWatcher {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            refreshFilter();
        }
    }
}
