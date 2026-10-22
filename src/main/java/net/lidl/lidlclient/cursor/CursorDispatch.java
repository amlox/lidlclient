package net.lidl.lidlclient.cursor;

/** Defer cursor changes until a key has had a complete client tick to reach other mods. */
public final class CursorDispatch {
    private boolean pending, ready;
    public boolean queue() {
        if (pending) return false;
        pending = true;
        ready = false;
        return true;
    }
    public boolean pending() { return pending; }
    public void afterTick() { if (pending) ready = true; }
    public boolean consume() {
        if (!pending || !ready) return false;
        cancel();
        return true;
    }
    public void cancel() { pending = false; ready = false; }
}
