package net.radiation.tutorialmod.block.entity;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;

import javax.annotation.Nullable;

/**
 * Вспомогательный класс для создания тикеров для BlockEntity
 */
public class TrapBlockEntityTicker {
    
    /**
     * Создает тикер для BlockEntity
     */
    @Nullable
    public static <T extends BlockEntity, E extends BlockEntity> BlockEntityTicker<T> createTicker(
            BlockEntityType<T> pType, BlockEntityType<E> pTargetType, BlockEntityTicker<? super E> pTicker) {
        return pTargetType == pType ? (BlockEntityTicker<T>) pTicker : null;
    }
    
    /**
     * Создает простой тикер для BearTrapBlockEntity
     */
    public static BlockEntityTicker<BearTrapBlockEntity> createBearTrapTicker(Level pLevel) {
        return pLevel.isClientSide ? null : BearTrapBlockEntity::tick;
    }
} 