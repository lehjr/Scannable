package li.cil.scannable.network.packets;

import li.cil.scannable.client.renderer.ScannerRenderer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Workaround for login event being fired server side only now
 */
public class ClientOnLoginPacket {
    public ClientOnLoginPacket() {
    }

    public static void encode(ClientOnLoginPacket msg, PacketBuffer packetBuffer) {
    }

    public static ClientOnLoginPacket decode(PacketBuffer packetBuffer) {
        return new ClientOnLoginPacket();
    }

    public static void handle(ClientOnLoginPacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ScannerRenderer.INSTANCE.init();
        });
        ctx.get().setPacketHandled(true);
    }
}