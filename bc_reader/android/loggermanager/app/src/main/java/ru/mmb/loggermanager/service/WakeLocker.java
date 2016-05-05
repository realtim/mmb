package ru.mmb.loggermanager.service;

import android.content.Context;
import android.os.PowerManager;

public abstract class WakeLocker {
    private static PowerManager.WakeLock wakeLock = null;

    public static void init(Context ctx) {
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE, "loggermanagerFullWake");
    }

    public static void acquire(Context ctx) {
        wakeLock.acquire();
    }

    public static void release() {
        wakeLock.release();
    }
}
