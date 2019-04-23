package ru.mmb.sportiduinomanager;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ru.mmb.sportiduinomanager.model.Station;
import ru.mmb.sportiduinomanager.task.ChipInfoTask;

import static ru.mmb.sportiduinomanager.model.Station.UID_SIZE;

/**
 * Read chip info and display it.
 */
public final class ChipInfoActivity extends MainActivity {
    /**
     * Date format for time print.
     */
    private static final ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("dd.MM.yyyy  HH:mm:ss", Locale.getDefault());
        }
    };

    /**
     * Chip info request not running now.
     */
    public static final int INFO_REQUEST_OFF = 0;

    /**
     * Chip info request is in progress.
     */
    public static final int INFO_REQUEST_ON = 1;

    /**
     * Main application thread with persistent data.
     */
    private MainApplication mMainApplication;

    /**
     * Station which was previously paired via Bluetooth.
     */
    private Station mStation;

    /**
     * Current state of chip info request.
     */
    private int mInfoRequest = INFO_REQUEST_OFF;

    @Override
    protected void onCreate(final Bundle instanceState) {
        super.onCreate(instanceState);
        mMainApplication = (MainApplication) getApplication();
        setContentView(R.layout.activity_chipinfo);
        updateMenuItems(mMainApplication, R.id.chip_info);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Set selection in drawer menu to current mode
        getMenuItem(R.id.chip_info).setChecked(true);
        // Disable startup animation
        overridePendingTransition(0, 0);
        // Get connected station from main application thread
        mStation = mMainApplication.getStation();
        updateLayout();
    }

    /**
     * Accessor method to change mInfoRequest from AsyncTask.
     *
     * @param newState new state INFO_REQUEST_ON or INFO_REQUEST_OFF
     */
    public void setInfoRequestState(final int newState) {
        mInfoRequest = newState;
    }

    /**
     * Read chip info.
     *
     * @param view View of button clicked
     */
    public void readChipInfo(final View view) {
        final TextView chipInfoText = findViewById(R.id.chip_info_text);
        chipInfoText.setText("");
        // Check station presence
        if (mStation == null) return;

        // Send command to station and check result
        new ChipInfoTask(this).execute();
    }

    /**
     * Update team controls after chip initialization.
     *
     * @param requestResult success or error
     */
    public void onChipInfoRequestResult(final boolean requestResult) {
        final TextView chipInfoText = findViewById(R.id.chip_info_text);
        if (requestResult) {
            chipInfoText.setText(convertResponseToText(mStation.getChipInfo()));
        } else {
            Toast.makeText(getApplicationContext(), mStation.getLastError(), Toast.LENGTH_LONG).show();
        }
        updateLayout();
    }

    /**
     * Decode byte array to human readable strings.
     *
     * @param chipInfo response byte array from station
     * @return pretty formatted string
     */
    private String convertResponseToText(final byte[] chipInfo) {
        if (chipInfo == null) return "";
        final int pagesCount = (chipInfo.length - UID_SIZE) / 5;
        final StringBuilder builder = new StringBuilder(1024);
        for (int i = 0; i < pagesCount; i++) {
            final int pos = UID_SIZE + i * 5;
            builder.append(String.format("Page#%2d: %02X %02X %02X %02X", chipInfo[pos],
                    chipInfo[pos + 1], chipInfo[pos + 2], chipInfo[pos + 3], chipInfo[pos + 4]));
            if (i == 4) {
                final int teamNum = Station.byteArray2Int(chipInfo, pos + 1, pos + 2);
                final int ntag = chipInfo[pos + 3] & 0xFF;
                final int version = chipInfo[pos + 4] & 0xFF;
                builder.append(String.format("\tTeam# %d, Ntag %d, version %d",
                        teamNum, ntag, version));
            } else if (i == 5) {
                final int initTime = Station.byteArray2Int(chipInfo, pos + 1, pos + 4);
                builder.append("\tInitTime: ")
                        .append(DATE_FORMAT.get().format(new Date(initTime * 1000L)));
            } else if (i == 6) {
                final int teamMask = Station.byteArray2Int(chipInfo, pos + 1, pos + 2);
                builder.append("\tTeamMask: ").append(Integer.toBinaryString(teamMask));
            } else if (i >= 8 && !(chipInfo[pos + 1] == 0 && chipInfo[pos + 2] == 0
                    && chipInfo[pos + 3] == 0 && chipInfo[pos + 4] == 0)) {
                // If all bytes are 0, then don't try to format data
                final int pointNumber = chipInfo[pos + 1] & 0xFF;
                final int todayUnix = (int) (System.currentTimeMillis() / 1000L);
                final int todayUpperByte = todayUnix & 0xFF000000;
                final int pointTime = Station.byteArray2Int(chipInfo, pos + 2, pos + 4) + todayUpperByte;
                builder.append("\tKP# ").append(pointNumber).append(", PointTime: ")
                        .append(DATE_FORMAT.get().format(new Date(pointTime * 1000L)));
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    /**
     * Refresh activity controls state.
     */
    public void updateLayout() {
        final Button infoButton = findViewById(R.id.chip_info_request);
        final ProgressBar infoProgress = findViewById(R.id.chip_info_request_progress);
        final TextView chipInfoText = findViewById(R.id.chip_info_text);
        if (mInfoRequest == INFO_REQUEST_ON) {
            chipInfoText.setVisibility(View.GONE);
            infoButton.setVisibility(View.GONE);
            infoProgress.setVisibility(View.VISIBLE);
        } else {
            chipInfoText.setVisibility(View.VISIBLE);
            infoButton.setVisibility(View.VISIBLE);
            infoProgress.setVisibility(View.GONE);
        }
    }

}
