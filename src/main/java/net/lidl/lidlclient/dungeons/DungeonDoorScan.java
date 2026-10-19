package net.lidl.lidlclient.dungeons;

/** Block at the centre of a dungeon doorway, Y=69, following Noamm's scan. */
public final class DungeonDoorScan {
    private DungeonDoorScan() { }
    public static DungeonMapModel.Door classify(String block) {
        return switch(block) {
            case "minecraft:coal_block"->DungeonMapModel.Door.WITHER;
            case "minecraft:red_terracotta"->DungeonMapModel.Door.BLOOD;
            case "minecraft:infested_chiseled_stone_bricks"->DungeonMapModel.Door.ENTRANCE;
            default->DungeonMapModel.Door.NORMAL;
        };
    }
}
