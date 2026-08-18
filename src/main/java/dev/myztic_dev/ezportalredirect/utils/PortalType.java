package dev.myztic_dev.ezportalredirect.utils;

import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * Enum representing different portal types that can be intercepted.
 * This makes it easy to add new portal types in the future.
 * 
 * @author myztic_dev
 */
public enum PortalType {
    
    /**
     * End portals that teleport players to/from the End dimension
     */
    END_PORTAL("end", "end-portals", "End Portal", TeleportCause.END_PORTAL),
    
    /**
     * Nether portals that teleport players to/from the Nether dimension
     */
    NETHER_PORTAL("nether", "nether-portals", "Nether Portal", TeleportCause.NETHER_PORTAL),
    
    /**
     * Ender pearl teleportation (optional support)
     */
    ENDER_PEARL("pearl", "ender-pearls", "Ender Pearl", TeleportCause.ENDER_PEARL);
    
    private final String configKey;
    private final String configSectionKey;
    private final String displayName;
    private final TeleportCause teleportCause;
    
    /**
     * Constructor for PortalType enum
     * 
     * @param configKey The key used in configuration files
     * @param configSectionKey The section name used in config.yml
     * @param displayName The human-readable name for display
     * @param teleportCause The Bukkit TeleportCause associated with this portal type
     */
    PortalType(String configKey, String configSectionKey, String displayName, TeleportCause teleportCause) {
        this.configKey = configKey;
        this.configSectionKey = configSectionKey;
        this.displayName = displayName;
        this.teleportCause = teleportCause;
    }
    
    /**
     * Get the configuration key for this portal type
     * 
     * @return The configuration key
     */
    public String getConfigKey() {
        return configKey;
    }
    
    /**
     * Get the display name for this portal type
     * 
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the canonical section name used for interception and portal links.
     *
     * @return The configuration section name
     */
    public String getConfigSectionKey() {
        return configSectionKey;
    }

    /**
     * Get the section name written by versions up to 1.0.0.
     *
     * @return The legacy configuration section name
     */
    public String getLegacyConfigSectionKey() {
        return configKey + "-portals";
    }
    
    /**
     * Get the TeleportCause associated with this portal type
     * 
     * @return The TeleportCause
     */
    public TeleportCause getTeleportCause() {
        return teleportCause;
    }
    
    /**
     * Get the config path for interception setting
     * 
     * @return The config path for enabling/disabling interception
     */
    public String getInterceptPath() {
        return "intercept." + configSectionKey;
    }

    /**
     * Get the path containing world-to-world links for this type.
     *
     * @return The portal links configuration path
     */
    public String getPortalLinksPath() {
        return "portal-links." + configSectionKey;
    }
    
    
    
    /**
     * Find a PortalType by its config key
     * 
     * @param configKey The config key to search for
     * @return The matching PortalType, or null if not found
     */
    public static PortalType fromConfigKey(String configKey) {
        for (PortalType type : values()) {
            if (type.getConfigKey().equalsIgnoreCase(configKey)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * Find a PortalType by its TeleportCause
     * 
     * @param cause The TeleportCause to search for
     * @return The matching PortalType, or null if not found
     */
    public static PortalType fromTeleportCause(TeleportCause cause) {
        for (PortalType type : values()) {
            if (type.getTeleportCause() == cause) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * Get all valid config keys for tab completion
     * 
     * @return Array of all config keys
     */
    public static String[] getConfigKeys() {
        String[] keys = new String[values().length];
        for (int i = 0; i < values().length; i++) {
            keys[i] = values()[i].getConfigKey();
        }
        return keys;
    }
}
