package net.radiation.tutorialmod.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.radiation.tutorialmod.RadiationMod;
import net.radiation.tutorialmod.block.BearTrapBlock;
import net.radiation.tutorialmod.block.ModBlocks;
import net.radiation.tutorialmod.block.entity.BearTrapBlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = RadiationMod.MOD_ID)
public class BearTrapEvents {
    // Карта для отслеживания блокпозов, в которых находятся пойманные игроки
    private static final Map<UUID, BlockPos> caughtPlayersMap = new HashMap<>();
    
    /**
     * Отменяем перемещение игрока, если он пойман в капкан
     */
    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (isPlayerCaught(player)) {
                // Отменяем прыжок
                player.setDeltaMovement(0, 0, 0);
            }
        }
    }
    
    /**
     * Отслеживаем движение игрока
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        // Проверяем, пойман ли игрок в капкан
        if (isPlayerCaught(player)) {
            // Отменяем движение
            player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
            
            // Получаем позицию капкана
            BlockPos trapPos = caughtPlayersMap.get(player.getUUID());
            if (trapPos != null) {
                // Центрируем игрока над капканом
                player.setPos(
                        trapPos.getX() + 0.5, 
                        player.getY(), 
                        trapPos.getZ() + 0.5
                );
            }
        } else {
            // Если игрок не пойман, но есть в карте, удаляем его
            if (caughtPlayersMap.containsKey(player.getUUID())) {
                caughtPlayersMap.remove(player.getUUID());
            }
        }
    }
    
    /**
     * Обрабатываем отпускание правой кнопки мыши
     */
    @SubscribeEvent
    public static void onRightClickRelease(PlayerInteractEvent.RightClickEmpty event) {
        // Сообщаем блоку капкана, что игрок перестал удерживать правую кнопку
        BearTrapBlock.onPlayerStoppedRightClick(event.getEntity());
    }
    
    /**
     * Проверяет, пойман ли игрок в капкан
     */
    private static boolean isPlayerCaught(Player player) {
        if (player.level().isClientSide) {
            return false;
        }
        
        Level level = player.level();
        
        // Проверяем, есть ли игрок в карте пойманных
        if (caughtPlayersMap.containsKey(player.getUUID())) {
            BlockPos trapPos = caughtPlayersMap.get(player.getUUID());
            
            // Проверяем, что блок по-прежнему является капканом
            if (level.getBlockState(trapPos).getBlock() == ModBlocks.BEAR_TRAP.get() && 
                level.getBlockState(trapPos).getValue(BearTrapBlock.TRIGGERED)) {
                
                // Проверяем, действительно ли игрок пойман в этом капкане
                if (level.getBlockEntity(trapPos) instanceof BearTrapBlockEntity trapEntity && 
                    trapEntity.isCaughtPlayer(player.getUUID())) {
                    // Игрок пойман, возвращаем true
                    return true;
                }
            }
            
            // Если дошли до сюда, значит, капкан был удален или изменен
            caughtPlayersMap.remove(player.getUUID());
            return false;
        }
        
        // Ищем капканы вокруг игрока
        int range = 2;
        BlockPos playerPos = player.blockPosition();
        
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    
                    if (state.getBlock() == ModBlocks.BEAR_TRAP.get() && 
                        state.getValue(BearTrapBlock.TRIGGERED)) {
                        
                        // Проверяем, пойман ли этот игрок в данном капкане
                        if (level.getBlockEntity(pos) instanceof BearTrapBlockEntity trapEntity && 
                            trapEntity.isCaughtPlayer(player.getUUID())) {
                            // Добавляем игрока в карту пойманных
                            caughtPlayersMap.put(player.getUUID(), pos);
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
} 