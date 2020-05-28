package ru.mmb.sportiduinomanager.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Support of distance parameters, points lists and discounts.
 */
public final class Distance {
    /**
     * Raid_id from website database.
     */
    private final int mRaidId;
    /**
     * Raid_name from website database.
     */
    private final String mRaidName;
    /**
     * Unixtime when distance has been downloaded from site.
     */
    private final long mTimeDownloaded;
    /**
     * Unixtime when website database will(was) become readonly.
     */
    private final long mTimeReadonly;
    /**
     * Unixtime of last control point closing time.
     */
    private final long mTimeFinish;
    /**
     * Email of authorized user who performs all interaction with website.
     */
    private final String mUserEmail;
    /**
     * Password of authorized user who performs all interaction with website.
     */
    private final String mUserPassword;
    /**
     * Which website database is used, test or main.
     */
    private final int mTestSite;
    /**
     * Default Sportiduino Bluetooth PIN-code.
     */
    private final String mBluetoothPin;
    /**
     * Sparse array of control points, array index == point number.
     */
    private Point[] mPoints;
    /**
     * List of discounts.
     */
    private Discount[] mDiscounts;
    /**
     * Id of last result downloaded from site.
     */
    private long mLastResultId;

    /**
     * Construct dummy empty distance for Application.mDistance initialization.
     **/
    public Distance() {
        mRaidId = 0;
        mRaidName = "";
        mTimeDownloaded = 0;
        mTimeReadonly = 0;
        mTimeFinish = 0;
        mUserEmail = "";
        mUserPassword = "";
        mTestSite = 1;
        mBluetoothPin = "";
        mLastResultId = 0;
    }

    /**
     * Construct distance from imported data.
     *
     * @param userEmail      Email of the user downloading the raid
     * @param userPassword   Password of the user downloading the raid
     * @param testSite       Download raid test site instead of main site
     * @param raidId         ID of the raid
     * @param raidName       ASCII raid name
     * @param timeDownloaded Time when the distance was download from site
     * @param timeReadonly   Time when the raid becomes readonly
     * @param timeFinish     Time when the last control point is closed
     * @param bluetoothPin   Sportiduino Bluetooth PIN-code
     * @param lastResultId   Id of last result downloaded from site
     */
    Distance(final String userEmail, final String userPassword, final int testSite,
             final int raidId, final String raidName, final long timeDownloaded,
             final long timeReadonly, final long timeFinish, final String bluetoothPin, final long lastResultId) {
        mUserEmail = userEmail;
        mUserPassword = userPassword;
        mTestSite = testSite;
        mRaidId = raidId;
        mRaidName = raidName;
        mTimeDownloaded = timeDownloaded;
        mTimeReadonly = timeReadonly;
        mTimeFinish = timeFinish;
        mBluetoothPin = bluetoothPin;
        mLastResultId = lastResultId;
    }

    /**
     * Get email of the authorized user who downloaded the distance from website.
     *
     * @return User email
     */
    public String getUserEmail() {
        return mUserEmail;
    }

    /**
     * Get password of the authorized user who downloaded the distance from website.
     *
     * @return User password
     */
    public String getUserPassword() {
        return mUserPassword;
    }

    /**
     * Get info about which website database we are using, main of test.
     *
     * @return Test site flag
     */
    public int getTestSite() {
        return mTestSite;
    }

    /**
     * Get default Sportiduino Bluetooth PIN-code.
     *
     * @return PIN-code as string
     */
    public String getBluetoothPin() {
        return mBluetoothPin;
    }

    /**
     * Get the time when the distance was downloaded from site.
     *
     * @return Unixtime
     */
    public long getTimeDownloaded() {
        return mTimeDownloaded;
    }

    /**
     * Get raid id.
     *
     * @return Raid id
     */
    int getRaidId() {
        return mRaidId;
    }

    /**
     * Get name of current raid.
     *
     * @return Raid name
     */
    public String getRaidName() {
        return mRaidName;
    }

    /**
     * Get time when the distance will become readonly at the site.
     *
     * @return Unixtime
     */
    long getTimeReadonly() {
        return mTimeReadonly;
    }

    /**
     * Get time when the last raid point will be closed.
     *
     * @return Unixtime
     */
    long getTimeFinish() {
        return mTimeFinish;
    }

    /**
     * Get id of last result downloaded from server.
     *
     * @return id of last result from last results downloading
     */
    long getLastResultId() {
        return mLastResultId;
    }

    /**
     * Update id of last result downloaded from site.
     *
     * @param lastResultId New id of last result downloaded
     */
    void setLastResultId(final long lastResultId) {
        mLastResultId = lastResultId;
    }

