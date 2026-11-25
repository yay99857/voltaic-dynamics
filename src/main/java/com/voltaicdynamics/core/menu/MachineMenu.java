package com.voltaicdynamics.core.menu;

import com.voltaicdynamics.core.sideconfig.SideConfig;
import com.voltaicdynamics.core.sideconfig.SideConfigurable;
import com.voltaicdynamics.core.sideconfig.TransferType;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntSupplier;

/**
 * Base of every Voltaic machine menu: player inventory slots and the side configuration, which is synced in
 * one {@link SyncedInt} per {@link TransferType} and changed through {@link #clickMenuButton} (no custom packets).
 */
public abstract class MachineMenu extends AbstractContainerMenu {
    public static final int PLAYER_SLOTS = 36;

    protected final BlockEntity machine;
    /** True on the server, where synced values read the machine; false on the client, where they are received. */
    private final boolean serverSide;
    private final SyncedInt[] sideBits = new SyncedInt[TransferType.values().length];
    /** Name of each machine slot, by slot index, for the screen layout (see {@link #addMachineSlot}). */
    private final Map<Integer, String> slotNames = new HashMap<>();

    protected MachineMenu(@Nullable MenuType<?> type, int containerId, BlockEntity machine) {
        super(type, containerId);
        this.machine = machine;
        this.serverSide = !machine.getLevel().isClientSide();
        SideConfigurable configurable = machine instanceof SideConfigurable c ? c : null;
        for (TransferType transfer : TransferType.values()) {
            sideBits[transfer.ordinal()] = syncInt(() -> configurable == null ? 0 : configurable.getSideConfig().pack(transfer));
        }
    }

    /**
     * Syncs one value from the server to this menu. Call it in the constructor, in the same order on both sides
     * (the client constructor runs the same code, so this holds unless a call is behind a side check).
     * {@code serverValue} is only read on the server.
     */
    protected final SyncedInt syncInt(IntSupplier serverValue) {
        SyncedInt synced = new SyncedInt(serverSide ? serverValue : null);
        addDataSlot(synced.lowHalf());
        addDataSlot(synced.highHalf());
        return synced;
    }

    /**
     * Adds a machine slot under a name. The screen layout ({@code assets/<mod>/screens/<machine>.json}) places it
     * on the client, so the slot is created at (0, 0) here: positions live only in the layout.
     */
    protected Slot addMachineSlot(String name, Slot slot) {
        Slot added = addSlot(slot);
        slotNames.put(added.index, name);
        return added;
    }

    /** The name given in {@link #addMachineSlot}, or null for player inventory slots. */
    @Nullable
    public String slotName(Slot slot) {
        return slotNames.get(slot.index);
    }

    /** Player inventory and hotbar, placed by the layout's {@code player_inventory} origin on the client. */
    protected void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    public BlockEntity getMachine() {
        return machine;
    }

    @Nullable
    public SideConfigurable getConfigurable() {
        return machine instanceof SideConfigurable configurable ? configurable : null;
    }

    /** Packed configuration of {@code type} as last synced (see {@link SideConfig#pack}). */
    public int getSideBits(TransferType type) {
        return sideBits[type.ordinal()].get();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= SideConfig.BUTTON_BASE && machine instanceof SideConfigurable configurable) {
            if (configurable.getSideConfig().handleButton(id)) {
                configurable.onSideConfigChanged();
                return true;
            }
            return false;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos()), player,
                machine.getBlockState().getBlock());
    }
}
