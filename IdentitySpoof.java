package com.klon.motor;

/**
 * Faz 2b (ilk deneme): GMS'nin "bu cagrini kim yapiyor" sorgusuna (getPackagesForUid,
 * getNameForUid) klon isleminin gercek UID'si icin oyunun paket adini da ekler.
 * Bu, GMS'nin "Unknown calling package" guvenlik reddini hedefler; GMS ayrica imza
 * kontrolu yapiyorsa bu yama yetmez.
 */
final class IdentitySpoof {
    static volatile int spoofUid = -1;
    static volatile String spoofPkg;

    static void activate(String pkg) {
        spoofUid = android.os.Process.myUid();
        spoofPkg = pkg;
        CrashLog.log("SPOOF", "uid=" + spoofUid + " icin paket eklenecek: " + pkg);
    }

    static String[] addIfMatch(int uid, String[] original) {
        if (uid != spoofUid || spoofPkg == null) return original;
        if (original == null) return new String[]{spoofPkg};
        for (String s : original) if (spoofPkg.equals(s)) return original;
        String[] out = new String[original.length + 1];
        System.arraycopy(original, 0, out, 0, original.length);
        out[original.length] = spoofPkg;
        return out;
    }

    private IdentitySpoof() {}
}
