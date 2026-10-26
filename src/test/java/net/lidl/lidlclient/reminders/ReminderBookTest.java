package net.lidl.lidlclient.reminders;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class ReminderBookTest {
    @Test void durationsSupportMixedUnitsAndRejectInvalidOrExcessiveInputs() {
        assertEquals(300000,ReminderBook.durationMillis("5m"));
        assertEquals(5400000,ReminderBook.durationMillis("1h30m"));
        assertEquals(93784000,ReminderBook.durationMillis("1D2H3M4S"));
        for (String input:new String[]{"","0s","-5m","5","1.5m","5min","1h 5m","1hno","366d","999999999999999999999999h"})
            assertThrows(IllegalArgumentException.class,() -> ReminderBook.durationMillis(input),input);
    }
    @Test void clockAlarmUsesPcZoneAndNextOccurrence() {
        ZoneId zone=ZoneId.of("Europe/Warsaw");
        Instant now=Instant.parse("2026-10-27T15:00:00Z");
        assertEquals(Instant.parse("2026-10-27T17:30:00Z").toEpochMilli(),ReminderBook.nextClockTime("18:30",now,zone));
        assertEquals(Instant.parse("2026-10-28T14:00:00Z").toEpochMilli(),ReminderBook.nextClockTime("15:00",now,zone));
        assertEquals(Instant.parse("2026-10-28T15:00:00Z").toEpochMilli(),ReminderBook.nextClockTime("16:00",now,zone));
        for (String input:new String[]{"24:00","18:60","5pm","18:30:00",""})
            assertThrows(IllegalArgumentException.class,() -> ReminderBook.nextClockTime(input,now,zone));
    }
    @Test void clockSchedulingHandlesDaylightSavingAndMidnight() {
        ZoneId zone=ZoneId.of("Europe/Warsaw");
        Instant now=Instant.parse("2026-03-28T23:00:00Z");
        // Java's zone rules move a nonexistent 02:30 forward into the DST gap's end.
        assertEquals(Instant.parse("2026-03-29T01:30:00Z").toEpochMilli(),ReminderBook.nextClockTime("02:30",now,zone));
        assertEquals(Instant.parse("2026-10-28T00:05:00Z").toEpochMilli(),
                ReminderBook.nextClockTime("00:05",Instant.parse("2026-10-27T23:59:00Z"),ZoneOffset.UTC));
    }
    @Test void dueEntriesSurviveMissedTicksAndFireOnlyOnceWhenAcknowledged() {
        var book=new ReminderBook();
        var timer=book.addTimer("5m","Tea is ready",1000);
        assertTrue(book.nextDue(300999).isEmpty());
        assertEquals(timer,book.nextDue(301000).orElseThrow());
        assertEquals(timer,book.nextDue(999999).orElseThrow());
        assertTrue(book.remove(timer.id()));
        assertTrue(book.nextDue(999999).isEmpty());
        assertFalse(book.remove(timer.id()));
    }
    @Test void restartPreservesOverdueEntriesCancellationAndSortedOrder() {
        var book=new ReminderBook();
        var later=book.addTimer("1h","Later",1000);
        var first=book.addTimer("1s","First",1000);
        var restored=ReminderBook.read(book.toJson());
        assertEquals(book.entries(),restored.entries());
        assertEquals(first,restored.nextDue(10000).orElseThrow());
        restored.remove(first.id());
        restored=ReminderBook.read(restored.toJson());
        assertEquals(java.util.List.of(later),restored.entries());
    }
    @Test void invalidSavedEntriesAreSkippedWithoutLosingValidOnesOrDuplicatingAlerts() {
        var book=new ReminderBook();
        var entry=book.addTimer("1s","Hello",0);
        String good=book.toJson().getAsJsonArray().get(0).toString();
        var restored=ReminderBook.read(JsonParser.parseString("["+good+",null,{}, "+good+"]"));
        assertEquals(java.util.List.of(entry),restored.entries());
    }
    @Test void capacityAndMessagesAreValidated() {
        var book=new ReminderBook();
        for (String message:new String[]{""," ","bad\nline","x".repeat(257)})
            assertThrows(IllegalArgumentException.class,() -> book.addTimer("1s",message,0));
        assertTrue(book.entries().isEmpty());
        for (int i=0;i<ReminderBook.MAX_ENTRIES;i++) book.addTimer("1s","Hello",0);
        assertThrows(IllegalArgumentException.class,() -> book.addTimer("1s","One too many",0));
        book.remove(book.entries().get(0).id());
        assertDoesNotThrow(() -> book.addTimer("1s","Fits again",0));
    }
    @Test void countdownRoundsUpAndNeverDisplaysNegativeTime() {
        assertEquals("1s",ReminderBook.remaining(1001,1000));
        assertEquals("1h 30m 0s",ReminderBook.remaining(5400000,0));
        assertEquals("Due",ReminderBook.remaining(1000,1001));
    }
    @Test void onlyExactLocalCommandRootsAreIntercepted() {
        assertTrue(Reminders.handles("timer 5m Tea"));
        assertTrue(Reminders.handles("rem"));
        assertTrue(Reminders.handles("reminders"));
        assertFalse(Reminders.handles("timerother 5m"));
        assertFalse(Reminders.handles("pc timer 5m"));
    }
}
