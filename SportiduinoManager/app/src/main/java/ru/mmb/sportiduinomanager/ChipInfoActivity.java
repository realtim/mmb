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
import java.util.List;
import java.util.Locale;

import ru.mmb.sportiduinomanager.model.Station;
import ru.mmb.sportiduinomanager.model.Teams;
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
        int teamNum = -1;
        for (int i = 0; i < pagesCount; i++) {
            final int pos = UID_SIZE + i * 5;
            builder.append(String.format("Page#%2d: %02X %02X %02X %02X", chipInfo[pos],
                    chipInfo[pos + 1], chipInfo[pos + 2], chipInfo[pos + 3], chipInfo[pos + 4]));
            if (i == 4) {
                teamNum = buildPage4DecodedInfo(chipInfo, builder, pos);
            } else if (i == 5) {
                buildPage5DecodedInfo(chipInfo, builder, pos);
            } else if (i == 6) {
                buildPage6DecodedInfo(chipInfo, builder, teamNum, pos);
            } else if (i >= 8 && !(chipInfo[pos + 1] == 0 && chipInfo[pos + 2] == 0
                    && chipInfo[pos + 3] == 0 && chipInfo[pos + 4] == 0)) {
                // If all bytes are 0, then don't try to format data
                buildKpPageDecodedInfo(chipInfo, builder, pos);
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    /**
     * Build info for teamNumber, Ntag and software version.
     *
     * @param chipInfo read_card_page response
     * @param builder  target StringBuilder to append text
     * @param pos      current page start position
     * @return decoded team number
     */
    private int buildPage4DecodedInfo(final byte[] chipInfo, final StringBuilder builder, final int pos) {
        final int teamNum = Station.byteArray2Int(chipInfo, pos + 1, pos + 2);
        final int ntag = chipInfo[pos + 3] & 0xFF;
        final int version = chipInfo[pos + 4] & 0xFF;
        builder.append(String.format("\n\tTeam# %d, Ntag %d, version %d", teamNum, ntag, version));
        return teamNum;
    }

    /**
     * Build info for chip init time.
     *
     * @param chipInfo read_card_page response
     * @param builder  target StringBuilder to append text
     * @param pos      current page start position
     */
    private void buildPage5DecodedInfo(final byte[] chipInfo, final StringBuilder builder, final int pos) {
        final int initTime = Station.byteArray2Int(chipInfo, pos + 1, pos + 4);
        builder.append("\n\tInitTime: ").append(DATE_FORMAT.get().format(new Date(initTime * 1000L)));
    }

    /**
     * Build teamMask decoded info.
     *
     * @param chipInfo read_card_page response
     * @param builder  target StringBuilder to append text
     * @param teamNum  current team number
     * @param pos      current page start position
     */
    private void buildPage6DecodedInfo(final byte[] chipInfo, final StringBuilder builder,
                                       final int teamNum, final int pos) {
        final int teamMask = Station.byteArray2Int(chipInfo, pos + 1, pos + 2);
        builder.append("\n\tTeamMask [").append(Integer.toBinaryString(teamMask))
                .append("] to members:");
        buildTeamMembersInfo(builder, teamNum, teamMask);
    }

    /**
     * Append to builder pretty text with current members status in team.
     *
     * @param builder  target StringBuilder to append text
     * @param teamNum  team number, -1 - no team selected
     * @param teamMask active members mask
     */
    private void buildTeamMembersInfo(final StringBuilder builder, final int teamNum, final int teamMask) {
        final Teams teams = mMainApplication.getTeams();
        final List<String> teamMembers = teams.getMembersNames(teamNum);
        if (teamMembers.isEmpty()) return;
        for (int i = 0; i < teamMembers.size(); i++) {
            final String teamMember = teamMembers.get(i);
            final String memberActivity = ((teamMask & (1 << i)) == 0) ? "(-)" : "(+)";
            builder.append("\n\t\t").append(teamMember).append(' ').append(memberActivity);
        }
    }

    /**
     * Build active point decoded info.
     *
     * @param chipInfo read_card_page response
     * @param builder  target StringBuilder to append text
     * @param pos      current page start position
     */
    private void buildKpPageDecodedInfo(final byte[] chipInfo, final StringBuilder builder, final int pos) {
        final int pointNumber = chipInfo[pos + 1] & 0xFF;
        final int todayUnix = (int) (System.currentTimeMillis() / 1000L);
        final int todayUpperByte = todayUnix & 0xFF000000;
        final int pointTime = Station.byteArray2Int(chipInfo, pos + 2, pos + 4) + todayUpperByte;
        builder.append("\n\tKP# ").append(pointNumber).append(", PointTime: ")
                .append(DATE_FORMAT.get().format(new Date(pointTime * 1000L)));
    }

    /**
     * Refresh UI controls state.
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
