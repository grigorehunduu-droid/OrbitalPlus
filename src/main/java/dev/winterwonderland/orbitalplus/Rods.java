package dev.winterwonderland.orbitalplus;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Creates rod items and reads which strike a rod fires. */
final class Rods {
    private final OrbitalPlus plugin;

    Rods(OrbitalPlus plugin) { this.plugin = plugin; }

    ItemStack create(StrikeType type) {
        FileConfiguration c = plugin.getConfig();
        Material m = Material.matchMaterial(c.getString("rod.material", "FISHING_ROD"));
        if (m == null || !m.isItem()) m = Material.FISHING_ROD;
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.color(c.getString("rod.name", "&bOrbital Rod - {type}").replace("{type}", type.nice())));
        List<Component> lore = new ArrayList<>();
        for (String line : c.getStringList("rod.lore")) lore.add(Text.color(line.replace("{type}", type.nice())));
        int uses = c.getInt("rod.uses", -1);
        if (uses > 0) lore.add(Text.color("&8Uses left: " + uses));
        meta.lore(lore);
        if (c.getBoolean("rod.glint", true)) meta.setEnchantmentGlintOverride(true);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(OrbitalPlus.ROD_KEY, PersistentDataType.STRING, type.id());
        if (uses > 0) pdc.set(OrbitalPlus.USES_KEY, PersistentDataType.INTEGER, uses);
        item.setItemMeta(meta);
        return item;
    }

    /** The strike type of a rod, or null if the item is not an orbital rod. */
    StrikeType typeOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return StrikeType.of(item.getItemMeta().getPersistentDataContainer().get(OrbitalPlus.ROD_KEY, PersistentDataType.STRING));
    }

    /** Uses up one charge. Returns false when the rod just broke. */
    boolean useCharge(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        Integer left = meta.getPersistentDataContainer().get(OrbitalPlus.USES_KEY, PersistentDataType.INTEGER);
        if (left == null) return true; // unlimited
        left--;
        if (left <= 0) { item.setAmount(item.getAmount() - 1); return false; }
        meta.getPersistentDataContainer().set(OrbitalPlus.USES_KEY, PersistentDataType.INTEGER, left);
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        if (!lore.isEmpty()) lore.set(lore.size() - 1, Text.color("&8Uses left: " + left));
        meta.lore(lore);
        item.setItemMeta(meta);
        return true;
    }

    /** A shulker box filled with 27 rods of one type. Returns null for an unknown colour. */
    ItemStack kit(StrikeType type, String color) {
        String c = color == null || color.isBlank() ? "" : color.toUpperCase(Locale.ROOT) + "_";
        Material box = Material.matchMaterial(c + "SHULKER_BOX");
        if (box == null) return null;
        ItemStack item = new ItemStack(box);
        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        ShulkerBox state = (ShulkerBox) meta.getBlockState();
        for (int i = 0; i < state.getInventory().getSize(); i++) state.getInventory().setItem(i, create(type));
        meta.setBlockState(state);
        meta.displayName(Text.color("&b" + type.nice() + " Rods"));
        item.setItemMeta(meta);
        return item;
    }
}
