package net.radiation.tutorialmod.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.radiation.tutorialmod.RadiationMod;

/**
 * Класс, представляющий зону радиации
 */
public class RadiationZone {
    private final BlockPos pos;
    private final int level; // Уровень радиации (от 1 до 5)
    private final int radius; // Радиус зоны в блоках
    
    /**
     * Создает новую зону радиации
     * @param pos Позиция центра зоны
     * @param level Уровень радиации (от 1 до 5)
     */
    public RadiationZone(BlockPos pos, int level) {
        this.pos = pos;
        this.level = Math.max(1, Math.min(5, level)); // Ограничиваем уровень от 1 до 5
        this.radius = 10; // Фиксированный радиус зоны
    }
    
    /**
     * Создает зону радиации из NBT
     * @param tag NBT тег
     */
    public RadiationZone(CompoundTag tag) {
        this.pos = BlockPos.of(tag.getLong("Pos"));
        this.level = tag.getInt("Level");
        this.radius = tag.getInt("Radius");
    }
    
    /**
     * Сохраняет зону радиации в NBT
     * @param tag NBT тег
     */
    public void save(CompoundTag tag) {
        tag.putLong("Pos", pos.asLong());
        tag.putInt("Level", level);
        tag.putInt("Radius", radius);
    }
    
    /**
     * Проверяет, находится ли позиция в зоне радиации
     * @param pos Позиция для проверки
     * @return true, если позиция в зоне
     */
    public boolean isInZone(BlockPos pos) {
        return this.pos.distSqr(pos) <= radius * radius;
    }
    
    /**
     * Проверяет, находится ли позиция в зоне радиации (для совместимости)
     * @param vec Позиция для проверки
     * @return true, если позиция в зоне
     */
    public boolean isInside(net.minecraft.world.phys.Vec3 vec) {
        BlockPos pos = new BlockPos((int)vec.x, (int)vec.y, (int)vec.z);
        return isInZone(pos);
    }
    
    /**
     * Получает уровень радиации в зоне
     * @return Уровень радиации (от 1 до 5)
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Получает множитель расхода фильтра в зависимости от уровня радиации
     * @return Множитель расхода фильтра
     */
    public float getFilterDrainMultiplier() {
        // Чем выше уровень радиации, тем быстрее расходуется фильтр
        return 1.0f + (level - 1) * 0.5f;
    }
    
    /**
     * Получает позицию центра зоны
     * @return Позиция центра зоны
     */
    public BlockPos getPos() {
        return pos;
    }
    
    /**
     * Получает радиус зоны
     * @return Радиус зоны в блоках
     */
    public int getRadius() {
        return radius;
    }
    
    @Override
    public String toString() {
        return "RadiationZone{pos=" + pos + ", level=" + level + ", radius=" + radius + "}";
    }
} 