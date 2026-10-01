package org.milkteamc.autotreechop.design;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Bedrock (Geyser / Floodgate) játékos felismerése — platformfüggetlen mag.
 *
 * <p>A Bedrock-kliensen a pack fontjai, a negatív térköz és a glyph-háttér nem működik: ott egyszerűsített (olvasható)
 * nézet kell. A Floodgate API-t reflexióval hívjuk (nincs fordítási függőség); ha nincs Floodgate, a Floodgate-UUID
 * formája dönt (a felső 64 bit 0).
 *
 * <p>Bukkit / Paper: {@link BukkitDesign#isBedrock}. Velocity: a hívó adja meg a Floodgate plugin classloaderét.
 */
public final class Bedrock {

    private Bedrock() {}

    /** A Floodgate-UUID formája: a felső 64 bit 0. */
    public static boolean byUuid(UUID id) {
        return id != null && id.getMostSignificantBits() == 0L;
    }

    /**
     * @param floodgateLoader a Floodgate plugin classloadere ({@code null}: nincs Floodgate → az UUID formája dönt)
     */
    public static boolean isBedrock(UUID id, ClassLoader floodgateLoader) {
        if (id == null) return false;
        if (floodgateLoader != null) {
            try {
                Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi", true, floodgateLoader);
                Object instance = api.getMethod("getInstance").invoke(null);
                Method m = api.getMethod("isFloodgatePlayer", UUID.class);
                return (boolean) m.invoke(instance, id);
            } catch (Throwable ignored) {
                // tovább az UUID-alapú felismerésre
            }
        }
        return byUuid(id);
    }
}
