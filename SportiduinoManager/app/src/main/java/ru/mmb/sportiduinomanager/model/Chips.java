package ru.mmb.sportiduinomanager.model;

import android.database.sqlite.SQLiteException;

import java.util.ArrayList;
import java.util.Collections;
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
    public void addNewEvent(final Station station, final long initTime, final int teamNumber,
                            final int teamMask, final int pointNumber, final long pointTime) {
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
     * Mark all unsent chip events as sent.
     *
     * @param expectedUnsentN Expected number of unsent events
     * @return true if actual number is equal to expected
     */
    boolean markChipsSent(final int expectedUnsentN) {
        // Get actual number of unsent chip events
        int actualUnsentN = 0;
        for (final ChipEvent event : mEvents) {
            if (event.getStatus() != ChipEvent.STATUS_SENT) actualUnsentN++;
        }
        // Do nothing if actual number of unsent events differ from expected
        if (expectedUnsentN != actualUnsentN) return false;
        // Data is consistent, mark all unsent events as sent
        for (int i = 0; i < mEvents.size(); i++) {
            final ChipEvent event = mEvents.get(i);
            if (event.getStatus() != ChipEvent.STATUS_SENT) {
                event.setStatus(ChipEvent.STATUS_SENT);
                mEvents.set(i, event);
            }
        }
        return true;
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

    /**
     * Get number of chip events.
     *
     * @return Number of events
     */
    public int size() {
        return mEvents.size();
    }

    /**
     * Clear list of chip events.
     */
    void clear() {
        mEvents.clear();
    }

    /**
     * Checks if the list of events contains an event for specific team.
     *
     * @param team Team number to search
     * @return true if an event for the team has been found
     */
    public boolean contains(final int team) {
        for (final ChipEvent event : mEvents) {
            if (event.mTeamNumber == team) return true;
        }
        return false;
    }

    /**
     * Loose merging of two lists of events with replacing of old visits with new.
     *
     * @param newEvents List of events to add
     * @return True if some events were added or replaced
     */
    public boolean merge(final Chips newEvents) {
        boolean dataChanged = false;
        for (final ChipEvent newEvent : newEvents.mEvents) {
            boolean isSameTeam = false;
            for (int i = 0; i < mEvents.size(); i++) {
                final ChipEvent event = mEvents.get(i);
                if (event.mTeamNumber == newEvent.mTeamNumber
                        && event.mPointNumber == newEvent.mPointNumber) {
                    // This team has visited this point before
                    isSameTeam = true;
                    if (event.mPointTime == newEvent.mPointTime
                            && event.mTeamMask == newEvent.mTeamMask) {
                        // It is the same visit, skip it
                        break;
                    }
                    if (event.mPointTime <= newEvent.mPointTime) {
                        // Team time and/or mask has been changed, replace old event with new
                        mEvents.set(i, newEvent);
                        dataChanged = true;
                        break;
                    }
                }
            }
            // If it was same/new visit of already seen team, do nothing
            if (isSameTeam) continue;
            // It is completely new event, add it to the list
            mEvents.add(newEvent);
            dataChanged = true;
        }
        return dataChanged;
    }

    /**
     * Strict joining of two lists of events,
     * events are not replaced, they are only added or skipped as full duplicates.
     *
     * @param newEvents List of events to add
     * @return True if some events were added
     */
    public boolean join(final Chips newEvents) {
        boolean dataChanged = false;
        for (final ChipEvent newEvent : newEvents.mEvents) {
            boolean isSameEvent = false;
            for (final ChipEvent event : this.mEvents) {
                if (event.mTeamNumber == newEvent.mTeamNumber
                        && event.mPointTime == newEvent.mPointTime
                        && event.mTeamMask == newEvent.mTeamMask
                        && event.mPointNumber == newEvent.mPointNumber
                        && event.mInitTime == newEvent.mInitTime
                        && event.mStationMAC == newEvent.mStationMAC
                        && event.mStationNumber == newEvent.mStationNumber
                        && event.mStationMode == newEvent.mStationMode) {
                    isSameEvent = true;
                    break;
                }
            }
            // Skip identical chip events
            if (isSameEvent) continue;
            mEvents.add(newEvent);
            dataChanged = true;
        }
        return dataChanged;
    }

    /**
     * Get statistic for sent/unsent chip initializations and teams results.
     *
     * @return Array of four integers
     */
    public List<Integer> getStatistic() {
        // Init counters
        final List<Integer> statistic = new ArrayList<>();
        int initUnsent = 0;
        int initSent = 0;
        int resultUnsent = 0;
        int resultSent = 0;
        // Find number of each type of events
        for (final ChipEvent event : mEvents) {
            if (event.getStatus() == ChipEvent.STATUS_SENT) {
                if (event.getMode() == Station.MODE_INIT_CHIPS) {
                    initSent++;
                } else {
                    resultSent++;
                }
            } else {
                if (event.getMode() == Station.MODE_INIT_CHIPS) {
                    initUnsent++;
                } else {
                    resultUnsent++;
                }
            }
        }

        // Return all numbers as array
        statistic.add(initUnsent);
        statistic.add(initSent);
        statistic.add(resultUnsent);
        statistic.add(resultSent);
        return statistic;
    }

    /**
     * Get chip events at current point and station
     * (check ins only, not events copied from chips).
     *
     * @param pointNumber Active point / station number
     * @param stationMAC  Station MAC as long
     * @return New chips object with list of filtered events
     */
    public Chips getChipsAtPoint(final int pointNumber, final long stationMAC) {
        final List<ChipEvent> visits = new ArrayList<>();
        // Filter events
        for (final ChipEvent event : mEvents) {
            if (event.mPointNumber == pointNumber && event.mStationMAC == stationMAC
                    && event.mStationNumber == pointNumber) {
                // Find previous visit of this team (if any)
                int index = -1;
                for (int i = 0; i < visits.size(); i++) {
                    if (visits.get(i).mTeamNumber == event.mTeamNumber) {
                        index = i;
                        break;
                    }
                }
                if (index >= 0) {
                    // Replace previous visit of the same team with new visit
                    // if the time of new visit is greater then old
                    if (event.mPointTime > visits.get(index).mPointTime) {
                        visits.remove(index);
                        visits.add(event);
                    }
                } else {
                    // It is the first visit of this team, add it
                    visits.add(event);
                }
            }
        }
        // Sort the array of events
        Collections.sort(visits);
        // Create new chips object and copy events to it
        final Chips chipsAtPoint = new Chips(0);
        for (final ChipEvent event : visits) {
            chipsAtPoint.addEvent(event);
        }
        return chipsAtPoint;
    }

    /**
     * Sort list of events by their time in ascending order.
     */
    public void sort() {
        Collections.sort(mEvents);
    }

    /**
     * Get team number from 'index' element of mEvents list
     * (mEvents should be previously filtered with getChipsAtPoint).
     *
     * @param index Index in mEvents list array
     * @return Team number for element with this index
     */
    public int getTeamNumber(final int index) {
        if (index < 0 || index >= mEvents.size()) return -1;
        return mEvents.get(index).mTeamNumber;
    }

    /**
     * Get time of arrival from 'index' element of mEvents list
     * (mEvents should be previously filtered with getChipsAtPoint).
     *
     * @param index Index in mEvents list array
     * @return Team visit unixtime for element with this index
     */
    public long getTeamTime(final int index) {
        if (index < 0 || index >= mEvents.size()) return -1;
        return mEvents.get(index).mPointTime;
    }

    /**
     * Get team members mask from 'index' element of mEvents list
     * (mEvents should be previously filtered with getChipsAtPoint).
     *
     * @param index Index in mEvents list array
     * @return Team mask for element with this index
     */
    public int getTeamMask(final int index) {
        if (index < 0 || index >= mEvents.size()) return -1;
        return mEvents.get(index).mTeamMask;
    }

    /**
     * Get chip init time from 'index' element of mEvents list
     * (mEvents should be previously filtered with getChipsAtPoint).
     *
     * @param index Index in mEvents list array
     * @return Chip initialization unixtime for element with this index
     */
    public long getInitTime(final int index) {
        if (index < 0 || index >= mEvents.size()) return -1;
        return mEvents.get(index).mInitTime;
    }

    /**
     * Get all visited active points for the chip checked at connected station.
     *
     * @param teamNumber     Team number in the chip
     * @param initTime       Initialization time of the chip
     * @param stationNumber  Connected station number
     * @param stationMAC     Connected station BT MAC address
     * @param maxPointNumber Max possible point number in current distance
     * @return List of points numbers
     */
    public List<Integer> getChipMarks(final int teamNumber, final long initTime,
                                      final int stationNumber, final long stationMAC,
                                      final int maxPointNumber) {
        // mark all visited points
        final boolean[] visited = new boolean[maxPointNumber + 1];
        for (final ChipEvent event : mEvents) {
            if (event.mTeamNumber == teamNumber && event.mInitTime == initTime
                    && event.mStationNumber == stationNumber && event.mStationMAC == stationMAC
                    && event.mPointNumber <= maxPointNumber) {
                visited[event.mPointNumber] = true;
            }
        }
        // Build sorted list of visited points
        final List<Integer> pointNumbers = new ArrayList<>();
        for (int i = 1; i <= maxPointNumber; i++) {
            if (visited[i]) {
                pointNumbers.add(i);
            }
        }
        return pointNumbers;
    }
}
