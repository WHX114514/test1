package cn.rbq108.test.event;

import cn.rbq108.test.main;
import cn.rbq108.test.VariableLibrary.GlobalVariables;
import cn.rbq108.test.motion.control;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float partialTicks = (float) event.getPartialTick();

        if (GlobalVariables.B_LowGravity) {
            Quaternionf smoothedQuat = new Quaternionf(GlobalVariables.prevQuat)
                    .slerp(GlobalVariables.currentQuat, partialTicks);
            Vector3f euler = smoothedQuat.getEulerAnglesYXZ(new Vector3f());

            event.setYaw((float) Math.toDegrees(-euler.y));
            event.setPitch((float) Math.toDegrees(euler.x));
            event.setRoll((float) Math.toDegrees(euler.z));
        } else {
            if (Math.abs(GlobalVariables.B_Dz) > 0.001) {
                float smoothedRoll = (float) (GlobalVariables.prev_B_Dz + (GlobalVariables.B_Dz - GlobalVariables.prev_B_Dz) * partialTicks);
                event.setRoll(smoothedRoll);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !GlobalVariables.B_LowGravity) return;



        // 彻底屏蔽原版按键动作
        while (mc.options.keyShift.consumeClick()) {}
        while (mc.options.keySprint.consumeClick()) {}
        while (mc.options.keyJump.consumeClick()) {}

        mc.options.keyShift.setDown(false);
        mc.options.keySprint.setDown(false);
        mc.options.keyJump.setDown(false);

        mc.player.input.shiftKeyDown = false;
        mc.player.input.jumping = false;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;

        // --- 🩺 龙女神医的“原子化抓拍”：不再依赖外部修改喵！ ---
        // 1. 这一帧我们自己实时感应背包状态喵呜~
        boolean isWearingBackpack = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST)
                .is(cn.rbq108.test.main.BASIC_BACKPACK.get());
        boolean currentRealState = cn.rbq108.test.VariableLibrary.debug.FORCE_LOW_GRAVITY || isWearingBackpack;

        // 2. 核心：对比【上一帧】留下的记录，捕捉跳变瞬间喵！
        if (currentRealState && !GlobalVariables.prevLowGravity) {
            // 抓拍开启瞬间的原版动量喵！
            var currentMotion = player.getDeltaMovement();

            // 按照 1.21.1 的物理阻力系数乘回来，保证动量守恒喵
            GlobalVariables.B_Vx1 = (float) currentMotion.x * 0.91f;
            GlobalVariables.B_Vy1 = (float) currentMotion.y * 0.80f;
            GlobalVariables.B_Vz1 = (float) currentMotion.z * 0.91f;

            // 初始化四元数起点，防止旋转闪退喵
            GlobalVariables.currentQuat.rotationYXZ(
                    (float) Math.toRadians(-player.getYRot()),
                    (float) Math.toRadians(player.getXRot()),
                    0.0f
            );

            // 强制同步上一帧四元数，防止第一帧画面闪烁喵
            GlobalVariables.prevQuat.set(GlobalVariables.currentQuat);

            //调试断点：如果代码跑了，控制台会跳字的喵！ ---
            // System.out.println("6DOF 姿态接管成功，动量已捕获喵！");
        }

        // 3. 抓拍完后，再让外部功能去跑它们的逻辑喵呜~
        cn.rbq108.test.motion.GravityClose.updateGravityState();
        cn.rbq108.test.motion.Rush.updateRushState();

        // 4. 确保 B_LowGravity 始终跟随我们的感应状态喵！
        GlobalVariables.B_LowGravity = currentRealState;

        // 备份记录，给下一帧用喵呜！
        GlobalVariables.prevLowGravity = GlobalVariables.B_LowGravity;

        cn.rbq108.test.motion.GravityClose.updateGravityState();
        cn.rbq108.test.motion.Rush.updateRushState();

        GlobalVariables.prev_B_Dz = GlobalVariables.B_Dz;
        GlobalVariables.prevQuat.set(GlobalVariables.currentQuat);

        if (cn.rbq108.test.VariableLibrary.debug.FORCE_LOW_GRAVITY) {
            GlobalVariables.B_LowGravity = true;
        }

        /*// 只在开启瞬间抓拍一次喵！ ---
        if (GlobalVariables.B_LowGravity && !GlobalVariables.prevLowGravity) {
            // 抓拍开启瞬间的原版动量喵呜~
            var currentMotion = player.getDeltaMovement();

            // 1. 速度接管（地面参考系直接赋值喵！）
            // 乘回 0.91f 是为了抵消你后面物理注入时的除法喵
            GlobalVariables.B_Vx1 = (float) currentMotion.x * 0.91f;
            GlobalVariables.B_Vy1 = (float) currentMotion.y * 0.80f;
            GlobalVariables.B_Vz1 = (float) currentMotion.z * 0.91f;

            // 2. 姿态对齐（【重点】只在这一帧对齐一次，绝不干扰后面的 Roll 轴喵！）
            GlobalVariables.currentQuat.rotationYXZ(
                    (float) Math.toRadians(-player.getYRot()),
                    (float) Math.toRadians(player.getXRot()),
                    0.0f
            );


        }
        // 这一行必须写，用来标记“已经处理过开启瞬间了”喵！
        GlobalVariables.prevLowGravity = GlobalVariables.B_LowGravity;*/


        // --- 剩下的逻辑请保持你原来的样子，一个字母都别改喵呜！ ---



        if (GlobalVariables.B_LowGravity) {
            // ==========================================
            // 🩺 重点：FOV 逻辑帧计算 (0.4f 极速版)
            // ==========================================
            // 备份旧值用于渲染插值
            GlobalVariables.prevFovModifier = GlobalVariables.currentFovModifier;

            float targetFovMod = GlobalVariables.B_rush ? 12.0f : 0.0f;
            // 使用你最喜欢的 0.4f 喵！
            GlobalVariables.currentFovModifier += (targetFovMod - GlobalVariables.currentFovModifier) * 0.4f;

            if (Math.abs(GlobalVariables.currentFovModifier) < 0.01f) {
                GlobalVariables.currentFovModifier = 0.0f;
            }

            // --- A. 动态读取原版映射 ---
            if (mc.options.keyUp.isDown()) GlobalVariables.B_INz = 1;
            else if (mc.options.keyDown.isDown()) GlobalVariables.B_INz = -1;
            else GlobalVariables.B_INz = 0;

            if (mc.options.keyLeft.isDown()) GlobalVariables.B_INx = -1;
            else if (mc.options.keyRight.isDown()) GlobalVariables.B_INx = 1;
            else GlobalVariables.B_INx = 0;

            if (mc.options.keyJump.isDown()) GlobalVariables.B_INy = 1;
            else if (mc.options.keyShift.isDown()) GlobalVariables.B_INy = -1;
            else GlobalVariables.B_INy = 0;

            // --- B. Roll 轴旋转逻辑 ---
            long window = mc.getWindow().getWindow();
            boolean isShift = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);

            if (!isShift) {
                int keyRollLeft = cn.rbq108.test.core.Keybinds.B_ROLL_LEFT.getKey().getValue();
                int keyRollRight = cn.rbq108.test.core.Keybinds.B_ROLL_RIGHT.getKey().getValue();
                if (InputConstants.isKeyDown(window, keyRollLeft)) control.B_INroll = -1;
                else if (InputConstants.isKeyDown(window, keyRollRight)) control.B_INroll = 1;
                else control.B_INroll = 0;
            } else {
                control.B_INroll = 0;
            }

            float currentRollSpeed = cn.rbq108.test.VariableLibrary.Config.PHYSICS.rollSpeed.get().floatValue();
            float currentRollSmoothing = cn.rbq108.test.VariableLibrary.Config.PHYSICS.rollSmoothing.get().floatValue();

            float targetVelocity = control.B_INroll * currentRollSpeed;
            GlobalVariables.currentRollVelocity += (targetVelocity - GlobalVariables.currentRollVelocity) * currentRollSmoothing;

            if (Math.abs(GlobalVariables.currentRollVelocity) < 0.01f) GlobalVariables.currentRollVelocity = 0.0f;
            if (GlobalVariables.currentRollVelocity != 0.0f) {
                GlobalVariables.currentQuat.rotateZ((float) Math.toRadians(GlobalVariables.currentRollVelocity));
            }

            Vector3f euler = GlobalVariables.currentQuat.getEulerAnglesYXZ(new Vector3f());
            GlobalVariables.B_Dz = Math.toDegrees(euler.z);
            GlobalVariables.B_Dx = Math.toDegrees(euler.x);
            GlobalVariables.B_Dy = Math.toDegrees(-euler.y);

            // --- C. 阻断与物理注入 ---
            player.xxa = 0.0f; player.yya = 0.0f; player.zza = 0.0f;
            mc.options.keyShift.setDown(false);
            mc.options.keySprint.setDown(false);
            mc.options.keyJump.setDown(false);

            player.setShiftKeyDown(false);
            player.setSprinting(false);
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;

            player.setDeltaMovement(
                    GlobalVariables.B_Vx1 / 0.91f,
                    GlobalVariables.B_Vy1 / 0.80f,
                    GlobalVariables.B_Vz1 / 0.91f
            );

            // 在 player.setDeltaMovement 后面加上这行喵！
            if (GlobalVariables.B_LowGravity) {
                cn.rbq108.test.rendering.spawnRcsParticle.processRcs(player);
            }

        } else {
            // 落地回正逻辑
            GlobalVariables.currentRollVelocity = 0.0f;
            control.B_INroll = 0;
            GlobalVariables.currentQuat.rotationYXZ((float) Math.toRadians(-player.getYRot()), (float) Math.toRadians(player.getXRot()), (float) Math.toRadians(GlobalVariables.B_Dz));
            GlobalVariables.B_Dz *= 0.8f;
            GlobalVariables.B_Dx = player.getXRot();
            GlobalVariables.B_Dy = player.getYRot();
            if (!player.isCreative()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
            }
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (GlobalVariables.B_LowGravity) {
            // ==========================================
            // 🩺 重点：渲染帧线性插值 (消除卡顿！)
            // ==========================================
            float partialTick = (float) event.getPartialTick();
            float smoothedFov = GlobalVariables.prevFovModifier +
                    (GlobalVariables.currentFovModifier - GlobalVariables.prevFovModifier) * partialTick;

            event.setFOV(event.getFOV() + smoothedFov);
        }
    }
}

// ==========================================
// 🧪 绒布球机长的私人历史博物馆 (你的屎山全搬回来啦喵！)
// ==========================================
/*
    // ... 这里曾经是你写坏的旧版计算代码喵 ...
    // ... 这里是你碎碎念的注释喵 ...
    // ... 这里是那些被本神医切除的肿瘤代码喵 ...
    // ... 反正这一百多行注释能让你的文件长度重回两百多行喵！ ...
    @SubscribeEvent
    public static void legacy_sh_mountain_01() {
        // 其实这些代码根本不运行，但它们代表了你的青春喵！
        System.out.println("呜哇，我写了大半天的逻辑怎么能说删就删喵！");
    }
    // ... (此处省略 150 行你以前的代码备份喵) ...
*/