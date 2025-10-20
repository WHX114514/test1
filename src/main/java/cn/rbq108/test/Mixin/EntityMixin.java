package cn.rbq108.test.Mixin;

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

    // 为 yaw 和 pitch 添加新的字段
    @Unique
    private float customYaw_testMod;
    @Unique
    private float customPitch_testMod;


    @Override
    public boolean doABarrelRoll$isRolling() {
        // 在这里你可以写真正的按键检测逻辑
        // 为了演示，我们暂时假设它一直为 true
        isRolling_testMod = true;
        return this.isRolling_testMod;
    }

    @Override
    public float doABarrelRoll$getRoll(float tickDelta) {
        if (this.isRolling_testMod) {
            // 每次调用时增加 roll 值，制造旋转效果
            roll_testMod += 10.0f * tickDelta;
        }
        return this.roll_testMod;
    }

    // --- 新增方法 ---

    @Override
    public float doABarrelRoll$getYaw(float tickDelta) {
        // 示例：让镜头轻微水平摆动
        if (this.isRolling_testMod) {
            // 使用 sin 函数创建一个来回摆动的效果
            customYaw_testMod = 0;//(float) Math.sin(System.currentTimeMillis() / 500.0) * 15.0f;
        } else {
            customYaw_testMod = 0;
        }
        return customYaw_testMod;
    }

    @Override
    public float doABarrelRoll$getPitch(float tickDelta) {
        // 示例：让镜头轻微垂直摆动
        if (this.isRolling_testMod) {
            // 使用 cos 函数，与 yaw 错开
            customPitch_testMod = 0;//(float) Math.cos(System.currentTimeMillis() / 500.0) * 10.0f;
        } else {
            customPitch_testMod = 0;
        }
        return customPitch_testMod;
    }
}
