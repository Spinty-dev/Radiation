package net.radiation.tutorialmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.radiation.tutorialmod.block.entity.BearTrapBlockEntity;

/**
 * Рендерер для капкана, использует ванильную анимацию
 * Этот класс нужен для регистрации, но не содержит пользовательской логики рендеринга
 */
public class BearTrapRenderer implements BlockEntityRenderer<BearTrapBlockEntity> {

    public BearTrapRenderer(BlockEntityRendererProvider.Context context) {
        // Конструктор пуст, так как мы используем ванильную анимацию
    }

    @Override
    public void render(BearTrapBlockEntity blockEntity, float partialTick, PoseStack poseStack, 
                      MultiBufferSource buffer, int packedLight, int packedOverlay) {
        // Не выполняем собственный рендеринг, так как используем ванильную анимацию
        // Minecraft обрабатывает анимацию открывания/закрывания через состояния блоков
    }
} 