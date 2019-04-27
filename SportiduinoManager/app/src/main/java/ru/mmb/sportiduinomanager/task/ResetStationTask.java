package ru.mmb.sportiduinomanager.task;

import android.os.AsyncTask;

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
     * Retain only a weak reference to the activity.
     *
     * @param context Context of calling activity
     */
    public ResetStationTask(final BluetoothActivity context) {
        super();
        mActivityRef = new WeakReference<>(context);
        mMainApplication = (MainApplication) context.getApplication();
    }

    /**
     * Change station number via station reset.
     *
     * @param newNumbers New station number
     * @return True if succeeded
     */
    protected Boolean doInBackground(final Byte... newNumbers) {
        final Station station = mMainApplication.getStation();
        if (station == null) return false;
        // TODO: Check if some teams data from station was not downloaded and download it
        // Reset station to change it's number
        final byte newNumber = newNumbers[0];
        if (!station.resetStation(newNumber)) return false;
        // Update station mode if needed
        final byte newMode = newNumbers[1];
        if (newMode != station.getMode()) {
            // TODO: Remove sleep for 1 second after firmware correction
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return station.newMode(newMode);
        }
        return true;
    }

    /**
     * Show error message in case of station reset failure and update screen layout.
     *
     * @param result False if station reset failed
     */
    protected void onPostExecute(final Boolean result) {
        // Get a reference to the activity if it is still there
        final BluetoothActivity activity = mActivityRef.get();
        if (activity == null || activity.isFinishing()) return;
        // Process reset result in UI activity
        activity.onStationResetResult(result);
    }

}
