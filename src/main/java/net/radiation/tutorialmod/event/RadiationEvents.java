package net.radiation.tutorialmod.event;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.radiation.tutorialmod.RadiationMod;
import net.radiation.tutorialmod.effect.ModEffects;
import net.radiation.tutorialmod.item.RadiationFilterItem;
import net.radiation.tutorialmod.radiation.RadiationManager.RadiationZone;
import net.radiation.tutorialmod.damage.ModDamageTypes;
import top.theillusivec4.curios.api.CuriosApi;
import net.radiation.tutorialmod.integration.curios.CuriosIntegration;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Mod.EventBusSubscriber(modid = RadiationMod.MOD_ID)
public class RadiationEvents {

    // Урон наносимый фильтру при нахождении в радиационной зоне (раз в секунду)
    private static final int FILTER_DAMAGE_PER_TICK = 1;
    
    // ID предметов защитного костюма hazmat
    // Обновленные ID с учетом скриншота 
    private static final String HAZMAT_HELMET = "marbledsarsenal:hazmat_armor_helmet";
    private static final String HAZMAT_CHESTPLATE = "marbledsarsenal:hazmat_armor_chestplate";
    private static final String HAZMAT_LEGGINGS = "marbledsarsenal:hazmat_armor_leggings";
    private static final String HAZMAT_BOOTS = "marbledsarsenal:hazmat_armor_boots";
    
    // Альтернативные варианты ID для поддержки разных версий мода
    private static final String[] HAZMAT_HELMET_VARIANTS = {
        "marbledsarsenal:hazmat_armor_helmet",
        "marbled's arsenal:hazmat_armor_helmet",
        "marbledarsenal:hazmat_armor_helmet"
    };
    
    private static final String[] HAZMAT_CHESTPLATE_VARIANTS = {
        "marbledsarsenal:hazmat_armor_chestplate",
        "marbled's arsenal:hazmat_armor_chestplate",
        "marbledarsenal:hazmat_armor_chestplate"
    };
    
    private static final String[] HAZMAT_LEGGINGS_VARIANTS = {
        "marbledsarsenal:hazmat_armor_leggings",
        "marbled's arsenal:hazmat_armor_leggings",
        "marbledarsenal:hazmat_armor_leggings"
    };
    
    private static final String[] HAZMAT_BOOTS_VARIANTS = {
        "marbledsarsenal:hazmat_armor_boots",
        "marbled's arsenal:hazmat_armor_boots",
        "marbledarsenal:hazmat_armor_boots"
    };

    /**
     * Обработчик события потенциального добавления эффекта радиации
     * Проверяет наличие фильтра и его состояние
     */
    @SubscribeEvent
    public static void onRadiationEffectApplied(MobEffectEvent.Applicable event) {
        // Проверяем, что эффект - это радиация
        if (event.getEffectInstance().getEffect() == ModEffects.RADIATION.get()) {
            // Проверяем, что это игрок
            if (event.getEntity() instanceof Player player) {
                // Если у игрока есть рабочий фильтр и полный защитный костюм, отменяем эффект
                if (hasWorkingFilter(player) && hasFullHazmatSuit(player)) {
                    event.setResult(Event.Result.DENY);
                    
                    // Наносим урон фильтру
                    damageRadiationFilter(player);
                    
                    // Логируем событие
                    RadiationMod.LOGGER.debug("Игрок {} защищен от радиации фильтром и костюмом", player.getName().getString());
                } else if (hasWorkingFilter(player) && !hasFullHazmatSuit(player)) {
                    // Предупреждаем игрока, что ему нужен полный костюм
                    if (player instanceof ServerPlayer serverPlayer) {
                        // Выводим отладочную информацию о броне игрока
                        logPlayerArmor(serverPlayer);
                        
                        serverPlayer.displayClientMessage(
                            Component.translatable("message.radiationmod.missing_hazmat_suit")
                                .withStyle(ChatFormatting.RED), 
                            true
                        );
                    }
                    RadiationMod.LOGGER.debug("Игрок {} имеет фильтр, но не имеет полного костюма", player.getName().getString());
                }
            }
        }
    }
    
