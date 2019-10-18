package li.cil.scannable.network.packets;

import li.cil.scannable.client.scanning.ScanResultProviderStructure;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class StructureResponsePacket {

    private static ScanResultProviderStructure.StructureLocation[] structures;

    // --------------------------------------------------------------------- //

    public StructureResponsePacket(final ScanResultProviderStructure.StructureLocation[] structures) {
        this.structures = structures;
    }

    @SuppressWarnings("unused") // For deserialization.
    public StructureResponsePacket() {
    }

    public ScanResultProviderStructure.StructureLocation[] getStructures() {
        return structures;
    }

    // --------------------------------------------------------------------- //
    public static StructureResponsePacket decode(final PacketBuffer packetBuffer) {
        final int length = packetBuffer.readInt();
        structures = new ScanResultProviderStructure.StructureLocation[length];
        for (int i = 0; i < length; i++) {
            final String name = packetBuffer.readString(128);
            final BlockPos pos = packetBuffer.readBlockPos();
            structures[i] = new ScanResultProviderStructure.StructureLocation(name, pos);
        }
        return new StructureResponsePacket(structures);
    }

    public void encode(final PacketBuffer packetBuffer) {
        packetBuffer.writeInt(structures.length);
        for (final ScanResultProviderStructure.StructureLocation structure : structures) {
            packetBuffer.writeString(structure.name);
            packetBuffer.writeBlockPos(structure.pos);
        }
    }

    public static void handle(final StructureResponsePacket message, final Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(()->{
            ScanResultProviderStructure.INSTANCE.setStructures(message.getStructures());
        });
        ctx.get().setPacketHandled(true);
    }
}