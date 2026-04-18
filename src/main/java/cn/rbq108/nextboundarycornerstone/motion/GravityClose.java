package cn.rbq108.nextboundarycornerstone.motion;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.client.Minecraft;

public class GravityClose {

    public static void updateGravityState() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 直接把记事本里的无重力状态同步给物理引擎
        // 如果 B_LowGravity 是 true，重力消失；如果是 false，重力恢复
        mc.player.setNoGravity(GlobalVariables.B_LowGravity);
    }
}