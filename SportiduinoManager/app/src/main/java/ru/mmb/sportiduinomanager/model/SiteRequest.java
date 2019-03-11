package ru.mmb.sportiduinomanager.model;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;

/**
 * Interaction with http://mmb.progressor.ru - send request and receive response.
 */
public final class SiteRequest {
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
