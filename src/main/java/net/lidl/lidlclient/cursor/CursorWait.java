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
    /** Consume one screen handoff; expired/cancelled waits never count as a menu opening. */
    public boolean acceptScreen(long now, boolean sameConnection, boolean focused) {
        long elapsed = now - startedAt;
        return cancel() && sameConnection && focused && elapsed >= 0 && elapsed < timeout;
    }
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
