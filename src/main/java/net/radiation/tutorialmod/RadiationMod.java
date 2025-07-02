package net.radiation.tutorialmod;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import net.radiation.tutorialmod.effect.ModEffects;
import net.radiation.tutorialmod.item.ModItems;
import net.radiation.tutorialmod.radiation.RadiationManager;
import net.radiation.tutorialmod.integration.curios.CuriosIntegration;
import net.radiation.tutorialmod.block.ModBlocks;
import net.radiation.tutorialmod.block.entity.ModBlockEntities;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.radiation.tutorialmod.damage.ModDamageTypes;
import net.minecraftforge.event.level.LevelEvent;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(RadiationMod.MOD_ID)
public class RadiationMod
{
    public static final String MOD_ID = "radiationmod";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final RadiationManager RADIATION_MANAGER = new RadiationManager();

    public RadiationMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Регистрируем эффекты, предметы, блоки
        ModEffects.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        
        // Регистрируем интеграцию с Curios API
        CuriosIntegration.register();

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Инициализируем менеджер радиации
        RADIATION_MANAGER.init();
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        // Добавляем предметы в креативную табу
        ModItems.addItemsToCreativeTab(event);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("Сервер начинает запуск...");
        // Сбрасываем флаг загрузки зон
        RadiationManager.resetLoadedFlag();
    }
    
    /**
     * Загружаем зоны радиации после полного запуска сервера
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event)
    {
        LOGGER.info("Сервер полностью запущен, загружаем зоны радиации...");
        // Загружаем данные о зонах радиации после полного запуска сервера
        RADIATION_MANAGER.load(event.getServer());
    }

    /**
     * Сохраняем зоны радиации при остановке сервера
     */
    @SubscribeEvent 
    public void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event)
    {
        // Сохраняем данные о зонах радиации перед остановкой сервера
        RADIATION_MANAGER.saveAllZones();
        LOGGER.info("Сохранение зон радиации при остановке сервера");
    }

    /**
     * Сохраняем зоны радиации при сохранении мира
     */
    @SubscribeEvent
    public void onLevelSave(LevelEvent.Save event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        // Сохраняем зоны радиации при сохранении мира
        LOGGER.info("Событие сохранения мира. Принудительное сохранение всех зон радиации...");
        RADIATION_MANAGER.saveAllZones();
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Регистрация клиентских компонентов
        }
    }
} 