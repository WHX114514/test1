package cn.rbq108.test.math;

import cn.rbq108.test.VariableLibrary.GlobalVariables;
import org.joml.Vector3f;

/*
该类负责将各种向量在玩家坐标系与世界坐标系之间互相转换
*/
public class CoordinateSystemTransformation {

    public static void transformVelocityToWorld() {
        // 1. 获取吃满所有倍率的“本地期望推力”
        float localX = GlobalVariables.B_Vx3_1; // 右
        float localY = GlobalVariables.B_Vy3_1; // 上
        float localZ = GlobalVariables.B_Vz3_1; // 前

        // ==========================================
        // 神医的终极空间折跃魔法：降维打击般的四元数引擎！
        // ==========================================
        // 核心修正：在 Minecraft 坐标系中，当面向正南(+Z)时，右边是正西(-X)。
        // 所以本地的“向右(localX为正)”实际上需要反转为负号，才能完美匹配 3D 引擎喵！
        Vector3f localVel = new Vector3f(-localX, localY, localZ);

        // 核心魔法：使用实时更新的视角四元数，将本地推力完美扭曲到世界绝对坐标系！
        // 因为 currentQuat 包含了 Pitch、Yaw、Roll 三个维度的绝对数据，
        // 无论你怎么倒立、侧翻、甚至转成麻花，它算出来的“下”永远是你飞船的“底盘”方向喵呜！
        GlobalVariables.currentQuat.transform(localVel);

        // 把转换好的“地面世界速度”解包，乖乖存进 4 号抽屉
        GlobalVariables.B_Vx4 = localVel.x;
        GlobalVariables.B_Vy4 = localVel.y;
        GlobalVariables.B_Vz4 = localVel.z;
    }
}