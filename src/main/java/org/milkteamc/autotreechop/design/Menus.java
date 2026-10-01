package org.milkteamc.autotreechop.design;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.Collection;
import java.util.Set;

/**
 * A v3 menü (chest-GUI) címe (user 2026-10-01, {@code topixity-gui-design/v3}): vanilla láda, a nem használt
 * slotokat a CÍM glyphjei takarják le (üvegpanel nincs), a kiválasztott slot zöld keretet kap, a cím kis, ritkított
 * nagybetű a sáv közepén. Tisztán számolás, Bukkit nélkül.
 *
 * <pre>
 *   Component cim = Menus.title("Kozmetikai bolt", 6, lathatoSlotok, kivalasztottSlotok, bedrock);
 *   Inventory inv = Bukkit.createInventory(holder, 54, cim);      // item CSAK a látható slotokra kerüljön
 * </pre>
 *
 * <p><b>Geometria:</b> a cím szövege a GUI bal szélétől x = 8-ról indul; a slot x = 7 + 18·oszlop, y = 17 + 18·sor
 * (a glyph {@code ascent}-je hordozza a sort). A cím glyphje az itemek ALATT van: a slot hátterét és keretét tudja
 * átszínezni, az ikon fölé nem rajzol.
 *
 * <p><b>Bedrock:</b> a glyph-háttér és a térköz ott nem megy — sima szöveges cím (a hívó a letakart slotokat
 * ilyenkor is üresen hagyja).
 */
public final class Menus {

    /** A menücím színe (a jóváhagyott „KIT VÁLASZTÓ" kép színe). */
    public static final TextColor TITLE_COLOR = TextColor.color(0x4A4A4A);
    /** A láda szélessége képpontban. */
    static final int GUI_WIDTH = 176;
    /** A cím szövegének kezdő x-e a GUI bal szélétől. */
    static final int TITLE_X = 8;

    /** Slot-takaró: U+EAA0 + sor·4 + j, ahol j a {1, 2, 4, 8} slot szélesség indexe. */
    private static final char COVER_BASE = '';
    private static final int[] COVER_WIDTHS = {1, 2, 4, 8};
    /** Kijelölő keret a 0. sorra; a többi sor a következő karakter. */
    private static final char FRAME_ROW0 = '';
    /** A keret 18 px széles, az előtolása 19. */
    private static final int FRAME_ADVANCE = 19;

    private Menus() {}

    /**
     * A teljes menücím.
     *
     * @param text     a cím szövege (a font csupa nagybetűvel rajzolja; kis- és nagybetű mindegy)
     * @param rows     a láda sorainak száma (1…6)
     * @param visible  a látható (használt) slotok; {@code null}: nincs takarás, minden slot látszik
     * @param framed   a zöld keretet kapó (kiválasztott / aktív) slotok; lehet {@code null}
     * @param bedrock  Bedrock-játékosnak sima szöveges cím megy
     */
    public static Component title(String text, int rows, Collection<Integer> visible, Collection<Integer> framed,
                                  boolean bedrock) {
        if (bedrock) return Component.text(text == null ? "" : text);
        StringBuilder glyphs = new StringBuilder();
        if (visible != null) glyphs.append(cover(rows, visible));
        if (framed != null) for (int slot : framed) glyphs.append(frame(slot));
        Component label = label(text);
        if (glyphs.length() == 0) return label;
        return Component.textOfChildren(
                Component.text(glyphs.toString()).font(GuiText.FONT_DEFAULT).color(NamedTextColor.WHITE), label);
    }

    /** Cím takarás és keret nélkül (csak a középre zárt felirat). */
    public static Component title(String text, boolean bedrock) {
        return title(text, 6, null, null, bedrock);
    }

    /**
     * A középre zárt felirat a {@code topixity:gui} fonttal. Ha a szöveg nem fér ki a ládába, balról indul.
     * A {@code ·} és más, a fontban nem szereplő jel a default fontból jön (a pack hivatkozása).
     */
    public static Component label(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        int width = FontWidths.text(text, GuiText.FONT_GUI, false);
        int x0 = Math.round((GUI_WIDTH - width) / 2f);
        int shift = width >= GUI_WIDTH - 2 * TITLE_X ? 0 : x0 - TITLE_X;
        return Component.text(GuiText.shift(shift) + text).font(GuiText.FONT_GUI).color(TITLE_COLOR);
    }

    /**
     * A nem látható slotok letakarása: a kurzor a cím kezdetéről (x = 8) indul és oda tér vissza.
     * A visszaadott szöveget a default fonttal, FEHÉR színnel kell kiírni (a takaró képe a láda-panel színe).
     */
    public static String cover(int rows, Collection<Integer> visible) {
        if (rows < 1 || rows > 6) throw new IllegalArgumentException("rows: " + rows);
        Set<Integer> shown = Set.copyOf(visible);
        StringBuilder sb = new StringBuilder();
        int x = 0;                                    // a kurzor a cím-kezdethez képest
        for (int row = 0; row < rows; row++) {
            int col = 0;
            while (col < 9) {
                if (shown.contains(row * 9 + col)) { col++; continue; }
                int end = col;
                while (end < 9 && !shown.contains(row * 9 + end)) end++;
                int target = 7 + 18 * col - TITLE_X;
                sb.append(GuiText.shiftAny(target - x));
                x = target;
                int n = end - col;
                for (int j = COVER_WIDTHS.length - 1; j >= 0; j--) {
                    while (n >= COVER_WIDTHS[j]) {
                        sb.append((char) (COVER_BASE + row * 4 + j)).append(GuiText.shift(-1));
                        x += 18 * COVER_WIDTHS[j];
                        n -= COVER_WIDTHS[j];
                    }
                }
                col = end;
            }
        }
        return sb.append(GuiText.shiftAny(-x)).toString();
    }

    /** A slot zöld kerete (a kurzor a cím kezdetéről indul és oda tér vissza). Default font, FEHÉR szín. */
    public static String frame(int slot) {
        if (slot < 0 || slot >= 54) return "";
        int x = 7 + 18 * (slot % 9) - TITLE_X;
        return GuiText.shiftAny(x) + (char) (FRAME_ROW0 + slot / 9) + GuiText.shiftAny(-(x + FRAME_ADVANCE));
    }
}
