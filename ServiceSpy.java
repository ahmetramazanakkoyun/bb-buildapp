package com.klon.motor;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Faz 2a: Oyunun ActivityManager ve PackageManager'a yaptigi Google ile ilgili cagrilari
 * kaydeder. Davranisi DEGISTIRMEZ, sadece loga yazar (neyin gerektigini gormek icin).
 */
final class ServiceSpy {
    private static final Set<String> AM_NAMES = new HashSet<>(Arrays.asList(
            "bindService", "bindIsolatedService", "bindServiceInstance",
            "startService", "stopService", "getContentProvider"));
    private static final Set<String> PM_ALWAYS = new HashSet<>(Arrays.asList(
            "getPackagesForUid", "getNameForUid", "getNameForUids", "getPackageUid",
            "checkUidSignatures"));
    private static final Set<String> PM_FILTERED = new HashSet<>(Arrays.asList(
            "getPackageInfo", "getApplicationInfo", "getInstallerPackageName",
            "getInstallSourceInfo", "checkSignatures", "resolveService", "queryIntentServices"));
    private static final Set<String> seen = new HashSet<>();
    private static int count;
    private static boolean installed;

    static synchronized void install() {
        if (installed) return;
        installed = true;
        try {
            Class<?> amClass = Class.forName("android.app.ActivityManager");
            Field sing = amClass.getDeclaredField("IActivityManagerSingleton");
            sing.setAccessible(true);
            Object singleton = sing.get(null);
            Class<?> sClass = Class.forName("android.util.Singleton");
            Method get = sClass.getDeclaredMethod("get");
            get.setAccessible(true);
            Object real = get.invoke(singleton);
            Field inst = sClass.getDeclaredField("mInstance");
            inst.setAccessible(true);
            Class<?> iam = Class.forName("android.app.IActivityManager");
            inst.set(singleton, proxy(iam, new H("AM", real, true)));
            CrashLog.log("SPY", "ActivityManager izleme takildi");
        } catch (Throwable t) {
            CrashLog.error("SPY-AM", t);
        }
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            Method gpm = at.getDeclaredMethod("getPackageManager");
            gpm.setAccessible(true);
            Object real = gpm.invoke(null);
            Class<?> ipm = Class.forName("android.content.pm.IPackageManager");
            Object px = proxy(ipm, new H("PM", real, false));
            Field spm = at.getDeclaredField("sPackageManager");
            spm.setAccessible(true);
            spm.set(null, px);
            PackageManager pm = App.instance.getPackageManager();
            Field mPM = pm.getClass().getDeclaredField("mPM");
            mPM.setAccessible(true);
            mPM.set(pm, px);
            CrashLog.log("SPY", "PackageManager izleme takildi");
        } catch (Throwable t) {
            CrashLog.error("SPY-PM", t);
        }
    }

    private static Object proxy(Class<?> iface, InvocationHandler h) {
        return Proxy.newProxyInstance(ServiceSpy.class.getClassLoader(), new Class<?>[]{iface}, h);
    }

    private static final class H implements InvocationHandler {
        final String tag;
        final Object real;
        final boolean am;

        H(String tag, Object real, boolean am) {
            this.tag = tag;
            this.real = real;
            this.am = am;
        }

              @Override
        public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
            Object r;
            try {
                r = m.invoke(real, args);
            } catch (InvocationTargetException e) {
                Throwable c = e.getCause();
                note(m.getName(), args, "HATA " + c);
                throw c;
            }
            r = spoof(m.getName(), args, r);
            note(m.getName(), args, r);
            return r;
        }

        /** getPackagesForUid/getNameForUid donusunde, klon islemimizin UID'si icin oyunun paketini de ekler. */
        private Object spoof(String name, Object[] args, Object r) {
            try {
                if (!am && "getPackagesForUid".equals(name) && args != null && args.length > 0
                        && args[0] instanceof Integer && r instanceof String[]) {
                    return IdentitySpoof.addIfMatch((Integer) args[0], (String[]) r);
                }
                if (!am && "getNameForUid".equals(name) && args != null && args.length > 0
                        && args[0] instanceof Integer && r instanceof String
                        && (Integer) args[0] == IdentitySpoof.spoofUid
                        && IdentitySpoof.spoofPkg != null) {
                    return IdentitySpoof.spoofPkg;
                }
            } catch (Throwable ignored) {
            }
            return r;
        }

        private void note(String name, Object[] args, Object result) {
            try {
                boolean always = am ? AM_NAMES.contains(name) : PM_ALWAYS.contains(name);
                boolean filtered = !am && PM_FILTERED.contains(name);
                if (!always && !filtered) return;
                String a = fmt(args);
                if (filtered || am) {
                    String l = a.toLowerCase();
                    if (!(l.contains("google") || l.contains("gms") || l.contains("vending")
                            || l.contains("gamespark"))) return;
                }
                String res = fmt(result);
                String key = tag + name + a + res;
                synchronized (seen) {
                    if (count > 300 || !seen.add(key)) return;
                    count++;
                }
                CrashLog.log("SPY", tag + "." + name + "(" + cut(a, 230) + ") -> " + cut(res, 160));
            } catch (Throwable ignored) {
            }
        }
    }

    private static String fmt(Object o) {
        if (o == null) return "null";
        if (o instanceof Object[]) {
            StringBuilder sb = new StringBuilder("[");
            for (Object x : (Object[]) o) sb.append(fmt(x)).append(", ");
            return sb.append("]").toString();
        }
        if (o instanceof String || o instanceof Number || o instanceof Boolean
                || o instanceof Intent || o instanceof ComponentName) return o.toString();
        if (o instanceof PackageInfo) return "PackageInfo:" + ((PackageInfo) o).packageName;
        return o.getClass().getSimpleName();
    }

    private static String cut(String s, int n) {
        return s.length() > n ? s.substring(0, n) + "..." : s;
    }

    private ServiceSpy() {}
}
