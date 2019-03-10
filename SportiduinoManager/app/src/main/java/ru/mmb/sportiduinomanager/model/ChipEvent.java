package ru.mmb.sportiduinomanager.model;

/**
 * A chip event (initialization or check in).
 */
final class ChipEvent {
    /**
     * Chip event status: new, exists only in app memory.
     */
    public static final int STATUS_NEW = 0;
    /**
     * Chip event status: saved to local SQLite database.
     */
    public static final int STATUS_SAVED = 1;
    //TODO: public static final int STATUS_SENT = 2;

    /**
     * Station Bluetooth adapter MAC as long integer.
     */
    public final long mStationMAC;

    /**
     * Station time at the moment when event was received by app.
     */
    public final int mStationTime;

    /**
     * Time difference between station and Android.
     */
    public final int mStationDrift;

    /**
     * Current number of the paired station.
     */
    public final int mStationNumber;

    /**
     * Station mode (initialization or active point).
     */
    public final int mStationMode;

    /**
     * Initialization time of the chip.
     */
    public final int mInitTime;

    /**
     * Team number from the chip.
     */
    public final int mTeamNumber;

    /**
     * Team members mask from the chip.
     */
    public final int mTeamMask;

    /**
     * Active point for registered event.
     */
    public final int mPointNumber;

    /**
     * Time of registered event.
     */
    public final int mPointTime;

    /**
     * Status of event processing (new, saved, sent).
     */
    private int mStatus;

    /**
     * Constructor for ChipEvent class.
     *
     * @param stationMAC    Station Bluetooth adapter MAC as long integer
     * @param stationTime   Station time at the moment when event was received by app
     * @param stationDrift  Time difference between station and Android
     * @param stationNumber Current number of the paired station
     * @param stationMode   Station mode (initialization or active point)
     * @param initTime      Initialization time of the chip
     * @param teamNumber    Team number from the chip
     * @param teamMask      Team members mask from the chip
     * @param pointNumber   Active point for registered event
     * @param pointTime     Time of registered event
     * @param status        Status of event processing (new, saved, sent)
     */
    ChipEvent(final long stationMAC, final int stationTime, final int stationDrift,
              final int stationNumber, final int stationMode, final int initTime,
              final int teamNumber, final int teamMask, final int pointNumber,
              final int pointTime, final int status) {
        mStationMAC = stationMAC;
        mStationTime = stationTime;
        mStationDrift = stationDrift;
        mStationNumber = stationNumber;
        mStationMode = stationMode;
        mInitTime = initTime;
        mTeamNumber = teamNumber;
        mTeamMask = teamMask;
        mPointNumber = pointNumber;
        mPointTime = pointTime;
        mStatus = status;
    }

    /**
     * Get current chip event status.
     *
     * @return Event status (new, saved, sent)
     */
    public int getStatus() {
        return mStatus;
    }

    /**
     * Set new chip event status.
     *
     * @param status Event status (new, saved, sent)
     */
    public void setStatus(final int status) {
        mStatus = status;
    }
}
