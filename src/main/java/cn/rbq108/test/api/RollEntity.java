package cn.rbq108.test.api;

public interface RollEntity {
    //void doABarrelRoll$changeElytraLook(double pitch, double yaw, double roll, Sensitivity sensitivity, double mouseDelta);

    //void doABarrelRoll$changeElytraLook(float pitch, float yaw, float roll);

    boolean doABarrelRoll$isRolling();

    void doABarrelRoll$setRolling(boolean rolling);

    float doABarrelRoll$getRoll();

    float doABarrelRoll$getRoll(float tickDelta);

    void doABarrelRoll$setRoll(float roll);
}
