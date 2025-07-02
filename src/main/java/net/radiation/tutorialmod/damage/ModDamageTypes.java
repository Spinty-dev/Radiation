package net.radiation.tutorialmod.damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.radiation.tutorialmod.RadiationMod;

public class ModDamageTypes {
    // Ключ для нашего типа урона от радиации
    public static final ResourceKey<DamageType> RADIATION = 
            ResourceKey.create(Registries.DAMAGE_TYPE, 
                    new ResourceLocation("minecraft", "radiation_poison"));
    
    // Создаем фабрику для нашего источника урона
    public static DamageSource radiation(Level level) {
        // Поскольку Mojang - придурки и никак не дают нам использовать 
        // кастомные типы урона, используем magic как заменитель
        return level.damageSources().magic();
    }
} 