    /**
     * Get list of control points names.
     *
     * @param prefix 'AP' numeric point name prefix
     * @return List of names
     */
    public List<String> getPointNames(final String prefix) {
        final List<String> names = new ArrayList<>();
        if (mPoints != null) {
            for (final Point point : mPoints) {
                if (point != null) {
                    final String name = point.mName;
                    if (name.charAt(0) >= '0' && name.charAt(0) <= '9') {
                        names.add(prefix + name);
                    } else {
                        names.add(name);
                    }
                }
            }
        }
        return names;
    }

    /**
     * Get point number from position in the list of all points in a distance.
     *
     * @param position Zero-based position in the list
     * @return Point number at this position
     */
    public int getNumberFromPosition(final int position) {
        if (mPoints == null) return 0;
        int counter = 0;
        for (int i = 0; i < mPoints.length; i++) {
            if (mPoints[i] == null) continue;
            if (position == counter) return i;
            counter++;
        }
        return 0;
    }

    /**
     * Get position in the list of distance points for a point with specific number.
     *
     * @param number Point number
     * @return Zero-based position in the list
     */
    public int getPositionFromNumber(final int number) {
        if (mPoints == null || number < 0 || number >= mPoints.length) return 0;
        int counter = 0;
        for (int i = 0; i < number; i++) {
            if (mPoints[i] != null) {
                counter++;
            }
        }
        return counter;
    }

    /**
     * Get name of a point which number is held at 'index' position in a list.
     *
     * @param list  Whole list of points
     * @param index Position in the list
     * @return String with point name from the distance, '#N' name or '?' name
     */
    private String pointFromList(final List<Integer> list, final int index) {
        if (index < 0 || index >= list.size()) return "?";
        final int number = list.get(index);
        if (mPoints == null || number < 0 || number >= mPoints.length || mPoints[number] == null) return "#" + number;
        return mPoints[number].mName;
    }

    /**
     * Builds the text representation of the from-to range of points.
     *
     * @param list      Whole list of points
     * @param fromIndex Starting index of the range in the list
     * @param toIndex   Ending index of the range in the list
     * @return Range as "1", "1,2" or "1-3"-like string
     */
    private String rangeName(final List<Integer> list, final int fromIndex, final int toIndex) {
        if (toIndex == fromIndex) {
            return pointFromList(list, fromIndex);
        } else {
            if (toIndex == fromIndex + 1) {
                return pointFromList(list, fromIndex) + "," + pointFromList(list, toIndex);
            } else {
                return pointFromList(list, fromIndex) + "-" + pointFromList(list, toIndex);
            }
        }
    }

    /**
     * Converts list of control points numbers to human-readable short list.
     *
     * @param list List of points numbers
     * @return String containing something like "1,3,4,6-8" or "-" for empty list
     */
    public String pointsNamesFromList(final List<Integer> list) {
        // Shortcuts for specific cases
        if (list.isEmpty()) return "-";
        final int total = list.size();
        if (total == 1) return pointFromList(list, 0);
        // Set continuous[i] flag to false for points which has skipped points before them
        boolean[] continuous = new boolean[list.size()];
        continuous[0] = true;
        for (int i = 1; i < total; i++) {
            boolean noHole = true;
            for (int index = list.get(i - 1) + 1; index < list.get(i); index++) {
                if (mPoints != null && mPoints[index] != null) {
                    noHole = false;
                    break;
                }
            }
            continuous[i] = noHole;
        }
        // Build list as a sequence of continuous ranges consisting of 1,2 or more points
        String result = "";
        int rangeStart = 0;
        for (int i = 1; i < total; i++) {
            if (!continuous[i]) {
                final String range = rangeName(list, rangeStart, i - 1);
                if ("".equals(result)) {
                    result = range;
                } else {
                    result = result.concat(",").concat(range);
                }
                rangeStart = i;
            }
        }
        // Add the last range to the list
        final String range = rangeName(list, rangeStart, total - 1);
        if ("".equals(result)) {
            result = range;
        } else {
            result = result.concat(",").concat(range);
        }
        return result;
    }

    /**
     * Create list of skipped points from a list of punched points.
     *
     * @param punchedPoints List of punched points, can be empty
     * @return List of skipped points, can be empty
     */
    public List<Integer> getSkippedPoints(final List<Integer> punchedPoints) {
        final List<Integer> skippedPoints = new ArrayList<>();
        // Return empty skipped list if the punched list is empty
        if (punchedPoints.isEmpty()) return skippedPoints;
        // Find last point from the distance which was punched
        int maxPoint = 0;
        for (final int number : punchedPoints) {
            if (number >= 0 && mPoints != null && number < mPoints.length && mPoints[number] != null
                    && number > maxPoint) {
                maxPoint = number;
            }
        }
        // Find all distance points between (but not including)
        // the chip init point and last punched point which are not punched
        for (int i = 1; i < maxPoint; i++) {
            if (mPoints != null && mPoints[i] != null && !punchedPoints.contains(i)) {
                skippedPoints.add(i);
            }
        }
        return skippedPoints;
    }

