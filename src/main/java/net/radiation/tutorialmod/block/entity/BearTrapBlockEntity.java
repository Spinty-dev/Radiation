package net.radiation.tutorialmod.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.radiation.tutorialmod.RadiationMod;
import net.radiation.tutorialmod.block.BearTrapBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;

public class BearTrapBlockEntity extends BlockEntity {
    private UUID caughtPlayerUUID;
    private long caughtTime;  // Время в миллисекундах, когда игрок был пойман
    private double caughtPlayerX;  // Позиция X игрока в момент попадания в капкан
    private double caughtPlayerY;  // Позиция Y игрока в момент попадания в капкан
    private double caughtPlayerZ;  // Позиция Z игрока в момент попадания в капкан
    
    // Максимальное время удержания (20 минут в миллисекундах)
    private static final long MAX_CAUGHT_TIME = 20 * 60 * 1000;
    
    // Игроки, имеющие иммунитет к этому капкану с временем истечения иммунитета
    private Map<UUID, Long> immunePlayers = new HashMap<>();
    
    // Задержка снятия иммунитета после выхода из зоны капкана (в миллисекундах)
    private static final long IMMUNITY_DELAY_AFTER_EXIT = 2000; // 2 секунды
    
    private boolean isBeingOpened = false;
    private float openingProgress = 0.0f;
    
