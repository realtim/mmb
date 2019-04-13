package ru.mmb.sportiduinomanager.task;

import android.os.AsyncTask;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import ru.mmb.sportiduinomanager.BluetoothActivity;
import ru.mmb.sportiduinomanager.MainApplication;
import ru.mmb.sportiduinomanager.model.Station;

/**
 * Run long station reset in separate thread.
 */
public class ResetStationTask extends AsyncTask<Byte, Void, Boolean> {
    /**
     * Reference to parent activity (which can cease to exist in any moment).
     */
    private final WeakReference<BluetoothActivity> mActivityRef;
    /**
     * Reference to main application thread.
     */
    private final MainApplication mMainApplication;
    /**
     * New mode to be set after successful station reset.
     */
    private final byte mNewMode;

    /**
     * Retain only a weak reference to the activity.
     *
     * @param context Context of calling activity
     */
    public ResetStationTask(final BluetoothActivity context, final byte newMode) {
        super();
        mActivityRef = new WeakReference<>(context);
        mMainApplication = (MainApplication) context.getApplication();
        mNewMode = newMode;
    }

    /**
     * Hide all station state controls before reset station call.
     */
    protected void onPreExecute() {
        // Get a reference to the activity if it is still there
        final BluetoothActivity activity = mActivityRef.get();
        if (activity == null || activity.isFinishing()) return;
        // Change reset station state
        activity.setResetStationState(BluetoothActivity.RESET_STATION_ON);
        // Update activity layout
        activity.updateLayout(false);
    }

    @Override
    protected Boolean doInBackground(final Byte... newNumbers) {
        final Station station = mMainApplication.getStation();
        // If there is no connected station, then return success immediately
        // This case is hardly possible, as check is performed before reset station call
        if (station == null) return true;

        final byte newNumber = newNumbers[0];
        return station.resetStation(newNumber);
    }

    /**
     * Show error message in case of station reset failure and update screen layout.
     *
     * @param result False if station reset failed
     */
    protected void onPostExecute(final Boolean result) {
        final Station station = mMainApplication.getStation();
        if (station == null) return;
        // Show error message if station reset failed
        if (!result) {
            Toast.makeText(mMainApplication, station.getLastError(), Toast.LENGTH_LONG).show();
            return;
        }
        // Get a reference to the activity if it is still there
        final BluetoothActivity activity = mActivityRef.get();
        if (activity == null || activity.isFinishing()) return;
        // Call finishing new mode change from main UI thread.
        activity.onStationNumberReset(mNewMode);
    }
}
