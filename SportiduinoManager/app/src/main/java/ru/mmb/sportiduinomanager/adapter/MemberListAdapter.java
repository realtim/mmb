package ru.mmb.sportiduinomanager.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import java.util.ArrayList;
import java.util.List;

import ru.mmb.sportiduinomanager.R;

/**
 * Provides the list of team members with checkboxes.
 */
public class MemberListAdapter extends RecyclerView.Adapter<MemberListAdapter.MemberHolder> {
    /**
     * Interface for list item click processing.
     */
    private final OnMemberClicked mOnClick;
    /**
     * Color of member name in the list which presence has not been changed.
     */
    private final int mOriginalColor;
    /**
     * Color of member name in the list which presence has been changed.
     */
    private final int mChangedColor;
    /**
     * List team member names.
     */
    private List<String> mNamesList;
    /**
     * Current mask of team members present at control point,
     * it could be changed by operator.
     */
    private int mMask;
    /**
     * Original mask of team members present at control point,
     * received from chip or local database before operator actions.
     */
    private int mOriginalMask;

    /**
     * Adapter constructor.
     *
     * @param onClick       Interface for click processing in calling activity.
     * @param originalColor Color of members which presence has not been changed.
     * @param changedColor  Color of members which presence has been changed.
     */
    public MemberListAdapter(final OnMemberClicked onClick, final int originalColor,
                      final int changedColor) {
        super();
        mOnClick = onClick;
        mNamesList = new ArrayList<>();
        mMask = 0;
        mOriginalMask = 0;
        mOriginalColor = originalColor;
        mChangedColor = changedColor;
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
        // Get original and current presence flag
        final boolean original = (mOriginalMask & (1 << position)) != 0;
        final boolean current = (mMask & (1 << position)) != 0;
        // Update the contents of the view with that member
        holder.mMember.setText(name);
        holder.mMember.setChecked(current);
        if (current == original) {
            holder.mMember.setTextColor(mOriginalColor);
        } else {
            holder.mMember.setTextColor(mChangedColor);
        }
        // Set my listener for the team member checkbox
        if (mOnClick != null) {
            holder.mMember.setOnClickListener(view -> mOnClick.onMemberClick(holder.getAdapterPosition()));
        }
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
     * @param names        List of team members names
     * @param originalMask Original mask with team members presence at control point
     * @param mask         Current mask which could be changed by operator
     */
    public void updateList(final List<String> names, final int originalMask, final int mask) {
        mNamesList = names;
        mMask = mask;
        mOriginalMask = originalMask;
        notifyDataSetChanged();
    }

    /**
     * Update current team members mask.
     *
     * @param mask New mask value
     */
    public void setMask(final int mask) {
        mMask = mask;
    }

    /**
     * Declare interface for click processing.
     */
    public interface OnMemberClicked {
        /**
         * Implemented in ChipInitActivity class.
         *
         * @param position Position of clicked member in a members list
         */
        void onMemberClick(int position);
    }

    /**
     * Custom ViewHolder for member_list_item layout.
     */
    final class MemberHolder extends RecyclerView.ViewHolder {
        /**
         * Checkbox with team name.
         */
        private final CheckBox mMember;

        /**
         * Holder for list element containing checkbox with team member name.
         *
         * @param view View of list item
         */
        private MemberHolder(final View view) {
            super(view);
            mMember = view.findViewById(R.id.check_member);
            if (mOnClick == null) mMember.setClickable(false);
        }
    }
}
