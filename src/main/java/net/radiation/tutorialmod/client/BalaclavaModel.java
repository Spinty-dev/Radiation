package net.radiation.tutorialmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.radiation.tutorialmod.item.BalaclavaItem;

public class BalaclavaModel extends HumanoidModel<LivingEntity> {
    public BalaclavaModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Используем стандартную модель головы, но с измененными текстурами
        PartDefinition head = partdefinition.addOrReplaceChild("head", 
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, 
                    new CubeDeformation(0.25F)), // Чуть больше, чем обычная голова
            PartPose.ZERO);

        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.head.visible = true;
        this.hat.visible = false; // Отключаем шляпу
        this.body.visible = false;
        this.rightArm.visible = false;
        this.leftArm.visible = false;
        this.rightLeg.visible = false;
        this.leftLeg.visible = false;

        if (this.young) {
            poseStack.pushPose();
            poseStack.scale(0.75F, 0.75F, 0.75F);
            poseStack.translate(0.0F, 1.0F, 0.0F);
            this.head.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            poseStack.popPose();
        } else {
            this.head.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    @Override
    public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        
        // Если это броня на стойке, не нужно поворачивать
        if (entity instanceof ArmorStand) {
            ArmorStand armorStand = (ArmorStand) entity;
            this.head.xRot = (float) Math.toRadians(armorStand.getHeadPose().getX());
            this.head.yRot = (float) Math.toRadians(armorStand.getHeadPose().getY());
            this.head.zRot = (float) Math.toRadians(armorStand.getHeadPose().getZ());
        }
    }
} 