    /**
     * Checks if there are mandatory points in a list of skipped points.
     *
     * @param skippedPoints List of skipped points
     * @return True, if a Start/SK/PF/F/OKP point is present in skipped list
     */
    public boolean mandatoryPointSkipped(final List<Integer> skippedPoints) {
        for (final int number : skippedPoints) {
            // All AP types except ordinary AP are mandatory
            if (mPoints != null && mPoints[number].mType < 5) return true;
        }
        return false;
    }

    /**
     * Fill lists with all points parameters for fast saving in local database.
     *
     * @param numbers    Points numbers
     * @param types      Points types
     * @param penalties  Points penalties
     * @param startTimes Points opening times
     * @param endTimes   Points closing times
     * @param names      Points names
     */
    void fillPointsLists(final List<Integer> numbers, final List<Integer> types,
                         final List<Integer> penalties, final List<Long> startTimes,
                         final List<Long> endTimes, final List<String> names) {
        if (mPoints == null) return;
        for (int i = 1; i < mPoints.length; i++) {
            if (mPoints[i] == null) continue;
            numbers.add(i);
            types.add(mPoints[i].mType);
            penalties.add(mPoints[i].mPenalty);
            startTimes.add(mPoints[i].mStart);
            endTimes.add(mPoints[i].mEnd);
            names.add(mPoints[i].mName);
        }
    }

    /**
     * Fill lists with all discounts parameters for fast saving in local database.
     *
     * @param minutes Discounts in minutes
     * @param fromN   Discount intervals starting points
     * @param toN     Discount intervals ending points
     */
    void fillDiscountsLists(final List<Integer> minutes, final List<Integer> fromN,
                            final List<Integer> toN) {
        if (mDiscounts == null) return;
        for (final Discount discount : mDiscounts) {
            minutes.add(discount.mMinutes);
            fromN.add(discount.mFrom);
            toN.add(discount.mTo);
        }
    }

    /**
     * Get the point type.
     *
     * @param number Point number
     * @return Point type
     */
    public int getPointType(final int number) {
        if (number < 0 || mPoints == null || number >= mPoints.length || mPoints[number] == null) return -1;
        return mPoints[number].mType;
    }

    /**
     * Get name of the point.
     *
     * @param number Point number
     * @param prefix 'СP' numeric point name prefix
     * @return Point name
     */
    public String getPointName(final int number, final String prefix) {
        if (number < 0 || mPoints == null || number >= mPoints.length || mPoints[number] == null) return "#" + number;
        final String name = mPoints[number].mName;
        if (name.charAt(0) >= '0' && name.charAt(0) <= '9') {
            return prefix + name;
        } else {
            return name;
        }
    }

    /**
     * Check if the database can be reloaded from server loosing all current data.
     *
     * @return True if it can be reloaded
     */
    public boolean canBeReloaded() {
        // Allow data loss if it was not initialized correctly
        if (mTimeReadonly == 0 || mTimeFinish == 0) return true;
        // Get current time
        final long now = System.currentTimeMillis() / 1000L;
        // Allow data loss if distance was not set readonly yet
        // (we can and should reload it)
        if (now < mTimeReadonly) return true;
        // Allow data loss if race was finished more then 1 month ago
        // (race was finalized anyway)
        return now > (mTimeFinish + 3600 * 24 * 30);
    }

    /**
     * Allocate point array with maxIndex as max array index.
     *
     * @param maxIndex       Max index in point array
     * @param initChipsPoint Name of pseudo point for chip initialization
     */
    void initPointArray(final int maxIndex, final String initChipsPoint) {
        mPoints = new Point[maxIndex + 1];
        mPoints[0] = new Point(0, 0, 0, 0, initChipsPoint);
    }

    /**
     * Allocate discount array with nDiscount as array size.
     *
     * @param numberOfDiscounts Number of discounts
     */
    void initDiscountArray(final int numberOfDiscounts) {
        mDiscounts = new Discount[numberOfDiscounts];
    }

