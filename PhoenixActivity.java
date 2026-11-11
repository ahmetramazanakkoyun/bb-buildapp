package com.klon.motor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;

/**
 * Oyunun "uygulamayi yeniden baslat" (ProcessPhoenix) istegini karsilar: eski klon islemini
 * oldurur, sonra klonu yeni bir islemde baslatir. Ayri islemde (:phoenix) calisir.
 */
public class PhoenixActivity extends Activity {
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Intent in = getIntent();
        Intent restart = in.getParcelableExtra("restart");
        int pid = in.getIntExtra("pid", -1);
        if (pid > 0) Process.killProcess(pid);
        if (restart != null) startActivity(restart);
        finish();
        Runtime.getRuntime().exit(0);
    }
}
