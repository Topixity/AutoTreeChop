package org.milkteamc.autotreechop.design;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** A v3 tooltip: akció-sorok, név, adat-sorok; Bedrockon a régi szöveges forma. */
class LoreTest {

    private static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    @Test
    void akcio_sor_eger_szo_nyil_akcio() {
        Component c = Lore.line("{bal} Kiválasztás", false);
        assertEquals(Lore.MOUSE_LEFT + " Bal " + Lore.ARROW + " Kiválasztás", plain(c));
        assertEquals(TextDecoration.State.FALSE, c.style().decoration(TextDecoration.ITALIC));
        assertEquals(GuiText.FONT_TSC, c.style().font());
        assertEquals(Lore.MOUSE_RIGHT + " Jobb " + Lore.ARROW + " Szerkesztés", plain(Lore.line("{jobb} Szerkesztés", false)));
        assertEquals(Lore.MOUSE_SHIFT_RIGHT + " Shift+Jobb " + Lore.ARROW + " Reset", plain(Lore.line("{shift_jobb} Reset", false)));
        assertEquals(Lore.MOUSE_LEFT + " Katt " + Lore.ARROW + " Megnyitás", plain(Lore.line("{katt} &eMegnyitás", false)), "a színkód az akcióból lekerül");
    }

    @Test
    void bedrockon_a_regi_szoveges_forma() {
        assertEquals("» Bal · Kiválasztás", plain(Lore.line("{bal} Kiválasztás", true)));
        assertEquals("✔ DeBuff", plain(Lore.name("DeBuff", true, true)));
        assertEquals("Ár 1 250", plain(Lore.line("&7Ár &f1 250 {ikon:erme}", true)).trim());
    }

    @Test
    void nev_small_caps_szurke_a_kivalasztott_zold_pipaval() {
        Component n = Lore.name("Elytra", false, false);
        assertEquals("Elytra", plain(n));
        assertEquals(NamedTextColor.GRAY, n.color());
        assertEquals(GuiText.FONT_TSC, n.style().font(), "az item-név small caps");
        assertEquals(TextDecoration.State.FALSE, n.style().decoration(TextDecoration.ITALIC));
        Component s = Lore.name("DeBuff", true, false);
        assertEquals(Lore.CHECK + " DeBuff", plain(s));
        assertEquals(Lore.SELECTED, s.color());
        assertEquals(GuiText.FONT_TSC, s.style().font());
        assertEquals(GuiText.FONT_DEFAULT, s.children().get(0).style().font(), "a pipa a default font glyphje");
    }

    @Test
    void nev_bedrockon_sima_betu() {
        Component n = Lore.name("Elytra", false, true);
        assertNull(n.style().font());
        assertEquals("✔ DeBuff", plain(Lore.name("DeBuff", true, true)));
    }

    @Test
    void kesz_nev_komponens_small_capsre_a_szinek_maradnak() {
        Component szines = Component.text("Legendás kard", net.kyori.adventure.text.format.TextColor.color(0xFFAA00));
        Component n = Lore.name(szines, false);
        assertEquals(GuiText.FONT_TSC, n.style().font());
        assertEquals(0xFFAA00, n.color().value());
        assertEquals(NamedTextColor.GRAY, Lore.name(Component.text("Sima"), false).color());
        assertNull(Lore.name(szines, true).style().font());
    }

    @Test
    void adat_sor_small_caps_ikonnal_es_ures_sor_kimarad() {
        List<Component> lines = Lore.lines(Arrays.asList("&7Állapot: &#62C98BEngedélyezve", "", "  ", "&7Ár &f1 250 {ikon:erme}", null), false);
        assertEquals(2, lines.size(), "az üres sorok kimaradnak");
        assertEquals("Állapot: Engedélyezve", plain(lines.get(0)));
        assertEquals("Ár 1 250 " + Icons.line("erme"), plain(lines.get(1)));
        assertEquals(GuiText.FONT_TSC, lines.get(0).style().font());
    }
}
