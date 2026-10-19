package net.lidl.lidlclient.dungeons;

import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;

/** Map calibration and palette interpretation adapted from NoammAddons (CC0). */
public final class DungeonMapPixels {
    public record Calibration(int x, int z, int roomSize) { }
    private DungeonMapPixels() { }

    public static Calibration calibrate(byte[] colors, int floor) {
        if (colors == null || colors.length != 128 * 128) return null;
        for (int z = 0; z < 128; z++) for (int x = 0; x < 128;) {
            if (pixel(colors, x, z) != 30) { x++; continue; }
            int start = x;
            while (x < 128 && pixel(colors, x, z) == 30) x++;
            int size = x - start;
            if (size != 16 && size != 18) continue;
            int pitch = size + 4;
            return switch (floor) {
                case 0 -> new Calibration(22, 22, size);
                case 1 -> new Calibration(22, 11, size);
                case 2, 3 -> new Calibration(11, 11, size);
                default -> new Calibration(start % pitch, z % pitch, size);
            };
        }
        return null;
    }

    private static int pixel(byte[] data, int x, int z) {
        return x >= 0 && z >= 0 && x < 128 && z < 128 ? Byte.toUnsignedInt(data[z * 128 + x]) : 0;
    }

    public static void update(DungeonMapModel model, byte[] colors, Calibration c) {
        if (colors == null || colors.length != 16384 || c == null) return;
        int half = c.roomSize() / 2, step = half + 2;
        for (int z = 0; z < GRID; z++) for (int x = 0; x < GRID; x++) {
            int px = c.x() + half + x * step, pz = c.z() + half + z * step;
            int middle = pixel(colors, px, pz);
            if (middle == 0) continue;
            int side = center(x,z) ? pixel(colors, px - half, pz - half)
                    : z % 2 == 1 ? pixel(colors, px - 4, pz) : pixel(colors, px, pz - 4);
            Tile t = model.at(x,z);
            Type type = roomColor(side);
            if (center(x,z)) {
                if (type == Type.UNKNOWN) continue;
                t.kind = Kind.ROOM;
                t.mapKnown=true;
                if (!t.identified) t.type = type;
                t.state = switch (middle) {
                    case 18 -> type == Type.PUZZLE ? State.FAILED : State.DISCOVERED;
                    case 30 -> type == Type.ENTRANCE ? State.DISCOVERED : State.GREEN;
                    case 34 -> State.CLEARED;
                    case 85, 119 -> State.UNOPENED;
                    default -> State.DISCOVERED;
                };
                if(t.type==Type.FAIRY&&t.state!=State.UNOPENED) t.state=State.DISCOVERED;
                t.discovered |= t.state != State.UNOPENED;
            } else if (x%2!=z%2 && (t.door==Door.WITHER||t.door==Door.BLOOD)) {
                // Generic brown/open map pixels must not erase a scanned special door.
                model.observeDoor(x,z,middle==18?Door.BLOOD:middle==119?Door.WITHER:Door.NORMAL);
                t.mapKnown=true;t.state=middle==119||middle==85?State.UNOPENED:State.DISCOVERED;
                t.discovered|=t.state!=State.UNOPENED;
            } else if (side != 0 && type != Type.UNKNOWN) {
                t.kind = Kind.CONNECTION; t.type = type; t.state = State.DISCOVERED;
                t.mapKnown=true;
                t.discovered |= middle != 85 && middle != 119;
            } else if ((x % 2) != (z % 2)) {
                Type doorType = roomColor(middle);
                if (middle == 119 || middle == 85 || doorType != Type.UNKNOWN) {
                    t.kind = Kind.DOOR;
                    t.mapKnown=true;
                    model.observeDoor(x,z,middle==119?Door.WITHER:middle==18?Door.BLOOD:middle==30?Door.ENTRANCE:Door.NORMAL);
                    t.type = middle == 119 ? Type.UNKNOWN : doorType;
                    t.state = middle == 119 || middle == 85 ? State.UNOPENED : State.DISCOVERED;
                    t.discovered |= t.state != State.UNOPENED;
                }
            }
        }
    }
}


