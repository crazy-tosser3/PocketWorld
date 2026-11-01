package dev.crzto3.pocketworld.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Небо карманного измерения: плавно меняющийся закат, звёзды, две луны и северное сияние.
 */
public class PocketSkyEffects extends DimensionSpecialEffects {

    private static final float RADIUS = 90.0F;
    private static final int SEGMENTS = 40;

    // Высоты колец купола (в градусах над горизонтом).
    private static final float[] ELEVATIONS = {-40, -2, 6, 22, 50, 90};

    // Тёплый закат и холодные сумерки; небо плавно перетекает между ними.
    private static final float[][] DUSK = {
            {0.28F, 0.12F, 0.38F}, {1.00F, 0.62F, 0.55F}, {0.95F, 0.45F, 0.62F},
            {0.52F, 0.25F, 0.68F}, {0.20F, 0.12F, 0.50F}, {0.07F, 0.05F, 0.28F}
    };
    private static final float[][] COOL = {
            {0.14F, 0.12F, 0.36F}, {0.62F, 0.78F, 0.95F}, {0.50F, 0.55F, 0.90F},
            {0.30F, 0.28F, 0.75F}, {0.12F, 0.10F, 0.48F}, {0.04F, 0.04F, 0.24F}
    };

    private static final float[][] DAY = {
            {0.55F, 0.75F, 0.95F}, {0.80F, 0.90F, 1.00F}, {0.62F, 0.82F, 1.00F},
            {0.42F, 0.68F, 0.98F}, {0.30F, 0.55F, 0.95F}, {0.22F, 0.45F, 0.90F}
    };
    private static final float[][] NIGHT = {
            {0.02F, 0.03F, 0.10F}, {0.08F, 0.10F, 0.25F}, {0.05F, 0.07F, 0.22F},
            {0.03F, 0.05F, 0.18F}, {0.02F, 0.03F, 0.14F}, {0.01F, 0.02F, 0.09F}
    };
    private static final float[][] POLAR = {
            {0.03F, 0.06F, 0.14F}, {0.10F, 0.22F, 0.30F}, {0.06F, 0.14F, 0.28F},
            {0.04F, 0.08F, 0.22F}, {0.03F, 0.05F, 0.18F}, {0.02F, 0.03F, 0.12F}
    };

    /**
     * Тема неба: палитра (null - плавная смена заката и сумерек), яркость звёзд,
     * наличие лун, сила северного сияния и вид солнца (0 нет, 1 у горизонта, 2 высоко).
     */
    private record Theme(float[][] palette, float stars, boolean moons, float aurora, int sun) {
    }

    private static Theme theme(String name) {
        return switch (name) {
            case "sunset" -> new Theme(DUSK, 0.25F, false, 0.0F, 1);
            case "day" -> new Theme(DAY, 0.0F, false, 0.0F, 2);
            case "night" -> new Theme(NIGHT, 1.3F, true, 0.0F, 0);
            case "aurora" -> new Theme(POLAR, 1.0F, false, 1.8F, 0);
            default -> new Theme(null, 1.0F, true, 1.0F, 0);
        };
    }

    private static final int STAR_COUNT = 1100;
    private static final float[][] STARS = new float[STAR_COUNT][6]; // x, y, z, размер, фаза, оттенок

    static {
        Random random = new Random(10842L);

        for (int i = 0; i < STAR_COUNT; i++) {
            double azimuth = random.nextDouble() * Math.PI * 2;
            double y = random.nextDouble() * 0.97 - 0.05;
            double horizontal = Math.sqrt(1 - y * y);

            STARS[i][0] = (float) (horizontal * Math.sin(azimuth));
            STARS[i][1] = (float) y;
            STARS[i][2] = (float) (horizontal * Math.cos(azimuth));
            STARS[i][3] = 0.3F + random.nextFloat() * random.nextFloat() * 0.9F;
            STARS[i][4] = random.nextFloat() * 6.28F;
            STARS[i][5] = random.nextFloat();
        }
    }

    /** Цвет горизонта, обновляется при отрисовке неба и используется для тумана. */
    private static volatile Vec3 horizonColor = new Vec3(0.8, 0.52, 0.66);

