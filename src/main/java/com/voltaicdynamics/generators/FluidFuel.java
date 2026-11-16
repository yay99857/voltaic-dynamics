package com.voltaicdynamics.generators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voltaicdynamics.VoltaicDynamics;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import org.jetbrains.annotations.Nullable;

/**
 * What a fluid is worth in the Fluid Generator: {@code energy_per_mb} FE for each mB burned, produced at
 * {@code rate} FE/t. Read from the {@code voltaicdynamics:fluid_fuels} data map
 * ({@code data/<namespace>/data_maps/fluid/fluid_fuels.json}), so packs can add any mod's fuel:
 * <pre>{"values": {"minecraft:lava": {"energy_per_mb": 200, "rate": 80},
 *             "#c:biodiesel": {"energy_per_mb": 300, "rate": 120}}}</pre>
 */
public record FluidFuel(int energyPerMb, int rate) {
    public static final Codec<FluidFuel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 1_000_000).fieldOf("energy_per_mb").forGetter(FluidFuel::energyPerMb),
            Codec.intRange(1, 1_000_000).fieldOf("rate").forGetter(FluidFuel::rate)
    ).apply(instance, FluidFuel::new));

    public static final DataMapType<Fluid, FluidFuel> DATA_MAP = DataMapType
            .builder(VoltaicDynamics.id("fluid_fuels"), Registries.FLUID, CODEC)
            .synced(CODEC, false)
            .build();

    @Nullable
    public static FluidFuel of(Fluid fluid) {
        return fluid.builtInRegistryHolder().getData(DATA_MAP);
    }

    @Nullable
    public static FluidFuel of(FluidStack stack) {
        return stack.isEmpty() ? null : of(stack.getFluid());
    }
}
