package com.voltaicdynamics.logistics;

import com.voltaicdynamics.core.menu.MachineMenu;
import com.voltaicdynamics.core.menu.SyncedInt;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class FluidTankMenu extends MachineMenu {
    public final FluidTankBlockEntity blockEntity;
    private final SyncedInt amount;
    private final SyncedInt capacity;

    public FluidTankMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public FluidTankMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(VoltaicLogistics.FLUID_TANK_MENU.get(), containerId, entity);
        this.blockEntity = (FluidTankBlockEntity) entity;
        FluidTankBlockEntity tank = this.blockEntity;
        this.amount = syncInt(() -> tank.getTank().getFluidAmount());
        this.capacity = syncInt(() -> tank.getTank().getCapacity());
        addSlot(new SlotItemHandler(blockEntity.getMenuItemHandler(), FluidTankBlockEntity.SLOT_IN, 44, 20));
        addSlot(new SlotItemHandler(blockEntity.getMenuItemHandler(), FluidTankBlockEntity.SLOT_OUT, 44, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addPlayerInventory(inv, 84);
    }

    public int getAmount() {
        return amount.get();
    }

    public int getCapacity() {
        return capacity.get();
    }

    /** Fluid type comes from the client block entity (synced for the renderer); the amount from the menu. */
    public FluidStack getFluid() {
        return blockEntity.getFluid();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();
        if (index < 2) {
            if (!moveItemStackTo(sourceStack, 2, 2 + PLAYER_SLOTS, true)) return ItemStack.EMPTY;
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
}
