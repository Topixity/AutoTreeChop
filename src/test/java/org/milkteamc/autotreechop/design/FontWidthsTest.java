package org.milkteamc.autotreechop.design;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** A szélesség-mérő (erre épül a chat-csík, a title sávja és a menücím középre zárása) és az actionbar. */
class FontWidthsTest {

    @Test
    void a_szelesseg_tabla_betoltodott() {
        assertTrue(FontWidths.loadedEntries() > 3000, "txdesign-font-widths.properties: " + FontWidths.loadedEntries());
    }

    @Test
    void small_caps_szoveg_szelessege() {
        // K e r e s é s: 7 × 6 képpont a tsc fontban; félkövéren jelenként +1
        assertEquals(42, FontWidths.text("Keresés", GuiText.FONT_TSC, false));
        assertEquals(49, FontWidths.text("Keresés", GuiText.FONT_TSC, true));
        assertEquals(4, FontWidths.text(" ", GuiText.FONT_TSC, false));
        assertEquals(4 + 2, FontWidths.text("i.", GuiText.FONT_TSC, false));
    }

    @Test
    void a_tsc_bol_hianyzo_jel_az_alap_font_szelessegevel_szamol() {
        Component c = GuiText.smallCaps(LegacyComponentSerializer.legacySection().deserialize("§fKeresés §8· §f→"));
        int expected = 42 + 4 + FontWidths.text("·", GuiText.FONT_DEFAULT, false) + 4 + FontWidths.text("→", GuiText.FONT_DEFAULT, false);
        assertEquals(expected, FontWidths.width(c));
        assertEquals(2, FontWidths.text("·", GuiText.FONT_DEFAULT, false));
    }

    @Test
    void a_felkover_oroklodik_a_gyerekekre() {
        Component c = Component.text("AB").decorate(TextDecoration.BOLD).append(Component.text("C"));
        assertEquals(3 * 7, FontWidths.width(c.font(GuiText.FONT_TSC)));
    }

    @Test
    void a_pack_glyphjei_a_tablabol_jonnek() {
        assertEquals(Icons.ADVANCE, FontWidths.text(Icons.chat("pipa"), GuiText.FONT_DEFAULT, false), "chat-ikon");
        assertEquals(Icons.ADVANCE, FontWidths.text(Icons.chat("info"), GuiText.FONT_DEFAULT, false), "a keskeny ikon is 8");
        assertEquals(Icons.ADVANCE, FontWidths.text(Icons.line("erme"), GuiText.FONT_TSC, false), "a tsc a defaultra esik vissza");
        assertEquals(8, FontWidths.text("\uE851", GuiText.FONT_DEFAULT, false), "régi scoreboard-ikon");
        assertEquals(129, FontWidths.text("\uEA87", GuiText.FONT_DEFAULT, false), "128 px-es csík-darab");
        assertEquals(19, FontWidths.text("\uE838", GuiText.FONT_GUI, false), "slot-keret a gui fonton át is");
        assertEquals(19, FontWidths.text("\uEAA0", GuiText.FONT_DEFAULT, false), "1 slotos takaró");
        assertEquals(145, FontWidths.text("\uEAA3", GuiText.FONT_DEFAULT, false), "8 slotos takaró");
    }

    @Test
    void az_actionbar_small_caps_es_nincs_mogotte_lapka() {
        Component out = Hud.actionBar(LegacyComponentSerializer.legacySection().deserialize("§a🛡 §fVédelem §8· §f0:08"), false);
        assertEquals(GuiText.FONT_TSC, out.style().font(), "a szöveg small caps");
        String plain = PlainTextComponentSerializer.plainText().serialize(out);
        assertEquals("🛡 Védelem · 0:08", plain, "se lapka-glyph, se térköz-karakter nincs a szövegben");
    }

    @Test
    void az_ikon_token_az_actionbarban_is_mukodik() {
        String plain = PlainTextComponentSerializer.plainText().serialize(
                Hud.actionBar(LegacyComponentSerializer.legacySection().deserialize("§b{ikon:villam} §fKeresés"), false));
        assertEquals("\uE85F Keresés", plain, "a régi PVP-kód változatlan");
        plain = PlainTextComponentSerializer.plainText().serialize(
                Hud.actionBar(LegacyComponentSerializer.legacySection().deserialize("§b{ikon:haz} §fOtthon"), false));
        assertEquals(Icons.line("haz") + " Otthon", plain, "új ikon: a design-rendszer sor-ikonja");
    }

    @Test
    void bedrockon_a_szoveg_valtozatlan() {
        Component msg = Component.text("Keresés");
        assertEquals(msg, Hud.actionBar(msg, true));
    }

    @Test
    void tavoli_eltolas_tobb_darabbol() {
        assertEquals(-600, FontWidths.text(GuiText.shiftAny(-600), GuiText.FONT_GUI, false));
        assertEquals(300, FontWidths.text(GuiText.shiftAny(300), GuiText.FONT_DEFAULT, false));
        assertEquals(0, FontWidths.text(GuiText.shiftAny(0), GuiText.FONT_GUI, false));
    }
}
