package cn.rbq108.test.rendering;

import cn.rbq108.test.main;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Quaternionf;


import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class OtherPlayer {

    private static final WeakHashMap<Player, Float[]> SAVED_ROTS = new WeakHashMap<>();

    private static final WeakHashMap<UUID, Quaternionf> SMOOTH_BODY_QUATS = new WeakHashMap<>();
    private static final WeakHashMap<UUID, Quaternionf> SMOOTH_BODY_QUATS_O = new WeakHashMap<>();

    private static final WeakHashMap<UUID, Quaternionf> NET_HEAD_QUATS = new WeakHashMap<>();
    private static final WeakHashMap<UUID, Quaternionf> NET_HEAD_QUATS_O = new WeakHashMap<>();

    public static class RotationData {
        public Quaternionf headQuatRender = new Quaternionf();
        public Quaternionf bodyQuatRender = new Quaternionf();
    }
    public static final ConcurrentHashMap<UUID, RotationData> ROTATION_SYNC_MAP = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() && player != Minecraft.getInstance().player) {
            UUID id = player.getUUID();
            if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
                Quaternionf targetQuat = NetworkHandler.REMOTE_ROTATIONS.get(id);
                if (targetQuat == null) return;

                Quaternionf currentHead = NET_HEAD_QUATS.computeIfAbsent(id, k -> new Quaternionf(targetQuat));
                NET_HEAD_QUATS_O.put(id, new Quaternionf(currentHead));
                currentHead.set(targetQuat);

                Quaternionf currentBody = SMOOTH_BODY_QUATS.computeIfAbsent(id, k -> new Quaternionf(targetQuat));
                SMOOTH_BODY_QUATS_O.put(id, new Quaternionf(currentBody));
                currentBody.slerp(targetQuat, 0.15f);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player == Minecraft.getInstance().player) return;

        UUID id = player.getUUID();
        if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
            Quaternionf headQuatNet = NetworkHandler.REMOTE_ROTATIONS.get(id);

            if (headQuatNet != null) {

                //float centerY = player.getBbHeight() * 0.5f;//这行是定义玩家模型偏离碰撞箱高度用的，用不着了


                float frameTime = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);

                Quaternionf prevHead = NET_HEAD_QUATS_O.getOrDefault(id, headQuatNet);
                Quaternionf currHead = NET_HEAD_QUATS.getOrDefault(id, headQuatNet);
                Quaternionf smoothHeadQuatRender = new Quaternionf(prevHead).slerp(currHead, frameTime);

                Quaternionf prevBody = SMOOTH_BODY_QUATS_O.getOrDefault(id, headQuatNet);
                Quaternionf currBody = SMOOTH_BODY_QUATS.getOrDefault(id, headQuatNet);
                Quaternionf smoothBodyQuatRender = new Quaternionf(prevBody).slerp(currBody, frameTime);

                // 💥 终极修复 1：矩阵保护锁！
                // 把当前的“直立画布”存起来，绝对不污染后续的碰撞箱渲染！
                event.getPoseStack().pushPose();

                // 💥 终极修复 2：转移旋转重心！
                // 把旋转轴心从脚底 (0) 拔高到身体重心 (0.9 左右)
                event.getPoseStack().translate(0.0D, 0.6D, 0.0D);
                // 在重心处进行完美 6DOF 旋转
                event.getPoseStack().mulPose(smoothBodyQuatRender);
                // 旋转完后再把轴心降回脚底，完美居中，彻底告别上移偏移！
                event.getPoseStack().translate(0.0D, -0.8D, 0.0D);




                RotationData data = ROTATION_SYNC_MAP.computeIfAbsent(id, k -> new RotationData());
                data.headQuatRender = smoothHeadQuatRender;
                data.bodyQuatRender = smoothBodyQuatRender;

                SAVED_ROTS.put(player, new Float[]{
                        player.yBodyRot, player.yBodyRotO,
                        player.yHeadRot, player.yHeadRotO,
                        player.getXRot(), player.xRotO,
                        player.getYRot(), player.yRotO
                });
                player.yBodyRot = 0; player.yBodyRotO = 0;
                player.yHeadRot = 0; player.yHeadRotO = 0;
                player.setXRot(0); player.xRotO = 0;
                player.setYRot(0); player.yRotO = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();

        // 💥 终极防闪退修复：把 popPose 移到冷冻柜的判断内部！
        // 只有在 Pre 阶段真正执行了 pushPose（即成功拿到网络数据）的玩家，
        // 他的数据才会被存进 SAVED_ROTS。
        // 所以只要进得来这个 if，就说明之前一定 pushPose 了，这时候 popPose 绝对不会报错喵！
        if (SAVED_ROTS.containsKey(player)) {

            // 1. 安全地关上抽屉（恢复矩阵）
            event.getPoseStack().popPose();

            // 2. 解冻原版数据
            Float[] rots = SAVED_ROTS.get(player);
            player.yBodyRot = rots[0]; player.yBodyRotO = rots[1];
            player.yHeadRot = rots[2]; player.yHeadRotO = rots[3];
            player.setXRot(rots[4]); player.xRotO = rots[5];
            player.setYRot(rots[6]); player.yRotO = rots[7];

            SAVED_ROTS.remove(player);
        }
    }
    /*public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();

        // 💥 终极修复 1 的下半部分：恢复矩阵！
        // 如果我们画的是失重玩家，渲染完后必须把画布“弹”回直立状态！
        if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(player.getUUID(), false)) {
            event.getPoseStack().popPose();
        }

        if (SAVED_ROTS.containsKey(player)) {
            Float[] rots = SAVED_ROTS.get(player);
            player.yBodyRot = rots[0]; player.yBodyRotO = rots[1];
            player.yHeadRot = rots[2]; player.yHeadRotO = rots[3];
            player.setXRot(rots[4]); player.xRotO = rots[5];
            player.setYRot(rots[6]); player.yRotO = rots[7];
            SAVED_ROTS.remove(player);
        }
    }*/
}


