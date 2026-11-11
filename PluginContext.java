package com.klon.motor;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/** Oyunun gordugu Context: paket adi, kaynak, sinif yukleyici ve klasorler oyuna ait. */
final class PluginContext extends ContextWrapper {
    private final PluginRuntime rt;

    PluginContext(Context base, PluginRuntime rt) {
        super(base);
        this.rt = rt;
    }

    @Override public String getPackageName() { return rt.pkg; }
    @Override public ApplicationInfo getApplicationInfo() { return rt.appInfo; }
    @Override public Resources getResources() { return rt.resources; }
    @Override public AssetManager getAssets() { return rt.resources.getAssets(); }
    @Override public ClassLoader getClassLoader() { return rt.classLoader; }

    @Override
    public String getPackageCodePath() {
        CrashLog.log("CTX", "getPackageCodePath -> " + rt.appInfo.sourceDir);
        return rt.appInfo.sourceDir;
    }

    @Override
    public String getPackageResourcePath() {
        CrashLog.log("CTX", "getPackageResourcePath -> " + rt.appInfo.sourceDir);
        return rt.appInfo.sourceDir;
    }

    @Override
    public Context getApplicationContext() {
        return rt.application != null ? rt.application : this;
    }

    private File dir(String name) {
        File d = new File(rt.dataRoot, name);
        //noinspection ResultOfMethodCallIgnored
        d.mkdirs();
        return d;
    }

    @Override public File getFilesDir() { return dir("files"); }
    @Override public File getCacheDir() { return dir("cache"); }
    @Override public File getCodeCacheDir() { return dir("code_cache"); }
    @Override public File getNoBackupFilesDir() { return dir("no_backup"); }
    @Override public File getDir(String name, int mode) { return dir("app_" + name); }
    @Override public File getDatabasePath(String name) { return new File(dir("databases"), name); }

    // --- Dis depolama klasorleri: oyuna ait, yazilabilir klasorlere yonlendir ---
    private File ext(String sub) {
        return dir("ext/" + sub);
    }

    @Override
    public File getExternalFilesDir(String type) {
        File d = ext(type == null ? "files" : "files/" + type);
        CrashLog.log("CTX", "getExternalFilesDir(" + type + ") -> " + d);
        return d;
    }

    @Override
    public File[] getExternalFilesDirs(String type) {
        return new File[]{getExternalFilesDir(type)};
    }

    @Override
    public File getExternalCacheDir() {
        File d = ext("cache");
        CrashLog.log("CTX", "getExternalCacheDir -> " + d);
        return d;
    }

    @Override
    public File[] getExternalCacheDirs() {
        return new File[]{getExternalCacheDir()};
    }

    @Override
    public File getObbDir() {
        File d = ext("obb");
        CrashLog.log("CTX", "getObbDir -> " + d);
        return d;
    }

    @Override
    public File[] getObbDirs() {
        return new File[]{getObbDir()};
    }

    @Override
    public File[] getExternalMediaDirs() {
        return new File[]{ext("media")};
    }

    @Override
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        return new FileInputStream(new File(getFilesDir(), name));
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        return new FileOutputStream(new File(getFilesDir(), name), (mode & MODE_APPEND) != 0);
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return super.getSharedPreferences("klon_" + rt.pkg + "_" + name, MODE_PRIVATE);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory f) {
        return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), f);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory f,
                                               DatabaseErrorHandler h) {
        return SQLiteDatabase.openDatabase(getDatabasePath(name).getPath(), f,
                SQLiteDatabase.CREATE_IF_NECESSARY, h);
    }
}
