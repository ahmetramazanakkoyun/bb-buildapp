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
                Class<?> ctw = Class.forName("android.view.ContextThemeWrapper");
                Reflect.set(ctw, a, "mResources", null);
                Reflect.set(ctw, a, "mTheme", null);
            } catch (Throwable t) {
                CrashLog.error("patch-tema", t);
            }
            int theme = rt.themeFor(cls);
            if (theme != 0) a.setTheme(theme);
            CrashLog.log("ACTIVITY", "baglam degistirildi, tema=0x" + Integer.toHexString(theme));
        } catch (Throwable t) {
            CrashLog.error("patch", t);
        }
    }

    @Override
    public void callActivityOnCreate(Activity a, Bundle b) {
        patch(a);
        base.callActivityOnCreate(a, b);
    }

    @Override
    public void callActivityOnCreate(Activity a, Bundle b, PersistableBundle p) {
        patch(a);
        base.callActivityOnCreate(a, b, p);
    }

    /** Oyunun baska ekran acma istegini (ve yeniden baslatma istegini) klon duzenine cevirir. */
    private Intent rewrite(Intent intent) {
        try {
            PluginRuntime rt = PluginRuntime.current();
            if (rt == null || intent == null || intent.getComponent() == null) return intent;
            ComponentName cn = intent.getComponent();
            if (!rt.pkg.equals(cn.getPackageName())) return intent;
            String cls = cn.getClassName();
            if (cls.endsWith("ProcessPhoenix")) {
                Intent restart = new Intent(App.instance, Stub0.class);
                restart.putExtra(Const.EXTRA_PKG, rt.pkg);
                restart.putExtra(Const.EXTRA_CLS, rt.launchClass);
                restart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Intent ph = new Intent(App.instance, PhoenixActivity.class);
                ph.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ph.putExtra("restart", restart);
                ph.putExtra("pid", Process.myPid());
                CrashLog.log("PHOENIX", "oyun yeniden baslatma istedi");
                return ph;
            }
            Intent s = new Intent(intent);
            s.setComponent(new ComponentName(App.instance, Stub0.class));
            s.putExtra(Const.EXTRA_PKG, rt.pkg);
            s.putExtra(Const.EXTRA_CLS, cls);
            CrashLog.log("START", "ekran yonlendirildi: " + cls);
            return s;
        } catch (Throwable t) {
            CrashLog.error("rewrite", t);
            return intent;
        }
    }

    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token,
                                            Activity target, Intent intent, int requestCode,
                                            Bundle options) {
        Intent out = rewrite(intent);
        try {
            Method m = Instrumentation.class.getDeclaredMethod("execStartActivity",
                    Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class,
                    int.class, Bundle.class);
            m.setAccessible(true);
            return (ActivityResult) m.invoke(base, who, contextThread, token, target, out,
                    requestCode, options);
        } catch (InvocationTargetException e) {
            Throwable c = e.getCause();
            CrashLog.error("execStartActivity", c);
            if (c instanceof RuntimeException) throw (RuntimeException) c;
            throw new RuntimeException(c);
        } catch (Throwable t) {
            CrashLog.error("execStartActivity-yansima", t);
            throw new RuntimeException(t);
        }
    }
}
