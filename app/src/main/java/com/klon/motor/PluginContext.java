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
