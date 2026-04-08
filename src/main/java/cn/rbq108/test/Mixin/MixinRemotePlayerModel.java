package cn.rbq108.test.Mixin;

import cn.rbq108.test.rendering.OtherPlayer;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(HumanoidModel.class)
public abstract class MixinRemotePlayerModel<T extends LivingEntity> {

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void onSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof Player player) {
            // 依然是：放过自己人喵！
            if (player == Minecraft.getInstance().player) return;

            if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(player.getUUID(), false)) {
                HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;

                // 💥 1. 安全获取数据，绝对不许报错！
                UUID uuid = player.getUUID();
                // 使用 getOrDefault 确保即使柜子里没东西也只会返回 null，而不是掀桌子
                OtherPlayer.RotationData data = OtherPlayer.ROTATION_SYNC_MAP.getOrDefault(uuid, null);

                // 💥 2. 只有数据完全齐活了（Data、身体四元数、脑袋四元数都不为空）才动手
                if (data != null && data.bodyQuatRender != null && data.headQuatRender != null) {
                    // 💥 3. 纯四元数计算相对差值
                    Quaternionf headTwist = new Quaternionf(data.bodyQuatRender).conjugate().mul(data.headQuatRender);
                    headTwist.normalize();

                    // 走最短路径，防止出现大于 180 度的诡异翻折
                    if (headTwist.w < 0.0f) {
                        headTwist.x = -headTwist.x;
                        headTwist.y = -headTwist.y;
                        headTwist.z = -headTwist.z;
                        headTwist.w = -headTwist.w;
                    }

                    // 💥 4. 纯四元数物理限制（“颈椎保护锁”算法）！
                    float maxW = 0.737277f;
                    if (headTwist.w < maxW) {
                        float currentSin = (float) Math.sqrt(1.0 - headTwist.w * headTwist.w);
                        float targetSin = (float) Math.sqrt(1.0 - maxW * maxW);
                        float scale = targetSin / currentSin;
                        headTwist.x *= scale;
                        headTwist.y *= scale;
                        headTwist.z *= scale;
                        headTwist.w = maxW; // 锁死最大旋转角
                    }

                    // 💥 5. 安全提取欧拉角
                    Vector3f euler = headTwist.getEulerAnglesZYX(new Vector3f());

                    // 💥 6. 赋值！！！（保留您调试出来的正确正负号）
                    model.head.xRot = euler.x;
                    model.head.yRot = -euler.y;
                    model.head.zRot = -euler.z;

                    model.hat.xRot = model.head.xRot;
                    model.hat.yRot = model.head.yRot;
                    model.hat.zRot = model.head.zRot;

                    // 躯干和四肢依然保持 0（因为外面 PoseStack 已经完美平滑转过去了）
                    model.body.yRot = 0;
                    model.leftArm.yRot = 0; model.rightArm.yRot = 0;
                    model.leftLeg.yRot = 0; model.rightLeg.yRot = 0;
                }

                /*这坨角度过大时还会有问题
                if (data != null) {

                    // 💥 1. 计算相对差值四元数（这步没毛病，保留）
                    Quaternionf headTwist = new Quaternionf(data.bodyQuatRender).conjugate().mul(data.headQuatRender);

                    // 💥 2. 暴力破解开始！我们在相对空间里，取一个指向正前方 (0, 0, 1) 的“鼻子”向量
                    Vector3f nose = new Vector3f(0, 0, 1);

                    // 把鼻子向量用我们的旋转差值转一下
                    nose.rotate(headTwist);

                    // 💥 3. 用最基础的三角函数，直接算出绝对的 Yaw (左右) 和 Pitch (上下)！
                    // Math.atan2 支持完美的 360 度圆周，绝对、绝对、绝对不会出现 180 度断脖子现象！
                    float yaw = (float) Math.atan2(nose.x, nose.z);
                    float pitch = (float) Math.asin(-nose.y);

                    // 💥 4. 赋值！
                    // 这里您继续用您刚才自己摸索出来的正负号！(如果上下左右反了，加个负号就行)
                    model.head.xRot = pitch;   // 比如您之前如果用了 -pitch 就继续用
                    model.head.yRot = yaw;     // 同上

                    // 💥 核心保护：彻底锁死 Z 轴 (Roll) 为 0！神仙来了这脑袋也折不断了喵！
                    model.head.zRot = 0;

                    model.hat.xRot = model.head.xRot;
                    model.hat.yRot = model.head.yRot;
                    model.hat.zRot = model.head.zRot;

                    // 保持躯干和四肢为 0，因为外面 PoseStack 已经整体平滑旋转过了
                    model.body.yRot = 0;
                    model.leftArm.yRot = 0; model.rightArm.yRot = 0;
                    model.leftLeg.yRot = 0; model.rightLeg.yRot = 0;
                }

                /*这坨角度过大时会有问题
                if (data != null) {
                    // 💥 1. 计算相对差值四元数
                    // 注意变量名：headTwist
                    Quaternionf headTwist = new Quaternionf(data.bodyQuatRender).conjugate().mul(data.headQuatRender);

                    // 💥 2. 核心修正：使用 ZYX 顺序提取，且变量名必须统一！
                    // 刚才就是这里的变量名和上面对不上导致报错喵！
                    Vector3f euler = headTwist.getEulerAnglesZYX(new Vector3f());

                    // 💥 ✅ 终极驱魔提取法：改成 YXZ 顺序！
                    // 在 YXZ 顺序下，受限制不超过 90 度的变成了 X (上下低头)，
                    // 而 Y (左右扭头) 获得了完整的 +-180 度解放！再怎么快转也不会折断脖子了喵！
                    //Vector3f euler = headTwist.getEulerAnglesYXZ(new Vector3f());

                    // 💥 3. 应用角度（ZYX 顺序下，这就是最纯净的对应关系喵！）
                    model.head.xRot = (euler.x);
                    model.head.yRot = -(euler.y);
                    model.head.zRot = -(euler.z);
                    // 重点：尝试调整这里的正负号，解决您看到的“反转”
                    // 如果 Pitch（上下）反了，就去掉 x 前面的负号
                    // 如果 Roll（翻滚）反了，就给 z 加上负号

                    model.hat.xRot = euler.x;
                    model.hat.yRot = euler.y;
                    model.hat.zRot = euler.z;

                    // 保持躯干和四肢为 0，因为外面 PoseStack 已经整体平滑旋转过了喵
                    model.body.yRot = 0;
                    model.leftArm.yRot = 0; model.rightArm.yRot = 0;
                    model.leftLeg.yRot = 0; model.rightLeg.yRot = 0;
                }*/
            }
        }
    }
}

