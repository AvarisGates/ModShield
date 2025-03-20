package com.avaris.modshield.network;

import com.avaris.modshield.ModShield;
import com.google.common.hash.HashCode;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public record ClientModsC2S(Map<String,String> mods, int hash,boolean valid) implements CustomPayload {
    public static final PacketCodec<PacketByteBuf, ClientModsC2S> CODEC;
    public static final Identifier PACKET_ID;
    public static final CustomPayload.Id<ClientModsC2S> ID;

    private static final int MAX_MOD_ID_LEN = 256;
    private static final int MAX_MOD_VERSION_LEN = 256;

    static {
        CODEC = new PacketCodec<>() {
            @Override
            public ClientModsC2S decode(PacketByteBuf buf) {
                HashMap<String,String> map = new HashMap<>();
                int listSize = buf.readInt();
                for(int i = 0;i < listSize;i++) {
                    map.put(buf.readString(MAX_MOD_ID_LEN),
                            buf.readString(MAX_MOD_VERSION_LEN));
                }
                boolean valid = map.hashCode() == buf.readInt();
                return new ClientModsC2S(map,map.hashCode(),valid);
            }

            @Override
            public void encode(PacketByteBuf buf, ClientModsC2S packet) {
                buf.writeInt(packet.mods.size());
                for(Map.Entry<String, String> entry : packet.mods.entrySet()){
                    int size = Math.min(MAX_MOD_ID_LEN, entry.getKey().length());
                    int size1 = Math.min(MAX_MOD_ID_LEN, entry.getValue().length());

                    buf.writeString(entry.getKey(),size);
                    buf.writeString(entry.getValue(),size1);
                }
                buf.writeInt(packet.mods.hashCode());
            }
        };
        PACKET_ID = ModShield.id("cast_player_class_ability");
        ID = new CustomPayload.Id<>(PACKET_ID);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