/*这个不错，但是碰撞箱错了，顺便修一下叭

package cn.rbq108.test.rendering;

import cn.rbq108.test.main;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Quaternionf;

import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class OtherPlayer {

    private static final WeakHashMap<Player, Float[]> SAVED_ROTS = new WeakHashMap<>();

    // 🩺 身体的平滑历史记录（带延迟惯性）
    private static final WeakHashMap<UUID, Quaternionf> SMOOTH_BODY_QUATS = new WeakHashMap<>();
    private static final WeakHashMap<UUID, Quaternionf> SMOOTH_BODY_QUATS_O = new WeakHashMap<>();

    // 💥 新增：脑袋的 20Hz 历史记录（用于纯净补帧，无延迟）
    private static final WeakHashMap<UUID, Quaternionf> NET_HEAD_QUATS = new WeakHashMap<>();
    private static final WeakHashMap<UUID, Quaternionf> NET_HEAD_QUATS_O = new WeakHashMap<>();

    public static class RotationData {
        public Quaternionf headQuatRender = new Quaternionf();
        public Quaternionf bodyQuatRender = new Quaternionf();
    }
    public static final ConcurrentHashMap<UUID, RotationData> ROTATION_SYNC_MAP = new ConcurrentHashMap<>();

    // 🏆 在稳定的 20Hz Tick 中更新数据
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() && player != Minecraft.getInstance().player) {
            UUID id = player.getUUID();
            if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
                Quaternionf targetQuat = NetworkHandler.REMOTE_ROTATIONS.get(id);
                if (targetQuat == null) return;

                // 💥 1. 脑袋的历史记录更新
                Quaternionf currentHead = NET_HEAD_QUATS.computeIfAbsent(id, k -> new Quaternionf(targetQuat));
                NET_HEAD_QUATS_O.put(id, new Quaternionf(currentHead)); // 把当前当做“上一刻”
                currentHead.set(targetQuat); // 更新为网络最新的瞬间目标

                // 2. 身体的惯性追赶更新（保持不变）
                Quaternionf currentBody = SMOOTH_BODY_QUATS.computeIfAbsent(id, k -> new Quaternionf(targetQuat));
                SMOOTH_BODY_QUATS_O.put(id, new Quaternionf(currentBody));
                currentBody.slerp(targetQuat, 0.15f); // 0.15f 决定了身体追赶的缓慢程度
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player == Minecraft.getInstance().player) return;

        UUID id = player.getUUID();
        if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
            Quaternionf headQuatNet = NetworkHandler.REMOTE_ROTATIONS.get(id);

            if (headQuatNet != null) {
                float frameTime = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);

                // 💥 魔法 1：对脑袋进行 PartialTick 补帧！
                // 这会在渲染的高帧率下，把 20Hz 的跳跃填补得像德芙一样丝滑！
                Quaternionf prevHead = NET_HEAD_QUATS_O.getOrDefault(id, headQuatNet);
                Quaternionf currHead = NET_HEAD_QUATS.getOrDefault(id, headQuatNet);
                Quaternionf smoothHeadQuatRender = new Quaternionf(prevHead).slerp(currHead, frameTime);

                // 魔法 2：对身体进行 PartialTick 补帧（保持不变）
                Quaternionf prevBody = SMOOTH_BODY_QUATS_O.getOrDefault(id, headQuatNet);
                Quaternionf currBody = SMOOTH_BODY_QUATS.getOrDefault(id, headQuatNet);
                Quaternionf smoothBodyQuatRender = new Quaternionf(prevBody).slerp(currBody, frameTime);

                // 把身体赋给绘图栈
                event.getPoseStack().mulPose(smoothBodyQuatRender);

                // 💥 魔法 3：把“插值过的高帧率脑袋”和“高帧率身体”传给 Mixin！
                RotationData data = ROTATION_SYNC_MAP.computeIfAbsent(id, k -> new RotationData());
                data.headQuatRender = smoothHeadQuatRender;
                data.bodyQuatRender = smoothBodyQuatRender;

                // 屏蔽原版旋转
                SAVED_ROTS.put(player, new Float[]{
                        player.yBodyRot, player.yBodyRotO,
                        player.yHeadRot, player.yHeadRotO,
                        player.getXRot(), player.xRotO,
                        player.getYRot(), player.yRotO
                });
                player.yBodyRot = 0; player.yBodyRotO = 0;
                player.yHeadRot = 0; player.yHeadRotO = 0;
                player.setXRot(0); player.xRotO = 0;
                player.setYRot(0); player.yRotO = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (SAVED_ROTS.containsKey(player)) {
            Float[] rots = SAVED_ROTS.get(player);
            player.yBodyRot = rots[0]; player.yBodyRotO = rots[1];
            player.yHeadRot = rots[2]; player.yHeadRotO = rots[3];
            player.setXRot(rots[4]); player.xRotO = rots[5];
            player.setYRot(rots[6]); player.yRotO = rots[7];
            SAVED_ROTS.remove(player);
        }
    }
}*/


