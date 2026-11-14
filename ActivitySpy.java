package com.klon.motor;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Faz 2c tanilama: Google/oyun ekranlarinin ne kadar surede, nasil bir sonucla (resultCode)
 * kapandigini kaydeder. "Ekran acilir acilmaz kapaniyor" seklindeki sorunlari gormek icindir.
 */
final class ActivitySpy {
    private static final Map<Activity, Long> opened = new HashMap<>();

    static void install(Application app) {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity a, Bundle b) {
                String cls = a.getClass().getName();
                if (!interesting(cls)) return;
                opened.put(a, System.currentTimeMillis());
                CrashLog.log("ASPY", "ACILDI: " + cls);
            }

            @Override
            public void onActivityResumed(Activity a) {
            }

            @Override
            public void onActivityPaused(Activity a) {
            }

            @Override
            public void onActivityDestroyed(Activity a) {
                String cls = a.getClass().getName();
                if (!interesting(cls)) return;
                Long t0 = opened.remove(a);
                long ms = t0 == null ? -1 : System.currentTimeMillis() - t0;
                String rc = "?";
                String extras = "?";
                try {
                    Field fCode = Activity.class.getDeclaredField("mResultCode");
                    fCode.setAccessible(true);
                    rc = String.valueOf(fCode.get(a));
                    Field fData = Activity.class.getDeclaredField("mResultData");
                    fData.setAccessible(true);
                    Intent data = (Intent) fData.get(a);
                    extras = data == null ? "null" : String.valueOf(data.getExtras());
                } catch (Throwable ignored) {
                }
                CrashLog.log("ASPY", "KAPANDI: " + cls + " sure=" + ms + "ms finishing="
                        + a.isFinishing() + " resultCode=" + rc + " extras=" + extras);
            }

            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
        });
        CrashLog.log("ASPY", "yasam dongusu izleyici takildi");
    }

    private static boolean interesting(String cls) {
        return cls.contains("google") || cls.contains("gms") || cls.contains("SignIn")
                || cls.contains("auth");
    }

    private ActivitySpy() {}
}
