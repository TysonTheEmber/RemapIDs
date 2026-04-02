# RemapIDs

Data-driven registry remap and alias system for Minecraft modpack developers. Redirect any block, item, fluid, entity type, tag, recipe, or loot table ID to another — no code required.

**Supported platforms:** Forge 1.20.1 · Fabric 1.20.1 · NeoForge 1.21.1 · Fabric 1.21.1

## Use Cases

- **Removed a mod from your modpack?** Remap its block/item IDs to equivalents from another mod so existing worlds don't lose data.
- **Consolidating duplicate items?** Redirect one mod's silver ingot to another mod's silver ingot across all recipes, loot tables, and tags.
- **Replacing vanilla content?** Remap vanilla blocks/items to modded equivalents — copper blocks become brass blocks, etc.
- **Migrating pre-1.13 worlds?** Use numerical IDs (e.g. `35:14`) as sources — they're resolved via a built-in flattening table.

## Getting Started

### 1. Install the mod

Drop the jar for your loader into your `mods/` folder.

### 2. Create remap files

Create JSON files in `config/remapids/remaps/`. Each file contains a `remaps` array:

```json
{
  "remaps": [
    {
      "source": "iceandfire:silver_ingot",
      "target": "othermod:silver_ingot",
      "types": ["item", "recipe", "loot_table", "tag"]
    }
  ]
}
```

### 3. Restart the game

Remaps are applied during startup. Registry-level remaps (block, item, fluid, entity_type) require a full restart. Recipe, loot table, and tag remaps can be reloaded with `/reload`.

## Remap File Format

Each entry in the `remaps` array has:

| Field | Required | Description |
|-------|----------|-------------|
| `source` | Yes | The ID to remap from (e.g. `iceandfire:silver_ingot`) |
| `target` | Yes | The ID to remap to (e.g. `othermod:silver_ingot`) |
| `types` | No | Array of remap types to apply. If omitted, applies to **all** applicable types. |

### Remap Types

**Registry types** (applied at startup, require restart):

| Type | Description |
|------|-------------|
| `block` | Block registry — affects placed blocks in the world |
| `item` | Item registry — affects items in inventories |
| `fluid` | Fluid registry |
| `entity_type` | Entity type registry |

**Reloadable types** (applied on datapack load, supports `/reload`):

| Type | Description |
|------|-------------|
| `recipe` | Rewrites recipe JSON — ingredient and result item IDs |
| `loot_table` | Rewrites loot table JSON — item and block references |
| `tag` | Rewrites tag entries — redirects tag membership |

## Examples

### Remove a mod and remap to alternatives

```json
{
  "remaps": [
    {
      "source": "silents_mechanisms:copper_ingot",
      "target": "create:brass_ingot",
      "types": ["item", "recipe", "loot_table", "tag"]
    },
    {
      "source": "silents_mechanisms:copper_block",
      "target": "create:brass_block",
      "types": ["item", "block", "recipe", "loot_table", "tag"]
    }
  ]
}
```

### Replace vanilla blocks with modded equivalents

```json
{
  "remaps": [
    {
      "source": "minecraft:copper_block",
      "target": "create:brass_block",
      "types": ["item", "block"]
    },
    {
      "source": "minecraft:copper_ingot",
      "target": "create:brass_ingot",
      "types": ["item"]
    }
  ]
}
```

### Wildcard remaps

Remap all items matching a pattern. Both source and target must contain `*`:

```json
{
  "remaps": [
    {
      "source": "iceandfire:silver_*",
      "target": "othermod:silver_*"
    }
  ]
}
```

### Pre-1.13 numerical IDs

Use numerical block/item IDs from pre-flattening Minecraft:

```json
{
  "remaps": [
    {
      "source": "35:14",
      "target": "minecraft:red_wool",
      "types": ["block"]
    }
  ]
}
```

Custom numerical ID mappings for modded pre-1.13 IDs can be added in `config/remapids/numerical_ids.json`.

### Tag remaps

Prefix tag sources with `#`:

```json
{
  "remaps": [
    {
      "source": "#forge:ores/silver",
      "target": "#forge:ores/tin",
      "types": ["tag"]
    }
  ]
}
```

## Chain Resolution

Remap chains are automatically flattened. If `A → B` and `B → C` are both defined, `A` resolves directly to `C`. Circular chains are detected and rejected. Maximum chain depth is 10.

## Commands

| Command | Description |
|---------|-------------|
| `/remapids id block` | Shows the registry ID of the block you're looking at, including any active remaps |
| `/remapids id hand` | Shows the registry ID of the item in your main hand, including any active remaps |

## How It Works

RemapIDs operates at multiple levels depending on the remap type:

- **Registry aliases** (block, item, fluid, entity_type): Injects aliases into the game registry's internal lookup maps so the source ID resolves to the target's registry entry. On Forge, also redirects `ForgeRegistry.getValue()` lookups.
- **Block state remapping**: When aliasing blocks, block state IDs in `Block.BLOCK_STATE_REGISTRY` are remapped so network serialization uses the target block's state IDs. Compatible block state properties (e.g. `axis`, `waterlogged`) are preserved.
- **JSON rewriting** (recipe, loot_table): Rewrites datapack JSON at load time, replacing item, block, and tag references.
- **Tag rewriting** (tag): Intercepts tag loading to redirect tag entries.
- **NBT interception** (Fabric/NeoForge): Intercepts block state and item stack deserialization from NBT for world migration.
- **Missing mappings** (Forge): Handles `MissingMappingsEvent` for world migration when a source mod is removed.

## File Structure

```
config/
└── remapids/
    ├── remaps/              ← Drop remap JSON files here
    │   ├── create_compat.json
    │   ├── old_mod_removal.json
    │   └── ...
    └── numerical_ids.json   ← Optional: custom pre-1.13 ID mappings
```

Multiple JSON files are supported — they're loaded alphabetically and merged. This lets you organize remaps by mod or purpose.

## License

See [modding licence](https://tysontheember.dev/modding-licence/).
