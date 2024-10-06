package ru.mmb.sportiduinomanager.adapter;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import ru.mmb.sportiduinomanager.MainApp;
import ru.mmb.sportiduinomanager.R;
import ru.mmb.sportiduinomanager.model.Records;

/**
 * Provides the list of control points punched read from a chip.
 */
public class PointListAdapter extends RecyclerView.Adapter<PointListAdapter.PointHolder> {
    /**
     * All punches from chip.
     */
    private Records mRecords = new Records(0);

    /**
     * Adapter constructor.
     *
     * @param records List of all punches in chip from ChipInfoActivity
     */
    public PointListAdapter(final Records records) {
        super();
        copyRecords(records);
    }

    /**
     * Create new views (invoked by the layout manager).
     */
    @NonNull
    @Override
    public PointListAdapter.PointHolder onCreateViewHolder(@NonNull final ViewGroup viewGroup, final int viewType) {
        final View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.point_list_item, viewGroup, false);
        return new PointHolder(view);
    }

    /**
     * Replace the contents of a view (invoked by the layout manager).
     */
    @Override
    public void onBindViewHolder(@NonNull final PointHolder holder, final int position) {
        final Resources resources = holder.itemView.getResources();
        // Get point data at this position
        final int pointNumber = mRecords.getPointNumber(position);
        final String pointName = MainApp.mDistance.getPointName(pointNumber,
                resources.getString(R.string.control_point_prefix));
        final long pointTime = mRecords.getTeamTime(position);
        // Update the contents of the view with that point
        holder.mName.setText(resources.getString(R.string.list_point_name, pointName));
        holder.mTime.setText(resources.getString(R.string.list_point_time,
                Records.printTime(pointTime, "dd.MM  HH:mm:ss")));
    }

    /**
     * Return the size of point list (invoked by the layout manager).
     */
    @Override
    public int getItemCount() {
        return mRecords.size();
    }

    /**
     * Fill list of punches after a new chip has been read.
     *
     * @param records reference to MainApp.mChipPunches
     */
    public void updateList(final Records records) {
        copyRecords(records);
        notifyDataSetChanged();
    }

    /**
     * Copy all records from 'records' except the first to mRecords.
     *
     * @param records reference to MainApp.mChipPunches
     */
    private void copyRecords(final Records records) {
        mRecords = new Records(0);
        for (int i = 1; i < records.size(); i++) {
            mRecords.addRecord(records.getRecord(i));
        }
    }

    /**
     * Custom ViewHolder for point_list_item layout.
     */
    public static final class PointHolder extends RecyclerView.ViewHolder {
        /**
         * Point name.
         */
        private final TextView mName;
        /**
         * Punch time at this point.
         */
        private final TextView mTime;

        /**
         * Holder for list element with point name and punch time.
         *
         * @param view View of list item
         */
        private PointHolder(final View view) {
            super(view);
            mName = view.findViewById(R.id.list_point_name);
            mTime = view.findViewById(R.id.list_point_time);
        }
    }
}
