package dev.crzto3.pocketworld.dimension;

import dev.crzto3.pocketworld.storage.PocketDimensionData;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class PocketDimensionManager {

    public static final List<String> AVAILABLE_TERRAINS = List.of(
            "normal",
            "flat",
            "mountains",
            "islands"
    );

    public static final List<String> AVAILABLE_SKIES = List.of(
            "twilight",
            "sunset",
            "day",
            "night",
            "aurora"
    );

    public static final List<String> AVAILABLE_SIZES = List.of(
            "small",
            "medium",
            "large",
            "huge"
    );

    public static final List<String> AVAILABLE_WEATHER = List.of(
            "clear",
            "rain",
            "thunder"
    );

    /** Биомы, между которыми переключается кнопка в конфигураторе. */
    public static final List<String> PRESET_BIOMES = List.of(
            "minecraft:plains",
            "minecraft:forest",
            "minecraft:birch_forest",
            "minecraft:taiga",
            "minecraft:snowy_plains",
            "minecraft:desert",
            "minecraft:badlands",
            "minecraft:jungle",
            "minecraft:savanna",
            "minecraft:cherry_grove",
            "minecraft:dark_forest",
            "minecraft:mushroom_fields",
            "minecraft:swamp",
            "minecraft:mangrove_swamp",
            "minecraft:flower_forest",
            "minecraft:ice_spikes"
    );

    private PocketDimensionManager() {
    }

    public static PocketDimensionData.PocketDimension createOrGet(
            MinecraftServer server,
            UUID id
    ) {
        return PocketDimensionData.get(server).getOrCreate(id);
    }

    public static PocketDimensionData.PocketDimension get(
            MinecraftServer server,
            UUID id
    ) {
        return PocketDimensionData.get(server).get(id);
    }

    public static Collection<PocketDimensionData.PocketDimension> getAll(
            MinecraftServer server
    ) {
        return PocketDimensionData.get(server).getAll();
    }

    public static boolean setBiome(
            MinecraftServer server,
            UUID id,
            String biome
    ) {
        if (biome == null || biome.isBlank()) {
            return false;
        }

        ResourceLocation biomeId = ResourceLocation.tryParse(biome);

        if (biomeId == null) {
            return false;
        }

        Registry<Biome> biomeRegistry =
                server.registryAccess().registryOrThrow(Registries.BIOME);

        ResourceKey<Biome> biomeKey =
                ResourceKey.create(Registries.BIOME, biomeId);

        if (!biomeRegistry.containsKey(biomeKey)) {
            return false;
        }

        return PocketDimensionData.get(server).setBiome(
                id,
                biomeId.toString()
        );
    }

    public static boolean setTerrain(
            MinecraftServer server,
            UUID id,
            String terrain
    ) {
        if (terrain == null || !AVAILABLE_TERRAINS.contains(terrain)) {
            return false;
        }

        return PocketDimensionData.get(server).setTerrain(id, terrain);
    }

    public static boolean setSky(MinecraftServer server, UUID id, String sky) {
        return AVAILABLE_SKIES.contains(sky) && PocketDimensionData.get(server).setSky(id, sky);
    }

    public static boolean setWeather(MinecraftServer server, UUID id, String weather) {
        return AVAILABLE_WEATHER.contains(weather) && PocketDimensionData.get(server).setWeather(id, weather);
    }

    public static boolean setSize(MinecraftServer server, UUID id, String size) {
        return AVAILABLE_SIZES.contains(size) && PocketDimensionData.get(server).setSize(id, size);
    }

    public static void remove(
            MinecraftServer server,
            UUID id
    ) {
        PocketDimensionData.get(server).remove(id);
    }
}
