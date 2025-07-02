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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RadiationDetectorItem extends Item {
    
    // Map for storing first click position for each player
    private static final Map<UUID, BlockPos> firstPos = new HashMap<>();
    
    public RadiationDetectorItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide) {
            BlockPos playerPos = player.blockPosition();
            
            // Debug message
            player.sendSystemMessage(Component.literal("DEBUG: Detector used at position: " + playerPos));
            RadiationMod.LOGGER.info("Detector used at position: " + playerPos);
            
            // Check if player is in radiation zone
            RadiationZone zone = RadiationMod.RADIATION_MANAGER.getRadiationZoneAt(playerPos);
            player.sendSystemMessage(Component.literal("DEBUG: Found zone: " + (zone != null ? zone.toString() : "none")));
            RadiationMod.LOGGER.info("Found zone: " + (zone != null ? zone.toString() : "none"));
            
            // Check shift key
            player.sendSystemMessage(Component.literal("DEBUG: Shift pressed: " + player.isShiftKeyDown()));
            RadiationMod.LOGGER.info("Shift pressed: " + player.isShiftKeyDown());
            
            // If player pressed Shift and is in zone - change level
            if (player.isShiftKeyDown() && zone != null) {
                player.sendSystemMessage(Component.literal("DEBUG: Changing radiation level. Current: " + zone.getLevel()));
                RadiationMod.LOGGER.info("Changing radiation level. Current: " + zone.getLevel());
                
                // SUPER SIMPLE VERSION - DIRECT COMPONENT MESSAGES
                simpleCycleRadiationLevel(player, zone, playerPos);
                return InteractionResultHolder.success(stack);
            }
            
            // Standard behavior: create zone with 2 clicks
            handleTwoClickSystem(player, playerPos);
        }
        
        return InteractionResultHolder.success(stack);
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide || context.getPlayer() == null) {
            return InteractionResult.SUCCESS;
        }
        
        BlockPos clickedPos = context.getClickedPos();
        Player player = context.getPlayer();
        
        // Debug message
        player.sendSystemMessage(Component.literal("DEBUG: Detector used on block: " + clickedPos));
        RadiationMod.LOGGER.info("Detector used on block: " + clickedPos);
        
        // Check if block is in radiation zone
        RadiationZone zone = RadiationMod.RADIATION_MANAGER.getRadiationZoneAt(clickedPos);
        player.sendSystemMessage(Component.literal("DEBUG: Found zone: " + (zone != null ? zone.toString() : "none")));
        RadiationMod.LOGGER.info("Found zone: " + (zone != null ? zone.toString() : "none"));
        
        // Check shift key
        player.sendSystemMessage(Component.literal("DEBUG: Shift pressed: " + player.isShiftKeyDown()));
        RadiationMod.LOGGER.info("Shift pressed: " + player.isShiftKeyDown());
        
        // If shift pressed and block in zone, change level
        if (player.isShiftKeyDown() && zone != null) {
            player.sendSystemMessage(Component.literal("DEBUG: Changing radiation level. Current: " + zone.getLevel()));
            RadiationMod.LOGGER.info("Changing radiation level. Current: " + zone.getLevel());
            
            // SUPER SIMPLE VERSION - DIRECT COMPONENT MESSAGES
            simpleCycleRadiationLevel(player, zone, clickedPos);
            return InteractionResult.SUCCESS;
        }
        
        // Standard behavior - two-click system
        handleTwoClickSystem(player, clickedPos);
        
        return InteractionResult.SUCCESS;
    }
    
    /**
     * Super simple method without complications to change radiation level
     */
    private void simpleCycleRadiationLevel(Player player, RadiationZone zone, BlockPos pos) {
        try {
            // Get current level and calculate new one (1 to 5)
            int currentLevel = zone.getLevel();
            int newLevel = currentLevel >= 5 ? 1 : currentLevel + 1;
            
            RadiationMod.LOGGER.info("Changing radiation level from {} to {}", currentLevel, newLevel);
            
            // Используем новый метод для обновления уровня зоны
            RadiationZone newZone = RadiationMod.RADIATION_MANAGER.updateRadiationZoneLevel(zone, newLevel);
            
            // Send message
            String message = String.format("Radiation level at (%d, %d, %d) changed from %d to %d", 
                    pos.getX(), pos.getY(), pos.getZ(), currentLevel, newLevel);
            
            player.sendSystemMessage(Component.literal(message)
                    .withStyle(ChatFormatting.GREEN));
            
            // Send localized message
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
     * Process the two-click system for creating radiation zone
     */
    private void handleTwoClickSystem(Player player, BlockPos clickPos) {
        try {
            UUID playerId = player.getUUID();
            
            if (!firstPos.containsKey(playerId)) {
                // First click - remember position
                firstPos.put(playerId, clickPos);
                
                player.sendSystemMessage(Component.literal("DEBUG: First click at position: " + clickPos));
                RadiationMod.LOGGER.info("First click at position: " + clickPos);
                
                // Send message to player - first via system message for guarantee
                player.sendSystemMessage(Component.literal("First point set at: " + clickPos));
                
                // Then via standard mechanism
                Component message = Component.translatable("message.radiationmod.detector.set_position",
                    clickPos.getX(), clickPos.getY(), clickPos.getZ(), 1)
                    .withStyle(ChatFormatting.GREEN);
                    
                RadiationMod.LOGGER.info("Sending message: " + message.getString());
                player.displayClientMessage(message, false);
            } else {
                // Second click - create zone between positions with level 1
                BlockPos firstPosition = firstPos.get(playerId);
                
                player.sendSystemMessage(Component.literal("DEBUG: Second click at position: " + clickPos + ", creating zone with " + firstPosition));
                RadiationMod.LOGGER.info("Second click at position: " + clickPos + ", creating zone with " + firstPosition);
                
                RadiationMod.RADIATION_MANAGER.addRadiationZone(firstPosition, clickPos, 1);
                
                // Send message to player - first via system message for guarantee
                player.sendSystemMessage(Component.literal("Radiation zone created between " + firstPosition + " and " + clickPos));
                
                // Then via standard mechanism
                Component message = Component.translatable("message.radiationmod.detector.zone_created",
                    firstPosition.getX(), firstPosition.getY(), firstPosition.getZ(), 1)
                    .withStyle(ChatFormatting.GREEN);
                    
                RadiationMod.LOGGER.info("Sending message: " + message.getString());
                player.displayClientMessage(message, false);
                
                // Clear saved first position
                firstPos.remove(playerId);
            }
        } catch (Exception e) {
            RadiationMod.LOGGER.error("Error creating radiation zone", e);
            player.sendSystemMessage(Component.literal("ERROR: " + e.getMessage()));
        }
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.radiationmod.detector").withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.radiationmod.detector.usage").withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.radiationmod.detector.usage2").withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.literal("Shift + Right click to change radiation level").withStyle(net.minecraft.ChatFormatting.YELLOW));
    }
} 