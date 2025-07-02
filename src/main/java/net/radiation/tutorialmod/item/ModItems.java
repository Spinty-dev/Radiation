package net.radiation.tutorialmod.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.radiation.tutorialmod.RadiationMod;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = 
            DeferredRegister.create(ForgeRegistries.ITEMS, RadiationMod.MOD_ID);
    
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RadiationMod.MOD_ID);
    
    // Инструмент для установки позиций радиации
    public static final RegistryObject<Item> RADIATION_WAND = registerItem("radiation_wand", 
            () -> new RadiationWandItem(new Item.Properties().stacksTo(1)));
    
    // Детектор радиации - добавляем новый предмет (активирует изменение уровня)
    public static final RegistryObject<Item> RADIATION_DETECTOR = registerItem("radiation_detector", 
            () -> new RadiationDetectorItem(new Item.Properties().stacksTo(1)));
    
    // Фильтр для защиты от радиации
    public static final RegistryObject<Item> RADIATION_FILTER = registerItem("radiation_filter", 
            () -> new RadiationFilterItem(new Item.Properties().stacksTo(1)));
    
    // Капкан - ловушка
    public static final RegistryObject<Item> BEAR_TRAP = registerItem("bear_trap",
            () -> new BearTrapItem(new Item.Properties().stacksTo(16)));
    
    // Балаклава - головной убор
    public static final RegistryObject<Item> BALACLAVA = registerItem("balaclava",
            () -> new BalaclavaItem(new Item.Properties().stacksTo(1)));
    
    // Креативная таба для предметов мода
    public static final RegistryObject<CreativeModeTab> RADIATION_TAB = CREATIVE_MODE_TABS.register("radiation_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.radiationmod.radiation_tab"))
                    .icon(() -> new ItemStack(ModItems.RADIATION_WAND.get()))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(RADIATION_WAND.get());
                        pOutput.accept(RADIATION_DETECTOR.get());
                        pOutput.accept(RADIATION_FILTER.get());
                        pOutput.accept(BEAR_TRAP.get());
                        pOutput.accept(BALACLAVA.get());
                    })
                    .build()
    );
    
    /**
     * Регистрирует предмет
     */
    private static <T extends Item> RegistryObject<T> registerItem(String name, Supplier<T> item) {
        return ITEMS.register(name, item);
    }
    
    /**
     * Регистрирует все необходимые компоненты
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        CREATIVE_MODE_TABS.register(eventBus);
    }
    
    /**
     * Добавляет предметы в креативную табу
     */
    public static void addItemsToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        // Добавляем предметы в соответствующие табы
        if (event.getTab() == RADIATION_TAB.get()) {
            event.accept(RADIATION_WAND.get());
            event.accept(RADIATION_DETECTOR.get());
            event.accept(RADIATION_FILTER.get());
            event.accept(BEAR_TRAP.get());
            event.accept(BALACLAVA.get());
        }
    }
} 