package com.voltaicdynamics.power;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import com.voltaicdynamics.core.transfer.Transfers;
import com.voltaicdynamics.core.transfer.VoltaicItemHandler;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.component.DataComponentGetter;
import com.voltaicdynamics.core.energy.CreativeEnergyStorage;
import com.voltaicdynamics.core.energy.EnergyBuffer;
import com.voltaicdynamics.core.energy.RecentInputFaces;

import com.voltaicdynamics.core.blockentity.VoltaicBlockEntity;
import com.voltaicdynamics.core.ModDataComponents;
import com.voltaicdynamics.core.energy.VoltaicEnergyStorage;
import com.voltaicdynamics.core.sideconfig.IoMode;
import com.voltaicdynamics.core.sideconfig.SideConfig;
import com.voltaicdynamics.core.sideconfig.SideConfigHelper;
import com.voltaicdynamics.core.sideconfig.SideConfigurable;
import com.voltaicdynamics.core.sideconfig.SidedHandlers;
import com.voltaicdynamics.core.sideconfig.TransferType;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

public class AccumulatorBlockEntity extends VoltaicBlockEntity implements MenuProvider, SideConfigurable, EnergyBuffer {
    public static final int SLOT_CHARGE = 0;
    public static final int SLOT_DISCHARGE = 1;

    private final AccumulatorTier tier;
    private final VoltaicEnergyStorage energyStorage;
    private final SideConfigHelper.Neighbors neighbors = new SideConfigHelper.Neighbors(this);
    private final SideConfig sideConfig = SideConfig.builder()
            .add(TransferType.ENERGY, List.of(IoMode.BOTH, IoMode.INPUT, IoMode.OUTPUT, IoMode.NONE), IoMode.BOTH, true, true)
            .add(TransferType.ITEM, List.of(IoMode.BOTH, IoMode.INPUT, IoMode.OUTPUT, IoMode.NONE), IoMode.BOTH, false, false)
            .build();
    private SidedHandlers sided;
    private final RecentInputFaces inputFaces = new RecentInputFaces(this);

    private final VoltaicItemHandler itemHandler = new VoltaicItemHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            saveChanges();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return Transfers.itemEnergy(stack) != null;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    /** What pipes and hoppers see: insert into either slot, but only take items out once they are done. */
    private final ResourceHandler<ItemResource> automationHandler = new DelegatingResourceHandler<>(itemHandler) {
        @Override
        public int extract(int slot, ItemResource resource, int amount, TransactionContext transaction) {
            EnergyHandler itemEnergy = Transfers.itemEnergy(itemHandler.getStackInSlot(slot));
            boolean done = itemEnergy == null
                    || (slot == SLOT_CHARGE ? itemEnergy.getAmountAsLong() >= itemEnergy.getCapacityAsLong()
                    : itemEnergy.getAmountAsLong() <= 0);
            return done ? super.extract(slot, resource, amount, transaction) : 0;
        }
    };

    public AccumulatorBlockEntity(BlockPos pos, BlockState blockState) {
        super(typeFor(tierOf(blockState)), pos, blockState);
        this.tier = tierOf(blockState);
        this.energyStorage = tier.isCreative()
                ? new CreativeEnergyStorage(tier.transfer())
                : new VoltaicEnergyStorage(tier.capacity(), tier.transfer(), tier.transfer(), this::saveChanges);
    }

    private static AccumulatorTier tierOf(BlockState state) {
        return state.getBlock() instanceof AccumulatorBlock cell ? cell.getTier() : AccumulatorTier.BASIC;
    }

    public static BlockEntityType<AccumulatorBlockEntity> typeFor(AccumulatorTier tier) {
        return switch (tier) {
            case BASIC -> VoltaicPower.BASIC_ACCUMULATOR_BE.get();
            case ADVANCED -> VoltaicPower.ADVANCED_ACCUMULATOR_BE.get();
            case ELITE -> VoltaicPower.ELITE_ACCUMULATOR_BE.get();
            case ULTIMATE -> VoltaicPower.ULTIMATE_ACCUMULATOR_BE.get();
            case CREATIVE -> VoltaicPower.CREATIVE_ACCUMULATOR_BE.get();
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AccumulatorBlockEntity entity) {
        int transfer = entity.tier.transfer();

        if (entity.energyStorage.getEnergyStored() > 0) {
            EnergyHandler itemEnergy = Transfers.itemEnergy(entity.itemHandler, SLOT_CHARGE);
            if (itemEnergy != null) EnergyHandlerUtil.move(entity.energyStorage, itemEnergy, transfer, null);
        }

        if (entity.energyStorage.getSpace() > 0) {
            EnergyHandler itemEnergy = Transfers.itemEnergy(entity.itemHandler, SLOT_DISCHARGE);
            if (itemEnergy != null) EnergyHandlerUtil.move(itemEnergy, entity.energyStorage, transfer, null);
        }

        // Into machines and cables (ours or other mods'), never into another Accumulator (except from a Creative one,
        // which fills them), and not back into a face that is charging it right now (another mod's cable network).
        if (entity.sideConfig.eject(TransferType.ENERGY)) {
            SideConfigHelper.ejectEnergy(entity, entity.neighbors, entity.energyStorage, transfer, !entity.tier.isCreative(),
                    entity.inputFaces);
        }
    }

    public int getComparatorSignal() {
        int energy = energyStorage.getEnergyStored();
        if (energy <= 0) return 0;
        return 1 + (int) ((long) energy * 14 / Math.max(1, energyStorage.getMaxEnergyStored()));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        drops();
    }

    public void drops() {
        if (level != null && !level.isClientSide()) {
            SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                inventory.setItem(i, itemHandler.getStackInSlot(i));
            }
            Containers.dropContents(level, worldPosition, inventory);
        }
    }

    private SidedHandlers sided() {
        if (sided == null) sided = new SidedHandlers(this, automationHandler, null, energyStorage);
        return sided;
    }

    @Nullable
    public EnergyHandler getEnergyStorage(@Nullable Direction side) {
        return inputFaces.wrap(side, sided().energy(side));
    }

    @Nullable
    public ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side) {
        return sided().items(side);
    }

    @Override
    public SideConfig getSideConfig() {
        return sideConfig;
    }

    /** Accumulators have no front: faces are shown as compass directions. */
    @Override
    public Direction getFacing() {
        return Direction.NORTH;
    }

    @Override
    public boolean hasFront() {
        return false;
    }

    @Override
    public void onSideConfigChanged() {
        SideConfigHelper.applyChange(this, sided());
    }

    /** Unrestricted handler for the GUI slots. */
    public VoltaicItemHandler getMenuItemHandler() {
        return itemHandler;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getEnergyCapacity() {
        return energyStorage.getMaxEnergyStored();
    }

    public AccumulatorTier getTier() {
        return tier;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AccumulatorMenu(containerId, playerInventory, this);
    }

    // Stored energy travels with the item when the cell is broken and placed again.
    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        energyStorage.setEnergy(components.getOrDefault(ModDataComponents.ENERGY.get(), 0));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (energyStorage.getEnergyStored() > 0 && !tier.isCreative()) {
            builder.set(ModDataComponents.ENERGY.get(), energyStorage.getEnergyStored());
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("Energy");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        itemHandler.serialize(output.child("Inventory"));
        energyStorage.serialize(output);
        sideConfig.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("Inventory").ifPresent(itemHandler::deserialize);
        energyStorage.deserialize(input);
        sideConfig.load(input);
        sided().invalidate();
    }
}
