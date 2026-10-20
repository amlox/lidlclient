package net.lidl.lidlclient.pets;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Verifies exact hook descriptors without initializing Minecraft or native rendering. */
class PetsClickTargetsTest {
    private Set<String> methods(String owner) throws IOException {
        InputStream resource = getClass().getClassLoader().getResourceAsStream(owner + ".class");
        assertNotNull(resource, owner);
        try (DataInputStream in = new DataInputStream(resource)) {
            assertEquals(0xCAFEBABE, in.readInt());
            in.readUnsignedShort(); in.readUnsignedShort();
            String[] pool = new String[in.readUnsignedShort()];
            for (int i = 1; i < pool.length; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1 -> pool[i] = in.readUTF();
                    case 3, 4, 9, 10, 11, 12, 17, 18 -> in.skipNBytes(4);
                    case 5, 6 -> { in.skipNBytes(8); i++; }
                    case 7, 8, 16, 19, 20 -> in.skipNBytes(2);
                    case 15 -> in.skipNBytes(3);
                    default -> throw new IOException("Unknown constant tag " + tag);
                }
            }
            in.skipNBytes(6);
            in.skipNBytes(in.readUnsignedShort() * 2L);
            int fields = in.readUnsignedShort();
            for (int i = 0; i < fields; i++) { in.skipNBytes(6); attributes(in); }
            int count = in.readUnsignedShort();
            Set<String> result = new HashSet<>();
            for (int i = 0; i < count; i++) {
                in.readUnsignedShort();
                result.add(pool[in.readUnsignedShort()] + pool[in.readUnsignedShort()]);
                attributes(in);
            }
            return result;
        }
    }

    private void attributes(DataInputStream in) throws IOException {
        int count = in.readUnsignedShort();
        for (int i = 0; i < count; i++) {
            in.readUnsignedShort();
            in.skipNBytes(Integer.toUnsignedLong(in.readInt()));
        }
    }

    @Test void minecraftVersionHasEarlyPetClickHooks() throws Exception {
        Map<String, List<String>> expected = Map.of(
                "net/minecraft/client/gui/screens/inventory/AbstractContainerScreen", List.of(
                        "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
                        "getHoveredSlot(DD)Lnet/minecraft/world/inventory/Slot;",
                        "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V"),
                "net/minecraft/client/multiplayer/MultiPlayerGameMode", List.of(
                        "handleContainerInput(IIILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V"));
        for (var entry : expected.entrySet()) {
            Set<String> actual = methods(entry.getKey());
            for (String method : entry.getValue()) {
                assertTrue(actual.contains(method), () -> entry.getKey() + " missing " + method + "; actual: " + actual);
            }
        }
    }
}
