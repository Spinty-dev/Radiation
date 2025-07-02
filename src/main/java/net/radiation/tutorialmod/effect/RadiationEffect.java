package net.radiation.tutorialmod.effect;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.radiation.tutorialmod.RadiationMod;
import net.radiation.tutorialmod.damage.ModDamageTypes;

public class RadiationEffect extends MobEffect {
    // Массив интервалов урона в тиках для каждого уровня (индекс = уровень - 1)
    private static final int[] DAMAGE_INTERVALS = {
        60,  // Уровень 1: 3 секунды (60 тиков)
        50,  // Уровень 2: 2.5 секунды (50 тиков)
        40,  // Уровень 3: 2 секунды (40 тиков)
        30,  // Уровень 4: 1.5 секунды (30 тиков)
        20   // Уровень 5: 1 секунда (20 тиков)
    };
    
    // Массив урона для каждого уровня (индекс = уровень - 1)
    private static final float[] DAMAGE_AMOUNTS = {
        1.0F,   // Уровень 1: 0.5 сердечка
        2.0F,   // Уровень 2: 1 сердечко
        3.0F,   // Уровень 3: 1.5 сердечка
        4.0F,   // Уровень 4: 2 сердечка
        5.0F    // Уровень 5: 2.5 сердечка (смертельная доза)
    };
    
    // Интервал и урон для критического (255) уровня
    private static final int CRITICAL_DAMAGE_INTERVAL = 10; // 0.5 секунды (10 тиков)
    private static final float CRITICAL_DAMAGE_AMOUNT = 10.0F; // 5 сердечек за раз

    public RadiationEffect() {
        super(MobEffectCategory.HARMFUL, 0x7FFF00);
    }
    
    /**
     * Переопределяем метод для имени эффекта, добавляя в начало символы для поднятия в списке.
     * Minecraft сортирует эффекты по имени, поэтому добавляем префикс, который будет в начале алфавита.
     */
    @Override
    public String getDescriptionId() {
        // Добавляем префикс "AAA", чтобы эффект оказался в начале списка при сортировке
        return "AAA_" + super.getDescriptionId();
    }
    
    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Проверка не клиентская сторона
        if (!entity.level().isClientSide()) {
            // Проверяем на смертельный уровень радиации (255)
            if (amplifier == 254) { // 254 = уровень 255
                // Наносим критический урон от радиации
                entity.hurt(ModDamageTypes.radiation(entity.level()), CRITICAL_DAMAGE_AMOUNT);
                
                // Получаем имя игрока для логирования
                String playerName = "существо";
                if (entity instanceof Player player) {
                    playerName = player.getName().getString();
                }
                
                RadiationMod.LOGGER.info(">>> КРИТИЧЕСКАЯ доза радиации для {}, урон: {}", 
                        playerName, CRITICAL_DAMAGE_AMOUNT);
                return;
            }
            
            // Ограничиваем amplifier диапазоном 0-4 (уровни 1-5)
            int safeAmplifier = Math.min(Math.max(amplifier, 0), 4);
            int level = safeAmplifier + 1; // Уровень от 1 до 5
            
            // Получаем имя игрока для логирования
            String playerName = "существо";
            if (entity instanceof Player player) {
                playerName = player.getName().getString();
            }
            
            RadiationMod.LOGGER.info(">>> Применение тика радиации для {}, уровень: {}", 
                    playerName, level);
            
            // Если это 5 уровень и у игрока мало здоровья, убиваем от радиации
            if (level == 5 && entity.getHealth() < 6.0F) {
                entity.hurt(ModDamageTypes.radiation(entity.level()), 100.0F);
                RadiationMod.LOGGER.info(">>> ВНИМАНИЕ: Уровень 5 радиации убил игрока {} с низким здоровьем!", 
                        playerName);
                return;
            }
            
            // Получаем урон для текущего уровня
            float damageAmount = DAMAGE_AMOUNTS[safeAmplifier];
            
            // Наносим урон от радиации
            entity.hurt(ModDamageTypes.radiation(entity.level()), damageAmount);
            RadiationMod.LOGGER.info(">>> Радиация наносит {} урона игроку {}", 
                    damageAmount, playerName);
            
            if (level == 1) {
                // Уровень 1: только урон
                RadiationMod.LOGGER.info(">>> Уровень 1 радиации: только урон");
            } else if (level >= 2) {
                // Уровень 2+: урон + базовые эффекты
                RadiationMod.LOGGER.info(">>> Добавление побочных эффектов радиации уровня {}", level);
                
                // Основные побочные эффекты (увеличиваем длительность для надежности)
                entity.addEffect(new HiddenMobEffectInstance(MobEffects.BLINDNESS, 200, 0));
                entity.addEffect(new HiddenMobEffectInstance(MobEffects.CONFUSION, 300, 0));
                entity.addEffect(new HiddenMobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0));
                entity.addEffect(new HiddenMobEffectInstance(MobEffects.WEAKNESS, 200, 0));
                
                // Повышаем интенсивность эффектов с ростом уровня
                if (level >= 3) {
                    entity.addEffect(new HiddenMobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1));
                    entity.addEffect(new HiddenMobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                }
                
                if (level >= 4) {
                    entity.addEffect(new HiddenMobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 0));
                }
                
                if (level == 5) {
                    // Удалили эффект иссушения WITHER
                    // При пятом уровне радиации скоро наступает смерть
                }
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Проверка на смертельный уровень радиации (255)
        if (amplifier == 254) { // 254 = уровень 255
            // Применяем урон каждые CRITICAL_DAMAGE_INTERVAL тиков
            boolean shouldTick = (duration % CRITICAL_DAMAGE_INTERVAL == 0);
            
            // Логируем для отладки
            if (duration % 20 == 0) {
                RadiationMod.LOGGER.info(">>> КРИТИЧЕСКАЯ доза: проверка тика радиации: duration={}, уровень=255, интервал={}",
                        duration, CRITICAL_DAMAGE_INTERVAL);
            }
            
            // Логируем, если сейчас будет нанесен критический урон
            if (shouldTick) {
                RadiationMod.LOGGER.info(">>> КРИТИЧЕСКИЙ УРОН РАДИАЦИИ БУДЕТ НАНЕСЕН НА ТИКЕ {}", 
                        duration);
            }
            
            return shouldTick;
        }
        
        // Ограничиваем amplifier диапазоном 0-4 (уровни 1-5)
        int safeAmplifier = Math.min(Math.max(amplifier, 0), 4);
        int level = safeAmplifier + 1; // Уровень от 1 до 5
        
        // Получаем интервал урона для данного уровня
        int interval = DAMAGE_INTERVALS[safeAmplifier];
        
        // Логируем для отладки каждую секунду
        if (duration % 20 == 0) {
            RadiationMod.LOGGER.info(">>> Проверка тика радиации: duration={}, уровень={}, интервал={}",
                    duration, level, interval);
        }
        
        // Возвращаем true, если текущий тик должен нанести урон
        boolean shouldTick = (duration % interval == 0);
        
        // Логируем, если сейчас будет нанесен урон
        if (shouldTick) {
            RadiationMod.LOGGER.info(">>> УРОН РАДИАЦИИ УРОВНЯ {} БУДЕТ НАНЕСЕН НА ТИКЕ {}", 
                    level, duration);
        }
        
        return shouldTick;
    }
}
