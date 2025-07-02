package net.radiation.tutorialmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.FakePlayer;
import net.radiation.tutorialmod.RadiationMod;
import net.radiation.tutorialmod.block.entity.BearTrapBlockEntity;
import net.radiation.tutorialmod.block.entity.TrapBlockEntityTicker;
import net.radiation.tutorialmod.block.entity.ModBlockEntities;
import net.radiation.tutorialmod.item.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BearTrapBlock extends Block implements EntityBlock {
    // Свойство состояния - открыт или закрыт капкан
    public static final BooleanProperty TRIGGERED = BooleanProperty.create("triggered");
    
    // Форма блока для взаимодействия с ним
    protected static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 3.0D, 15.0D);
    
    // Слежение за игроками, которые в процессе открытия капкана
    private static final Map<UUID, Long> playerRightClickMap = new HashMap<>();
    
    // Время в миллисекундах, которое нужно удерживать нажатие для открытия капкана
    private static final long OPEN_TIME_REQUIRED = 5000; // 5 секунд
    
    public BearTrapBlock(Properties properties) {
        super(properties);
        // Устанавливаем состояние по умолчанию - закрытый капкан (не triggered)
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(TRIGGERED, Boolean.FALSE));
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TRIGGERED);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(TRIGGERED, Boolean.FALSE); // По умолчанию капкан закрыт
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BearTrapBlockEntity(pos, state);
    }
    
    /**
     * Получает тикер для BlockEntity
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return TrapBlockEntityTicker.createTicker(blockEntityType, ModBlockEntities.BEAR_TRAP.get(), 
                (lvl, pos, st, be) -> BearTrapBlockEntity.tick(lvl, pos, st, be));
    }
    
    /**
     * Обрабатывает столкновение сущности с блоком
     */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity && !(entity instanceof FakePlayer)) {
            // Проверяем, что капкан не активирован (закрыт)
            if (!state.getValue(TRIGGERED)) {
                // Проверяем, имеет ли игрок иммунитет к этому капкану
                if (entity instanceof Player player) {
                    BearTrapBlockEntity blockEntity = level.getBlockEntity(pos) instanceof BearTrapBlockEntity ? 
                            (BearTrapBlockEntity) level.getBlockEntity(pos) : null;
                    
                    if (blockEntity != null) {
                        boolean isImmune = blockEntity.isImmunePlayer(player.getUUID());
                        
                        RadiationMod.LOGGER.info("Проверка иммунитета для " + player.getName().getString() + 
                                " на координатах " + pos.getX() + "," + pos.getY() + "," + pos.getZ() + 
                                ". Игрок иммунен: " + isImmune);
                        
                        if (isImmune) {
                            // Игрок имеет иммунитет - ничего не делаем
                            RadiationMod.LOGGER.info("Игрок " + player.getName().getString() + " имеет иммунитет к капкану");
                            return;
                        }
                    } else {
                        RadiationMod.LOGGER.warn("Не удалось получить BlockEntity для капкана на позиции " + 
                                pos.getX() + "," + pos.getY() + "," + pos.getZ());
                    }
                    
                    // Логируем для отладки
                    RadiationMod.LOGGER.info("Игрок " + player.getName().getString() + " не имеет иммунитета к капкану");
                }
                
                // Игрок попал в капкан - активируем его
                level.setBlock(pos, state.setValue(TRIGGERED, Boolean.TRUE), 3);
                level.gameEvent(entity, GameEvent.BLOCK_ACTIVATE, pos);
                
                // Наносим 5 урона 
                entity.hurt(level.damageSources().generic(), 5.0F);
                
                // Проигрываем звук активации капкана
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.5F);
                
                // Если это игрок, сохраняем его в BlockEntity, чтобы не позволить двигаться
                if (entity instanceof Player player && level.getBlockEntity(pos) instanceof BearTrapBlockEntity trapEntity) {
                    // Устанавливаем точную позицию Y для игрока в капкане (на 0.1 выше блока)
                    double exactY = pos.getY() + 0.1;
                    player.teleportTo(player.getX(), exactY, player.getZ());
                    
                    trapEntity.setCaughtPlayer(player.getUUID());
                    
                    // Накладываем эффект медлительности 255 уровня на бесконечное время (до освобождения)
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Integer.MAX_VALUE, 255, false, false, true));
                    
                    player.displayClientMessage(
                            Component.translatable("message.radiationmod.bear_trap.caught").withStyle(net.minecraft.ChatFormatting.RED), 
                            true
                    );
                }
            }
        }
    }
    
    /**
     * Обрабатывает правый клик по блоку
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, 
                                InteractionHand hand, BlockHitResult hit) {
        // Проверяем, что капкан активирован (закрыт с пойманным игроком)
        if (state.getValue(TRIGGERED)) {
            // Если это клиентская сторона, просто возвращаем успешный результат
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            
            // Получаем текущее время
            long currentTime = System.currentTimeMillis();
            
            // Проверяем, начал ли игрок открывать капкан
            if (!playerRightClickMap.containsKey(player.getUUID())) {
                // Игрок только начал открывать капкан
                playerRightClickMap.put(player.getUUID(), currentTime);
                
                player.displayClientMessage(
                        Component.translatable("message.radiationmod.bear_trap.opening").withStyle(net.minecraft.ChatFormatting.GOLD), 
                        true
                );
                return InteractionResult.SUCCESS;
            } else {
                // Игрок уже начал открывать капкан, проверяем, прошло ли достаточно времени
                long startTime = playerRightClickMap.get(player.getUUID());
                long elapsedTime = currentTime - startTime;
                
                if (elapsedTime >= OPEN_TIME_REQUIRED) {
                    // Игрок удерживал кнопку достаточно долго, открываем капкан
                    level.setBlock(pos, state.setValue(TRIGGERED, Boolean.FALSE), 3);
                    level.gameEvent(player, GameEvent.BLOCK_DEACTIVATE, pos);
                    level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.8F);
                    
                    // Освобождаем пойманного игрока и снимаем эффект медлительности
                    if (level.getBlockEntity(pos) instanceof BearTrapBlockEntity trapEntity) {
                        UUID caughtPlayerUUID = trapEntity.getCaughtPlayerUUID();
                        if (caughtPlayerUUID != null) {
                            Entity entity = ((ServerLevel)level).getEntity(caughtPlayerUUID);
                            if (entity instanceof Player caughtPlayer) {
                                caughtPlayer.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                                
                                // Больше не устанавливаем иммунитет здесь, это будет делаться в тикере блока
                                // пока игрок находится в блоке капкана после его открытия
                            }
                        }
                        trapEntity.clearCaughtPlayer();
                    }
                    
                    // Удаляем игрока из карты открывающих
                    playerRightClickMap.remove(player.getUUID());
                    
                    // Выдаем сообщение об успешном открытии
                    player.displayClientMessage(
                            Component.translatable("message.radiationmod.bear_trap.opened").withStyle(net.minecraft.ChatFormatting.GREEN), 
                            true
                    );
                    
                    // С небольшим шансом капкан может сломаться при открытии
                    if (level.random.nextFloat() < 0.2f) {
                        level.destroyBlock(pos, true);
                        player.spawnAtLocation(ModItems.BEAR_TRAP.get().getDefaultInstance());
                    }
                    
                    return InteractionResult.SUCCESS;
                } else {
                    // Сообщаем игроку, сколько времени осталось
                    int secondsLeft = (int)((OPEN_TIME_REQUIRED - elapsedTime) / 1000) + 1;
                    player.displayClientMessage(
                            Component.translatable("message.radiationmod.bear_trap.opening.time_left", secondsLeft)
                                    .withStyle(net.minecraft.ChatFormatting.GOLD), 
                            true
                    );
                    return InteractionResult.SUCCESS;
                }
            }
        }
        
        return InteractionResult.PASS;
    }
    
    /**
     * Удаляем игрока из карты открывающих, если он перестал удерживать правую кнопку мыши
     */
    public static void onPlayerStoppedRightClick(Player player) {
        playerRightClickMap.remove(player.getUUID());
    }
    
    /**
     * Проверяет, есть ли игрок в капкане
     */
    public static boolean isPlayerCaught(UUID playerUUID, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof BearTrapBlockEntity trapEntity) {
            return trapEntity.isCaughtPlayer(playerUUID);
        }
        return false;
    }
    
    /**
     * При уничтожении блока освобождаем пойманного игрока и снимаем эффект медлительности
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof BearTrapBlockEntity trapEntity) {
                UUID caughtPlayerUUID = trapEntity.getCaughtPlayerUUID();
                if (caughtPlayerUUID != null && level instanceof ServerLevel) {
                    Entity entity = ((ServerLevel)level).getEntity(caughtPlayerUUID);
                    if (entity instanceof Player caughtPlayer) {
                        caughtPlayer.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    }
                }
                trapEntity.clearCaughtPlayer();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
} 