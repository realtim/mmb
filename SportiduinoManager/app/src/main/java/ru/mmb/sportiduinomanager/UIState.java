package ru.mmb.sportiduinomanager;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps state of all UI elements to restore after activities recreation.
 */
class UIState {
    /**
     * Email of the user who authorized (or tried to authorize) at the site.
     */
    private String mUserEmail;
    /**
     * Password of the user who authorized (or tried to authorize) at the site.
     */
    private String mUserPassword;
    /**
     * Which website database we are using, main of test.
     */
    private int mTestSite;
    /**
     * List of previously discovered Bluetooth devices.
     */
    private List<BluetoothDevice> mBTDeviceList;
    /**
     * Current team number at chip initialization.
     */
    private int mTeamNumber;
    /**
     * Current members selection mask at chip initialization / control point.
     */
    private int mTeamMask;
    /**
     * Current position in team list at control point.
     */
    private int mTeamListPosition;
    /**
     * True if chip info records will be saved to database.
     */
    private boolean mChipInfoSaveToDB;

    /**
     * Set initial default values for all UI elements.
     */
    UIState() {
        mUserEmail = "";
        mUserPassword = "";
        mTestSite = 0;
        mBTDeviceList = new ArrayList<>();
        mTeamNumber = 0;
        mTeamMask = 0;
        mTeamListPosition = 0;
        mChipInfoSaveToDB = false;
    }

    /**
     * Get the email of the user who authorized (or tried to authorize) at the site.
     *
     * @return User email
     */
    String getUserEmail() {
        return mUserEmail;
    }

    /**
     * Get the password of the user who authorized (or tried to do it) at the site.
     *
     * @return User password
     */
    String getUserPassword() {
        return mUserPassword;
    }

    /**
     * Get info about which website database we are using, main of test.
     *
     * @return True if we are using test database
     */
    int getTestSite() {
        return mTestSite;
    }

    /**
     * Keep email and password of authorized user
     * and info about which website database we are using, main of test.
     *
     * @param userEmail    Email entered by user
     * @param userPassword Password entered by user
     * @param testSite     Test site selection (equal 1 if selected)
     */
    void setAuthorizationParameters(final String userEmail, final String userPassword, final int testSite) {
        mUserEmail = userEmail;
        mUserPassword = userPassword;
        mTestSite = testSite;
    }

    /**
     * Get the list of previously discovered Bluetooth devices.
     *
     * @return List od Bluetooth devices
     */
    List<BluetoothDevice> getBTDeviceList() {
        return mBTDeviceList;
    }

    /**
     * Save the list of previously discovered Bluetooth devices.
     *
     * @param deviceList List od Bluetooth devices
     */
    void setBTDeviceList(final List<BluetoothDevice> deviceList) {
        mBTDeviceList = deviceList;
    }

    /**
     * Get the current team number during chip initialization.
     *
     * @return Team number
     */
    public int getTeamNumber() {
        return mTeamNumber;
    }

    /**
     * Save the current team number during chip initialization.
     *
     * @param teamNumber Team number
     */
    public void setTeamNumber(final int teamNumber) {
        mTeamNumber = teamNumber;
    }

    /**
     * Get the current team mask at chip initialization / control point.
     *
     * @return Team mask
     */
    int getTeamMask() {
        return mTeamMask;
    }

    /**
     * Save the current team mask at chip initialization / control point.
     *
     * @param teamMask Team mask
     */
    void setTeamMask(final int teamMask) {
        mTeamMask = teamMask;
    }

    /**
     * Get the current position in team list.
     *
     * @return Zero-based position
     */
    int getTeamListPosition() {
        return mTeamListPosition;
    }

    /**
     * Save the current position in team list.
     *
     * @param teamListPosition Zero-based position
     */
    void setTeamListPosition(final int teamListPosition) {
        mTeamListPosition = teamListPosition;
    }

    /**
     * Get the value of ChipInfo SaveToDB flag.
     *
     * @return ChipInfoSaveToDB flag value
     */
    boolean getChipInfoSaveToDB() {
        return mChipInfoSaveToDB;
    }

    /**
     * Set the value of ChipInfo SaveToDB flag.
     *
     * @param chipInfoSaveToDB flag value
     */
    void setChipInfoSaveToDB(final boolean chipInfoSaveToDB) {
        mChipInfoSaveToDB = chipInfoSaveToDB;
    }
}
