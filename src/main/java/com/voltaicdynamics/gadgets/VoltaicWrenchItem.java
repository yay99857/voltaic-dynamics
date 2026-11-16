package com.voltaicdynamics.gadgets;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.block.WrenchConfigurable;
import com.voltaicdynamics.core.sideconfig.SideConfigurable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Voltaic Wrench. On Voltaic blocks:
 * <ul>
 *   <li>item and fluid cables ({@link WrenchConfigurable}): right-click, sneaking or not, an arm to cycle it:
 *       deliver -> pull -> off;</li>
 *   <li>machines: right-click turns them a quarter turn (the side configuration turns with them);</li>
 *   <li>any Voltaic block except item and fluid cables: sneak + right-click picks it up, keeping what the block
 *       keeps when broken (energy in Accumulators, fluid in the Fluid Tank).</li>
 * </ul>
 * It is in the {@code c:tools/wrench} tag, so other mods treat it as a wrench, and other mods' wrenches
 * configure our cables.
 */
public class VoltaicWrenchItem extends Item {
    public VoltaicWrenchItem(Properties properties) {
        super(properties);
    }

    /** Lets a sneak + right-click reach the cable's own handler instead of being swallowed by the item. */
    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return true;
    }

    /** Runs before the block opens its GUI, so a plain right-click on a machine turns it instead. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        if (player == null || !isVoltaic(state) || state.getBlock() instanceof WrenchConfigurable) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) dismantle((ServerLevel) level, pos, state, player, stack);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            if (!level.isClientSide) rotate(level, pos, state);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private static boolean isVoltaic(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals(VoltaicDynamics.MOD_ID);
    }

    private static void rotate(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        level.setBlock(pos, state.setValue(HorizontalDirectionalBlock.FACING, facing.getClockWise()), Block.UPDATE_ALL);
        // Faces are configured relative to the front, so the capabilities on each world side changed.
        if (level.getBlockEntity(pos) instanceof SideConfigurable configurable) {
            configurable.onSideConfigChanged();
        }
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    /** Drops go straight to the player (inventory contents still spill, as when the block is broken). */
    private static void dismantle(ServerLevel level, BlockPos pos, BlockState state, Player player, ItemStack wrench) {
        BlockEntity be = level.getBlockEntity(pos);
        List<ItemStack> drops = Block.getDrops(state, level, pos, be, player, wrench);
        level.removeBlock(pos, false);
        for (ItemStack drop : drops) {
            if (!player.getInventory().add(drop)) player.drop(drop, false);
        }
        level.playSound(null, pos, state.getSoundType(level, pos, player).getBreakSound(), SoundSource.BLOCKS, 1.0F, 0.9F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.voltaic_wrench.rotate").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.voltaic_wrench.cable").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.voltaic_wrench.dismantle").withStyle(ChatFormatting.GRAY));
    }
}
