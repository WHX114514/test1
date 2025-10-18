package cn.rbq108.test.Mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import nl.enjarai.doabarrelroll.api.RollCamera;
import nl.enjarai.doabarrelroll.api.RollEntity;
import nl.enjarai.doabarrelroll.math.MagicNumbers;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraContral implements RollCamera {
    @Shadow private Entity focusedEntity;

    @Unique
    private boolean isRolling;
    @Unique
    private float lastRollBack;
    @Unique
    private float rollBack;
    @Unique
    private float roll;
    @Unique
    private final ThreadLocal<Float> tempRoll = new ThreadLocal<>();

    @Inject(
            method = "update",
            at = @At("HEAD")
    )
    private void doABarrelRoll$captureTickDeltaAndUpdate(BlockGetter area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci, @Share("tickDelta") LocalFloatRef tickDeltaRef) {
        tickDeltaRef.set(tickDelta);
        isRolling = ((RollEntity) this.focusedEntity).doABarrelRoll$isRolling();

        if (!isRolling) {
            lastRollBack = rollBack;
            rollBack -= rollBack * 0.5f;
        }
    }

    @Inject(
            method = "update",
            at = @At("TAIL")
    )
    private void doABarrelRoll$updateRollBack(BlockGetter area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (isRolling) {
            rollBack = roll;
            lastRollBack = roll;
        }
    }

    @WrapWithCondition(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
                    ordinal = 0
            )
    )
    private boolean doABarrelRoll$addRoll1(Camera thiz, float yaw, float pitch, @Share("tickDelta") LocalFloatRef tickDelta) {
        if (isRolling) {
            tempRoll.set(-((RollEntity) focusedEntity).doABarrelRoll$getRoll(tickDelta.get()));
        } else {
            tempRoll.set(-Mth.lerp(tickDelta.get(), lastRollBack, rollBack));
        }
        return true;
    }

    @WrapWithCondition(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
                    ordinal = 1
            )
    )
    private boolean doABarrelRoll$addRoll2(Camera thiz, float yaw, float pitch) {
        tempRoll.set(-this.roll);
        return true;
    }

    @WrapWithCondition(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
                    ordinal = 2
            )
    )
    private boolean doABarrelRoll$addRoll3(Camera thiz, float yaw, float pitch) {
        tempRoll.set(0.0f);
        return true;
    }

    @ModifyArg(
            method = "setRotation",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;",
                    remap = false
            ),
            index = 2
    )
    private float doABarrelRoll$setRoll(float original) {
        var roll = tempRoll.get();
        if (roll != null) {
            this.roll = roll;
            return (float) (this.roll * MagicNumbers.TORAD);
        }
        return original;
    }

    @Override
    public float doABarrelRoll$getRoll() {
        return roll;
    }
}