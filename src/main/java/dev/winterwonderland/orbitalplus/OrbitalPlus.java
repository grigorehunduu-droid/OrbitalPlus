package dev.winterwonderland.orbitalplus;

import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class OrbitalPlus extends JavaPlugin {
    static NamespacedKey ROD_KEY, USES_KEY, STRIKE_KEY;

    private Strikes strikes;
    private Rods rods;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ROD_KEY = new NamespacedKey(this, "rod_type");
        USES_KEY = new NamespacedKey(this, "uses_left");
        STRIKE_KEY = new NamespacedKey(this, "strike_entity");

        strikes = new Strikes(this);
        rods = new Rods(this);
        Links links = new Links(this, strikes);
        OrbitalCommand command = new OrbitalCommand(this, strikes, rods, links);
        PluginCommand pc = getCommand("orbital");
        if (pc != null) { pc.setExecutor(command); pc.setTabCompleter(command); }
        getServer().getPluginManager().registerEvents(new RodListener(this, strikes, rods, links), this);
        getServer().getPluginManager().registerEvents(links, this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        Recipes.register(this, rods);
        getLogger().info("Ready: " + StrikeType.values().length + " strike types. Edit config.yml and run /orbital reload.");
    }

    void reload() {
        reloadConfig();
        Recipes.register(this, rods);
    }
}
