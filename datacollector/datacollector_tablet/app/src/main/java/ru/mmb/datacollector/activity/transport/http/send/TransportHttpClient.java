package ru.mmb.datacollector.activity.transport.http.send;

import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.transport.exporter.DataExtractorToJson;
import ru.mmb.datacollector.transport.exporter.ExportMode;
import ru.mmb.datacollector.transport.exporter.ExportState;
import ru.mmb.datacollector.transport.exporter.data.ExportDataMethodJson;

public class TransportHttpClient {
    private final Handler handler;
    private final SSLContext sslContext;
    private final CookieManager cookieManager;

    private boolean terminated = false;


    public TransportHttpClient(Handler handler) {
        this.handler = handler;
        this.sslContext = prepareSslContext();
        // initialize cookies
        this.cookieManager = new CookieManager();
        CookieHandler.setDefault(this.cookieManager);
    }

    public synchronized boolean isTerminated() {
        return terminated;
    }

    public synchronized void terminate() {
        this.terminated = true;
    }

    private synchronized void writeToConsole(String message) {
        if (!isTerminated()) {
            handler.sendMessage(Message.obtain(handler, ThreadMessageTypes.MSG_CONSOLE, message));
        }
    }

    private void sendFinishedSuccessNotification() {
        if (!isTerminated()) {
            handler.sendMessage(Message.obtain(handler, ThreadMessageTypes.MSG_FINISHED_SUCCESS));
        }
    }

    private void sendFinishedErrorNotification() {
        if (!isTerminated()) {
            handler.sendMessage(Message.obtain(handler, ThreadMessageTypes.MSG_FINISHED_ERROR));
        }
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

    private String drainCharsFromInputStream(URLConnection connection) throws IOException {
        InputStreamReader reader = new InputStreamReader(connection.getInputStream());
        StringBuilder sb = new StringBuilder();
        int data = reader.read();
        while (data != -1) {
            sb.append((char) data);
            data = reader.read();
        }
        return sb.toString();
    }

    private HttpsURLConnection prepareConnectionForGet(URL url) throws Exception {
        HttpsURLConnection result = (HttpsURLConnection) url.openConnection();
        result.setSSLSocketFactory(sslContext.getSocketFactory());
        result.setReadTimeout(15000);
        result.setConnectTimeout(30000);
        result.setRequestMethod("GET");
        return result;
    }

    private HttpsURLConnection prepareConnectionForFormPost(URL url, int contentLength) throws Exception {
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

    /*
    private void writeHeadersToConsole(HttpURLConnection connection) {
        Map<String, List<String>> headers = connection.getHeaderFields();
        for (String headerName : headers.keySet()) {
            writeToConsole(
                    "header [name: " + headerName + ", values: " + headers.get(headerName) + "]");
        }
    }
    */

    public void sendRawData() {
        if (sslContext == null) {
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
            sendData();
            logout();
            requestInitialPage();
            checkLoggedIn();
            writeToConsole("SUCCES data sent");
            sendFinishedSuccessNotification();
        } catch (Exception e) {
            Log.e("HTTP_CLIENT", "error", e);
            writeToConsole("ERROR on server communication: " + e.getMessage());
            sendFinishedErrorNotification();
        }
    }

    private void requestInitialPage() throws Exception {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(Settings.getInstance().getDataServerUrl() +
                              "/datacollector_server/secure/main-form.html");
            connection = prepareConnectionForGet(url);
            connection.connect();

            int statusCode = connection.getResponseCode();
            writeToConsole("requestInitialPage server response code: " + statusCode);

            drainCharsFromInputStream(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public int jSecurityCheck() throws Exception {
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
            writeToConsole("login server response code: " + statusCode);

            //writeToConsole("after j_security_check");
            //writeHeadersToConsole(connection);

            String responseString = drainCharsFromInputStream(connection);
            //writeToConsole("login server response: " + responseString);

            return statusCode;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String followRedirectToMainForm() throws Exception {
        HttpsURLConnection connection = null;
        String result = null;
        try {
            URL url = new URL(Settings.getInstance().getDataServerUrl() +
                              "/datacollector_server/secure/main-form.html");
            connection = prepareConnectionForGet(url);
            connection.connect();

            //writeToConsole("after followRedirectToMainForm");
            //writeHeadersToConsole(connection);

            int statusCode = connection.getResponseCode();
            writeToConsole("followRedirectToMainForm server response code: " + statusCode);

            drainCharsFromInputStream(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return result;
    }

    private boolean checkLoggedIn() throws Exception {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(Settings.getInstance().getDataServerUrl() +
                              "/datacollector_server/unchecked/checkLoggedIn");
            connection = prepareConnectionForGet(url);
            connection.connect();
            int statusCode = connection.getResponseCode();
            writeToConsole("checkLoggedIn server response code: " + statusCode);

            //writeToConsole("after checkLoggedIn");
            //writeHeadersToConsole(connection);

            String responseString = drainCharsFromInputStream(connection);
            writeToConsole("checkLoggedIn result string: " + responseString);

            if (statusCode == HttpURLConnection.HTTP_OK) {
                JSONObject checkResult = new JSONObject(responseString);
                boolean result = checkResult.getBoolean("userLoggedIn");
                writeToConsole("checkLoggedIn result: " + result);
                return result;
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return false;
    }

    public void sendData() throws Exception {
        String rawDataString = exportData();
        writeToConsole("sendData data to send size: " + rawDataString.length());
        String compressedData = packageDataToFormParameter(rawDataString);
        writeToConsole("sendData compressed data size: " + compressedData.length());
        String urlParameters ="data=" + URLEncoder.encode(compressedData, "UTF-8");

        HttpsURLConnection connection = null;
        try {
            URL url = new URL(Settings.getInstance().getDataServerUrl() +
                              "/datacollector_server/secure/loadDataFromTablet");
            connection = prepareConnectionForFormPost(url, urlParameters.getBytes().length);
            connection.connect();

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            int statusCode = connection.getResponseCode();
            writeToConsole("sendData server response code: " + statusCode);

            drainCharsFromInputStream(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String exportData() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(baos, "UTF8"));
        try {
            new ExportDataMethodJson(new ExportState(), new DataExtractorToJson(ExportMode.FULL), writer).exportData();
        } finally {
            writer.close();
        }
        return new String(baos.toByteArray());
    }

    private String packageDataToFormParameter(String dataToPackage) throws Exception {
        byte[] bytesToPackage = dataToPackage.getBytes();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(baos);
        gzip.write(bytesToPackage, 0, bytesToPackage.length);
        gzip.finish();
        gzip.close();
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    public void logout() throws Exception {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(Settings.getInstance().getDataServerUrl() +
                              "/datacollector_server/secure/logout");
            connection = prepareConnectionForGet(url);
            connection.connect();

            int statusCode = connection.getResponseCode();
            writeToConsole("logout server response code: " + statusCode);

            drainCharsFromInputStream(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
