package cn.rbq108.test.Mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import cn.rbq108.test.api.RollEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityMixin implements RollEntity {

    @Unique
    private boolean isRolling_testMod;
    @Unique
    private float roll_testMod;
    @Unique
    private float customYaw_testMod;
    @Unique
    private float customPitch_testMod;

    @Override
    public boolean doABarrelRoll$isRolling() {
        isRolling_testMod = true;
        return this.isRolling_testMod;
    }

    @Override
    public float doABarrelRoll$getRoll(float tickDelta) {
        if (this.isRolling_testMod) {
            return cn.rbq108.test.camera.CameraManager.prevRoll +
                    (cn.rbq108.test.camera.CameraManager.currentRoll - cn.rbq108.test.camera.CameraManager.prevRoll) * tickDelta;
        }
        return 0.0f;
    }

    @Override
    public float doABarrelRoll$getYaw(float tickDelta) {
        return 0.0f;
    }

    @Override
    public float doABarrelRoll$getPitch(float tickDelta) {
        return 0.0f;
    }

    @Inject(method = "turn(DD)V", at = @At("HEAD"), cancellable = true)
    private void doABarrelRoll$quaternionTurn(double yRot, double xRot, CallbackInfo ci) {
        if (cn.rbq108.test.VariableLibrary.GlobalVariables.B_LowGravity && (Object)this instanceof Player player) {
            ci.cancel();

            // 提取鼠标输入的增量
            float dy = (float) yRot * 0.15f; // 鼠标横向移动 (控制 Yaw)
            float dx = (float) xRot * 0.15f; // 鼠标纵向移动 (控制 Pitch)

            // 1. 神医修正版：绕 X 轴是俯仰(dx)，绕 Y 轴是偏航(dy)
            cn.rbq108.test.VariableLibrary.GlobalVariables.currentQuat.rotateX((float) Math.toRadians(dx));
            cn.rbq108.test.VariableLibrary.GlobalVariables.currentQuat.rotateY((float) Math.toRadians(-dy));

            // 2. 提取欧拉角时，各回各家！
            org.joml.Vector3f euler = cn.rbq108.test.VariableLibrary.GlobalVariables.currentQuat.getEulerAnglesYXZ(new org.joml.Vector3f());

            // euler.y 对应 Yaw，euler.x 对应 Pitch！
            float newYaw = (float) Math.toDegrees(-euler.y);
            float newPitch = (float) Math.toDegrees(euler.x);

            float currentYaw = player.getYRot();
            float currentPitch = player.getXRot();
            player.setYRot(currentYaw + net.minecraft.util.Mth.wrapDegrees(newYaw - currentYaw));
            player.setXRot(currentPitch + net.minecraft.util.Mth.wrapDegrees(newPitch - currentPitch));
        }
    }
}