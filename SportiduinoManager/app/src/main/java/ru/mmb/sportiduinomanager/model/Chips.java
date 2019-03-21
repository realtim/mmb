package ru.mmb.sportiduinomanager.model;

import android.database.sqlite.SQLiteException;

import java.util.ArrayList;
import java.util.List;

/**
 * Handling chip data (initialization event and check ins at active points).
 */
public final class Chips {
    /**
     * Used as return string when no SQL error has been occurred.
     */
    private static final String SUCCESS = "";

    /**
     * List of events (initializations and check ins).
     */
    private final List<ChipEvent> mEvents;

    /**
     * Unixtime when distance has been downloaded from site.
     */
    private final long mTimeDownloaded;

    /**
     * Construct empty list of chip events.
     *
     * @param timeDownloaded Time of distance dl (to send along with chips to site)
     */
    Chips(final long timeDownloaded) {
        mEvents = new ArrayList<>();
        mTimeDownloaded = timeDownloaded;
    }

    /**
     * Add a chip event (loaded from local database) to the list.
     *
     * @param event Chip event to add
     */
    void addEvent(final ChipEvent event) {
        mEvents.add(event);
    }

    /**
     * Create new event from paired station and add it to list of chip events.
     *
     * @param station     Station where the chip was initialized or checked in
     * @param initTime    Chip initialization time
     * @param teamNumber  Team number written in the chip
     * @param teamMask    Team members mask written in the chip
     * @param pointNumber Point visited by chip (can differ from station number)
     * @param pointTime   Point visit time
     */
    public void addNewEvent(final Station station, final int initTime, final int teamNumber,
                            final int teamMask, final int pointNumber, final int pointTime) {
        mEvents.add(new ChipEvent(station.getMACasLong(), station.getStationTime(),
                station.getTimeDrift(), station.getNumber(), station.getMode(), initTime,
                teamNumber, teamMask, pointNumber, pointTime, ChipEvent.STATUS_NEW));
    }

    /**
     * Get previously loaded time of distance download.
     *
     * @return Time as unixtime
     */
    long getTimeDownloaded() {
        return mTimeDownloaded;
    }

    /**
     * Check if some chip events were not sent to site yet.
     *
     * @return True if one or more events was not sent yet
     */
    public boolean hasUnsentEvents() {
        for (final ChipEvent event : mEvents) {
            if (event.getStatus() != ChipEvent.STATUS_SENT) return true;
        }
        return false;
    }

    /**
     * Save all new (unsaved) chip events to local database.
     *
     * @param database Database object from application thread
     * @return Empty string in case of success, SQL exception message in case of error
     */
    public String saveNewEvents(final Database database) {
        final List<ChipEvent> unsavedEvents = new ArrayList<>();
        // Find all unsaved events and put them in the list
        for (final ChipEvent event : mEvents) {
            if (event.getStatus() == ChipEvent.STATUS_NEW) {
                unsavedEvents.add(event);
            }
        }
        if (unsavedEvents.isEmpty()) return SUCCESS;
        // Try to save this list in the database
        try {
            database.saveChips(unsavedEvents);
        } catch (SQLiteException e) {
            return e.getMessage();
        }
        // Mark all new events as saved
        for (int i = 0; i < mEvents.size(); i++) {
            final ChipEvent event = mEvents.get(i);
            if (event.getStatus() == ChipEvent.STATUS_NEW) {
                event.setStatus(ChipEvent.STATUS_SAVED);
                mEvents.set(i, event);
            }
        }
        return SUCCESS;
    }

    /**
     * Get list of all unsent chip events converted to strings.
     *
     * @return Array of strings with events
     */
    List<String> getUnsentEvents() {
        final List<String> eventsAsString = new ArrayList<>();
        for (final ChipEvent event : mEvents) {
            if (event.getStatus() != ChipEvent.STATUS_SENT) {
                eventsAsString.add(event.toString());
            }
        }
        return eventsAsString;
    }
}
