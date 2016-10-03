package ru.mmb.datacollector.activity.input.data.withdraw.list;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.input.data.withdraw.WithdrawMemberActivityState;
import ru.mmb.datacollector.model.Participant;

public class MembersAdapter extends ArrayAdapter<TeamMemberRecord> {
    private final WithdrawMemberActivityState currentState;

    public MembersAdapter(Context context, int textViewResourceId, List<TeamMemberRecord> items, WithdrawMemberActivityState currentState) {
        super(context, textViewResourceId, items);
        this.currentState = currentState;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater =
                    (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.input_data_withdraw_row, null);
        }
        TeamMemberRecord item = getItem(position);
        if (item != null) {
            TextView tvMember = (TextView) view.findViewById(R.id.inputWithdrawRow_memberTextView);
            CheckBox checkWithdraw =
                    (CheckBox) view.findViewById(R.id.inputWithdrawRow_withdrawCheckBox);
            TextView tvAlreadyWithdrawn =
                    (TextView) view.findViewById(R.id.inputWithdrawRow_withdrawTextView);
            if (tvMember != null) tvMember.setText(item.getMemberName());
            if (checkWithdraw != null) {
                if (item.isPrevWithdrawn()) {
                    checkWithdraw.setEnabled(false);
                } else {
                    checkWithdraw.setChecked(currentState.isCurrWithdrawn(item.getMember()));
                    checkWithdraw.setOnClickListener(new WithdrawClickListener(item.getMember()));
                }
            }
            if (tvAlreadyWithdrawn != null) {
                if (!item.isPrevWithdrawn()) tvAlreadyWithdrawn.setText("");
            }
        }
        return view;
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getMemberId();
    }

    private class WithdrawClickListener implements OnClickListener {
        private final Participant member;

        public WithdrawClickListener(Participant member) {
            this.member = member;
        }

        @Override
        public void onClick(View v) {
            currentState.setCurrWithdrawn(member, !currentState.isCurrWithdrawn(member));
        }
    }

    public void refresh() {
        clear();
        for (TeamMemberRecord member : currentState.getMemberRecords()) {
            add(member);
        }
        notifyDataSetChanged();
    }

    public void setEnabled(boolean enabled, ListView parent) {
        int firstListItemPosition = parent.getFirstVisiblePosition();
        for (int i = 0; i < parent.getChildCount(); i++) {
            int pos = firstListItemPosition + i;
            View rowView = parent.getChildAt(i);
            // TODO finish
            /*
            if ()
            CheckBox
            */
        }
    }
}
