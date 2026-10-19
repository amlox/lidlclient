package net.lidl.lidlclient.dungeons;

import java.util.*;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;

/** Noamm-style 16px rooms, 4px joins and a 6px inset in a 128px square. */
public final class DungeonMapStyle {
    public static final int SIZE=128, INSET=6, ROOM=16, GAP=4, PITCH=20;
    public record Rect(double x,double y,double width,double height) { }
    private DungeonMapStyle() { }
    public static Rect tile(int x,int z,Kind kind) {
        double left=INSET+(x/2)*PITCH, top=INSET+(z/2)*PITCH;
        if(x%2==0 && z%2==0) return new Rect(left,top,ROOM,ROOM);
        if(x%2==1) left+=ROOM;
        if(z%2==1) top+=ROOM;
        double w=x%2==1?GAP:ROOM, h=z%2==1?GAP:ROOM;
        if(kind==Kind.DOOR) {
            if(x%2==1) { top+=5; h=6; } else { left+=5; w=6; }
        }
        return new Rect(left,top,w,h);
    }
    /** Label the whole rectangle for straight/2x2 rooms, or the longest bar of an L. */
    public static Rect label(List<Integer> group) {
        int minX=10,maxX=0,minZ=10,maxZ=0;
        Set<Integer> cells=new HashSet<>(group);
        for(int id:group) { int x=id%GRID,z=id/GRID; minX=Math.min(minX,x);maxX=Math.max(maxX,x);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z); }
        if(group.size()==((maxX-minX)/2+1)*((maxZ-minZ)/2+1))
            return new Rect(INSET+minX/2*PITCH,INSET+minZ/2*PITCH,(maxX-minX)/2*PITCH+ROOM,(maxZ-minZ)/2*PITCH+ROOM);
        int bestX=minX,bestZ=minZ,bestLength=0;
        for(int z=minZ;z<=maxZ;z+=2) for(int x=minX;x<=maxX;x+=2) {
            int length=0;
            while(x+length*2<=maxX && cells.contains(z*GRID+x+length*2)) length++;
            if(length>bestLength) {bestX=x;bestZ=z;bestLength=length;}
        }
        return new Rect(INSET+bestX/2*PITCH,INSET+bestZ/2*PITCH,(bestLength-1)*PITCH+ROOM,ROOM);
    }
    public static int color(Tile tile) {
        if(tile.kind==Kind.DOOR) return DungeonMapColors.palette().door(tile.door);
        if(tile.state==State.UNOPENED && !tile.identified) return 0xFF414141;
        return DungeonMapColors.palette().room(tile.type);
    }
    public static State state(DungeonMapModel model,List<Integer> group) {
        State result=State.UNKNOWN;
        for(int id:group) {
            State next=model.at(id%GRID,id/GRID).state;
            if(priority(next)>priority(result)) result=next;
        }
        if(result==State.FAILED || result==State.GREEN) return result;
        Tile room=model.representative(group);
        if(room.identified && room.secrets>0 && model.foundSecrets(group)>=room.secrets) return State.GREEN;
        if(result==State.CLEARED) return result;
        return group.stream().anyMatch(i->model.at(i%GRID,i/GRID).discovered)?State.DISCOVERED:State.UNOPENED;
    }
    public static int stateColor(int base,State state,boolean discovered,boolean darken,double darkness) {
        if(!discovered) return darken?DungeonMapVisibility.darken(base,darkness):base;
        if(state==State.GREEN) return blend(base,0xFF55FF55,0.7);
        if(state==State.CLEARED) return blend(base,0xFFFFFFFF,0.3);
        if(state==State.FAILED) return blend(base,0xFFFF3333,0.3);
        return base;
    }
    private static int blend(int base,int overlay,double amount) {
        int r=(int)(((base>>16)&255)*(1-amount)+((overlay>>16)&255)*amount);
        int g=(int)(((base>>8)&255)*(1-amount)+((overlay>>8)&255)*amount);
        int b=(int)((base&255)*(1-amount)+(overlay&255)*amount);
        return 0xFF000000|(r<<16)|(g<<8)|b;
    }
    private static int priority(State state) {
        return switch(state) { case FAILED->6;case GREEN->5;case CLEARED->4;case DISCOVERED->3;case UNOPENED->2;case UNKNOWN->1; };
    }
    public static double world(double coordinate) { return INSET+ROOM/2.0+(coordinate-WORLD_START)/32.0*PITCH; }
}

