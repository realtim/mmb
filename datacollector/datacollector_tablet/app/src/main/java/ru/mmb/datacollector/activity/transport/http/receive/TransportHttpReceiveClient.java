package ru.mmb.datacollector.activity.transport.http.receive;

import android.os.Handler;
import android.util.Base64;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import ru.mmb.datacollector.activity.transport.http.TransportHttpClient;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.transport.importer.ImportState;
import ru.mmb.datacollector.transport.importer.Importer;

public class TransportHttpReceiveClient extends TransportHttpClient {
    public TransportHttpReceiveClient(Handler handler) {
        super(handler);
    }

    @Override
    protected void doDataTransport() throws Exception {
        HttpsURLConnection connection = null;
        String receivedData = null;
        try {
            URL url = new URL(Settings.getInstance().getDataServerUrl() +
                              "/datacollector_server/secure/downloadDataForTablet");
            connection = prepareConnectionForGet(url);
            connection.connect();

            writeToConsole("receiveData: " + connection.getResponseMessage());

            receivedData = drainCharsFromInputStream(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        if (receivedData != null) {
            writeToConsole("receiveData chars arrived: " + receivedData.length());
            String jsonString = decodeReceivedData(receivedData);
            new Importer(new ImportState(), null).importPackageFromJsonObject(new JSONObject(jsonString));
            writeToConsole("receiveData data imported");
        }
    }

    private String decodeReceivedData(String receivedData) throws Exception {
        String result = null;
        byte[] decodedZip = Base64.decode(receivedData, Base64.NO_WRAP);
        GZIPInputStream zipInput = new GZIPInputStream(new ByteArrayInputStream(decodedZip, 0, decodedZip.length));
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead = zipInput.read(buffer);
            while (bytesRead != -1) {
                baos.write(buffer, 0, bytesRead);
                bytesRead = zipInput.read(buffer);
            }
            result = new String(baos.toByteArray());
            writeToConsole("receiveData unzipped chars: " + result.length());
        } finally {
            zipInput.close();
        }
        return result;
    }
}
