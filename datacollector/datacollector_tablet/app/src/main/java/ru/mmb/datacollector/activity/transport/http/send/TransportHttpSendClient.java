package ru.mmb.datacollector.activity.transport.http.send;

import android.os.Handler;
import android.util.Base64;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

import ru.mmb.datacollector.activity.transport.http.TransportHttpClient;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.transport.exporter.DataExtractorToJson;
import ru.mmb.datacollector.transport.exporter.ExportMode;
import ru.mmb.datacollector.transport.exporter.ExportState;
import ru.mmb.datacollector.transport.exporter.data.ExportDataMethodJson;

public class TransportHttpSendClient extends TransportHttpClient {
    public TransportHttpSendClient(Handler handler) {
        super(handler);
    }

    @Override
    protected void doDataTransport() throws Exception {
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

            writeToConsole("sendData: " + connection.getResponseMessage());

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
}
