package cn.rbq108.test.ServeMiao.communication;

import cn.rbq108.test.main;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import java.util.UUID;

// 🩺 这是一个包含玩家 ID、四元数和失重开关的“数据胶囊”
public record SyncRotationPayload(UUID playerId, Quaternionf quat, boolean lowGravity) implements CustomPacketPayload {

    public static final Type<SyncRotationPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(main.MODID, "rotation_sync"));

    // 🛠️ 序列化与反序列化（把数据拆成 0 和 1）
    public static final StreamCodec<FriendlyByteBuf, SyncRotationPayload> STREAM_CODEC = StreamCodec.ofMember(
            SyncRotationPayload::write, SyncRotationPayload::read
    );

    public static SyncRotationPayload read(FriendlyByteBuf buf) {
        return new SyncRotationPayload(
                buf.readUUID(),
                new Quaternionf(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat()),
                buf.readBoolean()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeFloat(quat.x);
        buf.writeFloat(quat.y);
        buf.writeFloat(quat.z);
        buf.writeFloat(quat.w);
        buf.writeBoolean(lowGravity);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}