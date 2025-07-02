package net.radiation.tutorialmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.radiation.tutorialmod.RadiationMod;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = 
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, RadiationMod.MOD_ID);
    
    public static final RegistryObject<MobEffect> RADIATION = 
            MOB_EFFECTS.register("radiation", RadiationEffect::new);
    
    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
} 