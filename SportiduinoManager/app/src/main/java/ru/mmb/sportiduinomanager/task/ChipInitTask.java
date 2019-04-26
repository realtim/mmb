package ru.mmb.sportiduinomanager.task;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;

import ru.mmb.sportiduinomanager.ChipInitActivity;
import ru.mmb.sportiduinomanager.MainApplication;
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
     * Reference to main application thread.
     */
    private final MainApplication mMainApplication;

    /**
     * Retain only a weak reference to the activity.
     *
     * @param context Context of calling activity
     */
    public ChipInitTask(final ChipInitActivity context) {
        super();
        mActivityRef = new WeakReference<>(context);
        mMainApplication = (MainApplication) context.getApplication();
    }

    /**
     * Hide all team data info before chip init start.
     */
    protected void onPreExecute() {
        // Get a reference to the activity if it is still there
        final ChipInitActivity activity = mActivityRef.get();
        if (activity == null || activity.isFinishing()) return;
        // Change chip init state
        activity.setChipInitState(ChipInitActivity.CHIP_INIT_ON);
        // Update activity layout
        activity.updateKeyboardState();
        activity.loadTeam(false);
    }

    @Override
    protected Boolean doInBackground(final Integer... teamParams) {
        final Station station = mMainApplication.getStation();
        // If there is no connected station, then return success immediately
        // This case is hardly possible, as check is performed before reset station call
        if (station == null) return true;

        final int teamNumber = teamParams[0];
        final int teamMask = teamParams[1];
        return station.initChip(teamNumber, teamMask);
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

        activity.setChipInitState(ChipInitActivity.CHIP_INIT_OFF);

        activity.onChipInitResult(result);
    }

}
