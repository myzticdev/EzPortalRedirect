package dev.myztic_dev.ezportalredirect;

import dev.myztic_dev.ezportalredirect.utils.PortalType;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EZPortalRedirectMockBukkitTest {

    private ServerMock server;
    private EZPortalRedirect plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EZPortalRedirect.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void loadsPluginAndRegistersPortalCommand() {
        assertTrue(plugin.isEnabled());
        assertNotNull(plugin.getConfigManager());
        assertNotNull(server.getPluginCommand("portal"));
    }

    @Test
    void adminCanCreatePortalLinkWithCommand() {
        WorldMock source = server.addSimpleWorld("command_source");
        server.addSimpleWorld("command_destination");
        PlayerMock player = server.addPlayer("Admin");
        player.setOp(true);
        player.teleport(source.getSpawnLocation());

        assertTrue(player.performCommand(
                "portal link end command_source command_destination"));
        assertEquals("command_destination",
                plugin.getConfigManager().getPortalLink(
                        PortalType.END_PORTAL, "command_source"));
    }

    @Test
    void endPortalRedirectsPlayerToConfiguredWorldAndCoordinates() {
        WorldMock source = server.addSimpleWorld("portal_source");
        WorldMock destination = server.addSimpleWorld("portal_destination");
        PlayerMock player = server.addPlayer("Traveler");
        player.teleport(source.getSpawnLocation());

        Location expected = new Location(destination, 12.5, 70.0, -8.5, 90.0f, 0.0f);
        plugin.getConfigManager().setPortalInterception(PortalType.END_PORTAL, true);
        plugin.getConfigManager().setPortalLink(
                PortalType.END_PORTAL, source.getName(), destination.getName());
        plugin.getConfigManager().setWorldCoordinates(destination.getName(), expected);

        PlayerPortalEvent event = new PlayerPortalEvent(
                player,
                player.getLocation(),
                destination.getSpawnLocation(),
                PlayerTeleportEvent.TeleportCause.END_PORTAL);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
        assertLocation(expected, player.getLocation());
    }

    @Test
    void portalWithoutLinkKeepsVanillaBehavior() {
        WorldMock source = server.addSimpleWorld("unlinked_source");
        WorldMock vanillaDestination = server.addSimpleWorld("vanilla_destination");
        PlayerMock player = server.addPlayer("VanillaTraveler");
        player.teleport(source.getSpawnLocation());

        PlayerPortalEvent event = new PlayerPortalEvent(
                player,
                player.getLocation(),
                vanillaDestination.getSpawnLocation(),
                PlayerTeleportEvent.TeleportCause.END_PORTAL);
        server.getPluginManager().callEvent(event);

        assertFalse(event.isCancelled());
        assertSame(vanillaDestination, event.getTo().getWorld());
    }

    @Test
    void blockedEndPortalCancelsTeleportEvenWhenInterceptionIsDisabled() {
        WorldMock source = server.addSimpleWorld("blocked_portal");
        WorldMock destination = server.addSimpleWorld("blocked_destination");
        PlayerMock player = server.addPlayer("BlockedTraveler");
        player.teleport(source.getSpawnLocation());
        plugin.getConfigManager().getConfig().set(
                "blocked-teleports.end-portals",
                Collections.singletonList(source.getName()));

        PlayerPortalEvent event = new PlayerPortalEvent(
                player,
                player.getLocation(),
                destination.getSpawnLocation(),
                PlayerTeleportEvent.TeleportCause.END_PORTAL);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
        assertSame(source, player.getWorld());
    }

    @Test
    void bypassPermissionAllowsBlockedPortal() {
        WorldMock source = server.addSimpleWorld("bypass_portal");
        WorldMock destination = server.addSimpleWorld("bypass_destination");
        PlayerMock player = server.addPlayer("BypassTraveler");
        player.teleport(source.getSpawnLocation());
        player.addAttachment(plugin, "ezportalredirect.bypass", true);
        plugin.getConfigManager().getConfig().set(
                "blocked-teleports.end-portals",
                Collections.singletonList(source.getName()));

        PlayerPortalEvent event = new PlayerPortalEvent(
                player,
                player.getLocation(),
                destination.getSpawnLocation(),
                PlayerTeleportEvent.TeleportCause.END_PORTAL);
        server.getPluginManager().callEvent(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void blockedEnderPearlCancelsTeleport() {
        WorldMock source = server.addSimpleWorld("blocked_pearls");
        PlayerMock player = server.addPlayer("BlockedPearlTraveler");
        player.teleport(source.getSpawnLocation());
        plugin.getConfigManager().getConfig().set(
                "blocked-teleports.ender-pearls",
                Collections.singletonList(source.getName()));

        PlayerTeleportEvent event = new PlayerTeleportEvent(
                player,
                player.getLocation(),
                source.getSpawnLocation(),
                PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void enderPearlEventReceivesConfiguredDestination() {
        WorldMock source = server.addSimpleWorld("pearl_source");
        WorldMock destination = server.addSimpleWorld("pearl_destination");
        PlayerMock player = server.addPlayer("PearlTraveler");
        player.teleport(source.getSpawnLocation());

        Location expected = new Location(destination, -20.0, 80.0, 35.0);
        plugin.getConfigManager().setPortalInterception(PortalType.ENDER_PEARL, true);
        plugin.getConfigManager().setPortalLink(
                PortalType.ENDER_PEARL, source.getName(), destination.getName());
        plugin.getConfigManager().setWorldCoordinates(destination.getName(), expected);

        PlayerTeleportEvent event = new PlayerTeleportEvent(
                player,
                player.getLocation(),
                source.getSpawnLocation(),
                PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
        server.getPluginManager().callEvent(event);

        assertLocation(expected, event.getTo());
    }

    private void assertLocation(Location expected, Location actual) {
        assertNotNull(actual);
        assertSame(expected.getWorld(), actual.getWorld());
        assertEquals(expected.getX(), actual.getX(), 0.001);
        assertEquals(expected.getY(), actual.getY(), 0.001);
        assertEquals(expected.getZ(), actual.getZ(), 0.001);
    }
}