/*这个测试完成了，但是由于没有平滑效果还是换一板叭
package cn.rbq108.test.rendering;

import cn.rbq108.test.main;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Quaternionf;

import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class OtherPlayer {

    private static final WeakHashMap<Player, Float[]> SAVED_ROTS = new WeakHashMap<>();

    // 🩺 我们需要完整的四元数插值，分别记录当前 Tick 和上一 Tick 的平滑身体姿态
    private static final WeakHashMap<UUID, Quaternionf> SMOOTH_BODY_QUATS = new WeakHashMap<>();
    private static final WeakHashMap<UUID, Quaternionf> SMOOTH_BODY_QUATS_O = new WeakHashMap<>();

    // 💥 重点：给 Mixin 传递瞬时和插值后的脑袋姿态
    public static class RotationData {
        public Quaternionf headQuatRender = new Quaternionf(); // 瞬时脑袋指向
        public Quaternionf bodyQuatRender = new Quaternionf(); // 插值身体指向
    }
    public static final ConcurrentHashMap<UUID, RotationData> ROTATION_SYNC_MAP = new ConcurrentHashMap<>();

    // 🏆 魔法 1：在稳定的 20Hz Tick 中计算身体延迟，彻底干掉抽搐！
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() && player != Minecraft.getInstance().player) {
            UUID id = player.getUUID();
            if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
                // 这是网络发来的 target 姿态（脑袋指向）
                Quaternionf targetQuat = NetworkHandler.REMOTE_ROTATIONS.get(id);
                if (targetQuat == null) return;

                // 拿到当前（O）的平滑姿态，如果没有就用目标姿态填充
                Quaternionf currentBodyQuat = SMOOTH_BODY_QUATS.computeIfAbsent(id, k -> new Quaternionf(targetQuat));

                // 1. 把现在的身体姿态存为“上一步 (O)”
                SMOOTH_BODY_QUATS_O.put(id, new Quaternionf(currentBodyQuat));

                // 2. 身体的四元数 Slerp（球面线性插值），慢慢向脑袋指向追赶 (0.15f 是追踪速度，可微调喵)
                currentBodyQuat.slerp(targetQuat, 0.15f);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player == Minecraft.getInstance().player) return;

        UUID id = player.getUUID();
        if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
            // 这是网络发来的目标姿态（脑袋指向）
            Quaternionf headQuatNet = NetworkHandler.REMOTE_ROTATIONS.get(id);

            if (headQuatNet != null) {
                // 💥 魔法 2：渲染时使用 FrameTime (PartialTick) 进行绝对无缝衔接！
                //float frameTime = Minecraft.getInstance().getFrameTime();
                float frameTime = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
                Quaternionf prevBody = SMOOTH_BODY_QUATS_O.getOrDefault(id, headQuatNet);
                Quaternionf currBody = SMOOTH_BODY_QUATS.getOrDefault(id, headQuatNet);

                // 🖌️ 3D数学魔法：完美的球面线性插值，填补网络帧率空隙喵！
                Quaternionf smoothBodyQuatRender = new Quaternionf(prevBody).slerp(currBody, frameTime);

                // 💥 🏆 魔法 3（终极修复）：把整个绘图栈 (PoseStack) 瞬间对齐到平滑身体姿态！！
                // 这行代码会让史蒂夫的身体、肩膀位置、胳膊腿整体保持完美的 hierarchical 同步！！
                // 肢体脱臼和位置抽搐彻底不复存在，因为肩膀的物理重心和身体一起平滑跟过去了喵！！♡
                event.getPoseStack().mulPose(smoothBodyQuatRender);

                // 4. 将瞬时脑袋姿态和平滑身体姿态存入黑板供 Mixin 使用
                RotationData data = ROTATION_SYNC_MAP.computeIfAbsent(id, k -> new RotationData());
                data.headQuatRender = headQuatNet;
                data.bodyQuatRender = smoothBodyQuatRender;

                // 5. 屏蔽原版旋转 (包含总偏航角) 防止二次旋转 Bug
                SAVED_ROTS.put(player, new Float[]{
                        player.yBodyRot, player.yBodyRotO,
                        player.yHeadRot, player.yHeadRotO,
                        player.getXRot(), player.xRotO,
                        player.getYRot(), player.yRotO
                });
                player.yBodyRot = 0; player.yBodyRotO = 0;
                player.yHeadRot = 0; player.yHeadRotO = 0;
                player.setXRot(0); player.xRotO = 0;
                player.setYRot(0); player.yRotO = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        // ... (保持不变的解冻逻辑，一定要把数据还给原版引擎喵！)
        Player player = event.getEntity();
        if (SAVED_ROTS.containsKey(player)) {
            Float[] rots = SAVED_ROTS.get(player);
            player.yBodyRot = rots[0]; player.yBodyRotO = rots[1];
            player.yHeadRot = rots[2]; player.yHeadRotO = rots[3];
            player.setXRot(rots[4]); player.xRotO = rots[5];
            player.setYRot(rots[6]); player.yRotO = rots[7];
            SAVED_ROTS.remove(player);
        }
    }
}*/



