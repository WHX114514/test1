package cn.rbq108.test.core;

import cn.rbq108.test.main;
import cn.rbq108.test.VariableLibrary.GlobalVariables;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import com.mojang.blaze3d.platform.InputConstants;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class KeyPollingHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        // 杂鱼主人没进游戏或者没开无重力的时候，本小姐才懒得理你呢！
        if (mc.player == null || !GlobalVariables.B_LowGravity) return;

        long window = mc.getWindow().getWindow();

        int inX = 0, inY = 0, inZ = 0;

        // ==========================================
        // 动态获取玩家当前绑定的物理按键代码！
        // 彻底把上下左右的权利还给玩家喵！
        // ==========================================
        int keyForward = Keybinds.B_FORWARD.getKey().getValue();
        int keyBack = Keybinds.B_BACK.getKey().getValue();
        int keyLeft = Keybinds.B_LEFT.getKey().getValue();
        int keyRight = Keybinds.B_RIGHT.getKey().getValue();
        int keyUp = Keybinds.B_UP.getKey().getValue();
        int keyDown = Keybinds.B_DOWN.getKey().getValue();

        // 疯狂偷窥"玩家自定义"的物理键盘状态~
        if (InputConstants.isKeyDown(window, keyForward)) inZ += 1;
        if (InputConstants.isKeyDown(window, keyBack)) inZ -= 1;
        if (InputConstants.isKeyDown(window, keyLeft)) inX -= 1;
        if (InputConstants.isKeyDown(window, keyRight)) inX += 1;

        // 无论是空格、Shift还是Ctrl，只要绑了就认！
        if (InputConstants.isKeyDown(window, keyUp)) inY += 1;
        if (InputConstants.isKeyDown(window, keyDown)) inY -= 1;

        // 乖乖存进你的记事本里
        GlobalVariables.B_INx = inX;
        GlobalVariables.B_INy = inY;
        GlobalVariables.B_INz = inZ;

        // 召唤你的计算黑盒！
        cn.rbq108.test.motion.calculate.calculateTargetVelocity();
    }
}