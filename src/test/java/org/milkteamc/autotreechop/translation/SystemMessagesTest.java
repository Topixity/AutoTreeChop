/*
 * Copyright (C) 2026 MilkTeaMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
 
package org.milkteamc.autotreechop.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import org.milkteamc.autotreechop.design.GuiText;
import org.milkteamc.autotreechop.design.SysMsg;

class SystemMessagesTest {

    @Test
    void typeTokenAtTheStartMakesASystemMessage() {
        assertEquals(SysMsg.Type.SIKER, SystemMessages.typeOf("{siker} AutoTreeChop enabled."));
        assertEquals(SysMsg.Type.INFO, SystemMessages.typeOf("{info:pont} License: GPL"));
        assertEquals(SysMsg.Type.HIBA, SystemMessages.typeOf("{!hiba} No."));
    }

    @Test
    void oldStyleLinesAndPlaceholdersAreNotSystemMessages() {
        assertNull(SystemMessages.typeOf("<red>You do not have permission.</red>"));
        assertNull(SystemMessages.typeOf("{player} enabled it"));
        assertNull(SystemMessages.typeOf(" {siker} leading space"));
        assertNull(SystemMessages.typeOf(null));
    }

    @Test
    void bodyKeepsThePlaceholdersAndColoursTheValue() {
        String body = SystemMessages.body("{var} Please wait {e}{cooldown_time} seconds{/e}.", SysMsg.Type.VAR);
        assertEquals("<#C0C2B8>Please wait <#E8914A>{cooldown_time} seconds<#C0C2B8>.", body);
        assertEquals(
                "Please wait {cooldown_time} seconds.",
                SystemMessages.plain("{var} Please wait {e}{cooldown_time} seconds{/e}."));
    }

    @Test
    void theVisibleTextOfTheSystemMessageIsTheTranslation() {
        String template = "{siker} AutoTreeChop was enabled by {e}{player}{/e}.";
        SysMsg.Type type = SystemMessages.typeOf(template);
        // the plugin's StyleRegistry turns {placeholder} into <placeholder> before MiniMessage
        String mini = SystemMessages.body(template, type).replace("{player}", "<player>");
        Component body = MiniMessage.miniMessage().deserialize(mini, Placeholder.parsed("player", "Seda"));
        Component java = SysMsg.of(type, type.icon, body, false, false);
        Component bedrock = SysMsg.of(type, type.icon, body, false, true);
        String javaText =
                PlainTextComponentSerializer.plainText().serialize(java).replaceAll("[\\x{E000}-\\x{F8FF}]", "");
        String bedrockText =
                PlainTextComponentSerializer.plainText().serialize(bedrock).replaceAll("[\\x{E000}-\\x{F8FF}]", "");
        assertEquals("AutoTreeChop was enabled by Seda.", javaText.trim());
        assertEquals("AutoTreeChop was enabled by Seda.", bedrockText.trim());
        assertTrue(hasFont(java, GuiText.FONT_TSC), "Java players get the small caps font");
    }

    private static boolean hasFont(Component component, Key font) {
        if (font.equals(component.font())) {
            return true;
        }
        for (Component child : component.children()) {
            if (hasFont(child, font)) {
                return true;
            }
        }
        return false;
    }
}
