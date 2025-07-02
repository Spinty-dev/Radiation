package net.radiation.tutorialmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Класс специального эффекта, который не показывает
 * иконку и частицы в пользовательском интерфейсе
 */
public class HiddenMobEffectInstance extends MobEffectInstance {
    
    /**
     * Конструктор для создания скрытого эффекта
     */
    public HiddenMobEffectInstance(MobEffect effect, int duration, int amplifier) {
        super(effect, duration, amplifier,
            false, // showParticles
            false, // showIcon
            true   // hiddenEffect
        );
    }
    
    /**
     * Переопределяем для скрытия иконки эффекта
     * В Minecraft 1.20.1 этот метод определен в MobEffectInstance
     */
    @Override
    public boolean showIcon() {
        return false;
    }
}
