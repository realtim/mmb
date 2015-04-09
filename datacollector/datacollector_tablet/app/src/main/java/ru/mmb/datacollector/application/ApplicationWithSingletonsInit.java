package ru.mmb.datacollector.application;

import android.app.Application;

import ru.mmb.datacollector.conf.AndroidConfigurationAdapter;
import ru.mmb.datacollector.db.SQLiteDatabaseAdapter;

public class ApplicationWithSingletonsInit extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AndroidConfigurationAdapter.init();
        SQLiteDatabaseAdapter.init();
    }
}
