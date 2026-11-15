package com.voltaicdynamics.menu;

import com.voltaicdynamics.block.ModBlocks;
import com.voltaicdynamics.blockentity.CombustionGeneratorBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class CombustionGeneratorMenu extends AbstractContainerMenu {
    public final CombustionGeneratorBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    // Client-side constructor (invoked via IMenuTypeExtension)
    public CombustionGeneratorMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
    }

    // Server-side constructor
    public CombustionGeneratorMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenus.COMBUSTION_GENERATOR_MENU.get(), containerId);
        checkContainerSize(inv, 1);
        this.blockEntity = (CombustionGeneratorBlockEntity) entity;
        this.level = inv.player.level();
        this.data = data;

        // Machine Fuel Slot (x: 80, y: 35)
        this.addSlot(new SlotItemHandler(this.blockEntity.getItemHandler(null), 0, 80, 35));

        // Player Inventory (3 rows x 9 columns)
        addPlayerInventory(inv);
        // Player Hotbar (9 slots)
        addPlayerHotbar(inv);

        // Register synced data
        addDataSlots(data);
    }

    public boolean isBurning() {
        return data.get(2) > 0;
    }

    public int getBurnScaled(int pixels) {
        int burnTime = data.get(2);
        int maxBurnTime = data.get(3);
        if (maxBurnTime == 0) return 0;
        return (burnTime * pixels) / maxBurnTime;
    }

    public int getEnergy() {
        return (data.get(1) << 16) | (data.get(0) & 0xFFFF);
    }

    public int getMaxEnergy() {
        return CombustionGeneratorBlockEntity.CAPACITY;
    }

    public int getEnergyScaled(int pixels) {
        int energy = getEnergy();
        int maxEnergy = getMaxEnergy();
        if (maxEnergy == 0) return 0;
        return (int) (((long) energy * pixels) / maxEnergy);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Slot 0 is machine fuel slot
        if (index == 0) {
            if (!moveItemStackTo(sourceStack, 1, 37, true)) {
                return ItemStack.EMPTY;
            }
            sourceSlot.onQuickCraft(sourceStack, copyOfSourceStack);
        } else {
            // Shift-click from player inventory into fuel slot
            if (!moveItemStackTo(sourceStack, 0, 1, false)) {
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
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.COMBUSTION_GENERATOR.get());
    }
}
