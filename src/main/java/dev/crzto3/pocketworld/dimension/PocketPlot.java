package dev.crzto3.pocketworld.dimension;

import dev.crzto3.pocketworld.registry.ModBlocks;
import dev.crzto3.pocketworld.storage.PocketDimensionData.PocketDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Участок общего измерения, принадлежащий одному карману:
 * парящий остров, форма которого зависит от выбранного типа местности.
 */
public final class PocketPlot {

    public static final int SPACING = 2048;
    public static final int PER_ROW = 50;
    public static final int BASE_Y = 64;
    /** Максимальный радиус острова плюс запас на неровный край и острова-спутники. */
    private static final int SCAN = 34;

    private PocketPlot() {
    }

    private record Column(int top, int bottom) {
    }

    private record Palette(BlockState top, BlockState filler, boolean greenery, Block log, Block leaves, boolean ponds) {
    }

    public static BlockPos center(int index) {
        return new BlockPos((index % PER_ROW) * SPACING, BASE_Y, (index / PER_ROW) * SPACING);
    }

    /** Индекс участка, в который попадают координаты, либо -1. */
    public static int indexAt(double x, double z) {
        long gx = Math.round(x / SPACING);
        long gz = Math.round(z / SPACING);

        if (gx < 0 || gx >= PER_ROW || gz < 0 || gz > 100000) {
            return -1;
        }

        return (int) (gz * PER_ROW + gx);
    }

    /** Дальше этого расстояния от центра игрока возвращают на остров. */
    public static int leaveRadius(PocketDimension dimension) {
        return Math.max(dimension.getRadius(), 16) + 16;
    }

    public static boolean isInside(PocketDimension dimension, double x, double y, double z) {
        BlockPos center = center(dimension.getIndex());
        double dx = x - center.getX();
        double dz = z - center.getZ();
        int leave = leaveRadius(dimension);

        return y > 8 && dx * dx + dz * dz <= leave * leave;
    }

    public static Vec3 spawn(PocketDimension dimension) {
        BlockPos center = center(dimension.getIndex());
        Column column = column(dimension.getTerrain(), dimension.getRadius(), 3, 0);
        int top = column == null ? BASE_Y : column.top();

        return new Vec3(center.getX() + 3.5, top + 1.0, center.getZ() + 0.5);
    }

    /** Форма столбца острова в точке (dx, dz) от центра. */
    private static Column column(String terrain, int radius, int dx, int dz) {
        double dist = Math.sqrt(dx * dx + dz * dz);

        if ("islands".equals(terrain)) {
            double scale = radius / 14.0;
            double mainRadius = 8.5 * scale;

            if (dist <= mainRadius) {
                int top = BASE_Y + (int) Math.round(1.2 * Math.sin(dx * 0.5) * Math.cos(dz * 0.5));
                return new Column(top, top - 3 - (int) ((mainRadius - dist) * 0.9));
            }

            int[][] islets = {{-12, -5, 4, 5}, {9, -11, 3, 8}, {11, 10, 4, 3}, {-4, 13, 3, 6}};

            for (int[] islet : islets) {
                double d = Math.hypot(dx - islet[0] * scale, dz - islet[1] * scale);
                double isletRadius = islet[2] * Math.max(1.0, scale);

                if (d <= isletRadius + 0.5) {
                    int top = BASE_Y + islet[3];
                    return new Column(top, top - 2 - (int) ((isletRadius - d) * 0.8));
                }
            }

            return null;
        }

        double edge = radius + 1.2 * Math.sin(Math.atan2(dz, dx) * 5);

        if (dist > edge) {
            return null;
        }

        int top;

        switch (terrain) {
            case "flat" -> top = BASE_Y;
            case "mountains" -> {
                double slope = Math.pow(Math.max(0, 1 - dist / radius), 1.3);
                top = BASE_Y + (int) Math.round(
                        11 * Math.max(1.0, radius / 14.0) * slope * (0.65 + 0.35 * Math.sin(dx * 0.6) * Math.cos(dz * 0.55))
                                + Math.sin(dx * 0.9) * Math.cos(dz * 0.8)
                );
            }
            default -> top = BASE_Y + (int) Math.round(
                    2 * Math.sin(dx * 0.35) * Math.cos(dz * 0.3) + 1.2 * Math.sin((dx + dz) * 0.2)
            );
        }

        return new Column(top, top - 4 - (int) ((edge - dist) * 0.7));
    }

