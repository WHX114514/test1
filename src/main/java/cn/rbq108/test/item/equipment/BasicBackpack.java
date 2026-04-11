package cn.rbq108.test.item.equipment;

import cn.rbq108.test.client.model.BASIC_BACKPACK_Converted;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;

/**
 * 绒布球主人钦定：3D重力背包物品类
 * 已集成了材质加载和 NeoForge 的 3D 模型接口喵呜！
 */
public class BasicBackpack extends ArmorItem {

    // 1. 定义你的专属材质（这是解决“皮革”问题的关键喵！）
    private static final Holder<ArmorMaterial> BACKPACK_MATERIAL = Holder.direct(new ArmorMaterial(
            Map.of(
                    ArmorItem.Type.CHESTPLATE, 1 // 防御力喵
            ),
            0, // 附魔等级，我们不需要它发光喵
            SoundEvents.ARMOR_EQUIP_LEATHER, // 穿戴声效喵
            () -> Ingredient.EMPTY, // 修复材料喵
            // 这里指定了贴图路径，注意全小写喵！它会去找 assets/test/textures/models/armor/basic_backpack_layer_1.png
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("test", "basic_backpack"))),
            0.0F, // 韧性
            0.0F  // 击退抗性
    ));

    public BasicBackpack() {
        super(BACKPACK_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1));
    }

    // 这里就是 NeoForge 原生 3D 模型挂载的接口喵！
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public @NotNull net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(
                    net.minecraft.world.entity.LivingEntity entity,
                    ItemStack stack,
                    net.minecraft.world.entity.EquipmentSlot slot,
                    net.minecraft.client.model.HumanoidModel<?> _default
            ) {
                // 关键喵：这里需要返回你修好的那个大写的 Java 模型类喵！
                // 同时把玩家默认的层结构传给它，确保它挂在 Body 上喵呜~！
                //return new BASIC_BACKPACK_Converted<>(_default.bakeLayer(BASIC_BACKPACK_Converted.LAYER_LOCATION));

                // 不要用 _default.bakeLayer，改用这个喵！
                return new BASIC_BACKPACK_Converted<>(
                        Minecraft.getInstance().getEntityModels().bakeLayer(BASIC_BACKPACK_Converted.LAYER_LOCATION)
                );
            }
        });
    }
}

/*package cn.rbq108.test.item.equipment;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import java.util.function.Consumer;

public class BasicBackpack extends ArmorItem {
    public BasicBackpack() {
        // 这里借用皮革材质，设定为胸甲类型喵
        super(ArmorMaterials.LEATHER, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1));
    }

    // 这里是连接 3D 模型的接口喵！
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        // 下一步我们在这里挂载那个复杂的模型类喵呜~
    }
}*/

/*这是一大坨一大坨超超级大的屎山！模型也塞进来了（虽然用不了）
package cn.rbq108.test.item.equipment; // 遵命！就在这个地址喵！


import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 绒布球主人钦定的 basic_backpack 模型喵！
 * 添加了 @OnlyIn(Dist.CLIENT) 防止服务器炸机喵呜~

@OnlyIn(Dist.CLIENT)
public class basic_backpack<T extends Entity> extends EntityModel<T> {
    // 修正为你的 ModID "test" 喵！
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("test", "basic_backpack"), "main");

    private final ModelPart bag; // 支架主体喵

    public basic_backpack(ModelPart root) {
        // 这里必须根据你 Blockbench 的层级来捞方块喵！
        // 你的导出文件里层级很深，本小姐帮你顺藤摸瓜捞到 bag 喵呜~
        this.bag = root.getChild("root2").getChild("waist2").getChild("body2").getChild("bag2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // --- 这里保留你 Blockbench 导出的那一大长串参数喵 ---
        // 注意：本小姐只放了骨架，具体的 CubeListBuilder 参数请保留你之前文件里的喵呜！
        PartDefinition root2 = partdefinition.addOrReplaceChild("root2", CubeListBuilder.create(), PartPose.offset(8.0F, 24.0F, -8.0F));
        PartDefinition waist2 = root2.addOrReplaceChild("waist2", CubeListBuilder.create(), PartPose.offset(0.0F, -12.0F, 0.0F));
        PartDefinition body2 = waist2.addOrReplaceChild("body2", CubeListBuilder.create(), PartPose.offset(0.0F, -12.0F, 0.0F));
        body2.addOrReplaceChild("bag2", CubeListBuilder.create().texOffs(7, 1).addBox(-1.25F, -0.25F, -2.5F, 1.0F, 1.0F, 4.75F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        // ... 剩下那几十个方块参数也得在这里面喵！

        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // 这里是 6DOF 同步的预留位喵！
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        bag.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
*/
/*
// 补上这些 NeoForge 的原生接口导入喵！

import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;



public class BasicBackpack extends ArmorItem {

    public BasicBackpack() {
        super(BACKPACK_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1));
    }

    // 这就是 NeoForge 原生支持 3D 盔甲的秘密入口喵！
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(
                    LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> _default
            ) {
                // 这里返回你刚才导出的那个 BackpackModel 类喵！
                // 它会替代原版的“纸片”，把 3D 支架画在玩家背上喵呜！
                return BackpackModel.INSTANCE;
            }
        });
    }
}

/*
下面是，一大坨！package cn.rbq108.test.item.equipment;


import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;

/**
 * 绒布球主人专属：重力背包 3D 模型挂载类
 * 已彻底切除皮革材质病灶，拒绝纸片化喵呜！

public class BasicBackpack extends ArmorItem {

    // 1. 定义你的专属材质（这是解决“皮革”问题的唯一解药喵！）
    private static final Holder<ArmorMaterial> BACKPACK_MATERIAL = Holder.direct(new ArmorMaterial(
            Map.of(
                    ArmorItem.Type.CHESTPLATE, 1 // 防御力喵
            ),
            0, // 附魔等级，我们不需要它发光喵
            SoundEvents.ARMOR_EQUIP_LEATHER, // 穿戴声效喵
            () -> Ingredient.EMPTY, // 修复材料喵
            // 这里就是关键喵！它会去 assets/test/textures/models/armor/ 目录下找 basic_backpack_layer_1.png
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("test", "basic_backpack"))),
            0.0F, // 韧性
            0.0F  // 击退抗性
    ));

    public BasicBackpack() {
        // 使用我们刚捏好的材质，指定为胸甲槽位喵
        super(BACKPACK_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties()
                .stacksTo(1)
        );
    }
}*/