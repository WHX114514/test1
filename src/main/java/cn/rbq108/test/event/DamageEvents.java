package cn.rbq108.test.event;

import cn.rbq108.test.main;
import cn.rbq108.test.VariableLibrary.GlobalVariables; // ⚠️ 神医提醒：必须导入你的记事本喵！
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

@EventBusSubscriber(modid = main.MODID)
public class DamageEvents {

    @SubscribeEvent
    public static void onPlayerFall(LivingFallEvent event) {
        // 只有玩家才归我们管，怪物随它去喵
        if (event.getEntity() instanceof Player player) {

            // ==========================================
            // 神医的终极降维打击：直接窃取客户端的物理引擎数据！
            // ==========================================
            // 回归 LivingFallEvent，这个事件只有在客户端明确宣告“我真的踩到方块了”时才会触发。
            // 完美杜绝半空中被空气撞死的 Bug！

            // 重点来了！直接读取你刚刚在 calculate.java 里算出来的、绝对精准的垂直速度！
            // 取绝对值，不管正负都变成冲击力喵！
            double realImpactSpeed = Math.abs(GlobalVariables.B_Vy1);

            // 重新设定你的安全速度阈值
            // (0.6 大概是原版跳跃的安全极限，你要是觉得1.2手感好也可以改成1.2喵)
            double safeSpeed = 0.6;

            if (realImpactSpeed <= safeSpeed) {
                // 只要你落地那一刻的真实速度够慢，哪怕你从 10000 格高的太空降落，也绝对不掉血！
                event.setCanceled(true);
                event.setDistance(0.0f);
            } else {
                // 动能超标，发生硬着陆！计算超出的速度！
                float crashSeverity = (float) (realImpactSpeed - safeSpeed);

                // 神医的硬核骨折公式：
                // 原版掉落伤害 = distance - 3。
                // 我们把超速部分乘以 20 作为基础惩罚，再加上 3，确保只要超速就立刻开始扣血！
                event.setDistance(crashSeverity * 20.0f + 3.0f);
            }
        }
    }
}