package ru.mmb.sportiduinomanager.task;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;

import ru.mmb.sportiduinomanager.ChipInfoActivity;
import ru.mmb.sportiduinomanager.MainApp;
import ru.mmb.sportiduinomanager.model.Station;

/**
 * Run long read chip info in separate thread.
 */
public class ChipInfoTask extends AsyncTask<Void, Void, Boolean> {
    /**
     * Reference to parent activity (which can cease to exist in any moment).
     */
    private final WeakReference<ChipInfoActivity> mActivityRef;

    /**
     * Retain only a weak reference to the activity.
     *
     * @param context Context of calling activity
     */
    public ChipInfoTask(final ChipInfoActivity context) {
        super();
        mActivityRef = new WeakReference<>(context);
    }

    /**
     * Hide all team data info before chip init start.
     */
    protected void onPreExecute() {
        // Get a reference to the activity if it is still there
        final ChipInfoActivity activity = mActivityRef.get();
        if (activity == null || activity.isFinishing()) return;
        // Change chip init state
        activity.setInfoRequestState(ChipInfoActivity.INFO_REQUEST_ON);
        // Update activity layout
        activity.updateLayout();
    }

    /**
     * Read data from chip at the connected station.
     *
     * @param params No parameters are sent to this function
     * @return True if succeeded
     */
    protected Boolean doInBackground(final Void... params) {
        if (MainApp.mStation == null) return false;
        return MainApp.mStation.readCardPage((byte) 20, 5, Station.CALLER_CHIP_INFO);
    }

    /**
     * Change chip init state, then perform necessary post-processing.
     *
     * @param result False if chip init failed
     */
    protected void onPostExecute(final Boolean result) {
        // Get a reference to the activity if it is still there
        final ChipInfoActivity activity = mActivityRef.get();
        if (activity == null || activity.isFinishing()) return;

        activity.setInfoRequestState(ChipInfoActivity.INFO_REQUEST_OFF);

        activity.onChipInfoRequestResult(result);
    }

}