    /**
     * Выводит в лог информацию о броне игрока для отладки
     */
    private static void logPlayerArmor(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        
        RadiationMod.LOGGER.info("=== Броня игрока {} ===", player.getName().getString());
        
        if (!helmet.isEmpty()) {
            ResourceLocation helmetId = ForgeRegistries.ITEMS.getKey(helmet.getItem());
            RadiationMod.LOGGER.info("Шлем: {} ({})", helmet.getDisplayName().getString(), helmetId);
            RadiationMod.LOGGER.info("  > Распознается как хазмат: {}", isHelmet(helmet));
            if (helmetId != null) {
                RadiationMod.LOGGER.info("  > ID в реестре: {}", helmetId.toString());
                RadiationMod.LOGGER.info("  > Совпадает с HAZMAT_HELMET: {}", helmetId.toString().equals(HAZMAT_HELMET));
            }
        } else {
            RadiationMod.LOGGER.info("Шлем: Отсутствует");
        }
        
        if (!chestplate.isEmpty()) {
            ResourceLocation chestplateId = ForgeRegistries.ITEMS.getKey(chestplate.getItem());
            RadiationMod.LOGGER.info("Нагрудник: {} ({})", chestplate.getDisplayName().getString(), chestplateId);
            RadiationMod.LOGGER.info("  > Распознается как хазмат: {}", isChestplate(chestplate));
            if (chestplateId != null) {
                RadiationMod.LOGGER.info("  > ID в реестре: {}", chestplateId.toString());
                RadiationMod.LOGGER.info("  > Совпадает с HAZMAT_CHESTPLATE: {}", chestplateId.toString().equals(HAZMAT_CHESTPLATE));
            }
        } else {
            RadiationMod.LOGGER.info("Нагрудник: Отсутствует");
        }
        
        if (!leggings.isEmpty()) {
            ResourceLocation leggingsId = ForgeRegistries.ITEMS.getKey(leggings.getItem());
            RadiationMod.LOGGER.info("Поножи: {} ({})", leggings.getDisplayName().getString(), leggingsId);
            RadiationMod.LOGGER.info("  > Распознается как хазмат: {}", isLeggings(leggings));
            if (leggingsId != null) {
                RadiationMod.LOGGER.info("  > ID в реестре: {}", leggingsId.toString());
                RadiationMod.LOGGER.info("  > Совпадает с HAZMAT_LEGGINGS: {}", leggingsId.toString().equals(HAZMAT_LEGGINGS));
            }
        } else {
            RadiationMod.LOGGER.info("Поножи: Отсутствуют");
        }
        
        if (!boots.isEmpty()) {
            ResourceLocation bootsId = ForgeRegistries.ITEMS.getKey(boots.getItem());
            RadiationMod.LOGGER.info("Ботинки: {} ({})", boots.getDisplayName().getString(), bootsId);
            RadiationMod.LOGGER.info("  > Распознается как хазмат: {}", isBoots(boots));
            if (bootsId != null) {
                RadiationMod.LOGGER.info("  > ID в реестре: {}", bootsId.toString());
                RadiationMod.LOGGER.info("  > Совпадает с HAZMAT_BOOTS: {}", bootsId.toString().equals(HAZMAT_BOOTS));
            }
        } else {
            RadiationMod.LOGGER.info("Ботинки: Отсутствуют");
        }
        
        RadiationMod.LOGGER.info("=== Полный хазмат костюм: {} ===", hasFullHazmatSuit(player));
    }
    
