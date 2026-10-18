package net.lidl.lidlclient.dungeons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import java.util.*;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;
import static net.lidl.lidlclient.dungeons.DungeonMapStyle.*;

/** Compact map styling based on Noamm's modern MapRenderer/MapConfig (CC0). */
public final class DungeonMapRenderer {
    private static final String[] CHECK={"         ##","        ## ","       ##  ","##    ##   "," ##  ##    ","  ####     ","   ##      "};
    private static final String[] CROSS={"##      ##"," ##    ## ","  ##  ##  ","   ####   ","    ##    ","   ####   ","  ##  ##  "," ##    ## ","##      ##"};
    private static final String[] QUESTION={" ###### ","##    ##","      ##","    ### ","   ##   ","   ##   ","        ","   ##   ","   ##   "};
    private DungeonMapRenderer() { }
    private static void fill(GuiGraphicsExtractor g,double x,double y,double w,double h,int color) {
        g.fill((int)Math.round(x),(int)Math.round(y),(int)Math.round(x+w),(int)Math.round(y+h),color);
    }
    private static void pixels(GuiGraphicsExtractor g,String[] pattern,double x,double y,int color) {
        for(int z=0;z<pattern.length;z++) for(int col=0;col<pattern[z].length();col++)
            if(pattern[z].charAt(col)=='#') fill(g,x+col,y+z,1,1,color);
    }
    private static void mark(GuiGraphicsExtractor g,State state,double x,double y) {
        String[] pattern=switch(state) {case GREEN,CLEARED->CHECK;case FAILED->CROSS;case UNOPENED->QUESTION;default->null;};
        if(pattern==null) return;
        int color=state==State.GREEN?0xFF55FF55:state==State.FAILED?0xFFFF3333:0xFFFFFFFF;
        double left=x-pattern[0].length()/2.0,top=y-pattern.length/2.0;
        pixels(g,pattern,left+1,top+1,0xC0000000);pixels(g,pattern,left,top,color);
    }
    private static int roomColor(Tile tile,boolean discovered) {
        int result=color(tile);
        return DungeonMap.fullMap() && DungeonMap.darkenUndiscovered() && !discovered
                ? DungeonMapVisibility.darken(result) : result;
    }
    private static void roomText(GuiGraphicsExtractor g,Tile room,State state,Rect bounds,boolean secrets,boolean dim) {
        if(room.type==Type.ENTRANCE || room.type==Type.FAIRY || (state==State.UNOPENED && !room.identified)) return;
        String name=room.identified?room.name:room.type.name();
        List<String> lines=new ArrayList<>();
        var font=Minecraft.getInstance().font;
        String current="";
        for(String word:name.split(" ")) {
            String candidate=current.isEmpty()?word:current+" "+word;
            if(!current.isEmpty() && font.width(candidate)>Math.max(24,bounds.width()*1.5)) {lines.add(current);current=word;}
            else current=candidate;
        }
        if(!current.isEmpty()) lines.add(current);
        if(secrets && room.identified && room.secrets>0) lines.add(room.secrets+" secrets");
        int maxWidth=lines.stream().mapToInt(font::width).max().orElse(1);
        float scale=(float)Math.min(DungeonMap.textScale(),Math.min((bounds.width()-2)/Math.max(1,maxWidth),(bounds.height()-2)/(lines.size()*9.0)));
        if(scale<=0) return;
        int color=state==State.GREEN?0xFF55FF55:state==State.CLEARED?0xFFFFFFFF:state==State.FAILED?0xFFFF3333:0xFFDDDDDD;
        if(dim) color=DungeonMapVisibility.darken(color);
        g.pose().pushMatrix();
        g.pose().translate((float)(bounds.x()+bounds.width()/2),(float)(bounds.y()+bounds.height()/2));
        g.pose().scale(scale,scale);
        for(int i=0;i<lines.size();i++) {
            String text=lines.get(i);
            g.text(font,Component.literal(text),-font.width(text)/2,-lines.size()*9/2+i*9,color,true);
        }
        g.pose().popMatrix();
    }
    public static void draw(GuiGraphicsExtractor g,DungeonMapModel model,int left,int top,
                            int extent,boolean overlays,boolean players,String floor) {
        var client=Minecraft.getInstance();
        g.pose().pushMatrix();g.pose().translate((float)left,(float)top);
        float scale=extent/(float)SIZE;g.pose().scale(scale,scale);
        // No map-wide fill or border: only rooms, overlays and real party heads.
        var visibility=new DungeonMapVisibility(model,DungeonMap.fullMap());
        var groups=model.groups();
        Map<Integer,Tile> owners=new HashMap<>();
        for(var group:groups) for(int id:group) owners.put(id,model.representative(group));
        for(int z=0;z<GRID;z++) for(int x=0;x<GRID;x++) {
            Tile t=model.at(x,z);
            if(!visibility.visible(x,z)) continue;
            if(x%2==1 && z%2==1) {
                Tile owner=owners.get((z-1)*GRID+x-1);
                if(owner!=null && owner==owners.get((z-1)*GRID+x+1)
                        && owner==owners.get((z+1)*GRID+x-1) && owner==owners.get((z+1)*GRID+x+1)) {
                    var junction=tile(x,z,Kind.CONNECTION);
                    fill(g,junction.x(),junction.y(),junction.width(),junction.height(),roomColor(owner,visibility.discovered(x,z)));
                    continue;
                }
            }
            if(t.kind==Kind.EMPTY) continue;
            Tile source=t;
            if(t.kind==Kind.CONNECTION) {
                if(x%2==1) source=owners.getOrDefault(z*GRID+x-1,t);
                else if(z>0) source=owners.getOrDefault((z-1)*GRID+x,t);
            }
            var rect=tile(x,z,t.kind);
            fill(g,rect.x(),rect.y(),rect.width(),rect.height(),roomColor(source,visibility.discovered(x,z)));
        }
        if(overlays) for(var group:groups) {
            int first=group.getFirst();
            if(!visibility.visible(first%GRID,first/GRID)) continue;
            Tile room=model.representative(group);
            Rect bounds=label(group);State state=state(model,group);
            boolean undiscovered=!visibility.discovered(first%GRID,first/GRID);
            if(DungeonMap.overlay().equals("Checkmarks")) mark(g,state,bounds.x()+bounds.width()/2,bounds.y()+bounds.height()/2);
            else roomText(g,room,state,bounds,DungeonMap.overlay().equals("Names + secrets"),
                    undiscovered && DungeonMap.darkenUndiscovered() && DungeonMap.fullMap());
        }
        if(players && client.level!=null) for(var player:client.level.players()) {
            if(!player.isAlive() || !((Object)player instanceof AbstractClientPlayer avatar)) continue;
            if(!DungeonMap.scanner().party().show(player.getName().getString(),player==client.player)) continue;
            if(!DungeonMap.fullMap() && !DungeonMapVisibility.marker(model,visibility,player.getX(),player.getZ())) continue;
            double mx=world(player.getX()),mz=world(player.getZ());
            if(mx<INSET || mz<INSET || mx>SIZE-INSET || mz>SIZE-INSET) continue;
            g.pose().pushMatrix();g.pose().translate((float)mx,(float)mz);
            g.pose().rotate((float)Math.toRadians(player.getYRot()+180));
            g.pose().scale((float)DungeonMap.headScale(),(float)DungeonMap.headScale());
            fill(g,-7,-7,14,14,player==client.player?0xFF55FF55:0xFF141414);
            var skin=avatar.getSkin().body().texturePath();
            g.blit(RenderPipelines.GUI_TEXTURED,skin,-6,-6,8f,8f,12,12,8,8,64,64,-1);
            g.blit(RenderPipelines.GUI_TEXTURED,skin,-6,-6,40f,8f,12,12,8,8,64,64,-1);
            g.pose().popMatrix();
        }
        if(DungeonMap.stats()) {
            long count=groups.stream().filter(group->{int id=group.getFirst();return visibility.visible(id%GRID,id/GRID);}).count();
            String text=floor+"  |  "+count+" rooms";
            g.pose().pushMatrix();g.pose().translate(64f,131f);g.pose().scale(0.65f,0.65f);
            g.text(client.font,Component.literal(text),-client.font.width(text)/2,0,0xFFFFFFFF,true);g.pose().popMatrix();
        }
        g.pose().popMatrix();
    }
}
