package com.klon.motor;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.PathClassLoader;

/** Bir klon islemindeki oyunun sinif yukleyicisi, kaynaklari ve Application'i. */
final class PluginRuntime {
    private static PluginRuntime inst;

    final String pkg;
    final ApplicationInfo appInfo;
    final Resources resources;
    final ClassLoader classLoader;
    final File dataRoot;
    private final PackageManager pm;
    Application application;

    static synchronized PluginRuntime get(Context host, String pkg) throws Exception {
        if (inst == null) inst = new PluginRuntime(host, pkg);
        return inst;
    }

    private PluginRuntime(Context host, String pkg) throws Exception {
        this.pkg = pkg;
        this.pm = host.getPackageManager();
        this.appInfo = pm.getApplicationInfo(pkg, 0);
        this.resources = pm.getResourcesForApplication(appInfo);
        this.dataRoot = new File(host.getFilesDir().getParentFile(), "klon/" + pkg);
        //noinspection ResultOfMethodCallIgnored
        dataRoot.mkdirs();

        List<String> apks = new ArrayList<>();
        apks.add(appInfo.sourceDir);
        if (appInfo.splitSourceDirs != null) {
            for (String s : appInfo.splitSourceDirs) apks.add(s);
        }
        String abi = Build.SUPPORTED_ABIS[0];
        StringBuilder libs = new StringBuilder();
        if (appInfo.nativeLibraryDir != null) libs.append(appInfo.nativeLibraryDir);
        for (String a : apks) {
            if (libs.length() > 0) libs.append(File.pathSeparator);
            libs.append(a).append("!/lib/").append(abi);
        }
        StringBuilder dex = new StringBuilder();
        for (String a : apks) {
            if (dex.length() > 0) dex.append(File.pathSeparator);
            dex.append(a);
        }
        ClassLoader parent = ClassLoader.getSystemClassLoader().getParent();
        this.classLoader = new PathClassLoader(dex.toString(), libs.toString(), parent);
        CrashLog.log("RUNTIME", "pkg=" + pkg + " apks=" + apks.size() + " abi=" + abi
                + "\n  dex=" + dex + "\n  libs=" + libs);
        
        // Tanilama: Unity'nin okumaya calistigi dosya hangi APK'da?
        for (String a : apks) {
            File f = new File(a);
            try (java.util.zip.ZipFile z = new java.util.zip.ZipFile(a)) {
                boolean has = z.getEntry("assets/bin/Data/unity_app_guid") != null;
                CrashLog.log("DIAG", f.getName() + " okunabilir=" + f.canRead() + " unity_app_guid=" + has);
            } catch (Throwable t) {
                CrashLog.log("DIAG", f.getName() + " zip acilamadi: " + t);
            }
        }
        try {
            resources.getAssets().open("bin/Data/unity_app_guid").close();
            CrashLog.log("DIAG", "AssetManager ile unity_app_guid OKUNDU");
        } catch (Throwable t) {
            CrashLog.log("DIAG", "AssetManager ile unity_app_guid okunamadi: " + t);
        }
    }

    /** Oyunun Application sinifini olusturur (ana iplikte, ilk ekrandan once). */
    synchronized void ensureApplication(Context host) throws Exception {
        if (application != null) return;
        String name = appInfo.className != null ? appInfo.className : "android.app.Application";
        Application app = (Application) classLoader.loadClass(name).newInstance();
        Method attach = Application.class.getDeclaredMethod("attach", Context.class);
        attach.setAccessible(true);
        attach.invoke(app, new PluginContext(host, this));
        application = app;
        CrashLog.log("RUNTIME", "Application olusturuldu: " + name);
        app.onCreate();
        CrashLog.log("RUNTIME", "Application.onCreate bitti");
    }

    int themeFor(String activityClass) {
        try {
            ActivityInfo ai = pm.getActivityInfo(new ComponentName(pkg, activityClass), 0);
            int t = ai.getThemeResource();
            if (t != 0) return t;
        } catch (Exception e) {
            CrashLog.error("THEME", e);
        }
        return appInfo.theme;
    }
}
