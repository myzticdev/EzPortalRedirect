package dev.myztic_dev.ezportalredirect.listeners;

import dev.myztic_dev.ezportalredirect.EZPortalRedirect;
import dev.myztic_dev.ezportalredirect.managers.ConfigManager;
import dev.myztic_dev.ezportalredirect.utils.PortalType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * Handles portal usage events and redirects players to custom destinations.
 * This listener intercepts portal events based on configuration settings.
 * 
 * @author myztic_dev
 */
public class PortalListener implements Listener {
    
    private final EZPortalRedirect plugin;
    private final ConfigManager configManager;
    
    public PortalListener(EZPortalRedirect plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    /**
     * Handle portal usage events (PlayerPortalEvent)
     * This event is fired when a player uses a portal (nether, end, etc.)
     * 
     * @param event The PlayerPortalEvent
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        TeleportCause cause = event.getCause();
        
        // Check if plugin is enabled
        if (!configManager.isEnabled()) {
            return;
        }
        
        // Get portal type from teleport cause
        PortalType portalType = PortalType.fromTeleportCause(cause);
        if (portalType == null) {
            // Unknown portal type, let vanilla behavior continue
            return;
        }

        String fromWorld = player.getWorld().getName();
        if (blockTeleportIfConfigured(event, player, portalType, fromWorld)) {
            return;
        }
        
        // Check if interception is enabled for this portal type
        if (!configManager.isPortalInterceptionEnabled(portalType)) {
            if (configManager.isDebugMode()) {
                plugin.getLogger().info("Portal interception disabled for " + portalType.getDisplayName() + 
                                      " (player: " + player.getName() + ")");
            }
            return;
        }
        
        // Get destination location
        Location destination = configManager.getDestination(portalType, fromWorld);
        if (destination == null) {
            // No configured link means vanilla portal behavior should continue.
            return;
        }
        
        // Cancel the original portal event
        event.setCancelled(true);
        
        // Log the portal redirection
        if (configManager.isDebugMode()) {
            plugin.getLogger().info("Redirecting " + player.getName() + " from " + 
                                  portalType.getDisplayName() + " to custom location: " + 
                                  destination.getWorld().getName() + " " + 
                                  destination.getX() + ", " + destination.getY() + ", " + destination.getZ());
        }
        
        // Teleport player to custom destination
        boolean teleported = player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
        if (!teleported) {
            plugin.getLogger().warning("Could not redirect " + player.getName() + " through "
                    + portalType.getDisplayName() + ". Another plugin may have cancelled the teleport.");
            return;
        }
        
        // Send teleport message
        String message = configManager.getTeleportMessage();
        if (message != null && !message.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }
    
    /**
     * Handle general teleport events for additional portal types
     * This catches teleport events that might not trigger PlayerPortalEvent
     * 
     * @param event The PlayerTeleportEvent
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        TeleportCause cause = event.getCause();
        
        // Check if plugin is enabled
        if (!configManager.isEnabled()) {
            return;
        }
        
        // Only handle specific teleport causes that aren't handled by PlayerPortalEvent
        if (cause != TeleportCause.ENDER_PEARL) {
            return;
        }
        
        // Get portal type from teleport cause
        PortalType portalType = PortalType.fromTeleportCause(cause);
        if (portalType == null) {
            return;
        }

        String fromWorld = player.getWorld().getName();
        if (blockTeleportIfConfigured(event, player, portalType, fromWorld)) {
            return;
        }
        
        // Check if interception is enabled for this portal type
        if (!configManager.isPortalInterceptionEnabled(portalType)) {
            if (configManager.isDebugMode()) {
                plugin.getLogger().info("Portal interception disabled for " + portalType.getDisplayName() + 
                                      " (player: " + player.getName() + ")");
            }
            return;
        }
        
        // Get destination location
        Location destination = configManager.getDestination(portalType, fromWorld);
        if (destination == null) {
            // No configured link means vanilla pearl behavior should continue.
            return;
        }
        
        // Update the teleport destination instead of cancelling
        event.setTo(destination);
        
        // Log the portal redirection
        if (configManager.isDebugMode()) {
            plugin.getLogger().info("Redirecting " + player.getName() + " from " + 
                                  portalType.getDisplayName() + " to custom location: " + 
                                  destination.getWorld().getName() + " " + 
                                  destination.getX() + ", " + destination.getY() + ", " + destination.getZ());
        }
        
        // Send teleport message (scheduled to avoid timing issues)
        final String message = configManager.getTeleportMessage();
        if (message != null && !message.isEmpty()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                }
            });
        }
    }

    private boolean blockTeleportIfConfigured(PlayerTeleportEvent event, Player player,
                                              PortalType portalType, String worldName) {
        if (!configManager.isTeleportBlocked(portalType, worldName)
                || player.hasPermission("ezportalredirect.bypass")) {
            return false;
        }

        event.setCancelled(true);
        if (configManager.isDebugMode()) {
            plugin.getLogger().info("Blocked " + portalType.getDisplayName() + " for "
                    + player.getName() + " in world " + worldName);
        }

        String message = configManager.getBlockedMessage();
        if (message != null && !message.isEmpty()) {
            message = message.replace("%type%", portalType.getDisplayName())
                    .replace("%world%", worldName);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
        return true;
    }
}
