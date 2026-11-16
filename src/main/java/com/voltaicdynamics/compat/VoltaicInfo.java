package com.voltaicdynamics.compat;

import com.voltaicdynamics.core.VoltaicCore;
import com.voltaicdynamics.gadgets.VoltaicGadgets;
import com.voltaicdynamics.generators.VoltaicGenerators;
import com.voltaicdynamics.logistics.VoltaicLogistics;
import com.voltaicdynamics.machines.VoltaicMachines;
import com.voltaicdynamics.power.VoltaicPower;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.function.Supplier;

/**
 * Info pages shown by JEI and EMI. Shared so both recipe viewers explain the same items with the same text
 * (lang keys {@code info.voltaicdynamics.<name>}).
 */
public final class VoltaicInfo {
    public record Entry(String name, Supplier<? extends ItemLike> item) {
        public Component text() {
            return Component.translatable("info.voltaicdynamics." + name);
        }
    }

    public static final List<Entry> ENTRIES = List.of(
            new Entry("voltaic_dust", VoltaicCore.VOLTAIC_DUST),
            new Entry("galvanic_alloy", VoltaicCore.GALVANIC_ALLOY),
            new Entry("thermoelectric_generator", VoltaicGenerators.THERMOELECTRIC_GENERATOR),
            new Entry("combustion_generator", VoltaicGenerators.COMBUSTION_GENERATOR),
            new Entry("basic_accumulator", VoltaicPower.BASIC_ACCUMULATOR),
            new Entry("advanced_accumulator", VoltaicPower.ADVANCED_ACCUMULATOR),
            new Entry("voltaic_cable", VoltaicPower.VOLTAIC_CABLE),
            new Entry("alloy_smelter", VoltaicMachines.ALLOY_SMELTER),
            new Entry("fluid_tank", VoltaicLogistics.FLUID_TANK),
            new Entry("item_cable", VoltaicLogistics.ITEM_CABLE),
            new Entry("fluid_cable", VoltaicLogistics.FLUID_CABLE),
            new Entry("pocket_accumulator", VoltaicGadgets.POCKET_ACCUMULATOR),
            new Entry("voltaic_wrench", VoltaicGadgets.VOLTAIC_WRENCH));

    private VoltaicInfo() {
    }
}
