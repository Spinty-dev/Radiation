package net.radiation.tutorialmod.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.radiation.tutorialmod.RadiationMod;
import net.radiation.tutorialmod.client.BalaclavaModel;
import net.radiation.tutorialmod.event.ClientModEvents;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Consumer;

public class BalaclavaItem extends Item {
    private static final ResourceLocation TEXTURE = new ResourceLocation(RadiationMod.MOD_ID, "textures/models/balaclava__layer_1.png");
    
    public BalaclavaItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BalaclavaModel model = null;
            
            @Override
            public @Nullable HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                // Мы будем использовать нашу модель только для слота HEAD
                if (equipmentSlot == EquipmentSlot.HEAD) {
                    // Ленивая инициализация модели по требованию
                    if (model == null) {
                        model = new BalaclavaModel(Minecraft.getInstance().getEntityModels().bakeLayer(ClientModEvents.BALACLAVA_LAYER));
                    }
                    
                    model.prepareMobModel(livingEntity, 0, 0, 1);
                    model.setupAnim(livingEntity, 0, 0, 0, 0, 0);
                    return model;
                }
                return null;
            }
        });
    }

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        // Указываем, что это предмет для слота головы
        return EquipmentSlot.HEAD;
    }
    
    // Эта функция поможет нам получить текстуру для модели
    public static ResourceLocation getTexture() {
        return TEXTURE;
    }
    
    // Переопределяем этот метод, чтобы предмет не давал никаких атрибутов (не как броня)
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return ImmutableMultimap.of();
    }
} 