package ru.mmb.loggermanager.activity.timeupdater;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

public class UpdateTimeAlarmReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        WakeLocker.acquire(context);
        new TimeUpdaterThread().start();
    }
}

