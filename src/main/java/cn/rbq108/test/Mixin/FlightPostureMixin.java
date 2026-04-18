/*package cn.rbq108.test.Mixin;

import cn.rbq108.test.VariableLibrary.GlobalVariables;
import cn.rbq108.test.body.motion;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class FlightPostureMixin {
    // 注入点选在 setupAnim 的末尾 (TAIL)，避免原版给咱占了
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void onSetupAnimTail(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (GlobalVariables.B_LowGravity && entity instanceof Player) {
            //调用motion类里写的绝对控制逻辑
            motion.applyFlightPosture((HumanoidModel)(Object)this);
        }
    }
}*/