package net.lidl.lidlclient.dungeons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;

public final class DungeonMapScreen extends Screen {
    private final Screen parent;
    private int extent,left,top;
    public DungeonMapScreen(Screen parent) { super(Component.literal("Lidl Dungeon Map")); this.parent=parent; }
    @Override protected void init() {
        extent=Math.max(72,Math.min(width-32,(int)((height-110)/1.1))); left=(width-extent)/2; top=25;
        addRenderableWidget(Button.builder(Component.literal("Close"),b->onClose())
                .bounds(width/2+4,height-25,96,20).build());
        addRenderableWidget(Button.builder(Component.literal("Rescan"),b->DungeonMap.scanner().reset())
                .bounds(width/2-100,height-25,96,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta) {
        graphics.fill(0,0,width,height,0xF0090E18);
        graphics.text(font,title,(width-font.width(title))/2,8,0xFFFFFFFF,true);
        var scanner=DungeonMap.scanner();
        if (scanner.active()) {
            DungeonMapRenderer.draw(graphics,scanner.model(),left,top,extent,true,DungeonMap.players(),DungeonMap.opacity(),scanner.floor());
            int gx=(int)((mouseX-left)*128.0/extent), gz=(int)((mouseY-top)*128.0/extent);
            int x=(int)Math.round((gx-14)/10.0),z=(int)Math.round((gz-14)/10.0);
            String detail="Hover a room for its name and total secrets";
            if(mouseX>=left&&mouseY>=top&&gx<128&&gz<128&&x>=0&&z>=0&&x<GRID&&z<GRID&&center(x,z)) {
                int id=z*GRID+x;
                var group=scanner.model().groups().stream().filter(g->g.contains(id)).findFirst();
                if(group.isPresent()) {
                    Tile t=scanner.model().representative(group.get());
                    detail=t.identified?t.name+" | "+t.secrets+" secrets | "+t.crypts+" crypts":t.type+" | Not identified yet";
                }
            }
            graphics.text(font,Component.literal(detail),(width-font.width(detail))/2,height-53,0xFFDAE5FA,true);
        } else {
            String text="Waiting for a Hypixel Catacombs run";
            graphics.text(font,Component.literal(text),(width-font.width(text))/2,height/2,0xFFADBBD0,true);
        }
        String legend="Green: complete   White: cleared   Red: failed";
        graphics.text(font,Component.literal(legend),(width-font.width(legend))/2,height-40,0xFFADBBD0,false);
        super.extractRenderState(graphics,mouseX,mouseY,delta);
    }
    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
