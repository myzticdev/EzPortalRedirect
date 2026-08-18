# EZPortalRedirect Roadmap

This is the living development plan for EZPortalRedirect. Use it to decide what
belongs in each release, prevent unnecessary scope growth, and keep implementation,
tests, configuration, and documentation moving together.

Last reviewed: 2026-08-17

## Product direction

EZPortalRedirect is a lightweight plugin for redirecting or blocking vanilla End
portals, Nether portals, and Ender Pearls per world. Its advantage is simple
configuration, safe defaults, and broad Bukkit-compatible server support—not an
attempt to replace custom-portal builders or full world-management suites.

New work should normally satisfy all of these rules:

- It directly improves vanilla portal or Ender Pearl control.
- It remains optional and preserves vanilla behavior when not configured.
- It does not break existing configuration without a documented migration path.
- It can be tested automatically and across the supported API range.
- Its value is worth the configuration and maintenance cost it introduces.

## Status definitions

- **Ready:** implemented and validated; awaiting publication.
- **Planned:** accepted direction for a future release, but details may change.
- **Proposed:** worth considering after higher-priority work and user feedback.
- **Backlog:** not planned unless demand provides a strong reason to reconsider.

## v1.0.0 — First release

Status: **Ready**

Release scope:

- Redirect End portals, Nether portals, and Ender Pearls.
- Configure directional source-world to destination-world links.
- Use custom arrival coordinates or destination-world spawn fallback.
- Independently block each supported teleport type by source world.
- Allow operators or permitted players to bypass blocked-world rules.
- Preserve vanilla behavior when interception is disabled or no link exists.
- Ship with all interception and blocking rules disabled by default.
- Support Bukkit-compatible Paper/Spigot servers from 1.13.2 through current Paper.
- Validate behavior with unit tests, MockBukkit integration tests, and representative
  real Paper startup tests.

Publication checklist:

- [x] Set the public version to 1.0.0.
- [x] Remove the previous developer identity from source and metadata.
- [x] Provide commented starter configuration and usage documentation.
- [x] Build a Java 8-compatible release JAR.
- [x] Pass automated tests and representative Paper startup tests.
- [ ] Add a project license.
- [ ] Add public support and issue-reporting links.
- [ ] Prepare a marketplace icon, description, screenshots, and v1.0.0 changelog.
- [ ] Publish the release artifact.

## v1.1.0 — Permissions and teleport safety

Status: **Planned**

Goals:

- Add separate bypass permissions for End portals, Nether portals, and Ender Pearls.
- Allow an optional permission requirement for an individual world link.
- Replace coordinate-only safety validation with real landing safety checks.
- Require a solid floor and enough clear space for the player.
- Avoid lava, fire, dangerous blocks, suffocation, and void destinations.
- Search a bounded nearby area for a safe alternative when configured coordinates
  are obstructed.
- Add `/portal validate` to report missing worlds, unsafe coordinates, invalid
  entries, and one-way links.
- Add command tab completion for portal types and loaded world names.
- Improve configuration warnings so administrators can act on them immediately.

Release requirements:

- Existing v1.0.0 configuration continues to work.
- Every permission and safe-location outcome has an automated test.
- Safe-location searching has a strict bound to prevent server lag.
- The default behavior remains unchanged until a server owner opts in.

## v1.2.0 — Entity and portal lifecycle control

Status: **Proposed**

Potential scope:

- Optionally redirect or block mobs, minecarts, and other portal-using entities.
- Configure player and non-player entity handling independently.
- Exclude selected entity types from redirection or blocking.
- Optionally prevent Nether portal creation.
- Optionally prevent End portal activation.
- Support a configurable fallback world or location for blocked travel.
- Protect against recursive teleport events and conflicts with other portal plugins.

This release should only proceed after player-focused v1.1.0 behavior is stable.

## v1.3.0 — Modern server integrations

Status: **Proposed**

Candidates, prioritized by real user demand:

- Native Folia scheduler support.
- PlaceholderAPI values for interception, links, and restriction status.
- Optional administrator update notifications.
- Configurable sounds, titles, and action-bar messages.
- Separate message/language files.
- Optional anonymous bStats metrics with clear disclosure.

Each integration must remain optional. Missing third-party plugins must never stop
EZPortalRedirect from loading.

## Backlog — Outside the current product direction

Status: **Backlog**

- Custom portal-region creation or selection wands.
- GUI portal editors.
- Vault economy and portal usage fees.
- BungeeCord or Velocity cross-server transport.
- Particle or hologram portal construction.
- Scheduled End or Nether access windows.
- Commands executed when a portal is entered.
- World generation, per-world inventories, or general world management.

These features overlap with larger portal and world-management plugins. They should
not be added without strong demand and a clear way to preserve this plugin's small,
focused design.

## Release workflow

For every release:

1. Confirm the feature belongs in the product direction above.
2. Write acceptance criteria before implementation.
3. Update configuration without silently changing existing server behavior.
4. Add focused unit or MockBukkit tests for new behavior and failure cases.
5. Run `mvn -Pmockbukkit clean test`.
6. Run `mvn clean package` to produce the distributable Java 8 JAR.
7. Run `scripts/Test-PaperMatrix.ps1` for representative legacy, modern, and current
   Paper startup validation when runtime behavior changes.
8. Review the JAR metadata, version, ownership, and bundled dependencies.
9. Update README documentation, this roadmap, and the release changelog.

Patch releases such as 1.0.1 should contain compatible bug fixes and documentation
corrections. Minor releases such as 1.1.0 may add backward-compatible features.
Breaking configuration or behavior changes require a major version and a migration
guide.

## Feature review template

Use this checklist when proposing work for this or a future project based on it:

```text
Feature:
Problem it solves:
Who needs it:
Why the current behavior is insufficient:
Configuration or permissions added:
Backward-compatibility impact:
Performance and safety risks:
Automated tests required:
Documentation required:
Target release:
Decision: Planned / Proposed / Backlog / Rejected
```

For a future project, copy this document, replace the product direction and release
sections, and retain the status definitions, release workflow, compatibility rules,
and feature review template. That keeps the same disciplined development process
without forcing EZPortalRedirect-specific features into an unrelated plugin.
