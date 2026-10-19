package net.lidl.lidlclient.dungeons;

/** Floor-specific boss-area boundaries, matching Odin's DungeonListener lookup. */
public final class DungeonBossRoom {
    private DungeonBossRoom() { }
    public static boolean contains(String floor,double x,double z) {
        if(floor==null||!floor.matches("[FM][1-7]")||!Double.isFinite(x)||!Double.isFinite(z)) return false;
        return switch(floor.charAt(1)) {
            case '1'->x> -71&&z> -39;
            case '2','3','4'->x> -39&&z> -39;
            case '5','6'->x> -39&&z> -7;
            case '7'->x> -7&&z> -7;
            default->false;
        };
    }
}
