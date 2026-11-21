package com.voltaicdynamics.logistics.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.ModDataComponents;
import com.voltaicdynamics.core.client.FluidRenderHelper;
import com.voltaicdynamics.logistics.FluidTankBlockEntity;
import com.voltaicdynamics.logistics.FluidTankItem;
import com.voltaicdynamics.logistics.VoltaicLogistics;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The Fluid Tank icon shows what the item holds: an empty glass tank, or the fluid at its level
 * (models {@code item/fluid_tank_1..8}, one row of glass each, picked by the {@code voltaicdynamics:fill} property) in the fluid's colour.
 */
public final class FluidTankItemRendering {
    /** Tint index of the fluid layer ({@code layer1} of the fill models). */
    private static final int FLUID_LAYER = 1;
    /** Average colour of fluid textures that the game does not tint, such as lava. */
    private static final Map<TextureAtlasSprite, Integer> SPRITE_COLORS = new WeakHashMap<>();

    private FluidTankItemRendering() {
    }

    /** Call from client setup (on the main thread). */
    public static void registerFillProperty() {
        for (var tank : VoltaicLogistics.tanks()) {
            ItemProperties.register(tank.asItem(), VoltaicDynamics.id("fill"),
                    (stack, level, entity, seed) -> {
                        SimpleFluidContent fluid = stack.getOrDefault(ModDataComponents.FLUID.get(), SimpleFluidContent.EMPTY);
                        if (fluid.isEmpty()) return 0F;
                        int capacity = stack.getItem() instanceof FluidTankItem item ? item.getTier().capacity() : FluidTankBlockEntity.capacity();
                        return Math.min(1F, fluid.getAmount() / (float) capacity);
                    });
        }
    }

    public static void registerColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == FLUID_LAYER ? fluidColor(stack) : -1,
                VoltaicLogistics.tanks().stream().map(tank -> tank.asItem()).toArray(net.minecraft.world.level.ItemLike[]::new));
    }

    private static int fluidColor(ItemStack stack) {
        FluidStack fluid = stack.getOrDefault(ModDataComponents.FLUID.get(), SimpleFluidContent.EMPTY).copy();
        if (fluid.isEmpty()) return -1;
        int tint = FluidRenderHelper.tint(fluid);
        if ((tint & 0xFFFFFF) != 0xFFFFFF) return tint | 0xFF000000;
        // Untinted fluids carry their colour in the texture: use its average.
        return SPRITE_COLORS.computeIfAbsent(FluidRenderHelper.sprite(fluid), FluidTankItemRendering::averageColor);
    }

    private static int averageColor(TextureAtlasSprite sprite) {
        NativeImage image = sprite.contents().getOriginalImage();
        int size = Math.min(image.getWidth(), image.getHeight()); // first frame of an animated texture
        long r = 0, g = 0, b = 0, count = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int abgr = image.getPixelRGBA(x, y);
                if ((abgr >>> 24) == 0) continue;
                r += abgr & 0xFF;
                g += (abgr >> 8) & 0xFF;
                b += (abgr >> 16) & 0xFF;
                count++;
            }
        }
        if (count == 0) return -1;
        return 0xFF000000 | (int) (r / count) << 16 | (int) (g / count) << 8 | (int) (b / count);
    }
}
