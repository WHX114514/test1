package cn.rbq108.test.event;

import cn.rbq108.test.main;
import cn.rbq108.test.VariableLibrary.GlobalVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

/**
 * 绒布球重力背包：核心异能管理处喵！
 * 负责检测穿脱状态，并控制 B_LowGravity 的生死大权喵呜~
 */
@EventBusSubscriber(modid = main.MODID) // 注意喵：这里不加 Dist.CLIENT，因为重力状态通常需要同步喵！
public class BackpackAbilityEvents {

    @SubscribeEvent
    public static void onArmorChange(LivingEquipmentChangeEvent event) {
        // 1. 只管咱们的玩家主人喵！
        if (!(event.getEntity() instanceof Player player)) return;

        // 2. 只检查胸甲槽位（也就是支架挂着的地方喵）
        if (event.getSlot() == EquipmentSlot.CHEST) {

            // 3. 检查穿上的是不是咱们那个 3D 重力背包喵！
            boolean isWearingBackpack = event.getTo().is(main.BASIC_BACKPACK.get());

            if (isWearingBackpack) {
                // --- 4. 重点：检查“外部拦截开关”喵！ ---
                if (GlobalVariables.B_CanBackpackGrantGravity) {
                    GlobalVariables.B_LowGravity = true; // 开启 6DOF 无重力模式喵！
                }
            } else {
                // 5. 只要脱下来，就立刻关掉无重力，变回原版操作喵呜！
                GlobalVariables.B_LowGravity = false;
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;

        // --- 🩺 龙女神医的“自感应”补丁喵！ ---
        // 客户端每帧自己检查一遍背上的衣服，把 B_LowGravity 同步到本地内存喵呜！
        boolean isWearingBackpack = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(cn.rbq108.test.main.BASIC_BACKPACK.get());

        // 结合你要求的“外部禁用”开关喵
        cn.rbq108.test.VariableLibrary.GlobalVariables.B_LowGravity = isWearingBackpack && cn.rbq108.test.VariableLibrary.GlobalVariables.B_CanBackpackGrantGravity;

        // --- 接下来接你刚才写的那个“瞬间动量接管”逻辑喵 ---
        if (GlobalVariables.B_LowGravity && !GlobalVariables.prevLowGravity) {
            // ... (动量接管代码) ...
        }
        GlobalVariables.prevLowGravity = GlobalVariables.B_LowGravity;

        // ... (剩下的 6DOF 物理逻辑照旧喵) ...
    }
}