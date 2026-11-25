package com.voltaicdynamics.logistics.client;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.ModDataComponents;
import com.voltaicdynamics.core.client.FluidRenderHelper;
import com.voltaicdynamics.logistics.FluidTankBlockEntity;
import com.voltaicdynamics.logistics.FluidTankItem;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Fluid Tank items show how full they are and in the fluid's colour. The item definitions
 * ({@code assets/voltaicdynamics/items/*_fluid_tank.json}) pick a fill model with the {@code voltaicdynamics:fill}
 * property and tint its fluid layer with the {@code voltaicdynamics:fluid} tint source.
 */
public final class FluidTankItemRendering {
    /** Average colour of fluid textures that the game does not tint, such as lava. */
    private static final Map<TextureAtlasSprite, Integer> SPRITE_COLORS = new WeakHashMap<>();

    private FluidTankItemRendering() {
    }

    public static void registerFillProperty(RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(VoltaicDynamics.id("fill"), Fill.CODEC);
    }

    public static void registerTint(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(VoltaicDynamics.id("fluid"), FluidTint.CODEC);
    }

    /** Fill level of the tank item, 0 to 1. */
    public record Fill() implements RangeSelectItemModelProperty {
        public static final MapCodec<Fill> CODEC = MapCodec.unit(new Fill());

        @Override
        public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
            SimpleFluidContent fluid = stack.getOrDefault(ModDataComponents.FLUID.get(), SimpleFluidContent.EMPTY);
            if (fluid.isEmpty()) return 0F;
            int capacity = stack.getItem() instanceof FluidTankItem item ? item.getTier().capacity() : FluidTankBlockEntity.capacity();
            return Math.min(1F, fluid.getAmount() / (float) capacity);
        }

        @Override
        public MapCodec<Fill> type() {
            return CODEC;
        }
    }

    /** Colour of the fluid inside the tank item. */
    public record FluidTint() implements ItemTintSource {
        public static final MapCodec<FluidTint> CODEC = MapCodec.unit(new FluidTint());

        @Override
        public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
            return fluidColor(stack);
        }

        @Override
        public MapCodec<FluidTint> type() {
            return CODEC;
        }
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
                int argb = image.getPixel(x, y);
                if ((argb >>> 24) == 0) continue;
                r += (argb >> 16) & 0xFF;
                g += (argb >> 8) & 0xFF;
                b += argb & 0xFF;
                count++;
            }
        }
        if (count == 0) return -1;
        return 0xFF000000 | (int) (r / count) << 16 | (int) (g / count) << 8 | (int) (b / count);
    }
}
