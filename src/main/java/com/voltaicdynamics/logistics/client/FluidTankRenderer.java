package com.voltaicdynamics.logistics.client;

import com.voltaicdynamics.core.client.FluidRenderHelper;
import com.voltaicdynamics.logistics.FluidTankBlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/** Draws the tank's fluid as a box that rises with the fill level. */
public class FluidTankRenderer implements BlockEntityRenderer<FluidTankBlockEntity, FluidTankRenderer.State> {
    private static final float MIN = 2.05F / 16F;
    private static final float MAX = 13.95F / 16F;
    private static final float BOTTOM = 1.05F / 16F;
    private static final float TOP = 14.95F / 16F;

    public FluidTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** What one frame needs, copied from the block entity. */
    public static final class State extends BlockEntityRenderState {
        @Nullable
        TextureAtlasSprite sprite;
        int color;
        int light;
        float top;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FluidTankBlockEntity tank, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tank, state, partialTicks, cameraPosition, breakProgress);
        FluidStack fluid = tank.getFluid();
        if (fluid.isEmpty()) {
            state.sprite = null;
            return;
        }
        float fill = Mth.clamp((float) fluid.getAmount() / Math.max(1, tank.getTank().getCapacity()), 0.02F, 1F);
        state.top = BOTTOM + (TOP - BOTTOM) * fill;
        state.sprite = FluidRenderHelper.sprite(fluid);
        int color = FluidRenderHelper.tint(fluid);
        state.color = (color >>> 24) == 0 ? color | 0xFF000000 : color;
        int glow = fluid.getFluidType().getLightLevel(fluid);
        state.light = glow > 0
                ? LightCoordsUtil.pack(Math.max(glow, LightCoordsUtil.block(state.lightCoords)), LightCoordsUtil.sky(state.lightCoords))
                : state.lightCoords;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        TextureAtlasSprite sprite = state.sprite;
        if (sprite == null) return;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentCull(sprite.atlasLocation()),
                (pose, buffer) -> FluidRenderHelper.box(pose, buffer, sprite, state.color, state.light,
                        MIN, BOTTOM, MIN, MAX, state.top, MAX));
    }
}
