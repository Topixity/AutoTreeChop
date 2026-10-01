package org.milkteamc.autotreechop.design;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** A chat-rendszerüzenet: típus-token → ikon + small caps + érték-szín + pontos szélességű csík. */
class SysMsgTest {

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
    void token_nelkuli_sor_nem_rendszeruzenet() {
        assertNull(SysMsg.format("&7Sima sor", false));
        assertNull(SysMsg.format("{player} belépett", false), "idegen {…} helyőrző nem típus");
        assertFalse(SysMsg.isSysMsg("&8» &fszia"));
        assertTrue(SysMsg.isSysMsg("{siker} Mentve"));
        assertTrue(SysMsg.isSysMsg("&7{hiba} Nincs jogod"), "a token előtti színkód belefér");
    }

    @Test
    void ikon_torzs_es_ertek_szin() {
        Component c = SysMsg.format("{siker} Mentve: {e}Harcos kit{/e}!", false);
        assertNotNull(c);
        assertEquals("Mentve: Harcos kit!", SysMsg.visibleText(c).trim());
        boolean icon = false, body = false, value = false;
        for (Piece p : flatten(c)) {
            if (p.text().contains(Icons.chat("pipa"))) {
                icon = true;
                assertEquals(SysMsg.Type.SIKER.color, p.style().color());
                assertEquals(GuiText.FONT_DEFAULT, p.style().font());
            } else if (p.text().contains("Mentve")) {
                body = true;
                assertEquals(SysMsg.BODY, p.style().color());
                assertEquals(GuiText.FONT_TSC, p.style().font());
            } else if (p.text().contains("Harcos")) {
                value = true;
                assertEquals(SysMsg.Type.SIKER.color, p.style().color(), "az érték a típus színével");
                assertEquals(GuiText.FONT_TSC, p.style().font());
            }
        }
        assertTrue(icon && body && value);
    }

    @Test
    void a_csik_netto_elotolasa_nulla_es_pontosan_a_tartalom_ala_er() {
        for (String line : new String[]{"{hiba} Nincs jogod ehhez.", "{var} Várj még {e}5 mp{/e}-et.", "{info} A",
                "{penz} Kaptál {e}500 ${/e}-t · egyenleg {e}12 950 ${/e}"}) {
            Component c = SysMsg.format(line, false);
            assertEquals(2, c.children().size(), "csík + tartalom: " + line);
            TextComponent band = (TextComponent) c.children().get(0);
            int content = FontWidths.width(c.children().get(1));
            assertEquals(0, FontWidths.text(band.content(), GuiText.FONT_DEFAULT, false), "nettó előtolás: " + line);
            // a csík darabjai: 2 vég (1 px) + a törzs; a teljes szélesség = tartalom + 2 × PAD
            int covered = 0;
            for (char ch : band.content().toCharArray()) {
                if (ch >= '\uEA80' && ch <= '\uEA87') covered += 1 << (ch - '\uEA80');
                else if (ch == '\uEA88') covered += 1;
            }
            assertEquals(content + 2 * SysMsg.PAD, covered, "a csík szélessége: " + line);
            assertEquals(content, FontWidths.width(c), "az egész sor szélessége = a tartalomé");
            assertEquals(SysMsg.STRIP, band.style().color());
        }
    }

    @Test
    void kiemelt_uzenet_a_tipus_fele_szinezett_csikkal_es_ellel() {
        Component c = SysMsg.format("{!privat} {e}Seda{/e} → te {sima}&8» &fjössz ma koth-ra?", false);
        Component strip = c.children().get(0);
        assertEquals(2, strip.children().size(), "sáv + él");
        TextComponent band = (TextComponent) strip.children().get(0);
        TextComponent edge = (TextComponent) strip.children().get(1);
        assertTrue(band.content().indexOf('\uEA91') >= 0, "a kiemelt csík darabjai (U+EA89…)");
        assertEquals(SysMsg.mix(SysMsg.STRIP, SysMsg.Type.PRIVAT.color, 0.30), band.style().color());
        assertTrue(edge.content().indexOf('\uEA92') >= 0);
        assertEquals(SysMsg.Type.PRIVAT.color, edge.style().color());
        assertEquals(0, FontWidths.text(band.content(), GuiText.FONT_DEFAULT, false));
        assertEquals(0, FontWidths.text(edge.content(), GuiText.FONT_DEFAULT, false));
        // a {sima} utáni rész nem small caps
        boolean plain = false;
        for (Piece p : flatten(c)) {
            if (p.text().contains("jössz")) {
                plain = true;
                assertNotEquals(GuiText.FONT_TSC, p.style().font(), "a játékos üzenete sima betű");
                assertEquals(TextColor.color(0xFFFFFF), p.style().color());
            }
        }
        assertTrue(plain);
    }

