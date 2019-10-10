package ru.mmb.sportiduinomanager.task;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;

import ru.mmb.sportiduinomanager.ChipInitActivity;
import ru.mmb.sportiduinomanager.MainApp;
import ru.mmb.sportiduinomanager.model.Station;

/**
 * Run long chip init in separate thread.
 */
public class ChipInitTask extends AsyncTask<Integer, Void, Boolean> {
    /**
     * Reference to parent activity (which can cease to exist in any moment).
     */
    private final WeakReference<ChipInitActivity> mActivityRef;

    /**
     * Retain only a weak reference to the activity.
     *
     * @param context Context of calling activity
     */
    public ChipInitTask(final ChipInitActivity context) {
        super();
        mActivityRef = new WeakReference<>(context);
    }

    /**
     * Run initChip command at connected station.
     *
     * @param teamParams Tam number and mask
     * @return True if succeeded
     */
    protected Boolean doInBackground(final Integer... teamParams) {
        if (MainApp.mStation == null) return false;
        // Send the command to station
        final int teamNumber = teamParams[0];
        final int teamMask = teamParams[1];
        return MainApp.mStation.initChip(teamNumber, teamMask, Station.CALLER_CHIP_INIT);
    }

    /**
     * Change chip init state, then perform necessary post-processing.
     *
     * @param result False if chip init failed
     */
    protected void onPostExecute(final Boolean result) {
        // Get a reference to the activity if it is still there
        final ChipInitActivity activity = mActivityRef.get();
        if (activity == null || activity.isFinishing()) return;
        // Process initialization result in UI activity
        activity.onChipInitResult(result);
    }

}
