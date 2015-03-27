package ru.mmb.datacollector.activity.transport;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.transport.bclogger.send.TransportLoggerSendActivity;
import ru.mmb.datacollector.activity.transport.transpimport.TransportImportActivity;
import ru.mmb.datacollector.model.registry.Settings;

public class TransportInputActivity extends Activity {
    private Button btnImportDictionaries;
    private Button btnSendToServer;
    private Button btnSendLoggerData;
    private Button btnSaveDataToFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getInstance().setCurrentContext(this);

        setContentView(R.layout.transport_input);

        btnImportDictionaries = (Button) findViewById(R.id.transportInput_importDictionariesBtn);
        btnSendToServer = (Button) findViewById(R.id.transportInput_sendToServerBtn);
        btnSendLoggerData = (Button) findViewById(R.id.transportInput_sendLoggerDataBtn);
        btnSaveDataToFile = (Button) findViewById(R.id.transportInput_saveDataToFileBtn);

        btnImportDictionaries.setOnClickListener(new ImportDictionariesClickListener());
        btnSendLoggerData.setOnClickListener(new SendLoggerDataClickListener());

        // TODO add all button click listeners
    }

    private class ImportDictionariesClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), TransportImportActivity.class);
            startActivity(intent);
        }
    }

    private class SendLoggerDataClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), TransportLoggerSendActivity.class);
            startActivity(intent);
        }
    }
}
