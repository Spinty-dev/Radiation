package net.radiation.tutorialmod.radiation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.radiation.tutorialmod.RadiationMod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Класс для сохранения статистики о радиации (только для совместимости с существующим кодом)
 * Основная логика радиации теперь перенесена в RadiationEffect
 */
public class RadiationTracker extends SavedData {
    private static final String DATA_NAME = RadiationMod.MOD_ID + "_radiation_tracker";
    
    // Сохраняем только для обратной совместимости
    private final Map<UUID, Integer> exposureDurations = new HashMap<>();
    
    private RadiationTracker() {
        super();
    }
    
    /**
     * Получить экземпляр трекера для указанного мира
     */
    public static RadiationTracker get(ServerLevel level) {
        RadiationTracker tracker = level.getDataStorage().computeIfAbsent(
                RadiationTracker::load,
                RadiationTracker::new,
                DATA_NAME);
        RadiationMod.LOGGER.debug("Получен трекер радиации для измерения {}", level.dimension().location());
        return tracker;
    }
    
    /**
     * Загрузка данных из NBT
     */
    public static RadiationTracker load(CompoundTag tag) {
        RadiationTracker tracker = new RadiationTracker();
        
        // Загружаем данные для совместимости, но они не используются
        CompoundTag exposuresTag = tag.getCompound("Exposures");
        for (String key : exposuresTag.getAllKeys()) {
            try {
                UUID playerId = UUID.fromString(key);
                int duration = exposuresTag.getInt(key);
                tracker.exposureDurations.put(playerId, duration);
            } catch (IllegalArgumentException e) {
                RadiationMod.LOGGER.error(">>> Невозможно распознать UUID: {}", key);
            }
        }
        
        return tracker;
    }
    
    /**
     * Сохранение данных в NBT
     */
    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag exposuresTag = new CompoundTag();
        
        // Сохраняем для совместимости
        for (Map.Entry<UUID, Integer> entry : exposureDurations.entrySet()) {
            exposuresTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        
        tag.put("Exposures", exposuresTag);
        
        RadiationMod.LOGGER.debug("Сохранены данные трекера радиации ({} записей)", exposureDurations.size());
        return tag;
    }
    
    /**
     * Принудительное сохранение данных трекера
     */
    public void forceSave(ServerLevel level) {
        this.setDirty();
        RadiationMod.LOGGER.info("Принудительное сохранение трекера радиации в измерении {}", 
                level.dimension().location());
    }
    
    // Методы ниже реализованы для обратной совместимости, но не используются

    public int getExposureDuration(UUID playerId) {
        return 0;
    }
    
    public float getExposureFactor(UUID playerId) {
        return 0.0f;
    }
    
    public int incrementExposure(ServerPlayer player) {
        return 0;
    }
    
    public int decrementExposure(ServerPlayer player) {
        return 0;
    }
    
    public void setExposureDuration(ServerPlayer player, int duration) {
        // Ничего не делаем
    }
    
    public void resetExposure(ServerPlayer player) {
        // Ничего не делаем
    }
} 