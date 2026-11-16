package com.voltaicdynamics.power;

import com.voltaicdynamics.core.menu.MachineMenu;
import com.voltaicdynamics.core.menu.SyncedInt;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class AccumulatorMenu extends MachineMenu {
    public final AccumulatorBlockEntity blockEntity;
    private final Level level;
    private final SyncedInt energy;
    private final SyncedInt capacity;
    private final SyncedInt maxTransfer;
    private final SyncedInt tier;

    public AccumulatorMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public AccumulatorMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(VoltaicPower.ACCUMULATOR_MENU.get(), containerId, entity);
        checkContainerSize(inv, 2);
        this.blockEntity = (AccumulatorBlockEntity) entity;
        this.level = inv.player.level();
        AccumulatorBlockEntity cell = this.blockEntity;
        this.energy = syncInt(cell::getEnergyStored);
        this.capacity = syncInt(cell::getEnergyCapacity);
        this.maxTransfer = syncInt(() -> cell.getTier().transfer());
        this.tier = syncInt(() -> cell.getTier().ordinal());

        addMachineSlot("charge", new SlotItemHandler(this.blockEntity.getMenuItemHandler(), 0, 0, 0));
        addMachineSlot("discharge", new SlotItemHandler(this.blockEntity.getMenuItemHandler(), 1, 0, 0));

        addPlayerInventory(inv);
    }

    public int getEnergy() {
        return energy.get();
    }

    public int getCapacity() {
        return capacity.get();
    }

    public int getMaxTransfer() {
        return maxTransfer.get();
    }

    public boolean isAdvanced() {
        return tier.get() == AccumulatorTier.ADVANCED.ordinal();
    }

    public int getEnergyScaled(int pixels) {
        int energy = getEnergy();
        int capacity = getCapacity();
        if (capacity == 0) return 0;
        return (int) (((long) energy * pixels) / capacity);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Slots 0 and 1 are Cell slots
        if (index == 0 || index == 1) {
            if (!moveItemStackTo(sourceStack, 2, 38, true)) {
                return ItemStack.EMPTY;
            }
            sourceSlot.onQuickCraft(sourceStack, copyOfSourceStack);
        } else {
            // From player inventory into Cell slots
            if (!moveItemStackTo(sourceStack, 0, 2, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(player, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player,
                blockEntity.getBlockState().getBlock());
    }
}
