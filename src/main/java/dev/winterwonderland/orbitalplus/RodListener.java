package dev.winterwonderland.orbitalplus;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Right-clicking a rod fires its strike at whatever the player is looking at. */
final class RodListener implements Listener {
    private final OrbitalPlus plugin;
    private final Strikes strikes;
    private final Rods rods;
    private final Links links;
    private final Map<UUID, Long> lastUse = new HashMap<>();

    RodListener(OrbitalPlus plugin, Strikes strikes, Rods rods, Links links) { this.plugin = plugin; this.strikes = strikes; this.rods = rods; this.links = links; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = e.getItem();
        StrikeType type = rods.typeOf(item);
        if (type == null) return;
        e.setCancelled(true); // never cast the fishing rod / place the item
        Player p = e.getPlayer();

        if (!p.hasPermission("orbitalplus.use")) { Text.send(p, "&cOnly admins can use orbital rods."); return; }
        if (p.isSneaking()) { links.sneakClick(p, type, e.getClickedBlock(), aim(p)); return; }
        if (!strikes.enabled(type)) { Text.send(p, "&cThe " + type.nice() + " strike is turned off in the config."); return; }

        long cooldownMs = (long) (plugin.getConfig().getDouble("rod.cooldown-seconds", 3) * 1000);
        long now = System.currentTimeMillis(), last = lastUse.getOrDefault(p.getUniqueId(), 0L);
        if (now - last < cooldownMs) { Text.send(p, "&eRecharging... " + String.format("%.1f", (cooldownMs - (now - last)) / 1000.0) + "s"); return; }

        Location target = type.firedFromUser() ? null : aim(p);
        if (!type.firedFromUser() && target == null) { Text.send(p, "&cNo target in range."); return; }

        lastUse.put(p.getUniqueId(), now);
        strikes.launch(type, target, p);
        if (!rods.useCharge(item)) Text.send(p, "&7Your rod broke.");
        if (plugin.getConfig().getBoolean("general.announce", true))
            p.sendActionBar(Text.color("&b" + type.nice() + " strike incoming!"));
    }

    /** The block above whatever the player looks at, or the ground at the end of their aim. */
    private Location aim(Player p) {
        double range = plugin.getConfig().getDouble("general.target-range", 300);
        RayTraceResult hit = p.rayTraceBlocks(range, FluidCollisionMode.NEVER);
        if (hit != null && hit.getHitBlock() != null) return hit.getHitBlock().getLocation().add(0, 1, 0);
        Location end = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(range));
        if (end.getY() < p.getWorld().getMinHeight()) return null;
        return p.getWorld().getHighestBlockAt(end).getLocation().add(0, 1, 0);
    }
}
