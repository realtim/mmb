package ru.mmb.sportiduinomanager.task;

import android.os.AsyncTask;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import ru.mmb.sportiduinomanager.ChipInfoActivity;
import ru.mmb.sportiduinomanager.MainApp;
import ru.mmb.sportiduinomanager.model.Records;
import ru.mmb.sportiduinomanager.model.StationAPI;

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
        // Change layout state
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
        // Send command to connected station
        if (MainApp.mStation == null) return Boolean.FALSE;
        final Boolean result = MainApp.mStation.readCard(StationAPI.CALLER_CHIP_INFO);
        // Save list of punches from the chip in main app
        MainApp.mChipPunches = new Records(0);
        MainApp.mChipPunches.join(MainApp.mStation.getRecords());
        return result;
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
        // Show error message
        if (!result) {
            Toast.makeText(activity, MainApp.mStation.getLastError(true),
                    Toast.LENGTH_LONG).show();
        }
        // Change layout state
        activity.setInfoRequestState(ChipInfoActivity.INFO_REQUEST_OFF);
        // Update activity layout
        activity.updateLayout();
    }
}
