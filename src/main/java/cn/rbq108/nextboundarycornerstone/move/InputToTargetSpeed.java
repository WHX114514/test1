package cn.rbq108.nextboundarycornerstone.move;

import net.minecraft.world.phys.Vec3;

public class InputToTargetSpeed {


    //轴向基础比例
    public static final double RATIO_X = 1.0;   // 左右基准
    public static final double RATIO_Y = 0.7;   // 上下较慢
    public static final double RATIO_Z = 1.2;   // 前进较快

    /**
     * 计算三轴目标速度 (玩家参考系)
     * * @param B_INx      X轴键输入 (-1, 0, 1)
     * @param B_INy      Y轴键输入 (-1, 0, 1)
     * @param B_INz      Z轴键输入 (-1, 0, 1)
     * @param B_rush     是否冲刺
     * @param B_Vmax     非冲刺最大移速
     * @param B_rush_xy  X和Y轴的冲刺倍率
     * @param B_rush_z   Z轴(前方)的冲刺倍率
     * @return 包含 B_Vx3, B_Vy3, B_Vz3 的向量
     */
    public static Vec3 calculateVelocity(
            int B_INx, int B_INy, int B_INz,
            boolean B_rush,
            double B_Vmax, double B_rush_xy, double B_rush_z) {

        //获取输入向量
        Vec3 rawInput = new Vec3(B_INx, B_INy, B_INz);

        //计算输入强度，并限制最大为 1.0
        double inputMagnitude = Math.min(1.0, rawInput.length());

        // 如果没有按任何键，直接返回零向量，省下 CPU 资源
        if (inputMagnitude < 0.001) {
            return Vec3.ZERO;
        }

        //应用基础轴向比例，生成扭曲后的方向权重
        double weightX = rawInput.x * RATIO_X;
        double weightY = rawInput.y * RATIO_Y;
        double weightZ = rawInput.z * RATIO_Z;
        Vec3 weightedDirection = new Vec3(weightX, weightY, weightZ);

        // 归一化
        Vec3 normalizedDir = weightedDirection.normalize();

        // 算出基础目标速度(玩家系
        double B_Vx3 = normalizedDir.x * (B_Vmax * inputMagnitude);
        double B_Vy3 = normalizedDir.y * (B_Vmax * inputMagnitude);
        double B_Vz3 = normalizedDir.z * (B_Vmax * inputMagnitude);

        // 冲刺判定：如果是 true，额外给倍率
        if (B_rush) {
            B_Vx3 *= B_rush_xy;
            B_Vy3 *= B_rush_xy;
            B_Vz3 *= B_rush_z;
        }

        // 最终返回的三轴目标速度
        return new Vec3(B_Vx3, B_Vy3, B_Vz3);
    }
}