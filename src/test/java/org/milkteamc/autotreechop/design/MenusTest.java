package org.milkteamc.autotreechop.design;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** A menücím: slot-takarók, kijelölő keret, középre zárt felirat — minden darab oda tér vissza, ahonnan indult. */
class MenusTest {

    /** A takaró-sor „lejátszása": mely slotokat fedi le, és hol áll a végén a kurzor. */
    private static Set<Integer> covered(String glyphs, int[] endX) {
        Set<Integer> out = new HashSet<>();
        int x = 0;
        for (char ch : glyphs.toCharArray()) {
            Integer sp = FontWidths.spacing(ch);
            if (sp != null) { x += sp; continue; }
            if (ch >= '\uEAA0' && ch <= '\uEAB7') {
                int row = (ch - '\uEAA0') / 4, n = new int[]{1, 2, 4, 8}[(ch - '\uEAA0') % 4];
                int px = x + 8 - 7;                     // a GUI bal szélétől mért x − 7
                assertEquals(0, px % 18, "a takaró slot-határon kezdődik");
                for (int i = 0; i < n; i++) out.add(row * 9 + px / 18 + i);
                x += 18 * n + 1;                       // bitmap-glyph: szélesség + 1
            }
        }
        endX[0] = x;
        return out;
    }

    @Test
    void a_takaro_pontosan_a_nem_lathato_slotokat_fedi() {
        Set<Integer> visible = Set.of(11, 15);
        int[] end = new int[1];
        Set<Integer> cov = covered(Menus.cover(3, visible), end);
        Set<Integer> expected = new HashSet<>();
        for (int s = 0; s < 27; s++) if (!visible.contains(s)) expected.add(s);
        assertEquals(expected, cov);
        assertEquals(0, end[0], "a kurzor visszatér a cím kezdetére");
        assertEquals(0, FontWidths.text(Menus.cover(3, visible), GuiText.FONT_DEFAULT, false));
    }

    @Test
    void hat_soros_menu_szetszort_slotokkal() {
        Set<Integer> visible = new HashSet<>(List.of(0, 8, 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 25, 37, 38, 39, 40, 41, 42, 43,
                46, 47, 48, 49, 50, 51, 52));
        int[] end = new int[1];
        Set<Integer> cov = covered(Menus.cover(6, visible), end);
        for (int s = 0; s < 54; s++) assertEquals(!visible.contains(s), cov.contains(s), "slot " + s);
        assertEquals(0, end[0]);
    }

    @Test
    void minden_slot_lathato_ures_takaras() {
        Set<Integer> all = new HashSet<>();
        for (int s = 0; s < 27; s++) all.add(s);
        assertEquals("", Menus.cover(3, all));
        assertThrows(IllegalArgumentException.class, () -> Menus.cover(7, all));
    }

    @Test
    void a_keret_a_slot_helyere_kerul_es_visszater() {
        String f = Menus.frame(46);                                  // 5. sor, 1. oszlop
        assertTrue(f.indexOf((char) ('\uE838' + 5)) >= 0);
        assertEquals(0, FontWidths.text(f, GuiText.FONT_DEFAULT, false));
        String before = f.substring(0, f.indexOf((char) ('\uE838' + 5)));
        assertEquals(7 + 18 - 8, FontWidths.text(before, GuiText.FONT_DEFAULT, false));
        assertEquals("", Menus.frame(-1));
    }

    @Test
    void a_felirat_kozepre_zart_a_gui_fonttal() {
        TextComponent label = (TextComponent) Menus.label("Főmenü");
        assertEquals(GuiText.FONT_GUI, label.style().font());
        assertEquals(Menus.TITLE_COLOR, label.style().color());
        int text = FontWidths.text("Főmenü", GuiText.FONT_GUI, false);
        int shift = FontWidths.text(label.content(), GuiText.FONT_GUI, false) - text;
        // a szöveg bal széle: 8 + shift; a közepe a láda közepén (88) ± 1 px
        assertEquals(88.0, 8 + shift + text / 2.0, 1.0);
        assertEquals(FontWidths.text("FŐMENÜ", GuiText.FONT_GUI, false), text, "a kisbetű ugyanaz a kép");
    }

    @Test
    void tul_hosszu_cim_balrol_indul() {
        String longTitle = "Ez egy nagyon hosszú menücím, ami nem fér ki sehogy";
        TextComponent label = (TextComponent) Menus.label(longTitle);
        assertEquals(longTitle, label.content());
    }

    @Test
    void teljes_cim_takaras_keret_felirat() {
        Component t = Menus.title("Megerősítés", 3, Set.of(11, 15), List.of(11), false);
        assertEquals(2, t.children().size());
        TextComponent glyphs = (TextComponent) t.children().get(0);
        assertEquals(GuiText.FONT_DEFAULT, glyphs.style().font());
        assertEquals(0xFFFFFF, glyphs.style().color().value(), "a takaró és a keret saját színű kép");
        assertEquals(0, FontWidths.text(glyphs.content(), GuiText.FONT_DEFAULT, false));
        assertTrue(glyphs.content().indexOf((char) ('\uE838' + 1)) >= 0, "keret az 1. sorra");
    }

    @Test
    void bedrockon_sima_szoveges_cim() {
        Component t = Menus.title("Megerősítés", 3, Set.of(11, 15), List.of(11), true);
        assertEquals("Megerősítés", PlainTextComponentSerializer.plainText().serialize(t));
        assertNull(t.style().font());
        assertTrue(t.children().isEmpty());
    }
}
