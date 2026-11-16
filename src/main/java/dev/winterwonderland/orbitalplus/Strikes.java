package dev.winterwonderland.orbitalplus;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.PointedDripstone;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.WitherSkull;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/** All strike effects. Every number comes from config.yml -> strikes.<type>. */
final class Strikes {
    private final OrbitalPlus plugin;
    private int budget;   // entities this strike may still spawn
    private int spawned;  // entities this strike spawned (for the log)

    Strikes(OrbitalPlus plugin) { this.plugin = plugin; }

    ConfigurationSection settings(StrikeType t) {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("strikes." + t.id());
        return s != null ? s : new MemoryConfiguration();
    }

    boolean enabled(StrikeType t) { return settings(t).getBoolean("enabled", true); }

    /** Starts a strike. target is the block above the ground that was aimed at; source may be null (console). */
    void launch(StrikeType t, Location target, Player source) {
        int warn = plugin.getConfig().getInt("general.warning-delay-ticks", 30);
        if (t.firedFromUser() || warn <= 0) { run(t, target, source); return; }
        warningBeam(target, warn);
        Bukkit.getScheduler().runTaskLater(plugin, () -> run(t, target, source), warn);
    }

    private void run(StrikeType t, Location target, Player src) {
        budget = Math.max(1, plugin.getConfig().getInt("general.max-entities-per-strike", 3000));
        spawned = 0;
        ConfigurationSection s = settings(t);
        Location c = target == null ? null : target.clone().add(0.5, 0, 0.5); // centre of the block
        switch (t) {
            case NUKE -> nuke(c, s);
            case STAB -> stab(c, s);
            case WITHER_NUKE -> rings(c.clone().add(0, s.getDouble("height", 30), 0), s.getInt("rings", 12), s.getInt("skulls-per-ring", 8), s.getDouble("ring-spacing", 3), s.getInt("center-skulls", 3),
                    l -> skull(l, s.getDouble("speed", 1.5), s.getBoolean("blue", true), (float) s.getDouble("explosion-power", 1.5)));
            case WITHER_STAB -> witherStab(c, s);
            case DRIPSTONE -> grid(c.clone().add(0, s.getDouble("height", 40), 0), s.getInt("grid-radius", 1), s.getDouble("spacing", 1), l -> falling(l, dripstone(), s));
            case DRIPSTONE_NUKE -> rings(c.clone().add(0, s.getDouble("height", 40), 0), s.getInt("rings", 8), s.getInt("per-ring", 8), s.getDouble("ring-spacing", 2.5), 1, l -> falling(l, dripstone(), s));
            case ANVIL -> grid(c.clone().add(0, s.getDouble("height", 40), 0), s.getInt("grid-radius", 1), s.getDouble("spacing", 1), l -> falling(l, Material.ANVIL.createBlockData(), s));
            case ANVIL_NUKE -> rings(c.clone().add(0, s.getDouble("height", 40), 0), s.getInt("rings", 8), s.getInt("per-ring", 8), s.getDouble("ring-spacing", 2.5), 1, l -> falling(l, Material.ANVIL.createBlockData(), s));
            case CREEPER_NUKE -> rings(c.clone().add(0, s.getDouble("height", 20), 0), s.getInt("rings", 8), s.getInt("per-ring", 5), s.getDouble("ring-spacing", 3), 1,
                    l -> creeper(l, s.getBoolean("charged", true), s.getInt("explosion-radius", 3), s.getInt("fuse-ticks", 40)));
            case WARDEN_NUKE -> warden(c, src, s);
            case TNTCART_NUKE -> grid(c.clone().add(0, s.getDouble("height", 30), 0), s.getInt("grid-radius", 6), s.getDouble("spacing", 4), l -> cart(l, s.getInt("fuse-ticks", 60)));
            case WOLF -> wolves(c, src, s);
            case RAILGUN -> railgun(src, s, 1, 0);
            case RAILGUN_WIDE -> railgun(src, s, s.getInt("lines", 5), s.getDouble("line-spacing", 0.8));
            case RAILGUN_AERIAL -> aerial(c, src, s);
            case STASIS -> stasis(c, src, s);
        }
        if (plugin.getConfig().getBoolean("general.log-strikes", true))
            plugin.getLogger().info(t.id() + " strike by " + (src == null ? "console" : src.getName()) + ": spawned " + spawned + (budget <= 0 ? " (hit max-entities-per-strike)" : "") + (t.firedFromUser() || t == StrikeType.RAILGUN_AERIAL || t == StrikeType.WARDEN_NUKE ? " so far" : ""));
    }

