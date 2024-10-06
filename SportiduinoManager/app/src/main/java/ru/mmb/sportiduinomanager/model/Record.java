package ru.mmb.sportiduinomanager.model;

import androidx.annotation.NonNull;

/**
 * A Sportiduino record (chip initialization or station punch).
 */
public final class Record implements Comparable<Record> {
    /**
     * Record status: new, exists only in app memory.
     */
    static final int STATUS_NEW = 0;
    /**
     * Record status: saved to local SQLite database.
     */
    static final int STATUS_SAVED = 1;

    /**
     * Record status: sent to site.
     */
    static final int STATUS_SENT = 2;

    /**
     * Station Bluetooth adapter MAC as long integer.
     */
    final long mStationMAC;

    /**
     * Station time at the moment when the record was received by app.
     */
    final long mStationTime;

    /**
     * Time difference between station and Android.
     */
    final int mStationDrift;

    /**
     * Current number of the paired station.
     */
    final int mStationNumber;

    /**
     * Station mode (initialization or control point).
     */
    final int mStationMode;

    /**
     * Initialization time of the chip.
     */
    final long mInitTime;

    /**
     * Team number from the chip.
     */
    final int mTeamNumber;

    /**
     * Team members mask from the chip.
     */
    final int mTeamMask;

    /**
     * Control point at which the chip was initialized or punched.
     */
    final int mPointNumber;

    /**
     * Time of initialization/punch.
     */
    final long mPointTime;

    /**
     * Status of record processing (new, saved, sent).
     */
    private int mStatus;

    /**
     * Constructor for Record class.
     *
     * @param stationMAC    Station Bluetooth adapter MAC as long integer
     * @param stationTime   Station time when the record was received by app
     * @param stationDrift  Time difference between station and Android
     * @param stationNumber Current number of the paired station
     * @param stationMode   Station mode (initialization or control point)
     * @param initTime      Initialization time of the chip
     * @param teamNumber    Team number from the chip
     * @param teamMask      Team members mask from the chip
     * @param pointNumber   Control point at which the chip was initialized or punched
     * @param pointTime     Time of initialization/punch
     * @param status        Status of record processing (new, saved, sent)
     */
    Record(final long stationMAC, final long stationTime, final int stationDrift,
           final int stationNumber, final int stationMode, final long initTime,
           final int teamNumber, final int teamMask, final int pointNumber,
           final long pointTime, final int status) {
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
     * Get current record status.
     *
     * @return Record status (new, saved, sent)
     */
    int getStatus() {
        return mStatus;
    }

    /**
     * Set new status for the record.
     *
     * @param status Record status (new, saved, sent)
     */
    void setStatus(final int status) {
        mStatus = status;
    }

    /**
     * Get station mode for the record
     * to distinguish between initialization and control point punch.
     *
     * @return See Station.MODE_* constants
     */
    int getMode() {
        return mStationMode;
    }

    @Override
    public int compareTo(final Record compareRecord) {
        // For ascending order
        return (int) (this.mPointTime - compareRecord.mPointTime);
    }

    /**
     * Get string representation of the record for sending to site.
     *
     * @return Record converted to string
     */
    @NonNull
    @Override
    public String toString() {
        return Long.toString(mStationMAC) + '\t' + mStationTime + '\t' + mStationDrift + '\t'
                + mStationNumber + '\t' + mStationMode + '\t' + mInitTime + '\t' + mTeamNumber
                + '\t' + mTeamMask + '\t' + mPointNumber + '\t' + mPointTime;
    }
}
