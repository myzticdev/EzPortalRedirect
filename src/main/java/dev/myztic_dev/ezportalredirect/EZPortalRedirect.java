package dev.myztic_dev.ezportalredirect;

import dev.myztic_dev.ezportalredirect.commands.PortalCommand;
import dev.myztic_dev.ezportalredirect.listeners.PortalListener;
import dev.myztic_dev.ezportalredirect.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for EZPortalRedirect.
 * 
 * A comprehensive portal redirection plugin that intercepts portal usage
 * and redirects players to custom locations with granular control.
 * 
 * Features:
 * - Support for multiple portal types (End, Nether, Ender Pearl)
 * - Per-portal-type and per-world destination configuration
 * - Granular control over which portal types are intercepted
 * - Safety checks for destination validity
 * - Comprehensive command system with tab completion
 * - Compatible with Minecraft 1.13+ (Spigot/Paper)
 * 
 * @author myztic_dev
 * @version 1.0.0
 */
public class EZPortalRedirect extends JavaPlugin {
    
    private ConfigManager configManager;
    private PortalListener portalListener;
    private PortalCommand portalCommand;
    
    @Override
    public void onEnable() {
        // Print startup message
        getLogger().info("=== EZPortalRedirect v" + getDescription().getVersion() + " ===");
        getLogger().info("Starting portal redirection plugin...");
        
        // Initialize configuration manager
        try {
            configManager = new ConfigManager(this);
            getLogger().info("Configuration manager initialized successfully.");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize configuration manager: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize and register event listeners
        try {
            portalListener = new PortalListener(this, configManager);
            getServer().getPluginManager().registerEvents(portalListener, this);
            getLogger().info("Portal event listener registered successfully.");
        } catch (Exception e) {
            getLogger().severe("Failed to register portal listener: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize and register commands
        try {
            portalCommand = new PortalCommand(this, configManager);
            PluginCommand command = getCommand("portal");
            if (command != null) {
                command.setExecutor(portalCommand);
                command.setTabCompleter(portalCommand);
                getLogger().info("Portal commands registered successfully.");
            } else {
                getLogger().warning("Portal command not found in plugin.yml!");
            }
        } catch (Exception e) {
            getLogger().severe("Failed to register portal commands: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Version compatibility check
        performVersionCompatibilityCheck();
        
        // Print success message with configuration status
        getLogger().info("EZPortalRedirect enabled successfully!");
        if (configManager.isEnabled()) {
            getLogger().info("Portal redirection is ACTIVE.");
        } else {
            getLogger().warning("Portal redirection is DISABLED in configuration.");
        }
        
        // Print debug mode status
        if (configManager.isDebugMode()) {
            getLogger().info("Debug mode is ENABLED - detailed logging active.");
        }
        
        // Print supported portal types
        getLogger().info("Supported portal types: End Portal, Nether Portal, Ender Pearl");
        
        getLogger().info("Use '/portal help' for command information.");
        getLogger().info("=== EZPortalRedirect startup complete ===");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("=== EZPortalRedirect shutdown ===");
        
        // Unregister listeners
        if (portalListener != null) {
            try {
                // Listeners are automatically unregistered when plugin disables
                getLogger().info("Portal listeners unregistered.");
            } catch (Exception e) {
                getLogger().warning("Error during listener cleanup: " + e.getMessage());
            }
        }
        
        // Save configuration
        if (configManager != null) {
            try {
                saveConfig();
                getLogger().info("Configuration saved successfully.");
            } catch (Exception e) {
                getLogger().warning("Error saving configuration: " + e.getMessage());
            }
        }
        
        // Clear references
        configManager = null;
        portalListener = null;
        portalCommand = null;
        
        getLogger().info("EZPortalRedirect disabled successfully.");
        getLogger().info("=== Shutdown complete ===");
    }
    
    /**
     * Perform version compatibility checks and warnings
     */
    private void performVersionCompatibilityCheck() {
        String version = Bukkit.getVersion();
        String bukkitVersion = Bukkit.getBukkitVersion();
        
        getLogger().info("Running on: " + version);
        getLogger().info("Bukkit version: " + bukkitVersion);
        
        // Validate that required events are available
        try {
            Class.forName("org.bukkit.event.player.PlayerPortalEvent");
            getLogger().info("PlayerPortalEvent available - portal detection ready.");
        } catch (ClassNotFoundException e) {
            getLogger().severe("PlayerPortalEvent not found - this version is not supported!");
        }
    }
    
    /**
     * Get the configuration manager instance
     * 
     * @return The ConfigManager instance
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Get the portal listener instance
     * 
     * @return The PortalListener instance
     */
    public PortalListener getPortalListener() {
        return portalListener;
    }
    
    /**
     * Get the portal command handler instance
     * 
     * @return The PortalCommand instance
     */
    public PortalCommand getPortalCommand() {
        return portalCommand;
    }
    
    /**
     * Reload the plugin configuration and update all components
     */
    public void reloadPluginConfig() {
        getLogger().info("Reloading EZPortalRedirect configuration...");
        
        if (configManager != null) {
            configManager.loadConfig();
            getLogger().info("Configuration reloaded successfully.");
            
            if (configManager.isEnabled()) {
                getLogger().info("Portal redirection is ACTIVE.");
            } else {
                getLogger().info("Portal redirection is DISABLED in configuration.");
            }
        } else {
            getLogger().warning("ConfigManager is null - cannot reload configuration.");
        }
    }
}
