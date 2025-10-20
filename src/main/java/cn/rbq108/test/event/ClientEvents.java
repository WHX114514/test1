package cn.rbq108.test.event;

import cn.rbq108.test.api.RollEntity;
import cn.rbq108.test.main;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * 这个类专门负责监听和处理所有客户端相关的事件。
 * @Mod.EventBusSubscriber 注解会自动将这个类注册到事件总线上，让它开始工作。
 */
// 修复 #2: 目标总线改为 NEOFORGE，并指定为客户端事件
@EventBusSubscriber(modid = main.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ClientEvents {

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        var cameraEntity = event.getCamera().getEntity();
        float roll = 0.0f;
        if (cameraEntity instanceof RollEntity) {
            roll = ((RollEntity) cameraEntity).doABarrelRoll$getRoll((float) event.getPartialTick());
        }

        float newPitch = 0.0f;
        double time = System.currentTimeMillis() / 1000.0;
        float newYaw = (float) (event.getYaw() + Math.sin(time) * 15.0);

        event.setPitch(newPitch);
        event.setYaw(newYaw);
        event.setRoll(roll);
    }

    @SubscribeEvent
    // 修复 #1: 将 ClientTickEvent 修改为具体的 ClientTickEvent.Post
    public static void onClientTick(ClientTickEvent.Post event) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        boolean isCameraControlledByMod = true;

        if (isCameraControlledByMod) {
            float targetPitch = 0.0f;
            double time = (player.tickCount) / 20.0;
            float targetYaw = (float) (player.getYRot() + Math.sin(time) * 15.0);

            player.setXRot(targetPitch);
            player.setYRot(targetYaw);
        }
    }
}