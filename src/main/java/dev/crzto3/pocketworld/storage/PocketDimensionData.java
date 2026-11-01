package dev.crzto3.pocketworld.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class PocketDimensionData extends SavedData {

    private static final String DATA_NAME = "pocketworld_dimensions";

    private final Map<UUID, PocketDimension> dimensions = new HashMap<>();
    private final Map<Integer, UUID> byIndex = new HashMap<>();
    private final Map<UUID, ReturnPoint> returnPoints = new HashMap<>();
    private int nextIndex = 0;

    /** Версия формата: в 2 участки разнесены на 2048 блоков, старые острова нужно создать заново. */
    private static final int VERSION = 2;

    public PocketDimensionData() {
    }

    public static PocketDimensionData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();

        return overworld.getDataStorage().computeIfAbsent(
                PocketDimensionData::load,
                PocketDimensionData::new,
                DATA_NAME
        );
    }

    public PocketDimension getOrCreate(UUID id) {
        PocketDimension dimension = dimensions.get(id);

        if (dimension == null) {
            dimension = register(new PocketDimension(id, "minecraft:plains", "normal"));
            setDirty();
        }

        return dimension;
    }

    private PocketDimension register(PocketDimension dimension) {
        if (dimension.index < 0 || byIndex.containsKey(dimension.index)) {
            dimension.index = nextIndex;
        }

        nextIndex = Math.max(nextIndex, dimension.index + 1);
        dimensions.put(dimension.id, dimension);
        byIndex.put(dimension.index, dimension.id);
        return dimension;
    }

    public PocketDimension get(UUID id) {
        return dimensions.get(id);
    }

    public PocketDimension getByIndex(int index) {
        UUID id = byIndex.get(index);
        return id == null ? null : dimensions.get(id);
    }

    public Collection<PocketDimension> getAll() {
        return Collections.unmodifiableCollection(dimensions.values());
    }

    public boolean setBiome(UUID id, String biome) {
        PocketDimension dimension = dimensions.get(id);

        if (dimension == null || biome == null || biome.isBlank()) {
            return false;
        }

        dimension.biome = biome;
        setDirty();
        return true;
    }

    public boolean setTerrain(UUID id, String terrain) {
        PocketDimension dimension = dimensions.get(id);

        if (dimension == null || terrain == null || terrain.isBlank()) {
            return false;
        }

        dimension.terrain = terrain;
        setDirty();
        return true;
    }

    public boolean setSky(UUID id, String sky) {
        PocketDimension dimension = dimensions.get(id);

        if (dimension == null || sky == null) {
            return false;
        }

        dimension.sky = sky;
        setDirty();
        return true;
    }

    public boolean setWeather(UUID id, String weather) {
        PocketDimension dimension = dimensions.get(id);

        if (dimension == null || weather == null) {
            return false;
        }

        dimension.weather = weather;
        setDirty();
        return true;
    }

    public boolean setSize(UUID id, String size) {
        PocketDimension dimension = dimensions.get(id);

        if (dimension == null || size == null) {
            return false;
        }

        dimension.size = size;
        setDirty();
        return true;
    }

    public void setOwner(UUID id, UUID owner, String ownerName) {
        PocketDimension dimension = dimensions.get(id);

        if (dimension != null) {
            dimension.ownerId = owner;
            dimension.ownerName = ownerName;
            setDirty();
        }
    }

    public boolean invite(UUID id, UUID player, String name) {
        PocketDimension dimension = dimensions.get(id);

        if (dimension == null || dimension.invited.put(player, name) != null) {
            return false;
        }

        setDirty();
        return true;
    }

    public boolean uninvite(UUID id, UUID player) {
        PocketDimension dimension = dimensions.get(id);

        if (dimension == null || dimension.invited.remove(player) == null) {
            return false;
        }

        setDirty();
        return true;
    }

    public void setGenerated(UUID id, boolean generated) {
        PocketDimension dimension = dimensions.get(id);

        if (dimension != null) {
            dimension.generated = generated;

            if (generated) {
                // Запоминаем, что именно построено, чтобы при смене настроек отличать рельеф от построек.
                dimension.generatedTerrain = dimension.terrain;
                dimension.generatedBiome = dimension.biome;
                dimension.generatedSize = dimension.size;
            }

            setDirty();
        }
    }

    public void setReturnPoint(UUID player, ReturnPoint point) {
        returnPoints.put(player, point);
        setDirty();
    }

    public ReturnPoint takeReturnPoint(UUID player) {
        ReturnPoint point = returnPoints.remove(player);

        if (point != null) {
            setDirty();
        }

        return point;
    }

    public void remove(UUID id) {
        PocketDimension removed = dimensions.remove(id);

        if (removed != null) {
            byIndex.remove(removed.index);
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();

        for (PocketDimension dimension : dimensions.values()) {
            CompoundTag dimensionTag = new CompoundTag();

            dimensionTag.putString("Id", dimension.id.toString());
            dimensionTag.putString("Biome", dimension.biome);
            dimensionTag.putString("Terrain", dimension.terrain);
            dimensionTag.putInt("Index", dimension.index);
            dimensionTag.putString("Sky", dimension.sky);
            dimensionTag.putString("Size", dimension.size);
            dimensionTag.putString("Weather", dimension.weather);

            if (dimension.generatedTerrain != null) {
                dimensionTag.putString("GeneratedTerrain", dimension.generatedTerrain);
                dimensionTag.putString("GeneratedBiome", dimension.generatedBiome);
                dimensionTag.putString("GeneratedSize", dimension.generatedSize);
            }
            dimensionTag.putBoolean("Generated", dimension.generated);

            if (dimension.ownerId != null) {
                dimensionTag.putUUID("Owner", dimension.ownerId);
                dimensionTag.putString("OwnerName", dimension.ownerName);
            }

            ListTag invited = new ListTag();

            dimension.invited.forEach((uuid, name) -> {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("Id", uuid);
                entry.putString("Name", name);
                invited.add(entry);
            });

            dimensionTag.put("Invited", invited);
            list.add(dimensionTag);
        }

        tag.put("Dimensions", list);
        tag.putInt("NextIndex", nextIndex);
        tag.putInt("Version", VERSION);

        ListTag returns = new ListTag();

        returnPoints.forEach((player, point) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", player);
            entry.putString("Dimension", point.dimension());
            entry.putDouble("X", point.x());
            entry.putDouble("Y", point.y());
            entry.putDouble("Z", point.z());
            entry.putFloat("Yaw", point.yaw());
            entry.putFloat("Pitch", point.pitch());
            returns.add(entry);
        });

        tag.put("ReturnPoints", returns);
        return tag;
    }

    public static PocketDimensionData load(CompoundTag tag) {
        PocketDimensionData data = new PocketDimensionData();

        data.nextIndex = tag.getInt("NextIndex");
        boolean outdated = tag.getInt("Version") < VERSION;

        ListTag list = tag.getList("Dimensions", Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag dimensionTag = list.getCompound(i);

            try {
                UUID id = UUID.fromString(dimensionTag.getString("Id"));

                String biome = dimensionTag.contains("Biome", Tag.TAG_STRING)
                        ? dimensionTag.getString("Biome")
                        : "minecraft:plains";

                String terrain = dimensionTag.contains("Terrain", Tag.TAG_STRING)
                        ? dimensionTag.getString("Terrain")
                        : "normal";

                PocketDimension dimension = new PocketDimension(id, biome, terrain);

                dimension.index = dimensionTag.contains("Index", Tag.TAG_INT)
                        ? dimensionTag.getInt("Index")
                        : -1;
                // Участки сдвинулись: остров появится на новом месте при следующем входе.
                dimension.generated = !outdated && dimensionTag.getBoolean("Generated");

                if (dimensionTag.contains("GeneratedTerrain", Tag.TAG_STRING)) {
                    dimension.generatedTerrain = dimensionTag.getString("GeneratedTerrain");
                    dimension.generatedBiome = dimensionTag.getString("GeneratedBiome");
                    dimension.generatedSize = dimensionTag.getString("GeneratedSize");
                }

                if (dimensionTag.contains("Weather", Tag.TAG_STRING)) {
                    dimension.weather = dimensionTag.getString("Weather");
                }

                if (dimensionTag.contains("Sky", Tag.TAG_STRING)) {
                    dimension.sky = dimensionTag.getString("Sky");
                }

                if (dimensionTag.contains("Size", Tag.TAG_STRING)) {
                    dimension.size = dimensionTag.getString("Size");
                }

                if (dimensionTag.hasUUID("Owner")) {
                    dimension.ownerId = dimensionTag.getUUID("Owner");
                    dimension.ownerName = dimensionTag.getString("OwnerName");
                }

                ListTag invited = dimensionTag.getList("Invited", Tag.TAG_COMPOUND);

                for (int j = 0; j < invited.size(); j++) {
                    CompoundTag entry = invited.getCompound(j);

                    if (entry.hasUUID("Id")) {
                        dimension.invited.put(entry.getUUID("Id"), entry.getString("Name"));
                    }
                }

                data.register(dimension);

            } catch (IllegalArgumentException exception) {
                // Пропускаем запись с некорректным UUID.
            }
        }

        ListTag returns = tag.getList("ReturnPoints", Tag.TAG_COMPOUND);

        for (int i = 0; i < returns.size(); i++) {
            CompoundTag entry = returns.getCompound(i);

            if (entry.hasUUID("Player")) {
                data.returnPoints.put(entry.getUUID("Player"), new ReturnPoint(
                        entry.getString("Dimension"),
                        entry.getDouble("X"),
                        entry.getDouble("Y"),
                        entry.getDouble("Z"),
                        entry.getFloat("Yaw"),
                        entry.getFloat("Pitch")
                ));
            }
        }

        return data;
    }

    /** Место, откуда игрок вошёл в карман. */
    public record ReturnPoint(String dimension, double x, double y, double z, float yaw, float pitch) {
    }

    public static class PocketDimension {

        private final UUID id;
        private String biome;
        private String terrain;
        private int index = -1;
        private String sky = "twilight";
        private String size = "medium";
        private String weather = "clear";
        private String generatedTerrain;
        private String generatedBiome;
        private String generatedSize;
        private UUID ownerId;
        private String ownerName = "";
        private boolean generated;
        private final Map<UUID, String> invited = new LinkedHashMap<>();

        public PocketDimension(UUID id, String biome, String terrain) {
            this.id = id;
            this.biome = biome;
            this.terrain = terrain;
        }

        public UUID getId() {
            return id;
        }

        public String getBiome() {
            return biome;
        }

        public String getTerrain() {
            return terrain;
        }

        public String getSky() {
            return sky;
        }

        /** Настройки, с которыми остров построен в последний раз (у старых островов равны текущим). */
        public String getGeneratedTerrain() {
            return generatedTerrain != null ? generatedTerrain : terrain;
        }

        public String getGeneratedBiome() {
            return generatedBiome != null ? generatedBiome : biome;
        }

        public int getGeneratedRadius() {
            return radiusOf(generatedSize != null ? generatedSize : size);
        }

        public String getWeather() {
            return weather;
        }

        public String getSize() {
            return size;
        }

        /** Радиус острова в блоках по выбранному размеру. */
        public int getRadius() {
            return radiusOf(size);
        }

        private static int radiusOf(String size) {
            return switch (size) {
                case "small" -> 9;
                case "large" -> 19;
                case "huge" -> 28;
                default -> 14;
            };
        }

        public int getIndex() {
            return index;
        }

        public UUID getOwnerId() {
            return ownerId;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public boolean isGenerated() {
            return generated;
        }

        public boolean isOwner(UUID player) {
            return ownerId != null && ownerId.equals(player);
        }

        public boolean isInvited(UUID player) {
            return invited.containsKey(player);
        }

        public Map<UUID, String> getInvited() {
            return Collections.unmodifiableMap(invited);
        }
    }
}
