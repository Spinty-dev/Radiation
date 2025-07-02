package net.radiation.tutorialmod.radiation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.radiation.tutorialmod.RadiationMod;
import net.radiation.tutorialmod.effect.ModEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.server.ServerStoppingEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = RadiationMod.MOD_ID)
public class RadiationManager extends SavedData {
    private static final String DATA_NAME = RadiationMod.MOD_ID + "_radiation_zones";
    
    // Флаг, показывающий, что данные уже были загружены
    private static boolean zonesAlreadyLoaded = false;
    
    // Хранение установленных позиций для каждого игрока
    private final Map<UUID, BlockPos> pos1Map = new ConcurrentHashMap<>();
    private final Map<UUID, BlockPos> pos2Map = new ConcurrentHashMap<>();
    
    // Список активных зон радиации
    private final List<RadiationZone> radiationZones = new ArrayList<>();
    
    // Игроки, которые уже получили эффект в текущем тике
    private final Set<UUID> affectedPlayers = new HashSet<>();
    
    // Карта для отслеживания игроков вне зоны радиации
    private static final Map<UUID, Integer> playersOutsideRadiation = new HashMap<>();
    
    // Дефолтный уровень радиации для новых зон
    private int defaultRadiationLevel = 1;
    
    // Обновляем счетчик, используем более частые проверки
    private static int updateCounter = 0;
    // Проверка КАЖДЫЙ тик, чтобы не упустить момент обновления эффекта
    private static final int UPDATE_FREQUENCY = 1; 
    
    // Код для отслеживания эскалации эффектов радиации
    private static final Map<UUID, Integer> radiationLevels = new ConcurrentHashMap<>();
    
    public RadiationManager() {
        super();
    }
    
    /**
     * Сбрасывает флаг загруженности зон
     * Должен вызываться при остановке сервера
     */
    public static void resetLoadedFlag() {
        zonesAlreadyLoaded = false;
        RadiationMod.LOGGER.info("Сброшен флаг загрузки зон радиации");
    }
    
    /**
     * Инициализация менеджера
     */
    public void init() {
        RadiationMod.LOGGER.info("Инициализация менеджера радиации");
    }
    
