package ru.mmb.loggermanager.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.SystemClock;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import ru.mmb.loggermanager.conf.Configuration;

public class UpdateTimeAlarmListener implements WakefulIntentService.AlarmListener {
    @Override
    public void scheduleAlarms(AlarmManager alarmManager, PendingIntent pendingIntent, Context context) {
        int alarmInterval = Configuration.getInstance().getUpdatePeriodMinutes();
        // FIXME restore alarm interval to minutes (60000)
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 60000,
                alarmInterval * 10000L, pendingIntent);
    }

    @Override
    public void sendWakefulWork(Context context) {
        WakefulIntentService.sendWakefulWork(context, UpdateTimeService.class);
    }

    @Override
    public long getMaxAge(Context context) {
        // FIXME restore alarm interval to minutes (60000)
        int alarmInterval = Configuration.getInstance().getUpdatePeriodMinutes();
        return (alarmInterval * 2 * 10000L);
    }
}
