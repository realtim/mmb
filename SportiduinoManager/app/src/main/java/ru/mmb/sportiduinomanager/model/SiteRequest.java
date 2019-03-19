package ru.mmb.sportiduinomanager.model;

import android.database.sqlite.SQLiteException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

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
     * Something unexpected has been happened.
     */
    public static final int LOAD_FATAL_ERROR = 3;
    /**
     * Member mCustomError contains custom text error message.
     */
    public static final int LOAD_CUSTOM_ERROR = 4;

    /**
     * Request type for distance download.
     */
    public static final int TYPE_DL_DISTANCE = 1;

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
            default:
                return LOAD_FATAL_ERROR;
        }

    }

    /**
     * Send some request to site with previously prepared body.
     *
     * @return Successfully opened connection to site or null in case of en error
     */
    private HttpURLConnection askSomething() {
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
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            return null;
        }
        connection.setRequestProperty("X-Sportiduino-Protocol", HTTP_API_VERSION);
        connection.setRequestProperty("X-Sportiduino-Auth", mUserEmail + "|" + mUserPassword);
        connection.setRequestProperty("X-Sportiduino-Action", String.valueOf(mType));
        try {
            connection.connect();
        } catch (IOException e) {
            return null;
        }
        return connection;
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
        final HttpURLConnection connection = askSomething();
        if (connection == null) return LOAD_READ_ERROR;
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
                    final int raidId = Integer.parseInt(values[1]);
                    final long raidTimeReadonly = Long.parseLong(values[2]);
                    final long raidTimeFinish = Long.parseLong(values[3]);
                    final String raidName = values[4];
                    distance = new Distance(mUserEmail, mUserPassword, mTestSite,
                            raidId, raidName, System.currentTimeMillis() / 1000,
                            raidTimeReadonly, raidTimeFinish);
                    break;
                case 'P':
                    // parse list of points
                    if (distance == null) return LOAD_PARSE_ERROR;
                    final int nPoints = Integer.parseInt(values[1]);
                    final int maxOrder = Integer.parseInt(values[2]);
                    distance.initPointArray(maxOrder, mChipInitName);
                    for (int i = 0; i < nPoints; i++) {
                        line = reader.readLine();
                        if (line == null) return LOAD_PARSE_ERROR;
                        values = line.split("\t", -1);
                        final int index = Integer.parseInt(values[1]);
                        final int type = Integer.parseInt(values[2]);
                        final int penalty = Integer.parseInt(values[3]);
                        final long start = Long.parseLong(values[4]);
                        final long end = Long.parseLong(values[5]);
                        final String name = values[6];
                        if (!distance.addPoint(index, type, penalty, start, end, name)) {
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
                        final int minutes = Integer.parseInt(values[1]);
                        final int fromPoint = Integer.parseInt(values[2]);
                        final int toPoint = Integer.parseInt(values[3]);
                        if (!distance.addDiscount(minutes, fromPoint, toPoint)) {
                            return LOAD_PARSE_ERROR;
                        }
                    }
                    break;
                case 'T':
                    // parse list of teams
                    final int nTeams = Integer.parseInt(values[1]);
                    final int maxNumber = Integer.parseInt(values[2]);
                    teams = new Teams(maxNumber);
                    for (int i = 0; i < nTeams; i++) {
                        line = reader.readLine();
                        if (line == null) return LOAD_PARSE_ERROR;
                        values = line.split("\t", -1);
                        final int number = Integer.parseInt(values[1]);
                        final int nMembers = Integer.parseInt(values[2]);
                        final int nMaps = Integer.parseInt(values[3]);
                        final String name = values[4];
                        if (!teams.addTeam(number, nMembers, nMaps, name)) {
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
                        final long memberId = Long.parseLong(values[1]);
                        final int team = Integer.parseInt(values[2]);
                        final String name = values[3];
                        final String phone = values[4];
                        if (!teams.addTeamMember(team, memberId, name, phone)) {
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
        // TODO: reload distance from database to ensure that it was saved correctly
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
         * Finalize builder.
         *
         * @return SiteRequest object
         */
        public SiteRequest build() {
            return new SiteRequest(this);
        }
    }

}
