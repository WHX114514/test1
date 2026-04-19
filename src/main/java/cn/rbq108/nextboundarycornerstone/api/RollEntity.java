//这是桶滚模组作者开源代码中 负责roll旋转相关的东西（也有可能是mixin注入啥的），看不懂不敢乱动，反正能跑不是

// Reference: https://github.com/enjarai/do-a-barrel-roll
package cn.rbq108.nextboundarycornerstone.api;

public interface RollEntity {
    //void doABarrelRoll$changeElytraLook(double pitch, double yaw, double roll, Sensitivity sensitivity, double mouseDelta);

    //void doABarrelRoll$changeElytraLook(float pitch, float yaw, float roll);

    boolean doABarrelRoll$isRolling();

    void doABarrelRoll$setRolling(boolean rolling);

    float doABarrelRoll$getRoll();

    float doABarrelRoll$getRoll(float tickDelta);

    void doABarrelRoll$setRoll(float roll);

    float doABarrelRoll$getYaw(float tickDelta);

    float doABarrelRoll$getPitch(float tickDelta);
}
