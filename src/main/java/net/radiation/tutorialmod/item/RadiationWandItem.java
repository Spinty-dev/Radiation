package net.radiation.tutorialmod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.radiation.tutorialmod.RadiationMod;
import net.radiation.tutorialmod.radiation.RadiationManager.RadiationZone;

import javax.annotation.Nullable;
import java.util.List;

public class RadiationWandItem extends Item {
    
    public RadiationWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        
        if (player == null || level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        
        BlockPos pos = context.getClickedPos();
        
        // Если Shift+ПКМ по блоку, проверяем наличие зоны радиации
        if (player.isShiftKeyDown()) {
            // Проверяем, есть ли зона радиации в этой точке
            RadiationZone zone = RadiationMod.RADIATION_MANAGER.getRadiationZoneAt(pos);
            if (zone != null) {
                // Если есть зона, меняем уровень радиации
                RadiationMod.LOGGER.info("Player {} is changing radiation level at {}", 
                    player.getName().getString(), pos.toShortString());
                
                changeRadiationLevel(player, zone, pos);
                return InteractionResult.SUCCESS;
            }
            
            // Если зоны нет, но нажат Shift, устанавливаем позицию 2
            RadiationMod.RADIATION_MANAGER.setPos2(player.getUUID(), pos);
            RadiationMod.LOGGER.info("Player {} set position 2: {}", 
                    player.getName().getString(), pos.toShortString());
            
            player.displayClientMessage(
                    Component.literal("Position 2 set: ")
                            .append(Component.literal(pos.toShortString()).withStyle(ChatFormatting.GREEN)), 
                    true);
            
            // Проверяем, можно ли создать зону
            if (RadiationMod.RADIATION_MANAGER.canCreateZone(player.getUUID())) {
                RadiationMod.LOGGER.info("Radiation zone ready to create for player {}", 
                        player.getName().getString());
                
                player.displayClientMessage(
                        Component.literal("Radiation zone ready to create! Use ")
                                .append(Component.literal("right-click in air").withStyle(ChatFormatting.YELLOW))
                                .append(Component.literal(" to create the zone.")), 
                        false);
            }
        } else {
            // Обычный ПКМ по блоку - устанавливаем позицию 1
            RadiationMod.RADIATION_MANAGER.setPos1(player.getUUID(), pos);
            RadiationMod.LOGGER.info("Player {} set position 1: {}", 
                    player.getName().getString(), pos.toShortString());
            
            player.displayClientMessage(
                    Component.literal("Position 1 set: ")
                            .append(Component.literal(pos.toShortString()).withStyle(ChatFormatting.GREEN)), 
                    true);
            
            player.displayClientMessage(
                    Component.literal("Select second position by ")
                            .append(Component.literal("crouching (Shift)").withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(" and right-clicking a block.")), 
                    false);
        }
        
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(itemStack, true);
        }
        
        // Если Shift+ПКМ в воздухе, проверяем наличие зоны радиации на позиции игрока
        if (player.isShiftKeyDown()) {
            BlockPos playerPos = player.blockPosition();
            RadiationZone zone = RadiationMod.RADIATION_MANAGER.getRadiationZoneAt(playerPos);
            if (zone != null) {
                // Если игрок в зоне радиации, меняем уровень
                RadiationMod.LOGGER.info("Player {} is changing radiation level at their position", 
                    player.getName().getString());
                
                changeRadiationLevel(player, zone, playerPos);
                return InteractionResultHolder.success(itemStack);
            } else {
                // Если зоны нет, но нажат Shift+ПКМ в воздухе, то меняем дефолтный уровень
                int newLevel = cycleDefaultRadiationLevel();
                RadiationMod.LOGGER.info("Player {} changed default radiation level to {}", 
                    player.getName().getString(), newLevel);
                
                player.sendSystemMessage(Component.literal("Default radiation level changed to " + newLevel)
                    .withStyle(ChatFormatting.GREEN));
                
                return InteractionResultHolder.success(itemStack);
            }
        }
        
