package cn.rbq108.test.core;

import cn.rbq108.test.main;
import cn.rbq108.test.VariableLibrary.GlobalVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class KeyInputHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        var mc = Minecraft.getInstance();
        // 只有按下按键、没开菜单、且在无重力状态下才生效喵
        if (mc.player == null || mc.screen != null || !GlobalVariables.B_LowGravity) return;

        if (event.getAction() == GLFW.GLFW_PRESS) {
            boolean isShiftDown = (event.getModifiers() & GLFW.GLFW_MOD_SHIFT) != 0;

            // --- 只有在 Shift 按下时，我们才手动触发原版功能喵 ---
            if (isShiftDown) {
                // 如果按的是背包键 (E)
                if (mc.options.keyInventory.matches(event.getKey(), event.getScanCode())) {
                    mc.setScreen(new InventoryScreen(mc.player));
                }
                // 如果按的是丢弃键 (Q)
                if (mc.options.keyDrop.matches(event.getKey(), event.getScanCode())) {
                    mc.player.drop(false);
                }
            }
        }
    }
}