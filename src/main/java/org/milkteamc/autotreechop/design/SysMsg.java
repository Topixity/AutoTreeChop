package org.milkteamc.autotreechop.design;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chat-rendszerüzenet a v3 „kártya" kinézettel (user 2026-09-30 / 10-01, {@code topixity-message-design/v3}):
 *
 * <pre>[csík ~68 %][ikon 7×7] SMALL CAPS MONDAT ÉRTÉK</pre>
 *
 * <p>Az üzenet sablonja sima szöveg marad; a sor ELEJÉN egy típus-token mondja meg az ikont és a színt:
 *
 * <pre>
 *   '{siker} Mentve: {e}%kit%'                       → pipa, zöld; az {e} utáni rész a típus színével („érték")
 *   '{var} Várj még {e}%sec% mp{/e}-et.'             → {/e} (vagy {n}) vissza a törzsszöveg színére
 *   '{!privat} {e}%sender%{/e} → te {sima}&8» &f%msg%' → kiemelt csík; a {sima} utáni rész NEM small caps
 *   '{info:haz} Otthon beállítva'                    → a típus színe, másik ikonnal
 * </pre>
 *
 * <p>Típusok: {@code siker hiba var figyelem hasznalat be ki penz tp privat harc info}. A sablonban a régi színkódok
 * ({@code &a}, {@code &#RRGGBB}, {@code §}) továbbra is működnek — pl. csapatszínhez. Token nélküli sor: a formázó
 * {@code null}-t ad, a hívó a régi módon küldi (a játékos-chat nem kap csíkot).
 *
 * <p><b>Bedrock:</b> a negatív térköz és a saját font ott nem megy: ikon + sima szöveg, csík nélkül.
 * <b>Túl hosszú sor</b> ({@value #MAX_STRIP_WIDTH} px fölött): csík nélkül megy ki (a kliens tördelné).
 */
public final class SysMsg {

    /** A törzsszöveg színe (TXT2). */
    public static final TextColor BODY = TextColor.color(0xC0C2B8);
    /** A csík színe. */
    static final TextColor STRIP = TextColor.color(0x202020);
    /** Efölött nincs csík: a chat alapból 320 px széles, a csík nem törhet sort. */
    static final int MAX_STRIP_WIDTH = 300;
    /** Az ikon után ennyi térköz (az ikon előtolása 8 → a szöveg az ikon bal szélétől 11 px-re indul). */
    static final int ICON_GAP = 3;
    /** A csík ennyivel lóg túl a tartalmon balra és jobbra. */
    static final int PAD = 3;

    /** A csík darabjai a default fontban: U+EA80+i = 2^i px (68 %), U+EA88 = lekerekített vég; kiemelt: U+EA89+i, U+EA91. */
    private static final char STRIP_BASE = '\uEA80';
    private static final char STRIP_HL_BASE = '\uEA89';
    private static final char STRIP_EDGE = '\uEA92';

    /** Az üzenet-típusok: token, ikon, szín. */
    public enum Type {
        SIKER("siker", "pipa", 0x62C98B),
        HIBA("hiba", "iksz", 0xE5534B),
        VAR("var", "homok", 0xE8914A),
        FIGYELEM("figyelem", "figyelem", 0xE8C34A),
        HASZNALAT("hasznalat", "kerdes", 0x5AA9E6),
        BE("be", "plusz", 0x62C98B),
        KI("ki", "minusz", 0xE5534B),
        PENZ("penz", "penz", 0xE8C34A),
        TP("tp", "rombusz", 0x5AA9E6),
        PRIVAT("privat", "boritek", 0xF5A524),
        HARC("harc", "koponya", 0xE5534B),
        INFO("info", "info", 0x5AA9E6);

        public final String token;
        public final String icon;
        public final TextColor color;

        Type(String token, String icon, int rgb) {
            this.token = token;
            this.icon = icon;
            this.color = TextColor.color(rgb);
        }
    }

    private static final Map<String, Type> TYPES = new HashMap<>();

    static {
        for (Type t : Type.values()) TYPES.put(t.token, t);
    }

    /** {@code {tipus}}, {@code {!tipus}} (kiemelt), {@code {tipus:ikon}} a sor elején (előtte legfeljebb színkódok). */
    private static final Pattern HEAD = Pattern.compile(
            "^((?:[&§](?:#[0-9A-Fa-f]{6}|[0-9A-Fa-fK-ORk-or]))*)\\{(!?)([a-z]+)(?::([a-z_0-9]+))?\\} ?");
    /** Minden saját token (a sima szöveghez): a típus-tokenek és az {e} {/e} {n} {sima} jelölők. */
    private static final Pattern ANY_TOKEN = Pattern.compile(
            "\\{!?(?:siker|hiba|var|figyelem|hasznalat|be|ki|penz|tp|privat|harc|info)(?::[a-z_0-9]+)?\\} ?"
            + "|\\{/e\\}|\\{e\\}|\\{n\\}|\\{sima\\}");
    private static final LegacyComponentSerializer AMP = LegacyComponentSerializer.builder()
            .character('&').hexColors().build();

    private SysMsg() {}

    /** Van-e a sor elején típus-token (azaz rendszerüzenet-e). */
    public static boolean isSysMsg(String line) {
        if (line == null) return false;
        Matcher m = HEAD.matcher(line);
        return m.find() && TYPES.containsKey(m.group(3));
    }

    /**
     * A sor formázása. A {@code line}-ban a helyőrzők már be vannak helyettesítve; a színkód lehet {@code &} vagy
     * {@code §}.
     *
     * @return a kész komponens, vagy {@code null}, ha a sor elején nincs típus-token (a hívó küldje a régi módon)
     */
    public static Component format(String line, boolean bedrock) {
        if (line == null) return null;
        Matcher m = HEAD.matcher(line);
        if (!m.find()) return null;
        Type type = TYPES.get(m.group(3));
        if (type == null) return null;
        boolean highlight = !m.group(2).isEmpty();
        String iconName = m.group(4) != null && Icons.exists(m.group(4)) ? m.group(4) : type.icon;
        String rest = line.substring(m.end());

        // {sima}: innentől nem small caps (pl. a játékos által írt privát üzenet)
        String plainPart = null;
        int cut = rest.indexOf("{sima}");
        if (cut >= 0) {
            plainPart = rest.substring(cut + "{sima}".length());
            rest = rest.substring(0, cut);
        }
        String hex = hex(type.color), body = hex(BODY);
        Component text = legacy(body + rest.replace("{e}", hex).replace("{/e}", body).replace("{n}", body));
        Component plain = plainPart == null ? null : legacy("&f" + plainPart);
        return build(type, iconName, text, plain, highlight, bedrock);
    }

    /**
     * Rendszerüzenet kész komponensből (pl. kattintható gombokkal): a {@code body} small capset kap, a színei
     * maradnak (a szín nélküli rész a törzsszöveg színét kapja).
     */
    public static Component of(Type type, Component body, boolean bedrock) {
        return build(type, type.icon, body.colorIfAbsent(BODY), null, false, bedrock);
    }

    /** Mint az {@link #of(Type, Component, boolean)}, saját ikonnal és kiemeléssel. */
    public static Component of(Type type, String iconName, Component body, boolean highlight, boolean bedrock) {
        return build(type, Icons.exists(iconName) ? iconName : type.icon, body.colorIfAbsent(BODY), null, highlight, bedrock);
    }

    private static Component build(Type type, String iconName, Component text, Component plain, boolean highlight,
                                   boolean bedrock) {
        text = Icons.replace(text, Icons::chat, true);
        Component icon = Component.text(Icons.chat(iconName)).font(GuiText.FONT_DEFAULT).color(type.color);
        if (bedrock) {
            Component out = Component.textOfChildren(icon, Component.text(" "), text);
            return noItalic(plain == null ? out : Component.textOfChildren(out, plain));
        }
        Component gap = Component.text(GuiText.shift(ICON_GAP)).font(GuiText.FONT_DEFAULT);
        Component content = Component.textOfChildren(icon, gap, GuiText.smallCaps(text));
        if (plain != null) content = Component.textOfChildren(content, plain);
        content = noItalic(content);
        int width = FontWidths.width(content);
        if (width <= 0 || width + 2 * PAD > MAX_STRIP_WIDTH) return content;
        return Component.textOfChildren(strip(width + 2 * PAD, type.color, highlight), content);
    }

    /** {@link #format}, de token nélküli sornál a sima (legacy) komponenst adja — közvetlenül küldhető. */
    public static Component formatOrLegacy(String line, boolean bedrock) {
        Component c = format(line, bedrock);
        return c != null ? c : legacy(line == null ? "" : line);
    }

    /** Konzolra / naplóba: a tokenek nélkül, sima szövegként (a színkódok maradnak). */
    public static String plain(String line) {
        return line == null ? "" : Icons.strip(ANY_TOKEN.matcher(line).replaceAll(""));
    }

    /**
     * A csík: {@code −PAD}-ra lép, kirakja a {@code total} széles sávot (lekerekített végekkel), és visszatér 0-ra —
     * nettó 0 előtolás, árnyék nélkül, így a szöveg helye nem változik. Kiemeltnél a szín 30 %-ban a típusé, és a bal
     * szélen 2 px-es él áll a típus színével.
     */
    static Component strip(int total, TextColor typeColor, boolean highlight) {
        char base = highlight ? STRIP_HL_BASE : STRIP_BASE;
        StringBuilder sb = new StringBuilder(GuiText.shift(-PAD));
        sb.append((char) (base + 8)).append(GuiText.shift(-1));          // bal vég (1 px)
        int body = total - 2;
        while (body >= 128) { sb.append((char) (base + 7)).append(GuiText.shift(-1)); body -= 128; }
        for (int bit = 6; bit >= 0; bit--) {
            if ((body & (1 << bit)) != 0) sb.append((char) (base + bit)).append(GuiText.shift(-1));
        }
        sb.append((char) (base + 8)).append(GuiText.shift(-1));          // jobb vég (1 px)
        sb.append(GuiText.shiftAny(-(total - PAD)));
        TextColor color = highlight ? mix(STRIP, typeColor, 0.30) : STRIP;
        Component band = Compat.noShadow(Component.text(sb.toString()).font(GuiText.FONT_DEFAULT).color(color));
        if (!highlight) return band;
        Component edge = Compat.noShadow(Component.text(GuiText.shift(-PAD) + STRIP_EDGE + GuiText.shift(-3 + PAD))
                .font(GuiText.FONT_DEFAULT).color(typeColor));
        return Component.textOfChildren(band, edge);
    }

    static TextColor mix(TextColor a, TextColor b, double t) {
        return TextColor.color((int) Math.round(a.red() + (b.red() - a.red()) * t),
                (int) Math.round(a.green() + (b.green() - a.green()) * t),
                (int) Math.round(a.blue() + (b.blue() - a.blue()) * t));
    }

    private static String hex(TextColor c) {
        return "&#" + String.format(Locale.ROOT, "%06X", c.value());
    }

    /** {@code &} és {@code §} kódos szöveg → komponens (hex is: {@code &#RRGGBB}, {@code §x§R§R…}). */
    static Component legacy(String s) {
        if (s.indexOf('§') >= 0) s = sectionToAmp(s);
        return AMP.deserialize(s);
    }

    /** A {@code §} kódok {@code &} alakra (a {@code §x§R§R§G§G§B§B} hexet {@code &#RRGGBB}-re). */
    static String sectionToAmp(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '§' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                if ((n == 'x' || n == 'X') && i + 13 < s.length() + 0 && isHexRun(s, i + 2)) {
                    sb.append("&#");
                    for (int k = 0; k < 6; k++) sb.append(s.charAt(i + 3 + 2 * k));
                    i += 13;
                    continue;
                }
                sb.append('&');
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static boolean isHexRun(String s, int from) {
        if (from + 12 > s.length()) return false;
        for (int k = 0; k < 6; k++) {
            if (s.charAt(from + 2 * k) != '§') return false;
            if (Character.digit(s.charAt(from + 2 * k + 1), 16) < 0) return false;
        }
        return true;
    }

    private static Component noItalic(Component c) {
        return c.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /** Csak teszthez: a komponens sima szövege a PUA-glyphek nélkül. */
    static String visibleText(Component c) {
        StringBuilder sb = new StringBuilder();
        collect(c, sb);
        return sb.toString().replaceAll("[\\x{E000}-\\x{F8FF}]", "");
    }

    private static void collect(Component c, StringBuilder sb) {
        if (c instanceof TextComponent) sb.append(((TextComponent) c).content());
        for (Component k : c.children()) collect(k, sb);
    }

    /** A csík nélküli fehér szín (a glyph saját színéhez) — a hívóknak kényelmi állandó. */
    public static final TextColor WHITE = NamedTextColor.WHITE;
}
