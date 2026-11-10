package com.klon.motor;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/** Faz 0: bilgisayarsiz hata ayiklama. Kendi uygulamamizin kayitlarini gosterir ve kopyalatir. */
public class LogActivity extends Activity {
    private TextView tv;
    private String current = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setFitsSystemWindows(true);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button refresh = new Button(this);
        refresh.setText("Yenile");
        refresh.setOnClickListener(v -> refresh());
        Button copy = new Button(this);
        copy.setText("Kopyala");
        copy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("klon-log", current));
            Toast.makeText(this, "Kopyalandi", Toast.LENGTH_SHORT).show();
        });
        Button clear = new Button(this);
        clear.setText("Temizle");
        clear.setOnClickListener(v -> {
            CrashLog.clear(this);
            try {
                new ProcessBuilder("logcat", "-c").start().waitFor();
            } catch (Exception ignored) {
            }
            refresh();
        });
        row.addView(refresh);
        row.addView(copy);
        row.addView(clear);
        root.addView(row);

        tv = new TextView(this);
        tv.setTextSize(10);
        tv.setTextIsSelectable(true);
        ScrollView sv = new ScrollView(this);
        sv.addView(tv);
        root.addView(sv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        refresh();
    }

    private void refresh() {
        tv.setText("Yukleniyor...");
        new Thread(() -> {
            final String s = collect();
            runOnUiThread(() -> {
                current = s;
                tv.setText(s);
            });
        }).start();
    }

    private String collect() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== KLON MOTOR KAYDI ===\n").append(CrashLog.read(this));
        sb.append("\n=== LOGCAT (filtreli) ===\n");
        try {
            Process p = new ProcessBuilder("logcat", "-d", "-t", "4000", "-v", "time")
                    .redirectErrorStream(true).start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.contains("KlonMotor") || line.contains("AndroidRuntime")
                        || line.contains("FATAL") || line.contains("Fatal signal")
                        || line.contains(" E/") || line.contains("backtrace")
                        || line.contains("#00 pc") || line.contains("#01 pc")
                        || line.contains("Caused by")) {
                    sb.append(line).append('\n');
                }
            }
        } catch (Exception e) {
            sb.append("(logcat okunamadi: ").append(e).append(")\n");
        }
        String s = sb.toString();
        return s.length() > 80000 ? s.substring(s.length() - 80000) : s;
    }
}
