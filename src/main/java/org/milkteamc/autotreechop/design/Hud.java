package org.milkteamc.autotreechop.design;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * A bossbar, a title és az actionbar egységes kinézete (user 2026-10-01).
 *
 * <ul>
 *   <li><b>Bossbar („B10"):</b> ikon + small caps felirat; a sáv tíz külön blokk (az a pack dolga: a vanilla sáv
 *       képeinek cseréje). Az ikon a {@code minecraft:default} font glyphje, fehér képpel: a szöveg színe színezi.</li>
 *   <li><b>Title („T8 + T6"):</b> sötét sáv, rajta ikon + átmenetes cím 3×-ban, alatta small caps alcím. A cím a
 *       {@code topixity:cim} fontot kapja: 0,75-szörös (a vanilla title 4×-ese így 3×), félkövérre rajzolt,
 *       függőleges átmenetes betűk. Ezért a kódból NEM kell félkövér; a szín élénkítve és enyhén világosítva megy ki.</li>
 *   <li><b>Actionbar:</b> a szöveg small capsszel, élénk színnel, háttér nélkül.</li>
 * </ul>
 *
 * <p><b>Ikon a szövegben:</b> {@code {ikon:<név>}} token (pl. {@code '&#06B6D4{ikon:ora} &fKezdésig &f%seconds%s'}); a
 * token a környező színt örökli. Ismeretlen név: a token eltűnik. Bedrockon a pack fontjai nem mennek: ott a token
 * eltűnik, a szöveg változatlan.
 */
public final class Hud {

    /** A title fontja (txPack-network {@code forras/assets/topixity/font/cim.json}). */
    public static final Key FONT_TITLE = Key.key("topixity", "cim");

    /** A régi (PVP-ből átvett) bossbar-ikonok kódjai — változatlanok; minden más név az {@link Icons} sor-ikonja. */
    static final Map<String, String> BAR_ICONS = Map.ofEntries(
            Map.entry("kard", ""), Map.entry("kupa", ""), Map.entry("csillag", ""),
            Map.entry("homok", ""), Map.entry("pulzus", ""), Map.entry("jel", ""),
            Map.entry("ember", ""), Map.entry("erme", ""), Map.entry("szem", ""),
            Map.entry("ora", ""), Map.entry("pajzs", ""), Map.entry("zona", ""),
            Map.entry("villam", ""));

    /** Title-ikonok a {@code topixity:cim} fontban (csak ezek vannak a title léptékében megrajzolva). */
    static final Map<String, String> TITLE_ICONS = Map.ofEntries(
            Map.entry("kupa", ""), Map.entry("iksz", ""), Map.entry("korok", ""),
            Map.entry("zona", ""), Map.entry("ora", ""), Map.entry("nyil", ""),
            Map.entry("stop", ""), Map.entry("kard", ""), Map.entry("felkialto", ""),
            Map.entry("egyenlo", ""), Map.entry("pajzs", ""));

    private static final Pattern TOKEN = Pattern.compile("\\{ikon:([a-z_0-9]+)\\} ?");
    /** Ami NINCS a cím-fontban (ASCII, Latin-1 / Extended-A betűk és a saját ikonok kivételével minden). */
    private static final Pattern NOT_IN_TITLE_FONT =
            Pattern.compile("[^\\s\\x{21}-\\x{7E}\\x{C0}-\\x{17F}\\x{E000}-\\x{F8FF}]+");
    private static final TextReplacementConfig TITLE_FALLBACK = TextReplacementConfig.builder()
            .match(NOT_IN_TITLE_FONT)
            .replacement((match, builder) -> builder.font(GuiText.FONT_DEFAULT))
            .build();
    /** A cím színének világosítása (a font képe lefelé 60 %-ra sötétít). */
    static final double TITLE_LIGHTEN = 0.12;

    private Hud() {}

    /** A sor-ikon glyphje: előbb a régi PVP-kódok (változatlan kimenet), utána a design-rendszer ikonjai. */
    static String barIcon(String name) {
        String old = BAR_ICONS.get(name);
        return old != null ? old : Icons.line(name);
    }

    /** Bossbar-cím: az ikon-tokenek glyphek, a szöveg small caps. */
    public static Component bossBar(Component legacy) {
        return GuiText.smallCaps(icons(vivid(legacy), Hud::barIcon, GuiText.FONT_DEFAULT, true));
    }

    /** Bossbar-cím Bedrockra: a tokenek eltűnnek, a szöveg változatlan. */
    public static Component bossBar(Component legacy, boolean bedrock) {
        return bedrock ? icons(legacy, n -> null, null, false) : bossBar(legacy);
    }

    /** Actionbar: small caps + élénkített színek + ikonok; Bedrockon a szöveg változatlan, a token eltűnik. */
    public static Component actionBar(Component message, boolean bedrock) {
        if (bedrock) return icons(message, n -> null, null, false);
        return GuiText.smallCaps(icons(vivid(message), Hud::barIcon, GuiText.FONT_DEFAULT, true));
    }

    /** Egy sáv-darab szélessége title-egységben (256 px-es kép 0,5-szörös léptékkel). */
    static final int BAND_PIECE = 128;
    /** Ennyi darab kerül egymás mellé: 512 egység = 2048 GUI képpont, a legszélesebb képernyőn is túlér. */
    static final int BAND_PIECES = 4;
    private static final String BAND_TALL = "";    // cím + alcím mögé
    private static final String BAND_SHORT = "";   // csak cím mögé

    /**
     * Title: sötét sáv a teljes szélességben, rajta ikon + a cím a {@code topixity:cim} fonttal
     * (élénkített, enyhén világosított szín, félkövér nélkül). Színes vonal NINCS a sáv tetején.
     *
     * <p>A sáv a cím ELÉ írt glyph-sor ugyanabban a fontban: a szöveg közepe alá tolva, nettó 0
     * előtolással és árnyék nélkül — így a kliens továbbra is a szöveget igazítja középre.
     *
     * @param hasSubtitle van-e alcím (a magas sáv az alcímet is takarja, az alacsony csak a címet)
     */
    public static Component title(Component legacy, boolean hasSubtitle, boolean bedrock) {
        if (bedrock) return icons(legacy, n -> null, null, false);
        legacy = vivid(legacy);
        Component c = icons(legacy, TITLE_ICONS::get, null, true);
        c = restyle(c, s -> {
            Style.Builder b = s.toBuilder().decoration(TextDecoration.BOLD, TextDecoration.State.FALSE);
            if (s.color() != null) b.color(lighten(s.color()));
            return b.build();
        });
        Component text = c.font(FONT_TITLE).replaceText(TITLE_FALLBACK);
        int width = FontWidths.width(text);
        if (width <= 0) return text;
        int total = BAND_PIECE * BAND_PIECES;
        int start = Math.floorDiv(width - total, 2);
        Component band = Compat.noShadow(Component.text(strip(start, hasSubtitle ? BAND_TALL : BAND_SHORT))
                .font(FONT_TITLE).color(NamedTextColor.WHITE));
        return Component.textOfChildren(band, text);
    }

    /** {@code start}-ra lép, kirakja a darabokat egymás mellé, és visszatér 0-ra. */
    static String strip(int start, String piece) {
        StringBuilder sb = new StringBuilder(GuiText.shiftAny(start));
        for (int i = 0; i < BAND_PIECES; i++) sb.append(piece).append(GuiText.shift(-1));
        return sb.append(GuiText.shiftAny(-(start + BAND_PIECE * BAND_PIECES))).toString();
    }

    /** Subtitle: small caps (a színek a hívótól jönnek). */
    public static Component subtitle(Component legacy, boolean bedrock) {
        if (bedrock) return icons(legacy, n -> null, null, false);
        return GuiText.smallCaps(icons(vivid(legacy), Hud::barIcon, GuiText.FONT_DEFAULT, true));
    }

    /**
     * Az {@code {ikon:név}} tokenek cseréje.
     *
     * @param font     a glyph fontja ({@code null}: a környezetét örökli)
     * @param keepGap  ha van ikon, a token utáni szóköz megmarad; ha nincs (ismeretlen név, Bedrock),
     *                 a szóközzel együtt tűnik el
     */
    static Component icons(Component c, Function<String, String> table, Key font, boolean keepGap) {
        return c.replaceText(TextReplacementConfig.builder().match(TOKEN).replacement((match, builder) -> {
            String glyph = table.apply(match.group(1));
            if (glyph == null) return Component.empty();
            boolean gap = keepGap && match.group().endsWith(" ");
            Component icon = Component.text(glyph);
            if (font != null) icon = icon.font(font);
            return gap ? Component.textOfChildren(icon, Component.text(" ")) : icon;
        }).build());
    }

    /**
     * Élénkebb szín (user 2026-10-01: „a title-nél, bossbarnál meg actionbarnál is élénkebb színeket"):
     * a fényerő teljesre megy, a meleg árnyalatok (piros, narancs, rózsaszín) telítettsége legalább 85 %.
     * A szürkék és a fehér (25 % alatti telítettség) változatlanok — a felirat és az alcím nem színeződik.
     */
    public static TextColor vivid(TextColor c) {
        double r = c.red() / 255.0, g = c.green() / 255.0, b = c.blue() / 255.0;
        double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        double sat = max == 0 ? 0 : (max - min) / max;
        if (sat < 0.25) return c;
        double d = max - min, hue;
        if (max == r) hue = ((g - b) / d + 6) % 6;
        else if (max == g) hue = (b - r) / d + 2;
        else hue = (r - g) / d + 4;
        hue *= 60;
        if (hue < 70 || hue > 300) sat = Math.max(sat, 0.85);
        double chroma = sat, x = chroma * (1 - Math.abs((hue / 60) % 2 - 1)), m = 1 - chroma;
        double[] rgb = hue < 60 ? new double[]{chroma, x, 0} : hue < 120 ? new double[]{x, chroma, 0}
                : hue < 180 ? new double[]{0, chroma, x} : hue < 240 ? new double[]{0, x, chroma}
                : hue < 300 ? new double[]{x, 0, chroma} : new double[]{chroma, 0, x};
        return TextColor.color((int) Math.round((rgb[0] + m) * 255), (int) Math.round((rgb[1] + m) * 255),
                (int) Math.round((rgb[2] + m) * 255));
    }

    /** Minden szín élénkítése a komponensen és a gyerekein. */
    public static Component vivid(Component c) {
        return restyle(c, s -> s.color() == null ? s : s.color(vivid(s.color())));
    }

    static TextColor lighten(TextColor c) {
        return TextColor.color(mix(c.red()), mix(c.green()), mix(c.blue()));
    }

    private static int mix(int v) {
        return (int) Math.round(v + (255 - v) * TITLE_LIGHTEN);
    }

    /** A stílus átírása a komponensen és minden gyerekén. */
    static Component restyle(Component c, UnaryOperator<Style> f) {
        List<Component> kids = new ArrayList<>(c.children().size());
        for (Component k : c.children()) kids.add(restyle(k, f));
        return c.style(f.apply(c.style())).children(kids);
    }
}
