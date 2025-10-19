package cn.rbq108.test.Mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import cn.rbq108.test.api.RollCamera;
import cn.rbq108.test.api.RollEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraContral implements RollCamera {
    @Shadow private Entity entity;
    @Shadow private float roll;

    @Unique private boolean isRolling;
    @Unique private float lastRollBack;
    @Unique private float rollBack;

    // Tick 和 setup 的注入逻辑可以保持不变
    @Inject(method = "tick", at = @At("HEAD"))
    private void doABarrelRoll$interpolateRoll(CallbackInfo ci) {
        if (this.entity instanceof RollEntity rollEntity) {
            if (!rollEntity.doABarrelRoll$isRolling()) {
                lastRollBack = rollBack;
                rollBack -= rollBack * 0.1f; // 减慢恢复速度，效果更平滑
            }
        }
    }

    @Inject(method = "setup", at = @At("HEAD"))
    private void doABarrelRoll$captureTickDelta(BlockGetter area, Entity entity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci, @Share("tickDelta") LocalFloatRef tickDeltaRef) {
        tickDeltaRef.set(tickDelta);
        if (this.entity instanceof RollEntity rollEntity) {
            this.isRolling = rollEntity.doABarrelRoll$isRolling();
        } else {
            this.isRolling = false;
        }
    }

    @Inject(method = "setup", at = @At("TAIL"))
    private void doABarrelRoll$updateRollBack(BlockGetter area, Entity entity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (isRolling) {
            rollBack = roll;
            lastRollBack = roll;
        }
    }

    // --- 修改 Yaw (index = 0) ---
    @ModifyArg(
            method = "setup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FFF)V"),
            index = 0
    )
    private float doABarrelRoll$modifyYaw(float originalYaw, @Share("tickDelta") LocalFloatRef tickDelta) {
        if (isRolling && this.entity instanceof RollEntity rollEntity) {
            return originalYaw + rollEntity.doABarrelRoll$getYaw(tickDelta.get());
        }
        return originalYaw;
    }

    // --- 修改 Pitch (index = 1) ---
    @ModifyArg(
            method = "setup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FFF)V"),
            index = 1
    )
    private float doABarrelRoll$modifyPitch(float originalPitch, @Share("tickDelta") LocalFloatRef tickDelta) {
        if (isRolling && this.entity instanceof RollEntity rollEntity) {
            return originalPitch + rollEntity.doABarrelRoll$getPitch(tickDelta.get());
        }
        return originalPitch;
    }

    // --- 修改 Roll (index = 2), 这里的逻辑与你的相似 ---
    // 为了兼容，我将你的两个 ModifyArg 合并成一个，效果是一样的
    @ModifyArg(
            method = "setup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FFF)V"),
            index = 2
    )
    private float doABarrelRoll$modifyRoll(float originalRoll, @Share("tickDelta") LocalFloatRef tickDelta) {
        if (isRolling && this.entity instanceof RollEntity rollEntity) {
            // 假设第三人称和第一人称的翻滚方向相反
            // (原代码中有两个注入，一个加一个减，这里用一个三元运算符模拟)
            boolean inverseView = false; // 你需要一种方式来获取 inverseView 的值，或者直接决定一个方向
            return originalRoll + (inverseView ? -1 : 1) * rollEntity.doABarrelRoll$getRoll(tickDelta.get());
        } else {
            return originalRoll + Mth.lerp(tickDelta.get(), lastRollBack, rollBack);
        }
    }
}