/*头反了！
package cn.rbq108.test.Mixin;

import cn.rbq108.test.rendering.OtherPlayer;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class MixinRemotePlayerModel<T extends LivingEntity> {

    private static final Logger log = LoggerFactory.getLogger(MixinRemotePlayerModel.class);

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void onSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof Player player) {
            if (player == Minecraft.getInstance().player) return;

            if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(player.getUUID(), false)) {
                HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;

                // 1. 获取黑板上的平滑姿态数据
                OtherPlayer.RotationData data = OtherPlayer.ROTATION_SYNC_MAP.get(player.getUUID());
                if (data != null) {
                    // 💥 2. 数学手术：计算脑袋相对于身体的差值四元数！
                    // 我们要算的是：怎么把那个笔直的身体，拧到那个瞬间跟手的脑袋方向去喵！
                    // 计算公式：inverse(smoothBodyQuat) * headQuatNet
                    Quaternionf headTwistQuat = new Quaternionf(data.bodyQuatRender).conjugate().mul(data.headQuatRender);

                    Vector3f euler = headTwistQuat.getEulerAnglesXYZ(new Vector3f());

                    // 重点：尝试调整这里的正负号，解决您看到的“反转”
                    // 如果 Pitch（上下）反了，就去掉 x 前面的负号
                    // 如果 Roll（翻滚）反了，就给 z 加上负号
                    model.head.xRot = euler.x;
                    model.head.yRot = -euler.y;
                    model.head.zRot = -euler.z;


                    model.hat.xRot = model.head.xRot;
                    model.hat.yRot = model.head.yRot;
                    model.hat.zRot = model.head.zRot;


                    // 保持躯干和四肢为 0，因为外面 PoseStack 已经转过了
                    model.body.yRot = 0;
                    model.leftArm.yRot = 0; model.rightArm.yRot = 0;
                    model.leftLeg.yRot = 0; model.rightLeg.yRot = 0;



                    // 💥 3. 将差值四元数转换为 Euler 角度 (Yaw, Pitch, Roll) 赋给脑袋模型喵！
                    // 乘以 -1 是因为 Minecraft 渲染模型的 X 和 Y 轴是反着的，为了让它动起来自然喵！
                    Vector3f eulerAngles = headTwistQuat.getEulerAnglesXYZ(new Vector3f());

                    // 瞬间、瞬间对齐脑袋！！♡
                    model.head.xRot = -eulerAngles.x; // pitch (低头抬头)
                    model.head.yRot = -eulerAngles.y; // yaw (扭头左看右看)
                    model.head.zRot = eulerAngles.z; // roll (虽然人形模型不支持，但是我们尽力了喵！)
                    model.hat.xRot = -eulerAngles.x; model.hat.yRot = -eulerAngles.y; model.hat.zRot = eulerAngles.z;

                    // 💥 4. 重点：四肢和躯干必须归零！！
                    // 因为外面 PoseStack 已经被整体插值平滑化了，所以四肢不需要额外的扭曲，
                    // 设为 0 能保证身体和四肢保持绝对笔直、整体跟上班的完美 hierarchical 姿态喵！！♡
                    model.body.yRot = 0;
                    model.leftArm.yRot = 0; model.rightArm.yRot = 0;
                    model.leftLeg.yRot = 0; model.rightLeg.yRot = 0;
                }
            }
        }
    }
}*/

