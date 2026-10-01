package org.milkteamc.autotreechop.design;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A design-rendszer ikonjai (txPack-network {@code _tools/design_glyphek.py}, 7×7-es FEHÉR glyphek a
 * {@code minecraft:default} fontban: a szöveg színe színezi őket).
 *
 * <ul>
 *   <li><b>chat-ikon</b> (U+EA00 + i, ascent 6): a small caps törzsével egy vonalban áll — chat-rendszerüzenetbe;</li>
 *   <li><b>sor-ikon</b> (U+EA40 + i, ascent 7): lore, hologram, bossbar, actionbar, scoreboard.</li>
 * </ul>
 *
 * <p>Szövegben {@code {ikon:<név>}} tokenként áll (pl. {@code '&#62C98B{ikon:pipa} &7Mentve'}); a token a környező
 * színt örökli. Ismeretlen név: a token eltűnik. A sorrend a pack kódkiosztása — ÚJ ikon csak a lista VÉGÉRE kerülhet.
 */
public final class Icons {

    /** A pack {@code IKONOK} listája, sorrendben (a kód = kezdő + index). */
    static final List<String> NAMES = List.of(
            "pipa", "iksz", "homok", "figyelem", "kerdes", "plusz", "minusz", "penz", "rombusz", "boritek", "koponya", "info",
            "csillag", "csengo", "ajandek", "ora", "ember", "lakat", "sziv", "kristaly", "glob", "pajzs", "kard", "villam",
            "korona", "zaszlo", "haz", "lap", "szint", "kosar", "kulcs", "tovabb", "kupa", "erme", "szem", "csakany",
            "pulzus", "jel", "zona", "eger", "fel", "le", "jobbra", "balra", "pont", "lista", "fogas", "kuka");

    static final int CHAT_BASE = 0xEA00;
    static final int LINE_BASE = 0xEA40;
    /** Minden ikon előtolása (7 px kép + 1): a keskeny ikonok képe is 7 px szélesnek számít (alfa = 1 képpont). */
    public static final int ADVANCE = 8;

    private static final Map<String, Integer> INDEX = new HashMap<>();

    static {
        for (int i = 0; i < NAMES.size(); i++) INDEX.put(NAMES.get(i), i);
    }

    /** {@code {ikon:név}} + az utána álló egy szóköz (ha az ikon eltűnik, a szóköz is). */
    static final Pattern TOKEN = Pattern.compile("\\{ikon:([a-z_0-9]+)\\} ?");

    private Icons() {}

    /** A chat-ikon karaktere (ascent 6), vagy {@code null}, ha nincs ilyen nevű ikon. */
    public static String chat(String name) {
        Integer i = INDEX.get(name);
        return i == null ? null : String.valueOf((char) (CHAT_BASE + i));
    }

    /** A sor-ikon karaktere (ascent 7), vagy {@code null}, ha nincs ilyen nevű ikon. */
    public static String line(String name) {
        Integer i = INDEX.get(name);
        return i == null ? null : String.valueOf((char) (LINE_BASE + i));
    }

    public static boolean exists(String name) {
        return INDEX.containsKey(name);
    }

    /**
     * Az {@code {ikon:név}} tokenek cseréje a komponensben.
     *
     * @param table    név → glyph (a hívó dönti el: chat- vagy sor-ikon, esetleg saját tábla)
     * @param keepGap  ha van ikon, a token utáni szóköz megmarad; ha nincs (ismeretlen név, Bedrock üres táblával),
     *                 a szóközzel együtt tűnik el
     */
    public static Component replace(Component c, java.util.function.Function<String, String> table, boolean keepGap) {
        return c.replaceText(TextReplacementConfig.builder().match(TOKEN).replacement((match, builder) -> {
            String glyph = table.apply(match.group(1));
            if (glyph == null) return Component.empty();
            boolean gap = keepGap && match.group().endsWith(" ");
            Component icon = Component.text(glyph).font(GuiText.FONT_DEFAULT);
            return gap ? Component.textOfChildren(icon, Component.text(" ")) : icon;
        }).build());
    }

    /** Sor-ikonok (lore, hologram, bossbar): {@code {ikon:név}} → U+EA40 + i. */
    public static Component lineIcons(Component c) {
        return replace(c, Icons::line, true);
    }

    /** A tokenek eltávolítása (Bedrock-tartalék, konzol): az ikon és az utána álló szóköz is eltűnik. */
    public static Component strip(Component c) {
        return replace(c, n -> null, false);
    }

    /** A tokenek eltávolítása sima szövegből. */
    public static String strip(String s) {
        return s == null ? null : TOKEN.matcher(s).replaceAll("");
    }
}
