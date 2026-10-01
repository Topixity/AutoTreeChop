package org.milkteamc.autotreechop.design;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A v3 tooltip (user 2026-10-01, „L2"): vanilla keret, szürke small caps név (a kiválasztotté zöld pipával), alatta
 * small caps sorok — akció-sor: egér-ikon + színes szó + kis nyíl + akció.
 *
 * <pre>
 * ✔ DeBuff                     ← Lore.name("DeBuff", true, bedrock)
 * [egér] BAL → KIVÁLASZTÁS      ← Lore.action(Click.BAL, "Kiválasztás", bedrock)   vagy sablonból: "{bal} Kiválasztás"
 * [egér] JOBB → CUSTOMIZÁLÁS    ← "{jobb} Customizálás"
 * [egér] SHIFT+JOBB → RESET     ← "{shift_jobb} Reset"
 * ÁLLAPOT: ENGEDÉLYEZVE         ← "&7Állapot: &#62C98BEngedélyezve"   (adat / leírás: small caps, {ikon:név} tokenekkel)
 * </pre>
 *
 * <p>Üres sor nincs. Bedrockon a glyphek nem mennek: ott a régi „» Bal · Kiválasztás" forma jön, small caps nélkül.
 */
public final class Lore {

    /** Tooltip-glyphek a default fontban: az egér saját színű kép (fehér szín kell hozzá), a nyíl és a pipa fehér kép. */
    public static final String MOUSE_LEFT = "", MOUSE_RIGHT = "", MOUSE_SHIFT_RIGHT = "";
    public static final String ARROW = "", CHECK = "";

    static final TextColor SELECTED = TextColor.color(0x55FF55);

    /** Melyik kattintás: a sablon-token, az egér-glyph, a szó színe és a szó. */
    public enum Click {
        BAL("bal", MOUSE_LEFT, 0x06B6D4, "Bal"),
        JOBB("jobb", MOUSE_RIGHT, 0xFFFF55, "Jobb"),
        SHIFT_BAL("shift_bal", MOUSE_LEFT, 0x06B6D4, "Shift+Bal"),
        SHIFT_JOBB("shift_jobb", MOUSE_SHIFT_RIGHT, 0xF43F5E, "Shift+Jobb"),
        KATT("katt", MOUSE_LEFT, 0x06B6D4, "Katt");

        final String token, glyph, word;
        final TextColor color;

        Click(String token, String glyph, int rgb, String word) {
            this.token = token;
            this.glyph = glyph;
            this.color = TextColor.color(rgb);
            this.word = word;
        }
    }

    private static final Pattern ACTION = Pattern.compile("^\\s*\\{(bal|jobb|shift_bal|shift_jobb|katt)\\}\\s*(.*)$");

    private Lore() {}

    /** Akció-sor: {@code [egér] SZÓ → akció}. A Shift+Jobb akciója piros (törlés / reset), a többié szürke. */
    public static Component action(Click click, String action, boolean bedrock) {
        TextColor actionColor = click == Click.SHIFT_JOBB ? NamedTextColor.RED : NamedTextColor.GRAY;
        if (bedrock) {
            return noItalic(Component.textOfChildren(
                    Component.text("» ", NamedTextColor.DARK_GRAY), Component.text(click.word, click.color),
                    Component.text(" · ", NamedTextColor.DARK_GRAY), Component.text(action, actionColor)));
        }
        Component line = Component.textOfChildren(
                GuiText.glyph(click.glyph), Component.text(" " + click.word + " ", click.color),
                GuiText.glyphTinted(ARROW).color(NamedTextColor.DARK_GRAY), Component.text(" " + action, actionColor));
        return GuiText.smallCaps(line);
    }

    /**
     * A név: <b>small caps</b> (a user 2026-10-01 este: „itemnevek … small capsel legyen" — a kézben tartott item
     * neve a hotbar fölött és a tooltip első sora is ez), szürke; a kiválasztott / aktív elemé zöld, előtte pipa.
     * Bedrockon a saját font nem megy: ott sima betű.
     */
    public static Component name(String plain, boolean selected, boolean bedrock) {
        if (bedrock) {
            return noItalic(selected ? Component.text("✔ " + plain, SELECTED) : Component.text(plain, NamedTextColor.GRAY));
        }
        if (!selected) return GuiText.smallCaps(Component.text(plain, NamedTextColor.GRAY));
        return GuiText.smallCaps(Component.textOfChildren(GuiText.glyphTinted(CHECK), Component.text(" " + plain)).color(SELECTED));
    }

    /**
     * Kész (saját színű) név-komponens small capsre: ritkaság-színes, átmenetes vagy több részből álló névhez.
     * A színekhez nem nyúl; a szín nélküli rész szürke. Bedrockon sima betű.
     */
    public static Component name(Component name, boolean bedrock) {
        Component c = name.colorIfAbsent(NamedTextColor.GRAY);
        return bedrock ? noItalic(c) : GuiText.smallCaps(c);
    }

    /**
     * Egy leírás-sor a sablonból: {@code {bal} Akció} / {@code {jobb} …} / {@code {shift_jobb} …} / {@code {katt} …}
     * akció-sor; minden más small caps sor (a színkódok maradnak, az {@code {ikon:név}} tokenek sor-ikonok).
     *
     * @return a sor, vagy {@code null}, ha üres (az üres sor kimarad)
     */
    public static Component line(String template, boolean bedrock) {
        if (template == null || template.trim().isEmpty()) return null;
        Matcher m = ACTION.matcher(template);
        if (m.matches()) {
            Click click = Click.valueOf(m.group(1).toUpperCase(Locale.ROOT));
            return action(click, SysMsg.plain(m.group(2)).replaceAll("[&§][0-9a-fk-orA-FK-OR]", "").trim(), bedrock);
        }
        Component c = SysMsg.legacy(template).colorIfAbsent(NamedTextColor.GRAY);
        if (bedrock) return noItalic(Icons.strip(c));
        return GuiText.smallCaps(Icons.lineIcons(c));
    }

    /** Több sor egyszerre; az üres sorok kimaradnak. */
    public static List<Component> lines(List<String> templates, boolean bedrock) {
        List<Component> out = new ArrayList<>();
        if (templates == null) return out;
        for (String t : templates) {
            Component c = line(t, bedrock);
            if (c != null) out.add(c);
        }
        return out;
    }

    private static Component noItalic(Component c) {
        return c.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
