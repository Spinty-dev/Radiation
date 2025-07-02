package net.radiation.tutorialmod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.radiation.tutorialmod.RadiationMod;
import net.radiation.tutorialmod.effect.ModEffects;
import net.radiation.tutorialmod.radiation.RadiationManager.RadiationZone;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class RadiationFilterItem extends Item {
    public static final int MAX_DURABILITY = 1000;
    public static final int WARN_THRESHOLD = MAX_DURABILITY / 2; // 50% прочности - жёлтое предупреждение
    public static final int DANGER_THRESHOLD = MAX_DURABILITY / 10; // 10% прочности - красное предупреждение

    public RadiationFilterItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level, 
                                @Nonnull List<Component> tooltips, @Nonnull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltips, flag);
        
        // Получаем текущую прочность
        int damage = getFilterDamage(stack);
        int durability = MAX_DURABILITY - damage;
        float percentage = (float) durability / MAX_DURABILITY * 100;
        
        // Добавляем информацию о прочности в подсказку
        ChatFormatting color = getColorForDurability(durability);
        Component durabilityText = Component.translatable("item.radiationmod.radiation_filter.tooltip.durability", 
                String.format("%.1f", percentage)).withStyle(color);
        
        // Если прочность низкая (менее 10%), делаем текст жирным
        if (durability <= DANGER_THRESHOLD) {
            durabilityText = Component.literal("").append(durabilityText).withStyle(ChatFormatting.BOLD);
        }
        
        tooltips.add(durabilityText);

        // Проверяем, находится ли игрок в зоне радиации
        if (level != null && level.isClientSide() && Minecraft.getInstance().player != null) {
            Player player = Minecraft.getInstance().player;
            BlockPos playerPos = player.blockPosition();
            
            // Получаем все зоны радиации и проверяем, находится ли игрок в какой-либо из них
            boolean inRadiationZone = false;
            float drainMultiplier = 1.0f;
            
            for (RadiationZone zone : RadiationMod.RADIATION_MANAGER.getRadiationZones()) {
                if (zone.isInside(player.position())) {
                    inRadiationZone = true;
                    drainMultiplier = zone.getFilterDrainMultiplier();
                    break;
                }
            }
            
            // Если игрок в зоне радиации, добавляем информацию о множителе расхода
            if (inRadiationZone) {
                Component drainText = Component.translatable("item.radiationmod.radiation_filter.tooltip.drain_rate", 
                        String.format("%.1f", drainMultiplier)).withStyle(ChatFormatting.RED);
                tooltips.add(drainText);
            }
        }

        // Добавляем информацию о назначении фильтра
        if (Screen.hasShiftDown()) {
            tooltips.add(Component.translatable("item.radiationmod.radiation_filter.tooltip.info.1")
                    .withStyle(ChatFormatting.GRAY));
            tooltips.add(Component.translatable("item.radiationmod.radiation_filter.tooltip.info.2")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltips.add(Component.translatable("item.radiationmod.tooltip.shift")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /**
     * Возвращает цвет текста в зависимости от прочности
     */
    private ChatFormatting getColorForDurability(int durability) {
        if (durability <= DANGER_THRESHOLD) {
            return ChatFormatting.RED;
        } else if (durability <= WARN_THRESHOLD) {
            return ChatFormatting.YELLOW;
        } else {
            return ChatFormatting.GREEN;
        }
    }

    /**
     * Получает текущий урон предмета
     */
    public static int getFilterDamage(ItemStack stack) {
        return stack.getOrCreateTag().getInt("Damage");
    }

    /**
     * Устанавливает урон для предмета
     */
    public static void setFilterDamage(ItemStack stack, int damage) {
        stack.getOrCreateTag().putInt("Damage", Math.min(MAX_DURABILITY, Math.max(0, damage)));
    }

    /**
     * Добавляет урон предмету
     * @return true если предмет сломался
     */
    public static boolean damageFilter(ItemStack stack, int amount) {
        int currentDamage = getFilterDamage(stack);
        int newDamage = currentDamage + amount;
        
        setFilterDamage(stack, newDamage);
        
        return newDamage >= MAX_DURABILITY;
    }

    /**
     * Проверяет, рабочий ли фильтр
     */
    public static boolean isFilterWorking(ItemStack stack) {
        return getFilterDamage(stack) < MAX_DURABILITY;
    }

    @Override
    public boolean isBarVisible(@Nonnull ItemStack stack) {
        return true; // Всегда показываем полоску прочности
    }

    @Override
    public int getBarWidth(@Nonnull ItemStack stack) {
        // Визуально более плавное отображение без явного шага
        float damage = getFilterDamage(stack);
        return Math.round(13.0F - damage * 13.0F / MAX_DURABILITY);
    }

    @Override
    public int getBarColor(@Nonnull ItemStack stack) {
        // Плавный переход от зеленого к красному по мере износа
        float durability = MAX_DURABILITY - getFilterDamage(stack);
        float ratio = durability / MAX_DURABILITY;
        
        // Используем HSV для плавного перехода цвета
        // От зеленого (120°) к желтому (60°) и к красному (0°)
        float hue = ratio * 120f; // 0-120 (красный->желтый->зеленый)
        
        // Конвертируем HSV в RGB
        float saturation = 1.0f;
        float value = 1.0f;
        
        int h = (int)(hue / 60) % 6;
        float f = hue / 60 - h;
        float p = value * (1 - saturation);
        float q = value * (1 - f * saturation);
        float t = value * (1 - (1 - f) * saturation);
        
        float r, g, b;
        switch (h) {
            case 0: r = value; g = t; b = p; break;
            case 1: r = q; g = value; b = p; break;
            case 2: r = p; g = value; b = t; break;
            case 3: r = p; g = q; b = value; break;
            case 4: r = t; g = p; b = value; break;
            default: r = value; g = p; b = q; break;
        }
        
        // Конвертируем в int цвет
        return 0xFF000000 | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }

    /**
     * Проверяет, есть ли у игрока рабочий фильтр в основных руках
     */
    public static boolean playerHasWorkingFilter(Player player) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        
        return (mainHand.getItem() instanceof RadiationFilterItem && isFilterWorking(mainHand)) ||
               (offHand.getItem() instanceof RadiationFilterItem && isFilterWorking(offHand));
    }

    /**
     * Наносит урон фильтру с учетом множителя
     */
    public void drainFilter(ItemStack stack, float multiplier) {
        int damage = (int) (1 * multiplier);
        if (damage < 1) damage = 1; // Минимальный урон
        
        damageFilter(stack, damage);
    }
} 