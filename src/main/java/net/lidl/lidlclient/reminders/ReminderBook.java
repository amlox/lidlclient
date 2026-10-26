package net.lidl.lidlclient.reminders;

import com.google.gson.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.Pattern;

/** Local one-shot alarms; deadlines survive restarts and missed ticks. */
public final class ReminderBook {
    public static final int MAX_ENTRIES=128;
    public record Entry(String id,String kind,long due,String message) {
        public Entry {
            UUID.fromString(id);
            if (!kind.equals("Timer") && !kind.equals("Reminder")) throw new IllegalArgumentException("Invalid reminder type");
            if (due<0) throw new IllegalArgumentException("Invalid due time");
            message=validateMessage(message);
        }
    }
    private final List<Entry> entries=new ArrayList<>();
    private static final Pattern DURATION=Pattern.compile("(\\d+)([dhms])",Pattern.CASE_INSENSITIVE);
    public static String validateMessage(String message) {
        if (message==null || message.isBlank() || message.length()>256 || message.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Enter a message of 1–256 characters.");
        return message.strip();
    }
    public static long durationMillis(String text) {
        if (text==null || text.length()>64) throw new IllegalArgumentException("Use a duration such as 5m or 1h30m.");
        String value=text.strip();
        var matcher=DURATION.matcher(value);
        int end=0; long seconds=0;
        try {
            while (matcher.find()) {
                if (matcher.start()!=end) throw new IllegalArgumentException("Use a duration such as 5m or 1h30m.");
                long unit=switch(matcher.group(2).toLowerCase(Locale.ROOT)) {
                    case "d" -> 86400; case "h" -> 3600; case "m" -> 60; default -> 1;
                };
                seconds=Math.addExact(seconds,Math.multiplyExact(Long.parseLong(matcher.group(1)),unit));
                end=matcher.end();
            }
            if (end!=value.length() || seconds<1 || seconds>365L*86400)
                throw new IllegalArgumentException("Duration must be 1 second–365 days, e.g. 5m.");
            return seconds*1000;
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException("Duration is too large.");
        }
    }
    public static long nextClockTime(String text,Instant now,ZoneId zone) {
        try {
            LocalTime time=LocalTime.parse(text.strip(),DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT));
            ZonedDateTime local=now.atZone(zone);
            ZonedDateTime target=local.toLocalDate().atTime(time).atZone(zone);
            if (!target.toInstant().isAfter(now)) target=local.toLocalDate().plusDays(1).atTime(time).atZone(zone);
            return target.toInstant().toEpochMilli();
        } catch (DateTimeException error) {
            throw new IllegalArgumentException("Use a 24-hour PC time, e.g. 18:30.");
        }
    }
    public Entry addTimer(String duration,String message,long now) {
        return add("Timer",Math.addExact(now,durationMillis(duration)),message);
    }
    public Entry addClock(String time,String message,Instant now,ZoneId zone) {
        return add("Reminder",nextClockTime(time,now,zone),message);
    }
    private Entry add(String kind,long due,String message) {
        if (entries.size()>=MAX_ENTRIES) throw new IllegalArgumentException("Remove a reminder first (128 maximum).");
        Entry entry=new Entry(UUID.randomUUID().toString(),kind,due,message);
        entries.add(entry); return entry;
    }
    public List<Entry> entries() {
        return entries.stream().sorted(Comparator.comparingLong(Entry::due)).toList();
    }
    public Optional<Entry> nextDue(long now) { return entries().stream().filter(e -> e.due()<=now).findFirst(); }
    public boolean remove(String id) { return entries.removeIf(e -> e.id().equals(id)); }
    public JsonElement toJson() { return new Gson().toJsonTree(entries); }
    public static ReminderBook read(JsonElement json) {
        var book=new ReminderBook();
        if (json==null || !json.isJsonArray()) return book;
        Set<String> ids=new HashSet<>();
        for (var item:json.getAsJsonArray()) {
            if (book.entries.size()>=MAX_ENTRIES) break;
            try {
                var o=item.getAsJsonObject();
                var e=new Entry(o.get("id").getAsString(),o.get("kind").getAsString(),
                        o.get("due").getAsBigDecimal().longValueExact(),o.get("message").getAsString());
                if (ids.add(e.id())) book.entries.add(e);
            } catch (RuntimeException ignored) { }
        }
        return book;
    }
    public static String remaining(long due,long now) {
        if (due<=now) return "Due";
        long seconds=(due-now+999)/1000;
        long days=seconds/86400,hours=seconds/3600%24,minutes=seconds/60%60;
        return (days>0?days+"d ":"")+(hours>0?hours+"h ":"")+(minutes>0?minutes+"m ":"")+seconds%60+"s";
    }
}
