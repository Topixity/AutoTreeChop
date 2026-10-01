package org.milkteamc.autotreechop.design;

import net.kyori.adventure.text.Component;

import java.lang.reflect.Method;

/**
 * Verzió-függő hívások reflexióval, hogy a modul régebbi Adventure / Paper API ellen is forduljon.
 *
 * <p>{@code shadow_color} (Adventure 4.18+, kliens 1.21.4+): a szöveg MÖGÉ tett háttér-glyphnek (chat-csík, title-sáv)
 * nem lehet árnyéka — különben a kliens 1 képponttal eltolva, sötétebben még egyszer kirajzolná. Régebbi API-n a
 * hívás elmarad (a háttér kap árnyékot: kicsit sötétebb, de minden más működik).
 */
final class Compat {

    private static final Method SHADOW_COLOR;
    private static final Object SHADOW_NONE;

    static {
        Method m = null;
        Object none = null;
        try {
            Class<?> sc = Class.forName("net.kyori.adventure.text.format.ShadowColor");
            none = sc.getMethod("none").invoke(null);
            for (Method cand : Component.class.getMethods()) {
                if (cand.getName().equals("shadowColor") && cand.getParameterCount() == 1
                        && cand.getParameterTypes()[0].isAssignableFrom(sc)) {
                    m = cand;
                    break;
                }
            }
        } catch (Throwable ignored) {
            // régebbi Adventure: nincs shadow_color
        }
        SHADOW_COLOR = m;
        SHADOW_NONE = m == null ? null : none;
    }

    private Compat() {}

    /** A komponens árnyék nélkül (ha az API tudja; különben változatlan). */
    static Component noShadow(Component c) {
        if (SHADOW_COLOR == null) return c;
        try {
            return (Component) SHADOW_COLOR.invoke(c, SHADOW_NONE);
        } catch (Throwable e) {
            return c;
        }
    }

    /** Tudja-e az API az árnyék kikapcsolását (teszthez, naplóhoz). */
    static boolean hasShadowColor() {
        return SHADOW_COLOR != null;
    }
}