    /**
     * Обработчик тика живого существа
     * Проверяет, есть ли у игрока эффект радиации и рабочий фильтр
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        // Проверяем только раз в 20 тиков (примерно раз в секунду) для оптимизации
        if (event.getEntity().tickCount % 20 != 0) {
            return;
        }
        
        // Проверяем, что это игрок
        if (event.getEntity() instanceof Player player) {
            // Проверяем, есть ли игрок в радиационной зоне
            boolean inRadiationZone = false;
            int radiationLevel = 0;
            
            for (RadiationZone zone : RadiationMod.RADIATION_MANAGER.getRadiationZones()) {
                if (zone.isInside(player.position())) {
                    inRadiationZone = true;
                    radiationLevel = zone.getLevel();
                    break;
                }
            }
            
            // Если игрок в радиационной зоне, проверяем защиту
            if (inRadiationZone) {
                // Проверяем, есть ли у игрока эффект радиации
                if (player.hasEffect(ModEffects.RADIATION.get())) {
                    // Если у игрока есть рабочий фильтр и полный защитный костюм, удаляем эффект радиации
                    if (hasWorkingFilter(player) && hasFullHazmatSuit(player)) {
                        // Удаляем эффект радиации
                        player.removeEffect(ModEffects.RADIATION.get());
                        
                        // Наносим урон фильтру
                        damageRadiationFilter(player);
                        
                        // Логируем событие
                        RadiationMod.LOGGER.debug("Игрок {} надел фильтр и костюм, эффект радиации снят", player.getName().getString());
                    } else if (hasWorkingFilter(player) && !hasFullHazmatSuit(player) && player.tickCount % 200 == 0) {
                        // Предупреждаем игрока каждые 10 секунд
                        if (player instanceof ServerPlayer serverPlayer) {
                            // Раз в минуту выводим отладочную информацию
                            if (player.tickCount % 1200 == 0) {
                                logPlayerArmor(serverPlayer);
                            }
                            
                            serverPlayer.displayClientMessage(
                                Component.translatable("message.radiationmod.missing_hazmat_suit")
                                    .withStyle(ChatFormatting.RED), 
                                true
                            );
                        }
                    }
                } else {
                    // У игрока нет эффекта радиации, проверяем защиту
                    if (hasFullHazmatSuit(player)) {
                        // Если есть полный костюм, проверяем есть ли рабочий фильтр
                        boolean broken = damageRadiationFilter(player);
                        
                        // Если фильтр сломался или его нет, добавляем эффект радиации
                        if (broken) {
                            // Урезаем значение до максимально допустимого (0-4 для уровней 1-5)
                            int amplifier = Math.min(radiationLevel - 1, 4);
                            
                            // Добавляем эффект радиации с уровнем, соответствующим зоне
                            player.addEffect(new MobEffectInstance(
                                ModEffects.RADIATION.get(),
                                // Продолжительность - чем выше уровень, тем дольше эффект
                                (6 - amplifier) * 200, // от 1000 до 200 тиков
                                amplifier, // Амплификатор (0-4)
                                false, // Ambient
                                true,  // Показывать частицы
                                true   // Показывать иконку
                            ));
                            
                            RadiationMod.LOGGER.info("Игроку {} добавлен эффект радиации уровня {} (фильтр сломан или отсутствует)",
                                player.getName().getString(), amplifier + 1);
                        }
                    } else if (radiationLevel > 0) {
                        // Нет костюма, добавляем эффект радиации сразу
                        // Урезаем значение до максимально допустимого (0-4 для уровней 1-5)
                        int amplifier = Math.min(radiationLevel - 1, 4);
                        
                        // Добавляем эффект радиации с уровнем, соответствующим зоне
                        player.addEffect(new MobEffectInstance(
                            ModEffects.RADIATION.get(),
                            // Продолжительность - чем выше уровень, тем дольше эффект
                            (6 - amplifier) * 200, // от 1000 до 200 тиков
                            amplifier, // Амплификатор (0-4)
                            false, // Ambient
                            true,  // Показывать частицы
                            true   // Показывать иконку
                        ));
                        
                        RadiationMod.LOGGER.info("Игроку {} добавлен эффект радиации уровня {} (нет защитного костюма)",
                            player.getName().getString(), amplifier + 1);
                    }
                }
            }
        }
    }
    
    /**
     * Проверяет, есть ли у игрока полный комплект защитного костюма hazmat
     */
    public static boolean hasFullHazmatSuit(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        
        // Проверяем каждую часть отдельно для логирования
        boolean hasHelmet = isHelmet(helmet);
        boolean hasChestplate = isChestplate(chestplate);
        boolean hasLeggings = isLeggings(leggings);
        boolean hasBoots = isBoots(boots);
        
        // Логируем результаты проверок для отладки
        RadiationMod.LOGGER.debug("Проверка хазмат костюма для игрока {}:", player.getName().getString());
        RadiationMod.LOGGER.debug("Шлем: {}", hasHelmet);
        RadiationMod.LOGGER.debug("Нагрудник: {}", hasChestplate);
        RadiationMod.LOGGER.debug("Поножи: {}", hasLeggings);
        RadiationMod.LOGGER.debug("Ботинки: {}", hasBoots);
        
        // Проверяем, что все части хазмат костюма надеты
        boolean hasHazmat = hasHelmet && hasChestplate && hasLeggings && hasBoots;
        
        RadiationMod.LOGGER.debug("Игрок {} {} полный комплект хазмат костюма", 
                player.getName().getString(), hasHazmat ? "имеет" : "НЕ имеет");
        
        return hasHazmat;
    }
    
