package com.voltaicdynamics.datagen;

import com.voltaicdynamics.generators.FluidFuel;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.data.DataMapProvider;

import java.util.concurrent.CompletableFuture;

/**
 * Fluid Generator fuels. Lava: 200 FE/mB at 80 FE/t, so a bucket gives 200,000 FE, the same as burning it in the
 * Combustion Generator but twice as fast and fed by pipes (Thermal's Magmatic Dynamo: about 120k-180k RF per
 * bucket at 80 RF/t). Packs add other mods' fuels to the same data map.
 */
public class ModDataMapProvider extends DataMapProvider {
    public ModDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        builder(FluidFuel.DATA_MAP)
                .add(Fluids.LAVA.builtInRegistryHolder(), new FluidFuel(200, 80), false);
    }
}
