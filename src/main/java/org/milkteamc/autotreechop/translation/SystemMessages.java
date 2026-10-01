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

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.milkteamc.autotreechop.design.Bedrock;
import org.milkteamc.autotreechop.design.SysMsg;

/**
 * Topixity design v3 system messages for the translation files.
 *
 * <p>A translation that starts with a type token is sent as a v3 system message (icon, strip, small caps):
 *
 * <pre>
 *   enabled={siker} AutoTreeChop enabled.
 *   stillInCooldown={var} Please wait {e}{cooldown_time} seconds{/e}.
 * </pre>
 *
 * <p>{@code {e}} switches to the colour of the message type (the "value"), {@code {/e}} back to the body colour.
 * Everything else is still MiniMessage with the plugin's {@code {placeholder}} syntax. A translation without a type
 * token is formatted the old way. The formatting itself is the synced {@code design} module ({@link SysMsg}) - this
 * class only joins it to the MiniMessage pipeline. (The module's Bukkit half is not used here: this plugin shades and
 * relocates Adventure, so components go out through the plugin's own audiences.)
 */
public final class SystemMessages {

    /** {@code {type}}, {@code {!type}} (highlighted) or {@code {type:icon}} at the very start of the line. */
    private static final Pattern HEAD = Pattern.compile("^\\{(!?)([a-z]+)(?::([a-z_0-9]+))?\\} ?");

    private SystemMessages() {}

    /** The message type of a template, or {@code null} if it does not start with a type token. */
    static SysMsg.Type typeOf(String template) {
        if (template == null) {
            return null;
        }
        Matcher m = HEAD.matcher(template);
        if (!m.find()) {
            return null;
        }
        for (SysMsg.Type type : SysMsg.Type.values()) {
            if (type.token.equals(m.group(2))) {
                return type;
            }
        }
        return null;
    }

    /** The body of a v3 template as MiniMessage: the head token is cut, {@code {e}} / {@code {/e}} become colours. */
    static String body(String template, SysMsg.Type type) {
        Matcher m = HEAD.matcher(template);
        String rest = m.find() ? template.substring(m.end()) : template;
        String value = "<" + hex(type.color.value()) + ">";
        String body = "<" + hex(SysMsg.BODY.value()) + ">";
        return body + rest.replace("{e}", value).replace("{/e}", body).replace("{n}", body);
    }

    /** The template without the design tokens - for the console. */
    static String plain(String template) {
        Matcher m = HEAD.matcher(template);
        String rest = m.find() ? template.substring(m.end()) : template;
        return rest.replace("{e}", "").replace("{/e}", "").replace("{n}", "");
    }

    /**
     * Formats a translation: a v3 system message if it starts with a type token, the old formatting otherwise.
     */
    public static Component format(
            MessageFormatter formatter, CommandSender viewer, String template, TagResolver... resolvers) {
        SysMsg.Type type = typeOf(template);
        if (type == null) {
            return formatter.format(template, resolvers);
        }
        if (!(viewer instanceof Player)) {
            return formatter.format(plain(template), resolvers);
        }
        Matcher m = HEAD.matcher(template);
        m.find();
        boolean highlight = !m.group(1).isEmpty();
        String icon = m.group(3) != null ? m.group(3) : type.icon;
        Component body = formatter.format(body(template, type), resolvers);
        return SysMsg.of(type, icon, body, highlight, isBedrock((Player) viewer));
    }

    /** Bedrock (Geyser / Floodgate) players get the simplified look: icon + plain text, no strip, no small caps. */
    static boolean isBedrock(Player player) {
        Plugin floodgate = Bukkit.getPluginManager().getPlugin("floodgate");
        ClassLoader loader = floodgate != null && floodgate.isEnabled()
                ? floodgate.getClass().getClassLoader()
                : null;
        return Bedrock.isBedrock(player.getUniqueId(), loader);
    }

    private static String hex(int rgb) {
        return "#" + String.format(Locale.ROOT, "%06X", rgb);
    }
}
