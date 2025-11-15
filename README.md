# CRLayers Extras

A Fabric mod for Minecraft 1.20.1 that makes Conquest Reforged layer blocks behave like their vanilla counterparts.

## Features

This mod adds natural mechanics to Conquest Reforged layer blocks:

### 1. Grass Spreading
- `conquest:grass_block_layer` now spreads to `conquest:loamy_dirt_slab` blocks
- Mimics vanilla `minecraft:grass_block` spreading to `minecraft:dirt`
- Requires proper light levels (light level 9+ at source, 4+ at target)

### 2. Mycelium Spreading
- `conquest:mycelium_layer` now spreads to `conquest:loamy_dirt_slab` blocks
- Mimics vanilla `minecraft:mycelium` spreading to `minecraft:dirt`
- Can spread in any light level (just like vanilla mycelium)

### 3. Sheep Grass Eating
- Sheep can now eat `conquest:grass_block_layer` blocks
- Converts them to `conquest:loamy_dirt_slab` (similar to vanilla grass → dirt conversion)
- Sheep will regrow their wool after eating, just like in vanilla

## Installation

1. Download the latest release from the releases page
2. Place the JAR file in your `.minecraft/mods` folder
3. Make sure you have Fabric Loader and Fabric API installed
4. Works best alongside Conquest Reforged mod

## Building from Source

```bash
./gradlew build
```

The built JAR will be in `build/libs/`

## Dependencies

- Minecraft 1.20.1
- Fabric Loader 0.15.0+
- Fabric API
- Java 17+

## License

MIT License - See LICENSE file for details

## Credits

Created by Arona74