/*角度正确，但是四肢脱臼
package cn.rbq108.test.rendering;

import cn.rbq108.test.main;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Quaternionf;

import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class OtherPlayer {

    private static final WeakHashMap<Player, Float[]> SAVED_ROTS = new WeakHashMap<>();
    private static final WeakHashMap<UUID, Quaternionf> SMOOTH_HEAD_QUATS = new WeakHashMap<>();

    // 🩺 重点：分别记录当前 Tick 和上一 Tick 的纯净身体角度
    private static final WeakHashMap<UUID, Float> CUSTOM_BODY_YAWS = new WeakHashMap<>();
    private static final WeakHashMap<UUID, Float> CUSTOM_BODY_YAWS_O = new WeakHashMap<>();

    public static final ConcurrentHashMap<UUID, Float> BODY_TWIST_MAP = new ConcurrentHashMap<>();

    // 💥 魔法 1：在稳定的 20Hz Tick 中计算身体延迟，彻底干掉抽搐！
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        // 只给别人的号算延迟
        if (player.level().isClientSide() && player != Minecraft.getInstance().player) {
            UUID id = player.getUUID();
            if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
                float targetHeadYaw = player.yHeadRot;
                float currentYaw = CUSTOM_BODY_YAWS.getOrDefault(id, targetHeadYaw);

                // 把现在的角度存为“上一步 (O)”
                CUSTOM_BODY_YAWS_O.put(id, currentYaw);
                // 身体慢慢向脑袋追赶 (0.15f 是身体的追踪速度，可微调)
                CUSTOM_BODY_YAWS.put(id, Mth.rotLerp(0.15f, currentYaw, targetHeadYaw));
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player == Minecraft.getInstance().player) return;

        UUID id = player.getUUID();
        if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
            Quaternionf targetQuat = NetworkHandler.REMOTE_ROTATIONS.get(id);

            if (targetQuat != null) {
                // 脑袋继续保持 Slerp 丝滑
                Quaternionf headQuat = SMOOTH_HEAD_QUATS.computeIfAbsent(id, k -> new Quaternionf(targetQuat));
                headQuat.slerp(targetQuat, 0.4f);
                event.getPoseStack().mulPose(headQuat);

                // 💥 魔法 2：渲染时使用 PartialTick (帧间隔时间) 进行完美平滑！
                float pt = event.getPartialTick(); // 如果 1.21.1 报错，请改成 Minecraft.getInstance().getFrameTime()
                float prevBody = CUSTOM_BODY_YAWS_O.getOrDefault(id, player.yHeadRotO);
                float currBody = CUSTOM_BODY_YAWS.getOrDefault(id, player.yHeadRot);

                float smoothBody = Mth.rotLerp(pt, prevBody, currBody);
                float smoothHead = Mth.rotLerp(pt, player.yHeadRotO, player.yHeadRot);

                // 计算给 Mixin 的差值
                float twist = smoothBody - smoothHead;
                BODY_TWIST_MAP.put(id, twist);

                // 冷冻原版旋转
                SAVED_ROTS.put(player, new Float[]{
                        player.yBodyRot, player.yBodyRotO, player.yHeadRot, player.yHeadRotO,
                        player.getXRot(), player.xRotO, player.getYRot(), player.yRotO
                });

                player.yBodyRot = 0; player.yBodyRotO = 0;
                player.yHeadRot = 0; player.yHeadRotO = 0;
                player.setXRot(0); player.xRotO = 0;
                player.setYRot(0); player.yRotO = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        // ... (保持不变的解冻逻辑)
        Player player = event.getEntity();
        if (SAVED_ROTS.containsKey(player)) {
            Float[] rots = SAVED_ROTS.get(player);
            player.yBodyRot = rots[0]; player.yBodyRotO = rots[1];
            player.yHeadRot = rots[2]; player.yHeadRotO = rots[3];
            player.setXRot(rots[4]); player.xRotO = rots[5];
            player.setYRot(rots[6]); player.yRotO = rots[7];
            SAVED_ROTS.remove(player);
        }
    }
}*/


