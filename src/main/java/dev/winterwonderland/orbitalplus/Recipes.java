package dev.winterwonderland.orbitalplus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ShapedRecipe;

import java.util.List;

/** Optional crafting recipes for rods, from config.yml -> recipes. */
final class Recipes {
    private Recipes() {}

    static void register(OrbitalPlus plugin, Rods rods) {
        for (StrikeType t : StrikeType.values()) Bukkit.removeRecipe(new NamespacedKey(plugin, "rod_" + t.id()));
        ConfigurationSection all = plugin.getConfig().getConfigurationSection("recipes");
        if (all == null) return;
        for (String id : all.getKeys(false)) {
            ConfigurationSection r = all.getConfigurationSection(id);
            StrikeType t = StrikeType.of(id);
            if (r == null || t == null || !r.getBoolean("enabled", false)) continue;
            try {
                List<String> shape = r.getStringList("shape");
                ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "rod_" + t.id()), rods.create(t));
                recipe.shape(shape.toArray(new String[0]));
                ConfigurationSection ing = r.getConfigurationSection("ingredients");
                if (ing != null) for (String key : ing.getKeys(false)) {
                    Material m = Material.matchMaterial(ing.getString(key, ""));
                    if (m == null) throw new IllegalArgumentException("unknown item '" + ing.getString(key) + "'");
                    recipe.setIngredient(key.charAt(0), m);
                }
                Bukkit.addRecipe(recipe);
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Recipe for " + id + " skipped: " + ex.getMessage());
            }
        }
    }
}
