package com.voltaicdynamics.machines;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Raises an Electric Furnace, Alloy Smelter or Pulverizer one tier in place (Basic → Advanced → Elite → Ultimate),
 * keeping its items, energy, upgrades and side configuration. Used up when it works.
 */
public class TierInstallerItem extends Item {
    private final MachineTier tier;

    public TierInstallerItem(MachineTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public MachineTier tier() {
        return tier;
    }

    /** Runs before the machine opens its GUI. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof ProcessingMachineBlockEntity machine)) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        MachineTier current = machine.getTier();
        if (current.next() != tier) {
            if (player != null && !level.isClientSide) {
                Component reason = current.ordinal() >= tier.ordinal()
                        ? Component.translatable("message.voltaicdynamics.tier_installer.already", tierName(current))
                        : Component.translatable("message.voltaicdynamics.tier_installer.needs", tierName(tier.previous()));
                player.displayClientMessage(reason.copy().withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            machine.setTier(tier);
            if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, context.getClickedPos(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1F, 1.2F);
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.voltaicdynamics.tier_installer.done",
                        tierName(tier), tier.parallel()).withStyle(tier.color()), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static Component tierName(MachineTier tier) {
        return Component.translatable("tier.voltaicdynamics." + tier.getSerializedName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.tier_installer", tier.parallel()).withStyle(tier.color()));
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.tier_installer.how", tierName(tier.previous()))
                .withStyle(ChatFormatting.GRAY));
    }
}