        // Обычный ПКМ в воздухе - создаем зону, если возможно
        if (!player.isShiftKeyDown()) {
            // Создаем зону радиации, если обе позиции установлены
            if (RadiationMod.RADIATION_MANAGER.canCreateZone(player.getUUID())) {
                RadiationMod.LOGGER.info("Player {} is creating a radiation zone...", 
                        player.getName().getString());
                
                // Используем текущий дефолтный уровень радиации
                int defaultLevel = RadiationMod.RADIATION_MANAGER.getDefaultRadiationLevel();
                RadiationMod.RADIATION_MANAGER.createRadiationZone(player.getUUID(), defaultLevel);
                
                RadiationMod.LOGGER.info("Radiation zone successfully created for player {} with level {}", 
                        player.getName().getString(), defaultLevel);
                
                player.displayClientMessage(
                        Component.literal("Radiation zone created! Level: ")
                                .append(Component.literal(String.valueOf(defaultLevel)).withStyle(ChatFormatting.RED)), 
                        false);
            } else {
                RadiationMod.LOGGER.info("Player {} tries to create a zone, but both positions are not set", 
                        player.getName().getString());
                
                player.displayClientMessage(
                        Component.literal("Set both positions first!").withStyle(ChatFormatting.RED), 
                        false);
            }
        }
        
        return InteractionResultHolder.success(itemStack);
    }
    
    /**
     * Changes radiation level in a zone (cycles from 1 to 5)
     */
    private void changeRadiationLevel(Player player, RadiationZone zone, BlockPos pos) {
        try {
            // Get current level and calculate new one (1 to 5)
            int currentLevel = zone.getLevel();
            int newLevel = currentLevel >= 5 ? 1 : currentLevel + 1;
            
            RadiationMod.LOGGER.info("Changing radiation level: {} -> {}", currentLevel, newLevel);
            
            // Используем новый метод для обновления уровня зоны
            RadiationZone newZone = RadiationMod.RADIATION_MANAGER.updateRadiationZoneLevel(zone, newLevel);
            
            // Принудительно обновляем зоны для игроков в этой зоне (чтобы сбросить кеш)
            RadiationMod.RADIATION_MANAGER.saveAllZones();
            
            // Send direct message to player
            String message = "Radiation level changed from " + currentLevel + " to " + newLevel;
            player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.YELLOW));
            
            // Also send localized message
            Component localizedMsg = Component.translatable("message.radiationmod.detector.zone_updated", 
                pos.getX(), pos.getY(), pos.getZ(), newLevel)
                .withStyle(ChatFormatting.GREEN);
            
            player.displayClientMessage(localizedMsg, false);
            
        } catch (Exception e) {
            RadiationMod.LOGGER.error("Error changing radiation level", e);
            player.sendSystemMessage(Component.literal("ERROR: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
        }
    }
    
    /**
     * Cycles the default radiation level from 1 to 5
     * @return The new default level
     */
    private int cycleDefaultRadiationLevel() {
        int currentLevel = RadiationMod.RADIATION_MANAGER.getDefaultRadiationLevel();
        int newLevel = currentLevel >= 5 ? 1 : currentLevel + 1;
        
        RadiationMod.LOGGER.info("Changing default radiation level: {} -> {}", currentLevel, newLevel);
        RadiationMod.RADIATION_MANAGER.setDefaultRadiationLevel(newLevel);
        
        return newLevel;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.radiationmod.radiation_wand.tooltip")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("- Right-click block: ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("set position 1").withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("- Shift + right-click block: ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("set position 2 or change radiation level").withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("- Right-click air: ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("create radiation zone").withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("- Shift + right-click in zone: ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("change radiation level").withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("- Shift + right-click in air: ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("change default radiation level").withStyle(ChatFormatting.WHITE)));
        
        super.appendHoverText(stack, level, tooltip, flag);
    }
} 