    /**
     * Проверяет, является ли предмет шлемом Hazmat
     */
    private static boolean isHelmet(ItemStack stack) {
        return isItemFromRegistryArray(stack, HAZMAT_HELMET_VARIANTS);
    }
    
    /**
     * Проверяет, является ли предмет нагрудником Hazmat
     */
    private static boolean isChestplate(ItemStack stack) {
        return isItemFromRegistryArray(stack, HAZMAT_CHESTPLATE_VARIANTS);
    }
    
    /**
     * Проверяет, является ли предмет поножами Hazmat
     */
    private static boolean isLeggings(ItemStack stack) {
        return isItemFromRegistryArray(stack, HAZMAT_LEGGINGS_VARIANTS);
    }
    
    /**
     * Проверяет, является ли предмет ботинками Hazmat
     */
    private static boolean isBoots(ItemStack stack) {
        return isItemFromRegistryArray(stack, HAZMAT_BOOTS_VARIANTS);
    }
    
    /**
     * Проверяет, соответствует ли ItemStack одному из указанных ID предметов в реестре
     */
    private static boolean isItemFromRegistryArray(ItemStack stack, String[] registryNames) {
        if (stack.isEmpty()) return false;
        
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return false;
        
        String itemIdStr = itemId.toString();
        
        RadiationMod.LOGGER.debug("Проверка предмета: " + itemIdStr);
        
        // Прямая проверка на точное совпадение
        for (String registryName : registryNames) {
            if (itemIdStr.equals(registryName)) {
                RadiationMod.LOGGER.debug("Найдено точное совпадение с " + registryName);
                return true;
            }
        }
        
        // Проверка на частичное совпадение, если точного нет
        for (String registryName : registryNames) {
            // Преобразуем имена для сравнения
            String simplifiedItemId = itemIdStr.replace(":", "_").toLowerCase();
            String simplifiedRegistryName = registryName.replace(":", "_").toLowerCase();
            
            // Если оба содержат hazmat и armor, вероятно это то, что нам нужно
            if (simplifiedItemId.contains("hazmat") && simplifiedItemId.contains("armor") &&
                simplifiedRegistryName.contains("hazmat") && simplifiedRegistryName.contains("armor")) {
                
                RadiationMod.LOGGER.debug("Найдено частичное совпадение с " + registryName);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Проверяет, соответствует ли ItemStack указанному ID предмета в реестре
     */
    private static boolean isItemFromRegistry(ItemStack stack, String registryName) {
        if (stack.isEmpty()) return false;
        
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return itemId != null && itemId.toString().equals(registryName);
    }
    
    /**
     * Проверяет, есть ли у игрока работающий радиационный фильтр
     * @param player игрок
     * @return true, если у игрока есть рабочий фильтр, false иначе
     */
    public static boolean hasWorkingFilter(Player player) {
        // Проверяем наличие фильтра в руках
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        
        if (mainHand.getItem() instanceof RadiationFilterItem && RadiationFilterItem.isFilterWorking(mainHand)) {
            return true;
        }
        
        if (offHand.getItem() instanceof RadiationFilterItem && RadiationFilterItem.isFilterWorking(offHand)) {
            return true;
        }
        
        // Проверяем наличие фильтра в слоте Curios
        AtomicBoolean hasWorkingFilter = new AtomicBoolean(false);
        
        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
            handler.getStacksHandler(CuriosIntegration.RADIATION_FILTER_SLOT).ifPresent(stacksHandler -> {
                for(int i = 0; i < stacksHandler.getSlots(); i++) {
                    ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                    if(stack.getItem() instanceof RadiationFilterItem && RadiationFilterItem.isFilterWorking(stack)) {
                        hasWorkingFilter.set(true);
                        break;
                    }
                }
            });
        });
        
        return hasWorkingFilter.get();
    }
    
    /**
     * Наносит урон радиационному фильтру игрока с учетом уровня радиации
     * @return true если фильтр сломался
     */
    public static boolean damageRadiationFilter(Player player) {
        AtomicBoolean filterBroken = new AtomicBoolean(false);
        
        // Определяем уровень радиации и множитель расхода фильтра в зоне, где находится игрок
        float damageMultiplier = 1.0f;
        BlockPos playerPos = player.blockPosition();
        for (RadiationZone zone : RadiationMod.RADIATION_MANAGER.getRadiationZones()) {
            if (zone.isInside(player.position())) {
                // Используем готовый метод для получения множителя расхода фильтра
                damageMultiplier = zone.getFilterDrainMultiplier();
                RadiationMod.LOGGER.debug("Фильтр игрока {} расходуется с множителем {} (уровень радиации {})", 
                        player.getName().getString(), damageMultiplier, zone.getLevel());
                break;
            }
        }
        
        // Рассчитываем урон фильтру - используем float для более плавного износа
        // Конвертируем в int только в самом конце
        float baseDamage = FILTER_DAMAGE_PER_TICK; // Базовый урон как float
        float totalDamage = baseDamage * damageMultiplier;
        
        // Проверяем, есть ли фильтр в руках
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        
        ItemStack filterStack = null;
        if (mainHand.getItem() instanceof RadiationFilterItem && RadiationFilterItem.isFilterWorking(mainHand)) {
            filterStack = mainHand;
        } else if (offHand.getItem() instanceof RadiationFilterItem && RadiationFilterItem.isFilterWorking(offHand)) {
            filterStack = offHand;
        }
        
        // Если фильтр не найден в руках, проверяем в слоте Curios
        if (filterStack == null) {
            AtomicReference<ItemStack> curiosFilterStack = new AtomicReference<>(null);
            
            // Проверяем наличие фильтра в слоте Curios
            CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
                handler.getStacksHandler(CuriosIntegration.RADIATION_FILTER_SLOT).ifPresent(stacksHandler -> {
                    for(int i = 0; i < stacksHandler.getSlots(); i++) {
                        ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                        if(stack.getItem() instanceof RadiationFilterItem && RadiationFilterItem.isFilterWorking(stack)) {
                            curiosFilterStack.set(stack);
                            break;
                        }
                    }
                });
            });
            
            filterStack = curiosFilterStack.get();
        }
        
