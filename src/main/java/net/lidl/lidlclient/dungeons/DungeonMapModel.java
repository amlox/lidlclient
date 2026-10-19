package net.lidl.lidlclient.dungeons;

import java.util.*;

/** Main-thread room graph. The 11x11 grid alternates room centers and connections. */
public final class DungeonMapModel {
    public static final int GRID = 11, WORLD_START = -185, WORLD_STEP = 16;
    public enum Type {
        UNKNOWN(0xFF333947), NORMAL(0xFF896343), ENTRANCE(0xFF4EB777), BLOOD(0xFFC94D53),
        FAIRY(0xFFDE82BA), RARE(0xFFD5B956), CHAMPION(0xFFD5B956), PUZZLE(0xFF9967CA), TRAP(0xFFDD994D);
        public final int color;
        Type(int color) { this.color = color; }
    }
    public enum State { UNKNOWN, UNOPENED, DISCOVERED, CLEARED, GREEN, FAILED }
    public enum Kind { EMPTY, ROOM, CONNECTION, DOOR }
    public enum Door { NORMAL, WITHER, BLOOD, ENTRANCE }
    public static final class Tile {
        public Kind kind = Kind.EMPTY;
        public Door door = Door.NORMAL;
        public Type type = Type.UNKNOWN;
        public State state = State.UNKNOWN;
        public String name = "Unknown";
        public int secrets, crypts, foundSecrets;
        public boolean identified, mapKnown;
        // Only the server's dungeon map can mark a room discovered.
        public boolean discovered;
    }
    private final Tile[] tiles = new Tile[GRID * GRID];
    public DungeonMapModel() { reset(); }
    public void reset() { Arrays.setAll(tiles, ignored -> new Tile()); }
    public Tile at(int x, int z) { return tiles[z * GRID + x]; }
    public static boolean center(int x, int z) { return x % 2 == 0 && z % 2 == 0; }
    public int roomTiles() {
        int count = 0;
        for (int z = 0; z < GRID; z += 2) for (int x = 0; x < GRID; x += 2)
            if (at(x, z).kind == Kind.ROOM) count++;
        return count;
    }
    public void identify(int x, int z, RoomCatalog.Room room) {
        Tile t = at(x, z);
        t.kind = Kind.ROOM; t.type = room.type(); t.name = room.name();
        t.secrets = room.secrets(); t.crypts = room.crypts(); t.identified = true;
    }
    /** Unique connected rooms, including L, 2x2 and long rooms; doors do not merge rooms. */
    public List<List<Integer>> groups() {
        List<List<Integer>> groups = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        for (int z = 0; z < GRID; z += 2) for (int x = 0; x < GRID; x += 2) {
            int start = z * GRID + x;
            if (at(x, z).kind != Kind.ROOM || !visited.add(start)) continue;
            List<Integer> group = new ArrayList<>();
            ArrayDeque<Integer> queue = new ArrayDeque<>(); queue.add(start);
            while (!queue.isEmpty()) {
                int id = queue.remove(); group.add(id);
                int cx = id % GRID, cz = id / GRID;
                for (int[] direction : new int[][]{{2,0},{-2,0},{0,2},{0,-2}}) {
                    int nx = cx + direction[0], nz = cz + direction[1];
                    if (nx < 0 || nz < 0 || nx >= GRID || nz >= GRID) continue;
                    if (at(cx + direction[0]/2, cz + direction[1]/2).kind != Kind.CONNECTION
                            || at(nx, nz).kind != Kind.ROOM) continue;
                    int next = nz * GRID + nx;
                    if (visited.add(next)) queue.add(next);
                }
            }
            groups.add(List.copyOf(group));
        }
        return groups;
    }
    public Tile representative(List<Integer> group) {
        return group.stream().map(i -> tiles[i]).filter(t -> t.identified).findFirst().orElse(tiles[group.getFirst()]);
    }
    /** Preserve observed special-door identity after opening or a generic map refresh. */
    public void observeDoor(int x,int z,Door observed) {
        if(x<0||z<0||x>=GRID||z>=GRID||x%2==z%2) return;
        Tile tile=at(x,z);tile.kind=Kind.DOOR;
        if(observed==Door.BLOOD||tile.door==Door.NORMAL||(observed==Door.WITHER&&tile.door!=Door.BLOOD)) tile.door=observed;
    }
    public void updateSecrets(int x,int z,int found,int total) {
        if(x<0||z<0||x>=GRID||z>=GRID||found<0||total<0||found>total) return;
        int id=z*GRID+x;
        for(var group:groups()) if(group.contains(id)) {
            Tile room=representative(group);
            if(!room.identified || room.secrets!=total) return;
            for(int member:group) {
                Tile tile=tiles[member];tile.foundSecrets=Math.max(tile.foundSecrets,found);
            }
            return;
        }
    }
    public int foundSecrets(List<Integer> group) {
        int total=representative(group).secrets,found=0;
        for(int id:group) {
            Tile tile=tiles[id];
            if(tile.state==State.GREEN) return total;
            found=Math.max(found,tile.foundSecrets);
        }
        return Math.min(total,found);
    }
    public static Type roomColor(int color) {
        return switch (color) {
            case 18 -> Type.BLOOD; case 82 -> Type.FAIRY; case 34 -> Type.RARE;
            case 74 -> Type.CHAMPION; case 66 -> Type.PUZZLE; case 62 -> Type.TRAP;
            case 63, 85 -> Type.NORMAL; case 30 -> Type.ENTRANCE; default -> Type.UNKNOWN;
        };
    }
}


