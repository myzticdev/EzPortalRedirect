package dev.myztic_dev.ezportalredirect;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ResourceConfigurationTest {

    @Test
    public void pluginDescriptorContainsFilteredReleaseMetadata() {
        YamlConfiguration descriptor = loadYaml("plugin.yml");

        assertEquals("EZPortalRedirect", descriptor.getString("name"));
        assertEquals("1.0.0", descriptor.getString("version"));
        assertEquals("myztic_dev", descriptor.getString("author"));
        assertEquals("1.13", descriptor.getString("api-version"));
        assertEquals("dev.myztic_dev.ezportalredirect.EZPortalRedirect", descriptor.getString("main"));
        assertEquals("op", descriptor.getString("permissions.ezportalredirect.bypass.default"));
    }

    @Test
    public void defaultConfigUsesCanonicalSafeDefaults() {
        YamlConfiguration config = loadYaml("config.yml");

        assertTrue(config.getBoolean("enabled"));
        assertFalse(config.getBoolean("intercept.end-portals"));
        assertFalse(config.getBoolean("intercept.nether-portals"));
        assertFalse(config.getBoolean("intercept.ender-pearls"));
        assertTrue(config.getStringList("blocked-teleports.end-portals").isEmpty());
        assertTrue(config.getStringList("blocked-teleports.nether-portals").isEmpty());
        assertTrue(config.getStringList("blocked-teleports.ender-pearls").isEmpty());
        assertEquals("&cYou cannot use %type% in %world%!", config.getString("blocked-message"));
        assertTrue(config.getConfigurationSection("portal-links.end-portals").getKeys(false).isEmpty());
        assertTrue(config.getConfigurationSection("portal-links.ender-pearls").getKeys(false).isEmpty());
    }

    private YamlConfiguration loadYaml(String resourceName) {
        InputStream resource = getClass().getClassLoader().getResourceAsStream(resourceName);
        assertNotNull("Missing test resource: " + resourceName, resource);
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(resource, StandardCharsets.UTF_8));
    }
}