    /**
     * Регистрация команд
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        registerRadiationCommand(event.getDispatcher());
    }
    
    /**
     * Регистрация команды для тестирования радиации
     */
    private static void registerRadiationCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("radiation")
                .requires(source -> source.hasPermission(2)) // Требует права оператора (уровень 2)
                .then(Commands.literal("set")
                    .then(Commands.argument("level", IntegerArgumentType.integer(1, 5))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            int level = IntegerArgumentType.getInteger(context, "level");
                            
                            // Применяем эффект радиации с указанным уровнем
                            player.addEffect(new MobEffectInstance(
                                    ModEffects.RADIATION.get(), 
                                    1200, // 1 минута
                                    level - 1, // Амплификатор на 1 меньше уровня
                                    false, 
                                    true, 
                                    true 
                            ));
                            
                            context.getSource().sendSuccess(() -> 
                                    Component.literal("Установлен уровень радиации " + level + " для игрока " 
                                            + player.getName().getString()), true);
                            
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("kill")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        
                        // Применяем максимальный уровень радиации (5)
                        player.addEffect(new MobEffectInstance(
                                ModEffects.RADIATION.get(), 
                                1200, // 1 минута (1200 тиков)
                                4, // Уровень 5 (амплификатор 4)
                                false, 
                                true, 
                                true 
                        ));
                        
                        context.getSource().sendSuccess(() -> 
                                Component.literal("Применена смертельная доза радиации (уровень 5) для игрока " 
                                        + player.getName().getString()), true);
                        
                        return 1;
                    })
                )
                .then(Commands.literal("clear")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        
                        // Удаляем эффект радиации
                        player.removeEffect(ModEffects.RADIATION.get());
                        
                        context.getSource().sendSuccess(() -> 
                                Component.literal("Удалена радиация для игрока " 
                                        + player.getName().getString()), true);
                        
                        return 1;
                    })
                )
        );
    }
    
    /**
     * Загрузка данных при старте сервера
     */
    public void load(MinecraftServer server) {
        // Если данные уже были загружены, не загружаем их повторно
        if (zonesAlreadyLoaded) {
            RadiationMod.LOGGER.info("Зоны радиации уже загружены, пропускаем повторную загрузку");
            return;
        }
        
        boolean zonesLoaded = false;
        RadiationMod.LOGGER.info("Загружаем зоны радиации...");
        
        for (ServerLevel level : server.getAllLevels()) {
            RadiationManagerData data = level.getDataStorage().computeIfAbsent(
                    RadiationManagerData::load, 
                    RadiationManagerData::new, 
                    DATA_NAME);
            
            if (data != null && data.radiationZones != null && !data.radiationZones.isEmpty()) {
                // Очищаем существующие зоны перед добавлением новых
                // Делаем это только один раз при первом загруженном мире с данными
                if (!zonesLoaded) {
                    this.radiationZones.clear();
                    zonesLoaded = true;
                }
                
                this.radiationZones.addAll(data.radiationZones);
                
                // Также загружаем дефолтный уровень радиации
                this.defaultRadiationLevel = data.defaultRadiationLevel;
                
                RadiationMod.LOGGER.info("Загружено {} зон радиации из измерения {}", 
                        data.radiationZones.size(), level.dimension().location());
            }
            
            // Инициализируем трекер радиации
            RadiationTracker.get(level);
        }
        
        RadiationMod.LOGGER.info("Всего загружено {} зон радиации", this.radiationZones.size());
        
        // Если данные успешно загружены, устанавливаем флаг
        zonesAlreadyLoaded = true;
    }
    
    /**
     * Сохранение данных
     */
    @Override
    public CompoundTag save(CompoundTag compound) {
        RadiationManagerData data = new RadiationManagerData(this);
        return data.save(compound);
    }
    
    /**
     * Установка первой позиции
     */
    public void setPos1(UUID playerId, BlockPos pos) {
        pos1Map.put(playerId, pos);
    }
    
    /**
     * Установка второй позиции
     */
    public void setPos2(UUID playerId, BlockPos pos) {
        pos2Map.put(playerId, pos);
    }
    
    /**
     * Проверка, можно ли создать зону
     */
    public boolean canCreateZone(UUID playerId) {
        return pos1Map.containsKey(playerId) && pos2Map.containsKey(playerId);
    }
    
    /**
     * Создание зоны радиации
     */
    public void createRadiationZone(UUID playerId, int level) {
        if (!canCreateZone(playerId)) {
            return;
        }
        
        BlockPos pos1 = pos1Map.get(playerId);
        BlockPos pos2 = pos2Map.get(playerId);
        
        RadiationZone zone = new RadiationZone(pos1, pos2, level);
        radiationZones.add(zone);
        
        // Очищаем временные позиции
        pos1Map.remove(playerId);
        pos2Map.remove(playerId);
        
        // Сохраняем изменения во всех мирах
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            saveToAllLevels(server);
        }
        
        RadiationMod.LOGGER.info("Создана новая зона радиации: {}", zone);
    }
    
    /**
     * Добавляет зону радиации с заданным уровнем между двумя позициями
     * @param pos1 Первая позиция для зоны
     * @param pos2 Вторая позиция для зоны
     * @param level Уровень радиации (от 1 до 5)
     */
    public void addRadiationZone(BlockPos pos1, BlockPos pos2, int level) {
        // Ограничиваем уровень радиации от 1 до 5
        level = Math.max(1, Math.min(5, level));
        
        // Создаем зону радиации с указанным уровнем
        RadiationZone zone = new RadiationZone(pos1, pos2, level);
        
        // Проверяем, не пересекается ли с существующими зонами
        for (int i = 0; i < radiationZones.size(); i++) {
            RadiationZone existingZone = radiationZones.get(i);
            if (existingZone.overlaps(zone)) {
                // Заменяем существующую зону
                radiationZones.set(i, zone);
                
                // Сохраняем зоны во всех мирах
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    saveToAllLevels(server);
                }
                
                // Логируем обновление зоны
                RadiationMod.LOGGER.info("Обновлена зона радиации уровня " + level + " между позициями " + pos1 + " и " + pos2);
                return;
            }
        }
        
        // Добавляем новую зону
        radiationZones.add(zone);
        
        // Сохраняем зоны во всех мирах
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            saveToAllLevels(server);
        }
        
        // Логируем создание зоны
        RadiationMod.LOGGER.info("Создана зона радиации уровня " + level + " между позициями " + pos1 + " и " + pos2);
    }
    
    /**
     * Добавляет зону радиации с заданным уровнем вокруг позиции
     * @param pos Позиция центра зоны радиации
     * @param level Уровень радиации (от 1 до 5)
     */
    public void addRadiationZone(BlockPos pos, int level) {
        // Создаем зону с фиксированным радиусом 5 блоков вокруг позиции
        BlockPos pos2 = pos.offset(5, 5, 5);
        addRadiationZone(pos.offset(-5, -5, -5), pos2, level);
    }
    
    /**
     * Проверка на каждом тике сервера
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        
        // Увеличиваем счетчик обновлений
        updateCounter++;
        
        // Проверяем игроков каждые 10 тиков (0.5 секунды) - чаще для более надежного отслеживания
        if (updateCounter >= 10) {
            // Очищаем список игроков, получивших эффект
            RadiationMod.RADIATION_MANAGER.affectedPlayers.clear();
            
            // Получаем трекер радиации
            RadiationTracker tracker = RadiationTracker.get(server.overworld());
            
            // Проверяем всех игроков
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                UUID playerId = player.getUUID();
                
                // Проверяем текущий уровень радиации, если есть
                MobEffectInstance effect = player.getEffect(ModEffects.RADIATION.get());
                int currentLevel = effect != null ? effect.getAmplifier() + 1 : 0;
                
                if (currentLevel > 0) {
                    RadiationMod.LOGGER.info(">>> Игрок {} имеет уровень радиации {}, оставшаяся длительность: {} тиков", 
                            player.getName().getString(), currentLevel, effect.getDuration());
                    
                    // Запоминаем текущий уровень радиации
                    radiationLevels.put(playerId, currentLevel);
                    
                    // Если радиация скоро закончится, обновляем уровень СЕЙЧАС
                    if (effect.getDuration() <= 40 && !RadiationMod.RADIATION_MANAGER.affectedPlayers.contains(playerId)) {
                        RadiationMod.LOGGER.info(">>> СРОЧНАЯ ПРОВЕРКА: Радиация скоро закончится у игрока {}, уровень: {}, осталось: {} тиков", 
                                player.getName().getString(), currentLevel, effect.getDuration());
                        
                        // Обновляем или эскалируем эффект радиации
                        escalateRadiationEffect(player, currentLevel);
                        
                        // Отмечаем, что игрок уже получил эффект
                        RadiationMod.RADIATION_MANAGER.affectedPlayers.add(playerId);
                    }
                } else {
                    // Удаляем из отслеживания, если эффект исчез
                    radiationLevels.remove(playerId);
                }
                
                // Проверяем, находится ли игрок в зоне радиации
                boolean inRadiationZone = RadiationMod.RADIATION_MANAGER.checkPlayerRadiation(player, tracker);
                
                // Обновляем счетчик нахождения вне зоны радиации
                if (!inRadiationZone) {
                    if (!playersOutsideRadiation.containsKey(playerId)) {
                        // Начинаем отсчет - игрок только что вышел из зоны
                        playersOutsideRadiation.put(playerId, 1);
                        RadiationMod.LOGGER.info(">>> Игрок {} вышел из зоны радиации", 
                                player.getName().getString());
                    }
                } else {
                    // Сбрасываем счетчик, если игрок в зоне радиации
                    playersOutsideRadiation.remove(playerId);
                    RadiationMod.LOGGER.info(">>> Игрок {} находится в зоне радиации", 
                            player.getName().getString());
                }
            }
            
            // Сбрасываем счетчик обновлений
            updateCounter = 0;
        }
    }
    
    /**
     * Метод для эскалации эффекта радиации
     * @param player Игрок
     * @param currentLevel Текущий уровень радиации
     */
    private static void escalateRadiationEffect(ServerPlayer player, int currentLevel) {
        if (currentLevel < 5) {
            // Для уровней 1-4, повышаем на 1
            int nextLevel = currentLevel + 1;
            int nextAmplifier = nextLevel - 1;
            
            RadiationMod.LOGGER.info(">>> ЭСКАЛАЦИЯ: Повышение уровня радиации с {} до {}", 
                    currentLevel, nextLevel);
            
            // Удаляем старый эффект и сразу же добавляем новый, чтобы не было перерыва
            player.removeEffect(ModEffects.RADIATION.get());
            
            // Добавляем эффект следующего уровня с полной продолжительностью
            player.addEffect(new MobEffectInstance(
                    ModEffects.RADIATION.get(),
                    1200, // 1 минута (1200 тиков)
                    nextAmplifier,
                    false,
                    true,
                    true
            ));
            
            RadiationMod.LOGGER.info(">>> ЭСКАЛАЦИЯ ЗАВЕРШЕНА: Уровень радиации игрока {} повышен с {} до {}",
                    player.getName().getString(), currentLevel, nextLevel);
        } else if (currentLevel == 5) {
            // Если был 5 уровень, повышаем до критического (255)
            RadiationMod.LOGGER.info(">>> КРИТИЧЕСКАЯ ЭСКАЛАЦИЯ: Переход с уровня 5 на 255");
            
            // Удаляем старый эффект и сразу добавляем новый
            player.removeEffect(ModEffects.RADIATION.get());
            
            player.addEffect(new MobEffectInstance(
                    ModEffects.RADIATION.get(),
                    1200, // 1 минута (1200 тиков)
                    254, // Уровень 255
                    false,
                    true,
                    true
            ));
            
            RadiationMod.LOGGER.info(">>> КРИТИЧЕСКАЯ ЭСКАЛАЦИЯ ЗАВЕРШЕНА: Игрок {} получил СМЕРТЕЛЬНУЮ дозу радиации (уровень 255)!", 
                    player.getName().getString());
        }
    }
    
    /**
     * Проверка игрока на нахождение в зоне радиации
     * @return true если игрок в зоне радиации
     */
    private boolean checkPlayerRadiation(ServerPlayer player, RadiationTracker tracker) {
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }
        
        UUID playerId = player.getUUID();
        if (affectedPlayers.contains(playerId)) {
            return false;
        }
        
        RadiationMod.LOGGER.info(">>> Проверка радиации для игрока {}", player.getName().getString());
        
        for (RadiationZone zone : radiationZones) {
            RadiationMod.LOGGER.info(">>> Проверка зоны: {}", zone);
            
            if (zone.isInside(player.position())) {
                RadiationMod.LOGGER.info(">>> Игрок {} находится в зоне радиации: {}", 
                        player.getName().getString(), zone);
                
                // Получаем уровень радиации из зоны
                int zoneLevel = zone.getLevel();
                int zoneAmplifier = zoneLevel - 1; // Амплификатор на 1 меньше уровня
                
                // Проверяем, есть ли уже эффект радиации
                MobEffectInstance currentEffect = player.getEffect(ModEffects.RADIATION.get());
                
                // Если эффекта нет
                if (currentEffect == null) {
                    // Запоминаем последний известный уровень радиации игрока
                    Integer lastKnownLevel = radiationLevels.get(playerId);
                    
                    if (lastKnownLevel != null && lastKnownLevel > 0) {
                        // Если игрок ранее подвергался радиации, используем эскалацию
                        int newLevel = Math.max(zoneLevel, lastKnownLevel);
                        
                        RadiationMod.LOGGER.info(">>> Восстановление радиации для игрока {} с уровнем {}", 
                                player.getName().getString(), newLevel);
                        
                        // ФИКС: всегда устанавливаем продолжительность 1200 тиков (1 минута)
                        player.addEffect(new MobEffectInstance(
                                ModEffects.RADIATION.get(),
                                1200, // 1 минута (1200 тиков)
                                newLevel < 5 ? newLevel - 1 : (newLevel == 5 ? 4 : 254), // Правильный амплификатор
                                false,
                                true,
                                true
                        ));
                    } else {
                        // Если игрок не подвергался радиации ранее, применяем радиацию с уровнем зоны
                        player.addEffect(new MobEffectInstance(
                                ModEffects.RADIATION.get(),
                                1200, // 1 минута (1200 тиков)
                                zoneAmplifier, // Уровень из зоны
                                false,
                                true,
                                true
                        ));
                        
                        RadiationMod.LOGGER.info(">>> Игроку {} добавлен новый эффект радиации уровня {}",
                                player.getName().getString(), zoneLevel);
                    }
                } else {
                    // Игрок уже под воздействием радиации
                    int currentLevel = currentEffect.getAmplifier() + 1; // текущий уровень от 1 до 5 или 255
                    
                    // ФИКС: обновляем эффект если уровень зоны выше ИЛИ если осталось мало времени
                    if (zoneLevel > currentLevel) {
                        player.removeEffect(ModEffects.RADIATION.get());
                        player.addEffect(new MobEffectInstance(
                                ModEffects.RADIATION.get(),
                                1200, // 1 минута (1200 тиков)
                                zoneAmplifier,
                                false,
                                true,
                                true
                        ));
                        RadiationMod.LOGGER.info(">>> Уровень радиации игрока {} обновлен до {} из-за нахождения в зоне более высокого уровня",
                                player.getName().getString(), zoneLevel);
                    } else if (currentEffect.getDuration() <= 200) { // Обновляем если меньше 10 секунд осталось
                        // ВАЖНЫЙ ФИКС: Сохраняем текущий уровень радиации, но обновляем длительность
                        player.removeEffect(ModEffects.RADIATION.get());
                        player.addEffect(new MobEffectInstance(
                                ModEffects.RADIATION.get(),
                                1200, // 1 минута (1200 тиков)
                                currentEffect.getAmplifier(), // Сохраняем текущий усилитель
                                false,
                                true,
                                true
                        ));
                        RadiationMod.LOGGER.info(">>> Длительность радиации игрока {} обновлена до 1 минуты. Текущий уровень: {}",
                                player.getName().getString(), currentLevel);
                    } else if (currentEffect.getDuration() <= 40) { // Если осталось меньше 2 секунд
                        // Эскалируем эффект радиации
                        escalateRadiationEffect(player, currentLevel);
                    } else {
                        RadiationMod.LOGGER.info(">>> Игрок {} уже под воздействием радиации уровня {}, прогресс не сбрасывается",
                                player.getName().getString(), currentLevel);
                    }
                }
                
                affectedPlayers.add(playerId);
                return true;
            }
        }
        
        if (radiationZones.isEmpty()) {
            RadiationMod.LOGGER.info(">>> Нет зон радиации для проверки!");
        } else {
            RadiationMod.LOGGER.info(">>> Игрок {} не находится ни в одной зоне радиации", 
                    player.getName().getString());
        }
        
        return false;
    }
    
    /**
     * Обработчик событий отключения игрока
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Удаляем отслеживание отключившегося игрока
            UUID playerId = player.getUUID();
            playersOutsideRadiation.remove(playerId);
            RadiationMod.RADIATION_MANAGER.affectedPlayers.remove(playerId);
            
            // Сохраняем текущее состояние в трекере
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                RadiationTracker tracker = RadiationTracker.get(server.overworld());
                tracker.setDirty(); // Явно помечаем как "грязные" данные для сохранения
                
                RadiationMod.LOGGER.info(">>> Сохраняем состояние радиации при выходе игрока {}", 
                        player.getName().getString());
            }
        }
    }
    
    /**
     * Получает дефолтный уровень радиации для новых зон
     * @return Уровень радиации (от 1 до 5)
     */
    public int getDefaultRadiationLevel() {
        return defaultRadiationLevel;
    }
    
    /**
     * Устанавливает дефолтный уровень радиации для новых зон
     * @param level Уровень радиации (от 1 до 5)
     */
    public void setDefaultRadiationLevel(int level) {
        this.defaultRadiationLevel = Math.max(1, Math.min(5, level));
        RadiationMod.LOGGER.info("Установлен дефолтный уровень радиации: {}", this.defaultRadiationLevel);
    }
    
    /**
     * Класс для сохранения данных о зонах радиации
     */
    public static class RadiationManagerData extends SavedData {
        private final List<RadiationZone> radiationZones;
        private int defaultRadiationLevel = 1;
        
        public RadiationManagerData() {
            this.radiationZones = new ArrayList<>();
        }
        
        public RadiationManagerData(RadiationManager manager) {
            this.radiationZones = new ArrayList<>(manager.radiationZones);
            this.defaultRadiationLevel = manager.defaultRadiationLevel;
        }
        
        public static RadiationManagerData load(CompoundTag tag) {
            RadiationManagerData data = new RadiationManagerData();
            
            ListTag zonesList = tag.getList("RadiationZones", Tag.TAG_COMPOUND);
            for (int i = 0; i < zonesList.size(); i++) {
                CompoundTag zoneTag = zonesList.getCompound(i);
                
                int x1 = zoneTag.getInt("X1");
                int y1 = zoneTag.getInt("Y1");
                int z1 = zoneTag.getInt("Z1");
                
                int x2 = zoneTag.getInt("X2");
                int y2 = zoneTag.getInt("Y2");
                int z2 = zoneTag.getInt("Z2");
                
                int level = zoneTag.getInt("Level");
                
                data.radiationZones.add(new RadiationZone(
                        new BlockPos(x1, y1, z1), 
                        new BlockPos(x2, y2, z2), 
                        level
                ));
            }
            
            if (tag.contains("DefaultRadiationLevel")) {
                data.defaultRadiationLevel = tag.getInt("DefaultRadiationLevel");
            }
            
            return data;
        }
        
        @Override
        public CompoundTag save(CompoundTag compound) {
            ListTag zonesList = new ListTag();
            
            for (RadiationZone zone : radiationZones) {
                CompoundTag zoneTag = new CompoundTag();
                
                zoneTag.putInt("X1", zone.getPos1().getX());
                zoneTag.putInt("Y1", zone.getPos1().getY());
                zoneTag.putInt("Z1", zone.getPos1().getZ());
                
                zoneTag.putInt("X2", zone.getPos2().getX());
                zoneTag.putInt("Y2", zone.getPos2().getY());
                zoneTag.putInt("Z2", zone.getPos2().getZ());
                
                zoneTag.putInt("Level", zone.getLevel());
                
                zonesList.add(zoneTag);
            }
            
            compound.put("RadiationZones", zonesList);
            compound.putInt("DefaultRadiationLevel", defaultRadiationLevel);
            
            return compound;
        }
    }
    
    /**
     * Класс, представляющий зону радиации
     */
    public static class RadiationZone implements INBTSerializable<CompoundTag> {
        private final BlockPos pos1;
        private final BlockPos pos2;
        private final int level;
        private final UUID creator;
        
        /**
         * Создает новую зону радиации
         * @param pos1 Первая позиция
         * @param pos2 Вторая позиция
         * @param level Уровень радиации (1-5)
         */
        public RadiationZone(BlockPos pos1, BlockPos pos2, int level) {
            this(pos1, pos2, level, UUID.randomUUID());
        }
        
        /**
         * Создает новую зону радиации
         * @param pos1 Первая позиция
         * @param pos2 Вторая позиция
         * @param level Уровень радиации (1-5)
         * @param creator UUID создателя
         */
        public RadiationZone(BlockPos pos1, BlockPos pos2, int level, UUID creator) {
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.level = Math.max(1, Math.min(5, level)); // Ограничиваем уровень от 1 до 5
            this.creator = creator;
        }
        
        /**
         * Создает зону радиации из NBT
         * @param tag NBT тег
         */
        public RadiationZone(CompoundTag tag) {
            this.pos1 = BlockPos.of(tag.getLong("Pos1"));
            this.pos2 = BlockPos.of(tag.getLong("Pos2"));
            this.level = tag.getInt("Level");
            this.creator = tag.hasUUID("Creator") ? tag.getUUID("Creator") : UUID.randomUUID();
        }
        
        /**
         * Проверяет, находится ли позиция в зоне радиации
         * @param pos Позиция для проверки (в виде Vec3)
         * @return true, если позиция в зоне
         */
        public boolean isInside(Vec3 pos) {
            int x = (int) Math.floor(pos.x);
            int y = (int) Math.floor(pos.y);
            int z = (int) Math.floor(pos.z);
            
            int minX = Math.min(pos1.getX(), pos2.getX());
            int minY = Math.min(pos1.getY(), pos2.getY());
            int minZ = Math.min(pos1.getZ(), pos2.getZ());
            
            int maxX = Math.max(pos1.getX(), pos2.getX());
            int maxY = Math.max(pos1.getY(), pos2.getY());
            int maxZ = Math.max(pos1.getZ(), pos2.getZ());
            
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
        
        /**
         * Сохраняет зону радиации в NBT
         */
        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("Pos1", pos1.asLong());
            tag.putLong("Pos2", pos2.asLong());
            tag.putInt("Level", level);
            tag.putUUID("Creator", creator);
            return tag;
        }
        
        /**
         * Загружает зону радиации из NBT
         */
        @Override
        public void deserializeNBT(CompoundTag tag) {
            // Ничего не делаем, так как поля финальные
        }
        
        /**
         * Получает первую позицию зоны
         */
        public BlockPos getPos1() {
            return pos1;
        }
        
        /**
         * Получает вторую позицию зоны
         */
        public BlockPos getPos2() {
            return pos2;
        }
        
        /**
         * Получает уровень радиации
         */
        public int getLevel() {
            return level;
        }
        
        /**
         * Получает UUID создателя
         */
        public UUID getCreator() {
            return creator;
        }
        
        /**
         * Получает множитель расхода фильтра в зависимости от уровня радиации
         * @return Множитель расхода фильтра
         */
        public float getFilterDrainMultiplier() {
            // Чем выше уровень радиации, тем быстрее расходуется фильтр
            // Уровни 1-5: множители 1.0, 1.5, 2.25, 3.4, 5.0
            switch (level) {
                case 1: return 1.0f;
                case 2: return 1.5f;
                case 3: return 2.25f;
                case 4: return 3.4f;
                case 5: return 5.0f;
                default: return 1.0f;
            }
        }
        
        @Override
        public String toString() {
            return "RadiationZone{pos1=" + pos1 + ", pos2=" + pos2 + ", level=" + level + "}";
        }
        
        /**
         * Проверяет, пересекаются ли две зоны
         * @param otherZone Другая зона для проверки
         * @return true, если зоны пересекаются
         */
        public boolean overlaps(RadiationZone otherZone) {
            return !((pos1.getX() > otherZone.pos2.getX()) || (pos2.getX() < otherZone.pos1.getX()) ||
                     (pos1.getY() > otherZone.pos2.getY()) || (pos2.getY() < otherZone.pos1.getY()) ||
                     (pos1.getZ() > otherZone.pos2.getZ()) || (pos2.getZ() < otherZone.pos1.getZ()));
        }
    }
    
    /**
     * Получает список всех зон радиации
     * @return Список зон радиации
     */
    public List<RadiationZone> getRadiationZones() {
        return Collections.unmodifiableList(radiationZones);
    }
    
    /**
     * Получает зону радиации по позиции
     * @param pos Позиция для проверки
     * @return Зона радиации или null, если зона не найдена
     */
    public RadiationZone getRadiationZoneAt(BlockPos pos) {
        for (RadiationZone zone : radiationZones) {
            if (zone.isInside(new Vec3(pos.getX(), pos.getY(), pos.getZ()))) {
                return zone;
            }
        }
        return null;
    }
    
    /**
     * Получает ID текущего обновления зон
     * @return ID обновления
     */
    public int getZonesUpdateId() {
        return updateCounter;
    }
    
    /**
     * Удаляет указанную зону радиации
     * @param zone Зона радиации для удаления
     */
    public void removeRadiationZone(RadiationZone zone) {
        radiationZones.remove(zone);
        
        // Сохраняем изменения во всех мирах
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            saveToAllLevels(server);
        }
        
        RadiationMod.LOGGER.info("Удалена зона радиации: {}", zone);
    }
    
    /**
     * Обновляет уровень радиации в существующей зоне
     * @param zone Существующая зона радиации
     * @param newLevel Новый уровень радиации (от 1 до 5)
     * @return Обновленная зона радиации
     */
    public RadiationZone updateRadiationZoneLevel(RadiationZone zone, int newLevel) {
        // Ограничиваем уровень радиации от 1 до 5
        newLevel = Math.max(1, Math.min(5, newLevel));
        
        // Если уровень не изменился, просто возвращаем ту же зону
        if (zone.getLevel() == newLevel) {
            return zone;
        }
        
        // Удаляем старую зону
        radiationZones.remove(zone);
        
        // Создаем новую зону с тем же размером, но другим уровнем
        RadiationZone newZone = new RadiationZone(
            zone.getPos1(),
            zone.getPos2(),
            newLevel,
            zone.getCreator()
        );
        
        // Добавляем новую зону
        radiationZones.add(newZone);
        
        // Сохраняем изменения во всех мирах
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            saveToAllLevels(server);
        }
        
        RadiationMod.LOGGER.info("Обновлен уровень радиации в зоне с {} на {}", zone.getLevel(), newLevel);
        
        return newZone;
    }
    
    /**
     * Сохранение данных во всех мирах
     */
    public void saveAllZones() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            // Сохраняем во всех мирах
            saveToAllLevels(server);
            RadiationMod.LOGGER.info("Зоны радиации сохранены во всех мирах");
        }
    }
    
    /**
     * Сохранение данных во всех измерениях
     * @param server Сервер для сохранения
     */
    private void saveToAllLevels(MinecraftServer server) {
        RadiationMod.LOGGER.info("Начинаем сохранение {} зон радиации во всех измерениях...", radiationZones.size());
        
        for (ServerLevel level : server.getAllLevels()) {
            RadiationManagerData data = new RadiationManagerData(this);
            level.getDataStorage().set(DATA_NAME, data);
            
            // Принудительное сохранение данных
            try {
                level.getDataStorage().save();
                
                // Также сохраняем состояние трекера радиации
                RadiationTracker tracker = RadiationTracker.get(level);
                if (tracker != null) {
                    tracker.forceSave(level);
                }
                
                RadiationMod.LOGGER.info("Зоны радиации успешно сохранены в измерении {}", level.dimension().location());
            } catch (Exception e) {
                RadiationMod.LOGGER.error("ОШИБКА при сохранении зон радиации в измерении {}: {}", 
                        level.dimension().location(), e.getMessage());
            }
        }
        
        RadiationMod.LOGGER.info("Сохранение зон радиации завершено");
    }
    
    /**
     * Обработчик события сохранения мира
     */
    @SubscribeEvent
    public static void onWorldSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel) {
            // Сохраняем зоны радиации
            RadiationMod.RADIATION_MANAGER.saveAllZones();
        }
    }
    
    /**
     * Обработчик события загрузки мира
     */
    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            // Загружаем зоны радиации при загрузке мира
            MinecraftServer server = serverLevel.getServer();
            if (server != null) {
                RadiationMod.LOGGER.info("Запускаю загрузку зон радиации при загрузке мира...");
                RadiationMod.RADIATION_MANAGER.load(server);
            }
        }
    }
    
    /**
     * Обработчик события выхода игрока из мира
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // Сохраняем зоны радиации при выходе игрока
        if (event.getEntity().level().isClientSide) return;
        
        RadiationMod.RADIATION_MANAGER.saveAllZones();
    }
    
    /**
     * Обработчик события остановки сервера
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        RadiationMod.LOGGER.info("Сервер останавливается. Сброс флага загрузки зон радиации...");
        resetLoadedFlag();
        
        // Дополнительно сохраняем все зоны
        RadiationMod.RADIATION_MANAGER.saveAllZones();
    }
} 