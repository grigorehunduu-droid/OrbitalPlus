package dev.winterwonderland.orbitalplus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

final class Text {
    private Text() {}

    /** Turns "&bHello" into a coloured, non-italic component. */
    static Component color(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s == null ? "" : s).decoration(TextDecoration.ITALIC, false);
    }

    static void send(CommandSender to, String s) { to.sendMessage(color("&b[Orbital] &r" + s)); }
}
