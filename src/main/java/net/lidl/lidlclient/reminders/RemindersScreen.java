package net.lidl.lidlclient.reminders;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class RemindersScreen extends Screen {
    private boolean timer;
    private String timeDraft="18:30",messageDraft="Reminder!",status="";
    private int page,perPage;
    private List<ReminderBook.Entry> visible=List.of();
    public RemindersScreen() { super(Component.literal("Timers & reminders")); }
    @Override protected void init() {
        int left=width/2-150;
        perPage=Math.max(1,(height-194)/26);
        visible=Reminders.entries();
        page=Math.clamp(page,0,Math.max(0,(visible.size()-1)/perPage));
        var time=addRenderableWidget(new EditBox(font,left,52,146,20,Component.literal(timer?"Duration":"PC time")));
        time.setMaxLength(64); time.setValue(timeDraft);
        time.setResponder(value -> timeDraft=value);
        addRenderableWidget(Button.builder(Component.literal(timer?"Mode: Timer":"Mode: PC time"),b -> {
            timer=!timer; timeDraft=timer?"5m":"18:30"; status=""; rebuildWidgets();
        }).bounds(left+154,52,146,20).build());
        var message=addRenderableWidget(new EditBox(font,left,88,300,20,Component.literal("Reminder message")));
        message.setMaxLength(256); message.setValue(messageDraft);
        message.setResponder(value -> messageDraft=value);
        addRenderableWidget(Button.builder(Component.literal(timer?"Start timer":"Add reminder"),b -> {
            try {
                var entry=Reminders.add(timer,timeDraft,messageDraft);
                status="Added: "+DateTimeFormatter.ofPattern("EEE HH:mm:ss").format(Instant.ofEpochMilli(entry.due()).atZone(ZoneId.systemDefault()));
                rebuildWidgets();
            } catch (IllegalArgumentException error) { status=error.getMessage(); }
        }).bounds(left,114,146,20).build());
        addRenderableWidget(Button.builder(Component.literal("Test sound"),b ->
                Minecraft.getInstance().getSoundManager().play(
                        net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,1f)))
                .bounds(left+154,114,146,20).build());
        for (int row=0;row<perPage && page*perPage+row<visible.size();row++) {
            var entry=visible.get(page*perPage+row);
            addRenderableWidget(Button.builder(Component.literal("Cancel"),b -> {
                Reminders.remove(entry.id()); rebuildWidgets();
            }).bounds(left+246,158+row*26,54,20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("<"),b -> { page--; rebuildWidgets(); })
                .bounds(left,height-28,40,20).build()).active=page>0;
        addRenderableWidget(Button.builder(Component.literal("Done"),b -> onClose())
                .bounds(width/2-50,height-28,100,20).build());
        addRenderableWidget(Button.builder(Component.literal(">"),b -> { page++; rebuildWidgets(); })
                .bounds(left+260,height-28,40,20).build()).active=(page+1)*perPage<visible.size();
        setInitialFocus(time);
    }
    @Override public void tick() {
        if (!visible.equals(Reminders.entries())) rebuildWidgets();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        int left=width/2-150;
        long now=System.currentTimeMillis();
        var local=Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault());
        g.fill(0,0,width,height,0xEB0B101B);
        g.centeredText(font,title,width/2,12,0xFFFFFFFF);
        g.centeredText(font,"PC: "+local.format(DateTimeFormatter.ofPattern("HH:mm:ss"))+"  "+local.getZone(),width/2,27,0xFFADBBD0);
        g.text(font,timer?"Duration: 5m / 1h30m":"Next local time: HH:mm",left,41,0xFFADBBD0);
        g.text(font,"Message",left,77,0xFFADBBD0);
        g.text(font,font.plainSubstrByWidth(status.isEmpty()?"Active: "+visible.size()+" (sound follows game volume)":status,300),left,141,0xFFFFFFFF);
        if (visible.isEmpty()) g.text(font,"No active timers or reminders.",left,163,0xFFADBBD0);
        for (int row=0;row<perPage && page*perPage+row<visible.size();row++) {
            var entry=visible.get(page*perPage+row);
            int y=158+row*26;
            String when=entry.kind().equals("Timer")?ReminderBook.remaining(entry.due(),now):
                    Instant.ofEpochMilli(entry.due()).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE HH:mm"));
            g.text(font,entry.kind()+" · "+when,left,y,0xFF92D7FF);
            g.text(font,font.plainSubstrByWidth(entry.message(),240),left,y+11,0xFFFFFFFF);
        }
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void onClose() { Minecraft.getInstance().setScreen(null); }
    @Override public boolean isPauseScreen() { return false; }
}
