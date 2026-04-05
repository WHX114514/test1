package cn.rbq108.test.body;

import cn.rbq108.test.main;
import cn.rbq108.test.VariableLibrary.GlobalVariables;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class model {

    // 🩺 用来保存原版神经数据的“生命维持系统”
    private static float oldXRot, oldYBodyRot, oldYHeadRot;
    private static float oldXRotO, oldYBodyRotO, oldYHeadRotO;

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (event.getEntity() instanceof LocalPlayer player && GlobalVariables.B_LowGravity) {

            // ==========================================
            // 🩺 1. 全身冰冻：把史蒂夫变成一个“僵硬的手办”
            // ==========================================
            // 备份原版数据
            oldXRot = player.getXRot(); oldXRotO = player.xRotO;
            oldYBodyRot = player.yBodyRot; oldYBodyRotO = player.yBodyRotO;
            oldYHeadRot = player.yHeadRot; oldYHeadRotO = player.yHeadRotO;

            // 强制洗脑：180度在 MC 里是消除模型初始旋转的“魔法角度”
            // 这样能剥离所有原版引擎的乱转，让他变成一根纯净的“指针”喵！
            player.setXRot(0f); player.xRotO = 0f;
            player.yBodyRot = 0f; player.yBodyRotO = 0f;
            player.yHeadRot = 0f; player.yHeadRotO = 0f;

            // ==========================================
            // 🩺 2. 空间劫持：视觉中心与物理重心完美缝合！
            // ==========================================
            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose(); // 开启私人力场！

            // 获取平滑的绝对相机姿态
            float pt = event.getPartialTick();
            Quaternionf camQuat = new Quaternionf(GlobalVariables.prevQuat)
                    .slerp(GlobalVariables.currentQuat, pt);

            // 💥 终极缝合魔法：
            // 相机的焦点永远在眼睛高度 (EyeHeight，约 1.62)
            float cameraCenter = player.getEyeHeight();
            // 你想要的飞船物理重心 (BbHeight * 0.6f，约 1.08)
            float pivotHeight = player.getBbHeight() * 0.6f;

            // 1. 第一步：把整个世界的旋转中心，移动到【相机的焦点】上！
            // 保证飞船在旋转时，绝对不会偏离屏幕正中心喵！
            poseStack.translate(0, cameraCenter, 0);

            // 2. 降维打击：在相机焦点处应用你最完美的 6DOF 旋转！
            poseStack.mulPose(camQuat);

            // 3. 第三步：往回退的时候，只退回【飞船物理重心】的距离！
            // 这相当于在视觉上把史蒂夫“拔高”了 0.5 格，让他的胸口（0.6f）刚好死死贴在相机的镜头中心喵！
            poseStack.translate(0, -pivotHeight, 0);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (event.getEntity() instanceof LocalPlayer player && GlobalVariables.B_LowGravity) {
            // ==========================================
            // 🩺 3. 术后复苏：把一切还给原版引擎
            // ==========================================
            event.getPoseStack().popPose(); // 关闭私人力场

            // 解冻神经，防止他的碰撞箱和移动逻辑坏掉喵！
            player.setXRot(oldXRot); player.xRotO = oldXRotO;
            player.yBodyRot = oldYBodyRot; player.yBodyRotO = oldYBodyRotO;
            player.yHeadRot = oldYHeadRot; player.yHeadRotO = oldYHeadRotO;
        }
    }
}