        if (filterStack != null) {
            // Получаем текущий урон фильтра
            int currentDamage = RadiationFilterItem.getFilterDamage(filterStack);
            
            // Добавляем новый урон с плавным изменением
            float newDamageFloat = currentDamage + totalDamage;
            int newDamage = Math.round(newDamageFloat);
            
            // Устанавливаем новый урон
            RadiationFilterItem.setFilterDamage(filterStack, newDamage);
            
            // Проверяем, сломался ли фильтр
            if (newDamage >= RadiationFilterItem.MAX_DURABILITY) {
                filterBroken.set(true);
                
                // Уведомляем игрока о поломке фильтра
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(
                        Component.translatable("message.radiationmod.filter_broken")
                            .withStyle(ChatFormatting.RED), 
                        true
                    );
                }
                
                // Когда фильтр сломался, применяем эффект радиации
                // Определяем уровень радиации в зоне
                int radiationLevel = 0;
                for (RadiationZone zone : RadiationMod.RADIATION_MANAGER.getRadiationZones()) {
                    if (zone.isInside(player.position())) {
                        radiationLevel = zone.getLevel();
                        break;
                    }
                }
                
                // Если игрок в радиационной зоне, добавляем эффект радиации
                if (radiationLevel > 0) {
                    // Урезаем значение до максимально допустимого (0-4 для уровней 1-5)
                    radiationLevel = Math.min(radiationLevel - 1, 4);
                    
                    // Добавляем эффект радиации с уровнем, соответствующим зоне
                    player.addEffect(new MobEffectInstance(
                        ModEffects.RADIATION.get(),
                        // Продолжительность - чем выше уровень, тем дольше эффект
                        (6 - radiationLevel) * 200, // от 1000 до 200 тиков
                        radiationLevel, // Амплификатор (0-4)
                        false, // Ambient
                        true,  // Показывать частицы
                        true   // Показывать иконку
                    ));
                    
                    RadiationMod.LOGGER.info("Игроку {} добавлен эффект радиации уровня {} после поломки фильтра",
                        player.getName().getString(), radiationLevel + 1);
                }
            }
        }
        
        return filterBroken.get();
    }
    
    /**
     * Обработчик события истечения эффекта радиации
     */
    @SubscribeEvent
    public static void onRadiationEffectExpire(MobEffectEvent.Expired event) {
        // Логирование для отладки
        RadiationMod.LOGGER.info(">>> СОБЫТИЕ ИСТЕЧЕНИЯ ЭФФЕКТА: {} - {}", 
                event.getEntity().getName().getString(),
                event.getEffectInstance().getEffect().getDescriptionId());
        
        // Убедимся, что это эффект радиации
        if (event.getEffectInstance().getEffect() != ModEffects.RADIATION.get()) {
            return;
        }
        
        // Только для игроков
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        // Получаем уровень эффекта радиации, который только что истек
        int currentLevel = event.getEffectInstance().getAmplifier() + 1; // Уровень от 1 до 5 или 255
        
        RadiationMod.LOGGER.info(">>> ЭФФЕКТ РАДИАЦИИ УРОВНЯ {} ИСТЕК у игрока {}", 
                currentLevel, player.getName().getString());
        
        // Проверяем, находится ли игрок в зоне радиации
        boolean inRadiationZone = false;
        int zoneLevel = 0;
        
        for (RadiationZone zone : RadiationMod.RADIATION_MANAGER.getRadiationZones()) {
            if (zone.isInside(player.position())) {
                inRadiationZone = true;
                zoneLevel = zone.getLevel();
                break;
            }
        }
        
        // Если игрок все еще в зоне радиации, эскалируем уровень
        if (inRadiationZone) {
            if (currentLevel < 5) {
                // Для уровней 1-4, повышаем на 1
                int nextLevel = currentLevel + 1;
                int nextAmplifier = nextLevel - 1;
                
                RadiationMod.LOGGER.info(">>> ЭСКАЛАЦИЯ ПРИ ИСТЕЧЕНИИ: Повышение уровня радиации с {} до {}", 
                        currentLevel, nextLevel);
                
                // Добавляем эффект следующего уровня
                player.addEffect(new MobEffectInstance(
                        ModEffects.RADIATION.get(),
                        1200, // 1 минута (1200 тиков)
                        nextAmplifier,
                        false,
                        true,
                        true
                ));
                
                RadiationMod.LOGGER.info(">>> ЭСКАЛАЦИЯ ПРИ ИСТЕЧЕНИИ ЗАВЕРШЕНА: Уровень радиации игрока {} повышен с {} до {}",
                        player.getName().getString(), currentLevel, nextLevel);
            } else if (currentLevel == 5) {
                // Если был 5 уровень, повышаем до критического (255)
                RadiationMod.LOGGER.info(">>> КРИТИЧЕСКАЯ ЭСКАЛАЦИЯ ПРИ ИСТЕЧЕНИИ: Переход с уровня 5 на 255");
                
                player.addEffect(new MobEffectInstance(
                        ModEffects.RADIATION.get(),
                        600, // 30 секунд
                        254, // Уровень 255
                        false,
                        true,
                        true
                ));
                
                RadiationMod.LOGGER.info(">>> КРИТИЧЕСКАЯ ЭСКАЛАЦИЯ ПРИ ИСТЕЧЕНИИ ЗАВЕРШЕНА: Игрок {} получил СМЕРТЕЛЬНУЮ дозу радиации (уровень 255)!", 
                        player.getName().getString());
            }
        } else {
            RadiationMod.LOGGER.info(">>> Игрок {} больше не находится в зоне радиации, эффект не продлен", 
                    player.getName().getString());
        }
    }
    
    /**
     * Обработчик события смерти для вывода кастомного сообщения
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        // Убедимся, что это игрок
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        // Проверяем, был ли активен эффект радиации при смерти
        if (player.hasEffect(ModEffects.RADIATION.get())) {
            // Если игрок погиб от магии (которую мы используем для эффекта радиации)
            if (event.getSource().getMsgId().equals("magic")) {
                // Логируем для отладки
                RadiationMod.LOGGER.info(">>> Игрок {} умер от радиации", 
                        player.getName().getString());
                
                // Блокируем стандартное сообщение о смерти
                event.setCanceled(true);
                
                // Показываем наше собственное сообщение о смерти
                Component deathMessage = Component.translatable(
                    "death.attack.radiationmod.radiation", 
                    player.getDisplayName()
                );
                
                // Отправляем сообщение всем игрокам вручную
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.server.getPlayerList().broadcastSystemMessage(
                        deathMessage, false);
                }
                
                // Убиваем игрока снова с другим типом урона
                player.kill();
            }
        }
    }
} 