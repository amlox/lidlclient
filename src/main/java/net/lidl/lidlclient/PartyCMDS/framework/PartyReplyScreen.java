package net.lidl.lidlclient.PartyCMDS.framework;

import net.lidl.lidlclient.PartyCMDS.PartyCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** One answer at a time, with explicit save/cancel and a selectable output destination. */
public final class PartyReplyScreen extends Screen {
    private final Screen parent;
    private final String original;
    private final List<String> answers=new ArrayList<>();
    private int selected;
    private boolean enabled=true,partyChat=true,bypassLoopProtection,runCommands;
    private EditBox trigger,input;
    private String triggerDraft="!hello",message="";
    public PartyReplyScreen(Screen parent,String trigger) {
        super(Component.literal(trigger==null?"New party reply":"Edit "+trigger));
        this.parent=parent; original=trigger;
        if (trigger==null) answers.add("Hello {player}!");
        else {
            var reply=PartyCommands.editableReply(trigger);
            answers.addAll(reply.answers()); enabled=reply.enabled(); partyChat=reply.partyChat(); triggerDraft=reply.trigger();
            bypassLoopProtection=reply.bypassLoopProtection(); runCommands=reply.runCommands();
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
        trigger.setResponder(value -> {
            triggerDraft=value;
            if (original==null) {
                try { PartyReply.requireAvailable(value,PartyCommands.commandNames()); message=""; }
                catch (IllegalArgumentException error) { message=error.getMessage(); }
            }
        });
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
        addRenderableWidget(Button.builder(Component.literal(runCommands?"Output: /Commands":partyChat?"Output: Party chat":"Output: Only me"),b -> {
            retain();
            if (partyChat) { partyChat=false; runCommands=false; }
            else if (!runCommands) runCommands=true;
            else { partyChat=true; runCommands=false; }
            rebuildWidgets();
        }).bounds(left,136,146,20).build());
        addRenderableWidget(Button.builder(Component.literal("Allow ! replies: "+(bypassLoopProtection?"On":"Off")),b -> {
            retain(); bypassLoopProtection=!bypassLoopProtection; rebuildWidgets();
        }).bounds(left+150,136,146,20).build());
        addRenderableWidget(Button.builder(Component.literal("Preview"),b -> {
            try { retain(); message=definition().render("TestPlayer","example arguments"); }
            catch (RuntimeException error) { message=error.getMessage(); }
        }).bounds(left,164,144,20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset base"),b -> {
            retain();
            try {
                var base=PartyCommands.baseReply(triggerDraft.strip());
                answers.clear(); answers.addAll(base.answers()); selected=0;
                message="Original base restored in draft"; rebuildWidgets();
            } catch (RuntimeException error) { message=error.getMessage(); }
        }).bounds(left+150,164,146,20).build());
        addRenderableWidget(Button.builder(Component.literal("Save"),b -> save())
                .bounds(left,height-28,144,20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b -> onClose()).bounds(left+150,height-28,146,20).build());
        setInitialFocus(original==null?trigger:input);
    }
    private PartyReply definition() { return new PartyReply(triggerDraft.strip(),List.copyOf(answers),enabled,partyChat,bypassLoopProtection,runCommands); }
    private void save() {
        try {
            retain(); var reply=definition();
            if (reply.runCommands()) for (String answer:reply.answers()) reply.commandToRun(answer);
            if (original==null) PartyReplyLibrary.create(reply); else PartyReplyLibrary.save(reply);
            onClose();
        } catch (RuntimeException error) { message=error.getMessage(); }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        int left=(width-296)/2;
        g.fill(0,0,width,height,0xEB0B101B); g.centeredText(font,title,width/2,16,0xFFFFFFFF);
        g.text(font,"Trigger",left,31,0xFFADBBD0);
        g.text(font,"Answer "+(selected+1)+"/"+answers.size()+"  {player} {args} {roll:1:6}",left,72,0xFFADBBD0);
        if (!message.isEmpty()) g.text(font,font.plainSubstrByWidth(message,width-24),12,height-47,0xFFFFFFFF);
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
