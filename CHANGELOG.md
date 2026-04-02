## 1.3.0

### Fixed
- **Modded remap targets not found in registry** (#1) — Remapping to modded IDs (e.g. `minecraft:copper_block → create:brass_block`) would fail with "not found in item/block registry" and skip the remap. Three separate issues caused this:
  - `RemapValidator` permanently removed entries for targets not yet registered at validation time. Validation is now non-destructive (warn-only) — entries are preserved and validated at actual injection time.
  - On Forge, the `MappedRegistry.freeze()` hook fired during bootstrap on the Render thread, racing with mod loading on worker threads. Modded content wasn't registered yet when `finalizeIfPending()` ran. Finalization and alias injection are now triggered from `FMLLoadCompleteEvent`, which is guaranteed to fire after all `RegisterEvent` handlers complete.
  - On Forge 1.20.1, modded entries live in `ForgeRegistries`, not in vanilla `MappedRegistry`. Target lookups now go through `ForgeRegistries.BLOCKS/ITEMS/etc.` instead of the vanilla registry maps.

### Added
- **Live vanilla block/item remapping** — Remapping existing vanilla IDs to modded IDs (e.g. `minecraft:copper_block → create:brass_block`) now works correctly. Block state IDs are remapped in `Block.BLOCK_STATE_REGISTRY` so network serialization writes target state IDs instead of crashing with `-1`.
- **Block state property matching** — When remapping between blocks with compatible properties (e.g. `axis`, `waterlogged`), matching property values are preserved on the target state. Unmatched properties fall back to the target block's defaults.
- **`IdMapperAccessor`** (all loaders) — Mixin accessor for `Block.BLOCK_STATE_REGISTRY` internals, enabling block state ID remapping without corrupting the reverse lookup table.
- **`MappedRegistryAccessor`** (Forge) — Mixin accessor for `MappedRegistry.byLocation`/`byKey`, used by the new `RegistryAliasInjector`.
- **`RegistryAliasInjector`** (Forge) — Dedicated alias injection helper that uses ForgeRegistries for target lookup and injects into vanilla MappedRegistry where possible.

### Changed
- **Forge alias injection moved out of `MappedRegistryMixin`** — Registry alias injection on Forge is now handled by `RegistryAliasInjector` triggered from `FMLLoadCompleteEvent`, replacing the unreliable `freeze()` hook. Other loaders retain the `freeze()`-based approach.