package cn.rbq108.test.event;

import cn.rbq108.test.main;
import cn.rbq108.test.client.model.BASIC_BACKPACK_Converted;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class ModClientEvents {


    public static boolean B_CanBackpackGrantGravity = true; // 默认允许背包提供无重力


    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BASIC_BACKPACK_Converted.LAYER_LOCATION, BASIC_BACKPACK_Converted::createBodyLayer);
    }

    //暂时用这个不依赖那个报错类
    @SubscribeEvent
    public static void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        // 那个Event怎么导不进去哇！导啊！导！
    }
}