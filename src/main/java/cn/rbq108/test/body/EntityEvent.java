/*package cn.rbq108.test.body;

import cn.rbq108.test.VariableLibrary.GlobalVariables;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;

@EventBusSubscriber(modid = cn.rbq108.test.main.MODID, value = Dist.CLIENT)
public class hitbox {

    @SubscribeEvent
    public static void onPlayerSize(EntityEvent.Size event) {
        // 🩺 只针对本地玩家且开启失重模式时生效
        if (event.getEntity() instanceof LocalPlayer && GlobalVariables.B_LowGravity) {

            float size = GlobalVariables.B_HitboxSize;

            // 🩺 1. 官方正统写法：定义一个新的维度包
            // 重点：.withEyeHeight(size * 0.85f) 必须写，否则视角会卡进天花板触发窒息
            EntityDimensions customDim = EntityDimensions.scalable(size, size)
                    .withEyeHeight(size * 0.85f);

            // 🩺 2. 彻底禁用原版互搏的关键：
            // 无论引擎现在觉得我是什么姿态（站立、潜行、游泳），我通通返回这个小盒子！
            event.setNewSize(customDim);

            // 💡 这样即便引擎想用“站立”的逻辑卡你，它拿到的高度也是 size 喵！
        }
    }
}*/