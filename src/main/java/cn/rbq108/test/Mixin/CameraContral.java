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

    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    private void doABarrelRoll$interpolateRollnt(CallbackInfo ci) {
        if (this.entity == null) {
            return;
        }

        if (!((RollEntity) this.entity).doABarrelRoll$isRolling()) {
            lastRollBack = rollBack;
            rollBack -= rollBack * 0.5f;
        }
    }

    @Inject(
            method = "setup", // 或者 "update"，根据你的环境
            at = @At("HEAD")
    )
    private void doABarrelRoll$captureTickDeltaAndUpdate(BlockGetter area, Entity entity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci, @Share("tickDelta") LocalFloatRef tickDeltaRef) {
        tickDeltaRef.set(tickDelta);

        // 👇 增加一个 instanceof 安全检查
        if (this.entity instanceof RollEntity rollEntity) {
            this.isRolling = rollEntity.doABarrelRoll$isRolling();
        } else {
            this.isRolling = false;
        }
    }

    // 这个注入也是正确的，无需修改
    @Inject(
            method = "setup", // 同上，确认你的环境里的方法名是 update 还是 setup
            at = @At("TAIL")
    )
    private void doABarrelRoll$updateRollBack(BlockGetter area, Entity entity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (isRolling) {
            rollBack = roll;
            lastRollBack = roll;
        }
    }
    @ModifyArg(
            method = "setup",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;setRotation(FFF)V", // 删除了 "render/"
                    ordinal = 0
            ),
            index = 2
    )
    private float doABarrelRoll$addRoll2(float original, @Share("tickDelta") LocalFloatRef tickDelta) {
        if (isRolling) {
            return original + ((RollEntity) entity).doABarrelRoll$getRoll(tickDelta.get());
        } else {
            return original + Mth.lerp(tickDelta.get(), lastRollBack, rollBack);
        }
    }

    @ModifyArg(
            method = "setup",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;setRotation(FFF)V",
                    ordinal = 1
            ),
            index = 2
    )
    private float doABarrelRoll$addRoll3(float original, @Share("tickDelta") LocalFloatRef tickDelta) {
        if (isRolling) {
            return original - ((RollEntity) entity).doABarrelRoll$getRoll(tickDelta.get());
        } else {
            return original - Mth.lerp(tickDelta.get(), lastRollBack, rollBack);
        }
    }

    @Override
    public float doABarrelRoll$getRoll() {
        return roll;
    }
}