package dev.myztic_dev.ezportalredirect.commands;

import dev.myztic_dev.ezportalredirect.EZPortalRedirect;
import dev.myztic_dev.ezportalredirect.managers.ConfigManager;
import dev.myztic_dev.ezportalredirect.utils.PortalType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all portal-related commands with tab completion.
 * Provides a comprehensive command interface for managing portal redirection.
 * 
 * @author myztic_dev
 */
public class PortalCommand implements CommandExecutor, TabCompleter {
    
    private final ConfigManager configManager;
    
    // Command constants
    private static final String[] SUB_COMMANDS = {
        "toggle", "set", "setworld", "reload", "status", "enable", "disable", 
        "link", "unlink", "coords", "help"
    };
    
    public PortalCommand(EZPortalRedirect plugin, ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check basic permission
        if (!sender.hasPermission("portal.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        // Show help if no arguments
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "toggle":
                return handleToggle(sender, args);
            case "set":
                return handleSet(sender, args);
            case "setworld":
                return handleSetWorld(sender, args);
            case "reload":
                return handleReload(sender);
            case "status":
                return handleStatus(sender);
            case "enable":
                return handleEnable(sender);
            case "disable":
                return handleDisable(sender);
            case "link":
                return handleLink(sender, args);
            case "unlink":
                return handleUnlink(sender, args);
            case "coords":
                return handleCoords(sender, args);
            case "help":
                showHelp(sender);
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /portal help for help.");
                return true;
        }
    }
    
    /**
     * Handle the toggle subcommand
     */
    private boolean handleToggle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("portal.toggle")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to toggle portal interception.");
            return true;
        }
        
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /portal toggle <portal-type>");
            sender.sendMessage(ChatColor.YELLOW + "Portal types: " + String.join(", ", PortalType.getConfigKeys()));
            return true;
        }
        
        PortalType portalType = PortalType.fromConfigKey(args[1]);
        if (portalType == null) {
            sender.sendMessage(ChatColor.RED + "Invalid portal type. Valid types: " + 
                             String.join(", ", PortalType.getConfigKeys()));
            return true;
        }
        
        boolean currentState = configManager.isPortalInterceptionEnabled(portalType);
        boolean newState = !currentState;
        
        configManager.setPortalInterception(portalType, newState);
        
        String status = newState ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled";
        sender.sendMessage(ChatColor.YELLOW + "Portal interception for " + 
                         portalType.getDisplayName() + " is now " + status + ChatColor.YELLOW + ".");
        
        return true;
    }
    
    /**
     * Handle the set subcommand
     */
    private boolean handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("portal.set")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to set portal destinations.");
            return true;
        }
        
        if (args.length != 6) {
            sender.sendMessage(ChatColor.RED + "Usage: /portal set <portal-type> <world> <x> <y> <z>");
            return true;
        }
        
        return setDestination(sender, args);
    }
    
    /**
     * Handle the setworld subcommand
     */
    private boolean handleSetWorld(CommandSender sender, String[] args) {
        if (!sender.hasPermission("portal.set")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to set portal destinations.");
            return true;
        }
        
        if (args.length != 7) {
            sender.sendMessage(ChatColor.RED + "Usage: /portal setworld <portal-type> <sourceWorld> <destWorld> <x> <y> <z>");
            return true;
        }
        
        return setWorldDestination(sender, args);
    }
    
    /**
     * Set destination for portal type
     */
    private boolean setDestination(CommandSender sender, String[] args) {
        PortalType portalType = PortalType.fromConfigKey(args[1]);
        if (portalType == null) {
            sender.sendMessage(ChatColor.RED + "Invalid portal type. Valid types: " + 
                             String.join(", ", PortalType.getConfigKeys()));
            return true;
        }
        
        String worldName = args[2];
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "World '" + worldName + "' not found.");
            return true;
        }
        
        try {
            double x = Double.parseDouble(args[3]);
            double y = Double.parseDouble(args[4]);
            double z = Double.parseDouble(args[5]);
            
            // Use current player's yaw and pitch if sender is a player
            float yaw = 0.0f;
            float pitch = 0.0f;
            if (sender instanceof Player) {
                Player player = (Player) sender;
                yaw = player.getLocation().getYaw();
                pitch = player.getLocation().getPitch();
            }
            
            Location destination = new Location(world, x, y, z, yaw, pitch);
            
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You must specify a source world when using this command from console.");
                return true;
            }
            Player player = (Player) sender;
            String fromWorld = player.getWorld().getName();

            configManager.setPortalLink(portalType, fromWorld, worldName);
            configManager.setWorldCoordinates(worldName, destination);

            sender.sendMessage(ChatColor.GREEN + "Portal link created: " + fromWorld + " -> " + worldName);
            sender.sendMessage(ChatColor.GREEN + "Coordinates set for " + worldName +
                             ": " + x + ", " + y + ", " + z);
            
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid coordinates. Please use numbers.");
        }
        
        return true;
    }
    
    /**
     * Set world-specific destination
     */
    private boolean setWorldDestination(CommandSender sender, String[] args) {
        PortalType portalType = PortalType.fromConfigKey(args[1]);
        if (portalType == null) {
            sender.sendMessage(ChatColor.RED + "Invalid portal type. Valid types: " + 
                             String.join(", ", PortalType.getConfigKeys()));
            return true;
        }
        
        String sourceWorldName = args[2];
        String destWorldName = args[3];
        
        World destWorld = Bukkit.getWorld(destWorldName);
        if (destWorld == null) {
            sender.sendMessage(ChatColor.RED + "Destination world '" + destWorldName + "' not found.");
            return true;
        }
        
        try {
            double x = Double.parseDouble(args[4]);
            double y = Double.parseDouble(args[5]);
            double z = Double.parseDouble(args[6]);
            
            Location destination = new Location(destWorld, x, y, z);
            
            // Use new structure: create portal link + set coordinates
            configManager.setPortalLink(portalType, sourceWorldName, destWorldName);
            configManager.setWorldCoordinates(destWorldName, destination);
            
            sender.sendMessage(ChatColor.GREEN + "Portal link created: " + sourceWorldName + " -> " + destWorldName);
            sender.sendMessage(ChatColor.GREEN + "Coordinates set for " + destWorldName + 
                             ": " + x + ", " + y + ", " + z);
            sender.sendMessage(ChatColor.YELLOW + "Note: Consider using '/portal link' and '/portal coords' commands for the new intuitive interface!");
            
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid coordinates. Please use numbers.");
        }
        
        return true;
    }
    
    /**
     * Handle the reload subcommand
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("portal.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to reload the configuration.");
            return true;
        }
        
        configManager.loadConfig();
        sender.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully.");
        return true;
    }
    
    /**
     * Handle the status subcommand
     */
    private boolean handleStatus(CommandSender sender) {
        if (!sender.hasPermission("portal.status")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to view portal status.");
            return true;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "=== EZPortalRedirect Status ===");
        sender.sendMessage(ChatColor.YELLOW + "Plugin Enabled: " + 
                         (configManager.isEnabled() ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
        
        sender.sendMessage(ChatColor.YELLOW + "Portal Interception Status:");
        for (PortalType type : PortalType.values()) {
            boolean enabled = configManager.isPortalInterceptionEnabled(type);
            String status = enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled";
            sender.sendMessage(ChatColor.YELLOW + "  " + type.getDisplayName() + ": " + status);
        }

        sender.sendMessage(ChatColor.YELLOW + "Blocked Worlds:");
        for (PortalType type : PortalType.values()) {
            java.util.List<String> blockedWorlds = configManager.getBlockedWorlds(type);
            String worlds = blockedWorlds.isEmpty()
                    ? ChatColor.GRAY + "None"
                    : ChatColor.RED + String.join(", ", blockedWorlds);
            sender.sendMessage(ChatColor.YELLOW + "  " + type.getDisplayName() + ": " + worlds);
        }
        
        // Show portal links
        sender.sendMessage(ChatColor.YELLOW + "Portal Links:");
        for (PortalType type : PortalType.values()) {
            java.util.Map<String, String> links = configManager.getPortalLinks(type);
            if (links.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "  " + type.getDisplayName() + ": " + ChatColor.GRAY + "No links configured");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "  " + type.getDisplayName() + ":");
                for (java.util.Map.Entry<String, String> entry : links.entrySet()) {
                    sender.sendMessage(ChatColor.YELLOW + "    " + entry.getKey() + " -> " + entry.getValue());
                }
            }
        }
        
        // Show worlds with custom coordinates
        java.util.Set<String> worldsWithCoords = configManager.getWorldsWithCustomCoordinates();
        if (!worldsWithCoords.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Worlds with Custom Coordinates:");
            for (String world : worldsWithCoords) {
                Location coords = configManager.getWorldCoordinates(world);
                if (coords != null) {
                    sender.sendMessage(ChatColor.YELLOW + "  " + world + ": " + 
                                     coords.getX() + ", " + coords.getY() + ", " + coords.getZ());
                }
            }
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Custom Coordinates: " + ChatColor.GRAY + "None configured");
        }
        
        return true;
    }
    
    /**
     * Handle the enable subcommand
     */
    private boolean handleEnable(CommandSender sender) {
        if (!sender.hasPermission("portal.enable")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to enable the plugin.");
            return true;
        }
        
        configManager.setEnabled(true);
        sender.sendMessage(ChatColor.GREEN + "EZPortalRedirect has been enabled.");
        return true;
    }
    
    /**
     * Handle the disable subcommand
     */
    private boolean handleDisable(CommandSender sender) {
        if (!sender.hasPermission("portal.disable")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to disable the plugin.");
            return true;
        }
        
        configManager.setEnabled(false);
        sender.sendMessage(ChatColor.RED + "EZPortalRedirect has been disabled.");
        return true;
    }
    
    /**
     * Handle the link subcommand - create world-to-world portal link
     */
    private boolean handleLink(CommandSender sender, String[] args) {
        if (!sender.hasPermission("portal.set")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to create portal links.");
            return true;
        }
        
        if (args.length != 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /portal link <portal-type> <from-world> <to-world>");
            sender.sendMessage(ChatColor.YELLOW + "Portal types: " + String.join(", ", PortalType.getConfigKeys()));
            return true;
        }
        
        PortalType portalType = PortalType.fromConfigKey(args[1]);
        if (portalType == null) {
            sender.sendMessage(ChatColor.RED + "Invalid portal type. Valid types: " + 
                             String.join(", ", PortalType.getConfigKeys()));
            return true;
        }
        
        String fromWorld = args[2];
        String toWorld = args[3];
        
        // Verify worlds exist
        if (Bukkit.getWorld(fromWorld) == null) {
            sender.sendMessage(ChatColor.RED + "Source world '" + fromWorld + "' not found.");
            return true;
        }
        
        if (Bukkit.getWorld(toWorld) == null) {
            sender.sendMessage(ChatColor.RED + "Destination world '" + toWorld + "' not found.");
            return true;
        }
        
        configManager.setPortalLink(portalType, fromWorld, toWorld);
        sender.sendMessage(ChatColor.GREEN + "Portal link created: " + fromWorld + " -> " + toWorld + 
                         " for " + portalType.getDisplayName());
        
        return true;
    }
    
    /**
     * Handle the unlink subcommand - remove world-to-world portal link
     */
    private boolean handleUnlink(CommandSender sender, String[] args) {
        if (!sender.hasPermission("portal.set")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to remove portal links.");
            return true;
        }
        
        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /portal unlink <portal-type> <from-world>");
            sender.sendMessage(ChatColor.YELLOW + "Portal types: " + String.join(", ", PortalType.getConfigKeys()));
            return true;
        }
        
        PortalType portalType = PortalType.fromConfigKey(args[1]);
        if (portalType == null) {
            sender.sendMessage(ChatColor.RED + "Invalid portal type. Valid types: " + 
                             String.join(", ", PortalType.getConfigKeys()));
            return true;
        }
        
        String fromWorld = args[2];
        String existingLink = configManager.getPortalLink(portalType, fromWorld);
        
        if (existingLink == null) {
            sender.sendMessage(ChatColor.RED + "No portal link found for " + portalType.getDisplayName() + 
                             " from world " + fromWorld);
            return true;
        }
        
        configManager.removePortalLink(portalType, fromWorld);
        sender.sendMessage(ChatColor.GREEN + "Portal link removed: " + fromWorld + " -> " + existingLink + 
                         " for " + portalType.getDisplayName());
        
        return true;
    }
    
    /**
     * Handle the coords subcommand - set world coordinates
     */
    private boolean handleCoords(CommandSender sender, String[] args) {
        if (!sender.hasPermission("portal.set")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to set world coordinates.");
            return true;
        }
        
        if (args.length != 5) {
            sender.sendMessage(ChatColor.RED + "Usage: /portal coords <world> <x> <y> <z>");
            return true;
        }
        
        String worldName = args[1];
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "World '" + worldName + "' not found.");
            return true;
        }
        
        try {
            double x = Double.parseDouble(args[2]);
            double y = Double.parseDouble(args[3]);
            double z = Double.parseDouble(args[4]);
            
            // Use current player's yaw and pitch if sender is a player
            float yaw = 0.0f;
            float pitch = 0.0f;
            if (sender instanceof Player) {
                Player player = (Player) sender;
                yaw = player.getLocation().getYaw();
                pitch = player.getLocation().getPitch();
            }
            
            Location location = new Location(world, x, y, z, yaw, pitch);
            configManager.setWorldCoordinates(worldName, location);
            
            sender.sendMessage(ChatColor.GREEN + "Coordinates set for world " + worldName + 
                             ": " + x + ", " + y + ", " + z);
            
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid coordinates. Please use numbers.");
        }
        
        return true;
    }
    
    /**
     * Show help message
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "=== EZPortalRedirect Commands ===");
        sender.sendMessage(ChatColor.GOLD + "New Intuitive Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/portal link <type> <from> <to>" + ChatColor.WHITE + " - Create world-to-world portal link");
        sender.sendMessage(ChatColor.YELLOW + "/portal unlink <type> <from>" + ChatColor.WHITE + " - Remove portal link");
        sender.sendMessage(ChatColor.YELLOW + "/portal coords <world> <x> <y> <z>" + ChatColor.WHITE + " - Set world coordinates");
        sender.sendMessage(ChatColor.YELLOW + "/portal status" + ChatColor.WHITE + " - Show links, coordinates, and status");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "Legacy Commands (still supported):");
        sender.sendMessage(ChatColor.YELLOW + "/portal set <type> <world> <x> <y> <z>" + ChatColor.WHITE + " - Set destination (current world)");
        sender.sendMessage(ChatColor.YELLOW + "/portal setworld <type> <source> <dest> <x> <y> <z>" + ChatColor.WHITE + " - Set world-specific destination");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "Management Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/portal toggle <type>" + ChatColor.WHITE + " - Toggle portal interception");
        sender.sendMessage(ChatColor.YELLOW + "/portal enable/disable" + ChatColor.WHITE + " - Enable/disable plugin");
        sender.sendMessage(ChatColor.YELLOW + "/portal reload" + ChatColor.WHITE + " - Reload configuration");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "Portal types: " + ChatColor.WHITE + String.join(", ", PortalType.getConfigKeys()));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GREEN + "Examples:");
        sender.sendMessage(ChatColor.WHITE + "/portal link end world my_custom_end");
        sender.sendMessage(ChatColor.WHITE + "/portal coords my_custom_end 100 70 200");
        sender.sendMessage(ChatColor.WHITE + "/portal toggle end");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("portal.admin")) {
            return completions;
        }
        
        if (args.length == 1) {
            // Complete subcommands
            for (String subCommand : SUB_COMMANDS) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("toggle") || subCommand.equals("set") || 
                subCommand.equals("setworld") ||
                subCommand.equals("link") || subCommand.equals("unlink")) {
                // Complete portal types
                for (String portalType : PortalType.getConfigKeys()) {
                    if (portalType.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(portalType);
                    }
                }
            } else if (subCommand.equals("coords")) {
                // Complete world names for coords command
                for (World world : Bukkit.getWorlds()) {
                    String worldName = world.getName();
                    if (worldName.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(worldName);
                    }
                }
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("set") || subCommand.equals("setworld")) {
                // Complete world names
                for (World world : Bukkit.getWorlds()) {
                    String worldName = world.getName();
                    if (worldName.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(worldName);
                    }
                }
            } else if (subCommand.equals("link") || subCommand.equals("unlink")) {
                // Complete source world names for link/unlink commands
                for (World world : Bukkit.getWorlds()) {
                    String worldName = world.getName();
                    if (worldName.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(worldName);
                    }
                }
            }
        } else if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("setworld") || subCommand.equals("link")) {
                // Complete destination world names for setworld/link commands
                for (World world : Bukkit.getWorlds()) {
                    String worldName = world.getName();
                    if (worldName.toLowerCase().startsWith(args[3].toLowerCase())) {
                        completions.add(worldName);
                    }
                }
            }
        }
        
        return completions;
    }
}
