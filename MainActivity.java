package com.klon.motor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private final List<String> labels = new ArrayList<>();
    private final List<String> pkgs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setFitsSystemWindows(true);
        root.setPadding(24, 24, 24, 24);

        TextView title = new TextView(this);
        title.setText("Klon Motor 0.1 (Faz 0 + 1)\nBir uygulamaya dokun: klonlayip baslatilir.");
        title.setTextSize(16);
        title.setGravity(Gravity.START);
        root.addView(title);

        Button logBtn = new Button(this);
        logBtn.setText("Hata kayitlari");
        logBtn.setOnClickListener(v -> startActivity(new Intent(this, LogActivity.class)));
        root.addView(logBtn);

        ListView list = new ListView(this);
        root.addView(list, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        loadApps();
        list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));
        list.setOnItemClickListener((p, v, pos, id) -> {
            final String pkg = pkgs.get(pos);
            new AlertDialog.Builder(this)
                    .setMessage(pkg + " klonlanip baslatilsin mi?")
                    .setNegativeButton("Vazgec", null)
                    .setPositiveButton("Baslat", (d, w) -> launch(pkg))
                    .show();
        });
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        Map<String, String> m = new LinkedHashMap<>();
        for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            m.put(pkg, ri.loadLabel(pm) + "\n" + pkg);
        }
        List<String> sorted = new ArrayList<>(m.values());
        Collections.sort(sorted, String::compareToIgnoreCase);
        for (String s : sorted) {
            labels.add(s);
            pkgs.add(s.substring(s.indexOf('\n') + 1));
        }
    }

    private void launch(String pkg) {
        try {
            Intent li = getPackageManager().getLaunchIntentForPackage(pkg);
            if (li == null || li.getComponent() == null) {
                Toast.makeText(this, "Baslatilabilir ekran bulunamadi", Toast.LENGTH_LONG).show();
                return;
            }
            String cls = li.getComponent().getClassName();
            CrashLog.log("LAUNCH", pkg + " / " + cls);
            Intent i = new Intent(this, Stub0.class);
            i.putExtra(Const.EXTRA_PKG, pkg);
            i.putExtra(Const.EXTRA_CLS, cls);
            startActivity(i);
        } catch (Throwable t) {
            CrashLog.error("LAUNCH", t);
            Toast.makeText(this, "Hata: " + t, Toast.LENGTH_LONG).show();
        }
    }
}