/*全是bug吐了
import cn.rbq108.test.main;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Quaternionf;

import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class OtherPlayer {

    // 1. 原版数据的冷冻柜 (防止 180度变360度 的双倍旋转Bug)
    private static final WeakHashMap<Player, Float[]> SAVED_ROTS = new WeakHashMap<>();

    // 2. 脑袋的平滑插值库 (解决 20帧 卡顿)
    private static final WeakHashMap<UUID, Quaternionf> SMOOTH_HEAD_QUATS = new WeakHashMap<>();

    // 3. 我们自己手搓的“纯净身体偏航角” (彻底干掉 WASD 平移扭曲)
    private static final WeakHashMap<UUID, Float> CUSTOM_BODY_YAWS = new WeakHashMap<>();

    // 4. 给 Mixin 传递的最终身体扭曲差值
    public static final ConcurrentHashMap<UUID, Float> BODY_TWIST_MAP = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player == Minecraft.getInstance().player) return;

        UUID id = player.getUUID();
        if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
            Quaternionf targetQuat = NetworkHandler.REMOTE_ROTATIONS.get(id);

            if (targetQuat != null) {
                // 💥 魔法1：脑袋 Slerp 插值（0.4f 代表快速且丝滑地跟手，告别20帧！）
                Quaternionf headQuat = SMOOTH_HEAD_QUATS.computeIfAbsent(id, k -> new Quaternionf(targetQuat));
                headQuat.slerp(targetQuat, 0.4f);
                event.getPoseStack().mulPose(headQuat); // PoseStack 代表脑袋的绝对方向

                // 💥 魔法2：手搓纯净身体延迟
                float realHeadYaw = player.yHeadRot; // 拿原版同步过来的脑袋方向作参考
                float currentBodyYaw = CUSTOM_BODY_YAWS.computeIfAbsent(id, k -> realHeadYaw);

                // Mth.rotLerp 可以完美处理 -180 到 180 度的跨界平滑
                // 0.1f 代表身体追踪速度很慢，完美还原“身子延迟一点点才跟过去”的手感！
                currentBodyYaw = Mth.rotLerp(0.1f, currentBodyYaw, realHeadYaw);
                CUSTOM_BODY_YAWS.put(id, currentBodyYaw);

                // 计算差值并存入黑板，让 Mixin 去取
                float twist = currentBodyYaw - realHeadYaw;
                BODY_TWIST_MAP.put(id, twist);

                // 💥 魔法3：冷冻柜，干掉原版所有乱七八糟的二次旋转
                SAVED_ROTS.put(player, new Float[]{
                        player.yBodyRot, player.yBodyRotO,
                        player.yHeadRot, player.yHeadRotO,
                        player.getXRot(), player.xRotO,
                        player.getYRot(), player.yRotO
                });

                player.yBodyRot = 0; player.yBodyRotO = 0;
                player.yHeadRot = 0; player.yHeadRotO = 0;
                player.setXRot(0); player.xRotO = 0;
                player.setYRot(0); player.yRotO = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (SAVED_ROTS.containsKey(player)) {
            Float[] rots = SAVED_ROTS.get(player);
            player.yBodyRot = rots[0]; player.yBodyRotO = rots[1];
            player.yHeadRot = rots[2]; player.yHeadRotO = rots[3];
            player.setXRot(rots[4]); player.xRotO = rots[5];
            player.setYRot(rots[6]); player.yRotO = rots[7];
            SAVED_ROTS.remove(player);
        }
    }
}*/

