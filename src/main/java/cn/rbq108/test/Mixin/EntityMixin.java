package cn.rbq108.test.Mixin;

import cn.rbq108.test.api.RollEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

// 1. Mixin 目标是 Entity 类
// 2. 让这个 Mixin 实现你的 RollEntity 接口
@Mixin(Entity.class)
public abstract class EntityMixin implements RollEntity {

    // 使用 @Unique 来添加新字段，用于存储翻滚状态
    @Unique
    private boolean isRolling_testMod; // 加个后缀避免冲突

    @Unique
    private float roll_testMod;

    // 3. 实现接口里的方法
    @Override
    public boolean doABarrelRoll$isRolling() {
        // 在这里写你的逻辑，比如检查某个按键是否被按下
        // 我这里先用一个简单的 false 作为占位符
         isRolling_testMod =true; /* 你的按键检查逻辑 */
        return this.isRolling_testMod;
    }

    @Override
    public float doABarrelRoll$getRoll(float tickDelta) {
        // 在这里写你的翻滚角度计算逻辑
        // 比如，如果正在翻滚，就让 roll_testMod 增加
        // 我这里先用一个简单的 0.0f 作为占位符
        if (this.isRolling_testMod) {
             roll_testMod += 10.0f * tickDelta;
        }
        return this.roll_testMod;
    }
}