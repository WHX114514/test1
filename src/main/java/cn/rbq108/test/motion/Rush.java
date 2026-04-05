/*
正如标题，这个是在失重条件下启停冲刺的

双击w进入冲刺状态，亦或者在按w平移的时候按住左Ctrl（物理级读取，不干扰原版）进入冲刺

撞到东西让速度降到一定阈值、或者松开w、或者按s均会退出冲刺
*/
package cn.rbq108.test.motion;

import cn.rbq108.test.VariableLibrary.GlobalVariables;
import cn.rbq108.test.core.Keybinds; // 🩺 导入你刚注册的按键类
import net.minecraft.client.Minecraft;

public class Rush {
    private static long lastWPressTime = 0;
    private static boolean wasWDown = false;
    private static final long DOUBLE_CLICK_INTERVAL = 300; // 300毫秒内双击有效

    public static void updateRushState() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 🩺 1. 读取玩家自定义的“前进键”状态
        boolean isForwardDown = mc.options.keyUp.isDown();

        // 🩺 2. 读取我们刚注册的专用冲刺键状态
        // 使用 isDown() 直接获取物理按下状态，不触发原版疾跑逻辑喵！
        boolean isRushKeyDown = Keybinds.B_RUSH_BUTTON.isDown();

        // ==========================================
        // 🩺 冲刺触发逻辑：双通道开启！
        // ==========================================

        // 通道 A：前进时按下专用冲刺键 (W + B_RushButton)
        if (isForwardDown && isRushKeyDown && GlobalVariables.B_INz > 0) {
            GlobalVariables.B_rush = true;
        }

        // 通道 B：双击前进检测 (Double W)
        if (isForwardDown && !wasWDown) { // 捕捉按下瞬间
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastWPressTime < DOUBLE_CLICK_INTERVAL) {
                GlobalVariables.B_rush = true;
            }
            lastWPressTime = currentTime;
        }
        wasWDown = isForwardDown;

        // ==========================================
        // 🩺 冲刺退出逻辑
        // ==========================================

        // 1. 如果松开了前进键，或者按下了后退键 (B_INz <= 0)
        if (GlobalVariables.B_INz <= 0) {
            GlobalVariables.B_rush = false;
        }

        // 2. 碰撞保底熄火：撞墙且速度极低时退出
        if (mc.player.horizontalCollision || mc.player.verticalCollision) {
            float speedSq = GlobalVariables.B_Vx1 * GlobalVariables.B_Vx1 +
                    GlobalVariables.B_Vy1 * GlobalVariables.B_Vy1 +
                    GlobalVariables.B_Vz1 * GlobalVariables.B_Vz1;
            if (speedSq < 0.001f) {
                GlobalVariables.B_rush = false;
            }
        }
    }
}