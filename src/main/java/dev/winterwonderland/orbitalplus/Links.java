package dev.winterwonderland.orbitalplus;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockRedstoneEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Buttons, levers and pressure plates that fire a strike when they turn on. Saved in links.yml as
 * "world,x,y,z" of the trigger -> strike type, where it lands, and who linked it.
 */
final class Links implements Listener {
    private final OrbitalPlus plugin;
    private final Strikes strikes;
    private final File file;
    private final YamlConfiguration data;
    private final Map<UUID, Location> marked = new HashMap<>();
    private final Map<String, Long> lastFired = new HashMap<>();

    Links(OrbitalPlus plugin, Strikes strikes) {
        this.plugin = plugin;
        this.strikes = strikes;
        file = new File(plugin.getDataFolder(), "links.yml");
        data = YamlConfiguration.loadConfiguration(file);
    }

    static boolean isTrigger(Material m) { return m == Material.LEVER || Tag.BUTTONS.isTagged(m) || Tag.PRESSURE_PLATES.isTagged(m); }

    private static String key(Location l) { return l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ(); }

    private static Location parse(String key) {
        String[] p = key.split(",");
        World w = p.length == 4 ? Bukkit.getWorld(p[0]) : null;
        return w == null ? null : new Location(w, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
    }

    private static String pretty(String key) { String[] p = key.split(","); return p[1] + " " + p[2] + " " + p[3] + " &7(" + p[0] + ")"; }

    private static String name(Material m) { return m.name().toLowerCase(Locale.ROOT).replace('_', ' '); }

    /** Sneak + right-click with a rod: on a button/lever/plate it links it, anywhere else it marks where the strike lands. */
    void sneakClick(Player p, StrikeType type, Block clicked, Location aim) {
        if (!p.hasPermission("orbitalplus.link")) { Text.send(p, "&cOnly admins can link rods."); return; }
        if (!plugin.getConfig().getBoolean("links.enabled", true)) { Text.send(p, "&cLinking is turned off in the config."); return; }
        if (clicked == null || !isTrigger(clicked.getType())) {
            if (aim == null) { Text.send(p, "&cNo target in range."); return; }
            marked.put(p.getUniqueId(), aim);
            Text.send(p, "&eTarget marked at &f" + pretty(key(aim)) + "&e. Now sneak + right-click a button, lever or pressure plate.");
            return;
        }
        if (type.firedFromUser()) { Text.send(p, "&c" + type.nice() + " fires from a player, so it can't be linked. Try railgun_aerial."); return; }
        Location target = marked.remove(p.getUniqueId());
        if (target == null) target = clicked.getLocation(); // no mark: the strike lands on the trigger itself (a trap)
        String k = key(clicked.getLocation());
        data.set(k + ".type", type.id());
        data.set(k + ".target", key(target));
        data.set(k + ".owner", p.getUniqueId().toString());
        save();
        Text.send(p, "&aLinked " + type.nice() + " to this " + name(clicked.getType()) + ". It lands at &f" + pretty(key(target)) + "&a.");
    }

    @EventHandler
    public void onPower(BlockRedstoneEvent e) {
        if (e.getOldCurrent() > 0 || e.getNewCurrent() <= 0) return; // only when it switches on
        String k = key(e.getBlock().getLocation());
        ConfigurationSection link = data.getConfigurationSection(k);
        if (link == null || !plugin.getConfig().getBoolean("links.enabled", true)) return;
        StrikeType type = StrikeType.of(link.getString("type"));
        Location target = parse(link.getString("target", k));
        if (type == null || target == null || !strikes.enabled(type)) return;

        long now = System.currentTimeMillis(), cooldown = (long) (plugin.getConfig().getDouble("links.cooldown-seconds", 5) * 1000);
        if (now - lastFired.getOrDefault(k, 0L) < cooldown) return;
        lastFired.put(k, now);

        String owner = link.getString("owner");
        Player linker = owner == null ? null : Bukkit.getPlayer(UUID.fromString(owner)); // wolves obey them, their own trap spares them
        if (plugin.getConfig().getBoolean("general.log-strikes", true))
            plugin.getLogger().info(type.id() + " fired by the " + name(e.getBlock().getType()) + " at " + pretty(k).replace("&7", ""));
        strikes.launch(type, target, linker);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent e) {
        String k = key(e.getBlock().getLocation());
        if (!data.contains(k)) return;
        data.set(k, null);
        save();
        Text.send(e.getPlayer(), "&eOrbital link removed.");
    }

    /** /orbital links. Also forgets links whose button/lever/plate no longer exists. */
    void list(CommandSender s) {
        Text.send(s, "&fLinked buttons, levers and plates:");
        int shown = 0;
        for (String k : new ArrayList<>(data.getKeys(false))) {
            Location at = parse(k);
            if (at != null && !isTrigger(at.getBlock().getType())) { data.set(k, null); continue; }
            shown++;
            s.sendMessage(Text.color("  &f" + data.getString(k + ".type") + " &7at &f" + pretty(k) + " &7-> lands &f" + pretty(data.getString(k + ".target", k))));
        }
        save();
        if (shown == 0) s.sendMessage(Text.color("  &7none"));
    }

    private void save() {
        try { data.save(file); } catch (IOException ex) { plugin.getLogger().warning("Could not save links.yml: " + ex.getMessage()); }
    }
}
