package net.radiation.tutorialmod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.radiation.tutorialmod.RadiationMod;
import net.radiation.tutorialmod.block.BearTrapBlock;
import net.radiation.tutorialmod.block.ModBlocks;

import javax.annotation.Nullable;
import java.util.List;

public class BearTrapItem extends Item {
    
    public BearTrapItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltips, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltips, flag);
        
        if (Screen.hasShiftDown()) {
            tooltips.add(Component.translatable("item.radiationmod.bear_trap.tooltip.info.1")
                    .withStyle(ChatFormatting.GRAY));
            tooltips.add(Component.translatable("item.radiationmod.bear_trap.tooltip.info.2")
                    .withStyle(ChatFormatting.GRAY));
            tooltips.add(Component.translatable("item.radiationmod.bear_trap.tooltip.info.3")
                    .withStyle(ChatFormatting.RED));
        } else {
            tooltips.add(Component.translatable("item.radiationmod.tooltip.shift")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
    
    /**
     * Размещение капкана при использовании предмета
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos blockpos = context.getClickedPos();
        BlockPos abovePos = blockpos.above();
        ItemStack itemstack = context.getItemInHand();
        Player player = context.getPlayer();
        
        // Проверяем, можно ли разместить капкан на этом блоке
        if (level.getBlockState(abovePos).isAir()) {
            BlockState trapState = ModBlocks.BEAR_TRAP.get().defaultBlockState();
            
            // Размещаем капкан
            level.setBlock(abovePos, trapState, 3);
            
            // Проигрываем звук размещения
            // SoundEvents.IRON_TRAPDOOR_CLOSE
            level.playSound(player, abovePos, net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE, 
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.8F);
            
            // Уменьшаем стак предметов, если игрок не в креативе
            if (player != null && !player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
            
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        
        return InteractionResult.PASS;
    }
} 