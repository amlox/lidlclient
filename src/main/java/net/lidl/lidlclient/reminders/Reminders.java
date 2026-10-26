package net.lidl.lidlclient.reminders;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.lidl.lidlclient.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import java.time.*;
import java.util.List;

/** Client-only commands and notifications; no server packets or background threads. */
public final class Reminders {
    private static final SystemToast.SystemToastId ALERT=new SystemToast.SystemToastId(5000L);
    private static ReminderBook book;
    private static boolean initialized;
    private static long nextAlertNanos;
    private Reminders() { }
    private static ReminderBook book() {
        if (book==null) book=ReminderBook.read(ClientConfig.store().get("reminders","entries"));
        return book;
    }
    private static void save() { ClientConfig.set("reminders","entries",book().toJson()); }
    public static List<ReminderBook.Entry> entries() { return book().entries(); }
    public static void remove(String id) { if (book().remove(id)) save(); }
    public static ReminderBook.Entry add(boolean timer,String time,String message) {
        var entry=timer?book().addTimer(time,message,System.currentTimeMillis())
                :book().addClock(time,message,Instant.now(),ZoneId.systemDefault());
        save(); return entry;
    }
    public static void open() {
        var client=Minecraft.getInstance();
        client.schedule(() -> client.setScreen(new RemindersScreen()));
    }
    public static void initialize() {
        if (initialized) return;
        initialized=true;
        book();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,registry) -> {
            for (String alias:List.of("reminders","rem"))
                dispatcher.register(ClientCommands.literal(alias).executes(context -> { open(); return 1; }));
            dispatcher.register(ClientCommands.literal("timer")
                    .executes(context -> { open(); return 1; })
                    .then(ClientCommands.argument("duration",StringArgumentType.word())
                            .executes(context -> startTimer(StringArgumentType.getString(context,"duration"),"Timer finished!"))
                            .then(ClientCommands.argument("message",StringArgumentType.greedyString())
                                    .executes(context -> startTimer(StringArgumentType.getString(context,"duration"),
                                            StringArgumentType.getString(context,"message"))))));
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Keep overdue entries until a world is available to display their notification.
            if (client.player==null || System.nanoTime()<nextAlertNanos) return;
            book().nextDue(System.currentTimeMillis()).ifPresent(entry -> {
                Component message=Component.literal("[Lidl "+entry.kind()+"] "+entry.message());
                client.player.sendSystemMessage(message);
                client.gui.setOverlayMessage(message,false);
                client.getToastManager().addToast(SystemToast.multiline(client,ALERT,
                        Component.literal("Lidl "+entry.kind()),Component.literal(entry.message())));
                client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP,1f));
                remove(entry.id());
                nextAlertNanos=System.nanoTime()+3_000_000_000L;
            });
        });
    }
    private static int startTimer(String duration,String message) {
        try {
            var entry=add(true,duration,message);
            feedback("Timer started: "+ReminderBook.remaining(entry.due(),System.currentTimeMillis())+" — "+entry.message());
            return 1;
        } catch (IllegalArgumentException error) {
            feedback(error.getMessage()); return 0;
        }
    }
    private static void feedback(String text) {
        var player=Minecraft.getInstance().player;
        if (player!=null) player.sendSystemMessage(Component.literal("[Reminders] "+text));
    }
    /** Route this mod's local commands for automatic party replies as well as typed chat. */
    public static boolean handles(String command) {
        String root=command.strip().split("\\s+",2)[0];
        return root.equals("timer") || root.equals("reminders") || root.equals("rem");
    }
    public static void execute(String command) {
        String[] parts=command.strip().split("\\s+",3);
        if (!parts[0].equals("timer")) {
            if (parts.length!=1) throw new IllegalArgumentException("Use /reminders or /rem.");
            open(); return;
        }
        if (parts.length==1) { open(); return; }
        var entry=add(true,parts[1],parts.length==3?parts[2]:"Timer finished!");
        feedback("Timer started: "+ReminderBook.remaining(entry.due(),System.currentTimeMillis())+" — "+entry.message());
    }
}