/*身体没有做独立旋转，铁板一块看着难受

package cn.rbq108.test.rendering;

import cn.rbq108.test.main;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Quaternionf;

import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class OtherPlayer {

    // 原版数据的冷冻柜 (防止原版二次旋转)
    private static final WeakHashMap<Player, Float[]> SAVED_ROTS = new WeakHashMap<>();

    // 🩺 新增：客户端平滑插值记忆库！记录每个玩家当前屏幕上实际显示的角度
    private static final WeakHashMap<UUID, Quaternionf> SMOOTH_QUATS = new WeakHashMap<>();

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player == Minecraft.getInstance().player) return;

        UUID id = player.getUUID();
        if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {

            // 🎯 这是服务器发来的“目标角度”（每秒更新 20 次）
            Quaternionf targetQuat = NetworkHandler.REMOTE_ROTATIONS.get(id);

            if (targetQuat != null) {
                // 🖌️ 获取屏幕上正在画的“当前角度”，如果没有就克隆一个目标角度
                Quaternionf currentSmoothQuat = SMOOTH_QUATS.computeIfAbsent(id, k -> new Quaternionf(targetQuat));

                // 💥 核心魔法：Slerp (Spherical Linear Interpolation) 球面线性插值！
                // 这行代码会在 Render 线程（每秒 60~144 次）里执行。
                // 0.2f 是平滑系数：1.0 是瞬间过去，0.0 是定死不动。
                // 0.2f 意味着每一帧它都会向目标追赶 20% 的差距。
                // 这个操作不仅完全不吃网络带宽，还会让整个旋转充满“沉重的惯性”，彻底消灭 20fps 的卡顿感喵！！
                currentSmoothQuat.slerp(targetQuat, 0.2f);

                // 叠加我们丝滑插值后的 6DOF 姿态
                event.getPoseStack().mulPose(currentSmoothQuat);

                // --- 下面是之前的冷冻柜逻辑（保持不变） ---
                SAVED_ROTS.put(player, new Float[]{
                        player.yBodyRot, player.yBodyRotO,
                        player.yHeadRot, player.yHeadRotO,
                        player.getXRot(), player.xRotO,
                        player.getYRot(), player.yRotO
                });
                player.yBodyRot = 0; player.yBodyRotO = 0;
                player.yHeadRot = 0; player.yHeadRotO = 0;
                player.setXRot(0); player.xRotO = 0;
                player.setYRot(0); player.yRotO = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        // ... (保持之前的解冻逻辑完全不变) ...
        Player player = event.getEntity();
        if (SAVED_ROTS.containsKey(player)) {
            Float[] rots = SAVED_ROTS.get(player);
            player.yBodyRot = rots[0]; player.yBodyRotO = rots[1];
            player.yHeadRot = rots[2]; player.yHeadRotO = rots[3];
            player.setXRot(rots[4]); player.xRotO = rots[5];
            player.setYRot(rots[6]); player.yRotO = rots[7];
            SAVED_ROTS.remove(player);
        }
    }
}*/

