package dev.winterwonderland.orbitalplus;

import java.util.Locale;

public enum StrikeType {
    NUKE, STAB, WITHER_NUKE, WITHER_STAB, DRIPSTONE, DRIPSTONE_NUKE, ANVIL, ANVIL_NUKE,
    CREEPER_NUKE, WARDEN_NUKE, TNTCART_NUKE, WOLF, RAILGUN, RAILGUN_WIDE, RAILGUN_AERIAL, STASIS;

    /** Name used in commands and the config, e.g. "wither_nuke". */
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** Nice name for item names and messages, e.g. "Wither Nuke". */
    public String nice() {
        StringBuilder b = new StringBuilder();
        for (String w : id().split("_")) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString().replace("Tntcart", "TNT Cart");
    }

    /** Railguns fire from the player instead of landing on a target. */
    public boolean firedFromUser() { return this == RAILGUN || this == RAILGUN_WIDE; }

    public static StrikeType of(String s) {
        if (s == null) return null;
        try { return valueOf(s.trim().toUpperCase(Locale.ROOT).replace('-', '_')); } catch (IllegalArgumentException e) { return null; }
    }
}
