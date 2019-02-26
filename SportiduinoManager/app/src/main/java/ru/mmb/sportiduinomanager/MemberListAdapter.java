package ru.mmb.sportiduinomanager;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides the list of team members with checkboxes.
 */
public class MemberListAdapter extends RecyclerView.Adapter<MemberListAdapter.MemberHolder> {
    /**
     * Interface for list item click processing.
     */
    private final OnItemClicked mOnClick;
    /**
     * List team member names.
     */
    private List<String> mNamesList;
    /**
     * Mask of team members present at active point.
     */
    private int mMask;

    /**
     * Adapter constructor.
     *
     * @param onClick Interface for click processing in calling activity.
     */
    MemberListAdapter(final OnItemClicked onClick) {
        super();
        mOnClick = onClick;
        mNamesList = new ArrayList<>();
        mMask = 0;
    }

    /**
     * Create new views (invoked by the layout manager).
     */
    @NonNull
    @Override
    public MemberListAdapter.MemberHolder onCreateViewHolder(@NonNull final ViewGroup viewGroup, final int viewType) {
        final View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.member_list_item, viewGroup, false);
        return new MemberListAdapter.MemberHolder(view);
    }

    /**
     * Replace the contents of a view (invoked by the layout manager).
     */
    @Override
    public void onBindViewHolder(@NonNull final MemberHolder holder, final int position) {
        // Get team member at this position
        final String name = mNamesList.get(position);
        // Update the contents of the view with that member
        holder.mMember.setText(name);
        holder.mMember.setChecked((mMask & (1 << position)) != 0);
        // Set my listener for the team member checkbox
        holder.mMember.setOnClickListener(view -> mOnClick.onItemClick(holder.getAdapterPosition()));
    }

    /**
     * Return the size of member list (invoked by the layout manager).
     */
    @Override
    public int getItemCount() {
        return mNamesList.size();
    }

    /**
     * Fill list of members after selecting new team.
     *
     * @param names List of team members names
     * @param mask  Bit mask with team members presence at active point
     */
    void fillList(final List<String> names, final int mask) {
        mNamesList = names;
        mMask = mask;
        notifyDataSetChanged();
    }

    /**
     * Declare interface for click processing.
     */
    public interface OnItemClicked {
        /**
         * Implemented in BluetoothActivity class.
         *
         * @param position Position of clicked device in the list of discovered devices
         */
        void onItemClick(int position);
    }

    /**
     * Custom ViewHolder for member_list_item layout.
     */
    class MemberHolder extends RecyclerView.ViewHolder {
        /**
         * Checkbox with team name.
         */
        private final CheckBox mMember;

        /**
         * Holder for list element containing checkbox with team member name.
         *
         * @param view View of list item
         */
        MemberHolder(final View view) {
            super(view);
            mMember = view.findViewById(R.id.check_member);
        }
    }
}
