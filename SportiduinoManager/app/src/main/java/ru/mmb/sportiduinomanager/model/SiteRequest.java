package ru.mmb.sportiduinomanager.model;

import android.database.sqlite.SQLiteException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Interaction with http://mmb.progressor.ru - send request and receive response.
 */
public final class SiteRequest {
    /**
     * Downloaded data parsing was successful.
     */
    public static final int LOAD_OK = 0;
    /**
     * Some unexpected open/read error has been occurred.
     */
    public static final int LOAD_READ_ERROR = 1;
    /**
     * Downloaded data has an error in its format.
     */
    public static final int LOAD_PARSE_ERROR = 2;
    /**
     * Local data was changed during upload process.
     */
    public static final int LOAD_DATA_CHANGED = 3;
    /**
     * Something unexpected has been happened.
     */
    public static final int LOAD_FATAL_ERROR = 4;
    /**
     * Member mCustomError contains custom text error message.
     */
    public static final int LOAD_CUSTOM_ERROR = 5;

    /**
     * Request type for distance download.
     */
    public static final int TYPE_DL_DISTANCE = 1;
    /**
     * Request type for chips events upload.
     */
    public static final int TYPE_UL_CHIPS = 2;
    /**
     * Request type for results download.
     */
    public static final int TYPE_DL_RESULTS = 3;

    /**
     * URL of test database interaction script.
     */
    private static final String TEST_DATABASE_URL = "http://mmb.progressor.ru/php/mmbscripts_git/sportiduino.php";
    /**
     * URL of main database interaction script.
     */
    private static final String MAIN_DATABASE_URL = "http://mmb.progressor.ru/php/mmbscripts/sportiduino.php";
    /**
     * Website script API version supported by this application.
     */
    private static final String HTTP_API_VERSION = "1";

    /**
     * User email for authorization.
     */
    private final String mUserEmail;
    /**
     * MD5 of user password for authorization.
     */
    private final String mUserPassword;
    /**
     * Selection of main/test version of the site.
     */
    private final int mTestSite;
    /**
     * Type of site request (see TYPE_* constants).
     */
    private final int mType;
    /**
     * Localized name for chip init active point.
     */
    private final String mChipInitName;
    /**
     * Database object for saving downloaded data.
     */
    private final Database mDatabase;
    /**
     * List of chips events for sending unsent events to site.
     */
    private final Chips mChips;

    /**
     * Custom error from first line of downloaded file or from SQLite exception.
     */
    private String mCustomError;

    /**
     * A distance successfully loaded from downloaded file.
     */
    private Distance mDistance;
    /**
     * Teams and team members successfully loaded from downloaded file.
     */
    private Teams mTeams;

    /**
     * private constructor, only used by the SiteRequestBuilder.
     *
     * @param srb Builder of all class variables
     */
    private SiteRequest(final SiteRequestBuilder srb) {
        this.mUserEmail = srb.mUserEmail;
        this.mUserPassword = srb.mUserPassword;
        this.mTestSite = srb.mTestSite;
        this.mType = srb.mType;
        this.mChipInitName = srb.mChipInitName;
        this.mDatabase = srb.mDatabase;
        this.mChips = srb.mChips;
    }

    /**
     * Returns an instance of SiteRequestBuilder.
     *
     * @return instance of SiteRequestBuilder
     */
    public static SiteRequestBuilder builder() {
        return new SiteRequestBuilder();
    }

    /**
     * Get a custom error message.
     *
     * @return String with custom error
     */
    public String getCustomError() {
        return mCustomError;
    }

    /**
     * Get the type of site request.
     *
     * @return See TYPE_* constants
     */
    public int getRequestType() {
        return mType;
    }

    /**
     * Get loaded distance.
     *
     * @return Distance object
     */
    public Distance getDistance() {
        return mDistance;
    }

    /**
     * Get loaded teams.
     *
     * @return Teams object
     */
    public Teams getTeams() {
        return mTeams;
    }

    /**
     * Get updated chip events with status changed from SAVED to SENT.
     *
     * @return Chips object
     */
    public Chips getChips() {
        return mChips;
    }

