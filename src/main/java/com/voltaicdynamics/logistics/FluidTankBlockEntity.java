package com.voltaicdynamics.logistics;

import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import com.voltaicdynamics.core.transfer.Transfers;
import com.voltaicdynamics.core.transfer.VoltaicItemHandler;
import com.voltaicdynamics.core.transfer.VoltaicFluidTank;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.component.DataComponentGetter;
import com.voltaicdynamics.core.blockentity.VoltaicBlockEntity;
import com.voltaicdynamics.VoltaicConfig;
import com.voltaicdynamics.core.ModDataComponents;
import com.voltaicdynamics.core.sideconfig.IoMode;
import com.voltaicdynamics.core.sideconfig.SideConfig;
import com.voltaicdynamics.core.sideconfig.SideConfigHelper;
import com.voltaicdynamics.core.sideconfig.SideConfigurable;
import com.voltaicdynamics.core.sideconfig.SidedHandlers;
import com.voltaicdynamics.core.sideconfig.TransferType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Single-fluid tank. Keeps its fluid when broken, fills and empties buckets and other containers (right-click
 * or the GUI slots), shows the fluid level through the glass and can auto-eject from configured faces.
 */
public class FluidTankBlockEntity extends VoltaicBlockEntity implements MenuProvider, SideConfigurable {
    public static final int SLOT_IN = 0;
    public static final int SLOT_OUT = 1;
    private static final int SYNC_INTERVAL = 4;

    private final FluidTankTier tier;
    private final VoltaicFluidTank tank;
    private final VoltaicItemHandler itemHandler = new VoltaicItemHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            saveChanges();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_IN && Transfers.isFluidContainer(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_IN ? 1 : 64;
        }
    };
    /** Pipes: insert containers into the in slot, take them from the out slot. */
    private final ResourceHandler<ItemResource> automationHandler = new DelegatingResourceHandler<>(itemHandler) {
        @Override
        public int insert(int slot, ItemResource resource, int amount, TransactionContext transaction) {
            return slot == SLOT_IN ? super.insert(slot, resource, amount, transaction) : 0;
        }

        @Override
        public int extract(int slot, ItemResource resource, int amount, TransactionContext transaction) {
            return slot == SLOT_OUT ? super.extract(slot, resource, amount, transaction) : 0;
        }
    };
    private final SideConfig sideConfig = SideConfig.builder()
            .add(TransferType.FLUID, List.of(IoMode.BOTH, IoMode.INPUT, IoMode.OUTPUT, IoMode.NONE), IoMode.BOTH, true, false)
            .add(TransferType.ITEM, List.of(IoMode.BOTH, IoMode.INPUT, IoMode.OUTPUT, IoMode.NONE), IoMode.BOTH, false, false)
            .build();
    private final SidedHandlers sided;
    private final SideConfigHelper.Neighbors neighbors = new SideConfigHelper.Neighbors(this);
    private boolean needsSync;
    private int lastComparator = -1;

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(VoltaicLogistics.FLUID_TANK_BE.get(), pos, state);
        this.tier = state.getBlock() instanceof FluidTankBlock block ? block.getTier() : FluidTankTier.BASIC;
        this.tank = tier.isCreative() ? new CreativeFluidTank(this::tankChanged) : new VoltaicFluidTank(tier.capacity()) {
            @Override
            protected void onContentsChanged() {
                tankChanged();
            }
        };
        this.sided = new SidedHandlers(this, automationHandler, tank, null);
    }

    /** Capacity of the basic tank in mB. */
    public static int capacity() {
        return FluidTankTier.BASIC.capacity();
    }

    private void tankChanged() {
        saveChanges();
        needsSync = true;
    }

    public FluidTankTier getTier() {
        return tier;
    }

    /** Empties a Creative tank so it can take another fluid; other tanks keep their fluid. */
    public boolean clearCreative() {
        if (!(tank instanceof CreativeFluidTank creative) || creative.isEmpty()) return false;
        creative.clear();
        return true;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluidTankBlockEntity tank) {
        tank.processContainers();
        if (tank.sideConfig.eject(TransferType.FLUID)) {
            SideConfigHelper.ejectFluid(tank, tank.neighbors, tank.tank, VoltaicConfig.get(VoltaicConfig.TANK_EJECT_RATE));
        }
        if (tank.needsSync && level.getGameTime() % SYNC_INTERVAL == 0) {
            tank.needsSync = false;
            level.sendBlockUpdated(pos, state, state, 2);
            int comparator = tank.getComparatorSignal();
            if (comparator != tank.lastComparator) {
                tank.lastComparator = comparator;
                level.updateNeighbourForOutputSignal(pos, state.getBlock());
            }
        }
    }

    /** Container in the in slot: empty it into the tank, or fill it from the tank when it is empty. */
    private void processContainers() {
        Transfers.processContainer(itemHandler, SLOT_IN, SLOT_OUT, tank, true);
    }

    public FluidStack getFluid() {
        return tank.getFluid();
    }

    public VoltaicFluidTank getTank() {
        return tank;
    }

    public int getComparatorSignal() {
        if (tank.isEmpty()) return 0;
        return 1 + (int) ((long) tank.getFluidAmount() * 14 / Math.max(1, tank.getCapacity()));
    }

    @Nullable
    public ResourceHandler<FluidResource> getFluidHandler(@Nullable Direction side) {
        return sided.fluids(side);
    }

    @Nullable
    public ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side) {
        return sided.items(side);
    }

    public VoltaicItemHandler getMenuItemHandler() {
        return itemHandler;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        drops();
    }

    public void drops() {
        if (level != null && !level.isClientSide()) {
            SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
            for (int i = 0; i < itemHandler.getSlots(); i++) inventory.setItem(i, itemHandler.getStackInSlot(i));
            Containers.dropContents(level, worldPosition, inventory);
        }
    }

    @Override
    public SideConfig getSideConfig() {
        return sideConfig;
    }

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
        SideConfigHelper.applyChange(this, sided);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FluidTankMenu(containerId, playerInventory, this);
    }

    // Fluid travels with the item when the tank is broken.
    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        tank.setFluid(components.getOrDefault(ModDataComponents.FLUID.get(), SimpleFluidContent.EMPTY).copy());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (!tank.isEmpty()) builder.set(ModDataComponents.FLUID.get(), SimpleFluidContent.copyOf(tank.getFluid()));
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("Tank");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.save(output, "Tank");
        itemHandler.serialize(output.child("Inventory"));
        sideConfig.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.load(input, "Tank");
        input.child("Inventory").ifPresent(itemHandler::deserialize);
        sideConfig.load(input);
        sided.invalidate();
    }

    /** Only the fluid goes to clients (for the renderer). */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        tank.save(output, "Tank");
        return output.buildResult();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
