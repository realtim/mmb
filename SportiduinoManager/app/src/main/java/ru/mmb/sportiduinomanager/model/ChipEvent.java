package ru.mmb.sportiduinomanager.model;

/**
 * A chip event (initialization or check in).
 */
final class ChipEvent implements Comparable<ChipEvent> {
    /**
     * Chip event status: new, exists only in app memory.
     */
    static final int STATUS_NEW = 0;
    /**
     * Chip event status: saved to local SQLite database.
     */
    static final int STATUS_SAVED = 1;

    /**
     * Chip event status: sent to site.
     */
    static final int STATUS_SENT = 2;

    /**
     * Station Bluetooth adapter MAC as long integer.
     */
    final long mStationMAC;

    /**
     * Station time at the moment when event was received by app.
     */
    final int mStationTime;

    /**
     * Time difference between station and Android.
     */
    final int mStationDrift;

    /**
     * Current number of the paired station.
     */
    final int mStationNumber;

    /**
     * Station mode (initialization or active point).
     */
    final int mStationMode;

    /**
     * Initialization time of the chip.
     */
    final int mInitTime;

    /**
     * Team number from the chip.
     */
    final int mTeamNumber;

    /**
     * Team members mask from the chip.
     */
    final int mTeamMask;

    /**
     * Active point for registered event.
     */
    final int mPointNumber;

    /**
     * Time of registered event.
     */
    final int mPointTime;

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
    int getStatus() {
        return mStatus;
    }

    /**
     * Set new chip event status.
     *
     * @param status Event status (new, saved, sent)
     */
    void setStatus(final int status) {
        mStatus = status;
    }

    /**
     * Get station mode for the chip event
     * to distinguish between chip init and active point check in.
     *
     * @return See Station.MODE_* constants
     */
    int getMode() {
        return mStationMode;
    }

    @Override
    public int compareTo(final ChipEvent compareEvent) {
        // For ascending order
        return this.mPointTime - compareEvent.mPointTime;
    }

    /**
     * Get string representation of chip event for sending to site.
     *
     * @return Chip event converted to string
     */
    public String toString() {
        return Long.toString(mStationMAC) + '\t' + mStationTime + '\t' + mStationDrift + '\t'
                + mStationNumber + '\t' + mStationMode + '\t' + mInitTime + '\t' + mTeamNumber
                + '\t' + mTeamMask + '\t' + mPointNumber + '\t' + mPointTime;
    }
}
