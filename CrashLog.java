package com.klon.motor;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Faz 0: tum islemlerin (ana + :p0) kayitlarini tek dosyada toplar. */
public final class CrashLog {
    private static File file;
    private static String proc = "?";

    static void init(Context c, String processName) {
        proc = processName == null ? "?" : processName;
        file = new File(c.getFilesDir(), "klon-log.txt");
        final Thread.UncaughtExceptionHandler old = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            log("CRASH", "thread=" + t.getName() + "\n" + Log.getStackTraceString(e));
            if (old != null) old.uncaughtException(t, e);
        });
    }

    static synchronized void log(String tag, String msg) {
        Log.e("KlonMotor", tag + ": " + msg);
        if (file == null) return;
        try (FileOutputStream o = new FileOutputStream(file, true)) {
            String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
            o.write((time + " [" + proc + "] " + tag + ": " + msg + "\n").getBytes("UTF-8"));
        } catch (IOException ignored) {
        }
    }

    static void error(String tag, Throwable t) {
        log(tag, Log.getStackTraceString(t));
    }

    static String read(Context c) {
        File f = new File(c.getFilesDir(), "klon-log.txt");
        if (!f.exists()) return "(kayit yok)\n";
        try (FileInputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } catch (IOException e) {
            return "(okunamadi: " + e + ")\n";
        }
    }

    static void clear(Context c) {
        //noinspection ResultOfMethodCallIgnored
        new File(c.getFilesDir(), "klon-log.txt").delete();
    }

    private CrashLog() {}
}
