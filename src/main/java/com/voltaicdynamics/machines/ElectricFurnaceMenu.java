package com.voltaicdynamics.menu;

import com.voltaicdynamics.block.ModBlocks;
import com.voltaicdynamics.blockentity.ElectricFurnaceBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ElectricFurnaceMenu extends AbstractContainerMenu {
    public final ElectricFurnaceBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public ElectricFurnaceMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(7));
    }

    public ElectricFurnaceMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenus.ELECTRIC_FURNACE_MENU.get(), containerId);
        checkContainerDataCount(data, 7);
        this.blockEntity = (ElectricFurnaceBlockEntity) entity;
        this.level = inv.player.level();
        this.data = data;

        addSlot(new SlotItemHandler(blockEntity.getMenuItemHandler(), ElectricFurnaceBlockEntity.SLOT_INPUT, 56, 35));
        addSlot(new SlotItemHandler(blockEntity.getMenuItemHandler(), ElectricFurnaceBlockEntity.SLOT_OUTPUT, 116, 35) {
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

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public int getEnergy() {
        return ((data.get(1) & 0xFFFF) << 16) | (data.get(0) & 0xFFFF);
    }

    public int getCapacity() {
        return ((data.get(3) & 0xFFFF) << 16) | (data.get(2) & 0xFFFF);
    }

    public int getEnergyScaled(int pixels) {
        int capacity = getCapacity();
        return capacity <= 0 ? 0 : (int) ((long) getEnergy() * pixels / capacity);
    }

    public int getProgressScaled(int pixels) {
        int max = data.get(5);
        return max <= 0 ? 0 : data.get(4) * pixels / max;
    }

    /** FE/t used right now. */
    public int getUsage() {
        return data.get(6);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();

        if (index < 2) {
            if (!moveItemStackTo(sourceStack, 2, 38, true)) return ItemStack.EMPTY;
            if (index == 1) sourceSlot.onQuickCraft(sourceStack, copy);
        } else if (!moveItemStackTo(sourceStack, 0, 1, false)) {
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

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.ELECTRIC_FURNACE.get());
    }
}
