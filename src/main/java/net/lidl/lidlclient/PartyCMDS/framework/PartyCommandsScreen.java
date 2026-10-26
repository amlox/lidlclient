package net.lidl.lidlclient.PartyCMDS.framework;

import net.lidl.lidlclient.PartyCMDS.PartyCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Paginated live command library; drafts are edited on a separate screen. */
public final class PartyCommandsScreen extends Screen {
    private final Screen parent;
    private int page;
    private String message="";
    private java.util.List<String> shown=java.util.List.of();
    public PartyCommandsScreen(Screen parent) { super(Component.literal("Party commands")); this.parent=parent; }
    @Override protected void init() {
        var commands=PartyCommands.commandNames(); shown=state();
        int perPage=Math.max(1,(height-120)/24),pages=Math.max(1,(commands.size()+perPage-1)/perPage);
        page=Math.clamp(page,0,pages-1);
        int left=(width-296)/2;
        addRenderableWidget(Button.builder(Component.literal("Add command"),b -> Minecraft.getInstance()
                .setScreen(new PartyReplyScreen(this,null))).bounds(left,34,116,20).build());
        addRenderableWidget(Button.builder(Component.literal("Shared room"),b -> Minecraft.getInstance()
                .setScreen(new PartyRoomScreen(this))).bounds(left+122,34,110,20).build());
        for (int row=0;row<perPage && page*perPage+row<commands.size();row++) {
            String trigger=commands.get(page*perPage+row); int y=62+row*24;
            boolean reserved=trigger.equals("!ban") || trigger.equals("!help");
            String source=PartyReplyLibrary.isLocal(trigger)?"local":PartyReplyLibrary.isShared(trigger)?"room":"built-in";
            var edit=addRenderableWidget(Button.builder(Component.literal(trigger+" ["+source+"]"),b ->
                    Minecraft.getInstance().setScreen(new PartyReplyScreen(this,trigger))).bounds(left,y,158,20).build());
            edit.active=!reserved;
            addRenderableWidget(Button.builder(Component.literal(PartyCommands.isCommandConfigured(trigger)?"On":"Off"),b -> {
                PartyCommands.setCommandEnabled(trigger,!PartyCommands.isCommandConfigured(trigger)); rebuildWidgets();
            }).bounds(left+164,y,44,20).build());
            var remove=addRenderableWidget(Button.builder(Component.literal("Remove"),b -> {
                if (PartyReplyLibrary.isLocal(trigger)) {
                    PartyReplyLibrary.remove(trigger); message="Local override removed"; rebuildWidgets();
                } else if (PartyReplyLibrary.isShared(trigger)) PartyRoomSync.delete(trigger,(ok,detail) -> {
                    message=detail; rebuildWidgets();
                });
            }).bounds(left+214,y,82,20).build());
            remove.active=PartyReplyLibrary.isLocal(trigger) || PartyReplyLibrary.isShared(trigger)&&PartyRoomSync.connected();
        }
        int bottom=height-28;
        var previous=addRenderableWidget(Button.builder(Component.literal("Previous"),b -> { page--; rebuildWidgets(); })
                .bounds(left,bottom,84,20).build()); previous.active=page>0;
        addRenderableWidget(Button.builder(Component.literal("Done"),b -> onClose()).bounds(width/2-38,bottom,76,20).build());
        var next=addRenderableWidget(Button.builder(Component.literal("Next"),b -> { page++; rebuildWidgets(); })
                .bounds(left+212,bottom,84,20).build()); next.active=page+1<pages;
    }
    private java.util.List<String> state() {
        return PartyCommands.commandNames().stream().map(trigger -> trigger+":"+PartyCommands.isCommandConfigured(trigger)
                +":"+PartyReplyLibrary.isLocal(trigger)+":"+PartyReplyLibrary.isShared(trigger)).toList();
    }
    @Override public void tick() { if (!shown.equals(state())) rebuildWidgets(); }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        g.fill(0,0,width,height,0xEB0B101B); g.centeredText(font,title,width/2,16,0xFFFFFFFF);
        g.centeredText(font,message.isEmpty()?"Remove restores a shared / built-in fallback, if present.":message,
                width/2,height-47,0xFFADBBD0);
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
