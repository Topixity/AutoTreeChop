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
    void nev_szurke_a_kivalasztott_zold_pipaval() {
        Component n = Lore.name("Elytra", false, false);
        assertEquals("Elytra", plain(n));
        assertEquals(NamedTextColor.GRAY, n.color());
        Component s = Lore.name("DeBuff", true, false);
        assertEquals(Lore.CHECK + " DeBuff", plain(s));
        assertEquals(Lore.SELECTED, s.color());
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
