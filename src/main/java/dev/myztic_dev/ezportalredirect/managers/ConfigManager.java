package dev.myztic_dev.ezportalredirect.managers;

import dev.myztic_dev.ezportalredirect.EZPortalRedirect;
import dev.myztic_dev.ezportalredirect.utils.PortalType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Manages all configuration operations for the EZPortalRedirect plugin.
 * Handles loading, saving, and accessing configuration values.
 * 
 * @author myztic_dev
 */
public class ConfigManager {
    
    private final EZPortalRedirect plugin;
    private FileConfiguration config;
    
    public ConfigManager(EZPortalRedirect plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        loadConfig();
    }
    
    /**
     * Load or reload the configuration from file
     */
    public void loadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // Set minimal defaults only if config is empty
        if (config.getKeys(false).isEmpty()) {
            setMinimalDefaults();
            plugin.saveConfig();
        }
    }
    
    /**
     * Set minimal default configuration values
     */
    private void setMinimalDefaults() {
        config.set("enabled", true);
        config.set("teleport-message", "&aYou have been teleported to a custom location!");
        config.set("blocked-message", "&cYou cannot use %type% in %world%!");
        config.set("safety-checks", true);
        config.set("debug-mode", false);
        
        config.set("intercept.end-portals", false);
        config.set("intercept.nether-portals", false);
        config.set("intercept.ender-pearls", false);

        config.set("blocked-teleports.end-portals", Collections.emptyList());
        config.set("blocked-teleports.nether-portals", Collections.emptyList());
        config.set("blocked-teleports.ender-pearls", Collections.emptyList());
        
        config.createSection("portal-links.end-portals");
        config.createSection("portal-links.nether-portals");
        config.createSection("portal-links.ender-pearls");
        config.createSection("coordinates");
    }
    
    /**
     * Check if the plugin is enabled
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return config.getBoolean("enabled", true);
    }
    
    /**
     * Set plugin enabled state
     * 
     * @param enabled Whether the plugin should be enabled
     */
    public void setEnabled(boolean enabled) {
        config.set("enabled", enabled);
        plugin.saveConfig();
    }
    
    /**
     * Check if portal interception is enabled for a specific portal type
     * 
     * @param portalType The portal type to check
     * @return true if interception is enabled, false otherwise
     */
    public boolean isPortalInterceptionEnabled(PortalType portalType) {
        String legacyPath = "intercept." + portalType.getLegacyConfigSectionKey();
        boolean enabled = config.getBoolean(portalType.getInterceptPath(), false);

        // 1.0.0 wrote intercept.pearl-portals when /portal toggle pearl was used.
        if (!legacyPath.equals(portalType.getInterceptPath()) && config.isSet(legacyPath)) {
            enabled = config.getBoolean(legacyPath, enabled);
        }

        return isEnabled() && enabled;
    }
    
    /**
     * Set portal interception status for a specific portal type
     * 
     * @param portalType The portal type
     * @param enabled Whether interception should be enabled
     */
    public void setPortalInterception(PortalType portalType, boolean enabled) {
        config.set(portalType.getInterceptPath(), enabled);
        String legacyPath = "intercept." + portalType.getLegacyConfigSectionKey();
        if (!legacyPath.equals(portalType.getInterceptPath())) {
            config.set(legacyPath, null);
        }
        plugin.saveConfig();
    }
    
    /**
     * Get teleport message
     * 
     * @return The teleport message with color codes
     */
    public String getTeleportMessage() {
        return config.getString("teleport-message", "&aYou have been teleported to a custom location!");
    }

    /**
     * Get the message sent when a teleport type is blocked in a world.
     *
     * @return The blocked message with color codes and placeholders
     */
    public String getBlockedMessage() {
        return config.getString("blocked-message", "&cYou cannot use %type% in %world%!");
    }

    /**
     * Check whether a teleport type is blocked in a source world.
     * World names are compared case-insensitively for configuration usability.
     *
     * @param portalType The teleport type
     * @param worldName The source world name
     * @return true when this type is blocked in the world
     */
    public boolean isTeleportBlocked(PortalType portalType, String worldName) {
        for (String blockedWorld : getBlockedWorlds(portalType)) {
            if (blockedWorld.equalsIgnoreCase(worldName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the configured blocked worlds for a teleport type.
     *
     * @param portalType The teleport type
     * @return A copy of the configured world-name list
     */
    public List<String> getBlockedWorlds(PortalType portalType) {
        return config.getStringList("blocked-teleports." + portalType.getConfigSectionKey());
    }
    
    /**
     * Check if safety checks are enabled
     * 
     * @return true if safety checks are enabled
     */
    public boolean areSafetyChecksEnabled() {
        return config.getBoolean("safety-checks", true);
    }
    
    /**
     * Check if debug mode is enabled
     * 
     * @return true if debug mode is enabled
     */
    public boolean isDebugMode() {
        return config.getBoolean("debug-mode", false);
    }
    
    /**
     * Get destination location for a portal type from a specific world
     * LOGIC: portal-links -> coordinates -> world spawn (if no link found, use vanilla behavior)
     * 
     * @param portalType The portal type
     * @param fromWorld The world the player is teleporting from
     * @return The destination location, or null if not found/invalid
     */
    public Location getDestination(PortalType portalType, String fromWorld) {
        // Check portal-links for world-to-world mapping
        String destinationWorld = getPortalLink(portalType, fromWorld);
        
        if (destinationWorld != null) {
            // Look up coordinates in coordinates section (falls back to world spawn)
            Location destination = getWorldCoordinates(destinationWorld);
            
            if (destination != null && isDebugMode()) {
                plugin.getLogger().info("Using portal link: " + fromWorld + " -> " + destinationWorld + 
                                      " for " + portalType.getDisplayName());
            }
            return destination;
        }
        
        // No portal link found - return null to use vanilla behavior
        if (isDebugMode()) {
            plugin.getLogger().info("No portal link configured for " + portalType.getDisplayName() + 
                                  " from world " + fromWorld + " - using vanilla behavior");
        }
        return null;
    }
    
    /**
     * Get portal link mapping for a specific portal type and source world
     * 
     * @param portalType The portal type
     * @param fromWorld The source world
     * @return The destination world name, or null if no link exists
     */
    public String getPortalLink(PortalType portalType, String fromWorld) {
        String destination = config.getString(portalType.getPortalLinksPath() + "." + fromWorld);
        if (destination != null) {
            return destination;
        }

        return config.getString(getLegacyPortalLinksPath(portalType) + "." + fromWorld);
    }
    
    /**
     * Set portal link mapping
     * 
     * @param portalType The portal type
     * @param fromWorld The source world
     * @param toWorld The destination world
     */
    public void setPortalLink(PortalType portalType, String fromWorld, String toWorld) {
        config.set(portalType.getPortalLinksPath() + "." + fromWorld, toWorld);
        if (!getLegacyPortalLinksPath(portalType).equals(portalType.getPortalLinksPath())) {
            config.set(getLegacyPortalLinksPath(portalType) + "." + fromWorld, null);
        }
        plugin.saveConfig();
    }
    
    /**
     * Remove portal link mapping
     * 
     * @param portalType The portal type
     * @param fromWorld The source world
     */
    public void removePortalLink(PortalType portalType, String fromWorld) {
        config.set(portalType.getPortalLinksPath() + "." + fromWorld, null);
        config.set(getLegacyPortalLinksPath(portalType) + "." + fromWorld, null);
        plugin.saveConfig();
    }
    
    /**
     * Get coordinates for a specific world
     * 
     * @param worldName The world name
     * @return The location with coordinates, or world spawn if not configured
     */
    public Location getWorldCoordinates(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World '" + worldName + "' not found!");
            return null;
        }
        
        // Check if custom coordinates are configured
        String coordPath = "coordinates." + worldName;
        if (config.contains(coordPath)) {
            return getLocationFromConfig(coordPath);
        }
        
        // Fall back to world spawn
        Location spawn = world.getSpawnLocation();
        if (isDebugMode()) {
            plugin.getLogger().info("Using world spawn for " + worldName + 
                                  " (no custom coordinates configured)");
        }
        return spawn;
    }
    
    /**
     * Set custom coordinates for a world
     * 
     * @param worldName The world name
     * @param location The location with coordinates
     */
    public void setWorldCoordinates(String worldName, Location location) {
        String path = "coordinates." + worldName;
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
        plugin.saveConfig();
    }
    
    /**
     * Get a location from configuration section
     * 
     * @param path The configuration path
     * @return The location, or null if invalid
     */
    private Location getLocationFromConfig(String path) {
        if (!config.contains(path)) {
            return null;
        }
        
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return null;
        }
        
        try {
            double x = section.getDouble("x");
            double y = section.getDouble("y");
            double z = section.getDouble("z");
            float yaw = (float) section.getDouble("yaw", 0.0);
            float pitch = (float) section.getDouble("pitch", 0.0);
            
            // Extract world name from the path
            String worldName = path.substring(path.lastIndexOf('.') + 1);
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("World '" + worldName + "' not found for coordinates at " + path);
                return null;
            }
            
            Location location = new Location(world, x, y, z, yaw, pitch);
            
            // Safety check
            if (areSafetyChecksEnabled() && !isSafeLocation(location)) {
                plugin.getLogger().warning("Unsafe destination location at " + path + ": " + 
                                         location.getX() + ", " + location.getY() + ", " + location.getZ());
                return null;
            }
            
            return location;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error parsing location from config at " + path, e);
            return null;
        }
    }
    
    /**
     * Check if a location is safe for teleportation
     * 
     * @param location The location to check
     * @return true if the location is safe
     */
    private boolean isSafeLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        
        World world = location.getWorld();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        if (Double.isNaN(x) || Double.isInfinite(x)
                || Double.isNaN(y) || Double.isInfinite(y)
                || Double.isNaN(z) || Double.isInfinite(z)) {
            return false;
        }

        return y >= getMinHeight(world) && y < world.getMaxHeight();
    }
    
    /**
     * Get all portal links for a specific portal type
     * 
     * @param portalType The portal type
     * @return Map of source_world -> destination_world
     */
    public java.util.Map<String, String> getPortalLinks(PortalType portalType) {
        java.util.Map<String, String> links = new java.util.HashMap<>();
        addPortalLinks(links, getLegacyPortalLinksPath(portalType));
        addPortalLinks(links, portalType.getPortalLinksPath());
        
        return links;
    }
    
    /**
     * Check if custom coordinates are configured for a world
     * 
     * @param worldName The world name
     * @return true if custom coordinates exist
     */
    public boolean hasCustomCoordinates(String worldName) {
        return config.contains("coordinates." + worldName);
    }
    
    /**
     * Get all worlds that have custom coordinates configured
     * 
     * @return Set of world names with custom coordinates
     */
    public java.util.Set<String> getWorldsWithCustomCoordinates() {
        java.util.Set<String> worlds = new java.util.HashSet<>();
        ConfigurationSection section = config.getConfigurationSection("coordinates");
        if (section != null) {
            worlds.addAll(section.getKeys(false));
        }
        return worlds;
    }
    
    /**
     * Check if any portal links exist for a portal type
     * 
     * @param portalType The portal type
     * @return true if at least one link exists
     */
    public boolean hasPortalLinks(PortalType portalType) {
        return hasPortalLinksAt(portalType.getPortalLinksPath())
                || hasPortalLinksAt(getLegacyPortalLinksPath(portalType));
    }

    private String getLegacyPortalLinksPath(PortalType portalType) {
        return "portal-links." + portalType.getLegacyConfigSectionKey();
    }

    private void addPortalLinks(java.util.Map<String, String> links, String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return;
        }

        for (String fromWorld : section.getKeys(false)) {
            String toWorld = section.getString(fromWorld);
            if (toWorld != null) {
                links.put(fromWorld, toWorld);
            }
        }
    }

    private boolean hasPortalLinksAt(String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        return section != null && !section.getKeys(false).isEmpty();
    }

    private int getMinHeight(World world) {
        try {
            return (Integer) world.getClass().getMethod("getMinHeight").invoke(world);
        } catch (ReflectiveOperationException | ClassCastException e) {
            // World#getMinHeight was added after the 1.13 compatibility floor.
            return 0;
        }
    }
    
    /**
     * Get the raw configuration object
     * 
     * @return The FileConfiguration object
     */
    public FileConfiguration getConfig() {
        return config;
    }
}
