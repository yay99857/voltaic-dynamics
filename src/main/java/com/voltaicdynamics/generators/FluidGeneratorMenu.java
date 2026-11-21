package com.voltaicdynamics.generators;

import com.voltaicdynamics.core.menu.MachineMenu;
import com.voltaicdynamics.core.menu.SyncedInt;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import com.voltaicdynamics.core.menu.FixedSlot;

/** Fluid Generator GUI: fuel container slots, the fuel gauge and the energy gauge. */
public class FluidGeneratorMenu extends MachineMenu {
    public final FluidGeneratorBlockEntity blockEntity;
    private final SyncedInt energy;
    private final SyncedInt capacity;
    private final SyncedInt fluidId;
    private final SyncedInt amount;
    private final SyncedInt tankCapacity;
    private final SyncedInt rate;

    public FluidGeneratorMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public FluidGeneratorMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(VoltaicGenerators.FLUID_GENERATOR_MENU.get(), containerId, entity);
        FluidGeneratorBlockEntity generator = (FluidGeneratorBlockEntity) entity;
        this.blockEntity = generator;
        this.energy = syncInt(generator::getEnergyStored);
        this.capacity = syncInt(generator::getEnergyCapacity);
        this.fluidId = syncInt(() -> BuiltInRegistries.FLUID.getId(generator.getFluid().getFluid()));
        this.amount = syncInt(() -> generator.getTank().getFluidAmount());
        this.tankCapacity = syncInt(() -> generator.getTank().getCapacity());
        this.rate = syncInt(generator::getCurrentRate);
        addMachineSlot("in", new FixedSlot(generator.getMenuItemHandler(), FluidGeneratorBlockEntity.SLOT_IN, 0, 0));
        addMachineSlot("out", new FixedSlot(generator.getMenuItemHandler(), FluidGeneratorBlockEntity.SLOT_OUT, 0, 0) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addPlayerInventory(inv);
    }

    public int getEnergy() {
        return energy.get();
    }

    public int getCapacity() {
        return capacity.get();
    }

    /** The fuel as last synced (type and amount both come through the menu). */
    public FluidStack getFluid() {
        int mb = amount.get();
        return mb <= 0 ? FluidStack.EMPTY : new FluidStack(BuiltInRegistries.FLUID.byId(fluidId.get()), mb);
    }

    public int getTankCapacity() {
        return tankCapacity.get();
    }

    public int getRate() {
        return rate.get();
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
