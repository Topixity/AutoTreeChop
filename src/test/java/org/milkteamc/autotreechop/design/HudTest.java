package org.milkteamc.autotreechop.design;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** A bossbar és a title kinézete: ikon-tokenek, fontok, világosított szín, félkövér nélkül. */
class HudTest {

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

    private static Component legacy(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(s);
    }

    private static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    @Test
    void bossbar_az_ikon_default_fontu_glyph_a_kornyezo_szinnel_a_szoveg_small_caps() {
        Component c = Hud.bossBar(legacy("§x§0§6§B§6§D§4{ikon:ora} §fKezdésig §f5s"));
        assertEquals("\uE85C Kezdésig 5s", plain(c));
        boolean icon = false;
        for (Piece p : flatten(c)) {
            if (p.text().contains("\uE85C")) {
                icon = true;
                assertEquals(GuiText.FONT_DEFAULT, p.style().font());
                assertEquals(Hud.vivid(TextColor.color(0x06B6D4)), p.style().color(), "az ikon a környező (élénkített) színt örökli");
            } else if (!p.text().isBlank()) {
                assertEquals(GuiText.FONT_TSC, p.style().font(), "small caps: " + p.text());
            }
        }
        assertTrue(icon);
    }

    @Test
    void ismeretlen_ikon_a_szokozzel_egyutt_eltunik() {
        assertEquals("Kezdésig", plain(Hud.bossBar(legacy("{ikon:nincsilyen} Kezdésig"))));
    }

    /** A title második gyereke a szöveg (az első a sáv; színes vonal nincs). */
    private static Component titleText(Component title) {
        assertEquals(2, title.children().size(), "sáv + szöveg");
        return title.children().get(1);
    }

    @Test
    void title_cim_font_vilagositott_szin_nincs_felkover() {
        Component text = titleText(Hud.title(legacy("§x§1§0§B§9§8§1{ikon:kupa} §lGYŐZELEM"), true, false));
        assertEquals("\uE870 GYŐZELEM", plain(text));
        for (Piece p : flatten(text)) {
            if (p.text().isBlank()) continue;
            assertEquals(Hud.FONT_TITLE, p.style().font(), "cím-font: " + p.text());
            assertNotEquals(TextDecoration.State.TRUE, p.style().decoration(TextDecoration.BOLD), "nincs félkövér: " + p.text());
            assertEquals(Hud.lighten(Hud.vivid(TextColor.color(0x10B981))), p.style().color(), "élénkített, enyhén világosított szín: " + p.text());
        }
    }

    @Test
    void title_a_sav_netto_elotolasa_nulla_a_szoveg_ala_er_es_nincs_szines_vonal() {
        Component title = Hud.title(legacy("§x§1§0§B§9§8§1{ikon:kupa} §lGYŐZELEM"), true, false);
        TextComponent band = (TextComponent) title.children().get(0);
        int width = FontWidths.width(titleText(title));
        assertTrue(width > 30 && width < 80, "a cím szélessége title-egységben: " + width);
        assertEquals(Hud.FONT_TITLE, band.style().font());
        assertEquals(net.kyori.adventure.text.format.ShadowColor.none(), band.style().shadowColor(), "árnyék nélkül");
        assertEquals(0, FontWidths.text(band.content(), Hud.FONT_TITLE, false), "nettó előtolás");
        assertEquals(4, band.content().chars().filter(ch -> ch == '\uE880').count(), "négy magas sáv-darab (van alcím)");
        assertFalse(plain(title).contains("\uE882"), "nincs színes vonal-glyph");
        // a sáv a szöveg közepe alá kerül: a kezdete (width − 512) / 2
        String first = band.content().substring(0, band.content().indexOf('\uE880'));
        assertEquals(Math.floorDiv(width - 512, 2), FontWidths.text(first, Hud.FONT_TITLE, false));
        assertEquals(width, FontWidths.width(title), "az egész sor szélessége = a szövegé");
    }

    @Test
    void alcim_nelkul_az_alacsony_sav_megy() {
        TextComponent band = (TextComponent) Hud.title(legacy("§cNE SPAMELJ"), false, false).children().get(0);
        assertEquals(4, band.content().chars().filter(ch -> ch == '\uE881').count());
        assertEquals(0, band.content().chars().filter(ch -> ch == '\uE880').count());
    }

    @Test
    void title_a_fontbol_hianyzo_jel_default_fonttal_megy() {
        Component c = Hud.title(legacy("§cA → B"), true, false);
        boolean arrow = false;
        for (Piece p : flatten(c)) {
            if (p.text().contains("→")) { arrow = true; assertEquals(GuiText.FONT_DEFAULT, p.style().font()); }
        }
        assertTrue(arrow);
    }

    @Test
    void bedrockon_a_token_eltunik_a_szoveg_valtozatlan() {
        Component in = legacy("§a{ikon:kupa} §lGYŐZELEM");
        Component out = Hud.title(in, true, true);
        assertEquals("GYŐZELEM", plain(out));
        for (Piece p : flatten(out)) assertNull(p.style().font());
        assertEquals("gg", plain(Hud.subtitle(legacy("§7gg"), true)));
    }

    @Test
    void subtitle_small_caps() {
        for (Piece p : flatten(Hud.subtitle(legacy("§7gg · jól játszottál"), false))) {
            if (p.text().contains("·")) assertEquals(GuiText.FONT_DEFAULT, p.style().font());
            else if (!p.text().isBlank()) assertEquals(GuiText.FONT_TSC, p.style().font());
        }
    }

    @Test
    void elenkites_teljes_fenyero_a_szurke_valtozatlan() {
        assertEquals(TextColor.color(0x07DBFF), Hud.vivid(TextColor.color(0x06B6D4)), "cián: csak a fényerő nő");
        assertEquals(TextColor.color(0x16FFB2), Hud.vivid(TextColor.color(0x10B981)), "zöld");
        TextColor red = Hud.vivid(TextColor.color(0xF43F5E));
        assertEquals(255, red.red());
        assertTrue(red.green() < 0x3F, "a piros telítettebb lesz: " + red.asHexString());
        for (int gray : new int[]{0xFFFFFF, 0xD6D6D6, 0xA1A1AA, 0x52525B, 0x000000}) {
            assertEquals(TextColor.color(gray), Hud.vivid(TextColor.color(gray)), "szürke marad: " + Integer.toHexString(gray));
        }
    }
}
