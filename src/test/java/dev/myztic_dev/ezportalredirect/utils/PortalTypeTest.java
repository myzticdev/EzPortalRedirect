package dev.myztic_dev.ezportalredirect.utils;

import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PortalTypeTest {

    @Test
    public void usesCanonicalConfigurationPaths() {
        assertEquals("intercept.end-portals", PortalType.END_PORTAL.getInterceptPath());
        assertEquals("portal-links.nether-portals", PortalType.NETHER_PORTAL.getPortalLinksPath());
        assertEquals("intercept.ender-pearls", PortalType.ENDER_PEARL.getInterceptPath());
        assertEquals("portal-links.ender-pearls", PortalType.ENDER_PEARL.getPortalLinksPath());
        assertEquals("pearl-portals", PortalType.ENDER_PEARL.getLegacyConfigSectionKey());
    }

    @Test
    public void resolvesCommandKeysAndTeleportCauses() {
        assertEquals(PortalType.END_PORTAL, PortalType.fromConfigKey("END"));
        assertEquals(PortalType.NETHER_PORTAL, PortalType.fromTeleportCause(TeleportCause.NETHER_PORTAL));
        assertEquals(PortalType.ENDER_PEARL, PortalType.fromTeleportCause(TeleportCause.ENDER_PEARL));
        assertNull(PortalType.fromConfigKey("unknown"));
    }
}
