package dev.winterwonderland.orbitalplus;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * /orbital list
 * /orbital give <type> [player] [amount]
 * /orbital kit <type> [player] [color]
 * /orbital strike <type> [x y z [world]]
 * /orbital info <type>
 * /orbital reload
 */
final class OrbitalCommand implements TabExecutor {
    private final OrbitalPlus plugin;
    private final Strikes strikes;
    private final Rods rods;
    private final Links links;

    OrbitalCommand(OrbitalPlus plugin, Strikes strikes, Rods rods, Links links) { this.plugin = plugin; this.strikes = strikes; this.rods = rods; this.links = links; }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        String sub = a.length == 0 ? "help" : a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> {
                StringBuilder b = new StringBuilder("&fStrikes: ");
                for (StrikeType t : StrikeType.values()) b.append(strikes.enabled(t) ? "&a" : "&8").append(t.id()).append("&7, ");
                Text.send(s, b.substring(0, b.length() - 4));
            }
            case "reload" -> {
                if (!need(s, "orbitalplus.admin")) return true;
                plugin.reload();
                Text.send(s, "&aConfig reloaded.");
            }
            case "give", "kit" -> {
                if (!need(s, "orbitalplus.give")) return true;
                StrikeType t = a.length > 1 ? StrikeType.of(a[1]) : null;
                if (t == null) { Text.send(s, "&cUsage: /" + label + " " + sub + " <type> [player] " + (sub.equals("give") ? "[amount]" : "[color]")); return true; }
                Player target = a.length > 2 ? Bukkit.getPlayerExact(a[2]) : (s instanceof Player p ? p : null);
                if (target == null) { Text.send(s, "&cPlayer not found (from the console you must name a player)."); return true; }
                if (sub.equals("give")) {
                    int amount = 1;
                    try { if (a.length > 3) amount = Math.max(1, Math.min(64, Integer.parseInt(a[3]))); } catch (NumberFormatException ignored) {}
                    for (int i = 0; i < amount; i++) giveOrDrop(target, rods.create(t));
                    Text.send(s, "&aGave " + amount + " " + t.nice() + " rod(s) to " + target.getName() + ".");
                } else {
                    ItemStack kit = rods.kit(t, a.length > 3 ? a[3] : "light_blue");
                    if (kit == null) { Text.send(s, "&cUnknown colour. Try: " + colours()); return true; }
                    giveOrDrop(target, kit);
                    Text.send(s, "&aGave a shulker of " + t.nice() + " rods to " + target.getName() + ".");
                }
            }
            case "strike" -> {
                if (!need(s, "orbitalplus.admin")) return true;
                StrikeType t = a.length > 1 ? StrikeType.of(a[1]) : null;
                if (t == null) { Text.send(s, "&cUsage: /" + label + " strike <type> [x y z [world]]"); return true; }
                Player p = s instanceof Player pl ? pl : null;
                Location at;
                if (a.length >= 5) {
                    World w = a.length > 5 ? Bukkit.getWorld(a[5]) : (p != null ? p.getWorld() : Bukkit.getWorlds().get(0));
                    if (w == null) { Text.send(s, "&cUnknown world."); return true; }
                    try { at = new Location(w, Double.parseDouble(a[2]), Double.parseDouble(a[3]), Double.parseDouble(a[4])).toBlockLocation(); }
                    catch (NumberFormatException e) { Text.send(s, "&cCoordinates must be numbers."); return true; }
                } else if (p != null) {
                    var hit = p.getTargetBlockExact((int) plugin.getConfig().getDouble("general.target-range", 300));
                    at = hit == null ? p.getLocation().toBlockLocation() : hit.getLocation().add(0, 1, 0);
                } else { Text.send(s, "&cFrom the console give coordinates: strike <type> <x> <y> <z> [world]"); return true; }
                if (t.firedFromUser() && p == null) { Text.send(s, "&cRailguns fire from a player; use a rod in game."); return true; }
                strikes.launch(t, at, p);
                Text.send(s, "&a" + t.nice() + " strike sent to " + at.getBlockX() + " " + at.getBlockY() + " " + at.getBlockZ() + ".");
            }
            case "links" -> {
                if (need(s, "orbitalplus.link")) links.list(s);
            }
            case "info" -> {
                StrikeType t = a.length > 1 ? StrikeType.of(a[1]) : null;
                if (t == null) { Text.send(s, "&cUsage: /" + label + " info <type>"); return true; }
                ConfigurationSection sec = strikes.settings(t);
                Text.send(s, "&f" + t.nice() + " settings:");
                for (String k : sec.getKeys(false)) s.sendMessage(Text.color("  &7" + k + ": &f" + sec.get(k)));
            }
            default -> {
                Text.send(s, "&f/" + label + " list &7- all strikes");
                Text.send(s, "&f/" + label + " give <type> [player] [amount] &7- get rods");
                Text.send(s, "&f/" + label + " kit <type> [player] [color] &7- shulker of 27 rods");
                Text.send(s, "&f/" + label + " strike <type> [x y z [world]] &7- strike now");
                Text.send(s, "&f/" + label + " info <type> &7- show its settings");
                Text.send(s, "&f/" + label + " links &7- linked buttons, levers and plates");
                Text.send(s, "&f/" + label + " reload &7- reload config.yml");
                Text.send(s, "&7Sneak + right-click a button, lever or pressure plate with a rod to link it.");
            }
        }
        return true;
    }

    private boolean need(CommandSender s, String perm) {
        if (s.hasPermission(perm)) return true;
        Text.send(s, "&cOnly admins can do that.");
        return false;
    }

    private void giveOrDrop(Player p, ItemStack item) {
        p.getInventory().addItem(item).values().forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
    }

    private String colours() {
        return String.join(", ", Arrays.stream(DyeColor.values()).map(c -> c.name().toLowerCase(Locale.ROOT)).toList());
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) out.addAll(List.of("list", "give", "kit", "strike", "info", "links", "reload"));
        else if (a.length == 2 && List.of("give", "kit", "strike", "info").contains(a[0].toLowerCase(Locale.ROOT)))
            for (StrikeType t : StrikeType.values()) out.add(t.id());
        else if (a.length == 3 && List.of("give", "kit").contains(a[0].toLowerCase(Locale.ROOT)))
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        else if (a.length == 4 && a[0].equalsIgnoreCase("kit"))
            for (DyeColor c : DyeColor.values()) out.add(c.name().toLowerCase(Locale.ROOT));
        String last = a.length == 0 ? "" : a[a.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
