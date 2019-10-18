package li.cil.scannable.network;

import li.cil.scannable.api.API;
import li.cil.scannable.network.packets.ClientOnLoginPacket;
import li.cil.scannable.network.packets.StructureRequestPacket;
import li.cil.scannable.network.packets.StructureResponsePacket;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class Network {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL_INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(API.MOD_ID, "data"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void registerPackets() {
        int i = 0;

        CHANNEL_INSTANCE.registerMessage(
                i++,
                StructureRequestPacket.class,
                StructureRequestPacket::encode,
                StructureRequestPacket::decode,
                StructureRequestPacket::handle);

        CHANNEL_INSTANCE.registerMessage(
                i++,
                StructureResponsePacket.class,
                StructureResponsePacket::encode,
                StructureResponsePacket::decode,
                StructureResponsePacket::handle);

        CHANNEL_INSTANCE.registerMessage(
                i++,
                ClientOnLoginPacket.class,
                ClientOnLoginPacket::encode,
                ClientOnLoginPacket::decode,
                ClientOnLoginPacket::handle);
    }
}
