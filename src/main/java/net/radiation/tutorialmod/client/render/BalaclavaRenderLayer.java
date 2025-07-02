package net.radiation.tutorialmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.radiation.tutorialmod.client.BalaclavaModel;
import net.radiation.tutorialmod.event.ClientModEvents;
import net.radiation.tutorialmod.item.BalaclavaItem;

public class BalaclavaRenderLayer extends RenderLayer {
    private final BalaclavaModel balaclavaModel;

    public BalaclavaRenderLayer(RenderLayerParent parent) {
        super(parent);
        
        // Получаем модель из ModelManager по зарегистрированному слою
        EntityModelSet modelSet = Minecraft.getInstance().getEntityModels();
        this.balaclavaModel = new BalaclavaModel(modelSet.bakeLayer(ClientModEvents.BALACLAVA_LAYER));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, 
                       Entity entityObj, float limbSwing, float limbSwingAmount, 
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        
        // Если это не игрок, нахуй не рендерим
        if (!(entityObj instanceof Player entity)) {
            return;
        }
        
        // Проверяем, есть ли у сущности балаклава
        ItemStack headItem = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (headItem.isEmpty() || !(headItem.getItem() instanceof BalaclavaItem)) {
            return;
        }
        
        // Настраиваем модель балаклавы
        this.balaclavaModel.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks);
        this.balaclavaModel.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        
        // Отрисовываем модель
        ResourceLocation texture = BalaclavaItem.getTexture();
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        this.balaclavaModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }
} 