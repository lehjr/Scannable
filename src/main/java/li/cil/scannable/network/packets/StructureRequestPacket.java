package li.cil.scannable.network.packets;

import li.cil.scannable.client.scanning.ScanResultProviderStructure;
import li.cil.scannable.common.config.CommonConfig;
import li.cil.scannable.network.Network;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class StructureRequestPacket {
    private static BlockPos center;
    private static float radius;
    private static boolean hideExplored;

    // --------------------------------------------------------------------- //

    public StructureRequestPacket(final BlockPos center, final float radius, final boolean hideExplored) {
        this.center = center;
        this.radius = radius;
        this.hideExplored = hideExplored;
    }

    @SuppressWarnings("unused") // For deserialization.
    public StructureRequestPacket() {
    }

    public BlockPos getCenter() {
        return center;
    }

    public float getRadius() {
        return radius;
    }

    public boolean hideExplored() {
        return hideExplored;
    }

    // --------------------------------------------------------------------- //
    // IMessage

    public static StructureRequestPacket decode(final PacketBuffer packetBuffer) {
        return new StructureRequestPacket(
                packetBuffer.readBlockPos(),
                packetBuffer.readFloat(),
                packetBuffer.readBoolean());
    }

    public void encode(final PacketBuffer packetBuffer) {
        packetBuffer.writeBlockPos(center);
        packetBuffer.writeFloat(radius);
        packetBuffer.writeBoolean(hideExplored);
    }

    public static void handle(final StructureRequestPacket message, final Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(()->{
            final World world = ctx.get().getSender().world;

            if (world == null) {
                return;
            }

            final BlockPos center = message.getCenter();
            final float radius = message.getRadius();
            final boolean hideExplored = message.hideExplored();

            final List<ScanResultProviderStructure.StructureLocation> structures = new ArrayList<>();
            final float sqRadius = radius * radius;
            for (final String name : CommonConfig.structures.get()) {
                final BlockPos pos = world.findNearestStructure(name, center, (int)radius, hideExplored);
                if (pos != null && center.distanceSq(pos) <= sqRadius) {
                    structures.add(new ScanResultProviderStructure.StructureLocation(name, pos));
                }
            }

            if (structures.isEmpty()) {
                return;
            }

            Network.CHANNEL_INSTANCE.reply(new StructureResponsePacket(structures.toArray(new ScanResultProviderStructure.StructureLocation[structures.size()])), ctx.get());
            ctx.get().setPacketHandled(true);
        });
    }
}
