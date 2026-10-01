package org.milkteamc.autotreechop.design;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Az ikon-tábla a pack kódkiosztását követi (txPack-network `_tools/ikon_kodok.properties`). */
class IconsTest {

    @Test
    void kodkiosztas() {
        assertEquals("\uEA00", Icons.chat("pipa"));
        assertEquals("\uEA40", Icons.line("pipa"));
        assertEquals("\uEA0B", Icons.chat("info"));
        assertEquals("\uEA21", Icons.chat("erme"));
        assertEquals("\uEA6F", Icons.line("kuka"));
        assertEquals(48, Icons.NAMES.size());
        assertNull(Icons.chat("nincsilyen"));
        for (String n : Icons.NAMES) assertEquals(Icons.chat(n).charAt(0) + 0x40, Icons.line(n).charAt(0), n);
    }

    @Test
    void token_eltavolitas() {
        assertEquals("Kezdés", Icons.strip("{ikon:ora} Kezdés"));
        assertEquals("A B", Icons.strip("A {ikon:x}B"));
    }
}
