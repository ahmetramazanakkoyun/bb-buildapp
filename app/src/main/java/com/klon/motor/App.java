package com.klon.motor;

import android.app.Application;
import android.content.Context;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

public class App extends Application {
    static App instance;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        instance = this;
        try {
            HiddenApiBypass.addHiddenApiExemptions("L");
        } catch (Throwable ignored) {
        }
        String proc = Application.getProcessName();
        CrashLog.init(base, proc);
        // Klon islemlerinde (:p0, :p1 ...) Instrumentation'a araya gir.
        if (proc != null && proc.contains(":p")) {
            try {
                HookInstrumentation.install();
                CrashLog.log("INIT", "Instrumentation kancasi takildi");
            } catch (Throwable t) {
                CrashLog.error("INIT", t);
            }
        }
    }
}