/*成功了，但是还没有平滑效果

package cn.rbq108.test.rendering;

import cn.rbq108.test.main;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Quaternionf;

import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class OtherPlayer {

    // 🩺 冷冻柜重出江湖！这次是为了彻底屏蔽原版的“二次旋转”
    private static final WeakHashMap<Player, Float[]> SAVED_ROTS = new WeakHashMap<>();

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();

        // 放过自己人！
        if (player == Minecraft.getInstance().player) return;

        UUID id = player.getUUID();
        if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
            Quaternionf remoteQuat = NetworkHandler.REMOTE_ROTATIONS.get(id);

            if (remoteQuat != null) {
                // 💥 1. 叠加我们的绝对 6DOF 姿态
                event.getPoseStack().mulPose(remoteQuat);

                // 💥 2. 核心止痛药：把原版的旋转数据锁起来！
                // 把原版服务器发来的偏航、俯仰角全部存起来，然后设为 0。
                // 这样原版引擎就会以为我们要画一个“没有任何角度偏移”的史蒂夫，
                // 它就不会再去傻傻地乘以第二遍旋转了喵！
                SAVED_ROTS.put(player, new Float[]{
                        player.yBodyRot, player.yBodyRotO,
                        player.yHeadRot, player.yHeadRotO,
                        player.getXRot(), player.xRotO,
                        player.getYRot(), player.yRotO // 包含总偏航角
                });

                // 全部归零，让原版引擎闭嘴！
                player.yBodyRot = 0;
                player.yBodyRotO = 0;
                player.yHeadRot = 0;
                player.yHeadRotO = 0;
                player.setXRot(0);
                player.xRotO = 0;
                player.setYRot(0);
                player.yRotO = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();

        // 🩺 画完之后，把数据还给实体，防止服务器判定防作弊报错
        if (SAVED_ROTS.containsKey(player)) {
            Float[] rots = SAVED_ROTS.get(player);
            player.yBodyRot = rots[0];
            player.yBodyRotO = rots[1];
            player.yHeadRot = rots[2];
            player.yHeadRotO = rots[3];
            player.setXRot(rots[4]);
            player.xRotO = rots[5];
            player.setYRot(rots[6]);
            player.yRotO = rots[7];

            SAVED_ROTS.remove(player);
        }
    }
}*/






/*下面这一坨是用坏的mixin
package cn.rbq108.test.rendering;

import cn.rbq108.test.main;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Quaternionf;
import java.util.UUID;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class OtherPlayer {

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();

        if (player == Minecraft.getInstance().player) return;

        UUID id = player.getUUID();
        if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
            Quaternionf remoteQuat = NetworkHandler.REMOTE_ROTATIONS.get(id);

            if (remoteQuat != null) {
                // 💥 现在这里只负责把整个人“连根拔起”拧到正确的 6DOF 角度
                // 里面的骨骼微调（头身分离）已经交给 Mixin 啦！
                event.getPoseStack().mulPose(remoteQuat);
            }
        }
    }
}*/


