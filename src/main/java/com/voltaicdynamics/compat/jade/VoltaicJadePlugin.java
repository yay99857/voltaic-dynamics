package com.voltaicdynamics.compat.jade;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.generators.CombustionGeneratorBlock;
import com.voltaicdynamics.generators.CombustionGeneratorBlockEntity;
import com.voltaicdynamics.generators.FluidGeneratorBlock;
import com.voltaicdynamics.generators.FluidGeneratorBlockEntity;
import com.voltaicdynamics.generators.ThermoelectricGeneratorBlock;
import com.voltaicdynamics.generators.ThermoelectricGeneratorBlockEntity;
import com.voltaicdynamics.core.ModDataComponents;
import com.voltaicdynamics.logistics.FluidTankBlock;
import com.voltaicdynamics.logistics.FluidTankBlockEntity;
import com.voltaicdynamics.logistics.LogisticsCableBlock;
import com.voltaicdynamics.logistics.LogisticsCableBlockEntity;
import com.voltaicdynamics.logistics.LogisticsNetwork;
import com.voltaicdynamics.logistics.LogisticsNetworkManager;
import com.voltaicdynamics.machines.MachineTier;
import com.voltaicdynamics.machines.TierInstallerItem;
import com.voltaicdynamics.machines.ProcessingMachineBlock;
import com.voltaicdynamics.machines.ProcessingMachineBlockEntity;
import com.voltaicdynamics.power.VoltaicCableBlockEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.List;

/**
 * Jade: energy and inventories come from Jade's own capability readers. This adds the furnace progress and
 * the generators' current output, and hides the cable's energy (a cable stores nothing; its capacity only
 * tells other mods how much it accepts, which Jade would show as "0 / 2,000 FE").
 */
@WailaPlugin(VoltaicDynamics.MOD_ID)
public class VoltaicJadePlugin implements IWailaPlugin {
    static final ResourceLocation MACHINE_INFO = ResourceLocation.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, "machine_info");
    static final ResourceLocation TANK_ICON = ResourceLocation.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, "tank_icon");
    static final ResourceLocation CABLE_ENERGY = ResourceLocation.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, "cable_energy");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(MachineInfo.INSTANCE, ProcessingMachineBlockEntity.class);
        registration.registerBlockDataProvider(MachineInfo.INSTANCE, LogisticsCableBlockEntity.class);
        registration.registerBlockDataProvider(MachineInfo.INSTANCE, CombustionGeneratorBlockEntity.class);
        registration.registerBlockDataProvider(MachineInfo.INSTANCE, FluidGeneratorBlockEntity.class);
        registration.registerBlockDataProvider(MachineInfo.INSTANCE, ThermoelectricGeneratorBlockEntity.class);
        registration.registerEnergyStorage(NoCableEnergy.INSTANCE, VoltaicCableBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(MachineInfo.INSTANCE, ProcessingMachineBlock.class);
        registration.registerBlockComponent(MachineInfo.INSTANCE, LogisticsCableBlock.class);
        registration.registerBlockComponent(MachineInfo.INSTANCE, CombustionGeneratorBlock.class);
        registration.registerBlockComponent(MachineInfo.INSTANCE, FluidGeneratorBlock.class);
        registration.registerBlockComponent(MachineInfo.INSTANCE, ThermoelectricGeneratorBlock.class);
        registration.registerBlockIcon(TankIcon.INSTANCE, FluidTankBlock.class);
    }

    enum MachineInfo implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return MACHINE_INFO;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            switch (accessor.getBlockEntity()) {
                case ProcessingMachineBlockEntity machine -> {
                    data.putInt("Progress", machine.getProgress());
                    data.putInt("Total", machine.getMaxProgress());
                    data.putInt("Tier", machine.getTier().ordinal());
                }
                case LogisticsCableBlockEntity cable -> {
                    if (accessor.getLevel() instanceof ServerLevel level
                            && cable.getBlockState().getBlock() instanceof LogisticsCableBlock block) {
                        LogisticsNetwork network = LogisticsNetworkManager.get(level, block.kind()).getNetwork(cable.getBlockPos());
                        if (network != null) {
                            data.putInt("Cables", network.size());
                            data.putInt("Pulls", network.pullCount());
                            data.putInt("Targets", network.targetCount());
                        }
                    }
                }
                case CombustionGeneratorBlockEntity generator -> data.putInt("Rate", generator.getCurrentRate());
                case FluidGeneratorBlockEntity generator -> data.putInt("Rate", generator.getCurrentRate());
                case ThermoelectricGeneratorBlockEntity generator -> data.putInt("Rate", generator.getLastGenerated());
                default -> {
                }
            }
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.contains("Progress")) {
                MachineTier tier = MachineTier.byOrdinal(data.getInt("Tier"));
                if (tier != MachineTier.BASIC) {
                    tooltip.add(Component.translatable("jade.voltaicdynamics.tier", TierInstallerItem.tierName(tier), tier.parallel())
                            .withStyle(tier.color()));
                }
                int total = Math.max(1, data.getInt("Total"));
                int percent = data.getInt("Progress") * 100 / total;
                if (percent > 0) {
                    tooltip.add(Component.translatable("jade.voltaicdynamics.progress", percent));
                }
            } else if (data.contains("Cables")) {
                tooltip.add(Component.translatable("jade.voltaicdynamics.network", data.getInt("Cables"),
                        data.getInt("Pulls"), data.getInt("Targets")).withStyle(ChatFormatting.GRAY));
            } else if (data.contains("Rate")) {
                int rate = data.getInt("Rate");
                tooltip.add(rate > 0
                        ? Component.translatable("jade.voltaicdynamics.generating", rate)
                        : Component.translatable("jade.voltaicdynamics.idle").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    /**
     * The tank's icon carries the fluid the block holds, so it shows the fill level and colour like the item does.
     * Reads the client block entity, which the tank keeps in sync for its renderer.
     */
    enum TankIcon implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return TANK_ICON;
        }

        @Override
        public IElement getIcon(BlockAccessor accessor, IPluginConfig config, IElement currentIcon) {
            if (!(accessor.getBlockEntity() instanceof FluidTankBlockEntity tank)) return currentIcon;
            ItemStack stack = new ItemStack(tank.getBlockState().getBlock());
            if (!tank.getFluid().isEmpty()) {
                stack.set(ModDataComponents.FLUID.get(), SimpleFluidContent.copyOf(tank.getFluid()));
            }
            return IElementHelper.get().item(stack);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        }
    }

    enum NoCableEnergy implements IServerExtensionProvider<CompoundTag> {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return CABLE_ENERGY;
        }

        @Override
        public List<ViewGroup<CompoundTag>> getGroups(Accessor<?> accessor) {
            return List.of(); // handled: show no energy bar for cables
        }
    }
}
