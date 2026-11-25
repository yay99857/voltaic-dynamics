package com.voltaicdynamics.core.sideconfig;

import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Per-face configuration of a machine, one entry per {@link TransferType} it supports (Mekanism style):
 * every face is off, input, output or both, and output faces can auto-eject.
 *
 * <p>Each entry packs into 13 bits (2 per face + the eject flag) so the menu can sync it in one data slot.
 */
public final class SideConfig {
    private final Map<TransferType, Entry> entries = new EnumMap<>(TransferType.class);

    public static Builder builder() {
        return new Builder();
    }

    public boolean has(TransferType type) {
        return entries.containsKey(type);
    }

    public Set<TransferType> types() {
        return entries.keySet();
    }

    @Nullable
    public Entry entry(TransferType type) {
        return entries.get(type);
    }

    /** Mode of the world face {@code side} for a machine facing {@code facing}; null side means "internal": BOTH. */
    public IoMode mode(TransferType type, @Nullable Direction side, Direction facing) {
        Entry entry = entries.get(type);
        if (entry == null) return IoMode.NONE;
        if (side == null) return IoMode.BOTH;
        return entry.modes[RelativeSide.of(facing, side).ordinal()];
    }

    public IoMode mode(TransferType type, RelativeSide side) {
        Entry entry = entries.get(type);
        return entry == null ? IoMode.NONE : entry.modes[side.ordinal()];
    }

    public boolean eject(TransferType type) {
        Entry entry = entries.get(type);
        return entry != null && entry.eject;
    }

    /** Applies a button press from the GUI. Returns true when something changed. */
    public boolean cycle(TransferType type, RelativeSide side, boolean backwards) {
        Entry entry = entries.get(type);
        if (entry == null) return false;
        List<IoMode> allowed = entry.allowed;
        int index = allowed.indexOf(entry.modes[side.ordinal()]);
        int next = backwards ? (index - 1 + allowed.size()) % allowed.size() : (index + 1) % allowed.size();
        entry.modes[side.ordinal()] = allowed.get(next);
        return true;
    }

    public boolean toggleEject(TransferType type) {
        Entry entry = entries.get(type);
        if (entry == null || !entry.ejectSupported) return false;
        entry.eject = !entry.eject;
        return true;
    }

    public void set(TransferType type, RelativeSide side, IoMode mode) {
        Entry entry = entries.get(type);
        if (entry != null && entry.allowed.contains(mode)) entry.modes[side.ordinal()] = mode;
    }

    public void setEject(TransferType type, boolean eject) {
        Entry entry = entries.get(type);
        if (entry != null && entry.ejectSupported) entry.eject = eject;
    }

    public int pack(TransferType type) {
        Entry entry = entries.get(type);
        if (entry == null) return 0;
        int bits = 0;
        for (int i = 0; i < 6; i++) bits |= entry.modes[i].ordinal() << (i * 2);
        if (entry.eject) bits |= 1 << 12;
        return bits;
    }

    public static IoMode unpackMode(int bits, RelativeSide side) {
        return IoMode.byId((bits >> (side.ordinal() * 2)) & 3);
    }

    public static boolean unpackEject(int bits) {
        return (bits & (1 << 12)) != 0;
    }

    /** Writes a "SideConfig" child of {@code output}. */
    public void save(ValueOutput output) {
        ValueOutput tag = output.child("SideConfig");
        for (Map.Entry<TransferType, Entry> e : entries.entrySet()) {
            tag.putInt(e.getKey().name(), pack(e.getKey()));
        }
    }

    /** Reads the "SideConfig" child of {@code input}, if there is one. */
    public void load(ValueInput input) {
        ValueInput tag = input.childOrEmpty("SideConfig");
        for (Map.Entry<TransferType, Entry> e : entries.entrySet()) {
            java.util.Optional<Integer> stored = tag.getInt(e.getKey().name());
            if (stored.isEmpty()) continue;
            int bits = stored.get();
            Entry entry = e.getValue();
            for (RelativeSide side : RelativeSide.values()) {
                IoMode mode = unpackMode(bits, side);
                if (entry.allowed.contains(mode)) entry.modes[side.ordinal()] = mode;
            }
            entry.eject = entry.ejectSupported && unpackEject(bits);
        }
    }

    public static final class Entry {
        final List<IoMode> allowed;
        final IoMode[] modes = new IoMode[6];
        final boolean ejectSupported;
        boolean eject;

        Entry(List<IoMode> allowed, Function<RelativeSide, IoMode> defaults, boolean ejectSupported, boolean ejectDefault) {
            this.allowed = List.copyOf(allowed);
            this.ejectSupported = ejectSupported;
            this.eject = ejectSupported && ejectDefault;
            for (RelativeSide side : RelativeSide.values()) modes[side.ordinal()] = defaults.apply(side);
        }

        public List<IoMode> allowed() {
            return allowed;
        }

        public boolean ejectSupported() {
            return ejectSupported;
        }
    }

    public static final class Builder {
        private final SideConfig config = new SideConfig();

        /** Adds a transfer type; {@code allowed} is the cycle order of the face buttons. */
        public Builder add(TransferType type, List<IoMode> allowed, Function<RelativeSide, IoMode> defaults,
                           boolean ejectSupported, boolean ejectDefault) {
            config.entries.put(type, new Entry(allowed, defaults, ejectSupported, ejectDefault));
            return this;
        }

        public Builder add(TransferType type, List<IoMode> allowed, IoMode allFaces, boolean ejectSupported, boolean ejectDefault) {
            return add(type, allowed, side -> allFaces, ejectSupported, ejectDefault);
        }

        public SideConfig build() {
            return config;
        }
    }

    /** Button ids sent through {@code AbstractContainerMenu#clickMenuButton}. */
    public static final int BUTTON_BASE = 1000;

    public static int buttonId(TransferType type, int action) {
        return BUTTON_BASE + type.ordinal() * 16 + action;
    }

    /** Actions: 0..5 cycle a face forward, 6..11 backwards, 12 toggles auto-eject. */
    public static final int ACTION_EJECT = 12;

    public boolean handleButton(int id) {
        if (id < BUTTON_BASE) return false;
        int raw = id - BUTTON_BASE;
        int typeIndex = raw / 16;
        int action = raw % 16;
        if (typeIndex >= TransferType.values().length) return false;
        TransferType type = TransferType.values()[typeIndex];
        if (action < 6) return cycle(type, RelativeSide.values()[action], false);
        if (action < 12) return cycle(type, RelativeSide.values()[action - 6], true);
        if (action == ACTION_EJECT) return toggleEject(type);
        return false;
    }
}