    @Test
    void mas_ikon_a_tipus_szinevel() {
        Component c = SysMsg.format("{info:haz} Otthon beállítva", false);
        assertTrue(flatten(c).stream().anyMatch(p -> p.text().contains(Icons.chat("haz"))
                && SysMsg.Type.INFO.color.equals(p.style().color())));
        Component unknown = SysMsg.format("{info:nincsilyen} Szöveg", false);
        assertTrue(flatten(unknown).stream().anyMatch(p -> p.text().contains(Icons.chat("info"))), "ismeretlen ikon: a típusé");
    }

    @Test
    void tul_szeles_sor_csik_nelkul_megy() {
        String hosszu = "{info} " + "Ez egy nagyon hosszú sor, ami nem fér ki. ".repeat(4);
        Component c = SysMsg.format(hosszu, false);
        assertTrue(FontWidths.width(c) > SysMsg.MAX_STRIP_WIDTH);
        assertFalse(flatten(c).stream().anyMatch(p -> p.text().indexOf('\uEA80') >= 0 || p.text().indexOf('\uEA88') >= 0));
    }

    @Test
    void bedrockon_ikon_es_sima_szoveg_csik_es_font_nelkul() {
        Component c = SysMsg.format("{figyelem} Az aréna {e}30 mp{/e} múlva zár.", true);
        for (Piece p : flatten(c)) {
            assertFalse(p.text().chars().anyMatch(ch -> ch >= 0xE840 && ch <= 0xE84F), "nincs térköz");
            assertFalse(p.text().chars().anyMatch(ch -> ch >= 0xEA80 && ch <= 0xEA92), "nincs csík");
            if (!p.text().contains(Icons.chat("figyelem"))) assertNull(p.style().font(), "nincs saját font: " + p.text());
        }
        assertEquals("Az aréna 30 mp múlva zár.", SysMsg.visibleText(c).trim());
    }

    @Test
    void szekcio_jeles_bemenet_es_regi_szinkod_is_megy() {
        Component c = SysMsg.format("{harc} Megölted: §c%victim%§r · &#E8C34A5 kill", false);
        assertEquals("Megölted: %victim% · 5 kill", SysMsg.visibleText(c).trim());
        assertTrue(flatten(c).stream().anyMatch(p -> p.text().contains("5 kill") && TextColor.color(0xE8C34A).equals(p.style().color())));
        assertEquals("&#FF0000x", SysMsg.sectionToAmp("§x§F§F§0§0§0§0x"));
    }

    @Test
    void konzolra_tokenek_nelkul() {
        assertEquals("Mentve: Harcos kit", SysMsg.plain("{siker} Mentve: {e}Harcos kit{/e}"));
        assertEquals("%player% belépett {valami}", SysMsg.plain("{be} %player% belépett {valami}"), "idegen helyőrző marad");
        assertEquals("Kezdés", SysMsg.plain("{info} {ikon:ora} Kezdés"));
    }

    @Test
    void kesz_komponensbol_is_megy() {
        Component c = SysMsg.of(SysMsg.Type.FIGYELEM, Component.text("Meghívó: ").append(Component.text("[Elfogad]")), false);
        assertEquals("Meghívó: [Elfogad]", SysMsg.visibleText(c).trim());
        assertEquals(2, c.children().size());
    }
}
