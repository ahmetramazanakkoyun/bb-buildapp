package com.klon.motor;

import android.os.Handler;
import android.os.Looper;

/**
 * Ana iplikte Google hizmetlerinden (GMS) gelen "Unknown calling package" tipi istisnalari
 * yutar, oyunun calismaya devam etmesini saglar. Baska hatalar eskisi gibi cokertir.
 */
final class CrashGuard {
    private static boolean installed;

    static void install() {
        if (installed) return;
        installed = true;
        new Handler(Looper.getMainLooper()).post(() -> {
            while (true) {
                try {
                    Looper.loop();
                    return;
                } catch (Throwable t) {
                    if (isGmsNoise(t)) {
                        CrashLog.log("GUARD", "GMS istisnasi yutuldu: " + t);
                    } else {
                        CrashLog.error("GUARD-GERCEK-HATA", t);
                        if (t instanceof RuntimeException) throw (RuntimeException) t;
                        if (t instanceof Error) throw (Error) t;
                        throw new RuntimeException(t);
                    }
                }
            }
        });
    }

    private static boolean isGmsNoise(Throwable t) {
        int depth = 0;
        for (Throwable c = t; c != null && depth++ < 8; c = c.getCause()) {
            String m = String.valueOf(c.getMessage());
            if (m.contains("Unknown calling package")) return true;
            if (c instanceof SecurityException) {
                for (StackTraceElement e : c.getStackTrace()) {
                    String cn = e.getClassName();
                    if (cn.startsWith("com.google.android.gms") || cn.startsWith("m7.")) return true;
                }
            }
        }
        return false;
    }

    private CrashGuard() {}
}