//以下是废弃掉的原版其他玩家渲染机制，现已改为mixin强行接管

/*package cn.rbq108.test.rendering;

import cn.rbq108.test.main;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Quaternionf;

import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class OtherPlayer {

    // 🩺 原版旋转数据的“冷冻柜”
    private static final WeakHashMap<Player, Float[]> SAVED_ROTS = new WeakHashMap<>();

    // 🎨 画画前：打麻药，剥离原版身体旋转
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();

        // 依然是：放过自己人！
        if (player == Minecraft.getInstance().player) {
            return;
        }

        UUID id = player.getUUID();
        if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
            Quaternionf remoteQuat = NetworkHandler.REMOTE_ROTATIONS.get(id);

            if (remoteQuat != null) {
                // 1. 挂载我们同步过来的 6DOF 绝对姿态
                event.getPoseStack().mulPose(remoteQuat);

                // 2. 提取原版的旋转数据
                float bodyYaw = player.yBodyRot;
                float bodyYawO = player.yBodyRotO;
                float headYaw = player.yHeadRot;
                float headYawO = player.yHeadRotO;
                float pitch = player.getXRot();
                float pitchO = player.xRotO;

                // 3. 把原版数据锁进冷冻柜 (新增了上下看 pitch 的保存)
                SAVED_ROTS.put(player, new Float[]{bodyYaw, bodyYawO, headYaw, headYawO, pitch, pitchO});

                // 💥 4. 终极反转：躯干延迟，脑袋瞬间！
                // 因为 remoteQuat 是瞬间跟随鼠标的，所以我们让躯干往反方向“退”一个差值
                // 这样躯干就会出现原版那种“慢慢跟过来”的延迟感！
                player.yBodyRot = bodyYaw - headYaw;
                player.yBodyRotO = bodyYawO - headYawO;

                // 脑袋保持 0 偏移，让它绝对、瞬间地服从 remoteQuat！
                player.yHeadRot = 0;
                player.yHeadRotO = 0;

                // 🩺 防双倍叠加：因为 remoteQuat 已经包含了上下看（Pitch）
                // 我们必须把原版的 Pitch 归零，否则低头时脑袋会往下折两次喵！
                player.setXRot(0);
                player.xRotO = 0;

                /*float bodyYaw = player.yBodyRot;
                float bodyYawO = player.yBodyRotO;
                float headYaw = player.yHeadRot;
                float headYawO = player.yHeadRotO;

                // 3. 把原版数据锁进冷冻柜，防止丢失导致服务器碰撞箱判定错乱
                SAVED_ROTS.put(player, new Float[]{bodyYaw, bodyYawO, headYaw, headYawO});

                // 💥 4. 终极正骨法：强制锁死身体！
                // 把身体的原版偏航角归零，让躯干绝对服从咱们的 remoteQuat！
                player.yBodyRot = 0;
                player.yBodyRotO = 0;

                // 让脑袋只进行“相对身体”的转动，恢复您想要的“头身分离”效果！
                player.yHeadRot = headYaw - bodyYaw;
                player.yHeadRotO = headYawO - bodyYawO;
            }
        }
    }

  //把数据还给原版引擎
    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();

        // 如果冷冻柜里有这个玩家的数据，就把原版数据塞回他体内
        if (SAVED_ROTS.containsKey(player)) {
            Float[] rots = SAVED_ROTS.get(player);
            player.yBodyRot = rots[0];
            player.yBodyRotO = rots[1];
            player.yHeadRot = rots[2];
            player.yHeadRotO = rots[3];
            player.setXRot(rots[4]);
            player.xRotO = rots[5];

            Float[] rots = SAVED_ROTS.get(player);
            player.yBodyRot = rots[0];
            player.yBodyRotO = rots[1];
            player.yHeadRot = rots[2];
            player.yHeadRotO = rots[3];

            // 清理冷冻柜
            SAVED_ROTS.remove(player);
        }
    }
}*/