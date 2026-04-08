package cn.rbq108.test.ServeMiao.communication;

import cn.rbq108.test.main;
import cn.rbq108.test.VariableLibrary.GlobalVariables;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// 🩺 重点：只在客户端运行 (Dist.CLIENT)，负责“对外发货”
@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class PlayerSent {

    /**
     * 📦 每一刻检测：如果姿态变了，就立刻通知全宇宙
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        // 🩺 1. 核心过滤：只有当前客户端控制的“本地玩家”才需要发包！
        // 如果不加这个判断，您的客户端会尝试帮场景里所有玩家发包，那服务器就炸了喵！
        if (player.level().isClientSide() && player.isLocalPlayer()) {

            // 🩺 2. 检查失重状态开关
            if (GlobalVariables.B_LowGravity) {

                // 💥 3. 正式发货！将四元数、UUID 和开关状态打包扔给服务器
                // 这里的 SyncRotationPayload 就是咱们之前定义的胶囊喵
                PacketDistributor.sendToServer(new SyncRotationPayload(
                        player.getUUID(),
                        GlobalVariables.currentQuat,
                        GlobalVariables.B_LowGravity
                ));
            }
        }
    }
}