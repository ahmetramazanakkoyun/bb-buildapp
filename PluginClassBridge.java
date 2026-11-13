package com.klon.motor;

import java.lang.reflect.Field;

/**
 * Ana uygulamanin sinif yukleyicisine, oyunun siniflarini da bulabilmesi icin bir kopru ekler.
 * Ekranlar arasi gonderilen paketli (Parcelable) veriler ana yukleyiciyle acildigi icin gerekir.
 */
final class PluginClassBridge {
    private static boolean done;

    static synchronized void install(final ClassLoader pluginCl) {
        if (done) return;
        done = true;
        try {
            ClassLoader hostCl = App.class.getClassLoader();
            Field f = ClassLoader.class.getDeclaredField("parent");
            f.setAccessible(true);
            final ClassLoader origParent = (ClassLoader) f.get(hostCl);
            ClassLoader bridge = new ClassLoader(origParent) {
                @Override
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                    return pluginCl.loadClass(name);
                }
            };
            f.set(hostCl, bridge);
            CrashLog.log("BRIDGE", "sinif yukleyici koprusu takildi");
        } catch (Throwable t) {
            CrashLog.error("BRIDGE", t);
        }
    }

    private PluginClassBridge() {}
}
