package org.milkteamc.autotreechop.design;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Egy szöveg-komponens kirajzolt szélessége képpontban, ahogy a kliens rajzolja.
 *
 * <p>Arra kell, hogy a szöveg MÖGÉ pontos szélességű háttér kerülhessen (chat-csík, title-sáv), és a menücím középre
 * zárható legyen: a háttér glyphekből áll, a hosszát előre tudni kell.
 *
 * <p>Az előtolások a {@code txdesign-font-widths.properties} erőforrásból jönnek (GENERÁLT: {@code gen_widths.py} a
 * kész packból): {@code tsc.<hex>} a small caps ({@code minecraft:tsc}), {@code def.<hex>} a kliens alap fontja a pack
 * PUA-glyphjeivel együtt, {@code gui.<hex>} a menücím betűi ({@code topixity:gui}), {@code cim.<hex>} a title fontja
 * ({@code topixity:cim}, title-egységben). Félkövérnél a kliens minden jelhez +1 képpontot ad. Ismeretlen jel:
 * {@value #UNKNOWN} (a háttér ilyenkor 1–2 képponttal eltérhet).
 */
public final class FontWidths {

    /** Ismeretlen jel becsült előtolása. */
    static final int UNKNOWN = 6;
    /** A szóköz előtolása a default és a tsc fontban ({@code minecraft:include/space}). */
    static final int SPACE = 4;

    private static final Map<Integer, Integer> TSC = new HashMap<>();
    private static final Map<Integer, Integer> DEF = new HashMap<>();
    private static final Map<Integer, Integer> GUI = new HashMap<>();
    /** A title fontja ({@code topixity:cim}); az előtolások title-egységben (1 egység = 4 GUI képpont). */
    private static final Map<Integer, Integer> CIM = new HashMap<>();

    static {
        try (InputStream in = FontWidths.class.getResourceAsStream("/txdesign-font-widths.properties")) {
            if (in != null) {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.isEmpty() || line.charAt(0) == '#') continue;
                    int dot = line.indexOf('.');
                    int eq = line.indexOf('=');
                    if (dot < 0 || eq < dot) continue;
                    int cp = Integer.parseInt(line.substring(dot + 1, eq), 16);
                    int adv = Integer.parseInt(line.substring(eq + 1).trim());
                    Map<Integer, Integer> table = line.startsWith("tsc.") ? TSC : line.startsWith("cim.") ? CIM
                            : line.startsWith("gui.") ? GUI : DEF;
                    table.put(cp, adv);
                }
            }
        } catch (Exception e) {
            // üres táblával minden jel UNKNOWN szélességű — a háttér pontatlan lesz, de semmi nem törik el
        }
    }

    private FontWidths() {}

    /** A komponens teljes szélessége képpontban (a gyerekekkel, az örökölt fonttal és félkövérrel). */
    public static int width(Component component) {
        return walk(component, null, false);
    }

    private static int walk(Component c, Key inheritedFont, boolean inheritedBold) {
        Key font = c.style().font() != null ? c.style().font() : inheritedFont;
        TextDecoration.State b = c.style().decoration(TextDecoration.BOLD);
        boolean bold = b == TextDecoration.State.NOT_SET ? inheritedBold : b == TextDecoration.State.TRUE;
        int w = 0;
        if (c instanceof TextComponent) w += text(((TextComponent) c).content(), font, bold);
        for (Component child : c.children()) w += walk(child, font, bold);
        return w;
    }

    /** Egy szövegdarab szélessége adott fonttal ({@code null} = default). */
    public static int text(String s, Key font, boolean bold) {
        if (Hud.FONT_TITLE.equals(font)) return titleText(s);
        boolean tsc = GuiText.FONT_TSC.equals(font);
        boolean gui = GuiText.FONT_GUI.equals(font);
        int w = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            Integer space = spacing(cp);
            if (space != null) {                       // térköz-karakter: minden fontban ugyanaz, félkövér nélkül
                w += space;
                continue;
            }
            int adv;
            if (gui && cp == 0xE831) adv = -8;
            else if (gui && cp == 0xE832) adv = -169;
            else if (gui && GUI.containsKey(cp)) adv = GUI.get(cp);
            else if (tsc && TSC.containsKey(cp)) adv = TSC.get(cp);
            else if (cp == ' ') adv = SPACE;
            else adv = DEF.getOrDefault(cp, UNKNOWN);
            w += adv + (bold && !gui ? 1 : 0);
        }
        return w;
    }

    /** A térköz-karakterek előtolása (U+E840+i = +2^i, U+E848+i = −2^i), különben {@code null}. */
    static Integer spacing(int cp) {
        if (cp >= 0xE840 && cp <= 0xE847) return 1 << (cp - 0xE840);
        if (cp >= 0xE848 && cp <= 0xE84F) return -(1 << (cp - 0xE848));
        return null;
    }

    /** Szöveg a {@code topixity:cim} fontban (title-egységben; a font maga félkövér, külön félkövér nincs). */
    private static int titleText(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            Integer space = spacing(cp);
            if (space != null) w += space;
            else if (cp == ' ') w += 3;
            else if (cp >= 0xE880 && cp <= 0xE882) w += Hud.BAND_PIECE + 1;   // sáv / vonal darab
            else w += CIM.getOrDefault(cp, 5);
        }
        return w;
    }

    /** Betöltődött-e a tábla (teszthez és indulási naplóhoz). */
    public static int loadedEntries() {
        return TSC.size() + DEF.size() + CIM.size() + GUI.size();
    }
}
