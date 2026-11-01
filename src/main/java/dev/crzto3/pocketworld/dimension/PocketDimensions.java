package dev.crzto3.pocketworld.dimension;

import dev.crzto3.pocketworld.PocketWorld;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class PocketDimensions {

    /** Общее измерение, в котором каждому карману отведён свой участок. */
    public static final ResourceKey<Level> POCKET_LEVEL = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation(PocketWorld.MOD_ID, "pocket")
    );

    private PocketDimensions() {
    }

    public static ServerLevel getLevel(MinecraftServer server) {
        return server.getLevel(POCKET_LEVEL);
    }

    public static boolean isPocket(Level level) {
        return level.dimension() == POCKET_LEVEL;
    }
}
