package net.radiation.tutorialmod.block.entity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.radiation.tutorialmod.RadiationMod;
import net.radiation.tutorialmod.block.ModBlocks;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RadiationMod.MOD_ID);

    // Регистрируем BlockEntity для капкана
    public static final RegistryObject<BlockEntityType<BearTrapBlockEntity>> BEAR_TRAP =
            BLOCK_ENTITIES.register("bear_trap",
                    () -> BlockEntityType.Builder.of(
                            BearTrapBlockEntity::new,
                            ModBlocks.BEAR_TRAP.get()
                    ).build(null));

    // Регистрируем BlockEntities
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
} 