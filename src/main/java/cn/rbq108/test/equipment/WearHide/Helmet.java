package cn.rbq108.test.equipment.WearHide;

import cn.rbq108.test.main;
import cn.rbq108.test.VariableLibrary.GlobalVariables;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.WeakHashMap;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class Helmet {

    // 🩺 储物柜：专门用来临时存放被摘下来的头盔（支持多人联机，一人一个格子）
    private static final WeakHashMap<Player, ItemStack> hiddenHelmets = new WeakHashMap<>();

    // 🎨 画画前（Pre）：查水表，没收头盔
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();

        if (GlobalVariables.B_LowGravity) {
            // 🩺 绕过底层限制，直接去玩家的背包内存列表里拿东西！(索引 3 代表头盔栏)
            ItemStack headItem = player.getInventory().armor.get(3);

            if (!headItem.isEmpty()) {
                String itemName = headItem.getItem().toString();

                // 💥 判定 VIP：如果不是咱们的王牌头盔
                if (!itemName.contains("test:pilot_helmet")) {

                    // 1. 塞进储物柜保管
                    hiddenHelmets.put(player, headItem);

                    // 💥 2. 神不知鬼不觉地清空背包里的头盔！
                    // 绝不使用 setItemSlot！这招完美避开了游戏底层的穿脱音效和护甲重算！
                    player.getInventory().armor.set(3, ItemStack.EMPTY);
                }
            }
        }
    }

    // 🎨 画画后（Post）：悄悄塞回去
    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();

        // 检查储物柜里有没有扣押他的头盔
        if (hiddenHelmets.containsKey(player)) {

            // 💥 3. 悄悄把头盔塞回玩家的包里，假装什么都没发生过！
            player.getInventory().armor.set(3, hiddenHelmets.get(player));

            // 清理储物柜
            hiddenHelmets.remove(player);
        }
    }
}