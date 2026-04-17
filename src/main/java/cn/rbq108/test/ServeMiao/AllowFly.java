package cn.rbq108.test.ServeMiao;

import cn.rbq108.test.main;
import cn.rbq108.test.VariableLibrary.GlobalVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;



@EventBusSubscriber(modid = main.MODID)
public class AllowFly {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) { // 🩺 重点在这里！加个 .Post
        Player player = event.getEntity();

        /*不是这个if在这有任何卵用吗？
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }*//*else{
            player.getAbilities().mayfly = false;
            player.onUpdateAbilities();
        }*/
        GameType gameMode = null;
        if (Minecraft.getInstance().gameMode != null) {
            gameMode = Minecraft.getInstance().gameMode.getPlayerMode();
        }//我写的这是啥？
        if (GlobalVariables.B_LowGravity) {
            player.getAbilities().mayfly = true;//zheli!
            player.onUpdateAbilities();
        } else if (GlobalVariables.B_LowGravity == false && (gameMode == GameType.SURVIVAL || gameMode == GameType.ADVENTURE)) {
            player.getAbilities().mayfly = false;
            //System.out.println("设置后(调用前)1: " + player.getAbilities().mayfly);
            //可能是时序问题？注释掉这两个println后就没用了，必须加一个sleep才能用，或者重新把这个println弄回来
            //try { Thread.sleep(1); } catch (InterruptedException e) {}
            player.onUpdateAbilities();
            //System.out.println("调用后2: " + player.getAbilities().mayfly);

        }


        /*
        if (GlobalVariables.B_LowGravity) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        } else {
            if (!player.isCreative() && !player.isSpectator() && player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }*/
    }
}


/**public class AllowFly {

    /**
     * 🏅 核心逻辑：绝对服从 B_LowGravity 的飞行授权系统
     * 只要失重开启，不管玩家在不在地上，一律发放“免死金牌”
     *
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        Player player = event.getEntity();

        // 🩺 1. 核心判定：只看失重开关是否为真 (B_LowGravity == true)
        if (GlobalVariables.B_LowGravity) {

            // 只要没领证，立刻发证，管你在不在地上蹦跶
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;

                // 💥 关键同步：告诉服务器这个玩家已经“合法起飞”了
                player.onUpdateAbilities();
            }
        }
        // 🩺 2. 只有当失重关闭时，才回收特权
        else {
            // 创造模式和旁观模式不归咱们管喵
            if (!player.isCreative() && !player.isSpectator()) {
                if (player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false; // 关掉开关后强制落地姿态
                    player.onUpdateAbilities();
                }
            }
        }
    }
}*/

//下面这一坨tm是啥啊
/**
package cn.rbq108.test.ServeMiao;

import cn.rbq108.test.main;
import cn.rbq108.test.VariableLibrary.GlobalVariables;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// 🩺 挂上牌匾，让 NeoForge 自动派员工来这里执勤
@EventBusSubscriber(modid = main.MODID)
public class AllowFly {

    /**
     * 🏅 核心逻辑：给失重飞行的玩家发“免死金牌”
     * 只要玩家的 mayfly 属性为 true，服务器的 anti-fly 检查就会自动闭嘴喵！
     *
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        Player player = event.getEntity();

        // 🩺 1. 检查全局失重开关是否开启
        if (GlobalVariables.B_LowGravity) {

            // 🏅 2. 检查玩家是否已经领证
            if (!player.getAbilities().mayfly) {
                // 强行把“允许飞行”开关拨到 ON
                player.getAbilities().mayfly = true;

                // 💥 关键同步：告诉服务器和客户端——“这家伙是有证的，别踢他！”
                player.onUpdateAbilities();
            }

        } else {
            // 🩺 3. 落地后的“收回特权”逻辑
            // 为了防止玩家关了失重后还能满地乱飞，咱们得在非创造/旁观模式下把证收回来
            if (!player.isCreative() && !player.isSpectator() && player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false; // 落地时停止飞行姿态
                player.onUpdateAbilities();
            }
        }
    }
}*/