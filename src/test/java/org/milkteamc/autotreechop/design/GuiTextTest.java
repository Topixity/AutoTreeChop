package org.milkteamc.autotreechop.design;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Small caps: minden jel vagy a {@code tsc}-ben van, vagy default fonttal megy (különben a
 * kliens hiányzó-jel dobozt rajzolna); a dőlt betű mindenhol ki van kapcsolva.
 */
class GuiTextTest {

    /** Egy kirajzolt darab: a szöveg és a ténylegesen érvényes (örökölt) stílus. */
    private record Piece(String text, Style style) {}

    private static List<Piece> flatten(Component c) {
        List<Piece> out = new ArrayList<>();
        walk(c, Style.empty(), out);
        return out;
    }

    private static void walk(Component c, Style parent, List<Piece> out) {
        Style eff = c.style().merge(parent, Style.Merge.Strategy.IF_ABSENT_ON_TARGET);
        if (c instanceof TextComponent t && !t.content().isEmpty()) out.add(new Piece(t.content(), eff));
        for (Component child : c.children()) walk(child, eff, out);
    }

    @Test
    void a_tsc_bol_hianyzo_jel_default_fonttal_megy() {
        Component c = GuiText.miniSmallCaps("<#06B6D4>✔ <kit>", Placeholder.unparsed("kit", "Íjász"));
        List<Piece> pieces = flatten(c);
        StringBuilder all = new StringBuilder();
        for (Piece p : pieces) {
            all.append(p.text());
            Key expected = p.text().codePoints().allMatch(GuiText::inTsc) ? GuiText.FONT_TSC : GuiText.FONT_DEFAULT;
            assertEquals(expected, p.style().font(), "rossz font: '" + p.text() + "'");
            assertEquals(TextColor.color(0x06B6D4), p.style().color(), "a szín öröklődjön: '" + p.text() + "'");
            assertEquals(TextDecoration.State.FALSE, p.style().decoration(TextDecoration.ITALIC));
        }
        assertEquals("✔ Íjász", all.toString());
        assertTrue(pieces.stream().anyMatch(p -> p.text().contains("✔") && GuiText.FONT_DEFAULT.equals(p.style().font())));
    }

    @Test
    void kozepso_pont_is_default_fonttal() {
        List<Piece> pieces = flatten(GuiText.miniSmallCaps("Pvp · Kitek"));
        for (Piece p : pieces) {
            if (p.text().contains("·")) assertEquals(GuiText.FONT_DEFAULT, p.style().font());
            else assertEquals(GuiText.FONT_TSC, p.style().font());
        }
    }

    @Test
    void tsc_karakterkeszlet_ekezetek_es_irasjelek() {
        for (int cp : "Bal katt kiválaszt Shift + jobb ŐŰ 0-9 !?".codePoints().toArray()) {
            assertTrue(GuiText.inTsc(cp), "hiányzik: " + new String(Character.toChars(cp)));
        }
        assertFalse(GuiText.inTsc('·'));
        assertFalse(GuiText.inTsc('✔'));
    }

    @Test
    void a_pack_glyph_megtartja_a_sajat_fontjat_small_caps_sorban() {
        Component line = GuiText.miniSmallCaps("<eger> <yellow>Jobb</yellow> <dark_gray><nyil></dark_gray> <gray>Reset",
                Placeholder.component("eger", GuiText.gui("\uE834")),
                Placeholder.component("nyil", GuiText.guiTinted("\uE836")));
        boolean eger = false, nyil = false;
        for (Piece p : flatten(line)) {
            if (p.text().contains("\uE834")) {
                eger = true;
                assertEquals(GuiText.FONT_GUI, p.style().font());
                assertEquals(TextColor.color(0xFFFFFF), p.style().color(), "a saját színű kép fehér szöveg-színt kap");
            } else if (p.text().contains("\uE836")) {
                nyil = true;
                assertEquals(GuiText.FONT_GUI, p.style().font());
                assertEquals(TextColor.color(0x555555), p.style().color(), "a fehér képet a környező szín színezi");
            } else if (!p.text().isBlank()) {
                assertEquals(GuiText.FONT_TSC, p.style().font(), "a szöveg small caps: " + p.text());
            }
        }
        assertTrue(eger && nyil);
    }

    @Test
    void shift_a_terkoz_karakterek_osszege() {
        assertEquals("", GuiText.shift(0));
        assertEquals("\uE840", GuiText.shift(1));
        assertEquals("\uE848", GuiText.shift(-1));
        assertEquals("\uE845\uE844\uE843", GuiText.shift(56));          // 32 + 16 + 8
        assertEquals("\uE84F\uE84D\uE84C\uE848", GuiText.shift(-177)); // −128 −32 −16 −1
        assertThrows(IllegalArgumentException.class, () -> GuiText.shift(256));
    }
}
