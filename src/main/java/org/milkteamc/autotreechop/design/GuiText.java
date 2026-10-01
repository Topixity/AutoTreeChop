package org.milkteamc.autotreechop.design;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.regex.Pattern;

/**
 * A Topixity design közös szöveg-eszközei: fontok, small caps ({@code minecraft:tsc}), térköz-karakterek, MiniMessage.
 *
 * <p>KANONIKUS FORRÁS: {@code claude-brain/tools/txdesign/java} — a pluginokba a {@code sync.sh} másolja, a csomagnév
 * átírásával. Ne a pluginban szerkeszd: a kanonikus példányt javítsd, és szinkronizáld újra.
 *
 * <p><b>Fontok (txPack-network, 2026-10-01):</b> minden magánhasználatú (PUA) design-glyph a {@code minecraft:default}
 * fontban van; a {@code topixity:gui} a menücím betűit adja (Figtree Bold, nagybetű) és a defaultra hivatkozik; a
 * {@code minecraft:tsc} a small caps, a hiányzó jeleknél a pack a defaultra esik vissza. A {@link #smallCaps} ennek
 * ellenére kódból is default fontra teszi a {@code tsc}-ből hiányzó jeleket — régebbi packkal is helyes marad.
 */
public final class GuiText {

    /** A pack small caps fontja. */
    public static final Key FONT_TSC = Key.key("minecraft", "tsc");
    /** A vanilla font (és minden PUA design-glyph fontja). */
    public static final Key FONT_DEFAULT = Key.key("minecraft", "default");
    /** A menücím fontja: Figtree-nagybetűk; minden más jel a default fontból jön. */
    public static final Key FONT_GUI = Key.key("topixity", "gui");

    /** A pack {@code tsc.png}-jének karakterkészlete. A szóközt a font {@code include/space} referenciája adja. */
    static final String TSC_CHARS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "áéíóöőúüűÁÉÍÓÖŐÚÜŰ0123456789.,:;!?()[]{}<>/|-_+=*&%$#@\"'";

    /** Egy vagy több egymás utáni jel, ami NINCS a {@code tsc}-ben (a whitespace és a PUA kivételével). */
    private static final Pattern NOT_TSC = Pattern.compile(notTscRegex());

    private static final TextReplacementConfig TSC_FALLBACK = TextReplacementConfig.builder()
            .match(NOT_TSC)
            .replacement((match, builder) -> builder.font(FONT_DEFAULT))
            .build();

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** Térköz-karakterek (a default, a gui és a cim fontban azonosak): U+E840+i = +2^i px, U+E848+i = −2^i px (i = 0…7). */
    private static final char SHIFT_PLUS = '';
    private static final char SHIFT_MINUS = '';

    private GuiText() {}

    private static String notTscRegex() {
        // A magánhasználatú tartomány (U+E000–F8FF) a pack glyphjeié: azok a SAJÁT fontjukkal jönnek,
        // a tartalék-ág nem írhatja át őket.
        StringBuilder sb = new StringBuilder("[^\\s\\x{E000}-\\x{F8FF}");
        TSC_CHARS.codePoints().forEach(cp -> sb.append("\\x{").append(Integer.toHexString(cp)).append('}'));
        return sb.append("]+").toString();
    }

    /** Vízszintes eltolás {@code px} képponttal (a térköz-karakterek összege; −255…+255). */
    public static String shift(int px) {
        if (px < -255 || px > 255) throw new IllegalArgumentException("shift: " + px);
        StringBuilder sb = new StringBuilder();
        char base = px < 0 ? SHIFT_MINUS : SHIFT_PLUS;
        int abs = Math.abs(px);
        for (int bit = 7; bit >= 0; bit--) {
            if ((abs & (1 << bit)) != 0) sb.append((char) (base + bit));
        }
        return sb.toString();
    }

    /** Eltolás tetszőleges távolságra (a {@link #shift} ±255-ig megy). */
    public static String shiftAny(int px) {
        StringBuilder sb = new StringBuilder();
        while (px > 255) { sb.append(shift(255)); px -= 255; }
        while (px < -255) { sb.append(shift(-255)); px += 255; }
        return sb.append(shift(px)).toString();
    }

    /** Glyph(ek) a {@code topixity:gui} fontban, a saját színükkel (fehér szöveg-szín = nincs színezés). */
    public static Component gui(String glyphs) {
        return Component.text(glyphs).font(FONT_GUI).color(NamedTextColor.WHITE);
    }

    /** Glyph(ek) a {@code topixity:gui} fontban, szín NÉLKÜL: a környező szöveg színe színezi (fehér kép). */
    public static Component guiTinted(String glyphs) {
        return Component.text(glyphs).font(FONT_GUI);
    }

    /** Glyph(ek) a default fontban, a saját színükkel (a környező fonttól függetlenül). */
    public static Component glyph(String glyphs) {
        return Component.text(glyphs).font(FONT_DEFAULT).color(NamedTextColor.WHITE);
    }

    /** Glyph(ek) a default fontban, szín NÉLKÜL: a környező szöveg színe színezi (fehér kép). */
    public static Component glyphTinted(String glyphs) {
        return Component.text(glyphs).font(FONT_DEFAULT);
    }

    /** Benne van-e a jel a {@code tsc} fontban (a whitespace mindig igen). */
    public static boolean inTsc(int codePoint) {
        return Character.isWhitespace(codePoint) || TSC_CHARS.indexOf(codePoint) >= 0;
    }

    /** MiniMessage → Component, dőlt betű nélkül (a lore/név alapból dőlt lenne). */
    public static Component mini(String miniMessage, TagResolver... resolvers) {
        if (miniMessage == null || miniMessage.isEmpty()) return Component.empty();
        return MM.deserialize(miniMessage, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /**
     * Small caps: a gyökér a {@code minecraft:tsc} fontot kapja (ha a szöveg maga nem adott
     * meg fontot), a {@code tsc}-ből hiányzó jelek pedig a default fontot.
     */
    public static Component smallCaps(Component component) {
        Component c = component.style().font() == null ? component.font(FONT_TSC) : component;
        return c.replaceText(TSC_FALLBACK).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /** {@link #mini} + {@link #smallCaps} egy lépésben. */
    public static Component miniSmallCaps(String miniMessage, TagResolver... resolvers) {
        return smallCaps(mini(miniMessage, resolvers));
    }
}
