package ru.mmb.datacollector.activity.transport.http;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import ru.mmb.datacollector.model.registry.Settings;

public abstract class TransportHttpClient {
    private final Handler handler;
    private final SSLContext sslContext;
    private final CookieManager cookieManager;

    private boolean terminated = false;

    protected abstract void doDataTransport() throws Exception;

    public TransportHttpClient(Handler handler) {
        this.handler = handler;
        this.sslContext = prepareSslContext();
        // initialize cookies
        this.cookieManager = new CookieManager();
        CookieHandler.setDefault(this.cookieManager);
    }

    private SSLContext prepareSslContext() {
        SSLContext result = null;
        try {
            // Load CAs from an InputStream
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            String crtFileName = Settings.getInstance().getMMBPathFromDBFile() + "/selfca.crt";
            InputStream caInput = new BufferedInputStream(new FileInputStream(crtFileName));
            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
            } finally {
                caInput.close();
            }
            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);
            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);
            // Create an SSLContext that uses our TrustManager
            result = SSLContext.getInstance("TLS");
            result.init(null, tmf.getTrustManagers(), null);
        } catch (Exception e) {
            Log.e("HTTP_CLIENT", "error", e);
        }
        return result;
    }

    public synchronized boolean isTerminated() {
        return terminated;
    }

    public synchronized void terminate() {
        this.terminated = true;
    }

    protected synchronized void writeToConsole(String message) {
        if (!isTerminated()) {
            handler.sendMessage(Message.obtain(handler, ThreadMessageTypes.MSG_CONSOLE, message));
        }
    }

    protected void sendFinishedSuccessNotification() {
        if (!isTerminated()) {
            handler.sendMessage(Message.obtain(handler, ThreadMessageTypes.MSG_FINISHED_SUCCESS));
        }
    }

    protected void sendFinishedErrorNotification() {
        if (!isTerminated()) {
            handler.sendMessage(Message.obtain(handler, ThreadMessageTypes.MSG_FINISHED_ERROR));
        }
    }

    protected boolean isSslContextInitialized() {
        return sslContext != null;
    }

    protected HttpsURLConnection prepareConnectionForGet(URL url) throws Exception {
        HttpsURLConnection result = (HttpsURLConnection) url.openConnection();
        result.setSSLSocketFactory(sslContext.getSocketFactory());
        result.setReadTimeout(15000);
        result.setConnectTimeout(30000);
        result.setRequestMethod("GET");
        return result;
    }

    protected HttpsURLConnection prepareConnectionForFormPost(URL url, int contentLength) throws Exception {
        HttpsURLConnection result = (HttpsURLConnection) url.openConnection();
        result.setSSLSocketFactory(sslContext.getSocketFactory());
        result.setReadTimeout(15000);
        result.setConnectTimeout(30000);
        result.setRequestMethod("POST");
        result.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        result.setRequestProperty("Content-Length", Integer.toString(contentLength));
        result.setUseCaches(false);
        result.setDoInput(true);
        result.setDoOutput(true);
        return result;
    }

    protected String drainCharsFromInputStream(URLConnection connection) throws IOException {
        InputStreamReader reader = new InputStreamReader(connection.getInputStream());
        StringBuilder sb = new StringBuilder();
        int data = reader.read();
        while (data != -1) {
            sb.append((char) data);
            data = reader.read();
        }
        return sb.toString();
    }

    public void transportData() {
        if (!isSslContextInitialized()) {
            writeToConsole("ERROR not loaded selfca.crt, SSL not initialized");
            sendFinishedErrorNotification();
        }
        try {
            requestInitialPage();
            int responseCode = jSecurityCheck();
            if (responseCode != HttpURLConnection.HTTP_MOVED_TEMP) {
                writeToConsole(
                        "ERROR j_security_check failed, unexpected response code: " + responseCode);
            }
            followRedirectToMainForm();
            boolean loggedIn = checkLoggedIn();
            if (!loggedIn) {
                writeToConsole("ERROR login check failed");
                sendFinishedErrorNotification();
                return;
            }
            // !!! call main transport method
            try {
                doDataTransport();
            } catch (Exception e) {
                Log.e("HTTP_CLIENT", "error", e);
                writeToConsole("ERROR data transport: " + e.getMessage());
            }
            // try perform logout necessarily
            logout();
            requestInitialPage();
            checkLoggedIn();
            writeToConsole("SUCCESS data transport");
            sendFinishedSuccessNotification();
        } catch (Exception e) {
            Log.e("HTTP_CLIENT", "error", e);
            writeToConsole("ERROR on server communication: " + e.getMessage());
            sendFinishedErrorNotification();
        }
    }

    protected void requestInitialPage() throws Exception {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(Settings.getInstance().getDataServerUrl() +
                              "/datacollector_server/secure/main-form.html");
            connection = prepareConnectionForGet(url);
            connection.connect();

            writeToConsole("requestInitialPage: " + connection.getResponseMessage());

            drainCharsFromInputStream(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    protected int jSecurityCheck() throws Exception {
        String urlParameters = "j_username=" +
                               URLEncoder.encode(Settings.getInstance().getDataServerUserName(), "UTF-8") +
                               "&j_password=" +
                               URLEncoder.encode(Settings.getInstance().getDataServerPassword(), "UTF-8");

        HttpsURLConnection connection = null;
        try {
            URL url = new URL(Settings.getInstance().getDataServerUrl() +
                              "/datacollector_server/secure/j_security_check");
            connection = prepareConnectionForFormPost(url, urlParameters.getBytes().length);

            connection.connect();

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            int statusCode = connection.getResponseCode();
            writeToConsole("j_security_check: " + connection.getResponseMessage());

            drainCharsFromInputStream(connection);

            return statusCode;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    protected String followRedirectToMainForm() throws Exception {
        HttpsURLConnection connection = null;
        String result = null;
        try {
            URL url = new URL(Settings.getInstance().getDataServerUrl() +
                              "/datacollector_server/secure/main-form.html");
            connection = prepareConnectionForGet(url);
            connection.connect();

            writeToConsole("followRedirectToMainForm: " + connection.getResponseMessage());

            drainCharsFromInputStream(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return result;
    }

    protected boolean checkLoggedIn() throws Exception {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(Settings.getInstance().getDataServerUrl() +
                              "/datacollector_server/unchecked/checkLoggedIn");
            connection = prepareConnectionForGet(url);
            connection.connect();
            int statusCode = connection.getResponseCode();

            String responseString = drainCharsFromInputStream(connection);

            if (statusCode == HttpURLConnection.HTTP_OK) {
                JSONObject checkResult = new JSONObject(responseString);
                boolean result = checkResult.getBoolean("userLoggedIn");
                String message = result ? "OK" : "FAIL";
                writeToConsole("checkLoggedIn result: " + message);
                return result;
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return false;
    }

    protected void logout() throws Exception {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(Settings.getInstance().getDataServerUrl() +
                              "/datacollector_server/secure/logout");
            connection = prepareConnectionForGet(url);
            connection.connect();

            writeToConsole("logout: " + connection.getResponseMessage());

            drainCharsFromInputStream(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