    public BearTrapBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BEAR_TRAP.get(), pos, state);
    }
    
    /**
     * Устанавливает пойманного игрока
     */
    public void setCaughtPlayer(UUID uuid) {
        // Проверка на иммунитет
        if (isImmunePlayer(uuid)) {
            return;
        }
        
        this.caughtPlayerUUID = uuid;
        this.caughtTime = System.currentTimeMillis();
        
        // Сохраняем начальную позицию игрока
        if (this.level instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(uuid);
            if (entity instanceof Player player) {
                this.caughtPlayerX = player.getX();
                this.caughtPlayerY = player.getY();
                this.caughtPlayerZ = player.getZ();
                RadiationMod.LOGGER.info("Сохранена начальная позиция пойманного игрока: " + this.caughtPlayerX + ", " + this.caughtPlayerY + ", " + this.caughtPlayerZ);
            }
        }
        
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }
    
    /**
     * Очищает пойманного игрока и добавляет ему иммунитет
     */
    public void clearCaughtPlayer() {
        if (this.caughtPlayerUUID != null) {
            // Устанавливаем иммунитет здесь напрямую
            setPlayerImmunity(this.caughtPlayerUUID);
            RadiationMod.LOGGER.info("Установлен иммунитет при освобождении игрока из капкана");
        }
        
        this.caughtPlayerUUID = null;
        this.caughtTime = 0;
        this.caughtPlayerX = 0;
        this.caughtPlayerY = 0;
        this.caughtPlayerZ = 0;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }
    
    /**
     * Проверяет, пойман ли конкретный игрок
     */
    public boolean isCaughtPlayer(UUID uuid) {
        return uuid != null && uuid.equals(this.caughtPlayerUUID);
    }
    
    /**
     * Проверяет, имеет ли игрок иммунитет
     */
    public boolean isImmunePlayer(UUID uuid) {
        return uuid != null && immunePlayers.containsKey(uuid);
    }
    
    /**
     * Удаляет иммунитет у игрока
     */
    public void removeImmunity(UUID uuid) {
        if (uuid != null && immunePlayers.containsKey(uuid)) {
            immunePlayers.remove(uuid);
            RadiationMod.LOGGER.info("Принудительно снят иммунитет для UUID: " + uuid);
            this.setChanged();
        }
    }
    
    /**
     * Получает UUID пойманного игрока
     */
    public UUID getCaughtPlayerUUID() {
        return this.caughtPlayerUUID;
    }
    
    /**
     * Запускает анимацию открытия капкана
     */
    public void startOpeningAnimation() {
        this.isBeingOpened = true;
        this.openingProgress = 0.0f;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }
    
    /**
     * Обновляет прогресс открытия капкана
     */
    public void updateOpeningProgress(float progress) {
        this.openingProgress = Math.min(1.0f, Math.max(0.0f, progress));
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }
    
    /**
     * Останавливает анимацию открытия капкана
     */
    public void stopOpeningAnimation() {
        this.isBeingOpened = false;
        this.openingProgress = 0.0f;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }
    
    /**
     * Получает прогресс открытия капкана (для клиентской анимации)
     */
    public float getOpeningProgress() {
        return this.openingProgress;
    }
    
    /**
     * Проверяет, находится ли капкан в процессе открытия
     */
    public boolean isBeingOpened() {
        return this.isBeingOpened;
    }
    
    /**
     * Проверяет, находится ли игрок все еще внутри блока капкана
     */
    private boolean isPlayerInTrapBounds(Player player, BlockPos pos) {
        // Получаем координаты игрока и блока
        double playerX = player.getX();
        double playerZ = player.getZ();
        
        // Координаты блока
        int blockX = pos.getX();
        int blockZ = pos.getZ();
        
        // Проверяем, находится ли игрок в пределах блока с небольшим запасом 
        // (0.1 блока с каждой стороны для надежности)
        boolean result = playerX >= (blockX - 0.1) && playerX <= (blockX + 1.1) && 
                         playerZ >= (blockZ - 0.1) && playerZ <= (blockZ + 1.1);
        
        // Дополнительное логирование для отладки
        RadiationMod.LOGGER.info("Проверка isPlayerInTrapBounds для " + player.getName().getString() +
                            ": игрок[" + playerX + "," + player.getY() + "," + playerZ + "] " +
                            "блок[" + blockX + "," + pos.getY() + "," + blockZ + "] результат: " + result);
        
        return result;
    }
    
    /**
     * Устанавливает иммунитет игроку (бессрочный, пока в зоне, и еще 2 секунды после выхода)
     */
    public void setPlayerImmunity(UUID uuid) {
        if (uuid != null) {
            // Проверяем, есть ли уже иммунитет у игрока
            boolean alreadyImmune = immunePlayers.containsKey(uuid);
            
            immunePlayers.put(uuid, Long.MAX_VALUE); // Бессрочный иммунитет, пока игрок в зоне
            
            if (!alreadyImmune) {
                RadiationMod.LOGGER.info("Установлен новый иммунитет для UUID: " + uuid + " (активен пока игрок в зоне блока)");
            } else {
                RadiationMod.LOGGER.info("Обновлен существующий иммунитет для UUID: " + uuid);
            }
            
            this.setChanged();
        }
    }
    
    /**
     * Устанавливает временный иммунитет игроку на указанное время
     */
    public void setTemporaryImmunity(UUID uuid, long durationMs) {
        if (uuid != null) {
            long expirationTime = System.currentTimeMillis() + durationMs;
            immunePlayers.put(uuid, expirationTime);
            RadiationMod.LOGGER.info("Установлен временный иммунитет для UUID: " + uuid + " на " + (durationMs / 1000) + " секунд");
            this.setChanged();
        }
    }
    
    /**
     * Тик для BlockEntity
     */
    public static void tick(Level level, BlockPos pos, BlockState state, BearTrapBlockEntity blockEntity) {
        // Проверяем наличие всех нужных объектов
        if (level == null || blockEntity == null || state == null || pos == null) {
            return;
        }

        // Предотвращаем выполнение на клиенте
        if (level.isClientSide()) {
            return;
        }
        
        try {
            // Проверяем, не прошло ли максимальное время удержания
            if (blockEntity.caughtPlayerUUID != null && blockEntity.caughtTime > 0) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - blockEntity.caughtTime > MAX_CAUGHT_TIME) {
                    // Прошло больше 20 минут, автоматически освобождаем игрока
                    if (state.hasProperty(BearTrapBlock.TRIGGERED) && state.getValue(BearTrapBlock.TRIGGERED)) {
                        level.setBlock(pos, state.setValue(BearTrapBlock.TRIGGERED, Boolean.FALSE), 3);
                    }
                    
                    blockEntity.clearCaughtPlayer();
                    RadiationMod.LOGGER.info("Игрок был автоматически освобожден из капкана из-за истечения времени");
                }
            }
            
            // Проверяем, что капкан НЕ активирован (открыт) - обрабатываем иммунитет
            if (state.hasProperty(BearTrapBlock.TRIGGERED) && !state.getValue(BearTrapBlock.TRIGGERED)) {
                if (!(level instanceof ServerLevel serverLevel)) {
                    return;
                }
                
                // Дополнительно логируем состояние капкана каждые 100 тиков
                if (level.getGameTime() % 100 == 0) {
                    RadiationMod.LOGGER.info("Капкан на " + pos.getX() + "," + pos.getY() + "," + pos.getZ() + 
                                          " открыт. Количество иммунных игроков: " + blockEntity.immunePlayers.size());
                }
                
                // Проверяем всех игроков в зоне действия капкана
                for (Player player : serverLevel.players()) {
                    UUID playerUUID = player.getUUID();
                    
                    // Если игрок находится внутри блока капкана
                    if (blockEntity.isPlayerInTrapBounds(player, pos)) {
                        RadiationMod.LOGGER.info("Игрок " + player.getName().getString() + 
                                 " находится на капкане " + pos.getX() + "," + pos.getY() + "," + pos.getZ());
                        
                        // Устанавливаем ему иммунитет
                        if (!blockEntity.isImmunePlayer(playerUUID)) {
                            blockEntity.setPlayerImmunity(playerUUID);
                            RadiationMod.LOGGER.info("Игрок " + player.getName().getString() + 
                                " получил иммунитет, находясь в зоне капкана на позиции " +
                                player.getX() + ", " + player.getY() + ", " + player.getZ());
                        }
                    }
                    // Если игрок вышел за пределы блока и имеет бессрочный иммунитет - заменяем на временный
                    else if (blockEntity.isImmunePlayer(playerUUID) && blockEntity.immunePlayers.get(playerUUID) == Long.MAX_VALUE) {
                        // Вместо удаления иммунитета, ставим его на 2 секунды
                        blockEntity.setTemporaryImmunity(playerUUID, IMMUNITY_DELAY_AFTER_EXIT);
                        RadiationMod.LOGGER.info("Игрок " + player.getName().getString() + 
                            " вышел из зоны капкана, но сохраняет иммунитет еще на 2 секунды. Текущая позиция: " +
                            player.getX() + ", " + player.getY() + ", " + player.getZ());
                    }
                }
                
                // Проверяем и удаляем истекшие временные иммунитеты
                long currentTime = System.currentTimeMillis();
                Set<UUID> expiredImmunities = new HashSet<>();
                
                for (Map.Entry<UUID, Long> entry : blockEntity.immunePlayers.entrySet()) {
                    // Пропускаем бессрочные иммунитеты
                    if (entry.getValue() == Long.MAX_VALUE) {
                        continue;
                    }
                    
                    // Проверяем истекшие иммунитеты
                    if (currentTime >= entry.getValue()) {
                        expiredImmunities.add(entry.getKey());
                        Entity entity = serverLevel.getEntity(entry.getKey());
                        if (entity instanceof Player player) {
                            RadiationMod.LOGGER.info("Временный иммунитет игрока " + player.getName().getString() + 
                                " истек. Текущая позиция: " + player.getX() + ", " + player.getY() + ", " + player.getZ());
                        }
                    }
                }
                
                // Удаляем истекшие иммунитеты
                for (UUID uuid : expiredImmunities) {
                    blockEntity.immunePlayers.remove(uuid);
                    blockEntity.setChanged();
                    RadiationMod.LOGGER.info("Иммунитет удален для UUID: " + uuid);
                }
            }
            
            // Проверяем, что капкан активирован (закрыт с пойманным игроком)
            if (state.hasProperty(BearTrapBlock.TRIGGERED) && state.getValue(BearTrapBlock.TRIGGERED) &&
                blockEntity.caughtPlayerUUID != null) {
                
                // Проверяем, что уровень является ServerLevel
                if (!(level instanceof ServerLevel serverLevel)) {
                    return;
                }
                
                // Если игрок удаляется от капкана или пытается прыгнуть, телепортируем его обратно
                Entity playerEntity = serverLevel.getEntity(blockEntity.caughtPlayerUUID);
                if (playerEntity instanceof Player player) {
                    double distanceSq = player.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    
                    // Если игрок слишком далеко (например, телепортировался)
                    if (distanceSq > 4.0) {
                        // Игрок слишком далеко от капкана - это может произойти при телепортации
                        blockEntity.clearCaughtPlayer();
                        BearTrapBlock.onPlayerStoppedRightClick(player);
                        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                        RadiationMod.LOGGER.info("Игрок слишком далеко от капкана - освобожден");
                    }
                    // Если игрок пытается прыгнуть (изменилась Y координата)
                    else if (Math.abs(player.getY() - blockEntity.caughtPlayerY) > 0.1) {
                        // Телепортируем игрока обратно
                        player.teleportTo(blockEntity.caughtPlayerX, blockEntity.caughtPlayerY, blockEntity.caughtPlayerZ);
                        RadiationMod.LOGGER.info("Игрок попытался прыгнуть - телепортирован обратно");
                    }
                } else {
                    // Игрок не найден - освобождаем капкан
                    blockEntity.clearCaughtPlayer();
                }
            }
            
        } catch (Exception e) {
            // Логируем исключение для отладки
            RadiationMod.LOGGER.error("Ошибка при обработке тика капкана: " + e.getMessage());
            e.printStackTrace();
            // Очищаем состояние в случае ошибки
            blockEntity.clearCaughtPlayer();
        }
    }
    
    /**
     * Сохраняем данные в NBT
     */
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        
        // Сохраняем UUID пойманного игрока
        if (this.caughtPlayerUUID != null) {
            tag.putUUID("CaughtPlayer", this.caughtPlayerUUID);
            tag.putLong("CaughtTime", this.caughtTime);
            tag.putDouble("CaughtPlayerX", this.caughtPlayerX);
            tag.putDouble("CaughtPlayerY", this.caughtPlayerY);
            tag.putDouble("CaughtPlayerZ", this.caughtPlayerZ);
        }
        
        // Сохраняем статус открытия
        tag.putBoolean("IsBeingOpened", this.isBeingOpened);
        tag.putFloat("OpeningProgress", this.openingProgress);
        
        // Сохраняем список игроков с иммунитетом (просто UUID, без времени)
        CompoundTag immunePlayersTag = new CompoundTag();
        int i = 0;
        for (UUID uuid : this.immunePlayers.keySet()) {
            immunePlayersTag.putUUID("Player" + i, uuid);
            i++;
        }
        tag.putInt("ImmunePlayersCount", i);
        tag.put("ImmunePlayers", immunePlayersTag);
    }
    
    /**
     * Загружаем данные из NBT
     */
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        
        // Загружаем UUID пойманного игрока
        if (tag.contains("CaughtPlayer")) {
            this.caughtPlayerUUID = tag.getUUID("CaughtPlayer");
            this.caughtTime = tag.getLong("CaughtTime");
            this.caughtPlayerX = tag.getDouble("CaughtPlayerX");
            this.caughtPlayerY = tag.getDouble("CaughtPlayerY");
            this.caughtPlayerZ = tag.getDouble("CaughtPlayerZ");
        } else {
            this.caughtPlayerUUID = null;
            this.caughtTime = 0;
            this.caughtPlayerX = 0;
            this.caughtPlayerY = 0;
            this.caughtPlayerZ = 0;
        }
        
        // Загружаем статус открытия
        this.isBeingOpened = tag.getBoolean("IsBeingOpened");
        this.openingProgress = tag.getFloat("OpeningProgress");
        
        // Загружаем список игроков с иммунитетом
        this.immunePlayers.clear();
        if (tag.contains("ImmunePlayers") && tag.contains("ImmunePlayersCount")) {
            CompoundTag immunePlayersTag = tag.getCompound("ImmunePlayers");
            int count = tag.getInt("ImmunePlayersCount");
            
            for (int i = 0; i < count; i++) {
                if (immunePlayersTag.contains("Player" + i)) {
                    UUID uuid = immunePlayersTag.getUUID("Player" + i);
                    this.immunePlayers.put(uuid, Long.MAX_VALUE); // Бессрочный иммунитет
                }
            }
        }
    }
    
    /**
     * Получаем данные для синхронизации с клиентом
     */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        if (caughtPlayerUUID != null) {
            tag.putUUID("CaughtPlayer", caughtPlayerUUID);
            tag.putLong("CaughtTime", this.caughtTime);
            tag.putDouble("CaughtPlayerX", this.caughtPlayerX);
            tag.putDouble("CaughtPlayerY", this.caughtPlayerY);
            tag.putDouble("CaughtPlayerZ", this.caughtPlayerZ);
        }
        tag.putBoolean("IsBeingOpened", isBeingOpened);
        tag.putFloat("OpeningProgress", openingProgress);
        
        // Синхронизируем список игроков с иммунитетом
        CompoundTag immunePlayersTag = new CompoundTag();
        int i = 0;
        for (UUID uuid : immunePlayers.keySet()) {
            immunePlayersTag.putUUID("Player" + i, uuid);
            i++;
        }
        immunePlayersTag.putInt("ImmunePlayersCount", i);
        tag.put("ImmunePlayers", immunePlayersTag);
        
        return tag;
    }
    
    /**
     * Создаем пакет для синхронизации с клиентом
     */
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
} 