    /**
     * Construct point and save it to appropriate position in point array.
     *
     * @param index   Position in point array
     * @param type    Point type (start, finish, etc)
     * @param penalty Penalty for missing the point
     * @param start   Unixtime when point starts registering of teams
     * @param end     Unixtime when point ends registering of teams
     * @param name    Point name
     * @return True in case of valid index value
     */
    boolean addPoint(final int index, final int type, final int penalty, final long start, final long end,
                     final String name) {
        // Check if point array was initialized
        if (mPoints == null) return false;
        // Check if point index is valid
        if (index <= 0 || index >= mPoints.length) return false;
        // Check if the point was already set
        if (mPoints[index] == null) {
            // set the point
            mPoints[index] = new Point(type, penalty, start, end, name);
            return true;
        }
        return false;
    }

    /**
     * Add new discount to the list of discounts.
     *
     * @param minutes   The discount
     * @param fromPoint First point of discount interval
     * @param toPoint   Last point of discount interval
     * @return True in case of success
     */
    boolean addDiscount(final int minutes, final int fromPoint, final int toPoint) {
        // Check if discount array was initialized
        if (mDiscounts == null) return false;
        for (int i = 0; i < mDiscounts.length; i++) {
            if (mDiscounts[i] == null) {
                mDiscounts[i] = new Discount(minutes, fromPoint, toPoint);
                return true;
            }
        }
        return false;
    }

    /**
     * Check the distance (loaded from site or from local db) for various errors.
     *
     * @return True if some errors were found
     */
    public boolean hasErrors() {
        // Check distance parameters
        if (mRaidId <= 0) return true;
        if (mTimeReadonly <= 0) return true;
        if (mTimeFinish <= 0) return true;
        if (mTimeFinish <= mTimeReadonly) return true;
        if ("".equals(mRaidName)) return true;

        // Check if some points were loaded
        if (mPoints == null) return true;
        if (mPoints.length <= 1) return true;
        // Check if all points were loaded
        if (mPoints[0] == null) return true;
        if (mPoints[mPoints.length - 1] == null) return true;
        // check point data
        for (int i = 1; i < mPoints.length; i++) {
            if (mPoints[i] != null) {
                if (mPoints[i].mType <= 0 || mPoints[i].mType > 5) return true;
                if (mPoints[i].mPenalty < 0) return true;
                if (mPoints[i].mStart > 0 && mPoints[i].mEnd < mPoints[i].mStart) return true;
                if ("".equals(mPoints[i].mName)) return true;
            }
        }
        // Check if some discounts were loaded
        if (mDiscounts == null) return true;
        if (mDiscounts.length == 0) return false;
        // Check discounts data
        for (final Discount discount : mDiscounts) {
            // Check if all discounts were loaded
            if (discount == null) return true;
            // Check discount value
            if (discount.mMinutes <= 0) return true;
            // Check discount interval
            if (discount.mFrom <= 0 || discount.mFrom >= mPoints.length) return true;
            if (discount.mTo <= 0 || discount.mTo >= mPoints.length) return true;
            if (discount.mFrom >= discount.mTo) return true;
            if (mPoints[discount.mFrom] == null) return true;
            if (mPoints[discount.mTo] == null) return true;
        }
        // No errors were detected
        return false;
    }

    /**
     * A control point parameters.
     */
    private static final class Point {
        /**
         * Point type (start, finish, etc).
         */
        private final int mType;
        /**
         * Penalty in minutes for missing the point.
         */
        private final int mPenalty;
        /**
         * Unixtime when the point starts to work.
         */
        private final long mStart;
        /**
         * Unixtime when the point ends working.
         */
        private final long mEnd;
        /**
         * Point name.
         */
        private final String mName;

        /**
         * Constructor for Point class.
         *
         * @param type    Point type
         * @param penalty Penalty for missing this point
         * @param start   Time at which this point start working
         * @param end     Time at which this point stop working
         * @param name    Point name
         */
        private Point(final int type, final int penalty, final long start, final long end, final String name) {
            mType = type;
            mPenalty = penalty;
            mStart = start;
            mEnd = end;
            mName = name;
        }
    }

    /**
     * Discount for missing some points.
     */
    private static final class Discount {
        /**
         * The discount in minutes.
         */
        private final int mMinutes;
        /**
         * First point of the distance part where discount is applied.
         */
        private final int mFrom;
        /**
         * Last point of the interval.
         */
        private final int mTo;

        /**
         * Constructor for Discount class.
         *
         * @param minutes   Value of discount in minutes
         * @param fromPoint Starting point for discount interval
         * @param toPoint   Ending point for discount interval
         */
        private Discount(final int minutes, final int fromPoint, final int toPoint) {
            mMinutes = minutes;
            mFrom = fromPoint;
            mTo = toPoint;
        }
    }
}
