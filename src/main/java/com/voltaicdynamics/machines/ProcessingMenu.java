package com.voltaicdynamics.machines;

import com.voltaicdynamics.core.menu.MachineMenu;
import com.voltaicdynamics.core.menu.SyncedInt;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Menu of a {@link ProcessingMachineBlockEntity}: input slots, one output slot, then the player inventory. */
public abstract class ProcessingMenu extends MachineMenu {
    public final ProcessingMachineBlockEntity blockEntity;
    private final int machineSlots;
    private final SyncedInt energy;
    private final SyncedInt capacity;
    private final SyncedInt progress;
    private final SyncedInt maxProgress;
    private final SyncedInt usage;

    /**
     * @param inputPositions x,y pairs of the input slots
     */
    protected ProcessingMenu(MenuType<?> type, int containerId, Inventory inv, BlockEntity entity,
                             int[][] inputPositions, int outputX, int outputY) {
        super(type, containerId, entity);
        ProcessingMachineBlockEntity machine = (ProcessingMachineBlockEntity) entity;
        this.blockEntity = machine;
        this.energy = syncInt(machine::getEnergyStored);
        this.capacity = syncInt(machine::getEnergyCapacity);
        this.progress = syncInt(machine::getProgress);
        this.maxProgress = syncInt(machine::getMaxProgress);
        this.usage = syncInt(() -> machine.isActive() ? machine.energyPerTick() : 0);

        for (int i = 0; i < inputPositions.length; i++) {
            addSlot(new SlotItemHandler(blockEntity.getMenuItemHandler(), i, inputPositions[i][0], inputPositions[i][1]));
        }
        addSlot(new SlotItemHandler(blockEntity.getMenuItemHandler(), blockEntity.outputSlot(), outputX, outputY) {
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
        this.machineSlots = inputPositions.length + 1;

        addPlayerInventory(inv, 84);
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

    public int getProgressScaled(int pixels) {
        int max = maxProgress.get();
        return max <= 0 ? 0 : (int) ((long) progress.get() * pixels / max);
    }

    /** FE/t used right now. */
    public int getUsage() {
        return usage.get();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();

        if (index < machineSlots) {
            if (!moveItemStackTo(sourceStack, machineSlots, machineSlots + PLAYER_SLOTS, true)) return ItemStack.EMPTY;
            if (index == machineSlots - 1) sourceSlot.onQuickCraft(sourceStack, copy);
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
