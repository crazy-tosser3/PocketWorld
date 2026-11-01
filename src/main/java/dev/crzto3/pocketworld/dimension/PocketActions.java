package dev.crzto3.pocketworld.dimension;

import dev.crzto3.pocketworld.registry.ModItems;
import dev.crzto3.pocketworld.storage.PocketDimensionData;
import dev.crzto3.pocketworld.storage.PocketDimensionData.PocketDimension;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/** Действия над карманным измерением, общие для команд и окна конфигуратора. */
public final class PocketActions {

    public static final List<EntityType<? extends Animal>> ANIMALS = List.of(
            EntityType.COW,
            EntityType.SHEEP,
            EntityType.PIG,
            EntityType.CHICKEN,
            EntityType.HORSE,
            EntityType.RABBIT,
            EntityType.FOX,
            EntityType.WOLF
    );

    /** Цена одного животного в осколках разлома. */
    public static final int ANIMAL_COST = 2;
    public static final int MAX_ANIMALS = 12;

    private static final Random RANDOM = new Random();

    private PocketActions() {
    }

    /** Пересоздаёт остров и возвращает на точку входа тех, кто на нём стоял. */
    public static boolean regenerate(MinecraftServer server, PocketDimension dimension) {
        ServerLevel pocket = PocketDimensions.getLevel(server);

        if (pocket == null) {
            return false;
        }

        PocketPlot.generate(pocket, dimension);
        PocketDimensionData.get(server).setGenerated(dimension.getId(), true);

        for (ServerPlayer inside : pocket.players()) {
            if (PocketPlot.indexAt(inside.getX(), inside.getZ()) == dimension.getIndex()) {
                PocketTravel.teleportToSpawn(inside, pocket, dimension);
            }
        }

        return true;
    }

    public static int countAnimals(MinecraftServer server, PocketDimension dimension) {
        ServerLevel pocket = PocketDimensions.getLevel(server);

        return pocket == null ? 0 : pocket.getEntitiesOfClass(Animal.class, plotBox(dimension)).size();
    }

    private static AABB plotBox(PocketDimension dimension) {
        Vec3 center = Vec3.atCenterOf(PocketPlot.center(dimension.getIndex()));
        double reach = PocketPlot.leaveRadius(dimension);

        return new AABB(center.x - reach, 0, center.z - reach, center.x + reach, 255, center.z + reach);
    }

    /** Покупает животное за осколки разлома. Возвращает ключ сообщения. */
    public static String buyAnimal(ServerPlayer player, PocketDimension dimension, int animalIndex) {
        if (animalIndex < 0 || animalIndex >= ANIMALS.size()) {
            return "message.pocketworld.animal_unknown";
        }

        ServerLevel pocket = PocketDimensions.getLevel(player.server);

        if (pocket == null) {
            return "message.pocketworld.dimension_missing";
        }

        if (!dimension.isGenerated()) {
            PocketPlot.generate(pocket, dimension);
            PocketDimensionData.get(player.server).setGenerated(dimension.getId(), true);
        }

        if (countAnimals(player.server, dimension) >= MAX_ANIMALS) {
            return "message.pocketworld.animal_limit";
        }

        if (!player.getAbilities().instabuild && !takeShards(player, ANIMAL_COST)) {
            return "message.pocketworld.animal_no_shards";
        }

        Vec3 at = PocketPlot.randomSurface(dimension, RANDOM);
        Mob mob = ANIMALS.get(animalIndex).create(pocket);

        if (mob == null) {
            return "message.pocketworld.animal_unknown";
        }

        mob.moveTo(at.x, at.y, at.z, RANDOM.nextFloat() * 360.0F, 0.0F);
        mob.setPersistenceRequired();
        mob.finalizeSpawn(pocket, pocket.getCurrentDifficultyAt(mob.blockPosition()),
                MobSpawnType.COMMAND, null, null);
        pocket.addFreshEntity(mob);
        return "message.pocketworld.animal_bought";
    }

    private static boolean takeShards(ServerPlayer player, int amount) {
        int have = 0;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.RIFT_SHARD.get())) {
                have += stack.getCount();
            }
        }

        if (have < amount) {
            return false;
        }

        int left = amount;

        for (ItemStack stack : player.getInventory().items) {
            if (left > 0 && stack.is(ModItems.RIFT_SHARD.get())) {
                int take = Math.min(left, stack.getCount());
                stack.shrink(take);
                left -= take;
            }
        }

        player.getInventory().setChanged();
        return true;
    }

    public static Component animalName(int index) {
        return ANIMALS.get(index).getDescription();
    }
}
