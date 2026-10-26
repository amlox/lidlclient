package net.lidl.lidlclient.PartyCMDS.framework;

import net.lidl.lidlclient.PartyCMDS.PartyCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** One answer at a time, explicit save/cancel; conflicts keep the complete draft open. */
public final class PartyReplyScreen extends Screen {
    private final Screen parent;
    private final String original;
    private final List<String> answers=new ArrayList<>();
    private int selected;
    private boolean enabled=true,busy;
    private EditBox trigger,input;
    private String triggerDraft="!hello",message="";
    private Button save;
    private long baseRevision;
    public PartyReplyScreen(Screen parent,String trigger) {
        super(Component.literal(trigger==null?"New party reply":"Edit "+trigger));
        this.parent=parent; original=trigger; baseRevision=PartyRoomSync.revision();
        if (trigger==null) answers.add("Hello {player}!");
        else {
            var reply=PartyCommands.editableReply(trigger);
            answers.addAll(reply.answers()); enabled=reply.enabled(); triggerDraft=reply.trigger();
        }
    }
    private void retain() {
        if (trigger!=null) triggerDraft=trigger.getValue();
        if (input!=null) answers.set(selected,input.getValue());
    }
    @Override protected void init() {
        int left=(width-296)/2;
        trigger=addRenderableWidget(new EditBox(font,left,43,296,20,Component.literal("Command trigger")));
        trigger.setMaxLength(25); trigger.setValue(triggerDraft); trigger.setEditable(original==null);
        input=addRenderableWidget(new EditBox(font,left,84,296,20,Component.literal("Answer")));
        input.setMaxLength(256); input.setValue(answers.get(selected));
        trigger.setResponder(value -> triggerDraft=value);
        input.setResponder(value -> answers.set(selected,value));
        addRenderableWidget(Button.builder(Component.literal("Previous"),b -> {
            retain(); selected=Math.floorMod(selected-1,answers.size()); rebuildWidgets();
        }).bounds(left,110,76,20).build());
        addRenderableWidget(Button.builder(Component.literal("Next"),b -> {
            retain(); selected=(selected+1)%answers.size(); rebuildWidgets();
        }).bounds(left+80,110,60,20).build());
        var add=addRenderableWidget(Button.builder(Component.literal("Add answer"),b -> {
            retain(); answers.add(""); selected=answers.size()-1; rebuildWidgets();
        }).bounds(left+144,110,90,20).build()); add.active=answers.size()<32;
        var remove=addRenderableWidget(Button.builder(Component.literal("Drop"),b -> {
            retain(); answers.remove(selected); selected=Math.min(selected,answers.size()-1); rebuildWidgets();
        }).bounds(left+238,110,58,20).build()); remove.active=answers.size()>1;
        addRenderableWidget(Button.builder(Component.literal(enabled?"Reply: On":"Reply: Off"),b -> {
            retain(); enabled=!enabled; rebuildWidgets();
        }).bounds(left,136,66,20).build());
        addRenderableWidget(Button.builder(Component.literal("Room"),b -> {
            var current=PartyReplyLibrary.roomReply(triggerDraft);
            message=current==null?"No shared definition":("Room: "+current.answers().getFirst());
        }).bounds(left+72,136,70,20).build());
        addRenderableWidget(Button.builder(Component.literal("Preview"),b -> {
            try { retain(); message=definition().render("TestPlayer","example arguments"); }
            catch (RuntimeException error) { message=error.getMessage(); }
        }).bounds(left+148,136,70,20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset base"),b -> {
            retain();
            try {
                var base=PartyCommands.baseReply(triggerDraft.strip());
                answers.clear(); answers.addAll(base.answers()); selected=0;
                message="Original base restored in draft"; rebuildWidgets();
            } catch (RuntimeException error) { message=error.getMessage(); }
        }).bounds(left+224,136,72,20).build());
        save=addRenderableWidget(Button.builder(Component.literal(PartyRoomSync.connected()?"Save & share":"Save locally"),b -> save(false))
                .bounds(left,height-28,106,20).build()); save.active=!busy;
        var local=addRenderableWidget(Button.builder(Component.literal("Local only"),b -> save(true))
                .bounds(left+112,height-28,90,20).build()); local.active=!busy;
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b -> onClose()).bounds(left+208,height-28,88,20).build());
        setInitialFocus(original==null?trigger:input);
        if (busy) for (var child:children()) if (child instanceof AbstractWidget widget) widget.active=false;
    }
    private PartyReply definition() { return new PartyReply(triggerDraft.strip(),List.copyOf(answers),enabled); }
    private void save(boolean localOnly) {
        if (busy) return;
        try {
            retain(); var reply=definition();
            if (!localOnly && PartyRoomSync.connected()) {
                busy=true; message="Sharing…"; rebuildWidgets();
                PartyRoomSync.publish(reply,baseRevision,(success,detail) -> {
                    busy=false; message=detail;
                    if (!success) baseRevision=PartyRoomSync.revision();
                    if (success && Minecraft.getInstance().screen==this) onClose();
                    else if (Minecraft.getInstance().screen==this) rebuildWidgets();
                });
            } else { PartyReplyLibrary.save(reply); onClose(); }
        } catch (RuntimeException error) { message=error.getMessage(); busy=false; save.active=true; }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        int left=(width-296)/2;
        g.fill(0,0,width,height,0xEB0B101B); g.centeredText(font,title,width/2,16,0xFFFFFFFF);
        g.text(font,"Trigger",left,31,0xFFADBBD0);
        g.text(font,"Random answer "+(selected+1)+" / "+answers.size(),left,72,0xFFADBBD0);
        g.text(font,"Variables: {player}  {args}  {roll:1:6}",left,164,0xFFADBBD0);
        if (!message.isEmpty()) g.text(font,font.plainSubstrByWidth(message,width-24),12,height-47,0xFFFFFFFF);
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
