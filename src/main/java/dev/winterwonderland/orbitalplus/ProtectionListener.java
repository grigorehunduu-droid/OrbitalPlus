package dev.winterwonderland.orbitalplus;

import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataType;

/** Applies general.allow-block-damage and general.keep-falling-blocks to things a strike spawned. */
final class ProtectionListener implements Listener {
    private final OrbitalPlus plugin;

    ProtectionListener(OrbitalPlus plugin) { this.plugin = plugin; }

    static boolean fromStrike(Entity e) {
        return e != null && e.getPersistentDataContainer().has(OrbitalPlus.STRIKE_KEY, PersistentDataType.BYTE);
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        if (fromStrike(e.getEntity()) && !plugin.getConfig().getBoolean("general.allow-block-damage", true)) e.blockList().clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onLand(EntityChangeBlockEvent e) {
        if (e.getEntity() instanceof FallingBlock fb && fromStrike(fb) && !plugin.getConfig().getBoolean("general.keep-falling-blocks", false)) {
            e.setCancelled(true);
            fb.remove();
        }
    }
}
