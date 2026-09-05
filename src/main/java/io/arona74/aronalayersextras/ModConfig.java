package io.arona74.aronalayersextras;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {
    private static ModConfig INSTANCE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "aronalayersextras.json";

    public boolean enableGrassSpreading = true;
    public boolean enableMyceliumSpreading = true;
    public boolean enableSheepEatingGrassLayers = true;
    public boolean preventGrassDecay = true;
    public boolean enableLayersFallWithSand = true;
    public boolean enableWetSands = true;
    public boolean enableBlockOffset = true;
    // If true, vanilla plants (flowers, grass, saplings, etc.) also receive the Y-offset treatment.
    // If false, only blocks listed in AdditionalOffsetBlocks get the offset.
    // Requires resource reload (F3+T) to take effect.
    public boolean VanillaBlockOffset = true;
    // Extra plant/decoration block IDs from other mods (e.g. Conquest Reforged) that should
    // also receive the Y-offset treatment, in addition to the built-in vanilla plant list.
    // Example: "conquest:seagrass", "conquest:tall_seagrass"
    public List<String> AdditionalOffsetBlocks = new ArrayList<>(List.of(
            "conquest:seagrass",
            "conquest:tall_seagrass",
            "minecraft:light_gray_petals"
    ));
    // Dry block -> wet block pairs used by enableWetSands. A dry block touching water
    // becomes its wet counterpart; a wet block with no water on any of its six faces
    // turns back into the dry one, so the mapping is read in both directions.
    // Pairs whose blocks are not both registered (no Conquest Reforged, say) are ignored.
    // Remove the "minecraft:sand" pair to leave vanilla sand alone.
    public Map<String, String> WetSandMappings = defaultWetSandMappings();

    // Written out with explicit puts rather than Map.of: Map.of has no iteration
    // order, so the generated JSON would list the pairs scrambled and reshuffle
    // them on every save.
    private static Map<String, String> defaultWetSandMappings() {
        Map<String, String> mappings = new LinkedHashMap<>();
        mappings.put("minecraft:sand", "conquest:wet_sand");
        mappings.put("conquest:sand_layer", "conquest:wet_sand_layer");
        mappings.put("conquest:sand_and_gravel_sand", "conquest:wet_sand_and_gravel_sand");
        mappings.put("conquest:sand_and_gravel_sand_layer", "conquest:wet_sand_and_gravel_sand_layer");
        mappings.put("conquest:sand_and_vegetation_sand", "conquest:wet_sand_and_vegetation_sand");
        mappings.put("conquest:sand_and_vegetation_sand_layer", "conquest:wet_sand_and_vegetation_sand_layer");
        mappings.put("conquest:dry_river_sand", "conquest:wet_river_sand");
        mappings.put("conquest:dry_river_sand_layer", "conquest:wet_river_sand_layer");
        mappings.put("conquest:gray_sand", "conquest:wet_gray_sand");
        mappings.put("conquest:gray_sand_layer", "conquest:wet_gray_sand_layer");
        mappings.put("conquest:light_gray_sand", "conquest:wet_light_gray_sand");
        mappings.put("conquest:light_gray_sand_layer", "conquest:wet_light_gray_sand_layer");
        mappings.put("conquest:lime_sand", "conquest:wet_lime_sand");
        mappings.put("conquest:lime_sand_layer", "conquest:wet_lime_sand_layer");
        mappings.put("conquest:pink_sand", "conquest:wet_pink_sand");
        mappings.put("conquest:pink_sand_layer", "conquest:wet_pink_sand_layer");
        mappings.put("conquest:purple_sand", "conquest:wet_purple_sand");
        mappings.put("conquest:purple_sand_layer", "conquest:wet_purple_sand_layer");
        mappings.put("conquest:brown_sand", "conquest:wet_brown_sand");
        mappings.put("conquest:brown_sand_layer", "conquest:wet_brown_sand_layer");
        mappings.put("conquest:tan_sand", "conquest:wet_tan_sand");
        mappings.put("conquest:tan_sand_layer", "conquest:wet_tan_sand_layer");
        
        return mappings;
    }

    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static ModConfig load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                INSTANCE = GSON.fromJson(reader, ModConfig.class);
                INSTANCE.save();
                return INSTANCE;
            } catch (Exception e) {
                AronaLayersExtras.LOGGER.error("Failed to load config, using defaults", e);
            }
        }
        INSTANCE = new ModConfig();
        INSTANCE.save();
        return INSTANCE;
    }

    public void save() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(this, writer);
        } catch (Exception e) {
            AronaLayersExtras.LOGGER.error("Failed to save config", e);
        }
    }
}
