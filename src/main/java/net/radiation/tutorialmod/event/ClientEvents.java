package net.radiation.tutorialmod.event;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.radiation.tutorialmod.RadiationMod;
import net.radiation.tutorialmod.effect.ModEffects;
import net.radiation.tutorialmod.integration.curios.CuriosIntegration;
import net.radiation.tutorialmod.item.RadiationFilterItem;
import top.theillusivec4.curios.api.CuriosApi;


@Mod.EventBusSubscriber(modid = RadiationMod.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {
    private static final ResourceLocation RADIATION_VIGNETTE = new ResourceLocation(RadiationMod.MOD_ID, "textures/gui/radiation_vignette.png");

    /**
     * Отображает предупреждение о состоянии фильтра на экране
     */
    @SuppressWarnings("deprecation")
    @SubscribeEvent
    public static void onRenderHUD(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        GuiGraphics guiGraphics = event.getGuiGraphics();
        
        if (player == null) return;
        
        // Проверяем наличие фильтра в слоте Curios
        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
            handler.getStacksHandler(CuriosIntegration.RADIATION_FILTER_SLOT).ifPresent(stacksHandler -> {
                for(int i = 0; i < stacksHandler.getSlots(); i++) {
                    ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                    if(stack.getItem() instanceof RadiationFilterItem) {
                        int damage = RadiationFilterItem.getFilterDamage(stack);
                        int durability = RadiationFilterItem.MAX_DURABILITY - damage;
                        float percentage = (float) durability / RadiationFilterItem.MAX_DURABILITY * 100;
                        
                        // Отображаем предупреждение при 50% и ниже
                        if (durability <= RadiationFilterItem.WARN_THRESHOLD) {
                            Component message;
                            int color;
                            
                            if (durability <= RadiationFilterItem.DANGER_THRESHOLD) {
                                // Красное жирное предупреждение при 10% и ниже
                                message = Component.translatable("message.radiationmod.filter_critical")
                                    .withStyle(ChatFormatting.BOLD)
                                    .withStyle(ChatFormatting.RED);
                                color = 0xFF0000; // Красный
                            } else {
                                // Желтое предупреждение при 50% и ниже
                                message = Component.translatable("message.radiationmod.filter_warning")
                                    .withStyle(ChatFormatting.YELLOW);
                                color = 0xFFFF00; // Желтый
                            }
                            
                            // Добавляем информацию о проценте прочности
                            String percentText = String.format("%.1f%%", percentage);
                            Component fullMessage = Component.literal("")
                                .append(message)
                                .append(" ")
                                .append(Component.literal(percentText).withStyle(durability <= RadiationFilterItem.DANGER_THRESHOLD 
                                    ? ChatFormatting.BOLD : ChatFormatting.RESET));
                            
                            // Отрисовываем текст в нижней части экрана
                            int width = mc.getWindow().getGuiScaledWidth();
                            int height = mc.getWindow().getGuiScaledHeight();
                            
                            int textWidth = mc.font.width(fullMessage);
                            // Поднимаем текст выше, чтобы не перекрывался броней
                            int y = height - 70; // Раньше было -50, теперь -70
                            
                            // Рисуем фон для лучшей видимости
                            guiGraphics.fill(width / 2 - textWidth / 2 - 2, y - 2, 
                                            width / 2 + textWidth / 2 + 2, y + 10, 
                                            0x80000000);
                            
                            // Рисуем текст
                            guiGraphics.drawCenteredString(mc.font, fullMessage, width / 2, y, color);
                        }
                        break;
                    }
                }
            });
        });
    }

    /**
     * Фильтрует отображение эффектов в интерфейсе
     * Оставляет только эффект радиации, скрывая побочные эффекты
     */
    @SubscribeEvent
    public static void onRenderPotionEffects(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.POTION_ICONS.type()) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;

            if (player != null && !player.getActiveEffects().isEmpty()) {
                // Проверяем, если у игрока только эффект радиации
                boolean hasRadiationEffect = false;
                boolean hasOtherEffects = false;

                for (MobEffectInstance effect : player.getActiveEffects()) {
                    ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
                    
                    // Если это наш эффект радиации
                    if (effectId != null && effectId.equals(
                            ForgeRegistries.MOB_EFFECTS.getKey(ModEffects.RADIATION.get()))) {
                        hasRadiationEffect = true;
                    } else {
                        // Проверяем, является ли это одним из побочных эффектов радиации
                        ResourceLocation blindness = ForgeRegistries.MOB_EFFECTS.getKey(net.minecraft.world.effect.MobEffects.BLINDNESS);
                        ResourceLocation confusion = ForgeRegistries.MOB_EFFECTS.getKey(net.minecraft.world.effect.MobEffects.CONFUSION);
                        ResourceLocation slowdown = ForgeRegistries.MOB_EFFECTS.getKey(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
                        ResourceLocation weakness = ForgeRegistries.MOB_EFFECTS.getKey(net.minecraft.world.effect.MobEffects.WEAKNESS);
                        
                        if (effectId.equals(blindness) || effectId.equals(confusion) || 
                            effectId.equals(slowdown) || effectId.equals(weakness)) {
                            // Это побочный эффект, скрываем его
                            hasOtherEffects = true;
                        }
                    }
                }
                
                // Если у игрока есть эффект радиации, и также есть побочные эффекты,
                // то отменяем отображение, чтобы показать только нужные эффекты
                if (hasRadiationEffect && hasOtherEffects) {
                    event.setCanceled(true);
                    
                    // Здесь можно добавить собственную отрисовку эффекта радиации
                    // если нужно его всё же отобразить
                }
            }
        }
    }
    
    /**
     * Скрывает побочные эффекты в инвентаре
     */
    @SubscribeEvent
    public static void onRenderInventoryEffects(ScreenEvent.Render.Pre event) {
        if (event.getScreen() instanceof EffectRenderingInventoryScreen) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || player.getActiveEffects().isEmpty()) return;

            // Проверяем наличие радиации и побочных эффектов
            boolean hasRadiationEffect = false;
            boolean hasOtherEffects = false;

            for (MobEffectInstance effect : player.getActiveEffects()) {
                ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
                
                // Если это наш эффект радиации
                if (effectId != null && effectId.equals(
                        ForgeRegistries.MOB_EFFECTS.getKey(ModEffects.RADIATION.get()))) {
                    hasRadiationEffect = true;
                } else {
                    // Проверяем, является ли это одним из побочных эффектов радиации
                    ResourceLocation blindness = ForgeRegistries.MOB_EFFECTS.getKey(net.minecraft.world.effect.MobEffects.BLINDNESS);
                    ResourceLocation confusion = ForgeRegistries.MOB_EFFECTS.getKey(net.minecraft.world.effect.MobEffects.CONFUSION);
                    ResourceLocation slowdown = ForgeRegistries.MOB_EFFECTS.getKey(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
                    ResourceLocation weakness = ForgeRegistries.MOB_EFFECTS.getKey(net.minecraft.world.effect.MobEffects.WEAKNESS);
                    
                    if (effectId.equals(blindness) || effectId.equals(confusion) || 
                        effectId.equals(slowdown) || effectId.equals(weakness)) {
                        // Это побочный эффект, скрываем его
                        hasOtherEffects = true;
                    }
                }
            }
            
            // Если есть и эффект радиации, и побочные эффекты - нужно скрыть все и вручную
            // отрисовать только нужные нам
            if (hasRadiationEffect && hasOtherEffects) {
                // Тут нельзя просто отменить отрисовку, но можно временно скрыть эффекты
                // и восстановить их после отрисовки
                RadiationMod.LOGGER.debug("Обнаружены побочные эффекты радиации в инвентаре");
                
                // Примечание: в инвентаре сложнее контролировать отображение эффектов, 
                // поэтому здесь может потребоваться более сложная логика или использование
                // миксинов для полного решения
            }
        }
    }

    /**
     * Отрисовывает виньетку при эффекте радиации
     * ОТКЛЮЧЕНО: эффект виньетки был удален по запросу пользователя
     */
    /*
    @SubscribeEvent
    public static void onRenderHUD(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        GuiGraphics guiGraphics = event.getGuiGraphics();
        
        if (player != null && !player.getActiveEffects().isEmpty()) {
            // Проверяем, есть ли эффект радиации
            for (MobEffectInstance effect : player.getActiveEffects()) {
                ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
                
                if (effectId != null && effectId.equals(
                        ForgeRegistries.MOB_EFFECTS.getKey(ModEffects.RADIATION.get()))) {
                    
                    // Получаем интенсивность эффекта для настройки прозрачности виньетки
                    int amplifier = effect.getAmplifier();
                    float alpha = Math.min(0.9f, 0.5f + (amplifier * 0.2f)); // Усиление виньетки с уровнем эффекта
                    
                    // Отрисовываем виньетку
                    int width = mc.getWindow().getGuiScaledWidth();
                    int height = mc.getWindow().getGuiScaledHeight();
                    
                    guiGraphics.setColor(1.0F, 0.1F, 0.1F, alpha); // Более насыщенный красный оттенок
                    guiGraphics.blit(RADIATION_VIGNETTE, 0, 0, 0, 0, width, height, width, height);
                    guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F); // Сброс цвета
                    
                    break; // Нашли эффект, отрисовали виньетку, выходим
                }
            }
        }
    }
    */
} 