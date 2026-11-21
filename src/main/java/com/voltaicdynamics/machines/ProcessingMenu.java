package com.voltaicdynamics.machines;

import com.voltaicdynamics.core.menu.MachineMenu;
import com.voltaicdynamics.core.menu.SyncedInt;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.voltaicdynamics.core.menu.FixedSlot;

/** Menu of a {@link ProcessingMachineBlockEntity}: input slots, one output slot, then the player inventory. */
public abstract class ProcessingMenu extends MachineMenu {
    public final ProcessingMachineBlockEntity blockEntity;
    private final int machineSlots;
    private final SyncedInt energy;
    private final SyncedInt capacity;
    private final SyncedInt progress;
    private final SyncedInt maxProgress;
    private final SyncedInt usage;
    private final SyncedInt tier;

    /**
     * Slots: {@code input_0..n} and {@code output}, then the player inventory, then {@code upgrade_0..1} (after
     * the player inventory, so recipe viewers keep their slot ranges). The screen layout places them all.
     */
    protected ProcessingMenu(MenuType<?> type, int containerId, Inventory inv, BlockEntity entity) {
        super(type, containerId, entity);
        ProcessingMachineBlockEntity machine = (ProcessingMachineBlockEntity) entity;
        this.blockEntity = machine;
        this.energy = syncInt(machine::getEnergyStored);
        this.capacity = syncInt(machine::getEnergyCapacity);
        this.progress = syncInt(machine::getProgress);
        this.maxProgress = syncInt(machine::getMaxProgress);
        this.usage = syncInt(() -> machine.isActive() ? machine.activeEnergyPerTick() : 0);
        this.tier = syncInt(() -> machine.getTier().ordinal());

        int inputs = blockEntity.getInputSlots();
        for (int i = 0; i < inputs; i++) {
            addMachineSlot("input_" + i, new FixedSlot(blockEntity.getMenuItemHandler(), i, 0, 0));
        }
        addMachineSlot("output", new FixedSlot(blockEntity.getMenuItemHandler(), blockEntity.outputSlot(), 0, 0) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                stack.onCraftedBy(player.level(), player, stack.getCount());
                blockEntity.awardExperience(player);
                super.onTake(player, stack);
            }
        });
        this.machineSlots = inputs + 1;

        addPlayerInventory(inv);
        for (int i = 0; i < ProcessingMachineBlockEntity.UPGRADE_SLOTS; i++) {
            addMachineSlot("upgrade_" + i, new FixedSlot(blockEntity.getUpgradeHandler(), i, 0, 0));
        }
    }

    public int getMachineSlots() {
        return machineSlots;
    }

    public int getInputCount() {
        return machineSlots - 1;
    }

    public int getEnergy() {
        return energy.get();
    }

    public int getCapacity() {
        return capacity.get();
    }

    public int getEnergyScaled(int pixels) {
        int capacity = getCapacity();
        return capacity <= 0 ? 0 : (int) ((long) getEnergy() * pixels / capacity);
    }

    public double getProgressFraction() {
        int max = maxProgress.get();
        return max <= 0 ? 0 : (double) progress.get() / max;
    }

    public int getProgressScaled(int pixels) {
        int max = maxProgress.get();
        return max <= 0 ? 0 : (int) ((long) progress.get() * pixels / max);
    }

    /** FE/t used right now. */
    public MachineTier getTier() {
        return MachineTier.byOrdinal(tier.get());
    }

    public int getUsage() {
        return usage.get();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();

        int playerEnd = machineSlots + PLAYER_SLOTS;
        int upgradeEnd = playerEnd + ProcessingMachineBlockEntity.UPGRADE_SLOTS;
        if (index < machineSlots || index >= playerEnd) {
            if (!moveItemStackTo(sourceStack, machineSlots, playerEnd, true)) return ItemStack.EMPTY;
            if (index == machineSlots - 1) sourceSlot.onQuickCraft(sourceStack, copy);
        } else if (sourceStack.getItem() instanceof UpgradeItem) {
            if (!moveItemStackTo(sourceStack, playerEnd, upgradeEnd, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(sourceStack, 0, machineSlots - 1, false)) {
            return ItemStack.EMPTY;
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(player, sourceStack);
        return copy;
    }
}