    /**
     * Make one of download/upload request according to mType.
     *
     * @return One of LOAD result constants, mCustomError can be also set
     * @throws IOException                    Input stream error
     * @throws NumberFormatException          A field does not contain integer/long
     * @throws ArrayIndexOutOfBoundsException Number of fields in a line is too small
     */
    public int makeRequest() throws IOException, NumberFormatException, ArrayIndexOutOfBoundsException {
        switch (mType) {
            case TYPE_DL_DISTANCE:
                return loadDistance();
            case TYPE_UL_CHIPS:
                return sendChipEvents();
            case TYPE_DL_RESULTS:
                return loadResults();
            default:
                return LOAD_FATAL_ERROR;
        }
    }

    /**
     * Create HttpURLConnection object from url of main/test site script.
     *
     * @return HttpURLConnection object or null in case of malformed url
     */
    private HttpURLConnection prepareConnection() {
        // Select correct url
        String urlString;
        if (mTestSite == 1) {
            urlString = TEST_DATABASE_URL;
        } else {
            urlString = MAIN_DATABASE_URL;
        }
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            return null;
        }
        try {
            return (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Set connection properties and try to open it.
     *
     * @param connection previously prepared in prepareConnection
     * @param postData   string with POST parameters or null in case of GET request
     * @return One of LOAD result constants
     */
    private int makeConnection(final HttpURLConnection connection, final String postData) {
        connection.setRequestProperty("X-Sportiduino-Protocol", HTTP_API_VERSION);
        connection.setRequestProperty("X-Sportiduino-Auth", mUserEmail + "|" + mUserPassword);
        connection.setRequestProperty("X-Sportiduino-Action", String.valueOf(mType));
        if (postData != null) {
            connection.setDoOutput(true);
            final byte[] postDataBytes = postData.getBytes(StandardCharsets.UTF_8);
            final int length = postDataBytes.length;
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", String.valueOf(length));
            connection.setFixedLengthStreamingMode(length);
            try {
                connection.getOutputStream().write(postDataBytes);
            } catch (IOException e) {
                return LOAD_READ_ERROR;
            }
        }
        try {
            connection.connect();
        } catch (IOException e) {
            return LOAD_READ_ERROR;
        }
        return LOAD_OK;
    }

    /**
     * Ask site for distance and teams data download.
     *
     * @return One of LOAD result constants, mCustomError can be also set
     * @throws IOException                    Unexpected end of file
     * @throws NumberFormatException          A field does not contain integer/long
     * @throws ArrayIndexOutOfBoundsException Number of fields in a line is too small
     */
    private int loadDistance()
            throws IOException, NumberFormatException, ArrayIndexOutOfBoundsException {
        // Prepare and open connection to site
        final HttpURLConnection connection = prepareConnection();
        if (connection == null) return LOAD_READ_ERROR;
        final int result = makeConnection(connection, null);
        if (result != LOAD_OK) return result;
        // Start reading server response
        final BufferedReader reader =
                new BufferedReader(new InputStreamReader(connection.getInputStream(),
                        StandardCharsets.UTF_8));
        // check for error message from server
        String line = reader.readLine();
        if (!"".equals(line)) {
            mCustomError = line;
            return LOAD_CUSTOM_ERROR;
        }
        // read the response
        Distance distance = null;
        Teams teams = null;
        String[] values;
        char blockType;
        do {
            line = reader.readLine();
            if (line == null) return LOAD_PARSE_ERROR;
            blockType = line.charAt(0);
            values = line.split("\t", -1);
            switch (blockType) {
                case 'R':
                    // get raid information
                    distance = new Distance(mUserEmail, mUserPassword, mTestSite,
                            Integer.parseInt(values[1]), values[4],
                            System.currentTimeMillis() / 1000,
                            Long.parseLong(values[2]), Long.parseLong(values[3]), 0);
                    break;
                case 'P':
                    // parse list of points
                    if (distance == null) return LOAD_PARSE_ERROR;
                    final int nPoints = Integer.parseInt(values[1]);
                    distance.initPointArray(Integer.parseInt(values[2]), mChipInitName);
                    for (int i = 0; i < nPoints; i++) {
                        line = reader.readLine();
                        if (line == null) return LOAD_PARSE_ERROR;
                        values = line.split("\t", -1);
                        if (!distance.addPoint(Integer.parseInt(values[1]),
                                Integer.parseInt(values[2]), Integer.parseInt(values[3]),
                                Long.parseLong(values[4]), Long.parseLong(values[5]), values[6])) {
                            return LOAD_PARSE_ERROR;
                        }
                    }
                    break;
                case 'D':
                    // parse list of discounts
                    if (distance == null) return LOAD_PARSE_ERROR;
                    final int nDiscounts = Integer.parseInt(values[1]);
                    distance.initDiscountArray(nDiscounts);
                    for (int i = 0; i < nDiscounts; i++) {
                        line = reader.readLine();
                        if (line == null) return LOAD_PARSE_ERROR;
                        values = line.split("\t", -1);
                        if (!distance.addDiscount(Integer.parseInt(values[1]),
                                Integer.parseInt(values[2]), Integer.parseInt(values[3]))) {
                            return LOAD_PARSE_ERROR;
                        }
                    }
                    break;
                case 'T':
                    // parse list of teams
                    final int nTeams = Integer.parseInt(values[1]);
                    teams = new Teams(Integer.parseInt(values[2]));
                    for (int i = 0; i < nTeams; i++) {
                        line = reader.readLine();
                        if (line == null) return LOAD_PARSE_ERROR;
                        values = line.split("\t", -1);
                        if (!teams.addTeam(Integer.parseInt(values[1]),
                                Integer.parseInt(values[2]), Integer.parseInt(values[3]),
                                values[4])) {
                            return LOAD_PARSE_ERROR;
                        }
                    }
                    break;
                case 'M':
                    // parse list of team members
                    if (teams == null) return LOAD_PARSE_ERROR;
                    final int nMembers = Integer.parseInt(values[1]);
                    for (int i = 0; i < nMembers; i++) {
                        line = reader.readLine();
                        if (line == null) return LOAD_PARSE_ERROR;
                        values = line.split("\t", -1);
                        if (!teams.addTeamMember(Long.parseLong(values[1]),
                                Integer.parseInt(values[2]), values[3], values[4])) {
                            return LOAD_PARSE_ERROR;
                        }
                    }
                    break;
                case 'E':
                    // End of distance data in server response
                    break;
                default:
                    return LOAD_PARSE_ERROR;
            }
        } while (blockType != 'E');
        reader.close();
        connection.disconnect();
        // check if all necessary data were present
        if (distance == null || teams == null) return LOAD_PARSE_ERROR;
        // Validate loaded distance and teams
        if (distance.hasErrors() || teams.hasErrors()) {
            // Downloaded distance had errors, use old distance from persistent memory
            return LOAD_PARSE_ERROR;
        }
        // Copy parsed distance and teams to class members
        mDistance = distance;
        mTeams = teams;
        // Save parsed distance and teams to local database
        try {
            mDatabase.saveDistance(distance);
            mDatabase.saveTeams(teams);
        } catch (SQLiteException e) {
            mCustomError = e.getMessage();
            return LOAD_CUSTOM_ERROR;
        }
        return LOAD_OK;
    }

    /**
     * Send all unsent chip events from local database to site database.
     *
     * @return One of LOAD result constants, mCustomError can be also set
     * @throws IOException Unexpected end of file
     */
    private int sendChipEvents() throws IOException {
        // Prepare connection to site
        final HttpURLConnection connection = prepareConnection();
        if (connection == null) return LOAD_READ_ERROR;
        // Prepare data to send
        final List<String> events = mChips.getUnsentEvents();
        final StringBuilder builder = new StringBuilder();
        for (final String event : events) {
            builder.append('\n').append(event);
        }
        final String data =
                "data=" + mChips.getTimeDownloaded() + '\t' + events.size() + builder.toString();
        // Send data to site
        final int result = makeConnection(connection, data);
        if (result != LOAD_OK) return result;
        // Read script response
        final BufferedReader reader =
                new BufferedReader(new InputStreamReader(connection.getInputStream(),
                        StandardCharsets.UTF_8));
        // Non-empty first line contains server error
        final String error = reader.readLine();
        if (!"".equals(error)) {
            mCustomError = error;
            return LOAD_CUSTOM_ERROR;
        }
        // Check that all chip events were received by server
        final String header = reader.readLine();
        if (header == null) return LOAD_READ_ERROR;
        try {
            if (Integer.parseInt(header) != events.size()) return LOAD_PARSE_ERROR;
        } catch (NumberFormatException e) {
            // This line can contain php error, show it to user
            mCustomError = header;
            return LOAD_CUSTOM_ERROR;
        }
        // Finish parsing of server response
        connection.disconnect();
        // Update chip events status in local database
        if (mDatabase.markChipsSent(events.size())) {
            // Update chip events status in memory
            if (mChips.markChipsSent(events.size())) {
                return LOAD_OK;
            } else {
                return LOAD_DATA_CHANGED;
            }
        } else {
            return LOAD_DATA_CHANGED;
        }
    }

    /**
     * Download new results from site database to local database.
     *
     * @return One of LOAD result constants, mCustomError can be also set
     * @throws IOException Unexpected end of file
     */
    private int loadResults() throws IOException {
        // Prepare and open connection to site
        final HttpURLConnection connection = prepareConnection();
        if (connection == null) return LOAD_READ_ERROR;
        final int result = makeConnection(connection, null);
        if (result != LOAD_OK) return result;
        // Start reading server response
        final BufferedReader reader =
                new BufferedReader(new InputStreamReader(connection.getInputStream(),
                        StandardCharsets.UTF_8));
        // check for error message from server
        final String error = reader.readLine();
        if (!"".equals(error)) {
            mCustomError = error;
            return LOAD_CUSTOM_ERROR;
        }
        // Get id of last result in site database
        final String header = reader.readLine();
        if (header == null) return LOAD_READ_ERROR;
        long lastResultId;
        try {
            lastResultId = Integer.parseInt(header);
        } catch (NumberFormatException e) {
            // This line can contain php error, show it to user
            mCustomError = header;
            return LOAD_CUSTOM_ERROR;
        }
        // TODO: mDistance is null, save it in future mResults object
        mDistance.setLastResultId(lastResultId);
        // TODO: parse teams results
        return LOAD_OK;
    }

    /**
     * Expose an API to construct Site Requests.
     */
    public static final class SiteRequestBuilder {
        /**
         * User email for authorization.
         */
        private String mUserEmail;
        /**
         * MD5 of user password for authorization.
         */
        private String mUserPassword;
        /**
         * Selection of main/test version of the site.
         */
        private int mTestSite;
        /**
         * Type of site request (see TYPE_* constants).
         */
        private int mType;
        /**
         * Localized name for chip init active point.
         */
        private String mChipInitName;
        /**
         * Database object for saving downloaded data.
         */
        private Database mDatabase;
        /**
         * List of chips events for sending unsent events to site.
         */
        private Chips mChips;

        /**
         * Use the static method SiteRequest.builder() to get an instance.
         */
        private SiteRequestBuilder() {
        }

        /**
         * Set user email.
         *
         * @param userEmail User email for authorization
         * @return this
         */
        public SiteRequestBuilder userEmail(final String userEmail) {
            this.mUserEmail = userEmail;
            return this;
        }

        /**
         * Set user password.
         *
         * @param userPassword MD5 of user password for authorization
         * @return this
         */
        public SiteRequestBuilder userPassword(final String userPassword) {
            this.mUserPassword = userPassword;
            return this;
        }

        /**
         * Set test site flag.
         *
         * @param testSite Selection of main/test version of the site
         * @return this
         */
        public SiteRequestBuilder testSite(final int testSite) {
            this.mTestSite = testSite;
            return this;
        }

        /**
         * Set type of site request.
         *
         * @param type See TYPE_* constants
         * @return this
         */
        public SiteRequestBuilder type(final int type) {
            this.mType = type;
            return this;
        }

        /**
         * Set localized name for chip init active point.
         *
         * @param chipInitName String with chip init name
         * @return this
         */
        public SiteRequestBuilder chipInitName(final String chipInitName) {
            this.mChipInitName = chipInitName;
            return this;
        }

        /**
         * Set database object for saving downloaded data.
         *
         * @param database Database object
         * @return this
         */
        public SiteRequestBuilder database(final Database database) {
            this.mDatabase = database;
            return this;
        }

        /**
         * Set a list of chips events for sending unsent events to site.
         *
         * @param chips Chip events
         * @return this
         */
        public SiteRequestBuilder chips(final Chips chips) {
            this.mChips = chips;
            return this;
        }

        /**
         * Finalize builder.
         *
         * @return SiteRequest object
         */
        public SiteRequest build() {
            return new SiteRequest(this);
        }
    }

}
