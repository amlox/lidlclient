package net.lidl.lidlclient.dungeons;

import java.util.*;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;

/** Render-only masks shared by rooms, connectors, labels, markers and hover details. */
public final class DungeonMapVisibility {
    private final boolean[] visible=new boolean[GRID*GRID], discovered=new boolean[GRID*GRID];
    public DungeonMapVisibility(DungeonMapModel model,boolean full) {
        for(var group:model.groups()) {
            boolean entered=group.stream().anyMatch(i->model.at(i%GRID,i/GRID).discovered);
            for(int id:group) {visible[id]=full||entered||model.at(id%GRID,id/GRID).mapKnown;discovered[id]=entered;}
        }
        for(int z=0;z<GRID;z++) for(int x=0;x<GRID;x++) {
            if(center(x,z)) continue;
            Tile tile=model.at(x,z);
            List<Integer> neighbors=new ArrayList<>();
            if(x%2==1 && z%2==0) {neighbors.add(z*GRID+x-1);neighbors.add(z*GRID+x+1);}
            else if(z%2==1 && x%2==0) {neighbors.add((z-1)*GRID+x);neighbors.add((z+1)*GRID+x);}
            else {neighbors.add((z-1)*GRID+x-1);neighbors.add((z-1)*GRID+x+1);neighbors.add((z+1)*GRID+x-1);neighbors.add((z+1)*GRID+x+1);}
            boolean entered=neighbors.stream().allMatch(i->discovered[i]);
            int id=z*GRID+x;
            discovered[id]=entered;
            visible[id]=full || (tile.kind==Kind.DOOR ? tile.mapKnown && neighbors.stream().anyMatch(i->visible[i]) : entered || tile.mapKnown && neighbors.stream().allMatch(i->visible[i]));
        }
    }
    public boolean visible(int x,int z) { return visible[z*GRID+x]; }
    public boolean discovered(int x,int z) { return discovered[z*GRID+x]; }
    public static int darken(int color) {
        return darken(color,65);
    }
    public static int darken(int color,double darkness) {
        double brightness=1-Math.clamp(darkness,0,100)/100;
        int r=(color>>16)&255,g=(color>>8)&255,b=color&255;
        return (color&0xFF000000)|((int)(r*brightness)<<16)|((int)(g*brightness)<<8)|(int)(b*brightness);
    }
    public static boolean marker(DungeonMapModel model,DungeonMapVisibility mask,double worldX,double worldZ) {
        int x=(int)Math.round((worldX-WORLD_START)/32.0)*2,z=(int)Math.round((worldZ-WORLD_START)/32.0)*2;
        return x>=0 && z>=0 && x<GRID && z<GRID && mask.visible(x,z) && model.at(x,z).kind==Kind.ROOM;
    }
}