    /** Случайная точка на поверхности острова для появления существ. */
    public static Vec3 randomSurface(PocketDimension dimension, Random random) {
        BlockPos center = center(dimension.getIndex());
        int radius = dimension.getRadius();

        for (int attempt = 0; attempt < 40; attempt++) {
            int dx = random.nextInt(2 * radius - 3) - (radius - 2);
            int dz = random.nextInt(2 * radius - 3) - (radius - 2);
            Column column = column(dimension.getTerrain(), radius, dx, dz);

            if (column != null && Math.abs(dx) + Math.abs(dz) > 2) {
                return new Vec3(center.getX() + dx + 0.5, column.top() + 1.0, center.getZ() + dz + 0.5);
            }
        }

        return spawn(dimension);
    }

    private static Palette palette(String biome) {
        String name = biome.contains(":") ? biome.substring(biome.indexOf(':') + 1) : biome;

        if (name.contains("mangrove_swamp")) {
            return new Palette(Blocks.MUD.defaultBlockState(), Blocks.MUD.defaultBlockState(), false,
                    Blocks.MANGROVE_LOG, Blocks.MANGROVE_LEAVES, true);
        }
        if (name.contains("swamp")) {
            return new Palette(Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), true,
                    Blocks.OAK_LOG, Blocks.OAK_LEAVES, true);
        }
        if (name.contains("desert") || name.contains("beach")) {
            return new Palette(Blocks.SAND.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false, null, null, false);
        }
        if (name.contains("badlands")) {
            return new Palette(Blocks.RED_SAND.defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(), false, null, null, false);
        }
        if (name.contains("mushroom")) {
            return new Palette(Blocks.MYCELIUM.defaultBlockState(), Blocks.DIRT.defaultBlockState(), false, null, null, false);
        }
        if (name.startsWith("snowy") || name.contains("frozen") || name.contains("ice_spikes")
                || name.equals("grove") || name.equals("jagged_peaks")) {
            return new Palette(Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), false,
                    Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES, false);
        }

        Block log = Blocks.OAK_LOG;
        Block leaves = Blocks.OAK_LEAVES;

        if (name.contains("birch")) {
            log = Blocks.BIRCH_LOG;
            leaves = Blocks.BIRCH_LEAVES;
        } else if (name.contains("taiga") || name.contains("spruce")) {
            log = Blocks.SPRUCE_LOG;
            leaves = Blocks.SPRUCE_LEAVES;
        } else if (name.contains("jungle")) {
            log = Blocks.JUNGLE_LOG;
            leaves = Blocks.JUNGLE_LEAVES;
        } else if (name.contains("cherry")) {
            log = Blocks.CHERRY_LOG;
            leaves = Blocks.CHERRY_LEAVES;
        } else if (name.contains("dark_forest")) {
            log = Blocks.DARK_OAK_LOG;
            leaves = Blocks.DARK_OAK_LEAVES;
        } else if (name.contains("savanna")) {
            log = Blocks.ACACIA_LOG;
            leaves = Blocks.ACACIA_LEAVES;
        }

        return new Palette(Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), true, log, leaves, false);
    }

    /** Чертёж местности: какие блоки должны стоять на острове. Ничего не меняет в мире. */
    private static final class Plan {

        private final Map<Long, BlockState> blocks = new HashMap<>();

        BlockState get(BlockPos pos) {
            return blocks.getOrDefault(pos.asLong(), Blocks.AIR.defaultBlockState());
        }

        void set(BlockPos pos, BlockState state) {
            blocks.put(pos.asLong(), state);
        }
    }

    private static long seed(PocketDimension dimension) {
        return dimension.getId().getLeastSignificantBits() ^ dimension.getIndex();
    }

    /**
     * Создаёт остров или применяет новые настройки к уже существующему.
     * Постройки игроков не ломаются: меняются только блоки, которые остались такими,
     * какими их создала предыдущая генерация, а рельеф подстраивается под постройки.
     */
    public static void generate(ServerLevel level, PocketDimension dimension) {
        BlockPos center = center(dimension.getIndex());
        Palette palette = palette(dimension.getBiome());

        Plan fresh = plan(dimension.getTerrain(), dimension.getRadius(), dimension.getBiome(),
                seed(dimension), center);
        Plan previous = dimension.isGenerated()
                ? plan(dimension.getGeneratedTerrain(), dimension.getGeneratedRadius(),
                dimension.getGeneratedBiome(), seed(dimension), center)
                : new Plan();

        applyBiome(level, center, dimension.getBiome());
        place(level, center, dimension.getTerrain(), dimension.getRadius(), previous, fresh, palette);
    }

    private static Plan plan(String terrain, int radius, String biome, long seed, BlockPos center) {
        Plan plan = new Plan();
        Palette palette = palette(biome);
        Random random = new Random(seed);

        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState base = ModBlocks.POCKET_WALL.get().defaultBlockState();

        for (int dx = -SCAN; dx <= SCAN; dx++) {
            for (int dz = -SCAN; dz <= SCAN; dz++) {
                Column column = column(terrain, radius, dx, dz);

                if (column == null) {
                    continue;
                }

                for (int y = column.bottom(); y <= column.top(); y++) {
                    BlockState state;

                    if (y == column.bottom()) {
                        state = base;
                    } else if (y == column.top()) {
                        state = palette.top();
                    } else if (y > column.top() - 4) {
                        state = palette.filler();
                    } else {
                        state = stone;
                    }

                    plan.set(new BlockPos(center.getX() + dx, y, center.getZ() + dz), state);
                }
            }
        }

        // Блок выхода в центре острова.
        Column middle = column(terrain, radius, 0, 0);
        int exitY = (middle == null ? BASE_Y : middle.top()) + 1;
        plan.set(new BlockPos(center.getX(), exitY, center.getZ()),
                ModBlocks.DIMENSION_EXIT.get().defaultBlockState());

        if (palette.ponds()) {
            ponds(plan, center, terrain, radius, random);
        }

        decorate(plan, center, terrain, radius, palette, random);
        return plan;
    }

    /** Радиус вокруг постройки, на котором рельеф плавно спускается к её основанию. */
    private static final int BLEND = 7;

    /** Состояние одного столбца острова перед перестройкой. */
    private static final class ColumnState {
        BlockState[] current;
        boolean[] player;
        int buildLow = Integer.MAX_VALUE;
        int buildHigh = Integer.MIN_VALUE;
        /** Постройка стоит на поверхности (над ней нет грунта). */
        boolean surface;
        /** Высота нового рельефа в этом столбце; Integer.MIN_VALUE - без изменений. */
        int blendTop = Integer.MIN_VALUE;

        boolean hasBuild() {
            return buildLow <= buildHigh;
        }
    }

    /**
     * Переносит чертёж в мир. Блок считается постройкой игрока, если он не совпадает
     * с предыдущим чертежом. Такие блоки остаются нетронутыми, а рельеф подстраивается:
     * вокруг построек он плавно спускается или поднимается к их основанию,
     * под ними подсыпается грунт, а внутрь не лезет.
     */
    private static void place(
            ServerLevel level, BlockPos center, String terrain, int radius,
            Plan previous, Plan fresh, Palette palette
    ) {
        int low = BASE_Y - 30;
        int high = BASE_Y + 60;
        int height = high - low + 1;
        int size = 2 * SCAN + 1;
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        ColumnState[][] columns = new ColumnState[size][size];

        // Проход 1: что стоит в каждом столбце и где постройки игроков.
        for (int dx = -SCAN; dx <= SCAN; dx++) {
            for (int dz = -SCAN; dz <= SCAN; dz++) {
                ColumnState state = new ColumnState();
                state.current = new BlockState[height];
                state.player = new boolean[height];
                columns[dx + SCAN][dz + SCAN] = state;

                for (int i = 0; i < height; i++) {
                    pos.set(center.getX() + dx, low + i, center.getZ() + dz);
                    state.current[i] = level.getBlockState(pos);
                    state.player[i] = !state.current[i].isAir() && !state.current[i].equals(previous.get(pos));

                    if (state.player[i]) {
                        state.buildLow = Math.min(state.buildLow, low + i);
                        state.buildHigh = Math.max(state.buildHigh, low + i);
                    }
                }

                // Постройка под землёй (над ней есть грунт) просто остаётся на месте.
                state.surface = state.hasBuild();

                for (int y = state.buildHigh + 1; state.hasBuild() && y <= high; y++) {
                    if (!state.current[y - low].isAir() && !state.player[y - low]) {
                        state.surface = false;
                    }
                }
            }
        }

        // Проход 2: рельеф около построек сползает к уровню их основания.
        for (int dx = -SCAN; dx <= SCAN; dx++) {
            for (int dz = -SCAN; dz <= SCAN; dz++) {
                ColumnState state = columns[dx + SCAN][dz + SCAN];
                Column ground = column(terrain, radius, dx, dz);

                if (state.surface || ground == null) {
                    continue;
                }

                double nearest = Double.MAX_VALUE;
                int floor = 0;

                for (int ox = Math.max(-SCAN, dx - BLEND); ox <= Math.min(SCAN, dx + BLEND); ox++) {
                    for (int oz = Math.max(-SCAN, dz - BLEND); oz <= Math.min(SCAN, dz + BLEND); oz++) {
                        ColumnState other = columns[ox + SCAN][oz + SCAN];
                        double distance = Math.hypot(ox - dx, oz - dz);

                        if (other.surface && distance < nearest) {
                            nearest = distance;
                            floor = other.buildLow - 1;
                        }
                    }
                }

                if (nearest > BLEND) {
                    continue;
                }

                double t = Math.min(1.0, Math.max(0.0, 1.0 - (nearest - 1.0) / BLEND));
                double weight = t * t * (3.0 - 2.0 * t);

                state.blendTop = Math.max(ground.bottom() + 1,
                        (int) Math.round(ground.top() + (floor - ground.top()) * weight));
            }
        }

        // Проход 3: запись в мир.
        for (int dx = -SCAN; dx <= SCAN; dx++) {
            for (int dz = -SCAN; dz <= SCAN; dz++) {
                ColumnState state = columns[dx + SCAN][dz + SCAN];
                Column ground = column(terrain, radius, dx, dz);
                int x = center.getX() + dx;
                int z = center.getZ() + dz;

                // Высота нового грунта прямо под полом постройки.
                int fillFrom = Integer.MAX_VALUE;

                if (state.surface) {
                    int newTop = Integer.MIN_VALUE;

                    for (int y = state.buildLow - 1; y >= low; y--) {
                        pos.set(x, y, z);

                        if (!fresh.get(pos).isAir()) {
                            newTop = y;
                            break;
                        }
                    }

                    if (newTop == Integer.MIN_VALUE) {
                        fillFrom = Math.max(low, state.buildLow - 5);
                    } else if (state.buildLow - 1 - newTop <= 10) {
                        fillFrom = newTop + 1;
                    }
                }

                for (int i = 0; i < height; i++) {
                    int y = low + i;

                    // Блоки игрока и пространство внутри постройки не трогаем.
                    if (state.player[i] || (state.hasBuild() && y >= state.buildLow && y <= state.buildHigh)) {
                        continue;
                    }

                    pos.set(x, y, z);
                    BlockState wanted = fresh.get(pos);

                    if (state.surface) {
                        if (y >= state.buildLow) {
                            wanted = air;
                        } else if (y == state.buildLow - 1) {
                            // Под постройкой всегда лежит верхний слой почвы.
                            if (!wanted.isAir() || y >= fillFrom) {
                                wanted = palette.top();
                            }
                        } else if (y >= fillFrom) {
                            wanted = palette.filler();
                        }
                    } else if (state.blendTop != Integer.MIN_VALUE) {
                        if (y < ground.bottom() || y > state.blendTop) {
                            wanted = air;
                        } else if (y == ground.bottom()) {
                            wanted = fresh.get(pos);
                        } else if (y == state.blendTop) {
                            wanted = palette.top();
                        } else if (y > state.blendTop - 4) {
                            wanted = palette.filler();
                        } else {
                            wanted = Blocks.STONE.defaultBlockState();
                        }
                    }

                    if (!state.current[i].equals(wanted)) {
                        level.setBlock(pos, wanted, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    /** Болотные лужи: мелкие водоёмы с кувшинками, окружённые берегом. */
    private static void ponds(Plan plan, BlockPos center, String terrain, int radius, Random random) {
        int[][] around = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        for (int dx = -SCAN; dx <= SCAN; dx++) {
            for (int dz = -SCAN; dz <= SCAN; dz++) {
                Column column = column(terrain, radius, dx, dz);

                if (column == null || Math.hypot(dx, dz) < 4 || Math.hypot(dx - 3, dz) < 3
                        || Math.sin(dx * 0.55 + 1.0) + Math.cos(dz * 0.5) < 1.15) {
                    continue;
                }

                // Вода не должна вытекать за край или в соседнюю яму.
                boolean enclosed = true;

                for (int[] step : around) {
                    Column next = column(terrain, radius, dx + step[0], dz + step[1]);
                    enclosed &= next != null && next.top() >= column.top();
                }

                if (!enclosed) {
                    continue;
                }

                BlockPos surface = new BlockPos(center.getX() + dx, column.top(), center.getZ() + dz);

                plan.set(surface.below(), Blocks.CLAY.defaultBlockState());
                plan.set(surface, Blocks.WATER.defaultBlockState());

                if (random.nextInt(3) == 0) {
                    plan.set(surface.above(), Blocks.LILY_PAD.defaultBlockState());
                }
            }
        }
    }

    private static void decorate(Plan plan, BlockPos center, String terrain, int radius, Palette palette, Random random) {
        Block[] flowers = {Blocks.DANDELION, Blocks.POPPY, Blocks.AZURE_BLUET, Blocks.CORNFLOWER};
        List<int[]> trees = new ArrayList<>();
        int maxTrees = Math.max(4, (int) (4 * Math.pow(radius / 14.0, 2)));

        for (int attempt = 0; attempt < 40 * maxTrees / 4 && palette.log() != null && trees.size() < maxTrees; attempt++) {
            int dx = random.nextInt(2 * radius - 5) - (radius - 3);
            int dz = random.nextInt(2 * radius - 5) - (radius - 3);
            Column column = column(terrain, radius, dx, dz);

            if (column == null || Math.abs(dx) + Math.abs(dz) < 6 || !surfaceOk(plan, center, column, dx, dz)) {
                continue;
            }

            boolean crowded = false;

            for (int[] other : trees) {
                crowded |= Math.hypot(other[0] - dx, other[1] - dz) < 5;
            }

            if (crowded || Math.hypot(dx - 3, dz) < 3) {
                continue;
            }

            trees.add(new int[]{dx, dz});
            tree(plan, new BlockPos(center.getX() + dx, column.top() + 1, center.getZ() + dz), random, palette);
        }

        for (int dx = -SCAN; dx <= SCAN; dx++) {
            for (int dz = -SCAN; dz <= SCAN; dz++) {
                Column column = column(terrain, radius, dx, dz);

                if (column == null || (dx == 0 && dz == 0) || (dx == 3 && dz == 0)) {
                    continue;
                }

                BlockPos above = new BlockPos(center.getX() + dx, column.top() + 1, center.getZ() + dz);

                if (!plan.get(above).isAir()) {
                    continue;
                }

                if (palette.greenery() && plan.get(above.below()).is(Blocks.GRASS_BLOCK)) {
                    double roll = random.nextDouble();

                    if (roll < 0.14) {
                        plan.set(above, Blocks.GRASS.defaultBlockState());
                    } else if (roll < 0.17) {
                        plan.set(above, flowers[random.nextInt(flowers.length)].defaultBlockState());
                    }
                } else if (plan.get(above.below()).is(Blocks.SAND) && random.nextDouble() < 0.02) {
                    plan.set(above, Blocks.DEAD_BUSH.defaultBlockState());
                }
            }
        }
    }

    private static boolean surfaceOk(Plan plan, BlockPos center, Column column, int dx, int dz) {
        BlockState top = plan.get(new BlockPos(center.getX() + dx, column.top(), center.getZ() + dz));
        return top.is(Blocks.GRASS_BLOCK) || top.is(Blocks.SNOW_BLOCK) || top.is(Blocks.MUD);
    }

    private static void tree(Plan plan, BlockPos base, Random random, Palette palette) {
        int height = 4 + random.nextInt(2);
        BlockState log = palette.log().defaultBlockState();
        BlockState leaves = palette.leaves().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);

        for (int dy = height - 3; dy <= height; dy++) {
            int radius = dy >= height - 1 ? 1 : 2;

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    boolean corner = Math.abs(dx) == radius && Math.abs(dz) == radius;

                    if (corner && (radius == 1 || random.nextBoolean())) {
                        continue;
                    }

                    BlockPos pos = base.offset(dx, dy, dz);

                    if (plan.get(pos).isAir()) {
                        plan.set(pos, leaves);
                    }
                }
            }
        }

        for (int dy = 0; dy < height; dy++) {
            plan.set(base.above(dy), log);
        }
    }

    /** Меняет биом всех чанков участка, чтобы менялись цвета травы и тумана. */
    private static void applyBiome(ServerLevel level, BlockPos center, String biome) {
        ResourceLocation id = ResourceLocation.tryParse(biome);

        if (id == null) {
            return;
        }

        Holder<Biome> holder = level.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getHolder(ResourceKey.create(Registries.BIOME, id))
                .orElse(null);

        if (holder == null) {
            return;
        }

        List<ChunkAccess> chunks = new ArrayList<>();

        for (int cx = (center.getX() - SCAN) >> 4; cx <= (center.getX() + SCAN) >> 4; cx++) {
            for (int cz = (center.getZ() - SCAN) >> 4; cz <= (center.getZ() + SCAN) >> 4; cz++) {
                ChunkAccess chunk = level.getChunk(cx, cz);
                chunk.fillBiomesFromNoise(
                        (x, y, z, sampler) -> holder,
                        level.getChunkSource().randomState().sampler()
                );
                chunk.setUnsaved(true);
                chunks.add(chunk);
            }
        }

        level.getChunkSource().chunkMap.resendBiomesForChunks(chunks);
    }
}
