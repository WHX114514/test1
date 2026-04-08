package cn.rbq108.test.body;

import cn.rbq108.test.main;
import cn.rbq108.test.VariableLibrary.GlobalVariables;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// 💥 重点：绝对不能加 value = Dist.CLIENT！必须让服务端（算物理的）也能跑这段代码！
@EventBusSubscriber(modid = main.MODID)
public class hitbox {

    // 🩺 1. 尺寸定义工厂
    @SubscribeEvent
    public static void onPlayerSize(EntityEvent.Size event) {
        if (event.getEntity() instanceof Player && GlobalVariables.B_LowGravity) {
            float size = GlobalVariables.B_HitboxSize;
            // 锁定眼高，防止视角卡进方块窒息

            // 🩺 定义你的“上移偏移量” (单位：格)
            // 你可以随时调这个数字，越大碰撞箱“看起来”就越高
            float offsetY = 0.5f;

            // 💥 关键修改：眼高加上偏移量，让摄像头跟着升起来！
            event.setNewSize(EntityDimensions.scalable(size, size)
                    .withEyeHeight(size * 0.85f + offsetY));

            event.setNewSize(EntityDimensions.scalable(size, size).withEyeHeight(size * 0.6f));
        }
    }

    // 🩺 2. 【核心修复】双端物理同步监工
    // 这个事件会在客户端和服务端双端触发，彻底干掉那个“看不见的站立幽灵”！
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (GlobalVariables.B_LowGravity) {
            // qiangzhizhanli!!!!
            player.setPose(Pose.STANDING);
            //player.setPose(Pose.SWIMMING);

            // 💥 智能刷新：如果服务端的盒子还没缩短，强制刷新！
            if (Math.abs(player.getBbHeight() - GlobalVariables.B_HitboxSize) > 0.01f) {
                player.refreshDimensions();
            }
        } else {
            // 💥 智能恢复：如果关了失重，但盒子还是小的，强制变回原状！
            if (Math.abs(player.getBbHeight() - GlobalVariables.B_HitboxSize) < 0.01f) {
                player.refreshDimensions();
            }
        }
    }
}