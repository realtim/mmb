package ru.mmb.sportiduinomanager.model;

import android.app.DownloadManager;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Interaction with http://mmb.progressor.ru - send request and receive response.
 */
public final class SiteRequest {
    /**
     * Downloaded file parsing was successful.
     */
    public static final int LOAD_OK = 0;
    /**
     * Some unexpected open/read error has been occurred.
     */
    public static final int LOAD_READ_ERROR = 1;
    /**
     * Downloaded file has an error in its format.
     */
    public static final int LOAD_PARSE_ERROR = 2;
    /**
     * Member mCustomError contains custom text error message.
     */
    public static final int LOAD_CUSTOM_ERROR = 3;

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
     * Download manager title.
     */
    private final String mTitle;
    /**
     * Application context to select download location.
     */
    private final Context mContext;
    /**
     * Defines callback where downloaded file will be processed.
     */
    private final DownloadManager mDownloadManager;

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
        this.mTitle = srb.mTitle;
        this.mContext = srb.mContext;
        this.mDownloadManager = srb.mDownloadManager;
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
     * Calculate MD5 from user password string.
     *
     * @param str String with a password
     * @return MD5 of the string
     */
    public static String md5(final String str) {
        try {
            // Create MD5 Hash
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(str.getBytes(Charset.forName("UTF-8")));
            final byte[] messageDigest = digest.digest();

            // Create Hex String
            final StringBuilder hexString = new StringBuilder();
            for (final byte aMessageDigest : messageDigest) {
                final String hexNumber = Integer.toHexString(0xFF & aMessageDigest);
                if (hexNumber.length() < 2) {
                    hexString.append('0');
                }
                hexString.append(hexNumber);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            return "";
        }
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
     * Ask site for distance and teams data download.
     *
     * @return ID of download request registered in download manager
     */
    public long askDistance() {
        return askSomething("1", "distance.temp");
    }

    /**
     * Send some request to site with previously prepared body.
     *
     * @param action   Type of request (distance dl, chips ul, results ul)
     * @param filename Filename where server response will be stored
     * @return ID of download request registered in download manager
     */
    private long askSomething(final String action, final String filename) {
        // Select correct url
        String url;
        if (mTestSite == 1) {
            url = TEST_DATABASE_URL;
        } else {
            url = MAIN_DATABASE_URL;
        }
        // Construct request for download manager
        final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.addRequestHeader("X-Sportiduino-Protocol", HTTP_API_VERSION);
        request.addRequestHeader("X-Sportiduino-Auth", mUserEmail + "|" + mUserPassword);
        request.addRequestHeader("X-Sportiduino-Action", action);
        request.setTitle(mTitle);
        request.setDestinationInExternalFilesDir(mContext, null, filename);
        // Send request to download manager
        return mDownloadManager.enqueue(request);
    }

    /**
     * Parse the file downloaded from the site and load distance and teams from it.
     *
     * @param path         Path to downloaded file
     * @param chipInitName Name of chip initialization point in current language
     * @param database     Database object for saving distance+teams in local database
     * @return One of LOAD result constants, mCustomError can be also set
     * @throws IOException                    Unexpected end of file
     * @throws NumberFormatException          A field does not contain integer/long
     * @throws ArrayIndexOutOfBoundsException Number of fields in a line is too small
     */
    public int loadDistance(final String path, final String chipInitName,
                            final Database database)
            throws IOException, NumberFormatException, ArrayIndexOutOfBoundsException {
        final BufferedReader reader = new BufferedReader(new FileReader(path));
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
                    distance.initPointArray(maxOrder, chipInitName);
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
            database.saveDistance(distance);
            database.saveTeams(teams);
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
         * Download manager title.
         */
        private String mTitle;
        /**
         * Application context to select download location.
         */
        private Context mContext;
        /**
         * Defines callback where downloaded file will be processed.
         */
        private DownloadManager mDownloadManager;

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
         * Set title.
         *
         * @param title Download manager title
         * @return this
         */
        public SiteRequestBuilder title(final String title) {
            this.mTitle = title;
            return this;
        }

        /**
         * Set context.
         *
         * @param context Application context to select download location.
         * @return this
         */
        public SiteRequestBuilder context(final Context context) {
            this.mContext = context;
            return this;
        }

        /**
         * Set download manager callback.
         *
         * @param downloadManager Defines callback where downloaded file will be processed
         * @return this
         */
        public SiteRequestBuilder downloadManager(final DownloadManager downloadManager) {
            this.mDownloadManager = downloadManager;
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
