package com.voltaicdynamics.generators;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import com.voltaicdynamics.core.transfer.VoltaicItemHandler;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStackTemplate;
import java.util.Optional;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.NeoForgeLootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.core.component.DataComponents;
import com.voltaicdynamics.core.blockentity.VoltaicBlockEntity;
import com.voltaicdynamics.VoltaicConfig;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

public class CombustionGeneratorBlockEntity extends VoltaicBlockEntity implements MenuProvider, SideConfigurable {
    /** FE/t pushed out to neighbours. */
    public static final int MAX_OUTPUT = 400;

    private final VoltaicEnergyStorage energyStorage;
    private final SideConfigHelper.Neighbors neighbors = new SideConfigHelper.Neighbors(this);
    private final SideConfig sideConfig = SideConfig.builder()
            .add(TransferType.ITEM, List.of(IoMode.BOTH, IoMode.INPUT, IoMode.OUTPUT, IoMode.NONE), IoMode.BOTH, true, false)
            .add(TransferType.ENERGY, List.of(IoMode.OUTPUT, IoMode.NONE), IoMode.OUTPUT, true, true)
            .build();
    private SidedHandlers sided;

    private final VoltaicItemHandler itemHandler = new VoltaicItemHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            saveChanges();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isFuel(stack);
        }
    };

    /** Pipes may insert fuel, and may only take out what is not fuel (the empty bucket left by lava). */
    private final ResourceHandler<ItemResource> automationHandler = new DelegatingResourceHandler<>(itemHandler) {
        @Override
        public int extract(int slot, ItemResource resource, int amount, TransactionContext transaction) {
            if (isFuel(itemHandler.getStackInSlot(slot))) return 0;
            return super.extract(slot, resource, amount, transaction);
        }
    };

    private int burnTime = 0;
    private int maxBurnTime = 0;
    private int currentRate = 0;

    public CombustionGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(VoltaicGenerators.COMBUSTION_GENERATOR_BE.get(), pos, blockState);
        this.energyStorage = new VoltaicEnergyStorage(VoltaicConfig.get(VoltaicConfig.COMBUSTION_CAPACITY),
                0, MAX_OUTPUT, this::saveChanges);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CombustionGeneratorBlockEntity entity) {
        int rate = VoltaicConfig.get(VoltaicConfig.COMBUSTION_GENERATION);
        boolean hasRoom = entity.energyStorage.getSpace() >= rate;

        // Only start a new item when the buffer can take a full tick of output,
        // and pause (keeping the fuel's remaining time) while the buffer is full.
        if (entity.burnTime <= 0 && hasRoom) {
            entity.consumeFuel(level);
        }

        boolean generating = false;
        if (entity.burnTime > 0 && hasRoom) {
            entity.burnTime--;
            entity.energyStorage.generateEnergy(rate);
            generating = true;
        }
        entity.currentRate = generating ? rate : 0;

        if (state.getValue(BlockStateProperties.LIT) != generating) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, generating), 3);
        }

        if (entity.sideConfig.eject(TransferType.ENERGY)) {
            SideConfigHelper.ejectEnergy(entity, entity.neighbors, entity.energyStorage, MAX_OUTPUT, false);
        }
        if (entity.sideConfig.eject(TransferType.ITEM) && Math.floorMod(level.getGameTime() + pos.asLong(), 10) == 0) {
            SideConfigHelper.ejectItems(entity, entity.neighbors, entity.automationHandler, new int[]{0}, 16);
        }
    }

    private void consumeFuel(Level level) {
        ItemStack fuel = itemHandler.getStackInSlot(0);
        int fuelBurn = burnTimeOf(fuel);
        if (fuelBurn <= 0) return;

        ItemStackTemplate remainderTemplate = fuel.getCraftingRemainder();
        ItemStack remainder = remainderTemplate == null ? ItemStack.EMPTY : remainderTemplate.create();
        int scaled = Math.max(1, (int) Math.round(fuelBurn * VoltaicConfig.get(VoltaicConfig.COMBUSTION_BURN_TIME_MULTIPLIER)));
        maxBurnTime = scaled;
        burnTime = scaled;
        if (fuel.getCount() == 1) {
            // Lava bucket -> empty bucket stays in the slot, like a vanilla furnace.
            itemHandler.setStackInSlot(0, remainder);
        } else {
            itemHandler.extractItem(0, 1, false);
            if (!remainder.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                        worldPosition.getZ() + 0.5, remainder);
            }
        }
        saveChanges();
    }

    /** Whether the stack burns in a furnace; works on the client too. */
    private static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.COOKING_FUEL);
    }

    /** Furnace burn time of the stack, resolved like a furnace does (server only). */
    private int burnTimeOf(ItemStack stack) {
        if (!isFuel(stack) || !(level instanceof ServerLevel server)) return 0;
        LootContext context = new LootContext.Builder(new LootParams.Builder(server)
                .withParameter(LootContextParams.BLOCK_STATE, getBlockState())
                .withParameter(LootContextParams.BLOCK_ENTITY, this)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
                .withParameter(LootContextParams.CONTAINER, new SimpleContainer(stack))
                .withOptionalParameter(NeoForgeLootContextParams.QUERIED_STACK, stack)
                .create(LootContextParamSets.CONTAINER_PROCESS))
                .create(Optional.empty());
        return ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, context, 0);
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
        return sided().energy(side);
    }

    @Nullable
    public ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side) {
        return sided().items(side);
    }

    @Override
    public SideConfig getSideConfig() {
        return sideConfig;
    }

    @Override
    public Direction getFacing() {
        return getBlockState().getValue(CombustionGeneratorBlock.FACING);
    }

    @Override
    public void onSideConfigChanged() {
        SideConfigHelper.applyChange(this, sided());
    }

    /** Unrestricted handler for the GUI slot, so players can take the empty bucket or fuel back. */
    public VoltaicItemHandler getMenuItemHandler() {
        return itemHandler;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getBurnTime() {
        return burnTime;
    }

    public int getEnergyCapacity() {
        return energyStorage.getMaxEnergyStored();
    }

    public int getMaxBurnTime() {
        return maxBurnTime;
    }

    /** FE/t produced this tick (0 when idle or full). Shown by the Jade integration. */
    public int getCurrentRate() {
        return currentRate;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.voltaicdynamics.combustion_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CombustionGeneratorMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        itemHandler.serialize(output.child("Inventory"));
        energyStorage.serialize(output);
        output.putInt("BurnTime", burnTime);
        output.putInt("MaxBurnTime", maxBurnTime);
        sideConfig.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("Inventory").ifPresent(itemHandler::deserialize);
        energyStorage.deserialize(input);
        burnTime = input.getIntOr("BurnTime", 0);
        maxBurnTime = input.getIntOr("MaxBurnTime", 0);
        sideConfig.load(input);
        sided().invalidate();
    }
}
