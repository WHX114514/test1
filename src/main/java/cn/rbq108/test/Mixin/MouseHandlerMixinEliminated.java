//显然，这个已经被废弃，当个坟墓做纪念叭
package cn.rbq108.test.Mixin;

import cn.rbq108.test.VariableLibrary.GlobalVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;



@Mixin(MouseHandler.class)
public class MouseHandlerMixinEliminated {

    @ModifyArgs(
            method = "turnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V")
    )
    private void doABarrelRoll$rotateMouseInputs(Args args) {
        // 当启用无重力操作时，拦截并旋转鼠标输入
        if (GlobalVariables.B_LowGravity) {
            double originalYRot = args.get(0); // 鼠标水平移动 -> 原 Yaw 变化量
            double originalXRot = args.get(1); // 鼠标垂直移动 -> 原 Pitch 变化量

            var player = Minecraft.getInstance().player;
            if (player != null) {
                // 获取玩家真实的 Pitch 并将其规范化到 [-180, 180] 之间
                float pitch = player.getXRot() % 360.0f;
                if (pitch > 180.0f) pitch -= 360.0f;
                if (pitch < -180.0f) pitch += 360.0f;

                // 核心修复：当 Pitch 超过 90 度或低于 -90 度时，玩家头朝下
                // 根据欧拉角特性，世界坐标系的左右与屏幕的左右脱节，必须将鼠标的偏航输入取反！
                if (Math.abs(pitch) > 90.0f) {
                    originalYRot = -originalYRot;
                }
            }

            // 将 Roll 角度 (B_Dz) 转换为弧度
            double rollRad = Math.toRadians(GlobalVariables.B_Dz);
            double cos = Math.cos(rollRad);
            double sin = Math.sin(rollRad);

            // 应用 2D 旋转矩阵，将屏幕坐标系的鼠标移动，完美映射到翻滚后的玩家本地坐标系
            double newYRot = originalYRot * cos - originalXRot * sin;
            double newXRot = originalXRot * cos + originalYRot * sin;

            args.set(0, newYRot);
            args.set(1, newXRot);
        }
    }
}