package net.radiation.tutorialmod.event;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.radiation.tutorialmod.RadiationMod;
import net.radiation.tutorialmod.client.BalaclavaModel;

@Mod.EventBusSubscriber(modid = RadiationMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    // Добавляем ModelLayerLocation для балаклавы
    public static final ModelLayerLocation BALACLAVA_LAYER = new ModelLayerLocation(
            new ResourceLocation(RadiationMod.MOD_ID, "balaclava"), "main");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Клиентская инициализация, если нужна
        RadiationMod.LOGGER.info("Инициализация клиентской части мода");
    }
    
    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // Регистрируем определение слоя модели балаклавы
        event.registerLayerDefinition(BALACLAVA_LAYER, BalaclavaModel::createBodyLayer);
        RadiationMod.LOGGER.info("Зарегистрирован слой модели для балаклавы");
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        // Закомментировано, пока не будет реализован BalaclavaRenderLayer
        LivingEntityRenderer renderer = event.getRenderer(EntityType.PLAYER);
        if (renderer != null) {
            // renderer.addLayer(new BalaclavaRenderLayer(renderer));
            RadiationMod.LOGGER.info("Слой балаклавы отключен");
        } else {
            RadiationMod.LOGGER.error("Не удалось получить рендерер игрока");
        }
    }
} 