package cn.rbq108.test.event; // 建议和小宇主人的事件包放在一起喵

import cn.rbq108.test.main;
import cn.rbq108.test.client.model.BASIC_BACKPACK_Converted;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 专门负责启动时的模型层注册喵！
 * 加上 bus = EventBusSubscriber.Bus.MOD 才是治好闪退的关键喵呜~
 */
//@EventBusSubscriber(modid = main.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // 这一行就是给你的 3D 支架“预留车位”喵！
        // 它会把 BASIC_BACKPACK_Converted 里的几百个方块参数注册进渲染引擎喵呜~
        event.registerLayerDefinition(BASIC_BACKPACK_Converted.LAYER_LOCATION, BASIC_BACKPACK_Converted::createBodyLayer);
    }


}

