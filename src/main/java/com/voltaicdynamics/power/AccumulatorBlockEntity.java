package com.voltaicdynamics.power;

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
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
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

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            saveChanges();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            IEnergyStorage itemEnergy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (itemEnergy == null) return false;
            return slot == SLOT_CHARGE ? itemEnergy.canReceive() : itemEnergy.canExtract();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    /** What pipes and hoppers see: insert into either slot, but only take items out once they are done. */
    private final IItemHandler automationHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return itemHandler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            IEnergyStorage itemEnergy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            boolean done = itemEnergy == null
                    || (slot == SLOT_CHARGE ? itemEnergy.getEnergyStored() >= itemEnergy.getMaxEnergyStored()
                    : itemEnergy.getEnergyStored() <= 0);
            return done ? itemHandler.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return itemHandler.isItemValid(slot, stack);
        }
    };

    public AccumulatorBlockEntity(BlockPos pos, BlockState blockState) {
        super(typeFor(tierOf(blockState)), pos, blockState);
        this.tier = tierOf(blockState);
        this.energyStorage = new VoltaicEnergyStorage(tier.capacity(), tier.transfer(), tier.transfer(), this::saveChanges);
    }

    private static AccumulatorTier tierOf(BlockState state) {
        return state.getBlock() instanceof AccumulatorBlock cell ? cell.getTier() : AccumulatorTier.BASIC;
    }

    public static BlockEntityType<AccumulatorBlockEntity> typeFor(AccumulatorTier tier) {
        return tier == AccumulatorTier.ADVANCED
                ? VoltaicPower.ADVANCED_ACCUMULATOR_BE.get()
                : VoltaicPower.BASIC_ACCUMULATOR_BE.get();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AccumulatorBlockEntity entity) {
        int transfer = entity.tier.transfer();

        ItemStack chargeStack = entity.itemHandler.getStackInSlot(SLOT_CHARGE);
        if (!chargeStack.isEmpty() && entity.energyStorage.getEnergyStored() > 0) {
            IEnergyStorage itemEnergy = chargeStack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (itemEnergy != null && itemEnergy.canReceive()) {
                int accepted = itemEnergy.receiveEnergy(Math.min(entity.energyStorage.getEnergyStored(), transfer), false);
                if (accepted > 0) entity.energyStorage.consumeEnergy(accepted);
            }
        }

        ItemStack dischargeStack = entity.itemHandler.getStackInSlot(SLOT_DISCHARGE);
        if (!dischargeStack.isEmpty() && entity.energyStorage.getSpace() > 0) {
            IEnergyStorage itemEnergy = dischargeStack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (itemEnergy != null && itemEnergy.canExtract()) {
                int extracted = itemEnergy.extractEnergy(Math.min(entity.energyStorage.getSpace(), transfer), false);
                if (extracted > 0) entity.energyStorage.generateEnergy(extracted);
            }
        }

        // Into machines and cables (ours or other mods'), never into another Accumulator, and not back into a
        // face that is charging it right now (another mod's cable network).
        if (entity.sideConfig.eject(TransferType.ENERGY)) {
            SideConfigHelper.ejectEnergy(entity, entity.neighbors, entity.energyStorage, transfer, true, entity.inputFaces);
        }
    }

    public int getComparatorSignal() {
        int energy = energyStorage.getEnergyStored();
        if (energy <= 0) return 0;
        return 1 + (int) ((long) energy * 14 / Math.max(1, energyStorage.getMaxEnergyStored()));
    }

    public void drops() {
        if (level != null && !level.isClientSide) {
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
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return inputFaces.wrap(side, sided().energy(side));
    }

    @Nullable
    public IItemHandler getItemHandler(@Nullable Direction side) {
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
    public IItemHandler getMenuItemHandler() {
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
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        energyStorage.setEnergy(input.getOrDefault(ModDataComponents.ENERGY.get(), 0));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (energyStorage.getEnergyStored() > 0) {
            builder.set(ModDataComponents.ENERGY.get(), energyStorage.getEnergyStored());
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Energy");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        tag.put("Energy", energyStorage.serializeNBT(registries));
        tag.put("SideConfig", sideConfig.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        if (tag.contains("Energy")) energyStorage.deserializeNBT(registries, tag.getCompound("Energy"));
        if (tag.contains("SideConfig")) sideConfig.load(tag.getCompound("SideConfig"));
        sided().invalidate();
    }
}