    // ---- shapes --------------------------------------------------------------------------------

    /** Center count, then ring 1..rings where ring N holds N x perRing points. */
    private void rings(Location c, int rings, int perRing, double spacing, int center, Consumer<Location> spawn) {
        for (int i = 0; i < center; i++) spawn.accept(c.clone());
        for (int r = 1; r <= rings; r++) {
            int n = Math.max(1, perRing * r);
            double radius = r * spacing;
            for (int i = 0; i < n; i++) {
                double a = 2 * Math.PI * i / n;
                spawn.accept(c.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius));
            }
        }
    }

    /** Square grid of (2 x radius + 1)^2 points. */
    private void grid(Location c, int radius, double spacing, Consumer<Location> spawn) {
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) spawn.accept(c.clone().add(x * spacing, 0, z * spacing));
    }

    private boolean take() { if (budget <= 0) return false; budget--; spawned++; return true; }

    private void tag(Entity e) { e.getPersistentDataContainer().set(OrbitalPlus.STRIKE_KEY, PersistentDataType.BYTE, (byte) 1); }

    private int rnd(int max) { return max <= 0 ? 0 : ThreadLocalRandom.current().nextInt(max + 1); }

    // ---- strikes -------------------------------------------------------------------------------

    private void nuke(Location c, ConfigurationSection s) {
        int fuse = s.getInt("fuse-ticks", 60), rand = s.getInt("fuse-random-ticks", 15);
        float power = (float) s.getDouble("explosion-power", 4);
        rings(c.clone().add(0, s.getDouble("height", 35), 0), s.getInt("rings", 15), s.getInt("tnt-per-ring", 8), s.getDouble("ring-spacing", 3), s.getInt("center-tnt", 3),
                l -> tnt(l, fuse + rnd(rand), power));
        c.getWorld().playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 6f, 0.5f);
    }

    private void stab(Location c, ConfigurationSection s) {
        int depth = s.getInt("depth", 80), fuse = s.getInt("fuse-ticks", 10), step = s.getInt("fuse-step-ticks", 2);
        double spacing = Math.max(0.5, s.getDouble("spacing", 2));
        float power = (float) s.getDouble("explosion-power", 4);
        int minY = c.getWorld().getMinHeight();
        grid(c, s.getInt("columns-radius", 0), 1.0, col -> {
            for (int i = 0; i * spacing <= depth; i++) {
                Location l = col.clone().add(0, -i * spacing, 0);
                if (l.getY() < minY) break;
                tnt(l, fuse + i * step, power);
            }
        });
    }

    private void witherStab(Location c, ConfigurationSection s) {
        int waves = Math.max(1, s.getInt("waves", 3)), delay = s.getInt("wave-delay-ticks", 6);
        for (int w = 0; w < waves; w++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> grid(c.clone().add(0, s.getDouble("height", 40), 0), s.getInt("grid-radius", 2), s.getDouble("spacing", 1.5),
                    l -> skull(l, s.getDouble("speed", 2), s.getBoolean("blue", true), (float) s.getDouble("explosion-power", 1.5))), (long) w * delay);
        }
    }

    private void tnt(Location l, int fuse, float power) {
        if (!take()) return;
        l.getWorld().spawn(l, TNTPrimed.class, t -> { t.setFuseTicks(Math.max(1, fuse)); t.setYield(power); tag(t); });
    }

    @SuppressWarnings("deprecation")
    private void skull(Location l, double speed, boolean blue, float power) {
        if (!take()) return;
        Vector down = new Vector(0, -1, 0);
        l.getWorld().spawn(l, WitherSkull.class, k -> {
            k.setCharged(blue);
            k.setDirection(down);
            k.setVelocity(down.clone().multiply(speed));
            k.setYield(power);
            tag(k);
        });
    }

    private BlockData dripstone() {
        PointedDripstone d = (PointedDripstone) Material.POINTED_DRIPSTONE.createBlockData();
        d.setVerticalDirection(BlockFace.DOWN);
        d.setThickness(PointedDripstone.Thickness.TIP);
        return d;
    }

    @SuppressWarnings("deprecation")
    private void falling(Location l, BlockData data, ConfigurationSection s) {
        if (!take()) return;
        FallingBlock fb = l.getWorld().spawnFallingBlock(l, data);
        fb.setHurtEntities(true);
        fb.setDamagePerBlock((float) s.getDouble("damage-per-block", 2));
        fb.setMaxDamage(s.getInt("max-damage", 40));
        fb.setDropItem(false);
        tag(fb);
    }

    private void creeper(Location l, boolean charged, int radius, int fuse) {
        if (!take()) return;
        l.getWorld().spawn(l, Creeper.class, cr -> {
            cr.setPowered(charged);
            cr.setExplosionRadius(radius);
            cr.setMaxFuseTicks(Math.max(1, fuse));
            tag(cr);
            cr.ignite();
        });
    }

    private void cart(Location l, int fuse) {
        if (!take()) return;
        l.getWorld().spawn(l, ExplosiveMinecart.class, m -> { tag(m); m.setFuseTicks(Math.max(1, fuse)); });
    }

    private void warden(Location c, Player src, ConfigurationSection s) {
        int rings = s.getInt("rings", 5), delay = s.getInt("ring-delay-ticks", 6);
        double spacing = s.getDouble("ring-spacing", 4), damage = s.getDouble("damage", 20), kb = s.getDouble("knockback", 1.8);
        boolean hurtUser = s.getBoolean("hurt-user", false);
        World w = c.getWorld();
        for (int r = 1; r <= rings; r++) {
            double outer = r * spacing, inner = outer - spacing;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                int points = Math.max(8, (int) (outer * 6));
                for (int i = 0; i < points; i++) {
                    double a = 2 * Math.PI * i / points;
                    w.spawnParticle(Particle.SONIC_BOOM, c.clone().add(Math.cos(a) * outer, 1, Math.sin(a) * outer), 1);
                }
                w.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 4f, 1f);
                for (Entity e : w.getNearbyEntities(c, outer, outer, outer)) {
                    if (!(e instanceof LivingEntity le) || (!hurtUser && e.equals(src))) continue;
                    double d = le.getLocation().distance(c);
                    if (d > outer || d < inner) continue;
                    le.damage(damage, src);
                    Vector push = le.getLocation().toVector().subtract(c.toVector()).setY(0);
                    if (push.lengthSquared() > 1e-6) le.setVelocity(push.normalize().multiply(kb).setY(0.6));
                }
            }, (long) (r - 1) * delay);
        }
    }

    private void wolves(Location c, Player src, ConfigurationSection s) {
        int count = s.getInt("count", 40), life = s.getInt("despawn-after-seconds", 120);
        double health = Math.max(1, s.getDouble("health", 20)), radius = s.getDouble("attack-radius", 24);
        boolean attackPlayers = s.getBoolean("attack-players", false);
        List<Wolf> pack = new ArrayList<>();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < count && take(); i++) {
            Location l = c.clone().add(r.nextDouble(-2.5, 2.5), 0.2, r.nextDouble(-2.5, 2.5));
            pack.add(c.getWorld().spawn(l, Wolf.class, wf -> {
                if (src != null) { wf.setTamed(true); wf.setOwner(src); }
                AttributeInstance max = wf.getAttribute(Attribute.MAX_HEALTH);
                if (max != null) max.setBaseValue(health);
                wf.setHealth(health);
                tag(wf);
            }));
        }
        for (Wolf wf : pack) {
            LivingEntity best = null; double bestD = Double.MAX_VALUE;
            for (Entity e : wf.getNearbyEntities(radius, radius, radius)) {
                if (!(e instanceof LivingEntity le) || e instanceof Wolf || e.equals(src)) continue;
                if (!(e instanceof Monster) && !(attackPlayers && e instanceof Player)) continue;
                double d = e.getLocation().distanceSquared(wf.getLocation());
                if (d < bestD) { bestD = d; best = le; }
            }
            if (best != null) wf.setTarget(best);
        }
        c.getWorld().playSound(c, "minecraft:entity.wolf.howl", 3f, 1f); // by id: wolf sounds became per-variant in 26.x
        if (life > 0) Bukkit.getScheduler().runTaskLater(plugin, () -> pack.forEach(wf -> { if (wf.isValid()) wf.remove(); }), life * 20L);
    }

    private void railgun(Player p, ConfigurationSection s, int lines, double lineSpacing) {
        if (p == null) return;
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        Vector side = dir.clone().crossProduct(new Vector(0, 1, 0));
        if (side.lengthSquared() < 1e-6) side = new Vector(1, 0, 0);
        side.normalize();
        List<Location> origins = new ArrayList<>();
        for (int i = 0; i < Math.max(1, lines); i++) origins.add(eye.clone().add(side.clone().multiply((i - (lines - 1) / 2.0) * lineSpacing)).add(dir.clone().multiply(1.5)));
        int total = s.getInt("arrows", 90), perTick = Math.max(1, s.getInt("arrows-per-tick", 10));
        float speed = (float) s.getDouble("speed", 6), spread = (float) s.getDouble("spread", 0);
        double damage = s.getDouble("damage", 20);
        int lifetime = s.getInt("arrow-lifetime-seconds", 10);
        p.getWorld().playSound(eye, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 3f, 0.5f);
        new BukkitRunnable() {
            int fired = 0;
            @Override public void run() {
                for (int k = 0; k < perTick && fired < total; k++, fired++)
                    for (Location o : origins) arrow(o, dir, speed, spread, damage, p, lifetime);
                if (fired >= total || budget <= 0) cancel();
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void aerial(Location c, Player src, ConfigurationSection s) {
        int total = s.getInt("arrows", 270), perTick = Math.max(1, s.getInt("arrows-per-tick", 30)), lifetime = s.getInt("arrow-lifetime-seconds", 10);
        double radius = s.getDouble("radius", 6), height = s.getDouble("height", 50), damage = s.getDouble("damage", 12);
        float speed = (float) s.getDouble("speed", 4);
        Vector down = new Vector(0, -1, 0);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        new BukkitRunnable() {
            int fired = 0;
            @Override public void run() {
                for (int k = 0; k < perTick && fired < total; k++, fired++) {
                    double a = r.nextDouble(0, 2 * Math.PI), d = Math.sqrt(r.nextDouble()) * radius;
                    arrow(c.clone().add(Math.cos(a) * d, height, Math.sin(a) * d), down, speed, 0, damage, src, lifetime);
                }
                if (fired >= total || budget <= 0) cancel();
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void arrow(Location from, Vector dir, float speed, float spread, double damage, Player shooter, int lifetimeSeconds) {
        if (!take()) return;
        Arrow a = from.getWorld().spawnArrow(from, dir, speed, spread);
        if (shooter != null) a.setShooter(shooter);
        a.setDamage(damage);
        a.setCritical(true);
        a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        tag(a);
        if (lifetimeSeconds > 0) Bukkit.getScheduler().runTaskLater(plugin, () -> { if (a.isValid()) a.remove(); }, lifetimeSeconds * 20L);
    }

    private void stasis(Location c, Player src, ConfigurationSection s) {
        double radius = s.getDouble("radius", 10);
        int ticks = Math.max(1, s.getInt("duration-seconds", 8)) * 20;
        boolean players = s.getBoolean("include-players", true), user = s.getBoolean("include-user", false);
        World w = c.getWorld();
        for (Entity e : w.getNearbyEntities(c, radius, radius, radius)) {
            if (!(e instanceof LivingEntity le) || e.getLocation().distance(c) > radius) continue;
            if (e instanceof Player && (!players || (!user && e.equals(src)))) continue;
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, 255, false, false));
            le.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, ticks, 200, false, false)); // above 127 = can't jump
            le.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, ticks, 4, false, false));
            le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, ticks, 0, false, false));
            le.setVelocity(new Vector());
            spawned++;
        }
        w.spawnParticle(Particle.END_ROD, c.clone().add(0, 1, 0), 200, radius / 2, 1, radius / 2, 0.02);
        w.playSound(c, Sound.BLOCK_BEACON_POWER_SELECT, 4f, 0.5f);
    }

    private void warningBeam(Location target, int ticks) {
        World w = target.getWorld();
        Location base = target.clone().add(0.5, 0, 0.5);
        w.playSound(base, Sound.BLOCK_BEACON_ACTIVATE, 6f, 0.6f);
        new BukkitRunnable() {
            int n = 0;
            @Override public void run() {
                if ((n += 4) > ticks) { cancel(); return; }
                for (int y = 0; y < 60; y += 2) w.spawnParticle(Particle.END_ROD, base.clone().add(0, y, 0), 1, 0, 0, 0, 0);
            }
        }.runTaskTimer(plugin, 0, 4);
    }
}
