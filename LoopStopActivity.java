package com.klon.motor;

import android.app.Activity;
import android.os.Bundle;

/**
 * Bir ekranin cok hizli tekrar tekrar acilmaya calisildigi (dongu) durumlarda,
 * HookInstrumentation bu zararsiz ekrani kullanip dongu zincirini kirar.
 */
public class LoopStopActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        CrashLog.log("LOOP", "zincir kirildi, bu ekran kapatiliyor");
        finish();
    }
}
