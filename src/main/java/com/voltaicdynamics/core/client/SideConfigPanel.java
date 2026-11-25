package com.voltaicdynamics.core.client;

import net.minecraft.client.renderer.RenderPipelines;
import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.menu.MachineMenu;
import com.voltaicdynamics.core.sideconfig.IoMode;
import com.voltaicdynamics.core.sideconfig.RelativeSide;
import com.voltaicdynamics.core.sideconfig.SideConfig;
import com.voltaicdynamics.core.sideconfig.SideConfigurable;
import com.voltaicdynamics.core.sideconfig.TransferType;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mekanism-style side configuration, drawn as a tab on the right edge of every machine GUI. The open tab shows
 * one page per transfer type (items, fluids, energy) with the six faces laid out as an unfolded cube:
 * <pre>
 *        [Top]
 * [Left][Front][Right]
 *       [Bottom][Back]
 * </pre>
 * Left-click a face for the next mode, right-click for the previous one. Colours: red input, blue output,
 * purple both, grey off. Changes go to the server through menu button clicks.
 */
public class SideConfigPanel {
    private static final Identifier ICONS =
            Identifier.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, "textures/gui/side_config.png");

    private static final int TAB_SIZE = 22;
    private static final int WIDTH = 86;
    private static final int HEIGHT = 128;
    private static final int FACE = 16;
    private static final int GAP = 2;
    /** Grid cell (column, row) of each RelativeSide, in enum order FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM. */
    private static final int[][] CELLS = {{1, 1}, {2, 2}, {0, 1}, {2, 1}, {1, 0}, {1, 2}};

    private static boolean open;
    @Nullable
    private static TransferType selected;

    private final MachineMenu menu;
    private final SideConfigurable machine;
    private final List<TransferType> types;
    private int left;
    private int top;

    private SideConfigPanel(MachineMenu menu, SideConfigurable machine) {
        this.menu = menu;
        this.machine = machine;
        this.types = new ArrayList<>(machine.getSideConfig().types());
        if (selected == null || !types.contains(selected)) selected = types.get(0);
    }

    @Nullable
    public static SideConfigPanel create(MachineMenu menu) {
        SideConfigurable machine = menu.getConfigurable();
        if (machine == null || machine.getSideConfig().types().isEmpty()) return null;
        return new SideConfigPanel(menu, machine);
    }

    /** Anchors the panel to the right edge of the machine GUI. */
    public void setPosition(int guiRight, int guiTop) {
        this.left = guiRight;
        this.top = guiTop + 4;
    }

    /** Screen areas JEI and EMI must keep clear. */
    public List<Rect2i> getAreas() {
        return List.of(open ? new Rect2i(left, top, WIDTH, HEIGHT) : new Rect2i(left, top, TAB_SIZE + 2, TAB_SIZE));
    }

    // ---- drawing ----

    private static void bevel(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, 0xFF373737);
        g.fill(x, y, x + w - 1, y + h - 1, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    private static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, 0xFFFFFFFF);
        g.fill(x, y, x + w - 1, y + h - 1, 0xFF373737);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    public void render(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY) {
        g.nextStratum(); // above the slots and their items
        if (!open) {
            bevel(g, left - 1, top, TAB_SIZE + 2, TAB_SIZE, 0xFFC6C6C6);
            g.blit(RenderPipelines.GUI_TEXTURED, ICONS, left + 3, top + 3, 0, 0, 16, 16, 64, 32);
            return;
        }

        bevel(g, left - 1, top, WIDTH, HEIGHT, 0xFFC6C6C6);
        g.blit(RenderPipelines.GUI_TEXTURED, ICONS, left + 3, top + 3, 0, 0, 16, 16, 64, 32);
        g.text(font, Component.translatable("sideconfig.voltaicdynamics.title"), left + 22, top + 7, 0xFF404040, false);

        // Type tabs
        for (int i = 0; i < types.size(); i++) {
            TransferType type = types.get(i);
            int x = left + 4 + i * 20;
            int y = top + 22;
            if (type == selected) {
                inset(g, x, y, 18, 18, 0xFF8B8B8B);
            } else {
                bevel(g, x, y, 18, 18, 0xFFC6C6C6);
            }
            g.item(type.icon(), x + 1, y + 1);
        }

        TransferType type = selected;
        g.text(font, type.label(), left + 5, top + 44, 0xFF404040, false);

        int bits = menu.getSideBits(type);
        for (RelativeSide side : RelativeSide.values()) {
            int x = faceX(side);
            int y = faceY(side);
            IoMode mode = SideConfig.unpackMode(bits, side);
            boolean hover = inside(mouseX, mouseY, x, y, FACE, FACE);
            inset(g, x, y, FACE, FACE, mode.color());
            if (hover) g.fill(x + 1, y + 1, x + FACE - 1, y + FACE - 1, 0x40FFFFFF);
            String letter = side.label(!machine.hasFront()).getString().substring(0, 1);
            g.centeredText(font, letter, x + FACE / 2, y + 4, 0xFFFFFFFF);
        }

        SideConfig.Entry entry = machine.getSideConfig().entry(type);
        if (entry != null && entry.ejectSupported()) {
            boolean eject = SideConfig.unpackEject(bits);
            int x = left + 4;
            int y = top + HEIGHT - 20;
            bevel(g, x, y, WIDTH - 9, 15, eject ? 0xFF4C9A4C : 0xFF8B8B8B);
            g.blit(RenderPipelines.GUI_TEXTURED, ICONS, x + 2, y, 16, 0, 16, 16, 64, 32);
            g.text(font, Component.translatable(eject ? "sideconfig.voltaicdynamics.eject_on" : "sideconfig.voltaicdynamics.eject_off"),
                    x + 18, y + 4, 0xFFFFFFFF, true);
        }
    }

    private int gridLeft() {
        return left - 1 + (WIDTH - (3 * FACE + 2 * GAP)) / 2;
    }

    private int faceX(RelativeSide side) {
        return gridLeft() + CELLS[side.ordinal()][0] * (FACE + GAP);
    }

    private int faceY(RelativeSide side) {
        return top + 55 + CELLS[side.ordinal()][1] * (FACE + GAP);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Tooltip lines for whatever is under the mouse, or null. */
    @Nullable
    public List<Component> tooltip(int mouseX, int mouseY) {
        if (!open) {
            return inside(mouseX, mouseY, left, top, TAB_SIZE, TAB_SIZE)
                    ? List.of(Component.translatable("sideconfig.voltaicdynamics.title")) : null;
        }
        for (int i = 0; i < types.size(); i++) {
            if (inside(mouseX, mouseY, left + 4 + i * 20, top + 22, 18, 18)) return List.of(types.get(i).label());
        }
        int bits = menu.getSideBits(selected);
        for (RelativeSide side : RelativeSide.values()) {
            if (!inside(mouseX, mouseY, faceX(side), faceY(side), FACE, FACE)) continue;
            IoMode mode = SideConfig.unpackMode(bits, side);
            List<Component> lines = new ArrayList<>();
            lines.add(side.label(!machine.hasFront()).copy().append(": ").append(mode.label().copy().withColor(mode.color() & 0xFFFFFF | 0x404040)));
            BlockState neighbor = neighborState(side);
            if (neighbor != null && !neighbor.isAir()) {
                lines.add(neighbor.getBlock().getName().withStyle(ChatFormatting.GRAY));
            }
            lines.add(Component.translatable("sideconfig.voltaicdynamics.hint").withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }
        SideConfig.Entry entry = machine.getSideConfig().entry(selected);
        if (entry != null && entry.ejectSupported() && inside(mouseX, mouseY, left + 4, top + HEIGHT - 20, WIDTH - 9, 15)) {
            return List.of(Component.translatable("sideconfig.voltaicdynamics.eject_hint"));
        }
        return null;
    }

    @Nullable
    private BlockState neighborState(RelativeSide side) {
        var be = menu.getMachine();
        if (be.getLevel() == null) return null;
        Direction dir = side.toWorld(machine.getFacing());
        return be.getLevel().getBlockState(be.getBlockPos().relative(dir));
    }

    /** Returns true when the click was handled by the panel. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!open) {
            if (inside(mouseX, mouseY, left, top, TAB_SIZE, TAB_SIZE)) {
                open = true;
                click();
                return true;
            }
            return false;
        }
        if (!inside(mouseX, mouseY, left - 1, top, WIDTH, HEIGHT)) return false;
        if (inside(mouseX, mouseY, left, top, TAB_SIZE, 20)) {
            open = false;
            click();
            return true;
        }
        for (int i = 0; i < types.size(); i++) {
            if (inside(mouseX, mouseY, left + 4 + i * 20, top + 22, 18, 18)) {
                selected = types.get(i);
                click();
                return true;
            }
        }
        for (RelativeSide side : RelativeSide.values()) {
            if (inside(mouseX, mouseY, faceX(side), faceY(side), FACE, FACE)) {
                int action = button == 1 ? 6 + side.ordinal() : side.ordinal();
                send(SideConfig.buttonId(selected, action));
                return true;
            }
        }
        SideConfig.Entry entry = machine.getSideConfig().entry(selected);
        if (entry != null && entry.ejectSupported() && inside(mouseX, mouseY, left + 4, top + HEIGHT - 20, WIDTH - 9, 15)) {
            send(SideConfig.buttonId(selected, SideConfig.ACTION_EJECT));
            return true;
        }
        return true; // clicks on the panel background never reach the slots behind
    }

    private void send(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(menu.containerId, id);
        click();
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
