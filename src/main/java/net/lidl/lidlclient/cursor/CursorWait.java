package net.lidl.lidlclient.cursor;

/** One bounded cursor release; repeat presses cannot extend it indefinitely. */
public final class CursorWait {
    public enum Result { NONE, WAITING, HANDOFF, RESTORE }
    private boolean pending;
    private long startedAt;
    private long timeout;

    public boolean begin(long now, long duration) {
        if (pending || duration <= 0) return false;
        pending = true;
        startedAt = now;
        timeout = duration;
        return true;
    }
    public boolean pending() { return pending; }
    public boolean cancel() {
        boolean wasPending = pending;
        pending = false;
        return wasPending;
    }
    public Result update(long now, boolean screenOpen, boolean sameConnection, boolean focused) {
        if (!pending) return Result.NONE;
        if (screenOpen || !sameConnection || !focused) {
            cancel();
            return Result.HANDOFF;
        }
        long elapsed = now - startedAt;
        if (elapsed < 0 || elapsed >= timeout) {
            cancel();
            return Result.RESTORE;
        }
        return Result.WAITING;
    }
}