    public PocketSkyEffects() {
        super(Float.NaN, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return horizonColor;
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    public boolean renderSky(
            ClientLevel level,
            int ticks,
            float partialTick,
            PoseStack poseStack,
            Camera camera,
            Matrix4f projectionMatrix,
            boolean isFoggy,
            Runnable setupFog
    ) {
        float time = ticks + partialTick;
        float mix = 0.5F + 0.5F * Mth.sin(time * 0.0015F);

        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.mulPoseMatrix(poseStack.last().pose());
        RenderSystem.applyModelViewMatrix();

        Theme theme = theme(ClientPocketState.getSky());
        float[][] palette = new float[ELEVATIONS.length][3];

        for (int i = 0; i < palette.length; i++) {
            for (int c = 0; c < 3; c++) {
                palette[i][c] = theme.palette() == null
                        ? Mth.lerp(mix, DUSK[i][c], COOL[i][c])
                        : theme.palette()[i][c];
            }
        }

        // Непогода приглушает и сереет небо.
        float rain = level.getRainLevel(partialTick);

        if (rain > 0.0F) {
            for (float[] color : palette) {
                float gray = (color[0] + color[1] + color[2]) / 3.0F * 0.6F;

                for (int c = 0; c < 3; c++) {
                    color[c] = Mth.lerp(rain * 0.75F, color[c], gray);
                }
            }
        }

        horizonColor = new Vec3(palette[1][0] * 0.9, palette[1][1] * 0.85, palette[1][2] * 0.9);

        drawDome(palette);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE
        );

        if (theme.sun() > 0) {
            float elevation = theme.sun() == 1 ? 0.07F : 1.1F;
            drawHalo(direction(0.6F, elevation), theme.sun() == 1 ? 34.0F : 28.0F, 0.45F, 1.0F, 0.8F, 0.55F);
        }

        modelView.pushPose();
        modelView.mulPose(Axis.YP.rotationDegrees(time * 0.01F));
        RenderSystem.applyModelViewMatrix();

        if (theme.stars() * (1.0F - rain) > 0.02F) {
            drawStars(time, theme.stars() * (1.0F - rain));
        }

        if (theme.moons()) {
            drawHalo(direction(2.2F, 0.55F), 26.0F, 0.22F, 0.8F, 0.7F, 1.0F);
            drawHalo(direction(-0.8F, 0.85F), 9.0F, 0.20F, 0.5F, 0.9F, 1.0F);
        }

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();

        if (theme.aurora() * (1.0F - rain) > 0.02F) {
            drawAurora(time, theme.aurora() * (1.0F - rain));
        }

        RenderSystem.defaultBlendFunc();

        if (theme.sun() > 0) {
            float elevation = theme.sun() == 1 ? 0.07F : 1.1F;
            drawDisc(direction(0.6F, elevation), 7.0F, 1.0F, 0.98F, 0.85F,
                    1.0F, theme.sun() == 1 ? 0.65F : 0.9F, 0.5F);
        }

        if (theme.moons()) {
            modelView.pushPose();
            modelView.mulPose(Axis.YP.rotationDegrees(time * 0.01F));
            RenderSystem.applyModelViewMatrix();
            drawDisc(direction(2.2F, 0.55F), 9.0F, 0.96F, 0.93F, 1.0F, 0.78F, 0.74F, 0.95F);
            drawDisc(direction(-0.8F, 0.85F), 3.6F, 0.70F, 0.95F, 0.95F, 0.45F, 0.75F, 0.85F);
            modelView.popPose();
        }

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        return true;
    }

    private static float[] direction(float azimuth, float elevation) {
        float horizontal = Mth.cos(elevation);

        return new float[]{horizontal * Mth.sin(azimuth), Mth.sin(elevation), horizontal * Mth.cos(azimuth)};
    }

    private static BufferBuilder begin(VertexFormat.Mode mode) {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(mode, DefaultVertexFormat.POSITION_COLOR);
        return builder;
    }

    private static void drawDome(float[][] palette) {
        BufferBuilder builder = begin(VertexFormat.Mode.QUADS);

        for (int ring = 0; ring < ELEVATIONS.length - 1; ring++) {
            float lowAngle = ELEVATIONS[ring] * Mth.DEG_TO_RAD;
            float highAngle = ELEVATIONS[ring + 1] * Mth.DEG_TO_RAD;
            float[] low = palette[ring];
            float[] high = palette[ring + 1];

            for (int segment = 0; segment < SEGMENTS; segment++) {
                float a0 = segment * Mth.TWO_PI / SEGMENTS;
                float a1 = (segment + 1) * Mth.TWO_PI / SEGMENTS;

                vertex(builder, direction(a0, lowAngle), RADIUS, low[0], low[1], low[2], 1.0F);
                vertex(builder, direction(a1, lowAngle), RADIUS, low[0], low[1], low[2], 1.0F);
                vertex(builder, direction(a1, highAngle), RADIUS, high[0], high[1], high[2], 1.0F);
                vertex(builder, direction(a0, highAngle), RADIUS, high[0], high[1], high[2], 1.0F);
            }
        }

        BufferUploader.drawWithShader(builder.end());
    }

    private static void drawStars(float time, float brightness) {
        BufferBuilder builder = begin(VertexFormat.Mode.QUADS);

        for (float[] star : STARS) {
            // Звёзды гаснут у светлого горизонта и мерцают.
            float visibility = Mth.clamp((star[1] + 0.02F) / 0.45F, 0.0F, 1.0F);
            float twinkle = 0.55F + 0.45F * Mth.sin(time * 0.06F + star[4]);
            float alpha = Math.min(1.0F, visibility * twinkle * brightness);

            if (alpha < 0.02F) {
                continue;
            }

            float[] d = {star[0], star[1], star[2]};
            float[] right = normalize(cross(d, Math.abs(d[1]) > 0.99F ? new float[]{1, 0, 0} : new float[]{0, 1, 0}));
            float[] up = cross(right, d);
            float size = star[3];
            float r = 0.85F + 0.15F * star[5];
            float g = 0.85F + 0.1F * (1 - star[5]);

            quad(builder, d, right, up, size, r, g, 1.0F, alpha);
        }

        BufferUploader.drawWithShader(builder.end());
    }

    private static void quad(
            BufferBuilder builder, float[] d, float[] right, float[] up,
            float size, float r, float g, float b, float a
    ) {
        for (int corner = 0; corner < 4; corner++) {
            float sx = (corner == 0 || corner == 3) ? -size : size;
            float sy = corner < 2 ? -size : size;

            builder.vertex(
                    d[0] * RADIUS + right[0] * sx + up[0] * sy,
                    d[1] * RADIUS + right[1] * sx + up[1] * sy,
                    d[2] * RADIUS + right[2] * sx + up[2] * sy
            ).color(r, g, b, a).endVertex();
        }
    }

    /** Мягкое свечение вокруг луны. */
    private static void drawHalo(float[] d, float radius, float alpha, float r, float g, float b) {
        BufferBuilder builder = begin(VertexFormat.Mode.TRIANGLE_FAN);
        float[] right = normalize(cross(d, new float[]{0, 1, 0}));
        float[] up = cross(right, d);

        vertex(builder, d, RADIUS, r, g, b, alpha);

        for (int i = 0; i <= 32; i++) {
            float angle = i * Mth.TWO_PI / 32;
            float cx = Mth.cos(angle) * radius;
            float cy = Mth.sin(angle) * radius;

            builder.vertex(
                    d[0] * RADIUS + right[0] * cx + up[0] * cy,
                    d[1] * RADIUS + right[1] * cx + up[1] * cy,
                    d[2] * RADIUS + right[2] * cx + up[2] * cy
            ).color(r, g, b, 0.0F).endVertex();
        }

        BufferUploader.drawWithShader(builder.end());
    }

    private static void drawDisc(
            float[] d, float radius,
            float cr, float cg, float cb, float er, float eg, float eb
    ) {
        BufferBuilder builder = begin(VertexFormat.Mode.TRIANGLE_FAN);
        float[] right = normalize(cross(d, new float[]{0, 1, 0}));
        float[] up = cross(right, d);

        builder.vertex(d[0] * (RADIUS - 0.5F), d[1] * (RADIUS - 0.5F), d[2] * (RADIUS - 0.5F))
                .color(cr, cg, cb, 1.0F).endVertex();

        for (int i = 0; i <= 40; i++) {
            float angle = i * Mth.TWO_PI / 40;
            float cx = Mth.cos(angle) * radius;
            float cy = Mth.sin(angle) * radius;

            builder.vertex(
                    d[0] * (RADIUS - 0.5F) + right[0] * cx + up[0] * cy,
                    d[1] * (RADIUS - 0.5F) + right[1] * cx + up[1] * cy,
                    d[2] * (RADIUS - 0.5F) + right[2] * cx + up[2] * cy
            ).color(er, eg, eb, 1.0F).endVertex();
        }

        BufferUploader.drawWithShader(builder.end());
    }

    /** Две волнистые ленты северного сияния. */
    private static void drawAurora(float time, float strength) {
        BufferBuilder builder = begin(VertexFormat.Mode.QUADS);
        int steps = 56;

        for (int ribbon = 0; ribbon < 2; ribbon++) {
            float start = ribbon == 0 ? -1.2F : 1.7F;
            float span = ribbon == 0 ? 2.6F : 2.2F;

            for (int step = 0; step < steps; step++) {
                float[] a = auroraPoint(ribbon, start, span, step / (float) steps, time);
                float[] b = auroraPoint(ribbon, start, span, (step + 1) / (float) steps, time);
                a[3] = Math.min(1.0F, a[3] * strength);
                b[3] = Math.min(1.0F, b[3] * strength);

                // Три яруса ленты: прозрачный низ, яркая середина, прозрачный верх.
                float[][] colors = {
                        {0.30F, 1.00F, 0.60F}, {0.30F, 0.90F, 0.85F}, {0.70F, 0.40F, 1.00F}
                };
                float[] weights = {0.0F, 0.34F, 1.0F};

                for (int layer = 0; layer < 2; layer++) {
                    float[] c0 = colors[layer];
                    float[] c1 = colors[layer + 1];
                    float a0 = layer == 0 ? 0.0F : 0.55F;
                    float a1 = layer == 0 ? 0.55F : 0.0F;

                    vertex(builder, direction(a[0], a[1] + a[2] * weights[layer]), RADIUS - 1,
                            c0[0], c0[1], c0[2], a0 * a[3]);
                    vertex(builder, direction(b[0], b[1] + b[2] * weights[layer]), RADIUS - 1,
                            c0[0], c0[1], c0[2], a0 * b[3]);
                    vertex(builder, direction(b[0], b[1] + b[2] * weights[layer + 1]), RADIUS - 1,
                            c1[0], c1[1], c1[2], a1 * b[3]);
                    vertex(builder, direction(a[0], a[1] + a[2] * weights[layer + 1]), RADIUS - 1,
                            c1[0], c1[1], c1[2], a1 * a[3]);
                }
            }
        }

        BufferUploader.drawWithShader(builder.end());
    }

    /** Азимут, нижняя высота, высота ленты и прозрачность в точке u ленты. */
    private static float[] auroraPoint(int ribbon, float start, float span, float u, float time) {
        float azimuth = start + u * span;
        float base = 0.30F + 0.07F * Mth.sin(u * 7.0F + time * 0.02F + ribbon * 2.0F);
        float height = 0.24F + 0.10F * Mth.sin(u * 5.0F - time * 0.015F + ribbon);
        float fade = (float) Math.pow(Mth.sin(u * Mth.PI), 0.7)
                * (0.7F + 0.3F * Mth.sin(time * 0.03F + u * 9.0F));

        return new float[]{azimuth, base, height, fade};
    }

    private static void vertex(BufferBuilder builder, float[] d, float radius, float r, float g, float b, float a) {
        builder.vertex(d[0] * radius, d[1] * radius, d[2] * radius).color(r, g, b, a).endVertex();
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[]{
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]
        };
    }

    private static float[] normalize(float[] v) {
        float length = Mth.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);

        return new float[]{v[0] / length, v[1] / length, v[2] / length};
    }
}
