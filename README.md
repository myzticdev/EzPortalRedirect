# EZPortalRedirect

EZPortalRedirect redirects or blocks End portals, Nether portals, and Ender Pearls with per-world configuration. If a source world has no configured rule or link, normal Minecraft behavior continues.

See [ROADMAP.md](ROADMAP.md) for planned releases, feature priorities, and the
development workflow used by this project.

- [Source code](https://github.com/myzticdev/EzPortalRedirect)
- [Report a bug or request a feature](https://github.com/myzticdev/EzPortalRedirect/issues)

## Compatibility

- Target range: Bukkit-compatible Paper/Spigot servers from Minecraft 1.13.2 through current Paper 26.3 alpha
- Release JAR bytecode: Java 8
- Server Java: use the Java version required by your Paper release (Paper 1.20 through 1.21.11 uses Java 21; Paper 26.1+ uses Java 25)
- No NMS, CraftBukkit internals, or version-specific server classes

The build checks both ends of the supported API range: Spigot 1.13.2 for the release artifact and Paper 26.3 alpha for current-API source compatibility. Runtime testing on a real server is still recommended before production deployment.

## Installation

1. Build or download `EZPortalRedirect-1.0.1-alpha.1.jar`.
2. Put it in the server's `plugins` directory.
3. Start the server once to create `plugins/EZPortalRedirect/config.yml`.
4. Configure links with commands or edit the YAML file, then run `/portal reload`.

Existing 1.0.0 configuration files remain usable. The old `pearl-portals` key written by `/portal toggle pearl` is recognized and is replaced by the canonical `ender-pearls` key the next time that setting is changed.

## Commands

All commands require `portal.admin`, which defaults to server operators and grants the child permissions listed below.

| Command | Purpose | Permission |
| --- | --- | --- |
| `/portal link <type> <from-world> <to-world>` | Create or replace a world link | `portal.set` |
| `/portal unlink <type> <from-world>` | Remove a world link | `portal.set` |
| `/portal coords <world> <x> <y> <z>` | Set arrival coordinates for a world | `portal.set` |
| `/portal toggle <type>` | Toggle `end`, `nether`, or `pearl` interception | `portal.toggle` |
| `/portal status` | Show enabled types, links, and coordinates | `portal.status` |
| `/portal reload` | Reload `config.yml` | `portal.reload` |
| `/portal enable` | Enable all configured redirection | `portal.enable` |
| `/portal disable` | Disable all redirection | `portal.disable` |
| `/portal set <type> <world> <x> <y> <z>` | Link the player's current world and set coordinates | `portal.set` |
| `/portal setworld <type> <source> <dest> <x> <y> <z>` | Link explicit worlds and set coordinates | `portal.set` |

Example:

```text
/portal link end world custom_end
/portal coords custom_end 100 70 200
/portal toggle end
```

## Configuration

```yaml
enabled: true
teleport-message: '&aYou have been teleported to a custom location!'
blocked-message: '&cYou cannot use %type% in %world%!'
safety-checks: true
debug-mode: false

intercept:
  end-portals: false
  nether-portals: false
  ender-pearls: false

blocked-teleports:
  end-portals:
    - lobby
    - prison
  nether-portals:
    - creative
  ender-pearls:
    - lobby
    - spawn

portal-links:
  end-portals:
    world: custom_end
    custom_end: world
  nether-portals: {}
  ender-pearls: {}

coordinates:
  custom_end:
    x: 100
    y: 70
    z: 200
    yaw: 180.0
    pitch: 0.0
```

Links are directional. Add the reverse mapping if players should be able to return. When a linked destination has no `coordinates` entry, the plugin uses that world's spawn location.

Blocked-world rules are checked before redirection. A listed world prevents that teleport type even when its `intercept` setting is `false`; unlisted worlds retain vanilla behavior. The `%type%` and `%world%` placeholders are available in `blocked-message`. Players with `ezportalredirect.bypass` ignore all blocked-world rules.

Safety checks reject non-finite coordinates and Y values outside the destination world's supported build height. Modern negative-height worlds are detected without giving up compatibility with pre-1.17 servers.

## Building

Build the distributable cross-version JAR:

```bash
mvn clean package
```

The output is `target/EZPortalRedirect-1.0.1-alpha.1.jar`.

Compile and test against the current Paper API:

```bash
mvn -Ppaper-current clean test
```

Do not distribute an artifact packaged with the `paper-current` profile; that profile intentionally compiles with Java 25 and exists only as a latest-API compatibility check.

Run the automated MockBukkit integration suite:

```bash
mvn -Pmockbukkit clean test
```

On Windows, run real headless startup checks against Paper 1.13.2, 1.21.11, and 26.3 alpha after building the release JAR:

```powershell
.\scripts\Test-PaperMatrix.ps1
```

The Paper matrix uses the Java 8, 21, and 25 installations required by those representative server versions. It downloads temporary stable Paper builds, verifies that each server finishes startup and enables EZPortalRedirect, shuts each server down cleanly, and writes `target/paper-smoke-results.json`.

## Permissions

| Permission | Default |
| --- | --- |
| `portal.admin` | OP |
| `portal.toggle` | OP |
| `portal.set` | OP |
| `portal.reload` | OP |
| `portal.status` | OP |
| `portal.enable` | OP |
| `portal.disable` | OP |
| `ezportalredirect.bypass` | OP |

## Troubleshooting

- Run `/portal status` and confirm the plugin and desired portal type are enabled.
- World names are case-sensitive and destination worlds must already be loaded.
- Enable `debug-mode` and inspect the server log for link resolution details.
- Restart the server after replacing the plugin JAR. Paper's full server reload command is not safe for plugin updates.
