package ru.mmb.datacollector.activity.transport;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.transport.transpexport.TransportExportActivity;
import ru.mmb.datacollector.activity.transport.transpimport.TransportImportActivity;
import ru.mmb.datacollector.model.registry.Settings;

public class TransportInputActivity extends Activity {
    private Button btnImportDictionaries;
    private Button btnSaveDataToFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getInstance().setCurrentContext(this);

        setContentView(R.layout.transport_input);

        btnImportDictionaries = (Button) findViewById(R.id.transportInput_importDictionariesBtn);
        btnSaveDataToFile = (Button) findViewById(R.id.transportInput_saveDataToFileBtn);

        btnImportDictionaries.setOnClickListener(new ImportDictionariesClickListener());
        btnSaveDataToFile.setOnClickListener(new SaveToDataFileClickListener());
    }

    private class ImportDictionariesClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), TransportImportActivity.class);
            startActivity(intent);
        }
    }

    private class SaveToDataFileClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), TransportExportActivity.class);
            startActivity(intent);
        }
    }
}
