package net.radiation.tutorialmod.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.radiation.tutorialmod.RadiationMod;
import net.radiation.tutorialmod.item.ModItems;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = 
            DeferredRegister.create(ForgeRegistries.BLOCKS, RadiationMod.MOD_ID);
    
    // Блок капкана
    public static final RegistryObject<Block> BEAR_TRAP = registerBlock("bear_trap",
            () -> new BearTrapBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f)   // Прочность
                    .noCollission()   // Без коллизии - можно пройти сквозь блок
                    .noOcclusion()    // Не блокирует видимость
            ));
    
    /**
     * Регистрирует блок и его BlockItem
     */
    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        // Не регистрируем ItemBlock, так как будем использовать кастомный предмет
        return toReturn;
    }
    
    /**
     * Регистрирует все блоки
     */
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
} 