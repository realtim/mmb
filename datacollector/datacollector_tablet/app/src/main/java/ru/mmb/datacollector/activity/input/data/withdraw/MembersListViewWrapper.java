package ru.mmb.datacollector.activity.input.data.withdraw;

import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.input.data.withdraw.list.MembersAdapter;
import ru.mmb.datacollector.activity.input.data.withdraw.list.TeamMemberRecord;

public class MembersListViewWrapper {

    private final WithdrawMemberActivity owner;
    private final WithdrawMemberActivityState currentState;
    private ListView lvMembers;
    private MembersAdapter lvMembersAdapter;


    public MembersListViewWrapper(WithdrawMemberActivity owner, WithdrawMemberActivityState currentState) {
        this.owner = owner;
        this.currentState = currentState;
        initListView();
    }

    private void initListView() {
        lvMembers = (ListView) owner.findViewById(R.id.inputWithdraw_withdrawList);
        initListAdapter();
    }

    private void initListAdapter() {
        lvMembersAdapter = new MembersAdapter(owner, R.layout.input_data_withdraw_row, currentState);
        lvMembers.setAdapter(lvMembersAdapter);
    }

    public void disableCheckboxes() {
        int firstListItemPosition = lvMembers.getFirstVisiblePosition();
        for (int i = 0; i < lvMembers.getChildCount(); i++) {
            int pos = firstListItemPosition + i;
            View rowView = lvMembers.getChildAt(i);
            TeamMemberRecord item = lvMembersAdapter.getItem(pos);
            CheckBox checkBox = (CheckBox) rowView.findViewById(R.id.inputWithdrawRow_withdrawCheckBox);
            checkBox.setEnabled(false);
        }
    }

    public void onStateReloaded() {
        lvMembersAdapter = new MembersAdapter(owner, R.layout.input_data_withdraw_row, currentState);
        lvMembers.setAdapter(lvMembersAdapter);
    }
}