/*角度正确但是四肢脱臼
package cn.rbq108.test.Mixin;

import cn.rbq108.test.rendering.OtherPlayer;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class MixinRemotePlayerModel<T extends LivingEntity> {

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void onSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof Player player) {
            if (player == Minecraft.getInstance().player) return;

            if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(player.getUUID(), false)) {
                HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;

                // 1. 脑袋瞬间跟手
                model.head.yRot = 0;
                model.head.xRot = 0;
                model.hat.yRot = 0;
                model.hat.xRot = 0;

                Float twist = OtherPlayer.BODY_TWIST_MAP.get(player.getUUID());
                if (twist != null) {
                    float radTwist = twist * 0.017453292F;

                    // 💥 2. 给躯干加上延迟扭曲
                    model.body.yRot = radTwist;

                    // 💥 3. 给四肢也加上相同的扭曲！
                    // 注意是 += ，这样他们原本走路前后摆动的动画就不会被覆盖喵！
                    model.leftArm.yRot += radTwist;
                    model.rightArm.yRot += radTwist;
                    model.leftLeg.yRot += radTwist;
                    model.rightLeg.yRot += radTwist;
                }
            }
        }
    }
}
*/


/*
这个版本四肢是抽搐的
package cn.rbq108.test.Mixin;

import cn.rbq108.test.rendering.OtherPlayer;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class MixinRemotePlayerModel<T extends LivingEntity> {

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void onSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof Player player) {
            if (player == Minecraft.getInstance().player) return;

            if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(player.getUUID(), false)) {
                HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;

                // 1. 脑袋归零！让脑袋绝对、瞬间服从 PoseStack (已经被 Slerp 丝滑化了)
                model.head.yRot = 0;
                model.head.xRot = 0;
                model.hat.yRot = 0;
                model.hat.xRot = 0;

                // 2. 身体读取我们手搓的“纯净扭曲值”！
                Float twist = OtherPlayer.BODY_TWIST_MAP.get(player.getUUID());
                if (twist != null) {
                    // 把角度转为弧度赋给身体
                    model.body.yRot = twist * 0.017453292F;
                }
            }
        }
    }
}*/


/*以下是没适配平滑版本otherplayer的版本，为了适配平滑的otherplayer弃用
package cn.rbq108.test.Mixin;


import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 🩺 直接劫持所有类人模型（包括玩家和盔甲）
@Mixin(HumanoidModel.class)
public abstract class MixinRemotePlayerModel<T extends LivingEntity> {

    /**
     * 🎨 在原版骨骼动画设置完毕后（TAIL），咱们来做最后的骨骼矫正

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void onSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {

        if (entity instanceof Player player) {
            // 1. 放过自己人！本地玩家的逻辑由您当初的单机 Mixin 负责喵
            if (player == Minecraft.getInstance().player) {
                return;
            }

            // 2. 查水表：这个其他玩家是不是在失重状态下？
            if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(player.getUUID(), false)) {

                // 拿到当前正在渲染的模型骨架
                HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;

                // 💥 3. 终极骨骼覆盖术！
                // 因为外面 PoseStack 已经被 remoteQuat 整体旋转过了，
                // remoteQuat 代表了鼠标/脑袋的瞬间指向，所以整个模型此时已经对齐了脑袋。

                float bodyYaw = player.yBodyRot;
                float headYaw = player.yHeadRot;

                // 脑袋保持 0 偏移，让它绝对、瞬间地服从 remoteQuat（鼠标方向）
                // 💥 3. 终极骨骼覆盖术（无情阉割版）！
                // remoteQuat 已经代表了绝对的正确姿态

                // 脑袋保持 0 偏移
                model.head.yRot = 0;
                model.head.xRot = 0;
                model.hat.yRot = 0;
                model.hat.xRot = 0;

                // 💥 重点在这里！彻底干掉 bodyYaw！
                // 既然 yBodyRot 含有原版平移扭曲的毒素，咱们就让身体的扭曲直接归零！
                // 这样躯干就会像焊死在脑袋上一样，完美保持“太空人”的笔直姿态喵！
                model.body.yRot = 0;

                // (可选) 如果您发现原版平移时，披风或某些盔甲还在乱扭，可以顺手把它们也镇压了
                // model.leftArm.yRot = 0;
                // model.rightArm.yRot = 0;

                // 可选：如果觉得胸甲没跟上，把下面这行也取消注释
                // if (model instanceof PlayerModel pm) { pm.jacket.yRot = model.body.yRot; }
            }
        }
    }
}*/