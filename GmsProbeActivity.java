package com.klon.motor;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Faz 2 - ucuz kesif: GMS'nin kodunu sanal ortamimiza yukleyip Application.onCreate()
 * calistirmayi dener. Hicbir ekran acmaz. Amac: GMS daha ilk adimda cokuyor mu, yoksa
 * ayakta kalabiliyor mu, bunu ucuza ogrenmek. Ayri bir islemde (:gmsprobe) calisir.
 */
public class GmsProbeActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        TextView tv = new TextView(this);
        tv.setPadding(40, 80, 40, 40);
        tv.setText("GMS deneyi calisiyor, lutfen bekleyin...\n"
                + "Ekran 10 saniyeden uzun donarsa son uygulamalardan kapatip\n"
                + "Hata kayitlarina bakabilirsiniz.");
        setContentView(tv);

        new Thread(() -> {
            long t0 = System.currentTimeMillis();
            CrashLog.log("GMSPROBE", "baslatiliyor...");
            try {
                PluginRuntime rt = PluginRuntime.get(App.instance, "com.google.android.gms");
                CrashLog.log("GMSPROBE", "apk bulundu, siniflar yukleniyor...");
                rt.ensureApplication(App.instance);
                long ms = System.currentTimeMillis() - t0;
                CrashLog.log("GMSPROBE", "BASARILI: GMS Application.onCreate tamamlandi, sure=" + ms + "ms");
            } catch (Throwable t) {
                long ms = System.currentTimeMillis() - t0;
                CrashLog.log("GMSPROBE", "COKTU, sure=" + ms + "ms");
                CrashLog.error("GMSPROBE", t);
            }
            runOnUiThread(() -> tv.setText("Deney bitti. Hata kayitlarina bakabilirsiniz. Geri donebilirsiniz."));
        }).start();
    }
}
