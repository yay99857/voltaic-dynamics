package com.voltaicdynamics.logistics.client;

import com.voltaicdynamics.core.client.FluidRenderHelper;
import com.voltaicdynamics.logistics.FluidTankBlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.FluidStack;

/** The fluid level seen through the tank glass; glowing fluids (lava) render at full brightness. */
public class FluidTankRenderer implements BlockEntityRenderer<FluidTankBlockEntity> {
    private static final float MIN = 2.05F / 16F;
    private static final float MAX = 13.95F / 16F;
    private static final float BOTTOM = 1.05F / 16F;
    private static final float TOP = 14.95F / 16F;

    public FluidTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FluidTankBlockEntity tank, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        FluidStack fluid = tank.getFluid();
        if (fluid.isEmpty()) return;
        float fill = Mth.clamp((float) fluid.getAmount() / Math.max(1, tank.getTank().getCapacity()), 0.02F, 1F);
        float top = BOTTOM + (TOP - BOTTOM) * fill;
        TextureAtlasSprite sprite = FluidRenderHelper.sprite(fluid);
        int color = FluidRenderHelper.tint(fluid);
        if ((color >>> 24) == 0) color |= 0xFF000000;
        int glow = fluid.getFluidType().getLightLevel(fluid);
        int packed = glow > 0 ? LightTexture.pack(Math.max(glow, LightTexture.block(light)), LightTexture.sky(light)) : light;
        FluidRenderHelper.box(pose, buffers.getBuffer(Sheets.translucentCullBlockSheet()), sprite, color, packed,
                MIN, BOTTOM, MIN, MAX, top, MAX);
    }
}
