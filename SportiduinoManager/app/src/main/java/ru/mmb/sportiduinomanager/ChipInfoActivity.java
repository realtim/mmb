package ru.mmb.sportiduinomanager;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

import ru.mmb.sportiduinomanager.model.Records;
import ru.mmb.sportiduinomanager.task.ChipInfoTask;

import static ru.mmb.sportiduinomanager.model.Station.UID_SIZE;

/**
 * Read chip info and display it.
 */
public final class ChipInfoActivity extends MainActivity {
    /**
     * Chip info request not running now.
     */
    public static final int INFO_REQUEST_OFF = 0;
    /**
     * Chip info request is in progress.
     */
    public static final int INFO_REQUEST_ON = 1;

    /**
     * Current state of chip info request.
     */
    private int mInfoRequest = INFO_REQUEST_OFF;

    @Override
    protected void onCreate(final Bundle instanceState) {
        super.onCreate(instanceState);
        setContentView(R.layout.activity_chipinfo);
        updateMenuItems(R.id.chip_info);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Set selection in drawer menu to current mode
        getMenuItem(R.id.chip_info).setChecked(true);
        // Disable startup animation
        overridePendingTransition(0, 0);
        // Update layout
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
     * @param view View of button clicked (unused)
     */
    public void readChipInfo(@SuppressWarnings("unused") final View view) {
        final TextView chipInfoText = findViewById(R.id.chip_info_text);
        chipInfoText.setText("");
        // Check station presence
        if (MainApp.mStation == null) return;
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
            chipInfoText.setText(convertResponseToText(MainApp.mStation.getChipInfo()));
        } else {
            Toast.makeText(getApplicationContext(), MainApp.mStation.getLastError(true),
                    Toast.LENGTH_LONG).show();
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
            builder.append(String.format(Locale.getDefault(), "Page#%2d: %02X %02X %02X %02X",
                    chipInfo[pos], chipInfo[pos + 1], chipInfo[pos + 2], chipInfo[pos + 3],
                    chipInfo[pos + 4]));
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
     * Convert byte array section to int.
     *
     * @param array Array of bytes
     * @param start Starting position of byte sequence which will be converted to int
     * @param end   Ending position of byte sequence
     * @return Long representation of byte sequence
     */
    private long byteArray2Long(final byte[] array, final int start, final int end) {
        long result = 0;
        for (int i = start; i <= end; i++) {
            result = result | (array[i] & 0xFF) << ((end - i) * 8);
        }
        return result;
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
        final int teamNum = (int) byteArray2Long(chipInfo, pos + 1, pos + 2);
        final int ntag = chipInfo[pos + 3] & 0xFF;
        final int version = chipInfo[pos + 4] & 0xFF;
        builder.append(String.format(Locale.getDefault(), "\n\tTeam# %d, Ntag %d, version %d",
                teamNum, ntag, version));
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
        final long initTime = byteArray2Long(chipInfo, pos + 1, pos + 4);
        builder.append("\n\tInitTime: ").append(Records.printTime(initTime, "dd.MM.yyyy  HH:mm:ss"));
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
        final int teamMask = (int) byteArray2Long(chipInfo, pos + 1, pos + 2);
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
        final List<String> teamMembers = MainApp.mTeams.getMembersNames(teamNum);
        if (teamMembers.isEmpty()) return;
        for (int i = 0; i < teamMembers.size(); i++) {
            final String teamMember = teamMembers.get(i);
            String memberActivity;
            if ((teamMask & (1 << i)) == 0) {
                memberActivity = "(-)";
            } else {
                memberActivity = "(+)";
            }
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
        final long pointTime = byteArray2Long(chipInfo, pos + 2, pos + 4) + todayUpperByte;
        builder.append("\n\tKP# ").append(pointNumber).append(", PointTime: ")
                .append(Records.printTime(pointTime, "dd.MM.yyyy  HH:mm:ss"));
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
