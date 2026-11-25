package com.voltaicdynamics.gametest;

import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import com.voltaicdynamics.core.transfer.Transfers;
import com.voltaicdynamics.core.transfer.VoltaicItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import com.voltaicdynamics.core.energy.VoltaicEnergyStorage;
import com.voltaicdynamics.machines.ProcessingMachineBlockEntity;
import com.voltaicdynamics.power.AccumulatorBlockEntity;

/** Helpers shared by the game test classes. */
final class GameTestSupport {
    private GameTestSupport() {
    }

    static final String EMPTY = "empty";

    static VoltaicEnergyStorage storageOf(AccumulatorBlockEntity cell) {
        return (VoltaicEnergyStorage) cell.getEnergyStorage(null);
    }

    static void power(ProcessingMachineBlockEntity machine, int amount) {
        ((VoltaicEnergyStorage) machine.getEnergyStorage(null)).setEnergy(amount);
    }

    static net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> recipeKey(String path) {
        return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                com.voltaicdynamics.VoltaicDynamics.id(path));
    }

    /** What a recipe makes, as shown in recipe books (recipes no longer expose a plain result item). */
    static ItemStack resultOf(net.minecraft.world.item.crafting.Recipe<?> recipe, net.minecraft.world.level.Level level) {
        if (recipe instanceof com.voltaicdynamics.machines.recipe.AlloyingRecipe alloying) return alloying.resultStack();
        if (recipe instanceof com.voltaicdynamics.machines.recipe.CrushingRecipe crushing) return crushing.resultStack();
        var context = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(level);
        for (var display : recipe.display()) {
            ItemStack stack = display.result().resolveForFirstStack(context);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** Slot-style calls on an item handler, each in its own transaction, so the tests read like before. */
    static Items items(@Nullable ResourceHandler<ItemResource> handler) {
        if (handler == null) throw new GameTestAssertException(Component.literal("no item handler"), 0);
        return new Items(handler);
    }

    /** Fill and drain on a fluid handler, each in its own transaction. */
    static Fluids fluids(@Nullable ResourceHandler<FluidResource> handler) {
        if (handler == null) throw new GameTestAssertException(Component.literal("no fluid handler"), 0);
        return new Fluids(handler);
    }

    record Items(ResourceHandler<ItemResource> handler) {
        int getSlots() {
            return handler.size();
        }

        /** For our own inventories the live stack, as menus see it; a copy otherwise. */
        ItemStack getStackInSlot(int slot) {
            if (handler instanceof VoltaicItemHandler own) return own.getStackInSlot(slot);
            return Transfers.getStack(handler, slot);
        }

        ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return Transfers.insertItem(handler, slot, stack, simulate);
        }

        ItemStack extractItem(int slot, int amount, boolean simulate) {
            return Transfers.extractItem(handler, slot, amount, simulate);
        }

        /** Replaces a slot's contents, bypassing the slot rules (only for our own inventories). */
        void setStackInSlot(int slot, ItemStack stack) {
            ((VoltaicItemHandler) handler).setStackInSlot(slot, stack);
        }
    }

    record Fluids(ResourceHandler<FluidResource> handler) {
        int fill(FluidStack stack, boolean simulate) {
            return Transfers.fill(handler, stack, simulate);
        }

        FluidStack drain(int amount, boolean simulate) {
            return Transfers.drain(handler, amount, simulate);
        }

        FluidStack drain(FluidStack stack, boolean simulate) {
            return Transfers.drain(handler, stack, simulate);
        }
    }
}
