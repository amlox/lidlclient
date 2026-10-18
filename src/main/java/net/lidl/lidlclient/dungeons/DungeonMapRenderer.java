package net.lidl.lidlclient.dungeons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import java.util.List;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;

public final class DungeonMapRenderer {
    private DungeonMapRenderer() { }
    private static void box(GuiGraphicsExtractor g, int left, int top, double scale,
                            double x, double z, double width, double height, int color) {
        g.fill(left + (int)Math.round(x * scale), top + (int)Math.round(z * scale),
                left + (int)Math.round((x + width) * scale), top + (int)Math.round((z + height) * scale), color);
    }
    private static String fit(String text, int width) {
        var font = Minecraft.getInstance().font;
        if (font.width(text) <= width) return text;
        while (!text.isEmpty() && font.width(text + ".") > width)
            text = text.substring(0, text.offsetByCodePoints(text.length(), -1));
        return text.isEmpty() ? "" : text + ".";
    }
    public static void draw(GuiGraphicsExtractor g, DungeonMapModel model, int left, int top,
                            int extent, boolean names, boolean players, double opacity, String floor) {
        var client = Minecraft.getInstance();
        int alpha = (int)(Math.clamp(opacity,0,100) * 255 / 100);
        g.fill(left,top,left+extent+12,top+extent+36,(alpha<<24)|0x0C1220);
        g.text(client.font, Component.literal("DUNGEON " + floor),left+6,top+6,0xFFE1E9FA,true);
        left += 6; top += 20;
        double scale = extent / 144.0;
        for (int z=0;z<GRID;z++) for (int x=0;x<GRID;x++) {
            Tile tile = model.at(x,z);
            double px = 12 + 12*x, pz = 12 + 12*z;
            if (tile.kind == Kind.EMPTY) {
                if (center(x,z)) box(g,left,top,scale,px-9,pz-9,18,18,0xFF1C2433);
                continue;
            }
            int color = tile.state == State.UNOPENED ? 0xFF424959 : tile.type.color;
            switch (tile.kind) {
                case ROOM -> box(g,left,top,scale,px-10,pz-10,20,20,color);
                case CONNECTION -> {
                    // Color separators from their connected room, including world-only scans.
                    if (x>0 && model.at(x-1,z).kind==Kind.ROOM) color=model.at(x-1,z).type.color;
                    else if (z>0 && model.at(x,z-1).kind==Kind.ROOM) color=model.at(x,z-1).type.color;
                    box(g,left,top,scale,px-(x%2==1?2:10),pz-(z%2==1?2:10),x%2==1?4:20,z%2==1?4:20,color);
                }
                case DOOR -> box(g,left,top,scale,px-(x%2==1?2:3),pz-(z%2==1?2:3),x%2==1?4:6,z%2==1?4:6,color);
                default -> { }
            }
        }
        var groups = model.groups();
        for (List<Integer> group : groups) {
            Tile room = model.representative(group);
            // Anchor inside an actual component, so L-shaped labels never float in empty space.
            int index = group.getFirst();
            int px = left + (int)((12+12*(index%GRID))*scale);
            int pz = top + (int)((12+12*(index/GRID))*scale);
            State state = group.stream().map(i->model.at(i%GRID,i/GRID).state)
                    .filter(s->s==State.FAILED || s==State.GREEN || s==State.CLEARED).findFirst().orElse(room.state);
            String mark = switch(state) { case GREEN -> "+"; case CLEARED -> "v"; case FAILED -> "X"; case UNOPENED -> "?"; default -> ""; };
            int markColor = state==State.FAILED?0xFFFF5555:state==State.GREEN?0xFF55FF80:0xFFFFFFFF;
            if (!mark.isEmpty()) g.text(client.font,Component.literal(mark),px-client.font.width(mark)/2,pz-8,markColor,true);
            if (names) {
                String label = room.identified ? room.name : room.type==Type.UNKNOWN?"?":room.type.name();
                label = fit(label,Math.max(10,(int)(22*scale)));
                g.text(client.font,Component.literal(label),px-client.font.width(label)/2,pz+2,0xFFFFFFFF,true);
            }
        }
        if (players && client.level!=null) for (var player:client.level.players()) {
            double mx=12+(player.getX()-WORLD_START)/32.0*24, mz=12+(player.getZ()-WORLD_START)/32.0*24;
            if (mx<0 || mz<0 || mx>=144 || mz>=144) continue;
            boolean self=player==client.player;
            box(g,left,top,scale,mx-2,mz-2,4,4,0xFF10141E);
            box(g,left,top,scale,mx-1.4,mz-1.4,2.8,2.8,self?0xFF58ECFF:0xFFFFFFFF);
        }
        g.text(client.font,Component.literal(groups.size()+" rooms | "+model.roomTiles()+" tiles"),left,top+extent+3,0xFFADBBD0,false);
    }
}
