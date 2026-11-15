package com.klon.motor;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.PersistableBundle;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Klon isleminde sistem Stub0 ekranini acarken, yerine oyunun gercek ekranini olusturur
 * ve ekranin Context'ini PluginContext ile degistirir.
 */
public final class HookInstrumentation extends Instrumentation {
    private final Instrumentation base;

    // Dongu korumasi: ayni (pkg,cls) cok hizli tekrar tekrar istenirse zinciri kirar.
    private static String loopKey;
    private static int loopStreak;
    private static long loopWindowStart;

    private HookInstrumentation(Instrumentation base) {
        this.base = base;
        for (Field f : Instrumentation.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            try {
                f.setAccessible(true);
                f.set(this, f.get(base));
            } catch (Throwable ignored) {
            }
        }
    }

    static void install() throws Exception {
        Class<?> at = Class.forName("android.app.ActivityThread");
        Object thread = at.getMethod("currentActivityThread").invoke(null);
        Instrumentation cur = (Instrumentation) Reflect.get(at, thread, "mInstrumentation");
        if (cur instanceof HookInstrumentation) return;
        Reflect.set(at, thread, "mInstrumentation", new HookInstrumentation(cur));
    }

    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        String pkg = intent == null ? null : intent.getStringExtra(Const.EXTRA_PKG);
        String cls = intent == null ? null : intent.getStringExtra(Const.EXTRA_CLS);
        if (pkg == null || cls == null) return base.newActivity(cl, className, intent);
        try {
            String key = pkg + "/" + cls;
            long now = android.os.SystemClock.elapsedRealtime();
            if (!key.equals(loopKey) || now - loopWindowStart > 1200) {
                loopKey = key;
                loopStreak = 1;
                loopWindowStart = now;
            } else {
                loopStreak++;
            }
            if (loopStreak > 15) {
                CrashLog.log("LOOP", "cok sik tekrar algilandi (" + loopStreak + "x): " + key
                        + " - zincir kiriliyor");
                return new LoopStopActivity();
            }
            PluginRuntime rt = PluginRuntime.get(App.instance, pkg);
            rt.ensureApplication(App.instance);
            if (rt.launchClass == null) rt.launchClass = cls;
            intent.setExtrasClassLoader(rt.classLoader);
            CrashLog.log("ACTIVITY", "olusturuluyor: " + cls);
            return (Activity) rt.classLoader.loadClass(cls).newInstance();
        } catch (Throwable t) {
            CrashLog.error("newActivity", t);
            ClassNotFoundException e = new ClassNotFoundException("klon: " + cls);
            e.initCause(t);
            throw e;
        }
    }

    private void patch(Activity a) {
        Intent in = a.getIntent();
        String pkg = in == null ? null : in.getStringExtra(Const.EXTRA_PKG);
        String cls = in == null ? null : in.getStringExtra(Const.EXTRA_CLS);
        if (pkg == null || cls == null) return;
        try {
            PluginRuntime rt = PluginRuntime.get(App.instance, pkg);
            if (a.getBaseContext() instanceof PluginContext) return;
            PluginContext pc = new PluginContext(a.getBaseContext(), rt);
            Reflect.set(ContextWrapper.class, a, "mBase", pc);
            Reflect.set(Activity.class, a, "mApplication", rt.application);
            try {
