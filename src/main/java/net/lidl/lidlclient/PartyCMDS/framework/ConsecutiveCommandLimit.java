package net.lidl.lidlclient.PartyCMDS.framework;

import java.util.Locale;

/** Counts completed outputs, across party members; rejected attempts never change the streak. */
public final class ConsecutiveCommandLimit {
    private String last;
    private int count;
    public boolean allows(String trigger,int limit) {
        return limit==0 || !trigger.toLowerCase(Locale.ROOT).equals(last) || count<limit;
    }
    public void completed(String trigger) {
        String normalized=trigger.toLowerCase(Locale.ROOT);
        if (normalized.equals(last)) {
            if (count<Integer.MAX_VALUE) count++;
        } else { last=normalized; count=1; }
    }
    public void reset() { last=null; count=0; }
}
