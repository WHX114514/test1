package cn.rbq108.test.event;

import cn.rbq108.test.api.RollEntity;
import cn.rbq108.test.camera.CameraManager; // <--- 1. 导入新的管理类
import cn.rbq108.test.main;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = main.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ClientEvents {

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        // --- 渲染方法 ---
        // 职责：只负责从状态管理器中获取数据，进行插值，然后应用到视觉相机。
        // 运行频率：每帧一次 (60+ FPS)

        // a. 获取渲染插值因子
        float partialTicks = (float) event.getPartialTick();

        // b. 计算 pitch 和 yaw 的平滑值
        float smoothedPitch = CameraManager.prevPitch + (CameraManager.currentPitch - CameraManager.prevPitch) * partialTicks;
        float smoothedYaw = CameraManager.prevYaw + (CameraManager.currentYaw - CameraManager.prevYaw) * partialTicks;

        // c. 从你的Mixin获取已平滑的roll值
        var cameraEntity = event.getCamera().getEntity();
        float roll = 0.0f;
        if (cameraEntity instanceof RollEntity) {
            // 假设你的 getRoll 方法已经处理了平滑，所以我们直接传入 partialTicks
            roll = ((RollEntity) cameraEntity).doABarrelRoll$getRoll(partialTicks);
        }

        // d. 将所有三个平滑后的角度应用到相机
        event.setPitch(smoothedPitch);
        event.setYaw(smoothedYaw);
        event.setRoll(roll);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // --- 逻辑方法 ---
        // 职责：只负责计算目标角度，并更新状态管理器和玩家实体的真实角度。
        // 运行频率：每游戏刻一次 (20 TPS)
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        boolean isCameraControlledByMod = true; // 你的开关

        if (isCameraControlledByMod) {
            // a. 将当前的 pitch/yaw 存为 "上一刻" 的值
            CameraManager.prevPitch = CameraManager.currentPitch;
            CameraManager.prevYaw = CameraManager.currentYaw;

            // b. 计算新的目标角度
            float targetPitch = 0.0f;//     ←——俯仰轴的角度！
            double time = (player.tickCount) / 20.0;
            // 重要：这里的计算应该基于一个稳定的值，而不是上一帧渲染的yaw
            float targetYaw = 0.0f;//////(float) (180 + Math.sin(time) * 45.0); // 示例：围绕180度来回摆动45度

            // c. 更新状态管理器中的 "当前" 值
            CameraManager.currentPitch = targetPitch;
            CameraManager.currentYaw = targetYaw;

            // d. 关键一步：更新玩家的真实角度，以保证准星交互正确
            player.setXRot(targetPitch);
            player.setYRot(targetYaw);
        } else {
            // 如果不由模组控制，则让状态管理器的值跟随玩家的鼠标输入
            // 这可以防止在关闭控制时相机瞬间跳变
            CameraManager.currentPitch = player.getXRot();
            CameraManager.currentYaw = player.getYRot();
            CameraManager.prevPitch = player.getXRot();
            CameraManager.prevYaw = player.getYRot();
        }
    }
}