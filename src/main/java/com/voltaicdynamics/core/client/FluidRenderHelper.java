package com.voltaicdynamics.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.FluidModel;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;

/** Draws fluids in GUIs (tiled still texture) and in the world (a tinted box). */
public final class FluidRenderHelper {
    private FluidRenderHelper() {
    }

    /** The fluid's model: its textures and tint now live there. */
    private static FluidModel model(FluidStack fluid) {
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.getFluid().defaultFluidState());
    }

    public static TextureAtlasSprite sprite(FluidStack fluid) {
        return model(fluid).stillMaterial().sprite();
    }

    /** ARGB tint of the fluid as an item-like stack (no biome); -1 when untinted. */
    public static int tint(FluidStack fluid) {
        FluidTintSource source = model(fluid).fluidTintSource();
        return source == null ? -1 : source.colorAsStack(fluid);
    }

    /** Fills a gauge of {@code width} x {@code height} from the bottom up to {@code amount / capacity}. */
    public static void drawGauge(GuiGraphicsExtractor g, FluidStack fluid, int amount, int capacity, int x, int y, int width, int height) {
        if (fluid.isEmpty() || amount <= 0 || capacity <= 0) return;
        int filled = (int) Math.max(1, Math.min(height, (long) amount * height / capacity));
        TextureAtlasSprite sprite = sprite(fluid);
        int color = tint(fluid);
        if ((color >>> 24) == 0) color |= 0xFF000000;
        int top = y + height - filled;
        g.enableScissor(x, top, x + width, y + height);
        for (int ty = y + height - 16; ty > top - 16; ty -= 16) {
            for (int tx = x; tx < x + width; tx += 16) {
                g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, tx, ty, 16, 16, color);
            }
        }
        g.disableScissor();
    }

    /** One textured, tinted face-culled box for the block entity renderer (coordinates in block units). */
    public static void box(PoseStack.Pose p, VertexConsumer consumer, TextureAtlasSprite sprite, int color, int light,
                           float x0, float y0, float z0, float x1, float y1, float z1) {
        float u0 = sprite.getU(x0), u1 = sprite.getU(x1);
        float v0 = sprite.getV(z0), v1 = sprite.getV(z1);
        // top
        quad(p, consumer, color, light, 0, 1, 0,
                x0, y1, z0, u0, v0, x0, y1, z1, u0, v1, x1, y1, z1, u1, v1, x1, y1, z0, u1, v0);
        float vy0 = sprite.getV(1 - y1), vy1 = sprite.getV(1 - y0);
        // north (z0)
        quad(p, consumer, color, light, 0, 0, -1,
                x1, y0, z0, u1, vy1, x0, y0, z0, u0, vy1, x0, y1, z0, u0, vy0, x1, y1, z0, u1, vy0);
        // south (z1)
        quad(p, consumer, color, light, 0, 0, 1,
                x0, y0, z1, u0, vy1, x1, y0, z1, u1, vy1, x1, y1, z1, u1, vy0, x0, y1, z1, u0, vy0);
        float uz0 = sprite.getU(z0), uz1 = sprite.getU(z1);
        // west (x0)
        quad(p, consumer, color, light, -1, 0, 0,
                x0, y0, z0, uz0, vy1, x0, y0, z1, uz1, vy1, x0, y1, z1, uz1, vy0, x0, y1, z0, uz0, vy0);
        // east (x1)
        quad(p, consumer, color, light, 1, 0, 0,
                x1, y0, z1, uz1, vy1, x1, y0, z0, uz0, vy1, x1, y1, z0, uz0, vy0, x1, y1, z1, uz1, vy0);
        // bottom
        quad(p, consumer, color, light, 0, -1, 0,
                x0, y0, z1, u0, v1, x0, y0, z0, u0, v0, x1, y0, z0, u1, v0, x1, y0, z1, u1, v1);
    }

    private static void quad(PoseStack.Pose p, VertexConsumer c, int color, int light, float nx, float ny, float nz,
                             float ax, float ay, float az, float au, float av,
                             float bx, float by, float bz, float bu, float bv,
                             float cx, float cy, float cz, float cu, float cv,
                             float dx, float dy, float dz, float du, float dv) {
        vertex(p, c, color, light, nx, ny, nz, ax, ay, az, au, av);
        vertex(p, c, color, light, nx, ny, nz, bx, by, bz, bu, bv);
        vertex(p, c, color, light, nx, ny, nz, cx, cy, cz, cu, cv);
        vertex(p, c, color, light, nx, ny, nz, dx, dy, dz, du, dv);
    }

    private static void vertex(PoseStack.Pose p, VertexConsumer c, int color, int light, float nx, float ny, float nz,
                               float x, float y, float z, float u, float v) {
        c.addVertex(p, x, y, z).setColor(color).setUv(u, v).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(p, nx, ny, nz);
    }
}
